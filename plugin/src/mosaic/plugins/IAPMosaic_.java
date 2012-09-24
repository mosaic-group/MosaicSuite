package mosaic.plugins;

import mosaic.ia.gui.GUIDesign;


import ij.plugin.PlugIn;


/**
 * @author arun.shivanandan@inf.ethz.ch
 * To do: Mask check. for q(d), p(d). ROI. h in q(d).3D
 * sorting array of NN: might be dangerous.
 *
 */
/**
 * @author arun
 *
 *
 *
 *
 */


public class IAPMosaic_ implements PlugIn {
	
	
	 
	@Override
	public void run(String arg0) {
	
		try {
			GUIDesign window = new GUIDesign();
			window.frmInteractionAnalysis.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	    return;
	}
	
	
	

	
	
	
	
	

	


		
	
	

	

			
			
	
	


	
	
}







