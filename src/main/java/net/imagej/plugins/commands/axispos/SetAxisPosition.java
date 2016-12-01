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

package net.imagej.plugins.commands.axispos;

import net.imagej.animation.AnimationService;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;

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
 * Sets the position of the current axis to a user-specified value.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Axes", mnemonic = 'a'),
	@Menu(label = "Set Axis Position...") }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class SetAxisPosition extends DynamicCommand {

	@Parameter
	private AnimationService animationService;

	@Parameter(type = ItemIO.BOTH)
	private ImageDisplay display;

	@Parameter(persist = false, initializer = "initPosition")
	private long oneBasedPosition = 1;

	
	protected void initPosition() {
		final MutableModuleItem<Long> positionItem =
			getInfo().getMutableInput("oneBasedPosition", Long.class);
		positionItem.setLabel("Position");
		positionItem.setMinimumValue(1L);
		final AxisType axis = display.getActiveAxis();
		if (axis == null) return;
		int dim = display.dimensionIndex(axis);
		Long value = display.dimension(dim);
		positionItem.setMaximumValue(value);
	}
	
	@Override
	public void run() {
		animationService.stop(display);
		final AxisType axis = display.getActiveAxis();
		if (axis == null) return;
		final long newPosition = oneBasedPosition - 1;
		display.setPosition(newPosition, axis);
	}
}
