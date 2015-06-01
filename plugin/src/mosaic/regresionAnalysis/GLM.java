package mosaic.regresionAnalysis;

public interface GLM {
	enum NoiseType {GAUSSIAN, POISSON};
	
	float link(float aX);
	float linkDerivative(float aX);
	float linkInverse(float aX);
	float varFunction(float aX);
	float nllMean(float[][] aImage, float aMu);
	NoiseType flag();
}
