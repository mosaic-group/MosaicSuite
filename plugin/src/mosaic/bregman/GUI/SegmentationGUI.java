package mosaic.bregman.GUI;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import mosaic.bregman.Analysis;
import mosaic.bregman.GenericDialogCustom;
import mosaic.bregman.GUI.BackgroundSubGUI.BackgroundSubHelp;
import mosaic.core.GUI.HelpGUI;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;


public class SegmentationGUI 
{
	class SegmentationGUIHelp extends HelpGUI implements ActionListener
	{
		public JDialog frame;
		//Initialize Buttons
		private JPanel panel;
		private JButton Close;
		private Font header = new Font(null, Font.BOLD,14);


		public SegmentationGUIHelp(int x, int y )
		{		
			frame = new JDialog();
			frame.setTitle("Segmentation Help");
			frame.setSize(500, 620);
			frame.setLocation(x+500, y-50);
			frame.setModal(true);
			//frame.toFront();
			//frame.setResizable(false);
			//frame.setAlwaysOnTop(true);

			panel= new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
			panel.setPreferredSize(new Dimension(500,620));

			JPanel pref= new JPanel(new GridBagLayout());
//			pref.setPreferredSize(new Dimension(555, 550));
//			pref.setSize(pref.getPreferredSize());
			
			setPanel(pref);
			setHelpTitle("Segmentation");
			
			String desc = new String("Set a regularization parameter for the segmentation. Use higher values" +
									 "to avoid segmenting noise- induced small intensity peaks values are between 0.05 and 0.25.");

			createField("Regularization parameter",desc,null);

			desc = new String("Set the threshold for the minimum object intensity to be considered. Intensity values are normalized" +
			 "between 0 for the smallest value occurring in the image and 1 for the largest value");

			createField("Cell mask thresholding",desc,null);
			
			
			desc = new String("compute segmentations with sub-pixel resolution." +
						 "The resolution of the segmentation is increased " +
						 "by an over-sampling factor of 8 for 2D images and " +
						 "4 for 3D images.");			

			createField("Sub-pixel segmentation",desc,null);

			desc = new String("Noise and intensity models");

			createField("Noise model",desc,null);
			
			desc = new String("Set the microscope PSF. In order to correct for diffraction blur, " +
					"the software needs information about the PSF of the microscope. " +
					"This can be done in either of the following two ways: " +
					"a)  Use a theoretical PSF model. Use Estimate PSF " +
					"b)  Use  Measure the microscope PSF from images of " +
					"fluorescent sub-diffraction beads. " +
					"Use the menu item Plugins → Mosaic → PSF Tool to measure " +
					"these parameters from images of beads.");

			createField("PSF",desc,null);
			
			//JPanel panel = new JPanel(new BorderLayout());

			panel.add(pref);
			//panel.add(label, BorderLayout.NORTH);


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
		
		public void actionPerformed(ActionEvent ae) 
		{
			Object source = ae.getSource();	// Identify Button that was clicked


			if(source == Close)
			{
				//IJ.log("close called");
				frame.dispose();				
			}


		}
		
	}
	
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;
	int posx, posy;

	public SegmentationGUI(int ParentPosx, int ParentPosy)
	{
		posx= ParentPosx+20;
		posy= ParentPosy+20;
	}


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);

		final GenericDialogCustom  gd = new GenericDialogCustom("Segmentation options");

		gd.setInsets(-10,0,3);


		gd.addMessage("    Segmentation parameters ",bf);

		Panel pp = new Panel();
		Button help_b = new Button("help");
		
		pp.add(help_b);
		
		help_b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				Point p =gd.getLocationOnScreen();
				
				SegmentationGUIHelp pth = new SegmentationGUIHelp(p.x,p.y);
				
			}});
		
		gd.addPanel(pp);
		
		gd.addNumericField("Regularization (>0)", Analysis.p.lreg, 3);

		gd.addNumericField("Minimum_object_intensity,_channel_1 (0 to 1)", Analysis.p.min_intensity, 3);
		gd.addNumericField("                         _channel_2 (0 to 1)", Analysis.p.min_intensityY, 3);
		
		/////////////// Patches positioning
		
		//FlowLayout fl = new FlowLayout(FlowLayout.LEFT,335,3);
//		p.setPreferredSize(new Dimension(565, 30));
		//p.setLayout(null);
		//p.setBackground(Color.black);

//		Button b = new Button("Preview cell mask");
//		b.addActionListener(new HelpOpenerActionListener(p,gd));
//		p.add(b);
		
		///////////////
		
		gd.addCheckbox("Subpixel segmentation", Analysis.p.subpixel);
		
		String choice1[] = {
				"Automatic (best energy)", "Low layer", "Medium layer (clustering)","High layer (clustering)"};
		gd.addChoice("Local intensity_estimation ", choice1, choice1[Analysis.p.mode_intensity]);

		String choice2[] = {
				"Poisson", "Gauss"};
		gd.addChoice("Noise Model ", choice2, choice2[Analysis.p.noise_model]);
		
		gd.addMessage("PSF model (Gaussian approximation)",bf);
		
		gd.addNumericField("standard deviation xy (in pixels)", Analysis.p.sigma_gaussian, 2);
		gd.addNumericField("standard deviation z  (in pixels)", Analysis.p.sigma_gaussian/Analysis.p.zcorrec, 2);
		
		Panel p = new Panel();
		Button b = new Button("Patch position");
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				// TODO Auto-generated method stub
				
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.showOpenDialog(null);
				File selFile = fc.getSelectedFile();
				
				Analysis.p.patches_from_file = selFile.getAbsolutePath();
			}
			
		});
		p.add(b);
		gd.addPanel(p);
		
		Button bp = new Button("Estimate PSF from objective properties");
		bp.addActionListener(new PSFOpenerActionListener(gd));
		p = new Panel();
		p.add(bp);
		gd.addPanel(p);

		gd.centerDialog(false);
		gd.setLocation(posx, posy);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		Analysis.p.lreg= gd.getNextNumber();
		Analysis.p.min_intensity=gd.getNextNumber();
		Analysis.p.min_intensityY=gd.getNextNumber();
		Analysis.p.subpixel= gd.getNextBoolean();
		Analysis.p.sigma_gaussian=gd.getNextNumber();
		Analysis.p.zcorrec=Analysis.p.sigma_gaussian/gd.getNextNumber();
		Analysis.p.mode_intensity=gd.getNextChoiceIndex();
		Analysis.p.noise_model=gd.getNextChoiceIndex();

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


		if(!Analysis.p.subpixel)
		{
			Analysis.p.oversampling2ndstep=1;
			Analysis.p.interpolation=1;
		}

		//		    hd.bcolocheadless(imagePlus);

		//Analysis.load2channels(imagePlus);
	}


	class PSFOpenerActionListener implements ActionListener
	{
		GenericDialogCustom gd;
		TextArea taxy;
		TextArea taz;

		public PSFOpenerActionListener(GenericDialogCustom gd)
		{
			this.gd=gd;
			//this.ta=ta;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Point p =gd.getLocationOnScreen();
			//IJ.log("plugin location :" + p.toString());
			PSFWindow hw = new PSFWindow(p.x, p.y, gd);
		}
	}

}
