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

package net.imagej.plugins.commands.upload;

import java.io.File;

import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.app.event.StatusEvent;
import org.scijava.command.CommandService;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;

/**
 * Tests the upload functionality.
 * <p>
 * This test is not automated since it requires interaction with the upload
 * server, possibly overwriting existing files.
 * </p>
 * 
 * @author Johannes Schindelin
 */
public class InteractiveUploadTest {

	public static void main(final String[] args) {
		try {
			final Context context =
				new Context(CommandService.class, StatusService.class,
					EventService.class);
			context.getService(EventService.class).subscribe(new Object() {

				@EventHandler
				protected void onEvent(final StatusEvent e) {
					final int value = e.getProgressValue();
					final int maximum = e.getProgressMaximum();
					final String message = e.getStatusMessage();
					if (maximum > 0) System.err.print("(" + value + "/" + maximum + ")");
					if (message != null) System.err.print(" " + message);
					System.err.println();
				}
			});
			context.getService(CommandService.class).run(SampleImageUploader.class,
				true, new Object[] {"sampleImage", new File("/tmp/test.tif")}); // FIXME
		}
		catch (final Throwable t) {
			t.printStackTrace();
		}
	}

}
