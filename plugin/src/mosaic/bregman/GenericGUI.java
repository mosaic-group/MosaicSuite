package mosaic.bregman;


import java.awt.Button;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.io.FileUtils;

import mosaic.bregman.GUI.BackgroundSubGUI;
import mosaic.bregman.GUI.ColocalizationGUI;
import mosaic.bregman.GUI.PSFWindow;
import mosaic.bregman.GUI.SegmentationGUI;
import mosaic.bregman.GUI.VisualizationGUI;
import mosaic.bregman.output.CSVOutput;
import mosaic.core.GUI.HelpGUI;
import mosaic.core.cluster.ClusterGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.plugins.BregmanGLM_Batch;


//import java.awt.event.FocusEvent;
//import java.awt.event.FocusListener;
//import java.awt.event.TextEvent;
//import java.awt.event.TextListener;
//import java.io.File;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.List;
//import java.util.StringTokenizer;

//import javax.swing.JTextArea;
//
//
//
//import ij.IJ;
//import ij.ImagePlus;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.io.DirectoryChooser;
import ij.io.Opener;
//import ij.gui.NonBlockingGenericDialog;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;

public class GenericGUI 
{
	boolean clustermode;
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;
	int posx, posy;
	static int screensizex, screensizey;
	
	public GenericGUI(boolean mode, ImagePlus img_p)
	{
		imgch1 = img_p;
		clustermode = mode;
	}
	
	public GenericGUI(boolean  	mode){
		clustermode=mode;
		//clustermode=true;
	}
	
	public static void setimagelocation(int x, int y, ImagePlus imp)
	{
		int wx, wy;

		Window w = imp.getWindow();
		wx= w.getWidth();
		wy= w.getHeight();

		w.setSize(Math.min(522, wx), Math.min(574, wy));// set all images to max 512x512 preview (window 522*574)
		w.setLocation(Math.min(x,screensizex-wx),Math.min(y,screensizey-wy));
		
		imp.getCanvas().fitToWindow();		
	}
	
	public static void setwindowlocation(int x, int y, Window w)
	{
		int wx, wy;
		wx= w.getWidth();
		wy= w.getHeight();
		w.setLocation(Math.min(x,screensizex-wx),Math.min(y,screensizey-wy));
		
	}

	/* !!! 2 BUGS concerning checkboxes in generic dialog when run in headless mode on a cluster :
		1 : all checkbox default value should be set to false (because headless mode uses:  (boolean value found in macro || default value) )
		2 : macro should be edited by appending a '=' after checkbox labels that are active ('subpixel' -> 'subpixel=')
	   !!!
	 */



	//    public static String getValue(String options, String key, String defaultValue) {
	//        key = Macro.trimKey(key);
	//        key += '=';
	//        int index=-1;
	//        do { // Require that key not be preceded by a letter
	//            index = options.indexOf(key, ++index);
	//            if (index<0) return defaultValue;
	//        } while (index!=0&&Character.isLetter(options.charAt(index-1)));
	//        options = options.substring(index+key.length(), options.length());
	//        if (options.charAt(0)=='\'') {
	//            index = options.indexOf("'",1);
	//            if (index<0)
	//                return defaultValue;
	//            else
	//                return options.substring(1, index);
	//        } else if (options.charAt(0)=='[') {
	//            index = options.indexOf("]",1);
	//            if (index<=1)
	//                return defaultValue;
	//            else
	//                return options.substring(1, index);
	//        } else {
	//            //if (options.indexOf('=')==-1) {
	//            //  options = options.trim();
	//            //  IJ.log("getValue: "+key+"  |"+options+"|");
	//            //  if (options.length()>0)
	//            //      return options;
	//            //  else
	//            //      return defaultValue;
	//            //}
	//            index = options.indexOf(" ");
	//            if (index<0)
	//                return defaultValue;
	//            else
	//                return options.substring(0, index);
	//        }
	//    }

	//	protected static boolean getMacroParameter(String label, boolean defaultValue) {
	//		boolean temp = getMacroParameter(label) != null ;
	//		boolean res = temp || defaultValue; 
	//		return res;
	//		//return getMacroParameter(label) != null || defaultValue;
	//	}
	//
	//	protected static double getMacroParameter(String label, double defaultValue) {
	//		String value = Macro.getValue(Macro.getOptions(), label, null);
	//		return value != null ? Double.parseDouble(value) : defaultValue;
	//	}
	//
	//	protected static String getMacroParameter(String label, String defaultValue) {
	//		return Macro.getValue(Macro.getOptions(), label, defaultValue);
	//	}
	//
	//	protected static String getMacroParameter(String label) {
	//		return Macro.getValue("filepath=/gpfs/home/rizk_a/Serotonin/180minsub/ rolling=10 gaussian_psf_" +
	//	"approximation,_stddev_xy=0.90 gaussian_psf_approximation,_stddev_z=0.80 lambda=0.10 minimum_object_intensity=0.11" +
	//	" subpixel= cell_mask_channel_1 cell_mask_channel_2 threshold_1=0.0015 threshold_2=0.0350 colorized objects outline save" +
	//	" number=4 intensity_estimation=[Automatic (Fisher scoring MLE)] noise=Poisson", label, null);
	//	}
	

	//	protected static String getMacroParameter(String label) {
	//		return Macro.getValue(Macro.getOptions(), label, null);
	//	}


	private void processJobsData(ClusterSession ss, ImagePlus aImp)
	{
		// Save all JobID to the image folder
		// or ask for a directory
		
		String dir[] = ss.getJobDirectories(0,null);
		
		if (dir.length > 0)
		{
			String dirS;
			
			if (aImp != null)
			{
				dirS = MosaicUtils.ValidFolderFromImage(aImp);
			}
			else
			{
				DirectoryChooser dc = new DirectoryChooser("Choose directory where to save result");
				dirS = dc.getDirectory();
			}
			
			for (int i = 0 ; i < dir.length ; i++)
			{
				try 
				{
					// Stitch Object.csv
					
					String outcsv[] = {"*_ObjectsData_c1.csv"};
					CSVOutput.Stitch(outcsv,"tmp_",new File(dir[i]), dirS);
					
					///////
					
					String[] tmp = dir[i].split(File.separator);
					
					File t = new File(dirS + File.separator + tmp[tmp.length-1]);
					
					ShellCommand.exeCmdNoPrint("cp -r " + dir[i] + " " + t);
					
					// after copy remove the directory
					
					ShellCommand.exeCmdNoPrint("rm -rf " + dir[i]);
				}
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	class MM
	{
		float min;
		float max;
	}
	
	/**
	 * 
	 * Get the maximum and the minimum of a video
	 * 
	 * @param mm output min and max
	 */
	
	private void getMaxMin(File fl, MM mm)
	{
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(fl.getAbsolutePath());
		
		float global_max = 0.0f;
		float global_min = 0.0f;
		
		if (imp != null)
		{
			StackStatistics stack_stats = new StackStatistics(imp);
			global_max = (float)stack_stats.max;
			global_min = (float)stack_stats.min;

			// get the min and the max
		}
		
		if (global_max > mm.max)
			mm.max = global_max;
		
		if (global_min < mm.min)
			mm.min = global_min;
	}
	
	/**
	 * 
	 * Get the maximum and the minimum of a video
	 * 
	 * @param mm output min and max
	 * 
	 */
	
	private void getFilesMaxMin(File fls[], MM mm)
	{	
		for (File fl : fls)
		{
			getMaxMin(fl,mm);
		}
	}
	
	/**
	 * 
	 * Process the image, min and max value are used for renormalization if
	 * max = 0.0 the max mavlue and min value of the image are used
	 * 
	 * @param aImp the image to process
	 * @param cg ClusterGUI
	 * @param min minimum value
	 * @param max maximum value
	 * @return the session cluster
	 */
	
	private ClusterSession processImage(ImagePlus aImp, ClusterGUI cg, Float max, Float min)
	{
		boolean sync = false;
		if (cg == null)
		{
			sync = true;
			cg = new ClusterGUI();
		}
	
		ClusterSession ss = cg.getClusterSession();
		
		// Get all image processor statistics and calculate the maximum
		
		if (max == 0.0)
		{		
			if (aImp != null)
			{
				StackStatistics stack_stats = new StackStatistics(aImp);
				max = (float)stack_stats.max;
				min = (float)stack_stats.min;
		
				// get the min and the max
			}
		}
		
		// Run plugin on frames
		
		if (ss.runPluginsOnFrames(aImp, "min="+ min + " max="+max, Analysis.out, 180.0, sync) == false)
			return null;
		
		return ss;
	}
	
	/**
	 * 
	 * Process a list of files
	 * 
	 * @param list of files to process
	 * @return the cluster session
	 */
	
	private ClusterSession processFiles(File list[])
	{
		ClusterGUI cg = new ClusterGUI();
		ClusterSession ss = cg.getClusterSession();
		
		MM mm = new MM();
		
		mm.min = new Float(Float.MAX_VALUE);
		mm.max = new Float(0.0);
		
		getFilesMaxMin(list,mm);
		
		for (File fl : list)
		{
			// File
			
			processFile(fl,cg,mm.max,mm.min);
		}
		
		ss.runPluginsOnFrames(null,null, Analysis.out, 180.0);
		return ss;
	}
	
	/**
	 * 
	 * Process a file
	 * 
	 * @param fl File to process
	 * @param min 
	 * @param max 
	 * @param ClusterGUI
	 * 
	 */
	private ClusterSession processFile(File fl, ClusterGUI cg, Float max, Float min)
	{
		boolean sync = false;
		
		// open the image and process image
		
		Opener opener = new Opener();  
		ImagePlus imp = opener.openImage(fl.getAbsolutePath());
		ClusterSession ss = processImage(imp,cg,max,min);
		
		if (cg == null)
		{
			sync = true;
			cg = new ClusterGUI();
		}
		
//		ClusterSession ss = cg.getClusterSession();
		
//		ss.runPluginsOnFrames(null,null, Analysis.out, 180.0, sync);
		return ss;
	}
	
	public void run(String arg, ImagePlus aImp)
	{
		boolean use_cluster = false;
		String wpath = null;
		Font bf = new Font(null, Font.BOLD,12);
		//String sgroup1[] = {"activate second step", ".. with subpixel resolution"};
		//boolean bgroup1[] = {false, false};

		GenericDialog gd = null;
		
		if (!clustermode)
			gd = new NonBlockingGenericDialog("Squassh");
		else
			gd = new GenericDialog("Squassh");
		
		//for rscript generation
		Analysis.p.initrsettings=true;
		
		if (!clustermode)
		{
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			screensizex= (int) screenSize.getWidth();
			screensizey = (int) screenSize.getHeight();
			
			gd.setInsets(-10,0,3);
			
			gd.addTextAreas("Input Image: \n" +
					"insert Path to file or folder, " +
					"or press Button below.", 
					null, 2, 50);
			
			//FlowLayout fl = new FlowLayout(FlowLayout.LEFT,335,3);
			FlowLayout fl = new FlowLayout(FlowLayout.LEFT,75,3);
			Panel p = new Panel(fl);

			Button b = new Button("Select File/Folder");
			b.addActionListener(new FileOpenerActionListener(p,gd, gd.getTextArea1()));
			p.add(b);

			Button bh = new Button("Help");
			bh.addActionListener(new HelpOpenerActionListener(p,gd));
			p.add(bh);

			gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
			
			
			// Image chooser
			
			MosaicUtils.chooseImage(gd,"Image",aImp);
			
			// Background Options

			Button backOption = new Button("Options");
			Label label = new Label("Background subtraction");
			label.setFont(bf);
			p = new Panel();
			p.add(label);
			p.add(backOption);
			backOption.addActionListener(new ActionListener() 
			{

				@Override
				public void actionPerformed(ActionEvent arg0) 
				{
				// TODO Auto-generated method stub
				
					BackgroundSubGUI gds = new BackgroundSubGUI(posx, posy);
					gds.run("");
				}
			});
			gd.addPanel(p);

			// seg Option button

			Button segOption = new Button("Options");
			label = new Label("Segmentation parameters");
			label.setFont(bf);
			p = new Panel();
			p.add(label);
			p.add(segOption);
			segOption.addActionListener(new ActionListener() 
			{

				@Override
				public void actionPerformed(ActionEvent arg0) 
				{
					// TODO Auto-generated method stub

					SegmentationGUI gds = new SegmentationGUI(posx, posy);
					gds.run("");
				}
			});
			gd.addPanel(p);

			Button colOption = new Button("Options");
			label = new Label("Colocalization (two channels images)");
			label.setFont(bf);
			p = new Panel();
			p.add(label);
			p.add(colOption);
			colOption.addActionListener(new ActionListener() 
			{

				@Override
				public void actionPerformed(ActionEvent arg0) 
				{
					// TODO Auto-generated method stub
				
					ColocalizationGUI gds = new ColocalizationGUI(imgch1,imgch2,posx, posy);
					gds.run("");
				}
			});
			gd.addPanel(p);
		
			//gd.addMessage("");

			Button visOption = new Button("Options");
			label = new Label("Vizualization and output");
			label.setFont(bf);
			p = new Panel();
			p.add(label);
			p.add(visOption);
			visOption.addActionListener(new ActionListener() 
			{

				@Override
				public void actionPerformed(ActionEvent arg0) 
				{
					// TODO Auto-generated method stub
				
					VisualizationGUI gds = new VisualizationGUI(posx, posy);
					gds.run("");
				}
			});
			gd.addPanel(p);
			
			gd.addCheckbox("Use cluster", false);
			
			gd.centerDialog(false);
			posx=100;
			posy=120;
			gd.setLocation(posx, posy);
			gd.showDialog();
			if (gd.wasCanceled()) return;
			
			wpath=  gd.getNextText();
			
			Runtime runtime = Runtime.getRuntime();
			int nrOfProcessors = runtime.availableProcessors();
			//IJ.log("Number of processors available to the Java Virtual Machine: " + nrOfProcessors);		
			Analysis.p.nthreads=nrOfProcessors;
			
			use_cluster = gd.getNextBoolean();
		}
		else
		{
			gd.addStringField("Filepath","path",10);
			gd.addStringField("config","path",10);
			
			gd.addNumericField("number of threads", 4, 0);
			
			gd.showDialog();
			if (gd.wasCanceled()) return;
			
			wpath=  gd.getNextString();
			
			Analysis.p.nthreads= (int) gd.getNextNumber();
			try 
			{
				BregmanGLM_Batch.LoadConfig(gd.getNextString());
			} 
			catch (ClassNotFoundException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if(Analysis.p.mode_voronoi2)
		{
			//betamleout to be determined by clustering of whole image

			Analysis.p.betaMLEindefault=1;						
			//Analysis.p.betaMLEoutdefault=0.1;	
			Analysis.p.regionthresh=Analysis.p.min_intensity;
			Analysis.p.regionthreshy=Analysis.p.min_intensityY;
			Analysis.p.refinement=true;
			Analysis.p.max_nsb=151;
			Analysis.p.regionSegmentLevel=1;//not used
			Analysis.p.dispvoronoi=Analysis.p.debug;
			Analysis.p.minves_size=2;
		}


		if(!Analysis.p.subpixel)
		{
			Analysis.p.oversampling2ndstep=1;
			Analysis.p.interpolation=1;
		}
		
		if (use_cluster == false && clustermode == false)
			Analysis.p.dispwindows = true;
		
		// Two different way to run the Task
		
		if (clustermode || use_cluster == false)
		{
			// We run locally
			
			BLauncher hd = null;
		
			if (wpath.startsWith("Input Image:"))
			{
				if (aImp == null)
				{
					IJ.error("No image to process");
					return;
				}
				hd= new BLauncher(aImp);
				
				String savepath;
				Analysis.p.wd = MosaicUtils.ValidFolderFromImage(aImp);
				if (wpath.startsWith("Input Image") == false)
					savepath =  wpath.substring(0,wpath.length()-4);
				else
				{
					savepath = Analysis.p.wd;
				}
				
				MosaicUtils.reorganize(Analysis.out_w,aImp.getShortTitle(),savepath,aImp.getNFrames());
				
				CSVOutput.Stitch(Analysis.out_w,"",new File(savepath),MosaicUtils.ValidFolderFromImage(aImp) + aImp.getTitle());
			}
			else
				hd= new BLauncher(wpath);

		//		    hd.bcolocheadless(imagePlus);*/
			

		}
		else
		{
			// We run on cluster
			
			try 
			{
				// Copying parameters
				
				Parameters p = new Parameters(Analysis.p);
				
				// disabling display options
				
				p.dispwindows = false;
				BregmanGLM_Batch.SaveConfig(p,"/tmp/spb_settings.dat");
			}
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Check if we selected a directory
			
			ClusterSession ss = null;
			File[] fileslist = null;
			
			if (aImp == null)
			{
				File fl = new File(wpath);
				if (fl.isDirectory() == true)
				{
					// we have a directory
					
					fileslist = fl.listFiles();
					
					ss = processFiles(fileslist);
				}
				else if (fl.isFile())
				{
					ss = processFile(fl,null,new Float(0.0),new Float(0.0));
				}
				else
				{
					ss = processImage(null,null,new Float(0.0),new Float(0.0));
				}
			}
			else
			{
				// It is a file
				
				ss = processImage(aImp,null,new Float(0.0),new Float(0.0));
			}
			
			// Get output format and Stitch the output in the output selected
			
			processJobsData(ss,aImp);
			
			////////////////
		}
		
		//Analysis.load2channels(imagePlus);
	}

	class FileOpenerActionListener implements ActionListener
	{
		GenericDialog gd;
		TextArea ta;
		Panel pp;

		public FileOpenerActionListener(Panel p,GenericDialog gd, TextArea ta)
		{
			this.gd=gd;
			this.ta=ta;
			this.pp=p;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if(imgch1!=null){imgch1.close();imgch1=null;}
			if(imgch2!=null){imgch2.close();imgch2=null;}// close previosuly opened images 
			
			String path;
			//with JFileChooser
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.showOpenDialog(null);
			File selFile = fc.getSelectedFile();
			path=selFile.getAbsolutePath();



			boolean processdirectory =(new File(path)).isDirectory();
			if(!processdirectory)
			{

				ImagePlus img2=IJ.openImage(path);




				ni=img2.getWidth();
				nj=img2.getHeight();
				nz=img2.getNSlices();
				nc=img2.getNChannels();

				int Amin=(int) Double.POSITIVE_INFINITY;
				int Amax=0;

				int Bmin=(int) Double.POSITIVE_INFINITY;;
				int Bmax=0;


				int bits = img2.getBitDepth();

				imgch1=new ImagePlus();
				ImageStack imga_s= new ImageStack(ni,nj);

				//channel 1
				for (int z=0; z<nz; z++){  
					img2.setPosition(1,z+1,1);

					ImageStatistics st1=img2.getStatistics();
					Amin=Math.min(Amin, (int) st1.min);
					Amax=Math.max(Amax, (int) st1.max);

					ImageProcessor impt;
					if(bits==32)
						impt=img2.getProcessor().convertToShort(false);
					else
						impt = img2.getProcessor();
					imga_s.addSlice("", impt);
				}




				imgch1.setStack(img2.getTitle(),imga_s);
				//imgA.setTitle("A2");
				imgch1.setTitle(imgch1.getShortTitle() + " ch1");

//				ImageStatistics st1=imgch1.getStatistics();
				//IJ.log("min " + Amin + "max " +Amax);

				imgch1.setDisplayRange(Amin,Amax);

				imgch1.show("");
				GenericGUI.setimagelocation(650,30,imgch1);


				if(nc>1){
					imgch2=new ImagePlus();
					ImageStack imgb_s= new ImageStack(ni,nj);

					//channel 2
					for (int z=0; z<nz; z++){  
						img2.setPosition(2,z+1,1);	
						ImageProcessor impt;

						ImageStatistics st2=img2.getStatistics();
						Bmin=Math.min(Bmin, (int) st2.min);
						Bmax=Math.max(Bmax, (int) st2.max);

						if(bits==32)
							impt=img2.getProcessor().convertToShort(false);
						else
							impt = img2.getProcessor();
						imgb_s.addSlice("", impt);
					}

					imgch2.setStack(img2.getTitle(),imgb_s);

					imgch2.setTitle(imgch2.getShortTitle() + " ch2");

					imgch2.setDisplayRange(Bmin,Bmax);

					imgch2.show("");
					GenericGUI.setimagelocation(650,610,imgch2);
				}


				
			}



			if(path!=null)//(file!=null && dir!=null)
			{
				ta.setText(path);
			}
		}
	}

	class PSFOpenerActionListener implements ActionListener
	{
		GenericDialogCustom gd;
		TextArea taxy;
		TextArea taz;
		Panel pp;

		public PSFOpenerActionListener(Panel p, GenericDialogCustom gd)
		{
			this.gd=gd;
			//this.ta=ta;
			this.pp=p;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{

			Point p =gd.getLocationOnScreen();
			//IJ.log("plugin location :" + p.toString());
			PSFWindow hw = new PSFWindow(p.x, p.y, gd);

		}
	}


	class HelpOpenerActionListener implements ActionListener
	{
		GenericDialog gd;

		Panel pp;

		public HelpOpenerActionListener(Panel p,GenericDialog gd)
		{
			this.gd=gd;

			this.pp=p;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Point p =gd.getLocationOnScreen();
			//IJ.log("plugin location :" + p.toString());
			Helpwindow hw = new Helpwindow(p.x, p.y);
		}
	}

	public class Helpwindow extends HelpGUI
	{
		JFrame frame;
		JPanel panel;
		
		public Helpwindow(int x, int y )
		{
			frame = new JFrame();
			frame.setSize(555, 480);
			frame.setLocation(x+500, y-50);
			//frame.toFront();
			//frame.setResizable(false);
			//frame.setAlwaysOnTop(true);

			panel= new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
			panel.setPreferredSize(new Dimension(575, 720));

			JPanel pref= new JPanel(new GridBagLayout());
//			pref.setPreferredSize(new Dimension(555, 550));
//			pref.setSize(pref.getPreferredSize());
			
			setPanel(pref);
			setHelpTitle("Squassh");
			createTutorial(null);
			createArticle("http://mosaic.mpi-cbg.de/docs/Paul2013a.pdf");
			String desc = new String("Background subtraction is performed first, as the segmentation model assumes locally " +
                                    "homogeneous intensities. Background variations are non-specific signals that are not accounted for by " +
									"this model. We subtract the image background using the rolling-ball algorithm");
			createField("Background subtraction",desc,null);
			desc = new String("Model-based segmentation aim at finding the segmentation that best explains the image. In other words, " +
									"they compute the segmentation that has the highest probability of resulting in the actually observed " +
									"image when imaged with the specific microscope used.");
			createField("Segmentation",desc,null);
			desc = new String("Object-based colocalization is computed after segmenting the objects using information about the shapes " +
								"and intensities of all objects in both channels. This allows straightforward calculation of the degree of " +
								"overlap between objects from the different channels.");
			createField("Colocalization",desc,null);
			desc = new String("Select one or more output and visualization options");
			createField("Visualization and output",desc,null);
			
			//JPanel panel = new JPanel(new BorderLayout());

			panel.add(pref);
			//panel.add(label, BorderLayout.NORTH);


			frame.add(panel);

			frame.setVisible(true);
		}

	}






	




}
