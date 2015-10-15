package mosaic.core.utils;


import ij.ImagePlus;
import ij.ImageStack;


/**
 * IntensityImage class for easier access to all pixels of (if needed normalized) input ImagePlus
 */
public class IntensityImage {

    private final ImagePlus iInputImg;
    
    private int iWidth;
    private int iHeight;
    private int[] iDimensions;
    private IndexIterator iterator;
    private float[] dataIntensity;

    
    /**
     * Initialize an intensity image from an Image Plus and normalize
     *
     * @param aInputImg
     */
    public IntensityImage(ImagePlus aInputImg) {
        this(aInputImg, true);
    }

    /**
     * Initialize an intensity image from an Image Plus
     * choosing is normalizing or not
     *
     * @param aInputImg ImagePlus
     * @param aShouldNormalize true normalize false don' t
     */
    public IntensityImage(ImagePlus aInputImg, boolean aShouldNormalize) {
        if (aShouldNormalize == true) {
            iInputImg = MosaicUtils.normalizeAllSlices(aInputImg);
        } else {
            iInputImg = aInputImg;
        }
    
        initMembers(getDimensions(aInputImg));
        initIntensityData(iInputImg);
    }

    /**
     * @return container with intensity of data
     */
    public float[] getDataIntensity() {
        return dataIntensity;
    }
    
    /**
     * @return original ImagePlus from which intensity data were taken
     */
    public ImagePlus getImageIP() {
        return iInputImg;
    }

    /**
     * @param aPoint input point
     * @return true if aPoint lays outside dimensions of IntensityImage
     */
    public boolean isOutOfBound(Point aPoint) {
        for (int i = 0; i < aPoint.x.length; ++i) {
            if (aPoint.x[i] < 0 || aPoint.x[i] >= iDimensions[i]) {
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
     * @return value for given index
     */
    public float get(int idx) {
        return dataIntensity[idx];
    }
    
    /**
     * returns the image data of the originalIP at Point p
     */
    public float get(Point p) {
        return get(iterator.pointToIndex(p));
    }
    
    /**
     * returns the image data of the originalIP at Point p, if out of bounds then returns 0
     */
    public float getSafe(Point aPoint) {
        if (isOutOfBound(aPoint)) return 0.0f;

        return get(iterator.pointToIndex(aPoint));
    }

    /**
     * Initializes all internal data of IntensityImage
     * @param aDimensions of input image
     */
    private void initMembers(int[] aDimensions) {
        iDimensions = aDimensions;
        
        // Verify dimensions - only 2D and 3D is supported
        if (iDimensions.length > 3) {
            throw new RuntimeException("Dim > 3 not supported");
        }

        iterator = new IndexIterator(aDimensions);

        iWidth = aDimensions[0];
        iHeight = aDimensions[1];

        // Calculate size of all data from dimensions and create needed container
        int size = 1;
        for (int i = 0; i < iDimensions.length; ++i) {
            size *= iDimensions[i];
        }
        dataIntensity = new float[size];
    }

    /**
     * Fill dataIntensity container with data from input image
     */
    private void initIntensityData(ImagePlus aImage) {
        if (aImage.getType() != ImagePlus.GRAY32) {
            throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
        }

        final int nSlices = aImage.getStackSize();
        final int sizeOfOneImage = iWidth * iHeight;
        final ImageStack stack = aImage.getStack();

        for (int i = 0; i < nSlices; ++i) {
            float[] pixels = (float[]) stack.getPixels(i + 1);
            int imageIndex = 0;
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    dataIntensity[i * sizeOfOneImage + imageIndex] = pixels[imageIndex];
                    ++imageIndex;
                }
            }
        }
    }

    /**
     * Gets dimensions from input image 
     * @param aImage input image
     * @return dimensions (width, height, numOfSlices)
     */
    private static int[] getDimensions(ImagePlus aImage) {
        final int[] dims = new int[aImage.getNDimensions()];

        // width, height, nChannels, nSlices, nFrames
        final int[] imageDimensions = aImage.getDimensions();
        
        dims[0] = imageDimensions[0];
        dims[1] = imageDimensions[1];
        // No matter what is a configuration of 3rd dim - get just stack size
        if (dims.length > 2) dims[2] = aImage.getStackSize();

        return dims;
    }
}
