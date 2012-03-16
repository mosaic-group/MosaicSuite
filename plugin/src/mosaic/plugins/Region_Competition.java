package mosaic.plugins;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mosaic.region_competition.*;
import mosaic.region_competition.netbeansGUI.GenericDialogGUI;
import mosaic.region_competition.netbeansGUI.InputReadable;
import mosaic.region_competition.netbeansGUI.InputReadable.LabelImageInitType;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.io.FileInfo;
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
	public Settings settings;
	LabelImage labelImage;		// data structure mapping pixels to labels
	ImagePlus originalIP;		// IP of the input image
	ImageStack stack;			// stack saving the segmentation progress images
	ImagePlus stackImPlus;		// IP showing the stack
	
	ImageProcessor initialLabelImageProcessor; // copy of the initial guess (without contour/boundary)
	
	public InputReadable userDialog;
	
	
	public int setup(String aArgs, ImagePlus aImp)
	{
		settings = new Settings();
		MVC=this;
		originalIP = aImp;
		
		userDialog = new GenericDialogGUI(settings);
		boolean success=userDialog.processInput();
		if(!success)
		{
			return DONE;
		}

		
//		RegionIterator.tester();
//		IJ.showMessage("version 1");
		
		/*
		userDialog = new UserDialog(settings);
//		userDialog.showDialog();
		userDialog.showNetbeans();
		*/
		
		////////////////////

		
//		IJ.run("8-bit");
//		originalIP.changes=false;
//		System.out.println("statmax "+ originalIP.getStatistics().max);
//		System.out.println("statmin "+ originalIP.getStatistics().min);
		
		
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
	
	void initInputImage()
	{
		
		ImagePlus ip = null;
		Opener o = new Opener();
		
		// first try: filepath of inputReader
		String file = userDialog.getInputImageFilename(); 
		if(file!=null && !file.isEmpty())
		{
			ip = o.openImage(file);
		}
		
		// next try: opened image
		if(ip==null)
		{
			ip=originalIP;
		}
		
		//debug
		// next try: default image
		if(ip==null)
		{
			String dir= IJ.getDirectory("current");
			String fileName= "Clipboard01.png";
			//		String fileName= "icecream3_shaded_130x130.tif";
			ip=o.openImage(dir+fileName);
		}
			
		if(ip!=null)
		{
			originalIP = ip;
			// image loaded
			boolean showOriginal = true;
			if(showOriginal)
			{
				ip.show();
			}
		}
		
		if(ip==null)
		{
			// failed to load anything
			originalIP=null;
			//TODO maybe show image opener dialog
			throw new RuntimeException("Failed to load an input image.");
		}

	}
	
	void initLabelImage()
	{
		labelImage = new LabelImage(MVC);
		labelImage.initZero();
		
		// Input Processing
		labelImage.settings.m_EnergyUseCurvatureRegularization=userDialog.useRegularization();
		
		LabelImageInitType input = userDialog.getLabelImageInitType();
		
		switch(input)
		{
			case UserDefinedROI:
			{
				System.out.println("manualSelect");
				manualSelect(labelImage);
				break;
			}
			case Rectangle:
			{
				labelImage.initialGuessGrowing(0.8);
				break;
			}
			case Ellipses:
			{
				labelImage.initialGuessRandom();
				break;
			}
			case Bubbles:
			{
				labelImage.initialGuessBubbles();
				break;
			}
			case File:
			{
				ImagePlus ip=null;
				
				String fileName = userDialog.getLabelImageFilename();
				if(fileName!=null && !fileName.isEmpty())
				{
					Opener o = new Opener();
					ip = o.openImage(fileName);
				}
				
				if(ip!=null){
					labelImage.initWithIP(ip);
					labelImage.connectedComponents(labelImage);
				} else {
					labelImage=null;
					throw new RuntimeException("Failed to load LabelImage");
				}
	
				break;
			}
			default:
			{
				// was aborted
				System.out.println("no valid input option in User Input. Abort");
				labelImage = null;
				return;
	// throw new RuntimeException("no valid input option in User Input. Abort");
			}
		}
		
		initialLabelImageProcessor = labelImage.getLabelImageProcessor().duplicate();
		
		// save the initial guess (random/user defined/whatever) to a tiff
		// so we can reuse it for debugging
		boolean doSaveGuess=true;
		if(doSaveGuess)
		{
//			String s = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
//			System.out.println(s);
			FileInfo fi = originalIP.getOriginalFileInfo();
			if(fi!=null)
			{
				String d = fi.directory;
				ImagePlus ip = new ImagePlus("", labelImage.getLabelImageProcessor());
				FileSaver fs = new FileSaver(ip);
				fs.saveAsTiff(d+"initialLabelImage.tiff");
			}
			else
			{
				System.out.println("image was created using file/new. initial label was not saved");
			}
		}
		
		labelImage.initBoundary();
		labelImage.generateContour();
				
	}
	
	void initStack()
	{
		if(userDialog.useStack())
		{
			
			ImageProcessor myIp = originalIP.getProcessor();
//			ImageProcessor myIp = originalIP.getProcessor().convertToShort(false);

			stack= originalIP.createEmptyStack();
//			stack = new ImageStack(myIp.getWidth(), myIp.getHeight());
			stack.addSlice("original", originalIP.getProcessor().convertToShort(false).getPixelsCopy());

			stackImPlus = new ImagePlus("Stack of "+originalIP.getTitle(), stack); 
					
			stackImPlus.show();
			
//			stack.addSlice("original", myIp.convertToShort(false).getPixelsCopy());
//			addSliceToStackAndShow("original", myIp.convertToShort(false).getPixelsCopy());
			
			// first stack image without boundary&contours
			addSliceToStackAndShow("init", initialLabelImageProcessor.getPixelsCopy());
			
			// next stack image is start of algo
			addSliceToStackAndShow("init", labelImage.getLabelImageProcessor().getPixelsCopy());
			
			IJ.run("3-3-2 RGB"); // stack has to contain at least 2 slices so this LUT applies to all future slices.
//					LutLoader lutloader = new LutLoader();
//					lutloader.run("3-3-2 RGB");
		}
	}
	

	void frontsCompetitionImageFilter()
	{
		
		initInputImage();
		
		initLabelImage();
		if(labelImage==null) // input was aborted
		{
			return;
		}
		initStack();

		if(userDialog.getKBest()>0)
		{
			
			Timer t = new Timer();
			
			ArrayList<Long> list = new ArrayList<Long>();

			for(int i=0; i<userDialog.getKBest(); i++)
			{
				t.tic();
				labelImage = new LabelImage(MVC);
				
				labelImage.initWithImageProc(initialLabelImageProcessor);
				labelImage.initBoundary();
				labelImage.generateContour();
				
				labelImage.GenerateData();
				t.toc();
				list.add(t.lastResult());
				showFinalResult(labelImage, i);
			}
			
			System.out.println("sorted kbest: (set in GenericDialogGui.kbest)");
			
			Collections.sort(list);
			for(Long l:list)
			{
				System.out.println(l);
			}
		}
		else
		{
			labelImage.GenerateData();
			showFinalResult(labelImage);
		}
		
		

		if(userDialog.showStatistics())
		{
			labelImage.showStatistics();
		}
		
//		macroContrast();
//		new ImagePlus("LabelImage", labelImage.getLabelImage()).show();
		
	}
	
	void showFinalResult(LabelImage li, Object title)
	{
		ImagePlus imp = new ImagePlus("ResultWindow "+title, li.getLabelImageProcessor());
		imp.show();
		IJ.run("3-3-2 RGB");
	}
	
	void showFinalResult(LabelImage li)
	{
		showFinalResult(li,"");
	}
	
	
	void manualSelect(final LabelImage labelImg)
	{
		
		Roi roi=null;
		roi = originalIP.getRoi();
		if(roi==null)
		{
			System.out.println("no ROIs yet. Get from UserInput");
	//		IJ.showMessage("Select initial guesses (holding shift). press space to process");
			
			ImageCanvas canvas = originalIP.getCanvas();
			
			// save old keylisteners, remove them (so we can use all keys to select guess ROIs)
			KeyListener[] kls = canvas.getKeyListeners();
			for(KeyListener kl: kls)
			{
				canvas.removeKeyListener(kl);
			}
			
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
			canvas.addKeyListener(keyListener);
			
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
		}
		
		// now we have a roi
		
		labelImg.getLabelImageProcessor().setValue(1);
		labelImg.getLabelImageProcessor().fill(roi);
		labelImg.connectedComponents(labelImg);
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
		labelImage.getLabelImageProcessor().insert(oProc, 0, 0);
		labelImage.initBoundary();
		labelImage.generateContour();
		labelImage.computeStatistics();
		
		new ImagePlus("LabelImage", labelImage.getLabelImageProcessor()).show();
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
		else
		{
			stack.addSlice(title, pixels);
			stackImPlus.setStack(stack);
			stackImPlus.setPosition(stack.getSize());
//			stackImPlus.show();
//		stackImPlus.getStack().addSlice(title, pixels);
		}
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
		labelImage.getLabelImageProcessor().insert(oProc, 0, 0);
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


