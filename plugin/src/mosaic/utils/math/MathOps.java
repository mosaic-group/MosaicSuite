package mosaic.utils.math;


public class MathOps {
    /**
     * Computes the factorial of aN
     * @param aN - input number
     * @return aN!
     */
    public static int factorial(int aN) {
        int factorial = 1;
        for (int i = 2; i <= aN; ++i) {
            factorial *= i;
        }
        return factorial;
    }
}
