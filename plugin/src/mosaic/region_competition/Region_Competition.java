package mosaic.plugins;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import mosaic.region_competition.Connectivity;
import mosaic.region_competition.ConnectivityOLD;
import mosaic.region_competition.ConnectivityOLD_2D_4;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Pair;
import mosaic.region_competition.Point;
import mosaic.region_competition.Timer;
import mosaic.region_competition.TopologicalNumberImageFunction;
import mosaic.region_competition.UnitCubeCCCounter;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
 * @author Stephan Semmler, ETH Zurich
 * @version 14.5.2011
 */

public class Region_Competition implements PlugInFilter{
		
	
	Region_Competition MVC;
	LabelImage labelImage;
	ImagePlus originalIP;
	ImageStack stack;
	ImagePlus stackImPlus;
	
	
	public int setup(String aArgs, ImagePlus aImp)
	{
		
		MVC=this;
		originalIP = aImp;

		////////////////////
		
		if(originalIP == null)
		{
			// try to open standard file
			String fileName= "Clipboard01.png";
			IJ.open(fileName);
			originalIP = WindowManager.getCurrentImage();
			
			while(originalIP == null) 
			{
				//file not found, open menu
//			IJ.showMessage("File "+fileName+" not found of incompatible, please select manually");
				IJ.open();
				originalIP = WindowManager.getCurrentImage();
			} 
			
		}
		
//		manualSelect();

		initStackIP();
		frontsCompetitionImageFilter();

		labelImage.showStatistics();
//		macroContrast();
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
	
	
	void frontsCompetitionImageFilter()
	{
		labelImage = new LabelImage(MVC);
		
		labelImage.initZero();
		labelImage.initBoundary();
//		labelImage.initialGuessGrowing(0.2);
		
//		labelImage.initialGuessGrowing(0.4);
		labelImage.initialGuessRandom();
		labelImage.generateContour();
		addSliceToStackAndShow("init", labelImage.getLabelImage().getPixelsCopy());

		labelImage.GenerateData();
		
//		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
		
	}
	
	
	void manualSelect()
	{
		IJ.showMessage("Select initial guesses (holding shift). press space to process");
		
		KeyListener keyListener = new KeyListener() 
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
//				System.out.println("code " + e.getKeyCode());
//				System.out.println("id " + e.getID());
//				System.out.println("char " + ((int)e.getKeyChar()));

				if(e.getKeyChar() == KeyEvent.VK_SPACE) 
				{
//					e.consume();
					Roi roi = originalIP.getRoi();
					
					ImageProcessor guessProcessor = new ShortProcessor(originalIP.getWidth(), originalIP.getHeight());
					ImagePlus guess = new ImagePlus("Guess", guessProcessor);
					
//					guess.show();
					
//					guess.setRoi(roi);
					guessProcessor.setValue(1);
					guessProcessor.fill(roi);
					
					// remove keylistener afterwards
					originalIP.getCanvas().removeKeyListener(this);
					
					labelImage = new LabelImage(MVC);
					labelImage.initWithIP(guess);
					labelImage.initBoundary();
					labelImage.generateContour();
					
					initStackIP();
					
					labelImage.GenerateData();
				}
			}
			@Override
			public void keyReleased(KeyEvent e){
				// TODO Auto-generated method stub
			}
			@Override
			public void keyPressed(KeyEvent e){
				// TODO Auto-generated method stub
			}
		};
		
		
		originalIP.getCanvas().addKeyListener(keyListener);
//		originalIP.getWindow().addKeyListener(keyListener);
//		IJ.getInstance().addKeyListener(keyListener);
}
	
	
	/**
	 * treats Input image as a label image / guess. 
	 * Does boundary, contour and statistics
	 */
	void initWithCustom(ImagePlus ip)
	{
		ImageProcessor oProc = ip.getChannelProcessor();
		labelImage = new LabelImage(MVC);
//		labelImage = oProc.duplicate();
		labelImage.getLabelImage().insert(oProc, 0, 0);
		labelImage.initBoundary();
		labelImage.generateContour();
		labelImage.computeStatistics();
		
		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
	}
	
	void initStackIP()
	{
		ImageProcessor myIp = originalIP.getProcessor();
		stack = new ImageStack(myIp.getWidth(), myIp.getHeight());
		stackImPlus = new ImagePlus("Stack of "+originalIP.getTitle(), myIp); 
		
		stack.addSlice("original", myIp.convertToShort(false).getPixelsCopy());
		
		stackImPlus.setStack(null, stack);
		stackImPlus.show();
	}
	
	public void addSliceToStackAndShow(String title, Object pixels)
	{
		if(stack==null)
		{
			System.out.println("stack is null");
			return;
		}
		stack.addSlice(title, pixels);
		stackImPlus.setStack(stack);
		stackImPlus.setPosition(stack.getSize());
	}

	public ImagePlus getStackImPlus()
	{
		return this.stackImPlus;
	}

	public ImagePlus getOriginalImPlus()
	{
		return this.originalIP;
	}

	void macroContrast()
	{
		IJ.run("Brightness/Contrast...");
		IJ.setMinAndMax(0, 2048);
		//call("ij.ImagePlus.setDefault16bitRange", 0);
		IJ.run("Close");
	}

	void testConnNew()
	{
		Connectivity conn = new Connectivity(2, 0);
		for(Point p : conn) {
			System.out.println(p);
		}
	}

	void testTopo()
	{
		
		IJ.open("rand.png");
		ImagePlus aImp = WindowManager.getCurrentImage();
		originalIP = aImp;
		
		LabelImage lbl = new LabelImage(MVC);
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
		 * compares runtime of recursive and explicit statistics
		 */
		void testStatistics()
		{
			ImagePlus ip = originalIP;
			ImageProcessor oProc = ip.getChannelProcessor();
			labelImage = new LabelImage(MVC);
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
	 * does overwriting of static part in Connectivity work?
	 */
	void testConnStaticInheritance()
	{
		ConnectivityOLD conn = new ConnectivityOLD_2D_4();

		for(Point p : conn) {
			System.out.println(p);
		}
	}
	
	void testCCC()
	{
//		Connectivity connFG = new Connectivity(2, 1);
//		Connectivity connBG = new Connectivity(2, 0);
		
		Connectivity connFG = new Connectivity(3, 1);
		Connectivity connBG = new Connectivity(3, 0);
		
		
		UnitCubeCCCounter cube = new UnitCubeCCCounter(connFG, connBG);
		
		boolean[][] orig=UnitCubeCCCounter.UnitCubeNeighbors(cube, connFG, connBG);
		System.out.println("original:");
		System.out.println(orig);
		
		boolean[][] sts=cube.UnitCubeNeighborsSTS(cube, connFG, connBG);
		System.out.println("sts:");
		System.out.println(sts);
		
		System.out.println("end");
		
		
		
	}


}
