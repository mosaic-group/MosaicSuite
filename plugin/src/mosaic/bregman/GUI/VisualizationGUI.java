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
import mosaic.bregman.output.CSVOutput;
import mosaic.core.GUI.OutputGUI;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mosaic.bregman.output.SquasshOutputChoose;


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
		
		Button b = new Button("Output options");
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
			
				OutputGUI og = new OutputGUI();
				
				CSVOutput.occ = (SquasshOutputChoose) og.visualizeOutput(CSVOutput.oc);
			}
			
		});
		
		gd.add(b);
		
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
