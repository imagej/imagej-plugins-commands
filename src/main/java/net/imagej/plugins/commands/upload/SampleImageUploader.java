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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import net.iharder.Base64;

import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;

/**
 * Uploads a sample image to the ImageJDev server for further inspection by the
 * developers.
 * 
 * @author Johannes Schindelin
 */
@Plugin(type = Command.class, menuPath = "Help>Upload Sample Image...")
public class SampleImageUploader implements Command {

	@Parameter
	private File sampleImage;

	@Parameter
	private StatusService status;

	@Parameter
	private LogService log;

	@Parameter(required = false)
	private UIService ui;

	private static String baseURL = "http://upload.imagej.net/";

	/**
	 * This method provides a Java API to upload sample images to the ImageJ2
	 * dropbox.
	 */
	public static void run(final File file, final StatusService status,
		final LogService log)
	{
		final SampleImageUploader uploader = new SampleImageUploader();
		uploader.sampleImage = file;
		uploader.status = status;
		uploader.log = log;
		uploader.run();
	}

	@Override
	public void run() {
		if (sampleImage.length() >= 20 * 1024 * 1024) {
			if (ui == null) {
				log.error("File too large: " + sampleImage);
				return;
			}
			final Result answer =
				ui.showDialog("The file is really large: " + sampleImage +
					". Continue?", "Huge file!", MessageType.QUESTION_MESSAGE,
					OptionType.OK_CANCEL_OPTION);
			if (answer != Result.OK_OPTION) return;
		}
		try {
			uploadFile(sampleImage);
			ui.showDialog("Finished uploading file: " + sampleImage,
				"Upload complete!");
		}
		catch (Exception e) {
			log.error(e);
			ui.showDialog("There was a problem uploading file: " + sampleImage +
				"\nSee the log for details.", "Upload failed");
		}
	}

	private void uploadFile(final File file) throws IOException,
		MalformedURLException
	{
		// Convert file to URL with proper encoding.
		String path = file.toURI().toURL().getFile();
		// Extract file name only, without trailing '/'
		path = path.substring(path.lastIndexOf('/', path.length() - 2) + 1);
		upload(baseURL + path, new BufferedInputStream(new FileInputStream(file)),
			file.length());
	}

	private void upload(final String url, final InputStream in,
		final long totalLength) throws IOException, MalformedURLException
	{
		if (status != null) status.showStatus("Uploading " + url);

		final HttpURLConnection connection =
			(HttpURLConnection) new URL(url).openConnection();
		final String authentication = "ij2-sample-upload:password";
		connection.setRequestProperty("Authorization", "Basic " +
			Base64.encodeBytes(authentication.getBytes()));
		connection.setRequestProperty("Content-Type", "application/octet-stream");
		connection.setDoOutput(true);
		connection.setRequestMethod("PUT");
		final OutputStream out = connection.getOutputStream();
		byte[] buffer = new byte[65536];
		long count = 0;
		for (;;) {
			int count2 = in.read(buffer);
			if (count2 < 0) break;
			out.write(buffer, 0, count2);
			count += count2;
			if (totalLength > 0 && status != null) status.showProgress((int) count,
				(int) totalLength);
		}
		out.close();
		in.close();

		final BufferedReader response =
			new BufferedReader(new InputStreamReader(connection.getInputStream()));
		for (;;) {
			String line = response.readLine();
			if (line == null) break;
			System.err.println(line);
		}
		response.close();

		if (status != null) {
			status.clearStatus();
			status.showStatus("Upload complete!");
		}
	}
}
