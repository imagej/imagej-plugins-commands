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

package net.imagej.plugins.commands.misc;

import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imagej.display.ColorMode;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.overlay.ThresholdOverlay;
import net.imagej.threshold.ThresholdService;
import net.imagej.types.DataType;
import net.imagej.types.DataTypeService;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Returns a multiline string containing metadata information about a given
 * {@link ImageDisplay}.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.IMAGE_LABEL, weight = MenuConstants.IMAGE_WEIGHT,
		mnemonic = MenuConstants.IMAGE_MNEMONIC),
	@Menu(label = "Show Info...", accelerator = "^I") },
	headless = true, attrs = { @Attr(name = "no-legacy") })
public class ShowInfo implements Command {

	// -- Parameters --

	@Parameter
	private DataTypeService typeService;

	@Parameter
	private ThresholdService thresholdService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ImageDisplay disp;

	@Parameter
	private Dataset ds;

	@Parameter(type = ItemIO.OUTPUT)
	private String info;

	// -- ShowInfo methods --
	
	public String getInfo() {
		return info;
	}

	// -- Command methods --

	@Override
	public void run() {
		info = infoString();
	}

	// -- helpers --

	private String infoString() {
		final StringBuilder builder = new StringBuilder();
		add(textString(), builder);
		add(titleString(), builder);
		add(widthString(), builder);
		add(heightString(), builder);
		add(depthString(), builder);
		add(resolutionString(), builder);
		add(pixelVoxelSizeString(), builder);
		add(originString(), builder);
		add(typeString(), builder);
		add(displayRangesString(), builder);
		add(currSliceString(), builder);
		add(compositeString(), builder);
		add(thresholdString(), builder);
		add(calibrationString(), builder);
		add(sourceString(), builder);
		add(selectionString(), builder);
		return builder.toString();
	}

	private void add(final String s, final StringBuilder builder) {
		if (s != null) builder.append(s);
	}

	private String textString() {
		// TODO - if metadata includes a textual description then use it here
		return null;
	}

	private String titleString() {
		return "Title: " + ds.getName() + '\n';
	}

	private String widthString() {
		return dimString(0, "Width: ");
	}

	private String heightString() {
		return dimString(1, "Height: ");
	}

	private String depthString() {
		int zIndex = ds.dimensionIndex(Axes.Z);
		if (zIndex < 0) return null;
		return dimString(zIndex, "Depth: ");
	}

	private String resolutionString() {
		// TODO - can't finish until unit library in place
		// In IJ1 you might see something like "Resolution:  3.175 pixels per µm"
		return null;
	}

	private String pixelVoxelSizeString() {
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final CalibratedAxis xAxis = xIndex < 0 ? null : ds.axis(xIndex);
		final CalibratedAxis yAxis = yIndex < 0 ? null : ds.axis(yIndex);
		final CalibratedAxis zAxis = zIndex < 0 ? null : ds.axis(zIndex);
		if (xAxis == null) return null;
		if (yAxis == null) return null;
		if (!(xAxis instanceof LinearAxis)) return null;
		if (!(yAxis instanceof LinearAxis)) return null;
		if (zAxis != null && !(zAxis instanceof LinearAxis)) return null;
		final double xCal = xAxis.averageScale(0, 1);
		final double yCal = yAxis.averageScale(0, 1);
		final double zCal = (zAxis == null) ? Double.NaN : zAxis.averageScale(0, 1);
		final double xSize = (Double.isNaN(xCal) ? 1 : xCal);
		final double ySize = (Double.isNaN(yCal) ? 1 : yCal);
		final double zSize = (Double.isNaN(zCal) ? 1 : zCal);
		if (zAxis == null) { // no z axis
			return "Pixel size: " + dToS(xSize) + " x " + dToS(ySize) + '\n';
		}
		// z axis present
		return "Voxel size: " + dToS(xSize) + " x " + dToS(ySize) + " x " +
			dToS(zSize) + '\n';
	}

	private String originString() {
		final StringBuilder builder = new StringBuilder("Axis origins:\n");
		for (int d = 0; d < ds.numDimensions(); d++) {
			builder.append(ds.axis(d).type() + ": " +
				dToS(ds.axis(d).calibratedValue(0)) + "\n");
		}
		return builder.toString();
	}

	private String typeString() {
		Object dataObj = ds.getImgPlus().firstElement();
		DataType<?> type = typeService.getTypeByClass(dataObj.getClass());
		if (type == null) return "Type: unknown" + '\n';
		return "Type: " + type.longName() + '\n';
	}

	private String displayRangesString() {
		int chAxis = disp.dimensionIndex(Axes.CHANNEL);
		if (chAxis < 0) return null;
		long numChan = disp.dimension(chAxis);
		final StringBuilder builder = new StringBuilder();
		for (int c = 0; c < numChan; c++) {
			final DatasetView datasetView =
				imageDisplayService.getActiveDatasetView(disp);
			final double min = datasetView.getChannelMin(c);
			final double max = datasetView.getChannelMax(c);
			builder.append("Display range channel " + c + ": " + min + "-" + max +
				"\n");
		}
		return builder.toString();
	}

	private String currSliceString() {
		Position position = disp.getActiveView().getPlanePosition();
		if (position.numDimensions() == 0) return null;
		String tmp = "";
		for (int i = 0; i < position.numDimensions(); i++) {
			long dim = disp.dimension(i + 2);
			long pos = position.getLongPosition(i) + 1;
			String label = disp.axis(i + 2).type().toString();
			tmp += "View position " + label + ": " + pos + "/" + dim + '\n';
		}
		return tmp;
	}

	private String compositeString() {
		// TODO: dislike this casting
		ColorMode mode = ((DatasetView) disp.getActiveView()).getColorMode();
		return "Composite mode: " + mode + '\n';
	}

	private String thresholdString() {
		if (thresholdService.hasThreshold(disp)) {
			ThresholdOverlay thresh = thresholdService.getThreshold(disp);
			return "Threshold: " + thresh.getRangeMin() + "-" + thresh.getRangeMax() +
				'\n';
		}
		return "Threshold: none" + '\n';
	}

	private String calibrationString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < ds.numDimensions(); i++) {
			CalibratedAxis axis = ds.axis(i);
			builder.append(axis.type());
			builder.append(" axis equation: ");
			builder.append(axis.particularEquation());
			builder.append('\n');
		}
		return builder.toString();
	}

	private String sourceString() {
		String source = ds.getSource();
		if (source == null) return null;
		return "Source: " + source + '\n';
	}

	private String selectionString() {
		// TODO
		// In IJ1 you might see
		// "Rectangle x =, y=, w=, h= on separate lines
		return null;
	}

	private String dimString(int axisIndex, String label) {
		CalibratedAxis axis = ds.axis(axisIndex);
		if (!(axis instanceof LinearAxis)) return label + "varies nonlinearly";
		double cal = axis.averageScale(0, 1);
		long size = ds.dimension(axisIndex);
		String unit = axis.unit();
		String tmp = label;
		if (Double.isNaN(cal) || cal == 1) {
			tmp += size;
			if (unit != null) tmp += " " + unit;
		}
		else {
			tmp += dToS(cal * size);
			if (unit != null) tmp += " " + unit;
			tmp += " (" + size + ")";
		}
		tmp += "\n";
		return tmp;
	}

	private String dToS(double num) {
		return String.format("%1.4f", num);
	}
}
