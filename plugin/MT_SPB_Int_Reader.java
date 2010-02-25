package ij.plugin; 

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.util.Random;

public class MT_SPB_Int_Reader extends MTandSPBsTracker_3D{
	protected static final String M0_FILE_SUFFIX = "_intM0.txt";
	protected static final String M2_FILE_SUFFIX = "_intM2.txt";
	protected static final String M3_FILE_SUFFIX = "_intM3.txt";
	protected static final String M4_FILE_SUFFIX = "_intM4.txt";
	protected static final String MAX_SUFFIX = "_peak.txt";
	float mRadiusXY = mSigmaPSFxy * 3;
	float mRadiusZ = mSigmaPSFz * 3;
	float[][] mIntensityMomentum0;
	float[][] mIntensityMomentum2;
	float[][] mIntensityMomentum3;
	float[][] mIntensityMomentum4;
	float[][] mIntensityMax;

	public int setup(String aArgs, ImagePlus aImp) 
	{
		//		if(IJ.versionLessThan("1.38u")) {
		//			return DONE;
		//		}
		//		if(aImp == null) {
		//			IJ.showMessage("Please open an image to track first.");
		//			return DONE;
		//		}		
		//		
		//		if(aImp.getNFrames() < 2){
		//			IJ.run("Properties...");
		//		}
		//		
		while(true) {
			String vUnit = aImp.getCalibration().getUnit();
			if(vUnit.equals("nm")) {
				mPxWidthInNm = (float)aImp.getCalibration().pixelWidth;
				mPxDepthInNm = (float)aImp.getCalibration().pixelDepth;
				break;
			} else if(vUnit.equals(IJ.micronSymbol+"m")) {
				mPxWidthInNm = (float)aImp.getCalibration().pixelWidth * 1000;
				mPxDepthInNm = (float)aImp.getCalibration().pixelDepth * 1000;
				break;
			} else if(vUnit.equals("mm")) {
				mPxWidthInNm = (float)aImp.getCalibration().pixelWidth * 1000000;
				mPxDepthInNm = (float)aImp.getCalibration().pixelDepth * 1000000;
				break;
			}
			IJ.showMessage("Please enter the pixel sizes in nm, " + IJ.micronSymbol + "m or mm");
			IJ.run("Properties...");
		}

		//
		// Init members
		//
		mOriginalImagePlus = aImp;
		mHeight = mOriginalImagePlus.getHeight();
		mWidth = mOriginalImagePlus.getWidth();
		mNFrames = mOriginalImagePlus.getNFrames();
		mNSlices = mOriginalImagePlus.getNSlices();
		mTrackTillFrame = mNFrames;
		mStartingFrame = sliceToFrame(mOriginalImagePlus.getCurrentSlice());
		mRandomGenerator = new Random(mSeed);		
		if(mDoUseTheoreticalPSF){
			mSigmaPSFxy = (0.21f * mWavelengthInNm / mNA);
			mSigmaPSFz = (0.66f * mWavelengthInNm * mn / (mNA*mNA));
		}

		mDimensionsDescription = getMDimensionsDescription();

		mIntensityMomentum0 = new float[3][mNFrames];
		mIntensityMomentum2 = new float[3][mNFrames];
		mIntensityMomentum3 = new float[3][mNFrames];
		mIntensityMomentum4 = new float[3][mNFrames];
		mIntensityMax = new float[3][mNFrames];


		//		StackStatistics vSS = new StackStatistics(mOriginalImagePlus);
		//		mBackground = (float)vSS.dmode;

		doZProjection();
		initVisualization();

		initPlugin();
		readIntensityMomenta(createMask(mRadiusXY, mRadiusZ));
		writeIntensityMomenta();
		IJ.freeMemory();	
		return DONE; //+ PARALLELIZE_STACKS; 
	}

	protected boolean[][][] createMask(float aRadiusXY, float aRadiusZ){
		float vRadiusXYinPx = aRadiusXY/getPixelWidthInNm();
		float vRadiusZinPx = aRadiusZ/getPixelDepthInNm();
		int vW = (int)(2.*vRadiusXYinPx)+1; //+1 such that we have a 'midpoint'
		int vH = (int)(2.*vRadiusXYinPx)+1;
		int vS = (int)(2.*vRadiusZinPx)+1;
		System.out.println("width = " + vW + ", height = " + vH + ",depth = " + vS);
		boolean[][][] vMask = new boolean[vS][vH][vW];
		for(int vZ = 0; vZ < vS; vZ++) {
			for(int vY = 0; vY < vH; vY++) {
				for (int vX = 0; vX < vW; vX++) {
					if(1.0> (vX-vRadiusXYinPx) * (vX-vRadiusXYinPx) / (vRadiusXYinPx * vRadiusXYinPx) + 
							(vY-vRadiusXYinPx) * (vY-vRadiusXYinPx) / (vRadiusXYinPx * vRadiusXYinPx) + 
							(vZ-vRadiusZinPx)  * (vZ-vRadiusZinPx)  / (vRadiusZinPx  * vRadiusZinPx)) {
						vMask[vZ][vY][vX] = true;
					} else {
						vMask[vZ][vY][vX] = false;
					}
				}
			}
		}
		return vMask;
	}

	protected void readIntensityMomenta(boolean[][][] aMask) {
		int vRadiusZ = aMask.length/2;
		int vRadiusY = aMask[0].length/2;
		int vRadiusX = aMask[0][0].length/2;
		for(FeatureObject vFO : mFeatureObjects){
			for(int vF = 0; vF < mNFrames; vF++) {
				//get the frame-stack:
				ImageStack vFrameIS = getAFrameCopy(mOriginalImagePlus, vF+1);

				//get the state; if result is = 0.0, the state is supposed to be invalid
				float[] vState = vFO.mStateVectorsMemory[vF];
				if(!(vState[0] < 1f)) {
					//do for the SPBs and the MT
					for(int vFP = 0; vFP < 3; vFP++){
						//calculate the correct starting voxel
						int vZStart = (int)(vState[2+vFP]/getPixelDepthInNm()+.5f)-vRadiusX;
						int vYStart = (int)(vState[1+vFP]/getPixelWidthInNm()+.5f)-vRadiusY;
						int vXStart = (int)(vState[0+vFP]/getPixelWidthInNm()+.5f)-vRadiusZ;
						//measure the momenta
						for(int vZ = 0; vZ < 2*vRadiusZ + 1; vZ++) {							
							for(int vY = 0; vY < 2*vRadiusY + 1; vY++) {
								for(int vX = 0; vX < 2*vRadiusX + 1; vX++) {
									if(aMask[vZ][vY][vX]) {
										if(vX+vXStart < 0 || vX + vXStart >= mWidth ||
												vY+vYStart < 0 || vY + vYStart >= mHeight ||
												vZ+vZStart < 0 || vZ + vZStart >= mNSlices) {
											continue;
										}
										float vPxValue = vFrameIS.getProcessor(vZ+vZStart+1).getf(vX+vXStart, vY+vYStart);
										if(vPxValue > mIntensityMax[vFP][vF]) mIntensityMax[vFP][vF] = vPxValue;
										float vDist2 = (vRadiusX-vX)*(vRadiusX-vX)+(vRadiusY-vY)*(vRadiusY-vY)+(vRadiusZ-vZ)*(vRadiusZ-vZ);
										mIntensityMomentum0[vFP][vF] += vPxValue;
										mIntensityMomentum2[vFP][vF] += vPxValue*(vDist2);
										mIntensityMomentum3[vFP][vF] += vPxValue*((float)Math.pow(vDist2, 1.5));
										mIntensityMomentum4[vFP][vF] += vPxValue*((float)Math.pow(vDist2, 2));
									}
								}
							}
						}
						mIntensityMomentum2[vFP][vF] /= mIntensityMomentum0[vFP][vF];
						mIntensityMomentum3[vFP][vF] /= mIntensityMomentum0[vFP][vF];
						mIntensityMomentum4[vFP][vF] /= mIntensityMomentum0[vFP][vF];

					}
				}
			}
		}
	}

	protected void writeIntensityMomenta() {
		//create the strings
		StringBuffer vM0Buffer = new StringBuffer();
		StringBuffer vM2Buffer = new StringBuffer();
		StringBuffer vM3Buffer = new StringBuffer();
		StringBuffer vM4Buffer = new StringBuffer();
		StringBuffer vMaxBuffer = new StringBuffer();



		for(int vF = 0; vF < mNFrames; vF++) {				
			vM0Buffer.append(
					mIntensityMomentum0[0][vF] + " " +
					mIntensityMomentum0[1][vF] + " " +
					mIntensityMomentum0[2][vF] + "\n");
			vM2Buffer.append(
					mIntensityMomentum2[0][vF] + " " +
					mIntensityMomentum2[1][vF] + " " +
					mIntensityMomentum2[2][vF] + "\n"); 
			vM3Buffer.append(
					mIntensityMomentum3[0][vF] + " " +
					mIntensityMomentum3[1][vF] + " " +
					mIntensityMomentum3[2][vF] + "\n");
			vM4Buffer.append(
					mIntensityMomentum4[0][vF] + " " +
					mIntensityMomentum4[1][vF] + " " +
					mIntensityMomentum4[2][vF] + "\n");
			vMaxBuffer.append(
					mIntensityMax[0][vF] + " " +
					mIntensityMax[1][vF] + " " +
					mIntensityMax[2][vF] + "\n");
		}

		//write the files
		writeTextFile(vM0Buffer, getTextFile(M0_FILE_SUFFIX));
		writeTextFile(vM2Buffer, getTextFile(M2_FILE_SUFFIX));
		writeTextFile(vM3Buffer, getTextFile(M3_FILE_SUFFIX));
		writeTextFile(vM4Buffer, getTextFile(M4_FILE_SUFFIX));
		writeTextFile(vMaxBuffer, getTextFile(MAX_SUFFIX));
	}

}
