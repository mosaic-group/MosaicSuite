package mosaic.core.sampling

import scalala.Scalala._
import scalala.tensor._
import scalala.tensor.dense._
import scalanlp.stats.sampling.Rand
import mosaic.interaction.ScalalaUtils
import weka.core.matrix.{Matrix => WekaMatrix}

object HypothesisTesting {
	
	val L = 20 	// 20
	var N = 100 //TODO: = |D|
	val K = 50 	// 1000
	
	val alpha = 0.05 // significance level α
	
	var range = 15d
	
	var f: (Vector => Vector) = null

	/**
	 * @param qDist : H0 null hypothesis q(d)
	 * @param pDist : H1 p(d)
	 */
	def testHypothesis(qDist: Rand[Double], pDist: Rand[Double]) = {
		// See section 5. 'Improving statistical power with Non-step potential' in paper "Beyond co-localization: ..."
		val Ts = new Array[Double](K)
		for (i <- (0 until K)) {
			Ts(i) = calculateT(scalala.tensor.Vector(qDist.sample(N):_*))
		}
				
		
		scala.util.Sorting.quickSort(Ts)
		
		
		// Rank T
		// If it ranked higher than (1 − α)K-th, H0 was rejected on the significance level α.
		val T = calculateT(scalala.tensor.Vector(pDist.sample(N) :_*)) // TODO get real samples from p(d)
		
		val H0  = isH0rejected(T, Ts)
		println("Hypothesis alternative is " + H0._1 + ", ranked as: " + H0._2 + " of K = " + K + ". N = " + N)	
	}
	
	def calculateT(d:scalala.tensor.Vector): Double ={
		-sum(f(d))
	}
	
	
	/**
	 * @param dist
	 */
	def testNonParamHypothesis(qDist: Rand[Double], pDist: Rand[Double]) = {
		// See subsection 8.3 'Test for Interaction' in paper "Beyond co-localization: ..."
		// Sample T from the null distribution to calculate E(T) and Cov(T)
		val TsforEandCov = new DenseMatrix(K,L)
		for (i <- (0 until K)) {
			TsforEandCov.getRow(i) := distanceCounts(qDist.sample(N))
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
			TsforU.getRow(i) := distanceCounts(qDist.sample(N))
			Us(i) = calculateU(TsforU.getRow(i), EofT, covOfTInverse)

		}
		scala.util.Sorting.quickSort(Us)
		
		
		// Rank U
		// If it ranked higher than (1 − α)K-th, H0 was rejected on the significance level α.
		val T = distanceCounts(pDist.sample(N)) // TODO get real samples from p(d)
		val U = calculateU(T, EofT, covOfTInverse)
		
		val H0 = isH0rejected(U, Us)
		println("Non-param: Hypothesis alternative is " + H0._1 + ", ranked as: " + H0._2 + " of K = " + K + ". N = " + N)	
	}
		
	def distanceCounts(samples : List[Double], range: Double = this.range): Vector = {
		val tls = linspace(0,range,L+1).toArray
		val indicator = (d:Double, t:Tuple2[Double,Double]) => (t._1 < d && d <= t._2)
		
		val pairs = tls.dropRight(1).zip(tls.drop(1))
		
		val T = pairs.map(t =>samples.filter(indicator(_,t)).length.toDouble)
		new DenseVector(T)
	}
	
	private def calculateU(T: Vector, expectationT:Vector, covTInverse: Matrix):Double = {
		val Tdiff = new DenseMatrix(L,1,(expectationT - T).toArray)
		// TODO solve vector.transpose hack nicely
		(Tdiff.transpose * covTInverse * Tdiff value)(0,0)
	}
	
	def isH0rejected(T:Double, Ts:Array[Double]) : (Boolean, Int) = {
		val rankK = {val index = Ts.findIndexOf(_ > T); if (index >= 0) index + 1 else Ts.size}
		
		val H0rejected = rankK > Math.ceil((1- alpha) * K)
		(H0rejected, rankK)
	}
}