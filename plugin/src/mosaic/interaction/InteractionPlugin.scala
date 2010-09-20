package mosaic.interaction

import mosaic.core.CellOutline
import ij.plugin.Macro_Runner
import mosaic.core.Particle
import mosaic.core.optimization._
import mosaic.core.ImagePreparation
import mosaic.calibration.NearestNeighbour

import ij.IJ
import ij.plugin.PlugIn
import ij.gui.GenericDialog
import scalala.Scalala._
import scalala.tensor.dense._
import scalala.tensor._
import cma.fitness.AbstractObjectiveFunction

class InteractionPlugin extends PlugIn with ImagePreparation {
	// Image/domain dimension, normally 2 or 3
	val dim = 3 
	// 
	val nn = new NearestNeighbour(dim)

	@Override
	def run(arg: String) {
		println("Run Interaction Plugin ")
		
		gd = new GenericDialog("Input selection...", IJ.getInstance());
		gd.addChoice("Input soutce:", Array("Image","Matlab"), "Image")
		gd.showDialog();
		val (domainSize,isInDomain,refGroup, testGroup) = gd.getNextChoiceIndex match {
				case 0 => generateModelInputFromImages
				case 1 => InteractionModelTest.readMatlabData
		}
		
		println("Image size " + domainSize(0) + "," + domainSize(1) + "," + domainSize(2) +  ", Sizes of refGroup, testGroup: " + refGroup.size + "," + testGroup.size)
//		no images below here
		
		val refGroupInDomain = refGroup.filter(isInDomain)
		val testGroupInDomain = testGroup.filter(isInDomain)
		println("In domain: Sizes of refGroupInDomain, testGroupInDomain: " + refGroupInDomain.size + "," + testGroupInDomain.size)

// nearest neighbor search(2x)
		initNearestNeighbour(refGroupInDomain)
		val (qOfD, d) = InteractionModel.calculateQofD(meshInCell(domainSize, isInDomain), getDistances)
		val dd = findD(testGroupInDomain)
		val shape = selectPotential()

//		nll optimization CMA
		val fitfun = new LikelihoodOptimizer(new DenseVector(qOfD), new DenseVector(d),new DenseVector(dd), PotentialFunctions.potentialShape(shape));
		PotentialFunctions.potentialParameters(shape) match { case (nbr,flag) => fitfun.nbrParameter = nbr;fitfun.nonParametric = flag }
		InteractionModel.potentialParamEst(fitfun)
		
//		hypothesis testing
//		Monte Carlo sample Tk with size K from the null distribution of T obtained by sampling N distances di from q(d)
//		additional Monte Carlo sample Uk to rank U
	}
	
	/** 
	 * Shows a dialog to the user, where he can choose one of the available potentials.
	 * The potential has to be defined in object PotentialFunctions
	 * @return by user selected potential
	 */
	def selectPotential():Int = {
		gd = new GenericDialog("Potential selection...", IJ.getInstance());
		gd.addChoice("Potential shape", PotentialFunctions.functions, PotentialFunctions.functions(0))
		gd.showDialog();
		val choice = gd.getNextChoiceIndex
		choice
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
	private def getDistances(queryPoints: Array[Array[Double]]):Array[Double]= {
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
	private def initNearestNeighbour(refPoints : Array[Array[Double]]) {
			val time = (new java.util.Date()).getTime()
		// generate KDTree
		nn.setReferenceGroup(refPoints)
			println("Generation KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	}
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private def showAbout() {
		IJ.showMessage("Interaction estimation based on statistical object-based co-localization framework",
				"TODO, shift the blame on the developper." //TODO showAbout   
		);
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