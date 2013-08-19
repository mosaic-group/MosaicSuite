package mosaic.bregman.GUI;

import java.awt.Button;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import mosaic.bregman.Analysis;
import mosaic.bregman.GenericDialogCustom;
import ij.ImagePlus;


public class SegmentationGUI 
{
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;

	public SegmentationGUI()
	{
	}


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);

		GenericDialogCustom  gd = new GenericDialogCustom("Segmentation options");

		gd.setInsets(-10,0,3);


		gd.addMessage("Segmentation parameters ",bf);

		gd.addNumericField("Regularization (>0)", Analysis.p.lreg, 3);

		gd.addNumericField("Minimum_object_intensity,_channel_1 (0 to 1)", Analysis.p.min_intensity, 3);
		gd.addNumericField("                         _channel_2 (0 to 1)", Analysis.p.min_intensityY, 3);
		
		gd.addCheckbox("Subpixel segmentation", Analysis.p.subpixel);
		
		String choice1[] = {
				"Automatic (best energy)", "Low layer", "Medium layer (clustering)","High layer (clustering)"};
		gd.addChoice("Local intensity_estimation ", choice1, choice1[Analysis.p.mode_intensity]);

		String choice2[] = {
				"Poisson", "Gauss"};
		gd.addChoice("Noise Model ", choice2, choice2[Analysis.p.noise_model]);
		
		gd.addMessage("PSF ",bf);
		
		gd.addNumericField("Gaussian , standard deviation xy (in pixels)", Analysis.p.sigma_gaussian, 2);
		gd.addNumericField("           standard deviation z  (in pixels)", Analysis.p.sigma_gaussian/Analysis.p.zcorrec, 2);
		
		Button bp = new Button("Estimate PSF");
		bp.addActionListener(new PSFOpenerActionListener(gd));
		
		Panel p = new Panel();
		p.add(bp);
		gd.addPanel(p);
		
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
