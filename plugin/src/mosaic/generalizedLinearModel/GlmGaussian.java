package mosaic.generalizedLinearModel;

import mosaic.math.Matrix;

public class GlmGaussian implements Glm {

	@Override
	public Matrix link(Matrix aX) {
		return aX.copy();
	}

	@Override
	public Matrix linkDerivative(Matrix aX) {
		return new Matrix(aX.numRows(), aX.numCols()).ones();
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
		Matrix nll = new Matrix(aWeights).elementMult( (new Matrix(aImage).sub(aMu)).pow2() );
		return nll.sum();
	}

	@Override
	public Matrix priorWeights(Matrix aImage) {
	   return new Matrix(aImage.numRows(), aImage.numCols()).ones();
	}
	
	@Override
	public NoiseType flag() {
		return NoiseType.GAUSSIAN;
	}

}
