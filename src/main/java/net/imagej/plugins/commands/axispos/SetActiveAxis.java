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

import java.util.ArrayList;

import net.imagej.animation.Animation;
import net.imagej.animation.AnimationService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Changes the axis to move along to user specified value. This axis of movement
 * is used in the move axis position forward/backward plugins.
 * 
 * @author Barry DeZonia
 */
@Plugin(
	type = Command.class,
	menu = {
		@Menu(label = MenuConstants.IMAGE_LABEL,
			weight = MenuConstants.IMAGE_WEIGHT,
			mnemonic = MenuConstants.IMAGE_MNEMONIC),
		@Menu(label = "Axes", mnemonic = 'a'), @Menu(label = "Set Active Axis...") },
	headless = true)
public class SetActiveAxis extends DynamicCommand {

	// -- Constants --

	private static final String AXIS_NAME = "axisName";

	// -- Parameters --

	@Parameter(type = ItemIO.BOTH)
	private ImageDisplay display;

	@Parameter(persist = false, initializer = "initAxisName")
	private String axisName;

	@Parameter
	private AnimationService animationService;
	
	// -- SetActiveAxis methods --

	public ImageDisplay getDisplay() {
		return display;
	}

	public void setDisplay(final ImageDisplay display) {
		this.display = display;
	}

	public AxisType getAxis() {
		return Axes.get(axisName);
	}

	public void setAxis(final AxisType axis) {
		axisName = axis.toString();
	}

	// -- Runnable methods --

	@Override
	public void run() {
		final AxisType axis = getAxis();
		if (axis != null) {
			display.setActiveAxis(axis);
			int axisIndex = display.dimensionIndex(axis);
			long last = display.dimension(axisIndex) - 1;
			Animation a = animationService.getAnimation(display);
			boolean active = a.isActive();
			if (active) a.stop();
			a.setAxis(axis);
			a.setFirst(0);
			a.setLast(last);
			if (active) a.start();
		}
	}

	// -- Initializers --

	protected void initAxisName() {
		final MutableModuleItem<String> axisNameItem =
			getInfo().getMutableInput(AXIS_NAME, String.class);
		final ArrayList<String> choices = new ArrayList<String>();
		for (int d = 0; d < display.numDimensions(); d++) {
			AxisType axisType = display.axis(d).type();
			if (axisType.isXY()) continue;
			choices.add(axisType.getLabel());
		}
		axisNameItem.setChoices(choices);
	}

}
