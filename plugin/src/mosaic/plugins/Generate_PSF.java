package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.io.File;

public class Generate_PSF implements  PlugInFilter{
	
	float mSigmaPSFxy = 25;
	float mSigmaPSFz = 50;
	int mSigmaPxSizeInNm = 2;
	int mSigmaFocalPlaneDistInNm = 20;
	float mIntensity = 100;
	
	ImagePlus mPSF = null;
	boolean mUsePSFMap = false;
	boolean mHideResult = false;
	int mWidth;
	int mHeight;
	int mNSlices;
	String mPSFDirectory;
	ImageStack vGaussIS;
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		
		if (mUsePSFMap == true)
			mPSFDirectory = IJ.getDirectory("Enter the path to PSF.tif");

		mUsePSFMap = usePSFMap();
		mWidth = (int)Math.ceil(8.0f * mSigmaPSFxy / mSigmaPxSizeInNm);
		mHeight = (int)Math.ceil(8.0f * mSigmaPSFxy / mSigmaPxSizeInNm);
		mNSlices = (int)Math.ceil(8.0f * mSigmaPSFz / mSigmaFocalPlaneDistInNm);
		
		
		float[][][] vPSFmapImage = new float[mNSlices][mHeight][mWidth];
		float[][][] vGaussBlobImage = new float[mNSlices][mHeight][mWidth];
		
		Point3D vMu = new Point3D(mWidth/2 * mSigmaPxSizeInNm, 
				mHeight/2 * mSigmaPxSizeInNm, 
				mNSlices/2 * mSigmaFocalPlaneDistInNm);
//		Point3D vMu = new Point3D(100, 200, 167);
				
		if(mUsePSFMap) {
			
			addPSF(vPSFmapImage, vMu, mIntensity, mSigmaPxSizeInNm, mSigmaFocalPlaneDistInNm);
		} 
		
		addGaussBlob(vGaussBlobImage, vMu, mIntensity, mSigmaPxSizeInNm, mSigmaFocalPlaneDistInNm);
		
		ImageStack vPSFmapIS = convert3DArrayToImageStack(vPSFmapImage);
		
		if (mHideResult == false)
			new ImagePlus("Sampled from PSF lookup table", vPSFmapIS).show();
		
		vGaussIS = convert3DArrayToImageStack(vGaussBlobImage);
		
		if (mHideResult == false)
			new ImagePlus("Sampled from gaussian pdf", vGaussIS).show();
		
		return DONE;
	}
	
	public void hideResult(boolean SH)
	{
		mHideResult = SH;
	}
	
	private void processGUI(GenericDialog gd)
	{
		mSigmaPSFxy = (float)gd.getNextNumber();
		mSigmaPSFz = (float)gd.getNextNumber();
		mSigmaPxSizeInNm = (int)gd.getNextNumber();
		mSigmaFocalPlaneDistInNm = (int)gd.getNextNumber();
	}
	
	public void setParametersGUI()
	{
		GenericDialog gd = new GenericDialog("Generate PSF");
		
		// Numeric Fields
		
		gd.addNumericField("SigmaXY (Nm)", mSigmaPSFxy, 4);
		gd.addNumericField("SigmaZ (Nm)", mSigmaPSFz, 4);
		gd.addNumericField("Sigma Pixel size (Nm)", mSigmaPxSizeInNm , 0);
		gd.addNumericField("Sigma Focal Plane Distance (Nm)", mSigmaFocalPlaneDistInNm , 0);
		
		gd.hideCancelButton();
		
		gd.showDialog();
		
		// Dialog destroyed
		// On OK, read parameters
		
		if (gd.wasOKed())
			processGUI(gd);
	}
	
	public ImageStack getGauss2DPsf()
	{
		int nSlices = vGaussIS.getSize();
		
		return convert1DArrayToImageStack((float [])vGaussIS.getPixels(nSlices/2),vGaussIS.getWidth(), vGaussIS.getHeight());
	}
	
	public ImageStack getGauss3DPsf()
	{
		return vGaussIS;
	}
	
	
	@Override
	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub
		
	}

	protected boolean usePSFMap() {
		
		File vPSFFile = new File(mPSFDirectory, "PSF.tif");
		if(vPSFFile.exists()){
//			System.out.println("file exists");
			try{
				mPSF = IJ.openImage(mPSFDirectory.concat("/PSF.tif"));
				// shift scale the image to the [0,1]- range
				ShiftScaleIPTo0To1(mPSF.getProcessor());
				return true;
			} catch(Exception e){
//				System.out.println("exception raised:" + e.getMessage());
				return false;				
			}
		}
		return false;
	}
	
	public void ShiftScaleIPTo0To1(ImageProcessor aIP) {
		aIP.add(aIP.getMin());
		aIP.resetMinAndMax();
		aIP.multiply(1.0 / aIP.getMax());
		aIP.resetMinAndMax();
	}
	
	protected void addFeaturePointTo3DImage(float[][][] aImage, Point3D aPoint, float aIntensity, float aPxWidthInNm, float aPxDepthInNm) {
		if(mUsePSFMap) {
			addPSF(aImage, aPoint, aIntensity, aPxWidthInNm, aPxDepthInNm);
		} else {
			addGaussBlob(aImage, aPoint, aIntensity, aPxWidthInNm, aPxDepthInNm);
		}
	}
	
	protected void addGaussBlob(float[][][] aImage, Point3D aPoint, float aIntensity, float aPxWidthInNm, float aPxDepthInNm) {
		float vVarianceXYinPx = mSigmaPSFxy * mSigmaPSFxy / (aPxWidthInNm * aPxWidthInNm);
		float vVarianceZinPx = mSigmaPSFz * mSigmaPSFz / (aPxDepthInNm * aPxDepthInNm);
		float vMaxDistancexy = 4*mSigmaPSFxy / aPxWidthInNm;
		float vMaxDistancez = 4*mSigmaPSFz / aPxDepthInNm; //in pixel!

		Point3D vPointInPx = new Point3D(aPoint.mX / aPxWidthInNm, aPoint.mY / aPxWidthInNm, aPoint.mZ / aPxDepthInNm);
		
		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;
		if(vPointInPx.mX + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(vPointInPx.mX + .5f) - (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mY + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(vPointInPx.mY + .5f) - (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mZ + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(vPointInPx.mZ + .5f) - (int)(vMaxDistancez + .5f);
		if(vPointInPx.mX + .5f + (vMaxDistancexy + .5f) >= mWidth) vXEnd = mWidth - 1; else vXEnd = (int)(vPointInPx.mX + .5f) + (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mY + .5f + (vMaxDistancexy + .5f) >= mHeight) vYEnd = mHeight - 1; else vYEnd = (int)(vPointInPx.mY + .5f) + (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mZ + .5f + (vMaxDistancez + .5f)  >= mNSlices) vZEnd = mNSlices - 1; else vZEnd = (int)(vPointInPx.mZ + .5f) + (int)(vMaxDistancez + .5f);

		for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
			for(int vY = vYStart; vY <= vYEnd; vY++){
				for(int vX = vXStart; vX <= vXEnd; vX++){
					aImage[vZ][vY][vX] += (float) (aIntensity  
							* Math.exp(-(Math.pow(vX - vPointInPx.mX, 2) + Math.pow(vY - vPointInPx.mY, 2)) / (2 * vVarianceXYinPx))
							* Math.exp(-Math.pow(vZ - vPointInPx.mZ, 2) / (2 * vVarianceZinPx)));
				}
			}
		}	
	}
	
	protected void addPSF(float[][][] aPixelArray, Point3D aPoint, float aIntensity, float aPxWidthInNm, float aPxDepthInNm) {
    	//the origin in the PSF map is at (offset, 0)
    	int vZOffsetPSFCoord = mPSF.getWidth() / 2;
    	
    	//calculate the maximal distance of the influence of the PSF.
    	float vPSFPixelWidth = (float) mPSF.getCalibration().pixelWidth;
    	float vPSFPixelHeight = (float) mPSF.getCalibration().pixelHeight;
    	int vZMaxInPx = (int) Math.ceil((mPSF.getWidth() / 2.0) * (vPSFPixelWidth / aPxDepthInNm));
    	int vRMaxInPx = (int) Math.ceil((mPSF.getHeight()       * (vPSFPixelHeight / aPxWidthInNm)));
    	
    	float vPX = aPoint.mX/aPxWidthInNm;
    	float vPY = aPoint.mY/aPxWidthInNm;
    	float vPZ = aPoint.mZ/aPxDepthInNm;
    	
    	//for speedup, get the pixelarray:
    	float[][] vPSFPixelArray = mPSF.getProcessor().getFloatArray();
    	int vRDim = mPSF.getHeight();
    	int vZDim = mPSF.getWidth();
    	//for all the pixel in the given influence region...
    	for(int vZ = -vZMaxInPx; vZ <= vZMaxInPx; vZ++) {
    		// calculate the distance (in z) of the px center to the point center in pixel
    		float vDistZInPx = (float) (Math.floor(vPZ) + vZ + .25f) - vPZ;
//    		float vDistZInPx = (float) (Math.floor(vPZ) + vZ) - vPZ;
    		//check, if the distance to the true position is not too large.	
    		if(vDistZInPx < -vZMaxInPx || vDistZInPx > vZMaxInPx) {
    			continue;
    		}
    		for(int vY = -vRMaxInPx; vY <= vRMaxInPx; vY++) {
    			for(int vX = -vRMaxInPx; vX <= vRMaxInPx; vX++) {
    				//check if we are in the image that is created:
    				if((int)vPZ+vZ < 0 || (int)vPZ+vZ >= mNSlices ||
    						(int)vPY+vY < 0 || (int)vPY+vY >= mHeight || 
    						(int)vPX+vX < 0 || (int)vPX+vX >= mWidth) {
    					continue;
    				}
 
    				//calculate the distance of the true position aX to the voxel to be filled
//    				float vDistXInPx = ((float)Math.floor(vPX) + vX - .5f) - vPX; 
//    				float vDistYInPx = ((float)Math.floor(vPY) + vY - .5f) - vPY;
    				float vDistXInPx = ((float)Math.floor(vPX) + vX ) - vPX; 
    				float vDistYInPx = ((float)Math.floor(vPY) + vY ) - vPY;
    				
    	    		// calculate the distance (in r) of the px center to the point center in pixel

    				float vDistRInPx = (float) Math.sqrt(vDistXInPx*vDistXInPx + vDistYInPx*vDistYInPx) + 1.5f; 
    				
    				//check, if the distance to the point position is not too large.
    				if(vDistRInPx > vRMaxInPx || vDistRInPx < 0) {
    					continue;
    				}
    				 
    				//convert the distances to the coordinates in the PSF map (the point we wish to sample)
    				float vPSFCoordinateR =  vDistRInPx * (aPxWidthInNm/vPSFPixelHeight);
    				
    				float vPSFCoordinateZ =  vZOffsetPSFCoord + vDistZInPx * (aPxDepthInNm/vPSFPixelWidth);
    				
    				//
    				// Bilinear interpolation: PSFmap values between 4 pixel with center next to point we wish to sample
    				//
    				// calc the vector from the pixel center to the point to sample:
    				float vCenterOfCenterPixelR = (float) (Math.floor(vPSFCoordinateR) + 0.5f);
    				float vCenterOfCenterPixelZ = (float) (Math.floor(vPSFCoordinateZ) + 0.5f);
    				float vSignedDist_Center_P_R = (vPSFCoordinateR - vCenterOfCenterPixelR);
    				float vSignedDist_Center_P_Z = (vPSFCoordinateZ - vCenterOfCenterPixelZ);
    				
    				// check in what quadrant of the pixel we are to figure out which 4 pixel to look at:
    				int vRoff = 1;
    				int vZoff = 1; 
    				if(vSignedDist_Center_P_R < 0) vRoff = -1;
    				if(vSignedDist_Center_P_Z < 0) vZoff = -1;
    				
    				// get the 4 PSF values from the lookup table (we apply Neumann boundary conditions)
    				float vPSFValue_CC = 0; // psf value of the center pixel: (c_r, c_z)
    				float vPSFValue_CR = 0; // psf value of the pixel: (c_r + vRoff, c_z)    				
    				float vPSFValue_CZ = 0; // psf value of the pixel: (c_r, c_z + vZoff)
    				float vPSFValue_RZ = 0; // psf value of the pixel: (c_r + vRoff, c_z + vZoff)
    				
    				if((int)vCenterOfCenterPixelZ < vZDim && (int)vCenterOfCenterPixelZ >= 0 &&  
    						(int)vCenterOfCenterPixelR < vRDim && (int)vCenterOfCenterPixelR >= 0) {
    					vPSFValue_CC = vPSFPixelArray[(int)vCenterOfCenterPixelZ][(int)vCenterOfCenterPixelR]; 
    				}
    				if((int)vCenterOfCenterPixelZ < vZDim && (int)vCenterOfCenterPixelZ >= 0 && 
    						(int)vCenterOfCenterPixelR + vRoff < vRDim && (int)vCenterOfCenterPixelR + vRoff >= 0) {
    					vPSFValue_CR = vPSFPixelArray[(int)vCenterOfCenterPixelZ][(int)vCenterOfCenterPixelR + vRoff]; 
    				}
    				if((int)vCenterOfCenterPixelZ + vZoff < vZDim && (int)vCenterOfCenterPixelZ + vZoff >= 0 && 
    						(int)vCenterOfCenterPixelR < vRDim && (int)vCenterOfCenterPixelR >= 0) {
    					vPSFValue_CZ = vPSFPixelArray[(int)vCenterOfCenterPixelZ + vZoff][(int)vCenterOfCenterPixelR]; 
    				}
    				if((int)vCenterOfCenterPixelZ + vZoff < vZDim && (int)vCenterOfCenterPixelZ + vZoff >= 0 && 
    						(int)vCenterOfCenterPixelR + vRoff < vRDim && (int)vCenterOfCenterPixelR + vRoff >= 0) {
    					vPSFValue_RZ = vPSFPixelArray[(int)vCenterOfCenterPixelZ + vZoff][(int)vCenterOfCenterPixelR + vRoff]; 
    				}

    				// boundary conditions:
    				if(vCenterOfCenterPixelR + vRoff < 0) {
    					vPSFValue_CR = vPSFValue_CC;
    					vPSFValue_RZ = vPSFValue_CZ;
    				}
    				
    				
    				// interpolate in R direction to get 2 PSFValues_interpR
    				float vAbsDist_Center_P_Z = Math.abs(vSignedDist_Center_P_Z);
    				float vAbsDist_Center_P_R = Math.abs(vSignedDist_Center_P_R);
    				float vPSFValue_interp1 = vAbsDist_Center_P_Z * vPSFValue_CZ + (1 - vAbsDist_Center_P_Z) * vPSFValue_CC;
    				float vPSFValue_interp2 = vAbsDist_Center_P_Z * vPSFValue_RZ + (1 - vAbsDist_Center_P_Z) * vPSFValue_CR;
    					
    				float vPSFValue = vAbsDist_Center_P_R * vPSFValue_interp2 + (1-vAbsDist_Center_P_R) * vPSFValue_interp1;
//    				if(Float.isNaN(vPSFValue)) {
//    					System.out.println("stop it");
//    				}
    				aPixelArray[(int)vPZ+vZ][(int)vPY+vY][(int)vPX+vX] += (aIntensity)*vPSFValue;
//    				aPixelArray[(int)vPZ+vZ][(int)vPY+vY][(int)vPX+vX] += (aIntensity)*vPSFValue_CC;
        		}
    		}
    	}
    	
    }
	
	public static ImageStack convert3DArrayToImageStack(float[][][] aArray) {
		int vNx = aArray[0][0].length;
		int vNy = aArray[0].length;
		int vNz = aArray.length;
		ImageStack vIS = new ImageStack(vNy, vNx);
		for(int vZ = 0;vZ < vNz; vZ++) {
			vIS.addSlice("", new FloatProcessor(aArray[vZ]));			
		}
		return vIS;
	}
	
	public static ImageStack convert1DArrayToImageStack(float[] aArray, int Nx, int Ny) {
		ImageStack vIS = new ImageStack(Nx, Ny);
		vIS.addSlice("", new FloatProcessor(Nx,Ny,aArray));			
		return vIS;
	}
	
	public class Point3D{
		public float mX,mY,mZ;
		public Point3D () {
			this(0f, 0f, 0f);
		}
		public Point3D (float aX, float aY, float aZ){
			mX = aX;
			mY = aY;
			mZ = aZ;
		}
		public Point3D clone() {
			return new Point3D(mX, mY, mZ);
		}
		public Point3D add(Point3D aB) {
			mX += aB.mX;
			mY += aB.mY;
			mZ += aB.mZ;
			return this;
		}
		/**
		 * this = this - aB
		 * @param aB
		 */
		public Point3D subtract(Point3D aB) {
			mX -= aB.mX;
			mY -= aB.mY;
			mZ -= aB.mZ;
			return this;
		}
		public float scalarProduct(Point3D aB) {
			return mX * aB.mX + mY * aB.mY + mZ * aB.mZ;
		}
		public Point3D cross(Point3D aB) {
			mX = mY * aB.mZ - mZ * aB.mY;
			mY = mZ * aB.mX - mX * aB.mZ;
			mZ = mX * aB.mY - mY * aB.mX;
			return this;
		}
		public float getLength() {
			return (float)Math.sqrt(scalarProduct(this));
		}
		public Point3D normalize(){
			float vL = getLength();
			mX /= vL;
			mY /= vL;
			mZ /= vL;
			return this;
		}
		public float getMX() {
			return mX;
		}
		public void setMX(float mx) {
			mX = mx;
		}
		public float getMY() {
			return mY;
		}
		public void setMY(float my) {
			mY = my;
		}
		public float getMZ() {
			return mZ;
		}
		public void setMZ(float mz) {
			mZ = mz;
		}
		
	}
}
