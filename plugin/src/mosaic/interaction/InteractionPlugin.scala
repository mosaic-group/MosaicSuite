package mosaic.interaction

import mosaic.core.ImagePreparation
import ij.IJ
import ij.plugin.PlugIn

class InteractionPlugin extends PlugIn with ImagePreparation {
	
	@Override
	def run(arg: String) {
		println("Run Interaction Plugin")
		allocateTwoImages();
		detect();
		
//		qOfD with NN and Kernelest
//		D with NN
//		Select potential
//		nll optimization CMA
	}
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private def showAbout() {
		IJ.showMessage("Interaction estimation based on statistical object-based co-localization framework",
				"TODO, shift the blame on the developper." //TODO showAbout   
		);
	}
}