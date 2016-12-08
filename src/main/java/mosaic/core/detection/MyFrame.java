package mosaic.core.detection;


import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ij.IJ;
import ij.measure.Calibration;
import mosaic.core.imageUtils.MaskOnSpaceMapper;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.masks.SphereMask;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.MosaicUtils.ToARGB;
import mosaic.particleTracker.Trajectory;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;


/**
 * Defines a MyFrame that is based upon an ImageProcessor or information from a text file.
 */
public class MyFrame {

    private Vector<Particle> iParticles;
    public int iFrameNumber;
    
    private StringBuffer iParticleInfoBeforeDiscrimination;
    private int iRadius = -1;

    /**
     * Constructor for image mode.
     */
    public MyFrame(int aFrameNumber) {
        iFrameNumber = aFrameNumber;
    }

    /**
     * Constructor for text mode from a vector of particles
     *
     * @param aParticles - particles in the frames
     * @param aFrameNumber - frame number
     */
    public MyFrame(Vector<Particle> aParticles, int aFrameNumber) {
        iFrameNumber = aFrameNumber;
        iParticles = aParticles;
    }

    /**
     * Constructor for text mode - Loads particles from text file
     */
    public MyFrame(String aTextFilePath) {
        BufferedReader r = null;

        try {
            r = new BufferedReader(new FileReader(aTextFilePath));
            loadParticlesFromFile(r, aTextFilePath);
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (r != null) r.close();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setParticleRadius(int aRadius) {
        iRadius = aRadius;
    }

    /**
     * Generates a "ready to print" string with all the
     * particles positions AFTER discrimination in this frame.
     *
     * @return a <code>StringBuffer</code> with the info
     */
    private StringBuffer getFrameInfoAfterDiscrimination() {

        final DecimalFormat nf = new DecimalFormat("#####0.000000");
        nf.setGroupingUsed(false);
        final StringBuffer info = new StringBuffer("%\tParticles after non-particle discrimination (");
        info.append(this.iParticles.size());
        info.append(" particles):\n");
        for (int i = 0; i < this.iParticles.size(); i++) {
            info.append("%\t\t");
            info.append(nf.format(this.iParticles.elementAt(i).iX));
            info.append(" ");
            info.append(nf.format(this.iParticles.elementAt(i).iY));
            info.append(" ");
            info.append(nf.format(this.iParticles.elementAt(i).iZ));

            info.append("\n");

        }
        return info;
    }

    /**
     * Generates a "ready to print" StringBuffer with all the particles initial
     * and refined positions BEFORE discrimination in this frame. <br>
     * sets <code>info_before_discrimination</code> to hold this info
     *
     * @see #iParticleInfoBeforeDiscrimination
     */
    private void generateFrameInfoBeforeDiscrimination() {
        final DecimalFormat nf = new DecimalFormat("#####0.000000");
        nf.setGroupingUsed(false);

        final StringBuffer info = new StringBuffer("% Frame ");
        info.append(this.iFrameNumber);
        info.append(":\n");
        info.append("%\t");
        info.append(this.iParticles.size());
        info.append(" particles found\n");
        info.append("%\tDetected particle positions:\n");
        for (int i = 0; i < this.iParticles.size(); i++) {
            info.append("%\t\t");
            info.append(nf.format(this.iParticles.elementAt(i).original_x));
            info.append(" ");
            info.append(nf.format(this.iParticles.elementAt(i).original_y));
            info.append(" ");
            info.append(nf.format(this.iParticles.elementAt(i).original_z));
            info.append("\n");
        }
        info.append("%\tParticles after position refinement:\n");
        for (int i = 0; i < this.iParticles.size(); i++) {
            info.append("%\t\t");
            info.append(nf.format(this.iParticles.elementAt(i).iX));
            info.append(" ");
            info.append(nf.format(this.iParticles.elementAt(i).iY));
            info.append(" ");
            info.append(nf.format(this.iParticles.elementAt(i).iZ));

            info.append("\n");
        }
        iParticleInfoBeforeDiscrimination = info;
    }

    /**
     * Generates (in real time) a "ready to print" StringBuffer with this frame
     * information before and after non particles discrimination
     *
     * @return a StringBuffer with the info
     * @see MyFrame#getFrameInfoAfterDiscrimination()
     * @see #iParticleInfoBeforeDiscrimination
     */
    public StringBuffer getFullFrameInfo() {
        final StringBuffer info = new StringBuffer();
        info.append(iParticleInfoBeforeDiscrimination);
        info.append(getFrameInfoAfterDiscrimination());
        return info;
    }

    /**
     * Generates a "ready to print" string that shows for each particle in this
     * frame
     * (AFTER discrimination) all the particles it is linked to.
     *
     * @return a String with the info
     */
    @Override
    public String toString() {
        return toStringBuffer().toString();
    }

    /**
     * The method <code>toString()</code> calls this method <br>
     * Generates a "ready to print" StringBuffer that shows for each particle in
     * this frame
     * (AFTER discrimination) all the particles it is linked to.
     *
     * @return a <code>StringBuffer</code> with the info
     */
    public StringBuffer toStringBuffer() {
        final DecimalFormat nf = new DecimalFormat("#####0.000000");
        nf.setGroupingUsed(false);
        final StringBuffer sb = new StringBuffer("% Frame ");
        sb.append(this.iFrameNumber);
        sb.append("\n");
        for (int j = 0; j < this.iParticles.size(); j++) {
            sb.append("%\tParticle ");
            sb.append(j);
            sb.append(" (");
            sb.append(nf.format(this.iParticles.elementAt(j).iX));
            sb.append(", ");
            sb.append(nf.format(this.iParticles.elementAt(j).iY));
            sb.append(", ");
            sb.append(nf.format(this.iParticles.elementAt(j).iZ));
            sb.append(")\n");
            if (iParticles.elementAt(j).next != null) {
                for (int k = 0; k < iParticles.elementAt(j).next.length; k++) {
                    sb.append("%\t\tlinked to particle ");
                    sb.append(this.iParticles.elementAt(j).next[k]);
                    sb.append(" in frame ");
                    sb.append((this.iFrameNumber + k + 1));
                    sb.append("\n");
                }
            }
            else {
                sb.append("%\t\thas empty link container\n");
            }
        }
        return sb;
    }

    /**
     * @return particles vector
     */
    public Vector<Particle> getParticles() {
        return iParticles;
    }

    /**
     * Generates (in real time) a "ready to save" <code>StringBuffer</code> with
     * information
     * about the detected particles defined in this MyFrame. <br>
     * The format of the returned <code>StringBuffer</code> is the same as
     * expected when
     * loading particles information from text files
     *
     * @param with_momentum if true, the momentum values (m0, m2) are also
     *            included
     *            if false - only x and y values are included
     * @return the <code>StringBuffer</code> with this information
     */
    public StringBuffer frameDetectedParticlesForSave(boolean with_momentum) {
        final DecimalFormat nf = new DecimalFormat("#####0.000000");
        nf.setGroupingUsed(false);
        final StringBuffer info1 = new StringBuffer("frame ");
        info1.append(this.iFrameNumber);
        info1.append("\n");
        for (int i = 0; i < this.iParticles.size(); i++) {
            info1.append(nf.format(this.iParticles.elementAt(i).iX));
            info1.append(" ");
            info1.append(nf.format(this.iParticles.elementAt(i).iY));
            info1.append(" ");
            info1.append(nf.format(this.iParticles.elementAt(i).iZ));
            if (with_momentum) {
                info1.append(" ");
                info1.append(nf.format(this.iParticles.elementAt(i).m0));
                info1.append(" ");
                info1.append(nf.format(this.iParticles.elementAt(i).m2));
            }
            info1.append("\n");
        }
        return info1;
    }

    public void setParticles(Vector<Particle> aParticles) {
        iParticles = aParticles;
        
        for (int i = iParticles.size() - 1; i >= 0; i--) {
            iParticles.elementAt(i).setFrame(iFrameNumber);
        }
        
        generateFrameInfoBeforeDiscrimination();
        removeNonParticle();
    }
    
    /**
     * removes particles that were discarded by the <code>nonParticleDiscrimination</code> method
     * from the particles array. <br>
     * Non particles will be removed from the <code>particles</code> array so if their info is
     * needed, it should be saved before calling this method
     * @return 
     * 
     * @see MyFrame#nonParticleDiscrimination()
     */
    private Vector<Particle> removeNonParticle() {
        for (int i = iParticles.size() - 1; i >= 0; i--) {
            if (!iParticles.elementAt(i).special) {
                iParticles.removeElementAt(i);
            }
        }
        return iParticles;
    }
    
    static private float[] getScaling(int dim, Calibration cal) {
        final float scaling[] = new float[dim];
        final float scaling_[] = new float[dim];

        if (cal != null) {
            scaling[0] = (float) (cal.pixelWidth);
            scaling[1] = (float) (cal.pixelHeight);
            if (scaling.length >= 3) {
                scaling[2] = (float) (cal.pixelDepth);
            }
        }
        else {
            scaling[0] = 1.0f;
            scaling[1] = 1.0f;
            if (scaling.length >= 3) {
                scaling[2] = 1.0f;
            }
        }

        final float min_s = minScaling(scaling);
        scaling_[0] = scaling[0] / min_s;
        scaling_[1] = scaling[1] / min_s;
        if (scaling.length >= 3) {
            scaling_[2] = scaling[2] / min_s;
        }

        return scaling_;
    }

    static private void drawParticlesWithRadius(RandomAccessibleInterval<ARGBType> out, List<Particle> pt, Calibration cal, float scaling, int col, int aRadius) {
        Map<Integer, MaskOnSpaceMapper> CircleCache = new HashMap<Integer, MaskOnSpaceMapper>();
        final RandomAccess<ARGBType> out_a = out.randomAccess();

        final int sz[] = new int[out_a.numDimensions()];
        for (int d = 0; d < out_a.numDimensions(); ++d) {
            sz[d] = (int) out.dimension(d);
        }

        // Iterate on all particles
        final double radius = aRadius;

        final float scaling_[] = getScaling(out_a.numDimensions(), cal);
        for (int i = 0; i < scaling_.length; i++) {
            scaling_[i] /= scaling;
        }

        int rc = (int) radius;

        // Create a circle Mask and an iterator
        MaskOnSpaceMapper rg_m = null;

        if ((rg_m = CircleCache.get(rc)) == null) {
            final SphereMask cm = new SphereMask(rc, (int) (2 * rc * scaling + 1), scaling_);
            rg_m = new MaskOnSpaceMapper(cm, sz);
            CircleCache.put(rc, rg_m);
        }

        final Iterator<Particle> pt_it = pt.iterator();

        while (pt_it.hasNext()) {
            final Particle ptt = pt_it.next();

            // Draw the Circle
            Point p_c = null;
            if (out_a.numDimensions() == 2) {
                p_c = new Point((int) (ptt.iX / scaling_[0]), (int) (ptt.iY / scaling_[1]));
            }
            else {
                p_c = new Point((int) (ptt.iX / scaling_[0]), (int) (ptt.iY / scaling_[1]), (int) (ptt.iZ / scaling_[2]));
            }

            rg_m.setMiddlePoint(p_c);

            while (rg_m.hasNext()) {
                final Point p = rg_m.nextPoint();

                if (p.isInside(sz)) {
                    out_a.setPosition(p.iCoords);
                    out_a.get().set(col);
                }
            }
        }
    }

    private static float minScaling(float s[]) {
        float min = Float.MAX_VALUE;
        for (int i = 0; i < s.length; i++) {
            if (s[i] < min) {
                min = s[i];
            }
        }

        return min;
    }

    static private void drawParticles(RandomAccessibleInterval<ARGBType> out, List<Particle> pt, Calibration cal, float scaling, int col) {
        Map<Integer, MaskOnSpaceMapper> CircleCache = new HashMap<Integer, MaskOnSpaceMapper>();
        final RandomAccess<ARGBType> out_a = out.randomAccess();

        final int sz[] = new int[out_a.numDimensions()];
        for (int d = 0; d < out_a.numDimensions(); ++d) {
            sz[d] = (int) out.dimension(d);
        }

        while (pt.size() != 0) {
            double radius;
            if (out_a.numDimensions() == 2) {
                radius = Math.sqrt(pt.get(0).m0 / Math.PI);
            }
            else {
                radius = Math.cbrt(pt.get(0).m0 * 3.0 / 4.0 / Math.PI);
            }

            if (radius < 1.0) {
                radius = 1.0;
            }

            final float scaling_[] = getScaling(out_a.numDimensions(), cal);
            for (int i = 0; i < scaling_.length; i++) {
                scaling_[i] /= scaling;
            }

            MaskOnSpaceMapper rg_m = null;
            int rc = (int) radius;
            if ((rg_m = CircleCache.get(rc)) == null) {
                if (rc < 1) {
                    rc = 1;
                }
                final SphereMask cm = new SphereMask(rc, (int) (2 * rc * scaling + 1), scaling_);
                rg_m = new MaskOnSpaceMapper(cm, sz);
                CircleCache.put(rc, rg_m);
            }

            final Iterator<Particle> pt_it = pt.iterator();

            while (pt_it.hasNext()) {
                final Particle ptt = pt_it.next();

                double radius_r;
                if (out_a.numDimensions() == 2) {
                    radius_r = Math.sqrt(pt.get(0).m0 / Math.PI);
                }
                else {
                    radius_r = Math.cbrt(pt.get(0).m0 * 3.0 / 4.0 / Math.PI);
                }

                if (radius_r <= 1.0) {
                    radius_r = 1;
                }

                if (radius_r == radius) {
                    // Draw the Circle

                    Point p_c = null;
                    if (out_a.numDimensions() == 2) {
                        p_c = new Point((int) (ptt.iX / scaling_[0]), (int) (ptt.iY / scaling_[1]));
                    }
                    else {
                        p_c = new Point((int) (ptt.iX / scaling_[0]), (int) (ptt.iY / scaling_[1]), (int) (ptt.iZ / scaling_[2]));
                    }
                    rg_m.setMiddlePoint(p_c);

                    while (rg_m.hasNext()) {
                        final Point p = rg_m.nextPoint();

                        if (p.isInside(sz)) {
                            out_a.setPosition(p.iCoords);
                            out_a.get().set(col);
                        }
                    }
                    pt_it.remove();
                }
            }
        }
    }

    /**
     * Bresenham line 3D algorithm
     *
     * @param out Image where to draw
     * @param p1 start point
     * @param p2 end line
     * @param col Color of the line
     */
    static private void drawLine(RandomAccessibleInterval<ARGBType> out, Particle p1, Particle p2, int col) {
        final long dims[] = new long[out.numDimensions()];
        out.dimensions(dims);

        final RandomAccess<ARGBType> out_a = out.randomAccess();

        int i, dx, dy, dz, l, m, n, x_inc, y_inc, z_inc, err_1, err_2, dx2, dy2, dz2;
        final long pixel[] = new long[3];

        pixel[0] = (int) p1.iX;
        pixel[1] = (int) p1.iY;
        pixel[2] = (int) p1.iZ;
        dx = (int) (p2.iX - p1.iX);
        dy = (int) (p2.iY - p1.iY);
        dz = (int) (p2.iZ - p1.iZ);
        x_inc = (dx < 0) ? -1 : 1;
        l = Math.abs(dx);
        y_inc = (dy < 0) ? -1 : 1;
        m = Math.abs(dy);
        z_inc = (dz < 0) ? -1 : 1;
        n = Math.abs(dz);
        dx2 = l << 1;
        dy2 = m << 1;
        dz2 = n << 1;

        if ((l >= m) && (l >= n)) {
            err_1 = dy2 - l;
            err_2 = dz2 - l;
            for (i = 0; i < l; i++) {
                boolean out_pix = false;
                for (int k = 0; k < out_a.numDimensions(); k++) {
                    if (pixel[k] >= dims[k]) {
                        out_pix = true;
                        break;
                    }
                }

                if (out_pix == true) {
                    continue;
                }

                out_a.setPosition(pixel);

                out_a.get().set(col);
                if (err_1 > 0) {
                    pixel[1] += y_inc;
                    err_1 -= dx2;
                }
                if (err_2 > 0) {
                    pixel[2] += z_inc;
                    err_2 -= dx2;
                }
                err_1 += dy2;
                err_2 += dz2;
                pixel[0] += x_inc;
            }
        }
        else if ((m >= l) && (m >= n)) {
            err_1 = dx2 - m;
            err_2 = dz2 - m;
            for (i = 0; i < m; i++) {
                boolean out_pix = false;
                for (int k = 0; k < out_a.numDimensions(); k++) {
                    if (pixel[k] >= dims[k]) {
                        out_pix = true;
                        break;
                    }
                }

                if (out_pix == true) {
                    continue;
                }

                out_a.setPosition(pixel);
                out_a.get().set(col);
                if (err_1 > 0) {
                    pixel[0] += x_inc;
                    err_1 -= dy2;
                }
                if (err_2 > 0) {
                    pixel[2] += z_inc;
                    err_2 -= dy2;
                }
                err_1 += dx2;
                err_2 += dz2;
                pixel[1] += y_inc;
            }
        }
        else {
            err_1 = dy2 - n;
            err_2 = dx2 - n;
            for (i = 0; i < n; i++) {
                boolean out_pix = false;
                for (int k = 0; k < out_a.numDimensions(); k++) {
                    if (pixel[k] >= dims[k]) {
                        out_pix = true;
                        break;
                    }
                }

                if (out_pix == true) {
                    continue;
                }

                out_a.setPosition(pixel);
                out_a.get().set(col);
                if (err_1 > 0) {
                    pixel[1] += y_inc;
                    err_1 -= dz2;
                }
                if (err_2 > 0) {
                    pixel[0] += x_inc;
                    err_2 -= dz2;
                }
                err_1 += dy2;
                err_2 += dx2;
                pixel[2] += z_inc;
            }
        }

        boolean out_pix = false;
        for (int k = 0; k < out_a.numDimensions(); k++) {
            if (pixel[k] >= dims[k]) {
                out_pix = true;
                break;
            }
        }

        if (out_pix == false) {
            out_a.setPosition(pixel);
            out_a.get().set(col);
        }
    }

    /**
     * Draw Lines on a Image
     *
     * @param out Image where to draw
     * @param lines List of lines
     * @param cal calibration
     * @param col Color of the line
     */
    static private void drawLines(RandomAccessibleInterval<ARGBType> out, List<pParticle> lines, Calibration cal, float scale, int col) {
        if (cal == null) {
            cal = new Calibration();
            cal.pixelDepth = 1.0;
            cal.pixelHeight = 1.0;
            cal.pixelWidth = 1.0;
        }

        final RandomAccess<ARGBType> out_a = out.randomAccess();

        final int sz[] = new int[out_a.numDimensions()];

        for (int d = 0; d < out_a.numDimensions(); ++d) {
            sz[d] = (int) out.dimension(d);
        }

        // Iterate on all lines

        for (final pParticle ptt : lines) {
            Particle p_end = new Particle(ptt.p1);
            Particle p_ini = new Particle(ptt.p2);

            final float scaling[] = new float[out_a.numDimensions()];
            final float scaling_[] = getScaling(out_a.numDimensions(), cal);
            for (int i = 0; i < scaling.length; i++) {
                scaling_[i] /= scale;
            }


            p_ini.iX /= scaling_[0];
            p_ini.iY /= scaling_[1];
            if (out_a.numDimensions() > 2) p_ini.iZ /= scaling_[2];

            p_end.iX /= scaling_[0];
            p_end.iY /= scaling_[1];
            if (out_a.numDimensions() > 2) p_end.iZ /= scaling_[2];


            drawLine(out, p_ini, p_end, col);

            final double radius = Math.cbrt(ptt.p1.m0 / 3.0f * 4.0f);

            final int rc = (int) radius;

            // draw several lines on z

            for (int i = 1; i <= rc; i++) {
                if (ptt.p1.iZ / (float) cal.pixelDepth - i >= 0 && ptt.p2.iZ / (float) cal.pixelDepth - i >= 0) {
                    p_end = new Particle(ptt.p1);
                    p_ini = new Particle(ptt.p2);

                    p_ini.iX /= scaling_[0];
                    p_ini.iY /= scaling_[1];
                    if (out_a.numDimensions() > 2) p_ini.iZ = p_ini.iZ / scaling[2] - i;

                    p_end.iX /= scaling_[0];
                    p_end.iY /= scaling_[1];
                    if (out_a.numDimensions() > 2) p_end.iZ = p_end.iZ / scaling_[2] - i;

                    drawLine(out, p_ini, p_end, col);
                }

                if (ptt.p2.iX / (float) cal.pixelDepth + i < out.dimension(out.numDimensions() - 1) && ptt.p1.iZ / (float) cal.pixelDepth + i < out.dimension(out.numDimensions() - 1)) {
                    p_end = new Particle(ptt.p1);
                    p_ini = new Particle(ptt.p2);

                    p_ini.iX /= scaling_[0];
                    p_ini.iY /= scaling_[1];
                    if (out_a.numDimensions() > 2) p_ini.iZ = p_ini.iZ / scaling_[2] + i;

                    p_end.iX /= scaling_[0];
                    p_end.iY /= scaling_[1];
                    if (out_a.numDimensions() > 2) p_end.iZ = p_end.iZ / scaling_[2] + i;

                    drawLine(out, p_ini, p_end, col);
                }
            }
        }
    }

    /**
     * Draw particles on out image
     *
     * @param out Out image
     * @param cal Calibration (scaling factor between particle position and the
     *            image pixel)
     * @param col Color
     */
    private void drawParticles(Img<ARGBType> out, Calibration cal, float scale, int col) {
        // Create a list of particles
        final List<Particle> pt = new ArrayList<Particle>();

        for (int i = 0; i < iParticles.size(); i++) {
            pt.add(iParticles.get(i));
        }

        if (iRadius == -1) {
            drawParticles(out, pt, cal, scale, col);
        }
        else {
            drawParticlesWithRadius(out, pt, cal, scale, col, iRadius);
        }
    }

    /**
     * Create an image from particle information with background
     *
     * @param background background image
     * @param cal calibration
     * @return image video
     */
    public <T extends RealType<T>> Img<ARGBType> createImage(Img<T> background, Calibration cal) {
        // Check that the image is really RealType
        try {
            background.firstElement();
        }
        catch (final ClassCastException e) {
            IJ.error("Error unsupported format, please convert your image into 8-bit, 16-bit or float");
            return null;
        }

        // the number of dimensions
        final int numDimensions = background.numDimensions();

        final long dims[] = new long[numDimensions];
        background.dimensions(dims);

        // Create image
        final ImgFactory<ARGBType> imgFactory = new ArrayImgFactory<ARGBType>();
        final Img<ARGBType> out = imgFactory.create(dims, new ARGBType());

        final Cursor<ARGBType> curOut = out.cursor();
        final Cursor<T> curBack = background.cursor();

        ToARGB conv = null;
        try {
            conv = MosaicUtils.getConversion(curBack.get(), background.cursor());
        }
        catch (final ClassCastException e) {
            IJ.error("Error unsupported format, please convert your image into 8-bit, 16-bit or float");
            return null;
        }

        // Copy the background
        while (curBack.hasNext()) {
            curOut.fwd();
            curBack.fwd();

            curOut.get().set(conv.toARGB(curBack.get()));
        }

        drawParticles(out, cal, 1.0f, ARGBType.rgba(255, 0, 0, 255));

        return out;
    }

    private static class pParticle {
        Particle p1;
        Particle p2;

        pParticle(Particle p1, Particle p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        void translate(Rectangle focus) {
            p1.translate(focus);
            p2.translate(focus);
        }
    }

    /**
     * Draw the trajectories
     *
     * @param out Where to draw (It is suppose to be a video, the last dimension
     *            is considered the time flow)
     * @param nframe Number of frames
     * @param tr Vector that store all trajectories
     * @param start_frame when focus area is active it specify from where the
     *            focus video start
     * @param focus boundary of the focus area (can be null);
     * @param cal_ Calibration basically the image spacing
     * @param aRadius when != -1 the radius of the particles is fixed
     * @param typ type of draw
     */
    private static void TrajectoriesDraw(RandomAccessibleInterval<ARGBType> out, int nframe, Vector<Trajectory> tr, int start_frame, Rectangle focus, Calibration cal_, float scaling, DrawType typ,
            int aRadius) {

        // Particles
        final Vector<Particle> vp = new Vector<Particle>();
        // Lines that indicate the trajectory
        final Vector<pParticle> lines = new Vector<pParticle>();
        // Lines that indicate trajectory jumps
        final Vector<pParticle> lines_jmp = new Vector<pParticle>();

        for (int frame = 0; frame < nframe; frame++) {
            // For all the trajectory

            for (int t = 0; t < tr.size(); t++) {
                // If we have to display the trajectory
                if (tr.get(t).toDisplay() == false) {
                    continue;
                }
                if (!tr.get(t).drawParticle()) aRadius = 0;
                
                vp.clear();
                lines.clear();
                lines_jmp.clear();

                if (frame + start_frame >= tr.get(t).getStartFrame() && frame + start_frame <= tr.get(t).getStopFrame()) {
                    // select the nearest particle to the frame
                    int j = 0;

                    for (j = 0; j < tr.get(t).iParticles.length; j++) {
                        if (tr.get(t).iParticles[j].getFrame() <= frame + start_frame
                                && (j + 1 >= tr.get(t).iParticles.length || tr.get(t).iParticles[j + 1].getFrame() > frame + start_frame)) {
                            break;
                        }
                    }

                    // Particle to draw

                    final Particle p = new Particle(tr.get(t).iParticles[j]);
                    if (focus != null) {
                        p.translate(focus);
                    }
                    vp.add(p);

                    // Collect spline to draw (Carefully only TRAJECTORY_HISTORY
                    // is tested)

                    if (typ == DrawType.NEXT) {
                        if (j + 1 < tr.get(t).iParticles.length) {
                            crateNewLine(tr, focus, lines, lines_jmp, t, j);
                        }
                    }
                    else if (typ == DrawType.PREV) {
                        if (j - 1 >= 0) {
                            createNewLine2(tr, focus, lines, lines_jmp, t, j);
                        }
                    }
                    else if (typ == DrawType.PREV_NEXT) {
                        if (j + 1 < tr.get(t).iParticles.length) {
                            crateNewLine(tr, focus, lines, lines_jmp, t, j);
                        }
                        if (j - 1 >= 0) {
                            createNewLine2(tr, focus, lines, lines_jmp, t, j);
                        }
                    }
                    else if (typ == DrawType.TRAJECTORY_HISTORY) {
                        // draw the full trajectory history, collect all the
                        // lines from j to the start of the trajectory
                        for (int i = j; i >= 1; i--) {
                            createNewLine3(tr, focus, lines, lines_jmp, t, i);
                        }
                    }
                    else if (typ == DrawType.TRAJECTORY_HISTORY_WITH_NEXT) {
                        for (int i = j + 1; i >= 1; i--) {
                            createNewLine3(tr, focus, lines, lines_jmp, t, i);
                        }
                        if (j + 1 < tr.get(t).iParticles.length) {
                            crateNewLine(tr, focus, lines, lines_jmp, t, j);
                        }
                    }
                }

                RandomAccessibleInterval<ARGBType> view = null;

                if (nframe != 1) {
                    view = Views.hyperSlice(out, out.numDimensions() - 1, frame);
                }
                else {
                    view = out;
                }

                if (aRadius == -1) {
                    drawParticles(view, vp, cal_, scaling, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));
                }
                else {
                    drawParticlesWithRadius(view, vp, cal_, scaling, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()),
                            aRadius);
                }

                // Real link
                drawLines(view, lines, cal_, scaling, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));

                // Jump link
                drawLines(view, lines_jmp, cal_, scaling, ARGBType.rgba(255, 0.0, 0.0, 0.0));
            }
        }
    }

    private static void createNewLine3(Vector<Trajectory> tr, Rectangle focus, final Vector<pParticle> lines, final Vector<pParticle> lines_jmp, int t, int j) {
        final pParticle l1 = new pParticle(new Particle(tr.get(t).iParticles[j]), new Particle(tr.get(t).iParticles[j - 1]));
        if (focus != null) {
            l1.translate(focus);
        }

        // Check if it is a jump
        final boolean jump = (tr.get(t).iParticles[j].getFrame() - tr.get(t).iParticles[j - 1].getFrame() != 1);

        if (jump == false) {
            lines.add(l1);
        }
        else {
            lines_jmp.add(l1);
        }
    }

    private static void createNewLine2(Vector<Trajectory> tr, Rectangle focus, final Vector<pParticle> lines, final Vector<pParticle> lines_jmp, int t, int j) {
        final pParticle l1 = new pParticle(new Particle(tr.get(t).iParticles[j]), new Particle(tr.get(t).iParticles[j + 1]));
        if (focus != null) {
            l1.translate(focus);
        }

        // Check if it is a jump
        final boolean jump = (tr.get(t).iParticles[j].getFrame() - tr.get(t).iParticles[j - 1].getFrame() != 1);

        if (jump == false) {
            lines.add(l1);
        }
        else {
            lines_jmp.add(l1);
        }
    }

    private static void crateNewLine(Vector<Trajectory> tr, Rectangle focus, final Vector<pParticle> lines, final Vector<pParticle> lines_jmp, int t, int j) {
        final pParticle l1 = new pParticle(new Particle(tr.get(t).iParticles[j]), new Particle(tr.get(t).iParticles[j + 1]));
        if (focus != null) {
            l1.translate(focus);
        }

        // Check if it is a jump
        final boolean jump = (tr.get(t).iParticles[j + 1].getFrame() - tr.get(t).iParticles[j].getFrame() != 1);

        if (jump == false) {
            lines.add(l1);
        }
        else {
            lines_jmp.add(l1);
        }
    }

    /**
     * Update the image
     *
     * @param out An array of frames
     * @param focus A focus area
     * @param start_frame of the focus view
     * @param a Vector of trajectories
     * @param Type of draw
     */
    static public void updateImage(RandomAccessibleInterval<ARGBType> out, Rectangle focus, int start_frame, Vector<Trajectory> tr, Calibration cal, DrawType typ, int aRadius) {
        // Adjust calibration according to magnification
        final int scale_x = (int) (out.dimension(0) / focus.width);
        final int nframe = (int) out.dimension(out.numDimensions() - 1);

        // Draw trajectories
        TrajectoriesDraw(out, nframe, tr, start_frame, focus, cal, scale_x, typ, aRadius);
    }

    /**
     * Update the image
     *
     * @param out An array of frames in ImgLib2 format (last dimension is frame)
     * @param A vector of Trajectory
     * @param cal Calibration
     * @param Type of draw
     * @param when p_radius != -1 the radius of the particles is fixed
     */
    static public void updateImage(Img<ARGBType> out, Vector<Trajectory> tr, Calibration cal, DrawType typ, int p_radius) {
        for (int i = 0; i < tr.size(); i++) {
            final int nframe = (int) out.dimension(out.numDimensions() - 1);

            // Collect particles to draw and spline to draw
            TrajectoriesDraw(out, nframe, tr, 0, null, cal, 1.0f, typ, p_radius);
        }
    }

    /**
     * Create an image from particle information with background and trajectory
     * information
     *
     * @param background background image
     * @param tr Trajectory information
     * @param cal calibration
     * @param frame number
     * @param Type of draw
     * @return image video
     */
    public <T extends RealType<T>> Img<ARGBType> createImage(Img<T> background, Vector<Trajectory> tr, Calibration cal, int frame, DrawType typ) {
        // if you have no trajectory draw use the other function

        if (tr == null) {
            return createImage(background, cal);
        }

        // the number of dimensions
        final int numDimensions = background.numDimensions();

        final long dims[] = new long[numDimensions];
        background.dimensions(dims);

        // Create image

        final ImgFactory<ARGBType> imgFactory = new ArrayImgFactory<ARGBType>();
        final Img<ARGBType> out = imgFactory.create(dims, new ARGBType());

        final Cursor<ARGBType> curOut = out.cursor();
        final Cursor<T> curBack = background.cursor();

        ToARGB conv = null;
        try {
            conv = MosaicUtils.getConversion(curBack.get(), background.cursor());
        }
        catch (final ClassCastException e) {
            IJ.error("Error unsupported format, please convert your image into 8-bit, 16-bit or float");
            return null;
        }

        // Copy the background
        while (curBack.hasNext()) {
            curOut.fwd();
            curBack.fwd();
            curOut.get().set(conv.toARGB(curBack.get()));
        }

        // Collect particles to draw and spline to draw
        TrajectoriesDraw(out, 1, tr, frame, null, cal, 1.0f, typ, iRadius);

        return out;
    }

    
// =================================== DRAWING STUFF =======================================
    public enum DrawType {
        TRAJECTORY_HISTORY, PREV, NEXT, PREV_NEXT, TRAJECTORY_HISTORY_WITH_NEXT
    }
    
    /**
     * Create an image from particle or provided trajectories
     * @return image
     */
    public Img<ARGBType> createImage(int aImgDimensions[], Vector<Trajectory> aTrajectories, int aFrameNumber, DrawType aType) {
        final Img<ARGBType> out = new ArrayImgFactory<ARGBType>().create(aImgDimensions, new ARGBType());

        if (aTrajectories == null) {
            drawParticles(out, null, 1.0f, ARGBType.rgba(255, 0, 0, 255));
        }
        else {
            TrajectoriesDraw(out, 1, aTrajectories, aFrameNumber, null, null, 1.0f, aType, iRadius);
        }
        
        return out;
    }
    
// ================================== CLEANED UP ================================================
    
    /**
     * Remove duplicated particles.
     */
    public void removeDuplicatedParticles() {
        for (int i = iParticles.size() - 1; i > 0; --i) {
            for (int j = i - 1; j >= 0; --j) {
                if (iParticles.get(i).match(iParticles.get(j))) {
                    iParticles.remove(i);
                    break;
                }
            }
        }
    }
    
    /**
     * Reads particle info from text data.
     * @param aInputStream - opened input stream with data
     * @param aInputInfo - path to read file (or other source used only for logs/errors strings)
     * @return true on success
     * @throws IOException
     */
    private boolean loadParticlesFromFile(BufferedReader aInputStream, String aInputInfo) throws IOException {
        // ----- First line - frame number with format "frame DECIMAL_NUMBER"
        String line = aInputStream.readLine();
        line = line.trim();
        if (line == null || !line.startsWith("frame")) {
            IJ.error("File [" + aInputInfo + "] doesn't have the string 'frame' in the begining of the first line!");
            return false;
        }
        String[] frameNumberInfo = line.split("\\s+");
        if (frameNumberInfo.length < 2 || frameNumberInfo[1] == null) {
            IJ.error("Malformed line, expacting \"frame x\", founded [" + line + "]");
            return false;
        }
        
        iFrameNumber = Integer.parseInt(frameNumberInfo[1]);

        // ----- Read rest of lines
        final Vector<String[]> particlesInfo = new Vector<String[]>();
        while ((line = aInputStream.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("%")) {
                line = line.substring(1);
            }
            line = line.trim();
            particlesInfo.addElement(line.split("\\s+"));
        }

        // ----- Process read lines and create particles
        iParticles = new Vector<Particle>();
        for (String[] pInfo : particlesInfo) {
            if (pInfo.length < 2) {
                IJ.error("Malformed line, expacting 2 floats but got " + pInfo.length);
                return false;
            }
            
            float x = Float.parseFloat(pInfo[0]);
            float y = Float.parseFloat(pInfo[1]);
            float z = (pInfo.length == 2) ? 0.0f : Float.parseFloat(pInfo[2]);
            Particle p = new Particle(x, y, z, iFrameNumber);

            if (pInfo.length >= 8 && pInfo[3] != null && pInfo[4] != null && pInfo[5] != null && pInfo[6] != null && pInfo[7] != null) {
                p.m0 = Float.parseFloat(pInfo[3]);
                p.m1 = Float.parseFloat(pInfo[4]);
                p.m2 = Float.parseFloat(pInfo[5]);
                p.m3 = Float.parseFloat(pInfo[6]);
                p.m4 = Float.parseFloat(pInfo[7]);
            }
            iParticles.add(p);
        }
        
        return true;
    }
}
