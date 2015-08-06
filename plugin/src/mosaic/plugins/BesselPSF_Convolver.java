package mosaic.plugins;
 
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

/**
 * A small ImageJ Plugin convolves an image or a stack with a bessel point spread function.
 * @author Janick Cardinale, ETH Zurich
 * @author Tao Pang, University of Nevada, for the implementation of the Bessel method
 * @version 1.0, January 08
 * 
 * <p><b>Disclaimer</b>
 * <br>IN NO EVENT SHALL THE ETH BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, 
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND
 * ITS DOCUMENTATION, EVEN IF THE ETH HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * THE ETH SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. 
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE ETH HAS NO 
 * OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.<p>
 */

public class BesselPSF_Convolver implements ExtendedPlugInFilter {
    // Setup stuff + input arguments
    static final int FLAGS = DOES_8G + DOES_16 + DOES_32;
    int mSetupFlags = FLAGS;
    ImagePlus mImp;
    
    // User input 
    double mRMax = .5e-6; // .5um
    double mNA = 1.2f;
    double mLambda  = 450*1e-9;

    // Stuff for kernel computing 
    double mXResolution = 1;
	double mYResolution = 1;
	int mKernelWidth = 0;
	int mKernelHeight = 0;
	
	@Override
	public int setup(String aArgs, ImagePlus aImp) {
	    mImp = aImp;
	    mSetupFlags = IJ.setupDialog(aImp, DOES_8G + DOES_16 + DOES_32);
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
	public void run(ImageProcessor aImageProcessor) {
	    float[] kernel = generateBesselKernel();
		aImageProcessor.convolve(kernel, mKernelWidth, mKernelHeight);
	}
	
	private float[] generateBesselKernel() {
        int vXRadius = (int) (mRMax / mXResolution) + 1; // in pixel
        int vYRadius = (int) (mRMax / mYResolution) + 1; // in pixel
        mKernelWidth = (vXRadius * 2 + 1);
        mKernelHeight = (vYRadius * 2 + 1);
        float[] vKernel = new float[mKernelWidth * mKernelHeight];
        
        // TODO: What to do (r=0 -> undefined)
        double vBesselMax = bessel_PSF(mXResolution / 10, mLambda, mNA);
        
        for (int vYI = 0; vYI < mKernelHeight; vYI++) {
            for (int vXI = 0; vXI < mKernelWidth; vXI++) {
                float vDist = (float) Math.sqrt((vXRadius - vXI) * mXResolution * (vXRadius - vXI) * mXResolution
                        + (vYRadius - vYI) * mYResolution * (vYRadius - vYI) * mYResolution);
                vKernel[coords(vXI, vYI)] = (float) (bessel_PSF(vDist, mLambda, mNA) / vBesselMax);
            }
        }
        vKernel[coords(vXRadius, vYRadius)] = 1f;

        return vKernel;
	}
	
	private int coords(int aX, int aY) {
		return aY * mKernelWidth + aX;
	}
	
	/**
	 * Gets information from image about its pixel resolution.
	 * @param aImp Input image
	 * @return true if setup correctly, false otherwise
	 */
	private boolean checkAndSetUnit(ImagePlus aImp) {
		if(!aImp.getCalibration().getUnit().endsWith("m")){
			IJ.showMessage("Please set a 'x-meter'(e.g. \"nm\",\"mm\" unit in the image properties.");
			return false;
		}
		
		String vUnit = aImp.getCalibration().getUnit();
		double vS = 0;
		if(vUnit.equalsIgnoreCase("km")) {
			vS = 0.001;
		}
		if(vUnit.equalsIgnoreCase("m")) {
			vS = 1;
		}
		if(vUnit.equalsIgnoreCase("dm")) {
			vS = 10;
		}
		if(vUnit.equalsIgnoreCase("cm")) {
			vS = 100;
		}
		if(vUnit.equalsIgnoreCase("mm")) {
			vS = 1000;
		}
		if(vUnit.equalsIgnoreCase(IJ.micronSymbol + "m") || vUnit.equalsIgnoreCase("um")) {
			vS = 1000000;
		}
		if(vUnit.equalsIgnoreCase("nm")) {
			vS = 1e9;
		}
		
		if(vS != 0) {
			mXResolution = aImp.getCalibration().pixelWidth / vS;
			mYResolution = aImp.getCalibration().pixelHeight / vS;
			return true;
		}
		
		IJ.showMessage("Unit in image properties unknown: " + vUnit +".");
		
		return false;
	}
	
   private double bessel_PSF(double aRadius, double aLambda, double aApparture) {
        double vA = 2 * Math.PI * aApparture / aLambda;
        double vR = 2 * bessel(aRadius * vA, 1, (int)(aRadius * vA + 10))[1] / aRadius;
        return vR * vR;
    }
    
    private boolean showDialog() {
        GenericDialog gd = new GenericDialog("Bessel PSF parameter");
        gd.addNumericField("wavelength", mLambda*1e9, 0, 5, "nm");
        gd.addNumericField("Numeric Aparture: ", mNA, 0, 5, "");
        gd.addNumericField("Max Radius",   mRMax*1e9, 0, 5, "nm");
        gd.showDialog();
        
        if (gd.wasCanceled()) return false;
        
        mLambda = gd.getNextNumber() * 1e-9;
        mNA = gd.getNextNumber();
        mRMax = gd.getNextNumber() * 1e-9;
        
        return true;
    }

    @Override
    public void setNPasses(int arg0) {
        // Nothing to be done here
    }
	    
    // ==================================================================================	   
	/**
	 * @author Tao Pang 2006                                                                             
	 *
	 *(1) This Java program is part of the book, "An Introduction to        
	 *Computational Physics, 2nd Edition," written by Tao Pang and      
	 *published by Cambridge University Press on January 19, 2006.      
	 *
	 *(2) No warranties, express or implied, are made for this program.     
	 */
	private static double[] bessel(double x, int n, int nb) {
		int nmax = n+nb;
		double y[] = new double[n+1];
		double z[] = new double[nmax+1];

		// Generate the Bessel function of 1st kind J_n(x)
		z[nmax-1] = 1;
		double s = 0; 
		for (int i=nmax-1; i>0; --i) {
			z[i-1] = 2*i*z[i]/x-z[i+1];
			if (i%2 == 0) s += 2*z[i];
		}
		s += z[0];
		for (int i=0; i<=n; ++i) 
			y[i] = z[i]/s;
		return y;
	}
}
