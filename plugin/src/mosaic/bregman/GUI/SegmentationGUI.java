package mosaic.bregman.GUI;

import ij.ImagePlus;

import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;

import mosaic.bregman.Analysis;
import mosaic.bregman.GenericDialogCustom;
import mosaic.bregman.GenericGUI;


public class SegmentationGUI 
{	
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
		getParameters();
		
		//Analysis.load2channels(imagePlus);
	}

	public static int getParameters()
	{
		final GenericDialogCustom  gd = new GenericDialogCustom("Segmentation options");
		
		Font bf = new Font(null, Font.BOLD,12);
		
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
				
				new SegmentationGUIHelp(p.x,p.y);
				
			}});
		
		gd.addPanel(pp);
		
		gd.addNumericField("Regularization_(>0)_ch1", Analysis.p.lreg_[0], 3);
		gd.addNumericField("Regularization_(>0)_ch2", Analysis.p.lreg_[1], 3);
		
		gd.addNumericField("Minimum_object_intensity_channel_1_(0_to_1)", Analysis.p.min_intensity, 3);
		gd.addNumericField("                        _channel_2_(0_to_1)", Analysis.p.min_intensityY, 3);
		
		/////////////// Patches positioning
		
		//FlowLayout fl = new FlowLayout(FlowLayout.LEFT,335,3);
//		p.setPreferredSize(new Dimension(565, 30));
		//p.setLayout(null);
		//p.setBackground(Color.black);

//		Button b = new Button("Preview cell mask");
//		b.addActionListener(new HelpOpenerActionListener(p,gd));
//		p.add(b);
		
		///////////////
		
		gd.addCheckbox("Subpixel_segmentation", Analysis.p.subpixel);
		gd.addCheckbox("Exclude_Z_edge", Analysis.p.exclude_z_edges);
		
		String choice1[] = {
				"Automatic", "Low", "Medium","High"};
		gd.addChoice("Local_intensity_estimation ", choice1, choice1[Analysis.p.mode_intensity]);

		String choice2[] = {
				"Poisson", "Gauss"};
		gd.addChoice("Noise_Model ", choice2, choice2[Analysis.p.noise_model]);
		
		gd.addMessage("PSF model (Gaussian approximation)",bf);
		
		gd.addNumericField("standard_deviation_xy (in pixels)", Analysis.p.sigma_gaussian, 2);
		gd.addNumericField("standard_deviation_z  (in pixels)", Analysis.p.sigma_gaussian/Analysis.p.zcorrec, 2);
		
		gd.addMessage("Region filter",bf);
		gd.addNumericField("Remove_region_with_intensities_<", Analysis.p.min_region_filter_intensities,0);
		
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
		
		if (GenericGUI.bypass_GUI == false)
		{
			gd.showDialog();
			if (gd.wasCanceled()) return -1;

			Analysis.p.lreg_[0]= gd.getNextNumber();
			Analysis.p.lreg_[1]= gd.getNextNumber();
			Analysis.p.min_intensity=gd.getNextNumber();
			Analysis.p.min_intensityY=gd.getNextNumber();
			Analysis.p.subpixel= gd.getNextBoolean();
			Analysis.p.exclude_z_edges = gd.getNextBoolean();
			Analysis.p.sigma_gaussian=gd.getNextNumber();
			Analysis.p.zcorrec=Analysis.p.sigma_gaussian/gd.getNextNumber();
			Analysis.p.min_region_filter_intensities = gd.getNextNumber();
			Analysis.p.mode_intensity=gd.getNextChoiceIndex();
			Analysis.p.noise_model=gd.getNextChoiceIndex();
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
		
		return 0;
	}
}
