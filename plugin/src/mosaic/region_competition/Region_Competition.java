package mosaic.plugins;

import mosaic.region_competition.Connectivity2D_4;
import mosaic.region_competition.Connectivity;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
 * @author Stephan Semmler, ETH Zurich
 * @version 14.5.2011
 */

public class Region_Competition implements PlugInFilter{
		
	LabelImage labelImage;
	ImagePlus originalIP;
	
	public int setup(String aArgs, ImagePlus aImp) 
	{
		originalIP = aImp;
		
		if (aImp == null) {
			createEmptyIP();
			initializeLabelImageAndContourContainer();
		} else {
			initWithCustom(aImp);
		}
		
		labelImage.showStatistics();
		
		return DOES_ALL + DONE;
	}
	
	
	void createEmptyIP()
	{
		IJ.showMessage("No image selected. Debug mode, create empty 400*300 image");
		originalIP = new ImagePlus("empty test image", new ShortProcessor(400, 300));
	}
	
	public void run(ImageProcessor aImageProcessor) 
	{
		
	}
	
	
	/**
	 * does overwriting of static part in Connectivity work?
	 */
	void testConnStaticInheritance()
	{
		Connectivity conn;
		conn = new Connectivity2D_4();
		
		for(Point p:conn)
		{
			System.out.println(p);
		}
	}
	
	/**
	 * treats Input image as a label image / guess. 
	 * Does boundary, contour and statistics
	 */
	void initWithCustom(ImagePlus ip)
	{
		ImageProcessor oProc = ip.getChannelProcessor();
		labelImage = new LabelImage(ip);
//		labelImage = oProc.duplicate();
		labelImage.getLabelImage().insert(oProc, 0, 0);
		labelImage.initBoundary();
		labelImage.generateContour();
		labelImage.computeStatistics();
		
		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
	}
	
	/** 
	 * Generates a labelImage filled with bgLabel and a one-pixel boundary of value forbiddenLabel
	 */
	private void initializeLabelImageAndContourContainer() 
	{
		labelImage = new LabelImage(originalIP);
		labelImage.initZero();
		labelImage.initBoundary();
		labelImage.initialGuess();
		labelImage.generateContour();
		labelImage.computeStatistics();
		
		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
	}


}
