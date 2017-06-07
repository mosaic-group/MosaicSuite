package mosaic.ia;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.scijava.vecmath.Point3d;

import ij.macro.Interpreter;
import mosaic.ia.Analysis.Result;
import mosaic.ia.Potentials.PotentialType;
import mosaic.test.framework.CommonBase;

/**
 * Tests of IA
 * TODO: These are not 'real' tests. They just written to help refactor code and
 * finally *make* it testable. They cover around 3/4 of IA which should be enough
 * to quite safely start refactoring.
 */
public class AnalysisTest extends CommonBase {

    @Test
    public void testCalcDist() {
        ArrayList<Point3d> xl = new ArrayList<Point3d>();
        for (int i = 0; i < 5; i++) {
            xl.add(new Point3d(i, 2, 0));
        }
        Point3d[] x = xl.toArray(new Point3d[0]);
        ArrayList<Point3d> yl = new ArrayList<Point3d>();
        for (int i = 0; i < 5; i += 2) {
            yl.add(new Point3d(i, i, 0));
        }
        Point3d[] y = yl.toArray(new Point3d[0]);

        Analysis analysis = new Analysis();
        analysis.calcDist(0.5, 0.001, 2.974, null, x, y, 0, 5, 0, 5, 0, 0);
        
        double epsilon = 1e-10;
        assertEquals(0, analysis.getMinDistance(), epsilon);
        assertEquals(2, analysis.getMaxDistance(), epsilon);
        assertArrayEquals(new double[] {2, 1, 0, 1, 2}, analysis.getDistances(), epsilon);
    }

    @Test
    public void testCmaOptimizationHernquitst() {
        Analysis analysis = prepereIaForTest();
        
        double epsilon = 1e-6;
        assertEquals(0.418188, analysis.getMinDistance(), epsilon);
        assertEquals(112.924864, analysis.getMaxDistance(), epsilon);
        assertEquals(77.722986, Analysis.calcWekaWeights(analysis.getDistances()), epsilon);
        analysis.setPotentialType(Potentials.createPotential(PotentialType.HERNQUIST));
        List<Result> results = new ArrayList<Result>();
        analysis.cmaOptimization(results, 1, true);
        
        epsilon = 1e-6;
        assertEquals(36.723224, results.get(0).iStrength, epsilon);
        assertEquals(241.636178, results.get(0).iThresholdScale, epsilon);
        assertEquals(0.001396, results.get(0).iResidual, epsilon);
        
        // No testing anything - just running to catch any unwanted null things..
        analysis.hypothesisTesting(100, 0.01);
    }

    private Analysis prepereIaForTest() {
        // Make IJ running in batch mode (no GUI)
        Interpreter.batchMode = true;
        
        // Define test data
        final String tcDirName = "IA/VirusEndosome/";
        copyTestResources("Virus.csv", getTestDataPath() + tcDirName, "/tmp");
        Point3d[] x = FileUtils.openCsvFile("X", "/tmp/" + "Virus.csv");
        copyTestResources("Endosome.csv", getTestDataPath() + tcDirName, "/tmp");
        Point3d[] y = FileUtils.openCsvFile("Y", "/tmp/" + "Endosome.csv");
        
        Analysis analysis = new Analysis();
        analysis.calcDist(0.5, 0.001, 35.9, null, x, y, 0, 385, 0, 511, 0, 0);
        return analysis;
    }
    
    @Test
    public void testCmaOptimizationNONPARAM() {
        Analysis analysis = prepereIaForTest();
        
        double epsilon = 1e-6;
        assertEquals(0.418188, analysis.getMinDistance(), epsilon);
        assertEquals(112.924864, analysis.getMaxDistance(), epsilon);
        assertEquals(77.722986, Analysis.calcWekaWeights(analysis.getDistances()), epsilon);
        analysis.setPotentialType(Potentials.createPotential(PotentialType.NONPARAM, analysis.getMinDistance(), analysis.getMaxDistance(), 41, 0.1));
        
        List<Result> results = new ArrayList<Result>();
        analysis.cmaOptimization(results, 1, true);

        epsilon = 1e-6;
        assertEquals(0.0, results.get(0).iStrength, epsilon);
        assertEquals(0.0, results.get(0).iThresholdScale, epsilon);
        assertEquals(0.007918, results.get(0).iResidual, epsilon);
        
        // No testing anything - just running to catch any unwanted null things..
        analysis.hypothesisTesting(100, 0.01);
    }
    
    @Test
    public void testCmaOptimizationSTEP() {
        Analysis analysis = prepereIaForTest();
        
        double epsilon = 1e-6;
        assertEquals(0.418188, analysis.getMinDistance(), epsilon);
        assertEquals(112.924864, analysis.getMaxDistance(), epsilon);
        assertEquals(77.722986, Analysis.calcWekaWeights(analysis.getDistances()), epsilon);
        analysis.setPotentialType(Potentials.createPotential(PotentialType.STEP));
        
        List<Result> results = new ArrayList<Result>();
        analysis.cmaOptimization(results, 1, true);
        
        epsilon = 1e-6;
        assertEquals(2.439648, results.get(0).iStrength, epsilon);
        assertEquals(6.678545, results.get(0).iThresholdScale, epsilon);
        assertEquals(0.002117, results.get(0).iResidual, epsilon);
        
        // No testing anything - just running to catch any unwanted null things..
        analysis.hypothesisTesting(100, 0.01);
    }
}
