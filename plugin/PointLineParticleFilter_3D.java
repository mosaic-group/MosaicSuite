package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Vector;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.*;

/**
 * @author Janick Cardinale, 2007, ETHZ
 *
 */

public class PointLineParticleFilter_3D extends PFTracking3D{
	/*
	 * Parameters
	 */
	public boolean mLSLikelihood = false;
	public int mNbParticles = 200;	
	public int mRepSteps = 5;

	public float[] mSigmaOfDynamics = new float[]{
			50f,
			50f,
			50f,
			30f,
			0.15f,
			0.15f,
			150,
			0.15f,
			0.15f,
			1,
			1,
			1};
	protected String[] mDimensionsDescription = new String[]{"x[nm]","y[nm]","z[nm]","L[nm]",
			"alpha_polar[rad]","alpha_azimuth[rad]", 
			"D[nm]", "beta_polar[rad]", "beta_azimuth[rad]", "Intensity_newSPB", "Intensity_oldSPB2", "Intensity_Tip"};
	public float mRestIntensity = 0f;

	/*
	 * Init parameters
	 */
	protected static enum STATE_OF_INIT {INIT_STEP1, INIT_STEP2, INIT_STEP3};
	protected STATE_OF_INIT mStateOfInit = STATE_OF_INIT.INIT_STEP1;
	public float mGaussBlurRadius = mSigmaPSFxy; //in etwa gleich PSF
	public int mMaxBins = 256;//not used 
	public int mInitParticleFilterIterations = 10;


	public boolean mDoPrintInitStates = false;

	/**
	 * Completes the state vectors stored in <code>mStateVectorsMemory</code> with the restored z-coordinate.
	 * Dimensions of variable: Frames -> Objects -> z-coordinates[of line start, of line end(nearer to tip), of tip]
	 */
	public Vector<Vector<float[]>> mZCoordinates = new Vector<Vector<float[]>>();
	/**
	 * A scalar to quantify the track for each frame
	 */
	public Vector<Float> mQualityMeasure = new Vector<Float>();

	public void run(ImageProcessor ip) 
	{
		CalculateLikelihoodMonitorProcessor(new FloatProcessor(1,1));
		super.run(ip);		
	}

	@Override
	protected boolean testForAbort(FeatureObject aFO, int aFrameIndex){
		//if microtubule-length is too short, then abort.
		if(aFO.mStateVectorsMemory[aFrameIndex-1][6] < 400){
			return true;
		}
		return false;
	}
	
	protected boolean autoInitFilter(ImageStack aImageStack)
	{	
		return false;
	}
	
	@Override
	protected void drawFromProposalDistribution(float[] aParticle, float aPxWidthInNm, float aPxDepthInNm) 
	{
		//
		// apriori: set sigma(L) to maximum L/12 //such that it does not happen that t
		//
//		float vSigmaL = mSigmaOfRandomWalkA[3] / aPxWidthInNm;
//		if(aParticle[3]/6f < vSigmaL) {
//			vSigmaL = aParticle[3]/6f;
//		}
		aParticle[0] = aParticle[0] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[0];
		aParticle[1] = aParticle[1] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[1];
		aParticle[2] = aParticle[2] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[2];
		aParticle[3] = aParticle[3] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[3];
//		aParticle[3] = aParticle[3] + Math.abs((float)mRandomGenerator.nextGaussian()) * mSigmaOfRandomWalk[3];
//		aParticle[3] = aParticle[3] + (float)mRandomGenerator.nextGaussian() * vSigmaL;
		aParticle[4] = aParticle[4] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[4];
		aParticle[5] = aParticle[5] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[5];
		aParticle[6] = aParticle[6] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[6];
		aParticle[7] = aParticle[7] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[7];
		aParticle[8] = aParticle[8] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[8];
		aParticle[9] = aParticle[9] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[9];
		aParticle[10] = aParticle[10] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[10];
		aParticle[11] = aParticle[11] + (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamics[11];

		// TODO: Check for all the particle boundaries (image boundaries for the coordinates)
		//
		// check the intensity for a negative value
		//
		if(aParticle[9] < 1) aParticle[9] = 1;
		if(aParticle[10] < 1) aParticle[10] = 1; 
		if(aParticle[11] < 1) aParticle[11] =  1; 
		//
		// check if the orientation(sign) of D or L change
		//
		if(aParticle[3] < 0) aParticle[3] *= -1;
		if(aParticle[6] < 0) aParticle[6] *= -1;
		//
		// take the angles modulo 2pi
		//
		float v2Pi = (float)Math.PI * 2f;
		aParticle[4] = (aParticle[4] + v2Pi) % v2Pi;
		aParticle[5] = (aParticle[5] + v2Pi) % v2Pi;
		aParticle[7] = (aParticle[7] + v2Pi) % v2Pi;
		aParticle[8] = (aParticle[8] + v2Pi) % v2Pi;
		
	}

	/**
	 * 
	 * @param aSPB1: the new spindle pole in pixel
	 * @param aSPB2: the old spindle pol in pixel
	 * @param aTip: coordinates of the microtubules tip in pixel
	 * @param aSpindleIntensity: the intensity of the spindle
	 * @param aTipIntensity: the intensity of the tip
	 */
	private float[] getStateVectorFromPixelPoints(Point3D aSPB1, Point3D aSPB2, Point3D aTip, int aSPB1Intensity, int aSPB2Intensity, int aTipIntensity) 
	{
		//
		// Read out the intensities
		//
//		float vPointIntensity = aImageProcessor.getf((int)Math.round(aSPB1x), (int)Math.round(aSPB1y));
//		float vLineIntensity = (aImageProcessor.getf((int)Math.round(aSPB1x), (int)Math.round(aSPB1y)) +
//		aImageProcessor.getf((int)Math.round(aSPB1x), (int)Math.round(aSPB1y))) /2f;
		//
		// Set up the state vector with straight forward calculations
		//
		Point3D vSPB1inNm = new Point3D(aSPB1.mX * getPixelWidthInNm(), aSPB1.mY * getPixelWidthInNm(), aSPB1.mZ * getPixelDepthInNm());
		Point3D vSPB2inNm = new Point3D(aSPB2.mX * getPixelWidthInNm(), aSPB2.mY * getPixelWidthInNm(), aSPB2.mZ * getPixelDepthInNm());
		Point3D vTipinNm = new Point3D(aTip.mX * getPixelWidthInNm(), aTip.mY * getPixelWidthInNm(), aTip.mZ * getPixelDepthInNm());
		float vLineLength = (float)Math.sqrt((vSPB1inNm.mX -vSPB2inNm.mX)*(vSPB1inNm.mX -vSPB2inNm.mX) 
				+ (vSPB1inNm.mY - vSPB2inNm.mY)*(vSPB1inNm.mY - vSPB2inNm.mY)
				+ (vSPB1inNm.mZ - vSPB2inNm.mZ)*(vSPB1inNm.mZ - vSPB2inNm.mZ));
		float vLineLengthXY = (float)Math.sqrt((vSPB1inNm.mX -vSPB2inNm.mX)*(vSPB1inNm.mX -vSPB2inNm.mX) 
				+ (vSPB1inNm.mY - vSPB2inNm.mY)*(vSPB1inNm.mY - vSPB2inNm.mY));
//		float vDistToL1 = (float)Math.sqrt((vTipinNm.mX - vSPB1inNm.mX) * (vTipinNm.mX - vSPB1inNm.mX) + (vTipinNm.mY - vSPB1inNm.mY) * (vTipinNm.mY - vSPB1inNm.mY));
		float vDistSPB2TipXY = (float)Math.sqrt((vTipinNm.mX - vSPB2inNm.mX) * (vTipinNm.mX - vSPB2inNm.mX) + (vTipinNm.mY - vSPB2inNm.mY) * (vTipinNm.mY - vSPB2inNm.mY));
		float vDistSPB2Tip = (float)Math.sqrt((vTipinNm.mX - vSPB2inNm.mX) * (vTipinNm.mX - vSPB2inNm.mX) + (vTipinNm.mY - vSPB2inNm.mY) * (vTipinNm.mY - vSPB2inNm.mY)
				+ (vTipinNm.mZ - vSPB2inNm.mZ) * (vTipinNm.mZ - vSPB2inNm.mZ));
		//
		//	calculate azimuth angles(in the xy plane) 
		//
		float vAlphaAzimuth = (float)Math.acos((vSPB2inNm.mX - vSPB1inNm.mX)/vLineLengthXY);
		if(vSPB2inNm.mY - vSPB1inNm.mY < 0) // check the quadrant
			vAlphaAzimuth = 2f * (float)Math.PI - vAlphaAzimuth;			
		float vBetaAzimuth = (float)Math.acos((vTipinNm.mX - vSPB2inNm.mX)/vDistSPB2TipXY);
		if(vTipinNm.mY - vSPB2inNm.mY < 0)
			vBetaAzimuth = 2f * (float)Math.PI - vBetaAzimuth;
//		vBetaAzimuth -= vAlphaAzimuth;
		//
		// calculate the polar angles
		//
		float vAlphaPolar = (float)Math.acos((vSPB2inNm.mZ - vSPB1inNm.mZ) / vLineLength);
		float vBetaPolar = (float)Math.acos((vTipinNm.mZ - vSPB2inNm.mZ)/vDistSPB2Tip);
		//
		// set up the state vector
		//
		return new float[]{
				vSPB1inNm.mX + vLineLength/2f * (float)Math.sin(vAlphaPolar) * (float)Math.cos(vAlphaAzimuth), 
				vSPB1inNm.mY + vLineLength/2f * (float)Math.sin(vAlphaPolar) * (float)Math.sin(vAlphaAzimuth), 
				vSPB1inNm.mZ + vLineLength/2f * (float)Math.cos(vAlphaPolar),
				vLineLength, vAlphaPolar, vAlphaAzimuth, vDistSPB2Tip, vBetaPolar - vAlphaPolar, vBetaAzimuth - vAlphaAzimuth, 
				aSPB1Intensity - mBackground, aSPB2Intensity - mBackground, aTipIntensity - mBackground};
	}

	private Vector<Vector<Integer>> SearchAreasInBitmap(boolean[] aBitmap) 
	{
		Vector<Vector<Integer>> vAreas = new Vector<Vector<Integer>>();
		boolean[] vAlreadyVisited = new boolean[aBitmap.length];
		for(int vP = 0; vP < aBitmap.length; vP++){
			if(aBitmap[vP] && !vAlreadyVisited[vP]){
				vAreas.add(SearchArea(aBitmap, vAlreadyVisited, vP));
			}
		}
		return vAreas;
	}

	private Vector<Integer> SearchArea(boolean[] aBitmap, boolean[] aAlreadyVisitedMask, int aPixel) 
	{
		if(!aBitmap[aPixel] || aAlreadyVisitedMask[aPixel])
			return new Vector<Integer>();

		Vector<Integer> vArea = new Vector<Integer>();
		vArea.add(aPixel);
		aAlreadyVisitedMask[aPixel] = true;
		if(aPixel % mWidth != mWidth - 1 && aBitmap[aPixel + 1] && !aAlreadyVisitedMask[aPixel + 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + 1));
		}
		if(aPixel % mWidth != mWidth -1 && aPixel > mWidth && aBitmap[aPixel - mWidth + 1] && ! aAlreadyVisitedMask[aPixel - mWidth + 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - mWidth + 1));
		}
		if(aPixel > mWidth && aBitmap[aPixel - mWidth] && !aAlreadyVisitedMask[aPixel - mWidth]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - mWidth));
		}
		if(aPixel % mWidth != 0 && aPixel > mWidth && aBitmap[aPixel - mWidth - 1] && ! aAlreadyVisitedMask[aPixel - mWidth - 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - mWidth - 1));
		}
		if(aPixel % mWidth != 0 && aBitmap[aPixel - 1] && !aAlreadyVisitedMask[aPixel - 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - 1));
		}
		if(aPixel % mWidth != 0 && aPixel < (mHeight-1)*mWidth && aBitmap[aPixel + mWidth - 1] && !aAlreadyVisitedMask[aPixel + mWidth - 1]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + mWidth - 1));
		}
		if(aPixel < (mHeight-1)*mWidth && aBitmap[aPixel + mWidth] && !aAlreadyVisitedMask[aPixel + mWidth]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + mWidth));
		}
		if(aPixel % mWidth != mWidth -1 && aPixel < (mHeight-1)*mWidth && aBitmap[aPixel + mWidth + 1] && !aAlreadyVisitedMask[aPixel + mWidth + 1]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + mWidth + 1));
		}
		return vArea;
	}

	private Vector<Integer> SearchBoundary(boolean[] aBitmap, Vector<Integer> aArea) {
		Vector<Integer> vB = new Vector<Integer>();
		for(int vP : aArea){
			//
			// Get the neighbours, first handle the boundaries of the image
			//
			//corners
			if(vP == 0 || vP == mWidth-1 || vP == (mHeight-1)*mWidth || vP == (mHeight-1)*mWidth + mWidth - 1) {
				vB.add(vP);
			}
			//right
			if(vP % mWidth == mWidth - 1) {
				if(!aBitmap[vP-mWidth] || !aBitmap[vP+mWidth]){
					vB.add(vP);
				}
				continue;
			}
			//top
			if(vP < mWidth) {
				if(!aBitmap[vP-1] || !aBitmap[vP+1]){
					vB.add(vP);
				}
				continue;
			}
			//left
			if(vP % mWidth == 0) {
				if(!aBitmap[vP-mWidth] || !aBitmap[vP+mWidth]){
					vB.add(vP);
				}
				continue;
			}
			//bottom
			if(vP > (mHeight - 1) * mWidth) {
				if(!aBitmap[vP-1] || !aBitmap[vP+1]){
					vB.add(vP);
				}
				continue;
			}
			//not boundary
			int vNeighbourCode = 0;
			if(!aBitmap[vP + 1]) vNeighbourCode += 1;
			if(!aBitmap[vP + 1 - mWidth]) vNeighbourCode += 2;
			if(!aBitmap[vP - mWidth]) vNeighbourCode += 4;
			if(!aBitmap[vP - 1 - mWidth]) vNeighbourCode += 8;
			if(!aBitmap[vP - 1]) vNeighbourCode += 16;
			if(!aBitmap[vP - 1 + mWidth]) vNeighbourCode += 32;
			if(!aBitmap[vP + mWidth]) vNeighbourCode += 64;
			if(!aBitmap[vP + mWidth + 1]) vNeighbourCode += 128;

			if((vNeighbourCode & 15) == 15  ||
					(vNeighbourCode & 30) == 30  ||
					(vNeighbourCode & 60) == 60  ||
					(vNeighbourCode & 120) == 120  ||
					(vNeighbourCode & 240) == 240  ||
					(vNeighbourCode & 225) == 225  ||
					(vNeighbourCode & 195) == 195  ||
					(vNeighbourCode & 135) == 135  ) {
				vB.add(vP);
			}

		}
		return vB;
	}

	private void QualityMeasure(Vector<Vector<float[]>> aParticles, int aFrameIndex) 
	{
		for(Vector<float[]> vFeaturePointParticles : aParticles){ //should only be one here
			int vDimOfState = aParticles.elementAt(0).elementAt(0).length - 1;
			float vNeff = 0;
			for(float[] vParticle : vFeaturePointParticles){
				vNeff += vParticle[vDimOfState] * vParticle[vDimOfState];
			}
			vNeff = 1/vNeff;
			mQualityMeasure.setElementAt(vNeff, aFrameIndex-1);
		}
	}

	private void CalculateLikelihoodMonitorProcessor(ImageProcessor aObservationImage)
	{
//		IJ.showStatus("Calculating likelihood image for monitoring");

//		PrintWriter vPW = null;
//		try{
//		BufferedWriter vBW = new BufferedWriter(new FileWriter("c:\\likelihoodsWrong.txt"));
//		vPW = new PrintWriter(vBW);

//		}catch (IOException aIOE){
//		IJ.error("unable to write to file");
//		}


//		if(!mLSLikelihood){
//		boolean[][] vBitmap = new boolean[mHeight][mWidth];
//		for(int vX = 0; vX < mWidth; vX++)
//		for(int vY = 0; vY < mHeight; vY++)
//		vBitmap[vY][vX] = true;

////		float[] vPixels = (float[])mLikelihoodMonitorProcessor.getPixels();
//		String vS = "";
//		int vXCounter = 0; 
//		int vYCounter = 0;
////		float[][] vLogLikelihoods = new float[200][200];

//		float vX_tip = 32.56f;
//		float vY_tip = 29.55f;

//		for(float vL = 6f ; vL < 10f; vL = vL + 4f/100f){	
//		vPW.write("\n");

//		float vSPB2x = 19.91f + vL/2f * (float)Math.cos(0.56);
//		float vSPB2y = 30.70f + vL/2f * (float)Math.sin(0.56);

//		float vD = (float)Math.sqrt((vX_tip-vSPB2x)*(vX_tip-vSPB2x) + (vY_tip-vSPB2y)*(vY_tip-vSPB2y)); 
//		float vBeta = (float)Math.asin((vY_tip - 30.7f - vL/2f*Math.sin(0.46))/vD)-0.46f;

//		for(float vI = 20f ; vI < 100f; vI = vI + 80f/100f){
//		float[][] vIdealImage = generateIdealImage(aObservationImage.getWidth(), 
//		aObservationImage.getHeight(),
//		new float[]{19.91f, 30.7f, vL, 0.46f, vD, vBeta, vI, 25.3f},
//		10);	
//		vS = vL + "," + vI + "," + vD + "," + vBeta + "," + calculateLogLikelihood(aObservationImage,vIdealImage,vBitmap);//vIdealImage[vY][vX];
//		vS += "\n";
//		vPW.write(vS);
////		vLogLikelihoods[vYCounter][vXCounter] = calculateLogLikelihood(aObservationImage,vIdealImage,vBitmap);
//		vXCounter++;
//		}
//		}
//		vYCounter++;
//		vXCounter = 0;


//		vPW.close();
//		}
//		else {
//		float[][] vLSLikelihoods = new float[200][200];
//		int vXCounter = 0;
//		int vYCounter = 0;
//		for(float vY = 19.5f; vY < 21.5; vY = vY + 0.01f){
////		IJ.showProgress((vY-19.5f)/2f);
//		for(float vX = 5f ; vX < 7f; vX = vX + .01f){

//		float[][] vIdealImage = generateIdealImage(aObservationImage.getWidth(), 
//		aObservationImage.getHeight(),
//		new float[]{vX, vY, 9.03f, 5.43f, 7.72f, -0.49f, 43.68f, 41.51f},//optimal values: 6.22	20.63	9.03	5.43	7.72	-0.49	43.68	41.51
//		1);	
////		vS += vX + " " + calculateLogLikelihood(aObservationImage,vIdealImage,vBitmap);//vIdealImage[vY][vX];
////		vS += "\n";
//		vLSLikelihoods[vYCounter][vXCounter] = CalculateLSLikelihood(aObservationImage,vIdealImage);
//		vXCounter++;
//		}
//		vYCounter++;
//		vXCounter = 0;
//		}
//		new ImageWindow(new ImagePlus("xy LeastSquares Likelihoods",new FloatProcessor(vLSLikelihoods)));
//		}
	}	
	
	
//	/**
//	 * Overrides the method in the base class.
//	 */
//	protected boolean[][][] GenerateParticlesIntensityBitmap_3D(Vector<float[]> aSetOfParticles, int aW, int aH, int aS) {
//		if(mIntensityBitmap == null) {
//			mIntensityBitmap = new boolean[aS][aH][aW];
//		}
//		for(int vZ = 0; vZ < aS;vZ++) {
//			for(int vY = 0; vY < aH; vY++) {
//				for(int vX = 0; vX < aW; vX ++) {
//					mIntensityBitmap[vZ][vY][vX] = false;
//				}
//			}
//		}
//		float vMaxDistancexy = 3*mSigmaPSFxy/getPixelWidthInNm();
//		float vMaxDistancez = 3*mSigmaPSFz/getPixelDepthInNm(); //in pixel!
//		for(float[] vP : aSetOfParticles) {
//			Point3D[] vPoints = getPointsFromState(vP);
//			addFeaturePointToBitmap(mIntensityBitmap, vPoints[0], vMaxDistancexy, vMaxDistancez, aW, aH, aS);
//			addFeaturePointToBitmap(mIntensityBitmap, vPoints[1], vMaxDistancexy, vMaxDistancez, aW, aH, aS);
//			addFeaturePointToBitmap(mIntensityBitmap, vPoints[2], vMaxDistancexy, vMaxDistancez, aW, aH, aS);
//		}
//		return mIntensityBitmap;
//	}

	/**
	 * Implementation of the abstract base class. 
	 * @return an Image with a gaussian blob as an intensity at position of the particle
	 */
	boolean mTestFirst = false;
	protected float[][][] generateIdealImage_3D(int aW, int aH, int aS, float[] aParticle, int aBackground, float aPxWidthInNm, float aPxDepthInNm)
	{
		float vIdealImage[][][] = new float[aS][aH][aW];
		//float vBackground = mBackground;
		for(int vZ = 0; vZ < aS; vZ++) {
			for(int vY = 0; vY < aH; vY++){
				for(int vX = 0; vX < aW; vX++){
					vIdealImage[vZ][vY][vX] = aBackground;		//TODO:have a look on performance!	
				}
			}
		}
//		float vVarianceXYinPx = mSigmaPSFxy * mSigmaPSFxy / (getPixelWidthInNm() * getPixelWidthInNm());
//		float vVarianceZinPx = mSigmaPSFz * mSigmaPSFz / (getPixelDepthInNm() * getPixelDepthInNm());

		Point3D[] vPoints = getPointsFromState(aParticle);
		Point3D vTip = vPoints[2];
		Point3D vSPB2 = vPoints[1];
		Point3D vSPB1 = vPoints[0];

		//
		//	Add the point
		//
		//calculate the boundaries for the gaussian blob to increase performance
		//int mLikelihoodRadius = 5;//5 IS OK, larger doesn't make sense ->result is 0.0
//		float vMaxDistancexy = 3*mSigmaPSFxy/getPixelWidthInNm();
//		float vMaxDistancez = 3*mSigmaPSFz/getPixelDepthInNm(); //in pixel!

//		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
		addFeaturePointTo3DImage(vIdealImage, vTip, aParticle[11], aW, aH, aS, aPxWidthInNm, aPxDepthInNm);
		addFeaturePointTo3DImage(vIdealImage, vSPB1, aParticle[9], aW, aH, aS, aPxWidthInNm, aPxDepthInNm);
		addFeaturePointTo3DImage(vIdealImage, vSPB2, aParticle[10], aW, aH, aS, aPxWidthInNm, aPxDepthInNm);

//		//
//		// Add the line
//		//
//		Line3D vSpindle = new Line3D(vSPB1, vSPB2);
//		Line3D vSpindleBB = vSpindle.getBoundingBox();
//		if(vSpindleBB.getMA().mX + .5f - vMaxDistancexy < 0) vXStart = 0; else vXStart = (int)(vSpindleBB.getMA().mX + .5f) - (int)vMaxDistancexy;
//		if(vSpindleBB.getMA().mY + .5f - vMaxDistancexy < 0) vYStart = 0; else vYStart = (int)(vSpindleBB.getMA().mY + .5f) - (int)vMaxDistancexy;
//		if(vSpindleBB.getMA().mZ + .5f - vMaxDistancez  < 0) vZStart = 0; else vZStart = (int)(vSpindleBB.getMA().mZ + .5f) - (int)vMaxDistancez;
//		if(vSpindleBB.getMB().mX + .5f + vMaxDistancexy >= aW) vXEnd = aW - 1; else vXEnd = (int)(vSpindleBB.getMB().mX + .5f) + (int)vMaxDistancexy;
//		if(vSpindleBB.getMB().mY + .5f + vMaxDistancexy >= aH) vYEnd = aH - 1; else vYEnd = (int)(vSpindleBB.getMB().mY + .5f) + (int)vMaxDistancexy;
//		if(vSpindleBB.getMB().mZ + .5f + vMaxDistancez  >= aS) vZEnd = aS - 1; else vZEnd = (int)(vSpindleBB.getMB().mZ + .5f) + (int)vMaxDistancez;
//
//
//		for(int vZ = vZStart; vZ < vZEnd; vZ++){
//			for(int vY = vYStart; vY < vYEnd; vY++){
//				for(int vX = vXStart; vX < vXEnd; vX++){					
//					Point2D.Float vPoint = new Point2D.Float(vX + .5f, vY + .5f);
//					Line2D.Float vLine = new Line2D.Float(vSpindle.getMA().mX, vSpindle.getMB().mX, 
//							vSpindle.getMA().mY, vSpindle.getMB().mY);
//					float vDistXY, vDistZ;
//					if((vDistXY = (float)vLine.ptSegDist(vPoint)) < vMaxDistancexy && 
//							(vDistZ = vSpindle.getDistanceToLine(new Point3D(vX + .5f, vY + .5f, vZ +.5f))) < vMaxDistancez){
//						vIdealImage[vZ][vY][vX] +=  (float) (aParticle[9] * Math.pow(Math.E, 
//								- (vDistXY * vDistXY / (2 * vVarianceXYinPx) 
//										+ Math.pow(vDistZ, 2)/ 2 * vVarianceZinPx)));
//					}
//					if(mDoMonitorIdealImage && mTestFirst){
//						tempArray[vZ][vY*mWidth+vX] += vIdealImage[vZ][vY][vX];
//					}
//				}
//			}	
//		}
		if(mTestFirst){
			mTestFirst = false;
			ImageStack vTempStack = new ImageStack(aH,aW);
			for(int vS = 0; vS < aS; vS++) {
				vTempStack.addSlice(""+vS, new FloatProcessor(vIdealImage[vS]));
			}
			new StackWindow(new ImagePlus("Single ideal image",vTempStack));
		}
		return vIdealImage;
	}

	private void addFeaturePointTo3DImage(float[][][] aImage, Point3D aPoint, float aIntensity, int aW, int aH, int aS, float aPxWidthInNm, float aPxDepthInNm) {
		float vVarianceXYinPx = mSigmaPSFxy * mSigmaPSFxy / (aPxWidthInNm * aPxWidthInNm);
		float vVarianceZinPx = mSigmaPSFz * mSigmaPSFz / (aPxDepthInNm * aPxDepthInNm);
		float vMaxDistancexy = 3*mSigmaPSFxy / aPxWidthInNm;
		float vMaxDistancez = 3*mSigmaPSFz / aPxDepthInNm; //in pixel!
		
		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
		if(aPoint.mX / aPxWidthInNm + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(aPoint.mX / aPxWidthInNm + .5f) - (int)(vMaxDistancexy + .5f);
		if(aPoint.mY / aPxWidthInNm + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(aPoint.mY / aPxWidthInNm + .5f) - (int)(vMaxDistancexy + .5f);
		if(aPoint.mZ / aPxDepthInNm + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(aPoint.mZ / aPxDepthInNm + .5f) - (int)(vMaxDistancez + .5f);
		if(aPoint.mX / aPxWidthInNm + .5f + (vMaxDistancexy + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(aPoint.mX / aPxWidthInNm + .5f) + (int)(vMaxDistancexy + .5f);
		if(aPoint.mY / aPxWidthInNm + .5f + (vMaxDistancexy + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(aPoint.mY / aPxWidthInNm + .5f) + (int)(vMaxDistancexy + .5f);
		if(aPoint.mZ / aPxDepthInNm + .5f + (vMaxDistancez + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(aPoint.mZ / aPxDepthInNm + .5f) + (int)(vMaxDistancez + .5f);

		for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
			for(int vY = vYStart; vY <= vYEnd; vY++){
				for(int vX = vXStart; vX <= vXEnd; vX++){
					aImage[vZ][vY][vX] += (float) (aIntensity * Math.pow(Math.E, 
							-(Math.pow(vX - aPoint.mX / aPxWidthInNm, 2) + Math.pow(vY - aPoint.mY / aPxWidthInNm, 2)) / (2 * vVarianceXYinPx))
							* Math.pow(Math.E, -Math.pow(vZ - aPoint.mZ / aPxDepthInNm, 2) / 2 * vVarianceZinPx));
				}
			}		
		}
	}
	
	private void addFeaturePointToBitmap(boolean[][][] aBitmap, Point3D aPoint, float aMaxDistancexyInPx, float aMaxDistancezInPx, int aW, int aH, int aS, float aPxWidthInNm, float aPxDepthInNm)
	{
		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
		if(aPoint.mX / aPxWidthInNm + .5f - (aMaxDistancexyInPx + .5f) < 0) vXStart = 0; else vXStart = (int)(aPoint.mX / aPxWidthInNm+ .5f) - (int)(aMaxDistancexyInPx + .5f);
		if(aPoint.mY / aPxWidthInNm + .5f - (aMaxDistancexyInPx + .5f) < 0) vYStart = 0; else vYStart = (int)(aPoint.mY / aPxWidthInNm+ .5f) - (int)(aMaxDistancexyInPx + .5f);
		if(aPoint.mZ / aPxDepthInNm + .5f - (aMaxDistancezInPx + .5f)  < 0) vZStart = 0; else vZStart = (int)(aPoint.mZ / aPxDepthInNm + .5f) - (int)(aMaxDistancezInPx + .5f);
		if(aPoint.mX / aPxWidthInNm + .5f + (aMaxDistancexyInPx + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(aPoint.mX / aPxWidthInNm+ .5f) + (int)(aMaxDistancexyInPx + .5f);
		if(aPoint.mY / aPxWidthInNm + .5f + (aMaxDistancexyInPx + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(aPoint.mY / aPxWidthInNm+ .5f) + (int)(aMaxDistancexyInPx + .5f);
		if(aPoint.mZ / aPxDepthInNm + .5f + (aMaxDistancezInPx + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(aPoint.mZ / aPxDepthInNm + .5f) + (int)(aMaxDistancezInPx + .5f);

		for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
			for(int vY = vYStart; vY <= vYEnd; vY++){
				for(int vX = vXStart; vX <= vXEnd; vX++){
					mIntensityBitmap[vZ][vY][vX] = true;
				}
			}		

		}
	}
	/**
	 * 
	 * @param aState
	 * @param aX
	 * @param aY
	 * @return 1,2 or 3 for SPB1, SPB2 resp. Tip
	 */
	public int getNearestBioobject2D(float[] aState, float aX, float aY){
		Point2D.Float vClickP = new Point.Float(aX,aY);
		Point3D[] vPoints = getPointsFromState(aState);
		float vD1 = (float)vClickP.distance(vPoints[0].mX / getPixelWidthInNm(),vPoints[0].mY / getPixelWidthInNm());
		float vD2 = (float)vClickP.distance(vPoints[1].mX / getPixelWidthInNm(),vPoints[1].mY / getPixelWidthInNm());
		float vDT = (float)vClickP.distance(vPoints[2].mX / getPixelWidthInNm(),vPoints[2].mY / getPixelWidthInNm());
		float vMinDist = Math.min(vD1, vD2);
		vMinDist = Math.min(vMinDist, vDT);
		if(vMinDist == vD1)
			return 1;
		if(vMinDist == vD2)
			return 2;
		if(vMinDist == vDT)
			return 3;
		return 0;
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
	/**
	 * Calculates the expected mean of a gaussian fitted to a Ray trough the imagestack.
	 * @param aX The x position of the ray
	 * @param aY The y position of the ray
	 * @param aIS The imageStack where the intensities are read out.
	 * @return the expected z position of a Gaussian in [1; <code>aIS.getSize</code>]
	 */
	private float calculateExpectedZPositionAt(int aX, int aY, ImageStack aIS) 
	{
		float vMaxInt = 0;
		int vMaxSlice = 0;
		for(int vZ = 0; vZ < mNSlices; vZ++) {
			float vThisInt;
			if((vThisInt = aIS.getProcessor(vZ+1).getf(aX, aY)) > vMaxInt) {
				vMaxInt =  vThisInt;
				vMaxSlice = vZ;
			}
			
		}
		float vSumOfIntensities = 0f;
		float vRes = 0f;
		int vStartSlice = Math.max(1, vMaxSlice-2);
		int vStopSlice = Math.min(mNSlices, vMaxSlice+2);
		for(int vZ = vStartSlice; vZ <= vStopSlice; vZ++) {
			vSumOfIntensities += aIS.getProcessor(vZ).getf(aX, aY);
			vRes += (vZ + 1) * aIS.getProcessor(vZ).getf(aX, aY);
		}
		return vRes / vSumOfIntensities;
	}

//	@SuppressWarnings("serial")
//	private class DrawCanvas extends ImageCanvas {
//		public DrawCanvas(ImagePlus aImagePlus){			
//			super(aImagePlus);
//		}
//		public void paint(Graphics aG){
//			//double vMagnificationMemory = this.getMagnification();			
//			super.paint(aG);
//			PaintLines(aG);
////			this.addMouseListener(this);
//
//		}
//
////		public void mouseClicked(MouseEvent aEvent) {
//////		super.mouseClicked(aEvent);
////		IJ.showMessage("mouse clicked");
//
////		}
//
//		private void PaintLines(Graphics aG){			
//			int vCurrentFrame = mZProjectedImagePlus.getCurrentSlice();
//			if(mStateVectorsMemory.elementAt(vCurrentFrame - 1) != null) {
//				try{
//					for(float[] vState : mStateVectorsMemory.elementAt(vCurrentFrame - 1)){
//						aG.setColor(Color.MAGENTA);
//						int vBodyX = (int)Math.round(vState[0]*magnification);
//						int vBodyY = (int)Math.round(vState[1]*magnification);
//						aG.drawLine(vBodyX - 5, vBodyY, vBodyX + 5, vBodyY);
//						aG.drawLine(vBodyX, vBodyY - 5, vBodyX, vBodyY + 5);						
//						aG.setColor(Color.yellow);
//						aG.drawLine(
//								(int)((vState[0] - (vState[2]/2f) * Math.cos(vState[3])) * magnification + 0.5f), 
//								(int)((vState[1] - (vState[2]/2f) * Math.sin(vState[3])) * magnification + 0.5f), 
//								(int)((vState[0] + (vState[2]/2f) * Math.cos(vState[3])) * magnification + 0.5f), 
//								(int)((vState[1] + (vState[2]/2f) * Math.sin(vState[3])) * magnification + 0.5f));
//						aG.setColor(Color.cyan);
//						int vPointX = (int)((vState[0] + (vState[2]/2f) * Math.cos(vState[3]) + vState[4] * Math.cos(vState[5] + vState[3])) * magnification + 0.5f);
//						int vPointY = (int)((vState[1] + (vState[2]/2f) * Math.sin(vState[3]) + vState[4] * Math.sin(vState[5] + vState[3])) * magnification + 0.5f);
//						aG.drawLine(
//								(int)((vState[0] + (vState[2]/2f) * Math.cos(vState[3]))  * magnification + 0.5f), 
//								(int)((vState[1] + (vState[2]/2f) * Math.sin(vState[3])) * magnification + 0.5f), 
//								vPointX, 
//								vPointY);
//						aG.setColor(Color.green);
//						aG.drawLine(vPointX - 5, vPointY, vPointX + 5, vPointY);
//						aG.drawLine(vPointX, vPointY - 5, vPointX, vPointY + 5);
//					}
//				}catch(java.lang.NullPointerException aE){
//					//nothing
//				}
//			}
//		}
//
//	}	
	
	
//	@SuppressWarnings("serial")
//	private class ParticleMonitorCanvas extends ImageCanvas {
//		ImagePlus mImagePlus;
//		public ParticleMonitorCanvas(ImagePlus aImagePlus){
//			super(aImagePlus);
//			mImagePlus = aImagePlus;
//		}
//
//		public void paint(Graphics g){
//			super.paint(g);
//			try{
//				int vFeaturePointInd = 1;
//				for(Vector<float[]> vFeaturePoint : mParticleMonitor.elementAt(mImagePlus.getCurrentSlice()-1)){
//					g.setColor(new Color(vFeaturePointInd*1000));
//					vFeaturePointInd++;
//					for (float[] vParticle : vFeaturePoint) {
//						
//					}
//				}
//			}
//			catch(java.lang.NullPointerException vE){
//				//do nothing
//			}
//		}
//	}

	public void CalculateGaussKernel(float[] aKernel){
		int vM = aKernel.length/2;
		for(int vI = 0; vI < vM; vI++){
			aKernel[vI] = (float)(1f/(Math.sqrt(2f*Math.PI)*mGaussBlurRadius) * 
					Math.exp(-(float)((vM-vI)*(vM-vI))/(2f*mGaussBlurRadius*mGaussBlurRadius)));
			aKernel[aKernel.length - vI - 1] = aKernel[vI];
		}
		aKernel[vM] = (float)(1f/(Math.sqrt(2f*Math.PI)*mGaussBlurRadius));		
	}


	@Override
	protected void paintOnCanvas(Graphics aG, float[] vState,
			double aMagnification) {
		float vPxWidth = getPixelWidthInNm();
		Point3D vPoints[] = getPointsFromState(vState);
		int vSPB1x = (int)Math.round(vPoints[0].mX*aMagnification/vPxWidth+1);
		int vSPB1y = (int)Math.round(vPoints[0].mY*aMagnification/vPxWidth+1);
		int vSPB2x = (int)Math.round(vPoints[1].mX*aMagnification/vPxWidth+1);
		int vSPB2y = (int)Math.round(vPoints[1].mY*aMagnification/vPxWidth+1);
		int vTipx = (int)Math.round(vPoints[2].mX*aMagnification/vPxWidth+1);
		int vTipy = (int)Math.round(vPoints[2].mY*aMagnification/vPxWidth+1);

		aG.setColor(Color.yellow);
		aG.drawLine(vSPB1x - 5, vSPB1y, vSPB1x + 5, vSPB1y);
		aG.drawLine(vSPB1x, vSPB1y - 5, vSPB1x, vSPB1y + 5);
		aG.drawLine(vSPB2x - 5, vSPB2y, vSPB2x + 5, vSPB2y);
		aG.drawLine(vSPB2x, vSPB2y - 5, vSPB2x, vSPB2y + 5);
		aG.drawLine(vSPB1x, vSPB1y, vSPB2x, vSPB2y);

		aG.setColor(Color.green);
		aG.drawLine( vSPB2x, vSPB2y, vTipx, vTipy);
		aG.drawLine(vTipx - 5, vTipy, vTipx + 5, vTipy);
		aG.drawLine(vTipx, vTipy - 5, vTipx, vTipy + 5);

//		aG.setColor(Color.red);
//		aG.drawLine((int)(vState[0]*aMagnification) - 5, (int)(vState[1]*aMagnification), (int)(vState[0]*aMagnification) + 5, (int)(vState[1]*aMagnification));
//		aG.drawLine((int)(vState[0]*aMagnification), (int)(vState[1]*aMagnification - 5), (int)(vState[0]*aMagnification), (int)(vState[1]*aMagnification + 5));

	}

	protected void paintParticleOnCanvas(Graphics aG, float[] aParticle, double aMagnification) 
	{
		float vPxWidth = getPixelWidthInNm();
		Point3D[] vPoints = getPointsFromState(aParticle);
		aG.setColor(Color.yellow);
		aG.drawRect((int)(aMagnification * vPoints[0].mX /vPxWidth + .5f),(int) (aMagnification * vPoints[0].mY/vPxWidth + .5f), 1, 1);

		aG.setColor(Color.green);
		aG.drawRect((int)(aMagnification * vPoints[1].mX/vPxWidth + .5f),(int) (aMagnification * vPoints[1].mY/vPxWidth + .5f), 1, 1);
		
		aG.setColor(Color.red);
		aG.drawRect((int)(aMagnification * vPoints[2].mX/vPxWidth + .5f),(int) (aMagnification * vPoints[2].mY/vPxWidth + .5f), 1, 1);
	}
	
	protected void initializeWithMouseButtonPressed()
	{
		super.initializeWithMouseButtonPressed();
		IJ.setTool(Toolbar.LINE);
	}
	
	private Point3D mInitSPB1, mInitSPB2, mInitTip;
	private int mPointToCorrect;
	protected void mouseReleased(int aX, int aY)
	{
		int vCurrentFrame = mZProjectedImagePlus.getCurrentSlice();
		if(mStateOfFilter == STATE_OF_FILTER.INIT && mStateOfInit == STATE_OF_INIT.INIT_STEP2) {
			mStateOfInit = STATE_OF_INIT.INIT_STEP3;
			mInitSPB2 = new Point3D(aX, aY, calculateExpectedZPositionAt(aX, aY, getAFrameCopy(mOriginalImagePlus, vCurrentFrame)));
			IJ.setTool(Toolbar.POINT);
		}
		if(mStateOfFilter == STATE_OF_FILTER.CORRECTING) {
			mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
			
			FeatureObject vFO = mFeatureObjects.elementAt(0);//we only track one object in this derived class
			float[] vState = vFO.mStateVectorsMemory[mZProjectedImagePlus.getCurrentSlice() - 1];
			
			//get the points from the state in pixels:
			Point3D[] vPoints = getPointsFromState(vState);
			vPoints[0].mX /= getPixelWidthInNm();vPoints[1].mX /= getPixelWidthInNm();vPoints[2].mX /= getPixelWidthInNm();
			vPoints[0].mY /= getPixelWidthInNm();vPoints[1].mY /= getPixelWidthInNm();vPoints[2].mY /= getPixelWidthInNm();
			vPoints[0].mZ /= getPixelDepthInNm();vPoints[1].mZ /= getPixelDepthInNm();vPoints[2].mZ /= getPixelDepthInNm();
			        
			//get the corrected point in pixels
			Point vClicked = new Point(aX, aY);
			Point3D vCorrectedPoint = new Point3D(vClicked.x, vClicked.y, 
					calculateExpectedZPositionAt(aX, aY, getSubStackFloat(mOriginalImagePlus.getStack(), (vCurrentFrame-1) * mNSlices + 1, vCurrentFrame * mNSlices + 1)));
			float vIntensityOfCorrectedPoint = mZProjectedImagePlus.getProcessor().getf((int)vCorrectedPoint.mX, (int)vCorrectedPoint.mY);
			if(mEMCCDMode) {
				vIntensityOfCorrectedPoint /= mGain;
			}
			//create a new state
			float[] vNewState = null;
			if(mPointToCorrect == 1) {
				vNewState = getStateVectorFromPixelPoints(vCorrectedPoint, vPoints[1], vPoints[2], (int)vIntensityOfCorrectedPoint, (int)vState[10], (int)vState[11]);
			}
			if(mPointToCorrect == 2) {
				vNewState = getStateVectorFromPixelPoints(vPoints[0], vCorrectedPoint, vPoints[2], (int)vState[9], (int)vIntensityOfCorrectedPoint, (int)vState[11]);
			}
			if(mPointToCorrect == 3) {
				vNewState = getStateVectorFromPixelPoints(vPoints[0], vPoints[1], vCorrectedPoint, (int)vState[9], (int)vState[10], (int)vIntensityOfCorrectedPoint);
			}
			vFO.mStateVectorsMemory[vCurrentFrame-1] = vNewState;
			mZProjectedImagePlus.repaintWindow();
		}
	}
	
	protected void mousePressed(int aX, int aY) 
	{
		if(mStateOfFilter == STATE_OF_FILTER.VISUALIZING) {
			//correction mode	
			if(mFeatureObjects.isEmpty()) 
				return;
			float[] vState = mFeatureObjects.elementAt(0).mStateVectorsMemory[mZProjectedImagePlus.getCurrentSlice() -1];
			if(IJ.shiftKeyDown() && vState != null){
				mStateOfFilter = STATE_OF_FILTER.CORRECTING;
				mPointToCorrect = getNearestBioobject2D(vState, aX, aY);
				System.out.println("nearest Point: " + mPointToCorrect);
			}
			return;
		}
		else if (mStateOfFilter == STATE_OF_FILTER.INIT) {
			int vCurrentFrame;
			switch(mStateOfInit) {
			case INIT_STEP1:
				//SPB1 set
				mStateOfInit = STATE_OF_INIT.INIT_STEP2;
				mStartingFrame = mZProjectedImagePlus.getCurrentSlice();
				mInitSPB1 = new Point3D(aX, aY, calculateExpectedZPositionAt(aX, aY, getAFrameCopy(mOriginalImagePlus, mStartingFrame)));
				break;
			case INIT_STEP2:
				//see mouseReleased
				break;
			case INIT_STEP3:
				mStateOfFilter = STATE_OF_FILTER.READY_TO_RUN;
				mStateOfInit = STATE_OF_INIT.INIT_STEP1;
				//Tip set
				vCurrentFrame = mZProjectedImagePlus.getCurrentSlice();
				mInitTip = new Point3D(aX, aY, calculateExpectedZPositionAt(aX, aY, 
						getAFrameCopy(mOriginalImagePlus, vCurrentFrame)));
				//set up the state vector and store it
				//TODO: search the brightest voxel in neighbourhood! 
				float vIntensityOfSPB1 = mZProjectedImagePlus.getProcessor().getf((int)mInitSPB1.mX, (int)mInitSPB1.mY);
				float vIntensityOfSPB2 = mZProjectedImagePlus.getProcessor().getf((int)mInitSPB2.mX, (int)mInitSPB2.mY);
				float vIntensityOfTip = mZProjectedImagePlus.getProcessor().getf((int)mInitTip.mX, (int)mInitTip.mY);
				float[] vInitState = null;
				if(!mEMCCDMode) {
					vInitState = getStateVectorFromPixelPoints(mInitSPB1, mInitSPB2, mInitTip, (int)vIntensityOfSPB1, (int)vIntensityOfSPB2, (int)vIntensityOfTip);
				} else {
					vInitState = getStateVectorFromPixelPoints(mInitSPB1, mInitSPB2, mInitTip, (int)(vIntensityOfSPB1 / mGain), (int)(vIntensityOfSPB2 / mGain), (int)(vIntensityOfTip / mGain));
				}
				//we only like to initialize one object at once, so test if it was already created:
				if(mFeatureObjects.isEmpty()) {
					registerNewObject(vInitState);
				} else {
					mFeatureObjects.elementAt(0).addInitStateAtFrame(vInitState, vCurrentFrame);
				}
				mZProjectedImagePlus.repaintWindow();
				break;				
			}	
		}
	}
	
	public float[] getMSigmaOfRandomWalk() 
	{
		return mSigmaOfDynamics;
	}
	
	/**
	 * Shows the dialog to enter the search radii 'sigma' of each paramter in the state vector.
	 * @return false if cancelled, else true.
	 */
	@Override
	protected boolean showParameterDialog() 
	{
		GenericDialog vGenericDialog = new GenericDialog("Enter search radius parameters",IJ.getInstance());
		for(int vD = 0; vD < mSigmaOfDynamics.length; vD++) {
			vGenericDialog.addNumericField(mDimensionsDescription[vD], mSigmaOfDynamics[vD], 2);
		}
		vGenericDialog.showDialog();
		for(int vD = 0; vD < mSigmaOfDynamics.length; vD++) {
			mSigmaOfDynamics[vD] = (float)vGenericDialog.getNextNumber();
		}

		if(vGenericDialog.wasCanceled())
			return false;

		return true;
	}
	
	public void setMSigmaOfRandomWalk(float[] sigmaOfRandomWalk) 
	{
		mSigmaOfDynamics = sigmaOfRandomWalk;
//		mSigmaOfRandomWalkA[0] *= getPixelWidthInNm();
//		mSigmaOfRandomWalkA[1] *= getPixelWidthInNm();
//		mSigmaOfRandomWalkA[2] *= getPixelDepthInNm();
//		mSigmaOfRandomWalkA[3] *= getPixelWidthInNm();
//		mSigmaOfRandomWalkA[6] *= getPixelWidthInNm();
	}
	
	public String[] getMDimensionsDescription() 
	{
		return mDimensionsDescription;
	}
	@Override
	public void setMBackground(float background) {
		mBackground = background;
		
	}

//	
//	@Override
//	protected float[][] getInitialCovarianceMatrix() {
//		float[][] vCov = new float[mSigmaOfDynamics.length][mSigmaOfDynamics.length];
//		for(int vD = 0; vD < mSigmaOfDynamics.length; vD++) {
//			vCov[vD][vD] = mSigmaOfDynamics[vD] / 10f;
//		}
//		return vCov;
//	}
	
	




	
}