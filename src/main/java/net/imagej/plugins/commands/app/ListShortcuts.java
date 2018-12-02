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

import java.util.List;

import org.scijava.ItemIO;
import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.input.Accelerator;
import org.scijava.menu.MenuConstants;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericTable;

/**
 * Lists ImageJ commands with keyboard shortcuts in a table.
 * <p>
 * Replaces {@code ij.plugin.CommandLister("shortcuts")}.
 * </p>
 *
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL,
		weight = MenuConstants.PLUGINS_WEIGHT,
		mnemonic = MenuConstants.PLUGINS_MNEMONIC), @Menu(label = "Shortcuts"),
	@Menu(label = "List Shortcuts...", weight = 1) }, attrs = { @Attr(name = "no-legacy") })
public class ListShortcuts extends ContextCommand {

	@Parameter
	private ModuleService moduleService;

	@Parameter(label = "Keyboard Shortcuts", type = ItemIO.OUTPUT)
	private GenericTable shortcuts;
	
	@Override
	public void run() {
		final List<ModuleInfo> modules = moduleService.getModules();

		final String hotKeyHeader = "Hot Key";
		final String commandHeader = "Command";

		shortcuts = new DefaultGenericTable();
		shortcuts.appendColumn(hotKeyHeader);
		shortcuts.appendColumn(commandHeader);

		int row = 0;
		for (final ModuleInfo info : modules) {
			final Accelerator shortcut = getAccelerator(info);
			if (shortcut == null) continue; // no keyboard shortcut
			shortcuts.appendRow();
			shortcuts.set(hotKeyHeader, row, shortcut.toString());
			shortcuts.set(commandHeader, row, info.getTitle());
			row++;
		}
	}

	private Accelerator getAccelerator(final ModuleInfo info) {
		final MenuPath menuPath = info.getMenuPath();
		if (menuPath == null) return null;
		final MenuEntry leaf = menuPath.getLeaf();
		if (leaf == null) return null;
		return leaf.getAccelerator();
	}

}
