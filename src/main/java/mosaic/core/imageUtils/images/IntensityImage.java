package mosaic.core.imageUtils.images;


import java.util.Arrays;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackStatistics;
import mosaic.core.imageUtils.Point;
import mosaic.utils.ImgUtils;


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
            iInputImg = ImgUtils.convertToNormalizedGloballyFloatType(aInputImg);
        } else {
            iInputImg = aInputImg;
        }
        initIntensityData(iInputImg);
    }

    /**
     * Create an empty IntensityImage of a given dimension
     * @param aDimensions dimensions of the LabelImage
     */
    public IntensityImage(int[] aDimensions) {
        super(aDimensions, 3);
        iInputImg = null;
        iDataIntensity = new float[getSize()];
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
    public ImagePlus getImage() {
        return iInputImg;
    }

    /**
     * @return value for given index
     */
    public float get(int idx) {
        return iDataIntensity[idx];
    }
    
    /**
     * Sets value for given index
     */
    public void set(int idx, float aValue) {
        iDataIntensity[idx] = aValue;
    }

    /**
     * Sets value at Point p
     */
    public void set(Point p, float aValue) {
        set(pointToIndex(p), aValue);
    }
    
    /**
     * Sets value at Point p
     */
    public void setSafe(Point aPoint, float aValue) {
        if (!isInBound(aPoint)) return;

        set(pointToIndex(aPoint), aValue);
    }
    
    /**
     * returns the image data of the originalIP at Point p
     */
    public float get(Point p) {
        return get(pointToIndex(p));
    }
    
    /**
     * returns the image data of the originalIP at Point p, if out of bounds then returns 0
     */
    public float getSafe(Point aPoint) {
        if (!isInBound(aPoint)) return 0.0f;

        return get(pointToIndex(aPoint));
    }

    /**
     * Fill dataIntensity container with data from input image
     */
    private void initIntensityData(ImagePlus aImage) {
        if (aImage.getType() != ImagePlus.GRAY32) {
            aImage.setStack(aImage.getStack().convertToFloat());
            //TODO: temprarily - tbi
//            throw new RuntimeException("ImageProcessor has to be of type FloatProcessor");
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
     * Converts LabelImage to ImagePlus (FloatProcessor)
     */
    @Override
    public ImagePlus convertToImg(String aTitle) {
        final String title = aTitle;
        final ImagePlus imp;
        imp = new ImagePlus(title, getFloatStack());
        StackStatistics stackStats = new StackStatistics(imp);
        imp.setDisplayRange(stackStats.min, stackStats.max);
        return imp;
    }
    
    /**
     * Converts LabelImage to a stack of FloatProcessors
     */
    public ImageStack getFloatStack() {
        int w = getWidth();
        int h = getHeight();
        final int area = w * h;
        
        final ImageStack stack = new ImageStack(w, h);
        for (int i = 0; i < getNumOfSlices(); ++i) {
            stack.addSlice("", Arrays.copyOfRange(iDataIntensity, i * area, (i + 1) * area));
        }
        
        return stack;
    }
}
