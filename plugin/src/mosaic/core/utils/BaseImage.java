package mosaic.core.utils;

/**
 * Base image class containing universal dimension-base calculations and methods.
 */
public class BaseImage {
    
    protected int iWidth;
    protected int iHeight;
    protected int[] iDimensions;
    public IndexIterator iIterator;
    private int iMaxNumDimensions;
    
    /**
     * Initialize an intensity image from an Image Plus
     * choosing is normalizing or not
     *
     * @param aInputImg ImagePlus
     * @param aShouldNormalize true normalize false don' t
     */
    public BaseImage(int aDimensions[], int aMaxDimensions) {
        iMaxNumDimensions = aMaxDimensions;
        initMembers(aDimensions);
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
     * @return dimensions (width, height, numOfSlices)
     */
    public int[] getDimensions() {
        return this.iDimensions;
    }

    /**
     * Initializes all internal data of IntensityImage
     * @param aDimensions of input image
     */
    private void initMembers(int[] aDimensions) {
        iDimensions = aDimensions;
        
        // Verify dimensions - only 2D and 3D is supported
        if (iDimensions.length > iMaxNumDimensions) {
            throw new RuntimeException("Dimensions number bigger than " + iMaxNumDimensions + " is not supported!");
        }

        iIterator = new IndexIterator(aDimensions);

        iWidth = aDimensions[0];
        iHeight = aDimensions[1];
    }
    
    /**
     * Calculate size of data needed to keep image (for all dimensions)
     */
    protected int getSizeOfAllData() {
        int size = 1;
        for (int i = 0; i < iDimensions.length; ++i) {
            size *= iDimensions[i];
        }
        return size;
    }
}
