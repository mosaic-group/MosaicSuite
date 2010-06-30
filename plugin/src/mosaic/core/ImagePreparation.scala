package mosaic.core

import mosaic.detection.MyFrame
import mosaic.detection.PreviewCanvas
import java.awt.event.ActionEvent
import mosaic.detection.PreviewInterface
import ij.IJ
import ij.ImagePlus
import ij.WindowManager
import ij.gui.GenericDialog
import mosaic.core.Particle
import mosaic.detection.FeaturePointDetector
import ij.process.StackStatistics
import scala.collection.JavaConversions


trait ImagePreparation extends PreviewInterface {
	
	var impA: ImagePlus= null; var impB:ImagePlus= null
	val frames = new Array[MyFrame](2);
	
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
			val imgs = openImages(2)
			impA = imgs(0)
			impB = imgs(1)
			true;
		} else if (windowList.length == 1) {
			// One image open, have to open another one.
			val imgs = openImages(1)
			impB = imgs(0)
			impA = WindowManager.getImage(windowList(0));
			true;
		} else if (windowList.length > 2) {
			// Select images
			// TODO Select images from windowList
			impA = WindowManager.getImage(windowList(0));
			impB = WindowManager.getImage(windowList(1));
			true;
		} else {
			// Two image open.
			impA = WindowManager.getImage(windowList(0));
			impB = WindowManager.getImage(windowList(1));
			true;
		}
	}
	
	private def openImages(nbr: Int, images :List[ImagePlus] = Nil):List[ImagePlus]  = {
		(nbr, images.length ) match{
			case (x,y) if x == y => images
			case (x,y) if x > y => openImages(x, {val n = IJ.openImage(); if (n== null) images else  n.show(); n::images})
		}
	}
	
	
	protected def detect() {
		import java.awt.GridBagConstraints;
		import java.awt.Insets;
		// TODO Do we use the same detector for both images or two different detectors?
		// get global minimum and maximum 
		val stack_statsA = new StackStatistics(impA);
		val stack_statsB = new StackStatistics(impB);
		val global_max = Math.max(stack_statsA.max, stack_statsB.max);
		val global_min = Math.min(stack_statsA.min, stack_statsB.min);
		detector = new FeaturePointDetector(global_max.asInstanceOf[Float], global_min.asInstanceOf[Float]);
		
		gd = new GenericDialog("Particle detection...", IJ.getInstance());
		detector.addUserDefinedParametersDialog(gd);
		gd.addPanel(detector.makePreviewPanel(this, impA), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));	        

		previewA = detector.generatePreviewCanvas(impA);
		previewB = detector.generatePreviewCanvas(impB);
		gd.showDialog();
		detector.getUserDefinedParameters(gd);

		//TODO Detection done with preview. 
		preview(null)
	}
	
//	protected def getParticles(): (List[Particle], List[Particle]) =  {
//		(frames(0).getParticles, frames(1).getParticles.toArray)
//	}
	
	@Override
	def preview(e: ActionEvent) {
		// do preview
		detector.preview(impA, previewA, gd);
		frames(0) = detector.getPreviewFrame();
		detector.preview(impB, previewB, gd);
		frames(1) = detector.getPreviewFrame();
		previewA.repaint();
		previewB.repaint();
		return;
	}
	
	@Override
	def saveDetected(e: ActionEvent) {
		// TODO Auto-generated method stub
	}
	
	def getParticlePositions(i : Int): Array[Array[Double]] = {
		for (particle <- frames(i).getParticles.toArray) yield {
			val part = particle.asInstanceOf[Particle]
			part.getPosition()
		}
	}

}
