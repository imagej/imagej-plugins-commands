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

package net.imagej.plugins.commands.binary;

import java.util.Arrays;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.autoscale.AutoscaleService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.ComputerOp;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imglib2.display.ColorTable8;
import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Creates or updates a mask {@link Dataset} of type {@link BitType}. Uses an
 * input Dataset that will be thresholded. Thresholding options can be specified
 * which are used to discriminate pixel values. The input Dataset can become the
 * mask if desired.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, initializer = "init", menu = { @Menu(
	label = MenuConstants.PROCESS_LABEL, weight = MenuConstants.PROCESS_WEIGHT,
	mnemonic = MenuConstants.PROCESS_MNEMONIC), @Menu(label = "Binary",
		mnemonic = 'b'), @Menu(label = "Binarize...") }, headless = true, attrs = {
			@Attr(name = "no-legacy") })
public class Binarize<T extends RealType<T>> extends ContextCommand {

	// TODO - is the following approach even necessary?

	// NB - to simplify headless operation this plugin uses primitives for its
	// @Parameter fields. Using enums might be safer but complicates headless
	// operation from a script for instance.

	// -- constants --

	public static final String INSIDE = "Inside threshold";
	public static final String OUTSIDE = "Outside threshold";
	public static final String WHITE = "White";
	public static final String BLACK = "Black";
	public static final String DEFAULT_METHOD = "Default";

	// -- Parameters --

	@Parameter
	private Dataset inputData;

	@Parameter(persist = false, autoFill = false, required = false)
	private Dataset inputMask = null;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset outputMask = null;

	@Parameter(label = "Threshold method")
	private String method = null; // DOTO: create some choices for the method

	@Parameter(label = "Mask pixels", choices = { INSIDE, OUTSIDE })
	private String maskPixels = INSIDE;

	@Parameter(label = "Mask color", choices = { WHITE, BLACK })
	private String maskColor = WHITE;

	@Parameter(label = "Fill mask foreground")
	private boolean fillFg = true;

	@Parameter(label = "Fill mask background")
	private boolean fillBg = true;

	@Parameter(label = "Threshold each plane")
	private boolean thresholdEachPlane = false;

	@Parameter(label = "Change input")
	private boolean changeInput = false;

	@Parameter
	private ImageDisplayService imgDispSrv;

	@Parameter
	private DatasetService datasetSrv;

	@Parameter
	private AutoscaleService autoscaleSrv;

	@Parameter
	private OpService opService;

	// -- accessors --

	/**
	 * Sets the threshold method to use for pixel discrimination.
	 * 
	 * @param thresholdMethod The name of the threshold method to use.
	 */
	public void setThresholdMethod(String thresholdMethod) {
		method = thresholdMethod;
	}

	/**
	 * Gets the threshold method used for pixel discrimination.
	 */
	public String thresholdMethod() {
		return method;
	}

	/**
	 * Sets which pixels are considered part of the mask. Those either inside or
	 * outside the threshold range. Use the constants MakeBinary.INSIDE or
	 * MakeBinary.OUTSIDE.
	 * 
	 * @param insideOrOutside One of the values INSIDE or OUTSIDE.
	 */
	public void setMaskPixels(String insideOrOutside) {
		if (insideOrOutside.equals(INSIDE)) maskPixels = INSIDE;
		else if (insideOrOutside.equals(OUTSIDE)) maskPixels = OUTSIDE;
		else throw new IllegalArgumentException(
			"Unknown mask pixel specification: " + insideOrOutside);
	}

	/**
	 * Gets which pixels are considered part of the mask. Either inside or outside
	 * the threshold range. Returns one of the constants MakeBinary.INSIDE or
	 * MakeBinary.OUTSIDE.
	 */
	public String maskPixels() {
		return maskPixels;
	}

	/**
	 * Sets the color of the mask pixels. Either black or white. Use the constants
	 * MakeBinary.BLACK or MakeBinary.WHITE.
	 * 
	 * @param blackOrWhite One of the values BLACK or WHITE.
	 */
	public void setMaskColor(String blackOrWhite) {
		if (blackOrWhite.equals(BLACK)) maskColor = BLACK;
		else if (blackOrWhite.equals(WHITE)) maskColor = WHITE;
		else throw new IllegalArgumentException(
			"Unknown mask color specification: " + blackOrWhite);
	}

	/**
	 * Gets the color of the mask pixels. Either black or white. One of the
	 * constants MakeBinary.BLACK or MakeBinary.WHITE.
	 */
	public String maskColor() {
		return maskColor;
	}

	/**
	 * Set whether to threshold each plane separately or to threshold the whole
	 * image at once.
	 */
	public void setThresholdEachPlane(boolean val) {
		thresholdEachPlane = val;
	}

	/**
	 * Gets whether to threshold each plane separately or to threshold the whole
	 * image at once.
	 */
	public boolean thresholdEachPlane() {
		return thresholdEachPlane;
	}

	/**
	 * Sets whether to fill foreground pixels of binary mask or not to the given
	 * specified value.
	 * 
	 * @param val The specified value.
	 */
	public void setFillMaskForeground(boolean val) {
		fillFg = val;
	}

	/**
	 * Gets whether to fill foreground pixels of binary mask or not.
	 */
	public boolean fillMaskForeground() {
		return fillFg;
	}

	/**
	 * Sets whether to fill background pixels of binary mask or not to the given
	 * specified value.
	 * 
	 * @param val The specified value.
	 */
	public void setFillMaskBackground(boolean val) {
		fillBg = val;
	}

	/**
	 * Gets whether to fill background pixels of binary mask or not.
	 */
	public boolean fillMaskBackground() {
		return fillBg;
	}

	/**
	 * Sets the reference input data.
	 * 
	 * @param dataset
	 */
	public void setInputData(Dataset dataset) {
		inputData = dataset;
	}

	/**
	 * Gets the reference input data.
	 */
	public Dataset inputData() {
		return inputData;
	}

	/**
	 * Sets the preexisting mask (if any).
	 */
	public void setInputMask(Dataset dataset) {
		inputMask = dataset;
	}

	/**
	 * Gets the preexisting mask (if any).
	 */
	public Dataset inputMask() {
		return inputMask;
	}

	/**
	 * Gets the output mask.
	 */
	public Dataset outputMask() {
		return outputMask;
	}

	/**
	 * Sets whether to change input data to contain the output mask.
	 */
	public void setChangeInput(boolean val) {
		changeInput = val;
	}

	/**
	 * Gets whether to change input data to contain the output mask.
	 */
	public boolean changeInput() {
		return changeInput;
	}

	/**
	 * Sets the threshold method to the default algorithm.
	 */
	public void setDefaultThresholdMethod() {
		method = "Ops.Threshold.Mean";
	}

	// -- Command methods --

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		long[] dims = Intervals.dimensionsAsLongArray(inputData);
		String err = checkInputMask(inputMask, dims);
		if (err != null) {
			cancel(err);
			return;
		}
		CalibratedAxis[] axes = new CalibratedAxis[dims.length];
		inputData.axes(axes);
		AxisType[] types = new AxisType[dims.length];
		for (int i = 0; i < dims.length; i++) {
			types[i] = axes[i].type();
		}
		Dataset mask = inputMask != null ? inputMask : datasetSrv.create(
			new BitType(), dims, "Mask", types, isVirtual(inputData));
		mask.setAxes(axes);
		Img<BitType> maskImg = mask.typedImg(new BitType());
		final ComputerOp<Dataset, Img<BitType>> thresholdMethod =
			(ComputerOp<Dataset, Img<BitType>>) opService.op(qualifiedMethod(),
				maskImg, inputData);
		if (thresholdEachPlane && planeCount(inputData) > 1) {
			// threshold each plane separately
			final int xDim = inputData.dimensionIndex(Axes.X);
			final int yDim = inputData.dimensionIndex(Axes.Y);
			final int[] xyDim = new int[] { xDim, yDim };
			opService.slicewise(maskImg, inputData, thresholdMethod, xyDim);
		}
		else { // threshold entire dataset once
			thresholdMethod.compute(inputData, maskImg);
		}
		if (!maskPixels.equals(INSIDE)) {
			final ComputerOp<BitType, BitType> not = opService.computer(
				Ops.Logic.Not.class, BitType.class, BitType.class);
			opService.map(maskImg, not);
		}
		assignColorTables(mask);
		if (changeInput) {
			// TODO - should inputData be ItemIO.BOTH????
			inputData.setImgPlus(mask.getImgPlus());
		}
		else outputMask = mask;
		// TODO - not sure should we update the dataset
		mask.update();
	}

	// -- helpers --

	// returns the qualified name of the threshold method

	private String qualifiedMethod() {
		return "Ops.threshold." + method;
	}

	// returns true if a given dataset is stored in a CellImg structure

	private boolean isVirtual(Dataset ds) {
		final Img<?> img = ds.getImgPlus().getImg();
		return AbstractCellImg.class.isAssignableFrom(img.getClass());
	}

	// returns the number of planes in a dataset

	private long planeCount(Dataset ds) {
		long count = 1;
		for (int d = 0; d < ds.numDimensions(); d++) {
			AxisType type = ds.axis(d).type();
			if (type == Axes.X || type == Axes.Y) continue;
			count *= ds.dimension(d);
		}
		return count;
	}

	// returns the dimensions of the space that contains the planes of a dataset.
	// this is the dataset dims minus the X and Y axes.

	long[] planeSpace(Dataset ds) {
		long[] planeSpace = new long[ds.numDimensions() - 2];
		int i = 0;
		for (int d = 0; d < ds.numDimensions(); d++) {
			AxisType type = ds.axis(d).type();
			if (type == Axes.X || type == Axes.Y) continue;
			planeSpace[i++] = ds.dimension(d);
		}
		return planeSpace;
	}

	// sets each dataset plane's color table

	private void assignColorTables(Dataset ds) {
		ColorTable8 table = maskColor.equals(WHITE) ? white() : black();
		long planeCount = planeCount(ds);
		if (planeCount > Integer.MAX_VALUE) {
			// TODO: for now just set all color tables. Later: throw exception?
			planeCount = Integer.MAX_VALUE;
		}
		ds.initializeColorTables((int) planeCount);
		for (int i = 0; i < planeCount; i++) {
			ds.setColorTable(table, i);
		}
	}

	// A color table where 0 pixels are black and all others are white

	private ColorTable8 white() {
		return makeTable(0, 255);
	}

	// A color table where 0 pixels are white and all others are black

	private ColorTable8 black() {
		return makeTable(255, 0);
	}

	// fills the first value of a CoplorTable with given value and sets the rest
	// values with the other given value.

	private ColorTable8 makeTable(int first, int rest) {
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		byte n;
		n = (byte) rest;
		Arrays.fill(r, n);
		Arrays.fill(g, n);
		Arrays.fill(b, n);
		n = (byte) first;
		r[0] = n;
		g[0] = n;
		b[0] = n;
		return new ColorTable8(r, g, b);
	}

	// checks for incompatibilities between input data and input mask

	private String checkInputMask(Dataset mask, long[] dims) {

		// if no mask specified then nothing to check
		if (mask == null) return null;

		// check that mask is of type BitType
		if (!(mask.getImgPlus().firstElement() instanceof BitType)) {
			return "Mask is not a binary image";
		}

		// check that passed in mask is correct size
		for (int d = 0; d < dims.length; d++) {
			long dim = dims[d];
			if (mask.dimension(d) != dim) {
				return "Mask shape not same as input data";
			}
		}

		// no errors found
		return null;
	}
}
