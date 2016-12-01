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

import net.imagej.display.ColorMode;
import net.imagej.display.DatasetView;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Plugin that allows toggling between different color modes.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC), @Menu(label = "Color"),
	@Menu(label = "Channels Tool...", accelerator = "shift ^Z", weight = -5) },
	iconPath = "/icons/commands/color_wheel.png", headless = true, attrs = { @Attr(name = "no-legacy") })
public class ChannelsTool extends ContextCommand implements Previewable {

	public static final String GRAYSCALE = "Grayscale";
	public static final String COLOR = "Color";
	public static final String COMPOSITE = "Composite";

	@Parameter(type = ItemIO.BOTH)
	private DatasetView view;

	// TODO: Add support for enums to plugin framework?

	@Parameter(label = "Color mode", persist = false, choices = { GRAYSCALE,
		COLOR, COMPOSITE })
	private String modeString = GRAYSCALE;

	public ChannelsTool() {
		if (view != null) setColorMode(view.getColorMode());
	}

	@Override
	public void run() {
		if (view == null) return;
		view.setColorMode(getColorMode());
		view.update();
	}

	@Override
	public void preview() {
		run();
	}

	@Override
	public void cancel() {
		// TODO
	}

	public DatasetView getView() {
		return view;
	}

	public void setView(final DatasetView view) {
		this.view = view;
	}

	public ColorMode getColorMode() {
		return ColorMode.get(modeString);
	}

	public void setColorMode(final ColorMode colorMode) {
		modeString = colorMode.getLabel();
	}

}
