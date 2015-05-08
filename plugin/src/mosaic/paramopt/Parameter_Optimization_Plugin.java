package mosaic.paramopt;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import mosaic.paramopt.es.CMAHansen;
import mosaic.paramopt.es.EvolutionStrategy;
import mosaic.paramopt.es.VarMuFixLambdaWithSigmaAdaption;
import mosaic.paramopt.ui.IParameterRankingUI;
import mosaic.paramopt.ui.IParameterSelectionUI;
import mosaic.paramopt.ui.ParameterRankingUI;
import mosaic.paramopt.ui.ParameterSelectionUI;


public class Parameter_Optimization_Plugin implements PlugInFilter,
		IParameterRankingController, IParameterSelectionController,
		TableModelListener{

	public Parameter_Optimization_Plugin() {}
	
	private IParameterSelectionUI selectionUI = null;
	private IParameterRankingUI rankingUI = null;
	// Configuration of the plug-in.
	private Configuration config;
	// History of the evolution strategy.
	private History history;
	private ImagePlus originalImp;
	private int imageCount;
	private int selectionMin;
	private int selectionMax;
	private ParameterTableModel parameterTableModel;
	private List<Class<?>> strategies;
	private List<String> strategyNames;
	// The strategy used for optimization.
	EvolutionStrategy strategy;
	double[][] currentPopulation;
	private double[] fitness;
	ImagePlus[] images;
	
	/**************************************************************************
	 *                      PLUGINFILTER INTERFACE                            *
	 **************************************************************************/

	/**
	 * Sets up the plug-in.
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		// This plugin requires an image to work on so if imp is null show an
		// error message and return.
		if (imp == null)
			return DONE;
		// Keep a reference to imp.
		originalImp = imp;
		// Initialize the list of strategies.
		initializeStrategies();
		// Create configuration
		config = new Configuration();
		history = new History();
		
		parameterTableModel = new ParameterTableModel();
		parameterTableModel.addTableModelListener(this);
		
		selectionUI = new ParameterSelectionUI(this);
		
		// Return the code representing the formats which this plug-in can
		// handle.
		return DOES_ALL | DOES_STACKS;
	}

	/**
	 * Runs the plug-in.
	 */
	@Override
	public void run(ImageProcessor ip) {
		// Show the selection UI.
		selectionUI.setVisible(true);
	}
	
	/**************************************************************************
	 *                   PARAMETER SELECTION INTERFACE                        *
	 **************************************************************************/
	
	/**
	 * Selects a file as the file from which the macro is to be loaded, reads
	 * the macro string from the file, parses the macro and updates the
	 * selection UI.
	 * 
	 * @param file
	 *            the file from which the macro is to be read
	 */
	public void selectMacroFile(File file) {
		// Check if a file has been passed as argument and if the file exists.
		if (file == null || !file.exists())
			return;
		
		// Read the content from the file.
		try {
			config.setMacroFile(file);
			
			// Update the parameter table model with the new macro.
			parameterTableModel.setMacro(config.getMacro());
			
			// If setting the new macro file succeeds then notify the selection
			// UI about the availability of new parameters that are contained
			// in the macro.
			selectionUI.notifyParameterChange();
		} catch (IOException e) {
			// If setting the new macro file fails or macro has no parameters
			// then show an error message and return to the selection UI.
			IJ.error("An error occured when trying to load the selected file.");
		}
	}
	
	/**
	 * Selects a file as the file from which the macro configuration is to be
	 * read. The macro configuration contains the path to the file containing
	 * the macro, optionally a string describing the strategy which is to be
	 * used for the optimization and finally the configuration for the
	 * parameters contained in the macro.
	 * 
	 * @param file
	 *            the file from which the configuration is to be read
	 */
	public void selectMacroConfigurationFile(File file) {
		try {
			// Load configuration from the specified file.
			config.loadFromFile(file);
			
			// Update the UI with the new configuration.
			selectionUI.setExportHistory(config.isExportHistory());
			selectionUI.setStrategy(getStrategyIndex(config.getStrategy()));
			
			// Update the parameter table model with the new macro.
			parameterTableModel.setMacro(config.getMacro());
			
			// If setting the new macro file succeeds then notify the selection
			// UI about the availability of new parameters that are contained
			// in the macro.
			selectionUI.notifyParameterChange();
		} catch (IOException e) {
			// Loading the configuration failed.
			IJ.error("Configuration could not be loaded.");
		}
	}
	
	/**
	 * Stores the current configuration in a configuration file.
	 * 
	 * @param selectedFile
	 *            the file to which the configuration is to be written.
	 */
	public void saveMacroConfigurationFile(File selectedFile) {
		try {
			config.writeToFile(selectedFile);
		} catch (IOException e) {
			IJ.error("Could not save the configuration.");
		}
	}
	
	/**
	 * Aborts the selection of parameters and quits the plug-in.
	 */
	public void abortSelection() {
		// TODO: Implement abort procedure.
	}

	/**
	 * Returns an array of with the names of the available strategies.
	 * 
	 * @return an array with names of strategies
	 */
	public List<String> getStrategies() {
		// Return strategies
		return strategyNames;
	}
	
	/**
	 * Sets the index of the selected strategy.
	 * 
	 * @param strategy
	 *            the index of the selected strategy 
	 */
	public void selectStrategy(int strategy) {
		// Check if strategy is a valid strategy number and if it is then set it
		// as the current strategy.
		if (strategy >= 0 && strategy < strategies.size()) {
			config.setStrategy(strategies.get(strategy));
			updateStatus();
		}
		// Set the strategy in the ui.
		selectionUI.setStrategy(getStrategyIndex(config.getStrategy()));
	}
	
	/**
	 * Sets the flag for the export of the evolution history.
	 * 
	 * @param enabled
	 *            the flag for whether the history shall be exported or not
	 */
	public void setHistoryEnabled(boolean enabled) {
		config.setExportHistory(enabled);
	}
	
	/**
	 * Returns an abstract table model for access to the parameter settings.
	 * 
	 * @return an abstract table model for access to the parameter settings
	 */
	public AbstractTableModel getParameterTableModel() {
		return parameterTableModel;
	}
	
	/**
	 * Returns the line number of the specified parameter.
	 * 
	 * @param selected
	 *            the index of the selected parameter
	 * @return the line number of the selected parameter
	 */
	public int getLineNumberOfParameter(int selected) {
		return config.getMacro().getLineNumberOfParameter(selected);
	}
	
	/**
	 * Returns the code of the current macro.
	 * 
	 * @return the current macro code.
	 */
	public String getMacro() {
		return config.getMacro().getMacroCode();
	}
	
	/**
	 * Starts the optimization process.
	 */
	public void startOptimization() {
		// Check if at least one parameter has been enabled and a strategy has
		// selected.
		if (config.getMacro() == null
				|| config.getMacro().getEnabledCount() < 1
				|| config.getStrategy() == null)
			return;
		
		// Create an instance of the strategy.
		try {
			strategy = (EvolutionStrategy) config.getStrategy().newInstance();
		} catch (Exception e) {
			IJ.error("Strategy could not be instanciated.");
		}
		if (strategy != null && strategy instanceof EvolutionStrategy) {
			// Strategy has been successfully instantiated so we can finish the
			// selection and start the optimization.
			new Thread(new Runnable() {
				@Override
				public void run() {
					setupOptimization();
				}
			}).start();
			
		} else {
			strategy = null;
			IJ.error("Illegal strategy.");
		}

	}
	
	/**************************************************************************
	 *                   PARAMETER RANKING INTERFACE                          *
	 **************************************************************************/

	/**
	 * Aborts the ranking of the produced images and quits the plugin.
	 */
	public void abortRanking() {}
	
	/**
	 * Creates the images of the next generation of parameters based on the
	 * users ranking of the images of the current generation of parameters.
	 */
	public void nextOptimization(int[] ranks) {
		// Check if ranks is not null and has the right dimension.
		if (ranks == null || ranks.length != imageCount)
			// Null or wrong dimension so return without doing anything.
			return;
		
		
		// Compute the new fitness values and store them in a temporary array.
		// Also count the number of images which have been ranked
		int numRanked = 0;
		double[] tmpFit = new double[imageCount];
		for (int i = 0; i < imageCount; i++) {
			if (ranks[i] > 0) {
				tmpFit[i] = 1.0 + 1.0 / ranks[i];
				numRanked++;
			} else
				tmpFit[i] = 0.0;
		}
		
		// Check if the number of images which have been ranked is valid.
		if (numRanked < selectionMin || numRanked > selectionMax)
			return;
		
		// Set the progress in the UI to 0 to signal that optimization has
		// started.
		rankingUI.setProgress(0);
		
		// Set the new fitness values.
		fitness = tmpFit;
		
		// Write the values of the current generation into the history together
		// with the fitness values.
		history.addGeneration(currentPopulation, fitness);
		
		// Pass the fitness to the strategy and generate the next generation of
		// parameters.
		strategy.setFitness(fitness);
		strategy.makeEvolutionStep();
		
		// Generate the images for the new generation of parameters by applying 
		// the macro with the values of the parameters to a copy of the original
		// image and update the displayed images.
		// After each updated image the UI is notified through the setProgress
		// method.
		createNextImages();
	}
	
	/**
	 * Repeats an optimization step if the user is not satisfied with any of the
	 * images. Try to generate parameters different from the last parameters.
	 */
	public void repeatOptimization() {
		// Set the progress in the UI to 0 to signal that optimization has
		// started.
		rankingUI.setProgress(0);
		
		// Generate a new population without setting the fitness for any image.
		strategy.repeatEvolutionStep();
		
		// Generate the images for the new generation of parameters by applying 
		// the macro with the values of the parameters to a copy of the original
		// image and update the displayed images.
		// After each updated image the UI is notified through the setProgress
		// method.
		createNextImages();
	}
	
	/**
	 * Finishes the optimization by applying the parameters of the selected
	 * image on the original image.
	 */
	public void finishOptimization(int[] ranks) {
		// In order to finish optimization the parameters of the selected image
		// will be applied to the original image.
		// Check if ranks is not null and has the right dimension.
		if (ranks == null || ranks.length != imageCount)
			// Null or wrong dimension so return without doing anything.
			return;

		// Count the number of images which have been selected and compute their
		// mean.
		int dimension = currentPopulation[0].length;
		double[] mean = new double[dimension];
		for (int i = 0; i < imageCount; i++) {
			if (ranks[i] > 0) {
				for (int j = 0; j < dimension; j++) {
					mean[j] += currentPopulation[i][j];
				}
			}
		}
		
		// Write the values of the current generation into the history together
		// with the fitness values.
		history.addGeneration(currentPopulation, fitness);
		
		// Apply the macro with the selected parameters on the original image.
		config.getMacro().setParameterValues(mean);
		WindowManager.setTempCurrentImage(originalImp);
		IJ.runMacro(config.getMacro().getMacroCode());
		WindowManager.setTempCurrentImage(originalImp);
		
		// Display the applied macro in a TextWindow
		new TextWindow("Applied macro", config.getMacro().getMacroCode(), 300, 300);
		
		// Write history of parameters into a file which can be imported into
		// MATLAB to analyze the optimization process.
		if (config.isExportHistory()) {
			JFileChooser fc = new JFileChooser();
			fc.setSelectedFile(new File("history.m"));
			switch(fc.showOpenDialog(null)) {
				case JFileChooser.APPROVE_OPTION:
					// A file has been chosen
					File f = fc.getSelectedFile();
					try {
						history.writeToFile(f);
					} catch (IOException e) {
						IJ.error("Could not write the history file.");
					}
					break;
				case JFileChooser.CANCEL_OPTION:
				case JFileChooser.ERROR_OPTION:
					// Selection has been canceled or an error occurred  
					break;
			}
		}
		
		// Clean up whatever necessary.
		rankingUI.close();
		rankingUI = null;
		
	}
	
	/**************************************************************************
	 *                           INTERNAL METHODS                             *
	 **************************************************************************/

	/**
	 * Initializes the strategies which are available for selection in the UI.
	 */
	private void initializeStrategies() {
		strategies = new Vector<Class<?>>();
		strategyNames = new Vector<String>();
		
		strategies.add(VarMuFixLambdaWithSigmaAdaption.class);
		strategyNames.add("(mu,lambda)-sigma adapted");

		strategies.add(CMAHansen.class);
		strategyNames.add("CMA");
	}
	
	/**
	 * Returns the index of the specified strategy in the strategies list it
	 * it is present and -1 otherwise.
	 * 
	 * @param strategy
	 *            the strategy which is to be located
	 * @return the index of the strategy if it is present and -1 otherwise
	 */
	private int getStrategyIndex(Class<?> strategy) {
		if (strategies == null)
			return -1;
		for (int i = 0; i < strategies.size(); i++) {
			if (strategies.get(i).equals(strategy))
				return i;
		}
		return -1;
	}
	
	/**
	 * This is a call back of the listener for table change events of the
	 * ParameterTableModel and it checks if at least one parameter has been
	 * selected for optimization in which case it notifies the ui about it.
	 * 
	 * @param e   the event which has been triggered
	 */
	@Override
	public void tableChanged(TableModelEvent e) {
		updateStatus();
	}
	
	private void updateStatus() {
		if (selectionUI == null)
			return;
		// Get the number of enabled parameters.
		int count = config.getMacro().getEnabledCount();
		// If at least one parameter is enabled and a strategy is selected then
		// notify the ui that the optimization can be started.
		if (count > 0 && config.getStrategy() != null)
			selectionUI.notifyOptimizationready(true);
		else
			selectionUI.notifyOptimizationready(false);
	}
	
	private void createNextImages() {
		currentPopulation = strategy.getCurrentPopulation();
		for (int i = 0; i < currentPopulation.length; i++) {
			// Set the parameter values.
			config.getMacro().setParameterValues(currentPopulation[i]);
			// Create a duplicate of the original image.
			//images[i] = originalImp.duplicate();
			images[i] = (new ij.plugin.Duplicator()).run(originalImp); 
			
			
			// Create a window for the new image and set its title.
			ImageWindow win = new ImageWindow(images[i]);
			win.setTitle("OptimizationImage");
			// Put the new window in focus in order to process the macro.
			WindowManager.setCurrentWindow(win);

//			WindowManager.setTempCurrentImage(images[i]);
			
			// Run the macro on the current image.
			IJ.runMacro(config.getMacro().getMacroCode());
			
			// Set the image to ignore flush calls to avoid it being flushed
			// when the window is closed.
			images[i].setIgnoreFlush(true);
			// Set the changes flag of the image to false in order to avoid the
			// save-changes dialog when the window is being closed.
			images[i].changes = false;
			// Close the window
			win.close();

//			WindowManager.setTempCurrentImage(null);
			
			// Update the ranking UI with the current progress.
			rankingUI.setImage(i, images[i]);
			rankingUI.setProgress(i + 1);
		}
	}
	
	/**
	 * Sets up the optimization process. This is where an instance of the
	 * selected strategy is being created and instantiated.
	 */
	private void setupOptimization() {
		// Close the selection UI.
		selectionUI.close();
		selectionUI = null;
		
		// Set the number of images which will be used.
		imageCount = 9;
		fitness = new double[imageCount];
		
		// Initialize the strategy parameters.
		strategy.setDimension(config.getMacro().getEnabledCount());
		
		strategy.setInitialX(config.getMacro()
				.getInitialParameterValues());
		strategy.setDimensionBounds(config.getMacro()
				.getLowerParameterBounds(), config.getMacro()
				.getUpperParameterBounds());
		strategy.setOffspringSize(imageCount);
		
		// Get the minimal an maximal number of images which need to be
		// selected.
		selectionMin = strategy.getMinSelectionSize();
		selectionMax = strategy.getMaxSelectionSize();
		
		// Create the ranking UI.
		rankingUI = new ParameterRankingUI(imageCount, selectionMin,
				selectionMax, this);
		rankingUI.setSliceMaximum(originalImp.getStackSize());
//		rankingUI.setOverlay(originalImp.duplicate());
		rankingUI.setOverlay((new ij.plugin.Duplicator()).run(originalImp));
		
		
		// Display the ranking UI.
		rankingUI.setVisible(true);
		rankingUI.setProgress(0);
		
		// Initialize the strategy and with it the first population.
		strategy.initialize();
		
		// Create an array for the instances of ImagePlus for each individual.
		images = new ImagePlus[imageCount];

		// Generate the images for the new generation of parameters by applying 
		// the macro with the values of the parameters to a copy of the original
		// image and update the displayed images.
		// After each updated image the UI is notified through the setProgress
		// method. 
		createNextImages(); 
	}
	
}
