package mosaic.bregman;


import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Resizer;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.core.psf.GaussPSF;
import net.imglib2.type.numeric.real.DoubleType;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;


class AnalysePatch implements Runnable {

    // add oversampling here
    private final Tools ATools;
    private final ImagePatches impa;
    private final int interpolation;
    private int interpolationz;
    private boolean obj = false;
    private boolean border_attained = false;
    private double mint;// min threshold
    double cin, cout, cout_front;// estimated intensities
    double intmin, intmax;
    private final int os;
    private int osz;
    // coordinate of pacth in original image
    int offsetx;
    int offsety;
    int offsetz;
    private int margin;
    private int zmargin;
    private final int ox, oy, oz;// size of full image
    // size of patch
    int sx;
    int sy;
    int sz;

    private int fsxy, fsz;

    int isx;
    int isy;
    int isz; // interpolated object sizes
    private double min_thresh;
    private final double[][][] patch;
    double[][][] object;
    double[][][] interpolated_object;
    private final double[][][] mask;// nregions nslices ni nj
    private final Parameters p;
    final Region r;
    private final int channel;
    private double rescaled_min_int, rescaled_min_int_all;
    private final double[][][] temp1;
    private final double[][][] temp2;
    private final double[][][] temp3;
    private double[][][] weights;
    final double firstminval;
    private final double[][][][] w3kpatch;
    private final short[][][] regions_refined;
    private double t_high;
    private ASplitBregmanSolver A_solver;

    /**
     * Create patches
     *
     * @param image Image
     * @param r Region
     * @param pa Paramenters for split Bregman
     * @param oversampling level of overlampling
     * @param channel ?
     * @param regionsf ?
     * @param impa ?
     */
    public AnalysePatch(double[][][] image, Region r, Parameters pa, int oversampling, int channel, short[][][] regionsf, ImagePatches impa) {
        cout = 0;
        cin = 1;
        mint = 0.2;
        this.impa = impa;
        this.os = oversampling;
        this.r = r;
        this.channel = channel;
        ox = pa.ni;
        oy = pa.nj;
        oz = pa.nz;

        margin = 6;
        zmargin = 1;// was 2
        this.regions_refined = regionsf;

        // create local parameters
        this.p = new Parameters(pa);

        // check that the margin is at least 8 time bigger than the PSF
        final int[] sz_psf = p.PSF.getSuggestedImageSize();
        if (sz_psf[0] > margin) {
            margin = sz_psf[0];
        }
        if (sz_psf[1] > margin) {
            margin = sz_psf[1];
        }
        if (sz_psf.length > 2 && sz_psf[2] > margin) {
            zmargin = sz_psf[2];
        }

        this.interpolation = pa.interpolation;

        // compute patch geometry :
        set_patch_geom(r, oversampling);
        temp1 = new double[sz][sx][sy];
        temp2 = new double[sz][sx][sy];
        temp3 = new double[sz][sx][sy];

        // create weights mask (binary)
        voronoi_mask(r.rvoronoi);// mask for voronoi region into weights

        // create patch image with oversampling
        this.patch = new double[sz][sx][sy];
        fill_patch(image);

        // for testing
        this.w3kpatch = new double[1][sz][sx][sy];
        fill_w3kpatch(impa.w3kbest);

        // create pbject (for result)
        this.object = new double[sz][sx][sy];

        // create mask
        this.mask = new double[sz][sx][sy];
        fill_mask(r);

        // set size
        p.ni = sx;
        p.nj = sy;
        p.nz = sz;
        this.ATools = new Tools(p.ni, p.nj, p.nz);
        // set psf
        if (p.nz > 1) {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(3, DoubleType.class);
            final DoubleType[] var = new DoubleType[3];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            var[2] = new DoubleType(p.sigma_gaussian / p.zcorrec);
            psf.setVar(var);
            p.PSF = psf;
        }
        else {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(2, DoubleType.class);
            final DoubleType[] var = new DoubleType[2];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            psf.setVar(var);
            p.PSF = psf;
        }
        normalize();

        // double firstminval;
        if (channel == 0) {
            firstminval = p.min_intensity;
        }
        else {
            firstminval = p.min_intensityY;
        }

        rescaled_min_int_all = firstminval / 0.99; // first val with ~3% margin
        // (15 % compensated in find_best_t_and_int...)

        // estimate ints
        if (p.debug) {
            IJ.log("object :" + r.value);
        }
        if (p.mode_intensity == 0) {
            find_best_thresh_and_int(w3kpatch[0]);
        }
        else {
            if (p.mode_intensity == 1) {
                estimate_int_weighted(mask);
            }
            else if (p.mode_intensity == 2) {
                estimate_int_clustering(p.mode_intensity - 1);// (-1 to correct for old numbering)
            }
        }
        p.cl[0] = Math.max(cout, 0);
        p.cl[1] = Math.max(0.75 * (firstminval - intmin) / (intmax - intmin), cin);

        if (p.mode_intensity == 3)// mode high
        {
            p.cl[0] = p.betaMLEoutdefault;
            p.cl[1] = 1;
            t_high = cin;
        }

        rescaled_min_int_all = Math.max(0, (rescaled_min_int - cout) / (cin - cout)); //cin
        if (p.debug) {
            IJ.log(r.value + "min all " + rescaled_min_int_all);
        }

        // max iterations
        p.max_nsb = 101;
        p.nlevels = 1;
        // betaMLE
        // done after launch :
        p.RSSinit = false;
        p.findregionthresh = false;
        p.RSSmodulo = 501;

        p.thresh = 0.75;
        p.remask = false;
        for (int i = 0; i < p.lreg_.length; i++) {
            p.lreg_[i] = p.lreg_[i] * oversampling;// *(intmax-intmin);
        }
    }

    /**
     * Analyse one Patch
     * Or run SplitBregman segmentation solver on it
     */
    @Override
    public void run() {
        final MasksDisplay md = new MasksDisplay(sx, sy, sz, p);

        p.nthreads = 1;
        p.firstphase = false;

        if (p.nz > 1) {
            // Check the delta beta, if it is bigger than two ignore it, because
            // I cannot warrant stability
            if (Math.abs(p.cl[0] - p.cl[1]) > 2.0) {
                // reset
                p.cl[0] = p.betaMLEoutdefault;
                p.cl[1] = p.betaMLEindefault;
            }

            A_solver = new ASplitBregmanSolverTwoRegions3DPSF(p, patch, null, w3kpatch, md, channel, this);// mask instead of w3kpatch
        }
        else {
            // Check the delta beta, if it is bigger than two ignore it, because I cannot warrant stability
            if (Math.abs(p.cl[0] - p.cl[1]) > 2.0) {
                // reset
                p.cl[0] = p.betaMLEoutdefault;
                p.cl[1] = p.betaMLEindefault;
            }

            A_solver = new ASplitBregmanSolverTwoRegionsPSF(p, patch, null, w3kpatch, md, channel, this);// mask instead of w3kpatch
        }

        try {
            A_solver.first_run();

            cout = p.cl[0];
            cin = p.cl[1];

            final int ll = p.mode_intensity;
            if (ll == 0 || ll == 1) {
                min_thresh = rescaled_min_int_all * 0.96;
            }
            else {
                min_thresh = 0.25;
            }

            if (p.debug) {
                IJ.log("region" + r.value + "minth " + min_thresh);

            }
            double t = 0;
            if (p.mode_intensity != 3) {
                t = find_best_thresh(A_solver.w3kbest[0]);
            }

            if (p.mode_intensity == 3)// mode high
            {

                estimate_int_clustering(2);// (-1

                t_high = cin;
                if (p.debug) {
                    IJ.log("obj" + r.value + " effective t:" + t_high);
                }
                t = t_high - 0.04;
            }

            if (p.debug) {
                IJ.log("best thresh : " + t + "region" + r.value);
            }
            set_object(A_solver.w3kbest[0], t);
            if (interpolation > 1) {
                build_interpolated_object(A_solver.w3kbest[0], t);
                object = interpolated_object;
            }

            // assemble result into full image
            // assemble_result_voronoi2();
            assemble_patch();
        }
        catch (final InterruptedException ex) {
        }
    }

    private void set_object(double[][][] w3kbest, double t) {
        obj = false;
        border_attained = false;
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (w3kbest[z][i][j] > t && weights[z][i][j] == 1) {
                        this.object[z][i][j] = 1;
                        obj = true;
                        if (sz <= 1) {
                            if ((i == 0 && offsetx != 0) || (i == (sx - 1) && (offsetx + sx / os) != ox) || (j == 0 && offsety != 0) || (j == (sy - 1) && (offsety + sy / os) != oy)) {
                                border_attained = true;
                                // }
                            }
                        }
                    }
                    else {
                        this.object[z][i][j] = 0;
                    }
                }
            }
        }
    }

    /**
     * Compute the geometry of the patch
     *
     * @param r Region
     * @param oversampling level of oversampling
     */
    private void set_patch_geom(Region r, int oversampling) {
        int xmin, ymin, zmin, xmax, ymax, zmax;
        xmin = ox;
        ymin = oy;
        zmin = oz;
        xmax = 0;
        ymax = 0;
        zmax = 0;

        for (final Pix p : r.pixels) {
            if (p.px < xmin) {
                xmin = p.px;
            }
            if (p.px > xmax) {
                xmax = p.px;
            }
            if (p.py < ymin) {
                ymin = p.py;
            }
            if (p.py > ymax) {
                ymax = p.py;
            }
            if (p.pz < zmin) {
                zmin = p.pz;
            }
            if (p.pz > zmax) {
                zmax = p.pz;
            }
        }

        xmin = Math.max(0, xmin - margin);
        xmax = Math.min(ox, xmax + margin + 1);

        ymin = Math.max(0, ymin - margin);
        ymax = Math.min(oy, ymax + margin + 1);

        if (p.nz > 1) {
            zmin = Math.max(0, zmin - zmargin);
            zmax = Math.min(oz, zmax + zmargin + 1);
        }

        this.offsetx = xmin;
        this.offsety = ymin;
        this.offsetz = zmin;

        this.sx = (xmax - xmin) * oversampling;// todo :correct :+1 : done
        this.sy = (ymax - ymin) * oversampling;// correct :+1

        this.fsxy = oversampling * interpolation;
        this.isx = sx * interpolation;
        this.isy = sy * interpolation;

        if (p.nz == 1) {// if (zmax-zmin==0){
            this.osz = 1;
            this.sz = 1;
            this.fsz = 1;
            this.isz = 1;
            interpolationz = 1;
        }
        else {
            this.osz = os;
            this.sz = (zmax - zmin) * oversampling;
            this.fsz = oversampling * interpolation;
            this.isz = sz * interpolation;
            interpolationz = interpolation;
        }
    }

    private void fill_patch(double[][][] image) {
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    this.patch[z][i][j] = image[z / osz + offsetz][i / os + offsetx][j / os + offsety];
                }
            }
        }

        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (weights[z][i][j] == 0) {
                        this.patch[z][i][j] = 0;
                    }
                }
            }
        }
    }

    private void fill_w3kpatch(double[][][] w3k) {
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    this.w3kpatch[0][z][i][j] = w3k[z / osz + offsetz][i / os + offsetx][j / os + offsety];
                }
            }
        }

        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (weights[z][i][j] == 0) {
                        this.w3kpatch[0][z][i][j] = 0;
                    }
                }
            }
        }
    }

    private void normalize() {
        intmin = Double.MAX_VALUE;
        intmax = 0;
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (patch[z][i][j] > intmax) {
                        intmax = patch[z][i][j];
                    }
                    if (patch[z][i][j] < intmin) {
                        intmin = patch[z][i][j];
                    }
                }
            }
        }

        if (channel == 0) {
            rescaled_min_int = ((p.min_intensity - intmin) / (intmax - intmin));
        }
        else {
            rescaled_min_int = ((p.min_intensityY - intmin) / (intmax - intmin));
        }

        // rescale between 0 and 1
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    patch[z][i][j] = (patch[z][i][j] - intmin) / (intmax - intmin);
                }
            }
        }

        if (p.mode_intensity == 3) {

            double intmin2 = Double.MAX_VALUE;
            double intmax2 = 0;
            for (int z = 0; z < sz; z++) {
                for (int i = 0; i < sx; i++) {
                    for (int j = 0; j < sy; j++) {
                        if (w3kpatch[0][z][i][j] > intmax2) {
                            intmax2 = w3kpatch[0][z][i][j];
                        }
                        if (w3kpatch[0][z][i][j] < intmin2) {
                            intmin2 = w3kpatch[0][z][i][j];
                        }
                    }
                }
            }

            for (int z = 0; z < sz; z++) {
                for (int i = 0; i < sx; i++) {
                    for (int j = 0; j < sy; j++) {
                        w3kpatch[0][z][i][j] = (w3kpatch[0][z][i][j] - intmin2) / (intmax2 - intmin2);
                    }
                }
            }
        }
    }

    private void estimate_int_weighted(double[][][] mask) {
        RegionStatisticsSolver RSS;

        if (p.debug) {
            IJ.log("estimate int weighted");
        }
        RSS = new RegionStatisticsSolver(temp1, temp2, temp3, patch, weights, 10, p);
        RSS.eval(mask);

        cout = RSS.betaMLEout;
        cout_front = cout;
        if (p.debug) {
            IJ.log("r" + r.value + "RSS" + RSS.betaMLEin);
        }
        cin = /* Math.min(1, */RSS.betaMLEin/* ) */;
        mint = 0.25;

        if (p.debug) {
            IJ.log("mint " + mint);
            IJ.log("reg" + r.value + "rescaled min int" + rescaled_min_int);
            IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));
        }

    }

    private void estimate_int_clustering(int level) {
        // clustering done on original image data (not on soft mask)
        // test doing it on soft mask ? // but original soft mask patch
        // extracted from whole image is not normalized..
        if (p.debug) {
            IJ.log("init obj:" + r.value);
        }

        int nk;
        if (level == 1) {
            nk = 4;
        }
        else {
            nk = 3;// 3level culstering for low (removed) and high, 4 levels for
            // medium
            //
            // nk=4;//test high mode
        }

        int nk_in = 0;
        // if (level==0)nk_in=1;//low
        if (level == 2) {
            nk_in = 2;// high
        }
        if (level == 1) {
            nk_in = 2;// medium
        }

        // nk_in=3;// test high mode
        // int nk=4;//3
        final double[] pixel = new double[1];
        final double[] levels = new double[nk];

        int cpt_vals = 0;
        final Dataset data = new DefaultDataset();
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    pixel[0] = patch[z][i][j];
                    if (level == 2) {
                        pixel[0] = A_solver.w3kbest[0][z][i][j]; // w3kpatch[0]
                    }
                    final Instance instance = new DenseInstance(pixel);
                    if (weights[z][i][j] == 1) {
                        data.add(instance);
                        cpt_vals++;
                    }
                }
            }
        }

        if (p.debug) {
            IJ.log("inst" + r.value + " nbvals" + cpt_vals);
        }

        if (cpt_vals > 3) {
            final Clusterer km = new KMeans(nk, 100);
            /*
             * Cluster the data, it will be returned as an array of data sets,
             * with each dataset representing a cluster.
             */
            final Dataset[] data2 = km.cluster(data);

            nk = data2.length;// get number of clusters really found (usually =
            // 3 = setNumClusters but not always)
            for (int i = 0; i < nk; i++) {
                // Instance inst =DatasetTools.minAttributes(data2[i]);
                final Instance inst = DatasetTools.average(data2[i]);
                levels[i] = inst.value(0);
            }

            Arrays.sort(levels);
            int nk2 = Math.min(nk_in, nk - 1);
            cin = Math.max(rescaled_min_int, levels[nk2]);// -1;
            final int nkm1 = Math.max(nk2 - 1, 0);
            cout_front = levels[nkm1];
            cout = levels[0];
            if (level == 2) {
                cout = cout_front;
            }

            mint = 0.25;

            if (p.debug) {
                IJ.log("mint " + mint);

                IJ.log("rescaled min int" + rescaled_min_int);
                IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));

                IJ.log("levels :");
                for (int i = 0; i < nk; i++) {
                    IJ.log("level r" + r.value + " " + (i + 1) + " : " + levels[i]);
                }
            }
        }
        else // too few values for clustering..
        {
            cin = 1;
            cout_front = p.betaMLEoutdefault;
            cout = p.betaMLEoutdefault;
            mint = 0.25;

            if (p.debug) {
                IJ.log("usuing default intensity for r" + r.value);
            }
        }
    }

    private void fill_mask(Region r) {
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    this.mask[z][i][j] = 0;
                }
            }
        }

        for (final Pix p : r.pixels) {
            int rz = os * (p.pz - offsetz);
            int rx = os * (p.px - offsetx);
            int ry = os * (p.py - offsety);
            for (int z = rz; z < rz + osz; z++) {
                for (int i = rx; i < rx + os; i++) {
                    for (int j = ry; j < ry + os; j++) {
                        this.mask[z][i][j] = 1;
                    }
                }
            }
        }
    }

    private void voronoi_mask(Region r) {
        this.weights = new double[sz][sx][sy];
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    this.weights[z][i][j] = 0;
                }
            }
        }

        for (final Pix p : r.pixels) {
            int rz = os * (p.pz - offsetz);
            int rx = os * (p.px - offsetx);
            int ry = os * (p.py - offsety);
            if (rz < 0 || rz + osz > sz || rx < 0 || rx + os > sx || ry < 0 || ry + os > sy) {
                continue;
            }

            for (int z = rz; z < rz + osz; z++) {
                for (int i = rx; i < rx + os; i++) {
                    for (int j = ry; j < ry + os; j++) {
                        this.weights[z][i][j] = 1;
                    }
                }
            }
        }
    }

    /**
     * Here is calculated the best threshold for the patches
     *
     * @param w3kbest the mask
     * @return the best threshold based on energy calculation
     */
    private double find_best_thresh(double[][][] w3kbest) {
        double energy = Double.MAX_VALUE;
        double threshold = 0.75;
        for (double t = 1; t > min_thresh; t -= 0.02) { // mint
            ///rescaled_min_int_all*0.85
            // 0.5*cout
            set_object(w3kbest, t);

            if (obj && !border_attained) {
                double temp;
                if (p.nz == 1) {
                    temp = ATools.computeEnergyPSF_weighted(temp1, object, temp2, temp3, weights, p.ldata, p.lreg_[channel], p.PSF, cout_front, cin, patch);
                }
                else {
                    temp = ATools.computeEnergyPSF3D_weighted(temp1, object, temp2, temp3, weights, p.ldata, p.lreg_[channel], p.PSF, cout_front, cin, patch);
                }

                if (p.debug) {
                    IJ.log("energy " + temp + " t " + t + "region" + r.value);
                }
                if (temp < energy) {
                    energy = temp;
                    threshold = t;
                }
            }
        }
        return threshold;
    }

    double find_best_thresh_and_int(double[][][] w3kbest) {
        double energy = Double.MAX_VALUE;
        double threshold = 0.75;
        double tbest = 0.95;
        double cinbest, coutbest, cin_previous, cout_previous;
        double min_energy = 0;
        double max_energy = 0;
        cin_previous = cin;
        cout_previous = cout;

        cinbest = 1;
        coutbest = 0.0001;
        for (double t = 0.95; t > rescaled_min_int_all * 0.96; t -= 0.02) {
            set_object(w3kbest, t);

            if (obj && !border_attained) {
                estimate_int_weighted(object);

                double temp;
                if (p.nz == 1) {
                    temp = ATools.computeEnergyPSF_weighted(temp1, object, temp2, temp3, weights, p.ldata, p.lreg_[channel], p.PSF, cout_front, cin, patch);
                }
                else {
                    temp = ATools.computeEnergyPSF3D_weighted(temp1, object, temp2, temp3, weights, p.ldata, p.lreg_[channel], p.PSF, cout_front, cin, patch);
                }

                if (p.debug == true) {
                    IJ.log("energy and int " + temp + " t " + t + "region" + r.value + " cin " + cin + " cout " + cout + " obj " + obj);
                }
                if (temp < min_energy) {
                    min_energy = temp;
                }
                if (temp > max_energy) {
                    max_energy = temp;
                }
                if (temp < energy) {
                    energy = temp;
                    threshold = t;
                    cinbest = cin;
                    coutbest = cout;
                    tbest = t;
                }
            }
            else {
                cin = 1;
                cout_front = 0;
            }
        }

        // Debug
        cin = cinbest;
        cout = coutbest;
        cout_front = cout;

        if (!obj) {
            cin = cin_previous;
            cout = cout_previous;
        }

        rescaled_min_int_all = Math.max((rescaled_min_int - cout) / (cin - cout), 0);
        if (p.debug) {
            IJ.log("fbest" + r.value + "min all " + rescaled_min_int_all);
        }

        if (p.debug) {
            IJ.log(" best energy and int " + energy + " t " + tbest + "region" + r.value + " cin " + cin + " cout " + cout);
        }

        return threshold;
    }

    // build local object list
    private void assemble_patch() {
        // find connected regions
        final ImagePlus maska_im = new ImagePlus();
        final ImageStack maska_ims = new ImageStack(isx, isy);

        for (int z = 0; z < isz; z++) {
            final byte[] maska_bytes = new byte[isx * isy];
            for (int i = 0; i < isx; i++) {
                for (int j = 0; j < isy; j++) {
                    if (object[z][i][j] >= 1) {
                        maska_bytes[j * isx + i] = (byte) (object[z][i][j]);
                    }
                    else {
                        maska_bytes[j * isx + i] = (byte) 0;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(isx, isy);
            bp.setPixels(maska_bytes);
            maska_ims.addSlice("", bp);
        }

        maska_im.setStack("test Mask vo2", maska_ims);

        final FindConnectedRegions fcr = new FindConnectedRegions(maska_im, isx, isy, isz);// maska_im only

        final double thr = 0.5;
        final float[][][] Ri = new float[isz][isx][isy];
        for (int z = 0; z < isz; z++) {
            for (int i = 0; i < isx; i++) {
                for (int j = 0; j < isy; j++) {
                    Ri[z][i][j] = (float) thr;
                }
            }
        }

        fcr.run(thr, isx * isy * isz, 0 * fsxy, 0, Ri);// min size was 5

        // add to list with critical section
        impa.addRegionstoList(fcr.results);

        // add to regions refined with correct indexes
        assemble(fcr.results);
    }

    private void assemble(ArrayList<Region> localList) {
        for (final Region r : localList) {
            final ArrayList<Pix> npixels = new ArrayList<Pix>();

            for (final Pix v : r.pixels) {
                npixels.add(new Pix(v.pz + offsetz * fsz, v.px + offsetx * fsxy, v.py + offsety * fsxy));
                // count number of free edges
                regions_refined[v.pz + offsetz * fsz][v.px + offsetx * fsxy][v.py + offsety * fsxy] = (short) r.value;
            }
            r.pixels = npixels;
        }
    }

    // interpolation
    private void build_interpolated_object(double[][][] cmask, double t) {
        ImagePlus iobject = new ImagePlus();
        ImageStack iobjectS;
        interpolated_object = new double[isz][isx][isy];

        // build imageplus form non interpolated object
        iobjectS = new ImageStack(sx, sy);

        for (int z = 0; z < sz; z++) {
            final float[] twoD_float = new float[sx * sy];
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    twoD_float[j * sx + i] = (float) cmask[z][i][j];
                }
            }
            final FloatProcessor fp = new FloatProcessor(sx, sy);
            fp.setPixels(twoD_float);
            iobjectS.addSlice("", fp);
        }
        iobject.setStack("Object x", iobjectS);

        final Resizer re = new Resizer();

        if (isz != sz) {
            iobject = re.zScale(iobject, isz, ImageProcessor.BILINEAR);
        }

        final ImageStack imgS2 = new ImageStack(isx, isy);
        for (int z = 0; z < isz; z++) {
            iobject.setSliceWithoutUpdate(z + 1);
            iobject.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
            imgS2.addSlice("", iobject.getProcessor().resize(isx, isy, true));
        }
        iobject.setStack(imgS2);

        // put data into interpolted object
        ImageProcessor imp;
        for (int z = 0; z < isz; z++) {
            iobject.setSlice(z + 1);
            imp = iobject.getProcessor();
            for (int i = 0; i < isx; i++) {
                for (int j = 0; j < isy; j++) {
                    interpolated_object[z][i][j] = Float.intBitsToFloat(imp.getPixel(i, j));
                }
            }
        }

        // threshold
        for (int z = 0; z < isz; z++) {
            for (int i = 0; i < isx; i++) {
                for (int j = 0; j < isy; j++) {
                    if (interpolated_object[z][i][j] > t && weights[z / interpolationz][i / interpolation][j / interpolation] == 1) {
                        this.interpolated_object[z][i][j] = 1;
                    }
                    else {
                        this.interpolated_object[z][i][j] = 0;
                    }
                }
            }
        }
    }
}
