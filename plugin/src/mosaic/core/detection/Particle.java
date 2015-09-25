package mosaic.core.detection;


import java.awt.Rectangle;
import java.text.DecimalFormat;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;


/**
 * Defines a particle that holds all the relevant info for it.
 * A particle is detected in an image or given as input in test file mode
 * X and Y coordinates are not in the usual graph coordinates sense but in the image sense;
 * (0,0) is the upper left corner
 * x is vertical top to bottom
 * y is horizontal left to right
 */

public class Particle {

    public static final String[] ParticleDetection_map = new String[] { "Frame", "x", "y", "z", "Size" };

    public static CellProcessor[] ParticleDetectionCellProcessor = new CellProcessor[] { new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble() };

    public float x, y, z; // the originally given coordinates - to be refined
    public float original_x; // the originally given coordinates - not to be changed
    public float original_y, original_z;
    private int frame; // the number of the frame this particle belonges to (can be 0)
    public boolean special; // a flag that is used while detecting and linking particles
    public int[] next; // array that holds in position i the next particle number in frame i
    // that this particle is linked to
    public int nbIterations = 0; // debug
    /* only relevant to particles detected in images */
    public float m0; // intensity moment
    public float m1, m2, m3, m4;
    public float score; // non-particle discrimination score
    public float distance;
    public float lx, ly, lz; // previous Linking x,y,z
    public float lxa, lya, lza; // accumulation link

    /* only relevant to particles given as input */
    public String[] all_params; // all params that relate to this particle,

    /**
     * Create a particle from another particle
     *
     * @param p Particle
     */

    Particle(Particle p) {
        this.frame = p.frame;
        this.x = p.x;
        this.y = p.y;
        this.z = p.z;
        this.m0 = p.m0;
        this.m1 = p.m1;
        this.m2 = p.m2;
        this.m3 = p.m3;
        this.m4 = p.m4;
    }

    /**
     * Set the particle data
     *
     * @param p Particle
     */

    public void setData(Particle p) {
        this.frame = p.frame;
        this.x = p.x;
        this.y = p.y;
        this.z = p.z;
        this.m0 = p.m0;
        this.m1 = p.m1;
        this.m2 = p.m2;
        this.m3 = p.m3;
        this.m4 = p.m4;
    }

    /**
     * translate coordinate according to a focus area
     *
     * @param rectangle of the focus area
     */
    void translate(Rectangle focus) {
        x = x - focus.x;
        y = y - focus.y;
    }

    /**
     * Get the module of the linking vector
     *
     * @return the module of the linking vector
     */

    public float linkModule() {
        return (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
    }

    /**
     * Get the square of the accumulated linking vector
     *
     * @return the square of the accumulated linking vector
     */

    public float linkModuleASq() {
        return lxa * lxa + lya * lya + lza * lza;
    }

    /**
     * Create a particle
     */

    public Particle() {
        this.special = true;

        distance = -1.0f;
    }

    /**
     * constructor.
     * 
     * @param x - original x coordinates
     * @param y - original y coordinates
     * @param frame_num - the number of the frame this particle belonges to
     * @param aLinkRange linking range
     */
    public Particle(float x, float y, float z, int frame_num, int linkrange) {
        this.x = x;
        this.original_x = x;
        this.y = y;
        this.original_y = y;
        this.z = z;
        this.original_z = z;
        this.special = true;
        this.setFrame(frame_num);
        this.next = new int[linkrange];
        distance = -1.0f;
    }

    /**
     * Set particle link range
     *
     * @param aLinkRange
     */

    void setLinkRange(int linkrange) {
        this.next = new int[linkrange];
    }

    /**
     * constructor for particles created from text files.
     * 
     * @param x - original x coordinates
     * @param y - original y coordinates
     * @param frame_num - the number of the frame this particle is in
     * @param params - all params that relate to this particle, first 2 should be x and y respectfully
     */
    Particle(float x, float y, float z, int frame_num, String[] params, int linkrange) {
        this.x = x;
        this.original_x = x;
        this.y = y;
        this.original_y = y;
        this.z = z;
        this.original_z = z;
        this.all_params = params;
        this.special = true;
        this.setFrame(frame_num);
        this.next = new int[linkrange];
        this.score = 0.0F;
        this.m0 = 0.0F;
        this.m1 = 0.0F;
        this.m2 = 0.0F;
        this.m3 = 0.0F;
        this.m4 = 0.0F;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return toStringBuffer().toString();
    }

    /**
     * The method <code>toString()</code> calls this method <br>
     * Generates (in real time) a "ready to print" <code>StringBuffer</code> with information
     * about this Particle:
     * <ul>
     * <li>frame
     * <li>x
     * <li>y
     * <li>m0
     * <li>m2
     * <li>score
     * </ul>
     * For text files mode - just prints all the information given for the particles
     * 
     * @return a StringBuffer with this infomation
     */
    public StringBuffer toStringBuffer() {

        // I work with StringBuffer since its faster than String
        // At the end convert to String and return
        final StringBuffer sb = new StringBuffer();
        final StringBuffer sp = new StringBuffer(" ");

        // format the number to look nice in print (same number of digits)
        // NumberFormat nf = NumberFormat.getInstance();
        // nf.setMaximumFractionDigits(6);
        // nf.setMinimumFractionDigits(6);
        final DecimalFormat nf = new DecimalFormat("######0.000000");
        nf.setGroupingUsed(false);
        sb.append(this.getFrame());

        sb.append(sp);
        sb.append(nf.format(this.x));
        sb.append(sp);
        sb.append(nf.format(this.y));
        sb.append(sp);
        sb.append(nf.format(this.z));
        sb.append(sp);
        sb.append(nf.format(this.m0));
        sb.append(sp);
        sb.append(nf.format(this.m1));
        sb.append(sp);
        sb.append(nf.format(this.m2));
        sb.append(sp);
        sb.append(nf.format(this.m3));
        sb.append(sp);
        sb.append(nf.format(this.m4));
        sb.append(sp);
        sb.append(nf.format(this.score));
        sb.append("\n");

        return sb;
    }

    boolean match(Particle p) {
        if (this.x == p.x && this.y == p.y && this.z == p.z && this.m0 == p.m0 && this.m1 == p.m1 && this.m2 == p.m2 && this.m3 == p.m3 && this.m4 == p.m4) {
            return true;
        }

        return false;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public int getFrame() {
        return frame;
    }

    public double[] getPosition() {
        final double[] result = { x, y, z };
        return result;
    }

    public void setImage_ID(int Image_ID_) {
        frame = Image_ID_;
    }

    public void setSize(double Size_) {
        m0 = (float) Size_;
    }

    public void setIntensity(double Intensity_) {
        m2 = (float) Intensity_;
    }

    public void setx(double Coord_X_) // NO_UCD (unused code)
    {
        x = (float) Coord_X_;
    }

    public void sety(double Coord_Y_) // NO_UCD (unused code)
    {
        y = (float) Coord_Y_;
    }

    public void setz(double Coord_Z_) // NO_UCD (unused code)
    {
        z = (float) Coord_Z_;
    }

    public void setCoord_X(double Coord_X_) {
        x = (float) Coord_X_;
    }

    public void setCoord_Y(double Coord_Y_) {
        y = (float) Coord_Y_;
    }

    public void setCoord_Z(double Coord_Z_) {
        z = (float) Coord_Z_;
    }

    public double getCoord_X() {
        return x;
    }

    public double getCoord_Y() {
        return y;
    }

    public double getCoord_Z() {
        return z;
    }

    public double getSize() {
        return m0;
    }

    public double getIntensity() {
        return m2;
    }

    public double getx() {
        return x;
    }

    public double gety() {
        return y;
    }

    public double getz() {
        return z;
    }
}
