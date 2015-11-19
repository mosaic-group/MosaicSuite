package mosaic.bregman;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Outdata;
import mosaic.core.utils.MosaicUtils;


public class Analysis {
    // This is the output for cluster
    public final static String out[] = { "*_ObjectsData_c1.csv", "*_ObjectsData_c2.csv", "*_mask_c1.zip", "*_mask_c2.zip", "*_ImagesData.csv", "*_outline_overlay_c1.zip", "*_outline_overlay_c2.zip",
            "*_intensities_c1.zip", "*_intensities_c2.zip", "*_seg_c1.zip", "*_seg_c2.zip", "*_coloc.zip", "*_soft_mask_c1.tiff", "*_soft_mask_c2.tiff", "*.tif" };

    // This is the output local
    public final static String out_w[] = { "*_ObjectsData_c1.csv", "*_ObjectsData_c2.csv", "*_mask_c1.zip", "*_mask_c2.zip", "*_ImagesData.csv", "*_outline_overlay_c1.zip", "*_outline_overlay_c2.zip",
            "*_intensities_c1.zip", "*_intensities_c2.zip", "*_seg_c1.zip", "*_seg_c2.zip", "*_soft_mask_c1.tiff", "*_soft_mask_c2.tiff", "*_coloc.zip" };

    static String currentImage = "currentImage";

    static ImagePlus imgA;
    private static ImagePlus imgB;
    public static Parameters p = new Parameters();

    private static byte[][][] maskA;// =new double [p.nz][p.ni][p.nj];
    private static byte[][][] maskB;// =new double [p.nz][p.ni][p.nj];
    static boolean[][][] cellMaskABinary;// =new double [p.nz][p.ni][p.nj];
    static boolean[][][] cellMaskBBinary;// =new double [p.nz][p.ni][p.nj];
    private static boolean[][][] overallCellMaskBinary;// =new double

    static int frame;

    // Maximum norm, it fix the range of the normalization, useful for video
    // normalization has to be done on all frame video, filled when the plugins
    // is called with the options min=... max=...
    public static double norm_max = 0.0;
    // Minimum norm
    public static double norm_min = 0.0;

    static short[][][][] regions;
    static ArrayList<Region> regionslist[];

    private static CountDownLatch DoneSignala;
    private static CountDownLatch DoneSignalb;
    static double[][][] imageb;// = new double [p.nz][p.ni][p.nj];
    static double[][][] imagea;// = new double [p.nz][p.ni][p.nj];

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void init() {
        regions = new short[2][][][];
        regionslist = new ArrayList[2];
    }

    static void load2channels(ImagePlus img2) {
        p.ni = img2.getWidth();
        p.nj = img2.getHeight();
        p.nz = img2.getNSlices();

        final int f = img2.getFrame();

        // IJ.log("creating a");
        imgA = new ImagePlus();
        final int bits = img2.getBitDepth();

        final ImageStack imga_s = new ImageStack(p.ni, p.nj);

        // channel 1
        for (int z = 0; z < p.nz; z++) {
            img2.setPosition(1, z + 1, f);
            ImageProcessor impt;
            if (bits == 32) {
                impt = img2.getProcessor().convertToShort(false);
            }
            else {
                impt = img2.getProcessor();
            }
            imga_s.addSlice("", impt);
        }

        imgA.setStack(img2.getTitle(), imga_s);
        // imgA.setTitle("A2");
        setimagea();

        imgB = new ImagePlus();
        final ImageStack imgb_s = new ImageStack(p.ni, p.nj);

        // channel 2
        for (int z = 0; z < p.nz; z++) {
            img2.setPosition(2, z + 1, f);
            ImageProcessor impt;
            if (bits == 32) {
                impt = img2.getProcessor().convertToShort(false);
            }
            else {
                impt = img2.getProcessor();
            }
            imgb_s.addSlice("", impt);
        }

        imgB.setStack(img2.getTitle(), imgb_s);
        setimageb();
    }

    /**
     * Get the objects list and set the frame
     *
     * @param f Frame
     * @param channel
     * @return Vector with objects
     */
    static Vector<? extends Outdata<Region>> getObjectsList(int f, int channel) {
        final Vector<? extends Outdata<Region>> v = CSVOutput.getVector(regionslist[channel]);

        // Set frame
        for (int i = 0; i < v.size(); i++) {
            v.get(i).setFrame(f);
        }

        // Convert it back to original type.
        return v;
    }

    static void load1channel(ImagePlus img2) {
        p.ni = img2.getWidth();
        p.nj = img2.getHeight();
        p.nz = img2.getNSlices();

        final int f = img2.getFrame();

        imgA = new ImagePlus();

        final ImageStack imga_s = new ImageStack(p.ni, p.nj);
        final int bits = img2.getBitDepth();
        // channel 1
        for (int z = 0; z < p.nz; z++) {
            img2.setPosition(1, z + 1, f);
            ImageProcessor impt;
            if (bits == 32) {
                impt = img2.getProcessor().convertToShort(false);
            }
            else {
                impt = img2.getProcessor();
            }
            imga_s.addSlice("", impt);
        }

        imgA.setStack(img2.getTitle(), imga_s);
        setimagea();
    }

    static double[] pearson_corr() {
        final Pearson ps = new Pearson(imgA, imgB, p);
        return ps.run();
    }

    final static ImagePlus out_soft_mask[] = new ImagePlus[2];

    /* Segment channel1 */
    static void segmentA() {

        // NRxegions nreg= new NRegions(img, p);
        // nreginos
        // PSF only working in two region problem
        // 3D only working for two problem
        currentImage = imgA.getTitle();

        DoneSignala = new CountDownLatch(1);

        // for this plugin AFAIK is always TwoRegion
        TwoRegions rg = null;

        if (p.usePSF == true || p.nz > 1 || p.nlevels == 1) {
            new Thread(rg = new TwoRegions(imgA, p, DoneSignala, 0)).start();
        }
        else {
            new Thread(new NRegions(imgA, p, DoneSignala, 0)).start();
        }

        try {
            Analysis.DoneSignala.await();
        }
        catch (final InterruptedException ex) {
        }

        // Merge frames
        if (p.dispSoftMask) {
            if (out_soft_mask[0] == null) {
                out_soft_mask[0] = new ImagePlus();
            }

            if (rg != null) MosaicUtils.MergeFrames(out_soft_mask[0], rg.out_soft_mask[0]);
            else {throw new RuntimeException("rg is null");}
            out_soft_mask[0].setStack(out_soft_mask[0].getStack());
        }
    }

    static void segmentb() {
        // NRegions nreg= new NRegions(img, p);
        currentImage = imgB.getTitle();
        DoneSignalb = new CountDownLatch(1);

        // for this plugin AFAIK is always TwoRegion
        TwoRegions rg = null;

        if (p.usePSF == true || p.nz > 1 || p.nlevels == 1) {
            new Thread(rg = new TwoRegions(imgB, p, DoneSignalb, 1)).start();
        }
        else {
            new Thread(new NRegions(imgB, p, DoneSignalb, 1)).start();
        }

        try {
            Analysis.DoneSignalb.await();
        }
        catch (final InterruptedException ex) {
        }

        // Merge software
        if (p.dispSoftMask) {
            if (out_soft_mask[1] == null) {
                out_soft_mask[1] = new ImagePlus();
            }

            if (rg != null)  MosaicUtils.MergeFrames(out_soft_mask[1], rg.out_soft_mask[1]);
            else {throw new RuntimeException("rg is null");}
            out_soft_mask[1].setStack(out_soft_mask[1].getStack());
        }
    }

    static void compute_connected_regions_a(double d, float[][][] RiN) {
        // IJ.log("connected ana"+d);
        final ImagePlus maska_im = new ImagePlus();
        final ImageStack maska_ims = new ImageStack(p.ni, p.nj);

        for (int z = 0; z < p.nz; z++) {
            final byte[] maska_bytes = new byte[p.ni * p.nj];
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    maska_bytes[j * p.ni + i] = maskA[z][i][j];
                }
            }
            final ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
            bp.setPixels(maska_bytes);
            maska_ims.addSlice("", bp);
        }

        maska_im.setStack("test Ma", maska_ims);

        final FindConnectedRegions fcr = new FindConnectedRegions(maska_im);// maska_im
        // only
        float[][][] Ri;
        if (p.mode_voronoi2) {
            Ri = new float[p.nz][p.ni][p.nj];
            for (int z = 0; z < p.nz; z++) {
                for (int i = 0; i < p.ni; i++) {
                    for (int j = 0; j < p.nj; j++) {
                        Ri[z][i][j] = (float) p.min_intensity;
                    }
                }
            }
        }
        else {
            if (RiN == null) {
                Ri = new float[p.nz][p.ni][p.nj];
                for (int z = 0; z < p.nz; z++) {
                    for (int i = 0; i < p.ni; i++) {
                        for (int j = 0; j < p.nj; j++) {
                            Ri[z][i][j] = (float) d;
                        }
                    }
                }
            }
            else {
                Ri = RiN;
            }
        }

        if (p.debug) {
            fcr.run(d, p.maxves_size, p.minves_size, 255 * p.min_intensity, Ri);// &&(!p.refinement)
        }
        else {
            fcr.run(d, p.maxves_size, p.minves_size, 255 * p.min_intensity, Ri);
        }

        regions[0] = fcr.tempres;
        regionslist[0] = fcr.results;
        if (!p.mode_voronoi2) {
            if (p.nz > 1) {
                IJ.log(regionslist[0].size() + " objects found in X, mean volume : " + Tools.round(meansize(regionslist[0]), 2) + " pixels.");
            }
            else {
                IJ.log(regionslist[0].size() + " objects found in X, mean area : " + Tools.round(meansize(regionslist[0]), 2) + " pixels.");
            }
        }
    }

    static void compute_connected_regions_b(double d, float[][][] RiN) {
        final ImagePlus maskb_im = new ImagePlus();
        final ImageStack maskb_ims = new ImageStack(p.ni, p.nj);

        boolean cellmask;

        for (int z = 0; z < p.nz; z++) {
            final byte[] maskb_bytes = new byte[p.ni * p.nj];
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    cellmask = true;

                    if (cellmask) {
                        maskb_bytes[j * p.ni + i] = maskB[z][i][j];
                    }
                    else {
                        maskb_bytes[j * p.ni + i] = 0;
                    }

                }
            }
            final ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
            bp.setPixels(maskb_bytes);
            maskb_ims.addSlice("", bp);
        }

        maskb_im.setStack("", maskb_ims);
        final FindConnectedRegions fcr = new FindConnectedRegions(maskb_im);

        float[][][] Ri;

        if (p.mode_voronoi2) {
            Ri = new float[p.nz][p.ni][p.nj];
            for (int z = 0; z < p.nz; z++) {
                for (int i = 0; i < p.ni; i++) {
                    for (int j = 0; j < p.nj; j++) {
                        Ri[z][i][j] = (float) p.min_intensityY;
                    }
                }
            }
        }
        else {
            if (RiN == null) {// ==true for testing with minimum intensity
                Ri = new float[p.nz][p.ni][p.nj];
                for (int z = 0; z < p.nz; z++) {
                    for (int i = 0; i < p.ni; i++) {
                        for (int j = 0; j < p.nj; j++) {
                            Ri[z][i][j] = (float) d;
                        }
                    }
                }
            }
            else {
                Ri = RiN;
            }
        }

        fcr.run(d, p.maxves_size, p.minves_size, 255 * p.min_intensityY, Ri);

        regions[1] = fcr.tempres;
        regionslist[1] = fcr.results;
        if (!p.mode_voronoi2) {
            if (p.nz > 1) {
                IJ.log(regionslist[1].size() + " objects found in Y, mean volume : " + Tools.round(meansize(regionslist[1]), 2) + " pixels.");
            }
            else {
                IJ.log(regionslist[1].size() + " objects found in Y, mean area : " + Tools.round(meansize(regionslist[1]), 2) + " pixels.");
            }
        }
    }

    static double colocsegA() {
        double sum = 0;
        int objects = 0;
        for (final Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
            final Region r = it.next();
            objects++;
            sum += regionsum(r, imageb);
        }

        return (sum / objects);
    }

    private static void setimagea() {
        double maxa = 0;
        double mina = Double.POSITIVE_INFINITY;

        ImageProcessor imp;

        imagea = new double[p.nz][p.ni][p.nj];

        for (int z = 0; z < p.nz; z++) {
            imgA.setSlice(z + 1);
            imp = imgA.getProcessor();
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    imagea[z][i][j] = imp.getPixel(i, j);
                    if (imagea[z][i][j] > maxa) {
                        maxa = imagea[z][i][j];
                    }
                    if (imagea[z][i][j] < mina) {
                        mina = imagea[z][i][j];
                    }
                }
            }
        }

        for (int z = 0; z < p.nz; z++) {
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    imagea[z][i][j] = (imagea[z][i][j] - mina) / (maxa - mina);
                }
            }
        }
    }

    private static void setimageb() {
        double maxb = 0;
        double minb = Double.POSITIVE_INFINITY;

        ImageProcessor imp;

        imageb = new double[p.nz][p.ni][p.nj];

        for (int z = 0; z < p.nz; z++) {
            imgB.setSlice(z + 1);
            imp = imgB.getProcessor();
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    imageb[z][i][j] = imp.getPixel(i, j);
                    if (imageb[z][i][j] > maxb) {
                        maxb = imageb[z][i][j];
                    }
                    if (imageb[z][i][j] < minb) {
                        minb = imageb[z][i][j];
                    }
                }
            }
        }

        for (int z = 0; z < p.nz; z++) {
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    imageb[z][i][j] = (imageb[z][i][j] - minb) / (maxb - minb);
                }
            }
        }
    }

    static double colocsegB() {
        double sum = 0;
        int objects = 0;
        for (final Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
            final Region r = it.next();
            objects++;
            sum += regionsum(r, imagea);
        }

        return (sum / objects);
    }

    static double colocsegAB() {
        double totalsignal = 0;
        double colocsignal = 0;

        for (final Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
            final Region r = it.next();

            if (regioncoloc(r, regionslist[1], regions[1])) {
                // TODO: if condition intentionally left here - not sure if it does any changes
                // objectscoloc++;
            }

            totalsignal += r.rsize * r.intensity;
            colocsignal += r.rsize * r.intensity * r.overlap;
        }

        return (colocsignal / totalsignal);
    }

    static double colocsegABsize() {
        double totalsize = 0;
        double colocsize = 0;

        for (final Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
            final Region r = it.next();
            if (regioncoloc(r, regionslist[1], regions[1])) {
                // TODO: if condition intentionally left here - not sure if it does any changes
                // objectscoloc++;
            }

            totalsize += r.rsize;
            colocsize += r.rsize * r.overlap;
        }

        return (colocsize / totalsize);
    }

    static double colocsegABnumber() {
        int objectscoloc = 0;
        int objects = 0;
        for (final Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
            final Region r = it.next();
            objects++;
            if (r.colocpositive) {
                objectscoloc++;
            }
        }

        return (((double) objectscoloc) / objects);
    }

    static double colocsegBAnumber() {
        int objectscoloc = 0;
        int objects = 0;
        for (final Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
            final Region r = it.next();
            // IJ.log("obj" + r.value);
            objects++;
            if (r.colocpositive) {
                objectscoloc++;
            }
        }

        return (((double) objectscoloc) / objects);
    }

    static double colocsegBA() {
        double totalsignal = 0;
        double colocsignal = 0;

        for (final Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
            final Region r = it.next();

            if (regioncoloc(r, regionslist[0], regions[0])) {
                // TODO: if condition intentionally left here - not sure if it does any changes
                // objectscoloc++;
            }

            totalsignal += r.rsize * r.intensity;
            colocsignal += r.rsize * r.intensity * r.overlap;
        }

        return (colocsignal / totalsignal);
    }

    static double colocsegBAsize() {
        double totalsize = 0;
        double colocsize = 0;

        for (final Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
            final Region r = it.next();

            if (regioncoloc(r, regionslist[0], regions[0])) {
                // TODO: if condition intentionally left here - not sure if it does any changes
                // objectscoloc++;
            }

            totalsize += r.rsize;
            colocsize += r.rsize * r.overlap;
        }

        return (colocsize / totalsize);
    }

    private static boolean regioncoloc(Region r, ArrayList<Region> regionlist, short[][][] regions) {
        boolean positive = false;
        int count = 0;
        int countcoloc = 0;
        int previousvalcoloc = 0;
        int valcoloc;
        boolean oneColoc = true;
        double intColoc = 0;
        double sizeColoc = 0;
        final int osxy = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix p = it.next();
            valcoloc = regions[p.pz][p.px][p.py];
            // IJ.log("valcoloc " + valcoloc);
            if (valcoloc > 0) {
                countcoloc++;
                if (previousvalcoloc != 0 && valcoloc != previousvalcoloc) {
                    oneColoc = false;
                }
                intColoc += regionlist.get(valcoloc - 1).intensity;
                sizeColoc += regionlist.get(valcoloc - 1).points;
                previousvalcoloc = valcoloc;
            }
            count++;
        }

        positive = ((double) countcoloc) / count > p.colocthreshold;
        r.colocpositive = positive;
        r.overlap = (float) Tools.round(((double) countcoloc) / count, 3);
        r.over_size = (float) Tools.round((sizeColoc) / countcoloc, 3);
        if (p.nz == 1) {
            r.over_size = (float) Tools.round(r.over_size / (osxy * osxy), 3);
        }
        else {
            r.over_size = (float) Tools.round(r.over_size / (osxy * osxy * osxy), 3);
        }

        r.over_int = (float) Tools.round((intColoc) / countcoloc, 3);
        r.singlec = oneColoc;

        return (positive);
    }

    static void SetRegionsObjsVoronoi(ArrayList<Region> regionlist, ArrayList<Region> regionsvoronoi, float[][][] ri) {
        int x, y, z;
        for (final Iterator<Region> it = regionlist.iterator(); it.hasNext();) {
            final Region r = it.next();
            x = r.pixels.get(0).px;
            y = r.pixels.get(0).py;
            z = r.pixels.get(0).pz;

            r.rvoronoi = regionsvoronoi.get((int) ri[z][x][y]);
        }
    }
    
    private static double regionsum(Region r, double[][][] image) {

        final int factor2 = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
        int fz2;
        if (Analysis.p.nz > 1) {
            fz2 = factor2;
        }
        else {
            fz2 = 1;
        }

        int count = 0;
        double sum = 0;
        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix p = it.next();
            sum += image[p.pz / fz2][p.px / factor2][p.py / factor2];
            count++;
        }

        r.coloc_o_int = (sum / count);
        return (sum / count);
    }

    static void regionCenter(Region r) {

        int count = 0;
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix p = it.next();
            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
            count++;
        }

        r.cx = (float) (sumx / count);
        r.cy = (float) (sumy / count);
        r.cz = (float) (sumz / count);
        if (Analysis.p.subpixel) {

            r.cx = r.cx / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
            r.cy = r.cy / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
            r.cz = r.cz / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
        }
    }

    /**
     * Allocate a byte maskA based on the double mask
     */
    static void setMaskaTworegions(double[][][] mask) {
        maskA = new byte[p.nz][p.ni][p.nj];
        for (int z = 0; z < p.nz; z++) {
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    maskA[z][i][j] = (byte) ((int) (255 * mask[z][i][j]));
                }
            }
        }
    }

    static void setMaskbTworegions(double[][][] mask) {
        maskB = new byte[p.nz][p.ni][p.nj];
        for (int z = 0; z < p.nz; z++) {
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    maskB[z][i][j] = (byte) ((int) (255 * mask[z][i][j]));
                }
            }
        }
    }

    static void setmaska(int[][][] mask) {
        maskA = new byte[p.nz][p.ni][p.nj];
        for (int z = 0; z < p.nz; z++) {
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    maskA[z][i][j] = (byte) ((mask[z][i][j]));
                }
            }
        }

    }

    static void setmaskb(int[][][] mask) {
        maskB = new byte[p.nz][p.ni][p.nj];
        for (int z = 0; z < p.nz; z++) {
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    maskB[z][i][j] = (byte) ((mask[z][i][j]));
                }
            }
        }

    }

    static double meansurface(ArrayList<Region> regionslist) {
        double totalsize = 0;
        int objects = 0;
        for (final Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
            final Region r = it.next();
            objects++;
            totalsize += r.perimeter;
        }

        return (totalsize / objects);
    }

    static double meanlength(ArrayList<Region> regionslist) {
        double totalsize = 0;
        int objects = 0;
        for (final Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
            final Region r = it.next();
            objects++;
            totalsize += r.length;
        }

        return (totalsize / objects);
    }

    static double meansize(ArrayList<Region> regionslist) {
        double totalsize = 0;
        int objects = 0;
        for (final Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
            final Region r = it.next();
            objects++;
            totalsize += r.points;
        }

        if (Analysis.p.subpixel) {
            return (totalsize / objects) / (Math.pow(Analysis.p.oversampling2ndstep * Analysis.p.interpolation, 2));
        }
        else {
            return (totalsize / objects);
        }
    }

    static double totalsize(ArrayList<Region> regionslist) {
        double totalsize = 0;

        for (final Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
            final Region r = it.next();

            totalsize += r.points;
        }

        return (totalsize);
    }

    static ArrayList<Region> removeExternalObjects(ArrayList<Region> regionslist) {
        final ArrayList<Region> newregionlist = new ArrayList<Region>();

        for (final Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
            final Region r = it.next();
            if (isInside(r)) {
                newregionlist.add(r);
            }
        }
        regionslist = newregionlist;

        return newregionlist;
    }

    private static boolean isInside(Region r) {
        final int factor2 = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
        int fz2;
        if (Analysis.p.nz > 1) {
            fz2 = factor2;
        }
        else {
            fz2 = 1;
        }
        double size = 0;
        int inside = 0;
        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix px = it.next();
            if (overallCellMaskBinary[px.pz / fz2][px.px / factor2][px.py / factor2]) {
                inside++;
            }
            size++;
        }
        return ((inside / size) > 0.1);
    }

    static void computeOverallMask() {

        final boolean mask[][][] = new boolean[p.nz][p.ni][p.nj];

        for (int z = 0; z < p.nz; z++) {
            for (int i = 0; i < p.ni; i++) {
                for (int j = 0; j < p.nj; j++) {
                    if (p.usecellmaskX && p.usecellmaskY) {
                        mask[z][i][j] = cellMaskABinary[z][i][j]// >254
                                && cellMaskBBinary[z][i][j];// >254;
                    }
                    else if (p.usecellmaskX) {
                        mask[z][i][j] = cellMaskABinary[z][i][j];
                    }// >254;}
                    else if (p.usecellmaskY) {
                        mask[z][i][j] = cellMaskBBinary[z][i][j];
                    }// >254;}
                    else {
                        mask[z][i][j] = true;
                    }

                }
            }
        }
        overallCellMaskBinary = mask;
    }

    static void setRegionsLabels(ArrayList<Region> regionslist, short[][][] regions) {
        final int factor2 = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
        int fz2;
        if (Analysis.p.nz > 1) {
            fz2 = factor2;
        }
        else {
            fz2 = 1;
        }
        int index = 1;

        for (int z = 0; z < p.nz * fz2; z++) {
            for (int i = 0; i < p.ni * factor2; i++) {
                for (int j = 0; j < p.nj * factor2; j++) {
                    regions[z][i][j] = 0;
                }
            }
        }

        for (final Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
            final Region r = it.next();
            // r.value=index; keep old index in csv file : do not (because
            // displaying happens before, with the previous values)
            setRegionLabel(r, regions, index);
            // IJ.log(" "+index);
            index++;
        }

    }

    private static void setRegionLabel(Region r, short[][][] regions, int label) {

        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix px = it.next();
            regions[px.pz][px.px][px.py] = (short) label;
        }
    }
}
