package mosaic.core.imageUtils.images;

import ij.IJ;
import ij.ImagePlus;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.SpaceIterator;

/**
 * Base image class containing universal dimension-base calculations and methods.
 */
abstract public class BaseImage {
    
    protected SpaceIterator iIterator;
    
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

        iIterator = new SpaceIterator(aDimensions);
    }

    /**
     * @param aPoint input point
     * @return true if aPoint lays outside dimensions of IntensityImage
     */
    public boolean isInBound(Point aPoint) {
        return iIterator.isInBound(aPoint);
    }
    
    /**
     * @param aIndex input index
     * @return true if aIndex lays outside dimensions of IntensityImage
     */
    public boolean isInBound(Integer aIndex) {
        return iIterator.isInBound(aIndex);
    }

    /**
     * @return number of dimensions of BaseImage
     */
    public int getNumOfDimensions() {
        return iIterator.getNumOfDimensions();
    }

    /**
     * @return dimensions 2D (width, height),  3D (width, height, numOfSlices)
     */
    public int[] getDimensions() {
        return iIterator.getDimensions();
    }

    /**
     * Returns dimension length for given index
     * @param aDimensionIndex
     * @return
     */
    public int getDimension(int aDimensionIndex) {
        return iIterator.getDimensions()[aDimensionIndex];
    }
    
    /**
     * @return Returns width (dimension index 0)
     */
    public int getWidth() {
        return iIterator.getDimensions()[0];
    }
    
    /**
     * @return Returns width (dimension index 1)
     */
    public int getHeight() {
        return iIterator.getDimensions()[1];
    }
    
    /**
     * @return Returns width (dimension index 1)
     */
    public int getNumOfSlices() {
        return getNumOfDimensions() == 2 ? 1 : iIterator.getDimensions()[2];
    }
    
    /**
     * Calculate size of data needed to keep image (for all dimensions)
     */
    public int getSize() {
        return iIterator.getSize();
    }
    
    public int pointToIndex(Point aPoint) {
        return iIterator.pointToIndex(aPoint);
    }
    
    public Point indexToPoint(int aIndex) {
        return iIterator.indexToPoint(aIndex);
    }
    
    /**
     * Gets dimensions from input image 
     * @param aImage input image
     * @return dimensions (width, height, numOfSlices)
     */
    protected static int[] getDimensions(ImagePlus aImage) {
        final int[] dims = new int[aImage.getNDimensions()];

        // width, height, nChannels, nSlices, nFrames
        final int[] imageDimensions = aImage.getDimensions();
        
        dims[0] = imageDimensions[0];
        dims[1] = imageDimensions[1];
        // No matter what is a configuration of 3rd dim - get just stack size
        if (dims.length > 2) dims[2] = aImage.getStackSize();

        return dims;
    }
    
    /**
     * Save the LabelImage as tiff
     * @param aFileName where to save (full or relative path)
     */
    public void save(String aFileName) {
        final ImagePlus ip = convertToImg("ResultWindow_save");
        IJ.save(ip, aFileName);
        ip.close();
    }

    /**
     * Shows LabelImage
     */
    public ImagePlus show(String aTitle) {
        final ImagePlus imp = convertToImg(aTitle);
        imp.show();
        return imp;
    }
    
    /**
     * Converts LabelImage to ImagePlus (ShortProcessor)
     * @param aTitle - title of created image
     */
    abstract public ImagePlus convertToImg(String aTitle);
    
    @Override
    public String toString() {
        String result = "BaseImage [";
        for (int i = 0; i < iIterator.getNumOfDimensions(); ++i) {
            result += iIterator.getDimensions()[i];
            result += ", ";
        }
        result = result.substring(0, result.length()-2);
        result += "]";
        return result;
    }
}
