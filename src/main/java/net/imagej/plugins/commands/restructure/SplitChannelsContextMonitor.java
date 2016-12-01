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

package net.imagej.plugins.commands.restructure;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplayService;
import net.imagej.event.DataRestructuredEvent;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.display.event.DisplayActivatedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * Service that monitors context for the {@link SplitChannelsContext} plugin.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class SplitChannelsContextMonitor extends AbstractService {

	@Parameter
	private EventService eventService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	// -- Event handlers --

	@EventHandler
	protected void onEvent(@SuppressWarnings("unused")
	final DisplayActivatedEvent event)
	{
		checkContext();
	}

	@EventHandler
	protected void onEvent(@SuppressWarnings("unused")
	final DataRestructuredEvent event)
	{
		checkContext();
	}

	// -- Helper methods --

	private void checkContext() {
		final Dataset dataset = imageDisplayService.getActiveDataset();
		if (dataset == null) {
			setContextAppropriate(false);
			return;
		}
		final int channelIndex = dataset.dimensionIndex(Axes.CHANNEL);
		if (channelIndex < 0) {
			setContextAppropriate(false);
			return;
		}
		final long channelCount = dataset.dimension(channelIndex);
		setContextAppropriate(channelCount > 1);
	}

	// TODO: Figure out why, when right-clicking an inactive window:
	// module info is not up-to-date.
	// To test:
	// 1) Open Blobs.
	// 2) Open Clown.
	// 3) Right-click Blobs. "Split Channels" is not grayed out.
	// WinActivated does fire before MsPressed, so the behavior is mysterious.

	private void setContextAppropriate(final boolean enabled) {
		final CommandInfo info =
			commandService.getCommand(SplitChannelsContext.class);
		info.setEnabled(enabled);
		info.update(eventService); // TODO: Is this needed here?
	}

}
