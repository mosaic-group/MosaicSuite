package mosaic.generalizedLinearModel;

import mosaic.math.Matrix;

public class GlmPoisson implements Glm {

    @Override
    public Matrix link(Matrix aX) {
        return aX.copy();
    }

    @Override
    public double linkDerivative(Matrix aX) {
        return 1.0;
    }

    @Override
    public Matrix linkInverse(Matrix aX) {
        return aX.copy();
    }

	@Override
	public Matrix varFunction(Matrix aX) {
	    return aX.copy();
	}

	@Override
	public double nllMean(Matrix aImage, Matrix aMu, Matrix aWeights) {
	    // MATLAB:
		// nll = weights .* ( image .* log((image+eps)./(mu+eps)) + mu - image );
		// snll = sum(nll(:));
		Matrix nll = new Matrix(aImage).add(Math.ulp(1.0)).elementDiv(new Matrix(aMu).add(Math.ulp(1.0))).log();
		nll.elementMult(aImage).add(aMu).sub(aImage).elementMult(aWeights);
		return nll.sum();
	}

    @Override
    public Matrix priorWeights(Matrix aImage) {
       return new Matrix(aImage.numRows(), aImage.numCols()).ones();
    }
}
