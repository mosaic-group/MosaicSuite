package mosaic.ia.nn;


import javax.vecmath.Point3d;

import ij.ImagePlus;

public class DistanceCalculationsCoords extends DistanceCalculations {

    public DistanceCalculationsCoords(Point3d[] X, Point3d[] Y, ImagePlus mask, double xmin, double ymin, double zmin, double xmax, double ymax, double zmax, double gridSize, double kernelWeightq,
            int discretizationSize) {
        super(mask, gridSize, kernelWeightq, discretizationSize);
        this.X = X;
        this.Y = Y;
        x1 = xmin;
        y1 = ymin;
        z1 = zmin;
        y2 = ymax;
        x2 = xmax;
        z2 = zmax;
    }

    private final Point3d[] X, Y; // unfiltered points
    private final double x1, x2, y1, y2, z1, z2; // ask for users input, if no mask. currently, force mask for csv.

    @Override
    public void calcDistances() {

        particleXSetCoord = applyMaskandgetCoordinates(X);
        particleYSetCoord = applyMaskandgetCoordinates(Y);
        
        genStateDensityForCoords();
        calcD();

    }

    private void genStateDensityForCoords() {
        stateDensity(x1, y1, z1, x2, y2, z2);
    }
}
