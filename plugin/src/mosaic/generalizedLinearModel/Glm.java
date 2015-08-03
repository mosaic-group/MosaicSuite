package mosaic.generalizedLinearModel;

import mosaic.math.Matrix;

/**
 * Interface providing common operations for Generalized Linear Models
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public interface Glm {
	Matrix link(Matrix aX);
	double linkDerivative(Matrix aX);
	Matrix linkInverse(Matrix aX);
	Matrix varFunction(Matrix aX);
	double nllMean(Matrix aImage, Matrix aMu, Matrix aWeights);
	Matrix priorWeights(Matrix aImage);
}
