/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
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

package net.imagej.plugins.commands.assign;

import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imagej.ops.ComputerOp;
import net.imagej.ops.OpService;
import net.imagej.overlay.Overlay;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;

/**
 * Base class for previewable math commands.
 * 
 * @author Barry DeZonia
 */
public abstract class MathCommand<T extends ComplexType<T>> extends
	ContextCommand implements Previewable
{
	// -- instance variables that are Parameters --

	@Parameter
	protected ImageDisplayService displayService;

	@Parameter
	protected OverlayService overlayService;

	@Parameter
	protected OpService opService;

	@Parameter(type = ItemIO.BOTH)
	protected ImageDisplay display;

	@Parameter(label = "Preview")
	protected boolean preview;

	@Parameter(label = "Apply to all planes")
	protected boolean allPlanes;

	// -- instance variables --

	T type;
	// TODO: not sure whether can we assume that data is of type <T>
	private RandomAccessibleInterval<T> data;
	private RandomAccessibleInterval<T> backup;
	private Dataset dataset;
	private Overlay overlay;
	private Position planePos;

	// -- public interface --

	@Override
	public void run() {
		if (dataset == null) {
			initialize();
		}
		else if (preview) {
			restorePreviewRegion();
		}
		transformFullRegion();
	}

	@Override
	public void preview() {
		if (dataset == null) {
			initialize();
			savePreviewRegion();
		}
		else restorePreviewRegion();
		if (preview) transformPreviewRegion();
	}

	@Override
	public void cancel() {
		if (preview) restorePreviewRegion();
	}

	public ImageDisplay getDisplay() {
		return display;
	}

	public void setDisplay(final ImageDisplay display) {
		this.display = display;
	}

	public boolean getPreview() {
		return preview;
	}

	public void setPreview(final boolean preview) {
		this.preview = preview;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public abstract ComputerOp<T, T> getOperation();

	// -- private helpers --

	@SuppressWarnings("unchecked")
	private void initialize() {
		dataset = displayService.getActiveDataset(display);
		overlay = overlayService.getActiveOverlay(display);
		DatasetView view = displayService.getActiveDatasetView(display);
		planePos = view.getPlanePosition();

		InplaceUnaryTransform<T> xform = getPreviewTransform(dataset, overlay);
		Interval region = xform.getRegion();
		data = (RandomAccessibleInterval<T>) Views.interval(dataset.getImgPlus(),
			region);
		backup = opService.copy().rai(data);
		type = data.randomAccess().get().createVariable();

		// check dimensions of Dataset
		final long w = region.dimension(0);
		final long h = region.dimension(1);
		if (w * h > Integer.MAX_VALUE) throw new IllegalArgumentException(
			"preview region too large to copy into memory");
	}

	private InplaceUnaryTransform<T> getPreviewTransform(Dataset ds, Overlay ov) {
		return new InplaceUnaryTransform<T>(getOperation(), ds, ov, planePos);
	}

	private InplaceUnaryTransform<T> getFinalTransform(Dataset ds, Overlay ov) {
		if (allPlanes) return new InplaceUnaryTransform<T>(getOperation(), ds, ov);
		return getPreviewTransform(ds, ov);
	}

	// NB
	// We are backing up preview region to doubles. This can cause precision
	// loss for long backed datasets with large values. But using dataset's
	// getPlane()/setPlane() code takes more ram/time than needed. And it has
	// various container limitations.

	private void savePreviewRegion() {
		opService.copy().rai(backup, data);
	}

	private void restorePreviewRegion() {
		opService.copy().rai(data, backup);
		dataset.update();
	}

	private void transformFullRegion() {
		getFinalTransform(dataset, overlay).run();
	}

	private void transformPreviewRegion() {
		getPreviewTransform(dataset, overlay).run();
	}

}
