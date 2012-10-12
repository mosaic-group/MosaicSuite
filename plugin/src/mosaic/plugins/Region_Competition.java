package mosaic.plugins;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JFrame;

import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.real.FloatType;

//import view4d.Timeline;

import mosaic.region_competition.*;
import mosaic.region_competition.GUI.ControllerFrame;
import mosaic.region_competition.GUI.GenericDialogGUI;
import mosaic.region_competition.GUI.InputReadable;
import mosaic.region_competition.deprecated.E_CurvatureBasedGradientFlow_sphis;
import mosaic.region_competition.deprecated.E_PS_sphis;
import mosaic.region_competition.energies.*;
import mosaic.region_competition.initializers.*;
import mosaic.region_competition.topology.Connectivity;
import mosaic.region_competition.utils.IntConverter;
import mosaic.region_competition.utils.Timer;
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
	ImageModel imageModel;
	private ImagePlus originalIP;		// IP of the input image
	
	ImageStack stack;			// stack saving the segmentation progress images
	ImagePlus stackImPlus;		// IP showing the stack
	boolean stackKeepFrames = false;
	
	ImageStack initialStack; // copy of the initial guess (without contour/boundary)
	
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
//		defaultFile = "/imagesAndPaper/icecream_shaded_130x130.tif";
//		defaultFile = "imagesAndPaper/sphere_3d.tif";
//		defaultFile = "imagesAndPaper/JZ_111207_SchMax_T_t0051-1.tif";
		
		defaultInputFile = defaultDir + defaultFile;
		
	}

	public boolean saveSettings = true;
	public String savedSettings = null;
	
	
	public int setup(String aArgs, ImagePlus aImp)
	{
//		if(testMacroBug())
//			return DONE;
		
//		Connectivity.test();
//		UnitCubeCCCounter.test();
		
		
		// load
		
		String dir = IJ.getDirectory("temp");
		savedSettings = dir+"rc_settings.dat";
		if(saveSettings)
		{
			System.out.println(savedSettings);
			try
			{
				FileInputStream fin = new FileInputStream(savedSettings);
				ObjectInputStream ois = new ObjectInputStream(fin);
				settings = (Settings)ois.readObject();
				ois.close();
			}
			catch (FileNotFoundException e)
			{
				System.err.println("Settings File not found "+savedSettings);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
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
		if(saveSettings)
		{
			try
			{
				FileOutputStream fout = new FileOutputStream(savedSettings);
				ObjectOutputStream oos = new ObjectOutputStream(fout);
				oos.writeObject(settings);
				oos.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
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
	
	
	void initEnergies()
	{
		Energy e_data = null;
		Energy e_length = null;
		Energy e_merge = null;
		
		HashMap<Integer, LabelInformation> labelMap = labelImage.getLabelMap();
		
		EnergyFunctionalType type = settings.m_EnergyFunctional;
		
		Energy e_merge_KL = new E_KLMergingCriterion(labelMap, 
				labelImage.bgLabel, 
				settings.m_RegionMergingThreshold);
		Energy e_merge_NONE = null;
		
		switch(type)
		{
			case e_PC: 
			{
				e_data = new E_CV(labelMap);
				e_merge = e_merge_KL;
				break;
			}
			case e_PS: 
			{
				e_data = new E_PS(labelImage, intensityImage, 
						labelMap, 
						settings.m_GaussPSEnergyRadius, 
						settings.m_RegionMergingThreshold);
				e_merge = e_merge_NONE;
				break;
			}
			case e_DeconvolutionPC:
			{
				int dims[] = intensityImage.getDimensions();
				e_data = new E_Deconvolution(intensityImage,labelMap,(ImgFactory< FloatType >)new ArrayImgFactory< FloatType >(),dims);
				break;
			}
			default : 
			{
				String s = "Unsupported Energy functional";
				IJ.showMessage(s);
				throw new RuntimeException(s);
			}
		}
		

		RegularizationType rType = settings.regularizationType;
		switch(rType)
		{
			case Sphere_Regularization:
			{
				int rad = (int)settings.m_CurvatureMaskRadius;
				e_length = new E_CurvatureFlow(labelImage, rad);
				break;
			}
			case Sphere_Regularization_OLD:
			{
				int rad = (int)settings.m_CurvatureMaskRadius;
				e_length = new E_CurvatureBasedGradientFlow_sphis(labelImage, rad);
				break;
			}
			case Approximative:
			{
				e_length = new E_Gamma(labelImage);
				break;
			}
			case None: 
			{
				e_length = null;
				break;
			}
			default: 
			{
				String s = "Unsupported Regularization";
				IJ.showMessage(s);
				throw new RuntimeException(s);
			}
		}
		
		imageModel = new ImageModel(e_data, e_length, e_merge, settings);
		
	}
	
	void initAlgorithm()
	{
		algorithm = new Algorithm(intensityImage, labelImage, imageModel, settings, this);
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
//				ImagePlusAdapter a;
//				originalIP.show();
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
		
		InitializationType input = userDialog.getLabelImageInitType();
		
		switch(input)
		{
			case ROI_2D:
			{
				System.out.println("manualSelect");
				manualSelect(labelImage);
				break;
			}
			case Rectangle:
			{
//				labelImage.initialGuessGrowing(0.8);
				BoxInitializer bi = new BoxInitializer(labelImage);
				bi.initRatio(0.8);
				break;
			}
//			case Ellipses:
//			{
//				labelImage.initialGuessRandom();
//				break;
//			}
			case Bubbles:
			{
				BubbleInitializer bi = new BubbleInitializer(labelImage);
//				bi.runInitialization();
//				bi.initBySize(10, 50);
				bi.initSizePaddig(10, 10);
//				bi.initWidthCount(5, 0.5);
//				labelImage.initialGuessBubbles();
				break;
			}
			case LocalMax: 
			{
				MaximaBubbles mb = new MaximaBubbles(intensityImage, labelImage);
//				mb.initBrightBubbles();
				mb.initFloodFilled();
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
					String msg = "Failed to load LabelImage ("+fileName+")";
					IJ.showMessage(msg);
					throw new RuntimeException(msg);
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
			initialStack = IntConverter.ImgToStack(labelImage.dataLabel, labelImage.getDimensions());
//			initialLabelImageProcessor = labelImage.getLabelImageProcessor().duplicate();
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
			stackKeepFrames = userDialog.showAllFrames();
			
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
			
			// add a windowlistener to 
			stackImPlus.getWindow().addWindowListener(new StackWindowListener());
			
			// first stack image without boundary&contours
			for(int i=1; i<=initialStack.getSize(); i++)
			{
				Object pixels = initialStack.getPixels(i);
				short[] shortData = IntConverter.intToShort((int[])pixels);
				addSliceToStackAndShow("init", shortData);
			}
//			addSliceToStackAndShow("init", initialLabelImageProcessor.convertToShort(false).getPixelsCopy());
			
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
		
		
		// Stop the algorithm if controllerframe is closed
		controllerFrame.addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e){}
			@Override
			public void windowIconified(WindowEvent e){}
			@Override
			public void windowDeiconified(WindowEvent e){}
			@Override
			public void windowDeactivated(WindowEvent e){}
			
			@Override
			public void windowClosing(WindowEvent e)
			{
				if(algorithm!=null)
				{
					algorithm.stop();
				}
			}
			
			@Override
			public void windowClosed(WindowEvent e)
			{
				if(algorithm!=null)
				{
					algorithm.stop();
				}
			}
			@Override
			public void windowActivated(WindowEvent e){}
		});
	}
	

	void frontsCompetitionImageFilter()
	{
		initInputImage();
		initLabelImage();
//		labelImage.initZero();
//		labelImage.initBrightBubbles(intensityImage);
//		labelImage.initSwissCheese(intensityImage);
//		labelImage.initBoundary();
		
		initEnergies();
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
//				if(true)
//				{
//					throw new RuntimeException("init with stack");
//				}
				labelImage.initWithStack(initialStack);
//				labelImage.initWithImageProc(initialLabelImageProcessor);
//				labelImage.initBoundary();
//				labelImage.generateContour();
				
				initEnergies();
				
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
		if(li.getDim()==3)
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
		if(!userDialog.show3DResult())
		{
			return;
		}
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
	
	public void showStatus(String s)
	{
		IJ.showStatus(s);
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
	void addSliceToStackAndShow(String title, Object pixels)
	{
		if(stack==null)
		{
			// stack was closed by user, don't reopen
			System.out.println("stack is null");
			return;
		}

//		stack = stackImPlus.getStack();
		
		if(!stackKeepFrames)
		{
			stack.deleteLastSlice();
		}
		
		stack.addSlice(title, pixels);
		stackImPlus.setStack(stack);
		stackImPlus.setPosition(stack.getSize());
		
		adjustLUT();
	}
	
	
	/**
	 * Adds slices for 3D images to stack, overwrites old images. 
	 */
	void add3DtoStaticStack(String title, ImageStack stackslice)
	{
		
		int oldpos = stackImPlus.getCurrentSlice();
		if(oldpos<1) oldpos = 1;
		
		while(stack.getSize()>0)
		{
			stack.deleteLastSlice();
		}
		
		int nnewslices = stackslice.getSize();
		for(int i=1; i<=nnewslices; i++)
		{
			stack.addSlice(title+" "+i, stackslice.getPixels(i));
		}
		
		stackImPlus.setStack(stack);
		stackImPlus.setPosition(oldpos);
		
		adjustLUT();
		
	}
	
	/**
	 * Shows 3D segmentation progress in a hyperstack
	 */
	void addSliceToHyperstack(String title, ImageStack stackslice)
	{
		if(stack==null)
		{
			// stack was closed by user, dont reopen
			System.out.println("stack is null");
			return;
		}
		
		if(!stackKeepFrames)
		{
			add3DtoStaticStack(title, stackslice);
			return;
		}
		
//		stack = stackImPlus.getStack();
		
		// clean the stack, hyperstack must not contain additional slices
		while(stack.getSize() % stackslice.getSize() != 0){
			stack.deleteSlice(1);
		}
		
		// in first iteration, convert to hyperstack
		if(stackImPlus.getNFrames()<=2)
		{
//			new HyperStackConverter().run("stacktohs");
			ImagePlus imp2 = stackImPlus;
			imp2.setOpenAsHyperStack(true);
			StackWindow win = new StackWindow(imp2);
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
		try
		{
			// sometimes here is a ClassCastException 
			// when scrolling in the hyperstack
			// it's a IJ problem... catch the Exception, hope it helps
			stackImPlus.setPosition(1, lastSlice, nextFrame);
		}
		catch (Exception e)
		{
			System.out.println(e);
		}

		
		adjustLUT();

	}

	private int maxLabel=100;
	private void adjustLUT()
	{
		if(algorithm.getBiggestLabel()>maxLabel)
		{
			maxLabel*=2;
		}
		IJ.setMinAndMax(stackImPlus, 0, maxLabel);
		IJ.run(stackImPlus, "3-3-2 RGB", null);
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

	
	/**
	 * This {@link WindowListener} sets stack to null if stackwindow was closed by user. 
	 * This indicates to not further producing stackframes. 
	 * For Hyperstacks (for which IJ reopens new Window on each update) it hooks to the new Windows. 
	 */
	class StackWindowListener implements WindowListener
	{
		@Override
		public void windowClosing(WindowEvent e)
		{
			System.out.println("stackimp closing");
			stack = null;
//			stackImPlus = null;
		}
		
		@Override
		public void windowClosed(WindowEvent e)
		{
			System.out.println("stackimp closed");
			// hook to new window
			Window win = stackImPlus.getWindow();
			if(win!=null){
				win.addWindowListener(this);
			}
		}
		
		@Override
		public void windowOpened(WindowEvent e){}
		@Override
		public void windowIconified(WindowEvent e){}
		@Override
		public void windowDeiconified(WindowEvent e){}
		@Override
		public void windowDeactivated(WindowEvent e){}
		@Override
		public void windowActivated(WindowEvent e){}
	
	}
}
	
	
	
