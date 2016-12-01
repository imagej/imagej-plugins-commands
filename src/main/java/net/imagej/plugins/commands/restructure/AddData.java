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
import net.imagej.space.SpaceUtils;
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
 * Adds hyperplanes of data to an input Dataset along a user specified axis.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Data", mnemonic = 'd'), @Menu(label = "Add Data...") },
	headless = true, initializer = "initAll", attrs = { @Attr(name = "no-legacy") })
public class AddData extends DynamicCommand {

	// -- Constants --

	private static final String AXIS_NAME = "axisName";
	private static final String POSITION = "position";
	private static final String QUANTITY = "quantity";

	// -- Parameters --

	@Parameter(type = ItemIO.BOTH)
	private Dataset dataset;

	@Parameter(label = "Axis to modify", persist = false,
		callback = "parameterChanged")
	private String axisName;

	@Parameter(label = "Insertion position", persist = false,
		callback = "parameterChanged")
	private long position = 1;

	@Parameter(label = "Insertion quantity", persist = false,
		callback = "parameterChanged")
	private long quantity = 1;

	// -- AddData methods --

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

	public long getPosition() {
		return position;
	}

	public void setPosition(final long position) {
		this.position = position;
	}

	public long getQuantity() {
		return quantity;
	}

	public void setQuantity(final long quantity) {
		this.quantity = quantity;
	}

	// -- Runnable methods --

	/**
	 * Creates new ImgPlus data copying pixel values as needed from an input
	 * Dataset. Assigns the ImgPlus to the input Dataset.
	 */
	@Override
	public void run() {
		final AxisType axis = Axes.get(axisName);
		if (inputBad(axis)) return;
		final AxisType[] axes = SpaceUtils.getAxisTypes(dataset);
		final long[] newDimensions =
			RestructureUtils.getDimensions(dataset, axis, quantity);
		final ImgPlus<? extends RealType<?>> dstImgPlus =
			RestructureUtils.createNewImgPlus(dataset, newDimensions, axes);
		fillNewImgPlus(dataset.getImgPlus(), dstImgPlus, axis);
		final int compositeChannelCount =
			compositeStatus(dataset, dstImgPlus, axis);
		dstImgPlus.setCompositeChannelCount(compositeChannelCount);
		RestructureUtils.allocateColorTables(dstImgPlus);
		if (axis.isXY()) {
			RestructureUtils.copyColorTables(dataset.getImgPlus(), dstImgPlus);
		}
		else {
			final ColorTableRemapper remapper =
				new ColorTableRemapper(new RemapAlgorithm());
			remapper.remapColorTables(dataset.getImgPlus(), dstImgPlus);
		}
		// TODO - metadata, etc.?
		dataset.setImgPlus(dstImgPlus);
	}

	// -- Initializers --

	protected void initAll() {
		initAxisName();
		initPosition();
		initQuantity();
	}

	// -- Callbacks --

	protected void parameterChanged() {
		setPositionRange();
		setQuantityRange();
		clampPosition();
		clampQuantity();
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

		// setup some working variables
		final int axisIndex = dataset.dimensionIndex(axis);
		final long axisSize = dataset.getImgPlus().dimension(axisIndex);

		// axis not present in Dataset
		if (axisIndex < 0) {
			cancel("Axis " + axis.getLabel() + " is not present in input dataset.");
			return true;
		}

		// bad value for startPosition
		if (position < 1 || position > axisSize + 1) {
			cancel("Insertion position is out of bounds.");
			return true;
		}

		// bad value for numAdding
		if (quantity <= 0 || (quantity > Long.MAX_VALUE - axisSize)) {
			cancel("Insertion quantity is out of bounds.");
			return true;
		}

		// if here everything is okay
		return false;
	}

	/**
	 * Fills the newly created ImgPlus with data values from a smaller source
	 * image. Copies data from existing hyperplanes.
	 */
	private void
		fillNewImgPlus(final ImgPlus<? extends RealType<?>> srcImgPlus,
			final ImgPlus<? extends RealType<?>> dstImgPlus,
			final AxisType modifiedAxis)
	{
		final int axisIndex = dataset.dimensionIndex(modifiedAxis);
		final long axisSize = dataset.dimension(axisIndex);
		final long numBeforeInsert = position - 1; // one-based position
		final long numInInsertion = quantity;
		final long numAfterInsertion = axisSize - numBeforeInsert;

		RestructureUtils.copyData(srcImgPlus, dstImgPlus, modifiedAxis, 0, 0,
			numBeforeInsert);
		RestructureUtils.copyData(srcImgPlus, dstImgPlus, modifiedAxis,
			numBeforeInsert, numBeforeInsert + numInInsertion, numAfterInsertion);
	}

	private int compositeStatus(final Dataset origData,
		final ImgPlus<?> dstImgPlus, final AxisType axis)
	{

		// adding along non-channel axis
		if (axis != Axes.CHANNEL) {
			return origData.getCompositeChannelCount();
		}

		// else adding hyperplanes along channel axis

		// calc working data
		final int currComposCount = dataset.getCompositeChannelCount();
		final int origAxisPos = origData.dimensionIndex(Axes.CHANNEL);
		final long numOrigChannels = origData.getImgPlus().dimension(origAxisPos);
		final long numNewChannels = dstImgPlus.dimension(origAxisPos);

		// was "composite" on 1 channel
		if (currComposCount == 1) {
			return 1;
		}

		// was composite on all channels
		if (numOrigChannels == currComposCount) {
			return (int) numNewChannels; // in future be composite on all channels
		}

		// was composite on a subset of channels that divides channels evenly
		if (numOrigChannels % currComposCount == 0 &&
			numNewChannels % currComposCount == 0)
		{
			return currComposCount;
		}

		// cannot figure out a good count - no longer composite
		return 1;
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
			final AxisType axis = Axes.get(axisName);
			final int axisIndex = dataset.dimensionIndex(axis);
			for (int i = 0; i < origPlanePos.length; i++) {
				if (i != axisIndex - 2) {
					newPlanePos[i] = origPlanePos[i];
				}
				else {
					if (origPlanePos[i] < position - 1) newPlanePos[i] = origPlanePos[i];
					else newPlanePos[i] = origPlanePos[i] + quantity;
				}
			}
		}
	}

	private void initAxisName() {
		final MutableModuleItem<String> axisNameItem =
			getInfo().getMutableInput(AXIS_NAME, String.class);
		final Dataset ds = getDataset();
		final ArrayList<String> choices = new ArrayList<String>();
		for (int i = 0; i < ds.numDimensions(); i++) {
			AxisType axisType = ds.axis(i).type();
			choices.add(axisType.getLabel());
		}
		axisNameItem.setChoices(choices);
	}

	private void initPosition() {
		final long max = getDataset().getImgPlus().dimension(0);
		setItemRange(POSITION, 1, max);
		setPosition(1);
	}

	private void initQuantity() {
		setItemRange(QUANTITY, 1, Long.MAX_VALUE);
		setQuantity(1);
	}

	private void setPositionRange() {
		final long dimLen = currDimLen();
		setItemRange(POSITION, 1, dimLen + 1);
	}

	private void setQuantityRange() {
		final long max = Long.MAX_VALUE - getPosition() + 1;
		setItemRange(QUANTITY, 1, max);
	}

	private void clampPosition() {
		final long max = currDimLen() + 1;
		final long pos = getPosition();
		if (pos < 1) setPosition(1);
		else if (pos > max) setPosition(max);
	}

	private void clampQuantity() {
		final long max = Long.MAX_VALUE - getPosition() + 1;
		final long total = getQuantity();
		if (total < 1) setQuantity(1);
		else if (total > max) setQuantity(max);
	}

	private long currDimLen() {
		final AxisType axis = getAxis();
		final int axisIndex = getDataset().dimensionIndex(axis);
		return getDataset().getImgPlus().dimension(axisIndex);
	}

	private void setItemRange(final String fieldName, final long min,
		@SuppressWarnings("unused") final long max)
	{
		final MutableModuleItem<Long> item =
			getInfo().getMutableInput(fieldName, Long.class);
		item.setMinimumValue(min);
		// TODO - disable until we fix ticket #886
		// item.setMaximumValue(max);
	}

}
