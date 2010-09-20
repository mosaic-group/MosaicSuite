package mosaic.calibration

import weka.estimators.KernelEstimator

	/**
	 * <br>KernelDensityEstimater class is a wrapper for the KernelEstimator from the WEKA library
	 */
class KernelDensityEstimator {
	
	val est = new KernelEstimator(1) //TODO fix precision, gives wrong results if not equal to 1
	
	def addValues(values : Array[Double]) {
		values.foreach(est.addValue(_, 1))
	}
	
	def getProbability(data: Double) = est.getProbability(data)// * 100 //TODO find explication why probablities have wrong scale?
	
	def getProbabilities(data : Array[Double]): Array[Double] = {
		for (i <- data) yield est.getProbability(i)// * 100 //TODO find explication why probablities have wrong scale?
	}
}
