package mosaic.psf2d;


import ij.process.ImageProcessor;
import mosaic.utils.ArrayOps;
import mosaic.utils.ConvertArray;
import mosaic.utils.math.Interpolation;


/**
 * Calculates PSF values around provided middle point.
 * 
 * TODO: This class is doing too much... It should only sample things but it is also responsible for creating report.
 *       It should also take as a input 2D array to be more versatile.
 */
public class PsfSampler {
    private float iPsf[]; 
    private float iRadius[];
    private final StringBuffer iPsfData = new StringBuffer("");

    /**
     * Calculates PSF values on concentric circles around given coordinates.
     * Number of circles is calculated based on maximal radius and magnification factor, i.e.
     * rad_max = 1, fact = 2 would lead to 3 circles with r = 0, r = 0.5, r = 1.
     *
     * @param aInputImg ImageProcessor of original image
     * @param aPsfCoords Position of Point Source
     * @param aMaxRadius Max. radius to calculate PSF for
     * @param aNumOfSamples Number of Sample Points on circles
     * @param aMagnificationFactor Magnification factor
     * @param aMagnification Magnification of input image
     * @param aPixelSize User-defined Pixel-Size
     */
    public PsfSampler(ImageProcessor aInputImg, PsfSourcePosition aPsfCoords, int aMaxRadius, int aNumOfSamples, int aMagnificationFactor, float aMagnification, float aPixelSize) {
        ImageProcessor fp = aInputImg.convertToFloat();
        double[][] img2d = ConvertArray.toDouble2D(ConvertArray.toDouble((float[])fp.getPixels()), fp.getWidth(), fp.getHeight());

        // Calculate maximal number of circles based on user-defined magnification factor (+1 for middle point with radius = 0).
        int numOfCircles = aMaxRadius * aMagnificationFactor + 1;
        iPsf = new float[numOfCircles];
        iRadius = new float[numOfCircles];

        for (int j = 0; j < numOfCircles; j++) {
            float currentRadius = (float) j / aMagnificationFactor;

            iPsf[j] = 0.0f;
            for (int i = 0; i < aNumOfSamples; i++) {
                double angleOfSample = ((double)i / aNumOfSamples) * (2 * Math.PI);
                float xPosOfSample = aPsfCoords.iX + currentRadius * (float) Math.cos(angleOfSample);
                float yPosOfSample = aPsfCoords.iY + currentRadius * (float) Math.sin(angleOfSample);
                iPsfData.append("(" + xPosOfSample + ", " + yPosOfSample + "); ");

                iPsf[j] += Interpolation.bicubicInterpolation(xPosOfSample, yPosOfSample, img2d, Interpolation.InterpolationMode.NONE);
            }
            iPsf[j] /= aNumOfSamples;

            // Produce output info
            iRadius[j] = 1000 * currentRadius * aPixelSize / aMagnification;
            iPsfData.append("\n\n%Radius:\t" + iRadius[j] + " nm");
            iPsfData.append("\n%Sampling Positions of Point Source:\n");
            iPsfData.append("\n%Overall Mean Intensity Value on Radius:\t" + iPsf[j] + "\n");
        }
        ArrayOps.normalize(iPsf);
    }

    /**
     * @return Returns PSF values
     */
    public float[] getPsf() {
        return iPsf;
    }

    /**
     * @return Returns Radius on which the PSF was calculated
     */
    public float[] getRadius() {
        return iRadius;
    }

    /**
     * @return Returns Results of PSF estimation
     */
    public StringBuffer getPsfReport() {
        return iPsfData;
    }
}
