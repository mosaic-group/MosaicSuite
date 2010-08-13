package mosaic.core.sampling

import scalala.Scalala._
import scalala.tensor._
import scalala.tensor.dense._
import scalanlp.stats.sampling.Rand
import mosaic.core.ScalalaUtils
import weka.core.matrix.{Matrix => WekaMatrix}

object HypothesisTesting {
	
	val L = 20 	// 20
	val N = 100 //TODO: = |D|
	val K = 50 	// 1000
	
	val alpha = 0.05 // significance level α
	
	var range = 3d

	
	def distanceCounts(samples : List[Double], range: Double = this.range): Vector = {
		val tls = linspace(0,range,L+1).toArray
		val indicator = (d:Double, t:Tuple2[Double,Double]) => (t._1 < d && d <= t._2)
		
		val pairs = tls.dropRight(1).zip(tls.drop(1))
		
		val T = pairs.map(t =>samples.filter(indicator(_,t)).length.toDouble)
		new DenseVector(T)
	}
	
	/**
	 * @param dist
	 */
	def testHypothesis(dist: Rand[Double]) = {
		// See subsection 8.3 'Test for Interaction' in paper "Beyond co-localization: ..."
		// Sample T from the null distribution to calculate E(T) and Cov(T)
		val TsforEandCov = new DenseMatrix(K,L)
		for (i <- (0 until K)) {
			TsforEandCov.getRow(i) := distanceCounts(dist.sample(N))
		}

		val EofT = ScalalaUtils.mean(TsforEandCov)
		val covOfT = ScalalaUtils.cov(TsforEandCov)
		
		val covOfTInverse = new DenseMatrix(L,L) // TODO scalala.inverse with \ operator
			// uses weka Matrix inverse method
			val mat = new WekaMatrix(L,L)
			covOfT.toArray.map(x => mat.set(x._1 ._1,x._1 ._2,x._2))
			val invCovArray = mat.inverse.getArray
			for (ii <- (0 until L)) {
				covOfTInverse.getRow(ii) := new DenseVector(invCovArray(ii))
			}
				
		// Sample T again to learn distribution of U = (E0(T) − T)t Cov0(T)−1 (E0(T) − T) .
		val TsforU = new DenseMatrix(K,L)
		val Us = new Array[Double](K)
		for (i <- (0 until K)) {
			TsforU.getRow(i) := distanceCounts(dist.sample(N))
			Us(i) = calculateU(TsforU.getRow(i), EofT, covOfTInverse)

		}
		scala.util.Sorting.quickSort(Us)
		
		
		// Rank U
		// If it ranked higher than (1 − α)K-th, H0 was rejected on the significance level α.
		val T = distanceCounts(dist.sample(N)) // TODO get real samples from p(d)
		val U = calculateU(T, EofT, covOfTInverse)
		val rankK = Us.findIndexOf(_ > U) + 1
		
		val H0rejected = rankK > Math.ceil((1- alpha) * K)
		println("Hypothesis alternative is " + H0rejected + ", ranked as: " + rankK + " of " + K + ".")
		
	}
	
	private def calculateU(T: Vector, expectationT:Vector, covTInverse: Matrix):Double = {
		val Tdiff = new DenseMatrix(L,1,(expectationT - T).toArray)
		// TODO solve vector.transpose hack nicely
		(Tdiff.transpose * covTInverse * Tdiff value)(0,0)
	}
	

}