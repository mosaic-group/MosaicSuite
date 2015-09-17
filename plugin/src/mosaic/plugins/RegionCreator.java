package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Region3DTrack;
import mosaic.core.detection.MyFrame;
import mosaic.core.psf.psf;
import mosaic.core.psf.psfList;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.core.utils.SphereMask;
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
 *
 * Plugins to produce regions with ground truth
 *
 * @author Pietro Incardona
 *
 */


public class RegionCreator implements PlugInFilter // NO_UCD
{
    int N_region;
    int Max_radius;
    int Min_radius;
    int Max_intensity;
    int Min_intensity;
    int N_frame;
    long Image_sz[];
    float Spacing[];
    psf<FloatType> cPSF;
    double Background;

    String conv;
    Choice cConv;
    String nModel;
    Choice cNoise;
    String imageT;
    String[] ImageType = {"8-bit","16-bit","float"};

    HashMap<Integer,SphereMask> map;

    /**
     *
     * Draw a Sphere with radius on out
     *
     * @param out Image
     * @param pt point
     * @param cal spacing
     * @param intensity of your region
     * @param p_radius radius of your region
     */

    <S extends NumericType<S>> int drawSphereWithRadius(RandomAccessibleInterval<S> out, Point pt , float[] cal, S intensity, int p_radius)
    {
        if (cal == null)
        {
            cal = new float[out.numDimensions()];
            for (int i = 0 ; i < out.numDimensions() ; i++)
            {
                cal[i] = 1;
            }
        }

        RandomAccess<S> out_a = out.randomAccess();

        int sz[] = new int [out_a.numDimensions()];

        for ( int d = 0; d < out_a.numDimensions(); ++d )
        {
            sz[d] = (int) out.dimension( d );
        }

        // Iterate on all particles

        double radius = p_radius;

        // Create a circle Mask and an iterator

        RegionIteratorMask rg_m = null;

        float min_s = MyFrame.minScaling(cal);
        Integer rc = (int) (radius / min_s);
        for (int i = 0 ; i < out.numDimensions() ; i++)
        {
            cal[i] /= min_s;
        }

        if (rc < 1) {
            rc = 1;
        }
        SphereMask cm = null;
        if ((cm = map.get(rc)) == null)
        {
            cm = new SphereMask(rc, 2*rc + 1, out_a.numDimensions(), cal,true);
            rg_m = new RegionIteratorMask(cm, sz);
        }


        Point ptt = pt;

        // Draw the Sphere

        Point p_c = new Point(ptt);
        p_c.div(cal);

        rg_m.setMidPoint(p_c);

        int sp = 0;

        while ( rg_m.hasNext() )
        {
            Point p = rg_m.nextP();

            if (p.isInside(sz))
            {
                out_a.setPosition(p.x);
                out_a.get().set(intensity);
                sp++;
            }
        }

        return sp;
    }

    /**
     *
     * Draw a Sphere list with radius on out
     *
     * @param out Image
     * @param pt list of point
     * @param cal spacing
     * @param intensity of your region
     * @param p_radius radius of your region
     */

    <S extends NumericType<S>> void drawSphereWithRadius(RandomAccessibleInterval<S> out, List<Point> pt , float[] cal, S intensity, int p_radius)
    {
        if (cal == null)
        {
            cal = new float[out.numDimensions()];
            for (int i = 0 ; i < out.numDimensions() ; i++)
            {
                cal[i] = 1;
            }
        }

        RandomAccess<S> out_a = out.randomAccess();

        int sz[] = new int [out_a.numDimensions()];

        for ( int d = 0; d < out_a.numDimensions(); ++d )
        {
            sz[d] = (int) out.dimension( d );
        }

        // Iterate on all particles

        double radius = p_radius;

        // Create a circle Mask and an iterator

        RegionIteratorMask rg_m = null;

        float min_s = MyFrame.minScaling(cal);
        int rc = (int) (radius / min_s);
        for (int i = 0 ; i < out.numDimensions() ; i++)
        {
            cal[i] /= min_s;
        }

        if (rc < 1) {
            rc = 1;
        }
        SphereMask cm = null;
        if ((cm = map.get(rc)) == null)
        {
            cm = new SphereMask(rc, 2*rc + 1, out_a.numDimensions(), cal,true);
            rg_m = new RegionIteratorMask(cm, sz);
        }

        Iterator<Point> pt_it = pt.iterator();

        while (pt_it.hasNext())
        {
            Point ptt = pt_it.next();

            // Draw the Sphere

            Point p_c = new Point(ptt);
            p_c.div(cal);

            rg_m.setMidPoint(p_c);

            while ( rg_m.hasNext() )
            {
                Point p = rg_m.nextP();

                if (p.isInside(sz))
                {
                    out_a.setPosition(p.x);
                    out_a.get().set(intensity);
                }
            }
        }
    }

    /**
     *
     * Draw a Sphere list with radius on out
     *
     * @param out Image
     * @param pt list of point
     * @param cal spacing
     * @param intensity of your regions
     * @param p_radius radius of your region
     */

    <S extends NumericType<S>> void drawSphereWithRadius(RandomAccessibleInterval<S> out, List<Point> pt , float[] cal, List<S> intensity, int p_radius)
    {
        if (cal == null)
        {
            cal = new float[out.numDimensions()];
            for (int i = 0 ; i < out.numDimensions() ; i++)
            {
                cal[i] = 1;
            }
        }

        RandomAccess<S> out_a = out.randomAccess();

        int sz[] = new int [out_a.numDimensions()];

        for ( int d = 0; d < out_a.numDimensions(); ++d )
        {
            sz[d] = (int) out.dimension( d );
        }

        // Iterate on all particles

        double radius = p_radius;

        // Create a circle Mask and an iterator

        RegionIteratorMask rg_m = null;

        float min_s = MyFrame.minScaling(cal);
        int rc = (int) (radius / min_s);
        for (int i = 0 ; i < out.numDimensions() ; i++)
        {
            cal[i] /= min_s;
        }

        if (rc < 1) {
            rc = 1;
        }
        SphereMask cm = null;
        if ((cm = map.get(rc)) == null)
        {
            cm = new SphereMask(rc, 2*rc + 1, out_a.numDimensions(), cal,true);
            rg_m = new RegionIteratorMask(cm, sz);
        }

        Iterator<Point> pt_it = pt.iterator();
        Iterator<S> int_it = intensity.iterator();

        while (pt_it.hasNext())
        {
            Point ptt = pt_it.next();
            S inte = int_it.next();

            // Draw the Sphere

            Point p_c = new Point(ptt);
            p_c.div(cal);

            rg_m.setMidPoint(p_c);

            while ( rg_m.hasNext() )
            {
                Point p = rg_m.nextP();

                if (p.isInside(sz))
                {
                    out_a.setPosition(p.x);
                    out_a.get().set(inte);
                }
            }
        }
    }

    /**
     *
     * Create an image of type specified in the string
     *
     * @param size of the image
     * @param s type of image "8-bit" "16-bit" "float"
     * @return An Img<? extends NumericType<?>>
     *
     */

    <T extends RealType<T> & NativeType<T>> Img<T> createImage(long[] size, Class<T> cls)
    {
        Img<T> out = null;

        final ImgFactory< T > imgFactory = new ArrayImgFactory< T >();
        try {
            out = imgFactory.create(Image_sz, cls.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return out;
    }

    /**
     *
     * Calculate how many point you can create on the grid on each dimension
     *
     * @return number of grid points on each dimension
     * @param spacing between points
     */

    int [] calculateGridPoint(int spacing)
    {
        // Calculate the grid size

        int szi = 0;

        for (int i = 0; i < Image_sz.length-1 ; i++)
        {
            if (Image_sz[i] != 1)
            {szi++;}
            else
            {break;}
        }

        int [] gs = new int[szi];

        // Calculate the number of grid point

        for (int i = 0; i < szi ; i++)
        {
            gs[i] = (int) ((Image_sz[i] - spacing/Spacing[i] ) / (spacing/Spacing[i]));
        }

        return gs;
    }

    /**
     *
     * Return the total point in the grid
     *
     * @param gs grid point on each dimension
     * @return the total number
     */

    long totalGridPoint(int gs[])
    {
        long gp = 1;

        for (int i = 0 ; i < gs.length ; i++)
        {
            gp *= gs[i];
        }

        return gp;
    }

    /**
     *
     * Fill the grid of point
     *
     * @param p Array of points as output
     * @param i grid specification
     * @param spacing between point
     */

    void FillGridPoint(Point p[], int i[], int spacing)
    {
        int cnt = 0;
        RegionIterator rg = new RegionIterator(i);

        Point t = new Point(i.length);
        for (int s = 0 ; s < i.length ; s++)
        {
            t.x[s] = 1;
        }

        while (rg.hasNext() && cnt < p.length)
        {
            rg.next();
            p[cnt] = rg.getPoint();
            p[cnt] = p[cnt].add(t);
            p[cnt] = p[cnt].mult(spacing);
            p[cnt] = p[cnt].div(Spacing);
            cnt++;
        }
    }

    double VolRadius(double radius)
    {
        return Math.PI * radius * radius;
    }

    /**
     *
     * Process the frames
     *
     * @param Background intensities
     * @param intensity of the region
     *
     */

    <T extends RealType<T> & NativeType<T>> void Process(T Background, T max_int, T min_int, int max_radius, int min_radius, Class<T> cls)
    {
        map = new HashMap<Integer,SphereMask>();

        // Vector if output region

        Vector<Region3DTrack> pt_r = new Vector<Region3DTrack>();

        // Grid of possible region

        Img<T> out = createImage(Image_sz,cls);

        Cursor<T> c = out.cursor();
        while (c.hasNext())
        {
            c.next();
            c.get().set(Background);
        }

        // for each frame

        for (int i = 0 ; i < Image_sz[Image_sz.length-1]; i++)
        {
            IJ.showStatus("Creating frame: " + i);

            // set intensity

            int gs[] = null;
            gs = calculateGridPoint(2*max_radius + 1);
            long np = totalGridPoint(gs);
            if (np == 0)
            {
                IJ.error("The size of the image is too small or the region too big");
                return;
            }

            if (np < N_region)
            {
                IJ.error("Too much region increase the size of the image or reduce the number of the region");
                return;
            }

            Point p[] = new Point[(int)np];
            FillGridPoint(p,gs,2*max_radius+1);

            // shuffle

            Collections.shuffle(Arrays.asList(p));

            // Create a view of out fixing frame

            IntervalView<T> vti = Views.hyperSlice(out, Image_sz.length-1, i);

            // Draw spheres with radius and intensities

            for (int k = 0 ; k < N_region ; k++)
            {
                double max_it = max_int.getRealDouble();
                double min_it = min_int.getRealDouble();
                double max_r = max_radius;
                double min_r = min_radius;
                Random r = new Random();
                T inte_a = null;
                try {
                    inte_a = cls.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                inte_a.setReal(min_it + (max_it - min_it) * r.nextDouble());
                int radius = (int) (min_r + (max_r - min_r) * r.nextDouble());

                Region3DTrack tmp = new Region3DTrack();

                // Add a Noise to the position (ensuring that region does not touch)

                double dif = (int) (max_r - radius);

                double tot = 0;
                float x[] = new float [Image_sz.length-1];
                for (int s = 0 ; s < Image_sz.length - 1 ; s++)
                {
                    x[s] = r.nextFloat();
                    tot += x[s] * x[s];
                }
                tot = Math.sqrt(tot);

                for (int s = 0 ; s < Image_sz.length - 1 ; s++)
                {
                    x[s] /= tot;
                    x[s] *= dif;
                    p[k].x[s] += x[s];
                }

                int nsp = drawSphereWithRadius(vti,p[k],Spacing,inte_a,radius);

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

        ImageJFunctions.show(out,ImgTitle);

        // Output ground thruth

        CSV<Region3DTrack> P_csv = new CSV<Region3DTrack>(Region3DTrack.class);

        // get output folder

        String output = IJ.getDirectory("Choose output directory");;
        output += ImgTitle + ".csv";

        //

        CsvColumnConfig oc = new CsvColumnConfig(CSVOutput.Region3DTrack_map, CSVOutput.getRegion3DTrackCellProcessor());

        P_csv.Write(output, pt_r, oc, false);
    }

    @Override
    public void run(ImageProcessor arg0)
    {
        if (imageT.equals("8-bit"))
        {
            UnsignedByteType bck = new UnsignedByteType();
            UnsignedByteType max_int = new UnsignedByteType();
            UnsignedByteType min_int = new UnsignedByteType();
            bck.setReal(Background);
            max_int.setReal(Max_intensity);
            min_int.setReal(Min_intensity);
            this.<UnsignedByteType>Process(bck,max_int,min_int,Max_radius,Min_radius,UnsignedByteType.class);
        }
        else if (imageT.equals("16-bit"))
        {
            ShortType bck = new ShortType();
            ShortType max_int = new ShortType();
            ShortType min_int = new ShortType();
            bck.setReal(Background);
            max_int.setReal(Max_intensity);
            min_int.setReal(Min_intensity);
            this.<ShortType>Process(bck,max_int,min_int,Max_radius,Min_radius,ShortType.class);
        }
        else if (imageT.equals("float"))
        {
            FloatType bck = new FloatType();
            FloatType max_int = new FloatType();
            FloatType min_int = new FloatType();
            bck.setReal(Background);
            max_int.setReal(Max_intensity);
            min_int.setReal(Min_intensity);
            this.<FloatType>Process(bck,max_int,min_int,Max_radius,Min_radius,FloatType.class);
        }

        // produce the Ground truth

    }

    @Override
    public int setup(String arg0, ImagePlus original_imp)
    {
        if (MosaicUtils.checkRequirement() == false) {
            return DONE;
        }

        /* get user defined params and set more initial params accordingly 	*/

        GenericDialog gd = new GenericDialog("Region creator");

        String nsn[] = {"Poisson"};

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

        gd.addChoice("Noise", nsn, nsn[0]);
        cNoise = (Choice)gd.getChoices().lastElement();

        Button optionButton = new Button("Options");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx=2; c.gridy=13; c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton,c);

        optionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
            }
        });

        String nsc[] = {"Gauss"};

        gd.addChoice("Blur", nsc, nsc[0]);
        cConv = (Choice)gd.getChoices().lastElement();

        optionButton = new Button("Options");
        c = new GridBagConstraints();
        c.gridx=2; c.gridy=14; c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton,c);

        optionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                conv = cConv.getSelectedItem();
                cPSF = psfList.factory(conv,3,FloatType.class);
                if (cPSF == null)
                {
                    IJ.error("Cannot create " + conv + ", convolution PSF");
                    return;
                }
                cPSF.getParamenters();
            }
        });

        gd.addChoice("Image_type: ", ImageType, ImageType[0]);
        gd.showDialog();

        if (gd.wasCanceled())
        {
            return DONE;
        }

        Background = gd.getNextNumber();
        Max_radius = (int) gd.getNextNumber();
        Min_radius = (int) gd.getNextNumber();
        Max_intensity = (int) gd.getNextNumber();
        Min_intensity = (int) gd.getNextNumber();

        long[] tmp = new long[4];
        tmp[3] = (long) gd.getNextNumber();
        tmp[0] = (long) gd.getNextNumber();
        tmp[1] = (long) gd.getNextNumber();
        tmp[2] = (long) gd.getNextNumber();
        if (tmp[2] == 1)
        {
            // 2D

            tmp[2] = tmp[3];
            Image_sz = new long[3];
            Image_sz[0] = tmp[0];
            Image_sz[1] = tmp[1];
            Image_sz[2] = tmp[2];

            Spacing = new float[2];

            Spacing[0] = (int)gd.getNextNumber();
            Spacing[1] = (int)gd.getNextNumber();
            gd.getNextNumber();
            N_region = (int) gd.getNextNumber();
        }
        else
        {
            // 3D

            Image_sz = new long[4];
            Image_sz[0] = tmp[0];
            Image_sz[1] = tmp[1];
            Image_sz[2] = tmp[2];
            Image_sz[3] = tmp[3];

            Spacing = new float[3];

            Spacing[0] = (int)gd.getNextNumber();
            Spacing[1] = (int)gd.getNextNumber();
            Spacing[2] = (int)gd.getNextNumber();
            N_region = (int) gd.getNextNumber();
        }

        // Get noise model

        nModel = gd.getNextChoice();

        // Get convolution

        if (cPSF == null)
        {
            conv = gd.getNextChoice();
            cPSF = psfList.factory(conv,Image_sz.length-1,FloatType.class);
            if (cPSF == null)
            {
                IJ.error("Cannot create " + conv + ", convolution PSF");
                return DONE;
            }
        }
        else
        {
            gd.getNextChoice();
        }

        // Get Image type

        imageT = gd.getNextChoice();

        // if it is a batch system

        if (IJ.isMacro())
        {
            // PSF get the parameters

            cPSF.getParamenters();
        }

        run(null);
        return DONE;
    }

}
