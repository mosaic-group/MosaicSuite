package mosaic.core

import ij.plugin.Macro_Runner
import ij.plugin.Duplicator
import mosaic.detection.MyFrame
import mosaic.detection.PreviewCanvas
import java.awt.event.ActionEvent
import mosaic.detection.PreviewInterface
import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import ij.gui.Roi

import mosaic.detection.FeaturePointDetector
import ij.process.StackStatistics
import scala.collection.JavaConversions


trait ImagePreparation {//extends PreviewInterface {
	
	val imageNbr = 2
	var imp = new Array[ImagePlus](imageNbr)
	val frames = new Array[MyFrame](imageNbr);
	var cellOutlines = new Array[CellOutline](imageNbr)
	
	var detector: FeaturePointDetector = null
	 
	var gd: GenericDialog = null;
	
	var  previewA: PreviewCanvas = null; var  previewB: PreviewCanvas = null;
	
	/**
	 * Initializes imgA and imgB so that 2 images are available.
	 * @return
	 */
	protected def allocateTwoImages(): Boolean = {
		// Get 2 Images ready and store them in imgA and imgB.
		val windowList = WindowManager.getIDList();
		
		if (windowList == null) {
			// No images open, have to open 2 images.
			val imp = openImages(2)
			true;
		} else if (windowList.length == 1) {
			// One image open, have to open another one.
			val imgs = openImages(1)
			imp(1) = imgs(0)
			imp(0) = WindowManager.getImage(windowList(0));
			true;
		} else if (windowList.length > 2) {
			// Select images
			// TODO Select images from windowList
			imp(0) = WindowManager.getImage(windowList(0));
			imp(1) = WindowManager.getImage(windowList(1));
			true;
		} else {
			// Two image open.
			imp(0) = WindowManager.getImage(windowList(0));
			imp(1) = WindowManager.getImage(windowList(1));
			true;
		}
	}
	
	/**
	 * Shows nbr open dialogs and returns the opened images as list
	 * @param nbr number of images to open
	 * @param images use default parameter value, (already opened images used for recursion)
	 * @return list of opened images
	 */
	private def openImages(nbr: Int, images :List[ImagePlus] = Nil):List[ImagePlus]  = {
		(nbr, images.length ) match{
			case (x,y) if x == y => images
			case (x,y) if x > y => openImages(x, {val n = IJ.openImage(); if (n== null) images else  n.show(); n::images})
		}
	}
	
	
	/**
	 * Detects particles in imp(0) and imp(1), 
	 * based on parameters entered by the user into the dialog 
	 */
	protected def detect() {
		import java.awt.GridBagConstraints;
		import java.awt.Insets;
		// TODO Do we use the same detector for both images or two different detectors?
		// get global minimum and maximum 
		val stack_statsA = new StackStatistics(imp(0));
		val stack_statsB = new StackStatistics(imp(1));
		val global_max = Math.max(stack_statsA.max, stack_statsB.max);
		val global_min = Math.min(stack_statsA.min, stack_statsB.min);
		detector = new FeaturePointDetector(global_max.asInstanceOf[Float], global_min.asInstanceOf[Float]);
		
		gd = new GenericDialog("Particle detection...", IJ.getInstance());
		detector.addUserDefinedParametersDialog(gd);
//		gd.addPanel(detector.makePreviewPanel(this, impA), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));	        

		previewA = detector.generatePreviewCanvas(imp(0));
		previewB = detector.generatePreviewCanvas(imp(1));
		gd.showDialog();
		detector.getUserDefinedParameters(gd);

		//TODO Detection done with preview. 
		preview(null)
	}
	
			
	protected def cellOutlineGeneration() {
		cellOutlines(0) = new CellOutline() 
		cellOutlines(0).init(imp(0))
		cellOutlines(1) = new CellOutline() 
		cellOutlines(1).init(imp(1))
	}
	
	/**
	 * @param e
	 */
	@Override
	def preview(e: ActionEvent) {
		// do preview
		detector.preview(imp(0), previewA, gd);
		frames(0) = detector.getPreviewFrame();
		detector.preview(imp(1), previewB, gd);
		frames(1) = detector.getPreviewFrame();
		previewA.repaint();
		previewB.repaint();
		return;
	}
	
	def getParticlePositions(i : Int): Array[Array[Double]] = {
		for (particle <- frames(i).getParticles.toArray) yield {
			val part = particle.asInstanceOf[Particle]
			part.getPosition()
			
			
			// TODO fix z coord.
		}
	}
//	protected def getParticles(): (List[Particle], List[Particle]) =  {
//		(frames(0).getParticles, frames(1).getParticles.toArray)
//	}

}