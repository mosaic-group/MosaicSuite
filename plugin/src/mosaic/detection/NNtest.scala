package mosaic.detection

import scalala.Scalala._;
import scalala.tensor.dense._
import weka.core.DenseInstance


object NNtest {
  def main(args : Array[String]) : Unit = {
	  
	  val d = 2

	  val nn = new NearestNeighbour
	  
	  val nbrRefPoints = 100
//	  val nbrRefpoints = 10000000 //uses approx. 1 GB ram
	  // independent randomly placed reference objects
//	  var refPoints = Array.fill(nbrRefPoints)(rand(d).toArray)
	  // regularly placed reference objects
	  var refPoints = nn.getMesh(1, 5, 1, 5)
	  
	  val nbrQueryPoints = 1000
	  // independent randomly placed query objects
//	  val queryPoints = Array.fill(nbrQueryPoints)(rand(d).toArray)
	  // regularly placed query objects
	  val queryPoints = nn.getMesh(1, 1000, 1, 2000)
	  
	  
	  val time = (new java.util.Date()).getTime()
	  // generate KDTree
	  nn.setReferenceGroup(refPoints)
	  println("Generation KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	  // find NN
      val dist = nn.getDistances(queryPoints) 
      println("Generation + Search KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	  dist.slice(0,5).map(println(_))
	  
	  
	  // estimate q(d)
	  val est = new KernelDensityEstimator()
	  est.addValues(dist)
	  
	  val maxDist = dist.reduceLeft(Math.max(_,_))
	  val minDist = dist.reduceLeft(Math.min(_,_))
	  
	  val x = linspace(minDist, maxDist, 100)
	  val prob = est.getProbabilities(x.toArray)
	  plot(x, new DenseVector(prob))
	  
  }
}
