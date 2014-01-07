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
//import ij.gui.NonBlockingGenericDialog;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

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


	public void run(String arg, ImagePlus aImp)
	{
		Font bf = new Font(null, Font.BOLD,12);
		//String sgroup1[] = {"activate second step", ".. with subpixel resolution"};
		//boolean bgroup1[] = {false, false};

		NonBlockingGenericDialog  gd = new NonBlockingGenericDialog("Squassh");
		
		//for rscript generation
		Analysis.p.initrsettings=true;
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		screensizex= (int) screenSize.getWidth();
		screensizey = (int) screenSize.getHeight();
		
		
		gd.setInsets(-10,0,3);
		if(!clustermode)
		{
			gd.addTextAreas("Input Image: \n" +
					"insert Path to file or folder, " +
					"or press Button below.", 
					null, 2, 50);
		}
		if(clustermode)
		{
			gd.addStringField("Filepath","path",10);
			gd.addStringField("config","path",10);
		}

		//panel with button
		if(!clustermode)
		{
			//FlowLayout fl = new FlowLayout(FlowLayout.LEFT,335,3);
			FlowLayout fl = new FlowLayout(FlowLayout.LEFT,75,3);
			Panel p = new Panel(fl);
//			p.setPreferredSize(new Dimension(565, 30));
			//p.setLayout(null);
			//p.setBackground(Color.black);

//			Button b = new Button("Preview cell mask");
//			b.addActionListener(new HelpOpenerActionListener(p,gd));
//			p.add(b);

			Button b = new Button("Select File/Folder");
			b.addActionListener(new FileOpenerActionListener(p,gd, gd.getTextArea1()));
			p.add(b);

			Button bh = new Button("Help");
			bh.addActionListener(new HelpOpenerActionListener(p,gd));
			p.add(bh);

			gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
		}

		// Image chooser
		
		int nOpenedImages = 0;
		int[] ids = WindowManager.getIDList();
		
		if(ids!=null){
			nOpenedImages = ids.length;
		}
		
		
		String[] names = new String[nOpenedImages+1];
		names[0]="";
		for(int i = 0; i<nOpenedImages; i++)
		{
			ImagePlus ip = WindowManager.getImage(ids[i]);
			names[i+1] = ip.getTitle();
		}
		
//		if(nOpenedImages>0)

			// Input Image
		if(!clustermode)
		{
			gd.addChoice("InputImage", names, names[0]);
			Choice choiceInputImage = (Choice)gd.getChoices().lastElement();
			if(aImp !=null)
			{
				String title = aImp.getTitle();
				choiceInputImage.select(title);
			}
		}

		if(!clustermode)
		{
			// Background Options

			Button backOption = new Button("Options");
			Label label = new Label("Background subtraction");
			label.setFont(bf);
			Panel p = new Panel();
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
		}
			
//		if(!clustermode)gd.addMessage("Advanced options ",bf);
		if(clustermode)
		{
			gd.addNumericField("number of threads", 4, 0);
		}
		
		if (!clustermode)
		{
			gd.addCheckbox("Use cluster", false);
		}
		
		gd.centerDialog(false);
		posx=100;
		posy=120;
		gd.setLocation(posx, posy);
		gd.showDialog();
		if (gd.wasCanceled()) return;


		String wpath;
		if(clustermode)
		{
			//IJ.log("cluster: " );
			wpath=  gd.getNextString();
		}
		else
		{
			//IJ.log("no cluster");
			wpath=  gd.getNextText();
			//wpath=  gd.getTextArea1().getText();
		}
		//IJ.log("path: " + wpath);

/*		//general options	
		Analysis.p.removebackground=gd.getNextBoolean();
		//IJ.log("rem back:" +  Analysis.p.removebackground);
		Analysis.p.size_rollingball=(int) gd.getNextNumber();
		//Analysis.p.usePSF=gd.getNextBoolean();*/

		//Expert options

		if(clustermode)
		{
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
		else
		{
			Runtime runtime = Runtime.getRuntime();
			int nrOfProcessors = runtime.availableProcessors();
			//IJ.log("Number of processors available to the Java Virtual Machine: " + nrOfProcessors);		
			Analysis.p.nthreads=nrOfProcessors;
		}

		//Analysis.p.regionSegmentLevel= (int) gd.getNextNumber();
//		Analysis.p.mode_intensity=gd.getNextChoiceIndex();


//		Analysis.p.noise_model=gd.getNextChoiceIndex();
		//IJ.log("noise model" + Analysis.p.noise_model);

		//		Analysis.p.betaMLEoutdefault=gd.getNextNumber();
		//		Analysis.p.betaMLEindefault=gd.getNextNumber();
		//		Analysis.p.automatic_int= gd.getNextBoolean();


		//parameters for cluster(chekboxes not working)

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


			//Analysis.p.mode_intensity=2;

			//Analysis.p.subpixel=false;

			//estimation des intensit�s des zones ?
			//dans ce mode il ne faut pas thresholder le masque pour avoir les objets (car il est calcul� avec int=1)
			// -> calculer les objets en faisant tourner l'algo dans la zone voronoi
			//par RSS ou par clustering ?
		}

		if(clustermode)
		{
			IJ.log("clustermode");
			Analysis.p.removebackground=true;
			//Analysis.p.automatic_int=false;
			if(Analysis.p.thresholdcellmask >0)
				Analysis.p.usecellmaskX=true;
			else
				Analysis.p.usecellmaskX=false;
			if(Analysis.p.thresholdcellmasky >0)
				Analysis.p.usecellmaskY=true;
			else
				Analysis.p.usecellmaskY=false;
			Analysis.p.livedisplay=false;
			Analysis.p.dispcolors=true;
			//Analysis.p.dispint=true;
			//Analysis.p.displabels=true;
			//Analysis.p.dispoutline=true;
			Analysis.p.save_images=true;

			//Analysis.p.nthreads=4;
		}



		if(!Analysis.p.subpixel){Analysis.p.oversampling2ndstep=1;
		Analysis.p.interpolation=1;
		}

		//IJ.log("subpixel" + Analysis.p.subpixel);


		//test Gaussian PSF 
		//		double r=235;
		//		GaussianPSFModel psf= new GaussianPSFModel(500,1.4,r,1.3);
		//		IJ.log("235");
		//		IJ.log("Lateral WFFM :" + psf.lateral_WFFM());
		//		IJ.log("Axial WFFM :" + psf.axial_WFFM());
		//		IJ.log("Lateral LSCM :" + psf.lateral_LSCM());
		//		IJ.log("Axial LSCM :" + psf.axial_LSCM());
		//		


		//		double r=0.5;
		//		GaussianPSFModel psf= new GaussianPSFModel(500,500,1.3,r,1.46);
		//		IJ.log("0.5");
		//		IJ.log("Lateral WFFM :" + psf.lateral_WFFM());
		//		IJ.log("Axial WFFM :" + psf.axial_WFFM());
		//		IJ.log("Lateral LSCM :" + psf.lateral_LSCM());
		//		IJ.log("Axial LSCM :" + psf.axial_LSCM());
		//
		//		IJ.log("stdx" +Analysis.p.sigma_gaussian+ "stdy" + Analysis.p.sigma_gaussian/Analysis.p.zcorrec);

		if (clustermode || gd.getNextBoolean() == false)
		{
		
			BLauncher hd = null;
		
			if (wpath.startsWith("Input Image:"))
				hd= new BLauncher(aImp);
			else
				hd= new BLauncher(wpath);
		//		    hd.bcolocheadless(imagePlus);*/
			
			String outcsv[] = {"*_ObjectsData_c1.csv","*_mask_c1.zip","*_ImagesData.csv","*_outline_overlay_c1.zip","*_seg_c1_RGB.zip","*.tif"};
			
			String savepath;
			Analysis.p.wd = MosaicUtils.ValidFolderFromImage(aImp);
			if (wpath.startsWith("Input Image") == false)
				savepath =  wpath.substring(0,wpath.length()-4);
			else
			{
				savepath = Analysis.p.wd;
			}	
			
			MosaicUtils.reorganize(outcsv,"",savepath,aImp.getNFrames());
			
			CSVOutput.Stitch(outcsv,"",new File(savepath),MosaicUtils.ValidFolderFromImage(aImp) + aImp.getTitle());
		}
		else
		{
			ClusterGUI cg = new ClusterGUI();
			ClusterSession ss = cg.getClusterSession();
			try 
			{
				BregmanGLM_Batch.SaveConfig(Analysis.p,"/tmp/spb_settings.dat");
			} 
			catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String out[] = {"*_ObjectsData_c1.csv","*_mask_c1.zip","*_ImagesData.csv","*_outline_overlay_c1.zip","*_seg_c1_RGB.zip","*.tif"};
			ss.runPluginsOnFrames(aImp, "", out, 180.0);
			
			// Save all JobID to the image folder
			// or ask for a directory
			
			String dir[] = ss.getJobDirectories(0);
			
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
						CSVOutput.Stitch(outcsv,"tmp_",new File(dir[i]), MosaicUtils.ValidFolderFromImage(aImp));
						
						///////
						
						String[] tmp = dir[i].split(File.separator);
						
						File t = new File(dirS + File.separator + tmp[tmp.length-1]);
						
						ShellCommand.exeCmdNoPrint("cp -r " + dir[i] + " " + t);
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
			
			// Get output format and Stitch the output in the output selected
			
			
			
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

			//			Point p =gd.getLocationOnScreen();
			//			IJ.log("plugin location :" + p.toString());
			//			JFrame frame;
			//			frame = new JFrame();
			//			frame.setSize(300, 700);
			//			frame.setLocation(p.x+900, p.y);
			//			frame.toFront();
			//			frame.requestFocus();
			//			frame.setResizable(false);
			//			frame.setVisible(true);
			//			frame.repaint();
			//			frame.setAlwaysOnTop( true );
			//			IJ.log("frame location :" + frame.getLocationOnScreen().toString() + "focusable " + frame.isFocusableWindow());

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


//	class MaskOpenerActionListener implements ActionListener
//	{
//		GenericDialogCustom gd;
//		TextArea taxy;
//		TextArea taz;
//		Panel pp;
//
//		public MaskOpenerActionListener(Panel p, GenericDialogCustom gd)
//		{
//			this.gd=gd;
//			//this.ta=ta;
//			this.pp=p;
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e)
//		{
//			//IJ.log("plugin location :" + p.toString());
//			MaskWindow hw = new MaskWindow(0, 0);
//
//		}
//	}


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

			//			JFrame frame;
			//			frame = negit rm deletew JFrame();
			//			frame.setSize(300, 700);
			//			frame.setLocation(p.x+900, p.y);
			//			frame.toFront();
			//			frame.requestFocus();
			//			frame.setResizable(false);
			//			frame.setVisible(true);
			//			frame.repaint();
			//			frame.setAlwaysOnTop(true);
			//IJ.log("frame location :" + frame.getLocationOnScreen().toString() + "focusable " + frame.isFocusableWindow());

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




/*			panel= new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
			panel.setPreferredSize(new Dimension(575, 890));

			JLabel label = new JLabel();
			label.setText("<html>"
					//+ "<h3>**File selection**</h3>"
					+"<div align=\"center\">"
					+"<h4> Subcellular object segmentation and features extraction plugin : volume, surface, length, intensity and overlap with other channel. </h4>"
					+"</div>"
					+"<br>"
					+"<div align=\"justify\">"
					+ "<h4>**File selection**</h4>"
					+ "� Select file (processes one or two channels, 2D or Z-stacks image file), select folder (processes all image files contained) or paste path to file/folder."
					+"<br>"
					+"<br>"
					+"<h4>**Segmentation**</h4>"
					+"� Remove background: performs background removal through rolling ball method."
					+" If activated set size of rolling ball window in pixels. Should be larger than searched objects diameter."
					+"<br>"
					+"� Point spread function (PSF): set standard deviation in pixels of the gaussian distribution (in x/y and in z) used to approximate microscope's PSF. Use 'Estimate PSF' to compute these values from microscope objective characteristics."
					+"<br>"
					+"� Regularization: penalty weight on object lengths. Low values enable to separate and find smaller objects, higher values prevents over fitting and remove small objects (which can be only noise) whose intensity do not differ enough from the background."
					+"<br>"
					+"� Minimum object intensity: minimum intensity of objects looked for. Intensities values are normalized between 0 (minimum pixel intensity in the image) and 1 (maximum intensity)."
					+"<br>"
					+"� Subpixel segmentation: performs segmentation in subpixel resolution. (resolution x8 in 2D images, resolution x4 in Z-stacks images)."
					+"<br>"
					+"<br>"
					+"<h4>**Colocalization**</h4>"
					+"� Cell mask: computes a mask of positive cells in a channel. Mask is obtained by thresholding original image with given intensity value and holes filling. Useful to perform colocalization analysis only in cells positive for both channels."
					+"<br>"
					+"� Threshold values for channel 1 and 2 used to compute cell masks. Use 'Preview Cell Masks' to find appropriate value."
					+"<br>"
					+"<br>"
					+ "<h4>**Visualisation and output**</h4>"
					+"� 'Intermediate steps' display intermediate segmentation steps as well as segmentation energies in the log window. Colorized objects, intensities, labels and outlines respectively display segmented objects with random colors, objects intensities (black green for low intensity to bright green for high intensity ), objects integer labels, and outlines of objects overlaid with initial image."
					+"<br>"
					+"� Save option saves output images as zipped tiff files (openable in imageJ) and segmented objects data in one .csv file for each channel in folder containing processed image file."
					//+"First data column is the image number the object belongs to. Columns 2 to 6 are object label size, surface, length and intensity. Coord X, Y and Z are the coordinates of the center of mass of the object. For two channels images overlap gives the fraction of the object overlapping with objects in the other channel while Mcoloc size and Mcoloc Intensity give the mean size and intensity of the overlapping objects in the other channel. 'single coloc' is true if the object is overlapping with a single object."
					+"When processing a folder a third .csv file is written with one line per image processed indicating image name, image number, mean number of objects, mean object size and overlap."
					//+"</p>"
					+"<br>"
					+"<br>"
					+ "<h4>**Advanced options**</h4>"
					+"� Intensity estimation modes : useful when objects are made of several intensity layers. High layer mode only segments highest intensity portions of an object and thus also increases object separation."
					+"</div>"
					+ "</html>");

			//JPanel panel = new JPanel(new BorderLayout());

			JPanel pref= new JPanel(new BorderLayout());
			pref.setPreferredSize(new Dimension(555, 850));
			pref.setSize(pref.getPreferredSize());
			pref.add(label);


			panel.add(pref);*/
			//panel.add(label, BorderLayout.NORTH);



			//			panel.add(h1);
			//			panel.add(h2);
			//			panel.add(h3);
			//			panel.add(h4);
			//			panel.add(h5);
			//close
			//			Close = new JButton("Close");
			//			Close.setSize(Close.getPreferredSize());
			//
			//			panel.add(Close);
			//			Close.addActionListener(this);


/*			frame.add(panel);


			//frame.repaint();

			frame.setVisible(true);*/
			//frame.requestFocus();
			//frame.setAlwaysOnTop(true);

			//			JOptionPane.showMessageDialog(frame,
			//				    "Eggs are not supposed to be green.\n dsfdsfsd",
			//				    "A plain message",
			//				    JOptionPane.PLAIN_MESSAGE);


		}

	}






	




}
