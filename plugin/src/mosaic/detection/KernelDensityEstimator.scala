package mosaic.detection

import weka.estimators.KernelEstimator

	/**
	 * <br>KernelDensityEstimater class is a wrapper for the KernelEstimator from the WEKA library
	 */
class KernelDensityEstimator {
	
	val est = new KernelEstimator(0)
	
	def addValues(values : Array[Double]) {
		values.foreach(est.addValue(_, 1))
	}
	
	def getProbability(data: Double) = est.getProbability(data)
	
	def getProbabilities(data : Array[Double]): Array[Double] = {
		for (i <- data) yield est.getProbability(i)
	}
}
