package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import mosaic.core.detection.Particle;
import mosaic.core.psf.GaussPSF;
import mosaic.core.utils.LabelImage;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.core.utils.SphereMask;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;
import net.imglib2.type.numeric.real.DoubleType;


/**
 * Class that process the first Split bregman segmentation and refine with patches
 *
 * @author Aurelien Ritz
 */

class TwoRegions extends NRegions {

    double[][][][] SpeedData;

    public TwoRegions(ImagePlus img, Parameters params, CountDownLatch DoneSignal, int channel) {
        super(img, params, DoneSignal, channel);

        if (p.nlevels > 1 || !p.usePSF) {
            // save memory when Ei not needed
            SpeedData = new double[1][nz][ni][nj];// only one level used

            for (int z = 0; z < nz; z++) {
                for (int i = 0; i < ni; i++) {
                    for (int j = 0; j < nj; j++) {
                        SpeedData[0][z][i][j] = Ei[1][z][i][j] - Ei[0][z][i][j];
                    }
                }
            }
        }
        else {
            SpeedData = null;
            // Tools.disp_vals(SpeedData[1][0], "speedData");
            // Tools.disp_vals(mask[1][0], "mask");
        }

    }

    /**
     * Create a sphere of radius r, used to force patches around the spheres that you draw
     *
     * @param out image
     * @param pt vector of particles
     * @param radius of the sphere
     */

    private void drawParticles(double[][][] out, double[][][] mask, Vector<Particle> pt, int radius) {
        // Iterate on all particles

        int sz[] = new int[3];
        sz[1] = out[0][0].length;
        sz[0] = out[0].length;
        sz[2] = out.length;

        // Create a circle Mask and an iterator

        SphereMask cm = new SphereMask(radius, 2 * radius + 1, 3);
        RegionIteratorMask rg_m = new RegionIteratorMask(cm, sz);

        Iterator<Particle> pt_it = pt.iterator();

        while (pt_it.hasNext()) {
            Particle ptt = pt_it.next();

            // Draw the sphere

            Point p_c = new Point((int) (ptt.x), (int) (ptt.y), (int) (ptt.z));

            rg_m.setMidPoint(p_c);

            while (rg_m.hasNext()) {
                Point p = rg_m.nextP();

                if (p.x[0] < sz[0] && p.x[0] >= 0 && p.x[1] < sz[1] && p.x[1] >= 0 && p.x[2] < sz[2] && p.x[2] >= 0) {
                    out[p.x[2]][p.x[0]][p.x[1]] = 255.0f;
                    mask[p.x[2]][p.x[0]][p.x[1]] = 1.0f;
                }
            }
        }
    }

    public ImagePlus out_soft_mask[] = new ImagePlus[2];

    /**
     * Get the particles related to one frame
     *
     * @param part particle vector
     * @param frame frame number
     * @return a vector with particles related to one frame
     */

    private Vector<Particle> getPart(Vector<Particle> part, int frame) {
        Vector<Particle> pp = new Vector<Particle>();

        // get the particle related to one frame

        for (int i = 0; i < part.size(); i++) {
            if (part.get(i).getFrame() == frame) {
                pp.add(part.get(i));
            }
        }

        return pp;
    }

    /**
     * Run the split Bregman + patch refinement
     */

    @Override
    public void run() {
        // p.nlevels=1;//create only one region not 2 : nz-1

        // This store the output mask
        md = new MasksDisplay(ni, nj, nz, nl, p.cl, p);

        ASplitBregmanSolver A_solver = null;
        // TODO save test
        p.cl[0] = p.betaMLEoutdefault;
        // p.cl[1]=0.2340026;
        // p.cl[1]=0.2;
        p.cl[1] = p.betaMLEindefault;

        // p.cl[0]=0.006857039757524;;
        // p.cl[1]=0.709785769586498;
        p.nlevels = 1;

        // IJ.log(String.format("Photometry default:%n backgroung %7.2e %n foreground %7.2e", p.cl[0],p.cl[1]));
        // Tools.showmem();
        if (p.usePSF && p.nz > 1) {
            // Tools.gaussian3Dbis(p.PSF, p.kernelx, p.kernely, p.kernelz,(int)(p.sigma_gaussian*8.0), p.sigma_gaussian*p.model_oversampling, p.zcorrec);

            GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(3, DoubleType.class);
            DoubleType[] var = new DoubleType[3];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            var[2] = new DoubleType(p.sigma_gaussian / p.zcorrec);
            psf.setVar(var);
            p.PSF = psf;

            A_solver = new ASplitBregmanSolverTwoRegions3DPSF(p, image, SpeedData, mask, md, channel, null);
        }
        else if (p.usePSF && p.nz == 1) {

            // Tools.gaussian2D(p.PSF[0], p.kernelx, p.kernely, 7, 0.8);
            // Tools.gaussian2D(p.PSF[0], p.kernelx, p.kernely, (int)(p.sigma_gaussian*8.0), p.sigma_gaussian*p.model_oversampling);
            // Tools.disp_valsc(p.PSF[1], "PSF computed 1");

            GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(2, DoubleType.class);
            DoubleType[] var = new DoubleType[2];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            psf.setVar(var);
            p.PSF = psf;

            A_solver = new ASplitBregmanSolverTwoRegionsPSF(p, image, SpeedData, mask, md, channel, null);

        }
        else if (!p.usePSF && p.nz > 1) {
            // Tools.gaussian3D(p.PSF, p.kernelx, p.kernely,p.kernelz, 7, 1);
            A_solver = new ASplitBregmanSolverTwoRegions3D(p, image, SpeedData, mask, md, channel, null);
        }
        else {
            A_solver = new ASplitBregmanSolverTwoRegions(p, image, SpeedData, mask, md, channel, null);
        }

        if (Analysis.p.patches_from_file == null) {
            try {
                // Tools.showmem();
                A_solver.first_run();
                // Tools.showmem();
            }
            catch (InterruptedException ex) {
            }
        }
        else {
            // Here we have patches
            // Load particles

            Vector<Particle> pt;

            CSV<Particle> csv = new CSV<Particle>(Particle.class);

            csv.setCSVPreferenceFromFile(Analysis.p.patches_from_file);
            pt = csv.Read(Analysis.p.patches_from_file, new CsvColumnConfig(Particle.ParticleDetection_map, Particle.ParticleDetectionCellProcessor));

            // Get the particle related inly to one frames

            Vector<Particle> pt_f = getPart(pt, Analysis.frame - 1);

            // create a mask Image

            double img[][][] = new double[p.nz][p.ni][p.nj];

            drawParticles(img, A_solver.w3kbest[0], pt_f, (int) 3.0);

            // Tools.disp_array3D_new(img, "particles");

            A_solver.regions_intensity_findthresh(img);
        }

        mergeSoftMask(A_solver);

        if (channel == 0) {
            // Take the soft membership mask

            Analysis.setMaskaTworegions(A_solver.w3kbest[0]);
            // Analysis.setMaskaTworegions(A_solver.w3kbest[0],A_solver.bp_watermask);

            // A_solver A
            float[][][] RiN;
            RiN = new float[p.nz][p.ni][p.nj];
            LocalTools.copytab(RiN, A_solver.Ri[0]);
            float[][][] RoN;
            RoN = new float[p.nz][p.ni][p.nj];

            LocalTools.copytab(RoN, A_solver.Ro[0]);

            ArrayList<Region> regions = A_solver.regionsvoronoi;

            // A_solver=null; //for testing

            if (!Analysis.p.looptest) {
                if (p.findregionthresh) {
                    Analysis.compute_connected_regions_a(255 * p.thresh, RiN);
                }
                else {
                    Analysis.compute_connected_regions_a(255 * p.thresh, null);
                }
                // A_solver=null; // for testing
                // test
                // IJ.log("start test" + "nlevels " +p.nlevels);
                if (Analysis.p.refinement && Analysis.p.mode_voronoi2) {
                    Analysis.setregionsThresholds(Analysis.regionslist[0], RiN, RoN);
                    Analysis.SetRegionsObjsVoronoi(Analysis.regionslist[0], regions, RiN);
                    IJ.showStatus("Computing segmentation  " + 55 + "%");
                    IJ.showProgress(0.55);

                    // Tools.showmem();

                    ImagePatches ipatches = new ImagePatches(p, Analysis.regionslist[0], image, channel, A_solver.w3kbest[0], min, max);
                    A_solver = null;
                    ipatches.run();
                    Analysis.regionslist[0] = ipatches.regionslist_refined;
                    Analysis.regions[0] = ipatches.regions_refined;
                }

                if (Analysis.p.refinement && Analysis.p.mode_classic && A_solver != null) {
                    ImagePatches ipatches = new ImagePatches(p, Analysis.regionslist[0], image, channel, A_solver.w3kbest[0], min, max);
                    A_solver = null;
                    ipatches.run();
                    Analysis.regionslist[0] = ipatches.regionslist_refined;
                    Analysis.regions[0] = ipatches.regions_refined;
                }

                // Here we solved the patches and the regions that come from the patches
                // we rescale the intensity to the original one

                for (Region r : Analysis.regionslist[0]) {
                    r.intensity = r.intensity * (max - min) + min;
                }

                // Well we did not finished yet at this stage you can have several artifact produced by the patches
                // for example one region can be segmented partially by two patches, this mean that at least in theory
                // you should repatch (this produce a finer decomposition) again and rerun the second stage until each
                // patches has one and only one region.
                // The method eventually converge because it always produce finer decomposition, in the opposite case you stop
                // The actual patching algorithm use
                // A first phase Split-Bregman segmentation + Threasholding + Voronoi (unfortunately only 2D, for 3D a 2D
                // Maximum projection is computed)
                // The 2D Maximal projection unfortunately complicate
                // even more the things, and produce even more artefatcs in particular for big PSF with big margins
                // patch.
                //
                // IMPROVEMENT:
                //
                // 1) 3D Voronoi (ImageLib2 Voronoi like segmentation)
                // 2) Good result has been achieved using Particle tracker for Patch positioning.
                // 3) Smart patches given the first phase we cut the soft membership each cut produce a segmentation
                // that include the previous one, going to zero this produce a tree graph. the patches are positioned
                // on the leaf ( other algorithms can be implemented to analyze this graph ... )
                //
                // (Temporarily we fix in this way)
                // Save the old intensity label image as an hashmap (to save memory)
                // run find connected region to recompute the regions again
                // recompute the statistics using the old intensity label image

                HashMap<Integer, Float> lblInt = new HashMap<Integer, Float>();

                for (Region r : Analysis.regionslist[0]) {
                    for (Pix p : r.pixels) {
                        Integer id = p.px + p.py * ni + p.pz * ni * nj;
                        lblInt.put(id, Float.valueOf((float) r.intensity));
                    }
                }

                // we run find connected regions

                LabelImage img = new LabelImage(Analysis.regions[0]);
                img.connectedComponents();

                HashMap<Integer, Region> r_list = new HashMap<Integer, Region>();

                // Run on all pixels of the label to add pixels to the regions

                RegionIterator rit = new RegionIterator(img.getDimensions());
                while (rit.hasNext()) {
                    rit.next();
                    Point p = rit.getPoint();
                    int lbl = img.getLabel(p);
                    if (lbl != 0) {
                        // foreground

                        Region r = r_list.get(lbl);
                        if (r == null) {
                            r = new Region(lbl, 0);
                            r.pixels.add(new Pix(p.x[2], p.x[0], p.x[1]));
                            r_list.put(lbl, r);
                        }
                        else {
                            r.pixels.add(new Pix(p.x[2], p.x[0], p.x[1]));
                        }
                    }
                }

                // Now we run Object properties on this regions list

                int osxy = p.oversampling2ndstep * p.interpolation;
                int sx = p.ni * p.oversampling2ndstep * p.interpolation;
                int sy = p.nj * p.oversampling2ndstep * p.interpolation;
                int sz = 1;
                int osz = 1;
                // IJ.log("sx " + sx);
                if (p.nz == 1) {
                    sz = 1;
                    osz = 1;
                }
                else {
                    sz = p.nz * p.oversampling2ndstep * p.interpolation;
                    osz = p.oversampling2ndstep * p.interpolation;
                }

                ImagePatches.assemble(r_list.values(), Analysis.regions[0]);

                for (Region r : r_list.values()) {
                    ObjectProperties obj = new ObjectProperties(image, r, sx, sy, sz, p, osxy, osz, null, Analysis.regions[0]);
                    obj.run();
                }

            }
            // else
            // Analysis.A_solverX=A_solver; // add for loop settings
        }
        else {
            Analysis.setMaskbTworegions(A_solver.w3kbest[0]);
            // Analysis.setMaskaTworegions(A_solver.w3kbest[0],A_solver.bp_watermask);

            // A_solver A
            float[][][] RiN;
            RiN = new float[p.nz][p.ni][p.nj];
            LocalTools.copytab(RiN, A_solver.Ri[0]);
            float[][][] RoN;
            RoN = new float[p.nz][p.ni][p.nj];

            LocalTools.copytab(RoN, A_solver.Ro[0]);

            ArrayList<Region> regions = A_solver.regionsvoronoi;

            // A_solver=null;

            if (!Analysis.p.looptest) {
                if (p.findregionthresh) {
                    Analysis.compute_connected_regions_b(255 * p.thresh, RiN);
                }
                else {
                    Analysis.compute_connected_regions_b(255 * p.thresh, null);
                    // A_solver=null;
                }

                if (Analysis.p.refinement && Analysis.p.mode_voronoi2) {
                    Analysis.setregionsThresholds(Analysis.regionslist[1], RiN, RoN);
                    Analysis.SetRegionsObjsVoronoi(Analysis.regionslist[1], regions, RiN);
                    IJ.showStatus("Computing segmentation  " + 55 + "%");
                    IJ.showProgress(0.55);

                    ImagePatches ipatches = new ImagePatches(p, Analysis.regionslist[1], image, channel, A_solver.w3kbest[0], min, max);
                    A_solver = null;
                    ipatches.run();
                    Analysis.regionslist[1] = ipatches.regionslist_refined;
                    Analysis.regions[1] = ipatches.regions_refined;
                }

                if (Analysis.p.refinement && Analysis.p.mode_classic && A_solver != null) {
                    ImagePatches ipatches = new ImagePatches(p, Analysis.regionslist[1], image, channel, A_solver.w3kbest[0], min, max);
                    A_solver = null;
                    ipatches.run();
                    Analysis.regionslist[1] = ipatches.regionslist_refined;
                    Analysis.regions[1] = ipatches.regions_refined;
                }

                // Here we solved the patches and the regions that come from the patches
                // we rescale the intensity to the original one

                for (Region r : Analysis.regionslist[1]) {
                    r.intensity = r.intensity * (max - min) + min;
                }

            }
        }

        // correct the level number
        p.nlevels = 2;
        DoneSignal.countDown();

    }

    /**
     * Merge the soft mask
     *
     * @param A_solver the solver used to produce the soft mask
     */

    private void mergeSoftMask(ASplitBregmanSolver A_solver) {
        if (p.dispSoftMask) {
            if (p.nz > 1) {
                out_soft_mask[channel] = md.display2regions3Dnew(A_solver.w3k[channel], "Mask", channel, false);
            }
            else {
                out_soft_mask[channel] = md.display2regionsnew(A_solver.w3k[channel][0], "Mask", channel, false);
            }
        }
    }

}
