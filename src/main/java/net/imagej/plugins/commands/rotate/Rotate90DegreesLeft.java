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

package net.imagej.plugins.commands.rotate;

import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imagej.plugins.commands.imglib.ImgLibDataTransform;
import net.imagej.plugins.commands.rotate.XYFlipper.FlipCoordinateTransformer;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.RealRect;

// TODO - IJ1 updates the calibration so that pixel width & depth swap after this operation. Must implement here.

/**
 * Modifies an input Dataset by rotating its pixels 90 degrees to the left.
 * Rotates all image pixels regardless of selection region.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Transform", mnemonic = 't'),
	@Menu(label = "Rotate 90 Degrees Left", weight = 5) }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class Rotate90DegreesLeft extends ContextCommand {

	// -- instance variables that are Parameters --

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private OverlayService overlayService;

	@Parameter(type = ItemIO.BOTH)
	private ImageDisplay display;

	// -- public interface --

	@Override
	public void run() {
		final Dataset input = imageDisplayService.getActiveDataset(display);
		final RealRect bounds = overlayService.getSelectionBounds(display);
		final FlipCoordinateTransformer flipTransformer =
			new NinetyLeftTransformer();
		final XYFlipper flipper = new XYFlipper(input, bounds, flipTransformer);
		@SuppressWarnings("unchecked")
		final ImgLibDataTransform runner = new ImgLibDataTransform(input, flipper);
		runner.run();
	}

	public void setDisplay(ImageDisplay disp) {
		display = disp;
	}
	
	public ImageDisplay getDisplay() {
		return display;
	}

	// -- private interface --

	private class NinetyLeftTransformer implements FlipCoordinateTransformer {

		public NinetyLeftTransformer() {
			// nothing to do
		}

		@Override
		public void calcOutputPosition(final long[] inputDimensions,
			final long[] inputPosition, final long[] outputPosition)
		{
			outputPosition[1] = inputDimensions[0] - inputPosition[0] - 1;
			outputPosition[0] = inputPosition[1];
			for (int i = 2; i < inputDimensions.length; i++)
				outputPosition[i] = inputPosition[i];
		}

		@Override
		public long[] calcOutputDimensions(final long[] inputDimensions) {
			final long[] outputDims = inputDimensions.clone();

			outputDims[0] = inputDimensions[1];
			outputDims[1] = inputDimensions[0];

			return outputDims;
		}

		@Override
		public boolean isShapePreserving() {
			return false;
		}
	}
}
