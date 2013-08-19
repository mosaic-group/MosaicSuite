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
import javax.swing.JDialog;
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

import mosaic.bregman.GUI.BackgroundSubGUI;
import mosaic.bregman.GUI.ColocalizationGUI;
import mosaic.bregman.GUI.PSFWindow;
import mosaic.bregman.GUI.SegmentationGUI;
import mosaic.bregman.GUI.VisualizationGUI;


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


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);
		String sgroup1[] = {"activate second step", ".. with subpixel resolution"};
		boolean bgroup1[] = {false, false};

		GenericDialogCustom  gd = new GenericDialogCustom("Bregman Segmentation");

		gd.setInsets(-10,0,3);
		if(!clustermode)
		{
			gd.addTextAreas("Input Image: \n" +
					"insert Path to file or folder, " +
					"or press Button below.", 
					null, 2, 40);
		}
		if(clustermode)
		{
			gd.addStringField("Filepath","path",10);
		}

		//panel with button
		if(!clustermode)
		{
			//FlowLayout fl = new FlowLayout(FlowLayout.LEFT,335,3);
			FlowLayout fl = new FlowLayout(FlowLayout.LEFT,75,3);
			Panel p = new Panel(fl);
			p.setPreferredSize(new Dimension(865, 30));
			//p.setLayout(null);
			//p.setBackground(Color.black);


			Button b = new Button("Select File/Folder");
			b.addActionListener(new FileOpenerActionListener(p,gd, gd.getTextArea1()));
			p.add(b);

			Button bp3 = new Button("Preview Cell Masks");
			bp3.addActionListener(new MaskOpenerActionListener(p,gd));
			p.add(bp3);

			Button bh = new Button("Help");
			bh.addActionListener(new HelpOpenerActionListener(p,gd));
			p.add(bh);

			gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
		}

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
				
				BackgroundSubGUI gds = new BackgroundSubGUI();
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
				
				SegmentationGUI gds = new SegmentationGUI();
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
				
				ColocalizationGUI gds = new ColocalizationGUI();
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
				
				VisualizationGUI gds = new VisualizationGUI();
				gds.run("");
			}
		});
		gd.addPanel(p);

//		if(!clustermode)gd.addMessage("Advanced options ",bf);
		if(clustermode)
		{
			gd.addNumericField("number of threads", 4, 0);
		}

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

/*		//general options	
		Analysis.p.removebackground=gd.getNextBoolean();
		//IJ.log("rem back:" +  Analysis.p.removebackground);
		Analysis.p.size_rollingball=(int) gd.getNextNumber();
		//Analysis.p.usePSF=gd.getNextBoolean();*/

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






	public class MaskWindow implements ActionListener,ChangeListener, PropertyChangeListener
	{
		boolean init1=false;
		boolean init2=false;
		ImagePlus maska_im1,maska_im2;
		private int y;
		//private int ni,nj,nz;
		public double thr1, thr2;
		public JDialog frame;
		//Initialize Buttons
		private JPanel panel;
		private JButton Close;
		private Font header = new Font(null, Font.BOLD,14);
		private boolean confocal = true;
		double max=0;
		double min=Double.POSITIVE_INFINITY;
		double max2=0;
		double min2=Double.POSITIVE_INFINITY;
//		TextField tx;
//		TextField tz;
		int calls=0;
		double val1,val2;

		boolean fieldval = false;
		boolean sliderval = false;
		boolean boxval= false;

//		Checkbox b1,b2;
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


//			tx =gd.getField(6);//field x
//			tz =gd.getField(7);//filed z
//			b1=gd.getBox(2);
//			b2=gd.getBox(3);


			frame = new JDialog();
			frame.setModal(true);
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
//				b1.setState(b);
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
//				b2.setState(b);
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
//						tx.setText(String.format(Locale.US,"%.4f", v));
					}
					//IJ.log("v1 init: do" + boxval +" " + sliderval);

				} else if (source == v2 && init2) {
					double v =(double) ((Number)v2.getValue()).doubleValue();
					if(!sliderval && val2!=v){
						val2=v;
						previewBinaryCellMask(v,imgch2,maska_im2,2);
						int vv= (int) (logvalue(v));
						t2.setValue(vv);
//						tz.setText(String.format(Locale.US,"%.4f", v));
					}
				} else if (source == v1 && !init1) {
					double v =(double) ((Number)v1.getValue()).doubleValue();
					if(!sliderval){
						//val1=v;
						int vv= (int) (logvalue(v));
						t1.setValue(vv);
//						tx.setText(String.format(Locale.US,"%.4f", v));
					}
					//IJ.log("v1 not init");
				} else if (source == v2 && !init2) {
					double v =(double) ((Number)v2.getValue()).doubleValue();
					if(!sliderval){
						//val2=v;
						int vv= (int) (logvalue(v));
						t2.setValue(vv);
//						tz.setText(String.format(Locale.US,"%.4f", v));
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
//					tx.setText(String.format(Locale.US,"%.4f", vv));
					//IJ.log("t1 : do" + fieldval);

				}

				if (origin==t1 && init1 && t1.getValueIsAdjusting()){
					double value=t1.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v1.setValue(vv);
//					tx.setText(String.format(Locale.US,"%.4f", vv));
					//val1=vv;
					}
					//IJ.log("t1 ch");
				}

				if (origin==t1 && !init1){
					double value=t1.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v1.setValue(vv);
//					tx.setText(String.format(Locale.US,"%.4f", vv));
					//val1=vv;
					}
					//IJ.log("t1 not init");	
				}

				if (origin==t2 && !init2){
					double value=t2.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v2.setValue(vv);
//					tz.setText(String.format(Locale.US,"%.4f", vv));
					//val2=vv;
					}
				}


				if (origin==t2 && init2 && !t2.getValueIsAdjusting()){
					double value=t2.getValue();
					double vv= expvalue(value);
					if(!fieldval && val2!=vv) {v2.setValue(vv);
					previewBinaryCellMask(vv,imgch2,maska_im2,2);
					val2=vv;}
//					tz.setText(String.format(Locale.US,"%.4f", vv));
				}

				if (origin==t2 && init2 && t2.getValueIsAdjusting()){
					double value=t2.getValue();
					double vv= expvalue(value);
					if(!fieldval) {v2.setValue(vv);
//					tx.setText(String.format(Locale.US,"%.4f", vv));
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
