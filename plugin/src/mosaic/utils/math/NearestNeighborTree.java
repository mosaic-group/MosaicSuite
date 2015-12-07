package mosaic.utils.math;


import java.util.ArrayList;

import javax.vecmath.Point3d;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;


/**
 * This class implements Nearest Neighbor tree. It calculates nearest distance(s) of given point(s) 
 * to provided in constructor base points.
 */
public class NearestNeighborTree {

    private KDTree iTree;
    private Instances iBasePoints;

    /**
     * @param aPoints Input base points to which distances will be calculated
     */
    public NearestNeighborTree(Point3d[] aPoints) {
        final ArrayList<Attribute> attributes = new ArrayList<Attribute>(3);
        attributes.add(new Attribute("x"));
        attributes.add(new Attribute("y"));
        attributes.add(new Attribute("z"));
        iBasePoints = new Instances("Y", attributes, 0);
        for (int i = 0; i < aPoints.length; i++) {
            final double[] coordinates = new double[3];
            aPoints[i].get(coordinates);
            final Instance inst = new DenseInstance(1, coordinates);
            inst.setDataset(iBasePoints);
            iBasePoints.add(inst);
        }

        try {
            iTree = new KDTree();
            iTree.setInstances(iBasePoints);
            final EuclideanDistance df = (EuclideanDistance) iTree.getDistanceFunction();
            df.setDontNormalize(true);
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param aPoints Points for which nearest neighbor distances should be calculated
     * @return array of distances for each input point
     */
    public double[] getDistancesToNearestNeighbors(Point3d[] aPoints) {
        final double[] distances = new double[aPoints.length];

        for (int i = 0; i < aPoints.length; i++) {
            distances[i] = getDistanceToNearestNeighbor(aPoints[i]);
        }
        
        return distances;
    }
    
    /**
     * @param aPoint Point for which nearest neighbor distance should be calculated
     * @return array of distances for each input point
     */
    public double getDistanceToNearestNeighbor(Point3d aPoint) {
        final double[] coordinates = new double[3];
        aPoint.get(coordinates);
        final Instance inst = new DenseInstance(1, coordinates);
        inst.setDataset(iBasePoints);
        
        double distance = Double.MAX_VALUE;
        try {
            iTree.nearestNeighbour(inst);
            distance = iTree.getDistances()[0];
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return distance;
    }
}
