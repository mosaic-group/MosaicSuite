package mosaic.core.sampling

import scalanlp.stats.sampling._
import scalala.Scalala._
import scalala.tensor.Vector
import scalala.tensor.dense.DenseVector
import mosaic.core.ScalalaUtils

class QDistribution(qofD:Vector, xis:Vector) extends ContinuousDistr[Double] {

	var CdfqofD:Vector = Vector(0)
	var Fj:Vector = Vector(0)
	var xj:Vector = Vector(0)
	
	precomputeCdf
	computeMidpointValues
	
//	plot(xis, CdfqofD)
//	subplot(2,1,2)
//	plot(xj, Fj)
	

	def unnormalizedLogPdf(x:Double): Double ={
		0
	}
	
	def logNormalizer : Double ={
		0
	}
	
	def draw(): Double ={
		//   1. Generate a random number from the standard uniform distribution; call this u.
		val u = Rand.uniform.draw
		//   2. Compute the value x such that F(x) = u; call this xchosen.
		val xChosen = inverseCdf(u)
		//   3. Take xchosen to be the random number drawn from the distribution described by F.
		xChosen
	}
	
	def inverseCdf(u: Double): Double = {
		val uPos = Fj.filter(ui => ui._2 <= u).size
		// interpolate between two data points
		ScalalaUtils.interpolateLinear(xj(uPos - 1), Fj(uPos - 1), xj(uPos), Fj(uPos), u)
	}
	
	def Cdf(x: Double): Double = {
		val xPos = xis.filter(xi => xi._2 <= x).size
//		val cdf:Double = qofD.filter(qi => qi._1 < xPos).map(_._2).reduceLeft(_ + _)
		CdfqofD(xPos-1)
	}

	def precomputeCdf {
		
		CdfqofD = Vector((0.0::partialSums(qofD.toArray.toList)) : _*)
		
		def partialSums(items:List[Double], currentSum:Double = 0):List[Double] = {
				items match {
					case Nil => Nil
					case h::Nil => List(currentSum + h)
					case h::t => {val newSum = currentSum + h; newSum::partialSums(t, newSum)}
				}
		}
	}
	
	def computeMidpointValues {
		//		% compute the midpoint values
		val n = xis.size - 1
		//Fj = (Fi(1:end-1)+Fi(2:end))/2;
		val tmp1:Vector = Vector(CdfqofD(0 until n).toArray: _*)
		val tmp2:Vector = Vector(CdfqofD(1 until n+1).toArray: _*)
		Fj = (tmp1 + tmp2) /2
		//xj = xi(2:end);
		xj = new DenseVector(n+2)
		//xj = [xj(1)-Fj(1)*(xj(2)-xj(1))/((Fj(2)-Fj(1)));
		//       xj;
		//       xj(n)+(1-Fj(n))*((xj(n)-xj(n-1))/(Fj(n)-Fj(n-1)))];
		for (i <- 1 to n) xj(i) = xis(i) 
		xj(0) = xj(1)-Fj(0)*(xj(2)-xj(1))/(Fj(1)-Fj(0))
		xj(n+1) = xj(n)+(1-Fj(n-2))*(xj(n)-xj(n-1))/(Fj(n-2)-Fj(n-3)) 
		//Fj = [0; Fj; 1];
		Fj  =  new DenseVector((Vector(0).elements ++ Fj.elements ++ Vector(1).elements).map(_._2).toArray)
	}
}