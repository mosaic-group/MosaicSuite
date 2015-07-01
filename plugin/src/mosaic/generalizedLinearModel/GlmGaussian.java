package mosaic.generalizedLinearModel;

import mosaic.math.Matrix;

public class GlmGaussian implements Glm {

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
		return new Matrix(aX.numRows(), aX.numCols()).ones();
	}

	@Override
	public double nllMean(Matrix aImage, Matrix aMu, Matrix aWeights) {
	    // MATLAB:
		// nll = weights.*(image-mu).^2;
		// snll = sum(nll(:));
		Matrix nll = new Matrix(aImage).sub(aMu).pow2().elementMult(aWeights);
        return nll.sum();
	}

	@Override
	public Matrix priorWeights(Matrix aImage) {
	   return new Matrix(aImage.numRows(), aImage.numCols()).ones();
	}
}
