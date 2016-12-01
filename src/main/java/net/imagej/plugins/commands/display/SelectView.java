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

import net.imagej.axis.AxisType;
import net.imagej.display.DataView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayView;
import net.imagej.overlay.Overlay;
import net.imagej.overlay.RectangleOverlay;
import net.imglib2.roi.RegionOfInterest;

import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Selects an overlay that encompasses the current view. If no such overlay
 * currently exists one is created.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
		mnemonic = MenuConstants.EDIT_MNEMONIC),
	@Menu(label = "Selection", mnemonic = 's'),
	@Menu(label = "Select View", mnemonic = 'v', // TODO - accelerator
		weight = 0) }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class SelectView extends ContextCommand {

	@Parameter
	private Context context;
	
	@Parameter
	private ImageDisplayService imgDispService;
	
	@Parameter(type = ItemIO.BOTH)
	private ImageDisplay display;

	@Override
	public void run() {

		// first deselect all overlay views
		for (final DataView view : display) {
			if (view instanceof OverlayView) {
				view.setSelected(false);
			}
		}
		
		// then start searching the views
		
		for (final DataView view : display) {
			
			// skip all views other than overlay views
			if (!(view instanceof OverlayView)) continue;
			
			// else we have an OverlayView
			OverlayView overlayView = (OverlayView) view;
			
			if (viewIsInCurrentDisplayedPlane(display, view)) {
				if (viewFillsDisplay(overlayView, display)) {
					view.setSelected(true);
					return;
				}
			}
		}
		
		// if here no overlay was found on currently viewed plane that selects
		//   everything. so create one that does.
		DataView dataView = makeOverlayView(display);
		display.add(dataView);
		dataView.setSelected(true);
	}

	public ImageDisplay getDisplay() {
		return display;
	}

	public void setDisplay(final ImageDisplay display) {
		this.display = display;
	}

	// -- private helpers --
	
	private boolean viewIsInCurrentDisplayedPlane(ImageDisplay disp, DataView view) {
		for (int d = 0; d < disp.numDimensions(); d++) {
			AxisType axisType = disp.axis(d).type();
			if (axisType.isXY()) continue;
			if (disp.getLongPosition(axisType) != view.getLongPosition(axisType)) {
				return false;
			}
		}
		return true;
	}

	private boolean viewFillsDisplay(OverlayView view, ImageDisplay disp) {
		Overlay o = view.getData();
		if (!(o instanceof RectangleOverlay)) return false;
		RegionOfInterest region = o.getRegionOfInterest();
		if (region.realMin(0) > 0) return false;
		if (region.realMin(1) > 0) return false;
		if (region.realMax(0) < disp.dimension(0)) return false;
		if (region.realMax(1) < disp.dimension(1)) return false;
		return true;
	}
	
	private DataView makeOverlayView(ImageDisplay disp) {
		Overlay newOverlay = makeOverlay(disp);
		DataView dataView = imgDispService.createDataView(newOverlay);
		for (int i = 0; i < disp.numDimensions(); i++) {
			final AxisType axis = disp.axis(i).type();
			if (axis.isXY()) continue;
			if (dataView.getData().dimensionIndex(axis) < 0) {
				dataView.setPosition(disp.getLongPosition(axis), axis);
			}
		}
		return dataView;
	}

	private Overlay makeOverlay(ImageDisplay disp) {
		RectangleOverlay rect = new RectangleOverlay(context);
		rect.setOrigin(0, 0);
		rect.setOrigin(0, 1);
		rect.setExtent(disp.dimension(0), 0);  // TODO - extent too big by 1?
		rect.setExtent(disp.dimension(1), 1);
		return rect;
	}
	
}
