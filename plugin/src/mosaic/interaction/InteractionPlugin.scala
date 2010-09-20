package mosaic.interaction

import ij.IJ
import ij.plugin.PlugIn
import scalala.Scalala._
import scalala.tensor.dense._
import scalala.tensor._
import cma.fitness.AbstractObjectiveFunction

class InteractionPlugin extends PlugIn with InteractionGUI {

		
	@Override
	def run(arg: String) {
		println("Run Interaction Plugin ")
		GUI()
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