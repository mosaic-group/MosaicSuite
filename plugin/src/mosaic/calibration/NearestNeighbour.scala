package mosaic.calibration

import java.util.ArrayList
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instance
import weka.core.Instances
import weka.core.neighboursearch.KDTree

import scalala.Scalala._

	/**
	 * <br>NearestNeighbour class is a wrapper for the NearestNeighbourSearch implemented through KDTree from the WEKA library
	 */
class NearestNeighbour(dim: Int = 2) {

	private var instances : Instances = null
	private val kdtree = new KDTree()
	
	def setReferenceGroup(points: Array[Array[Double]]) {
		
		val atts: ArrayList[Attribute] = new ArrayList[Attribute]();
		for (i <- Iterator.range(0, dim)) atts.add(new Attribute("coord" + i))
		instances = new Instances("ReferencePoints", atts, points.length)
		for (point <-points) instances.add(new DenseInstance(1, point))
		kdtree.setInstances(instances)
	}
	
	private def getDistance(queryPoint: Array[Double]):Double ={
		val inst = new DenseInstance(1,queryPoint)
		inst.setDataset(instances)
		kdtree.nearestNeighbour(inst)
		(kdtree.getDistances())(0)
	}
	
	def getDistances(queries : Array[Array[Double]]): Array[Double] ={
		queries.map(getDistance(_))
	}
	
	
	/** 
	 * @param List
	 */
	def getSampling(dim: List[(Int,Int)]) : List[Array[Double]]= {
		def recPermutation(lists : List[Array[Double]]): List[List[Double]] = lists match {
			case Nil => Nil
			case h::Nil => (for (i <- h) yield List(i)).toList
			case h::tail => {
				var result:List[List[Double]] = Nil
				for (permutation <- recPermutation(tail)) {
					for (i <- h) {
						result = (i:: permutation)::result
					}
				}
				result
			}
		}
		val dimCoordinates = for ((n, nbr) <- dim) yield linspace(0,n,nbr).toArray
		(for (res <- recPermutation(dimCoordinates)) yield res.toArray)
	}
//	def getSampling(n:Int, hNbr: Int, m:Int, vNbr: Int ) : Array[Array[Double]]= {
//		val x = linspace(0,n,hNbr).toArray
//		val y = linspace(0,m,vNbr).toArray
//		for (i <- x; j <- y) yield Array(i,j)
//	}
}
