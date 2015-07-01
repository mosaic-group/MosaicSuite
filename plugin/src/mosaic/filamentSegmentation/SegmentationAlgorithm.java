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

import mosaic.generalizedLinearModel.Glm;
import mosaic.generalizedLinearModel.GlmGaussian;
import mosaic.generalizedLinearModel.GlmPoisson;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.math.RegionStatisticsSolver;
import mosaic.nurbs.BSplineSurface;
import mosaic.plugins.utils.ImgUtils;

public class SegmentationAlgorithm {
    // Possible noise type of input image
    public static enum NoiseType {GAUSSIAN, POISSON};
    
    // PSF kernel type / Microscopy type
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
    
    public SegmentationAlgorithm(double[][] aImage, NoiseType aNoiseType, PsfType aPsfType, Dimension aPsfSize, double aSubpixelSampling, int aCoeffsStepScale, double aRegularizerTerm) {
        this(aImage, aNoiseType, aPsfType, aPsfSize, aSubpixelSampling, aCoeffsStepScale, aRegularizerTerm, 100);
    }
    public SegmentationAlgorithm(double[][] aImage, NoiseType aNoiseType, PsfType aPsfType, Dimension aPsfSize, double aSubpixelSampling, int aCoeffsStepScale, double aRegularizerTerm, int aMaxNoOfIterations) {
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
     
        // TODO: this is temporary return value, should be changed probably
        return resultOfEnergyMinimalization.iMask.getArrayYX();
    }

    class EnergyOutput {
        EnergyOutput(Matrix aMask, RegionStatisticsSolver aRss) {iMask = aMask; iRss = aRss;}
        public Matrix iMask;
        public RegionStatisticsSolver iRss;
    }
    
    EnergyOutput minimizeEnergy() {
        // Calculate initial energy
        Matrix mask = generateMask();     
        RegionStatisticsSolver rss = generateRss(mask);
        Matrix mu = rss.getModelImage();
        double totalEnergy = calculateTotalEnergy(mask, mu);
        
        Matrix phiCoefs = new Matrix(iPhi.getCoefficients());
        Matrix psiCoefs = new Matrix(iPsi.getCoefficients());
        
        boolean isEnergyOptimal = false;
        
        for (int i = 0; i < iNoOfIterations && isEnergyOptimal == false ; ++i) {
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
                double alpha = (double)alphaCount/noOfSteps;

                // Adjust coefficients of B-splines
                phiCoefsTemp = grad_Phi.copy().scale(-alpha).add(phiCoefs).normalize();
                iPhi.setCoefficients(phiCoefsTemp.getArrayYX());
                psiCoefsTemp = grad_Psi.copy().scale(-alpha).add(psiCoefs).normalize();
                iPsi.setCoefficients(psiCoefsTemp.getArrayYX());
                
                // Recalculate energy with new coefficients
                mask = generateMask();     
                rss = generateRss(mask);
                mu = rss.getModelImage();
                double tempEngy = calculateTotalEnergy(mask, mu);

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
        
        return new EnergyOutput(mask, rss);
    }

    private Matrix[] calculateEnergyGradient(Matrix aMu, RegionStatisticsSolver aRss) {
        Matrix w_g0 = null;
        switch (iNoiseType) {
            case GAUSSIAN:
                w_g0 = iImageData.copy().sub(aMu).scale(2*(aRss.getBetaMLEout() - aRss.getBetaMLEin()));
                break;
            case POISSON:
                w_g0 = iImageData.copy().ones().sub(iImageData).elementDiv(aMu).scale(aRss.getBetaMLEin()-aRss.getBetaMLEout());
                break;
            default:
                new RuntimeException("Uknown NoiseType: [" + iNoiseType + "]");
                break;
        }

        Matrix phiPts2 = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPhi);
        Matrix psiPts2 = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), 1, iPsi);

        Matrix der_phi = calculateDiffDirac(phiPts2).elementMult(calculateHeavySide(psiPts2));
        Matrix der_psi = calculateDirac(phiPts2).elementMult(calculateDirac(psiPts2));
        Matrix grad_Phi = CalculateEnergyGradient(w_g0, der_phi).transpose();
        Matrix grad_Psi = CalculateEnergyGradient(w_g0, der_psi).transpose();
        
        return new Matrix[] {grad_Phi, grad_Psi};
    }

    private double calculateTotalEnergy(Matrix mask, Matrix mu) {
        double dataEnergy = iGlm.nllMean(iImageData, mu, iGlm.priorWeights(iImageData));
        double regularizerEnergy = calculateRegularizerEnergy(mask, iWeightBoundary);
        double totalEnergy = iLambdaData * dataEnergy + iLambdaReg * regularizerEnergy;
        
        return totalEnergy;
    }

    private RegionStatisticsSolver generateRss(Matrix mask) {
        Matrix kmask = Matlab.imfilterSymmetric(mask, iPsf);
        RegionStatisticsSolver rss = new RegionStatisticsSolver(iImageData, kmask, iGlm, iImageData, 6).calculate();

        return rss;
    }

    private Matrix generateMask() {
        Matrix phiPts = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPhi);
        Matrix psiPts = calculateBSplinePoints(iImageData.numCols(), iImageData.numRows(), iSubpixelSampling, iPsi);
        Matrix mask = SegmentationFunctions.generateMask(phiPts, psiPts);
        mask = Matlab.imresize(mask, iSubpixelSampling);
        
        return mask;
    }

    private Matrix CalculateEnergyGradient(Matrix w_g0, Matrix der_phi) {
        Matrix rEngyPhi = calculateRegularizerEnergyMatrix(der_phi, iWeightBoundary);
        Matrix w_g = w_g0.copy().elementMult(der_phi).add( rEngyPhi.copy().scale(iLambdaReg) ).normalize();
        w_g = Matlab.imfilterSymmetric(w_g, iPsf);
        Matrix grad_Phi = Matlab.imfilterSymmetric(Matlab.imfilterSymmetric(w_g, iEnergyGradOfBases.copy().transpose()), iEnergyGradOfBases);
        grad_Phi = grad_Phi.resize(0, 0, iStepSize, iStepSize);
        return grad_Phi;
    }
    
    /**
     * Initializes all data needed for segmentation (PSF, GLM, IMM, B-Spline surfaces, ...) and prepares input image.
     */
    private void initialize() {
        iStepSize = (int) Math.pow(2, iCoeffsStepScale);

        // --------------------------------------------------------------
        // Normalize and resize original image according to step size
        iOriginalWidth = iImage[0].length;
        iOriginalHeight = iImage.length;
        int width = (int) (Math.ceil((double)iOriginalWidth/iStepSize) * iStepSize);
        int height = (int) (Math.ceil((double)iOriginalHeight/iStepSize) * iStepSize);
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
                // sigma 0.5 follows default value for sigma in Matlab's command 'fspecial'
                iPsf = GaussPsf.generate(iPsfSize.width, iPsfSize.height, 0.5);
                break;
            case PHASE_CONTRAST:
                // Values taken from Matlab's implementation
                iPsf = PhaseContrastPsf.generate(2.9, 0.195, 3);
                break;
            case NONE:
                iPsf = new Matrix(new double[][] {{1}});
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
