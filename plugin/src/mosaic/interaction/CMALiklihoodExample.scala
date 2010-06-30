package mosaic.interaction

import cma._
import cma.fitness._
import mosaic.calibration._

object CMALiklihoodExample {
	
	def main(args : Array[String]) : Unit = {
		val i = 0
		
		val DCell = ReadMat.getDenseVector("DCell.mat", "DCell",i)
		val ddCell = ReadMat.getDenseVector("ddCell.mat", "dCell",i)
		val qCell = ReadMat.getDenseVector("qCell.mat", "qCell" ,i)
		
		def fitfun: AbstractObjectiveFunction = new LikelihoodOptimizer(qCell, ddCell,DCell, PotentialFunctions.potentialShape(1));
		
		CMAOptimization.optimize(fitfun)
		
		println(fitfun.valueOf(Array(-0.021777343750001)))
	} // main  
}