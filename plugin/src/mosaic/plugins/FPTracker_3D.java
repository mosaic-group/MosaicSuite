package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.FileInfo;

import java.awt.Color;
import java.awt.Graphics;


/**
 * This is a derived class of the Particle Filter Tracking base class (PFTracking3D). It implements 
 * a feature point tracker without any further dynamic model (random walk).  
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
public class FPTracker_3D extends PFTracking3D {
	private float[] mSigmaOfDynamics = {300, 300, 300, 1};
	
//	int debugiterator = 0;
	
	@Override
	protected float[][][] generateIdealImage_3D(int aw, int ah, int as,
			float[] particle, int background, float pxWidthInNm,
			float pxDepthInNm) {
		float[][][] vIdealImage = new float[as][ah][aw];
		addBackgroundToImage(vIdealImage, mBackground);
		addFeaturePointTo3DImage(vIdealImage, new Point3D(particle[0],particle[1],particle[2]), particle[3], pxWidthInNm, pxDepthInNm, null);

		
		//		debugiterator++;
//		System.out.println("debugiterator = " + debugiterator);
//		if(debugiterator == 1 || debugiterator == 10) {
//			ImageStack vIP = new ImageStack(ah,aw);
//			for(int vZ = 0; vZ < as;vZ++) {
//				vIP.addSlice("debug",new FloatProcessor(vIdealImage[vZ]).duplicate());				
//			}
//			new ImagePlus("debug image "+debugiterator,vIP).show();
//			
//		}
		return vIdealImage;
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
	
	
	
	@Override
	protected void drawFromProposalDistribution(float[] particle, 
			float pxWidthInNm, float pxDepthInNm) {
//		particle[0] = particle[0] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[0]);
//		particle[1] = particle[1] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[1]);
//		particle[2] = particle[2] + (float)mRandomGenerator.nextGaussian()*(mSigmaOfDynamics[2]);
//		particle[3] = particle[3] + (float)mRandomGenerator.nextGaussian()*mSigmaOfDynamics[3];
//		if(particle[3]< mBackground)
//			particle[3] = mBackground + 1;
		particle[0] = particle[0] + (mRandomGenerator.nextFloat()-.5f)*2f*(mSigmaOfDynamics[0]);
		particle[1] = particle[1] + (mRandomGenerator.nextFloat()-.5f)*2f*(mSigmaOfDynamics[1]);
		particle[2] = particle[2] + (mRandomGenerator.nextFloat()-.5f)*2f*(mSigmaOfDynamics[2]);
		particle[3] = particle[3] + (mRandomGenerator.nextFloat()-.5f)*2f*(mSigmaOfDynamics[3]);
		if(particle[3]< mBackground)
			particle[3] = mBackground + 1;
	}

//	@Override
//	protected boolean[][][] generateParticlesIntensityBitmap_3D(float[][] aSetOfParticles, int aW, int aH, int aS) {
//		boolean[][][] vBitmap = new boolean[aS][aH][aW];
//		float vMaxDistancexy = 3*mSigmaPSFxy / getPixelWidthInNm();
//		float vMaxDistancez = 3*mSigmaPSFz / getPixelDepthInNm(); //in pixel!
//
//		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;//defines a bounding box around the tip
//		for(float[] vParticle : aSetOfParticles) {
//			if(vParticle[0] + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(vParticle[0] + .5f) - (int)(vMaxDistancexy + .5f);
//			if(vParticle[1] + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(vParticle[1] + .5f) - (int)(vMaxDistancexy + .5f);
//			if(vParticle[2] + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(vParticle[2] + .5f) - (int)(vMaxDistancez + .5f);
//			if(vParticle[0] + .5f + (vMaxDistancexy + .5f) >= aW) vXEnd = aW - 1; else vXEnd = (int)(vParticle[0] + .5f) + (int)(vMaxDistancexy + .5f);
//			if(vParticle[1] + .5f + (vMaxDistancexy + .5f) >= aH) vYEnd = aH - 1; else vYEnd = (int)(vParticle[1] + .5f) + (int)(vMaxDistancexy + .5f);
//			if(vParticle[2] + .5f + (vMaxDistancez + .5f)  >= aS) vZEnd = aS - 1; else vZEnd = (int)(vParticle[2] + .5f) + (int)(vMaxDistancez + .5f);
//
//			for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
//				for(int vY = vYStart; vY <= vYEnd; vY++){
//					for(int vX = vXStart; vX <= vXEnd; vX++){
//						vBitmap[vZ][vY][vX] = true;
//					}
//				}
//			}		
//		}
//		return vBitmap;
//	}


	
	@Override
	public void setMBackground(float background) {
		mBackground = background;
		
	}
	@Override
	public String[] getMDimensionsDescription() {
		String[] vS = {"x[nm]","y[nm]","z[nm]","Intensity"};
		return vS;
	}
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

	@Override
	protected void paintOnCanvas(Graphics ag, float[] state,
			double magnification) {
			int vX = (int)Math.round(state[0]/getPixelWidthInNm()*magnification);
			int vY = (int)Math.round(state[1]/getPixelWidthInNm()*magnification);
			ag.setColor(Color.red);
			ag.drawLine(vX - 5, vY, vX + 5, vY);
			ag.drawLine(vX, vY - 5, vX, vY + 5);
		
	}

	@Override
		protected void paintParticleOnCanvas(Graphics ag, float[] particle,
				double magnification) {
	//		ag.setColor(new Color(1f, 0f, 0f, 0.6f));
	//		float radius = (float)magnification*4f*particle[particle.length-1];
	//		if(radius < .5f*magnification) radius = .5f * (float)magnification;
	//		ag.fillOval((int)(magnification * particle[0] / getPixelWidthInNm()-radius+.5f), 
	//				(int)(magnification * particle[1] / getPixelWidthInNm()-radius+.5f), 
	//				(int)(2*radius), 
	//				(int)(2*radius));
			ag.setColor(Color.red);
			ag.drawRect((int)(magnification * particle[0]/getPixelWidthInNm() + .5f),(int) (magnification * particle[1]/getPixelWidthInNm() + .5f), 1, 1);
		}

	@Override
	protected void mouseReleased(int ax, int ay) {
		if(getMStateOfFilter() == PFTracking3D.STATE_OF_FILTER.INIT) {
			setMStateOfFilter(STATE_OF_FILTER.READY_TO_RUN);
			ImageStack vInitFrame = getAFrameCopy(getMOriginalImagePlus(), getMZProjectedImagePlus().getCurrentSlice());
			float[] vZInformation = calculateExpectedZPositionAt(ax, ay, vInitFrame);
			if(mEMCCDMode) {
				vZInformation[1] /= mGain;
			}
			float[] vFirstState = new float[]{ax * getPixelWidthInNm(), ay * getPixelWidthInNm(), vZInformation[0] * getPixelDepthInNm(), vZInformation[1]};
			registerNewObject(vFirstState);
		}
	}
}
