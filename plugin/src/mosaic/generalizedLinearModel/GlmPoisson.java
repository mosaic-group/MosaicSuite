package mosaic.generalizedLinearModel;

import mosaic.math.Matrix;

public class GlmPoisson implements Glm {

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
	    return aX.copy();
	}

	@Override
	public double nllMean(Matrix aImage, Matrix aMu, Matrix aWeights) {
		// nll = weights .* ( image .* log((image+eps)./(mu+eps)) + mu - image );
		// snll = sum(nll(:));
		Matrix snll = new Matrix(aImage).add(Double.MIN_VALUE).elementDiv(new Matrix(aMu).add(Double.MIN_VALUE)).log();
		snll.elementMult(aImage).add(aMu).sub(aImage).elementMult(aWeights);
		return snll.sum();
	}

	@Override
	public NoiseType flag() {
		return NoiseType.POISSON;
	}


}
