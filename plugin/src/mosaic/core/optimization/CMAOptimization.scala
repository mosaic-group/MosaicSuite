package mosaic.core.optimization

import cma.fitness.AbstractObjectiveFunction
import cma.CMAEvolutionStrategy

object CMAOptimization {
	
	def optimize(fitfun: AbstractObjectiveFunction, dim:Int):(Double,Array[Double]) = {
	
		// new a CMA-ES and set some initial values
		val cma = new CMAEvolutionStrategy();
		cma.readProperties(); // read options, see file CMAEvolutionStrategy.properties
		cma.setDimension(dim); // overwrite some loaded properties
		cma.setInitialX(0.5); // in each dimension, also setTypicalX can be used
		cma.setInitialStandardDeviation(0.2); // also a mandatory setting 
		cma.options.stopFitness = 1e-12;       // optional setting

		// initialize cma and get fitness array to fill in later
		val fitness = cma.init();  // new double[cma.parameters.getPopulationSize()];

		// initial output to files
		//TODO write to file CMA cma.writeToDefaultFilesHeaders(0); // 0 == overwrites old files

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
			//TODO write to file CMA	cma.writeToDefaultFiles();
			val outmod = 150;
			if (cma.getCountIter() % (15*outmod) == 1)
				cma.printlnAnnotation(); // might write file as well
			if (cma.getCountIter() % outmod == 1)
				cma.println(); 
		}
		// evaluate mean value as it is the best estimator for the optimum
		cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates the best ever solution 

		// final output
//TODO write to file CMA		cma.writeToDefaultFiles(1);
		cma.println();
		cma.println("Terminated due to");
		for (s <- cma.stopConditions.getMessages())
			cma.println("  " + s);
		cma.println("best function value " + cma.getBestFunctionValue() 
				+ " at evaluation " + cma.getBestEvaluationNumber());
			
		// we might return cma.getBestSolution() or cma.getBestX()
		(cma.getBestSolution().getFitness,cma.getBestX())
	}
}
