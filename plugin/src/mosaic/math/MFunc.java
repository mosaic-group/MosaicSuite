package mosaic.math;

/**
 * Interface for methods to be used with Matrix class.
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public interface MFunc {
    /**
     * 
     * @param aElement current element value
     * @param aRow element row number
     * @param aCol element column number
     * @return
     */
	double f(double aElement, int aRow, int aCol);
}
