package mosaic.core.imageUtils;


import java.util.Arrays;

import mosaic.utils.ConvertArray;


/**
 * It define a Point in an N dimensional space
 * @author Stephan Seemmler & Pietro Incardona
 */
public class Point {
    final public int iCoords[];


    /**
     * Constructs a Point by copying a Point
     * @param Point
     */
    public Point(Point aPoint) {
        iCoords = aPoint.iCoords.clone();
    }

    /**
     * Constructs a Point taking ownership of coords
     * @param iCoords
     */
    public Point(int... aCoords) {
        iCoords = aCoords;
    }
    
    /**
     * Constructs a Point by copying the coords
     * @param iCoords (long)
     */
    public Point(long aCoords[]) {
        iCoords = ConvertArray.toInt(aCoords);
    }

    /**
     * Is the point inside
     */
    public boolean isInside(int aMaxValues[]) {
        for (int i = 0; i < aMaxValues.length; i++) {
            if (iCoords[i] >= aMaxValues[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Euclidean distance between points
     * @param aPoint
     * @return
     */
    public double distance(Point aPoint) {
        double ret = 0.0;
        for (int i = 0; i < iCoords.length; i++) {
            ret += (aPoint.iCoords[i] - iCoords[i]) * (aPoint.iCoords[i] - iCoords[i]);
        }
        return Math.sqrt(ret);
    }

    /**
     * Add a Point to a Point
     * @param aPoint Point to add
     * @return returns new Point
     */
    public Point add(Point aPoint) {
        final Point result = new Point(iCoords.clone());
        for (int i = 0; i < iCoords.length; i++) {
            result.iCoords[i] += aPoint.iCoords[i];
        }
        return result;
    }
    
    /**
     * Add a scalar to each coordinate of a Point
     * @param aValue value to add
     * @return returns new Point
     */
    public Point add(double aValue) {
        final Point result = new Point(iCoords.clone());
        for (int i = 0; i < iCoords.length; i++) {
            result.iCoords[i] += aValue;
        }
        return result;
    }

    /**
     * Subtract a point to a Point
     * @param aPoint Point to subtract
     * @return returns new Point
     */
    public Point sub(Point aPoint) {
        final Point result = new Point(iCoords.clone());
        for (int i = 0; i < iCoords.length; i++) {
            result.iCoords[i] -= aPoint.iCoords[i];
        }
        return result;
    }

    /**
     * Subtract a scalar to each coordinate of a Point
     * @param aValue value to subtract
     * @return returns new Point
     */
    public Point sub(double aValue) {
        final Point result = new Point(iCoords.clone());
        for (int i = 0; i < iCoords.length; i++) {
            result.iCoords[i] -= aValue;
        }
        return result;
    }
    
    /**
     * Multiply a Point by a factor
     * @param aValue scaling factor
     * @return returns new Point
     */
    public Point mult(int aValue) {
        final Point result = new Point(iCoords.clone());
        for (int i = 0; i < iCoords.length; i++) {
            result.iCoords[i] *= aValue;
        }
        return result;
    }

    /**
     * Divide a Point by a factor
     * @param aValue factor division
     * @return returns new Point
     */
    public Point div(int aValue) {
        final Point result = new Point(iCoords.clone());
        for (int i = 0; i < iCoords.length; i++) {
            result.iCoords[i] /= aValue;
        }
        return result;
    }

    /**
     * Divide a Point by a scaling factor
     * @param aScalingFactors scaling factors for each coordinate
     * @return returns new Point
     */
    public Point div(float aScalingFactors[]) {
        final Point result = new Point(iCoords.clone());
        for (int i = 0; i < iCoords.length; i++) {
            result.iCoords[i] /= aScalingFactors[i];
        }
        return result;
    }

    /**
     * Set coordinates to zero
     * @return 
     */
    public Point zero() {
        for (int i = 0; i < iCoords.length; i++) {
            iCoords[i] = 0;
        }
        return this;
    }
    
    /**
     * Set coordinates to one
     * @return 
     */
    public Point one() {
        for (int i = 0; i < iCoords.length; i++) {
            iCoords[i] = 1;
        }
        return this;
    }
    
    /**
     * Counts the number of zeros in the coordinates of a Point
     * @param p A point representing an offset to the midpoint
     * @return number of zeros
     */
    public int numOfZerosInCoordinates() {
        int count = 0;
        for (final int i : iCoords) {
            if (i == 0) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Get the point coordinates as a String
     * @return string
     */
    @Override
    public String toString() {
        String result = "[";
        int i = 0;
        for (i = 0; i < iCoords.length - 1; i++) {
            result = result + iCoords[i] + ", ";
        }
        result = result + iCoords[i] + "]";
        return result;
    }

    /**
     * Clone the point
     * @return the new point
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Point(iCoords);
    }

    /**
     * Get the dimension of the point
     * @return space size
     */
    public int getNumOfDimensions() {
        return iCoords.length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(iCoords);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Point other = (Point) obj;
        if (!Arrays.equals(iCoords, other.iCoords)) return false;
        return true;
    } 
}
