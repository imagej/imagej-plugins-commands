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

package net.imagej.plugins.commands.app;

import org.scijava.command.Command;
import org.scijava.command.UnimplementedCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Plugin;

// TODO - replace the kind of functionality that IJ1's Create Shortcut command
// has. Since we have our own menu handling code we need this functionality in
// a pure modern ImageJ form.

/**
 * This is a placeholder command for as yet unfinished Create Shortcut command.
 * The associated legacy command does not work in ImageJ2 and will need to be
 * rewritten.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, iconPath = "/icons/commands/bricks.png",
	menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL,
			weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = MenuConstants.PLUGINS_MNEMONIC), @Menu(label = "Shortcuts"),
		@Menu(label = "Create Shortcut...", weight = 2) }, attrs = { @Attr(name = "no-legacy") })
public class CreateShortcut extends UnimplementedCommand {
	// TODO
}
