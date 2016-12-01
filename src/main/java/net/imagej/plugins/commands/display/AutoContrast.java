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

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.display.DatasetView;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Plugin that auto-thresholds each channel.
 * <p>
 * This is modeled after the ImageJ 1.x ContrastAdjuster plugin, but I'd like to
 * improve upon it since it can be pretty harsh for many images.
 * </p>
 * <p>
 * One consideration is to do Histogram equalization as described here:
 * http://en.wikipedia.org/wiki/Histogram_equalization but this requires more
 * than setting the min and max of the RealLUTConverters and would more likely
 * involve assigning new LUT(s) or something since the mapping isn't linear.
 * This would probably make sense as a separate plugin altogether.
 * </p>
 * 
 * @author Adam Fraser
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC), @Menu(label = "Adjust"),
	@Menu(label = "Auto-Contrast", accelerator = "shift alt ^L", weight = 0) },
	headless = true, attrs = { @Attr(name = "no-legacy") })
public class AutoContrast extends ContextCommand {

	private static final int BINS = 256;
	private static final int AUTO_THRESHOLD = 5000;
	private static int autoThreshold;

	@Parameter(type = ItemIO.BOTH)
	private DatasetView view;

	@Override
	public void run() {
		final Dataset dataset = view.getData();

		final int[] histogram = computeHistogram(dataset);
		final int pixelCount = countPixels(histogram);

		if (autoThreshold < 10) autoThreshold = AUTO_THRESHOLD;
		else autoThreshold /= 2;
		final int threshold = pixelCount / autoThreshold;
		final int limit = pixelCount / 10;
		int i = -1;
		boolean found = false;
		int count;
		do {
			i++;
			count = histogram[i];
			if (count > limit) count = 0;
			found = count > threshold;
		}
		while (!found && i < BINS - 1);
		final int hmin = i;

		i = BINS;
		do {
			i--;
			count = histogram[i];
			if (count > limit) count = 0;
			found = count > threshold;
		}
		while (!found && i > 0);
		final int hmax = i;

		double min;
		double max;
		final double histMin = dataset.getType().getMinValue();
		final double histMax = dataset.getType().getMaxValue();
		final double binSize = (histMax - histMin) / (BINS - 1);

		if (hmax >= hmin) {
			min = histMin + hmin * binSize;
			max = histMin + hmax * binSize;
			// XXX:
			// http://imagej.net/source/ij/plugin/frame/ContrastAdjuster.java
//			if (RGBImage && roi!=null) 
//				imp.setRoi(roi);
		}
		else {
			// reset by clamping the histogram
			double mn = Double.MAX_VALUE;
			double mx = Double.MIN_VALUE;
			for (long ch = 0; ch < getNumChannels(dataset); ch++) {
				if (ch < mn) mn = ch;
				if (ch > mx) mx = ch;
			}
			max = mx;
			min = mn;

			// alternatively we could set the values to the raw min, max
//			min = histMin;
//			max = histMax;
			autoThreshold = AUTO_THRESHOLD;
		}

		setMinMax(min, max);
	}

	public void setDatasetView(DatasetView view) {
		this.view = view;
	}
	
	public DatasetView getDatasetView() {
		return view;
	}

	// -- Helper methods --

	private int[] computeHistogram(final Dataset dataset) {
		//
		// afraser TODO: Not sure how to handle RGB images here
		//
		// CTR FIXME - Autoscaling needs to be reworked.
		//
		final double histMin = dataset.getType().getMinValue();
		final double histMax = dataset.getType().getMaxValue();

		final Cursor<? extends RealType<?>> c = dataset.getImgPlus().cursor();
		final int[] histogram = new int[BINS];
		while (c.hasNext()) {
			final double value = c.next().getRealDouble();
			final int bin = computeBin(value, histMin, histMax);
			histogram[bin]++;
		}
		return histogram;
	}

	private int computeBin(final double value, final double histMin,
		final double histMax)
	{
		double v = value;
		if (v < histMin) v = histMin;
		if (v > histMax) v = histMax;
		final int bin = (int) ((BINS - 1) * (v - histMin) / (histMax - histMin));
		return bin;
	}

	private int countPixels(final int[] histogram) {
		int sum = 0;
		for (final int v : histogram) {
			sum += v;
		}
		return sum;
	}

	private void setMinMax(final double min, final double max) {
		view.setChannelRanges(min, max);
		view.getProjector().map();
		view.update();
	}

	private long getNumChannels(final Dataset dataset) {
		int chIdx = dataset.dimensionIndex(Axes.CHANNEL);
		if (chIdx == -1) return 1;
		return dataset.dimension(chIdx);
	}

}
