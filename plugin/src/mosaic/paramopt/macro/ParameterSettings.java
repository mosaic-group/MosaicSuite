package mosaic.paramopt.macro;

public class ParameterSettings {

	// The parameter to which these settings correspond.
	private final Parameter parameter;
	private final String methodName;
	private final String name;
	private final int lineNumber;
	private boolean enabled;
	private double initialValue;
	private Double upperBound;
	private Double lowerBound;
	
	public ParameterSettings(Parameter parameter, String methodName,
			String name, int lineNumber) {
		this.parameter = parameter;
		this.methodName = methodName;
		this.name = name;
		this.lineNumber = lineNumber;
		initialValue = parameter.getValue();
		lowerBound = new Double(0.0);
	}

	public Parameter getParameter() {
		return parameter;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getName() {
		return name;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public double getInitialValue() {
		return initialValue;
	}

	public void setInitialValue(double initialValue) {
		this.initialValue = initialValue;
		parameter.setValue(initialValue);
	}

	public Double getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(Double upperBound) {
		this.upperBound = upperBound;
	}

	public Double getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(Double lowerBound) {
		this.lowerBound = lowerBound;
	}
	
	
}
