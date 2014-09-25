package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.awt.geom.Line2D;
import java.util.Random;

public class Generate_MT_Data_2D implements PlugIn{
	
	int mNbFrames = 200;
	int mBackGround = 10; //fixed: adapt intensities.
	int mSeed = 8882;
	float mTruePos[][] = new float[mNbFrames][8];
	float[] mStdDevs = new float[]{1f, 1f, 1f, .1f, 2f, 0.1f, 0.0f, 0.0f}; //intensities should not adapt to hold SNR
	Random mRandomGenerator = new Random(mSeed);
	int mImageWidth = 60;
	int mImageHeight = 60;
	double mPixelWidth = .064 * 1e-6;
	double mPixelHeight = .064 * 1e-6;
	String mUnit = "m";
	double mLambda = 450 * 1e-9;
	double mNA = 1.2;
	double mRMax = 0.38 * 1e-6;
	
	int mS = 10; //Scaling for the accuracy while generating data and convolving them
	
	private Roi mRoi;
	
	public void run(String aArgs) {
		//the intensities get smaller (normalized kernel!!!-->overall intenisiy remains the same) -->using binning it should be ok again!
		generateTruePositions(new float[]{30,30,8,0,10,-.5f,4f,7f * 19f});
		//64nm: I = 4,7*19 --> 2.65 SNR
		//64nm: I = 9,15*19 --> 4.5 SNR
		//64nm: I = 24,24*19 --> 7 SNR
		//64nm: I = 25,25*19 --> 7.9 SNR
		//64nm: I = 35f,40f * 26 --> 10 SNR
		ImageStack vStack = generateImages(mTruePos);
		ImagePlus vIP = new ImagePlus("Artificial MT Data", vStack);
		
		vIP.getCalibration().pixelWidth = mPixelWidth;
		vIP.getCalibration().pixelHeight = mPixelHeight;
		vIP.getCalibration().setUnit(mUnit);
		vIP.setDimensions(1, 1, mNbFrames);
		
		double vPeak = vIP.getStack().getProcessor(1).getMax();
		double vSNR = (vPeak - mBackGround)/Math.sqrt(vPeak);
		vIP.setTitle("movie_" + mSeed + "_SNR_" + Double.toString(vSNR));
		
		vIP.show();
		
		//show true positions
		new TextWindow("True positions of MT Data movie","x\ty\tL1\talpha\tD\tbeta\tI1\tI2", GenerateTruePositionsString(), 200,400);
	}
		
	
	private void generateTruePositions(float[] aInitPosition) {
		for(int vV = 0; vV < 8; vV++)
			mTruePos[0][vV] = aInitPosition[vV];

		for(int vF = 1; vF < mNbFrames; vF++) {
			mTruePos[vF][0] = 30 + (float)mRandomGenerator.nextGaussian() * mStdDevs[0];
			mTruePos[vF][1] = 30 + (float)mRandomGenerator.nextGaussian() * mStdDevs[1];
			for(int vV = 2; vV < 8; vV++) {
				mTruePos[vF][vV] = mTruePos[vF-1][vV] + (float)mRandomGenerator.nextGaussian() * mStdDevs[vV];
			}
			
			if(mTruePos[vF][2] < 2) {
				mTruePos[vF][2] = 4 + (4-mTruePos[vF][2]);
			}
			if(mTruePos[vF][2] > 8) {
				mTruePos[vF][2] = 8 - (mTruePos[vF][2] - 8);
			}
			if(mTruePos[vF][4] < 4) {
				mTruePos[vF][4] = 4 + (4-mTruePos[vF][4]);
			}
			if(mTruePos[vF][4] > 20) {
				mTruePos[vF][4] = 20 - (mTruePos[vF][4]-20);
			}
			if(mTruePos[vF][5] < -Math.PI/2) {
				mTruePos[vF][5] = (float)(-Math.PI/2 + (-Math.PI/2 - mTruePos[vF][5]));
			}
			if(mTruePos[vF][5] > Math.PI/2) {
				mTruePos[vF][5] = (float)(Math.PI/2 - (mTruePos[vF][5] - Math.PI/2));
			}
			mTruePos[vF][3] = (float)(mTruePos[vF][3] % (Math.PI*2));
			mTruePos[vF][5] = (float)(mTruePos[vF][5] % (Math.PI*2));
			
			
		}
	}
	
	private ImageStack generateImages(float[][] mTruePositions) {
		ImageStack vImageStack = new ImageStack(mImageWidth, mImageHeight);
		for(int vF = 0; vF < mTruePositions.length; vF++) {
			IJ.showStatus("Movie_" + mSeed + ": " + "gen frame " +vF+"/"+mNbFrames);
			FloatProcessor vFP = new FloatProcessor(mImageWidth*mS, mImageHeight*mS);
						
			drawImage(mTruePositions[vF], vFP);
			
//			new ImagePlus("bef convolution "+vF,vFP).show();
//			long vOldTime = System.currentTimeMillis();
			convolveImageWithPSF(vFP);
//			FFT fft = new FFT();			
//			fft.run("fft");
//			System.out.println("convolution takes ms: " + (System.currentTimeMillis() - vOldTime));
//			new ImagePlus("after convolution "+vF,vFP).show();
			
			FloatProcessor vDownScaledFP = downScaleImage(vFP);
			addBackground(mBackGround, vDownScaledFP);
			
			vImageStack.addSlice("", vDownScaledFP);
		}
		return vImageStack;
	}
	
	private FloatProcessor downScaleImage(FloatProcessor aFP){
//		FloatProcessor vRes = new FloatProcessor(aFP.getWidth()/mS, aFP.getHeight()/mS);
//		return vRes;
		
//		aFP.setInterpolate(false);
//		return (FloatProcessor)aFP.resize(aFP.getWidth()/mS, aFP.getHeight()/mS);
		
		return downScaleUsingBinning(aFP, mS);
	}
	
	private void convolveImageWithPSF(ImageProcessor aIP){
		BesselPSF_Convolver vSBP = new BesselPSF_Convolver();
		vSBP.setForAPICall(mPixelHeight/mS, mPixelWidth/mS, mLambda, mNA, mRMax);
		float[] vBesselKernel = vSBP.generateBesselKernel();
				
//		float[][] vPseudoKernel = new float[vSBP.getKernelHeight()][vSBP.getKernelWidth()];
//		for(int vY = 0; vY < vSBP.getKernelHeight();vY++) {
//			for(int vX = 0; vX < vSBP.getKernelWidth(); vX++) {
//				vPseudoKernel[vY][vX] = vBesselKernel[vX + vSBP.getKernelWidth() * vY];
//			}
//		}
//		new ImagePlus("bessel visualized", new FloatProcessor(vPseudoKernel)).show();
		
		aIP.setRoi(
				mRoi.getBoundingRect().x - vSBP.getKernelWidth(), 
				mRoi.getBoundingRect().y - vSBP.getKernelHeight(), 
				mRoi.getBoundingRect().width + 2*vSBP.getKernelWidth(), 
				mRoi.getBoundingRect().height + 2*vSBP.getKernelWidth());
		
		aIP.convolve(vBesselKernel, vSBP.getKernelWidth(), vSBP.getKernelHeight());
	}
	
	private void drawImage(float[] mTruePositions, ImageProcessor aIP) {
		
		
		float vPointX = mTruePositions[0] + (mTruePositions[2]/2f) * (float)Math.cos(mTruePositions[3]) + mTruePositions[4] * (float)Math.cos(mTruePositions[3] + mTruePositions[5]);
		float vPointY = mTruePositions[1] + (mTruePositions[2]/2f) * (float)Math.sin(mTruePositions[3]) + mTruePositions[4] * (float)Math.sin(mTruePositions[3] + mTruePositions[5]);
		
		Line2D.Float vLine = new Line2D.Float(
				mTruePositions[0] - (mTruePositions[2]/2f)*(float)Math.cos(mTruePositions[3]), 
				mTruePositions[1] - (mTruePositions[2]/2f)*(float)Math.sin(mTruePositions[3]), 
				mTruePositions[0] + (mTruePositions[2]/2f)*(float)Math.cos(mTruePositions[3]),
				mTruePositions[1] + (mTruePositions[2]/2f)*(float)Math.sin(mTruePositions[3]));
//		Line2D.Float vRestIntensityLine = new Line2D.Float(
//				mTruePositions[0] + (mTruePositions[2]/2f)*(float)Math.cos(mTruePositions[3]),
//				mTruePositions[1] + (mTruePositions[2]/2f)*(float)Math.sin(mTruePositions[3]),
//				vPointX,
//				vPointY
//		);
		
		aIP.setValue(mTruePositions[6]);
		aIP.drawLine((int)(mS*vLine.x1+.5), (int)(mS*vLine.y1+.5), (int)(mS*vLine.x2+.5), (int)(mS*vLine.y2+.5));
		aIP.setValue(mTruePositions[7]);
		aIP.drawDot((int)(mS*vPointX), (int)(mS*vPointY));
		
		double vxMin = Math.min(Math.min(vPointX, vLine.x1), vLine.x2);
		double vxMax = Math.max(Math.max(vPointX, vLine.x1), vLine.x2);
		double vyMin = Math.min(Math.min(vPointY, vLine.y1), vLine.y2);
		double vyMax = Math.max(Math.max(vPointY, vLine.y1), vLine.y2);
		mRoi = new Roi((int)(mS*vxMin+.5), (int)(mS*vyMin+.5), (int)(mS*(vxMax-vxMin)+.5), (int)(mS*(vyMax-vyMin)+.5));
		
		
	}
	
	private void addBackground(int aBG, ImageProcessor aIP) {
		//add the background
		aIP.setRoi(0, 0, aIP.getWidth(), aIP.getHeight());
		aIP.add(aBG);
//		aIP.setValue(aBG);
//		aIP.fill();
	}
	private FloatProcessor downScaleUsingBinning(FloatProcessor aIP, int aS) {
		FloatProcessor vDownScaledIP = new FloatProcessor(aIP.getWidth()/aS, aIP.getHeight()/aS);
		float[] vDownScaledPixels = (float[])vDownScaledIP.getPixels();
		float[] vPixels = (float[])aIP.getPixels();
		for(int vX = 0; vX < aIP.getWidth(); vX++) {
			for(int vY = 0; vY < aIP.getHeight(); vY++) {
				vDownScaledPixels[vX/aS + vDownScaledIP.getWidth()*(vY/aS)] += vPixels[vX+aIP.getWidth()*vY];
			}
		}
		return vDownScaledIP;
	}
	
	private String GenerateTruePositionsString() {
		String vS = "";
		for(int vF = 0; vF < mTruePos.length; vF++) {
			for(float vV:mTruePos[vF]) {
				vS += vV + "\t";
			}
			vS += "\n";
		}
		return vS;
	}
}
