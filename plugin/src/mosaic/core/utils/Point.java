package mosaic.core.utils;


import java.util.Arrays;

import mosaic.utils.ConvertArray;


/**
 * It define a Point in an N dimensional space
 * @author Stephan Seemmler & Pietro Incardona
 */
public class Point {
    public int x[];


    /**
     * Constructs a Point by copying a Point
     * @param Point
     */
    public Point(Point aPoint) {
        this.x = aPoint.x.clone();
    }

    /**
     * Constructs a Point taking ownership of coords
     * @param coords
     */
    public Point(int... aCoords) {
        this.x = aCoords;
    }
    
    /**
     * Constructs a Point by copying the coords
     * @param coords (long)
     */
    public Point(long aCoords[]) {
        x = ConvertArray.toInt(aCoords);
    }

    /**
     * Is the point inside
     */
    public boolean isInside(int aMaxValues[]) {
        for (int i = 0; i < aMaxValues.length; i++) {
            if (x[i] >= aMaxValues[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Distance between point
     * @param aPoint
     * @return
     */
    public double distance(Point aPoint) {
        double ret = 0.0;
        for (int i = 0; i < x.length; i++) {
            ret += (aPoint.x[i] - x[i]) * (aPoint.x[i] - x[i]);
        }
        return Math.sqrt(ret);
    }

    /**
     * Add a Point to a Point
     * @param p Point to add
     * @return return the new Point
     */
    public Point add(Point p) {
        final Point result = new Point(x.clone());
        for (int i = 0; i < x.length; i++) {
            result.x[i] += p.x[i];
        }
        return result;
    }

    /**
     * Subtract a point to a Point
     * @param aPoint Point to subtract
     * @return
     */
    public Point sub(Point aPoint) {
        final Point result = new Point(x.clone());
        for (int i = 0; i < x.length; i++) {
            result.x[i] -= aPoint.x[i];
        }
        return result;
    }

    /**
     * Multiply a Point by a factor
     * @param aValue factor
     * @return the new Point
     */
    public Point mult(int aValue) {
        final Point result = new Point(x.clone());
        for (int i = 0; i < x.length; i++) {
            result.x[i] *= aValue;
        }
        return result;
    }

    /**
     * Divide a Point by a factor
     * @param aValue factor division
     * @return
     */
    public Point div(int aValue) {
        final Point result = new Point(x.clone());
        for (int i = 0; i < x.length; i++) {
            result.x[i] /= aValue;
        }
        return result;
    }

    /**
     * Divide a Point by a scaling factor
     * @param f factor division
     * @return
     */
    public Point div(float aScalingFactors[]) {
        final Point result = new Point(x.clone());
        for (int i = 0; i < x.length; i++) {
            result.x[i] /= aScalingFactors[i];
        }
        return result;
    }

    /**
     * Set coordinate to zero
     */
    public void zero() {
        for (int i = 0; i < x.length; i++) {
            x[i] = 0;
        }
    }

    /**
     * Get the point coordiantes as a String
     * @return string
     */
    @Override
    public String toString() {
        String result = "[";
        int i = 0;
        for (i = 0; i < x.length - 1; i++) {
            result = result + x[i] + ", ";
        }
        result = result + x[i] + "](" + x.length+ ")";
        return result;
    }

    /**
     * Clone the point
     * @return the new point
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Point(this.x);
    }

    /**
     * Get the dimension of the point
     * @return space size
     */
    public int getDimension() {
        return x.length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(x);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Point other = (Point) obj;
        if (!Arrays.equals(x, other.x)) return false;
        return true;
    } 
}
