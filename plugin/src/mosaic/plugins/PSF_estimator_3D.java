package mosaic.plugins;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.plugin.ZProjector;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;
import mosaic.interpolators.BilinearInterpolator;

//TODO: GUI,
//TODO: print number of points considered.
//TODO: currently the BG subtraction is per bead based max(PSF-0.1*PSF,0)
//		and also the mean bead is 'cleaned' with 0.005 perc. threshold. (very heuristically)
//TODO: edge of map,
//TODO: smart map size,
//TODO: see further todo entries.
public class PSF_estimator_3D implements  PlugInFilter{
    //parameters
    int mMaskRadius = 5;
    double mRInc = 0.001; //in px
    double mPhiInc = Math.PI / 20.;
    int mRMaxInNm = 800;
    int mZMaxInNm = 1*1600; //only in one direction, the generated map will be twice as large.
    int mMapSizeR = 1000;
    int mMapSizeZ = 1000; // in both direction (total image width)
    float mGaussPreprocessRadius = 1;

    //member variables
    int mMask[][][];
    int mHeight;
    int mWidth;
    int mNSlices;
    float mGlobalMin;
    float mGlobalMax;
    double mPxWidthInNm;
    double mPxDepthInNm;
    protected ImagePlus mZProjectedImagePlus;
    ImagePlus mOriginalImagePlus;
    ImageStack mPreprocessedFrameImage;
    int mPreprocessedFrameNb = 0;
    Vector<Bead> mBeads = new Vector<Bead>();
    ImagePlus mBeadImage;

    //int mMaskHeight = 10;

    @Override
    public int setup(String arg, ImagePlus aOrigImp) {
        if (aOrigImp == null) {
            IJ.showMessage("Please open an image with beads first.");
            return DONE;
        }

        while (true) {
            final String vUnit = aOrigImp.getCalibration().getUnit();
            if (vUnit.equals("nm")) {
                mPxWidthInNm = aOrigImp.getCalibration().pixelWidth;
                mPxDepthInNm = aOrigImp.getCalibration().pixelDepth;
                break;
            } else if (vUnit.equals(IJ.micronSymbol+"m")) {
                mPxWidthInNm = aOrigImp.getCalibration().pixelWidth * 1000;
                mPxDepthInNm = aOrigImp.getCalibration().pixelDepth * 1000;
                break;
            } else if (vUnit.equals("mm")) {
                mPxWidthInNm = aOrigImp.getCalibration().pixelWidth * 1000000;
                mPxDepthInNm = aOrigImp.getCalibration().pixelDepth * 1000000;
                break;
            }
            IJ.showMessage("Please enter correct voxel sizes in nm, " + IJ.micronSymbol + "m or mm");
            IJ.run("Properties...");
        }

        mOriginalImagePlus = aOrigImp;
        mHeight = aOrigImp.getHeight();
        mWidth = aOrigImp.getWidth();
        mNSlices = aOrigImp.getNSlices();

        if (!getUserDefinedParams()) {
            return DONE;
        }

        mRInc = 1./(mMapSizeR*1.1);

        final StackStatistics vSS = new StackStatistics(aOrigImp);
        mGlobalMin = (float)vSS.min;
        mGlobalMax = (float)vSS.max;

        mMask = generateMask(mMaskRadius);

        doZProjection(mOriginalImagePlus);
        initVisualization();



        return DONE;
    }

    @Override
    public void run(ImageProcessor ip) {

    }

    protected boolean registerOrDeleteNewBeadAt(int aX, int aY) {
        //
        //centroid detection
        //
        final int vFrame = mZProjectedImagePlus.getCurrentSlice();
        if (mPreprocessedFrameNb != vFrame) {
            mPreprocessedFrameImage = getAFrameCopy(mOriginalImagePlus, vFrame);
            normalizeFrameFloat(mPreprocessedFrameImage);
            gaussBlur3D(mPreprocessedFrameImage, mGaussPreprocessRadius);
            //			new StackWindow(new ImagePlus("after gauss blur", getSubStackFloatCopy(mPreprocessedFrameImage, 1, 30)));
            boxCarBackgroundSubtractor(mPreprocessedFrameImage);
            //			new StackWindow(new ImagePlus("after BG subtraction", getSubStackFloatCopy(mPreprocessedFrameImage, 1, 30)));
            mPreprocessedFrameNb = vFrame;
            //			new StackWindow(new ImagePlus("preprocessed image", getSubStackFloatCopy(mPreprocessedFrameImage, 1, 30)));
        }

        //		double[] vCentroid = centroidDetectionRefinement(mPreprocessedFrameImage,
        //				aX, aY, calculateExpectedZPositionAt(aX, aY, mPreprocessedFrameImage));
        final double[] vCentroid = centroidDetectionRefinement(mPreprocessedFrameImage,
                aX, aY, getBrightestSliceIndexAt(aX, aY, mPreprocessedFrameImage));

        //
        //	Check if bead was already used. We use the centroid as an identifier.
        //
        for (int vB = 0; vB < mBeads.size(); vB++) {
            if (Math.abs(mBeads.elementAt(vB).mCentroidX - vCentroid[0]) < 0.5 &&
                    Math.abs(mBeads.elementAt(vB).mCentroidY - vCentroid[1]) < 0.5 && // && vBead.mCentroidZ - vCentroid[2] < .5f
                    vFrame == mBeads.elementAt(vB).mFrame) {
                mBeads.removeElementAt(vB);
                return false;
            }
        }

        //
        // create the bead using the data of a non-preprocessed image
        //
        final ImageStack vIS = getAFrameCopy(mOriginalImagePlus, vFrame);
        mBeads.add(new Bead(vCentroid[0],vCentroid[1],vCentroid[2],vFrame,vIS));
        //		mBeads.add(new Bead(vCentroid[0],vCentroid[1],vCentroid[2],vFrame,mPreprocessedFrameImage));

        final Bead meanBead = meanBeads(mBeads);
        meanBead.thresholdPSFMap(0.005f);
        meanBead.showBead();

        return true;
    }

    public void boxCarBackgroundSubtractor(ImageStack is) {
        final Convolver convolver = new Convolver();
        final float[] kernel = new float[mMaskRadius * 2 +1];
        final int n = kernel.length;
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = 1f/n;
        }
        for (int s = 1; s <= is.getSize(); s++) {
            final ImageProcessor bg_proc = is.getProcessor(s).duplicate();
            convolver.convolveFloat(bg_proc, kernel, 1, n);
            convolver.convolveFloat(bg_proc, kernel, n, 1);
            is.getProcessor(s).copyBits(bg_proc, 0, 0, Blitter.SUBTRACT);
        }
    }

    protected double[] centroidDetectionRefinement(ImageStack aIS, float aX, float aY, float aZ){
        double vEpsX = 1.0;
        double vEpsY = 1.0;
        double vEpsZ = 1.0;
        final int vRadius = mMaskRadius;
        int vCounter = 10;

        for (int s = 0; s < aIS.getSize(); s++) {
            //				for (int i = 0; i < ips.getHeight(); i++) {
            //					for (int j = 0; j < ips.getWidth(); j++) {
            //						if (ips.getProcessor(s + 1).getPixelValue(j, i) < 0.0)
            //							ips.getProcessor(s + 1).putPixelValue(j, i, 0.0);
            //
            //					}
            //				}
            final float[] pixels = (float[])aIS.getPixels(s+1);
            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] < 0) {
                    pixels[i] = 0f;
                }
            }
        }

        while (vCounter > 0 &&
                (vEpsX > 0.5 || vEpsX < -0.5 || vEpsY > 0.5 || vEpsY < -0.5 || vEpsZ < 0.5 || vEpsZ > 0.5)) {
            vCounter--;
            float vM0 = 0.0F;

            vEpsX = 0.0F;
            vEpsY = 0.0F;
            vEpsZ = 0.0F;
            for (int vS = -vRadius; vS <= vRadius; vS++) {
                if (((int)aZ + vS) < 0 || ((int)aZ + vS) >= aIS.getSize()) {
                    continue;
                }
                final int vZ = (int)aZ + vS;
                for (int vV = -vRadius; vV <= vRadius; vV++) {
                    if (((int)aY + vV) < 0 || ((int)aY + vV) >= aIS.getHeight()) {
                        continue;
                    }
                    final int vY = (int)aY + vV;

                    for (int vU = -vRadius; vU <= vRadius; vU++) {
                        if ((aX + vU) < 0 || ((int)aX + vU) >= aIS.getWidth()) {
                            continue;
                        }
                        final int vX = (int)aX + vU;

                        final float vPxVal = aIS.getProcessor(vZ + 1).getPixelValue(vX, vY) * mMask[vS + vRadius][vV + vRadius][vU + vRadius];
                        vM0 += vPxVal;
                        vEpsX += vU * vPxVal;
                        vEpsY += vV * vPxVal;
                        vEpsZ += vS * vPxVal;
                    }
                }
            }
            if (vCounter <= 0) {
                System.out.println("no convergence in centroid detection!");
            }

            vEpsX /= vM0;
            vEpsY /= vM0;
            vEpsZ /= vM0;

            // This is a little hack to avoid numerical inaccuracy
            final int tx = (int)(10.0 * vEpsX);
            final int ty = (int)(10.0 * vEpsY);
            final int tz = (int)(10.0 * vEpsZ);

            if ((tx)/10.0 > 0.5) {
                if ((int)aX + 1 < aIS.getHeight()) {
                    aX++;
                }
            }
            else if ((tx)/10.0 < -0.5) {
                if ((int)aX - 1 >= 0) {
                    aX--;
                }
            }
            if ((ty)/10.0 > 0.5) {
                if ((int)aY + 1 < aIS.getWidth()) {
                    aY++;
                }
            }
            else if ((ty)/10.0 < -0.5) {
                if ((int)aY - 1 >= 0) {
                    aY--;
                }
            }
            if ((tz)/10.0 > 0.5) {
                if ((int)aZ + 1 < aIS.getSize()) {
                    aZ++;
                }
            }
            else if ((tz)/10.0 < -0.5) {
                if ((int)aZ - 1 >= 0) {
                    aZ--;
                }
            }

            if ((tx)/10.0 <= 0.5 && (tx)/10.0 >= -0.5 &&
                    (ty)/10.0 <= 0.5 && (ty)/10.0 >= -0.5 &&
                    (tz)/10.0 <= 0.5 && (tz)/10.0 >= -0.5) {
                break;
            }
        }
        return new double[]{aX + vEpsX + .5f, aY + vEpsY + .5f, aZ + vEpsZ + .5f};
        //		return new float[]{aX + vEpsX , aY + vEpsY , aZ + vEpsZ };
    }

    /**
     * Generates the dilation mask
     * <code>mask</code> is a var of class ParticleTracker_ and its modified internally here
     * Adapted from Ingo Oppermann implementation
     * @param mask_radius the radius of the mask (user defined)
     */
    public int[][][] generateMask(int mask_radius) {

        final int width = (2 * mask_radius) + 1;
        final int[][][] vMask = new int[width][width][width];
        for (int s = -mask_radius; s <= mask_radius; s++){
            for (int i = -mask_radius; i <= mask_radius; i++) {
                for (int j = -mask_radius; j <= mask_radius; j++) {
                    if ((i * i) + (j * j) + (s * s) <= mask_radius * mask_radius) {
                        vMask[s + mask_radius][j + mask_radius][i + mask_radius] = 1;
                    }
                    else {
                        vMask[s + mask_radius][j + mask_radius][i + mask_radius] = 0;
                    }

                }
            }
        }
        return vMask;
    }

    /**
     * Normalizes a given <code>ImageProcessor</code> to [0,1].
     * <br>According to the pre determend global min and max pixel value in the movie.
     * <br>All pixel intensity values I are normalized as (I-gMin)/(gMax-gMin)
     * @param ip ImageProcessor to be normalized
     */
    private void normalizeFrameFloat(ImageStack is) {
        for (int s = 1; s <= is.getSize(); s++){
            final float[] pixels=(float[])is.getPixels(s);
            float tmp_pix_value;
            for (int i = 0; i < pixels.length; i++) {
                tmp_pix_value = (pixels[i]-mGlobalMin)/(mGlobalMax - mGlobalMin);
                pixels[i] = (tmp_pix_value);
            }
        }
    }

    private void gaussBlur3D(ImageStack is, float aRadius) {
        final float[] vKernel = CalculateNormalizedGaussKernel(aRadius);
        int kernel_radius = vKernel.length / 2;
        final int nSlices = is.getSize();
        final int vWidth = is.getWidth();
        for (int i = 1; i <= nSlices; i++){
            final ImageProcessor restored_proc = is.getProcessor(i);
            final Convolver convolver = new Convolver();
            // no need to normalize the kernel - its already normalized
            convolver.setNormalize(false);
            //the gaussian kernel is separable and can done in 3x 1D convolutions!
            convolver.convolve(restored_proc, vKernel, vKernel.length , 1);
            convolver.convolve(restored_proc, vKernel, 1 , vKernel.length);
        }
        //2D mode, abort here; the rest is unnecessary
        if (is.getSize() == 1) {
            return;
        }

        //TODO: which kernel? since lambda_n = 1 pixel, it does not depend on the resolution -->not rescale
        //rescale the kernel for z dimension
        //			vKernel = CalculateNormalizedGaussKernel((float)(aRadius / (original_imp.getCalibration().pixelDepth / original_imp.getCalibration().pixelWidth)));

        kernel_radius = vKernel.length / 2;
        //to speed up the method, store the processor in an array (not invoke getProcessor()):
        final float[][] vOrigProcessors = new float[nSlices][];
        final float[][] vRestoredProcessors = new float[nSlices][];
        for (int s = 0; s < nSlices; s++) {
            vOrigProcessors[s] = (float[])is.getProcessor(s + 1).getPixelsCopy();
            vRestoredProcessors[s] = (float[])is.getProcessor(s + 1).getPixels();
        }
        //begin convolution with 1D gaussian in 3rd dimension:
        for (int y = kernel_radius; y < is.getHeight() - kernel_radius; y++){
            for (int x = kernel_radius; x < is.getWidth() - kernel_radius; x++){
                for (int s = kernel_radius + 1; s <= is.getSize() - kernel_radius; s++) {
                    float sum = 0;
                    for (int i = -kernel_radius; i <= kernel_radius; i++) {
                        sum += vKernel[i + kernel_radius] * vOrigProcessors[s + i - 1][y*vWidth+x];
                    }
                    vRestoredProcessors[s-1][y*vWidth+x] = sum;
                }
            }
        }
    }

    public float[] CalculateNormalizedGaussKernel(float aRadius){
        int vL = (int)aRadius * 3 * 2 + 1;
        if (vL < 3) {
            vL = 3;
        }
        final float[] vKernel = new float[vL];
        final int vM = vKernel.length/2;
        for (int vI = 0; vI < vM; vI++){
            vKernel[vI] = (float)(1f/(2f*Math.PI*aRadius*aRadius) * Math.exp(-(float)((vM-vI)*(vM-vI))/(2f*aRadius*aRadius)));
            vKernel[vKernel.length - vI - 1] = vKernel[vI];
        }
        vKernel[vM] = (float)(1f/(2f*Math.PI*aRadius*aRadius));

        //normalize the kernel numerically:
        float vSum = 0;
        for (int vI = 0; vI < vKernel.length; vI++){
            vSum += vKernel[vI];
        }
        final float vScale = 1.0f/vSum;
        for (int vI = 0; vI < vKernel.length; vI++){
            vKernel[vI] *= vScale;
        }
        return vKernel;
    }

    /**
     * Returns a copy of a single frame. Note that the properties of the ImagePlus have to be correct
     * @param aMovie
     * @param aFrameNumber beginning with 1...#frames
     * @return The frame copy.
     */
    public static ImageStack getAFrameCopy(ImagePlus aMovie, int aFrameNumber)
    {
        if (aFrameNumber > aMovie.getNFrames() || aFrameNumber < 1) {
            throw new IllegalArgumentException("frame number = " + aFrameNumber);
        }
        final int vS = aMovie.getNSlices();
        return getSubStackFloatCopy(aMovie.getStack(), (aFrameNumber-1) * vS + 1, aFrameNumber * vS);
    }

    /**
     *
     * @param is
     * @param startPos
     * @param endPos
     * @return
     */
    private static ImageStack getSubStackFloatCopy(ImageStack is, int startPos, int endPos){
        final ImageStack res = new ImageStack(is.getWidth(), is.getHeight());
        if (startPos > endPos || startPos < 0 || endPos < 0) {
            return null;
        }
        for (int i = startPos; i <= endPos; i++) {
            res.addSlice(is.getSliceLabel(i), is.getProcessor(i).convertToFloat().duplicate());
        }
        return res;
    }

    protected void doZProjection(ImagePlus aIMP)
    {
        final ImageStack vZProjectedStack = new ImageStack(mWidth, mHeight);
        final ZProjector vZProjector = new ZProjector(aIMP);
        vZProjector.setMethod(ZProjector.MAX_METHOD);
        for (int vC = 0; vC < aIMP.getNFrames(); vC++){
            vZProjector.setStartSlice(vC * mNSlices + 1);
            vZProjector.setStopSlice((vC + 1) * mNSlices);
            vZProjector.doProjection();
            vZProjectedStack.addSlice("", vZProjector.getProjection().getProcessor());
        }
        mZProjectedImagePlus = new ImagePlus("Z-Projected " + aIMP.getTitle(), vZProjectedStack);
        mZProjectedImagePlus.repaintWindow();
        //		vZProjectedImage.show();
    }

    /**
     * Calculates the expected mean of a gaussian fitted to a ray trough the imagestack.
     * @param aX The x position of the ray
     * @param aY The y position of the ray
     * @param aIS The imageStack where the intensities are read out.
     * @return the expected z position of a Gaussian in [1; <code>aIS.getSize</code>]
     */
    //	private float calculateExpectedZPositionAt(int aX, int aY, ImageStack aIS)
    //	{
    //		float vMaxInt = 0;
    //		int vMaxSlice = 0;
    //		for (int vZ = 0; vZ < mNSlices; vZ++) {
    //			float vThisInt;
    //			if ((vThisInt = aIS.getProcessor(vZ+1).getf(aX, aY)) > vMaxInt) {
    //				vMaxInt =  vThisInt;
    //				vMaxSlice = vZ;
    //			}
    //
    //		}
    //		float vSumOfIntensities = 0f;
    //		float vRes = 0f;
    //		int vStartSlice = Math.max(1, vMaxSlice-2);
    //		int vStopSlice = Math.min(mNSlices, vMaxSlice+2);
    //		for (int vZ = vStartSlice; vZ <= vStopSlice; vZ++) {
    //			vSumOfIntensities += aIS.getProcessor(vZ).getf(aX, aY);
    //			vRes += (vZ + 1) * aIS.getProcessor(vZ).getf(aX, aY);
    //		}
    //		return vRes / vSumOfIntensities;
    //	}

    public int getBrightestSliceIndexAt(int aX, int aY, ImageStack aIS) {
        float vMaxInt = 0;
        int vMaxSlice = 0;
        for (int vZ = 0; vZ < mNSlices; vZ++) {
            float vThisInt;
            if ((vThisInt = aIS.getProcessor(vZ+1).getf(aX, aY)) > vMaxInt) {
                vMaxInt =  vThisInt;
                vMaxSlice = vZ;
            }
        }
        return vMaxSlice;
    }

    protected void initVisualization()
    {
        // generate the previewCanvas - while generating it the drawing will be done
        final DrawCanvas vDrawCanvas = new DrawCanvas(mZProjectedImagePlus);

        // display the image and canvas in a stackWindow
        new TrajectoryStackWindow(mZProjectedImagePlus, vDrawCanvas);
    }

    public Bead meanBeads(Vector<Bead> aBeads) {
        final double[][] vMeanMap = new double[mMapSizeZ][mMapSizeR];
        final int[][] vMeanMapScaler = new int[mMapSizeZ][mMapSizeR];


        for (int vZ = 0; vZ < mMapSizeZ; vZ++) {
            for (int vR = 0; vR < mMapSizeR; vR++) {
                vMeanMap[vZ][vR] = 0;
                vMeanMapScaler[vZ][vR] = 0;
            }
        }

        //float vScaler = 1f / (float)aBeads.size();
        for (final Bead vB : aBeads) {
            final double[][] vBeadMap = vB.getPSFMap();
            for (int vZ = 0; vZ < mMapSizeZ; vZ++) {
                for (int vR = 0; vR < mMapSizeR; vR++) {
                    //if (vB.mPSFMapValid[vZ][vR]) {
                    if (vBeadMap[vZ][vR]>0) {
                        vMeanMap[vZ][vR] += vBeadMap[vZ][vR];// * vScaler[][];
                        vMeanMapScaler[vZ][vR] += 1;
                    }
                }
            }
        }

        for (int vZ = 0; vZ < mMapSizeZ; vZ++) {
            for (int vR = 0; vR < mMapSizeR; vR++) {
                if (vMeanMapScaler[vZ][vR] > 0) {
                    vMeanMap[vZ][vR] /= vMeanMapScaler[vZ][vR];
                } else {
                    vMeanMap[vZ][vR] = 0;
                }
            }
        }
        //new ImagePlus("map normalizer",new FloatProcessor(vMeanMapScaler)).show();
        return new Bead(vMeanMap);
    }

    private class Bead {
        double mCentroidX = -1f, mCentroidY = -1f, mCentroidZ = -1f;
        int mFrame = -1;
        double[][] mPSFMap;

        /**
         * Constructor
         * @param aX Bead Position
         * @param aY Bead Position
         * @param aZ Bead Position
         * @param aFrame the corrsponding Frame
         * @param aIS The corrsponding Stack data
         */
        public Bead(double aX, double aY, double aZ, int aFrame, ImageStack aIS) {
            mCentroidX = aX;
            mCentroidY = aY;
            mCentroidZ = aZ;
            mFrame = aFrame;


            final double[] vCentroid = new double[]{mCentroidX, mCentroidY, mCentroidZ};
            System.out.println("Centroid: x = " + mCentroidX + ", y = " + mCentroidY + "z = " + mCentroidZ);
            mPSFMap = generatePSFmap(aIS, vCentroid, mRMaxInNm, mZMaxInNm, mMapSizeR, mMapSizeZ);
            //			thresholdPSFMap(0.1f);
            normalizePSFMap();
        }

        public Bead(double[][] meanMap) {
            mPSFMap = meanMap;
        }


        public void showBead(double[][] aPSFmap) {
            final float[][] vMap = new float[aPSFmap.length][aPSFmap[0].length];
            for (int vZ = 0; vZ < aPSFmap.length; vZ++) {
                for (int vR = 0; vR < aPSFmap[0].length; vR++) {
                    vMap[vZ][vR] = (float)aPSFmap[vZ][vR];
                }
            }

            if (mBeadImage != null) {
                mBeadImage.changes = false;
                mBeadImage.close(); // TODO: doesn't work
            }
            mBeadImage = new ImagePlus("bsp map",new FloatProcessor(vMap));
            mBeadImage.getCalibration().setUnit("nm");
            mBeadImage.getCalibration().pixelWidth = (2f*mZMaxInNm / mMapSizeZ);
            mBeadImage.getCalibration().pixelHeight = ((float)mRMaxInNm / (float)mMapSizeR);
            mBeadImage.show();
        }

        public void showBead() {
            showBead(mPSFMap);
        }

        protected void thresholdPSFMap(float aPercentile){
            // get min and max:
            double vMin = Double.MAX_VALUE;
            double vMax = Double.MIN_VALUE;
            for (int vI = 0; vI < mPSFMap.length; vI++) {
                for (int vJ = 0; vJ < mPSFMap[0].length; vJ++) {
                    if (mPSFMap[vI][vJ] < vMin) {
                        vMin = mPSFMap[vI][vJ];
                    }
                    if (mPSFMap[vI][vJ] > vMax) {
                        vMax = mPSFMap[vI][vJ];
                    }
                }
            }
            // shift to 0:
            for (int vI = 0; vI < mPSFMap.length; vI++) {
                for (int vJ = 0; vJ < mPSFMap[0].length; vJ++) {
                    mPSFMap[vI][vJ] -= vMin;
                }
            }
            // the new max value:
            vMax -= vMin;

            // shift to 0:
            for (int vI = 0; vI < mPSFMap.length; vI++) {
                for (int vJ = 0; vJ < mPSFMap[0].length; vJ++) {
                    final double vNewVal = mPSFMap[vI][vJ] - aPercentile * vMax;
                    if (vNewVal < 0) {
                        mPSFMap[vI][vJ] = 0;
                    } else  {
                        mPSFMap[vI][vJ] = vNewVal;
                    }
                }

            }


        }

        protected void normalizePSFMap() {
            float vSum = 0;
            for (int vI = 0; vI < mPSFMap.length; vI++) {
                for (int vJ = 0; vJ < mPSFMap[0].length; vJ++) {
                    vSum += mPSFMap[vI][vJ];
                }
            }
            for (int vI = 0; vI < mPSFMap.length; vI++) {
                for (int vJ = 0; vJ < mPSFMap[0].length; vJ++) {
                    mPSFMap[vI][vJ] /= vSum;
                }
            }
        }

        protected boolean interpolatePSFmap(double[][] aMap) {
            //
            //first, search all valuable lines in z
            //
            final Vector<Integer> vValuableZIndices = new Vector<Integer>();

            for (int vZ = 0; vZ < aMap.length; vZ++) {
                //boolean vRowAlreadyChosen = false;
                for (int vR = 0; vR < aMap[vZ].length; vR++) {
                    if (aMap[vZ][vR] > 0f) {
                        //if (!vRowAlreadyChosen) {
                        vValuableZIndices.add(vZ);
                        //vRowAlreadyChosen = true;
                        //continue;
                        break;
                        //}
                    }

                }
            }
            //
            //second, interpolate in Z direction (splines)
            //
            for (int vR = 0; vR < aMap[0].length; vR++) {
                final float[] vNodeX = new float[vValuableZIndices.size()];
                final float[] vNodeY = new float[vValuableZIndices.size()];
                for (int vI = 0; vI < vValuableZIndices.size(); vI++) {
                    vNodeX[vI] = vValuableZIndices.elementAt(vI);
                    vNodeY[vI] = (float)aMap[vValuableZIndices.elementAt(vI)][vR];
                }
                if (vValuableZIndices.isEmpty())
                {
                    continue; /// a empty line in R
                }
                for (int vZ = vValuableZIndices.elementAt(0); vZ <= vValuableZIndices.lastElement(); vZ++) {
                    if (aMap[vZ][vR] > 0f) {
                        continue;
                    }
                    //					aMap[vZ][vR] = interpolateQuadratic(vZ, vNodeX, vNodeY);
                    aMap[vZ][vR] = interpolateLinear(vZ, vNodeX, vNodeY);

                }
            }
            return true;
        }

        protected double[][] generatePSFmap(ImageStack aIS, double[] aCentroid, float aRMaxDist, float aZMaxDist, int aMapSizeR, int aMapSizeZ){
            final double[][] vPSFmap = generatePSFmapSparse(aIS, aCentroid, aRMaxDist, aZMaxDist, aMapSizeR, aMapSizeZ);

            if (!checkSparseMap(vPSFmap)) {
                IJ.showMessage("Too small sampling rate !!");
            }

            interpolatePSFmap(vPSFmap);
            //			fillPSFmapEdges(vPSFmap); deprecated!
            return vPSFmap;

        }

        protected boolean checkSparseMap(double[][] aMap) {
            for (int vR = 0; vR < aMap[0].length; vR++) {
                boolean vFoundNonZeroValue = false;
                for (int vZ = 0; vZ < aMap.length; vZ++) {
                    if (aMap[vZ][vR] > 0f) {
                        vFoundNonZeroValue = true;
                    }
                }
                if (!vFoundNonZeroValue) {
                    return false;
                }
            }
            return true;
        }

        /**
         * This method is deprecated: It constantly diffuses intensity in axial direction
         * leading to a bias of too large PSF maps in z-direction if there is indeed a 0 entry
         * at the boarder of the sparse map. It seems to be better to
         * leave the edges unchanged and recording a lot of beads :-)
         * @param aMap
         */
        //		protected void fillPSFmapEdges(double[][] aMap) {
        //			int vFirstZ = -1;
        //			int vLastZ = -1;
        //			//search for first valuable row
        //			for (int vZ = 0; vZ < aMap.length; vZ++) {
        //				for (int vR = 0; vR < aMap[0].length; vR++) {
        //					if (aMap[vZ][vR] > 0f) {
        //						vFirstZ = vZ;
        //						break;
        //					}
        //				}
        //				if (vFirstZ >= 0) break;
        //			}
        //			//search for last valuable row
        //			for (int vZ = aMap.length-1; vZ >= 0; vZ--) {
        //				for (int vR = aMap[0].length-1; vR >= 0; vR--) {
        //					if (aMap[vZ][vR] > 0f) {
        //						vLastZ = vZ;
        //						break;
        //					}
        //				}
        //				if (vLastZ >= 0) break;
        //			}
        //			//copy the first valuable row to all rows before
        //			for (int vZ = 0; vZ < vFirstZ; vZ++) {
        //				for (int vR = 0; vR < aMap[0].length; vR++) {
        //					aMap[vZ][vR] = aMap[vFirstZ][vR];
        //				}
        //			}
        //			//copy the last valuable row to all rows after
        //			for (int vZ = vLastZ+1; vZ < aMap.length; vZ++) {
        //				for (int vR = 0; vR < aMap[0].length; vR++) {
        //					aMap[vZ][vR] = aMap[vLastZ][vR];
        //				}
        //			}
        //		}

        /**
         *
         * @param aIS
         * @param aCentroid in image coordinates (in px)
         * @param aRMaxDist in real coordinates in Nm
         * @param aZMaxDist in real coordinates in Nm
         * @param aMapSizeR specifies the resolution in R of the map.
         * @param aMapSizeZ specifies the resolution in Z of the map.
         * @return
         */
        protected double[][] generatePSFmapSparse(ImageStack aIS, double[] aCentroid, float aRMaxDist, float aZMaxDist, int aMapSizeR, int aMapSizeZ){
            final double[][] vMap = new double[aMapSizeZ][aMapSizeR];
            final int[][] vMapCount = new int[aMapSizeZ][aMapSizeR];

            //float vRStepSize

            final double vMaxRInPx = aRMaxDist / mPxWidthInNm;
            final double vMaxZInPx = aZMaxDist / mPxDepthInNm;

            final double vRIncrementPerPxInPx = vMaxRInPx / aMapSizeR; //map coordinate grid increment
            final double vZIncrementPerPxInPx = 2*vMaxZInPx / aMapSizeZ;

            int vZStart = (int)(aCentroid[2] - vMaxZInPx);// - 1);
            int vYStart = (int)(aCentroid[1] - vMaxRInPx);// - 1);
            int vXStart = (int)(aCentroid[0] - vMaxRInPx);// - 1);
            int vZEnd = (int)(aCentroid[2] + vMaxZInPx)+1;// + 1);
            int vYEnd = (int)(aCentroid[1] + vMaxRInPx);// + 1);
            int vXEnd = (int)(aCentroid[0] + vMaxRInPx);// + 1);


            if (vZStart < 0 || vZEnd >= mNSlices || vYStart < 0 || vYEnd >= mHeight || vXStart < 0 || vXEnd >= mWidth) {
                System.out.println("Warning: sampling region is partly out of the image domain.");
                //TODO: give appropriate output
                vZStart = Math.max(0, vZStart);
                vYStart = Math.max(0, vYStart);
                vXStart = Math.max(0, vXStart);
                vZEnd = Math.min(vZEnd, mNSlices-1);
                vYEnd = Math.min(vYEnd, mHeight-1);
                vXEnd = Math.min(vXEnd, mWidth-1);

            }
            for (int vZ = vZStart; vZ <= vZEnd; vZ++) {
                if (vZ < 1 || vZStart < 0 || vZEnd > mNSlices) {
                    continue;
                }
                //				BicubicInterpolator vBI = new BicubicInterpolator();
                final BilinearInterpolator vBI = new BilinearInterpolator();
                vBI.setImageProcessor(aIS.getProcessor(vZ));

                for (double vR = 0f; vR <= vMaxRInPx; vR = vR + mRInc) {
                    for (double vPhi = 0; vPhi < Math.PI*2; vPhi = vPhi + mPhiInc) {
                        //+.5 offset since we want the distance from the centroid to the middle of a voxel.
                        //						float vRDistInPx = (float)Math.sqrt((vX+.5-aCentroid[0])*(vX+.5-aCentroid[0])+(vY+.5-aCentroid[1])*(vY+.5-aCentroid[1]));
                        final float vZDistInPx = (float) ((vZ ) - aCentroid[2]); //might be negative

                        final double vX = (aCentroid[0] + vR*Math.cos(vPhi));
                        final double vY =  (aCentroid[1] + vR*Math.sin(vPhi));
                        final int vXs = (int)vX;
                        final int vYs = (int)vY;

                        //
                        // Bilinear interpolation
                        //
                        double vXRest;
                        double vYRest;
                        double vInterpolatedIntensity = 0.0;
                        if ((vXRest = vX-vXs-.5) < 0f) {
                            vXRest *= -1f;
                            if ((vYRest = vY-vYs-.5) < 0f) {
                                //3rd quadrant
                                vYRest *= -1f;
                                final double vInterpolatedOnNorthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs-1, vYs);

                                final double vInterpolatedOnSouthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs-1)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs-1, vYs-1);

                                vInterpolatedIntensity = (1f-vYRest)* vInterpolatedOnNorthGridEdge + vYRest * vInterpolatedOnSouthGridEdge;

                            } else {
                                //2nd quadrant
                                final double vInterpolatedOnSouthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs-1, vYs);

                                final double vInterpolatedOnNorthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs+1)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs-1, vYs+1);

                                vInterpolatedIntensity = (1f-vYRest)* vInterpolatedOnSouthGridEdge + vYRest * vInterpolatedOnNorthGridEdge;
                            }
                        } else 	{
                            if ((vYRest = vY-vYs - 0.5f) < 0f) {
                                //4th quadrant
                                vYRest *= -1f;
                                final double vInterpolatedOnNorthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs+1, vYs);

                                final double vInterpolatedOnSouthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs-1)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs+1, vYs-1);

                                vInterpolatedIntensity = (1f-vYRest)* vInterpolatedOnNorthGridEdge + vYRest * vInterpolatedOnSouthGridEdge;
                            } else {
                                //1st quadrant
                                final double vInterpolatedOnSouthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs+1, vYs);

                                final double vInterpolatedOnNorthGridEdge = (1f-vXRest)*aIS.getProcessor(vZ).getPixelValue(vXs, vYs+1)
                                        + vXRest*aIS.getProcessor(vZ).getPixelValue(vXs+1, vYs+1);

                                vInterpolatedIntensity = (1f-vYRest)* vInterpolatedOnSouthGridEdge + vYRest * vInterpolatedOnNorthGridEdge;

                            }
                        }
                        // TODO: the next check is dirty and slow: choose a reasonable sampling domain(in Z and R)
                        if ((int)((vZDistInPx + vMaxZInPx) / vZIncrementPerPxInPx) < 0 || (int)((vZDistInPx + vMaxZInPx) / vZIncrementPerPxInPx) >= vMap.length ||
                                (int)(vR / vRIncrementPerPxInPx) >= mMapSizeR || (int)(vR / vRIncrementPerPxInPx) < 0) {
                            continue;
                        }

                        //						if ((int)((vZDistInPx + vMaxZInPx) / vZIncrementPerPxInPx) >= 1000 || (int)(vR / vRIncrementPerPxInPx) >= 1000) {
                        //							System.out.print("stop");
                        //						}
                        //						vMap[(int)((vZDistInPx + vMaxZInPx) / vZIncrementPerPxInPx)][(int)(vR / vRIncrementPerPxInPx)] +=
                        //							vBI.getInterpolatedPixel(new Point2D.Float(vX+.5f,vY+.5f));
                        vMap[(int)((vZDistInPx + vMaxZInPx) / vZIncrementPerPxInPx)][(int)(vR / vRIncrementPerPxInPx)] +=
                                vInterpolatedIntensity;
                        vMapCount[(int)((vZDistInPx + vMaxZInPx) / vZIncrementPerPxInPx)][(int)(vR / vRIncrementPerPxInPx)]++;
                    }
                }
            }


            for (int vZ = 0; vZ < aMapSizeZ; vZ++) {
                for (int vR = 0; vR < aMapSizeR; vR++) {
                    if (vMapCount[vZ][vR] != 0) {
                        vMap[vZ][vR] /= vMapCount[vZ][vR];
                    }
                }
            }

            return vMap;
        }

        public float interpolateLinear(float aX, float[] aKnotsX, float[] aKnotsY) {
            int vI = 0;
            if (aX < aKnotsX[0] || aX > aKnotsX[aKnotsX.length-1]) {
                throw new IllegalArgumentException("x is not in knot interval");
            }
            for (vI = 0; vI < aKnotsX.length - 1; vI++) {
                if (aX < aKnotsX[vI]) {
                    break;
                }
            }
            final float vL = aKnotsX[vI] - aKnotsX[vI - 1];
            final float vA = aX - aKnotsX[vI - 1];
            return aKnotsY[vI - 1] * (1.f-vA/vL) + aKnotsY[vI] * (vA/vL);
        }

        //		public float interpolateQuadratic(float aX, float[] aKnotsX, float[] aKnotsY) {
        //			//next bigger element is:
        //			int vI = 0;
        //			if (aX < aKnotsX[0] || aX > aKnotsX[aKnotsX.length-1])
        //				throw new IllegalArgumentException("x is not in knot interval");
        //			for (vI = 0; vI < aKnotsX.length; vI++) {
        //				if (aX < aKnotsX[vI]) {
        //					break;
        //				}
        //			}
        //			//which one is closer?
        //			if (aKnotsX[vI] - aX > aX - aKnotsX[vI -1]) {
        //				vI = vI - 1;
        //			}
        //			//at the boundaries:
        //			if (vI == 0) vI++;
        //			if (vI == aKnotsX.length-1) vI--;
        //
        //			//now vI is the index of the mid-point
        //			float vY = 0;
        //			vY += aKnotsY[vI-1] * ((aX - aKnotsX[vI]) * (aX - aKnotsX[vI+1])) / ((aKnotsX[vI-1]-aKnotsX[vI])*(aKnotsX[vI-1] - aKnotsX[vI+1]));
        //			vY += aKnotsY[vI]   * ((aX - aKnotsX[vI-1]) * (aX - aKnotsX[vI+1])) / ((aKnotsX[vI]-aKnotsX[vI-1])*(aKnotsX[vI] - aKnotsX[vI+1]));
        //			vY += aKnotsY[vI+1] * ((aX - aKnotsX[vI-1]) * (aX - aKnotsX[vI])) / ((aKnotsX[vI+1]-aKnotsX[vI-1])*(aKnotsX[vI+1] - aKnotsX[vI]));
        //			return vY;
        //		}

        public double[][] getPSFMap() {
            return mPSFMap;
        }
    }

    @SuppressWarnings("serial")
    private class DrawCanvas extends ImageCanvas {
        public DrawCanvas(ImagePlus aImagePlus){
            super(aImagePlus);
        }
        @Override
        public void paint(Graphics aG){
            super.paint(aG);
            final int vFrame = mZProjectedImagePlus.getCurrentSlice();
            aG.setColor(Color.red);
            for (final Bead vBead : mBeads) {
                if (vBead.mFrame == vFrame){
                    final int vX = (int)Math.round(vBead.mCentroidX*magnification);
                    final int vY = (int)Math.round(vBead.mCentroidY*magnification);
                    aG.drawLine(vX - 5, vY, vX + 5, vY);
                    aG.drawLine(vX, vY - 5, vX, vY + 5);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    private class TrajectoryStackWindow extends StackWindow implements MouseListener{

        public TrajectoryStackWindow(ImagePlus aIMP, ImageCanvas aIC) {
            super(aIMP, aIC);
            aIC.addMouseListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent arg0) {


        }

        @Override
        public void mouseEntered(MouseEvent arg0) {


        }

        @Override
        public void mouseExited(MouseEvent arg0) {


        }

        @Override
        public void mousePressed(MouseEvent arg0) {


        }

        @Override
        public void mouseReleased(MouseEvent aE) {
            PSF_estimator_3D.this.mouseClicked(this.ic.offScreenX(aE.getPoint().x),this.ic.offScreenY(aE.getPoint().y));
        }

    }

    public void mouseClicked(int aXCoord, int aYCoord) {
        if (IJ.shiftKeyDown()){
            registerOrDeleteNewBeadAt(aXCoord, aYCoord);
            mZProjectedImagePlus.repaintWindow();
        }
    }

    boolean getUserDefinedParams(){
        // Start Dialog to get necessary plugin parameters
        final GenericDialog gd = new GenericDialog("Configuration");
        gd.addMessage("Pease make sure to have correctly set the\n pixel size in nm in the image properties");
        gd.addMessage("--------");
        gd.addMessage("Please enter necessary Information");
        //		gd.addNumericField("Apparent Radius of Point Sources [Pixel]", 3, 2);
        gd.addNumericField("Maximum lateral sampling radius[nm]", mRMaxInNm, 0);
        gd.addNumericField("Maximum axial sampling radius[nm]", mZMaxInNm, 0);
        gd.addNumericField("Final lateral PSF map size [px]", mMapSizeR, 0);
        gd.addNumericField("Final axial PSF map size [px]", mMapSizeZ, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }
        // Read user inputs
        //		radius = gd.getNextNumber();
        mRMaxInNm = (int)gd.getNextNumber();
        mZMaxInNm = (int)gd.getNextNumber();
        mMapSizeR = (int)gd.getNextNumber();
        mMapSizeZ = (int)gd.getNextNumber();

        return true;
    }
}
