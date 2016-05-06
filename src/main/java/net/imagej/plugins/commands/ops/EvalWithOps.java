package net.imagej.plugins.commands.ops;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.imagej.Dataset;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

@Plugin(type = Command.class, menu = {
		@Menu(label = MenuConstants.PROCESS_LABEL, weight = MenuConstants.PROCESS_WEIGHT),
 @Menu(label = "Math"),
		@Menu(label = "Eval... ") }, label = "Equation Evaluator")
public class EvalWithOps extends ContextCommand {

	@Parameter(type = ItemIO.BOTH)
	private Dataset displayed;

	private Dataset original;

	// -- Other fields --

	@Parameter(persist=false, required=false)
	private String equation;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, initializer = "initHeader")
	private String header;

	@Parameter(visibility = ItemVisibility.INVISIBLE, persist = false,
		callback = "preview")
	private boolean preview;

	@Parameter(label = "Help", callback="helpClicked")
	private Button helpButton;

	@Parameter
	private OpService ops;

	@Parameter
	private LogService logService;

	@Parameter
	private PlatformService platformService;

	@Parameter
	private DisplayService displayService;

	// -- Command methods --

	@Override
	public void run() {
		doRun();
	}

	@SuppressWarnings("unchecked")
	private <T extends RealType<T>> void doRun() {
		original = displayed.duplicate();
		final long time = System.currentTimeMillis();
		//FIXME:
		// 1. set up loops of input and output image
		// 2. for each position, set up variables
		//FIXME x, y, z, etc = pixel positions in that axis.. check # dims of input data
		//FIXME v = current pixel value
		// 3. run ops.run(image.eval) with the given vars
		if (equation == null || equation.isEmpty()) return;

		final Cursor<T> displayedCursor = (Cursor<T>) displayed.cursor();
		final Cursor<T> referenceCursor = (Cursor<T>) original.localizingCursor();
		final double[] pos = new double[referenceCursor.numDimensions()];
		final Map<String, Object> vars = new HashMap<>();

		while (referenceCursor.hasNext()) {
			displayedCursor.next();
			referenceCursor.next();
			referenceCursor.localize(pos);

			for (int i=0; i<pos.length; i++) {
				vars.put("d" + (i+1), pos[i]);
			}

			vars.put("v", referenceCursor.get().getRealDouble());
			Number o = (Number)ops.run(net.imagej.ops.Ops.Eval.class, equation, vars);
//			Number o = (Number)ops.run("eval", equation, vars);

			displayedCursor.get().setReal(o.doubleValue());
		}
		logService.error("Took: " + (System.currentTimeMillis() - time) + " ms");
	}

	// -- Callback methods --

	/** Called when the {@link #preview} parameter value changes. */
	protected void preview() {
		if (preview) run();
	}

	/**
	 * Called when {@link #helpButton} is clicked.
	 */
	protected void helpClicked() {
		try {
			platformService.open(new URL("http://imagej.net/Equation_Evaluator"));
		} catch (final IOException exc) {
			logService.error(exc);
		}
	}

	// -- Initializer methods --

	/**
	 * Build the {@link #header} field from available Ops.
	 */
	protected void initHeader() {
		//FIXME fill with math.eval ops..
		header = "Available operations: ";
	}


}
