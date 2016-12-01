/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2016 Board of Regents of the University of
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

package net.imagej.plugins.commands.restructure;

import java.util.ArrayList;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Adds a new axis to an input Dataset.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Axes", mnemonic = 'a'), @Menu(label = "Add Axis...") },
	headless = true, initializer = "initAll", attrs = { @Attr(name = "no-legacy") })
public class AddAxis extends DynamicCommand {

	// -- Constants --

	private static final String AXIS_NAME = "axisName";
	private static final String AXIS_SIZE = "axisSize";

	// -- Parameters --

	@Parameter(type = ItemIO.BOTH)
	private Dataset dataset;

	@Parameter(label = "Axis to add", persist = false)
	private String axisName;

	@Parameter(label = "Axis size", persist = false)
	private long axisSize = 2;

	// -- AddAxis methods --

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(final Dataset dataset) {
		this.dataset = dataset;
	}

	public AxisType getAxis() {
		return Axes.get(axisName);
	}

	public void setAxis(final AxisType axis) {
		axisName = axis.toString();
	}

	public long getAxisSize() {
		return axisSize;
	}

	public void setAxisSize(final long axisSize) {
		this.axisSize = axisSize;
	}

	// -- Runnable methods --

	/**
	 * Creates new ImgPlus data with an additional axis. Sets pixels of 1st
	 * hyperplane of new imgPlus to original imgPlus data. Assigns the ImgPlus to
	 * the input Dataset.
	 */
	@Override
	public void run() {
		final AxisType axis = Axes.get(axisName);
		if (inputBad(axis)) return;
		final AxisType[] newAxes = getNewAxes(dataset, axis);
		final long[] newDimensions = getNewDimensions(dataset, axisSize);
		final ImgPlus<? extends RealType<?>> dstImgPlus =
			RestructureUtils.createNewImgPlus(dataset, newDimensions, newAxes);
		fillNewImgPlus(dataset.getImgPlus(), dstImgPlus);
		dstImgPlus.setCompositeChannelCount(dataset.getCompositeChannelCount());
		RestructureUtils.allocateColorTables(dstImgPlus);
		final ColorTableRemapper remapper =
			new ColorTableRemapper(new RemapAlgorithm());
		remapper.remapColorTables(dataset.getImgPlus(), dstImgPlus);
		// TODO - metadata, etc.?
		dataset.setImgPlus(dstImgPlus);
	}

	// -- Initializers --

	protected void initAll() {
		initAxisName();
		initAxisSize();
	}

	// -- Helper methods --

	/**
	 * Detects if user specified data is invalid
	 */
	private boolean inputBad(final AxisType axis) {
		// axis not determined by dialog
		if (axis == null) {
			cancel("Axis must not be null.");
			return true;
		}

		// axis already present in Dataset
		final int axisIndex = dataset.dimensionIndex(axis);
		if (axisIndex >= 0) {
			cancel("Axis "+axis.getLabel()+" already present in dataset.");
			return true;
		}

		// axis size invalid
		if (axisSize <= 0) {
			cancel("Axis size invalid: "+axisSize);
			return true;
		}

		return false;
	}

	/**
	 * Creates an Axis[] that consists of all the axes from a Dataset and an
	 * additional axis appended.
	 */
	private AxisType[] getNewAxes(final Dataset ds, final AxisType axis) {
		final AxisType[] newAxes = new AxisType[ds.numDimensions() + 1];
		for (int i = 0; i < ds.numDimensions(); i++)
			newAxes[i] = ds.axis(i).type();
		newAxes[newAxes.length - 1] = axis;
		return newAxes;
	}

	/**
	 * Creates a long[] that consists of all the dimensions from a Dataset and an
	 * additional value appended.
	 */
	private long[]
		getNewDimensions(final Dataset ds, final long lastDimensionSize)
	{
		final long[] newDims = new long[ds.numDimensions() + 1];
		for (int d = 0; d < ds.numDimensions(); d++)
			newDims[d] = ds.dimension(d);
		newDims[newDims.length - 1] = lastDimensionSize;
		return newDims;
	}

	/**
	 * Fills the 1st hyperplane in the new Dataset with the entire contents of the
	 * original image
	 */
	private void fillNewImgPlus(final ImgPlus<? extends RealType<?>> srcImgPlus,
		final ImgPlus<? extends RealType<?>> dstImgPlus)
	{
		final long[] srcOrigin = new long[srcImgPlus.numDimensions()];
		final long[] dstOrigin = new long[dstImgPlus.numDimensions()];

		final long[] srcSpan = new long[srcOrigin.length];
		final long[] dstSpan = new long[dstOrigin.length];

		srcImgPlus.dimensions(srcSpan);
		dstImgPlus.dimensions(dstSpan);
		dstSpan[dstSpan.length - 1] = 1;

		RestructureUtils.copyHyperVolume(srcImgPlus, srcOrigin, srcSpan,
			dstImgPlus, dstOrigin, dstSpan);
	}

	private class RemapAlgorithm implements ColorTableRemapper.RemapAlgorithm {

		@Override
		public boolean isValidSourcePlane(final long i) {
			return true;
		}

		@Override
		public void remapPlanePosition(final long[] origPlaneDims,
			final long[] origPlanePos, final long[] newPlanePos)
		{
			for (int i = 0; i < origPlaneDims.length; i++)
				newPlanePos[i] = origPlanePos[i];
			newPlanePos[newPlanePos.length - 1] = 0;
		}
	}

	private void initAxisName() {
		final MutableModuleItem<String> axisNameItem =
			getInfo().getMutableInput(AXIS_NAME, String.class);
		final ArrayList<String> choices = new ArrayList<String>();
		for (final AxisType axis : Axes.knownTypes()) {
			if (axis.isXY()) continue;
			if (getDataset().dimensionIndex(axis) < 0) choices.add(axis.getLabel());
		}
		axisNameItem.setChoices(choices);
	}

	private void initAxisSize() {
		final MutableModuleItem<Long> axisSizeModuleItem =
			getInfo().getMutableInput(AXIS_SIZE, Long.class);
		axisSizeModuleItem.setMinimumValue(2L);
	}

}
