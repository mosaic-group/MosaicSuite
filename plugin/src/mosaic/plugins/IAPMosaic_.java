package mosaic.plugins;

import ij.plugin.PlugIn;
import mosaic.core.utils.MosaicUtils;
import mosaic.ia.gui.GUIDesign;


/**
 * @author arun.shivanandan@inf.ethz.ch
 * To do: Mask check. for q(d), p(d). ROI. h in q(d).3D
 * sorting array of NN: might be dangerous.
 */
public class IAPMosaic_ implements PlugIn {
	@Override
	public void run(String arg0) {
		if (MosaicUtils.checkRequirement() == false) return;
		
		GUIDesign window = new GUIDesign();
		window.frmInteractionAnalysis.setVisible(true);
	}
}
