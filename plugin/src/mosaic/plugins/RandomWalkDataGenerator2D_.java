package mosaic.plugins;


import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;

import java.util.Random;


public class RandomWalkDataGenerator2D_ implements PlugIn {
	int mSeed = 11;
	int mFrames = 600;
	int mWidth = 20;
	int mHeight = 20;
	float mBackground = 10f;
	float mSNR = 10; 
	float mIntensity = (float)Math.pow((-mSNR - Math.sqrt(mSNR * mSNR + 4*mBackground))/2, 2);
	float mSigmaPointSpread = 2f;
	float mSigmaOfMovement[] = new float[]{1f, 0.00001f};
	float[][] mPositions;
	ImageStack mImageStack;
	Random mRandomGenerator = new Random(mSeed);
	
    public void run(String arg) {    	
    	mImageStack = new ImageStack(mWidth, mHeight);
    	
        mPositions = new float[mFrames][2];
        //ImageProcessor ip = new FloatProcessor(mWidth, mHeight);
        //float[] vPixels = (float[])ip.getPixels();
        GeneratePositions();
        GenerateTheStack();
        ImagePlus vIP = new ImagePlus("Random Walk Movie",mImageStack);
        vIP.setDimensions(1, 1, mFrames);
        new StackWindow(vIP);
    }
    
    private void GenerateTheStack() {
    	for(int vFrame = 0; vFrame < mFrames; vFrame++) {
    		float[] vTempArray = new float[mHeight*mWidth];
    		AddPointSpread(vTempArray, mPositions[vFrame][0], mPositions[vFrame][1]);
    		AddNoise(vTempArray);
    		mImageStack.addSlice("frame " + vFrame+": " + "x = " + mPositions[vFrame][0]+ "y = " + mPositions[vFrame][1], 
    				vTempArray);    		
    	}
    }

    private void AddNoise(float[] aPixelArray){
    	
    }
    
    private void AddPointSpread(float[] aPixelArray, float aX, float aY){
    	for(int vY = 0; vY < mHeight; vY++){
    		for(int vX = 0; vX < mWidth; vX++){
    			aPixelArray[mWidth*vY+vX] += mBackground + (float) (mIntensity * Math.exp( 
    					-(Math.pow(vX - aX, 2) + Math.pow(vY - aY, 2)) / 4 * mSigmaPointSpread*mSigmaPointSpread));
    		}
    	}
    }

    float mTempVelocity = 0;
    private void GeneratePositions(){
    	/*
    	 * init position x0 and y0
    	 */
    	mPositions[0][0] = 10;//mRandomGenerator.nextFloat()*mWidth;
    	mPositions[0][1] = 10;//mRandomGenerator.nextFloat()*mHeight;
    	/*
    	 * for each frame, generate a direction and a step length
    	 */
    	for(int vFrame = 1; vFrame < mFrames; vFrame++){
//    		float vAngle = 0f + mRandomGenerator.nextFloat()*(float)Math.PI * mSigmaOfMovement[1];
//    		mTempVelocity += 2f + (float)mRandomGenerator.nextGaussian()*mSigmaOfMovement[0]; //acceleration
//    		float vStepLength = mTempVelocity;
    		
    		
    		mPositions[vFrame][0] = mPositions[vFrame-1][0] + (1/200f);//(float)Math.cos(vAngle)*vStepLength;
    		mPositions[vFrame][1] = mPositions[vFrame-1][1]; //+ (float)Math.sin(vAngle)*vStepLength;
    	}
    }

}