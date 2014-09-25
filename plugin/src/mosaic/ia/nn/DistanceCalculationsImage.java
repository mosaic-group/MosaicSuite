package mosaic.ia.nn;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;


import mosaic.core.detection.Particle;
import mosaic.ia.utils.ImageProcessUtils;

public class DistanceCalculationsImage extends DistanceCalculations {
	
	



	private ImagePlus X,Y;
	public DistanceCalculationsImage(ImagePlus X,ImagePlus Y,ImagePlus mask, double gridSize,double kernelWeightq,int discretizationSize) {
		super(mask, gridSize,kernelWeightq,discretizationSize);
		this.X=X;
		this.Y=Y;
			
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	
	private Point3d [] extractParticles(ImagePlus image)
	{
		
		Calibration calibration =image.getCalibration();
		
		zscale=calibration.pixelDepth;
		xscale=calibration.pixelHeight;
		yscale=calibration.pixelWidth;
			
		// X and Y should be 1.0 Z any
		
		yscale /= xscale;
		zscale /= xscale;
		xscale = 1.0;
		
		Vector<Particle> particle =new Vector<Particle>();
		
		particle = ImageProcessUtils.detectParticlesinStack(image);
		
		return applyMaskandgetCoordinates(ImageProcessUtils.getCoordinates(particle));
	}

	
	

	private void genStateDensityForImages()
	{
		
		stateDensity(0,0,0,X.getHeight()-1,X.getWidth()-1,X.getNSlices()-1);
			
		
	}



	@Override
	public void calcDistances() {

		particleXSetCoord = extractParticles(X);
		particleYSetCoord = extractParticles(Y);
//		ImageProcessUtils.saveCoordinates(particleXSetCoord, particleYSetCoord);
		
	//	DGrid=genD_grid();
		genStateDensityForImages();
		calcD();
		
	}
	
	

}
