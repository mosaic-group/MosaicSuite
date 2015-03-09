package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

import java.util.Random;

/**
 * - Convention for this plugin: x, y and z begin with 0.
 * @author Janigo
 *
 */
public class MTDataGenerator_3D implements PlugIn {
	int mSeed = 1;
	int mNFrames = 20;
	int mNSlices = 25;
	int mWidth = 40;
	int mHeight = 40; 
	int mPxWidthInNm = 160; //nm
	int mPxDepthInNm = 200; // nm
	float mBackground = 10f;
	float mSNR = 10; 
	float mIntensity = 0f;
	float mSigmaPointSpreadXY = 219f; //nm
	float mSigmaPointSpreadZ = 400f;  //nm
	float mSigma = 2f;
	float[][] mPositions;
	ImageStack mImageStack;
	Random mRandomGenerator = new Random(mSeed);
	boolean mLinearMovement = true;
	
	//The intensities should not be changed too much since in extrem cases/long movies, the SNR is completely changed. Nevertheless the small
	//changes should simulate small fluctuations such that there is no bias generated due to numerical issues.
	float[] mSigmaOfDynamics = new float[]{mPxWidthInNm/10f, mPxWidthInNm/10f, mPxDepthInNm/10f, 
			Math.max(mPxWidthInNm, mPxDepthInNm)/10f, (float)Math.asin(1f/600f), (float)Math.asin(1f/600f), 
			Math.max(mPxWidthInNm, mPxDepthInNm)/10f, (float)Math.asin(1f/1200f), (float)Math.asin(1f/1200f), .01f, .01f, .01f};
	
    public void run(String arg) {    	
    	if(!getUserDefinedParams())
    		return;
    	mImageStack = new ImageStack(mWidth, mHeight);
    	
        mPositions = new float[mNFrames][12];
        //ImageProcessor ip = new FloatProcessor(mWidth, mHeight);
        //float[] vPixels = (float[])ip.getPixels();
        
//        if(mLinearMovement) {
//        	GenerateLinearPositions();
//        }else {
//        	GeneratePositions();
//        }
        calculateIntensity();
        GeneratePositions();
        GenerateTheStack();
        ImagePlus vIP = new ImagePlus("Random Walk Movie",mImageStack);
        vIP.setDimensions(1, mNSlices, mNFrames);
        vIP.getCalibration().pixelWidth = mPxWidthInNm;
        vIP.getCalibration().pixelHeight = mPxWidthInNm;
        vIP.getCalibration().pixelDepth = mPxDepthInNm;
        vIP.getCalibration().setUnit("nm");
        new StackWindow(vIP);
        PrintOutPositions();
    }

    private void GenerateTheStack() {
    	//DecimalFormat vDF = new DecimalFormat("#.00");
    	for(int vFrame = 0; vFrame < mNFrames; vFrame++) {
    		IJ.showProgress(vFrame, mNFrames);
    		float[][] vTempArray = new float[mNSlices][mHeight*mWidth];  
    		AddBackground(vTempArray);
    		Point3D[] vState = getPointsFromState(mPositions[vFrame]);
    		for(int vP = 0; vP < 3; vP++ ) {
    			addGaussPSF(vTempArray, vState[vP].mX, vState[vP].mY, vState[vP].mZ, 
    					mPositions[vFrame][9+vP], mWidth, mHeight, mNSlices, mPxWidthInNm, mPxDepthInNm);
    		}
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

    private void calculateIntensity() {
    	mIntensity = (float)Math.pow((-mSNR - Math.sqrt(mSNR * mSNR + 4*mBackground))/2, 2);
    }
    
    private void AddNoise(float[][] aPixelArray){

    }

    
    private void addGaussPSF(float[][] aImage, float aX, float aY, float aZ, float aIntensity, int aW, int aH, int aS, float aPxWidthInNm, float aPxDepthInNm) {
		float vVarianceXYinPx = mSigmaPointSpreadXY * mSigmaPointSpreadXY / (aPxWidthInNm * aPxWidthInNm);
		float vVarianceZinPx = mSigmaPointSpreadZ * mSigmaPointSpreadZ / (aPxDepthInNm * aPxDepthInNm);
		float vMaxDistancexy = 4*mSigmaPointSpreadXY / aPxWidthInNm;
		float vMaxDistancez = 4*mSigmaPointSpreadZ / aPxDepthInNm; //in pixel!
		
		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
		if(aX / aPxWidthInNm + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(aX / aPxWidthInNm + .5f) - (int)(vMaxDistancexy + .5f);
		if(aY / aPxWidthInNm + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(aY / aPxWidthInNm + .5f) - (int)(vMaxDistancexy + .5f);
		if(aZ / aPxDepthInNm + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(aZ / aPxDepthInNm + .5f) - (int)(vMaxDistancez + .5f);
		if(aX / aPxWidthInNm + .5f + (vMaxDistancexy + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(aX / aPxWidthInNm + .5f) + (int)(vMaxDistancexy + .5f);
		if(aY / aPxWidthInNm + .5f + (vMaxDistancexy + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(aY / aPxWidthInNm + .5f) + (int)(vMaxDistancexy + .5f);
		if(aZ / aPxDepthInNm + .5f + (vMaxDistancez + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(aZ / aPxDepthInNm + .5f) + (int)(vMaxDistancez + .5f);

		for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
			for(int vY = vYStart; vY <= vYEnd; vY++){
				for(int vX = vXStart; vX <= vXEnd; vX++){
					aImage[vZ][mWidth*vY+vX] += (float) (aIntensity * Math.pow(Math.E, 
							-(Math.pow(vX - aX / aPxWidthInNm, 2) + Math.pow(vY - aY / aPxWidthInNm, 2)) / (2 * vVarianceXYinPx))
							* Math.pow(Math.E, -Math.pow(vZ - aZ / aPxDepthInNm, 2) / 2 * vVarianceZinPx));
				}
			}		
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

    private void GeneratePositions() {
    	mPositions[0] = new float[]{(mWidth-1) * (.3f) * mPxWidthInNm,(mHeight-1) * (.5f) * mPxWidthInNm ,(mNSlices-1) * (.5f) * mPxDepthInNm,
    			800f,1.5f,.1f,
    			1600f,0.1f,.3f,
    			mIntensity,mIntensity,mIntensity};


    	for(int vFrame = 1; vFrame < mNFrames; vFrame++){
    		for(int vE = 0; vE < mPositions[0].length; vE++){
    			if(mRandomGenerator.nextBoolean())
    				mPositions[vFrame][vE] = mPositions[vFrame-1][vE] + (float)mRandomGenerator.nextFloat() * mSigmaOfDynamics[vE];
    			else
    				mPositions[vFrame][vE] = mPositions[vFrame-1][vE] - (float)mRandomGenerator.nextFloat() * mSigmaOfDynamics[vE];
    		}
    	}
    }

    /**
     * 
	 * @param aState
	 * @return position 0:SPB1(new), 1:SPB2(old), 2:Tip. Coordinates are in nanometers.
	 */
	public Point3D[] getPointsFromState(float[] aState) {
		Point3D[] vRes = new Point3D[3];
		Point3D vSPB1 = new Point3D();
		vSPB1.setMX(aState[0] - (aState[3]/2f) * (float)Math.sin(aState[4]) *(float)Math.cos(aState[5]));
		vSPB1.setMY(aState[1] - (aState[3]/2f) * (float)Math.sin(aState[4]) *(float)Math.sin(aState[5]));
		vSPB1.setMZ(aState[2] - (aState[3]/2f) * (float)Math.cos(aState[4]));
		Point3D vSPB2 = new Point3D();
		vSPB2.setMX(aState[0] + (aState[3]/2f) * (float)Math.sin(aState[4]) *(float)Math.cos(aState[5]));
		vSPB2.setMY(aState[1] + (aState[3]/2f) * (float)Math.sin(aState[4]) *(float)Math.sin(aState[5]));
		vSPB2.setMZ(aState[2] + (aState[3]/2f) * (float)Math.cos(aState[4]));
		Point3D vTip = new Point3D();
		vTip.setMX(vSPB2.mX + aState[6] * (float)Math.sin(aState[4] + aState[7]) * (float)Math.cos(aState[5]+aState[8]));
		vTip.setMY(vSPB2.mY + aState[6] * (float)Math.sin(aState[4] + aState[7]) * (float)Math.sin(aState[5]+aState[8]));
		vTip.setMZ(vSPB2.mZ + aState[6] * (float)Math.cos(aState[4] + aState[7]));
		vRes[0] = vSPB1;
		vRes[1] = vSPB2;
		vRes[2] = vTip;
		return vRes;
	}
	
    private void PrintOutPositions() {
    	String vS = "";
    	for(int vF = 0; vF < mPositions.length; vF++) {
    		vS += (vF+1);
    		for(int vV = 0; vV < mPositions[vF].length; vV++) {
    			vS += "\t" + (mPositions[vF][vV]);
    		}
    		vS += "\n";
    	}
    	new TextWindow("True positions", "frame\tx\ty\tz\tL\talpha\tbeta\tD\talpha\tbeta\tI_spb1\tI_spb2\tI_tip", vS, 200, 600);
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