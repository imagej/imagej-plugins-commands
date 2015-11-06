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

package net.imagej.plugins.commands.assign;

import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.ops.ComputerOp;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.overlay.Overlay;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.plugin.Parameter;

/**
 * Helper class for use by many plugins that apply a {@link ComputerOp} to some
 * input image. The run() method modifies the current selection of the active
 * {@link Dataset} of the given {@link ImageDisplay}. The given
 * {@link ComputerOp} is applied on a pixel by pixel basis.
 * 
 * @author Barry DeZonia
 */
public class InplaceUnaryTransform<T extends ComplexType<T>> {

	// -- instance variables --

	private final Op map;
	private final Dataset dataset;
	private Interval region;

	@Parameter
	private OpService ops;

	// -- constructor --

	/** All planes version */
	public InplaceUnaryTransform(final ComputerOp<T, T> operation,
		Dataset dataset, Overlay overlay)
	{
		this.dataset = dataset;
		setRegion(dataset, overlay);
		@SuppressWarnings("unchecked")
		final Img<T> img = (Img<T>) dataset.getImgPlus();
		final IntervalView<T> subImg = Views.interval(img, region);
		// some ComputerOp that accepts and Img in construction and for a given
		// location (long[]), it converts the value of the Img at that location
		// to type O and set it as the output value.

//		final ComplexImageFunction<I,O> f1 =
//				new ComplexImageFunction<I,O>(img, outType.createVariable());

//		final GeneralUnaryFunction<long[],O,O> function = new
//				GeneralUnaryFunction<long[],O,O>(
//					f1, operation, outType.createVariable());

//		final InputIteratorFactory<long[]> factory =
//			new PointInputIteratorFactory();
//		assigner1 = new ImageAssignment<I, O, long[]>(img, origin, span, function,
//			condition, factory);
		map = ops.computer(Ops.Map.class, subImg, operation);
	}

	/** Single plane versions */
	public InplaceUnaryTransform(final ComputerOp<T, T> operation,
		Dataset dataset, Overlay overlay, Position planePos)
	{
		this.dataset = dataset;
		setRegion(dataset, overlay, planePos);
		@SuppressWarnings("unchecked")
		final Img<T> img = (Img<T>) dataset.getImgPlus();
		final IntervalView<T> subImg = Views.interval(img, region);
//		 some ComputerOp that accepts and Img in construction and for a given
//		 location (long[]), it converts the value of the Img at that location
//		 to type O and set it as the output value.
//		final ComputerOp<long[], O> f1;
//		final ComplexImageFunction<I,O> f1 =
//				new ComplexImageFunction<I,O>(img, outType.createVariable());
//
//		final DefaultJoinComputerAndComputer<long[], O, O> function =
//			new DefaultJoinComputerAndComputer<long[], O, O>();
//		function.setFirst(f1);
//		function.setSecond(operation);
//		final GeneralUnaryFunction<long[],O,O> function = new
//				GeneralUnaryFunction<long[],O,O>(
//					f1, operation, outType.createVariable());
//
//		final InputIteratorFactory<long[]> factory =
//			new PointInputIteratorFactory();

		// TODO: not sure whether the Slicewise will work as expected
		map = ops.computer(Ops.Slicewise.class, subImg, operation);
	}

	// -- public interface --

	public void run() {
		map.run();
		dataset.update();
	}

	public Interval getRegion() {
		return region;
	}

	// -- private helpers --

	/** All planes version */
	private void setRegion(Dataset ds, Overlay overlay) {

		// check dimensions of Dataset
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		if ((xIndex < 0) || (yIndex < 0)) throw new IllegalArgumentException(
			"display does not have XY planes");

		LongRect rect = findXYRegion(ds, overlay, xIndex, yIndex);

		// calc origin and span values
		final int numDimensions = ds.numDimensions();
		final long[] minSize = new long[numDimensions * 2];
		for (int i = 0; i < numDimensions; i++) {
			if (i == xIndex) {
				minSize[xIndex] = rect.x;
				minSize[numDimensions + xIndex] = rect.w;
			}
			else if (i == yIndex) {
				minSize[yIndex] = rect.y;
				minSize[numDimensions + yIndex] = rect.h;
			}
			else {
				minSize[i] = 0;
				minSize[numDimensions + i] = ds.dimension(i);
			}
		}
		final Interval interval = Intervals.createMinSize(minSize);
		final Interval roi = Intervals.largestContainedInterval(overlay
			.getRegionOfInterest());
		region = Intervals.intersect(roi, interval);
	}

	/** Single plane version */
	private void setRegion(Dataset ds, Overlay overlay, Position planePos) {

		// check dimensions of Dataset
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int chIndex = ds.dimensionIndex(Axes.CHANNEL);
		if ((xIndex < 0) || (yIndex < 0)) throw new IllegalArgumentException(
			"display does not have XY planes");

		LongRect rect = findXYRegion(ds, overlay, xIndex, yIndex);

		// calc origin and span values
		final int numDimensions = ds.numDimensions();
		final long[] minSize = new long[numDimensions * 2];
		int p = 0;
		for (int i = 0; i < numDimensions; i++) {
			if (i == xIndex) {
				minSize[xIndex] = rect.x;
				minSize[numDimensions + xIndex] = rect.w;
			}
			else if (i == yIndex) {
				minSize[yIndex] = rect.y;
				minSize[numDimensions + yIndex] = rect.h;
			}
			else {
				if (i == chIndex && ds.isRGBMerged()) {
					minSize[i] = 0;
					minSize[numDimensions + i] = 3;
					p++;
				}
				else {
					minSize[i] = planePos.getLongPosition(p++);
					minSize[numDimensions + i] = 1;
				}
			}
		}
		final Interval interval = Intervals.createMinSize(minSize);
		final Interval roi = Intervals.largestContainedInterval(overlay
			.getRegionOfInterest());
		region = Intervals.intersect(roi, interval);
	}

	private LongRect findXYRegion(Dataset ds, Overlay overlay, int xIndex,
		int yIndex)
	{

		// calc XY outline boundary
		final LongRect rect = new LongRect();
		if (overlay == null) {
			rect.x = 0;
			rect.y = 0;
			rect.w = ds.dimension(xIndex);
			rect.h = ds.dimension(yIndex);
		}
		else {
			rect.x = (long) overlay.realMin(0);
			rect.y = (long) overlay.realMin(1);
			rect.w = Math.round(overlay.realMax(0) - rect.x);
			rect.h = Math.round(overlay.realMax(1) - rect.y);
		}
		return rect;
	}

	private class LongRect {

		public long x, y, w, h;
	}
}
