package mosaic.interaction

import cma.fitness.AbstractObjectiveFunction
import cma.CMAEvolutionStrategy
import ij.gui.GenericDialog
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
	
		// new a CMA-ES and set some initial values
		val cma = new CMAEvolutionStrategy();
		cma.readProperties(); // read options, see file CMAEvolutionStrategy.properties
		cma.setDimension(1); // overwrite some loaded properties
		cma.setInitialX(0.5); // in each dimension, also setTypicalX can be used
		cma.setInitialStandardDeviation(0.2); // also a mandatory setting 
		cma.options.stopFitness = 1e-12;       // optional setting

		// initialize cma and get fitness array to fill in later
		val fitness = cma.init();  // new double[cma.parameters.getPopulationSize()];

		// initial output to files
		cma.writeToDefaultFilesHeaders(0); // 0 == overwrites old files

		// iteration loop
		while(cma.stopConditions.getNumber() == 0) {

            // --- core iteration step ---
			val pop = cma.samplePopulation(); // get a new population of solutions
			pop.length
			for(i <- Iterator.range(0,pop.length)) {    // for each candidate solution i
            	// a simple way to handle constraints that define a convex feasible domain  
            	// (like box constraints, i.e. variable boundaries) via "blind re-sampling" 
            	                                       // assumes that the feasible domain is convex, the optimum is  
				while (!fitfun.isFeasible(pop(i)))     //   not located on (or very close to) the domain boundary,  
					pop(i) = cma.resampleSingle(i);    //   initialX is feasible and initialStandardDeviations are  
                                                       //   sufficiently small to prevent quasi-infinite looping here
                // compute fitness/objective value	
				fitness(i) = fitfun.valueOf(pop(i)); // fitfun.valueOf() is to be minimized
			}
			cma.updateDistribution(fitness);         // pass fitness array to update search distribution
            // --- end core iteration step ---

			// output to files and console 
			cma.writeToDefaultFiles();
			val outmod = 150;
			if (cma.getCountIter() % (15*outmod) == 1)
				cma.printlnAnnotation(); // might write file as well
			if (cma.getCountIter() % outmod == 1)
				cma.println(); 
		}
		// evaluate mean value as it is the best estimator for the optimum
		cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates the best ever solution 

		// final output
		cma.writeToDefaultFiles(1);
		cma.println();
		cma.println("Terminated due to");
		for (s <- cma.stopConditions.getMessages())
			cma.println("  " + s);
		cma.println("best function value " + cma.getBestFunctionValue() 
				+ " at evaluation " + cma.getBestEvaluationNumber());
			
		// we might return cma.getBestSolution() or cma.getBestX()
		
		println(fitfun.valueOf(Array(-0.021777343750001)))
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