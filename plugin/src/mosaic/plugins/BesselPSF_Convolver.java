package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
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
 *
 */
public class BesselPSF_Convolver implements PlugInFilter{

	double mXResolution = 1;
	double mYResolution = 1;
	double mRMax = .5e-6; // .5um
	double mNA = 1.2f;
	float[] mKernel;
	int mKernelWidth = 0;
	int mKernelHeight = 0;
	double mLambda  = 450*1e-9;
	//Green wavelength: 490-575 nm
//	double mLambdaMax = 575*1e-9;
//	double mLambdaMin = 400*1e-9;
	
	
	public int setup(String aArgs, ImagePlus aImp) {
		if(aImp == null) {
			IJ.showMessage("Please open an image.");
			return DONE;
		}
		if(!checkAndSetUnit(aImp)) {
			return DONE;
		}
		if(!showDialog()) {
			return DONE;
		}
		
		mKernel = generateBesselKernel();
		
		return IJ.setupDialog(aImp, DOES_8G + DOES_16 + DOES_32);
	}
	
	public void run(ImageProcessor aImageProcessor) {
		aImageProcessor.convolve(mKernel, mKernelWidth, mKernelHeight);
	}
	
	public float[] generateBesselKernel() {
		int vXRadius = (int)(mRMax / mXResolution) + 1; //in pixel
		int vYRadius = (int)(mRMax / mYResolution) + 1; //in pixel
		mKernelWidth = (vXRadius*2+1);
		mKernelHeight = (vYRadius*2+1);
		float[] vKernel = new float[mKernelWidth*mKernelHeight];
//		float[][] vPseudoKernel = new float[mKernelWidth][mKernelHeight];
		double vBesselMax = bessel_PSF(mXResolution/10, mLambda, mNA); //TODO: What to do(r=0) undefined
		for(int vYI = 0; vYI < mKernelHeight; vYI++) {
			for(int vXI = 0; vXI < mKernelWidth; vXI++) {
				float vDist = (float)Math.sqrt(
						(vXRadius-vXI)*mXResolution * (vXRadius-vXI)*mXResolution +
						(vYRadius-vYI)*mYResolution * (vYRadius-vYI)*mYResolution);
				vKernel[coords(vXI,vYI)] = (float)(bessel_PSF(vDist, mLambda, mNA) / vBesselMax);
//				vPseudoKernel[vXI][vYI] = (float)(bessel_PSF(vDist, mLambda, mNA) / vBesselMax);
			}
		}
		vKernel[coords(vXRadius,vYRadius)] = 1f;
//		vPseudoKernel[vXRadius][vYRadius] = 1f;
//		new ImagePlus("big kernel",new FloatProcessor(vPseudoKernel)).show();

		return vKernel;
	}
	
	public int getKernelWidth() {
		return mKernelWidth;
	}
	public int getKernelHeight() {
		return mKernelHeight;
	}
	/**
	 * Sets the necessary parameters to generate a kernel using <code>CreateBesselKernel</code>. 
	 * Use <code>GetKernelWidth</code> to get the width of the kernel (after generation!).
	 * @param aXResolution The pixel-width of the image(in m).
	 * @param aYResolution The pixel-height of the image(in m).
	 * @param aLambda The light wave length (in m).
	 * @param aNA Numerical aparture of the microscope
	 * @param vRMax The maximal distance which the kernel supports (for ex. 1e-6 = 1um).
	 */
	public void setForAPICall(double aXResolution, double aYResolution, double aLambda, double aNA, double aRMax) 
	{
		mXResolution = aXResolution;
		mYResolution = aYResolution;
		mLambda = aLambda;
		mNA = aNA;
		mRMax = aRMax;
	}
	
	private int coords(int aX, int aY) {
		return aY * mKernelWidth + aX;
	}
	
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
		if(vUnit.equalsIgnoreCase(IJ.micronSymbol + "m")) {
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
	/**
	 * @author © Tao Pang 2006                                                                             
	 *
	 *(1) This Java program is part of the book, "An Introduction to        
	 *Computational Physics, 2nd Edition," written by Tao Pang and      
	 *published by Cambridge University Press on January 19, 2006.      
	 *
	 *(2) No warranties, express or implied, are made for this program.     
	 */
	public static double[] bessel(double x, int n, int nb) {
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
	
	public double bessel_PSF(double aRadius, double aLambda, double aApparture) {
		double vA = 2 * Math.PI * aApparture / aLambda;
		double vR = 2 * bessel(aRadius * vA, 1, (int)(aRadius * vA + 10))[1] / aRadius;
		return vR * vR;
	}
	
	public boolean showDialog() {
		GenericDialog vGD = new GenericDialog("Bessel PSF parameter");
		vGD.addNumericField("wavelength", mLambda*1e9, 0, 0, "nm");
		vGD.addNumericField("Numeric Aparture: ", mNA, 0);
		vGD.addNumericField("Max Radius", mRMax*1e9, 0, 0, "nm");
		vGD.showDialog();
		if (vGD.wasCanceled()) 
			return false;
		mLambda = vGD.getNextNumber() * 1e-9;
		mNA = vGD.getNextNumber();
		mRMax = vGD.getNextNumber() * 1e-9;
		return true;
	}	
}
