package mosaic.plugins;


import java.awt.Graphics;
import java.util.Vector;

import Jama.Matrix;

public class Metaphase_Detector2 /*extends ParticleTracker3D_ implements PlugInFilter*/{
	
	Vector<Float> mEntropies = new Vector<Float>();
	Vector<float[]> mPlanes = new Vector<float[]>();
	
	public Metaphase_Detector2(Vector<Vector<float[]>> aParticles) {
		int vFrameCounter = 0;
		mEntropies.setSize(aParticles.size());
		mPlanes.setSize(aParticles.size());
		for(Vector<float[]> vFrameParticles : aParticles) {
			vFrameCounter++;
			double[][] vParticleValues = new double[vFrameParticles.size()][4];
			double[][] vResVec = new double[4][1];
			int vParticleCounter = 0;
			for(float[] vParticle : vFrameParticles) {
				vParticleValues[vParticleCounter][0] = vParticle[0];
				vParticleValues[vParticleCounter][1] = vParticle[1];
				vParticleValues[vParticleCounter][2] = vParticle[2];
				vParticleValues[vParticleCounter][3] = -1.0;
				vParticleCounter++;
			}
			vResVec[0][0] = 0.0;
			vResVec[1][0] = 0.0;
			vResVec[2][0] = 0.0;
			vResVec[3][0] = 0.0;
			
			Matrix vParticleValuesMatrix = new Matrix(vParticleValues);
			Matrix vParticleValuesMatrixT = vParticleValuesMatrix.transpose();
			Matrix vAMatrix = vParticleValuesMatrixT.times(vParticleValuesMatrix);
			Matrix vResMatrix = new Matrix(vResVec);
			System.out.println("Matrix A ");
			vAMatrix.print(7, 3);
			System.out.println("null vector");
			vResMatrix.print(7, 3);
			Matrix vNormalVec = vAMatrix.solve(vResMatrix);
			System.out.println("solution");
			vNormalVec.print(7, 3);
		
			
			//now we have the explicit form of the plane equation. 
			
			
			float vNormalizer = (float)Math.sqrt(
					vNormalVec.get(0, 0) * vNormalVec.get(0, 0) +
					vNormalVec.get(1, 0) * vNormalVec.get(1, 0) +
					vNormalVec.get(2, 0) * vNormalVec.get(2, 0));
			
			vNormalVec.set(0,0, vNormalVec.get(0, 0) / vNormalizer);
			vNormalVec.set(1,0, vNormalVec.get(1, 0) / vNormalizer);
			vNormalVec.set(2,0, vNormalVec.get(2, 0) / vNormalizer);
			vNormalVec.set(3,0, vNormalVec.get(3, 0) / vNormalizer);
			
			float[] vPlane = new float[4];
			vPlane[0] = (float)vNormalVec.get(0, 0);
			vPlane[1] = (float)vNormalVec.get(1, 0);
			vPlane[2] = (float)vNormalVec.get(2, 0);
			vPlane[3] = (float)vNormalVec.get(3, 0);
			mPlanes.setElementAt(vPlane, vFrameCounter -1);
			
			//for each particle, calculate the distance to the plane and sum them up
			float vSquaredSum = 0f;
			for(float[] vParticle : vFrameParticles) {
				float vDist = (float)(vParticle[0] * vNormalVec.get(0, 0) +  vParticle[1] * vNormalVec.get(1, 0) +  vParticle[2] * vNormalVec.get(2, 0) - vNormalVec.get(3, 0));
				vSquaredSum += (vDist * vDist);
			}
			vSquaredSum /= vParticleCounter;			
			mEntropies.setElementAt(vSquaredSum /* (float)Math.log10(vSquaredSum)*/, vFrameCounter - 1);
		}
		
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
//		if(mMeans.elementAt(aFrameIndex - 1) == null ||
//				mPlanes.elementAt(aFrameIndex - 1) == null) {
//			return;
//		}
//		aG.setColor(Color.green);
//		//paint the center of mass.
//		int vMeanX = (int)Math.round(mMeans.elementAt(aFrameIndex - 1)[1]);
//		int vMeanY = (int)Math.round(mMeans.elementAt(aFrameIndex - 1)[0]);
//		aG.fillRect(vMeanX - 2, vMeanY - 2, 4, 4);
//		aG.drawLine(
//				(int)Math.round(vMeanX - 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[0]), 
//				(int)Math.round(vMeanY - 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[1]),
//				(int)Math.round(vMeanX + 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[0]), 
//				(int)Math.round(vMeanY + 50 * aMagnification * mPlanes.elementAt(aFrameIndex - 1)[1]));
	}
}

