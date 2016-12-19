package mosaic.bregman.segmentation;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mosaic.bregman.solver.ASplitBregmanSolver;
import mosaic.bregman.solver.SolverParameters;
import mosaic.core.psf.GaussPSF;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.ConvertArray;
import mosaic.utils.Debug;
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
    public short[][][] iLabeledRegions;
    public ArrayList<Region> iRegionsList;
    public double[][][] iSoftMask;
    public final List<float[][][]> iAllMasks = new ArrayList<float[][][]>();
    
    public SquasshSegmentation(double[][][] aInputImg, SegmentationParameters aParameters, double aGlobalMin, double aGlobalMax) {
        logger.debug(aParameters);
        logger.debug("Input Image dimensions (z/x/y):" + Debug.getArrayDims(aInputImg));
        logger.debug("Global min/max: " + aGlobalMin + " / " + aGlobalMax);
        
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
        // Test for quite successful mask init. It is close to original so it is still room for calculating gradients etc. 
        // But because it is "almost there" it is quite fast.
//        ArrayOps.normalize(aInputImg, iImage, iGlobalMin*0.9, iGlobalMax*1.1);
        iPsf = generatePsf();
        
        SolverParameters solverParams = new SolverParameters(iParameters.numOfThreads, 
                                                             SolverParameters.NoiseModel.valueOf(iParameters.noiseModel.name()),
                                                             iParameters.defaultBetaMleIn, 
                                                             iParameters.defaultBetaMleOut, 
                                                             iParameters.lambdaRegularization);
        iSolver = ASplitBregmanSolver.create(solverParams, iImage, iMask, iPsf);
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
        setProgress(0);
        
        final int numOfIterations = 151;
        
        boolean isDone = false;
        int iteration = 0;
        while (iteration < numOfIterations && !isDone) {
            final boolean lastIteration = (iteration == numOfIterations - 1);
            isDone = iSolver.performIteration(lastIteration);
            if (iParameters.debug) iAllMasks.add(ConvertArray.toFloat(iSolver.w3k));
            // Will progress  from 0 to 50 percent for step one.
            setProgress((50 * iteration)/(numOfIterations - 1));
            iteration++;
        }
        iSolver.postprocess();
    }

    private void stepOneFromPatches(double[][][] aInputMask) {
        SegmentationTools.copytab(iSolver.w3kbest, aInputMask);
    }

    private void stepTwoSegmentation() {
        iSoftMask = iSolver.w3kbest.clone();
        computeConnectedRegions(iSolver.w3kbest);
        
        setProgress(51);
    
        computeVoronoiRegions();
        final ImagePatches ipatches = new ImagePatches(iParameters, iRegionsList, iImage, iSolver.w3kbest, iGlobalMin, iGlobalMax, iParameters.lambdaRegularization, iParameters.minObjectIntensity, iPsf);
        ipatches.processPatches();
        iRegionsList = ipatches.getRegionsList();
        iLabeledRegions = ipatches.getLabeledRegions();
        
        relabelRegions(iRegionsList, iLabeledRegions);
    }
    
    private void relabelRegions(ArrayList<Region> aRegionsList, short[][][] aLabeledRegions) {
        short label = 1;
        for (Region r : aRegionsList) {
            for (Pix p : r.iPixels) {
                aLabeledRegions[p.pz][p.px][p.py] = label;
            }
            r.iLabel = label;
            label++;
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
        final ImagePlus maskImg = new ImagePlus("ConnectedRegions", maskStack);
        
        final FindConnectedRegions fcr = new FindConnectedRegions(maskImg);
        fcr.run(-1 /* no maximum size */, iParameters.minRegionSize, (float) (255 * iParameters.minObjectIntensity), iParameters.excludeEdgesZ, 1, 1);

        iLabeledRegions = fcr.getLabeledRegions();
        iRegionsList = fcr.getFoundRegions();
        if (iParameters.debug) {
            ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(iLabeledRegions), "iLabeledRegions").show();
        }
    }
    
    private void computeVoronoiRegions() {
        // project regions on one plane (Z-projection) - Voronoi is done only in 2D
        final byte[] mask_bytes = new byte[ni * nj];
        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    // Found regions have label in range 1 - ...
                    if (iLabeledRegions[z][i][j] > 0) {
                        mask_bytes[j * ni + i] = (byte) 255;
                    }
                }
            }
        }
        final ByteProcessor bp = new ByteProcessor(ni, nj, mask_bytes); bp.invert();
        ImagePlus maskImg = new ImagePlus("Mask Thresholded", bp);
        
        // do voronoi in 2D on Z projection
        // Here we compute the Voronoi segmentation starting from the threshold mask
        
        // EDM is using Prefs.blackBackground global setting. We need false here.
        boolean tempBlackBackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        final EDM filtEDM = new EDM();
        filtEDM.setup("voronoi", maskImg);
        filtEDM.run(maskImg.getProcessor());
        ij.Prefs.blackBackground = tempBlackBackground;

        maskImg.getProcessor().invert();
        
        setProgress(53);
        
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
        
        // Here we are elaborating the Voronoi mask to get a nice subdivision. After previous commands Voronoi regions have value 255.
        final FindConnectedRegions fcr = new FindConnectedRegions(maskImg);
        fcr.run(ni * nj * nz, 0, 255f, iParameters.excludeEdgesZ, 1, 1);
        ArrayList<Region> regionsvoronoi = fcr.getFoundRegions();
        logger.debug("NumOfRegions - connected regions / voronoi: " + iRegionsList.size() + " / " + regionsvoronoi.size());
        setRegionsObjsVoronoi(iRegionsList, regionsvoronoi);
        
        setProgress(54);
    }

    private void setProgress(int aPercent) {
        logger.debug("Segmentation progress: " + aPercent);
        IJ.showStatus("Computing segmentation " + aPercent + "%");
        IJ.showProgress(aPercent / 100.0);
    }
    
    private void setRegionsObjsVoronoi(ArrayList<Region> aRegionsList, ArrayList<Region> aRegionsVoronoi) {
        // use Ri to store voronoi regions indices
        float[][][] voronoiIndexMap = new float[nz][ni][nj];
        ArrayOps.fill(voronoiIndexMap, 255);
        for (final Region r : aRegionsVoronoi) {
            final int index = aRegionsVoronoi.indexOf(r);
            for (final Pix p : r.iPixels) {
                voronoiIndexMap[p.pz][p.px][p.py] = index;
            }
        }
        
        if (iParameters.debug) {
            ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(voronoiIndexMap), "voronoiIndexMap").show();
        }
        
        // Find voronoi for each region
        for (Region r : aRegionsList) {
            final Pix pixel = r.iPixels.get(0);
            int x = pixel.px;
            int y = pixel.py;
            int z = pixel.pz;
            r.rvoronoi = aRegionsVoronoi.get((int) voronoiIndexMap[z][x][y]);
        }
    }
}
