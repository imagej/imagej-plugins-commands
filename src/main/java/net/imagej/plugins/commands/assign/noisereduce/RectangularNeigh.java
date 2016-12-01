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

package net.imagej.plugins.commands.assign.noisereduce;

import net.imglib2.ops.pointset.HyperVolumePointSet;
import net.imglib2.ops.pointset.PointSet;

/**
 * A rectangular neighborhood of specifiable size.
 * 
 * @author Barry DeZonia
 */
public class RectangularNeigh implements Neighborhood {

	private final long[] posOffsets;
	private final long[] negOffsets;
	private final PointSet points;

	public RectangularNeigh(final long[] posOffsets, final long[] negOffsets) {
		this.posOffsets = posOffsets;
		this.negOffsets = negOffsets;
		final long[] origin = new long[posOffsets.length];
		points = new HyperVolumePointSet(origin, posOffsets, negOffsets);
	}

	@Override
	public PointSet getPoints() {
		return points;
	}

	@Override
	public String getDescription() {
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < posOffsets.length; i++) {
			if (i != 0) {
				builder.append(" x ");
			}
			builder.append(1 + posOffsets[i] + negOffsets[i]);
		}
		builder.append(" rectangular neighborhood");
		return builder.toString();
	}

}
