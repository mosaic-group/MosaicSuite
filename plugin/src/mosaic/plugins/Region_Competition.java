package mosaic.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import mosaic.region_competition.*;
import mosaic.region_competition.netbeansGUI.GenericDialogGUI;
import mosaic.region_competition.netbeansGUI.InputReadable;
import mosaic.region_competition.netbeansGUI.InputReadable.LabelImageInitType;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
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
	JFrame cancelButton;
	
	String defaultInputFile= "C:/Users/Stephan/Desktop/BA/imagesAndPaper/icecream5_410x410.tif";
	
	
	boolean testMacroBug()
	{
		GenericDialog gd = new GenericDialog("test");
		gd.addTextAreas(null, null, 1, 1);
		gd.addNumericField("dummyfield", 0, 0);
		gd.addStringField("stringdield", "");
		gd.addCheckbox("show_me", true);
		
		gd.showDialog();
		
		gd.getNextText();
		gd.getNextText();
		gd.getNextNumber();
		gd.getNextString();
		boolean showme = gd.getNextBoolean();
		IJ.showMessage("showme was "+ showme);
		
		return true;
		
	}
	
	static void testNumbers()
	{
		Double d = 14.2;
		Integer i = 12;
		
		Number n1 = d;
		Number n2 = i;
//		boolean b = n1>n2; //error
		boolean b = d<i; // ok
		
		Comparable<Number> c1 = (Comparable<Number>)n1;
		Comparable c2 = (Comparable)n2;
		c1.compareTo(n2);
		c2.compareTo(c1);
	}
	
	public int setup(String aArgs, ImagePlus aImp)
	{
//		if(testMacroBug())
//			return DONE;
		
//		Connectivity.test();
//		UnitCubeCCCounter.test();
		
		settings = new Settings();
		MVC=this;
		
		originalIP = aImp;
		
		userDialog = new GenericDialogGUI(this);
		
//		if(userDialog!=null)
//		{
//			System.out.println("testing: abort for testing");
//			return DONE;
//		}
		
		boolean success=userDialog.processInput();
		if(!success)
		{
			return DONE;
		}
		
//		RegionIterator.tester();
//		IJ.showMessage("version 1");
		
		////////////////////
		
		
		frontsCompetitionImageFilter();

		return DOES_ALL + DONE;
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
//			String dir = IJ.getDirectory("current");
//			String fileName= "Clipboard01.png";
			//		String fileName= "icecream3_shaded_130x130.tif";
//			ip = o.openImage(dir+fileName);
			
			ip = o.openImage(defaultInputFile);
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
//		labelImage = LabelImage.getLabelImageNeg(this);
//		labelImage = LabelImage.getLabelImageFloatNeg(this);
//		labelImage = new LabelImage(MVC);
		labelImage.initZero();
		
		// Input Processing
		labelImage.settings.m_EnergyUseCurvatureRegularization = userDialog.useRegularization();
		
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
					labelImage.connectedComponents();
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
		boolean doSaveGuess = true;
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
			
			ImageProcessor labelImageProc = labelImage.getLabelImageProcessor();
			ImagePlus labelImPlus = new ImagePlus("dummy", labelImageProc);
			stack = labelImPlus.createEmptyStack();
			
			// add input image to stack
			// (convert it to the same type as labelImage)
			ImageProcessor originalProc = originalIP.getProcessor();
			Object originalPixels = null;
			if(labelImageProc instanceof FloatProcessor)
			{
				originalPixels = originalProc.convertToFloat().getPixelsCopy();
			} 
			else if(labelImageProc instanceof ShortProcessor)
			{
				originalPixels = originalProc.convertToShort(false).getPixelsCopy();
			} 
			else if(labelImageProc instanceof ColorProcessor)
			{
				originalPixels = originalProc.convertToRGB().getPixelsCopy();
			} 
			else
			{
				throw new RuntimeException("Unsupported LabelImage Format");
			}
			stack.addSlice("original", originalPixels);
			

			stackImPlus = new ImagePlus("Stack of "+originalIP.getTitle(), stack); 
			stackImPlus.show();
			
			// first stack image without boundary&contours
			addSliceToStackAndShow("init", initialLabelImageProcessor.getPixelsCopy());
			
			// next stack image is start of algo
			addSliceToStackAndShow("init", labelImage.getLabelImageProcessor().getPixelsCopy());
			
			IJ.setMinAndMax(stackImPlus, 0, maxLabel);
			IJ.run(stackImPlus, "3-3-2 RGB", null); // stack has to contain at least 2 slices so this LUT applies to all future slices.
		}
	}
	
	
	void initStopButton()
	{
		cancelButton = new JFrame();
		JPanel panel = new JPanel();
		
		JButton b = new JButton("Stop");
		b.setToolTipText("Stops Algorithm after current iteration");
		b.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				labelImage.stop();
//				cancelButton.setVisible(false);
				cancelButton.dispose();
			}
		});
//		p.setUndecorated(true);
		panel.add(b);
		cancelButton.add(panel);
		cancelButton.pack();
		cancelButton.setLocationByPlatform(true);
//		cancelButton.setLocationRelativeTo(IJ.getInstance());
//		java.awt.Point p = cancelButton.getLocation();
//		p.x-=150;
//		cancelButton.setLocation(p);
		cancelButton.setVisible(true);
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
		initStopButton();

		if(userDialog.getKBest()>0)
		{
			Timer t = new Timer();
			
			ArrayList<Long> list = new ArrayList<Long>();

			for(int i=0; i<userDialog.getKBest(); i++)
			{
				t.tic();
				labelImage.initMembers();
				labelImage.initWithImageProc(initialLabelImageProcessor);
				labelImage.initBoundary();
				labelImage.generateContour();
				
				labelImage.GenerateData();
				t.toc();
				list.add(t.lastResult());
				
				if(stackImPlus!=null)
				{
					IJ.setMinAndMax(stackImPlus, 0, labelImage.getBiggestLabel());
				}
//				stackImPlus.updateAndDraw();
				showFinalResult(labelImage, i);
			}
			
			System.out.println("--- kbest: (set in GenericDialogGui.kbest) ---");
			
			for(Long l:list)
			{
				System.out.println(l);
			}
			System.out.println("--- sorted ---");
			Collections.sort(list);
			for(Long l:list)
			{
				System.out.println(l);
			}
		}
		else // no kbest
		{
			labelImage.GenerateData();
			
			if(stackImPlus!=null)
			{
				IJ.setMinAndMax(stackImPlus, 0, labelImage.getBiggestLabel());
			}
			showFinalResult(labelImage);
		}
		

		if(userDialog.showStatistics())
		{
			labelImage.showStatistics();
		}
		
		cancelButton.dispose();
		
	}
	
	ImagePlus showFinalResult(LabelImage li, Object title)
	{
		ImageProcessor imProc = li.getLabelImageProcessor();
		imProc.abs();
		ImagePlus imp = new ImagePlus("ResultWindow "+title, imProc);
		IJ.setMinAndMax(imp, 0, li.getBiggestLabel());
		IJ.run(imp, "3-3-2 RGB", null);
		imp.show();
		
		return imp;
	}
	
	void showFinalResult(LabelImage li)
	{
		showFinalResult(li,"");
	}
	
	
	/**
	 * Initializes labelImage with ROI <br>
	 * If there was no ROI in input image, asks user to draw a roi. 
	 */
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
		labelImg.initBoundary();
		labelImg.connectedComponents();
		
//		originalIP.getWindow().addKeyListener(keyListener);
//		IJ.getInstance().addKeyListener(keyListener);
}
	
	
	
	private int maxLabel=100;
/**
 * Adds a new slice pixels to the end of the stack, 
 * and sets the new stack position to this slice
 * @param title		Title of the stack slice
 * @param pixels	data of the new slice (pixel array)
 */
	public void addSliceToStackAndShow(String title, Object pixels)
	{
		if(stackImPlus!=null)
		{
			stack = stackImPlus.getStack();
			
			stack.addSlice(title, pixels);
			stackImPlus.setStack(stack);
			stackImPlus.setPosition(stack.getSize());
			
			if(labelImage.getBiggestLabel()>maxLabel)
			{
				maxLabel*=2;
				IJ.setMinAndMax(stackImPlus, 0, maxLabel);
				IJ.run(stackImPlus, "3-3-2 RGB", null);
			}
		}
		else
		{
			// stack was closed by user
			System.out.println("stackImPlus is null");
		}

	}
	
	public ImagePlus getStackImPlus()
	{
		return this.stackImPlus;
	}
	
	public ImagePlus getOriginalImPlus()
	{
		return this.originalIP;
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
	 * does overwriting of static part in Connectivity work?
	 */
	void testConnStaticInheritance()
	{
		ConnectivityOLD conn = new ConnectivityOLD_2D_4();

		for(Point p : conn) {
			System.out.println(p);
		}
	}
	

	void testProcessors()
	{
		int width = 10;
		int height = 10;
		
		int index = 3;
		int value = -42;
		
		ImageProcessor p = new FloatProcessor(width, height);
		
		p.set(index, value);
		
		// high number
		int result = p.get(index);
		result = (int)p.get(index);
		
		// -43
		p.setf(index, value);
		float f = p.getf(index); // -42.0
		
		p.set(index, value);
		f = p.getf(index);		//NaN
		
		System.out.println(result);
	}
	
	
}


