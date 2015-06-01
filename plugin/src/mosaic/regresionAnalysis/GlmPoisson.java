package mosaic.regresionAnalysis;

import mosaic.math.Matrix;

public class GlmPoisson implements Glm {

	@Override
	public double link(double aX) {
		return aX;
	}

	@Override
	public double linkDerivative(double aX) {
		return 1;
	}

	@Override
	public double linkInverse(double aX) {
		return aX;
	}

	@Override
	public double varFunction(double aX) {
		return aX;
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
