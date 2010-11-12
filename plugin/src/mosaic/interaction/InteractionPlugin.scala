package mosaic.interaction

import ij.IJ
import ij.plugin.PlugIn

class InteractionPlugin extends PlugIn with InteractionGUI  {

		
	@Override
	def run(arg: String) {
		if (arg == "about") {
			showAbout
		} else {
			println("Run the Object-based Interaction Analyis Plugin ")
			GUI()			
		}	
	}
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private def showAbout() {
		IJ.showMessage("Interaction estimation based on a statistical object-based co-localization framework.",
				"Shift the blame on the developper, for everything." //TODO showAbout   
		);
	}
}