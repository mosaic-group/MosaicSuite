package mosaic.plugins;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JFrame;

//import view4d.Timeline;

import mosaic.region_competition.*;
import mosaic.region_competition.netbeansGUI.ControllerFrame;
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
import ij.gui.StackWindow;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij3d.Image3DUniverse;


/**
 * @author Stephan Semmler, ETH Zurich
 * @version 2012.06.11
 */

public class Region_Competition implements PlugInFilter
{
	Region_Competition MVC;		// interface to image application (imageJ)
	public Settings settings;
	
	Algorithm algorithm;
	LabelImage labelImage;		// data structure mapping pixels to labels
	IntensityImage intensityImage; 
	private ImagePlus originalIP;		// IP of the input image
	
//	ImagePlus dataNormalizedIP;		// input image scaled to [0,1]
	ImageStack stack;			// stack saving the segmentation progress images
	ImagePlus stackImPlus;		// IP showing the stack
	
	ImageProcessor initialLabelImageProcessor; // copy of the initial guess (without contour/boundary)
	
	public InputReadable userDialog;
	JFrame controllerFrame;
	
	static String defaultInputFile;
	static
	{
		String defaultDir = "C:/Users/Stephan/Desktop/BA/";
		String defaultFile ="";
		defaultFile = "imagesAndPaper/Cell1_nuclei_w460.tif";
		defaultFile = "sphere_3d_z56.tif";
		defaultFile = "imagesAndPaper/icecream5_410x410.tif";
		defaultFile = "/imagesAndPaper/icecream_shaded_130x130.tif";
		defaultFile = "imagesAndPaper/sphere_3d.tif";
		
		defaultFile = "imagesAndPaper/JZ_111207_SchMax_T_t0051-1.tif";
		
		defaultInputFile = defaultDir + defaultFile;
		
	}

	
	
	public int setup(String aArgs, ImagePlus aImp)
	{
//		if(testMacroBug())
//			return DONE;
		
//		Connectivity.test();
//		UnitCubeCCCounter.test();
		
		
		// load
		
		String dir = IJ.getDirectory("temp");
		String file = dir+"/rc_settings.dat";
		System.out.println(file);
		
		try
		{
			FileInputStream fin = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fin);
			settings = (Settings)ois.readObject();
			ois.close();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Settings File not found "+file);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
		
		if(settings == null){
			settings = new Settings();
		}
		
		MVC = this;
		originalIP = aImp;
		userDialog = new GenericDialogGUI(this);
		
		
		//TODO ugly
		((GenericDialogGUI)userDialog).showDialog();
		boolean success=userDialog.processInput();
		if(!success)
		{
			return DONE;
		}
		

		// save
		try
		{
			FileOutputStream fout = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(settings);
			oos.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		
//		RegionIterator.tester();
//		IJ.showMessage("version 1");
		
		////////////////////
		
		
		try
		{
			frontsCompetitionImageFilter();
		}
		catch (Exception e)
		{
			if(controllerFrame!=null)
				controllerFrame.dispose();
			e.printStackTrace();
		}

		return DONE;
	}
	
	
	public void run(ImageProcessor aImageProcessor) 
	{
	}
	
	
	void initAlgorithm()
	{
		algorithm = new Algorithm(intensityImage, labelImage, settings, this);
	}
	
	
	void initInputImage()
	{
		
		ImagePlus ip = null;
		
		String file = userDialog.getInputImageFilename();
		ImagePlus choiceIP = (ImagePlus)userDialog.getInputImage();
		
		// first try: filepath of inputReader
		if(file!=null && !file.isEmpty())
		{
			Opener o = new Opener();
			ip = o.openImage(file);
		}
		else // selected opened file
		{
			ip = choiceIP;
		}
		
		// next try: opened image
		if(ip==null)
		{
			ip=originalIP;
			
			// manually open image in a new frame.
//			if(ip!=null)
//			{
//				Image image = ip.getImage();
//				JFrame f = new JFrame();
//				JPanel p = new JPanel();
//				f.add(p);
//				
//				JLabel jl = new JLabel(new ImageIcon(image));
//				p.add(jl);
//				f.pack();
//				f.show();
//			}
		}
		
		//debug
		// next try: default image
		if(ip==null)
		{
//			String dir = IJ.getDirectory("current");
//			String fileName= "Clipboard01.png";
			//		String fileName= "icecream3_shaded_130x130.tif";
//			ip = o.openImage(dir+fileName);
			Opener o = new Opener();
			ip = o.openImage(defaultInputFile);
		}
		
			
		if(ip!=null)
		{
			originalIP = ip;
			
			intensityImage = new IntensityImage(originalIP);
//			dataNormalizedIP = new ImagePlus("Normalized Input Image", stack);
			
			// image loaded
			boolean showOriginal = true;
			if(showOriginal)
			{
				originalIP.show();
			}
			
			if(userDialog.showNormalized())
			{
				intensityImage.imageIP.show();
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
		labelImage = new LabelImage(intensityImage.getDimensions());
		
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
				ImagePlus choiceIP = (ImagePlus)userDialog.getLabelImage();
				
			
				// first priority: filename was entered
				if(fileName!=null && !fileName.isEmpty())
				{
					Opener o = new Opener();
					ip = o.openImage(fileName);
				}
				else // no filename. fileName == null || fileName()
				{
					ip = choiceIP;
				}
				
				if(ip!=null){
					labelImage.initWithIP(ip);
					labelImage.initBoundary();
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
				labelImage = null;
				throw new RuntimeException("No valid input option in User Input. Abort");
			}
		}
		
		if(labelImage == null)
		{
			throw new RuntimeException("Not able to build a LabelImage.");
		}
		
		
//		TODO sts 3D_comment
//		if(labelImage.getDim()==2)
		{
			initialLabelImageProcessor = labelImage.getLabelImageProcessor().duplicate();
		}
		
		saveInitialLabelImage();
		
		labelImage.initBoundary();
	}
	
	
	void saveInitialLabelImage()
	{
		// save the initial guess (random/user defined/whatever) to a tiff
		// so we can reuse it for debugging
		boolean doSaveGuess = false;
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
	}
	
	
	void initStack()
	{
		if(userDialog.useStack())
		{
			
			ImageProcessor labelImageProc; // = labelImage.getLabelImageProcessor().convertToShort(false);
			
			int[] dims = labelImage.getDimensions();
			int width = dims[0];
			int height = dims[1];
			
			labelImageProc = new ShortProcessor(width, height);
			ImagePlus labelImPlus = new ImagePlus("dummy", labelImageProc);
			stack = labelImPlus.createEmptyStack();
			
			stackImPlus = new ImagePlus(null, labelImageProc);
//			stackImPlus = new ImagePlus("Stack of "+originalIP.getTitle(), stack); 
			stackImPlus.show();
			
			// first stack image without boundary&contours
			addSliceToStackAndShow("init", initialLabelImageProcessor.convertToShort(false).getPixelsCopy());
			
			// next stack image is start of algo
//			addSliceToStackAndShow("init", labelImage.getLabelImageProcessor().getPixelsCopy());
			addSlice(labelImage, "init");
			
			IJ.setMinAndMax(stackImPlus, 0, maxLabel);
			IJ.run(stackImPlus, "3-3-2 RGB", null); // stack has to contain at least 2 slices so this LUT applies to all future slices.
		}
	}
	
	
	void initControls()
	{
		controllerFrame = new ControllerFrame(this);
		controllerFrame.setVisible(true);
	}
	

	void frontsCompetitionImageFilter()
	{
		initInputImage();
		initLabelImage();
//		labelImage.initZero();
//		labelImage.initBrightBubbles(intensityImage);
//		labelImage.initSwissCheese(intensityImage);
//		labelImage.initBoundary();
		
		initAlgorithm();
		initStack();
		initControls();
		
//		localMax(intensityImage);
		
		int n = userDialog.getKBest();
		if(n<1) n = 1;
		Timer t = new Timer();
		ArrayList<Long> timeList = new ArrayList<Long>();
		
//		for(int i=0; i<n; i++)
//		{
//			t.tic();
//			labelImage.initMembers();
//			labelImage.initWithIP(asdfasdf);
//			t.toc();
//		}
		

		if(userDialog.getKBest()>0)
		{
			ArrayList<Long> list = new ArrayList<Long>();

			for(int i=0; i<userDialog.getKBest(); i++)
			{
				t.tic();
				labelImage.initMembers();
				labelImage.initWithImageProc(initialLabelImageProcessor);
//				labelImage.initBoundary();
//				labelImage.generateContour();
				
				
				initAlgorithm();
				algorithm.GenerateData();
				t.toc();
				
				updateProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);
				list.add(t.lastResult());
				
				if(stackImPlus!=null)
				{
					IJ.setMinAndMax(stackImPlus, 0, algorithm.getBiggestLabel());
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
			algorithm.GenerateData();
			
			updateProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);
			
			if(stackImPlus!=null)
			{
				IJ.setMinAndMax(stackImPlus, 0, algorithm.getBiggestLabel());
			}
			showFinalResult(labelImage);
		}
		

		if(userDialog.showStatistics())
		{
			algorithm.showStatistics();
		}
		
		controllerFrame.dispose();
		
	}
	

	void showFinalResult(LabelImage li)
	{
		showFinalResult(li,"");
	}
	
	
	ImagePlus showFinalResult(LabelImage li, Object title)
	{
		if(this.originalIP.getNSlices()>1)
		{
			return showFinalResult3D(li, title);
		}
		
		// Colorprocessor doesn't support abs() (does nothing). 
//		li.absAll();
		ImageProcessor imProc = li.getLabelImageProcessor();
//		System.out.println(Arrays.toString((int[])(imProc.getPixels())));
		
		// convert it to short
		short[] shorts = li.getShortCopy();
		for(int i=0; i<shorts.length; i++){
			shorts[i] = (short)Math.abs(shorts[i]);
		}
		ShortProcessor shortProc = new ShortProcessor(imProc.getWidth(), imProc.getHeight());
		shortProc.setPixels(shorts);
		
//		TODO !!!! imProc.convertToShort() does not work, first converts to byte, then to short...
		String s = "ResultWindow "+title;
		String titleUnique = WindowManager.getUniqueName(s);
		
		ImagePlus imp = new ImagePlus(titleUnique, shortProc);
		IJ.setMinAndMax(imp, 0, algorithm.getBiggestLabel());
		IJ.run(imp, "3-3-2 RGB", null);
		imp.show();
		return imp;
	}
	
	public void show3DViewer()
	{
		if(stackImPlus==null)
			return;
		ImagePlus imp = new Duplicator().run(stackImPlus);
		ImageStack stack = imp.getImageStack();
		
		// clean up
		int n = stack.getSize();
		for(int i=1; i<=n; i++)
		{
			short[] pixels = (short[])stack.getPixels(i);
			for(int j=0; j<pixels.length; j++)
			{
				short val = pixels[j];
				if(val<0) 
					pixels[j] = (short)(-val);
				if(val == Short.MAX_VALUE)
					pixels[j] = 0;
			}
		}
		
		
		IJ.run(imp, "8-bit", "");
		Image3DUniverse univ = new Image3DUniverse();
		univ.show();
		univ.addVoltex(imp, null, "Name", 0, new boolean[] {true, true, true}, 1);
//		univ.addVoltex(stackImPlus, null, null, 1, 1, 1);
//		univ.addVoltex(imp);
		
		// uncomment to see 3D
//		Timeline tl = univ.getTimeline();
//		tl.play();
	}
	
	public ImagePlus showFinalResult3D(LabelImage li, Object title)
	{
		
		ImagePlus imp = new ImagePlus("ResultWindow "+title, li.get3DShortStack(true));
		
		IJ.setMinAndMax(imp, 0, algorithm.getBiggestLabel());
		IJ.run(imp, "3-3-2 RGB", null);
		
		imp.show();
		show3DViewer();
		
//		IJ.run(imp, "Z Project...", "start=1 stop="+z+" projection=[Average Intensity]");

//		HyperStackConverter hs = new HyperStackConverter();
		
//		imp.show();
		
		return imp;
	}
	
	
	/**
	 * Invoke this method after done an itation
	 */
	public void updateProgress(int iteration, int maxIterations)
	{
		IJ.showProgress(iteration, maxIterations);
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
	
	public void addSlice(LabelImage labelImage, String title)
	{
		int dim = labelImage.getDim();
		if(dim==2)
		{
			addSliceToStackAndShow(title, labelImage.getSlice());
		}
		if(dim==3)
		{
			addSliceToHyperstack(title, labelImage.get3DShortStack(false));
		}
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
			// stack was closed by user, don't reopen
			System.out.println("stack is null");
			return;
		}

//		stack = stackImPlus.getStack();
		
		stack.addSlice(title, pixels);
		stackImPlus.setStack(stack);
		stackImPlus.setPosition(stack.getSize());
		
		adjustLUT();
	}
	
	
	public void addSliceToHyperstack(String title, ImageStack stackslice)
	{
		if(stack==null)
		{
			// stack was closed by user, dont reopen
			System.out.println("stack is null");
			return;
		}
		
//		stack = stackImPlus.getStack();
		
		// clean the stack, hyperstack must not contain additional slices
		while(stack.getSize() % stackslice.getSize() != 0){
			stack.deleteLastSlice();
		}
		
		// in first iteration, convert to hyperstack
		if(stackImPlus.getNFrames()<=2)
		{
//			new HyperStackConverter().run("stacktohs");
			ImagePlus imp2 = stackImPlus;
			imp2.setOpenAsHyperStack(true);
			new StackWindow(imp2);
		}
		
		int lastSlice = stackImPlus.getSlice();
		int lastFrame = stackImPlus.getFrame();
		boolean wasLastFrame = lastFrame == stackImPlus.getDimensions()[4];
		
		for(int i=1; i<=stackslice.getSize(); i++)
		{
			stack.addSlice(title+i, stackslice.getProcessor(i));
		}
		
		int total = stack.getSize();
		int depth = stackslice.getSize();
		int timeSlices = total/depth;
		
//		imp.setDimensions(nChannels, nSlices, nFrames)
		stackImPlus.setDimensions(1, depth, timeSlices);
		
		// scroll lock on last frame
		int nextFrame = lastFrame;
		if(wasLastFrame){
			nextFrame++;
		}
		
		//go to mid in first iteration
		if(timeSlices<=2){
			lastSlice = depth/2;
		}
		stackImPlus.setPosition(1, lastSlice, nextFrame);

		
		adjustLUT();

	}

	private int maxLabel=100;
	private void adjustLUT()
	{
		if(algorithm.getBiggestLabel()>maxLabel)
		{
			maxLabel*=2;
			IJ.setMinAndMax(stackImPlus, 0, maxLabel);
			IJ.run(stackImPlus, "3-3-2 RGB", null);
		}
	}
	
	public LabelImage getLabelImage()
	{
		return this.labelImage;
	}
	
	public Algorithm getAlgorithm()
	{
		return this.algorithm;
	}

	public ImagePlus getStackImPlus()
	{
		return this.stackImPlus;
	}
	
	public ImagePlus getOriginalImPlus()
	{
		return this.originalIP;
	}
	
//	public ImagePlus getNormalizedIP()
//	{
//		return this.dataNormalizedIP;
//	}

	
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
	
	
	void testOpenedImages()
	{
		int[] ids = WindowManager.getIDList();
		if(ids!=null)
		{
			for(int id: ids)
			{
				ImagePlus ip = WindowManager.getImage(id);
				System.out.println(ip.getTitle());
			}
		}
		
	}

	void testConnNew()
	{
		Connectivity conn = new Connectivity(2, 0);
		for(Point p : conn) {
			System.out.println(p);
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
	

	boolean testMacroBug()
	{
		GenericDialog gd = new GenericDialog("test");
		gd.addTextAreas(null, null, 1, 1);
		gd.addNumericField("dummyfield", 0, 0);
		gd.addStringField("stringfield", "");
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
	
	
	

	private ArrayList<Point> localMax(IntensityImage intensityImage)
	{
		ArrayList<Point> list = new ArrayList<Point>();
		
		ImagePlus imp = intensityImage.imageIP;
		
		MaximumFinder3D locmax = new MaximumFinder3D(intensityImage.getDimensions());
		int[][] points = locmax.getMaxima(intensityImage.dataIntensity, 0.001, true);
		if(points==null)
		{
			System.out.println("no maxima");
			return list;
		}
		int[] xs = points[0];
		int[] ys = points[1];
		int[] zs = points[2];

		int n = xs.length;
		for(int i=0; i<n; i++)
		{
			int x = xs[i];
			int y = ys[i];
			int z = zs[i];
//			System.out.println(x+" "+y+" "+" "+z);
			
			Point p = Point.CopyLessArray(new int[]{x,y,z});
			list.add(p);
		}
		
		// view immediately
		
		boolean showPoints = false;
		
		if(showPoints)
		{
			ImagePlus imp2 = new Duplicator().run(imp);
			imp2.setTitle("points");
			IJ.run(imp2, "Set...", "value=255 stack");
			
			ImageStack stack = imp2.getStack();
			for(int i=0; i<n; i++)
			{
				int x = xs[i];
				int y = ys[i];
				int z = zs[i];
				stack.getProcessor(z+1).set(x, y, 0);
			}
			imp2.show();
		}
		
//		MaximumFinder
//		IJ.run(imp, "Find Maxima...", "noise=4 output=[Maxima Within Tolerance] light");
		
		return list;
	}
	
	
}
	
	
	
