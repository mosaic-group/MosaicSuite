package mosaic.plugins;


import ij.IJ;
import ij.ImageStack;
import ij.gui.GenericDialog;

import java.awt.Color;
import java.awt.Graphics;
/**
 * This is a derived class of the Particle Filter Tracking base class (PFTracking3D). It implements 
 * a feature point tracker for 2 points, e.g. two spindle poles, without any further dynamic model (random walk).  
 * @author Janick Cardinale, ETHZ 2008
 * @version 1.0
 * @see PFTracking3D
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
public class SPBsTracker_ extends PFTracking3D {
	private float mBackground = 1;
	private float[] mSigmaOfDynamics = {150, 150, 150, 1};
	protected static enum STATE_OF_INIT {NOTHING, POINT1_CLICKED};
	protected STATE_OF_INIT mStateOfInit = STATE_OF_INIT.NOTHING;
	
	@Override
	protected float[][][] generateIdealImage_3D(int aw, int ah, int as,
			float[] particle, int background, float pxWidthInNm,
			float pxDepthInNm) {
		float[][][] vIdealImage = new float[as][ah][aw];
		addBackgroundToImage(vIdealImage, mBackground);
		addFeaturePointTo3DImage(vIdealImage, new Point3D(particle[0],particle[1],particle[2]), particle[3], aw, ah, as, pxWidthInNm, pxDepthInNm, null);
		addFeaturePointTo3DImage(vIdealImage, new Point3D(particle[4],particle[5],particle[6]), particle[7], aw, ah, as, pxWidthInNm, pxDepthInNm, null);
		return vIdealImage;
	}
	
	@Override
	protected float calculatePriorPDF(float[] aSample,
			float[] aReferenceParticle) {
		return 1;
	}

	float[] mInitFirstPoint;
	@Override
	protected void mouseReleased(int ax, int ay) {
		if(getMStateOfFilter() == PFTracking3D.STATE_OF_FILTER.INIT) {
			if(mStateOfInit == STATE_OF_INIT.NOTHING) {
				mStateOfInit = STATE_OF_INIT.POINT1_CLICKED;
				ImageStack vInitFrame = getAFrameCopy(getMOriginalImagePlus(), getMZProjectedImagePlus().getCurrentSlice());
				float[] vZInformation = calculateExpectedZPositionAt(ax, ay, vInitFrame);
				mInitFirstPoint = new float[]{ax*getPixelWidthInNm(), ay*getPixelWidthInNm(), vZInformation[0]*getPixelDepthInNm(), vZInformation[1]};
				return;
			}
			if(mStateOfInit == STATE_OF_INIT.POINT1_CLICKED){
				setMStateOfFilter(STATE_OF_FILTER.READY_TO_RUN);
				mStateOfInit = STATE_OF_INIT.NOTHING;
				ImageStack vInitFrame = getAFrameCopy(getMOriginalImagePlus(), getMZProjectedImagePlus().getCurrentSlice());
				float[] vZInformation = calculateExpectedZPositionAt(ax, ay, vInitFrame);
				float[] vFirstState = new float[]{mInitFirstPoint[0], mInitFirstPoint[1], mInitFirstPoint[2], mInitFirstPoint[3]-mBackground, 
						ax*getPixelWidthInNm(), ay*getPixelWidthInNm(), vZInformation[0]*getPixelDepthInNm(), vZInformation[1]-mBackground};
				registerNewObject(vFirstState);
				return;
			}
		}
	}

	/**
	 * Calculates the expected mean of a gaussian fitted to a Ray trough the imagestack.
	 * @param aX The x position of the ray
	 * @param aY The y position of the ray
	 * @param aIS The imageStack where the intensities are read out.
	 * @return the expected z position of a Gaussian in [1; aIS.getSize<code></code>] at position 0 and
	 * the maximal intensity at position (ax,ay) in this stack.
	 */
	private float[] calculateExpectedZPositionAt(int aX, int aY, ImageStack aIS) 
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
		int vStartSlice = Math.max(0, vMaxSlice-2);
		int vStopSlice = Math.min(mNSlices - 1, vMaxSlice+2);
		for(int vZ = vStartSlice; vZ <= vStopSlice; vZ++) {
			vSumOfIntensities += aIS.getProcessor(vZ+1).getf(aX, aY);
			vRes += (vZ + 1) * aIS.getProcessor(vZ+1).getf(aX, aY);
		}
		return new float[]{vRes / vSumOfIntensities, vMaxInt};
	}
	
	protected void addBackgroundToImage(float[][][] aImage, float aBackground) 
	{
		for(float[][] vSlice : aImage) {
			for(float[] vRow : vSlice) {
				for(int vI = 0; vI < vRow.length; vI++) {
					vRow[vI] += aBackground;
				}
			}
		}
	}
	
	@Override
	protected void paintParticleOnCanvas(Graphics ag, float[] particle,
			double magnification) {
		ag.setColor(Color.green);
		ag.drawRect((int)(magnification * particle[0]/getPixelWidthInNm()+.5f), (int)(magnification * particle[1]/getPixelWidthInNm()+.5f), 
				1, 1);
		ag.setColor(Color.red);
		ag.drawRect((int)(magnification * particle[4]/getPixelWidthInNm()+.5f), (int)(magnification * particle[5]/getPixelWidthInNm()+.5f), 
				1, 1);
	}
	
	private void addFeaturePointTo3DImage(float[][][] aImage, Point3D aPoint, float aIntensity, int aW, int aH, int aS, float aPxWidthInNm, float aPxDepthInNm, float aGhostImage[][]) {
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
					aImage[vZ][vY][vX] += (float) (aIntensity  
							* Math.pow(Math.E, -(Math.pow(vX - aPoint.mX / aPxWidthInNm + .5f, 2) + Math.pow(vY - aPoint.mY / aPxWidthInNm + .5f, 2)) / (2 * vVarianceXYinPx))
							* Math.pow(Math.E, -Math.pow(vZ - aPoint.mZ / aPxDepthInNm + .5f, 2) / (2 * vVarianceZinPx)));
				}
			}
		}		
	}

	@Override
	protected void drawFromProposalDistribution(float[] particle,
			float pxWidthInNm, float pxDepthInNm) {
		particle[0] = particle[0] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[0]);
		particle[1] = particle[1] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[1]);
		particle[2] = particle[2] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[2]);
		particle[3] = particle[3] + (float)mRandomGenerator.nextGaussian()*mSigmaOfDynamics[3];
		particle[4] = particle[4] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[0]);
		particle[5] = particle[5] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[1]);
		particle[6] = particle[6] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[2]);
		particle[7] = particle[7] + (float)mRandomGenerator.nextGaussian()*mSigmaOfDynamics[3];
		if(particle[3]< 1)
			particle[3] = 1;
		if(particle[7]< 1)
			particle[7] = 1;
	}
	
//	@Override
//	protected void drawParticlesForOptimization(Vector<float[]> aParticles) {
//		super.drawParticlesForOptimization(aParticles);//the random walk
//			for(float[] vParticle : aParticles) {
//				if(vParticle[3] < 1)
//					vParticle[3] = 1;
//				if(vParticle[7] < 1)
//					vParticle[7] = 1;
//			}
//	}

//	@Override
//	protected boolean[][][] generateParticlesIntensityBitmap_3D(
//			float[][] setOfParticles, int aW, int aH, int aS) {
//		boolean[][][] vBitmap = new boolean[aS][aH][aW];
//		float vMaxDistancexy = 3*mSigmaPSFxy / getPixelWidthInNm();
//		float vMaxDistancez = 3*mSigmaPSFz / getPixelDepthInNm(); //in pixel!
//
//		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
//		for(float[] vParticle : setOfParticles) {
//			for(int vSPBi = 0; vSPBi < 2; vSPBi++) {
//				if(vParticle[0 + 4*vSPBi] + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(vParticle[0 + 4*vSPBi] + .5f) - (int)(vMaxDistancexy + .5f);
//				if(vParticle[1 + 4*vSPBi] + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(vParticle[1 + 4*vSPBi] + .5f) - (int)(vMaxDistancexy + .5f);
//				if(vParticle[2 + 4*vSPBi] + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(vParticle[2 + 4*vSPBi] + .5f) - (int)(vMaxDistancez + .5f);
//				if(vParticle[0 + 4*vSPBi] + .5f + (vMaxDistancexy + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(vParticle[0 + 4*vSPBi] + .5f) + (int)(vMaxDistancexy + .5f);
//				if(vParticle[1 + 4*vSPBi] + .5f + (vMaxDistancexy + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(vParticle[1 + 4*vSPBi] + .5f) + (int)(vMaxDistancexy + .5f);
//				if(vParticle[2 + 4*vSPBi] + .5f + (vMaxDistancez + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(vParticle[2 + 4*vSPBi] + .5f) + (int)(vMaxDistancez + .5f);
//
//				for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
//					for(int vY = vYStart; vY <= vYEnd; vY++){
//						for(int vX = vXStart; vX <= vXEnd; vX++){
//							vBitmap[vZ][vY][vX] = true;
//						}
//					}
//				}		
//			}
//		}
//		return vBitmap;
//	}

	@Override
	protected void paintOnCanvas(Graphics aG, float[] aState, double magnification) 
	{
			int vX = (int)Math.round(aState[0] / getPixelWidthInNm() *magnification);
			int vY = (int)Math.round(aState[1] / getPixelWidthInNm()*magnification);
			aG.setColor(Color.yellow);
			aG.drawLine(vX - 5, vY, vX + 5, vY);
			aG.drawLine(vX, vY - 5, vX, vY + 5);
			vX = (int)Math.round(aState[4] / getPixelWidthInNm()*magnification);
			vY = (int)Math.round(aState[5] / getPixelWidthInNm()*magnification);
			aG.setColor(Color.red);
			aG.drawLine(vX - 5, vY, vX + 5, vY);
			aG.drawLine(vX, vY - 5, vX, vY + 5);
	}


	
	@Override
	public void setMBackground(float background) {
		mBackground = background;
		
	}
	@Override
	public String[] getMDimensionsDescription() {
		String[] vS = {"x_1[nm]","y_1[nm]","z_1[nm]","Int_1","x_2[nm]","y_2[nm]","z_2[nm]","Int_2"};
		return vS;
	}
//	@Override
//	public boolean getMDoPrecisionOptimization() {
//		return mDoPrecisionCorrection;
//	}
	/**
	 * Shows the dialog to enter the search radii 'sigma' of each paramter in the state vector.
	 * @return false if cancelled, else true.
	 */
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
}
