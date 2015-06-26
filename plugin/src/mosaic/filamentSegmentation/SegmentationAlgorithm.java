package mosaic.filamentSegmentation;

import java.awt.Dimension;

import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.math.RegionStatisticsSolver;
import mosaic.nurbs.BSplineSurface;
import mosaic.plugins.utils.ImgUtils;
import mosaic.generalizedLinearModel.*;
import static mosaic.filamentSegmentation.SegmentationFunctions.*;

class SegmentationAlgorithm {
    public static enum NoiseType {GAUSSIAN, POISSON};
    public static enum PsfType {PHASE_CONTRAST, DARK_FIELD, GAUSSIAN, NONE};
    
    // Input data
    private double[][] iImage;
    private NoiseType iNoiseType;
    private PsfType iPsfType;
    private Dimension iPsfSize;
    private double iSubpixelSampling;
    private int iCoeffsStepScale;
    private double iLambdaReg;
    
    private int iNoOfIterations;
    
    // Calculated data
    private int iStepSize;
    private Matrix iImageData;
    
    // Input image dimensions
    private int iOriginalWidth;
    private int iOriginalHeight;
    
    // IMM
    private Matrix iWeightBoundary;
    private double iLambdaData;
    private Glm iGlm;
    private Matrix iPsf;

    // VLS
    BSplineSurface iPsi;
    BSplineSurface iPhi;
    Matrix iEnergyGradOfBases;
    
    SegmentationAlgorithm(double[][] aImage, NoiseType aNoiseType, PsfType aPsfType, Dimension aPsfSize, double aSubpixelSampling, int aCoeffsStepScale, double aRegularizerTerm) {
        iImage = aImage;
        iNoiseType = aNoiseType;
        iPsfType = aPsfType;
        iPsfSize = aPsfSize;
        iSubpixelSampling = aSubpixelSampling;
        iCoeffsStepScale = aCoeffsStepScale;
        iLambdaReg = aRegularizerTerm;
        
        iNoOfIterations = 1;
        
        initialize();
    }
    
    void performSegmentation() {
        // Matlab's Energy ------------>
        Matrix phiPts = SegmentationFunctions.calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPhi);
        Matrix psiPts = SegmentationFunctions.calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPsi);
        Matrix mask = SegmentationFunctions.generateMask(phiPts, psiPts);
        mask = Matlab.imresize(mask, iSubpixelSampling);
        
        Matrix kmask = Matlab.imfilterSymmetric(mask, iPsf);
        
        RegionStatisticsSolver rss = new RegionStatisticsSolver(iImageData, kmask, iGlm, iImageData, 5).calculate();
        Matrix mu = rss.getModelImage();
        double betaMLEin = rss.getBetaMLEin();
        double betaMLEout = rss.getBetaMLEout();
        double dEngy = iGlm.nllMean(iImageData, mu, iGlm.priorWeights(iImageData));
        double rEngy = SegmentationFunctions.calculateRegularizerEnergy(mask);
        double ttEngy = iLambdaData * dEngy + iLambdaReg * rEngy;
        // <------------------------- Energy
        
        for (int i = 0; i < iNoOfIterations; ++i) {
            // Matlab's Energy Grad ------------------>
            Matrix w_g0 = null;
            switch (iNoiseType) {
                case GAUSSIAN:
                    w_g0 = iImageData.copy().sub(mu).scale(2*(betaMLEout - betaMLEin));
                    break;
                case POISSON:
                    w_g0 = iImageData.copy().ones().sub(iImageData).elementDiv(mu).scale(betaMLEin-betaMLEout);
                    break;
                default:
                    new RuntimeException("Uknown NoiseType: [" + iNoiseType + "]");
                    break;
            }
            System.out.println(w_g0);
            
            Matrix phiPts2 = SegmentationFunctions.calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPhi);
            Matrix psiPts2 = SegmentationFunctions.calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPsi);
            Matrix der_phi = calculateDiffDirac(phiPts2).elementMult(calculateHeavySide(psiPts2));
            System.out.println(der_phi);
            Matrix rEngyPhi = calculateRegularizerEnergyMatrix(der_phi);
            System.out.println(rEngyPhi);
            Matrix w_g = w_g0.copy().elementMult(der_phi).add( rEngyPhi.copy().scale(iLambdaReg) ).normalize();
            System.out.println(w_g);
            w_g = Matlab.imfilterSymmetric(w_g, iPsf);
            System.out.println(w_g);
            // -------------------------< Energy Grad
        }
    }
    
    void initialize() {
        iStepSize = (int) Math.pow(2, iCoeffsStepScale);

        // --------------------------------------------------------------
        // Normalize and resize original image according to step size
        iOriginalWidth = iImage[0].length;
        iOriginalHeight = iImage.length;
        int width = (int) (Math.ceil((float)iOriginalWidth/iStepSize) * iStepSize);
        int height = (int) (Math.ceil((float)iOriginalHeight/iStepSize) * iStepSize);
        double[][] img = new double[height][width];
        ImgUtils.imgResize(iImage, img);
        ImgUtils.normalize(img);
        iImageData = new Matrix(img);

        // --------------------------------------------------------------
        // IMM data (subpixel sumpling, regularizer term are already given)
        
        // Generate Generalized Linear Model
        switch(iNoiseType) {
            case POISSON:
                iGlm = new GlmPoisson();
                break;
            case GAUSSIAN:
            default:
                iGlm = new GlmGaussian();
                break;
        }
        
        // Generate PSF
        switch(iPsfType) {
            case DARK_FIELD:
                // Intentionally falling down
            case GAUSSIAN:
                iPsf = GaussPsf.generate(iPsfSize.width, iPsfSize.height, 0.5);
                break;
            case PHASE_CONTRAST:
                iPsf = PhaseContrastPsf.generate(2.9, 0.195, 3);
                break;
            case NONE:
                iPsf = new Matrix(new double[][] {{1}});
                break;
            default:
                iPsf = GaussPsf.generate(4, 4, 0.5);
                break;
         }
        
        iLambdaData = 1;
        iWeightBoundary = iImageData.copy().ones();
        
        // --------------------------------------------------------------
        // VLS model
        iPsi = SegmentationFunctions.generatePsi(iImageData.numCols(), iImageData.numRows(), iStepSize);
        iPhi = SegmentationFunctions.generatePhi(iImageData.numCols(), iImageData.numRows(), iStepSize);
        iEnergyGradOfBases = EnergyGriadientsOfBasesFunctions.getEnergyGradients(iCoeffsStepScale);
    }
    
    
}
