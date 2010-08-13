package mosaic.calibration

import mosaic.core.ImagePreparation
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.StackStatistics;


import java.awt.event.ActionEvent;
import javax.vecmath._;

import mosaic.core.Particle;
import mosaic.detection.Regression;

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

import org.apache.commons.math.stat.regression._
import javax.vecmath._
import scalala.tensor.dense._
import scalala.Scalala._


class CalibriScala_ extends PlugIn with ImagePreparation {
	
	/* parameters for linking*/
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
//TODO refactor 2map		val xShifts = shifts.map(_.apply(0))
		
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
		
		val shiftsReg: DenseVector = (pos * slope + intercept)
		val zipped = shifts zip shiftsReg zip pos zip Array.range(0, shifts.size)
		val removed = Array.fill(shifts.size)(false)
		
		var redo = true
		while(redo) {
			intercept = reg.getIntercept
			slope = reg.getSlope
			mse = reg.getMeanSquareError
			println("intercept " + intercept +  ", slope " + slope + ", MSE " + mse);
			
			redo = false
			for ((((shift,(_,shiftReg)),(_,p)),i) <- zipped; if (!removed(i) && isOutlier(scala.Math.sqrt(mse), shift, shiftReg))) {
				val str = reg.getMeanSquareError
				reg.removeData(p, shift)
				removed(i) = true
				println("MSE " + str+ " new " + reg.getMeanSquareError +" "+p+ " "+shift)
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
		(scala.Math.abs( y- yEstimated)  > 10 *std)// && (std > 00000.1))
	}

	private def calculateShifts():(Array[Array[Double]],Array[Array[Double]]) = {
		val particles0 = frames(0).getParticles().toArray(new Array[Particle](0))
		val particles1 = frames(1).getParticles();
		val maxNbrShifts = scala.Math.min(particles0.size, particles1.size)
		// TODO find a data representation that fits Regression, Frame and plotting API
		val shifts = new Array[Array[Double]](maxNbrShifts,3);
		val shiftsPosition = new Array[Array[Double]](maxNbrShifts,3);
		val shiftsVec = new java.util.Vector[Vector3f];
		val shiftsPositionsVec = new java.util.Vector[Vector3f];
		val shiftPartVec = new java.util.Vector[Particle];
		var i = 0;
			
		for (parA <- particles0)
		{
			if (parA.next(0) >= 0) {
				var parB = particles1.get(parA.next(0));
				var shift = Array(parB.x -parA.x,parB.y -parA.y, parB.z -parA.z)
				
				shiftsVec += new Vector3f(shift);
				shiftsPositionsVec +=new Vector3f(parA.x,parA.y,parA.z);
				shiftPartVec += parB;
				
				shifts(i) = Array(shift(0),shift(1),shift(2))
				shiftsPosition(i) = Array(parA.x,parA.y,parA.z)
				
				i = i+1;
			}
		}
		previewCanvas(0).shifts = shiftsVec;
		previewCanvas(0).shiftPositions = shiftsPositionsVec;
		previewCanvas(0).particlesShiftedToDisplay = shiftPartVec;
		
		(shifts.slice(0, i), shiftsPosition.slice(0, i))
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