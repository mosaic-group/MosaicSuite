package mosaic.detection

import scalala.Scalala._;
import weka.core.DenseInstance


object NNtest {
  def main(args : Array[String]) : Unit = {
	  
	  
	  val n = 10000
//	  val n = 10000000 //uses approx. 1 GB ram

	  var refPoints = (for (i <- Iterator.range(0,n)) yield rand(2).toArray)
	  
	  val nn = new NearestNeighbour
	  var xx = new Array[Array[Double]](n,2)
	  refPoints.copyToArray(xx)
	  nn.setReferenceGroup(xx)
	  

	   val dist = nn.getDistance(rand(2).toArray) 
	   println(dist)
	   println(nn.getDistance(rand(2).toArray))
	   println(nn.getDistance(rand(2).toArray))
	   println(nn.getDistance(rand(2).toArray))
	   println(nn.getDistance(rand(2).toArray))
	   println(nn.getDistance(rand(2).toArray))
  }
}
