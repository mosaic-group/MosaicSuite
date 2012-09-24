package mosaic.ia.nn;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;


import mosaic.core.detection.Particle;
import mosaic.ia.utils.ImageProcessUtils;

public class DistanceCalculationsImage extends DistanceCalculations {
	
	



	private ImagePlus X,Y;
	public DistanceCalculationsImage(ImagePlus X,ImagePlus Y,ImagePlus mask, double gridSize) {
		super(mask, gridSize);
		this.X=X;
		this.Y=Y;
			
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	
	private Point3d [] extractParticles(ImagePlus image)
	{
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

	
	

	private float [] genD_grid()
	{
		
			return genCubeGridDist(0,0,0,X.getHeight()-1,X.getWidth()-1,X.getNSlices()-1);
			
		
	}



	@Override
	public void calcDistances() {

		particleXSetCoord = extractParticles(X);
		particleYSetCoord = extractParticles(Y);
		DGrid=genD_grid();
		calcD();
		
	}
	
	

}
