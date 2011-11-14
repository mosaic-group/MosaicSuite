package mosaic.paramopt.macro;
import java.util.List;
import java.util.Vector;




public class ParameterizedMacro {

//	// The string of the original macro
//	private String originalMacroString;
//	// List of all parameters
//	private List<MacroParameter> parameterList = new Vector<MacroParameter>();
//	// List of all parameters which are to be optimized
//	private List<MacroParameter> optParameterList =
//		new Vector<MacroParameter>();
//	// List of lines of code of the parsed macro.
//	private List<MacroCodeLine> codeLines = new Vector<MacroCodeLine>();
//	// Current line of code.
//	private MacroCodeLine currentCodeLine;
//	// Number of the current line of code
//	private int currentLineNumber = 0;
//	// Current indentation of the code.
//	private int currentIndentation = 0;
//	// Current function name if parser is processing a function
//	private String functionName;
//	
//	private int[] tokens;
//	private int curtoken;
//	private int localtoken;
//	private List<Object> localCodeList = new Vector<Object>();
//	private Program pgm;

//	private Vector<Object> consolidatedCode = new Vector<Object>();
//	private Vector<MacroParameter> consolidatedParameters = 
//			new Vector<MacroParameter>();
//
//	public boolean set = false;
	
	/**************************************************************************/
	
	private List<ParameterSettings> parameterSettings;
	private List<Object> codeList;
	
	public ParameterizedMacro() {
		parameterSettings = new Vector<ParameterSettings>();
		codeList = new Vector<Object>();
	}
	
	void addStaticCode(String code) {
		if (code != null) {
			int last = codeList.size() - 1;
			// Check if last element in list is a string.
			if (last >= 0 && codeList.get(last) instanceof String) {
				String lastString = (String) codeList.get(last);
				// Last element in the list is a string so concatenate the last
				// element with the new code.
				codeList.set(last, lastString + "" + code);
			} else {
				// Last element in the list is not a string so add the new code
				// just as a new element.
				codeList.add(code);
			}
		}
	}
	
	void addParameter(int lineNumber, double value, String name,
			String method) {
		// Create a new parameter and its settings.
		Parameter parameter = new Parameter(value);
		ParameterSettings settings = new ParameterSettings(parameter, method, 
				name, lineNumber);
		// Add the new parameter to the code and the parameter settings to the 
		// corresponding list.
		codeList.add(parameter);
		parameterSettings.add(settings);
	}
	
	public void addParameter(Parameter parameter, ParameterSettings settings) {
		// Add the new parameter to the code and the parameter settings to the 
		// corresponding list.
		codeList.add(parameter);
		parameterSettings.add(settings);
	}

	/**
	 * Sets the enabled parameters to the specified values.
	 * 
	 * @param values
	 *            the values for the enabled parameters
	 */
	public void setParameterValues(double[] values) {
		// Get the current parameter count.
		int paramCount = getEnabledCount();
		
		// Check if values has correct length.
		if (values == null || values.length != paramCount)
			throw new IllegalArgumentException("Values array has wrong size!");
		
		// Set the values of the enabled parameters.
		int cur = 0;
		for (int i = 0; i < parameterSettings.size(); i++) {
			if (parameterSettings.get(i).isEnabled())
				parameterSettings.get(i).getParameter().setValue(values[cur++]);
		}
	}
	
	/**
	 * Returns the initial values of the enabled parameters.
	 * 
	 * @return the initial values of the enabled parameters
	 */
	public double[] getInitialParameterValues() {
		// Get the current parameter count.
		int paramCount = getEnabledCount();
		
		// Create an array of the right size.
		double[] result = new double[paramCount];
		
		// Set the values of the enabled parameters.
		int cur = 0;
		for (int i = 0; i < parameterSettings.size() && cur < paramCount; i++) {
			if (parameterSettings.get(i).isEnabled())
				result[cur++] = parameterSettings.get(i).getInitialValue();
		}
		return result;
	}
	
	/**
	 * Returns the lower bounds of the enabled parameters.
	 * 
	 * @return the lower bounds of the enabled parameters
	 */
	public Double[] getLowerParameterBounds() {
		// Get the current parameter count.
		int paramCount = getEnabledCount();
		
		// Create an array of the right size.
		Double[] result = new Double[paramCount];
		
		// Set the values of the enabled parameters.
		int cur = 0;
		for (int i = 0; i < parameterSettings.size() && cur < paramCount; i++) {
			if (parameterSettings.get(i).isEnabled())
				result[cur++] = parameterSettings.get(i).getLowerBound();
		}
		return result;
	}
	
	/**
	 * Returns the upper bounds of the enabled parameters.
	 * 
	 * @return the upper bounds of the enabled parameters
	 */
	public Double[] getUpperParameterBounds() {
		// Get the current parameter count.
		int paramCount = getEnabledCount();
		
		// Create an array of the right size.
		Double[] result = new Double[paramCount];
		
		// Set the values of the enabled parameters.
		int cur = 0;
		for (int i = 0; i < parameterSettings.size() && cur < paramCount; i++) {
			if (parameterSettings.get(i).isEnabled())
				result[cur++] = parameterSettings.get(i).getUpperBound();
		}
		return result;
	}
	
	/**
	 * Counts and returns the number of parameters which are enabled.
	 * 
	 * @return the number of enabled parameters
	 */
	public int getEnabledCount() {
		int enabledCount = 0;
		// Count how many parameter are enabled.
		for (ParameterSettings settings : parameterSettings)
			if (settings.isEnabled())
				enabledCount++;
		return enabledCount;
	}
	
	public List<ParameterSettings> getParameterSettings() {
		return parameterSettings;
	}
	
	/**
	 * Return the macro code with the current parameter values set.
	 * 
	 * @return macro code with current parameter values
	 */
	public String getMacro() {
		String macro = "";
		for (Object o : codeList) {
			macro += o.toString();
		}
		return macro;
	}
	
	public int getLineNumberOfParameter(int i) {
		if (i < 0 || i >= parameterSettings.size())
			throw new IndexOutOfBoundsException("There is no parameter with " +
					"index i=" + i);
		return parameterSettings.get(i).getLineNumber();
	}

	/**************************************************************************/
	
//	public ParameterizedMacro(String macro) {
//		originalMacroString = macro;
//		// Create a new line of code with 0 indentation and add it to the list
//		// of code lines.
//		currentCodeLine = new MacroCodeLine(currentIndentation);
//		codeLines.add(currentCodeLine);
//		currentLineNumber++;
//	}
//	
//	/**
//	 * Get the string version of the parameterized macro with the parameters
//	 * set to their current value.	
//	 * @return A string of the macro with parameters replaced by their current
//	 * values.
//	 */
//	public String getMacroString() {
//		// Instantiate the string for the return value.
//		String macro = new String();
//		// Get the code from all lines of code.
//		for(MacroCodeLine line : codeLines) {
//			macro += line.getCodeString(true) + "\n";
//		}
//		return macro;
//	}
	
//	/**
//	 * This method returns the original macro on which this parametrized
//	 * macro is based.
//	 * @return The originial macro string which was passed to the constructor
//	 */
//	public String getOriginalMacroString() {
//		return originalMacroString;
//	}
//	
//	public List<MacroParameter> getParameterList() {
//		return parameterList;
//	}
//	
//	public List<MacroParameter> getOptimizationParameterList() {
//		optParameterList.clear();
//		// Fetch all parameters which are to be optimized.
//		for (MacroParameter param : parameterList)
//			if (param.getEnabled())
//				optParameterList.add(param);
//		return optParameterList;
//	}	
	
}
