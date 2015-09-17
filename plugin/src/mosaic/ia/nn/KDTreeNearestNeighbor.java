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

    KDTree kdtree;
    Instances pointsYinstances;

    public void createKDTree(Point3d [] pointsY)
    {
        Attribute x = new Attribute("x");
        Attribute y = new Attribute("y");
        Attribute z = new Attribute("z");


        ArrayList<Attribute> attributes = new ArrayList<Attribute>(3);
        attributes.add(x);
        attributes.add(y);
        attributes.add(z);



        pointsYinstances = new Instances("Y", attributes, 0);

        for (int i=0;i<pointsY.length;i++)
        {
            double [] val=new double[3];
            pointsY[i].get(val);
            Instance inst = new DenseInstance(1,val);
            inst.setDataset(pointsYinstances);
            pointsYinstances.add(inst);
        }
        kdtree = new KDTree();

        try
        {
            kdtree.setInstances(pointsYinstances);

            EuclideanDistance df=(EuclideanDistance)kdtree.getDistanceFunction();
            df.setDontNormalize(true);

        }
        catch (Exception e) { e.printStackTrace();}

    }


    public double [] getNNDistances(Point3d [] pointsX)
    {

        double [] D=new double[pointsX.length];


        for (int i=0;i<pointsX.length;i++)
        {
            try {
                D[i]=getNNDistance(pointsX[i]);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
        return D;

    }






    public double  getNNDistance(Point3d p) throws Exception
    {
        Attribute x = pointsYinstances.attribute(0);
        Attribute y = pointsYinstances.attribute(1);
        Attribute z = pointsYinstances.attribute(2);

        ArrayList<Attribute> attributes = new ArrayList<Attribute>(3);
        attributes.add(x);
        attributes.add(y);
        attributes.add(z);
        double [] val=new double[3];



        p.get(val);

        Instance inst = new DenseInstance(1,val);
        inst.setDataset(pointsYinstances);
        kdtree.nearestNeighbour(inst);
        return kdtree.getDistances()[0];



    }




}
