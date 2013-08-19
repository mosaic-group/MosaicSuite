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


public class ColocalizationGUI 
{
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;

	public ColocalizationGUI()
	{
	}


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);
		
		GenericDialogCustom  gd = new GenericDialogCustom("Colocalization options");
		
		gd.setInsets(-10,0,3);
		gd.addMessage("Colocalization (two channels images)",bf);
		
		String sgroup3[] = {"Cell_mask_channel_1", "Cell_mask_channel_2"};
		boolean bgroup3[] = {false, false};

		bgroup3[0] = Analysis.p.usecellmaskX;
		bgroup3[1] = Analysis.p.usecellmaskY;
		
		//gd.addCheckbox("Cell_mask_channel_1", false);
		//gd.addCheckbox("Cell_mask_channel_2", false);
		gd.addCheckboxGroup(1, 2, sgroup3, bgroup3);
		gd.addNumericField("threshold_channel_1 (0 to 1)", Analysis.p.thresholdcellmask, 4);
		gd.addNumericField("threshold_channel_2 (0 to 1)", Analysis.p.thresholdcellmasky, 4);
		
		gd.showDialog();
		if (gd.wasCanceled()) return;

		Analysis.p.usecellmaskX= gd.getNextBoolean();
		//IJ.log("maskX:" +  Analysis.p.usecellmaskX);
		Analysis.p.usecellmaskY= gd.getNextBoolean();
		//IJ.log("maskY:" +  Analysis.p.usecellmaskY);
		Analysis.p.thresholdcellmask= gd.getNextNumber();
		Analysis.p.thresholdcellmasky= gd.getNextNumber();
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
