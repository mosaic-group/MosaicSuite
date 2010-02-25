package ij.plugin;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;


import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import ij.plugin.filter.PlugInFilter;

public class Metaphase_Detector_PCA /*extends ParticleTracker3D_ implements PlugInFilter*/{
	
	Vector<Float> mEntropies = new Vector<Float>();
	Vector<float[]> mMeans = new Vector<float[]>();
	Vector<float[]> mPlanes = new Vector<float[]>();
	
	public Metaphase_Detector_PCA(Vector<Vector<float[]>> aParticles) {
		int vFrameCounter = 0;
		mEntropies.setSize(aParticles.size());
		mMeans.setSize(aParticles.size());
		mPlanes.setSize(aParticles.size());
		for(Vector<float[]> vFrameParticles : aParticles) {
			vFrameCounter++;
			double[][] vParticleValues = new double[3][vFrameParticles.size()];
			float[] vMean = new float[3];
			int vParticleCounter = 0;
			for(float[] vParticle : vFrameParticles) {
				vParticleValues[0][vParticleCounter] = vParticle[0];
				vParticleValues[1][vParticleCounter] = vParticle[1];
				vParticleValues[2][vParticleCounter] = vParticle[2]; //z scaled already!
				vMean[0] += vParticle[0];
				vMean[1] += vParticle[1];
				vMean[2] += vParticle[2];
				vParticleCounter++;
			}
			vMean[0] /= (float)vParticleCounter;
			vMean[1] /= (float)vParticleCounter;
			vMean[2] /= (float)vParticleCounter;
			mMeans.setElementAt(vMean, vFrameCounter-1);
			for(int vI = 0; vI < vParticleCounter; vI++) {
				vParticleValues[0][vI] -= vMean[0];
				vParticleValues[1][vI] -= vMean[1];
				vParticleValues[2][vI] -= vMean[2];
			}
			
			Matrix vParticleValuesMatrix = new Matrix(vParticleValues);
			Matrix vParticleValuesMatrixT = vParticleValuesMatrix.transpose();
			
			Matrix vCovMatrix = vParticleValuesMatrix.times(vParticleValuesMatrixT);
			vCovMatrix = vCovMatrix.times(1.0 / (double)vParticleCounter);
			
			EigenvalueDecomposition vEigenvalueDecomposition = new EigenvalueDecomposition(vCovMatrix);
			Matrix vEigenValues = vEigenvalueDecomposition.getD();
			
			// search biggest eigenvalue:
			int vMinEigenvalueIndex = 0;
			for(int vI = 2; vI < vEigenValues.getRowDimension(); vI++) {
				if(vEigenValues.get(vMinEigenvalueIndex, vMinEigenvalueIndex) > vEigenValues.get(vI, vI)){
					vMinEigenvalueIndex = vI;
				}
			}
			
			//now we have the explicit form of the plane equation. 
			
			
			float[] vNormalVec = new float[]{
					(float)vEigenvalueDecomposition.getV().get(0, vMinEigenvalueIndex), 
					(float)vEigenvalueDecomposition.getV().get(1, vMinEigenvalueIndex), 
					(float)vEigenvalueDecomposition.getV().get(2, vMinEigenvalueIndex)};			
			float vNormalizer = (float)Math.sqrt(
					vNormalVec[0] * vNormalVec[0] +
					vNormalVec[1] * vNormalVec[1] +
					vNormalVec[2] * vNormalVec[2]);
			vNormalVec[0] = (vNormalVec[0] / vNormalizer);
			vNormalVec[1] = (vNormalVec[1] / vNormalizer);
			vNormalVec[2] = (vNormalVec[2] / vNormalizer);
			
			
			
			float vD = (float)(vMean[0] * vNormalVec[0] + vMean[1] * vNormalVec[1] + vMean[2] * vNormalVec[2]);
			float[] vPlane = new float[4];
			vPlane[0] = vNormalVec[0];
			vPlane[1] = vNormalVec[1];
			vPlane[2] = vNormalVec[2];
			vPlane[3] = vD;
			mPlanes.setElementAt(vPlane, vFrameCounter -1);
			//for each particle, calculate the distance to the plane and sum them up
			float vSquaredSum = 0f;
			for(float[] vParticle : vFrameParticles) {
				float vDist = vParticle[0] * vNormalVec[0] +  vParticle[1] * vNormalVec[1] +  vParticle[2] * vNormalVec[2] - vD;
				vSquaredSum += (vDist * vDist);
			}
			vSquaredSum /= vParticleCounter;			
			mEntropies.setElementAt(vSquaredSum /* (float)Math.log10(vSquaredSum)*/, vFrameCounter - 1);
		}
		
//		int vF = 0;
//		for(float[] vm : mMeans) {
//			vF++;
//			System.out.println("mean at frame " + vF + ", "+ vm[0] + ", "+ vm[1] + ", "+ vm[2]);
//		}
		
	}
	
	public Vector<Float> getEntropies() {
		return mEntropies;
	}

	/**
	 * paints the metaphase plate at the current frame. If there are no informations stored for
	 * this frame, nothing is painted.
	 * @param aG A graphics object to paint on.
	 * @param aFrameIndex The current frame 1 <= aFrameIndex <= #frames
	 * @param aMagnification The magnification of the window.
	 */
	public void paint(Graphics aG, int aFrameIndex, int aMagnification) {
		if(mMeans.elementAt(aFrameIndex - 1) == null ||
				mPlanes.elementAt(aFrameIndex - 1) == null) {
			return;
		}
		aG.setColor(Color.green);
		//paint the center of mass.
		int vMeanX = (int)Math.round(mMeans.elementAt(aFrameIndex - 1)[1]);
		int vMeanY = (int)Math.round(mMeans.elementAt(aFrameIndex - 1)[0]);
		aG.fillRect(vMeanX - 2, vMeanY - 2, 4, 4);
		aG.drawLine(
				(int)Math.round(vMeanX - 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[0]), 
				(int)Math.round(vMeanY - 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[1]),
				(int)Math.round(vMeanX + 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[0]), 
				(int)Math.round(vMeanY + 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[1]));
	}
}

