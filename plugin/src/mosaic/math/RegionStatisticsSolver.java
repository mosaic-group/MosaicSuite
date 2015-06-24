package mosaic.math;

import mosaic.generalizedLinearModel.Glm;

public class RegionStatisticsSolver {
    private Matrix iImage;
    private Matrix iMask;
    private Matrix iMu;
    private Glm iGlm;
    private int iNumOfIterations;
    
    // Calculated beta
    private double iBetaMLEout = 0.0;
    private double iBetaMLEin = 0.0;
    
    // Calculate model image
    private Matrix iModelImage;
    
    public RegionStatisticsSolver(Matrix aImage, Matrix aMask, Glm aGlm, Matrix aMuInit, int aNumOfIterations) {
        iImage = aImage;
        iMask = aMask;
        iMu = aMuInit;
        iGlm = aGlm;
        iNumOfIterations = aNumOfIterations;
        
        
    }

    public RegionStatisticsSolver calculate() {
        Matrix mu = iMu.copy();
        Matrix Z = iImage.copy().sub(mu).elementMult(iGlm.linkDerivative(mu)).add(iGlm.link(mu));
        Matrix W = iGlm.priorWeights(iImage).elementDiv(iGlm.varFunction(mu).elementMult(iGlm.linkDerivative(mu).pow2().add(Float.MIN_NORMAL)));
        System.out.println(Z);
        System.out.println(W);
        
        Matrix ones = iMask.copy().ones();
        Matrix maskSubFrom1 = ones.copy().sub(iMask);
        Matrix maskPow2 = iMask.copy().pow2();
        Matrix maskMul1SubMask = maskSubFrom1.copy().elementMult(iMask);
        Matrix pow2Of1SubMask = maskSubFrom1.copy().pow2();
        
        for (int i = 0; i < iNumOfIterations; ++i) {
            double K11 = W.copy().elementMult(pow2Of1SubMask).sum();
            double K12 = W.copy().elementMult(maskMul1SubMask).sum();
            double K22 = W.copy().elementMult(maskPow2).sum();
            
            double U1 = W.copy().elementMult(Z).elementMult(maskSubFrom1).sum();
            double U2 = W.copy().elementMult(Z).elementMult(iMask).sum();
            
            double detK = K11 * K22 - Math.pow(K12, 2);
            iBetaMLEout = (K22*U1-K12*U2)/detK;
            iBetaMLEin = (-K12*U1+K11*U2)/detK;
            System.out.println(iBetaMLEout +" "+ iBetaMLEin);
            Matrix linearPredictor = iMask.copy().scale(iBetaMLEin - iBetaMLEout).add(iBetaMLEout);
            mu = iGlm.linkInverse(linearPredictor);
            Z = iImage.copy().sub(mu).elementMult(iGlm.linkDerivative(mu)).add(iGlm.link(mu));
            W = iGlm.priorWeights(iImage).elementDiv(iGlm.varFunction(mu).elementMult(iGlm.linkDerivative(mu).pow2().add(Float.MIN_NORMAL)));
        }
        
        if (iBetaMLEout > iBetaMLEin) {
            double temp = iBetaMLEout;
            iBetaMLEout = iBetaMLEin;
            iBetaMLEin = temp;
        }
        
        Matrix linearPredictor = iMask.copy().scale(iBetaMLEin - iBetaMLEout).add(iBetaMLEout);
        mu = iGlm.linkInverse(linearPredictor);
        iModelImage = mu;
        
        return this;
    }
    
    public double getBetaMLEout() {
        return iBetaMLEout;
    }
    
    public double getBetaMLEin() {
        return iBetaMLEin;
    }
    
    public Matrix getModelImage() {
        return iModelImage;
    }
}
