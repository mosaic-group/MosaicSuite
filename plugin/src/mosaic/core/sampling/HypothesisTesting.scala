package mosaic.core.sampling

import scalala.Scalala._
import scalala.tensor._
import scalala.tensor.dense._
import scalanlp.stats.sampling.Rand
import mosaic.interaction.ScalalaUtils
import weka.core.matrix.{Matrix => WekaMatrix}

object HypothesisTesting {
	
	var L = 20 	// 20
	var N = 100 // := |D|
	var K = 50 	// 1000
	
	var alpha = 0.05 // significance level alpha
	
	var maxdInDomain = 15d // arg max q(d) >=0
	
	var f: (Vector => Vector) = null

	/**
	 * @param qDist : H0 null hypothesis q(d)
	 * @param pDist : H1 p(d)
	 */
	def testHypothesis(qDist: Rand[Double], D: Vector): (Boolean, Int, String) = {
		// See section 5. 'Improving statistical power with Non-step potential' in paper "Beyond co-localization: ..."
		val Ts = new Array[Double](K)
		for (i <- (0 until K)) {
			Ts(i) = calculateT(scalala.tensor.Vector(qDist.sample(N):_*))
		}
				
		
		scala.util.Sorting.quickSort(Ts)
		
		
		// Rank T
		// If it ranked higher than (1 -alpha)K-th, H0 was rejected on the significance level alpha.
		val T = calculateT(D)
		
		val H0  = isH0rejected(T, Ts)
		val result = "Hypothesis alternative is " + H0._1 + ", ranked as: " + H0._2 + " of K = " + K + ". Monte Carlo Samples N = " + N

		println(result)	
		(H0._1, H0._2, result)
	}
	
	def calculateT(d:scalala.tensor.Vector): Double ={
		-sum(f(d))
	}
	
	
	/**
	 * @param dist
	 */
	def testNonParamHypothesis(qDist: Rand[Double], D:Vector): (Boolean, Int, String) = {
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
		// If it ranked higher than (1 -alpha)K-th, H0 was rejected on the significance level alpha.
		val T = distanceCounts(D.toArray.toList) 
		val U = calculateU(T, EofT, covOfTInverse)
		
		val H0 = isH0rejected(U, Us)
		val result = "Hypothesis alternative is " + H0._1 + ", ranked as: " + H0._2 + " of K = " + K + ". Monte Carlo Samples N = " + N

		println("Non-param: "+ result)	
		(H0._1, H0._2, result)
	}
		
	def distanceCounts(samples : List[Double], range: Double = this.maxdInDomain): Vector = {
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