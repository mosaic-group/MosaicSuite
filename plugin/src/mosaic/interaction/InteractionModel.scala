package mosaic.interaction

import ij.IJ
import scalanlp.optimize.StochasticGradientDescent
import mosaic.core.optimization.CMAOptimization
import mosaic.core.optimization.LikelihoodOptimizer
import scalala.Scalala._
import scalala.tensor.dense._
import scalala.tensor._
import mosaic.calibration.KernelDensityEstimator


object InteractionModel {
	
	/**
	 * qOfD with NN and Kernel estimation
	 * @param domainSize size of domain, in which state density q should be sampled
	 * @return state density as tuple (q,d) with values of state density q and distances d, at which q is specified
	 */
	def calculateQofD(mesh: Array[Array[Double]], calculateDistance: (Array[Array[Double]]) => Array[Double]):(Array[Double],Array[Double])= {
	   
	  val dist = calculateDistance(mesh)
      
      // estimate q(d)
	  estimateDensity(dist)
	}
	
		
	/**
	 * p(d) with Kernel estimation
	 * @param distances
	 * @return state density as tuple (q,d) with values of state density q and distances d, at which q is specified
	 */
	def estimateDensity(distances:Array[Double]):(Array[Double],Array[Double])= {
		val est = new KernelDensityEstimator()
	  est.addValues(distances)
	  
	  val maxDist = distances.reduceLeft(Math.max(_,_))
	  val minDist = Math.min(0,distances.reduceLeft(Math.min(_,_))) //TODO correct? with 0?
	  
	  val x = linspace(minDist, maxDist, 1000)
	  val xArray = x.toArray
	  val prob = est.getProbabilities(xArray)
	  // TODO check prob. with integration equals 1.
	  plot(x, (new DenseVector(prob)))
	  title("q(D)"); xlabel("d"); ylabel("q(d)")
	  val fig = figure()
	  subplot(fig.rows+1,fig.cols,fig.rows * fig.cols +1)
	  val histH = hist(new DenseVector(distances),new DenseVector(Array(0d,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17)))
	  
	  (prob, xArray)
	}
	
		/**
	 * Estimates parameter of potential
	 * @param fitfun function to optimize, which has potential parameter as parameter 
	 */
	def potentialParamEst(fitfun: LikelihoodOptimizer, nbrParameter:Int){
		
		// CMA Optimization
		val sol = CMAOptimization.optimize(fitfun, nbrParameter)
		val solOutput = "CMA Optimization: " + PotentialFunctions.parametersToString(sol._2) + " min. value: " + sol._1
		println(solOutput)
		
		// Stochastic Steepest Descent
		val alpha = 0.0001
		val maxIter = 1000
		val batchSize = 10
		val initGuess = rand(nbrParameter)
		val stochasticSteepestDescent = new StochasticGradientDescent[Int, Vector](alpha, maxIter, batchSize)
		val minGuess = stochasticSteepestDescent.minimize(fitfun,initGuess)
		val solSteepestDescent = (fitfun.valueAt(minGuess), minGuess)
		val solSteepestDescentOutput = "Stochastic Steepest Descent:  " + PotentialFunctions.parametersToString(solSteepestDescent._2.toArray) + " min. value: " + solSteepestDescent._1
		println(solSteepestDescentOutput)
		
		plotPotential(fitfun.potentialShape, sol._2)
		plotPotential(fitfun.potentialShape, solSteepestDescent._2.toArray)
		
		IJ.showMessage("Interaction Plugin: parameter estimation", solOutput + '\n' + solSteepestDescentOutput);
		
	}
	
	/** Plots potential with specified shape and parameters
	 * @param shape :		potential shape
	 * @param parameters :	potential parameters
	 */
	def plotPotential(shape: (Vector,Double,Double) => Vector, parameters: Array[Double]) ={
		
		val para = PotentialFunctions.defaultParameters(parameters)
		val x = linspace(-5,100)
		val y = shape(x,para(1),para(2)) * para(0)
		
		val fig = figure()
		subplot(fig.rows+1,fig.cols,fig.rows * fig.cols +1)
		
		plot(x,y)
		title("phi(d) with " + PotentialFunctions.parametersToString(parameters) ); xlabel("d");	ylabel("phi(d)")	
	}

}