package mosaic.nurbs;

/**
 * Interface for function taking 2 parameters
 * @author Krzysztof Gonciarz
 */
public interface Function {
	/**
	 * Returns value of function in point (u, v)  
	 * @param u value of u (or x)
	 * @param v value of v (or y)
	 * @return value of function (z)
	 */
	double getValue(double u, double v);
}
