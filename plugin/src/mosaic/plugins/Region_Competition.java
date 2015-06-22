package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JFrame;

import mosaic.core.ImagePatcher.ImagePatch;
import mosaic.core.ImagePatcher.ImagePatcher;
import mosaic.core.cluster.ClusterGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.psf.GeneratePSF;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;
import mosaic.core.utils.Segmentation;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Settings;
import mosaic.region_competition.GUI.ControllerFrame;
import mosaic.region_competition.GUI.GenericDialogGUI;
import mosaic.region_competition.GUI.InputReadable;
import mosaic.region_competition.energies.E_CV;
import mosaic.region_competition.energies.E_CurvatureFlow;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.E_Gamma;
import mosaic.region_competition.energies.E_KLMergingCriterion;
import mosaic.region_competition.energies.E_PS;
import mosaic.region_competition.energies.Energy;
import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.energies.RegularizationType;
import mosaic.region_competition.initializers.BoxInitializer;
import mosaic.region_competition.initializers.BubbleInitializer;
import mosaic.region_competition.initializers.InitializationType;
import mosaic.region_competition.initializers.MaximaBubbles;
import mosaic.region_competition.output.RCOutput;
import mosaic.region_competition.utils.IntConverter;
//import view4d.Timeline;
import mosaic.region_competition.utils.Timer;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;


/**
 * @author Stephan Semmler, ETH Zurich
 * @version 2012.06.11
 */

public class Region_Competition implements Segmentation
{
	
	private String[] out = {"*_ObjectsData_c1.csv","*_seg_c1.tif"};
	//private String output_label;
	Region_Competition MVC;		// interface to image application (imageJ)
	public Settings settings;

	Algorithm algorithm;
	LabelImageRC labelImage;		// data structure mapping pixels to labels
	IntensityImage intensityImage; 
	ImageModel imageModel;
	Calibration cal;
	ImagePlus originalIP;		// IP of the input image
	Vector<ImagePlus> OpenedImages;
	
	ImageStack stack;			// stack saving the segmentation progress images
	ImagePlus stackImPlus;		// IP showing the stack
	boolean stackKeepFrames = false;
	boolean normalize_ip = false;
	
	ImageStack initialStack; // copy of the initial guess (without contour/boundary)
	
	public InputReadable userDialog;
	JFrame controllerFrame;
	
	
	static public void SaveConfigFile(String sv, Settings settings) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(sv);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(settings);
		oos.close();
	}

	
	private boolean LoadConfigFile(String savedSettings)
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
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * 
	 * Return the dimension of a file
	 * 
	 * @param f file
	 * @return
	 */
	
	int getDimension(File f)
	{
		Opener o = new Opener();
		ImagePlus ip = o.openImage(f.getAbsolutePath());
		
		return getDimension(ip);
	}
	
	/**
	 * 
	 * Return the dimension of an image
	 * 
	 * @param aImp image
	 * @return
	 */
	
	private int getDimension(ImagePlus aImp)
	{
		if (aImp.getNSlices() == 1)
			return 2;
		else
			return 3;
	}
	
	private String getOptions(File f)
	{
		String par = new String();
		
		// get file dimension

		int d = getDimension(f);
		par += "Dimensions=" + d + " ";
		
		// if deconvolving create a PSF generator window
		
		if (settings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC)
		{
			GeneratePSF psf = new GeneratePSF();
			psf.generate(d);
			par += psf.getParameters();
		}
			
		return par;
	}
	
	private String getOptions(ImagePlus aImp)
	{
		String par = new String();
		
		// get file dimension

		int d = getDimension(aImp);
		par += "Dimensions=" + d + " ";
		
		// if deconvolving create a PSF generator window
		
		GeneratePSF psf = new GeneratePSF();
		psf.generate(d);
		par += psf.getParameters();
		
		return par;
	}
	
	/**
	 * 
	 * Run the segmentation on ImgLib2
	 * 
	 * @param aArgs arguments
	 * @param img Image
	 * @param lbl Label image
	 * @return
	 */
	
	public<T extends RealType<T>,E extends IntegerType<E>> void runOnImgLib2(String aArgs, Img<T> img, Img<E> lbl, Class<T> cls)
	{
		initAndParse();
		
		// Run Region Competition as usual
		
		RCImageFilter(img,lbl,cls);
	}
	
	/**
	 * 
	 * Init Region Competition and parse command
	 * 
	 */
	
	String sv = null;
	
	private void initAndParse()
	{
        String options = Macro.getOptions();
		
		normalize_ip = true;
		if (options != null)
		{		
			// Command line interface search for config file
			
			String path;
			
			String tmp = null;
			Boolean tmp_b = null;
			
			
			// normalize 
			
			if ((tmp_b = MosaicUtils.parseNormalize(options)) != null)
			{
				normalize_ip = tmp_b;
			}
				
			// config
			
			if ((tmp = MosaicUtils.parseConfig(options)) != null)
			{
				path = tmp;
						
				LoadConfigFile(path);
			}
			else
			{
				// load config file
				
				String dir = IJ.getDirectory("temp");
				sv = dir+"rc_settings.dat";
				LoadConfigFile(sv);
			}
			
			output = MosaicUtils.parseOutput(options);
			
			// no config file open the GUI
		}
		else
		{
			// load config file
			
			String dir = IJ.getDirectory("temp");
			sv = dir+"rc_settings.dat";
			LoadConfigFile(sv);
		}
		
		if(settings == null)
		{
			settings = new Settings();
		}
		
		MVC = this;
	}
	
	Img<FloatType> image_psf;
	
	public int setup(String aArgs, ImagePlus aImp)
	{		
		if (MosaicUtils.checkRequirement() == false)
			return DONE;
		
		initAndParse();
		
		originalIP = aImp;
		userDialog = new GenericDialogGUI(this.settings, this.getOriginalImPlus());
		userDialog.showDialog();
		
		boolean success=userDialog.processInput();
		if(!success)
		{
			return DONE;
		}
		
		if (userDialog.getInputImage() != null)
		{
			originalIP = (ImagePlus) userDialog.getInputImage();
			if (originalIP != null)
				cal = originalIP.getCalibration();
		}
		
		if (userDialog.useCluster() == true)
		{
			// We run on cluster
			
			try 
			{
				// Copying parameters
				
				Settings p = new Settings(settings);
				
				// saving config file
				
				SaveConfigFile("/tmp/settings.dat",p);
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ClusterGUI cg = new ClusterGUI();
			ClusterSession ss = cg.getClusterSession();
			ss.setInputArgument("text1");
			ss.setSlotPerProcess(1);
			File[] fileslist = null;
			
			// Check if we selected a directory
			
			if (aImp == null)
			{
				File fl = new File(userDialog.getInputImageFilename());
				File fl_l = new File(userDialog.getLabelImageFilename());
				if (fl.isDirectory() == true)
				{
					// we have a directory
					
					String opt = getOptions(fl);
					if (settings.labelImageInitType == InitializationType.File)
					{
						// upload label images
						
						ss = cg.getClusterSession();
						fileslist = fl_l.listFiles();
						File dir = new File("label");
						ss.upload(dir,fileslist);
						opt += " text2=" + ss.getClusterDirectory() + File.separator + dir.getPath();
					}
					
					fileslist = fl.listFiles();
					
					ss = ClusterSession.processFiles(fileslist,"Region Competition",opt+" show_and_save_statistics",out,cg);
				}
				else if (fl.isFile())
				{
					String opt = getOptions(fl);
					if (settings.labelImageInitType == InitializationType.File)
					{
						// upload label images
						
						ss = cg.getClusterSession();
						fileslist = new File[1];
						fileslist[0] = fl_l;
						ss.upload(fileslist);
						opt += " text2=" + ss.getClusterDirectory() + File.separator + fl_l.getName();
					}
					
					ss = ClusterSession.processFile(fl,"Region Competition",opt+" show_and_save_statistics",out,cg);
				}
				else
				{
					ss = ClusterSession.getFinishedJob(out,"Region Competition",cg);
				}
			}
			else
			{
				// It is an image
				
				String opt = getOptions(aImp);
				
				if (settings.labelImageInitType == InitializationType.File)
				{
					// upload label images
					
					ss = cg.getClusterSession();
					ss.splitAndUpload((ImagePlus)userDialog.getLabelImage(),new File("label"),null);
					opt += " text2=" + ss.getClusterDirectory() + File.separator + "label" + File.separator + ss.getSplitAndUploadFilename(0);
				}
				
				ss = ClusterSession.processImage(aImp,"Region Competition",opt+" show_and_save_statistics",out,cg);
			}
			
			// Get output format and Stitch the output in the output selected
			
			String outcsv[] = {"*_ObjectsData_c1.csv"};
			File f = ClusterSession.processJobsData(outcsv,MosaicUtils.ValidFolderFromImage(aImp),RCOutput.class);
			
			if (aImp != null)
				MosaicUtils.StitchCSV(MosaicUtils.ValidFolderFromImage(aImp),out,MosaicUtils.ValidFolderFromImage(aImp) + File.separator + aImp.getTitle());
			else
				MosaicUtils.StitchCSV(f.getParent(),out,null);
				
			
			////////////////
			
			return NO_IMAGE_REQUIRED;
		}
		else
		{
			// save
			try
			{
				SaveConfigFile(sv,settings);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			// init the input image we need to open it before run
		
			// if is 3D save the originalIP
		
			if (aImp != null)
			{
				if (aImp.getNSlices() != 1)
				{
					originalIP = aImp;
			
				}
			}
			else
			{
				originalIP = null;
				return NO_IMAGE_REQUIRED;
			}
			
			if (settings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC)
			{
		        // Here, no PSF has been set by the user. Hence, Generate it
		    	
				GeneratePSF gPsf = new GeneratePSF();
		    	
				if (aImp.getNSlices() == 1)
					image_psf = gPsf.generate(2);
				else
					image_psf = gPsf.generate(3);
			}
		}
		return DOES_ALL+NO_CHANGES;
	}
	
	/**
	 * 
	 * Eliminate Forbidden region
	 * 
	 * @param ip
	 */
	
//	private <T extends IntegerType<T>>void EliminateForbidden(ImagePatch ip)
//	{
//		Img<T> lbg = ip.getResult();
//		
//		Cursor<T> cur = lbg.cursor();
//		
//		while (cur.hasNext())
//		{
//			cur.next();
//			
//			if (cur.get().getInteger() == labelImage.forbiddenLabel)
//			{
//				cur.get().setInteger(0);
//			}
//		}
//	}
	
	/**
	 * 
	 * Set the spacing of the Image
	 * 
	 * @param cal Calibration
	 */
	
	void setCalibration(Calibration cal)
	{
		this.cal = cal;
	}
	
	String output;
	
	/**
	 * 
	 * Run region competition plugins
	 * 
	 */
	
	public void run(ImageProcessor aImageP) 
	{
		if (settings.labelImageInitType == InitializationType.File_Patcher)
		{
			// Open the label and intensity image with imgLib2
			
			initInputImage();
			initLabelImage();
			 
			// Patches the image
			
			int margins[] = new int [intensityImage.getDim()];
			
			for (int i = 0 ; i < margins.length ; i++)
			{
				margins[i] = (int) (image_psf.dimension(i));
				if (margins[i] < 50)
					margins[i] = 50;
			}
			
			ImagePatcher<FloatType,IntType> ip = new ImagePatcher<FloatType,IntType>(intensityImage.getImgLib2(FloatType.class),labelImage.getImgLib2(IntType.class),margins);
			
			// for each patch run region competition
			
			@SuppressWarnings("unchecked")
			ImagePatch<FloatType,IntType>[] ips = ip.getPathes();
			
			
			for (int i = 1 ; i < ips.length ; i++)
			{
				settings.labelImageInitType = InitializationType.File;
				
				// Run region competition on the patches image
				
				ips[i].show();
				Region_Competition RC = new Region_Competition();
				RC.setSettings(settings);
				RC.setPSF(image_psf);
				RC.setCalibration(cal);
//				RC.HideProcess();
				RC.runOnImgLib2(null,ips[i].getImage(),ips[i].getLabelImage(),FloatType.class);
				RC.labelImage.eliminateForbidden();
				ips[i].setResult(RC.labelImage.getImgLib2(IntType.class));
				ips[i].showResult();
			}
			
			// Assemble result
			
			ImageJFunctions.show(ip.assemble(IntType.class,1));
			
//			labelImage = new LabelImageRC(ip.assemble(IntType.class));
			
			// save result
			
//			labelImage = new labelImage(ip.getAssembles());
//			labelImage.connectedComponents();
//			labelImage.show("Segmented", 255);
		}
		else
		{
			// Run Region Competition as usual
			
			try
			{
				RCImageFilter();
			}
			catch (Exception e)
			{
				if(controllerFrame!=null)
					controllerFrame.dispose();
				e.printStackTrace();
			}
		}
		
		String folder = MosaicUtils.ValidFolderFromImage(MVC.getOriginalImPlus());
		
		// Remove eventually extension
		
		if (labelImage == null)
			return;
		
		if (output == null)
			labelImage.save(folder + File.separator + MosaicUtils.getRegionMaskName(MVC.getOriginalImPlus().getTitle()));
		else
			labelImage.save(output);
		
		labelImage.calculateRegionsCenterOfMass();
		
		if(userDialog.showAndSaveStatistics())
		{
			showAndSaveStatistics(algorithm.getLabelMap());
		}
	}
	
	boolean hide_p = false;
	
	/**
	 * 
	 * Hide all processing
	 * 
	 */
	
//	private void HideProcess() 
//	{
//		hide_p = true;
//	}

	/**
	 * 
	 * Hide get hide processing status
	 * 
	 */
	
	public boolean getHideProcess() 
	{
		return hide_p;
	}

	/**
	 * 
	 * Set the PSF of this Region competition istancee
	 * 
	 * @param Image reppresenting the PSF
	 * 
	 */
	
	private void setPSF(Img<FloatType> img)
	{
		image_psf = img;
	}
	
	/**
	 * 
	 * Initialize the energy function
	 * 
	 */
	
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
				e_data = new E_Deconvolution(intensityImage,labelMap,new ArrayImgFactory< FloatType >(),dims);
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
				e_length = new E_CurvatureFlow(labelImage, rad, cal);
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
			if (ip != null)
			{
				FileInfo fi = ip.getFileInfo();
				fi.directory = file.substring(0,file.lastIndexOf(File.separator));
				ip.setFileInfo(fi);
			}
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
			new Opener();
		}
		
			
		if(ip!=null)
		{
			originalIP = ip;
			
			if (normalize_ip)
				intensityImage = new IntensityImage(originalIP);
			else
				intensityImage = new IntensityImage(originalIP,false);
//			dataNormalizedIP = new ImagePlus("Normalized Input Image", stack);
			
			// image loaded
			boolean showOriginal = true;
			if(showOriginal && userDialog != null)
			{
				originalIP.show();
			}
			
			if(userDialog != null && userDialog.showNormalized())
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
			IJ.noImage();
			throw new RuntimeException("Failed to load an input image.");
		}

	}
	
	void initLabelImage()
	{
		labelImage = new LabelImageRC(intensityImage.getDimensions());
		InitializationType input;
		
		if (userDialog != null)
			input = userDialog.getLabelImageInitType();
		else
			input = settings.labelImageInitType;
		
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
				bi.initRatio(settings.l_BoxRatio);
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
				bi.initSizePaddig(settings.m_BubblesRadius, settings.m_BubblesDispl);
//				bi.initWidthCount(5, 0.5);
//				labelImage.initialGuessBubbles();
				break;
			}
			case LocalMax: 
			{
				MaximaBubbles mb = new MaximaBubbles(intensityImage, labelImage, settings.l_BubblesRadius, settings.l_Sigma,settings.l_Tolerance, settings.l_RegionTolerance);
//				mb.initBrightBubbles();
				mb.initFloodFilled();
				break;
			}
			case File_Patcher:
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
					if (ip == null)
						ip = choiceIP;
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
			initialStack = IntConverter.intArrayToStack(labelImage.dataLabel, labelImage.getDimensions());
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
		if (IJ.isMacro() == true || hide_p == true)
			return;
		
		if (userDialog != null)
			stackKeepFrames = userDialog.showAllFrames();
		else
			stackKeepFrames = false;
			
		ImageProcessor labelImageProc; // = labelImage.getLabelImageProcessor().convertToShort(false);
		
		int[] dims = labelImage.getDimensions();
		int width = dims[0];
		int height = dims[1];
			
		labelImageProc = new ShortProcessor(width, height);
		ImagePlus labelImPlus = new ImagePlus("dummy", labelImageProc);
		stack = labelImPlus.createEmptyStack();

		stackImPlus = new ImagePlus(null, labelImageProc);
//		stackImPlus = new ImagePlus("Stack of "+originalIP.getTitle(), stack); 
		stackImPlus.show();
			
		// add a windowlistener to 
			
		if (IJ.isMacro() == false)
			stackImPlus.getWindow().addWindowListener(new StackWindowListener());
			
		// first stack image without boundary&contours
		for(int i=1; i<=initialStack.getSize(); i++)
		{
			Object pixels = initialStack.getPixels(i);
			short[] shortData = IntConverter.intToShort((int[])pixels);
			addSliceToStackAndShow("init", shortData);
		}
//		addSliceToStackAndShow("init", initialLabelImageProcessor.convertToShort(false).getPixelsCopy());
			
		// next stack image is start of algo
//		addSliceToStackAndShow("init", labelImage.getLabelImageProcessor().getPixelsCopy());
		addSlice(labelImage, "init");
		
		IJ.setMinAndMax(stackImPlus, 0, maxLabel);
		IJ.run(stackImPlus, "3-3-2 RGB", null); // stack has to contain at least 2 slices so this LUT applies to all future slices.
	}
	
	
	void initControls()
	{
		// no control when is a script
		
		if (IJ.isMacro() == true)
			return;
		
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
	
	private void doRC()
	{
		initEnergies();
		initAlgorithm();
		
		initStack();
		initControls();
		
//		localMax(intensityImage);
		
		int n = 1;
		if (userDialog != null)
			n = userDialog.getKBest();
		if(n<1) n = 1;
		Timer t = new Timer();
		
//		for(int i=0; i<n; i++)
//		{
//			t.tic();
//			labelImage.initMembers();
//			labelImage.initWithIP(asdfasdf);
//			t.toc();
//		}
		

		if(userDialog != null && userDialog.getKBest()>0)
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
				if (algorithm.GenerateData(image_psf) == false)
					return;
				t.toc();
				
				updateProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);
				list.add(t.lastResult());
				
				if(stackImPlus!=null)
				{
					IJ.setMinAndMax(stackImPlus, 0, algorithm.getBiggestLabel());
				}
//				stackImPlus.updateAndDraw();
				if (userDialog != null && output == null)
					OpenedImages.add(labelImage.show("", algorithm.getBiggestLabel()));
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
			algorithm.GenerateData(image_psf);
			
			updateProgress(settings.m_MaxNbIterations, settings.m_MaxNbIterations);
			
			if(stackImPlus!=null)
			{
				IJ.setMinAndMax(stackImPlus, 0, algorithm.getBiggestLabel());
			}
			if (userDialog != null)
				showFinalResult(labelImage);
		}
		
		if (IJ.isMacro() == false)
			controllerFrame.dispose();
	}
	
	<T extends RealType<T>,E extends IntegerType<E>> void RCImageFilter(Img<T> img, Img<E> lbl,Class<T> cls)
	{
		labelImage = new LabelImageRC(lbl);
		intensityImage = new IntensityImage(img,cls);
		
		initialStack = IntConverter.intArrayToStack(labelImage.dataLabel, labelImage.getDimensions());
//		initialLabelImageProcessor = labelImage.getLabelImageProcessor().duplicate();
	
		labelImage.initBoundary();
		
		doRC();
	}
	
	void RCImageFilter()
	{
		initInputImage();
		initLabelImage();
		
		doRC();
	}
	
	void showFinalResult(LabelImageRC li)
	{
		OpenedImages.add(li.show("", algorithm.getBiggestLabel()));
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
	void manualSelect(final LabelImageRC labelImg)
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
	
	public void addSlice(LabelImageRC labelImage, String title)
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

	public void closeAll()
	{
		labelImage.close();
		intensityImage.close();
		stackImPlus.close();
		if (originalIP != null)
			originalIP.close();
		
		for (int i = 0 ; i < OpenedImages.size() ; i++)
		{
			OpenedImages.get(i).close();
		}
		algorithm.close();
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
	
	public LabelImageRC getLabelImage()
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
	
	/**
	 * 
	 * Get the original imagePlus
	 * 
	 * @return
	 */
	
	public ImagePlus getOriginalImPlus()
	{
		return this.originalIP;
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
		result = p.get(index);
		
		// -43
		p.setf(index, value);
		//float f = p.getf(index); // -42.0
		
		p.set(index, value);
		//f = p.getf(index);		//NaN
		
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
	
//	static void testNumbers()
//	{
//		Double d = 14.2;
//		Integer i = 12;
//		
//		Number n1 = d;
//		Number n2 = i;
////		boolean b = n1>n2; //error
//		
//		Comparable<Number> c1 = (Comparable<Number>)n1;
//		Comparable c2 = (Comparable)n2;
//		c1.compareTo(n2);
//		c2.compareTo(c1);
//	}

	
	/**
	 * This {@link WindowListener} sets stack to null if stackwindow was closed by user. 
	 * This indicates to not further producing stackframes. 
	 * For Hyperstacks (for which IJ reopens new Window on each update) it hooks to the new Windows. 
	 */
	private class StackWindowListener implements WindowListener
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

	public Region_Competition()
	{
		OpenedImages = new Vector<ImagePlus>();
	}
	
	public Algorithm getAlghorithm()
	{
		return algorithm;
	}
	
	public void runP()
	{
		try
		{
			RCImageFilter();
		}
		catch (Exception e)
		{
			if(controllerFrame!=null)
				controllerFrame.dispose();
			e.printStackTrace();
		}		
	}


	/**
	 * 
	 * Set the parameters for Region Competition
	 * 
	 * @param set set of parameters
	 */
	
	public void setSettings(Settings set) 
	{
		settings = set;
	}
	
	/**
	 * 
	 * Show and save statistics
	 * 
	 * @param labelMap HashMap that contain the labels information
	 * 
	 */
	
	public void showAndSaveStatistics(HashMap<Integer, LabelInformation> labelMap)
	{
		ResultsTable rts = createStatistics(labelMap);

		String folder = MosaicUtils.ValidFolderFromImage(MVC.getOriginalImPlus());
		
		saveStatistics(folder + File.separator + MosaicUtils.getRegionCSVName(MVC.getOriginalImPlus().getTitle()), labelMap);
		
		// if is headless do not show
		
		boolean headless_check = GraphicsEnvironment.isHeadless();
		
		if (headless_check == false)
			rts.show("statistics");
	}
	
	/**
	 * 
	 * Save the csv region statistics
	 * 
	 * @param fold where to save
	 * @param labelMap HashMap that save the label information
	 */
	
	public void saveStatistics(String fold,HashMap<Integer, LabelInformation> labelMap)
	{
		// Remove the string file:
		
		if (fold.indexOf("file:") >= 0)
			fold = fold.substring(fold.indexOf("file:")+5);
		
		ResultsTable rts = createStatistics(labelMap);
		
		try
		{
			rts.saveAs(fold);
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String oip = originalIP.getTitle().substring(0, originalIP.getTitle().lastIndexOf("."));
		
		boolean headless_check = GraphicsEnvironment.isHeadless();
		
		if (headless_check == false)
			MosaicUtils.reorganize(out,oip, fold.substring(0,fold.lastIndexOf(File.separator)), 1);
	}
	
	public ResultsTable createStatistics(HashMap<Integer, LabelInformation> labelMap)
	{
		ResultsTable rt = new ResultsTable();
		
		// over all labels
		for(Entry<Integer, LabelInformation> entry: labelMap.entrySet())
		{
			LabelInformation info = entry.getValue();
			
			rt.incrementCounter();
			rt.addValue("Image_ID", 0);
			rt.addValue("label", info.label);
			rt.addValue("size", info.count);
			rt.addValue("mean", info.mean);
			rt.addValue("variance", info.var);
			rt.addValue("Coord_X", info.mean_pos[0]);
			rt.addValue("Coord_Y", info.mean_pos[1]);
			if (info.mean_pos.length > 2)
				rt.addValue("Coord_Z", info.mean_pos[2]);
			else
				rt.addValue("Coord_Z", 0.0);
		}
		
		return rt;
	}

	/**
	 * 
	 * Get CSV regions list name output
	 * 
	 * @param aImp image
	 * @return set of possible output
	 */
	
	@Override
	public String[] getRegionList(ImagePlus aImp) 
	{
		String[] gM = new String[1];
		gM[0] = new String(aImp.getTitle() + "_ObjectsData_c1.csv");
		return gM;
	}


	public String[] getMask(ImagePlus aImp) 
	{
		String[] gM = new String[1];
		gM[0] = new String(aImp.getTitle() + "_seg_c1.tif");
		return gM;
	}


	@Override
	public String getName() 
	{
		return new String("Region_Competition");
	}


	boolean test_mode;
	
	@Override
	public void setIsOnTest(boolean test) 
	{
		test_mode = test;
	}

	@Override
	public boolean isOnTest() 
	{
		return test_mode;
	}
}
	
	
	
