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

import java.util.HashMap;

import net.imagej.display.ImageDisplay;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Context menu command for Split Channels legacy command.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, menu = { @Menu(label = "Split Channels",
	mnemonic = 's') }, menuRoot = ImageDisplay.CONTEXT_MENU_ROOT, headless = true, attrs = { @Attr(name = "no-legacy") })
public class SplitChannelsContext extends ContextCommand {

	// -- Parameters --

	@Parameter
	private CommandService commandService;

	// -- Command methods --

	@Override
	public void run() {
		// TODO: Figure out why the parameter order is messed up and this fails:
//		commandService.run("imagej.legacy.plugin.LegacyCommand",
//			"ij.plugin.ChannelSplitter");
		final HashMap<String, Object> inputValues = new HashMap<String, Object>();
		inputValues.put("className", "ij.plugin.ChannelSplitter");
		// FIXME: Bad to invoke a command via reflection this way.
		commandService.run("imagej.legacy.plugin.LegacyCommand", true, inputValues);

		// TODO - replace with LegacyService::reunLegacyCommand() ?
	}

}
