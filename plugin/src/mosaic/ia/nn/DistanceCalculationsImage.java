package mosaic.ia.nn;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;


import mosaic.core.detection.Particle;
import mosaic.ia.gui.GUIDesign;
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
			
		Vector<Particle> particle =new Vector<Particle>();
		
	try{
	 particle = ImageProcessUtils.detectParticlesinStack(image);
	
	}
	catch(NullPointerException npe)
	{
		System.out.println("NPE caught");
	
	}
	
	
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
	//	DGrid=genD_grid();
		
		genStateDensityForImages();
		
		calcD();
		
	}
	
	

}
