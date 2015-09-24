package mosaic.core.utils;


import java.util.Arrays;


/**
 * It define a Point in an N dimensional space
 *
 * @author Stephan Seemmler & Pietro Incardona
 */

public class Point {

    public int x[];
    private int dim;

    public Point(int dimension) {
        this.dim = dimension;
        this.x = new int[dim];
    }

    public Point() {
    }

    /**
     * Constructs a Point by copying a Point
     * 
     * @param Point
     */
    public Point(Point p) {
        this.dim = p.x.length;
        this.x = p.x.clone();
    }

    /**
     * Constructs a Point by copying the coords
     * 
     * @param coords (int)
     */
    public Point(int coords[]) {
        this.dim = coords.length;
        this.x = coords.clone();
    }

    /**
     * Construct a 3D point
     *
     * @param x
     * @param y
     * @param z
     */
    public Point(int x, int y, int z) {
        this.dim = 3;
        this.x = new int[3];
        this.x[0] = x;
        this.x[1] = y;
        this.x[2] = z;
    }

    /**
     * Construct a 2D point
     *
     * @param x
     * @param y
     */
    public Point(int x, int y) {
        this.dim = 2;
        this.x = new int[2];
        this.x[0] = x;
        this.x[1] = y;
    }

    /**
     * Constructs a Point by copying the coords
     * 
     * @param coords (long)
     */
    public Point(long coords[]) {
        this.dim = coords.length;
        this.x = new int[dim];
        for (int i = 0; i < coords.length; i++) {
            x[i] = (int) coords[i];
        }
    }

    /**
     * Makes a Point from an Array, without copying the array.
     * 
     * @param array Coordinates of the Point
     * @return Point
     */
    public static Point CopyLessArray(int array[]) {
        Point p = new Point();
        p.dim = array.length;
        p.x = array;
        return p;
    }

    /**
     * Is the point inside
     */
    public boolean isInside(int sz[]) {
        for (int i = 0; i < sz.length; i++) {
            if (x[i] >= sz[i]) {
                return false;
            }
        }

        return true;
    }

    // public static Point PointWithDim(int dimension)
    // {
    // Point p = new Point();
    // p.init(dimension);
    // return p;
    // }
    //
    // private void init(int dimension)
    // {
    // dim = dimension;
    // x = new int[dim];
    // }

    // private void set(int p[])
    // {
    // //TODO efficiency
    // x=p.clone();
    // }

    /**
     * Distance between point
     *
     * @param p
     * @return
     */

    public double distance(Point p) {
        double ret = 0.0;
        for (int i = 0; i < dim; i++) {
            ret += (p.x[i] - x[i]) * (p.x[i] - x[i]);
        }
        return Math.sqrt(ret);
    }

    /**
     * Add a Point to a Point
     *
     * @param p Point to add
     * @return return the new Point
     */

    public Point add(Point p) {
        Point result = new Point(dim);
        for (int i = 0; i < dim; i++) {
            result.x[i] = x[i] + p.x[i];
        }
        return result;
    }

    /**
     * Subtract a point to a Point
     *
     * @param p Point to subtract
     * @return
     */

    public Point sub(Point p) {
        Point result = new Point(dim);
        for (int i = 0; i < dim; i++) {
            result.x[i] = x[i] - p.x[i];
        }
        return result;
    }

    /**
     * Multiply a Point by a factor
     *
     * @param f factor
     * @return the new Point
     */

    public Point mult(int f) {
        Point result = new Point(dim);
        for (int i = 0; i < dim; i++) {
            result.x[i] = (x[i] * f);
        }
        return result;
    }

    /**
     * Divide a Point by a factor
     *
     * @param f factor division
     * @return
     */
    public Point div(int f) {
        Point result = new Point(dim);
        for (int i = 0; i < dim; i++) {
            result.x[i] = (x[i] / f);
        }
        return result;
    }

    /**
     * Divide a Point by a scaling factor
     *
     * @param f factor division
     * @return
     */
    public Point div(float scaling[]) {
        Point result = new Point(dim);
        for (int i = 0; i < dim; i++) {
            result.x[i] = (int) (x[i] / scaling[i]);
        }
        return result;
    }

    /**
     * Set coordinate to zero
     */
    public void zero() {
        for (int i = 0; i < dim; i++) {
            x[i] = 0;
        }
    }

    /**
     * Get the point coordiantes as a String
     *
     * @return string
     */
    @Override
    public String toString() {
        String result = "[";
        int i = 0;
        for (i = 0; i < x.length - 1; i++) {
            result = result + x[i] + ", ";
        }
        result = result + x[i] + "]";
        return result;
    }

    /**
     * Check if two points are equals
     *
     * @return true if they are equal false otherwise
     */
    @Override
    public boolean equals(Object o) {
        return Arrays.equals(x, ((Point) o).x);
    }

    /**
     * Clone the point
     *
     * @return the new point
     */

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Point(this.x);
    }

    /**
     * Hashing
     *
     * @return integer
     */

    @Override
    public int hashCode() {
        int sum = 0;
        for (int i = 0; i < dim; i++) {
            sum = sum * 1024 + x[i];
        }
        return sum;
    }

    /**
     * Get the dimension of the point
     *
     * @return space size
     */

    public int getDimension() {
        return dim;
    }
}
