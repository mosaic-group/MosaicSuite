package mosaic.paramopt.macro;

public class Parameter {
	
	private double value;
	
	public Parameter(double value) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
	public String toString() {
		return Double.toString(value);
	}

}
