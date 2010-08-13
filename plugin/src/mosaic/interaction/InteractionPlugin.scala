package mosaic.interaction

import ij.plugin.Macro_Runner
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
	// Image/domain dimension, normally 2 or 3
	val dim = 3 
	// 
	val nn = new NearestNeighbour(dim)

	@Override
	def run(arg: String) {
		println("Run Interaction Plugin")
		
			(new Macro_Runner).run("JAR:macros/StacksOpen_.ijm") // TODO Remove debug
					
		allocateTwoImages()
		detect()
		cellOutlineGeneration()
		val voxelDepth = imp(0).getCalibration.pixelDepth.toInt
		val isInDomain = (x:Array[Double]) => {x(2) = x(2)/voxelDepth; cellOutline.inRoi(x)}
		initNearestNeighbour()
		val domainSize = Array[Int](imp(0).getHeight, imp(0).getWidth, imp(0).getNSlices * voxelDepth)
//		no images below here
		
		val (qOfD, d) = calculateQofD(domainSize, isInDomain)
		val dd = findD(isInDomain)
		val shape = selectPotential()

//		nll optimization CMA
		val fitfun = new LikelihoodOptimizer(new DenseVector(qOfD), new DenseVector(d),new DenseVector(dd), shape);
		potentialParamEst(fitfun)
		
//		hypothesis testing
//		Monte Carlo sample Tk with size K from the null distribution of T obtained by sampling N distances di from q(d)
//		additional Monte Carlo sample Uk to rank U
	}
	
	/** 
	 * Shows a dialog to the user, where he can choose one of the available potentials.
	 * The potential has to be defined in object PotentialFunctions
	 * @return by user selected potential
	 */
	def selectPotential(): ((DenseVector,Double,Double) => DenseVector) = {
		gd = new GenericDialog("Potential selection...", IJ.getInstance());
		gd.addChoice("Potential shape", PotentialFunctions.functions, PotentialFunctions.functions(0))
		gd.showDialog();
		PotentialFunctions.potentialShape(gd.getNextChoiceIndex)
	}
		
	/**
	 * qOfD with NN and Kernel estimation
	 * @param domainSize size of domain, in which state density q should be sampled
	 * @return state density as tuple (q,d) with values of state density q and distances d, at which q is specified
	 */
	def calculateQofD(domainSize: Array[Int], isInDomain: (Array[Double] => Boolean)):(Array[Double],Array[Double])= {
	  
	  val nbrQueryPoints = 10
	  val scale = new DenseVector(domainSize map(_.toDouble))
	   
	  // independent randomly placed query objects
//	  val queryPoints = List.fill(nbrQueryPoints)((rand(dim) :*scale).toArray)
	  
	  // regularly placed query objects
	  val queryPoints = nn.getSampling(List((domainSize(0), nbrQueryPoints),(domainSize(1), nbrQueryPoints),(domainSize(2), nbrQueryPoints)))
	  // only take samples in the cell/domain
	  println("Number of query points: " + queryPoints.size)
	  val validQueryPoints = queryPoints.filter(isInDomain(_))
	  println("Number of valid query points: " + validQueryPoints.size)
	   
	   //TODO Fix Z Coord.
	  val dist = getDistances(validQueryPoints.toArray)
      
      // estimate q(d)
	  val est = new KernelDensityEstimator()
	  est.addValues(dist)
	  
	  val maxDist = dist.reduceLeft(Math.max(_,_))
	  val minDist = Math.min(0,dist.reduceLeft(Math.min(_,_))) //TODO correct? with 0?
	  
	  val x = linspace(minDist, maxDist, 100)
	  val xArray = x.toArray
	  val prob = est.getProbabilities(xArray)
	  plot(x, new DenseVector(prob))
	  title("q(D)"); xlabel("d"); ylabel("q(d)")

	  (prob, xArray)
	}
	
	//	D with NN
	def findD(isInDomain: (Array[Double] => Boolean)):Array[Double]= {
	  val queryPoints: Array[Array[Double]] = getParticlePositions(1)
	  getDistances(queryPoints.filter(isInDomain(_)))
	}
	
	/**
	 * Calculate distances of queryPoints (X) to nearest neighbor of reference group (Y)
	 * @param queryPoints for which we measure the distance to the nearest neighbor in the reference group
	 * @return distance of each query point to it's nearest neighbor
	 */
	private def getDistances(queryPoints: Array[Array[Double]]):Array[Double]= {
			// find NN
				val time = (new java.util.Date()).getTime()
			val dist = nn.getDistances(queryPoints) 
				println("Search nearest neighbour in KDtree "+((new java.util.Date()).getTime() - time)*0.001)
			dist
	}
	
	/**
	 * Initializes KDTree with reference group (Y), to allow fast nearest neigbhor search
	 */
	private def initNearestNeighbour() {
		val refPoints : Array[Array[Double]] = getParticlePositions(0)
			val time = (new java.util.Date()).getTime()
		// generate KDTree
		nn.setReferenceGroup(refPoints)
			println("Generation KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	}
	
	/**
	 * Estimates parameter of potential
	 * @param fitfun function to optimize, which has potential parameter as parameter 
	 */
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