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

package net.imagej.plugins.commands.calculator;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.ops.ComputerOp;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.space.SpaceUtils;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * Fills an output Dataset with a combination of two input Datasets. The
 * combination is specified by the user (such as Add, Min, Average, etc.).
 * 
 * @author Barry DeZonia
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, iconPath = "/icons/commands/calculator.png",
	menu = { @Menu(label = MenuConstants.PROCESS_LABEL,
		weight = MenuConstants.PROCESS_WEIGHT,
		mnemonic = MenuConstants.PROCESS_MNEMONIC), @Menu(
			label = "Image Calculator...", weight = 22) }, headless = true, attrs = {
				@Attr(name = "no-legacy") })
public class ImageCalculator<U extends RealType<U>, V extends RealType<V>>
	extends ContextCommand
{

	// -- instance variables that are Parameters --

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private OpService opService;

	@Parameter(type = ItemIO.BOTH)
	private Dataset input1;

	@Parameter
	private Dataset input2;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset output;

	@Parameter(label = "Operation to do between the two input images")
	private ComputerOp<U, V> op;

	@Parameter(label = "Create new window")
	private boolean newWindow = true;

	@Parameter(label = "Floating point result")
	private boolean wantDoubles = false;

	// -- public interface --

	/**
	 * Runs the plugin filling the output image with the user specified binary
	 * combination of the two input images.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		final ImgPlus<U> img1 = (ImgPlus<U>) input1.getImgPlus();
		final ImgPlus<V> img2 = (ImgPlus<V>) input2.getImgPlus();
		final int n = img1.numDimensions();
		if (n != img1.numDimensions()) {
			throw new IllegalArgumentException(
				"The dimensions of the two images do not match.");
		}
		final RandomAccessibleInterval<U> offsetImg1 = Views.zeroMin(img1);
		final RandomAccessibleInterval<V> offsetImg2 = Views.zeroMin(img2);
		final Interval intersect = Intervals.intersect(offsetImg1, offsetImg2);
		final RandomAccessibleInterval<U> img1Sub = opService.image().crop(
			offsetImg1, intersect);
		final RandomAccessibleInterval<V> img2Sub = opService.image().crop(
			offsetImg2, intersect);
		Img<DoubleType> img = opService.create().<DoubleType> img(intersect);

		try {
			// TODO: need Ops between two images (not just add/div/mul/sub)
			opService.map(img, img1Sub, img2Sub, op);
		}
		catch (final IllegalArgumentException e) {
			cancel(e.toString());
			return;
		}

		// replace original data if desired by user
		if (!wantDoubles && !newWindow) {
			output = null;
			final ComputerOp<DoubleType, U> converter = opService.computer(
				Ops.Convert.Copy.class, img1.firstElement(), img.firstElement());
			opService.map(offsetImg1, img, converter);
			input1.update();
		}
		else { // write into output
			int bits = input1.getType().getBitsPerPixel();
			boolean floating = !input1.isInteger();
			boolean signed = input1.isSigned();
			if (wantDoubles) {
				bits = 64;
				floating = true;
				signed = true;
			}
			// TODO : HACK - this next line works but always creates a PlanarImg
			final long[] span = new long[n];
			img.dimensions(span);
			output = datasetService.create(span, "Result of operation", SpaceUtils
				.getAxisTypes(input1), bits, signed, floating);
			final ComputerOp<DoubleType, U> converter = opService.computer(
				Ops.Convert.Copy.class, img1.firstElement(), img.firstElement());
			opService.map(output.getImgPlus(), img, converter);
			output.update(); // TODO - probably unnecessary
		}
	}

	/**
	 * Returns the {@link Dataset} that is input 1 for this image calculation.
	 */
	public Dataset getInput1() {
		return input1;
	}

	/**
	 * Sets the {@link Dataset} that will be input 1 for this image calculation.
	 */
	public void setInput1(final Dataset input1) {
		this.input1 = input1;
	}

	/**
	 * Returns the {@link Dataset} that is input 2 for this image calculation.
	 */
	public Dataset getInput2() {
		return input2;
	}

	/**
	 * Sets the {@link Dataset} that will be input 2 for this image calculation.
	 */
	public void setInput2(final Dataset input2) {
		this.input2 = input2;
	}

	/**
	 * Returns the output {@link Dataset} of the image calculation.
	 */
	public Dataset getOutput() {
		return output;
	}

	/**
	 * Returns the {@link ComputerOp} that was used in the image calculation.
	 */
	public ComputerOp<U, V> getOperation() {
		return op;
	}

	// TODO - due to generics is this too difficult to specify for real world use?

	/**
	 * Sets the {@link ComputerOp} to be used in the image calculation.
	 */
	public void setOperation(final ComputerOp<U, V> operation) {
		op = operation;
	}

	/**
	 * Returns true if image calculation with create a new image window. Otherwise
	 * the existing image that is input 1 is changed.
	 */
	public boolean isNewWindow() {
		return newWindow;
	}

	/**
	 * Sets whether image calculation will create a new image window. If true then
	 * yes else the existing image that is input 1 is changed.
	 */
	public void setNewWindow(final boolean newWindow) {
		this.newWindow = newWindow;
	}

	/**
	 * Returns true if image calculation will return a double backed Img as
	 * output.
	 */
	public boolean isDoubleOutput() {
		return wantDoubles;
	}

	/**
	 * Sets whether image calculation will create double backed output. If true
	 * then yes else data is created that matches input image 1's data type.
	 */
	public void setDoubleOutput(final boolean wantDoubles) {
		this.wantDoubles = wantDoubles;
	}

}
