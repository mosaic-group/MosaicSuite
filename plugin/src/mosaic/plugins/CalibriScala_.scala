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
import scala.collection.JavaConversions._

import org.apache.commons.math.stat.regression._
import javax.vecmath._
import scalala.tensor.dense._
import scalala.Scalala._


class CalibriScala_ extends PlugIn with PreviewInterface {
	
	var impA: ImagePlus= null; var impB:ImagePlus= null
	var detector: FeaturePointDetector = null
	
	var  previewA: PreviewCanvas = null; var  previewB: PreviewCanvas = null;
	val frames = new Array[MyFrame](2);
	var gd: GenericDialog = null;
	
	/* user defined parameters for linking*/
	val linkrange: Int =  1; 			// default
	val displacement: Double = 10.0; 	// default
	val frames_number: Int = 2;
	
	
	@Override
	def run(arg: String) {
		allocateTwoImages();
		detect();
		detector.linkParticles(frames, frames_number, linkrange, displacement);
		val shiftsWithPosition = calculateShifts();
		regression(shiftsWithPosition);
	}

	private def regression(shiftsWithPosition: Array[Array[Double]] ) {
		
		val xShifts = new Array[Double](shiftsWithPosition.length)
		val yShifts = new Array[Double](shiftsWithPosition.length)
		val dataX = new Array[Array[Double]](shiftsWithPosition.length,2)
		val dataY = new Array[Array[Double]](shiftsWithPosition.length,2)
		val xPos = new DenseVector(shiftsWithPosition.length)
		val yPos = new DenseVector(shiftsWithPosition.length)
		for (i <- Iterator.range(0, shiftsWithPosition.length-1)) {
			xShifts(i) = shiftsWithPosition(i)(0);
			yShifts(i) = shiftsWithPosition(i)(1);
			dataX(i)(1) = shiftsWithPosition(i)(0);
			dataY(i)(1) = shiftsWithPosition(i)(1);
			xPos(i) = shiftsWithPosition(i)(3)
			yPos(i) = shiftsWithPosition(i)(4)
			dataX(i)(0) = shiftsWithPosition(i)(3)
			dataY(i)(0) = shiftsWithPosition(i)(4)
			}
				
		val regression = new OLSMultipleLinearRegression()
//		// example from apache: http://commons.apache.org/math/userguide/stat.html#a1.5_Multiple_linear_regression
		regression.newSampleData(xPos.toArray , dataX)

		val beta = regression.estimateRegressionParameters()
		println("betas: " + beta.map("%.3f".format(_)).mkString(", "))
//		// residuals, if needed
		val residuals = regression.estimateResiduals()
		
		// Regression X
		val reg = new SimpleRegression();
		reg.addData(dataX);
		val xIntercept = reg.getIntercept()
		val xSlope = reg.getSlope()
		println("x intercept " + xIntercept +  ", x slope " + xSlope + ", x MSE " + reg.getMeanSquareError);

		// Regression Y
		reg.clear

		reg.addData(dataY);
		val yIntercept = reg.getIntercept()
		val ySlope = reg.getSlope()
		println("y intercept " + yIntercept + ", y slope " + ySlope + ", y MSE " + reg.getMeanSquareError) ;

		hold(false)
		scatter(xPos ,new DenseVector(xShifts), DenseVector((xShifts.length))(0.8), DenseVector(xShifts.length)(0.8))
		hold(true)
		plot(xPos, xPos * xSlope +xIntercept)
		xlabel("x axis")
		ylabel("y axis")
		hold(false)
		subplot(2,1,2)
		
		scatter(yPos ,new DenseVector(yShifts), DenseVector((yShifts.length))(0.8), DenseVector(yShifts.length)(0.1))
		hold(true)
		plot(yPos, yPos * ySlope +yIntercept)
		xlabel("x axis")
		ylabel("y axis")
		 
	}

	private def calculateShifts():Array[Array[Double]] = {
		val particlesA = frames(0).getParticles().toArray(new Array[Particle](0))
		val particlesB = frames(1).getParticles();
		val shiftsWithPosition = new Array[Array[Double]](particlesA.size,6);
		val shifts = new ListBuffer[Vector3f];
		val shiftPositions = new ListBuffer[Vector3f]();
		val shiftPart = new ListBuffer[Particle]();
		var i = 0;
		var tempVec = new Array[Double](3);
			
		for (parA <- particlesA)
		{
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
		var output = shiftsWithPosition.slice(0, i-1)
		previewA.shifts = new java.util.Vector[Vector3f](shifts);
		previewA.shiftPositions = new java.util.Vector[Vector3f](shiftPositions);
		previewA.particlesShiftedToDisplay = new java.util.Vector[Particle](shiftPart);
		output
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