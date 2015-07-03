package mosaic.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import mosaic.generalizedLinearModel.Glm;
import mosaic.generalizedLinearModel.GlmGaussian;
import mosaic.generalizedLinearModel.GlmPoisson;
import mosaic.test.framework.CommonBase;

import org.junit.Test;

/** 
 * This class is responsible for testing {@link RegionStatisticsSolver} class. 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class RegionStatisticsSolverTest extends CommonBase {

    @Test
    public void testRssWithGaussianGlm() {
        // expectations taken from Matlab's RegionStatisticsSolver
        Matrix expectedImageModel = new Matrix(new double[][] {{18, 17, 16}, {15, 14, 13}, {12, 11, 10}});
        double expectedMLEout = 9.0;
        double expectedMLEin = 10.0;
        
        // Input data
        Matrix image = new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        Matrix mask = new Matrix(new double[][] {{9, 8, 7}, {6, 5, 4}, {3, 2, 1}});
        Glm glm = new GlmGaussian();
        
        RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 4);
        Matrix resultImageModel = rss.calculate().getModelImage();

        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.0001));
        assertEquals("BetaMLEout", expectedMLEout, rss.getBetaMLEout(), 0.001);
        assertEquals("BetaMLEin", expectedMLEin, rss.getBetaMLEin(), 0.001);
        
    }
    
    @Test
    public void testRssWithPoissonGlm() {
        // expectations taken from Matlab's RegionStatisticsSolver
        Matrix expectedImageModel = new Matrix(new double[][] {{1.4, 1.4, 1.4}, {1.2, 1.2, 1.2}, {1.6, 1.6, 1.6}});
        double expectedMLEout = 1.15;
        double expectedMLEin = 1.65;
        
        // Input data
        Matrix image = new Matrix(new double[][] {{1, 1, 1}, {1.5, 1.5, 1.5}, {2, 2, 2}});
        Matrix mask = new Matrix(new double[][] {{0.5, 0.5, 0.5}, {0.1, 0.1, 0.1}, {0.9, 0.9, 0.9}});
        Glm glm = new GlmPoisson();
        
        RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 1);
        Matrix resultImageModel = rss.calculate().getModelImage();
        
        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.0001));
        assertEquals("BetaMLEout", expectedMLEout, rss.getBetaMLEout(), 0.001);
        assertEquals("BetaMLEin", expectedMLEin, rss.getBetaMLEin(), 0.001);
    }
    
    @Test
    public void testRssWithPoissonGlm2() {
        // expectations taken from Matlab's RegionStatisticsSolver
        Matrix expectedImageModel = new Matrix(new double[][] {{0.111, 0.111, 0.111}, {-0.011, -0.011, -0.011}, {0.233, 0.233, 0.233}});
        double expectedMLEout = -0.042;
        double expectedMLEin = 0.264;
        
        // Input data
        Matrix image = new Matrix(new double[][] {{0, 0, 0}, {0, 1, 0}, {0, 0, 0}});
        Matrix mask = new Matrix(new double[][] {{0.5, 0.5, 0.5}, {0.1, 0.1, 0.1}, {0.9, 0.9, 0.9}});
        Glm glm = new GlmPoisson();
        
        RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 4).calculate();
        Matrix resultImageModel = rss.getModelImage();

        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.001));
        assertEquals("BetaMLEout", expectedMLEout, rss.getBetaMLEout(), 0.001);
        assertEquals("BetaMLEin", expectedMLEin, rss.getBetaMLEin(), 0.001);
    }
    
    @Test
    public void testZeroImage() {
        Matrix expectedImageModel = new Matrix(new double[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
        
        // Input data
        Matrix image = new Matrix(new double[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
        Matrix mask = new Matrix(new double[][] {{0, 1, 0}, {0, 0, 0}, {0, 0, 0}});
        Glm glm = new GlmPoisson();
        
        RegionStatisticsSolver rss = new RegionStatisticsSolver(image, mask, glm, image, 3);
        Matrix resultImageModel = rss.calculate().getModelImage();
        
        assertTrue("Image Model should match", expectedImageModel.compare(resultImageModel, 0.0001));
    }

    
}
