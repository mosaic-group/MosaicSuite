package mosaic.core.distance

import weka.core.EuclideanDistance
import scalala.tensor.dense.DenseVector
import java.util.ArrayList
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instance
import weka.core.Instances
import weka.core.neighboursearch.KDTree

import scalala.Scalala._
import scalala.Scalala

	/**
	 * <br>NearestNeighbour class is a wrapper for the NearestNeighbourSearch implemented through KDTree from the WEKA library
	 */
class NearestNeighbour(dim: Int = 2) {

	private var instances : Instances = null
	private val kdtree = new KDTree()
	
	private var refPoints:Array[Array[Double]] = null
	
	/** Initialize KDTree with reference group, so we can later find the nearest neighbor out of this points.
	 * @param points reference points
	 */
	def setReferenceGroup(points: Array[Array[Double]]) {
		
		refPoints = points
		
		val atts: ArrayList[Attribute] = new ArrayList[Attribute]();
		for (i <- Iterator.range(0, dim)) atts.add(new Attribute("coord" + i))
		instances = new Instances("ReferencePoints", atts, points.length)
		for (point <-points) instances.add(new DenseInstance(1, point))
		//set don't normalize flag, otherwise we get normalized distances. 
		(kdtree.getDistanceFunction).asInstanceOf[EuclideanDistance].setDontNormalize(true)
		kdtree.setInstances(instances)
	}
	
	/** Find nearest neighbor in reference group for each query point and return the distance to it.
	 * @param queries query points for which we want to find nearest neighbor distances
	 * @return distances to nearest neighbor for each query point
	 */
	def getDistances(queries : Array[Array[Double]]): Array[Double] ={
		queries.map(getDistance(_))
	}
	
	/** Find nearest neighbor in reference group for the query point and return the distance to it.
	 * @param queryPoint query point for which we want to find nearest neighbor distance
	 * @return distance to nearest neighbor for the query point
	 */
	private def getDistance(queryPoint: Array[Double]):Double ={
			val inst = new DenseInstance(1,queryPoint)
			inst.setDataset(instances)
			val nn = kdtree.nearestNeighbour(inst)
			(kdtree.getDistances())(0)
	}
	
	/** Generates a sampling grid from 0 to the value of the first element of the tuple with 
	 * the value of the second tuple element as number of grid points.
	 * Each tuple in the parameter list is a dimension in the domain
	 * @param domain For each dimension a tuple (max value of dimension, nbr of samples in this dimension)
	 */
	def getSampling(domain: List[(Int,Int)]) : List[Array[Double]]= {
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
		val dimCoordinates = for ((n, nbr) <- domain) yield linspace(0,n,nbr).toArray
		(for (res <- recPermutation(dimCoordinates)) yield res.toArray)
	}
//	def getSampling(n:Int, hNbr: Int, m:Int, vNbr: Int ) : Array[Array[Double]]= {
//		val x = linspace(0,n,hNbr).toArray
//		val y = linspace(0,m,vNbr).toArray
//		for (i <- x; j <- y) yield Array(i,j)
//	}
	
	def bruteForceNN(queries : Array[Array[Double]]): Array[Double] ={
		def dist(aV: DenseVector, b: Array[Double]):Double ={
			val bV = new DenseVector(b)
			norm(aV-bV,2)
		}
		queries.map( x => {val xV = new DenseVector(x); refPoints.map(dist(xV,_)).reduceLeft(_.min(_))})
	}
}
