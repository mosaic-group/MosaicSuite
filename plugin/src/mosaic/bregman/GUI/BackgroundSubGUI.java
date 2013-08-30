package mosaic.bregman.GUI;


import java.awt.Font;

import mosaic.bregman.Analysis;
import mosaic.bregman.GenericDialogCustom;
import ij.ImagePlus;


public class BackgroundSubGUI 
{
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;

	public BackgroundSubGUI()
	{
	}


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);
		
		GenericDialogCustom  gd = new GenericDialogCustom("Background subtractor options");
		
		gd.setInsets(-10,0,3);
		gd.addMessage("Background subtractor",bf);
		
		gd.addCheckbox("Remove background", Analysis.p.removebackground);
		
		gd.addNumericField("rolling ball window size (in pixels)", Analysis.p.size_rollingball,0);
		
		gd.showDialog();
		if (gd.wasCanceled()) return;

		//general options	
		Analysis.p.removebackground=gd.getNextBoolean();
		//IJ.log("rem back:" +  Analysis.p.removebackground);
		Analysis.p.size_rollingball=(int) gd.getNextNumber();
		//Analysis.p.usePSF=gd.getNextBoolean();
	}

}
