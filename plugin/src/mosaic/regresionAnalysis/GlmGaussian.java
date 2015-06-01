package mosaic.regresionAnalysis;

public class GlmGaussian implements GLM {

	@Override
	public float link(float aX) {
		return aX;
	}

	@Override
	public float linkDerivative(float aX) {
		return 1;
	}

	@Override
	public float linkInverse(float aX) {
		return aX;
	}

	@Override
	public float varFunction(float aX) {
		return 1;
	}

	@Override
	public float nllMean(float[][] aImage, float aMu) {
		float sum = 0;
		
		return 0;
	}

	@Override
	public NoiseType flag() {
		return NoiseType.GAUSSIAN;
	}


}
