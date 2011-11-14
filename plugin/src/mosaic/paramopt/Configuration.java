package mosaic.paramopt;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import mosaic.plugin.macro.ParameterSettings;
import mosaic.plugin.macro.ParameterizedMacro;
import mosaic.plugin.macro.ParameterizedMacroParser;


public class Configuration {
	
	// The macro file.
	private File macroFile;
	// The current strategy.
	private Class<?> strategy;
	// The current parameterized macro.
	private ParameterizedMacro macro;
	// Export optimization history as MATLAB file.
	private boolean exportHistory = false;

	public Configuration() {
		
	}
	
	public void loadFromFile(File file) throws IOException {
		BufferedReader configReader = new BufferedReader(new FileReader(file));
		String inStr = skipComments(configReader);
		// First non comment line is assumed to be containing the absolute path
		// to the macro file.
		String macroFileName = inStr;
		if (macroFileName == null || macroFileName == "")
			throw new IOException();
		
		// Next line should contain the strategy.
		inStr = skipComments(configReader);
		Class<?> strategy = null;
		try {
			strategy = getClass().getClassLoader().loadClass(inStr);
		} catch (Exception e) {
			// Class can not be loaded so loading the configuration has failed.
			throw new IOException();
		}
		
		// Next line should contain the export flag.
		inStr = skipComments(configReader);
		boolean exportHistory = Boolean.parseBoolean(inStr);
		
		// Try to read the macro
		File macroFile = new File(macroFileName);
		String macro = getFileContent(macroFile);
		
		// Create and parse a parameterized macro from the macro string.
		ParameterizedMacro pmacro = ParameterizedMacroParser.parseMacro(macro);
		
		// Get a list of all parameters in the macro.
		List<ParameterSettings> paramList = pmacro.getParameterSettings();
		for (ParameterSettings param : paramList) {
			// For each parameter try to read its configuration from the
			// configuration file.
			inStr = skipComments(configReader);
			// Split the string on all commas.
			String[] parts = inStr.split(",");
			// Check if string is split in the correct number of arguments.
			// 0: method name
			// 1: parameter name
			// 2: enabled
			// 3: initial value
			// 4: lower bound
			// 5: upper bound
			if (parts.length != 6)
				throw new IOException();
			// Process 'enabled'.
			param.setEnabled(Boolean.parseBoolean(parts[2]));
			// Process 'initial value'
			param.setInitialValue(Double.parseDouble(parts[3]));
			// Process 'lower bound'.
			if (parts[4].equals("_"))
				param.setLowerBound(null);
			else
				param.setLowerBound(Double.parseDouble(parts[4]));
			// Process 'upper bound'.
			if (parts[5].equals("_"))
				param.setUpperBound(null);
			else
				param.setUpperBound(Double.parseDouble(parts[5]));
		}
		configReader.close();
		
		// The configuration file contains a valid configuration.
		// Commit the new settings.
		this.macroFile = macroFile;
		this.macro = pmacro;
		this.strategy = strategy;
		this.exportHistory = exportHistory;
		return;
	}
	
	private String skipComments(BufferedReader configReader) 
			throws IOException {
		String inStr = configReader.readLine();
		if (inStr == null)
			throw new IOException();
		// Skip comment lines (beginning with #).
		while (inStr.startsWith("#")) {
			inStr = configReader.readLine();
			// Check if end of file is reached.
		    if (inStr == null)
		    	throw new IOException();
		}
		return inStr;
	}
	
	/**
	 * Writes the current configuration into the specified file.
	 * 
	 * @param file
	 *            the file into which the current configuration is written
	 */
	public void writeToFile(File file) throws IOException {
		if (file == null || macro == null)
			return;
		
		FileWriter writer = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(writer);
		// Write the name of the Macro
		out.write(macroFile.getAbsolutePath() + "\n");
		// Write name of strategy class.
		out.write(strategy.getName() + "\n");
		// Write export setting.
		out.write(Boolean.toString(exportHistory) + "\n");
		
		// Write configuration of all parameters.
		for (ParameterSettings param : macro.getParameterSettings()) {
			out.write(param.getMethodName() + ",");
			out.write(param.getName() + ",");
			out.write(Boolean.toString(param.isEnabled()) + ",");
			out.write(param.getInitialValue() + ",");
			if (param.getLowerBound() != null)
				out.write(param.getLowerBound() + ",");
			else
				out.write("_,");
			if (param.getUpperBound() != null)
				out.write(param.getUpperBound() + "\n");
			else
				out.write("_\n");
		}
		out.close();
	}
	
	/**
	 * Reads the content of the specified file and returns it as a string.
	 * 
	 * @param file
	 *            the file from which the content is to be read
	 * @return the content of the file as a string
	 */
	private String getFileContent(File file) throws IOException{
	    BufferedReader in = new BufferedReader(new FileReader(file));
	    String content = "";
	    String str;
	    while ((str = in.readLine()) != null)
	    	content += str + "\n";
	    in.close();
	    return content;
	}

	/**
	 * Returns the current macro file.
	 * 
	 * @return the current macro file
	 */
	public File getMacroFile() {
		return macroFile;
	}

	public void setMacroFile(File macroFile) throws IOException{
		// Read the macro from the specified file.
		String macro = getFileContent(macroFile);
		ParameterizedMacro pmacro = null;
		if (macro != null) {
			// The macro has been successfully read from the specified file.
			// Create a new Parameterize macro and parse it.
		    pmacro = ParameterizedMacroParser.parseMacro(macro);
		}
		this.macroFile = macroFile;
		this.macro = pmacro;
	}

	public Class<?> getStrategy() {
		return strategy;
	}

	public void setStrategy(Class<?> class1) {
		this.strategy = class1;
	}

	public ParameterizedMacro getMacro() {
		return macro;
	}

	public boolean isExportHistory() {
		return exportHistory;
	}

	public void setExportHistory(boolean exportHistory) {
		this.exportHistory = exportHistory;
	}
	
}
