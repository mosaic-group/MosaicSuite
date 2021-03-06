package mosaic.plugins;


import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import mosaic.bregman.segmentation.Region;
import mosaic.core.imageUtils.MaskOnSpaceMapper;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.SpaceIterator;
import mosaic.core.imageUtils.masks.BallMask;
import mosaic.core.psf.psf;
import mosaic.core.psf.psfList;
import mosaic.utils.Debug;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


/**
 * Plugins to produce regions with ground truth
 *
 * @author Pietro Incardona
 */
public class RegionCreator implements PlugInFilter // NO_UCD
{

    private int N_region;
    private int Max_radius;
    private int Min_radius;
    private int Max_intensity;
    private int Min_intensity;
    private long Image_sz[];
    private float Spacing[];
    protected psf<FloatType> cPSF;
    private double Background;
    protected String conv;
    protected Choice cConv;
    private String imageT;
    private final String[] ImageType = { "8-bit", "16-bit", "float" };
    private HashMap<Integer, BallMask> map;

    /**
     * Draw a Sphere with radius on out
     *
     * @param out Image
     * @param pt point
     * @param cal spacing
     * @param intensity of your region
     * @param p_radius radius of your region
     */
    private <S extends NumericType<S>> int drawSphereWithRadius(RandomAccessibleInterval<S> out, Point pt, float[] cal, S intensity, int p_radius) {
        if (cal == null) {
            cal = new float[out.numDimensions()];
            for (int i = 0; i < out.numDimensions(); i++) {
                cal[i] = 1;
            }
        }

        final RandomAccess<S> out_a = out.randomAccess();
        final int sz[] = new int[out_a.numDimensions()];
        for (int d = 0; d < out_a.numDimensions(); ++d) {
            sz[d] = (int) out.dimension(d);
        }

        // Iterate on all particles
        final double radius = p_radius;

        // Create a circle Mask and an iterator
        MaskOnSpaceMapper rg_m = null;
        float min = Float.MAX_VALUE;
        for (int i1 = 0; i1 < cal.length; i1++) {
            if (cal[i1] < min) {
                min = cal[i1];
            }
        }

        final float min_s = min;
        Integer rc = (int) (radius / min_s);
        for (int i = 0; i < out.numDimensions(); i++) {
            cal[i] /= min_s;
        }

        if (rc < 1) {
            rc = 1;
        }
        BallMask cm = null;
        if ((cm = map.get(rc)) == null) {
            cm = new BallMask(rc, 2 * rc + 1, cal);
            rg_m = new MaskOnSpaceMapper(cm, sz);
        }
        else {
            throw new RuntimeException();
        }

        final Point ptt = pt;

        // Draw the Sphere
        final Point p_c = new Point(ptt);
        p_c.div(cal);

        rg_m.setMiddlePoint(p_c);

        int sp = 0;

        while (rg_m.hasNext()) {
            final Point p = rg_m.nextPoint();

            if (p.isInside(sz)) {
                out_a.setPosition(p.iCoords);
                out_a.get().set(intensity);
                sp++;
            }
        }

        return sp;
    }

    /**
     * Create an image of type specified in the string
     *
     * @param size of the image
     * @param s type of image "8-bit" "16-bit" "float"
     * @return An Img<? extends NumericType<?>>
     */
    private <T extends RealType<T> & NativeType<T>> Img<T> createImage(Class<T> cls) {
        Img<T> out = null;

        final ImgFactory<T> imgFactory = new ArrayImgFactory<T>();
        try {
            out = imgFactory.create(Image_sz, cls.newInstance());
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
        }

        return out;
    }

    /**
     * Calculate how many point you can create on the grid on each dimension
     *
     * @return number of grid points on each dimension
     * @param spacing between points
     */
    private int[] calculateGridPoint(int spacing) {
        // Calculate the grid size
        int szi = 0;
        System.out.println("Max object size: " + spacing);
        System.out.println("Spacing: " + Debug.getArrayDims(Spacing) + " " + Arrays.toString(Spacing));
        System.out.println("Image_sz: " + Debug.getArrayDims(Image_sz) + " " + Arrays.toString(Image_sz));
        for (int i = 0; i < Image_sz.length - 1; i++) {
            if (Image_sz[i] != 1) {
                szi++;
            }
            else {
                break;
            }
        }

        final int[] gs = new int[szi];

        // Calculate the number of grid point
        for (int i = 0; i < szi; i++) {
            int numOfObjects = (int) (Image_sz[i]/(spacing + Spacing[i]));
            if (Image_sz[i] - (long) (numOfObjects * (spacing + Spacing[i])) < Spacing[i]) numOfObjects--;
            if (numOfObjects < 0) numOfObjects = 0;
            gs[i] = numOfObjects; //(int) ((Image_sz[i] - spacing / Spacing[i]) / (spacing / Spacing[i]));
        }
        System.out.println("gs: " + Debug.getArrayDims(gs) + " " + Arrays.toString(gs));
        return gs;
    }

    /**
     * Return the total point in the grid
     *
     * @param gs grid point on each dimension
     * @return the total number
     */
    private long totalGridPoint(int gs[]) {
        long gp = 1;

        for (int i = 0; i < gs.length; i++) {
            gp *= gs[i];
        }

        return gp;
    }

    /**
     * Fill the grid of point
     *
     * @param p Array of points as output
     * @param i grid specification
     * @param spacing between point
     */
    private void FillGridPoint(Point p[], int i[], int spacing) {
        int cnt = 0;
        final Iterator<Point> rg = new SpaceIterator(i).getPointIterator();

        final Point t = new Point(new int [i.length]);
        for (int s = 0; s < i.length; s++) {
            t.iCoords[s] = 1;
        }

        while (rg.hasNext() && cnt < p.length) {
            p[cnt] = rg.next();
            p[cnt] = p[cnt].add(t);
            p[cnt] = p[cnt].mult(spacing);
            p[cnt] = p[cnt].div(Spacing);
            cnt++;
        }
    }
    
    public static final String[] Region3DTrack_map         = new String[] { "Frame", "x", "y", "z", "Size", "Intensity", "Surface" };
    /**
     * Get CellProcessor for Region3DTrack objects
     */
    public static CellProcessor[] getRegion3DTrackCellProcessor() {
        return new CellProcessor[] { new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), };
    }
    
    public class Region3DTrack {

        private int Frame;
        private double x;
        private double y;
        private double z;
        private double Size;
        private double Intensity;
        private double Surface;

        public void setFrame(int fr) {
            Frame = fr;
        }

        public void setx(double x_) { // NO_UCD (unused code)
            x = x_;
        } // NO_UCD (unused code)

        public void sety(double y_) { // NO_UCD (unused code)
            y = y_;
        } // NO_UCD (unused code)

        public void setz(double z_) { // NO_UCD (unused code)
            z = z_;
        } // NO_UCD (unused code)

        public void setIntensity(double Intensity_) {
            Intensity = Intensity_;
        }

        public void setSize(double Size_) {
            Size = Size_;
        }

        public void setSurface(double Surface_) {
            Surface = Surface_;
        }

        public int getFrame() {
            return Frame;
        }

        public double getx() { // NO_UCD (unused code)
            return x;
        } // NO_UCD (unused code)

        public double gety() { // NO_UCD (unused code)
            return y;
        } // NO_UCD (unused code)

        public double getz() { // NO_UCD (unused code)
            return z;
        } // NO_UCD (unused code)

        public double getIntensity() {
            return Intensity;
        }

        public double getSize() {
            return Size;
        }

        public double getSurface() {
            return Surface;
        }

        public Region3DTrack() {
        }

        public void setData(Region r) {
            Frame = 0;
            x = r.getcx();
            y = r.getcy();
            z = r.getcz();
            Size = r.getrsize();
            Intensity = r.getintensity();
            Surface = r.getperimeter();
        }

        public void setData(Region3DTrack r) {
            Frame = r.Frame;
            x = r.x;
            y = r.y;
            z = r.z;
            Size = r.Size;
            Intensity = r.Intensity;
            Surface = r.Surface;
        }

        public void setObject_ID(@SuppressWarnings("unused") int Object_ID_) {

        }

        public void setPerimeter(double Perimeter_) {
            Surface = Perimeter_;
        }

        public void setLength(@SuppressWarnings("unused") double Length_) {

        }

        public void setImage_ID(int Image_ID_) {
            Frame = Image_ID_;
        }

        public void Coord_X(double Coord_X_) { // NO_UCD (unused code)
            x = Coord_X_;
        }
        
        public void Coord_Y(double Coord_Y_) { // NO_UCD (unused code)
            y = Coord_Y_;
        }
        
        public void Coord_Z(double Coord_Z_) { // NO_UCD (unused code)
            z = Coord_Z_;
        } 
        
        public void setCoord_X(double Coord_X_) {
            x = Coord_X_;
        }

        public void setCoord_Y(double Coord_Y_) {
            y = Coord_Y_;
        }

        public void setCoord_Z(double Coord_Z_) {
            z = Coord_Z_;
        }

        public int getImage_ID() {
            return Frame;
        }

        public int getObject_ID() {
            return 0;
        }

        public double getPerimeter() {
            return 0;
        }

        public double getLength() {
            return 0;
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

        public void setData(Point point) {
            if (point.iCoords.length >= 3) {
                x = point.iCoords[0];
                y = point.iCoords[1];
                z = point.iCoords[2];
            }
            else {
                x = point.iCoords[0];
                y = point.iCoords[1];
            }
        }

        public void setFile(@SuppressWarnings("unused") String dummy) {
        }

        public String getFile() {
            return null;
        }
    }

    
    /**
     * Process the frames
     *
     * @param Background intensities
     * @param intensity of the region
     */
    private <T extends RealType<T> & NativeType<T>> void Process(T Background, T max_int, T min_int, int max_radius, int min_radius, Class<T> cls) {
        map = new HashMap<Integer, BallMask>();

        // Vector if output region
        final Vector<Region3DTrack> pt_r = new Vector<Region3DTrack>();

        // Grid of possible region
        final Img<T> out = createImage(cls);
        
        final Cursor<T> c = out.cursor();
        while (c.hasNext()) {
            c.next();
            c.get().set(Background);
        }

        // for each frame
        for (int i = 0; i < Image_sz[Image_sz.length - 1]; i++) {
            IJ.showStatus("Creating frame: " + i);

            // set intensity
            int gs[] = null;
            System.out.println("MAX: max_radius: " + max_radius);
            gs = calculateGridPoint(2 * max_radius + 1);
            final long np = totalGridPoint(gs);
            if (np == 0) {
                IJ.error("The size of the image is too small or the region too big");
                return;
            }

            if (np < N_region) {
                IJ.error("Too much region increase the size of the image or reduce the number of the region");
                return;
            }

            final Point p[] = new Point[(int) np];
            FillGridPoint(p, gs, 2 * max_radius + 1);

            // shuffle
            Collections.shuffle(Arrays.asList(p));

            // Create a view of out fixing frame
            final IntervalView<T> vti = Views.hyperSlice(out, Image_sz.length - 1, i);

            // Draw spheres with radius and intensities
            for (int k = 0; k < N_region; k++) {
                final double max_it = max_int.getRealDouble();
                final double min_it = min_int.getRealDouble();
                final double max_r = max_radius;
                final double min_r = min_radius;
                final Random r = new Random();
                T inte_a = null;
                try {
                    inte_a = cls.newInstance();
                }
                catch (final InstantiationException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
                catch (final IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
                inte_a.setReal(min_it + (max_it - min_it) * r.nextDouble());
                final int radius = (int) (min_r + (max_r - min_r) * r.nextDouble());

                final Region3DTrack tmp = new Region3DTrack();

                // Add a Noise to the position (ensuring that region does not touch)
                final double dif = (int) (max_r - radius);

                double tot = 0;
                final float x[] = new float[Image_sz.length - 1];
                for (int s = 0; s < Image_sz.length - 1; s++) {
                    x[s] = r.nextFloat();
                    tot += x[s] * x[s];
                }
                tot = Math.sqrt(tot);

                for (int s = 0; s < Image_sz.length - 1; s++) {
                    x[s] /= tot;
                    x[s] *= dif;
                    p[k].iCoords[s] += x[s];
                }

                final int nsp = drawSphereWithRadius(vti, p[k], Spacing, inte_a, radius);

                tmp.setData(p[k]);
                tmp.setSize(nsp);
                tmp.setIntensity(inte_a.getRealDouble());
                tmp.setFrame(i);

                pt_r.add(tmp);
            }

            // Convolve the pictures
            cPSF.convolve(vti, Background);
        }

        // Image title
        String ImgTitle = new String();
        ImgTitle += "Regions_size_" + max_radius + "_" + min_radius + "_" + max_int + "_" + min_int;

        ImageJFunctions.show(out, ImgTitle).setDimensions(1, (int)Image_sz[2], (int)Image_sz[3]);

        // Output ground truth
        final CSV<Region3DTrack> P_csv = new CSV<Region3DTrack>(Region3DTrack.class);

        // get output folder
        String output = IJ.getDirectory("Choose output directory");
        if (output != null) {
            output += ImgTitle + ".csv";
            final CsvColumnConfig oc = new CsvColumnConfig(Region3DTrack_map, getRegion3DTrackCellProcessor());
            P_csv.Write(output, pt_r, oc, false);
        }
    }

    @Override
    public void run(ImageProcessor arg0) {
        if (imageT.equals("8-bit")) {
            final UnsignedByteType bck = new UnsignedByteType();
            final UnsignedByteType max_int = new UnsignedByteType();
            final UnsignedByteType min_int = new UnsignedByteType();
            bck.setReal(Background);
            max_int.setReal(Max_intensity);
            min_int.setReal(Min_intensity);
            this.<UnsignedByteType> Process(bck, max_int, min_int, Max_radius, Min_radius, UnsignedByteType.class);
        }
        else if (imageT.equals("16-bit")) {
            final ShortType bck = new ShortType();
            final ShortType max_int = new ShortType();
            final ShortType min_int = new ShortType();
            bck.setReal(Background);
            max_int.setReal(Max_intensity);
            min_int.setReal(Min_intensity);
            this.<ShortType> Process(bck, max_int, min_int, Max_radius, Min_radius, ShortType.class);
        }
        else if (imageT.equals("float")) {
            final FloatType bck = new FloatType();
            final FloatType max_int = new FloatType();
            final FloatType min_int = new FloatType();
            bck.setReal(Background);
            max_int.setReal(Max_intensity);
            min_int.setReal(Min_intensity);
            this.<FloatType> Process(bck, max_int, min_int, Max_radius, Min_radius, FloatType.class);
        }
    }

    @Override
    public int setup(String arg0, ImagePlus original_imp) {
        /* get user defined params and set more initial params accordingly */
        final GenericDialog gd = new GenericDialog("Region creator");

        gd.addNumericField("Background: ", 7, 3);
        gd.addNumericField("Max_radius", 10.0, 0);
        gd.addNumericField("Min_radius", 10.0, 0);
        gd.addNumericField("Max_intensity", 45, 3);
        gd.addNumericField("Min_intensity", 10, 3);
        gd.addNumericField("N_frame", 100.0, 0);
        gd.addNumericField("Image_X", 512.0, 0);
        gd.addNumericField("Image_Y", 512.0, 0);
        gd.addNumericField("Image_Z", 50.0, 0);
        gd.addNumericField("Spacing_X", 1.0, 1);
        gd.addNumericField("Spacing_Y", 1.0, 1);
        gd.addNumericField("Spacing_Z", 3.0, 1);
        gd.addNumericField("N_regions", 20, 0);

        final String nsc[] = { "Gauss" };
        gd.addChoice("Blur", nsc, nsc[0]);
        cConv = (Choice) gd.getChoices().lastElement();

        Button optionButton = new Button("Options");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 13;
        c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton, c);

        optionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                conv = cConv.getSelectedItem();
                cPSF = psfList.factory(conv, 3, FloatType.class);
                if (cPSF == null) {
                    IJ.error("Cannot create " + conv + ", convolution PSF");
                    return;
                }
                cPSF.getParamenters();
            }
        });

        gd.addChoice("Image_type: ", ImageType, ImageType[0]);
        gd.showDialog();

        if (gd.wasCanceled()) {
            return DONE;
        }

        Background = gd.getNextNumber();
        Max_radius = (int) gd.getNextNumber();
        Min_radius = (int) gd.getNextNumber();
        Max_intensity = (int) gd.getNextNumber();
        Min_intensity = (int) gd.getNextNumber();

        final long[] tmp = new long[4];
        tmp[3] = (long) gd.getNextNumber();
        tmp[0] = (long) gd.getNextNumber();
        tmp[1] = (long) gd.getNextNumber();
        tmp[2] = (long) gd.getNextNumber();
        if (tmp[2] == 1) {
            // 2D
            tmp[2] = tmp[3];
            Image_sz = new long[3];
            Image_sz[0] = tmp[0];
            Image_sz[1] = tmp[1];
            Image_sz[2] = tmp[2];

            Spacing = new float[2];

            Spacing[0] = (int) gd.getNextNumber();
            Spacing[1] = (int) gd.getNextNumber();
            gd.getNextNumber();
            N_region = (int) gd.getNextNumber();
        }
        else {
            // 3D
            Image_sz = new long[4];
            Image_sz[0] = tmp[0];
            Image_sz[1] = tmp[1];
            Image_sz[2] = tmp[2];
            Image_sz[3] = tmp[3];

            Spacing = new float[3];

            Spacing[0] = (int) gd.getNextNumber();
            Spacing[1] = (int) gd.getNextNumber();
            Spacing[2] = (int) gd.getNextNumber();
            N_region = (int) gd.getNextNumber();
        }

        // Get convolution
        if (cPSF == null) {
            conv = gd.getNextChoice();
            cPSF = psfList.factory(conv, Image_sz.length - 1, FloatType.class);
            if (cPSF == null) {
                IJ.error("Cannot create " + conv + ", convolution PSF");
                return DONE;
            }
        }
        else {
            gd.getNextChoice();
        }

        // Get Image type
        imageT = gd.getNextChoice();

        // if it is a batch system
        if (IJ.isMacro()) {
            // PSF get the parameters
            cPSF.getParamenters();
        }

        run(null);
        return DONE;
    }
}
