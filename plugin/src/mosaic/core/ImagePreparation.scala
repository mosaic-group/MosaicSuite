package mosaic.core

import ij.plugin.filter.ImageProperties
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
	val frames = new Array[MyFrame](imageNbr)
	var cellOutline: CellOutline = null
	
	var detectors = new Array[FeaturePointDetector](imageNbr)
	 
	var gd: GenericDialog = null;
	
	var previewCanvas = new Array[PreviewCanvas](imageNbr)
	
	/**
	 * Initializes imp so that 2 images are available.
	 * @return true, if images are available
	 */
	protected def allocateTwoImages() = {
		// Get 2 Images ready and store them in imp(0) and imp(1).
		var windowList = WindowManager.getIDList();
		
		if (windowList == null){
			(new Macro_Runner).run("JAR:macros/StacksOpen_.ijm") // TODO Remove debug
			windowList = WindowManager.getIDList();
		}	
		
		if (windowList == null) {
			// No images open, have to open 2 images.
			val imp = openImages(imageNbr)
		} else if (windowList.length == 1) {
			// One image open, have to open another one.
			val imgs = openImages(imageNbr-1)
			imp(1) = imgs(0)
			imp(0) = WindowManager.getImage(windowList(0));
		} else if (windowList.length > 2) {
			// Select images
			// TODO Select images from windowList
			imp(0) = WindowManager.getImage(windowList(0));
			imp(1) = WindowManager.getImage(windowList(1));
		} else {
			// Two image open.
			imp(0) = WindowManager.getImage(windowList(0));
			imp(1) = WindowManager.getImage(windowList(1));
		}
		imp.map(checkVoxelDepth(_))
	}
	
	/** Checks current voxel depth and shows a dialog to change it, if it's set to 1 pixel.
	 * @param imp image processor to check
	 */
	private def checkVoxelDepth(imp: ImagePlus) = {
		val cal = imp.getCalibration
		if ((cal.pixelDepth == 1) & (cal.getZUnit == "pixel")) {
//			similiar to ij.plugin.filter.ImageProperties
			val gd = new GenericDialog(imp.getTitle())
			gd.addMessage("Unit of Length: "+cal.getZUnit);
			gd.addNumericField("Voxel_Depth:", cal.pixelDepth, 4, 8, null)
			var global1 = imp.getGlobalCalibration()!=null
			gd.addCheckbox("Global", global1)
			gd.showDialog()
			if (!gd.wasCanceled()) {
				cal.pixelDepth = gd.getNextNumber()
				if (gd.getNextBoolean()) {
					imp.setGlobalCalibration(cal)					
				}
			}
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
		
		detectors = imp.map(getDetector(_))
		
		gd = new GenericDialog("Particle detection...", IJ.getInstance());
		detectors(0).addUserDefinedParametersDialog(gd);
//		gd.addPanel(detector.makePreviewPanel(this, impA), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));	        

		previewCanvas(0) = detectors(0).generatePreviewCanvas(imp(0));
		previewCanvas(1) = detectors(1).generatePreviewCanvas(imp(1));
		gd.showDialog();
		detectors(0).getUserDefinedParameters(gd);
		detectors(1).setUserDefinedParameters(detectors(0).cutoff,detectors(0).percentile,detectors(0).radius,detectors(0).absIntensityThreshold)

		//TODO Detection done with preview. 
		preview(null)
	}
	
	private def getDetector(imp: ImagePlus): FeaturePointDetector ={
		// get global minimum and maximum 
		val stack_stats = new StackStatistics(imp);
		new FeaturePointDetector(stack_stats.max.asInstanceOf[Float], stack_stats.min.asInstanceOf[Float]);
	}
	
			
	protected def cellOutlineGeneration() {
		cellOutline = new CellOutline(imp(0))
	}
	
	/**
	 * @param e
	 */
	@Override
	def preview(e: ActionEvent) {
		// do preview
		detectors(0).preview(imp(0), previewCanvas(0), gd);
		frames(0) = detectors(0).getPreviewFrame();
		detectors(1).preview(imp(1), previewCanvas(1), gd);
		frames(1) = detectors(1).getPreviewFrame();
		previewCanvas(0).repaint();
		previewCanvas(1).repaint();
		return;
	}
	
	def getParticlePositions(i : Int): Array[Array[Double]] = {
		for (particle <- frames(i).getParticles.toArray) yield {
			val part = particle.asInstanceOf[Particle]
			part.getPosition()
		}
	}
//	protected def getParticles(): (List[Particle], List[Particle]) =  {
//		(frames(0).getParticles, frames(1).getParticles.toArray)
//	}

}