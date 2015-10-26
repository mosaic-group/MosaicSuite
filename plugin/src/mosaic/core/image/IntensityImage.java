package mosaic.core.image;


import ij.ImagePlus;
import ij.ImageStack;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;


/**
 * IntensityImage class for easier access to all pixels of (if needed normalized) input ImagePlus
 */
public class IntensityImage extends BaseImage {

    private final ImagePlus iInputImg;
    private float[] iDataIntensity;

    
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
        super(getDimensions(aInputImg), /* max num of dimensions */ 3);
        
        if (aShouldNormalize == true) {
            iInputImg = MosaicUtils.normalizeAllSlices(aInputImg);
        } else {
            iInputImg = aInputImg;
        }
        initIntensityData(iInputImg);
    }

    /**
     * @return container with intensity of data
     */
    public float[] getDataIntensity() {
        return iDataIntensity;
    }
    
    /**
     * @return original ImagePlus from which intensity data were taken
     */
    public ImagePlus getImageIP() {
        return iInputImg;
    }

    /**
     * @return value for given index
     */
    public float get(int idx) {
        return iDataIntensity[idx];
    }
    
    /**
     * returns the image data of the originalIP at Point p
     */
    public float get(Point p) {
        return get(iIterator.pointToIndex(p));
    }
    
    /**
     * returns the image data of the originalIP at Point p, if out of bounds then returns 0
     */
    public float getSafe(Point aPoint) {
        if (isOutOfBound(aPoint)) return 0.0f;

        return get(iIterator.pointToIndex(aPoint));
    }

    /**
     * Fill dataIntensity container with data from input image
     */
    private void initIntensityData(ImagePlus aImage) {
        if (aImage.getType() != ImagePlus.GRAY32) {
            throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
        }
        
        // Create container for image data and fill it
        iDataIntensity = new float[getSize()];
        final int nSlices = aImage.getStackSize();
        final int sizeOfOneImage = getWidth() * getHeight();
        final ImageStack stack = aImage.getStack();

        for (int i = 0; i < nSlices; ++i) {
            float[] pixels = (float[]) stack.getPixels(i + 1);
            int imageIndex = 0;
            for (int y = 0; y < getHeight(); ++y) {
                for (int x = 0; x < getWidth(); ++x) {
                    iDataIntensity[i * sizeOfOneImage + imageIndex] = pixels[imageIndex];
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
