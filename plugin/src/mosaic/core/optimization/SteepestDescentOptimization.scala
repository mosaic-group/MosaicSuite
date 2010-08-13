package mosaic.core.optimization

import scalala.Scalala._;
import scalala.tensor.{Tensor,Tensor1,Tensor2,Vector,Matrix};
import scalala.tensor.dense.{DenseVector,DenseMatrix};
import scalala.tensor.sparse._;

object SteepestDescentOptimization {
	val dim = 1

	def optimize(fitfun: DiffAbstractObjectiveFunction):(Double,Array[Double]) = {
		var xOld = new DenseVector(dim)
		var xNew = new DenseVector(Array(6d)) // The algorithm starts at x=6
		val eps = 0.01 // step size
		val precision = 0.00001
		
//	  		// gradient approximation with finite differences
		def grad(x: DenseVector, h:  Array[Double], function: Array[Double] => Double):DenseVector = {
			val xArray = x.toArray
			val fx = function(xArray)
			val tmp = h.zip(x)
			val gradient = tmp map ((i) => {val xTmp = xArray;  xTmp(i._2._1) =xTmp(i._2._1) + i._1; (function(xTmp) - fx) / i._1})
			new DenseVector(gradient)
		}

 
	    while (Math.abs(1) > precision) {
	    	xOld = xNew
	    	xNew = xOld - grad(xOld,Array.fill(xNew.size)(1d), fitfun.valueOf(_)) * eps
	    }
	    println("Local minimum occurs at " + xNew)
	    		// we might return cma.getBestSolution() or cma.getBestX()
	    val xNewArray = xNew.toArray
		(fitfun.valueOf(xNewArray),xNewArray)
	}
}
