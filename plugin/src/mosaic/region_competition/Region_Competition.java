package mosaic.plugins;

import java.util.List;

import mosaic.region_competition.Connectivity;
import mosaic.region_competition.ConnectivityOLD;
import mosaic.region_competition.ConnectivityOLD_2D_4;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Pair;
import mosaic.region_competition.Point;
import mosaic.region_competition.Timer;
import mosaic.region_competition.TopologicalNumberImageFunction;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
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
	ImageStack stack;
	
	
	public int setup(String aArgs, ImagePlus aImp)
	{

		//tests
		
//		testConnNew();
//		testTopo();
		
		////////////////////
		
		IJ.open("Clipboard01.png");
		aImp = WindowManager.getCurrentImage();
		originalIP = aImp;
		
		if(aImp == null) {
			createEmptyIP();
			testStatistics();
			initializeLabelImageAndContourContainer();
		} 
		else 
		{
			//stack
			ImageProcessor myIp = aImp.getProcessor();
			stack = new ImageStack(myIp.getWidth(), myIp.getHeight());
			
//			stack.addSlice("Original", myIp);
		frontsCompetitionImageFilter();
			
			ImagePlus myTargetImPlus = new ImagePlus("Stack of "+aImp.getTitle(), myIp);	//The .getTitle() method, recuperates the title of the image. 
			myTargetImPlus.show();
			myTargetImPlus.setStack(null, stack);		
		}

//		labelImage.showStatistics();
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
		ConnectivityOLD conn = new ConnectivityOLD_2D_4();
		
		for(Point p:conn)
		{
			System.out.println(p);
		}
	}
	
	/**
	 * compares runtime of recursive and explicit statistics
	 */
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
	
	
	void frontsCompetitionImageFilter()
	{
		ImagePlus ip = originalIP;
		
		labelImage = new LabelImage(ip);
		labelImage.initZero();
		labelImage.initBoundary();
		labelImage.initialGuess();
		labelImage.generateContour();
		
//		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
		
		labelImage.setStack(stack);
//	stack.addSlice("ip", ip.getProcessor());
//	stack.addSlice("Original", labelImage.getLabelImage());
		labelImage.GenerateData();
//	stack.addSlice("Original2", labelImage.getLabelImage());
//	stack.addSlice("Final", labelImage.getLabelImage());
		
		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
		
	}
	
	
	void testConnNew()
	{
		
		Connectivity conn= new Connectivity(2,0);
		for(Point p : conn)
		{
			System.out.println(p);
		}
		
		
	}
	
	void testTopo()
	{
		
		IJ.open("rand.png");
		ImagePlus aImp = WindowManager.getCurrentImage();
		originalIP = aImp;
		
		LabelImage lbl = new LabelImage(aImp);
		//debug
		lbl.initWithIP(aImp);
		Point index = new Point(10,10);
		
		
		Connectivity connFG = new Connectivity(2,1);
		Connectivity connBG = new Connectivity(2,0);
		
		TopologicalNumberImageFunction topo = new TopologicalNumberImageFunction(lbl, connFG, connBG);
		List<Pair<Integer, Pair<Integer, Integer>>> result = topo.EvaluateAdjacentRegionsFGTNAtIndex(index);
		
		System.out.println(result);
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
