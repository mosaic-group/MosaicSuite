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
import mosaic.utils.ArrayOps;
import mosaic.utils.Debug;
import mosaic.utils.ArrayOps.MinMax;
import net.imglib2.type.numeric.real.DoubleType;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.DatasetTools;


class AnalysePatch implements Runnable {
    // size of original image 
    private final int iSizeOrigX;
    private final int iSizeOrigY;
    private final int iSizeOrigZ;

    // coordinate of patch in original image
    private int iOffsetOrigX;
    private int iOffsetOrigY;
    private int iOffsetOrigZ;
    
    // size of patch (oversampled)
    private int iSizeOversX;
    private int iSizeOversY;
    private int iSizeOversZ;
    
    // interpolated object sizes (interpolated and oversampled)
    private int iSizeOverInterX;
    private int iSizeOverInterY;
    private int iSizeOverInterZ;
    
    private int iInterpolationXY;
    private int iInterpolationZ;
    
    private int iOversamplingInXY;
    private int iOverInterInXY; 
    private int iOversamplingInZ;
    private int iOverInterInZ;
    
    // Input parameters
    final Region iInputRegion;
    private final int iChannel;
    private final ImagePatches iImagePatches;

    private final Tools iLocalTools;
    private final Parameters iLocalParams;
    
    double iIntensityMin;
    double iIntensityMax;
    double iScaledIntensityMin;
    
    private boolean border_attained = false;
    private boolean objectFound = false;
    double[][][] object;
    
    private double[][][] iRegionMask;
    
    private double mint;// min threshold
    double cin, cout, cout_front;// estimated intensities
    double firstminval;
    private double min_thresh;
    private final double[][][] iPatch;
    private final double[][][] mask;// nregions nslices ni nj
    private double rescaled_min_int_all;
    private final double[][][][] w3kpatch;
    private double t_high;
    private ASplitBregmanSolver A_solver;
    
    // Temporary buffers for RSS and computeEnergyPSF_weighted methods
    private final double[][][] temp1;
    private final double[][][] temp2;
    private final double[][][] temp3;

    /**
     * Create patches
     *
     * @param aInputImage Image
     * @param aInputRegion Region
     * @param aParameters Paramenters for split Bregman
     * @param aOversampling level of overlampling
     * @param aChannel ?
     * @param regionsf ?
     * @param aImagePatches ?
     */
    public AnalysePatch(double[][][] aInputImage, Region aInputRegion, Parameters aParameters, int aOversampling, int aChannel, ImagePatches aImagePatches, double[][][] w3kbest) {
        iSizeOrigX = aParameters.ni;
        iSizeOrigY = aParameters.nj;
        iSizeOrigZ = aParameters.nz;

        iInputRegion = aInputRegion;
        iLocalParams = new Parameters(aParameters);
        iChannel = aChannel;
        iImagePatches = aImagePatches;
        
        cout = 0;
        cin = 1;
        mint = 0.2;

        // Setup patch margins
        // old comment: check that the margin is at least 8 time bigger than the PSF
        int margin = 6;
        int zmargin = 1;// was 2
        final int[] sz_psf = iLocalParams.PSF.getSuggestedImageSize();
        if (sz_psf[0] > margin) {
            margin = sz_psf[0];
        }
        if (sz_psf[1] > margin) {
            margin = sz_psf[1];
        }
        if (sz_psf.length > 2 && sz_psf[2] > margin) { // TODO: shouldn't compare with zmargin?
            zmargin = sz_psf[2];
        }

        // compute patch geometry
        computePatchGeometry(aInputRegion, aOversampling, aParameters.interpolation, margin, zmargin);
        
        // create weights mask (binary)
        iRegionMask = generateMask(aInputRegion.rvoronoi, true);// mask for voronoi region into weights

        // create patch image with oversampling
        iPatch = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        fill_patch(aInputImage, iPatch, iRegionMask, iOversamplingInXY, iOversamplingInZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);

        // for testing
        w3kpatch = new double[1][iSizeOversZ][iSizeOversX][iSizeOversY];
        fill_patch(w3kbest, w3kpatch[0], iRegionMask, iOversamplingInXY, iOversamplingInZ, iOffsetOrigX, iOffsetOrigY, iOffsetOrigZ);
        
        // create object (for result)
        object = new double[iSizeOversZ][iSizeOversX][iSizeOversY];

        // create mask
        mask = generateMask(aInputRegion, false);

        // set size
        iLocalParams.ni = iSizeOversX;
        iLocalParams.nj = iSizeOversY;
        iLocalParams.nz = iSizeOversZ;
        iLocalTools = new Tools(iLocalParams.ni, iLocalParams.nj, iLocalParams.nz);
        
        // set psf
        if (iLocalParams.nz > 1) {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(3, DoubleType.class);
            final DoubleType[] var = new DoubleType[3];
            var[0] = new DoubleType(iLocalParams.sigma_gaussian);
            var[1] = new DoubleType(iLocalParams.sigma_gaussian);
            var[2] = new DoubleType(iLocalParams.sigma_gaussian / iLocalParams.zcorrec);
            psf.setVar(var);
            iLocalParams.PSF = psf;
        }
        else {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(2, DoubleType.class);
            final DoubleType[] var = new DoubleType[2];
            var[0] = new DoubleType(iLocalParams.sigma_gaussian);
            var[1] = new DoubleType(iLocalParams.sigma_gaussian);
            psf.setVar(var);
            iLocalParams.PSF = psf;
        }
        
        // normalize
        MinMax<Double> minMax = ArrayOps.normalize(iPatch);
        iIntensityMin = minMax.getMin();
        iIntensityMax = minMax.getMax();
        iScaledIntensityMin = ( ((iChannel == 0) ? iLocalParams.min_intensity : iLocalParams.min_intensityY) - iIntensityMin) / (iIntensityMax - iIntensityMin);
        if (iLocalParams.mode_intensity == 3) {
            ArrayOps.normalize(w3kpatch[0]);
        }

        firstminval = (aChannel == 0) ? iLocalParams.min_intensity : iLocalParams.min_intensityY;
        rescaled_min_int_all = firstminval / 0.99; // first val with ~3% margin
        // (15 % compensated in find_best_t_and_int...)

        temp1 = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        temp2 = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        temp3 = new double[iSizeOversZ][iSizeOversX][iSizeOversY];
        
        // estimate ints
        if (iLocalParams.mode_intensity == 0) {
            find_best_thresh_and_int(w3kpatch[0]);
        }
        else {
            if (iLocalParams.mode_intensity == 1) {
                estimate_int_weighted(mask);
            }
            else if (iLocalParams.mode_intensity == 2) {
                estimate_int_clustering(iLocalParams.mode_intensity - 1);// (-1 to correct for old numbering)
            }
        }
        iLocalParams.cl[0] = Math.max(cout, 0);
        iLocalParams.cl[1] = Math.max(0.75 * (firstminval - iIntensityMin) / (iIntensityMax - iIntensityMin), cin);

        if (iLocalParams.mode_intensity == 3)// mode high
        {
            iLocalParams.cl[0] = iLocalParams.betaMLEoutdefault;
            iLocalParams.cl[1] = 1;
            t_high = cin;
        }

        rescaled_min_int_all = Math.max(0, (iScaledIntensityMin - cout) / (cin - cout)); //cin
        if (iLocalParams.debug) {
            IJ.log(aInputRegion.value + "min all " + rescaled_min_int_all);
        }

        iLocalParams.max_nsb = 101;
        iLocalParams.nlevels = 1;
        iLocalParams.RSSinit = false;
        iLocalParams.findregionthresh = false;
        iLocalParams.RSSmodulo = 501;
        iLocalParams.thresh = 0.75;
        iLocalParams.remask = false;
        for (int i = 0; i < iLocalParams.lreg_.length; i++) {
            iLocalParams.lreg_[i] = iLocalParams.lreg_[i] * aOversampling;
        }
    }

    /**
     * Analyse one Patch
     * Or run SplitBregman segmentation solver on it
     */
    @Override
    public void run() {
        final MasksDisplay md = new MasksDisplay(iSizeOversX, iSizeOversY, iSizeOversZ);

        iLocalParams.nthreads = 1;
        iLocalParams.firstphase = false;

        if (iLocalParams.nz > 1) {
            // Check the delta beta, if it is bigger than two ignore it, because
            // I cannot warrant stability
            if (Math.abs(iLocalParams.cl[0] - iLocalParams.cl[1]) > 2.0) {
                // reset
                iLocalParams.cl[0] = iLocalParams.betaMLEoutdefault;
                iLocalParams.cl[1] = iLocalParams.betaMLEindefault;
            }

            A_solver = new ASplitBregmanSolverTwoRegions3DPSF(iLocalParams, iPatch, w3kpatch, md, iChannel, this);// mask instead of w3kpatch
        }
        else {
            // Check the delta beta, if it is bigger than two ignore it, because I cannot warrant stability
            if (Math.abs(iLocalParams.cl[0] - iLocalParams.cl[1]) > 2.0) {
                // reset
                iLocalParams.cl[0] = iLocalParams.betaMLEoutdefault;
                iLocalParams.cl[1] = iLocalParams.betaMLEindefault;
            }

            A_solver = new ASplitBregmanSolverTwoRegionsPSF(iLocalParams, iPatch, w3kpatch, md, iChannel, this);// mask instead of w3kpatch
        }

        try {
            A_solver.first_run();

            cout = iLocalParams.cl[0];
            cin = iLocalParams.cl[1];

            final int ll = iLocalParams.mode_intensity;
            if (ll == 0 || ll == 1) {
                min_thresh = rescaled_min_int_all * 0.96;
            }
            else {
                min_thresh = 0.25;
            }

            if (iLocalParams.debug) {
                IJ.log("region" + iInputRegion.value + "minth " + min_thresh);

            }
            double t = 0;
            if (iLocalParams.mode_intensity != 3) {
                t = find_best_thresh(A_solver.w3kbest[0]);
            }

            if (iLocalParams.mode_intensity == 3)// mode high
            {

                estimate_int_clustering(2);// (-1

                t_high = cin;
                if (iLocalParams.debug) {
                    IJ.log("obj" + iInputRegion.value + " effective t:" + t_high);
                }
                t = t_high - 0.04;
            }

            if (iLocalParams.debug) {
                IJ.log("best thresh : " + t + "region" + iInputRegion.value);
            }
            set_object(A_solver.w3kbest[0], t);
            if (iInterpolationXY > 1) {
                object = createInterpolatedObject(A_solver.w3kbest[0], t);
            }

            // assemble result into full image
            assemble_patch();
        }
        catch (final InterruptedException ex) {
        }
    }

    private void set_object(double[][][] w3kbest, double t) {
        objectFound = false;
        border_attained = false;
        for (int z = 0; z < iSizeOversZ; z++) {
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    if (w3kbest[z][i][j] > t && iRegionMask[z][i][j] == 1) {
                        object[z][i][j] = 1;
                        objectFound = true;
                        if (iSizeOversZ <= 1) {
                            if ((i == 0 && iOffsetOrigX != 0) || (i == (iSizeOversX - 1) && (iOffsetOrigX + iSizeOversX / iOversamplingInXY) != iSizeOrigX) || 
                                (j == 0 && iOffsetOrigY != 0) || (j == (iSizeOversY - 1) && (iOffsetOrigY + iSizeOversY / iOversamplingInXY) != iSizeOrigY)) {
                                border_attained = true;
                            }
                        }
                    }
                    else {
                        object[z][i][j] = 0;
                    }
                }
            }
        }
    }

    /**
     * Compute the geometry of the patch
     *
     * @param aInputRegion Region
     * @param aOversampling level of oversampling
     */
    private void computePatchGeometry(Region aInputRegion, int aOversampling, int aInterpolation, int aMarginXY, int aMarginZ) {
        Pix[] mm = aInputRegion.getMinMaxCoordinates();
        Pix min = mm[0]; Pix max = mm[1];
        int xmin = min.px;
        int ymin = min.py;
        int zmin = min.pz;
        int xmax = max.px;
        int ymax = max.py;
        int zmax = max.pz;
        
        // Adjust patch coordinates with margin and fit it into min/max coordinates
        xmin = Math.max(0, xmin - aMarginXY);
        xmax = Math.min(iSizeOrigX, xmax + aMarginXY + 1);
        ymin = Math.max(0, ymin - aMarginXY);
        ymax = Math.min(iSizeOrigY, ymax + aMarginXY + 1);
        if (iLocalParams.nz > 1) {
            zmin = Math.max(0, zmin - aMarginZ);
            zmax = Math.min(iSizeOrigZ, zmax + aMarginZ + 1);
        }

        iOffsetOrigX = xmin;
        iOffsetOrigY = ymin;
        iOffsetOrigZ = zmin;
        
        iOversamplingInXY = aOversampling;
        iSizeOversX = (xmax - xmin) * iOversamplingInXY;
        iSizeOversY = (ymax - ymin) * iOversamplingInXY;
        
        iInterpolationXY = aInterpolation;
        iSizeOverInterX = iSizeOversX * iInterpolationXY;
        iSizeOverInterY = iSizeOversY * iInterpolationXY;

        iOverInterInXY = iOversamplingInXY * iInterpolationXY;
        
        if (iLocalParams.nz == 1) {
            iOversamplingInZ = 1;
            iSizeOversZ = 1;
            iInterpolationZ = 1;
            iSizeOverInterZ = 1;
            iOverInterInZ = 1;
        }
        else {
            iOversamplingInZ = aOversampling;
            iSizeOversZ = (zmax - zmin) * iOversamplingInZ;
            iInterpolationZ = aInterpolation;
            iSizeOverInterZ = iSizeOversZ * iInterpolationZ;
            iOverInterInZ = iOversamplingInZ * iInterpolationZ;
        }
    }

    private void fill_patch(double[][][] aSourceImage, double[][][] aOutput, double[][][] aWeights, int aOversamplingXY, int aOversamplingZ, int aOffsetX, int aOffsetY, int aOffsetZ) {
        for (int z = 0; z < iSizeOversZ; z++) {
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    // aWeights are set to 0 or 1.
                    aOutput[z][i][j] = aWeights[z][i][j] * aSourceImage[z / aOversamplingZ + aOffsetZ][i / aOversamplingXY + aOffsetX][j / aOversamplingXY + aOffsetY];
                }
            }
        }
    }

    private void estimate_int_weighted(double[][][] mask) {
        RegionStatisticsSolver RSS = new RegionStatisticsSolver(temp1, temp2, temp3, iPatch, iRegionMask, 10, iLocalParams);
        RSS.eval(mask);

        cout = RSS.betaMLEout;
        cout_front = cout;
        cin = RSS.betaMLEin;
        mint = 0.25;

        if (iLocalParams.debug) {
            IJ.log("mint " + mint);
            IJ.log("reg" + iInputRegion.value + "rescaled min int" + iScaledIntensityMin);
            IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));
        }
    }

    private void estimate_int_clustering(int level) {
        // clustering done on original image data (not on soft mask)
        // test doing it on soft mask ? // but original soft mask patch
        // extracted from whole image is not normalized..
        if (iLocalParams.debug) {
            IJ.log("init obj:" + iInputRegion.value);
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
        for (int z = 0; z < iSizeOversZ; z++) {
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    pixel[0] = iPatch[z][i][j];
                    if (level == 2) {
                        pixel[0] = A_solver.w3kbest[0][z][i][j]; // w3kpatch[0]
                    }
                    final Instance instance = new DenseInstance(pixel);
                    if (iRegionMask[z][i][j] == 1) {
                        data.add(instance);
                        cpt_vals++;
                    }
                }
            }
        }

        if (iLocalParams.debug) {
            IJ.log("inst" + iInputRegion.value + " nbvals" + cpt_vals);
        }

        if (cpt_vals > 3) {
            final Clusterer km = new KMeans(nk, 100);
            // Cluster the data, it will be returned as an array of data sets,
            // with each dataset representing a cluster.
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
            cin = Math.max(iScaledIntensityMin, levels[nk2]);// -1;
            final int nkm1 = Math.max(nk2 - 1, 0);
            cout_front = levels[nkm1];
            cout = levels[0];
            if (level == 2) {
                cout = cout_front;
            }

            mint = 0.25;

            if (iLocalParams.debug) {
                IJ.log("mint " + mint);
                IJ.log("rescaled min int" + iScaledIntensityMin);
                IJ.log(String.format("Photometry patch:%n background %7.2e %n foreground %7.2e", cout, cin));
                IJ.log("levels :");
                for (int i = 0; i < nk; i++) {
                    IJ.log("level r" + iInputRegion.value + " " + (i + 1) + " : " + levels[i]);
                }
            }
        }
        else // too few values for clustering..
        {
            cin = 1;
            cout_front = iLocalParams.betaMLEoutdefault;
            cout = iLocalParams.betaMLEoutdefault;
            mint = 0.25;

            if (iLocalParams.debug) {
                IJ.log("usuing default intensity for r" + iInputRegion.value);
            }
        }
    }

    private double[][][] generateMask(Region r, boolean aCheckBoundaries) {
        double[][][] mask = new double[iSizeOversZ][iSizeOversX][iSizeOversY];

        for (final Pix p : r.pixels) {
            int rz = iOversamplingInXY * (p.pz - iOffsetOrigZ);
            int rx = iOversamplingInXY * (p.px - iOffsetOrigX);
            int ry = iOversamplingInXY * (p.py - iOffsetOrigY);
            if (aCheckBoundaries && (rz < 0 || rz + iOversamplingInZ > iSizeOversZ || rx < 0 || rx + iOversamplingInXY > iSizeOversX || ry < 0 || ry + iOversamplingInXY > iSizeOversY)) {
                continue;
            }
            
            for (int z = rz; z < rz + iOversamplingInZ; z++) {
                for (int i = rx; i < rx + iOversamplingInXY; i++) {
                    for (int j = ry; j < ry + iOversamplingInXY; j++) {
                        mask[z][i][j] = 1;
                    }
                }
            }
        }
        return mask;
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

            if (objectFound && !border_attained) {
                double temp;
                if (iLocalParams.nz == 1) {
                    temp = iLocalTools.computeEnergyPSF_weighted(temp1, object, temp2, temp3, iRegionMask, iLocalParams.ldata, iLocalParams.lreg_[iChannel], iLocalParams.PSF, cout_front, cin, iPatch);
                }
                else {
                    temp = iLocalTools.computeEnergyPSF3D_weighted(temp1, object, temp2, temp3, iRegionMask, iLocalParams.ldata, iLocalParams.lreg_[iChannel], iLocalParams.PSF, cout_front, cin, iPatch);
                }

                if (iLocalParams.debug) {
                    IJ.log("energy " + temp + " t " + t + "region" + iInputRegion.value);
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
        double threshold = 0.95;
        double cinbest;
        double coutbest; 
        double cin_previous = cin;
        double cout_previous = cout;

        cinbest = 1;
        coutbest = 0.0001;
        for (double thr = threshold; thr > rescaled_min_int_all * 0.96; thr -= 0.02) {
            set_object(w3kbest, thr);

            if (objectFound && !border_attained) {
                estimate_int_weighted(object);

                double tempEnergy;
                if (iLocalParams.nz == 1) {
                      tempEnergy = iLocalTools.computeEnergyPSF_weighted(temp1, object, temp2, temp3, iRegionMask, iLocalParams.ldata, iLocalParams.lreg_[iChannel], iLocalParams.PSF, cout_front, cin, iPatch);
                }
                else {
                    tempEnergy = iLocalTools.computeEnergyPSF3D_weighted(temp1, object, temp2, temp3, iRegionMask, iLocalParams.ldata, iLocalParams.lreg_[iChannel], iLocalParams.PSF, cout_front, cin, iPatch);
                }

                if (iLocalParams.debug == true) {
                    IJ.log("energy and int " + tempEnergy + " t " + thr + "region" + iInputRegion.value + " cin " + cin + " cout " + cout + " obj " + objectFound);
                }
                if (tempEnergy < energy) {
                    energy = tempEnergy;
                    threshold = thr;
                    cinbest = cin;
                    coutbest = cout;
                }
            }
            else {
                cin = 1;
                cout_front = 0;
            }
        }

        cin = cinbest;
        cout = coutbest;
        cout_front = cout;

        if (!objectFound) {
            cin = cin_previous;
            cout = cout_previous;
        }

        rescaled_min_int_all = Math.max((iScaledIntensityMin - cout) / (cin - cout), 0);
        if (iLocalParams.debug) {
            IJ.log("fbest" + iInputRegion.value + "min all " + rescaled_min_int_all);
        }

        if (iLocalParams.debug) {
            IJ.log(" best energy and int " + energy + " t " + threshold + "region" + iInputRegion.value + " cin " + cin + " cout " + cout);
        }

        return threshold;
    }

    // build local object list
    private void assemble_patch() {
        // find connected regions
        final ImagePlus maska_im = new ImagePlus();
        final ImageStack maska_ims = new ImageStack(iSizeOverInterX, iSizeOverInterY);

        for (int z = 0; z < iSizeOverInterZ; z++) {
            final byte[] maska_bytes = new byte[iSizeOverInterX * iSizeOverInterY];
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    if (object[z][i][j] >= 1) {
                        maska_bytes[j * iSizeOverInterX + i] = (byte) (object[z][i][j]);
                    }
                    else {
                        maska_bytes[j * iSizeOverInterX + i] = (byte) 0;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(iSizeOverInterX, iSizeOverInterY);
            bp.setPixels(maska_bytes);
            maska_ims.addSlice("", bp);
        }

        maska_im.setStack("test Mask vo2", maska_ims);

        final FindConnectedRegions fcr = new FindConnectedRegions(maska_im, iSizeOverInterX, iSizeOverInterY, iSizeOverInterZ);// maska_im only

        final double thr = 0.5;
        final float[][][] Ri = new float[iSizeOverInterZ][iSizeOverInterX][iSizeOverInterY];
        for (int z = 0; z < iSizeOverInterZ; z++) {
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    Ri[z][i][j] = (float) thr;
                }
            }
        }

        fcr.run(thr, iSizeOverInterX * iSizeOverInterY * iSizeOverInterZ, 0 * iOverInterInXY, 0, Ri);// min size was 5

        // add to list with critical section
        iImagePatches.addRegionsToList(fcr.results);

        // add to regions refined with correct indexes
        assemble(fcr.results);
    }

    private void assemble(ArrayList<Region> localList) {
        for (final Region r : localList) {
            final ArrayList<Pix> npixels = new ArrayList<Pix>();
            for (final Pix p : r.pixels) {
                npixels.add(new Pix(p.pz + iOffsetOrigZ * iOverInterInZ, p.px + iOffsetOrigX * iOverInterInXY, p.py + iOffsetOrigY * iOverInterInXY));
            }
            r.pixels = npixels;
        }
    }

    private double[][][] createInterpolatedObject(double[][][] aInputData, double aThreshold) {
        // Build non interpolated image
        ImageStack imgStack = new ImageStack(iSizeOversX, iSizeOversY);
        for (int z = 0; z < iSizeOversZ; z++) {
            final float[] pixels = new float[iSizeOversX * iSizeOversY];
            for (int i = 0; i < iSizeOversX; i++) {
                for (int j = 0; j < iSizeOversY; j++) {
                    pixels[j * iSizeOversX + i] = (float) aInputData[z][i][j];
                }
            }
            final FloatProcessor fp = new FloatProcessor(iSizeOversX, iSizeOversY, pixels);
            imgStack.addSlice("", fp);
        }
        ImagePlus interpolatedImg = new ImagePlus("Object x", imgStack);
        
        // Interpolate in Z and XY planes
        if (iSizeOverInterZ != iSizeOversZ) {
            interpolatedImg = new Resizer().zScale(interpolatedImg, iSizeOverInterZ, ImageProcessor.BILINEAR);
        }
        final ImageStack imgStackInter = new ImageStack(iSizeOverInterX, iSizeOverInterY);
        for (int z = 0; z < iSizeOverInterZ; z++) {
            interpolatedImg.setSliceWithoutUpdate(z + 1);
            interpolatedImg.getProcessor().setInterpolationMethod(ImageProcessor.BILINEAR);
            imgStackInter.addSlice("", interpolatedImg.getProcessor().resize(iSizeOverInterX, iSizeOverInterY, true));
        }
        interpolatedImg.setStack(imgStackInter);

        // put thresholded data into interpolated object 
        double[][][] interpolated_object = new double[iSizeOverInterZ][iSizeOverInterX][iSizeOverInterY];
        for (int z = 0; z < iSizeOverInterZ; z++) {
            interpolatedImg.setSlice(z + 1);
            ImageProcessor imp = interpolatedImg.getProcessor();
            for (int i = 0; i < iSizeOverInterX; i++) {
                for (int j = 0; j < iSizeOverInterY; j++) {
                    double value = imp.getPixelValue(i, j);
                    if (value > aThreshold && iRegionMask[z / iInterpolationZ][i / iInterpolationXY][j / iInterpolationXY] == 1) {
                        interpolated_object[z][i][j] = 1;
                    }
                    else {
                        interpolated_object[z][i][j] = 0;
                    }
                }
            }
        }

        return interpolated_object;
    }
}
