package mosaic.bregman.segmentation;


import java.util.ArrayList;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import mosaic.core.psf.GaussPSF;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.ImgUtils;
import net.imglib2.type.numeric.real.DoubleType;


public class SquasshSegmentation {
    private static final Logger logger = Logger.getLogger(SquasshSegmentation.class);
    
    // Input parameters
    private final SegmentationParameters iParameters;
    private final double iGlobalMin;
    private final double iGlobalMax;
    
    // Internal data
    private final int ni, nj, nz;
    private final double[][][] iImage;
    private final double[][][] iMask;
    private final psf<DoubleType> iPsf;
    private final ASplitBregmanSolver iSolver;
    
    // Output of segmentation
    // TODO: Make it accessible via getters
    public short[][][] regions;
    public ArrayList<Region> regionsList;
    public ImagePlus out_soft_mask;
    
    
    public SquasshSegmentation(double[][][] aInputImg, SegmentationParameters aParameters, double aGlobalMin, double aGlobalMax) {
        logger.debug(aParameters);
        
        iParameters = aParameters;
        iGlobalMin = aGlobalMin;
        iGlobalMax = aGlobalMax;
        
        ni = aInputImg[0].length;
        nj = aInputImg[0][0].length;
        nz = aInputImg.length;

        iImage = new double[nz][ni][nj];
        ArrayOps.normalize(aInputImg, iImage, iGlobalMin, iGlobalMax);

        iMask = new double[nz][ni][nj];
        createmask(iMask, iImage, iParameters.defaultBetaMleIn);
        
        iPsf = generatePsf();
        
        iSolver = (nz > 1) 
                ? new ASplitBregmanSolverTwoRegions3DPSF(iParameters, iImage, iMask, null, iParameters.defaultBetaMleOut, iParameters.defaultBetaMleIn, iParameters.regularization, iParameters.minObjectIntensity, iPsf)
                : new ASplitBregmanSolverTwoRegions2DPSF(iParameters, iImage, iMask, null, iParameters.defaultBetaMleOut, iParameters.defaultBetaMleIn, iParameters.regularization, iParameters.minObjectIntensity, iPsf);
    }

    public void run() {        
        stepOneFromImage();
        stepTwoSegmentation();
    }
    
    public void runWithProvidedMask(double[][][] aMask) {        
        stepOneFromPatches(aMask);
        stepTwoSegmentation();
    }

    private void stepOneFromImage() {
        try {
            IJ.showStatus("Computing segmentation 0%");
            IJ.showProgress(0.0);
            iSolver.first_run();
        }
        catch (final InterruptedException ex) {
        }
    }

    private void stepOneFromPatches(double[][][] aInputMask) {
        Tools.copytab(iSolver.w3kbest, aInputMask);
    }

    private void stepTwoSegmentation() {
        out_soft_mask = ImgUtils.ZXYarrayToImg(iSolver.w3k);
    
        // ========================      Compute segmentation
        compute_connected_regions(iSolver.w3kbest);
    
        // refinement
        iSolver.regions_intensity_findthresh(iSolver.w3kbest);
        SetRegionsObjsVoronoi(regionsList, iSolver.regionsvoronoi, iSolver.Ri);
        IJ.showStatus("Computing segmentation 55%");
        IJ.showProgress(0.55);
    
        final ImagePatches ipatches = new ImagePatches(iParameters, regionsList, iImage, iSolver.w3kbest, iGlobalMin, iGlobalMax, iParameters.regularization, iParameters.minObjectIntensity, iPsf);
        ipatches.distributeRegions();
        regionsList = ipatches.getRegionsList();
        regions = ipatches.getRegions();
        
        // Here we solved the patches and the regions that come from the patches
        // we rescale the intensity to the original one
        for (final Region r : regionsList) {
            r.intensity = r.intensity * (iGlobalMax - iGlobalMin) + iGlobalMin;
        }
    }

    private GaussPSF<DoubleType> generatePsf() {
        int psfDims = (nz > 1) ? 3 : 2;
        final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(psfDims, DoubleType.class);
        final DoubleType[] var = new DoubleType[psfDims];
        var[0] = new DoubleType(iParameters.sigmaGaussianXY);
        var[1] = new DoubleType(iParameters.sigmaGaussianXY);
        if (psfDims == 3) var[2] = new DoubleType(iParameters.sigmaGaussianZ);
        psf.setStdDeviation(var);
        // Prepare PSF for further use (It prevents from multi-threading problems so it must be like that).
        psf.getSeparableImageAsDoubleArray(0);
        return psf;
    }
    
    private void createmask(double[][][] res, double[][][] image, double thr) {
        if (thr == 1) {
            // should not have threhold == 1: creates empty mask and wrong behavior in dct3D  computation
            thr = 0.5;
        }
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (image[z][i][j] >= thr) {
                        res[z][i][j] = 1;
                    }
                    else {
                        res[z][i][j] = 0;
                    }
                }
            }
        }
    }

    private void SetRegionsObjsVoronoi(ArrayList<Region> regionlist, ArrayList<Region> regionsvoronoi, float[][][] ri) {
        for (Region r : regionlist) {
            int x = r.pixels.get(0).px;
            int y = r.pixels.get(0).py;
            int z = r.pixels.get(0).pz;
            r.rvoronoi = regionsvoronoi.get((int) ri[z][x][y]);
        }
    }
    
    private void compute_connected_regions(double[][][] mask) {
        int ni = mask[0].length;
        int nj = mask[0][0].length;
        int nz = mask.length;
        byte[][][] maskA = new byte[nz][ni][nj];
        copyScaledMask(maskA, mask);

        final FindConnectedRegions fcr = processConnectedRegions(iParameters.minObjectIntensity, maskA);
        regions = fcr.getLabeledRegions();
        regionsList = fcr.getFoundRegions();
    }

    private FindConnectedRegions processConnectedRegions(double intensity, byte[][][] mask) {
        int ni = mask[0].length;
        int nj = mask[0][0].length;
        int nz = mask.length;
        final ImagePlus mask_im = new ImagePlus();
        final ImageStack mask_ims = new ImageStack(ni, nj);

        for (int z = 0; z < nz; z++) {
            final byte[] mask_bytes = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_bytes[j * ni + i] = mask[z][i][j];
                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(mask_bytes);
            mask_ims.addSlice("", bp);
        }
        mask_im.setStack("", mask_ims);
        final FindConnectedRegions fcr = new FindConnectedRegions(mask_im);
        fcr.run(-1 /* no maximum size */, iParameters.minRegionSize, (float) (255 * intensity), iParameters.excludeEdgesZ, 1, 1);
        
        return fcr;
    }
    
    private void copyScaledMask(byte[][][] aDestination, double[][][] aSource) {
        int ni = aSource[0].length;
        int nj = aSource[0][0].length;
        int nz = aSource.length;
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    aDestination[z][i][j] = (byte) ((int) (255 * aSource[z][i][j]));
                }
            }
        }
    }
}
