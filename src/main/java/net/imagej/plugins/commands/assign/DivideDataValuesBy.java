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

import net.imagej.options.OptionsMisc;
import net.imglib2.ops.operation.real.unary.RealDivideConstant;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.scijava.command.Command;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Fills an output Dataset by dividing an input Dataset by a user defined
 * constant value.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Command.class, menu = {
	@Menu(label = MenuConstants.PROCESS_LABEL,
		weight = MenuConstants.PROCESS_WEIGHT,
		mnemonic = MenuConstants.PROCESS_MNEMONIC),
	@Menu(label = "Math", mnemonic = 'm'),
	@Menu(label = "Divide...", weight = 4) }, headless = true, attrs = { @Attr(name = "no-legacy") })
public class DivideDataValuesBy<T extends RealType<T>>
	extends MathCommand<T,DoubleType>
{

	// -- instance variables that are Parameters --

	@Parameter
	private OptionsService optionsService;

	@Parameter(label = "Value")
	private double value;

	// -- public interface --

	public DivideDataValuesBy() {
		super(new DoubleType());
	}

	@Override
	public RealDivideConstant<DoubleType, DoubleType> getOperation() {
		final OptionsMisc optionsMisc =
			optionsService.getOptions(OptionsMisc.class);
		final String dbzString = optionsMisc.getDivByZeroVal();
		double dbzVal;
		try {
			dbzVal = Double.parseDouble(dbzString);
		}
		catch (final NumberFormatException e) {
			dbzVal = Double.POSITIVE_INFINITY;
		}
		return new RealDivideConstant<DoubleType, DoubleType>(value, dbzVal);
	}

	public double getValue() {
		return value;
	}

	public void setValue(final double value) {
		this.value = value;
	}

}
