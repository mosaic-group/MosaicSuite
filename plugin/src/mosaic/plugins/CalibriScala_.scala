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
		val (shifts, shiftsPosition) = calculateShifts();
		regression(shifts, shiftsPosition);
	}

	private def regression(shifts: Array[Array[Double]], shiftsPosition: Array[Array[Double]] ) {
		
		val n = shifts.length
		val xShifts = new Array[Double](n)
		val yShifts = new Array[Double](n)
		val dataX = new Array[Array[Double]](n,2)
		val dataY = new Array[Array[Double]](n,2)
		val xPos = new DenseVector(n)
		val yPos = new DenseVector(n)
		for (i <- Iterator.range(0, n)) {
			xShifts(i) = shifts(i)(0);
			yShifts(i) = shifts(i)(1);
			xPos(i) = shiftsPosition(i)(0)
			yPos(i) = shiftsPosition(i)(1)
			dataX(i)(1) = shifts(i)(0);
			dataY(i)(1) = shifts(i)(1);
			dataX(i)(0) = shiftsPosition(i)(0)
			dataY(i)(0) = shiftsPosition(i)(1)
			}
		
		// Regression X
		clf()
		println ("Regression X coord")
		executeRegression(dataX, xShifts, xPos, n)

		subplot(2,1,2)
		// Regression Y
		println ("Regression Y coord")
		executeRegression(dataY, yShifts, yPos, n)
	}
	
	private def executeRegression(data: Array[Array[Double]], shifts: Array[Double], pos: DenseVector, n: Int) {
		val reg = new SimpleRegression();
		reg.addData(data);
		var intercept :Double = 0
		var slope :Double = 0
		var mse :Double = 0
		
		var redo = true
		while(redo) {
			intercept = reg.getIntercept
			slope = reg.getSlope
			mse = reg.getMeanSquareError
			println("intercept " + intercept +  ", slope " + slope + ", MSE " + mse);
			
			val shiftsReg: DenseVector = (pos * slope + intercept)
			redo = false
			val zipped = shifts zip shiftsReg zip pos
			for (((shift,(_,shiftReg)),(_,p)) <-zipped; if isOutlier(scala.Math.sqrt(mse), shift, shiftReg)) {
				reg.removeData(p, shift)
				println("MSE new " + reg.getMeanSquareError)
				redo = true
			}
		}		

		scatter(pos ,new DenseVector(shifts), DenseVector(n)(0.8), DenseVector(n)(0.8))
		hold(true)
		plot(pos, pos * slope + intercept)
		xlabel("coordinates " + "Nbr: " + pos.size)
		ylabel("shifts")
		hold(false)
	}
	
	
	private def isOutlier(std: Double, y: Double, yEstimated : Double) : Boolean= {
		//println(scala.Math.abs( y- yEstimated))
		//TODO outlier detection with std, how can MSE be 0?
		((scala.Math.abs( y- yEstimated)  > 3 *std) && (std > 00000.1))
	}

	private def calculateShifts():(Array[Array[Double]],Array[Array[Double]]) = {
		val particlesA = frames(0).getParticles().toArray(new Array[Particle](0))
		val particlesB = frames(1).getParticles();
		val maxNbrShifts = scala.Math.min(particlesA.size, particlesB.size)
		// TODO find a data representation that fits Regression, Frame and plotting API
		val shifts = new Array[Array[Double]](maxNbrShifts,3);
		val shiftsPosition = new Array[Array[Double]](maxNbrShifts,3);
		val shiftsVec = new java.util.Vector[Vector3f];
		val shiftsPositionsVec = new java.util.Vector[Vector3f];
		val shiftPartVec = new java.util.Vector[Particle];
		var i = 0;
			
		for (parA <- particlesA)
		{
			if (parA.next(0) >= 0) {
				var parB = particlesB.get(parA.next(0));
				var shift = Array(parB.x -parA.x,parB.y -parA.y, parB.z -parA.z)
				
				shiftsVec += new Vector3f(shift);
				shiftsPositionsVec +=new Vector3f(parA.x,parA.y,parA.z);
				shiftPartVec += parB;
				
				shifts(i) = Array(shift(0),shift(1),shift(2))
				shiftsPosition(i) = Array(parA.x,parA.y,parA.z)
				
				i = i+1;
			}
		}
		previewA.shifts = shiftsVec;
		previewA.shiftPositions = shiftsPositionsVec;
		previewA.particlesShiftedToDisplay = shiftPartVec;
		
		(shifts.slice(0, i), shiftsPosition.slice(0, i))
	}


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

		//TODO Detection done with preview. 
		preview(null)
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
			// TODO Select images from windowList
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
				"TODO, shift the blame on the developper." //TODO showAbout   
		);
	}

}