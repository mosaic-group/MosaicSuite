package mosaic.detection

import weka.core.Attribute
import java.util.ArrayList
import weka.core.DenseInstance
import weka.core.Instance
import weka.core.Instances
import weka.core.neighboursearch.KDTree

	/**
	 * <br>NearestNeighbour class is a wrapper for the NearestNeighbourSearch implemented through KDTree from the WEKA library
	 */

class NearestNeighbour(d: Int = 2) {
	
	private var instances : Instances = null
	private val kdtree = new KDTree()
	
	def setReferenceGroup(points: Array[Array[Double]]) {
		
		val atts: ArrayList[Attribute] = new ArrayList[Attribute]();
		atts.add(new Attribute("x"))
		atts.add(new Attribute("y"))
		instances = new Instances("ReferencePoints", atts, points.length)
		for (point <-points) instances.add(new DenseInstance(1, point))
		kdtree.setInstances(instances)
	}
	
	def getDistance(queryPoint: Array[Double]):Double ={
		val inst = new DenseInstance(1,queryPoint)
		inst.setDataset(instances)
		kdtree.nearestNeighbour(inst)
		(kdtree.getDistances())(0)
	}
}
