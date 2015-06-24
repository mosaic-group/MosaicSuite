package mosaic.generalizedLinearModel;

import mosaic.math.Matrix;

/**
 * Interface providing common operations for Generalized Linear Models
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public interface Glm {
	enum NoiseType {GAUSSIAN, POISSON};
	
	Matrix link(Matrix aX);
	Matrix linkDerivative(Matrix aX);
	Matrix linkInverse(Matrix aX);
	Matrix varFunction(Matrix aX);
	double nllMean(Matrix aImage, Matrix aMu, Matrix aWeights);
	Matrix priorWeights(Matrix aImage);
	
	NoiseType flag();
}
