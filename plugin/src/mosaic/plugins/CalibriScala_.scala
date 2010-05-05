package mosaic.plugins

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.StackStatistics;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.vecmath._;

import mosaic.core.Particle;
import mosaic.detection.FeaturePointDetector;
import mosaic.detection.MyFrame;
import mosaic.detection.PreviewCanvas;
import mosaic.detection.PreviewInterface;
import mosaic.detection.Regression;

import scala.collection.mutable.ListBuffer


class CalibriScala_ extends PlugIn with PreviewInterface {
	
	var impA: ImagePlus= null; var impB:ImagePlus= null
	var detector: FeaturePointDetector = null
	
	var  previewA: PreviewCanvas = null; var  previewB: PreviewCanvas = null;
	val frames = new Array[MyFrame](2);
	var gd: GenericDialog = null;
	
	/* user defined parameters for linking*/
	val linkrange: Int = 1; 			// default
	val displacement: Double = 10.0; 	// default
	val frames_number: Int = 2;
	
	
	@Override
	def run(arg: String) {
		allocateTwoImages();
		detect();
		detector.linkParticles(frames, frames_number, linkrange, displacement);
		calculateShifts();
		regression(calculateShifts());
	}

	private def regression(shiftsWithPosition: Array[Array[Double]] ) {
		Regression.regression(shiftsWithPosition);
	}

	private def calculateShifts():Array[Array[Double]] = {
		val particlesA = frames(0).getParticles().toArray(new Array[Particle](0))
		val particlesB = frames(1).getParticles();
		val shiftsWithPosition = new Array[Array[Double]](particlesA.size,6);
		val shifts = new ListBuffer[Vector3f];
		val shiftPositions = new ListBuffer[Vector3f]();
		val shiftPart = new ListBuffer[Particle]();
			
		for (parA <- particlesA)
		{
			var i = 0;
			var tempVec = new Array[Double](3);
			if (parA.next(0) >= 0) {
				var parB = particlesB.get(parA.next(0));
				shifts += new Vector3f(parB.x -parA.x,parB.y -parA.y, parB.z -parA.z);
				shiftPositions +=new Vector3f(parA.x,parA.y,parA.z);
				
				//TODO clean up: makes data structure scala compatible
				(new Vector3d(shifts(i))).get(tempVec);
				System.arraycopy(tempVec, 0, shiftsWithPosition(i), 0, 3);
				(new Vector3d(shiftPositions(i))).get(tempVec);
				System.arraycopy(tempVec, 0, shiftsWithPosition(i), 3, 3);
				
				shiftPart += parB;
				i = i+1;
			}
		}
		previewA.shifts = new java.util.Vector[Vector3f](scala.collection.JavaConversions.asCollection(shifts));
		previewA.shiftPositions = new java.util.Vector[Vector3f](scala.collection.JavaConversions.asCollection(shiftPositions));
		previewA.particlesShiftedToDisplay = new java.util.Vector[Particle](scala.collection.JavaConversions.asCollection(shiftPart));
		shiftsWithPosition
	}


	/**
	 * Returns a * c + b
	 * @param a: y-coordinate
	 * @param b: x-coordinate
	 * @param c: width
	 * @return
	 */
	private def coord (a: Int, b: Int, c : Int):Int = (((a) * (c)) + (b));

	private def detect() {
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
		// Detection done with preview. TODO
	}

	/**
	 * Initializes imgA and imgB so that 2 images are available.
	 * @return
	 */
	def allocateTwoImages(): Boolean = {
		// Get 2 Images ready and store them in imgA and imgB.
		val windowList = WindowManager.getIDList();
		
		if (windowList == null) {
			// No images open, have to open 2 images.
			impA = IJ.openImage();
			impA.show();
			impB = IJ.openImage();
			impB.show();
			true;
		} else if (windowList.length == 1) {
			// One image open, have to open another one.
			impB = IJ.openImage();
			impB.show();
			impA = WindowManager.getImage(windowList(0));
			true;
		} else if (windowList.length > 2) {
			// Select images
			// TODO
			false;
		} else {
			// Two image open.
			impA = WindowManager.getImage(windowList(0));
			impB = WindowManager.getImage(windowList(1));
			true;
		}
	}
		
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
	
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private def showAbout() {
		IJ.showMessage("Calibration...",
				"TODO, shift the blame on the developper." //TODO     
		);
	}

}