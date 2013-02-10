package mosaic.bregman;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.TextField;
//import java.awt.datatransfer.DataFlavor;
//import java.awt.datatransfer.Transferable;
//import java.awt.dnd.DnDConstants;
//import java.awt.dnd.DropTarget;
//import java.awt.dnd.DropTargetDragEvent;
//import java.awt.dnd.DropTargetDropEvent;
//import java.awt.dnd.DropTargetEvent;
//import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


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
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
//import ij.gui.NonBlockingGenericDialog;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class GenericGUI {
	boolean clustermode;
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;

	public GenericGUI(boolean  	mode){
		clustermode=mode;
		//clustermode=true;
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


	public void run(String arg) {
		Font bf = new Font(null, Font.BOLD,12);
		String sgroup1[] = {"activate second step", ".. with subpixel resolution"};
		boolean bgroup1[] = {false, false};



		GenericDialogCustom  gd = new GenericDialogCustom("Bregman Segmentation");

		//GenericDialog  gd = new GenericDialog("Bregman Segmentation");
		//		gd.addMessage("");
		//		gd.addMessage("Image __________________", new Font(null, Font.BOLD,12));
		//		gd.addMessage("");

		gd.setInsets(-10,0,3);
		if(!clustermode){
			gd.addTextAreas("Input Image: \n" +
					"insert Path to file or folder, " +
					"or press Button below.", 
					null, 2, 90);
		}
		if(clustermode){
			gd.addStringField("Filepath","path",10);
		}

		//panel with button
		if(!clustermode){
			//FlowLayout fl = new FlowLayout(FlowLayout.LEFT,335,3);
			FlowLayout fl = new FlowLayout(FlowLayout.LEFT,75,3);
			Panel p = new Panel(fl);
			p.setPreferredSize(new Dimension(865, 30));
			//p.setLayout(null);
			//p.setBackground(Color.black);


			Button b = new Button("Select File/Folder");
			b.addActionListener(new FileOpenerActionListener(p,gd, gd.getTextArea1()));
			p.add(b);


			Button bp = new Button("Estimate PSF");
			bp.addActionListener(new PSFOpenerActionListener(p,gd));
			p.add(bp);

			Button bp3 = new Button("Preview Cell Masks");
			bp3.addActionListener(new MaskOpenerActionListener(p,gd));
			p.add(bp3);

			Button bh = new Button("Help");
			bh.addActionListener(new HelpOpenerActionListener(p,gd));
			p.add(bh);

			gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));


		}


		//		if(!clustermode){
		//			Panel p = new Panel();
		//			Button b = new Button("Estimate PSF");
		//			b.addActionListener(new PSFOpenerActionListener(p,gd));
		//			p.add(b);
		//			gd.addPanel(p, GridBagConstraints.EAST, new Insets(-5, 0, 0, 0));
		//		}

		if(!clustermode)gd.addMessage("Segmentation parameters __________________________________________________________________________________________________________________",bf);

		if(!clustermode)gd.addCheckbox("Remove background", true);
		else gd.addCheckbox("Remove background", false);

		gd.addNumericField("rolling ball window size (in pixels)", 10, 0);
		//gd.addMessage("Load PSF");

		gd.addNumericField("Gaussian_PSF_approximation,_stddev_xy  (in pixels)", 0.9, 2);
		gd.addNumericField("Gaussian_PSF_approximation,_stddev_z (in pixels)", 0.8, 2);


		//		Button bh = new Button("PSF estimation");
		//		bh.addActionListener(new PSFOpenerActionListener(p,gd, gd.getTextArea1()));
		//		p.add(bh);
		//		gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));

		gd.addNumericField("Regularization (>0)", 0.15, 3);

		//gd.addNumericField("Minimum_object_size: ", 5, 0);
		//gd.addNumericField("Maximum_object_size: ", -1, 0);
		gd.addNumericField("Minimum_object_intensity,_channel_1 (0 to 1)", 0.15, 3);
		gd.addNumericField("Minimum_object_intensity,_channel_2 (0 to 1)", 0.15, 3);
		//gd.addNumericField("Minimum_object_intensity_channel_2: ", 0.2, 2);

		//if(!clustermode)gd.addMessage("First segmentation step ______________________________________________________________________________________________________________________________",bf);


		//gd.addCheckbox("Local thresholding", true);
		//gd.addNumericField("Voronoi thresh: ", 0.2, 2);


		//add threshold value

		//if(!clustermode)gd.addMessage("Second segmentation step (object segmentation refinement)_________________________________________________________________________________________",bf);
		//gd.addCheckboxGroup(1, 2, sgroup1, bgroup1);
		gd.addCheckbox("Subpixel segmentation", false);
		//gd.addCheckbox("Segmentation refinement", false);
		//gd.addCheckbox(" .. with subpixel", false);
		//gd.addNumericField("model oversampling", 2, 0);
		//gd.addNumericField("soft mask interpolation", 4, 0);



		if(!clustermode)gd.addMessage("Colocalization options (for two channels images)_________________________________________________________________________________________",bf);
		String sgroup3[] = {"Cell_mask_channel_1", "Cell_mask_channel_2"};
		boolean bgroup3[] = {false, false};

		//gd.addCheckbox("Cell_mask_channel_1", false);
		//gd.addCheckbox("Cell_mask_channel_2", false);
		gd.addCheckboxGroup(1, 2, sgroup3, bgroup3);
		gd.addNumericField("threshold_channel_1 (0 to 1)", 0.0015, 4);
		gd.addNumericField("threshold_channel_2 (0 to 1)", 0.035, 4);
		//gd.addNumericField("Number of levels",2,0);


		//gd.addMessage("Select file or folder:");
		//gd.addStringField("Path: ", "/working_directory",30);
		//if(!clustermode)gd.addMessage("");
		if(!clustermode)gd.addMessage("Vizualization and output _________________________________________________________________________________________________________________",bf);
		//gd.addMessage("");

		String sgroup2[] = {
				"Intermediate steps", "Colorized objects","Objects intensities",
				"Labelized objects","Outlines overlay","Save objects characteristics and images"};
		boolean bgroup2[] =
			{
				false, false,false,
				false,true,false
			};
		gd.addCheckboxGroup(2, 3, sgroup2, bgroup2);
		//		gd.addCheckbox("Live segmentation",true);
		//		gd.addCheckbox("Random color objects",true);
		//		gd.addCheckbox("Intensities reconstruction",false);
		//		gd.addCheckbox("Objects labels",false);
		//		gd.addCheckbox("Outline overlay",false);
		//		gd.addCheckbox("Display colocalization",false);
		//		
		//		gd.addCheckbox("Save object data in .csv file and save images", false);

		if(!clustermode)gd.addMessage("Advanced options____________________________________________________________________________________________________________________________",bf);
		if(clustermode){
			gd.addNumericField("number of threads", 4, 0);
		}
		String choice1[] = {
				"Automatic (best energy)", "Low layer", "Medium layer (clustering)","High layer (clustering)"};
		gd.addChoice("Local intensity_estimation ", choice1, "Automatic");


		String choice2[] = {
				"Poisson", "Gauss"};
		gd.addChoice("Noise Model ", choice2, "Poisson");





		//		gd.addMessage("todo :");
		//		gd.addMessage("noise choice");
		//		gd.addMessage("load PSF");
		//	gd.addNumericField("Intensitiy layer (0, 1 or 2): ", 0, 0);


		//		gd.addNumericField("Background intensity (0 to 1): ", 0.003, 3);
		//		gd.addNumericField("Objects intensity (0 to 1): ", 0.3, 3);
		//		gd.addCheckbox("automatic intensities ", false);


		gd.showDialog();
		if (gd.wasCanceled()) return;

		//int mode= gd.getNextChoiceIndex();
		//IJ.log("mode + mode");

		GraphicsEnvironment ge = 
				GraphicsEnvironment.getLocalGraphicsEnvironment(); 
		boolean headless_check = ge.isHeadless();
		//IJ.log("headless check: " + headless_check);



		String wpath;
		if(clustermode){
			//IJ.log("cluster: " );
			wpath=  gd.getNextString();
		}
		else{
			//IJ.log("no cluster");
			wpath=  gd.getNextText();
			//wpath=  gd.getTextArea1().getText();
		}
		//IJ.log("path: " + wpath);

		//general options	
		Analysis.p.removebackground=gd.getNextBoolean();
		//IJ.log("rem back:" +  Analysis.p.removebackground);
		Analysis.p.size_rollingball=(int) gd.getNextNumber();
		//Analysis.p.usePSF=gd.getNextBoolean();
		Analysis.p.sigma_gaussian=gd.getNextNumber();
		Analysis.p.zcorrec=Analysis.p.sigma_gaussian/gd.getNextNumber();

		//IJ.log("nbthreads:" +  Analysis.p.nthreads);
		Analysis.p.lreg= gd.getNextNumber();

		//Analysis.p.minves_size=(int) gd.getNextNumber();
		//Analysis.p.maxves_size=(int) gd.getNextNumber();
		Analysis.p.min_intensity=gd.getNextNumber();
		//Analysis.p.min_intensityY=Analysis.p.min_intensity;
		Analysis.p.min_intensityY=gd.getNextNumber();


		//first step

		//IJ.log("aut int:" +  Analysis.p.automatic_int);
		//Analysis.p.findregionthresh= gd.getNextBoolean();
		//Analysis.p.regionthresh= gd.getNextNumber();



		//second step
		//Analysis.p.refinement= gd.getNextBoolean();
		//IJ.log("refinement:" +  Analysis.p.refinement);
		//IJ.log("subpixel before setting" + Analysis.p.subpixel);

		Analysis.p.subpixel= gd.getNextBoolean();
		//IJ.log("subpixel:" +  Analysis.p.subpixel);
		//Analysis.p.oversampling2ndstep=(int) gd.getNextNumber();
		//Analysis.p.interpolation=(int) gd.getNextNumber();

		//colocalization step
		//IJ.log("subpixel after setting" + Analysis.p.subpixel);

		Analysis.p.usecellmaskX= gd.getNextBoolean();
		//IJ.log("maskX:" +  Analysis.p.usecellmaskX);
		Analysis.p.usecellmaskY= gd.getNextBoolean();
		//IJ.log("maskY:" +  Analysis.p.usecellmaskY);
		Analysis.p.thresholdcellmask= gd.getNextNumber();
		Analysis.p.thresholdcellmasky= gd.getNextNumber();
		//Analysis.p.nlevels=(int) gd.getNextNumber();

		Analysis.p.save_images= true;

		//Vizualization
		Analysis.p.livedisplay= gd.getNextBoolean();
		//IJ.log("live:" +  Analysis.p.livedisplay);
		Analysis.p.dispcolors= gd.getNextBoolean();
		//IJ.log("colors:" +  Analysis.p.dispcolors);
		Analysis.p.dispint= gd.getNextBoolean();
		//IJ.log("dispint:" +  Analysis.p.dispint);
		Analysis.p.displabels= gd.getNextBoolean();
		//IJ.log("displabels:" +  Analysis.p.displabels);
		Analysis.p.dispoutline= gd.getNextBoolean();
		//IJ.log("dispoutline:" +  Analysis.p.dispoutline);
		//Analysis.p.dispcoloc= gd.getNextBoolean();

		Analysis.p.save_images= gd.getNextBoolean();
		//IJ.log("save images:" +  Analysis.p.save_images);
		//IJ.log(Analysis.p.wd);
		//Analysis.p.dispvesicles = false;


		//		      ImagePlus imagePlus = IJ.getImage();
		//				if (imagePlus == null) {
		//					IJ.error("No image to operate on.");
		//					return;
		//				}
		//IJ.log("path" +wpath);
		//IJ.log("disp colors" + Analysis.p.dispcolors);

		//Expert options

		if(clustermode){
			Analysis.p.nthreads= (int) gd.getNextNumber();
		}
		else{
			Runtime runtime = Runtime.getRuntime();
			int nrOfProcessors = runtime.availableProcessors();
			//IJ.log("Number of processors available to the Java Virtual Machine: " + nrOfProcessors);		
			Analysis.p.nthreads=nrOfProcessors;
		}

		//Analysis.p.regionSegmentLevel= (int) gd.getNextNumber();
		Analysis.p.mode_intensity=gd.getNextChoiceIndex();


		Analysis.p.noise_model=gd.getNextChoiceIndex();
		//IJ.log("noise model" + Analysis.p.noise_model);

		//		Analysis.p.betaMLEoutdefault=gd.getNextNumber();
		//		Analysis.p.betaMLEindefault=gd.getNextNumber();
		//		Analysis.p.automatic_int= gd.getNextBoolean();


		//parameters for cluster(chekboxes not working)

		if(Analysis.p.mode_voronoi2){
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

			//estimation des intensitŽs des zones ?
			//dans ce mode il ne faut pas thresholder le masque pour avoir les objets (car il est calculŽ avec int=1)
			// -> calculer les objets en faisant tourner l'algo dans la zone voronoi
			//par RSS ou par clustering ?
		}

		//IJ.log("nt " + Analysis.p.nthreads);

		//		IJ.log("Read parameters : " + 
		//				//Analysis.p.removebackground=gd.getNextBoolean();
		//				//Analysis.p.size_rollingball=(int) gd.getNextNumber();
		//				"wpath" + wpath + "\n" + 
		//				"remove" + Analysis.p.removebackground + "\n" +
		//				"rolling " +Analysis.p.size_rollingball+ "\n" + 
		//				"sigma " +Analysis.p.sigma_gaussian+ "\n" + 
		//				"sigmaz " +Analysis.p.zcorrec+ "\n" + 
		//				"lreg " +Analysis.p.lreg+ "\n" + 
		//				"min_int " +Analysis.p.min_intensity+ "\n" + 
		//
		//				"subpixel " +Analysis.p.subpixel+ "\n" +
		//				"cellmaskx "+Analysis.p.usecellmaskX+ "\n" + 
		//				"cellmasky " +Analysis.p.usecellmaskY+ "\n" + 
		//				"tcellmaskx " +Analysis.p.thresholdcellmask+ "\n" + 
		//				"tcellmasky " +Analysis.p.thresholdcellmasky+ "\n" + 
		//				"live " +Analysis.p.livedisplay+ "\n" + 
		//				"colors " +Analysis.p.dispcolors+ "\n" + 
		//				"intensity " +Analysis.p.dispint+ "\n" + 
		//				"labels " +Analysis.p.displabels+ "\n" + 
		//				"outline " +Analysis.p.dispoutline+ "\n" + 
		//				"save " +Analysis.p.save_images+ "\n" + 
		//				"nthreads " +Analysis.p.nthreads+ "\n" + 
		//				"mode " +Analysis.p.mode_intensity+ "\n" + 
		//				"noise model " +Analysis.p.noise_model
		//				);

		//		IJ.log(" ");
		////		IJ.log("test get value" +
		////		getValue("filepath=/gpfs/home/rizk_a/Serotonin/180minsub/ rolling=10 gaussian_psf_" +
		////				"approximation,_stddev_xy=0.90 gaussian_psf_approximation,_stddev_z=0.80 lambda=0.10 minimum_object_intensity=0.11" +
		////				" subpixel= cell_mask_channel_1 cell_mask_channel_2 threshold_1=0.0015 threshold_2=0.0350 colorized objects outline save" +
		////				" number=4 intensity_estimation=[Automatic (Fisher scoring MLE)] noise=Poisson", "subpixel", null)
		////		);
		//		IJ.log("subpixel test " + getMacroParameter("Subpixel segmentation", false));
		//		IJ.log("remove test " + getMacroParameter("Remove background", true));
		//		IJ.log(" ");
		//		
		//		IJ.log("new test: ");
		//		//		Vector cbs = gd.getCheckboxes(); 
		//		//        Checkbox cb0 = (Checkbox) cbs.get(0); 
		//		//        String label = cb0.getLabel();
		//		//        IJ.log("checkbox " + label + " is " + cb0.getState());
		//		IJ.log("macro options : " + Macro.getOptions());
		//		//        String key = Macro.trimKey(label);
		//		//        IJ.log("key : " + key);

		if(clustermode){
			Analysis.p.removebackground=true;
			//Analysis.p.automatic_int=false;
			Analysis.p.usecellmaskX=true;
			Analysis.p.usecellmaskY=true;
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


		BLauncher hd= new BLauncher(wpath);	
		//		    hd.bcolocheadless(imagePlus);

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


			String path;
			//with JFileChooser
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.showOpenDialog(null);
			File selFile = fc.getSelectedFile();
			path=selFile.getAbsolutePath();



			boolean processdirectory =(new File(path)).isDirectory();
			if(!processdirectory){

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

				ImageStatistics st1=imgch1.getStatistics();
				//IJ.log("min " + Amin + "max " +Amax);

				imgch1.setDisplayRange(Amin,Amax);

				imgch1.show("");

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


	class MaskOpenerActionListener implements ActionListener
	{
		GenericDialogCustom gd;
		TextArea taxy;
		TextArea taz;
		Panel pp;

		public MaskOpenerActionListener(Panel p, GenericDialogCustom gd)
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
			MaskWindow hw = new MaskWindow(p.x, p.y, gd);

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

			//			JFrame frame;
			//			frame = new JFrame();
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

	public class Helpwindow implements ActionListener
	{
		public JFrame frame;
		//Initialize Buttons
		private JPanel panel;
		private JButton Close;
		private Font header = new Font(null, Font.BOLD,14);


		public Helpwindow(int x, int y ){
			frame = new JFrame();
			frame.setSize(575, 890);
			frame.setLocation(x+500, y-50);
			//frame.toFront();
			//frame.setResizable(false);
			//frame.setAlwaysOnTop(true);




			panel= new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
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
					+ "¥ Select file (processes one or two channels, 2D or Z-stacks image file), select folder (processes all image files contained) or paste path to file/folder."
					+"<br>"
					+"<br>"
					+"<h4>**Segmentation**</h4>"
					+"¥ Remove background: performs background removal through rolling ball method."
					+" If activated set size of rolling ball window in pixels. Should be larger than searched objects diameter."
					+"<br>"
					+"¥ Point spread function (PSF): set standard deviation in pixels of the gaussian distribution (in x/y and in z) used to approximate microscope's PSF. Use 'Estimate PSF' to compute these values from microscope objective characteristics."
					+"<br>"
					+"¥ Regularization: penalty weight on object lengths. Low values enable to separate and find smaller objects, higher values prevents over fitting and remove small objects (which can be only noise) whose intensity do not differ enough from the background."
					+"<br>"
					+"¥ Minimum object intensity: minimum intensity of objects looked for. Intensities values are normalized between 0 (minimum pixel intensity in the image) and 1 (maximum intensity)."
					+"<br>"
					+"¥ Subpixel segmentation: performs segmentation in subpixel resolution. (resolution x8 in 2D images, resolution x4 in Z-stacks images)."
					+"<br>"
					+"<br>"
					+"<h4>**Colocalization**</h4>"
					+"¥ Cell mask: computes a mask of positive cells in a channel. Mask is obtained by thresholding original image with given intensity value and holes filling. Useful to perform colocalization analysis only in cells positive for both channels."
					+"<br>"
					+"¥ Threshold values for channel 1 and 2 used to compute cell masks. Use 'Preview Cell Masks' to find appropriate value."
					+"<br>"
					+"<br>"
					+ "<h4>**Visualisation and output**</h4>"
					+"¥ 'Intermediate steps' display intermediate segmentation steps as well as segmentation energies in the log window. Colorized objects, intensities, labels and outlines respectively display segmented objects with random colors, objects intensities (black green for low intensity to bright green for high intensity ), objects integer labels, and outlines of objects overlaid with initial image."
					+"<br>"
					+"¥ Save option saves output images as zipped tiff files (openable in imageJ) and segmented objects data in one .csv file for each channel in folder containing processed image file."
					//+"First data column is the image number the object belongs to. Columns 2 to 6 are object label size, surface, length and intensity. Coord X, Y and Z are the coordinates of the center of mass of the object. For two channels images overlap gives the fraction of the object overlapping with objects in the other channel while Mcoloc size and Mcoloc Intensity give the mean size and intensity of the overlapping objects in the other channel. 'single coloc' is true if the object is overlapping with a single object."
					+"When processing a folder a third .csv file is written with one line per image processed indicating image name, image number, mean number of objects, mean object size and overlap."
					//+"</p>"
					+"<br>"
					+"<br>"
					+ "<h4>**Advanced options**</h4>"
					+"¥ Intensity estimation modes : useful when objects are made of several intensity layers. High layer mode only segments highest intensity portions of an object and thus also increases object separation."
					+"</div>"
					+ "</html>");

			//JPanel panel = new JPanel(new BorderLayout());

			JPanel pref= new JPanel(new BorderLayout());
			pref.setPreferredSize(new Dimension(555, 850));
			pref.setSize(pref.getPreferredSize());
			pref.add(label);


			panel.add(pref);
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


			frame.add(panel);


			//frame.repaint();

			frame.setVisible(true);
			//frame.requestFocus();
			//frame.setAlwaysOnTop(true);

			//			JOptionPane.showMessageDialog(frame,
			//				    "Eggs are not supposed to be green.\n dsfdsfsd",
			//				    "A plain message",
			//				    JOptionPane.PLAIN_MESSAGE);


		}

		public void actionPerformed(ActionEvent ae) {
			Object source = ae.getSource();	// Identify Button that was clicked


			if(source == Close)
			{
				//IJ.log("close called");
				frame.dispose();				
			}


		}

	}


	public class PSFWindow implements ActionListener,PropertyChangeListener
	{

		private GridBagLayout grid;
		private GridBagConstraints c;
		private int y;
		public double lem, lex, NA, n, pinhole, pix_xy, pix_z;
		public JFrame frame;
		//Initialize Buttons
		private JPanel panel;
		private JButton Close;
		private Font header = new Font(null, Font.BOLD,14);
		private boolean confocal = true;

		private String [] items={"Confocal Microscope", "Wide Field Fluorescence Microscope"};

		//jcb.setModel(new DefaultComboBoxModel(potentialOptions));

		NumberFormat nf = NumberFormat.getInstance(Locale.US);

		private JFormattedTextField Vlem= new JFormattedTextField(nf);
		private JFormattedTextField Vlex= new JFormattedTextField(nf);
		private JFormattedTextField VNA= new JFormattedTextField(nf);
		private JFormattedTextField Vn= new JFormattedTextField(nf);
		private JFormattedTextField Vpinhole= new JFormattedTextField(nf);
		private JFormattedTextField Vpix_xy= new JFormattedTextField(nf);
		private JFormattedTextField Vpix_z= new JFormattedTextField(nf);

		private  JComboBox micr = new JComboBox(items);
		private JButton estimate = new JButton("Compute PSF");

		private JLabel ref= new JLabel(
				"<html>"
						+"<div align=\"justify\">"
						+ "Gaussian PSF approximation."
						+"<br>"
						+"<br>"
						+"Model from: Gaussian approximations of fluorescence microscope point-spread function models. "
						+"B Zhang, J Zerubia, J C Olivo-Marin. Appl. Optics (46) 1819-29, 2007."
						+"</div>"
						+ "</html>");



		//	"Gaussian PSF model from : 'Gaussian approximations of fluorescence microscope point-spread function models. B Zhang, J Zerubia, J C Olivo-Marin. Appl. Optics (46) 1819-29, 2007.''");
		private JLabel tlem= new JLabel("Emission wavelength (nm)");
		private JLabel tlex= new JLabel("Excitation wavelength (nm)");
		private JLabel tNA = new JLabel("Numerical aperture");
		private JLabel tn = new JLabel("Refraction index");
		private JLabel tpinhole = new JLabel("Pinhole size (Airy units)");
		private JLabel tpix_xy = new JLabel("Lateral pixel size (nm)");
		private JLabel tpix_z = new JLabel("Axial pixel size (nm)");

		private JLabel result = new JLabel("");
		private GenericDialogCustom gd;

		public PSFWindow(int x, int y, GenericDialogCustom gd){
			y=0;
			this.gd=gd;

			lem=520;lex=488;NA=1.3;n=1.46;pinhole=1;
			pix_xy=100;pix_z=400;

			Vlem.setValue(lem);Vlex.setValue(lex);
			VNA.setValue(NA);Vn.setValue(n);
			Vpinhole.setValue(pinhole);
			Vpix_xy.setValue(pix_xy);Vpix_z.setValue(pix_z);

			frame = new JFrame();
			frame.setSize(300, 500);
			frame.setLocation(x+450, y+150);
			//frame.toFront();
			//frame.setResizable(false);
			//frame.setAlwaysOnTop(true);
			grid = new GridBagLayout();
			c = new GridBagConstraints();

			panel= new JPanel();
			panel.setPreferredSize(new Dimension(300, 500));
			panel.setSize(panel.getPreferredSize());
			panel.setLayout(null);

			JPanel pref= new JPanel(new BorderLayout());
			pref.setPreferredSize(new Dimension(280, 120));
			pref.setSize(pref.getPreferredSize());
			pref.add(ref);

			JPanel pres= new JPanel(new BorderLayout());
			pres.setPreferredSize(new Dimension(280, 80));
			pres.setSize(pres.getPreferredSize());
			pres.add(result);


			//JLabel label = new JLabel();
			//panel.add(label, BorderLayout.NORTH);				
			Vlem.setColumns(4);Vlex.setColumns(4);
			VNA.setColumns(4);Vn.setColumns(4);
			Vpinhole.setColumns(4);Vpix_xy.setColumns(4);
			Vpix_z.setColumns(4);

			Vlem.setHorizontalAlignment(SwingConstants.CENTER);Vlex.setHorizontalAlignment(SwingConstants.CENTER);
			VNA.setHorizontalAlignment(SwingConstants.CENTER);Vn.setHorizontalAlignment(SwingConstants.CENTER);
			Vpinhole.setHorizontalAlignment(SwingConstants.CENTER);Vpix_xy.setHorizontalAlignment(SwingConstants.CENTER);
			Vpix_z.setHorizontalAlignment(SwingConstants.CENTER);

			Vlem.setLocale(Locale.US);Vlex.setLocale(Locale.US);
			VNA.setLocale(Locale.US);Vn.setLocale(Locale.US);
			Vpinhole.setLocale(Locale.US);Vpix_xy.setLocale(Locale.US);
			Vpix_z.setLocale(Locale.US);

			//ref.setSize(ref.getPreferredSize());
			micr.setSize(micr.getPreferredSize());
			tlem.setSize(tlem.getPreferredSize());Vlem.setSize(Vlem.getPreferredSize());
			tlex.setSize(tlex.getPreferredSize());Vlex.setSize(Vlex.getPreferredSize());
			tNA.setSize(tNA.getPreferredSize());VNA.setSize(VNA.getPreferredSize());
			tn.setSize(tn.getPreferredSize());Vn.setSize(Vn.getPreferredSize());
			tpinhole.setSize(tpinhole.getPreferredSize());Vpinhole.setSize(Vpinhole.getPreferredSize());
			tpix_xy.setSize(tpix_xy.getPreferredSize());Vpix_xy.setSize(Vpix_xy.getPreferredSize());
			tpix_z.setSize(tpix_z.getPreferredSize());Vpix_z.setSize(Vpix_z.getPreferredSize());
			estimate.setSize(estimate.getPreferredSize());


			//			
			//			
			//			micr.setLocation(145,120);

			//rlevel.setLocation(145,120);
			//rlevell.setLocation(20,125);
			pref.setLocation(10,0);
			panel.add(pref);

			micr.setLocation(10,125);			
			tlem.setLocation(20,165);Vlem.setLocation(200,160);
			tlex.setLocation(20,195);Vlex.setLocation(200,190);
			tNA.setLocation(20,225);VNA.setLocation(200,220);
			tn.setLocation(20,255);Vn.setLocation(200,250);
			tpinhole.setLocation(20,285);Vpinhole.setLocation(200,280);
			tpix_xy.setLocation(20,315);Vpix_xy.setLocation(200,310);
			tpix_z.setLocation(20,345);Vpix_z.setLocation(200,340);
			estimate.setLocation(80,375);
			result.setLocation(10,405);

			panel.add(micr);
			panel.add(tlem);panel.add(Vlem);
			panel.add(tlex);panel.add(Vlex);
			panel.add(tNA);panel.add(VNA);
			panel.add(tn);panel.add(Vn);
			panel.add(tpinhole);panel.add(Vpinhole);

			panel.add(tpix_xy);panel.add(Vpix_xy);
			panel.add(tpix_z);panel.add(Vpix_z);

			panel.add(estimate);
			pres.setLocation(10,400);
			panel.add(pres);


			frame.add(panel);



			estimate.addActionListener(this);


			Vlem.addPropertyChangeListener(this);
			Vlex.addPropertyChangeListener(this);
			VNA.addPropertyChangeListener(this);
			Vn.addPropertyChangeListener(this);
			Vpinhole.addPropertyChangeListener(this);
			Vpix_xy.addPropertyChangeListener(this);
			Vpix_z.addPropertyChangeListener(this);

			micr.addActionListener(this);
			//

			frame.setVisible(true);
			//frame.requestFocus();
			//frame.setAlwaysOnTop(true);

			//			JOptionPane.showMessageDialog(frame,
			//				    "Eggs are not supposed to be green.\n dsfdsfsd",
			//				    "A plain message",
			//				    JOptionPane.PLAIN_MESSAGE);


		}

		public void propertyChange(PropertyChangeEvent e) {
			Object source = e.getSource();
			if (source == Vlem) {
				lem=(int) ((Number)Vlem.getValue()).doubleValue();
			} else if (source == Vlex) {
				lex=((Number)Vlex.getValue()).doubleValue();
			} else if (source == VNA) {
				NA=((Number)VNA.getValue()).doubleValue();
			} else if (source == Vn) {
				n=((Number)Vn.getValue()).doubleValue();
			} else if (source == Vpinhole) {
				pinhole=((Number)Vpinhole.getValue()).doubleValue();
			}else if (source == Vpix_xy) {
				pix_xy=((Number)Vpix_xy.getValue()).doubleValue();
			}else if (source == Vpix_z) {
				pix_z=((Number)Vpix_z.getValue()).doubleValue();
			}
		}

		public void actionPerformed(ActionEvent ae) {
			Object source = ae.getSource();	// Identify Button that was clicked

			if(source == estimate)
			{

				GaussianPSFModel psf= new GaussianPSFModel(lem,lex,NA,pinhole,n);

				double sz,sx;
				if(confocal){
					sz=1000*psf.axial_LSCM();
					sx=1000*psf.lateral_LSCM();
				}
				else{
					sz=1000*psf.axial_WFFM();
					sx=1000*psf.lateral_WFFM();
				}

				TextField tx =gd.getField(1);//field x
				TextField tz =gd.getField(2);//filed z

				tx.setText(String.format(Locale.US,"%.3f", sx/pix_xy));
				tz.setText(String.format(Locale.US,"%.3f", sz/pix_z));

				result.setText
				(	"<html>"
						+"<div align=\"justify\">"
						+ "Gaussian PSF stddev:"
						+"<br>"
						+"Lateral : "+String.format(Locale.US,"%.2f", sx) +" nm "
						+"Axial : "+String.format(Locale.US,"%.2f", sz) +" nm"
						+"<br>"
						+"("+String.format(Locale.US,"%.3f", sx/pix_xy)+", "+ String.format(Locale.US,"%.3f", sz/pix_z)+" in pixels)"
						+"</div>"
						+ "</html>");


			}

			if(source==micr)
			{
				JComboBox cb = (JComboBox)source;
				String selected = (String)cb.getSelectedItem();
				//System.out.println("Selected: "+selected);
				if(selected==items[1]){
					//widefield
					confocal=false;

				}
				if(selected==items[0]){
					//confocal
					confocal=true;
				}
			}	
		}
	}



	public class MaskWindow implements ActionListener,ChangeListener, PropertyChangeListener
	{
		boolean init1=false;
		boolean init2=false;
		ImagePlus maska_im1,maska_im2;
		private int y;
		//private int ni,nj,nz;
		public double thr1, thr2;
		public JFrame frame;
		//Initialize Buttons
		private JPanel panel;
		private JButton Close;
		private Font header = new Font(null, Font.BOLD,14);
		private boolean confocal = true;
		double max=0;
		double min=Double.POSITIVE_INFINITY;
		double max2=0;
		double min2=Double.POSITIVE_INFINITY;
		TextField tx;
		TextField tz;
		int calls=0;
		double val1,val2;

		boolean fieldval = false;
		boolean sliderval = false;
		boolean boxval= false;

		Checkbox b1,b2;
		double minrange=0.001;
		double maxrange=1;
		double logmin =  Math.log10(minrange);
		double logspan= Math.log10(maxrange) - Math.log10(minrange);
		int maxslider=1000;

		DecimalFormat df = new DecimalFormat("0.0000", new DecimalFormatSymbols(Locale.US));

		double initt1=0.0015;
		double initt2=0.0350;
		private String [] items={"Confocal Microscope", "Wide Field Fluorescence Microscope"};

		//jcb.setModel(new DefaultComboBoxModel(potentialOptions));

		NumberFormat nf = NumberFormat.getInstance(Locale.US);

		private JCheckBox m1= new JCheckBox("", false);
		private JCheckBox m2= new JCheckBox("", false);

		private JButton estimate = new JButton("Compute PSF");

		private JLabel ref= new JLabel(
				"<html>"
						+"<div align=\"justify\">"
						+ "Set thresholds and preview cell masks."
						+"</div>"
						+ "</html>");



		//	"Gaussian PSF model from : 'Gaussian approximations of fluorescence microscope point-spread function models. B Zhang, J Zerubia, J C Olivo-Marin. Appl. Optics (46) 1819-29, 2007.''");
		private JSlider t1= new JSlider();
		private JSlider t2= new JSlider();

		private JLabel l1 = new JLabel("Channel 1 cell mask");
		private JLabel l2 = new JLabel("Channel 2 cell mask");

		private JFormattedTextField v1 = new JFormattedTextField(df);
		private JFormattedTextField v2 = new JFormattedTextField(df);

		private JLabel warning = new JLabel("");

		private GenericDialogCustom gd;

		public MaskWindow(int x, int y, GenericDialogCustom gd){
			y=0;
			this.gd=gd;


			tx =gd.getField(6);//field x
			tz =gd.getField(7);//filed z
			b1=gd.getBox(2);
			b2=gd.getBox(3);


			frame = new JFrame();
			frame.setSize(300, 250);
			frame.setLocation(x+450, y+240);
			//frame.toFront();



			panel= new JPanel();
			panel.setPreferredSize(new Dimension(300, 500));
			panel.setSize(panel.getPreferredSize());
			//panel.setLayout(null);


			v1.setColumns(5);
			v2.setColumns(5);
			v1.setHorizontalAlignment(SwingConstants.CENTER);
			v2.setHorizontalAlignment(SwingConstants.CENTER);


			panel.add(ref);
			panel.add(l1);
			panel.add(m1);
			panel.add(t1);
			panel.add(v1);

			panel.add(l2);
			panel.add(m2);
			panel.add(t2);
			panel.add(v2);

			panel.add(warning);


			frame.add(panel);

			
			t1.addChangeListener(this);
			t2.addChangeListener(this);

			v1.addPropertyChangeListener(this);
			v2.addPropertyChangeListener(this);
			
			m1.addActionListener(this);
			m2.addActionListener(this);

			
			t1.setMinimum(0);
			t1.setMaximum(maxslider);
			//t1.setValue((int) (logvalue(initt1)));			

			t2.setMinimum(0);
			t2.setMaximum(maxslider);
			//t2.setValue((int) (logvalue(initt2)));
			
			
			v1.setValue(initt1);			
			v2.setValue(initt2);
			
			val1 =(double) ((Number)v1.getValue()).doubleValue();
			val2 =(double) ((Number)v2.getValue()).doubleValue();
			
			//IJ.log("val2 " + val2 + "initt2 " + initt2);			
			//IJ.log("val1 " + val1 + "initt1 " + initt1);

			frame.setVisible(true);
			//frame.requestFocus();
			//frame.setAlwaysOnTop(true);

			//			JOptionPane.showMessageDialog(frame,
			//				    "Eggs are not supposed to be green.\n dsfdsfsd",
			//				    "A plain message",
			//				    JOptionPane.PLAIN_MESSAGE);


		}

		public double expvalue(double slidervalue){

			return(Math.pow(10,(slidervalue/maxslider)*logspan + logmin));

		}

		public double logvalue(double tvalue){

			return(maxslider*(Math.log10(tvalue) - logmin)/logspan);

		}

		public void actionPerformed(ActionEvent ae) {
			Object source = ae.getSource();	// Identify Button that was clicked

			boxval=true;
			if(source == m1)
			{
				boolean b=m1.isSelected();
				b1.setState(b);
				if(b){
					if(imgch1!=null){
						if(maska_im1==null)
							maska_im1= new ImagePlus();
						initpreviewch1(imgch1);
						previewBinaryCellMask(initt1,imgch1,maska_im1,1);						
						maska_im1.show();						
						init1=true;
					}
					else
						warning.setText("Please open an image first.");
				}
				else{
					//hide and clean
					if(maska_im1!=null)
					maska_im1.hide();
					//maska_im1=null;
					init1=false;
				}
			}

			if(source==m2)
			{
				boolean b=m2.isSelected();
				b2.setState(b);
				if(b){
					if(imgch2!=null){
						if(maska_im2==null)
							maska_im2= new ImagePlus();
						initpreviewch2(imgch2);
						previewBinaryCellMask(initt2,imgch2,maska_im2,2);
						maska_im2.show();
						init2=true;
					}
					else{
						warning.setText("Please open an image with two channels first.");
					}
				}
				else{
					//close and clean
					if(maska_im2!=null)
						maska_im2.hide();
//					maska_im2.close();
//					maska_im2=null;
					init2=false;
				}

			}
			//IJ.log("boxval to false");
			boxval=false;
		}

		public void propertyChange(PropertyChangeEvent e) {
			Object source = e.getSource();

			if(!boxval && !sliderval){
				fieldval=true;
				if (source == v1 && init1) {
					double v =(double) ((Number)v1.getValue()).doubleValue();
					//v=expvalue(v);
					//IJ.log("val1" + val1 + "v" + v);
					if(!sliderval && val1!=v){
						val1=v;
						previewBinaryCellMask(v,imgch1,maska_im1,1);
						int vv= (int) (logvalue(v));
						t1.setValue(vv);
						tx.setText(String.format(Locale.US,"%.4f", v));
					}
					//IJ.log("v1 init: do" + boxval +" " + sliderval);

				} else if (source == v2 && init2) {
					double v =(double) ((Number)v2.getValue()).doubleValue();
					if(!sliderval && val2!=v){
						val2=v;
						previewBinaryCellMask(v,imgch2,maska_im2,2);
						int vv= (int) (logvalue(v));
						t2.setValue(vv);
						tz.setText(String.format(Locale.US,"%.4f", v));
					}
				} else if (source == v1 && !init1) {
					double v =(double) ((Number)v1.getValue()).doubleValue();
					if(!sliderval){
						//val1=v;
						int vv= (int) (logvalue(v));
						t1.setValue(vv);
						tx.setText(String.format(Locale.US,"%.4f", v));
					}
					//IJ.log("v1 not init");
				} else if (source == v2 && !init2) {
					double v =(double) ((Number)v2.getValue()).doubleValue();
					if(!sliderval){
						//val2=v;
						int vv= (int) (logvalue(v));
						t2.setValue(vv);
						tz.setText(String.format(Locale.US,"%.4f", v));
					}
				} 

				fieldval=false;}
		}


		public void stateChanged(ChangeEvent e) {
			Object origin=e.getSource();

			if(!boxval && !fieldval){
				sliderval=true;
				if (origin==t1 && init1 && !t1.getValueIsAdjusting() ){
					double value=t1.getValue();
					double vv= expvalue(value);
					//IJ.log("val1" + val1 + "vv" + vv);
					if(!fieldval && val1!=vv) {
						v1.setValue(vv);
						val1=vv;
						previewBinaryCellMask(vv,imgch1,maska_im1,1);}
					tx.setText(String.format(Locale.US,"%.4f", vv));
					//IJ.log("t1 : do" + fieldval);

				}

				if (origin==t1 && init1 && t1.getValueIsAdjusting()){
					double value=t1.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v1.setValue(vv);
					tx.setText(String.format(Locale.US,"%.4f", vv));
					//val1=vv;
					}
					//IJ.log("t1 ch");
				}

				if (origin==t1 && !init1){
					double value=t1.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v1.setValue(vv);
					tx.setText(String.format(Locale.US,"%.4f", vv));
					//val1=vv;
					}
					//IJ.log("t1 not init");	
				}

				if (origin==t2 && !init2){
					double value=t2.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v2.setValue(vv);
					tz.setText(String.format(Locale.US,"%.4f", vv));
					//val2=vv;
					}
				}


				if (origin==t2 && init2 && !t2.getValueIsAdjusting()){
					double value=t2.getValue();
					double vv= expvalue(value);
					if(!fieldval && val2!=vv) {v2.setValue(vv);
					previewBinaryCellMask(vv,imgch2,maska_im2,2);
					val2=vv;}
					tz.setText(String.format(Locale.US,"%.4f", vv));
				}

				if (origin==t2 && init2 && t2.getValueIsAdjusting()){
					double value=t2.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v2.setValue(vv);
					tx.setText(String.format(Locale.US,"%.4f", vv));
					//val2=vv;
					}
				}

				sliderval = false;
			}

		}


		public void initpreviewch1(ImagePlus img){


			ImageProcessor imp;
			for (int z=0; z<nz; z++){
				img.setSlice(z+1);
				imp=img.getProcessor();
				for (int i=0; i<ni; i++){  
					for (int j=0;j< nj; j++){  
						if(imp.getPixel(i,j)>max)max=imp.getPixel(i,j);
						if(imp.getPixel(i,j)<min)min=imp.getPixel(i,j);
					}	
				}
			}



		}

		public void initpreviewch2(ImagePlus img){
			//img.duplicate().show();

			ImageProcessor imp;
			for (int z=0; z<nz; z++){
				img.setSlice(z+1);
				imp=img.getProcessor();
				for (int i=0; i<ni; i++){  
					for (int j=0;j< nj; j++){  
						if(imp.getPixel(i,j)>max2)max2=imp.getPixel(i,j);
						if(imp.getPixel(i,j)<min2)min2=imp.getPixel(i,j);
					}	
				}
			}
		}

		public void previewBinaryCellMask(double threshold_i, ImagePlus img, ImagePlus maska_im, int channel){
			//IJ.log("cellmask" + calls);
			calls++;
			int ni,nz,nj;
			ni=img.getWidth();
			nj=img.getHeight();
			nz=img.getNSlices();

			int ns =img.getSlice();
			double threshold;


			//change : use max value instead of 65536
			ImageProcessor imp;


			if(channel==1)
				threshold= threshold_i*(max-min)+min;
			else
				threshold= threshold_i*(max2-min2)+min2;


			//IJ.log("max " + max + " min " + min + "threshold " + threshold);

			//ImagePlus maska_im= new ImagePlus();
			ImageStack maska_ims= new ImageStack(ni,nj);

			for (int z=0; z<nz; z++){
				img.setSlice(z+1);
				imp=img.getProcessor();
				byte[] maska_bytes = new byte[ni*nj];
				for (int i=0; i<ni; i++){  
					for (int j=0;j< nj; j++){  
						if(imp.getPixel(i,j)>threshold)
							maska_bytes[j * ni + i]=(byte) 255;
						else 
							maska_bytes[j * ni + i]=0;

					}	
				}
				ByteProcessor bp = new ByteProcessor(ni, nj);
				bp.setPixels(maska_bytes);
				maska_ims.addSlice("", bp);
			}



			maska_im.setStack("Cell mask channel " + (channel),maska_ims);
			//maska_im.duplicate().show();
			IJ.run(maska_im, "Invert", "stack");


			//		
			//		//IJ.run(maska_im, "Erode", "");
			IJ.run(maska_im, "Fill Holes", "stack");


			IJ.run(maska_im, "Open", "stack");


			IJ.run(maska_im, "Invert", "stack");

			maska_im.updateAndDraw();
			//maska_im.show("mask");
			maska_im.changes= false;

			img.setSlice(ns);
		}

	}




}
