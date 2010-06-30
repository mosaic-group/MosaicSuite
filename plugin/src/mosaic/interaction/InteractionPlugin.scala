package mosaic.interaction

import mosaic.core.Particle
import mosaic.core.ImagePreparation
import mosaic.calibration.KernelDensityEstimator
import mosaic.calibration.NearestNeighbour

import ij.IJ
import ij.plugin.PlugIn
import ij.gui.GenericDialog
import scalala.Scalala._
import scalala.tensor.dense._
import cma.fitness.AbstractObjectiveFunction

class InteractionPlugin extends PlugIn with ImagePreparation {
	val dim = 3 //Image dimensions
	
	@Override
	def run(arg: String) {
		println("Run Interaction Plugin")
		allocateTwoImages();
		detect();
		val (d, qOfD) = calculateQofD()
		val dd = findD()
		val shape = selectPotential()

//		nll optimization CMA
		val fitfun = new LikelihoodOptimizer(new DenseVector(qOfD), new DenseVector(d),new DenseVector(dd), shape);
		potentialParamEst(fitfun)
	}
	
	def selectPotential(): ((DenseVector,Double,Double) => DenseVector)= {
		gd = new GenericDialog("Potential selection...", IJ.getInstance());
		gd.addChoice("Potential shape", PotentialFunctions.functions, PotentialFunctions.functions(0))
		gd.showDialog();
		PotentialFunctions.potentialShape(gd.getNextChoiceIndex)
	}
	
//		qOfD with NN and Kernel estimation
	def calculateQofD():(Array[Double],Array[Double])= {
	  
	  val nbrQueryPoints = 100
	  // independent randomly placed query objects
	  val queryPoints = Array.fill(nbrQueryPoints)(rand(dim).toArray)
	  // regularly placed query objects
//	  val queryPoints = nn.getMesh(List((1, 1*nbrQueryPoints),(1, 2*nbrQueryPoints),(1, 2*nbrQueryPoints)))
	  
	  val dist = getDistances(queryPoints)
      
      // estimate q(d)
	  val est = new KernelDensityEstimator()
	  est.addValues(dist)
	  
	  val maxDist = dist.reduceLeft(Math.max(_,_))
	  val minDist = dist.reduceLeft(Math.min(_,_))
	  
	  val x = linspace(minDist, maxDist, 100)
	  val xArray = x.toArray
	  val prob = est.getProbabilities(xArray)
	  plot(x, new DenseVector(prob))
	  (xArray, prob)
	}
	
	//		D with NN
	def findD():Array[Double]= {
	  val queryPoints: Array[Array[Double]] = getParticlePositions(1)
	  getDistances(queryPoints)
	}
	
	private def getDistances(queryPoints: Array[Array[Double]]):Array[Double]= {
			val nn = new NearestNeighbour(dim)
			val refPoints : Array[Array[Double]] = getParticlePositions(0)
			val time = (new java.util.Date()).getTime()
			// generate KDTree
			nn.setReferenceGroup(refPoints)
			println("Generation KDtree "+((new java.util.Date()).getTime() - time)*0.001)
			// find NN
			val dist = nn.getDistances(queryPoints) 
			println("Generation + Search KDtree "+((new java.util.Date()).getTime() - time)*0.001)
			dist
	}
	
	def potentialParamEst(fitfun: AbstractObjectiveFunction){
		CMAOptimization.optimize(fitfun)
	}
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private def showAbout() {
		IJ.showMessage("Interaction estimation based on statistical object-based co-localization framework",
				"TODO, shift the blame on the developper." //TODO showAbout   
		);
	}
}