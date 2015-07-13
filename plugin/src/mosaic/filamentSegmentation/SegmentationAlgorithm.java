package mosaic.filamentSegmentation;

import static mosaic.filamentSegmentation.SegmentationFunctions.calculateBSplinePoints;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateDiffDirac;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateDirac;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateHeavySide;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateRegularizerEnergy;
import static mosaic.filamentSegmentation.SegmentationFunctions.calculateRegularizerEnergyMatrix;
import static mosaic.filamentSegmentation.SegmentationFunctions.generatePhi;
import static mosaic.filamentSegmentation.SegmentationFunctions.generatePsi;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mosaic.core.utils.Point;
import mosaic.generalizedLinearModel.Glm;
import mosaic.generalizedLinearModel.GlmGaussian;
import mosaic.generalizedLinearModel.GlmPoisson;
import mosaic.math.CubicSmoothingSpline;
import mosaic.math.MFunc;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.math.RegionStatisticsSolver;
import mosaic.nurbs.BSplineSurface;
import mosaic.plugins.utils.Convert;
import mosaic.plugins.utils.Debug;
import mosaic.plugins.utils.ImgUtils;
import mosaic.region_competition.utils.MaximumFinder2D;

public class SegmentationAlgorithm {
    // Possible noise type of input image
    public static enum NoiseType {
        GAUSSIAN, POISSON
    };

    // PSF kernel type / Microscopy type
    public static enum PsfType {
        PHASE_CONTRAST, DARK_FIELD, GAUSSIAN, NONE
    };

    // Input data
    private double[][] iImage;
    private NoiseType iNoiseType;
    private PsfType iPsfType;
    private Dimension iPsfSize;
    private double iSubpixelSampling;
    private int iCoeffsStepScale;
    private double iLambdaReg;

    private int iNoOfIterations;

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
    BSplineSurface iPsi;
    BSplineSurface iPhi;
    Matrix iEnergyGradOfBases;

    public SegmentationAlgorithm(double[][] aImage, NoiseType aNoiseType, PsfType aPsfType, Dimension aPsfSize,
            double aSubpixelSampling, int aCoeffsStepScale, double aRegularizerTerm) {
        this(aImage, aNoiseType, aPsfType, aPsfSize, aSubpixelSampling, aCoeffsStepScale, aRegularizerTerm, 100);
    }

    public SegmentationAlgorithm(double[][] aImage, NoiseType aNoiseType, PsfType aPsfType, Dimension aPsfSize,
            double aSubpixelSampling, int aCoeffsStepScale, double aRegularizerTerm, int aMaxNoOfIterations) {
        iImage = aImage;
        iNoiseType = aNoiseType;
        iPsfType = aPsfType;
        iPsfSize = aPsfSize;
        iSubpixelSampling = aSubpixelSampling;
        iCoeffsStepScale = aCoeffsStepScale;
        iLambdaReg = aRegularizerTerm;

        iNoOfIterations = aMaxNoOfIterations;

        initialize();
    }

    public double[][] performSegmentation() {

        // Minimize total energy
        EnergyOutput resultOfEnergyMinimalization = minimizeEnergy();

        ThresholdFuzzyOutput resultOfthresholdFuzzyVLS = ThresholdFuzzyVLS(resultOfEnergyMinimalization.iTotalEnergy);
        System.out.println("LENGHT........");
        List<CubicSmoothingSpline> filamentInfo = generateFilamentInfo(resultOfthresholdFuzzyVLS);
        System.out.println("LIST: ");
        for (CubicSmoothingSpline css : filamentInfo) {
            System.out.println("LEN: ");
            System.out.println(calcualteFilamentLenght(css));
            Debug.print("LEN", calcualteFilamentLenght(css));
        }
        
        // TODO: this is temporary return value, should be changed probably
        double[][] img = resultOfEnergyMinimalization.iMask.getArrayYX();
        
        for (CubicSmoothingSpline css : filamentInfo) {
            final CubicSmoothingSpline css1 = css;
            double start = css1.getKnot(0);
            double stop = css1.getKnot(css1.getNumberOfKNots() - 1);
            
            final Matrix x  = Matlab.linspace(start, stop, 1000);
            Matrix y = x.copy().process(new MFunc() { 
                @Override
                public double f(double aElement, int aRow, int aCol) {
                    return css1.getValue(x.get(aRow, aCol));
                }
            });
            
            for (int i = 0; i < x.size(); ++i) {
                img[(int)y.get(i)][(int)x.get(i)] = 3;
            }
        }
        
        return img;
    }

    private double calcualteFilamentLenght(final CubicSmoothingSpline css) {
        double start = css.getKnot(0);
        double stop = css.getKnot(css.getNumberOfKNots() - 1);
        
        final Matrix x  = Matlab.linspace(start, stop, 1000);
        Matrix y = x.copy().process(new MFunc() { 
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return css.getValue(x.get(aRow, aCol));
            }
        });
        
        double prevX = x.get(0);
        double prevY = y.get(0);
        double length = 0.0;
        
        for (int i = 0; i < x.size(); ++i) {
            length += Math.sqrt(Math.pow(x.get(i) - prevX,2) + Math.pow(y.get(i) - prevY,2));
            prevX = x.get(i);
            prevY = y.get(i);
        }
        
        
        return length;
    }

    List<CubicSmoothingSpline> generateFilamentInfo(ThresholdFuzzyOutput resultOfthresholdFuzzyVLS) {
        Matrix mk = resultOfthresholdFuzzyVLS.iopt_MK;
        Matrix H_f = resultOfthresholdFuzzyVLS.iH_f;
        Map<Integer, List<Integer>> cc = Matlab.bwconncomp(mk, true /*
                                                                     * 8-connected
                                                                     */);
        int nComp = cc.size();
        System.out.println("NUMBER OF CC : " + nComp);

        List<CubicSmoothingSpline> result = new ArrayList<CubicSmoothingSpline>();
        if (nComp == 0) {
            // TODO: What to return?
            return result;
        }

        
        for (Entry<Integer, List<Integer>> e : cc.entrySet()) {
            List<Integer> PxIdx = e.getValue();
            Matrix tmk = new Matrix(mk.numRows(), mk.numCols()).zeros();
            for (int idx : PxIdx)
                tmk.set(idx, 1);
            Matrix H_f_fi = H_f.elementMult(tmk);

            MaximumFinder2D maximumFinder2D = new MaximumFinder2D(H_f_fi.numCols(), H_f_fi.numRows());
            List<Point> mm = maximumFinder2D.getMaximaPointList(Convert.toFloat(H_f_fi.getData()), 0.0, false);
            Matrix localMaxIntensity = new Matrix(H_f_fi.numRows(), H_f_fi.numCols()).zeros();
            for (Point p : mm) {
                localMaxIntensity.set(p.x[1], p.x[0], 1.0);
            }
            H_f_fi = H_f_fi.elementMult(localMaxIntensity);

            for (int col = 0; col < H_f_fi.numCols(); ++col) {
                double maxVal = 0.0;
                int idx = -1;
                for (int row = 0; row < H_f_fi.numRows(); ++row) {
                    double val = H_f_fi.get(row, col);
                    if (val > maxVal) {
                        maxVal = val;
                        idx = row;
                    }
                }
                if (idx == -1)
                    continue;
                tmk.insertCol(new Matrix(tmk.numRows(), 1).zeros(), col);
                tmk.set(idx, col, 1.0);
            }

            List<Integer> x = new ArrayList<Integer>();
            List<Integer> y = new ArrayList<Integer>();
            for (int col = 0; col < H_f_fi.numCols(); ++col) {
                for (int row = 0; row < H_f_fi.numRows(); ++row) {
                    if (tmk.get(row, col) > 0) {x.add(col); y.add(row);}
                }
            }

            int divider = 1;
            int size = x.size();
            System.out.println("SIZE: ------------------------- " + size);
            if (size < 10) continue;
            if (size < 30) divider = 1;
            else if (size < 50) divider = 3;
            else if (size < 100) divider = 4;
            else divider = 2; // TODO: ??? to be verified
            
            Matrix  indices = Matlab.linspace(0, size - 1, size/divider);

            double[] xv = new double[indices.size()]; 
            double[] yv = new double[indices.size()]; 
            for (int i = 0; i < indices.size(); ++i) { 
                xv[i] = (double) x.get((int)indices.get(i));
                yv[i] = y.get((int)indices.get(i));
            }
            
            
            
            List<Double> xc = new ArrayList<Double>();
            List<Double> yc= new ArrayList<Double>();
            double sum = 0.0;
            int lastIdx = -1;
            int count = 1;
            for (int i = 0; i < xv.length; ++i) {
                int xIdx = (int)xv[i];
                if (xIdx != lastIdx) {
                    if (lastIdx != -1) {
                        xc.add((double)lastIdx + 1);
                        yc.add(sum/count + 1);
                    }
                    count = 1;
                    lastIdx = xIdx;
                    sum = yv[i];
                }
                else {
                    sum += yv[i];
                    count++;
                }
            }
            xc.add((double)lastIdx + 1);
            yc.add(sum/count + 1);
            
            double[] xz = new double[xc.size()]; 
            double[] yz = new double[xc.size()]; 
            for (int i = 0; i < xc.size(); ++i) { 
                xz[i] = xc.get(i);
                yz[i] = yc.get(i);
            }
            
            Debug.print(xc);
            CubicSmoothingSpline css = new CubicSmoothingSpline(xz, yz, 0.01);
            result.add(css);
            //for (Polynomial p : ps) System.out.println(p);
            // for (double xs : xv) {System.out.println(css.getValue(xs));}
        }
        
        for (CubicSmoothingSpline css : result) {
            Debug.print("LEN", calcualteFilamentLenght(css));
        }
        
        return result;
    }

    class ThresholdFuzzyOutput {
        ThresholdFuzzyOutput(Matrix aopt_MK, Matrix aH_f) {
            iopt_MK = aopt_MK;
            iH_f = aH_f;
        }

        public Matrix iopt_MK; // best founded mask
        public Matrix iH_f; // thresholded mask
    }

    ThresholdFuzzyOutput ThresholdFuzzyVLS(double aTotalEnergy) {
        final Matrix mask = generateMask(false /* resize */);

        // Thresholding constants
        final double key_pts_th = 0.70;
        final double th_step = 0.02;

        int noOfIterations = (int) (key_pts_th / th_step);
        Matrix maskLogical = logical(mask, key_pts_th);

        List<Matrix> BMK = new ArrayList<Matrix>(noOfIterations);
        List<Double> energies = new ArrayList<Double>(noOfIterations);
        for (; noOfIterations >= 0; noOfIterations--) {
            final double th = th_step * noOfIterations;

            Matrix bmk = logical(mask, th);
            Map<Integer, List<Integer>> cc = Matlab.bwconncomp(bmk, true /*
                                                                          * 8-
                                                                          * connected
                                                                          */);
            for (List<Integer> list : cc.values()) {
                Matrix tmk = new Matrix(bmk.numRows(), bmk.numCols()).zeros();
                for (int idx : list)
                    tmk.set(idx, 1);
                double flag = tmk.elementMult(maskLogical).sum();
                if (flag == 0.0) {
                    for (int idx : list)
                        bmk.set(idx, 0);
                }
            }
            BMK.add(bmk);

            Matrix rmk = Matlab.imresize(bmk, iSubpixelSampling);
            Matrix Krmk = Matlab.imfilterSymmetric(rmk, iPsf);
            RegionStatisticsSolver rss = new RegionStatisticsSolver(iImageData, Krmk, iGlm, iImageData, 2).calculate();

            double totalEnergy = calculateTotalEnergy(iImageData, rmk, rss.getModelImage(), true);
            double diffEnergy = totalEnergy - aTotalEnergy;
            energies.add(diffEnergy);
        }
        int minIndex = energies.indexOf(Collections.min(energies));
        Matrix opt_Mask = BMK.get(minIndex);
        Matrix H_f = mask.elementMult(opt_Mask);

        return new ThresholdFuzzyOutput(opt_Mask, H_f);
    }

    private Matrix logical(final Matrix mask, final double th) {
        Matrix bmk = mask.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return aElement > th ? 1 : 0;
            }
        });
        return bmk;
    }

    class EnergyOutput {
        EnergyOutput(Matrix aMask, RegionStatisticsSolver aRss, double aTotalEnergy) {
            iMask = aMask;
            iRss = aRss;
            iTotalEnergy = aTotalEnergy;
        }

        public Matrix iMask;
        public RegionStatisticsSolver iRss;
        public double iTotalEnergy;
    }

    EnergyOutput minimizeEnergy() {
        // Calculate initial energy
        Matrix mask = generateMask(true /* resize */);
        RegionStatisticsSolver rss = generateRss(mask);
        Matrix mu = rss.getModelImage();

        double totalEnergy = calculateTotalEnergy(iImageData, mask, mu);

        Matrix phiCoefs = new Matrix(iPhi.getCoefficients());
        Matrix psiCoefs = new Matrix(iPsi.getCoefficients());

        boolean isEnergyOptimal = false;

        for (int i = 0; i < iNoOfIterations && isEnergyOptimal == false; ++i) {
            Matrix[] grads = calculateEnergyGradient(mu, rss);
            Matrix grad_Phi = grads[0];
            Matrix grad_Psi = grads[1];

            // Number of steps in range 1..0
            final int noOfSteps = 16;

            int alphaCount = noOfSteps;
            double diffEnergy = 0.0;
            Matrix phiCoefsTemp = null;
            Matrix psiCoefsTemp = null;

            for (; alphaCount > 0; --alphaCount) {
                double alpha = (double) alphaCount / noOfSteps;

                // Adjust coefficients of B-splines
                phiCoefsTemp = grad_Phi.copy().scale(-alpha).add(phiCoefs).normalize();
                iPhi.setCoefficients(phiCoefsTemp.getArrayYX());
                psiCoefsTemp = grad_Psi.copy().scale(-alpha).add(psiCoefs).normalize();
                iPsi.setCoefficients(psiCoefsTemp.getArrayYX());

                // Recalculate energy with new coefficients
                mask = generateMask(true /* resize */);
                rss = generateRss(mask);
                mu = rss.getModelImage();

                double tempEngy = calculateTotalEnergy(iImageData, mask, mu);
                diffEnergy = tempEngy - totalEnergy;
                if (diffEnergy < 0) {
                    totalEnergy = tempEngy;
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

        return new EnergyOutput(mask, rss, totalEnergy);
    }

    private Matrix[] calculateEnergyGradient(Matrix aMu, RegionStatisticsSolver aRss) {
        Matrix w_g0 = null;
        switch (iNoiseType) {
        case GAUSSIAN:
            w_g0 = iImageData.copy().sub(aMu).scale(2 * (aRss.getBetaMLEout() - aRss.getBetaMLEin()));
            break;
        case POISSON:
            w_g0 = iImageData.copy().ones().sub(iImageData.copy().elementDiv(aMu))
                    .scale(aRss.getBetaMLEin() - aRss.getBetaMLEout());
            break;
        default:
            new RuntimeException("Uknown NoiseType: [" + iNoiseType + "]");
            break;
        }

        Matrix phiPts2 = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPhi);
        Matrix psiPts2 = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPsi);

        Matrix der_phi = calculateDiffDirac(phiPts2).elementMult(calculateHeavySide(psiPts2));
        Matrix der_psi = calculateDirac(phiPts2).elementMult(calculateDirac(psiPts2));

        Matrix grad_Phi = CalculateEnergyGradient(w_g0, der_phi, false).transpose();
        Matrix grad_Psi = CalculateEnergyGradient(w_g0, der_psi, true).transpose();

        return new Matrix[] { grad_Phi, grad_Psi };
    }

    private double calculateTotalEnergy(Matrix aImageData, Matrix mask, Matrix mu) {
        return calculateTotalEnergy(aImageData, mask, mu, false);
    }

    private double calculateTotalEnergy(Matrix aImageData, Matrix mask, Matrix mu, boolean aIsMatrixLogical) {
        double dataEnergy = iGlm.nllMean(aImageData, mu, iGlm.priorWeights(aImageData));
        double regularizerEnergy = calculateRegularizerEnergy(mask, iWeightBoundary, aIsMatrixLogical);
        double totalEnergy = iLambdaData * dataEnergy + iLambdaReg * regularizerEnergy;

        return totalEnergy;
    }

    private RegionStatisticsSolver generateRss(Matrix mask) {
        Matrix kmask = Matlab.imfilterSymmetric(mask, iPsf);
        RegionStatisticsSolver rss = new RegionStatisticsSolver(iImageData, kmask, iGlm, iImageData, 6).calculate();

        return rss;
    }

    private Matrix generateMask(boolean aResizeMask) {
        Matrix phiPts = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPhi);
        Matrix psiPts = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPsi);
        Matrix mask = SegmentationFunctions.generateMask(phiPts, psiPts);

        if (aResizeMask)
            mask = Matlab.imresize(mask, iSubpixelSampling);

        return mask;
    }

    private Matrix CalculateEnergyGradient(Matrix w_g0, Matrix der_phi, boolean aShouldConvolve) {
        Matrix rEngyPhi = calculateRegularizerEnergyMatrix(der_phi, iWeightBoundary, false);
        Matrix w_g = w_g0.copy().elementMult(der_phi).add(rEngyPhi.copy().scale(iLambdaReg)).normalize();

        if (aShouldConvolve)
            w_g = Matlab.imfilterConv(w_g, iPsf);
        else
            w_g = Matlab.imfilterSymmetric(w_g, iPsf);

        Matrix grad_Phi = Matlab.imfilterSymmetric(
                Matlab.imfilterSymmetric(w_g, iEnergyGradOfBases.copy().transpose()), iEnergyGradOfBases);
        grad_Phi = grad_Phi.resize(0, 0, iStepSize, iStepSize);
        return grad_Phi;
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
        int width = (int) (Math.ceil((double) iOriginalWidth / iStepSize) * iStepSize);
        int height = (int) (Math.ceil((double) iOriginalHeight / iStepSize) * iStepSize);
        double[][] img = new double[height][width];
        ImgUtils.imgResize(iImage, img);
        ImgUtils.normalize(img);
        iImageData = new Matrix(img);

        // --------------------------------------------------------------
        // IMM data (subpixel sumpling, regularizer term are already given)

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
}
