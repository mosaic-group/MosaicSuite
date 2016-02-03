package mosaic.bregman.segmentation;


import java.util.ArrayList;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
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
    public ArrayList<Region> iRegionsList;
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

        // threshold should not be = 1: creates empty mask and wrong behavior in dct3D computation
        double maskThreshold = (iParameters.defaultBetaMleIn == 1) ? 0.5 : iParameters.defaultBetaMleIn;
        iMask = createMask(iImage, maskThreshold);
        
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
        computeConnectedRegions(iSolver.w3kbest);
        
        // refinement
        computeVoronoiRegions(iSolver.w3kbest);
        IJ.showStatus("Computing segmentation 55%");
        IJ.showProgress(0.55);
    
        final ImagePatches ipatches = new ImagePatches(iParameters, iRegionsList, iImage, iSolver.w3kbest, iGlobalMin, iGlobalMax, iParameters.regularization, iParameters.minObjectIntensity, iPsf);
        ipatches.distributeRegions();
        iRegionsList = ipatches.getRegionsList();
        regions = ipatches.getRegions();
        
        // Here we solved the patches and the regions that come from the patches
        // we rescale the intensity to the original one
        for (final Region r : iRegionsList) {
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
    
    private double[][][] createMask(double[][][] aInputImage, double aMaskThreshold) {
        double[][][] result = new double[nz][ni][nj];
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (aInputImage[z][i][j] >= aMaskThreshold) {
                        result[z][i][j] = 1;
                    }
                }
            }
        }
        
        return result;
    }

    private void computeConnectedRegions(double[][][] aMask) {
        final ImageStack maskStack = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final byte[] mask_bytes = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_bytes[j * ni + i] = (byte)(255 * aMask[z][i][j]);
                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj, mask_bytes);
            maskStack.addSlice("", bp);
        }
        final ImagePlus maskImg = new ImagePlus("", maskStack);
        
        final FindConnectedRegions fcr = new FindConnectedRegions(maskImg);
        fcr.run(-1 /* no maximum size */, iParameters.minRegionSize, (float) (255 * iParameters.minObjectIntensity), iParameters.excludeEdgesZ, 1, 1);
        
        regions = fcr.getLabeledRegions();
        iRegionsList = fcr.getFoundRegions();
    }
    
    private void computeVoronoiRegions(double[][][] mask) {
        ImagePlus maskImg = ImgUtils.ZXYarrayToImg(mask);
        
        // project mask on single slice (maximum values)
        final ZProjector proj = new ZProjector(maskImg);
        proj.setImage(maskImg);
        proj.setStartSlice(1);
        proj.setStopSlice(nz);
        proj.setMethod(ZProjector.MAX_METHOD);
        proj.doProjection();
        maskImg = proj.getProjection();
        IJ.showStatus("Computing segmentation 52%");
        IJ.showProgress(0.52);

        // threshold mask
        ImageProcessor imp = maskImg.getProcessor();
        final byte[] mask_bytes = new byte[ni * nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                // weird conversion to have same thing than in find connected regions
                if ( (int)(255 * imp.getPixelValue(i, j)) <= (int)(255 * iParameters.minObjectIntensity) ) {
                    mask_bytes[j * ni + i] = (byte) 255;
                }
            }
        }
        final ByteProcessor bp = new ByteProcessor(ni, nj, mask_bytes);
        maskImg.setProcessor("Mask Thresholded", bp);

        // do voronoi in 2D on Z projection
        // Here we compute the Voronoi segmentation starting from the threshold mask
        final EDM filtEDM = new EDM();
        filtEDM.setup("voronoi", maskImg);
        filtEDM.run(maskImg.getProcessor());
        maskImg.getProcessor().invert();
        IJ.showStatus("Computing segmentation 53%");
        IJ.showProgress(0.53);

        // expand Voronoi in 3D
        ImageProcessor impVoronoi = maskImg.getProcessor();
        final byte[] maskBytesVoronoi = new byte[ni * nj];
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                maskBytesVoronoi[j * ni + i] = (byte) impVoronoi.getPixelValue(i, j);
            }
        }
        final ImageStack imgStackVoronoi = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            imgStackVoronoi.addSlice("", new ByteProcessor(ni, nj, maskBytesVoronoi.clone()));
        }
        maskImg.setStack("Voronoi", imgStackVoronoi);

        // Here we are elaborating the Voronoi mask to get a nice subdivision
        final FindConnectedRegions fcr = new FindConnectedRegions(maskImg);
        fcr.run(ni * nj * nz, 0, /* threshold > 254 = 255 */ 254f, iParameters.excludeEdgesZ, 1, 1);
        ArrayList<Region> regionsvoronoi = fcr.getFoundRegions();
        
        // use Ri to store voronoi regions indices
        float[][][] Ri = new float[nz][ni][nj];
        ArrayOps.fill(Ri, 255);
        for (final Region r : regionsvoronoi) {
            for (final Pix p : r.pixels) {
                Ri[p.pz][p.px][p.py] = regionsvoronoi.indexOf(r);
            }
        }
        IJ.showStatus("Computing segmentation 54%");
        IJ.showProgress(0.54);
        
        setRegionsObjsVoronoi(iRegionsList, regionsvoronoi, Ri);
    }

    private void setRegionsObjsVoronoi(ArrayList<Region> aRegionsList, ArrayList<Region> aRegionsVoronoi, float[][][] aRi) {
        for (Region r : aRegionsList) {
            final Pix pixel = r.pixels.get(0);
            int x = pixel.px;
            int y = pixel.py;
            int z = pixel.pz;
            r.rvoronoi = aRegionsVoronoi.get((int) aRi[z][x][y]);
        }
    }
}
