package mosaic.ia.nn;


import java.util.ArrayList;

import javax.vecmath.Point3d;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;


/*
 * Notation: pointsY: base set of points, pointsX: query points.
 */

public class KDTreeNearestNeighbor {

    private KDTree kdtree;
    private Instances pointsYinstances;

    public KDTreeNearestNeighbor(Point3d[] pointsY) {
        final Attribute x = new Attribute("x");
        final Attribute y = new Attribute("y");
        final Attribute z = new Attribute("z");

        final ArrayList<Attribute> attributes = new ArrayList<Attribute>(3);
        attributes.add(x);
        attributes.add(y);
        attributes.add(z);

        pointsYinstances = new Instances("Y", attributes, 0);

        for (int i = 0; i < pointsY.length; i++) {
            final double[] val = new double[3];
            pointsY[i].get(val);
            final Instance inst = new DenseInstance(1, val);
            inst.setDataset(pointsYinstances);
            pointsYinstances.add(inst);
        }
        kdtree = new KDTree();

        try {
            kdtree.setInstances(pointsYinstances);

            final EuclideanDistance df = (EuclideanDistance) kdtree.getDistanceFunction();
            df.setDontNormalize(true);

        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public double[] getNNDistances(Point3d[] pointsX) {
        final double[] D = new double[pointsX.length];

        for (int i = 0; i < pointsX.length; i++) {
            try {
                D[i] = getNNDistance(pointsX[i]);
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return D;
    }

    double getNNDistance(Point3d p) throws Exception {
        final Attribute x = pointsYinstances.attribute(0);
        final Attribute y = pointsYinstances.attribute(1);
        final Attribute z = pointsYinstances.attribute(2);

        final ArrayList<Attribute> attributes = new ArrayList<Attribute>(3);
        attributes.add(x);
        attributes.add(y);
        attributes.add(z);
        final double[] val = new double[3];

        p.get(val);

        final Instance inst = new DenseInstance(1, val);
        inst.setDataset(pointsYinstances);
        kdtree.nearestNeighbour(inst);
        return kdtree.getDistances()[0];
    }
}
