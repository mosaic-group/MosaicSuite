package mosaic.core.utils;

/**
 * Base image class containing universal dimension-base calculations and methods.
 */
public class BaseImage {
    
    public IndexIterator iIterator;
    private final int[] iDimensions;
    
    /**
     * Initialize an intensity image from an Image Plus
     * choosing is normalizing or not
     *
     * @param aInputImg ImagePlus
     * @param aShouldNormalize true normalize false don' t
     */
    public BaseImage(int aDimensions[], int aMaxDimensions) {
        // Verify dimensions
        if (aDimensions.length > aMaxDimensions) {
            throw new RuntimeException("Dimensions number bigger than " + aMaxDimensions + " is not supported!");
        }

        iIterator = new IndexIterator(aDimensions);
        iDimensions = aDimensions;
    }

    /**
     * @param aPoint input point
     * @return true if aPoint lays outside dimensions of IntensityImage
     */
    public boolean isOutOfBound(Point aPoint) {
        for (int i = 0; i < aPoint.iCoords.length; ++i) {
            if (aPoint.iCoords[i] < 0 || aPoint.iCoords[i] >= iDimensions[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return number of dimensions of IntensityImage
     */
    public int getNumOfDimensions() {
        return iDimensions.length;
    }

    /**
     * @return dimensions 2D (width, height),  3D (width, height, numOfSlices)
     */
    public int[] getDimensions() {
        return iDimensions;
    }

    /**
     * Returns dimension length for given index
     * @param aDimensionIndex
     * @return
     */
    public int getDimension(int aDimensionIndex) {
        return iDimensions[aDimensionIndex];
    }
    
    /**
     * @return Returns width (dimension index 0)
     */
    public int getWidth() {
        return iDimensions[0];
    }
    
    /**
     * @return Returns width (dimension index 1)
     */
    public int getHeight() {
        return iDimensions[1];
    }
    
    /**
     * @return Returns width (dimension index 1)
     */
    public int getNumOfSlices() {
        return getNumOfDimensions() == 2 ? 1 : iDimensions[2];
    }
    
    /**
     * Calculate size of data needed to keep image (for all dimensions)
     */
    public int getSize() {
        return iIterator.getSize();
    }
}
