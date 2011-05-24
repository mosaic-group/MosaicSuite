package mosaic.plugins;

import mosaic.region_competition.Connectivity2D_4;
import mosaic.region_competition.Connectivity;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import mosaic.region_competition.Timer;
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
		
		testStatistics();
		
		if (aImp == null) 
		{
			createEmptyIP();
			initializeLabelImageAndContourContainer();
		} else 
		{
			initWithCustom(aImp);
		}
		
		labelImage.showStatistics();
		macroContrast();
		return DOES_ALL + DONE;
	}
	
	void macroContrast()
	{
		IJ.run("Brightness/Contrast...");
		IJ.setMinAndMax(0, 2048);
		//call("ij.ImagePlus.setDefault16bitRange", 0);
		IJ.run("Close");
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
	
	
	void testStatistics()
	{
		ImagePlus ip = originalIP;
		ImageProcessor oProc = ip.getChannelProcessor();
		labelImage = new LabelImage(ip);
		labelImage.getLabelImage().insert(oProc, 0, 0);
		labelImage.initBoundary();
		labelImage.generateContour();
		
		Timer t = new Timer();
		
		for(int i=0; i<10; i++)
		{
			t.tic(); 
			labelImage.computeStatistics();
			long time=t.toc();
			System.out.println("compute time: "+time);
//			labelImage.showStatistics();
			
			t.tic();
			labelImage.renewStatistics();
			time=t.toc();
			System.out.println("renew time: "+time);
//			labelImage.showStatistics();
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
