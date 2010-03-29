

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.process.FloatProcessor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * This class extends the PFTracking3D Framework to track spindle pole buddies and a microtubule
 * which appear as 3 3D-point spread functions.
 * The class limits the framework to only one object at time at the moment.
 * @author janigo
 *
 */
public class MTandSPBsTracker_3D extends PFTracking3D {
	private int mDim = 12;//hack?
	public float[] mSigmaOfDynamicsInSphereCoords = new float[]{
			100f,
			100f,
			100f,
			50f,
			0.5f,
			0.5f,
			300,
			.5f,
			0.5f,
			1,
			1,
			1};
	protected float[] mInitialCovMatrix = new float[]{30f, 30f, 30f, 30f, 30f, 30f, 30f, 30f, 30f, 1f, 1f, 1f};
	
	protected String[] mDimensionsDescription = new String[]{
			"xSPBnew[nm]","ySPBnew[nm]","zSPBnew[nm]",
			"xSPBold[nm]","ySPBold[nm]","zSPBold[nm]",
			"xTip[nm]", "xTip[nm]", "xTip[nm]", 
			"Intensity_SPBnew", "Intensity_SPB2new", "Intensity_Tip"};
	
	protected String[] mSphereCoordinatesDimensionsDescription = new String[]{"x[nm]","y[nm]","z[nm]","L[nm]",
			"alpha_polar[rad]","alpha_azimuth[rad]", 
			"D[nm]", "beta_polar[rad]", "beta_azimuth[rad]", "Intensity_newSPB", "Intensity_oldSPB2", "Intensity_Tip"};
	
	//TODO:does not work??:
//	protected String RESULT_FILE_SUFFIX = "_mtTracker_results.txt";
//	protected String INIT_FILE_SUFFIX = "_mtTracker_initValues.txt";
//	protected String COV_FILE_SUFFIX = "_mtTracker_cartCovMatrix.txt";
	protected String COV_FILE2_SUFFIX = "_mtTracker_sphereCovMatrix.txt";
//	protected String PARAM_FILE_SUFFIX = "_mtTracker_params.txt";
	/*
	 * Init parameters
	 */
	protected static enum STATE_OF_INIT {INIT_STEP1, INIT_STEP2, INIT_STEP3};
	protected STATE_OF_INIT mStateOfInit = STATE_OF_INIT.INIT_STEP1;
	
	//this derived class only allows one feature object
	float[][][] mCovMatricesInSphereCoord;
	@Override
	protected boolean testForAbort(FeatureObject aFO, int aFrameIndex){
		//if microtubule-length is too short, then abort.
		Point3D vOldSPB = new Point3D(
				aFO.mStateVectorsMemory[aFrameIndex-1][3], 
				aFO.mStateVectorsMemory[aFrameIndex-1][4], 
				aFO.mStateVectorsMemory[aFrameIndex-1][5]);
		Point3D vTip = new Point3D(
				aFO.mStateVectorsMemory[aFrameIndex-1][6], 
				aFO.mStateVectorsMemory[aFrameIndex-1][7], 
				aFO.mStateVectorsMemory[aFrameIndex-1][8]);
		if(vOldSPB.subtract(vTip).getLength() < 600){
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
		Point3D[] vPoints = new Point3D[3];
		vPoints[0] = new Point3D(
				aParticle[0], 
				aParticle[1], 
				aParticle[2]);
		vPoints[1] = new Point3D(
				aParticle[3], 
				aParticle[4], 
				aParticle[5]);
		vPoints[2] = new Point3D(
				aParticle[6], 
				aParticle[7], 
				aParticle[8]);
		float[] vParticleInSphere = getSphereCoordinates(vPoints[0], vPoints[1], vPoints[2], aParticle[9], aParticle[10], aParticle[11]);
		for(int vI = 0; vI < aParticle.length-2; vI++) {
//			vParticleInSphere[vI] += (float)mRandomGenerator.nextGaussian() * mSigmaOfDynamicsInSphereCoords[vI];
			vParticleInSphere[vI] += (mRandomGenerator.nextFloat()-.5f) * 2f * mSigmaOfDynamicsInSphereCoords[vI];
		}
		vPoints = getPointsFromSphereCoordinates(vParticleInSphere);

		aParticle[0] = vPoints[0].mX;
		aParticle[1] = vPoints[0].mY;
		aParticle[2] = vPoints[0].mZ;
		aParticle[3] = vPoints[1].mX;
		aParticle[4] = vPoints[1].mY;
		aParticle[5] = vPoints[1].mZ;
		aParticle[6] = vPoints[2].mX;
		aParticle[7] = vPoints[2].mY;
		aParticle[8] = vPoints[2].mZ;
		aParticle[9] = vParticleInSphere[9];
		aParticle[10] = vParticleInSphere[10];
		aParticle[11] = vParticleInSphere[11];
		
		// TODO: Check for all the particle boundaries (image boundaries for the coordinates)
		//
		// check the intensity for a negative value
		//
		if(vParticleInSphere[9] < 1) vParticleInSphere[9] = 1;
		if(vParticleInSphere[10] < 1) vParticleInSphere[10] = 1; 
		if(vParticleInSphere[11] < 1) vParticleInSphere[11] =  1; 
		//
		// check if the orientation(sign) of D or L change
		//
		if(vParticleInSphere[3] < 0) vParticleInSphere[3] *= -1;
		if(vParticleInSphere[6] < 0) vParticleInSphere[6] *= -1;
	}
	
//	boolean mTestFirst = true;
	protected float[][][] generateIdealImage_3D(int aW, int aH, int aS, float[] aParticle, int aBackground, float aPxWidthInNm, float aPxDepthInNm)
	{
		float vIdealImage[][][] = new float[aS][aH][aW];
		for(int vZ = 0; vZ < aS; vZ++) {
			for(int vY = 0; vY < aH; vY++){
				for(int vX = 0; vX < aW; vX++){
					vIdealImage[vZ][vY][vX] = aBackground;		//TODO:have a look on performance!	
				}
			}
		}


		addFeaturePointTo3DImage(vIdealImage, new Point3D(aParticle[6], aParticle[7], aParticle[8]), aParticle[11], aPxWidthInNm, aPxDepthInNm, null);
		addFeaturePointTo3DImage(vIdealImage, new Point3D(aParticle[0], aParticle[1], aParticle[2]), aParticle[9], aPxWidthInNm, aPxDepthInNm, null);
		addFeaturePointTo3DImage(vIdealImage, new Point3D(aParticle[3], aParticle[4], aParticle[5]), aParticle[10],  aPxWidthInNm, aPxDepthInNm, null);

		//		if(mTestFirst) {			
		//			mTestFirst = false;
		//			
		//			ImageStack vIS = new ImageStack(aH,aW);
		//			for(int vZ = 0; vZ < aS; vZ++) {
//				vIS.addSlice("slice: "+vZ, new FloatProcessor(vIdealImage[vZ]));
//			}
//			StackWindow vSW = new StackWindow(new ImagePlus("ideal image",vIS));
//			vSW.setVisible(true);
//		}
		return vIdealImage;
	}
	
	public int getNearestBioobject2D(float[] aState, float aX, float aY){
		Point2D.Float vClickP = new Point.Float(aX,aY);
		float vD1 = (float)vClickP.distance(aState[0] / getPixelWidthInNm(),aState[1] / getPixelWidthInNm());
		float vD2 = (float)vClickP.distance(aState[3] / getPixelWidthInNm(),aState[4] / getPixelWidthInNm());
		float vDT = (float)vClickP.distance(aState[6] / getPixelWidthInNm(),aState[7] / getPixelWidthInNm());
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
	
//	private void addFeaturePointTo3DImage(float[][][] aImage, float aX, float aY, float aZ, float aIntensity, int aW, int aH, int aS, float aPxWidthInNm, float aPxDepthInNm) {
//		//convert values to pixels
//		float vVarianceXYinPx = mSigmaPSFxy * mSigmaPSFxy / (aPxWidthInNm * aPxWidthInNm);
//		float vVarianceZinPx = mSigmaPSFz * mSigmaPSFz / (aPxDepthInNm * aPxDepthInNm);
//		float vMaxDistancexy = 4*mSigmaPSFxy / aPxWidthInNm;
//		float vMaxDistancez = 4*mSigmaPSFz / aPxDepthInNm; //in pixel!
//		float vPointX = aX /aPxWidthInNm;
//		float vPointY = aY / aPxWidthInNm;
//		float vPointZ = aZ / aPxDepthInNm;
//		
//		int vXStart=0, vXEnd=mWidth-1, vYStart=0, vYEnd=mHeight-1, vZStart=0, vZEnd=mNSlices-1;//defines a bounding box around the tip
//		if(vPointX + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(vPointX + .5f) - (int)(vMaxDistancexy + .5f);
//		if(vPointY + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(vPointY + .5f) - (int)(vMaxDistancexy + .5f);
//		if(vPointZ + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(vPointZ + .5f) - (int)(vMaxDistancez + .5f);
//		if(vPointX + .5f + (vMaxDistancexy + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(vPointX + .5f) + (int)(vMaxDistancexy + .5f);
//		if(vPointY + .5f + (vMaxDistancexy + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(vPointY + .5f) + (int)(vMaxDistancexy + .5f);
//		if(vPointZ + .5f + (vMaxDistancez + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(vPointZ + .5f) + (int)(vMaxDistancez + .5f);
//
//		//TODO:debug this:
////		if(vXStart < 0 || vYStart < 0 || vZStart < 0)
////			System.out.println("Stop debug");
//		
//		for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
//			for(int vY = vYStart; vY <= vYEnd; vY++){
//				for(int vX = vXStart; vX <= vXEnd; vX++){
//					aImage[vZ][vY][vX] += (float) (aIntensity * Math.pow(Math.E, 
//							-(Math.pow(vX - vPointX, 2) + Math.pow(vY - vPointY, 2)) / (2 * vVarianceXYinPx))
//							* Math.pow(Math.E, -Math.pow(vZ - vPointZ, 2) / (2 * vVarianceZinPx)));
//				}
//			}		
//		}
//	}
	
	/**
	 * Finds and returns the maximal intensity of a ray trough the imagestack.
	 * @param aX
	 * @param aY
	 * @param aIS
	 * @return
	 */
	private float findMaxIntensityPixelValueAt(int aX, int aY, ImageStack aIS){
		float vMaxInt = 0;
		for(int vZ = 0; vZ < mNSlices; vZ++) {
			float vThisInt;
			if((vThisInt = aIS.getProcessor(vZ+1).getf(aX, aY)) > vMaxInt) {
				vMaxInt =  vThisInt;
			}
			
		}
		return vMaxInt;
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
	
	protected float[] getSphereCoordinates(Point3D aSPB1, Point3D aSPB2, Point3D aTip,
			float aSPB1Intensity, float aSPB2Intensity, float aTipIntensity) {
		float vLineLength = (float)Math.sqrt((aSPB1.mX -aSPB2.mX)*(aSPB1.mX -aSPB2.mX) 
				+ (aSPB1.mY - aSPB2.mY)*(aSPB1.mY - aSPB2.mY)
				+ (aSPB1.mZ - aSPB2.mZ)*(aSPB1.mZ - aSPB2.mZ));
		float vLineLengthXY = (float)Math.sqrt((aSPB1.mX -aSPB2.mX)*(aSPB1.mX -aSPB2.mX) 
				+ (aSPB1.mY - aSPB2.mY)*(aSPB1.mY - aSPB2.mY));
		float vDistSPB2TipXY = (float)Math.sqrt((aTip.mX - aSPB2.mX) * (aTip.mX - aSPB2.mX) + (aTip.mY - aSPB2.mY) * (aTip.mY - aSPB2.mY));
		float vDistSPB2Tip = (float)Math.sqrt((aTip.mX - aSPB2.mX) * (aTip.mX - aSPB2.mX) + (aTip.mY - aSPB2.mY) * (aTip.mY - aSPB2.mY)
				+ (aTip.mZ - aSPB2.mZ) * (aTip.mZ - aSPB2.mZ));
		//
		//	calculate azimuth angles(in the xy plane) 
		//
		float vAlphaAzimuth = (float)Math.acos((aSPB2.mX - aSPB1.mX)/vLineLengthXY);
		if(aSPB2.mY - aSPB1.mY < 0) // check the quadrant
			vAlphaAzimuth = 2f * (float)Math.PI - vAlphaAzimuth;			
		float vBetaAzimuth = (float)Math.acos((aTip.mX - aSPB2.mX)/vDistSPB2TipXY);
		if(aTip.mY - aSPB2.mY < 0)
			vBetaAzimuth = 2f * (float)Math.PI - vBetaAzimuth;
		//
		// calculate the polar angles
		//
		float vAlphaPolar = (float)Math.acos((aSPB2.mZ - aSPB1.mZ) / vLineLength);
		float vBetaPolar = (float)Math.acos((aTip.mZ - aSPB2.mZ)/vDistSPB2Tip);
		//
		// set up the state vector
		//
		return new float[]{
				aSPB1.mX + vLineLength/2f * (float)Math.sin(vAlphaPolar) * (float)Math.cos(vAlphaAzimuth), 
				aSPB1.mY + vLineLength/2f * (float)Math.sin(vAlphaPolar) * (float)Math.sin(vAlphaAzimuth), 
				aSPB1.mZ + vLineLength/2f * (float)Math.cos(vAlphaPolar),
				vLineLength, vAlphaPolar, vAlphaAzimuth, vDistSPB2Tip, vBetaPolar - vAlphaPolar, vBetaAzimuth - vAlphaAzimuth, 
				aSPB1Intensity, aSPB2Intensity, aTipIntensity};
	}

	protected float[] getSphereCoordinatesFromPixelPoints(Point3D aSPB1, Point3D aSPB2, Point3D aTip, float aSPB1Intensity, float aSPB2Intensity, float aTipIntensity) 
	{
		//
		// Set up the state vector with straight forward calculations
		//
		Point3D vSPB1inNm = new Point3D(aSPB1.mX * getPixelWidthInNm(), aSPB1.mY * getPixelWidthInNm(), aSPB1.mZ * getPixelDepthInNm());
		Point3D vSPB2inNm = new Point3D(aSPB2.mX * getPixelWidthInNm(), aSPB2.mY * getPixelWidthInNm(), aSPB2.mZ * getPixelDepthInNm());
		Point3D vTipinNm = new Point3D(aTip.mX * getPixelWidthInNm(), aTip.mY * getPixelWidthInNm(), aTip.mZ * getPixelDepthInNm());
		return getSphereCoordinates(vSPB1inNm, vSPB2inNm, vTipinNm, aSPB1Intensity, aSPB2Intensity, aTipIntensity);
	}
	
	protected float[] getStateVectorFromPixelPoints(Point3D aSPB1, Point3D aSPB2, Point3D aTip, float aSPB1Intensity, float aSPB2Intensity, float aTipIntensity) {
		return new float[]{
				aSPB1.mX * getPixelWidthInNm(), aSPB1.mY * getPixelWidthInNm(), aSPB1.mZ * getPixelDepthInNm(), 
				aSPB2.mX * getPixelWidthInNm(), aSPB2.mY * getPixelWidthInNm(), aSPB2.mZ * getPixelDepthInNm(), 
				aTip.mX * getPixelWidthInNm(), aTip.mY * getPixelWidthInNm(), aTip.mZ * getPixelDepthInNm(), 
				aSPB1Intensity, aSPB2Intensity, aTipIntensity};
	}
	
	/**
	 * 
	 * @param aState
	 * @return position 0:SPB1(new), 1:SPB2(old), 2:Tip. Coordinates are in nanometers.
	 */
	protected Point3D[] getPointsFromSphereCoordinates(float[] aState) {
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
	 * Converts a state or a particle from sphere to cartesian coordinates. It does not matter if is is a particle and has thus
	 * dimension n or just a state and has dimension n-2. After entry n-3, entries are copied into the result array.
	 * @param aCartesianCoordinateParticle particle to convert.
	 * @return the converted state/particle
	 */
	protected float[] getSphereCoordinateParticle(float[] aCartesianCoordinateParticle) {
		float[] vTempA = getSphereCoordinates(
				new Point3D(aCartesianCoordinateParticle[0],aCartesianCoordinateParticle[1],aCartesianCoordinateParticle[2]), 
				new Point3D(aCartesianCoordinateParticle[3],aCartesianCoordinateParticle[4],aCartesianCoordinateParticle[5]), 
				new Point3D(aCartesianCoordinateParticle[6],aCartesianCoordinateParticle[7],aCartesianCoordinateParticle[8]), 
				aCartesianCoordinateParticle[9], aCartesianCoordinateParticle[10], aCartesianCoordinateParticle[11]);
		if(aCartesianCoordinateParticle.length == 12) {//then we're already done.
			return vTempA;
		}
		//copy the rest
		float[] vRes = new float[aCartesianCoordinateParticle.length];
		for(int vI = 0; vI < 12; vI++) {
			vRes[vI] = vTempA[vI];
		}
		for(int vI = 12; vI < aCartesianCoordinateParticle.length; vI++) {
			vRes[vI] = aCartesianCoordinateParticle[vI];
		}
		return vRes;
	}
	
	protected float[] getStateVectorFromPoints(Point3D aSPB1, Point3D aSPB2, Point3D aTip, 
			float aSPB1Intensity, float aSPB2Intensity, float aTipIntensity) {
		return new float[]{
				aSPB1.mX, aSPB2.mY, aSPB1.mZ, 
				aSPB2.mX, aSPB2.mY, aSPB2.mZ, 
				aTip.mX, aTip.mY, aTip.mZ, 
				aSPB1Intensity, aSPB2Intensity, aTipIntensity};
	}
	
	@Override
	protected void paintOnCanvas(Graphics aG, float[] vState,
			double aMagnification) {
		float vPxWidth = getPixelWidthInNm();
		int vSPB1x = (int)Math.round(vState[0]*aMagnification/vPxWidth+1);
		int vSPB1y = (int)Math.round(vState[1]*aMagnification/vPxWidth+1);
		int vSPB2x = (int)Math.round(vState[3]*aMagnification/vPxWidth+1);
		int vSPB2y = (int)Math.round(vState[4]*aMagnification/vPxWidth+1);
		int vTipx = (int)Math.round(vState[6]*aMagnification/vPxWidth+1);
		int vTipy = (int)Math.round(vState[7]*aMagnification/vPxWidth+1);

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
		
		int vCurrentFrame = getMZProjectedImagePlus().getCurrentSlice();
		
		//uncertainties: Tip 
		aG.setColor(new Color(0f, 1f, 0f, 0.6f));
		
		float radiusX = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][6][6]*aMagnification*3f/getPixelWidthInNm());
		float radiusY = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][7][7]*aMagnification*3f/getPixelWidthInNm());
		float radiusZ = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][8][8]*aMagnification*3f/getPixelWidthInNm());
		float xPos=(float) (aMagnification*vState[6]/getPixelWidthInNm());
		float yPos=(float) (aMagnification*vState[7]/getPixelWidthInNm());
		aG.fillOval((int)(xPos-radiusX+.5f),
			(int)(yPos-radiusY+.5f),
			(int)(2*radiusX),
			(int)(2*radiusY));
		aG.setColor(new Color(0f,0f,1f));
		aG.drawPolyline(new int[]{(int) (xPos-10+.5), (int) (xPos-10+.5)}, new int[]{(int) (yPos-10+.5), (int) (yPos-5+radiusZ+.5)}, 2);
		
		//uncertainties: spb1 
		aG.setColor(new Color(1f, 1f, 0f, 0.6f));	
		radiusX = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][0][0]*aMagnification*3f/getPixelWidthInNm());
		radiusY = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][1][1]*aMagnification*3f/getPixelWidthInNm());
		radiusZ = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][2][2]*aMagnification*3f/getPixelWidthInNm());
		xPos=(float) (aMagnification*vState[0]/getPixelWidthInNm());
		yPos=(float) (aMagnification*vState[1]/getPixelWidthInNm());
		aG.fillOval((int)(xPos-radiusX+.5f),
			(int)(yPos-radiusY+.5f),
			(int)(2*radiusX),
			(int)(2*radiusY));
		aG.setColor(new Color(0f,0f,1f));
		aG.drawPolyline(new int[]{(int) (xPos-10+.5), (int) (xPos-10+.5)}, new int[]{(int) (yPos-10+.5), (int) (yPos-5+radiusZ+.5)}, 2);
		
		
		//uncertainties: spb2
		aG.setColor(new Color(1f, 1f, 0f, 0.6f));		
		radiusX = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][3][3]*aMagnification*3f/getPixelWidthInNm());
		radiusY = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][4][4]*aMagnification*3f/getPixelWidthInNm());
		radiusZ = (float)Math.sqrt(mFeatureObjects.elementAt(0).mCovMatrix[vCurrentFrame-1][5][5]*aMagnification*3f/getPixelWidthInNm());
		xPos=(float) (aMagnification*vState[3]/getPixelWidthInNm());
		yPos=(float) (aMagnification*vState[4]/getPixelWidthInNm());
		aG.fillOval((int)(xPos-radiusX+.5f),
			(int)(yPos-radiusY+.5f),
			(int)(2*radiusX),
			(int)(2*radiusY));
		aG.setColor(new Color(0f,0f,1f));
		aG.drawPolyline(new int[]{(int) (xPos-10+.5), (int) (xPos-10+.5)}, new int[]{(int) (yPos-10+.5), (int) (yPos-5+radiusZ+.5)}, 2);
		
		
//		if(radius < .5f*magnification) radius = .5f * (float)magnification;
//		ag.fillOval((int)(magnification * particle[0] / getPixelWidthInNm()-radius+.5f), 
//				(int)(magnification * particle[1] / getPixelWidthInNm()-radius+.5f), 
//				(int)(2*radius), 
//				(int)(2*radius));
	}
	
	protected void paintParticleOnCanvas(Graphics aG, float[] aParticle, double aMagnification) 
	{
		float vPxWidth = getPixelWidthInNm();
		aG.setColor(Color.yellow);
		aG.drawRect((int)(aMagnification * aParticle[0] /vPxWidth + .5f),(int) (aMagnification * aParticle[1]/vPxWidth + .5f), 1, 1);

		aG.setColor(Color.green);
		aG.drawRect((int)(aMagnification * aParticle[3]/vPxWidth + .5f),(int) (aMagnification * aParticle[4]/vPxWidth + .5f), 1, 1);
		
		aG.setColor(Color.cyan);
		aG.drawRect((int)(aMagnification * aParticle[6]/vPxWidth + .5f),(int) (aMagnification * aParticle[7]/vPxWidth + .5f), 1, 1);
		
		
//		int vYOffset=mOriginalImagePlus.getHeight()/2;
//		int vXOffset=mOriginalImagePlus.getWidth()/2;
		aG.setColor(Color.yellow);
		aG.drawRect(10, (int) (aMagnification * aParticle[2]/vPxWidth +.5f), 2, 2);
		aG.setColor(Color.green);
		aG.drawRect(15, (int) (aMagnification * aParticle[5]/vPxWidth +.5f), 2, 2);
		aG.setColor(Color.cyan);
		aG.drawRect(20, (int) (aMagnification * aParticle[8]/vPxWidth +.5f), 2, 2);
	}
	
	protected void initializeWithMouseButtonPressed()
	{
		super.initializeWithMouseButtonPressed();
		IJ.setTool(Toolbar.LINE);
	}
	
	private Point3D mInitSPB1inPx, mInitSPB2inPx, mInitTipinPx;
	private int mPointToCorrect;
	protected void mouseReleased(int aX, int aY)
	{
		int vCurrentFrame = mZProjectedImagePlus.getCurrentSlice();
		if(mStateOfFilter == STATE_OF_FILTER.INIT && mStateOfInit == STATE_OF_INIT.INIT_STEP2) {
			mStateOfInit = STATE_OF_INIT.INIT_STEP3;
			mInitSPB2inPx = new Point3D(aX, aY, calculateExpectedZPositionAt(aX, aY, getAFrameCopy(mOriginalImagePlus, vCurrentFrame)));
			IJ.setTool(Toolbar.POINT);
		}
		if(mStateOfFilter == STATE_OF_FILTER.CORRECTING) {
			mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
			
			FeatureObject vFO = mFeatureObjects.elementAt(0);//we only track one object in this derived class
			float[] vState = vFO.mStateVectorsMemory[mZProjectedImagePlus.getCurrentSlice() - 1];
			
			        
			//get the corrected point 
			Point vClicked = new Point(aX, aY);
			ImageStack vCurrentIS = getSubStackFloat(mOriginalImagePlus.getStack(), (vCurrentFrame-1) * mNSlices + 1, vCurrentFrame * mNSlices + 1);
			Point3D vCorrectedPoint = new Point3D(
					vClicked.x * getPixelWidthInNm(), 
					vClicked.y * getPixelWidthInNm(), 
					calculateExpectedZPositionAt(aX, aY, vCurrentIS) * getPixelDepthInNm());
			
			float vIntensityOfCorrectedPoint = findMaxIntensityPixelValueAt(aX, aY, vCurrentIS);
			if(mEMCCDMode) {
				vIntensityOfCorrectedPoint /= mGain;
			}
			
			//create a new state
			float[] vNewState = null;
			if(mPointToCorrect == 1) {
				vNewState = new float[]{
						vCorrectedPoint.mX, vCorrectedPoint.mY, vCorrectedPoint.mZ, 
						vState[3], vState[4], vState[5],
						vState[6], vState[7], vState[8],
						vIntensityOfCorrectedPoint, vState[10], vState[11]};
			}
			if(mPointToCorrect == 2) {
				vNewState = new float[]{
						vState[0], vState[1], vState[2],
						vCorrectedPoint.mX, vCorrectedPoint.mY, vCorrectedPoint.mZ, 
						vState[6], vState[7], vState[8],
						vState[9], vIntensityOfCorrectedPoint, vState[11]};
			}
			if(mPointToCorrect == 3) {
				vNewState = new float[]{
						vState[0], vState[1], vState[2],
						vState[3], vState[4], vState[5],
						vCorrectedPoint.mX, vCorrectedPoint.mY, vCorrectedPoint.mZ, 
						vState[9], vState[10], vIntensityOfCorrectedPoint};
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
			}
			return;
		}
		else if (mStateOfFilter == STATE_OF_FILTER.INIT) {
			int vCurrentFrameIndex;
			switch(mStateOfInit) {
			case INIT_STEP1:
				//SPB1 set
				mStateOfInit = STATE_OF_INIT.INIT_STEP2;
				mStartingFrame = mZProjectedImagePlus.getCurrentSlice();
				mInitSPB1inPx = new Point3D(aX, aY, calculateExpectedZPositionAt(aX, aY, getAFrameCopy(mOriginalImagePlus, mStartingFrame)));
				break;
			case INIT_STEP2:
				//see mouseReleased
				break;
			case INIT_STEP3:
				mStateOfFilter = STATE_OF_FILTER.READY_TO_RUN;
				mStateOfInit = STATE_OF_INIT.INIT_STEP1;
				//Tip set
				vCurrentFrameIndex = mZProjectedImagePlus.getCurrentSlice();
				ImageStack vCurrentFrameIS = getAFrameCopy(mOriginalImagePlus, vCurrentFrameIndex);
				mInitTipinPx = new Point3D(aX, aY, calculateExpectedZPositionAt(aX, aY, 
						vCurrentFrameIS));
				//set up the state vector and store it
				//TODO: search the brightest voxel in neighbourhood! 
				//Note that the background is not yet subtracted!
				float vIntensityOfSPB1; 
				float vIntensityOfSPB2; 
				float vIntensityOfTip; 
				float[] vInitState = null;

				if(!mEMCCDMode) {
					vIntensityOfSPB1 = findMaxIntensityPixelValueAt((int)mInitSPB1inPx.mX, (int)mInitSPB1inPx.mY, vCurrentFrameIS) - mBackground;
					vIntensityOfSPB2 = findMaxIntensityPixelValueAt((int)mInitSPB2inPx.mX, (int)mInitSPB2inPx.mY, vCurrentFrameIS)- mBackground;
					vIntensityOfTip = findMaxIntensityPixelValueAt((int)mInitTipinPx.mX, (int)mInitTipinPx.mY, vCurrentFrameIS)- mBackground;
					vInitState = getStateVectorFromPixelPoints(mInitSPB1inPx, mInitSPB2inPx, mInitTipinPx, vIntensityOfSPB1, vIntensityOfSPB2, vIntensityOfTip);
				} else {
					vIntensityOfSPB1 = findMaxIntensityPixelValueAt((int)mInitSPB1inPx.mX, (int)mInitSPB1inPx.mY, vCurrentFrameIS)/(float)mGain - mBackground;
					vIntensityOfSPB2 = findMaxIntensityPixelValueAt((int)mInitSPB2inPx.mX, (int)mInitSPB2inPx.mY, vCurrentFrameIS)/(float)mGain - mBackground;
					vIntensityOfTip = findMaxIntensityPixelValueAt((int)mInitTipinPx.mX, (int)mInitTipinPx.mY, vCurrentFrameIS)/(float)mGain - mBackground;
					vInitState = getStateVectorFromPixelPoints(mInitSPB1inPx, mInitSPB2inPx, mInitTipinPx, 
							vIntensityOfSPB1, vIntensityOfSPB2, vIntensityOfTip);
				}
				//we only like to initialize one object at once, so test if it was already created:
				if(mFeatureObjects.isEmpty()) {
					registerNewObject(vInitState);
				} else {
					mFeatureObjects.elementAt(0).addInitStateAtFrame(vInitState, vCurrentFrameIndex);
				}
				mZProjectedImagePlus.repaintWindow();
				break;				
			}	
		}
	}
			
	
	
	/**
	 * Shows the dialog to enter the search radii 'sigma' of each paramter in the state vector.
	 * @return false if cancelled, else true.
	 */
	@Override
	protected boolean showParameterDialog() 
	{
		GenericDialog vGenericDialog = new GenericDialog("Enter search radius parameters",IJ.getInstance());
		for(int vD = 0; vD < mSigmaOfDynamicsInSphereCoords.length; vD++) {
			vGenericDialog.addNumericField(mSphereCoordinatesDimensionsDescription[vD], mSigmaOfDynamicsInSphereCoords[vD], 2);
		}
		vGenericDialog.showDialog();
		for(int vD = 0; vD < mSigmaOfDynamicsInSphereCoords.length; vD++) {
			mSigmaOfDynamicsInSphereCoords[vD] = (float)vGenericDialog.getNextNumber();
		}

		if(vGenericDialog.wasCanceled())
			return false;

		return true;
	}
	
	public String[] getMDimensionsDescription() 
	{
		return mDimensionsDescription;
	}
	
//	@Override
//	protected float[][] getInitialCovarianceMatrix() {
//		float[] vSigmas = mInitialCovMatrix;
//		float[][] vCov = new float[vSigmas.length][vSigmas.length];
//		for(int vD = 0; vD < vSigmas.length; vD++) {
//			vCov[vD][vD] = vSigmas[vD] * vSigmas[vD];
//		}
//		return vCov;
//	}
//	
	@Override
	protected String generateParamString() {
		String vOut = super.generateParamString();
		for(int vI = 0; vI < mSigmaOfDynamicsInSphereCoords.length; vI++) {
			vOut += "sigma of dynamics in sphere coords["+vI+"] = " + mSigmaOfDynamicsInSphereCoords[vI] + "\n";
		}
		return vOut;
	}
	
	@Override
	protected void doStatistics(float[][] aSetOfParticles, int aFrameIndex) {
		int vDimOfState = aSetOfParticles[0].length - 2;
		int vNbParticles = aSetOfParticles.length;
		
		//the first time, init the data structure (hack:at this timepoint, everything is initialized properly)
		if(mCovMatricesInSphereCoord == null) {
			mCovMatricesInSphereCoord = new float[getMNFrames()][vDimOfState][vDimOfState];
			
		}
		
		//convert the set of particles
		float[][] vConvertedParticles = new float[vNbParticles][];
		for(int vP = 0; vP < vNbParticles; vP++) {
			vConvertedParticles[vP] = getSphereCoordinateParticle(aSetOfParticles[vP]);
		}
		
		//
		//	calculate the weighted mean.
		//
		float[] vWeightedMean = new float[vDimOfState];
		for (float[] vParticle : vConvertedParticles) {
			for(int vI = 0; vI < vDimOfState; vI++) {
				vWeightedMean[vI] += vParticle[vI] / (float)vNbParticles;// * vParticle[vDimOfState + 1];//falsch!
			}
		}
		
		//update the matrix
		for (float[] vParticle : vConvertedParticles){
			float[] vResidual = new float[vDimOfState];
			for(int vI = 0; vI < vDimOfState; vI++) {
				vResidual[vI] = vParticle[vI] - vWeightedMean[vI];
			}
			for(int vY = 0; vY < vDimOfState; vY++) {
				for(int vX = 0; vX < vDimOfState; vX++) {
					mCovMatricesInSphereCoord[aFrameIndex-1][vY][vX] += /*vParticle[vDimOfState + 1] * */ vResidual[vY] * vResidual[vX];
				}
			}
		}
		//normalize
		float vNorm = 1f/(float)(vNbParticles-1);
		for(int vY = 0; vY < vDimOfState; vY++) {
			for(int vX = 0; vX < vDimOfState; vX++) {
				mCovMatricesInSphereCoord[aFrameIndex-1][vY][vX] *= vNorm;
			}
		}

	}
//	/**
//	 * This method is overrided to also update the second covariance matrix (in sphere coords)
//	 * if the user deletes the 'results...' for the rest of the movie. 
//	 */
//	@Override
//	protected void deleteRestOfResultsButtonPressed() {
//		int frameToStart = mZProjectedImagePlus.getCurrentSlice();
//		for(int vF = frameToStart-1; vF < mNFrames; vF++) {
//			for(int vI = 0; vI < mCovMatricesInSphereCoord[0].length;vI++) {
//				for(int vJ = 0; vJ < mCovMatricesInSphereCoord[vF][0].length;vJ++) {
//					mCovMatricesInSphereCoord[vF][vI][vJ] = 0.0f;
//				}
//			}
//		}		
//		super.deleteRestOfResultsButtonPressed();
//	}
	
	@Override
	protected void deleteResults(int frameToStart, int frameToEnd) {
		for(int vF = frameToStart-1; vF < frameToEnd; vF++) {
			for(int vI = 0; vI < mCovMatricesInSphereCoord[0].length;vI++) {
				for(int vJ = 0; vJ < mCovMatricesInSphereCoord[vF][0].length;vJ++) {
					mCovMatricesInSphereCoord[vF][vI][vJ] = 0.0f;
				}
			}
		}	
		super.deleteResults(frameToStart, frameToEnd);
	}
	/**
	 * Since we'd like to output a second cov matrix, we overwrite this method of the base class and 
	 * invoke the original method.
	 */
	@Override
	protected boolean writeCovMatrixFile(File aFile) {
		StringBuffer vW = new StringBuffer();

		for(int vF = 0; vF < mNFrames; vF++){
			vW.append("Frame " + (vF + 1) + ":\n\n");
			for(int vI = 0; vI < mCovMatricesInSphereCoord[vF].length; vI++) {
				vW.append("\n");
				for(int vJ = 0; vJ < mCovMatricesInSphereCoord[vF][0].length; vJ++) {
					vW.append(mCovMatricesInSphereCoord[vF][vI][vJ] + "\t");
				}
			}
			vW.append("\n\n");
		}

		if(!writeTextFile(vW,getTextFile(COV_FILE2_SUFFIX))) {
			return false;
		}
		return super.writeCovMatrixFile(aFile);
	}
	
	@Override
	protected boolean createParticleVisualization(float[][] aSetOfParticles) {
		int vZoomfactor = 10;
		int vIntensity = 10;
		int vWidth = getMWidth() * vZoomfactor;
		int vHeight = getMHeight() * vZoomfactor;
		int vSlices = getMNSlices() * vZoomfactor;
		ImageStack vIS = new ImageStack(vWidth,vHeight);
		for(int vS = 0; vS < vSlices; vS++) {
			vIS.addSlice(" "+vS, new FloatProcessor(vWidth,vHeight));
		}
		for(float[] vParticle : aSetOfParticles) {
//			byte[] vPixels = (byte[])vIS.getProcessor((int)(vParticle[2]/getPixelDepthInNm()+.5f)).getPixels();
//			vPixels[(int)(vParticle[1] / getPixelWidthInNm() * vHeight + vParticle[0] / getPixelWidthInNm())] += vIntensity;
			
			float vOldValue = vIS.getProcessor((int)(vParticle[2]/getPixelDepthInNm()*vZoomfactor+.5f)).getf((int)(vParticle[0] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[1] / getPixelWidthInNm()*vZoomfactor+.5f));
			vIS.getProcessor((int)(vParticle[2]/getPixelDepthInNm()*vZoomfactor+.5f)).setf((int)(vParticle[0] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[1] / getPixelWidthInNm()*vZoomfactor+.5f), vOldValue + vIntensity);
			
			vOldValue = vIS.getProcessor((int)(vParticle[5]/getPixelDepthInNm()*vZoomfactor+.5f)).getf((int)(vParticle[3] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[4] / getPixelWidthInNm()*vZoomfactor+.5f));
			vIS.getProcessor((int)(vParticle[5]/getPixelDepthInNm()*vZoomfactor+.5f)).setf((int)(vParticle[3] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[4] / getPixelWidthInNm()*vZoomfactor+.5f), vOldValue + vIntensity);
			
			vOldValue = vIS.getProcessor((int)(vParticle[8]/getPixelDepthInNm()*vZoomfactor+.5f)).getf((int)(vParticle[6] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[7] / getPixelWidthInNm()*vZoomfactor+.5f));
			vIS.getProcessor((int)(vParticle[8]/getPixelDepthInNm()*vZoomfactor+.5f)).setf((int)(vParticle[6] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[7] / getPixelWidthInNm()*vZoomfactor+.5f), vOldValue + vIntensity);
			
		}
		new StackWindow(new ImagePlus("Particle Visualization",vIS)).setVisible(true);
		
		return true;
	}
	
	@Override
	protected boolean createParticleIntensityVisualization(float[][] aSetOfParticles) {
		
		int vZoomfactor = 10;
		int vIntensity = 10;
		int vWidth = getMWidth() * vZoomfactor;
		int vHeight = getMHeight() * vZoomfactor;
		int vSlices = getMNSlices() * vZoomfactor;
		ImageStack vIS = new ImageStack(vWidth,vHeight);
		for(int vS = 0; vS < vSlices; vS++) {
			vIS.addSlice(" "+vS, new FloatProcessor(vWidth,vHeight));
		}
		for(float[] vParticle : aSetOfParticles) {
//			byte[] vPixels = (byte[])vIS.getProcessor((int)(vParticle[2]/getPixelDepthInNm()+.5f)).getPixels();
//			vPixels[(int)(vParticle[1] / getPixelWidthInNm() * vHeight + vParticle[0] / getPixelWidthInNm())] += vIntensity;
			
			float vOldValue = vIS.getProcessor((int)(vParticle[2]/getPixelDepthInNm()*vZoomfactor+.5f)).getf((int)(vParticle[0] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[1] / getPixelWidthInNm()*vZoomfactor+.5f));
			vIS.getProcessor((int)(vParticle[2]/getPixelDepthInNm()*vZoomfactor+.5f)).setf((int)(vParticle[0] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[1] / getPixelWidthInNm()*vZoomfactor+.5f), vOldValue + vParticle[13]);
			
			vOldValue = vIS.getProcessor((int)(vParticle[5]/getPixelDepthInNm()*vZoomfactor+.5f)).getf((int)(vParticle[3] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[4] / getPixelWidthInNm()*vZoomfactor+.5f));
			vIS.getProcessor((int)(vParticle[5]/getPixelDepthInNm()*vZoomfactor+.5f)).setf((int)(vParticle[3] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[4] / getPixelWidthInNm()*vZoomfactor+.5f), vOldValue + vParticle[13]);
			
			vOldValue = vIS.getProcessor((int)(vParticle[8]/getPixelDepthInNm()*vZoomfactor+.5f)).getf((int)(vParticle[6] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[7] / getPixelWidthInNm()*vZoomfactor+.5f));
			vIS.getProcessor((int)(vParticle[8]/getPixelDepthInNm()*vZoomfactor+.5f)).setf((int)(vParticle[6] / getPixelWidthInNm()*vZoomfactor+.5f), (int)(vParticle[7] / getPixelWidthInNm()*vZoomfactor+.5f), vOldValue + vParticle[13]);
			
		}
		new StackWindow(new ImagePlus("Particle Likelihood",vIS)).setVisible(true);
		
		return true;
	}
	
	@Override protected boolean readCovarianceFile(File aFile) {
		mCovMatricesInSphereCoord = new float[getMNFrames()][mDim][mDim];
		File vCovFile = getTextFile(COV_FILE2_SUFFIX);		
		//read in the matrix if there is one already
		if(vCovFile.exists()){
			readSphereCovarianceFile(vCovFile);
		}
		return super.readCovarianceFile(aFile);
	}
	
	/**
	 * Reads the covariance file. The parameters read are then stored in the statevectormemory member. 
	 * @param aFile
	 * @see getInitFile()
	 * @return true if successful. false if not.
	 */
	protected boolean readSphereCovarianceFile(File aFile) 
	{
		BufferedReader vR = null;
		try {
			vR = new BufferedReader(new FileReader(aFile));
		}
		catch(FileNotFoundException aFNFE) {
			return false;
		}
		String vLine;
		int vFrameMem = Integer.MAX_VALUE;
		FeatureObject vCurrentObject = null;
		int vObjectCounter = 0;
		int vCurrentCovarianceLine = 0;
		int vFrame = 0;
		try {
			while((vLine = vR.readLine()) != null) {
				if(vLine.startsWith("#")) continue; //ignore
				if(vLine.matches("(\\s)*")) continue; //empty line
				Pattern vPattern = Pattern.compile("(,|\\||;|\\s|:)");
				String[] vPieces = vPattern.split(vLine);
				
				
				if(vPieces[0].equalsIgnoreCase("frame")){
					//then, the next pattern is the frame number
					try {
						vFrame = Integer.parseInt(vPieces[1])-1;
						vCurrentCovarianceLine = 0;
						continue; 
					}catch(NumberFormatException aNFE) {
						continue;
					}
				}

				//if the frame number of this line is smaller than the last one we begin with a new object
				if(vFrame < vFrameMem) {
					vFrameMem = -1;
					vCurrentObject = mFeatureObjects.elementAt(vObjectCounter);
					vObjectCounter++;
					
				}
				else {
					vFrameMem = vFrame;
				}
				
				for (int vPx = 0; vPx < vCurrentObject.getMDim(); vPx++) {
					try {
						mCovMatricesInSphereCoord[vFrame][vCurrentCovarianceLine][vPx] =  Float.parseFloat(vPieces[vPx]);

					}catch(NumberFormatException aNFE) {
						continue;//perhaps there is another matching line
					}
					
				}
				vCurrentCovarianceLine++;
				
			}
		}catch(IOException aIOE) {
			aIOE.printStackTrace();
			return false;
		}
		finally{
			try {
				vR.close();
			}catch(IOException aIOE) {
				aIOE.printStackTrace();
				return false;
			}
		}

		return true;
	}
	
	@Override
	protected void deleteAllButtonPressed() {
		if(getTextFile(COV_FILE2_SUFFIX) != null)
			getTextFile(COV_FILE2_SUFFIX).delete();
		super.deleteAllButtonPressed();
	}

}
