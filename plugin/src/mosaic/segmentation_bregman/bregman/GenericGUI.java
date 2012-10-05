package bregman;

import java.awt.Button;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextArea;
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
import java.io.File;

import javax.swing.JFileChooser;
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
import ij.gui.GenericDialog;
//import ij.gui.NonBlockingGenericDialog;

public class GenericGUI {
	boolean clustermode;

	public GenericGUI(boolean mode){
		clustermode=mode;
		//clustermode=true;
	}

	public void run(String arg) {
		Font bf = new Font(null, Font.BOLD,12);
		String sgroup1[] = {"activate second step", ".. with subpixel resolution"};
		boolean bgroup1[] = {false, false};

		

		GenericDialog  gd = new GenericDialog("Bregman Segmentation");

		//		gd.addMessage("");
		//		gd.addMessage("Image __________________", new Font(null, Font.BOLD,12));
		//		gd.addMessage("");
		gd.setInsets(0,0,3);
		if(!clustermode){
			gd.addTextAreas("Input Image: \n" +
					"insert Path to file or folder, " +
					"or press Button below.", 
					null, 2, 70);
		}
		if(clustermode){
			gd.addStringField("Filepath","path",10);
		}

		//panel with button
		if(!clustermode){
			Panel p = new Panel();
			Button b = new Button("Select File/Folder");
			b.addActionListener(new FileOpenerActionListener(p,gd, gd.getTextArea1()));
			p.add(b);
			gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0));
		}

		if(!clustermode)gd.addMessage("Segmentation parameters __________________________________________________________________________________________________________________",bf);
		gd.addCheckbox("Remove background", true);
		gd.addNumericField("rolling ball window size (in pixels)", 10, 0);
		//gd.addMessage("Load PSF");

		gd.addNumericField("Gaussian_PSF_approximation,_stddev_xy  (in pixels)", 0.9, 2);
		gd.addNumericField("Gaussian_PSF_approximation,_stddev_z ", 0.8, 2);

		gd.addNumericField("Lambda prior: ", 0.15, 3);

		//gd.addNumericField("Minimum_object_size: ", 5, 0);
		//gd.addNumericField("Maximum_object_size: ", -1, 0);
		gd.addNumericField("Minimum_object_intensity: ", 0.15, 3);
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
		gd.addNumericField("threshold_1 (0 to 1)", 0.0015, 4);
		gd.addNumericField("threshold_2 (0 to 1)", 0.035, 4);
		//gd.addNumericField("Number of levels",2,0);


		//gd.addMessage("Select file or folder:");
		//gd.addStringField("Path: ", "/working_directory",30);
		if(!clustermode)gd.addMessage("");
		if(!clustermode)gd.addMessage("Vizualization and output _________________________________________________________________________________________________________________",bf);
		//gd.addMessage("");

		String sgroup2[] = {
				"Live segmentation", "Colorized objects","Objects intensities",
				"Labelized objects","Outlines overlay",
		"Save objects characteristics and images"};
		boolean bgroup2[] = {false, true,
				false,false,false,false};
		gd.addCheckboxGroup(2, 3, sgroup2, bgroup2);
		//		gd.addCheckbox("Live segmentation",true);
		//		gd.addCheckbox("Random color objects",true);
		//		gd.addCheckbox("Intensities reconstruction",false);
		//		gd.addCheckbox("Objects labels",false);
		//		gd.addCheckbox("Outline overlay",false);
		//		gd.addCheckbox("Display colocalization",false);
		//		
		//		gd.addCheckbox("Save object data in .csv file and save images", false);

		if(!clustermode)gd.addMessage("Expert options____________________________________________________________________________________________________________________________",bf);
		if(clustermode){
			gd.addNumericField("number of threads", 8, 0);
		}
		String choice1[] = {
				"Automatic (Fisher scoring MLE)", "Medium layer (kmeans clustering)","High layer (kmeans clustering)"};
		gd.addChoice("Intensity_estimation ", choice1, "Automatic");
		
		
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
		Analysis.p.min_intensityY=Analysis.p.min_intensity;
		//Analysis.p.min_intensityY=gd.getNextNumber();


		//first step

		//IJ.log("aut int:" +  Analysis.p.automatic_int);
		//Analysis.p.findregionthresh= gd.getNextBoolean();
		//Analysis.p.regionthresh= gd.getNextNumber();



		//second step
		//Analysis.p.refinement= gd.getNextBoolean();
		//IJ.log("refinement:" +  Analysis.p.refinement);
		Analysis.p.subpixel= gd.getNextBoolean();
		//IJ.log("subpixel:" +  Analysis.p.subpixel);
		//Analysis.p.oversampling2ndstep=(int) gd.getNextNumber();
		//Analysis.p.interpolation=(int) gd.getNextNumber();

		//colocalization step

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
			Analysis.p.regionthresh=Analysis.p.min_intensity;
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



		if(clustermode){
			Analysis.p.removebackground=true;
			Analysis.p.automatic_int=false;
			//Analysis.p.refinement=false;
			//Analysis.p.subpixel=false;
			Analysis.p.usecellmaskX=true;
			Analysis.p.usecellmaskY=true;
			Analysis.p.livedisplay=false;
			Analysis.p.dispcolors=true;
			Analysis.p.dispint=true;
			//Analysis.p.displabels=true;
			Analysis.p.dispoutline=true;
			Analysis.p.save_images=true;
		}



		if(!Analysis.p.subpixel){Analysis.p.oversampling2ndstep=1;
		Analysis.p.interpolation=1;
		}



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

			String path;
			//with JFileChooser
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.showOpenDialog(null);
			File selFile = fc.getSelectedFile();
			path=selFile.getAbsolutePath();


			//			//with filedialog
			//			FileDialog fd = new FileDialog(gd);
			//			fd.setVisible(true);
			//			dir = fd.getDirectory();
			//			file = fd.getFile();

			if(path!=null)//(file!=null && dir!=null)
			{
				ta.setText(path);
			}
		}
	}




}
