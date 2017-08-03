package mosaic.region_competition.energies;


import ij.measure.Calibration;
import mosaic.core.imageUtils.MaskOnSpaceMapper;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.masks.BallMask;


public class CurvatureBasedFlow {

    private final MaskOnSpaceMapper sphereIt;
    private final LabelImage labelImage;

    private final int dim;
    private final int[] inputDims;
    private final float rad;

    private final BallMask sphere;

    // Helpers
    private final double vVolume;
    
    public CurvatureBasedFlow(int rad, LabelImage labelImage, Calibration cal) {
        this.rad = rad;
        this.dim = labelImage.getNumOfDimensions();
        this.inputDims = labelImage.getDimensions();
        this.labelImage = labelImage;

        float spacing[] = null;
        if (cal != null) {
            if (dim == 2) {
                spacing = new float[2];
                spacing[0] = (float) cal.pixelWidth;
                spacing[1] = (float) cal.pixelHeight;
                sphere = new BallMask(rad, 2 * rad, spacing);
            }
            else {
                spacing = new float[3];
                spacing[0] = (float) cal.pixelWidth;
                spacing[1] = (float) cal.pixelHeight;
                spacing[2] = (float) cal.pixelDepth;
                sphere = new BallMask(rad, 2 * rad, spacing);
            }
        }
        else {
            sphere = new BallMask(rad, dim);
        }
        sphereIt = new MaskOnSpaceMapper(sphere, inputDims);
        
        if (dim == 2) {
            vVolume = 3.141592f * rad * rad;
        }
        else if (dim == 3) {
            vVolume = 1.3333333f * 3.141592f * rad * rad * rad;
        }
        else {
            throw new RuntimeException("Curvature flow only implemented for 2D and 3D");
        }
//        //System.out.println("CURV VOLUME/RADIUS: " + vVolume + "/" + rad);
    }

    /**
     * uglier but faster version without sphere iterator
     * but field accesses
     */
    public double generateData(Point origin, int aFrom, int aTo) {
        int vNto = 0;
        int vNFrom = 0;

        /*
         * Point half = (new Point(m_Size)).div(2);
         * Point start = origin.sub(half); // "upper left point"
         */

        sphereIt.setMiddlePoint(origin);

        while (sphereIt.hasNext()) {
            final int idx = sphereIt.next();
            final int absLabel = labelImage.getLabelAbs(idx);

            if (absLabel == aTo) {
                vNto++;
            }
            else if (absLabel == aFrom) {
                vNFrom++;
            }
        }

        double vCurvatureFlow = 0.0;
        if (aFrom == LabelImage.BGLabel) // growing
        {
            final int vN = vNto;
            if (dim == 2) {
                vCurvatureFlow -= 3.0f * 3.141592f / rad * ((vN) / vVolume - 0.5f);
            }
            else if (dim == 3) {
                vCurvatureFlow -= 16.0f / (3.0f * rad) * ((vN) / vVolume - 0.5f);
            }
            //System.out.println("CURV1: " + vVolume + " " + "COEF OUTSIDE" + " " + vCurvatureFlow + " " + vN );
        }
        else {
            if (aTo == LabelImage.BGLabel) // proper shrinking
            {
                final int vN = vNFrom;
                // This is a point on the contour (innerlist) OR
                // touching the contour (Outer list)
                if (dim == 2) {
                    vCurvatureFlow += 3.0f * 3.141592f / rad * ((vN) / vVolume - 0.5f);
                }
                else if (dim == 3) {
                    vCurvatureFlow += 16.0f / (3.0f * rad) * ((vN) / vVolume - 0.5f);
                }
            }
            else // fighting fronts
            {
                if (dim == 2) {
                    vCurvatureFlow -= 3.0f * 3.141592f / rad * ((vNto) / vVolume - 0.5f);
                    vCurvatureFlow += 3.0f * 3.141592f / rad * ((vNFrom) / vVolume - 0.5f);
                }
                else if (dim == 3) {
                    vCurvatureFlow -= 16.0f / (3.0f * rad) * ((vNto) / vVolume - 0.5f);
                    vCurvatureFlow += 16.0f / (3.0f * rad) * ((vNFrom) / vVolume - 0.5f);
                }
            }
            //System.out.println("CURV2: " + vVolume + " " + "COEF OUTSIDE" + " " + vCurvatureFlow + " " + vNto + " " + vNFrom );
        }
        //System.out.println("CURV: " + vVolume + " " + "COEF OUTSIDE" + " " + vCurvatureFlow);
        return vCurvatureFlow;
    }
}
