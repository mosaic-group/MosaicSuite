package mosaic.bregman.GUI;


import ij.gui.GenericDialog;

import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import mosaic.bregman.Analysis;

public class RScriptListener implements ActionListener {

	GenericDialog gd;
	int nbgroups;
	
	public RScriptListener(GenericDialog gd)
	{
		this.gd=gd;
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {

		nbgroups=new Integer(((TextField) gd.getNumericFields().elementAt(0)).getText());
		
		
		if((nbgroups != Analysis.p.nbconditions) || Analysis.p.initrsettings==true)
		{
			
			Analysis.p.nbimages= new int[nbgroups];
			for (int i=0; i < nbgroups; i++)
			{
				Analysis.p.nbimages[i]=1;
			}
			Analysis.p.groupnames= new String[nbgroups];

			for (int i=0; i < nbgroups; i++)
			{
				Analysis.p.groupnames[i]="Condition " + (i+1) + " name";
			}
			Analysis.p.nbconditions=nbgroups;
			Analysis.p.initrsettings=false;
			//Analysis.p.ch1="channel 1 name";
			//Analysis.p.ch2="channel 2 name";
		}

		RScriptWindow rw = new RScriptWindow(nbgroups);
		rw.run("");
	}

}
