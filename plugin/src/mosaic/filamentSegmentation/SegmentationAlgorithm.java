package mosaic.filamentSegmentation;

import static mosaic.filamentSegmentation.SegmentationFunctions.calculateBSplinePoints;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateDiffDirac;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateDirac;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateHeavySide;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateRegularizerEnergy;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateRegularizerEnergyMatrix;
import static mosaic.filamentSegmentation.SegmentationFunctions.generateNormalizedMask;
import static mosaic.filamentSegmentation.SegmentationFunctions.generatePhi;
import static mosaic.filamentSegmentation.SegmentationFunctions.generatePsi;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mosaic.core.imageUtils.Point;
import mosaic.region_competition.utils.MaximumFinder2D;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import mosaic.utils.math.RegionStatisticsSolver;
import mosaic.utils.math.generalizedLinearModel.Glm;
import mosaic.utils.math.generalizedLinearModel.GlmGaussian;
import mosaic.utils.math.generalizedLinearModel.GlmPoisson;
import mosaic.utils.nurbs.BSplineSurface;


/**
 * Implementation of filament segmentation algorithm.
 *
 * "Generalized Linear Models and B_Spline Level-Sets enable Automatic Optimal
 *  Filament Segmentation with Sub-pixel Accuracy"
 *  Xun Xiao, Veikko Geyer, Hugo Bowne-Anderson, Jonathon Howard, Ivo F. Sbalzarini
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SegmentationAlgorithm {

    // Possible noise type of input image
    public static enum NoiseType {
        GAUSSIAN, POISSON
    }

    // PSF kernel type / Microscopy type
    public static enum PsfType {
        GAUSSIAN, DARK_FIELD, PHASE_CONTRAST, NONE
    }

    // Input data
    private final double[][] iImage;
    private final NoiseType iNoiseType;
    private final PsfType iPsfType;
    private final Dimension iPsfSize;
    private final double iSubpixelSampling;
    private final int iCoeffsStepScale;
    private final double iLambdaReg;

    private final int iNoOfIterations;

    // Calculated segmentation data
    private int iStepSize;
    private Matrix iImageData;

    // Input image dimensions
    private int iOriginalWidth;
    private int iOriginalHeight;

    // IMM - energy model
    private Matrix iWeightBoundary;
    private double iLambdaData;
    private Glm iGlm;
    private Matrix iPsf;

    // VLS - vector level set
    private BSplineSurface iPsi;
    private BSplineSurface iPhi;
    private Matrix iEnergyGradOfBases;


    public SegmentationAlgorithm(double[][] aImage, NoiseType aNoiseType, PsfType aPsfType, Dimension aPsfSize,
                                 double aSubpixelSampling, int aCoeffsStepScale, double aRegularizerTerm, 
                                 int aMaxNoOfIterations) {
        iImage = aImage;
        iNoiseType = aNoiseType;
        iPsfType = aPsfType;
        iPsfSize = aPsfSize;
        iSubpixelSampling = aSubpixelSampling;
        iCoeffsStepScale = aCoeffsStepScale;
        iLambdaReg = aRegularizerTerm;
        iNoOfIterations = aMaxNoOfIterations;

        // (Pre)calculate and setup all common things needed by segmenation.
        initialize();
    }

    /**
     * This method calculates all computes each step of segmentation.
     * @return List of generated cubis smoothing splines for each found filament.
     */
    public List<CubicSmoothingSpline> performSegmentation() {

        // Step 1 - Minimize total energy
        final EnergyOutput resultOfEnergyMinimalization = minimizeEnergy();

        // Step 2 - find best threshold values basing on found energy value
        final ThresholdFuzzyOutput resultOfthresholdFuzzyVLS = ThresholdFuzzyVLS(resultOfEnergyMinimalization.iTotalEnergy);

        // Step 3 - generate cubic smoothing splines for each found filament
        //          (filter filaments shorter than 10 points)
        final List<CubicSmoothingSpline> filamentInfo = generateFilamentInfo(resultOfthresholdFuzzyVLS);

        return filamentInfo;
    }

    class EnergyOutput {
        protected EnergyOutput(Matrix aMask, RegionStatisticsSolver aRss, double aTotalEnergy) {
            iMask = aMask;
            iRss = aRss;
            iTotalEnergy = aTotalEnergy;
        }
    
        final Matrix iMask;
        final RegionStatisticsSolver iRss;
        final double iTotalEnergy;
    }

    /**
     * Performs first part of algorithm. Tries to minimize energy between generated
     * mask (based on phi/psi b-splines) and input image.
     */
    EnergyOutput minimizeEnergy() {
        // Calculate initial energy
        Matrix mask = generateMask(true /* resize */);
        RegionStatisticsSolver rss = calculateRss(mask);
        Matrix mu = rss.getModelImage();
    
        double totalEnergy = calculateTotalEnergy(iImageData, mask, mu, false);
    
        Matrix phiCoefs = new Matrix(iPhi.getCoefficients());
        Matrix psiCoefs = new Matrix(iPsi.getCoefficients());
    
        // Initiate best founded matches - they can be updated during
        // next iterations
        Matrix finalMask = mask;
        RegionStatisticsSolver finalRss = rss;
        Matrix finalPhiCoefs = phiCoefs;
        Matrix finalPsiCoefs = psiCoefs;
    
        boolean isEnergyOptimal = false;
    
        for (int i = 0; i < iNoOfIterations && isEnergyOptimal == false; ++i) {
    
            final Matrix[] grads = calculateEnergyGradients(mu, rss);
            final Matrix grad_Phi = grads[0];
            final Matrix grad_Psi = grads[1];
    
            double diffEnergy = 0.0;
            Matrix phiCoefsTemp = null;
            Matrix psiCoefsTemp = null;
    
            // Number of steps in range 1..0
            final int noOfSteps = 16;
            int alphaCount = noOfSteps;
            for (; alphaCount > 0; --alphaCount) {
                final double alpha = (double) alphaCount / noOfSteps;
    
                // Adjust coefficients of B-splines
                phiCoefsTemp = grad_Phi.copy().scale(-alpha).add(phiCoefs).normalize();
                iPhi.setCoefficients(phiCoefsTemp.getArrayYX());
                psiCoefsTemp = grad_Psi.copy().scale(-alpha).add(psiCoefs).normalize();
                iPsi.setCoefficients(psiCoefsTemp.getArrayYX());
    
                // Recalculate energy with new coefficients
                mask = generateMask(true /* resize */);
                rss = calculateRss(mask);
                mu = rss.getModelImage();
                final double tempEngy = calculateTotalEnergy(iImageData, mask, mu, false);
    
                // Remember all best matches found so far
                diffEnergy = tempEngy - totalEnergy;
    
                if (diffEnergy < 0) {
                    totalEnergy = tempEngy;
                    finalMask = mask;
                    finalRss = rss;
                    finalPhiCoefs = phiCoefsTemp;
                    finalPsiCoefs = psiCoefsTemp;
                    break;
                }
            }
    
            // Update B-spline coefficients with the best match
            phiCoefs = phiCoefsTemp;
            psiCoefs = psiCoefsTemp;
    
            if (diffEnergy > 0 && alphaCount == 0) {
                // Not able to minimize energy further
                isEnergyOptimal = true;
            }
        }
    
        // Set the best matching coefficients
        iPhi.setCoefficients(finalPhiCoefs.getArrayYX());
        iPsi.setCoefficients(finalPsiCoefs.getArrayYX());
    
        return new EnergyOutput(finalMask, finalRss, totalEnergy);
    }

    class ThresholdFuzzyOutput {
        ThresholdFuzzyOutput(Matrix aopt_MK, Matrix aH_f) {
            iopt_MK = aopt_MK;
            iH_f = aH_f;
        }
    
        final Matrix iopt_MK; // best founded mask
        final Matrix iH_f; // thresholded mask
    }

    ThresholdFuzzyOutput ThresholdFuzzyVLS(double aTotalEnergy) {
        final Matrix mask = generateMask(false /* resize */);
    
        // Thresholding constants
        final double key_pts_th = 0.70;
        final double th_step = 0.02;
    
        int noOfIterations = (int) (key_pts_th / th_step);
        final Matrix maskLogical = Matlab.logical(mask, key_pts_th);
        final List<Matrix> BMK = new ArrayList<Matrix>(noOfIterations);
        final List<Double> energies = new ArrayList<Double>(noOfIterations);
        for (; noOfIterations >= 0; noOfIterations--) {
            final double treshold = th_step * noOfIterations;
            final double diffEnergy = calculateEnergyForGivenTreshold(aTotalEnergy, mask, maskLogical, BMK, treshold);
            energies.add(diffEnergy);
        }
        final int minIndex = energies.indexOf(Collections.min(energies));
        final Matrix opt_Mask = BMK.get(minIndex);
        final Matrix H_f = mask.elementMult(opt_Mask);
    
        return new ThresholdFuzzyOutput(opt_Mask, H_f);
    }

    /**
     * Generates filament information - for each found filament longer or equal 10 pixels CubicSmoothingSpline is
     * generated.
     * @param aResultOfthresholdFuzzyVLS - input with masks after thresholding
     * @return list of all CubicSmoothingSplines for each filament
     */
    List<CubicSmoothingSpline> generateFilamentInfo(ThresholdFuzzyOutput aResultOfthresholdFuzzyVLS) {

        // best founded mask
        final Matrix mk = aResultOfthresholdFuzzyVLS.iopt_MK;
        // thresholded mask
        final Matrix H_f = aResultOfthresholdFuzzyVLS.iH_f;

        // Find all possible location of filaments
        final Map<Integer, List<Integer>> cc = Matlab.bwconncomp(mk, true /* 8-connected */);
        final int nComp = cc.size();
        final List<CubicSmoothingSpline> result = new ArrayList<CubicSmoothingSpline>();
        if (nComp == 0) {
            // nothing was found... return empty list
            return result;
        }

        // Go through each founded connected region and try fit cubic smoothing spline into it.
        for (final Entry<Integer, List<Integer>> e : cc.entrySet()) {
            // Generate coordinates for filament being processed.
            final List<Integer> x = new ArrayList<Integer>();
            final List<Integer> y = new ArrayList<Integer>();
            generateCoordinatesOfOneFilament(mk, H_f, e, x, y);

            // If data set is too small just continue with next filament
            // If it's big enough then limit number of point to be processed if necessary
            final int size = x.size();
            if (size < 10) {
                continue;
            }
            final List<Double> xv = new ArrayList<Double>();
            final List<Double> yv= new ArrayList<Double>();
            final List<Double> wv= new ArrayList<Double>();
            generateOutputPointsAndWeights(xv,yv,wv, x, y);

            // Check if after merging we have enough points to generate css.
            if (xv.size() > 1) {
                final CubicSmoothingSpline css = calculateCubicSmoothingSpline(xv, yv, wv);

                // Save result and continue
                result.add(css);
            }
        }

        return result;
    }

    private void generateCoordinatesOfOneFilament(final Matrix mk, final Matrix H_f, final Entry<Integer, List<Integer>> e, final List<Integer> x, final List<Integer> y) {
        // Create new Matrix from mask with only one filament (zero pixels outside of current region)
        final List<Integer> PxIdx = e.getValue();
        final Matrix selectedRegion = new Matrix(mk.numRows(), mk.numCols()).zeros();
        for (final int idx : PxIdx) {
            selectedRegion.set(idx, 1);
        }
        final Matrix selectedFilament = H_f.copy().elementMult(selectedRegion);

        generateCoordinatesOfFilament(selectedRegion, selectedFilament, x, y);
    }

    private CubicSmoothingSpline calculateCubicSmoothingSpline(final List<Double> xv, final List<Double> yv, final List<Double> wv) {
        // Calculate cubic smoothing spline for calculated points and weights
        final double[] xz = ConvertArray.toDouble(xv);
        final double[] yz = ConvertArray.toDouble(yv);
        final double[] wz = ConvertArray.toDouble(wv);

        final CubicSmoothingSpline css = new CubicSmoothingSpline(xz, yz, 0.01, wz);
        return css;
    }

    /**
     * Limits number of Original (input) points to 10-20 points in Output containers. This should be more than enough for further
     * calculations and limit amount of processing needed. The first and last point from "Original" containers are always includedin "Output" containers.
     * @param aOutputX - list for storing x coordinates
     * @param aOutputY - list for storing y coordinates
     * @param aOutputWeights list for storing weights
     * @param aOriginalX - input x points
     * @param aOriginalY - intpu y points
     */
    void generateOutputPointsAndWeights(List<Double> aOutputX, List<Double> aOutputY, List<Double> aOutputWeights, List<Integer> aOriginalX, List<Integer> aOriginalY) {
        final int size = aOriginalX.size();

        // Choose divider to limit number of points for further calculation. Original implementation
        // from Matlab decides about divider using logic commented below. It seems not to be very correct since it limits
        // points in the beginning and then fix divider to 2...
        // ====================================
        // if (size < 30) divider = 1;
        // else if (size < 50) divider = 3;
        // else if (size < 100) divider = 4;
        // else divider = 2;
        // ====================================
        final int divider = (size/20) + 1;

        final Matrix indices = Matlab.linspace(0, size - 1, size/divider);

        for (int i = 0; i < indices.size(); ++i) {
            aOutputX.add((double) aOriginalX.get((int)(indices.get(i)+0.5)));
            aOutputY.add((double) aOriginalY.get((int)(indices.get(i)+0.5)));
            aOutputWeights.add(1.0);
        }
        
        // Merge points with same x values and adequately calculate y (mean value) and weights (sum of weights).
        mergePointsWithSameX(aOutputX, aOutputY, aOutputWeights);
    }

    /**
     * Merge points with same X coordinates. Y values for given X are averaged and
     * weights are added. After merging input containers are shrunk to fit new number
     * of elements.
     * @param aX - input X
     * @param aY - input Y
     * @param aWeights - input weights
     */
    void mergePointsWithSameX(List<Double> aX, List<Double> aY, List<Double> aWeights) {
        double sum = 0.0;
        double weight = 0.0;
        int lastIdx = -1;
        int count = 1;
        int outIndex = 0;
        for (int i = 0; i < aX.size(); ++i) {
            final int xIdx = aX.get(i).intValue();
            if (xIdx != lastIdx) {
                if (lastIdx != -1) {
                    aX.set(outIndex, (double)lastIdx);
                    aY.set(outIndex, sum/count);
                    aWeights.set(outIndex, weight);
                    outIndex++;
                }
                count = 1;
                lastIdx = xIdx;
                sum = aY.get(i);
                weight = aWeights.get(i);
            }
            else {
                sum += aY.get(i);
                weight += aWeights.get(i);
                count++;
            }
        }

        // Save last point being processed
        aX.set(outIndex, (double)lastIdx);
        aY.set(outIndex, sum/count);
        aWeights.set(outIndex, weight);
        outIndex++;

        // Remove from list unused entries
        aX.subList(outIndex, aX.size()).clear();
        aY.subList(outIndex, aY.size()).clear();
        aWeights.subList(outIndex, aWeights.size()).clear();
    }

    void generateCoordinatesOfFilament(Matrix selectedRegion, Matrix selectedFilament, List<Integer> aX, List<Integer> aY) {

        final MaximumFinder2D maximumFinder2D = new MaximumFinder2D(selectedFilament.numCols(), selectedFilament.numRows());
        final List<Point> mm = maximumFinder2D.getMaximaPointList(ConvertArray.toFloat(selectedFilament.getData()), 0.0, false);

        // Find local max intensity matrix by getting pixels from Maximum Finder and doing simple graph search.
        // If neighbor pixel have same intensity as those find by MaximumFinder they are will be also marked as 1
        // in localMaxIntensity matrix.
        final Matrix localMaxIntensity = new Matrix(selectedFilament.numRows(), selectedFilament.numCols()).zeros();
        findLocalMaxIntensityPoints(selectedFilament, mm, localMaxIntensity);

        selectedFilament = selectedFilament.elementMult(localMaxIntensity);
        for (int col = 0; col < selectedFilament.numCols(); ++col) {
            double maxVal = 0.0;
            int idx = -1;
            for (int row = 0; row < selectedFilament.numRows(); ++row) {
                final double val = selectedFilament.get(row, col);
                if (val > maxVal) {
                    maxVal = val;
                    idx = row;
                }
            }
            if (idx == -1) {
                continue;
            }
            selectedRegion.insertCol(new Matrix(selectedRegion.numRows(), 1).zeros(), col);
            selectedRegion.set(idx, col, 1.0);
        }

        // Find all (filament) points and put them into containers. Add 1 to
        // all elements to be compatible with Matlab (Indexing starts from 1).
        for (int col = 0; col < selectedFilament.numCols(); ++col) {
            for (int row = 0; row < selectedFilament.numRows(); ++row) {
                if (selectedRegion.get(row, col) > 0) {
                    aX.add((int)(col * iSubpixelSampling) + 1);
                    aY.add((int)(row * iSubpixelSampling) + 1);
                }
            }
        }
    }

    private void findLocalMaxIntensityPoints(Matrix selectedFilament, final List<Point> mm, final Matrix localMaxIntensity) {
        for (final Point p : mm) {
            final List<Point> q = new ArrayList<Point>();
            final List<Point> d = new ArrayList<Point>();
            q.add(p);
            final float val = (float)selectedFilament.get(p.iCoords[1], p.iCoords[0]);
            while (!q.isEmpty()) {
                // Get first element on the list and remove it
                final Point id = q.remove(0);
                d.add(id);
                final int x = id.iCoords[0];
                final int y = id.iCoords[1];

                // Mark pixel as belonging to local maximum
                localMaxIntensity.set(y, x, 1.0);

                // Check all neighbours of currently processed pixel
                // (do some additional logic to skip point itself and to handle 4/8
                // base connectivity)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            final int indX = x + dx;
                            final int indY = y + dy;
                            if (indX >= 0 && indX < localMaxIntensity.numCols() && indY >= 0 && indY < localMaxIntensity.numRows()) {
                                // final boolean aIs8connected = true;
                                // if (aIs8connected || (dy * dx == 0)) {
                                    // Intentionally cast to float to match float result from MaximumFinder2D
                                    if ((float)selectedFilament.get(indY, indX) == val) {
                                        final Point idx = new Point(indX, indY, 0);
                                        if (!q.contains(idx) && !d.contains(idx)) {
                                            // If element was not visited yet put it on list
                                            q.add(idx);
                                        }
                                    }
                                // }
                            }
                        }
                    }
                }
            }
        }
    }

    private double calculateEnergyForGivenTreshold(double aTotalEnergy, final Matrix mask, final Matrix maskLogical, final List<Matrix> BMK, final double th) {
        final Matrix bmk = Matlab.logical(mask, th);
        final Map<Integer, List<Integer>> cc = Matlab.bwconncomp(bmk, true /* 8-connected */);
        for (final List<Integer> list : cc.values()) {
            final Matrix tmk = new Matrix(bmk.numRows(), bmk.numCols()).zeros();
            for (final int idx : list) {
                tmk.set(idx, 1);
            }
            final double flag = tmk.elementMult(maskLogical).sum();
            if (flag == 0.0) {
                for (final int idx : list) {
                    bmk.set(idx, 0);
                }
            }
        }
        BMK.add(bmk);

        final Matrix rmk = Matlab.imresize(bmk, iSubpixelSampling);
        final Matrix Krmk = Matlab.logical(Matlab.imfilterSymmetric(rmk, iPsf), 0.49999);
        final RegionStatisticsSolver rss = new RegionStatisticsSolver(iImageData, Krmk, iGlm, iImageData, 2).calculate();
        final double totalEnergy = calculateTotalEnergy(iImageData, rmk, rss.getModelImage(), true);
        final double diffEnergy = totalEnergy - aTotalEnergy;
        return diffEnergy;
    }

    private double calculateTotalEnergy(Matrix aImageData, Matrix mask, Matrix mu, boolean aIsMatrixLogical) {
        final double dataEnergy = iGlm.nllMean(aImageData, mu, iGlm.priorWeights(aImageData));
        final double regularizerEnergy = calculateRegularizerEnergy(mask, iWeightBoundary, aIsMatrixLogical);
        final double totalEnergy = iLambdaData * dataEnergy + iLambdaReg * regularizerEnergy;

        return totalEnergy;
    }

    /**
     * Calcualtes energy gradients for given model image (aMu - naming follows Matlab code)
     * and output from region statistics solver.
     * @param aMu
     * @param aRss
     * @return array Matrix[2] (index 0 -> gradient of Phi, index 1 -> gradient of Psi)
     */
    private Matrix[] calculateEnergyGradients(Matrix aMu, RegionStatisticsSolver aRss) {
        Matrix w_g0 = calculateBregmanDivergance(aMu);
        w_g0.scale(aRss.getBetaMLEin() - aRss.getBetaMLEout());
        
        final Matrix phiPts2 = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPhi);
        final Matrix psiPts2 = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPsi);

        final Matrix der_phi = calculateDiffDirac(phiPts2).elementMult(calculateHeavySide(psiPts2));
        final Matrix der_psi = calculateDirac(phiPts2).elementMult(calculateDirac(psiPts2));

        final Matrix grad_Phi = calculateEnergyGradient(w_g0, der_phi, false /* shouldConvolve */).transpose();
        final Matrix grad_Psi = calculateEnergyGradient(w_g0, der_psi, true /* shouldConvolve */).transpose();

        return new Matrix[] { grad_Phi, grad_Psi };
    }

    /**
     * Calcualtes Bregman divergence basing on current noise type settings.
     * @param aMu Input matrix (divergence is calculated with respect to iImageData)
     * @return bregman divergance
     */
    private Matrix calculateBregmanDivergance(Matrix aMu) {
        Matrix w_g0 = null;
        switch (iNoiseType) {
            case GAUSSIAN:
                w_g0 = iImageData.copy().sub(aMu).scale(-2);
                break;
            case POISSON:
                w_g0 = iImageData.copy().ones().sub(iImageData.copy().elementDiv(aMu));
                break;
            default:
                new RuntimeException("Uknown NoiseType: [" + iNoiseType + "]");
                break;
        }
        
        return w_g0;
    }

    /**
     * Helper method for calculating energy gradient for given Phi/Psi set of values
     * @param w_g0
     * @param aFuncPoints - Matrix with calculated values of Psi/Phi
     * @param aShouldConvolve - if true then convolves if not then runs symmetric filter
     * @return
     */
    private Matrix calculateEnergyGradient(Matrix w_g0, Matrix aFuncPoints, boolean aShouldConvolve) {
        final Matrix rEngyPhi = calculateRegularizerEnergyMatrix(aFuncPoints, iWeightBoundary, false);

        Matrix w_g = w_g0.copy().elementMult(aFuncPoints).add(rEngyPhi.copy().scale(iLambdaReg)).normalize();

        if (aShouldConvolve) {
            w_g = Matlab.imfilterConv(w_g, iPsf);
        }
        else {
            w_g = Matlab.imfilterSymmetric(w_g, iPsf);
        }

        // Run filter for both - rows and columns.
        Matrix grad = Matlab.imfilterSymmetric(
                Matlab.imfilterSymmetric(w_g, iEnergyGradOfBases.copy().transpose()),
                iEnergyGradOfBases);

        // Resize gradient to original image size by choosing each iStepSize value from each row/col.
        grad = grad.resize(0, 0, iStepSize, iStepSize);

        return grad;
    }

    /**
     * calculates Region Statistics Solver / MLE / model image for given mask. 
     * @param mask
     * @return
     */
    private RegionStatisticsSolver calculateRss(Matrix mask) {
        final Matrix kmask = Matlab.imfilterSymmetric(mask, iPsf);
        final RegionStatisticsSolver rss = new RegionStatisticsSolver(iImageData, kmask, iGlm, iImageData, 6).calculate();

        return rss;
    }
    
    /**
     * Generates mask basing on current values of Phi/Psi functions, and subpixel segmentation settings.
     * @param aResizeMask - should mask be resize to original dimensions if subpixelsampling is != 1?
     * @return generated mask
     */
    private Matrix generateMask(boolean aResizeMask) {
        final Matrix phiPts = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPhi);
        final Matrix psiPts = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPsi);

        Matrix mask = generateNormalizedMask(phiPts, psiPts);

        if (aResizeMask) {
            mask = Matlab.imresize(mask, iSubpixelSampling);
        }

        return mask;
    }

    /**
     * Initializes all data needed for segmentation (PSF, GLM, IMM, B-Spline
     * surfaces, ...) and prepares input image.
     */
    private void initialize() {
        iStepSize = (int) Math.pow(2, iCoeffsStepScale);

        // --------------------------------------------------------------
        // Normalize and resize original image according to step size
        iOriginalWidth = iImage[0].length;
        iOriginalHeight = iImage.length;
        final int width = (int) (Math.ceil((double) iOriginalWidth / iStepSize) * iStepSize);
        final int height = (int) (Math.ceil((double) iOriginalHeight / iStepSize) * iStepSize);
        final double[][] img = new double[height][width];
        ImgUtils.imgResize(iImage, img);
        ImgUtils.normalize(img);
        iImageData = new Matrix(img);


        // --------------------------------------------------------------
        // IMM data (subpixel sampling, regularizer term are already given)

        // Generate Generalized Linear Model
        switch (iNoiseType) {
            case POISSON:
                iGlm = new GlmPoisson();
                break;
            case GAUSSIAN:
            default:
                iGlm = new GlmGaussian();
                break;
        }

        // Generate PSF
        switch (iPsfType) {
            case DARK_FIELD:
                // Intentionally falling down
            case GAUSSIAN:
                // sigma 0.5 follows default value for sigma in Matlab's command
                // 'fspecial'
                iPsf = GaussPsf.generate(iPsfSize.width, iPsfSize.height, 0.5);
                break;
            case PHASE_CONTRAST:
                // Values taken from Matlab's implementation
                iPsf = PhaseContrastPsf.generate(2.9, 0.195, 3);
                break;
            case NONE:
                iPsf = new Matrix(new double[][] { { 1 } });
                break;
            default:
                // Default case - use 4x4 Gauss psf
                iPsf = GaussPsf.generate(4, 4, 0.5);
                break;
        }

        iLambdaData = 1;
        iWeightBoundary = new Matrix(iImageData.numRows(), iImageData.numCols()).ones();

        // --------------------------------------------------------------
        // VLS model
        iPsi = generatePsi(iImageData.numCols(), iImageData.numRows(), iStepSize);
        iPhi = generatePhi(iImageData.numCols(), iImageData.numRows(), iStepSize);
        iEnergyGradOfBases = EnergyGriadientsOfBasesFunctions.getEnergyGradients(iCoeffsStepScale);
    }

    @Override
    public String toString() {
        String str = "-------- Segmentation Algorithm Parameters --------\n";
        str +=       "Input Img dims: " + iOriginalWidth + "x" + iOriginalHeight + "\n";
        str +=       "NoiseType: " + iNoiseType.toString() + "\n";
        str +=       "PsfType: " +  iPsfType.toString() + "\n";
        str +=       "W/H: " + iPsfSize.width + "/" + iPsfSize.height + "\n";
        str +=       "Subpixel sampling: " + iSubpixelSampling + "\n";
        str +=       "StepScale: " + iCoeffsStepScale + "\n";
        str +=       "No Of Iterations: " + iNoOfIterations + "\n";
        str +=       "LambdaReg: " + iLambdaReg + "\n";
        str +=       "---------------------------------------------------\n";
        return str;
    }
}
