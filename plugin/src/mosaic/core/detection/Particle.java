package mosaic.core.detection;


import java.awt.Rectangle;
import java.text.DecimalFormat;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;


/**
 * Defines a particle
 */
public class Particle {

    public float iX, iY, iZ;
    public float m0 = 0;
    public float m1 = 0;
    public float m2 = 0; 
    public float m3 = 0;
    public float m4 = 0;
    public float nonParticleDiscriminationScore = 0;
    
    // detection/linking stuff
    public float distance = -1;
    public boolean special = true; // a flag that is used while detecting and linking particles
    private int frame = 0; // the number of the frame this particle belongs to (can be 0)
    float original_x = 0; // the originally given coordinates - not to be changed
    float original_y = 0;
    float original_z = 0;
    public int[] next; // array that holds in position i the next particle number in frame i
    public float lx, ly, lz; // previous Linking x,y,z
    public float lxa, lya, lza; // accumulation link

    /**
     * Create a particle from another particle
     * @param aParticle Particle
     */
    Particle(Particle aParticle) {
        iX = aParticle.iX;
        iY = aParticle.iY;
        iZ = aParticle.iZ;
        m0 = aParticle.m0;
        m1 = aParticle.m1;
        m2 = aParticle.m2;
        m3 = aParticle.m3;
        m4 = aParticle.m4;
        frame = aParticle.frame;
    }

    /**
     * @param aX - original x coordinates
     * @param aY - original y coordinates
     * @param aZ - original z coordinates
     * @param aFrameNumber - the number of the frame this particle belongs to
     * @param aLinkRange linking range
     */
    public Particle(float aX, float aY, float aZ, int aFrameNumber, int aLinkRange) {
        iX = aX;
        original_x = aX;
        iY = aY;
        original_y = aY;
        iZ = aZ;
        original_z = aZ;
        
        frame = aFrameNumber;
        next = new int[aLinkRange];
    }
    
    /**
     * Create a default particle (needed currently by CSV, accessed via reflection)
     */
    public Particle() {}

    /**
     * @return the square of the accumulated linking vector
     */
    public float linkModuleASq() {
        return lxa * lxa + lya * lya + lza * lza;
    }

    /**
     * @param rectangle of the focus area
     */
    void translate(Rectangle focus) {
        iX = iX - focus.x;
        iY = iY - focus.y;
    }
    
    /**
     * @return the module of the linking vector
     */
    public float linkModule() {
        return (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
    }

    /**
     * Set particle link range
     * @param aLinkRange
     */
    void setLinkRange(int linkrange) {
        next = new int[linkrange];
    }

    @Override
    public String toString() {
        return toStringBuffer().toString();
    }

    /**
     * @return StringBuffer with this information about particle (x, y, z, m0...)
     */
    public StringBuffer toStringBuffer() {
        final StringBuffer result = new StringBuffer();
        final String space = " ";
        
        final DecimalFormat nf = new DecimalFormat("######0.000000");
        nf.setGroupingUsed(false);
        
        result.append(getFrame());
        result.append(space);
        result.append(nf.format(iX));
        result.append(space);
        result.append(nf.format(iY));
        result.append(space);
        result.append(nf.format(iZ));
        result.append(space);
        result.append(nf.format(m0));
        result.append(space);
        result.append(nf.format(m1));
        result.append(space);
        result.append(nf.format(m2));
        result.append(space);
        result.append(nf.format(m3));
        result.append(space);
        result.append(nf.format(m4));
        result.append(space);
        result.append(nf.format(nonParticleDiscriminationScore));
        result.append("\n");

        return result;
    }

    public boolean match(Particle aParticle) {
        if (iX == aParticle.iX && 
            iY == aParticle.iY && 
            iZ == aParticle.iZ && 
            m0 == aParticle.m0 && 
            m1 == aParticle.m1 && 
            m2 == aParticle.m2 && 
            m3 == aParticle.m3 && 
            m4 == aParticle.m4) 
        {
            return true;
        }

        return false;
    }

    // ------------------------------------------------------------------------
    // CSV definitions and setters/getters used by Particle itself
    public static final String[] ParticleDetection_map = new String[] { "Frame", "x", "y", "z", "Size" };
    public final static CellProcessor[] ParticleDetectionCellProcessor = new CellProcessor[] { new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble() };
    
    public void setFrame(int aFrameNumber) {
        this.frame = aFrameNumber;
    }
    public int getFrame() {
        return frame;
    }

    public void setx(double aX) {
        iX = (float) aX;
    }
    public double getx() {
        return iX;
    }

    public void sety(double aY) {
        iY = (float) aY;
    }
    public double gety() {
        return iY;
    }
    
    public void setz(double aZ) {
        iZ = (float) aZ;
    }
    public double getz() {
        return iZ;
    }
    
    public void setSize(double aSize) {
        m0 = (float) aSize;
    }
    public double getSize() {
        return m0;
    }
    
    // ------------------------------------------------------------------------
    // CSV definitions and setters/getters used when reading Squassh output 
    // TODO: this is terrible solution, it should be handled differently 
    public void setImage_ID(int aImageId) {
        frame = aImageId;
    }
    public int getImage_ID() {
        return frame;
    }
    
    public void setIntensity(double aIntensity) {
        m2 = (float) aIntensity;
    }
    public double getIntensity() {
        return m2;
    }

    public void setCoord_X(double aX) {
        iX = (float) aX;
    }
    public double getCoord_X() {
        return iX;
    }

    public void setCoord_Y(double aY) {
        iY = (float) aY;
    }
    public double getCoord_Y() {
        return iY;
    }

    public void setCoord_Z(double aZ) {
        iZ = (float) aZ;
    }
    public double getCoord_Z() {
        return iZ;
    }
}
