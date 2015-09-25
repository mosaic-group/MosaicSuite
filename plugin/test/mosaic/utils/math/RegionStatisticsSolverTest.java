package mosaic.utils.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.generalizedLinearModel.Glm;
import mosaic.utils.math.generalizedLinearModel.GlmGaussian;
import mosaic.utils.math.generalizedLinearModel.GlmPoisson;

import org.junit.Test;

/**
 * This class is responsible for testing {@link RegionStatisticsSolver} class.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class RegionStatisticsSolverTest extends CommonBase {

    @Test
    public void testRssWithGaussianGlm() {
        // expectations taken from Matlab's RegionStatisticsSolver
        final Matrix expectedImageModel = new Matrix(new double[][] {{18, 17, 16}, {15, 14, 13}, {12, 11, 10}});
        final double expectedMLEout = 9.0;
        final double expectedMLEin = 10.0;

        // Input data
        final Matrix image = new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        final Matrix mask = new Matrix(new double[][] {{9, 8, 7}, {6, 5, 4}, {3, 2, 1}});
        final Glm glm = new GlmGaussian();

        final RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 4);
        final Matrix resultImageModel = rss.calculate().getModelImage();

        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.0001));
        assertEquals("BetaMLEout", expectedMLEout, rss.getBetaMLEout(), 0.001);
        assertEquals("BetaMLEin", expectedMLEin, rss.getBetaMLEin(), 0.001);

    }

    @Test
    public void testRssWithPoissonGlm() {
        // expectations taken from Matlab's RegionStatisticsSolver
        final Matrix expectedImageModel = new Matrix(new double[][] {{1.4, 1.4, 1.4}, {1.2, 1.2, 1.2}, {1.6, 1.6, 1.6}});
        final double expectedMLEout = 1.15;
        final double expectedMLEin = 1.65;

        // Input data
        final Matrix image = new Matrix(new double[][] {{1, 1, 1}, {1.5, 1.5, 1.5}, {2, 2, 2}});
        final Matrix mask = new Matrix(new double[][] {{0.5, 0.5, 0.5}, {0.1, 0.1, 0.1}, {0.9, 0.9, 0.9}});
        final Glm glm = new GlmPoisson();

        final RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 1);
        final Matrix resultImageModel = rss.calculate().getModelImage();

        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.0001));
        assertEquals("BetaMLEout", expectedMLEout, rss.getBetaMLEout(), 0.001);
        assertEquals("BetaMLEin", expectedMLEin, rss.getBetaMLEin(), 0.001);
    }

    @Test
    public void testRssWithPoissonGlm2() {
        // expectations taken from Matlab's RegionStatisticsSolver
        final Matrix expectedImageModel = new Matrix(new double[][] {{0.111, 0.111, 0.111}, {-0.011, -0.011, -0.011}, {0.233, 0.233, 0.233}});
        final double expectedMLEout = -0.042;
        final double expectedMLEin = 0.264;

        // Input data
        final Matrix image = new Matrix(new double[][] {{0, 0, 0}, {0, 1, 0}, {0, 0, 0}});
        final Matrix mask = new Matrix(new double[][] {{0.5, 0.5, 0.5}, {0.1, 0.1, 0.1}, {0.9, 0.9, 0.9}});
        final Glm glm = new GlmPoisson();

        final RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 4).calculate();
        final Matrix resultImageModel = rss.getModelImage();

        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.001));
        assertEquals("BetaMLEout", expectedMLEout, rss.getBetaMLEout(), 0.001);
        assertEquals("BetaMLEin", expectedMLEin, rss.getBetaMLEin(), 0.001);
    }

    @Test
    public void testZeroImage() {
        final Matrix expectedImageModel = new Matrix(new double[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});

        // Input data
        final Matrix image = new Matrix(new double[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
        final Matrix mask = new Matrix(new double[][] {{0, 1, 0}, {0, 0, 0}, {0, 0, 0}});
        final Glm glm = new GlmPoisson();

        final RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 3);
        final Matrix resultImageModel = rss.calculate().getModelImage();

        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.0001));
    }


}
