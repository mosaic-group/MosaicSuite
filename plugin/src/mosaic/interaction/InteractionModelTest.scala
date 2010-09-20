package mosaic.interaction

import scalala.tensor.dense.DenseVector
import mosaic.core.optimization.LikelihoodOptimizer
import mosaic.calibration.NearestNeighbour
import mosaic.interaction._

object InteractionModelTest {
	
		val dim = 3 
	// 
	val nn = new NearestNeighbour(dim)
	val path = "/Users/marksutt/Documents/MA/data/"
	
	
	def main(args : Array[String]) : Unit = {
	
			
		val (domainSize,isInDomain,refGroup, testGroup) = readMatlabData
	
	
		val refGroupInDomain = refGroup.filter(isInDomain)
		val testGroupInDomain = testGroup.filter(isInDomain)
		println("In domain: Sizes of refGroupInDomain, testGroupInDomain: " + refGroupInDomain.size + "," + testGroupInDomain.size)

// nearest neighbor search(2x)
		nn.setReferenceGroup(refGroupInDomain)
		val dist = Array(0d,2,3,4,5,6,75,4)
		val (qOfD, d) = InteractionModel.estimateDensity(dist)
		val dd = getDistances(testGroupInDomain)
		val shape = PotentialFunctions.potentialShape(2) //hermquist

//		nll optimization CMA
		val fitfun = new LikelihoodOptimizer(new DenseVector(qOfD), new DenseVector(d),new DenseVector(dd), shape);
		fitfun.nbrParameter = 2
		InteractionModel.potentialParamEst(fitfun)
		
		
		// check optimization scriptMarkus.m, params_he =  6.2870    3.7056
		val (q,dq,dval) =ReadTestData.readOptimTest(InteractionModelTest.path)
		val fitfunTest = new LikelihoodOptimizer(q, dq,dval, shape);
		fitfunTest.nbrParameter = 2
		InteractionModel.potentialParamEst(fitfunTest)
		
//		hypothesis testing
//		Monte Carlo sample Tk with size K from the null distribution of T obtained by sampling N distances di from q(d)
//		additional Monte Carlo sample Uk to rank U
		
	}
	
	def readMatlabData():(Array[Int],(Array[Double] => Boolean),Array[Array[Double]],Array[Array[Double]]) = {
		import mosaic.calibration.ReadMat
		

		val (refGroup,queryGroup) =	ReadTestData.readPositions(path)
		val arr:Array[Array[Double]] = (for (i <- (0 until refGroup.rows)) yield refGroup.getRow(i).toArray).toArray
		val arrQuery:Array[Array[Double]] = (for (i <- (0 until queryGroup.rows)) yield queryGroup.getRow(i).toArray).toArray
		
		val (domainSize,isInDomain) = ReadTestData.readOutlinefromImages(path)
		(domainSize, isInDomain, arr, arrQuery)
	}
	
		/**
	 * Calculate distances of queryPoints (X) to nearest neighbor of reference group (Y)
	 * @param queryPoints for which we measure the distance to the nearest neighbor in the reference group
	 * @return distance of each query point to it's nearest neighbor
	 */
	private def getDistances(queryPoints: Array[Array[Double]]):Array[Double]= {
			// find NN
				val time = (new java.util.Date()).getTime()
//			val dist = nn.getDistances(queryPoints) 
//				println("Search nearest neighbour in KDtree "+((new java.util.Date()).getTime() - time)*0.001)
			val dist = nn.bruteForceNN(queryPoints) 
			    println("brute force nearest neighbour search "+((new java.util.Date()).getTime() - time)*0.001)
			dist
	}
	
	
}