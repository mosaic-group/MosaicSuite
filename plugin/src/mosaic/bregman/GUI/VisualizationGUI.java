package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import mosaic.bregman.Analysis;
import mosaic.bregman.GenericDialogCustom;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;




public class VisualizationGUI 
{
	
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;

	public VisualizationGUI()
	{
	}


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);
		
		GenericDialog  gd = new GenericDialog("Visualization and output options");
		
		gd.setInsets(-10,0,3);
		gd.addMessage("Visualization and output",bf);

		String sgroup2[] = 
		{
				"Intermediate steps", "Colorized objects","Objects intensities",
				"Labelized objects","Outlines overlay","Save objects characteristics and images"
		};
		boolean bgroup2[] =
		{
				false, false,false,
				false,false,false
		};
		
		bgroup2[0] = Analysis.p.livedisplay;
		bgroup2[1] = Analysis.p.dispcolors;
		bgroup2[2] = Analysis.p.dispint;
		bgroup2[3] = Analysis.p.displabels;
		bgroup2[4] = Analysis.p.dispoutline;
		bgroup2[5] = Analysis.p.save_images;
		
		gd.addCheckboxGroup(2, 3, sgroup2, bgroup2);
		//		gd.addCheckbox("Live segmentation",true);
		//		gd.addCheckbox("Random color objects",true);
		//		gd.addCheckbox("Intensities reconstruction",false);
		//		gd.addCheckbox("Objects labels",false);
		//		gd.addCheckbox("Outline overlay",false);
		//		gd.addCheckbox("Display colocalization",false);
		//		
		//		gd.addCheckbox("Save object data in .csv file and save images", false);
		
		//IJ.log(" la" + Analysis.p.nbconditions);
		gd.addNumericField("Number of conditions", Analysis.p.nbconditions, 0);
		
//		Button bp = new Button("Set names and number of images per condition");
//		Panel p = new Panel();
//		p.add(bp);
//		bp.addActionListener(new PSFOpenerActionListener(gd));
//		
//		gd.addPanel(p);

		gd.addMessage("    R script data analysis settings",bf);
		
		Button rscript = new Button("Set condition names and number of images per condition");
		Panel p = new Panel();
		p.add(rscript);
		rscript.addActionListener(new RScriptListener(gd)); 

		
		gd.addPanel(p);
		
		
		
		
		gd.showDialog();
		if (gd.wasCanceled()) return;

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
		
		//Analysis.p.nbgroups=(int) gd.getNextNumber();
	}


}
