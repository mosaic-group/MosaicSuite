package mosaic.interaction

import mosaic.core.Particle
import mosaic.core.ImagePreparation
import mosaic.calibration.KernelDensityEstimator
import mosaic.calibration.NearestNeighbour

import ij.IJ
import ij.plugin.PlugIn
import scalala.Scalala._
import scalala.tensor.dense._

class InteractionPlugin extends PlugIn with ImagePreparation {
	val dim = 3 //Image dimensions
	
	@Override
	def run(arg: String) {
		println("Run Interaction Plugin")
		allocateTwoImages();
		detect();
		val (d, qOfD) = calculateQofD()
		val dd = findD()
		
//		Select potential
//		nll optimization CMA
	}
	
//		qOfD with NN and Kernelest
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
	
	def getDistances(queryPoints: Array[Array[Double]]):Array[Double]= {
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
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private def showAbout() {
		IJ.showMessage("Interaction estimation based on statistical object-based co-localization framework",
				"TODO, shift the blame on the developper." //TODO showAbout   
		);
	}
}