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

package net.imagej.plugins.commands.imglib;

import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.overlay.ThresholdOverlay;
import net.imagej.threshold.ThresholdService;
import net.imglib2.RandomAccess;
import net.imglib2.ops.pointset.PointSet;
import net.imglib2.ops.pointset.PointSetIterator;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Fills the background pixels of an image with NaN. The image is defined as the
 * active Dataset of a given ImageDisplay. The input image must be a thresholded
 * floating type dataset. The definition of what is background is determined by
 * the threshold settings of the image.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.PROCESS_LABEL,
		weight = MenuConstants.PROCESS_WEIGHT,
		mnemonic = MenuConstants.PROCESS_MNEMONIC),
	@Menu(label = "Math", mnemonic = 'm'),
	@Menu(label = "NaN Background", weight = 18) }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class NanBackground extends ContextCommand {

	// -- Parameters --

	@Parameter
	private ThresholdService threshSrv;

	@Parameter
	private ImageDisplayService dispSrv;

	@Parameter(type = ItemIO.BOTH)
	private ImageDisplay display;

	// -- instance variables --

	private Dataset input;

	// -- public interface --

	@Override
	public void run() {
		if (inputBad()) return;
		assignPixels();
		// TODO no longer necessary?
		// input.update();
	}

	public void setDisplay(ImageDisplay display) {
		this.display = display;
	}
	
	public ImageDisplay getDisplay() {
		return display;
	}

	// -- private interface --

	private boolean inputBad() {
		input = dispSrv.getActiveDataset(display);
		if (input == null) {
			cancel("Image display does not have an active dataset");
			return true;
		}
		if (input.isInteger()) {
			cancel("This plugin requires a floating point dataset");
			return true;
		}
		if (input.getImgPlus() == null) {
			cancel("Input ImgPlus is null");
			return true;
		}
		if (!threshSrv.hasThreshold(display)) {
			cancel("Input image is not thresholded");
			return true;
		}
		return false;
	}

	private void assignPixels() {
		ThresholdOverlay thresh = threshSrv.getThreshold(display);
		PointSet ps = thresh.getPointsOutside();
		PointSetIterator iter = ps.iterator();
		RandomAccess<? extends RealType<?>> accessor =
			input.getImgPlus().randomAccess();
		while (iter.hasNext()) {
			long[] pos = iter.next();
			accessor.setPosition(pos);
			accessor.get().setReal(Double.NaN);
		}
	}

}
