/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.plugins.commands.typechange;

import java.util.ArrayList;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ColorTables;
import net.imagej.ops.ComputerOp;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.space.SpaceUtils;
import net.imagej.types.BigComplex;
import net.imagej.types.DataType;
import net.imagej.types.DataTypeService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * TypeChanger changes the type of the data in a {@link Dataset}. The
 * {@link DataType} of the data is chosen from the types discovered at runtime
 * by the {@link DataTypeService}. Channels can be combined in the process if
 * desired. Combination is done via channel averaging. After conversion data
 * values are preserved as much as possible but do get clamped to the new data
 * type's valid range.
 *
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = { @Menu(label = MenuConstants.IMAGE_LABEL,
	weight = MenuConstants.IMAGE_WEIGHT, mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Type", mnemonic = 't'), @Menu(label = "Change...",
		mnemonic = 'c') }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class TypeChanger<U extends RealType<U>, V extends RealType<V> & NativeType<V>>
	extends DynamicCommand
{

	// TODO: expects types to be based on RealType and sometimes NativeType. The
	// as yet to be used unbounded types defined in the data types package don't
	// support NativeType. At some point we need to relax these constraints such
	// that U and V just extend Type<U> and Type<V>. The DatasetService must be
	// able to make Datasets that have this kind of signature:
	// ImgPlus<? extends Type<?>>. And the Img opening/saving routines also need
	// to be able to encode arbitrary types.

	// -- Parameters --

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private DataTypeService dataTypeService;

	@Parameter
	private OpService opService;

	@Parameter
	private Dataset data;

	@Parameter(label = "Type", persist = false, initializer = "init")
	private String typeName;

	@Parameter(label = "Combine channels", persist = false)
	private boolean combineChannels;

	// -- Command methods --

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		final Class<?> typeClass = data.getImgPlus().firstElement().getClass();
		final DataType<U> inType = (DataType<U>) dataTypeService.getTypeByClass(
			typeClass);
		final DataType<V> outType = (DataType<V>) dataTypeService.getTypeByName(
			typeName);
		final int chAxis = data.dimensionIndex(Axes.CHANNEL);
		final long channelCount = (chAxis < 0) ? 1 : data.dimension(chAxis);
		Dataset newData;
		if (combineChannels && channelCount > 1 &&
			channelCount <= Integer.MAX_VALUE)
		{
			newData = channelAveragingCase(inType, outType, chAxis);
		}
		else { // straight 1 for 1 pixel casting
			newData = channelPreservingCase(inType, outType);
		}
		data.setImgPlus(newData.getImgPlus());
		data.setRGBMerged(false); // we never end up with RGB merged data
	}

	// -- initializers --

	protected void init() {
		final MutableModuleItem<String> input = getInfo().getMutableInput(
			"typeName", String.class);
		final List<String> choices = new ArrayList<String>();
		for (final DataType<?> dataType : dataTypeService.getInstances()) {
			choices.add(dataType.longName());
		}
		input.setChoices(choices);
		final RealType<?> dataVar = data.getImgPlus().firstElement();
		final DataType<?> type = dataTypeService.getTypeByClass(dataVar.getClass());
		if (type == null) input.setValue(this, choices.get(0));
		else input.setValue(this, type.longName());
	}

	// -- helpers --

	private Dataset channelAveragingCase(final DataType<U> inType,
		final DataType<V> outType, final int chAxis)
	{
		final ImgPlus<U> imgIn = data.typedImg(inType.getType());
		final long[] dims = calcDims(Intervals.dimensionsAsLongArray(data), chAxis);
		final AxisType[] axes = calcAxes(SpaceUtils.getAxisTypes(data), chAxis);
		final Dataset newData = datasetService.create(outType.createVariable(),
			dims, "Converted Image", axes);
		final ImgPlus<V> imgOut = newData.typedImg(outType.getType());
		// TODO: not sure if we need converters that work with BigComplex
		// We could use DataType.cast() to convert instead of relying on the serice
		final ComputerOp<Iterable<U>, V> meanOp = opService.computer(
			Ops.Stats.Mean.class, outType.getType(), (Iterable<U>) imgIn);
		opService.image().project(imgOut, imgIn, meanOp, chAxis);
		copyMetaDataChannelsCase(data.getImgPlus(), newData.getImgPlus());
		return newData;
	}

	private Dataset channelPreservingCase(final DataType<U> inType,
		final DataType<V> outType)
	{
		final Dataset newData = datasetService.create(outType.createVariable(),
			Intervals.dimensionsAsLongArray(data), "Converted Image", SpaceUtils
				.getAxisTypes(data));
		final Cursor<U> inCursor = data.typedImg(inType.getType()).cursor();
		final RandomAccess<V> outAccessor = newData.typedImg(outType.getType())
			.randomAccess();
		final BigComplex tmp = new BigComplex();
		while (inCursor.hasNext()) {
			inCursor.fwd();
			outAccessor.setPosition(inCursor);
			dataTypeService.cast(inType, inCursor.get(), outType, outAccessor.get(),
				tmp);
		}
		copyMetaDataDefaultCase(data.getImgPlus(), newData.getImgPlus());
		return newData;
	}

	private void copyMetaDataDefaultCase(final ImgPlus<?> src,
		final ImgPlus<?> dest)
	{

		// dims and axes already correct

		// name
		dest.setName(src.getName());

		// color tables
		final int tableCount = src.getColorTableCount();
		dest.initializeColorTables(tableCount);
		for (int i = 0; i < tableCount; i++) {
			dest.setColorTable(src.getColorTable(i), i);
		}

		// channel min/maxes
		final int chAxis = src.dimensionIndex(Axes.CHANNEL);
		int channels;
		if (chAxis < 0) channels = 1;
		else channels = (int) src.dimension(chAxis);
		for (int i = 0; i < channels; i++) {
			final double min = src.getChannelMinimum(i);
			final double max = src.getChannelMaximum(i);
			dest.setChannelMinimum(i, min);
			dest.setChannelMaximum(i, max);
		}

		// dimensional axes
		for (int d = 0; d < src.numDimensions(); d++) {
			dest.setAxis(src.axis(d).copy(), d);
		}
	}

	private void copyMetaDataChannelsCase(final ImgPlus<?> src,
		final ImgPlus<?> dest)
	{

		final int chAxis = src.dimensionIndex(Axes.CHANNEL);

		// dims and axes already correct

		// name
		dest.setName(src.getName());

		// color tables
		// ACK what is best here?
		final int tableCount = (int) calcTableCount(src, chAxis);
		dest.initializeColorTables(tableCount);
		for (int i = 0; i < tableCount; i++) {
			dest.setColorTable(ColorTables.GRAYS, i);
		}

		// channel min/maxes
		double min = src.getChannelMinimum(0);
		double max = src.getChannelMaximum(0);
		int channels;
		if (chAxis < 0) channels = 1;
		else channels = (int) src.dimension(chAxis);
		for (int i = 1; i < channels; i++) {
			min = Math.min(min, src.getChannelMinimum(i));
			max = Math.max(max, src.getChannelMaximum(i));
		}
		dest.setChannelMinimum(0, min);
		dest.setChannelMaximum(0, max);

		// dimensional axes
		int dDest = 0;
		for (int dSrc = 0; dSrc < src.numDimensions(); dSrc++) {
			if (dSrc == chAxis) continue;
			dest.setAxis(src.axis(dSrc).copy(), dDest++);
		}
	}

	private long[] calcDims(final long[] dims, final int chAxis) {
		final long[] outputDims = new long[dims.length - 1];
		int d = 0;
		for (int i = 0; i < dims.length; i++) {
			if (i == chAxis) continue;
			outputDims[d++] = dims[i];
		}
		return outputDims;
	}

	private AxisType[] calcAxes(final AxisType[] axes, final int chAxis) {
		final AxisType[] outputAxes = new AxisType[axes.length - 1];
		int d = 0;
		for (int i = 0; i < axes.length; i++) {
			if (i == chAxis) continue;
			outputAxes[d++] = axes[i];
		}
		return outputAxes;
	}

	private long calcTableCount(final ImgPlus<?> src, final int chAxis) {
		long count = 1;
		for (int i = 0; i < src.numDimensions(); i++) {
			if (i == chAxis) continue;
			count *= src.dimension(i);
		}
		return count;
	}
}
