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

package net.imagej.plugins.commands.display;

import net.imagej.Dataset;
import net.imagej.display.ImageCanvas;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ScreenCaptureService;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;

// NOTE: the following TODO may be invalid
// TODO - write code that captures part of the screen as a merged color Dataset.
// Then use it for Capture Screen, Capture Image, and Flatten. Can get rid of
// the capture logic in the ImageDisplayViewer hierarchy. And can get rid of
// ImageGrabber maybe. Would also fix issue where current Flatten code draws
// JHotDraw ellipses less well than how they appear in the canvas.

// NOTE:
// In IJ1 Flatten and Image Capture are different beasts.
//   Flatten makes an RGB image from the current view. 
//     Zoom level is ignored and the data dimensions match the input image.
//     Modern IJ's flatten seems to be working correctly.
//   Image Capture does a screen grab of the current image window. So its pixel
//     format can be a number of things (though in practice I always get RGB).
//     Regardless since its is represented by an ImagePlus it must end up one
//     of ImageJ1's 4 pixel types. Note that a zoomed image captures as the
//     magnified data and dimensions may not match original input image.
//
// So we probably need a way to capture data in screen pixel format of the
// current image window view (i.e. current zoom matters). And in Imglib world
// we only have ARGB data. So we should do a screen grab like modern ImageJ of
// the coords of the current image window. All without using AWT.

/**
 * Captures the current view of an {@link ImageDisplay} to a {@link Dataset}.
 * Unlike the Flatten command this plugin captures a view of the current image
 * canvas (without mouse cursor present). Therefore the current zoom level is
 * represented in the output data.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL,
			weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "Utilities"),
	@Menu(label = "Capture Image", weight = 21)}, attrs = { @Attr(name = "no-legacy") })
public class CaptureImage extends ContextCommand {

	// -- Parameters --
	
	@Parameter
	private ScreenCaptureService captureService;
	
	@Parameter
	private UIService uiService;
	
	@Parameter
	private ImageDisplay display;
	
	@Parameter(type=ItemIO.OUTPUT)
	private Dataset output;

	// -- accessors --
	
	public void setImageDisplay(ImageDisplay disp) {
		display = disp;
	}

	public ImageDisplay getImageDisplay() {
		return display;
	}
	
	public Dataset getOutput() {
		return output;
	}
	
	// -- run() method --
	
	@Override
	public void run() {
		DisplayViewer<?> viewer = uiService.getDisplayViewer(display);
		DisplayWindow window = viewer.getWindow();
		ImageCanvas canvas = display.getCanvas();
		int x = window.findDisplayContentScreenX();
		int y = window.findDisplayContentScreenY();
		int width = canvas.getViewportWidth();
		int height = canvas.getViewportHeight();
		output = captureService.captureScreenRegion(x, y, width, height);
		String name = display.getName();
		output.setName(name);
	}

}
