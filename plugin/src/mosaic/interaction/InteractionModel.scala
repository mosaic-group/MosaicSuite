package mosaic.interaction

import mosaic.calibration.NearestNeighbour
import ij.IJ
import scalanlp.optimize.StochasticGradientDescent
import mosaic.core.optimization.CMAOptimization
import mosaic.core.optimization.LikelihoodOptimizer
import scalala.Scalala._
import scalala.tensor.dense._
import scalala.tensor._
import mosaic.calibration.KernelDensityEstimator


class InteractionModel {
	
	// Image/domain dimension, normally 2 or 3
	val dim = 3 
	// 
	val nn = new NearestNeighbour(dim)
	var potentialShape:Potential = PotentialFunctions.potentials(0)
	
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
//	  val fig = figure()
//	  subplot(fig.rows+1,fig.cols,fig.rows * fig.cols +1)
//	  val xbins = linspace(minDist, maxDist, 100)
//	  val histH = hist(new DenseVector(distances),xbins)
	  
	  (prob, xArray)
	}
	
		/**
	 * Estimates parameter of potential
	 * @param fitfun function to optimize, which has potential parameter as parameter 
	 */
	def potentialParamEst(fitfun: LikelihoodOptimizer) : Array[Double] = {
		val nbrParameter = fitfun.nbrParameter
		
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
		//plotPotential(fitfun.potentialShape, solSteepestDescent._2.toArray)
		
		IJ.showMessage("Interaction Plugin: parameter estimation", solOutput + '\n' + solSteepestDescentOutput);
		
		//CMA solution parameter
		sol._2
	}
	
	/** Plots potential with specified shape and parameters
	 * @param shape :		potential shape
	 * @param parameters :	potential parameters
	 */
	def plotPotential(shape: (Vector,List[Double]) => Vector, parameters: Array[Double]) ={
		
		val para = PotentialFunctions.defaultParameters(parameters)
		val x = linspace(-5,50)
		val y = shape(x,para.tail) * para(0)
		
		val fig = figure()
		subplot(fig.rows+1,fig.cols,fig.rows * fig.cols +1)
		
		plot(x,y)
		title("phi(d) with " + PotentialFunctions.parametersToString(parameters) ); xlabel("d");	ylabel("phi(d)")	
	}
	
		//	D with NN
	def findD(queryPoints: Array[Array[Double]]):Array[Double]= {
	  getDistances(queryPoints)
	}
	
	/**
	 * Calculate distances of queryPoints (X) to nearest neighbor of reference group (Y)
	 * @param queryPoints for which we measure the distance to the nearest neighbor in the reference group
	 * @return distance of each query point to it's nearest neighbor
	 */
	def getDistances(queryPoints: Array[Array[Double]]):Array[Double]= {
			// find NN
				val time = (new java.util.Date()).getTime()
			val dist = nn.getDistances(queryPoints) 
				println("Search nearest neighbour in KDtree "+((new java.util.Date()).getTime() - time)*0.001)
//			val distf = nn.bruteForceNN(queryPoints) 
//			    println("brute force nearest neighbour search "+((new java.util.Date()).getTime() - time)*0.001)
// histogram of distances
			   val fig = figure()
			   if (fig.rows > 1) {
			    subplot(fig.rows+1,fig.cols,fig.rows * fig.cols +1)
			   }
			    val xbins = linspace(0, dist.reduceLeft(Math.max(_, _)), 20)
			hist(new DenseVector(dist),xbins)
			dist
	}
	
	/**
	 * Initializes KDTree with reference group (Y), to allow fast nearest neighbor search
	 */
	def initNearestNeighbour(refPoints : Array[Array[Double]]) {
			val time = (new java.util.Date()).getTime()
		// generate KDTree
		nn.setReferenceGroup(refPoints)
			println("Generation KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	}
	
	/**
	 * mesh to estimate q(d)
	 * @param domainSize size of domain, in which state density q should be sampled
	 * @return mesh
	 */
	def meshInCell(domainSize: Array[Int], isInDomain: (Array[Double] => Boolean)) : Array[Array[Double]]= {
	  
	  val nbrQueryPointLimit = 100000
	  val zQueryPointRes = domainSize(2)
	  var xQueryPointRes = 1
	  while (zQueryPointRes * (xQueryPointRes+1) * (xQueryPointRes+1) < nbrQueryPointLimit ){
	 	  xQueryPointRes = xQueryPointRes + 1
	  }
	  
	  //val scale = new DenseVector(domainSize map(_.toDouble))
	   
	  // independent randomly placed query objects
//	  val queryPoints = List.fill(nbrQueryPoints)((rand(dim) :*scale).toArray)
	  
	  // regularly placed query objects
	  val queryPoints = nn.getSampling(List((domainSize(0), xQueryPointRes),(domainSize(1), xQueryPointRes),(domainSize(2), zQueryPointRes))).toArray //TODO less queries in z direction.
	  // only take samples in the cell/domain
	  println("Number of query points: " + queryPoints.size)
	  val validQueryPoints = queryPoints.filter(isInDomain(_))
	  println("Number of valid query points: " + validQueryPoints.size)
	  validQueryPoints
	}

}