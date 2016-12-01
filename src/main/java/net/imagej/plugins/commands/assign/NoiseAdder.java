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

package net.imagej.plugins.commands.assign;

import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.overlay.Overlay;
import net.imglib2.ops.operation.real.unary.RealAddNoise;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Fills an output Dataset by applying random noise to an input Dataset. This
 * class is used by AddDefaultNoiseToDataValues and
 * AddSpecificNoiseToDataValues. They each manipulate setStdDev(). This class
 * can be used to implement simple (1 pixel neighborhood) gaussian noise
 * addition without requiring a plugin.
 * 
 * @author Barry DeZonia
 */
public class NoiseAdder<T extends RealType<T>> {

	// -- instance variables --

	private final Dataset dataset;
	private final Overlay overlay;
	private final Position planePos;
	
	/**
	 * The stand deviation of the gaussian random value used to create perturbed
	 * values
	 */
	private double rangeStdDev;

	/**
	 * Maximum allowable values - varies by underlying data type. For instance
	 * (0,255) for 8 bit and (0,65535) for 16 bit. Used to make sure that returned
	 * values do not leave the allowable range for the underlying data type.
	 */
	private double rangeMin, rangeMax;

	// -- constructor --

	/**
	 * Constructor - takes an input Dataset as the baseline data to compute
	 * perturbed values from.
	 */
	public NoiseAdder(Dataset dataset, Overlay overlay, Position pos) {
		this.dataset = dataset;
		this.overlay = overlay;
		this.planePos = pos;
	}

	// -- public interface --

	/**
	 * Specify the standard deviation of the gaussian range desired. affects the
	 * distance of perturbation of each data value.
	 */
	protected void setStdDev(final double stdDev) {
		this.rangeStdDev = stdDev;
	}

	/**
	 * Runs the operation and returns the Dataset that contains the output data
	 */
	public void run() {
		calcTypeMinAndMax();

		final RealAddNoise<DoubleType, DoubleType> op =
			new RealAddNoise<DoubleType,DoubleType>(rangeMin, rangeMax, rangeStdDev);

		final InplaceUnaryTransform<T,DoubleType> transform;
		
		if (planePos == null)
			transform = 
				new InplaceUnaryTransform<T,DoubleType>(op, new DoubleType(), dataset, overlay);
		else
			transform =
				new InplaceUnaryTransform<T, DoubleType>(op, new DoubleType(), dataset, overlay, planePos);

		transform.run();
	}

	// -- private interface --

	/**
	 * Calculates the min and max allowable data range for the image : depends
	 * upon its underlying data type
	 */
	private void calcTypeMinAndMax() {
		rangeMin = dataset.getType().getMinValue();
		rangeMax = dataset.getType().getMaxValue();
	}

}
