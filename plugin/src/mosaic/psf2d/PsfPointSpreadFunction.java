package mosaic.psf2d;



import ij.process.ImageProcessor;

import java.awt.geom.Point2D;

import mosaic.interpolators.BicubicInterpolator;

public class PsfPointSpreadFunction{
    /*	Internal variables	*/
    private float PSF[], Rad[];
    private int rad_max, sample_number, fact;
    private float pixel_size;
    private float mag;
    private PsfSourcePosition source;		// an array Particle, holds all the particles selected
    private ImageProcessor org;			// Image Processor of original image
    private StringBuffer psf_data = new StringBuffer("");

    /**
     * Constructor
     * @param ip ImageProcessor of original image
     * @param pat Position of Point Source
     * @param r Max. radius to calculate PSF for
     * @param s Number of Sample Points on circles
     * @param f Magnification factor
     * @param p User-defined Pixel-Size
     */
    public PsfPointSpreadFunction(ImageProcessor ip, PsfSourcePosition pat, int r, int s, int f, float m, float p){
        this.org = ip;
        this.source = pat;
        this.rad_max = r;
        this.sample_number = s;
        this.fact = f;
        this.mag = m;
        this.pixel_size = p;
    }


    /**
     * Calculates PSF values on concentric circles around particle positions
     * <br>Number of circles is calculated based on maximal radius and magnification factor, i.e.
     * rad_max = 1, fact = 2 would lead to 3 circles with r = 0, r = 0.5, r = 1.
     */
    public void calculatePSF(){
        //Calculate sampling positions
        float x_pos, y_pos;		// Sampling position in x/y-coordinates
        float phi;				// Angle of sampling position in polar-coordinates
        float val_int = 0.0f;	// Interpolated intensity value
        int k;					// Number of circles
        float radius = 0;		// Circle radius

        // Calculate maximal number of circles based on user-defined magnification factor
        k = rad_max * fact;
        PSF = new float[k+1];
        Rad = new float[k+1];
        // Loop over circles
        for (int j=0; j<=k; j++){
            // Calculate circle radius
            radius = (float)j/fact;
            Rad[j] = 1000*radius*pixel_size/mag;
            //TODO
            psf_data.append("\n\n%Radius:\t" + Rad[j] + " nm");
            PSF[j] = 0.0f;
            val_int = 0.0f;
            psf_data.append("\n%Sampling Positions of Point Source:\n");
            // Loop over Sampling Points
            for (int i=0; i<sample_number; i++){
                phi = (float)(2*Math.PI*i)/sample_number;
                x_pos = source.x + radius*(float)Math.cos(phi);
                y_pos = source.y + radius*(float)Math.sin(phi);
                psf_data.append("(" + x_pos + " , " + y_pos + ");");
                //Interpolate intensity values TODO
                BicubicInterpolator bi = new BicubicInterpolator();
                bi.setImageProcessor(org);
                Point2D pnt = new Point2D.Float(x_pos, y_pos);
                val_int = (float)bi.getInterpolatedPixel(pnt);
                PSF[j] += val_int;
            }

            //Calculate PSF on circle
            PSF[j] /= sample_number;
            psf_data.append("\n%Overall Mean Intensity Value on Radius:\t" + PSF[j] + "\n");
        }
        normalizePSF(PSF);
    }


    /**
     * Normalizes PSF values to interval [0,1]
     */
    public void normalizePSF(float[] PSF){
        float max = PSF[0];
        float min = PSF[0];
        float c = 1;
        for (int v=0; v<PSF.length; v++){
            float p = PSF[v];
            if (p>max) {
                max = p;
            }
            if (p<min) {
                min = p;
            }
        }
        c = max - min;
        for (int v=0; v<PSF.length; v++){
            PSF[v] = (PSF[v]-min)/c;
        }
    }


    /**
     * Returns Point Spread Function Values as Float Array
     * @return Point Spread Function Values
     */
    public float[] getPSF(){
        return PSF;
    }


    /**
     * Returns Radius on which the PSF was calculated as Float Array
     * @return Radius on which the PSF was calculated
     */
    public float[] getRadius(){
        return Rad;
    }


    /**
     * Returns Results of PSF estimation as StringBuffer
     * @return Results of PSF estimation
     */
    public StringBuffer getPSFreport(){
        return psf_data;
    }
}