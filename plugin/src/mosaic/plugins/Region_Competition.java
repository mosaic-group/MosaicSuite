package mosaic.plugins;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import mosaic.region_competition.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;


/**
 * @author Stephan Semmler, ETH Zurich
 * @version 14.5.2011
 */

public class Region_Competition implements PlugInFilter
{
		
	Region_Competition MVC;		// interface to image application (imageJ)
	LabelImage labelImage;		// data structure mapping pixels to labels
	ImagePlus originalIP;		// IP of the input image
	ImageStack stack;			// stack saving the segmentation progress images
	ImagePlus stackImPlus;		// IP showing the stack
	
	UserDialog userDialog;
	
	
	public int setup(String aArgs, ImagePlus aImp)
	{
		
//		IJ.showMessage("version 2011 11 15");
		
		userDialog = new UserDialog("starterpanel");
		userDialog.showDialog();
		
				
		MVC=this;
		originalIP = aImp;

		////////////////////
		
		if(originalIP == null)
		{
			// try to open standard file
			String fileName= "Clipboard01.png";
//			String fileName= "icecream3_shaded_130x130.tif";
			IJ.open(fileName);
			originalIP = WindowManager.getCurrentImage();
			
			while(originalIP == null) 
			{
				//file not found, open menu
//			IJ.showMessage("File "+fileName+" not found or incompatible, please select manually");
				IJ.open();
				originalIP = WindowManager.getCurrentImage();
			} 
			
		}
		
		frontsCompetitionImageFilter();

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
		
		
		// Input Processing
		labelImage.settings.m_EnergyUseCurvatureRegularization=userDialog.useRegularization();
		
		if(userDialog.doUser)
		{
			System.out.println("manualSelect");
			manualSelect(labelImage);
		}
		else if(userDialog.doRand)
		{
			labelImage.initialGuessRandom();
		}
		else if(userDialog.doFile)
		{
			Opener o = new Opener();
			ImagePlus ip = o.openImage(userDialog.filename);
			
			labelImage.initWithIP(ip);
		}
		else
		{
	//		labelImage.initialGuessGrowing(0.2);
			labelImage.initialGuessGrowing(0.4);
	//		
	//		labelImage.initialGuessEllipsesFromString("4 " +
	//				"152 79 147 12 1 " +
	//				"130 91 117 79 2 " +
	//				"78 18 40 10 3 " +
	//				"134 107 88 104 4 ");
		}
		
		
		initStackIP();
		// first stack image without boundary&contours
		addSliceToStackAndShow("init", labelImage.getLabelImage().getPixelsCopy());
		

		// save the initial guess (random/user defindet/whatever) to a tiff
		// so we can reuse it for debugging
		boolean doSave=true;
		if(doSave)
		{
//			String s = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
//			System.out.println(s);
			String d = originalIP.getOriginalFileInfo().directory;
			ImagePlus ip = new ImagePlus("", stackImPlus.getProcessor());
			FileSaver fs = new FileSaver(ip);
			fs.saveAsTiff(d+"initialLabelImage.tiff");
		}
				
		labelImage.initBoundary();
		labelImage.generateContour();
		// next stack image is start of algo
		addSliceToStackAndShow("init", labelImage.getLabelImage().getPixelsCopy());

		IJ.run("3-3-2 RGB"); // stack has to contain at least 2 slices so this LUT applies to all future slices.

		labelImage.GenerateData();
		
//		LutLoader lutloader = new LutLoader();
//		lutloader.run("3-3-2 RGB");

		labelImage.showStatistics();
//		macroContrast();
		
//		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
		
	}
	
	
	void manualSelect(final LabelImage labelImg)
	{
//		IJ.showMessage("Select initial guesses (holding shift). press space to process");
		
		KeyListener keyListener = new KeyListener() 
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
//				System.out.println("code " + e.getKeyCode());
//				System.out.println("id " + e.getID());
//				System.out.println("char " + ((int)e.getKeyChar()));

//				if(e.getKeyChar() == KeyEvent.VK_SPACE) 
				{
//					e.consume();
					
					synchronized(labelImg) 
					{
						labelImg.notifyAll();
					}
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
		
		ImageCanvas canvas = originalIP.getCanvas();
		
		// save old keylisteners, remove them (so we can use all keys to select guess ROIs)
		KeyListener[] kls = canvas.getKeyListeners();
		for(KeyListener kl: kls)
		{
			canvas.removeKeyListener(kl);
		}
		
		canvas.addKeyListener(keyListener);
		
		Roi roi=null;
		// try to get a ROI from user
		while(roi==null)
		{
			synchronized(labelImg)
			{
				try {
					System.out.println("Waiting for user input (pressing space");
					labelImg.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println("end waiting (button was pressed)");
				
				roi = originalIP.getRoi();
				if(roi==null)
				{
					IJ.showMessage("No ROI selcted. maybe wrong window");
				}
			}
		}
		//we have a roi, remove keylistener and reattach the old ones
		canvas.removeKeyListener(keyListener);
		for(KeyListener kl: kls)
		{
			canvas.addKeyListener(kl);
		}
		
		labelImg.getLabelImage().setValue(1);
		labelImg.getLabelImage().fill(roi);
//		labelImg.initBoundary();
		
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
	
	/**
	 * Initializes the stackIP, putting the originalIP in the first slice
	 */
	void initStackIP()
	{
		ImageProcessor myIp = originalIP.getProcessor();
//		ImageProcessor myIp = originalIP.getProcessor().convertToShort(false);

//		stack= originalIP.createEmptyStack();
		stack = new ImageStack(myIp.getWidth(), myIp.getHeight());
		stackImPlus = new ImagePlus("Stack of "+originalIP.getTitle(), myIp); 
		
		stack.addSlice("original", myIp.convertToShort(false).getPixelsCopy());
				
		stackImPlus.setStack(null, stack);
		stackImPlus.show();
	}
	
/**
 * Adds a new slice pixels to the end of the stack, 
 * and sets the new stack position to this slice
 * @param title		Title of the stack slice
 * @param pixels	data of the new slice (pixel array)
 */
	public void addSliceToStackAndShow(String title, Object pixels)
	{
		if(stack==null)
		{
			System.out.println("stack is null");
			return;
		}
//		stackImPlus.getStack().addSlice(title, pixels);
		
		stack.addSlice(title, pixels);
		stackImPlus.setStack(stack);
		stackImPlus.setPosition(stack.getSize());
	}
	
	/**
	 * Debug: selects a point in the current image to see it faster in the GUI.
	 */
	public void selectPoint(Point p)
	{
		//TODO multidim
//		stackImPlus.setRoi(p.x[0], p.x[1], 1, 1);
		IJ.makePoint(p.x[0], p.x[1]);
//		stackImPlus.draw();
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
		int[] xs = new int[]{10,10};
		Point index = new Point(xs);
		
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


