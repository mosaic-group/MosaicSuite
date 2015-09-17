package mosaic.utils.math.generalizedLinearModel;

import mosaic.utils.math.MFunc;
import mosaic.utils.math.Matrix;

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
        Matrix denominator = new Matrix(aMu).add(Math.ulp(1.0));
        Matrix temp = new Matrix(aImage).add(Math.ulp(1.0)).elementDiv( denominator);
        temp.process(new MFunc() {
            // This part is different from algorithm in Matlab. It happens that we have here small negative
            // numbers. Matlab handles that via complex numbers but here... we just put small numbers just
            // to continue without bigger problems. Such elements happen only in places where image is
            // equall to 0 so they are zerod anyway. That is why '1' is set for such places. After log
            // it becomes 0 and let calculations to proceed.
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return aElement > 0 ? aElement : 1;
            }});

        Matrix nll = temp.log();

        nll.elementMult(aImage).add(aMu).sub(aImage).elementMult(aWeights);

        return nll.sum();
    }

    @Override
    public Matrix priorWeights(Matrix aImage) {
        return new Matrix(aImage.numRows(), aImage.numCols()).ones();
    }
}
