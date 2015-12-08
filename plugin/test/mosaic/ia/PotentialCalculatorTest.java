package mosaic.ia;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import mosaic.ia.Potential.PotentialType;


public class PotentialCalculatorTest {

    @Test
    public void testCalculateWithAndWithoutEpsilon() {
        double epsilon = 0.000001;
        
        {   // STEP
            PotentialCalculator pc = new PotentialCalculator(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2},  PotentialType.STEP);
            
            pc.calculate();
            assertEquals(-12.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.0, -3.0, -3.0, -3.0, 0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {20.085536923187668, 20.085536923187668, 20.085536923187668, 20.085536923187668, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWOEpsilon();
            assertEquals(-4.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-1.0, -1.0, -1.0, -1.0, 0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {2.718281828459045, 2.718281828459045, 2.718281828459045, 2.718281828459045, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);
        }
        {   // HERNQUIST
            PotentialCalculator pc = new PotentialCalculator(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2},  PotentialType.HERNQUIST);
            
            pc.calculate();
            assertEquals(-23.557142857142857, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-10.5, -4.5, -3.0, -2.0, -1.5, -1.2000000000000002, -0.8571428571428571}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {36315.502674246636, 90.01713130052181, 20.085536923187668, 7.38905609893065, 4.4816890703380645, 3.320116922736548, 2.3564184423836605}, pc.getGibbsPotential(), epsilon);

            pc.calculateWOEpsilon();
            assertEquals(-7.852380952380953, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.5, -1.5, -1.0, -0.6666666666666666, -0.5, -0.4, -0.2857142857142857}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {33.11545195869231, 4.4816890703380645, 2.718281828459045, 1.9477340410546757, 1.6487212707001282, 1.4918246976412703, 1.33071219744735}, pc.getGibbsPotential(), epsilon);
        }
        {   // L1
            PotentialCalculator pc = new PotentialCalculator(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2},  PotentialType.L1);
          
            pc.calculate();
            assertEquals(-19.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-10.5, -4.5, -3.0, -1.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {36315.502674246636, 90.01713130052181, 20.085536923187668, 4.4816890703380645, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWOEpsilon();
            assertEquals(-6.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.5, -1.5, -1.0, -0.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {33.11545195869231, 4.4816890703380645, 2.718281828459045, 1.6487212707001282, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);
        }
        {   // L2
            PotentialCalculator pc = new PotentialCalculator(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2},  PotentialType.L2);
          
            pc.calculate();
            assertEquals(-10.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.0, -3.0, -3.0, -1.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {20.085536923187668, 20.085536923187668, 20.085536923187668, 4.4816890703380645, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWOEpsilon();
            assertEquals(-3.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-1.0, -1.0, -1.0, -0.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {2.718281828459045, 2.718281828459045, 2.718281828459045, 1.6487212707001282, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);
        }
        {   // PlUMMER
            PotentialCalculator pc = new PotentialCalculator(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2},  PotentialType.PlUMMER);
          
            pc.calculate();
            assertEquals(-16.58287453429739, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.0, -3.0, -3.0, -2.6832815729997477, -2.121320343559643, -1.6641005886756874, -1.1141720290623112}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {20.085536923187668, 20.085536923187668, 20.085536923187668, 14.633033961614853, 8.342144716476799, 5.280921392640696, 3.047044270363581}, pc.getGibbsPotential(), epsilon);

            pc.calculateWOEpsilon();
            assertEquals(-5.527624844765796, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-1.0, -1.0, -1.0, -0.8944271909999159, -0.7071067811865476, -0.5547001962252291, -0.3713906763541037}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {2.718281828459045, 2.718281828459045, 2.718281828459045, 2.445934334917087, 2.0281149816474726, 1.7414188225684233, 1.4497493455535544}, pc.getGibbsPotential(), epsilon);
        }
        {   // NONPARAM
            Potential.NONPARAM_WEIGHT_SIZE = 41;
            Potential.NONPARAM_SMOOTHNESS = 0.1;
            Potential.initializeNonParamWeights(0, 2);
            
            PotentialCalculator pc = new PotentialCalculator(new double[] {0, .05, .1, .15, .2}, new double[] {2, 3},  PotentialType.NONPARAM);
         
            pc.calculate();
            assertEquals(8.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {3.0, 2.0, 3.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.049787068367863944, 0.1353352832366127, 0.049787068367863944, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWOEpsilon();
            assertEquals(8.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {3.0, 2.0, 3.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.049787068367863944, 0.1353352832366127, 0.049787068367863944, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);        
        }
        {   // COULOMB
            PotentialCalculator pc = new PotentialCalculator(new double[] {-4, -2, -1, 1, 2, 4}, new double[] {3, 2},  PotentialType.COULOMB);
          
            pc.calculate();
            assertEquals(31.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {0.75, 3.0, 12.0, 12.0, 3.0, 0.75}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.4723665527410147, 0.049787068367863944, 6.14421235332821E-6, 6.14421235332821E-6, 0.049787068367863944, 0.4723665527410147}, pc.getGibbsPotential(), epsilon);

            pc.calculateWOEpsilon();
            assertEquals(10.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {0.25, 1.0, 4.0, 4.0, 1.0, 0.25}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.7788007830714049, 0.36787944117144233, 0.01831563888873418, 0.01831563888873418, 0.36787944117144233, 0.7788007830714049}, pc.getGibbsPotential(), epsilon);
        }
    }

}
