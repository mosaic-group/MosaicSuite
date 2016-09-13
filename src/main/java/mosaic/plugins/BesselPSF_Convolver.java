package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import mosaic.utils.math.MathOps;

/**
 * A small ImageJ Plugin convolves an image or a stack with a bessel point spread function.
 * @author Janick Cardinale, ETH Zurich
 * @version 1.0, January 08
 */

public class BesselPSF_Convolver implements ExtendedPlugInFilter { // NO_UCD
    // Setup stuff + input arguments
    static final int FLAGS = DOES_8G + DOES_16 + DOES_32;
    int mSetupFlags = FLAGS;
    ImagePlus mImp;

    // User input
    double mRMax = 0.5e-6; // 0.5um
    double mNA = 1.2f;
    double mLambda  = 450 * 1e-9;

    // Stuff for kernel computing
    double mXResolution = 1;
    double mYResolution = 1;
    int mKernelWidth = 0;
    int mKernelHeight = 0;

    @Override
    public int setup(String aArgs, ImagePlus aImp) {
        mImp = aImp;
        mSetupFlags = IJ.setupDialog(mImp, FLAGS);
        return mSetupFlags;
    }

    @Override
    public int showDialog(ImagePlus arg0, String arg1, PlugInFilterRunner arg2) {
        if (!checkAndSetUnit(mImp)) {
            return DONE;
        }
        if (!showDialog()) {
            return DONE;
        }
        return mSetupFlags;
    }

    @Override
    public void setNPasses(int arg0) {
        // Nothing to be done here
    }

    @Override
    public void run(ImageProcessor aImageProcessor) {
        final float[] kernel = generateBesselKernel();
        aImageProcessor.convolve(kernel, mKernelWidth, mKernelHeight);
    }

    private float[] generateBesselKernel() {
        final int vXRadius = (int) (mRMax / mXResolution) + 1; // in pixel
        final int vYRadius = (int) (mRMax / mYResolution) + 1; // in pixel
        mKernelWidth = (vXRadius * 2 + 1);
        mKernelHeight = (vYRadius * 2 + 1);
        final float[] vKernel = new float[mKernelWidth * mKernelHeight];

        // TODO: What to do (r=0 -> undefined)
        final double vBesselMax = bessel_PSF(mXResolution / 10, mLambda, mNA);

        for (int vYI = 0; vYI < mKernelHeight; vYI++) {
            for (int vXI = 0; vXI < mKernelWidth; vXI++) {
                double vDist = Math.sqrt( Math.pow((vXRadius - vXI) * mXResolution, 2) + Math.pow((vYRadius - vYI) * mYResolution, 2) );
                vKernel[vYI * mKernelWidth + vXI] = (float) (bessel_PSF(vDist, mLambda, mNA) / vBesselMax);
            }
        }
        vKernel[vYRadius * mKernelWidth + vXRadius] = 1f;

        return vKernel;
    }

     private double bessel_PSF(double aRadius, double aLambda, double aApparture) {
        final double vA = 2 * Math.PI * aApparture / aLambda;
        final double vR = 2 * MathOps.bessel1(aRadius * vA) / aRadius;
        return vR * vR;
    }

    /**
     * Gets information from image about its pixel resolution.
     * @param aImp Input image
     * @return true if setup correctly, false otherwise
     */
    private boolean checkAndSetUnit(ImagePlus aImp) {
        if (!aImp.getCalibration().getUnit().endsWith("m")){
            IJ.showMessage("Please set a 'x-meter'(e.g. \"nm\",\"mm\" unit in the image properties.");
            return false;
        }

        final String vUnit = aImp.getCalibration().getUnit();
        double vS = 0;
        if (vUnit.equalsIgnoreCase("km")) {
            vS = 0.001;
        }
        if (vUnit.equalsIgnoreCase("m")) {
            vS = 1;
        }
        if (vUnit.equalsIgnoreCase("dm")) {
            vS = 10;
        }
        if (vUnit.equalsIgnoreCase("cm")) {
            vS = 100;
        }
        if (vUnit.equalsIgnoreCase("mm")) {
            vS = 1000;
        }
        if (vUnit.equalsIgnoreCase(IJ.micronSymbol + "m") || vUnit.equalsIgnoreCase("um")) {
            vS = 1000000;
        }
        if (vUnit.equalsIgnoreCase("nm")) {
            vS = 1e9;
        }

        if (vS != 0) {
            mXResolution = aImp.getCalibration().pixelWidth / vS;
            mYResolution = aImp.getCalibration().pixelHeight / vS;
            return true;
        }

        IJ.showMessage("Unit in image properties unknown: " + vUnit +".");

        return false;
    }

    private boolean showDialog() {
        final GenericDialog gd = new GenericDialog("Bessel PSF parameter");
        gd.addNumericField("wavelength", mLambda*1e9, 0, 5, "nm");
        gd.addNumericField("Numeric Aparture: ", mNA, 0, 5, "");
        gd.addNumericField("Max Radius",   mRMax*1e9, 0, 5, "nm");
        gd.showDialog();

        if (gd.wasCanceled()) {
            return false;
        }

        mLambda = gd.getNextNumber() * 1e-9;
        mNA = gd.getNextNumber();
        mRMax = gd.getNextNumber() * 1e-9;

        return true;
    }
}
