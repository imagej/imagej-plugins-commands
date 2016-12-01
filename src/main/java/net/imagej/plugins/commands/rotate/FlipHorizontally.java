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
import net.imagej.Extents;
import net.imagej.ImgPlus;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.RealRect;

/**
 * Modifies an input Dataset by flipping its pixels horizontally. Flips all
 * image pixels unless a selected region is available from the OverlayService.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Transform", mnemonic = 't'),
	@Menu(label = "Flip Horizontally", weight = 1) }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class FlipHorizontally extends ContextCommand {

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
		final RealRect selection = overlayService.getSelectionBounds(display);
		flipPixels(input, selection);
	}

	public void setDisplay(ImageDisplay disp) {
		display = disp;
	}
	
	public ImageDisplay getDisplay() {
		return display;
	}

	// -- private interface --

	private void flipPixels(final Dataset input, final RealRect selection) {

		final int xAxis = input.dimensionIndex(Axes.X);
		final int yAxis = input.dimensionIndex(Axes.Y);
		if ((xAxis < 0) || (yAxis < 0)) throw new IllegalArgumentException(
			"cannot flip image that does not have XY planes");

		long oX = 0;
		long oY = 0;
		long width = input.dimension(xAxis);
		long height = input.dimension(yAxis);

		if ((selection.width >= 1) && (selection.height >= 1)) {
			oX = (long) selection.x;
			oY = (long) selection.y;
			width = (long) selection.width;
			height = (long) selection.height;
		}

		final long[] planeDims = new long[input.numDimensions() - 2];
		int d = 0;
		for (int i = 0; i < input.numDimensions(); i++) {
			if (i == xAxis) continue;
			if (i == yAxis) continue;
			planeDims[d++] = input.dimension(i);
		}

		final Position planePos = new Extents(planeDims).createPosition();

		if (input.numDimensions() == 2) { // a single plane
			flipPlane(input, xAxis, yAxis, new long[] {}, oX, oY, width, height);
		}
		else { // has multiple planes
			final long[] planeIndex = new long[planeDims.length];
			while (planePos.hasNext()) {
				planePos.fwd();
				planePos.localize(planeIndex);
				flipPlane(input, xAxis, yAxis, planeIndex, oX, oY, width, height);
			}
		}
		input.update();
	}

	private void flipPlane(final Dataset input, final int xAxis, final int yAxis,
		final long[] planeIndex, final long oX, final long oY, final long width,
		final long height)
	{
		if (height == 1) return;

		final ImgPlus<? extends RealType<?>> imgPlus = input.getImgPlus();

		final RandomAccess<? extends RealType<?>> acc1 = imgPlus.randomAccess();
		final RandomAccess<? extends RealType<?>> acc2 = imgPlus.randomAccess();

		final long[] pos1 = new long[planeIndex.length + 2];
		final long[] pos2 = new long[planeIndex.length + 2];

		int d = 0;
		for (int i = 0; i < pos1.length; i++) {
			if (i == xAxis) continue;
			if (i == yAxis) continue;
			pos1[i] = planeIndex[d];
			pos2[i] = planeIndex[d];
			d++;
		}

		long col1, col2;

		if ((width & 1) == 0) { // even number of cols
			col2 = width / 2;
			col1 = col2 - 1;
		}
		else { // odd number of cols
			col2 = width / 2 + 1;
			col1 = col2 - 2;
		}

		while (col1 >= 0) {
			pos1[xAxis] = oX + col1;
			pos2[xAxis] = oX + col2;
			for (long y = oY; y < oY + height; y++) {
				pos1[yAxis] = y;
				pos2[yAxis] = y;
				acc1.setPosition(pos1);
				acc2.setPosition(pos2);
				final double value1 = acc1.get().getRealDouble();
				final double value2 = acc2.get().getRealDouble();
				acc1.get().setReal(value2);
				acc2.get().setReal(value1);
			}
			col1--;
			col2++;
		}
	}
}
