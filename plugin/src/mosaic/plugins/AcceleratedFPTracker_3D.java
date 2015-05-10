package mosaic.plugins;


import ij.IJ;
import ij.ImageStack;
import ij.gui.GenericDialog;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This is a derived class of the Particle Filter Tracking base class (PFTracking3D). It implements 
 * a feature point tracker with the constraint of inertia: The feature points all have a certain velocity. The 
 * acceleration is modelled as Gaussian noise.  
 * @author Janick Cardinale, ETH Zurich
 * @version 1.0, January 08
 * @see PFTracking3D, PFTracking3D, BackgroundSubtractor
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
public class AcceleratedFPTracker_3D extends PFTracking3D {
	float mBackground = 10;
	float[] mSigmaOfDynamics = {200, 200, 0, 1};
	boolean mDoPrecisionOptimization = true;
	int mNFeaturePoints = 1;
	int mKMeansIterations = 5*mNFeaturePoints;
	int mDimOfState = 10;
	
	@Override
	protected float[][][] generateIdealImage_3D(int aw, int ah, int as,
			float[] particle, int background, float pxWidthInNm,
			float pxDepthInNm) {
		float[][][] vIdealImage = new float[as][ah][aw];
		addBackgroundToImage(vIdealImage, mBackground);
		addFeaturePointTo3DImage(vIdealImage, new Point3D(particle[0],particle[1],particle[2]), particle[9], aw, ah, as, pxWidthInNm, pxDepthInNm, null);
		return vIdealImage;
	}
	
	@Override
	protected float calculatePriorPDF(float[] aSample,
			float[] aReferenceParticle) {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	protected void mouseReleased(int ax, int ay) {
		if(getMStateOfFilter() == PFTracking3D.STATE_OF_FILTER.INIT) {
			setMStateOfFilter(STATE_OF_FILTER.READY_TO_RUN);
			ImageStack vInitFrame = getAFrameCopy(getMOriginalImagePlus(), getMZProjectedImagePlus().getCurrentSlice());
			float[] vZInformation = calculateExpectedZPositionAt(ax, ay, vInitFrame);
			float[] vFirstState = new float[]{ax, ay, vZInformation[0], 0, 0, 0, 0, 0, 0, vZInformation[1]};
			registerNewObject(vFirstState);
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
//		ag.setColor(Color.yellow);
//		ag.drawRect((int)(magnification * particle[0]+.5f), (int)(magnification * particle[1]+.5f), 
//				1, 1);
		ag.setColor(new Color(1f, 0f, 0f, 0.6f));
		float radius = (float)magnification*4f*particle[particle.length-1];
		if(radius < .5f*magnification) radius = .5f * (float)magnification;
		ag.fillOval((int)(magnification * particle[0]-radius+.5f), 
				(int)(magnification * particle[1]-radius+.5f), 
				(int)(2*radius), 
				(int)(2*radius));
	}
	
	private void addFeaturePointTo3DImage(float[][][] aImage, Point3D aPoint, float aIntensity, int aW, int aH, int aS, float aPxWidthInNm, float aPxDepthInNm, float aGhostImage[][]) {
		float vVarianceXYinPx = mSigmaPSFxy * mSigmaPSFxy / (aPxWidthInNm * aPxWidthInNm);
		float vVarianceZinPx = mSigmaPSFz * mSigmaPSFz / (aPxDepthInNm * aPxDepthInNm);
		float vMaxDistancexy = 3*mSigmaPSFxy / aPxWidthInNm;
		float vMaxDistancez = 3*mSigmaPSFz / aPxDepthInNm; //in pixel!

		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
		if(aPoint.mX + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(aPoint.mX + .5f) - (int)(vMaxDistancexy + .5f);
		if(aPoint.mY + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(aPoint.mY + .5f) - (int)(vMaxDistancexy + .5f);
		if(aPoint.mZ + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(aPoint.mZ + .5f) - (int)(vMaxDistancez + .5f);
		if(aPoint.mX + .5f + (vMaxDistancexy + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(aPoint.mX + .5f) + (int)(vMaxDistancexy + .5f);
		if(aPoint.mY + .5f + (vMaxDistancexy + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(aPoint.mY + .5f) + (int)(vMaxDistancexy + .5f);
		if(aPoint.mZ + .5f + (vMaxDistancez + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(aPoint.mZ + .5f) + (int)(vMaxDistancez + .5f);

		for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
			for(int vY = vYStart; vY <= vYEnd; vY++){
				for(int vX = vXStart; vX <= vXEnd; vX++){
					aImage[vZ][vY][vX] += (float) (aIntensity * Math.pow(Math.E, 
							-(Math.pow(vX - aPoint.mX + .5f, 2) + Math.pow(vY - aPoint.mY + .5f, 2)) / (2 * vVarianceXYinPx))
							* Math.pow(Math.E, -Math.pow(vZ - aPoint.mZ + .5f, 2) / 2 * vVarianceZinPx));
				}
			}
		}		
	}

	@Override
	protected void drawFromProposalDistribution(float[] particle,
			float pxWidthInNm, float pxDepthInNm) {
		//acceleration with noise
		particle[6] = particle[6] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[0]/pxWidthInNm);
		particle[7] = particle[7] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[1]/pxWidthInNm);
		particle[8] = particle[8] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[2]/pxDepthInNm);
		//velocities
		particle[3] = particle[3] + particle[6];
		particle[4] = particle[4] + particle[7];
		particle[5] = particle[5] + particle[8];
		//positions
		particle[0] = particle[0] + particle[3];
		particle[1] = particle[1] + particle[4];
		particle[2] = particle[2] + particle[5];
		particle[9] = particle[9] + (float)mRandomGenerator.nextGaussian()*mSigmaOfDynamics[3];
		if(particle[9] < 1)
			particle[9] = 1;
	}

	@Override
	protected boolean[][][] generateParticlesIntensityBitmap_3D(
			float[][] setOfParticles, int aW, int aH, int aS) {
		boolean[][][] vBitmap = new boolean[aS][aH][aW];
		float vMaxDistancexy = 3*mSigmaPSFxy / getPixelWidthInNm();
		float vMaxDistancez = 3*mSigmaPSFz / getPixelDepthInNm(); //in pixel!

		if(aS > 1){
			System.out.println("not 2d");
		}
		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
		for(float[] vParticle : setOfParticles) {
			if(vParticle[0] + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(vParticle[0] + .5f) - (int)(vMaxDistancexy + .5f);
			if(vParticle[1] + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(vParticle[1] + .5f) - (int)(vMaxDistancexy + .5f);
			if(vParticle[2] + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(vParticle[2] + .5f) - (int)(vMaxDistancez + .5f);
			if(vParticle[0] + .5f + (vMaxDistancexy + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(vParticle[0] + .5f) + (int)(vMaxDistancexy + .5f);
			if(vParticle[1] + .5f + (vMaxDistancexy + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(vParticle[1] + .5f) + (int)(vMaxDistancexy + .5f);
			if(vParticle[2] + .5f + (vMaxDistancez + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(vParticle[2] + .5f) + (int)(vMaxDistancez + .5f);

			for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
				for(int vY = vYStart; vY <= vYEnd; vY++){
					for(int vX = vXStart; vX <= vXEnd; vX++){
						vBitmap[vZ][vY][vX] = true;
					}
				}
			}		
		}
		return vBitmap;
	}
	
	@Override
	protected boolean autoInitFilter(ImageStack aImageStack) 
	{
		//
		//TODO: may be convolve with Gaussian shape here.
		//
		
		//
		// get the brightest voxel in the frame and store them in vNBrightesPixel
		//
		float[][] vPixels = new float[mNSlices][mWidth*mHeight]; //a 3D image
		for(int vI = 0; vI < mNSlices; vI++){
			vPixels[vI] = (float[])aImageStack.getProcessor(1+vI).convertToFloat().getPixels();
		}
		
		LinkedList<float[]> vNBrightestPixel = new LinkedList<float[]>();
				
		for(int vInd = 0; vInd < mNFeaturePoints; vInd++) {
			insert(vNBrightestPixel, new float[]{vPixels[0][vInd], vInd, 0});
		}
		float vMinValueInList = vNBrightestPixel.getFirst()[0];
		for(int vSliceInd = 0; vSliceInd < mNSlices; vSliceInd++){
			for(int vInd = 0; vInd < vPixels[0].length; vInd++) {			
				if(vPixels[vSliceInd][vInd] > vMinValueInList){
					insert(vNBrightestPixel, new float[]{vPixels[vSliceInd][vInd], vInd, vSliceInd});
					vNBrightestPixel.removeFirst();
					vMinValueInList = vNBrightestPixel.getFirst()[0];
				}
			}
		}
		//
		// Threshold the image to fasten up the algorithm.
		//
		float[][] vCentroids = new float[mNFeaturePoints][3];		
		ArrayList<float[]> vImageThresholded = new ArrayList<float[]>(mNFeaturePoints*3);//*mFeatruePointRadius
		
		for(int vSliceInd = 0; vSliceInd < mNSlices; vSliceInd++){
			for(int vI = 0; vI < vPixels[0].length; vI++) {
				if(vPixels[vSliceInd][vI] > (vMinValueInList + getMBackground())/2f) {
					vImageThresholded.add(new float[] {vI%mWidth, vI/mWidth, vSliceInd, vPixels[vSliceInd][vI]}); //evt. noch das Gewicht rein												
				}
			}
		}
		
		//
		// Initialize the centroids k-means
		//
		int vFeaturePointCounter = 0;
		for( float vFeaturePoint[]: vNBrightestPixel){
			vCentroids[vFeaturePointCounter][0] = vFeaturePoint[1]%mWidth; //the x-cooridinate
			vCentroids[vFeaturePointCounter][1] = (int)(vFeaturePoint[1]/mWidth); // the y-coordinate
			vCentroids[vFeaturePointCounter][2] = vFeaturePoint[2]; //the z-coordinate
			//vCentroids[vFeaturePointCounter][0] = vFeaturePoint[0]; //the intensity		
			vFeaturePointCounter++;
								
		}
		//
		// Begin k-means
		//
		for(int vIteration = 0; vIteration < mKMeansIterations; vIteration++){
			//reset the sum
			int[] vClassCounter = new int[mNFeaturePoints];
			float[][] vCentroidsSummedUp = new float[mNFeaturePoints][3];
			
			for(int vI = 0; vI < vCentroidsSummedUp.length; vI++){
				vCentroidsSummedUp[vI][0] = 0;
				vCentroidsSummedUp[vI][1] = 0;
				vCentroidsSummedUp[vI][2] = 0;
				vClassCounter[vI] = 0;
			}
			//classify the pixel which have an intensity above a certain threshold to a class
			for(float[] vDataPoint : vImageThresholded){
				int vCentroidIndex = 0;
				int vBestIndex = -1;
				float vZScale = getPixelDepthInNm() / getPixelWidthInNm();
				float vBestScore = mWidth*mWidth + mHeight*mHeight + mNSlices * mNSlices * vZScale * vZScale;
				for(float[] vCentroid : vCentroids){
					float vScore = (vCentroid[0]-vDataPoint[0]) * (vCentroid[0]-vDataPoint[0]) + 
					(vCentroid[1]-vDataPoint[1])*(vCentroid[1]-vDataPoint[1]) +
					(vCentroid[2]-vDataPoint[2])*(vCentroid[2]-vDataPoint[2])*vZScale*vZScale;
					if(vScore < vBestScore){
						vBestScore = vScore;
						vBestIndex = vCentroidIndex;					
					}
					vCentroidIndex++;
				}		
				vClassCounter[vBestIndex] += vDataPoint[3];
				vCentroidsSummedUp[vBestIndex][0] += vDataPoint[0] * vDataPoint[3];
				vCentroidsSummedUp[vBestIndex][1] += vDataPoint[1] * vDataPoint[3];
				vCentroidsSummedUp[vBestIndex][2] += vDataPoint[2] * vDataPoint[3];
			}		
			//recalculate the centroids
			for(int vC = 0; vC < vCentroids.length; vC++){
				if(vClassCounter[vC] > 0){
					vCentroids[vC][0] = vCentroidsSummedUp[vC][0]/vClassCounter[vC];
					vCentroids[vC][1] = vCentroidsSummedUp[vC][1]/vClassCounter[vC];
					vCentroids[vC][2] = vCentroidsSummedUp[vC][2]/vClassCounter[vC];
				}
			}
			
		}

		//
		// finally copy the list to the statevectors-vector
		// 
		for( float vCentroid[]: vCentroids){
			float[] vState = new float[mDimOfState];
			vState[0] = vCentroid[0]; // the x-cooridinate
			vState[1] = vCentroid[1]; // the y-coordinate
			vState[2] = vCentroid[2]; // the z-coordinate
			vState[3] = 0; //the velocity
			vState[4] = 0; //the velocity
			vState[5] = 0; //the velocity
			int[] vLocalMax = searchLocalMaximumIntensityWithSteepestAscent(Math.round(vCentroid[0]), 
					Math.round(vCentroid[1]), 
					Math.round(vCentroid[2]+1), 
					aImageStack);
			
			vState[6] = vPixels[vLocalMax[2]-1][mWidth*vLocalMax[1] + vLocalMax[0]]; //the intensity
			registerNewObject(vState);
		}
		return true;
	}

	/**
	 * Inserts such that the linked list remains sorted (descending)
	 * @param aLinkedList
	 * @param aNewValue
	 */
	private void insert(LinkedList<float[]> aLinkedList, float[] aNewValue){
		if(aLinkedList.isEmpty()){
			aLinkedList.add(aNewValue);
			return;
		}
		int vIndex = 0;
		for(float[] vValue : aLinkedList) {
			
			if (vValue[0] < aNewValue[0]){
				vIndex++;
				continue;
			}
			else {
				break;
			}
		}
		aLinkedList.add(vIndex, aNewValue);
	}
	

	@Override
	public String[] getMDimensionsDescription() {
		String[] vS = {"x", "y", "z", "vx", "vy", "vz", "ax", "ay", "az", "Intensity"};
		return vS;
	}
	@Override
	public void setMBackground(float background) {
		mBackground = background;
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
			vGenericDialog.addNumericField(mDimensionsDescription[vD+6], mSigmaOfDynamics[vD], 2);
		}
		vGenericDialog.showDialog();
		for(int vD = 0; vD < mSigmaOfDynamics.length; vD++) {
			mSigmaOfDynamics[vD] = (float)vGenericDialog.getNextNumber();
		}

		if(vGenericDialog.wasCanceled())
			return false;

		return true;
	}

	@Override
	protected void paintOnCanvas(Graphics ag, float[] state,
			double magnification) {
		int vX = (int)Math.round(state[0]*magnification);
		int vY = (int)Math.round(state[1]*magnification);
		ag.setColor(Color.red);
		ag.drawLine(vX - 5, vY - 5, vX + 5, vY + 5);
		ag.drawLine(vX + 5, vY - 5, vX - 5, vY + 5);

		
	}
}
