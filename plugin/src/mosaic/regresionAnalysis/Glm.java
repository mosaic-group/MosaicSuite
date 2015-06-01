package mosaic.regresionAnalysis;

import mosaic.math.Matrix;

public interface Glm {
	enum NoiseType {GAUSSIAN, POISSON};
	
	double link(double aX);
	double linkDerivative(double aX);
	double linkInverse(double aX);
	double varFunction(double aX);
	double nllMean(Matrix aImage, Matrix aMu, Matrix aWeights);
	NoiseType flag();
}
