package ij.plugin;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.util.Random;

/**
 * - Convention for this plugin: x, y and z begin with 0.
 * @author Janigo
 *
 */
public class RandomWalkDataGenerator3D_ implements PlugInFilter {
	int mSeed = 1;
	int mNPoints = 1;
	int mNFrames = 200;
	int mNSlices = 30;
	int mWidth = 30;
	int mHeight = 30; 
	float mBackground = 50f;
	float mSNR = 10; 
	float mGain = 40;
	float mENF = 1.5f;
	float mIntensity = 0f;
	float mPxWidth = 160;
	float mPxDepth = 200f;
	float mSigmaPSF = 110f / mPxWidth;
	float mSigmaPSFz = 333f / mPxDepth;	
	float mSigmaDynamics = .2f;
	float[][][] mPositions;
	ImageStack mImageStack;
	Random mRandomGenerator = new Random(mSeed);
	boolean mLinearMovement = true;
	ImagePlus mPSF;
	
    public void run(ImageProcessor ip) {
    			
	}

	public int setup(String arg, ImagePlus aIMP) {
		if(!getUserDefinedParams())
    		return DONE;
    	
		mPSF = aIMP;
		normalizeFrameFloat(mPSF.getProcessor());
    	mImageStack = new ImageStack(mWidth, mHeight);
    	
        mPositions = new float[mNPoints][mNFrames][3];
        //ImageProcessor ip = new FloatProcessor(mWidth, mHeight);
        //float[] vPixels = (float[])ip.getPixels();
        
//        if(mLinearMovement) {
//        	GenerateLinearPositions();
//        }else {
//        	GeneratePositions();
//        }
        
        //calculateIntensity();
        calculateEMCCDIntensity();
        System.out.println("intensity - bg = " + (mIntensity-mBackground));
        
        GenerateUniformPositions(mSigmaDynamics);
        
        GenerateTheStack();
        ImagePlus vIP = new ImagePlus("3D Random Walk Movie",mImageStack);
        vIP.setDimensions(1, mNSlices, mNFrames);
        vIP.getCalibration().pixelWidth = mPxWidth;
        vIP.getCalibration().pixelHeight = mPxWidth;
        vIP.getCalibration().pixelDepth = mPxDepth;
        vIP.getCalibration().setUnit("nm");
        new StackWindow(vIP);
        PrintOutPositions();
        return DONE;
	}

//	public void run(String arg) {    	
//    	if(!getUserDefinedParams())
//    		return;
//    	
//    	mImageStack = new ImageStack(mWidth, mHeight);
//    	
//        mPositions = new float[mNPoints][mNFrames][3];
//        //ImageProcessor ip = new FloatProcessor(mWidth, mHeight);
//        //float[] vPixels = (float[])ip.getPixels();
//        
////        if(mLinearMovement) {
////        	GenerateLinearPositions();
////        }else {
////        	GeneratePositions();
////        }
//        calculateIntensity();
//        GenerateUniformPositions(mSigmaDynamics);
//        
//        GenerateTheStack();
//        ImagePlus vIP = new ImagePlus("Random Walk Movie",mImageStack);
//        vIP.setDimensions(1, mNSlices, mNFrames);
//        new StackWindow(vIP);
//        PrintOutPositions();
//    }

    private void GenerateTheStack() {
    	//DecimalFormat vDF = new DecimalFormat("#.00");
    	for(int vFrame = 0; vFrame < mNFrames; vFrame++) {
    		IJ.showProgress(vFrame, mNFrames);
    		float[][] vTempArray = new float[mNSlices][mHeight*mWidth];  
    		AddBackground(vTempArray);
    		for(int vPoint = 0; vPoint < mNPoints; vPoint++){
//    			addGaussBlob(vTempArray, mPositions[vPoint][vFrame][0], mPositions[vPoint][vFrame][1], mPositions[vPoint][vFrame][2]);
    			addPSF(vTempArray, mPositions[vPoint][vFrame][0], mPositions[vPoint][vFrame][1], mPositions[vPoint][vFrame][2]);
    			
    		}
    		amplifyWithGain(vTempArray);
    		AddNoise(vTempArray);
    		for(int vZ = 0; vZ < mNSlices; vZ++){
    			mImageStack.addSlice("frame " + vFrame/*+": " + 
    					"x = " + vDF.format(mPositions[vFrame][0]) + 
    					"y = " + vDF.format(mPositions[vFrame][1]) + 
    					"z = " + vDF.format(mPositions[vFrame][2])*/, 
    					vTempArray[vZ]);    		
    		}
    	}
    }
    
    private void amplifyWithGain(float[][] aPixelArray) {
    	float vFac = mGain*mENF;
    	for(int vY = 0; vY < aPixelArray.length; vY++) {
    		for(int vX = 0; vX < aPixelArray[0].length; vX++) {
    			aPixelArray[vY][vX] = aPixelArray[vY][vX]*mGain +(float) (mRandomGenerator.nextGaussian() * vFac * Math.sqrt(aPixelArray[vY][vX]));
    			if(aPixelArray[vY][vX] < 0) {
    				//TODO: not proper solution!
    				aPixelArray[vY][vX] = -aPixelArray[vY][vX];
    			}
    		}
    	}
    }

    private void AddNoise(float[][] aPixelArray){
    	
    	
    }
    
    /**
     * set the intensity member for poisson distributed noise.
     */
    private void calculateIntensity() {
    	mIntensity = (float)Math.pow((-mSNR - Math.sqrt(mSNR * mSNR + 4*mBackground))/2, 2);
    }
    
    /**
     * set the intensity member variable for EMCCD cams (poisson with excess noise factor)
     */
    private void calculateEMCCDIntensity() {
    	mIntensity = (float) (2f*mSNR*mSNR*mENF*mENF+2f*mSNR*mENF*Math.sqrt(mSNR*mSNR*mENF*mENF+4*mBackground)+4*mBackground)/4f;
    }

    private void addGaussBlob(float[][] aPixelArray, float aX, float aY, float aZ){    	
    	for(int vZ = 0; vZ < mNSlices; vZ++){    		
    		for(int vY = 0; vY < mHeight; vY++){
    			for(int vX = 0; vX < mWidth; vX++){
    				aPixelArray[vZ][mWidth*vY+vX] += (float) ((mIntensity-mBackground) * Math.exp( 
							-(Math.pow(vX - aX, 2) + Math.pow(vY - aY, 2)) / (2 * mSigmaPSF * mSigmaPSF))
							* Math.exp( -Math.pow(vZ - aZ, 2) / (2 * mSigmaPSFz * mSigmaPSFz)));
    			}
    		}
    	}
    }
    
    private void addPSF(float[][] aPixelArray, float aX, float aY, float aZ) {
    	//the origin in the PSF map is at (offset, 0)
    	int vZOffsetPSFCoord = mPSF.getWidth() / 2;
    	
    	//calculate the maximal distance of the influence of the PSF.
    	int vZMaxInPx = (int) ((mPSF.getWidth() / 2) * (mPSF.getCalibration().pixelWidth / mPxDepth));
    	int vRMaxInPx = (int) (mPSF.getHeight()      * (mPSF.getCalibration().pixelWidth / mPxWidth));
    	
    	//for all the pixel in the given influence region...
    	for(int vZ = -vZMaxInPx; vZ <= vZMaxInPx; vZ++) {
    		for(int vY = -vRMaxInPx; vY <= vRMaxInPx; vY++) {
    			for(int vX = -vRMaxInPx; vX <= vRMaxInPx; vX++) {
    				//check if we are in the image that is created:
    				if((int)aZ+vZ < 0 || (int)aZ+vZ > mNSlices ||
    						(int)aY+vY < 0 || (int)aY+vY > mHeight || 
    						(int)aX+vX < 0 || (int)aX+vX > mWidth) {
    					continue;
    				}
    				//calculate the distance of the true position aX to the voxel to be filled
    				float vDistX = ((float)Math.floor(aX) + vX + .5f) - aX; 
    				float vDistY = ((float)Math.floor(aY) + vY + .5f) - aY;
    				
    				float vDistRInPx = (float) Math.sqrt(vDistX*vDistX + vDistY*vDistY); 
    				float vDistZInPx = (float) (Math.floor(aZ) + vZ +.5f) - aZ;
    				
    				//check, if the distance is to the true position is not too large.
    				if(vDistRInPx > vRMaxInPx) {
    					continue;
    				}
    				
    				//convert the distances to the coordinates in the PSF map
    				int vPSFCoordinateR = (int) (vDistRInPx *(mPxWidth/mPSF.getCalibration().pixelHeight)+.5f);
    				int vPSFCoordinateZ = (int) (vZOffsetPSFCoord + vDistZInPx * (mPxDepth/mPSF.getCalibration().pixelWidth)+.5f);
    				
    				float vPSFValue = mPSF.getProcessor().getPixelValue(vPSFCoordinateZ,vPSFCoordinateR);    				
    				aPixelArray[(int)aZ+vZ][mWidth*((int)aY+vY)+((int)aX+vX)] += (mIntensity-mBackground)*vPSFValue;
        		}
    		}
    	}
    	
    }
    
	/**
	 * Normalizes a given <code>ImageProcessor</code> to [0,1].
	 * <br>According to the pre determend global min and max pixel value in the movie.
	 * <br>All pixel intensity values I are normalized as (I-gMin)/(gMax-gMin)
	 * @param ip ImageProcessor to be normalized
	 */
	private void normalizeFrameFloat(ImageProcessor ip) {
		
		float global_max = (float)ip.getMax();
		float global_min = (float)ip.getMin();
		
		float[] pixels=(float[])ip.getPixels();
		float tmp_pix_value;
		for (int i = 0; i < pixels.length; i++) {
			tmp_pix_value = (pixels[i]-global_min)/(global_max - global_min);
			pixels[i] = (float)(tmp_pix_value);
		}
	}
	
    private void AddBackground(float[][] aPixelArray){
    	for(int vZ = 0; vZ < mNSlices; vZ++){    		
    		for(int vY = 0; vY < mHeight; vY++){
    			for(int vX = 0; vX < mWidth; vX++){
    				aPixelArray[vZ][mWidth*vY+vX] += mBackground;
    			}
    		}
    	}
    }

    private void GenerateUniformPositions(float aScaler) {
    	for(int vPoint = 0; vPoint < mNPoints; vPoint++){
    		mPositions[vPoint][0][0] = (mWidth-1) * .5f;
    		mPositions[vPoint][0][1] = (mHeight-1) * .5f;
    		mPositions[vPoint][0][2] = (mNSlices-1) * .5f;

    		for(int vFrame = 1; vFrame < mNFrames; vFrame++){
    			mPositions[vPoint][vFrame][0] = mPositions[vPoint][vFrame-1][0] + ((float)mRandomGenerator.nextFloat()-.5f) * 2f * aScaler;
    			mPositions[vPoint][vFrame][1] = mPositions[vPoint][vFrame-1][1] + ((float)mRandomGenerator.nextFloat()-.5f) * 2f * aScaler;
    			mPositions[vPoint][vFrame][2] = mPositions[vPoint][vFrame-1][2] + ((float)mRandomGenerator.nextFloat()-.5f) * 2f * aScaler;
    		}
    	}
    }
    private void GenerateLinearPositions() {
    	for(int vPoint = 0; vPoint < mNPoints; vPoint++){
    		float velocityX = 5.0f; //pixel
    		float velocityY = 0.5f; //pixel
    		float velocityZ = 0.5f;	//slices
    		mPositions[vPoint][0][0] = (int) (0.1*(mWidth-1) + vPoint*(mWidth*.8f)*(1f/mNPoints));
    		mPositions[vPoint][0][1] = (mHeight-1) * .5f;
    		mPositions[vPoint][0][2] = (mNSlices-1) * .5f;

    		for(int vFrame = 1; vFrame < mNFrames; vFrame++){
    			velocityX = velocityX + (float)mRandomGenerator.nextGaussian() * 1.0f;
    			velocityY = velocityY + (float)mRandomGenerator.nextGaussian() * 0.2f;
    			velocityZ = velocityZ + (float)mRandomGenerator.nextGaussian() * 0.2f;
    			mPositions[vPoint][vFrame][0] = mPositions[vPoint][vFrame-1][0] + velocityX;
    			mPositions[vPoint][vFrame][1] = mPositions[vPoint][vFrame-1][1] + velocityY;
    			mPositions[vPoint][vFrame][2] = mPositions[vPoint][vFrame-1][2] + velocityZ;
    		}
    	}
    }
    private void GeneratePositions(){
    	/*
    	 * init position x0 and y0
    	 */
    	for(int vPoint = 0; vPoint < mNPoints; vPoint++){
//    		mPositions[vPoint][0][0] = mRandomGenerator.nextFloat()*mWidth;
//    		mPositions[vPoint][0][1] = mRandomGenerator.nextFloat()*mHeight;
//    		mPositions[vPoint][0][2] = mRandomGenerator.nextFloat()*mNSlices;
    		mPositions[vPoint][0][0] = (int) (0.1*(mWidth-1) + vPoint*(mWidth*.8f)*(1f/mNPoints));
    		mPositions[vPoint][0][1] = (mHeight-1) * .5f;
    		mPositions[vPoint][0][2] = (mNSlices-1) * .5f;

    		float vYScale = (float)mHeight / (float)mWidth;
    		float vZScale = (float)mNSlices / (float)mWidth;
    		/*
    		 * for each frame, generate a direction and a step length
    		 */
    		for(int vFrame = 1; vFrame < mNFrames; vFrame++){
    			float vAnglePolar = mRandomGenerator.nextFloat()*(float)Math.PI;
    			float vAngleAzimuth = mRandomGenerator.nextFloat()*(float)Math.PI;
    			float vStepLength = (float)mRandomGenerator.nextGaussian()*mSigmaDynamics*mSigmaDynamics;
    			float vCosAzimuth = (float)Math.cos(vAngleAzimuth);
    			float vSinAzimuth = (float)Math.sin(vAngleAzimuth);
    			float vSinPolar = (float)Math.sin(vAnglePolar);
    			float vCosPolar = (float)Math.cos(vAnglePolar);
    			mPositions[vPoint][vFrame][0] = mPositions[vPoint][vFrame-1][0] + vCosAzimuth * vSinPolar * vStepLength;
    			mPositions[vPoint][vFrame][1] = mPositions[vPoint][vFrame-1][1] + vYScale * vSinAzimuth * vSinPolar * vStepLength;
    			mPositions[vPoint][vFrame][2] = mPositions[vPoint][vFrame-1][2] + vZScale * vCosPolar * vStepLength;
    			if(mPositions[vPoint][vFrame][0] < 0)
    				mPositions[vPoint][vFrame][0] *= -1; //TODO: find a proper solution
    			if(mPositions[vPoint][vFrame][0] >= mWidth)
    				mPositions[vPoint][vFrame][0] = mWidth - mPositions[vPoint][vFrame][0];
    			
    			if(mPositions[vPoint][vFrame][1] < 0)
    				mPositions[vPoint][vFrame][1] *= -1; //TODO: find a proper solution
    			if(mPositions[vPoint][vFrame][1] >= mHeight)
    				mPositions[vPoint][vFrame][1] = mHeight - mPositions[vPoint][vFrame][1];
    			
    			if(mPositions[vPoint][vFrame][2] < 0)
    				mPositions[vPoint][vFrame][2] *= -1; //TODO: find a proper solution
    			if(mPositions[vPoint][vFrame][2] >= mNSlices)
    				mPositions[vPoint][vFrame][2] = mWidth - mPositions[vPoint][vFrame][2];
    			
    		}

    	}
    }
    
    public boolean getUserDefinedParams() {
    	GenericDialog gd = new GenericDialog("Enter param");
    	gd.addNumericField("SNR", mSNR, 1);
    	gd.showDialog();
    	if(gd.wasCanceled())
    		return false;
    	mSNR = (float)gd.getNextNumber();
    	return true;
    }
    
    private void PrintOutPositions() {
    	String vS = "";
    	for(int vP = 0; vP < mPositions.length; vP++) {
    		vS += "Target Point " + (vP+1) + "\n";
    		for(int vF = 0; vF < mPositions[vP].length; vF++) {
    			vS += (vF+1) + " " + (mPositions[vP][vF][0]) + " " + (mPositions[vP][vF][1]) + " "+ (mPositions[vP][vF][2]) + "\n"; 
    		}
    	}
    	new TextWindow("True positions", vS, 200, 600);
    }
}