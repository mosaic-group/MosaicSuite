package mosaic.ia;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import mosaic.ia.Potentials.Potential;
import mosaic.ia.Potentials.PotentialType;


public class PotentialCalculatorTest {

    @Test
    public void testCalculateWithAndWithoutEpsilon() {
        double epsilon = 0.000001;
        
        {   // STEP
            Potential pc = Potentials.createPotential(PotentialType.STEP);
            
            assertEquals(PotentialType.STEP, pc.getType());
            
            pc.calculate(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-12.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.0, -3.0, -3.0, -3.0, 0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {20.085536923187668, 20.085536923187668, 20.085536923187668, 20.085536923187668, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWithoutEpsilon(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-4.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-1.0, -1.0, -1.0, -1.0, 0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {2.718281828459045, 2.718281828459045, 2.718281828459045, 2.718281828459045, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);
        }
        {   // NONPARAM
            Potential pc = Potentials.createPotential(PotentialType.NONPARAM, 0, 2, 41, 0.1);
            
            assertEquals(PotentialType.NONPARAM, pc.getType());
            
            pc.calculate(new double[] {0, .05, .1, .15, .2}, new double[] {2, 3});
            assertEquals(8.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {3.0, 2.0, 3.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.049787068367863944, 0.1353352832366127, 0.049787068367863944, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWithoutEpsilon(new double[] {0, .05, .1, .15, .2}, new double[] {2, 3});
            assertEquals(8.0, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {3.0, 2.0, 3.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.049787068367863944, 0.1353352832366127, 0.049787068367863944, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);        
        }
        {   // HERNQUIST
            Potential pc = Potentials.createPotential(PotentialType.HERNQUIST);
            
            assertEquals(PotentialType.HERNQUIST, pc.getType());
            
            pc.calculate(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-23.557142857142857, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-10.5, -4.5, -3.0, -2.0, -1.5, -1.2000000000000002, -0.8571428571428571}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {36315.502674246636, 90.01713130052181, 20.085536923187668, 7.38905609893065, 4.4816890703380645, 3.320116922736548, 2.3564184423836605}, pc.getGibbsPotential(), epsilon);

            pc.calculateWithoutEpsilon(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-7.852380952380953, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.5, -1.5, -1.0, -0.6666666666666666, -0.5, -0.4, -0.2857142857142857}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {33.11545195869231, 4.4816890703380645, 2.718281828459045, 1.9477340410546757, 1.6487212707001282, 1.4918246976412703, 1.33071219744735}, pc.getGibbsPotential(), epsilon);
        }
        {   // L1
            Potential pc = Potentials.createPotential(PotentialType.L1);
            
            assertEquals(PotentialType.L1, pc.getType());
            
            pc.calculate(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-19.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-10.5, -4.5, -3.0, -1.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {36315.502674246636, 90.01713130052181, 20.085536923187668, 4.4816890703380645, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWithoutEpsilon(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-6.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.5, -1.5, -1.0, -0.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {33.11545195869231, 4.4816890703380645, 2.718281828459045, 1.6487212707001282, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);
        }
        {   // L2
            Potential pc = Potentials.createPotential(PotentialType.L2);
            
            assertEquals(PotentialType.L2, pc.getType());
            
            pc.calculate(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-10.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.0, -3.0, -3.0, -1.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {20.085536923187668, 20.085536923187668, 20.085536923187668, 4.4816890703380645, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);

            pc.calculateWithoutEpsilon(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-3.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-1.0, -1.0, -1.0, -0.5, -0.0, 0.0, 0.0}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {2.718281828459045, 2.718281828459045, 2.718281828459045, 1.6487212707001282, 1.0, 1.0, 1.0}, pc.getGibbsPotential(), epsilon);
        }
        {   // PlUMMER
            Potential pc = Potentials.createPotential(PotentialType.PlUMMER);
            
            assertEquals(PotentialType.PlUMMER, pc.getType());
            
            pc.calculate(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-16.58287453429739, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-3.0, -3.0, -3.0, -2.6832815729997477, -2.121320343559643, -1.6641005886756874, -1.1141720290623112}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {20.085536923187668, 20.085536923187668, 20.085536923187668, 14.633033961614853, 8.342144716476799, 5.280921392640696, 3.047044270363581}, pc.getGibbsPotential(), epsilon);

            pc.calculateWithoutEpsilon(new double[] {-5, -1, 0, 1, 2, 3, 5}, new double[] {3, 2});
            assertEquals(-5.527624844765796, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {-1.0, -1.0, -1.0, -0.8944271909999159, -0.7071067811865476, -0.5547001962252291, -0.3713906763541037}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {2.718281828459045, 2.718281828459045, 2.718281828459045, 2.445934334917087, 2.0281149816474726, 1.7414188225684233, 1.4497493455535544}, pc.getGibbsPotential(), epsilon);
        }
        {   // COULOMB
            Potential pc = Potentials.createPotential(PotentialType.COULOMB);
            
            assertEquals(PotentialType.COULOMB, pc.getType());
            
            pc.calculate(new double[] {-4, -2, -1, 1, 2, 4}, new double[] {3, 2});
            assertEquals(31.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {0.75, 3.0, 12.0, 12.0, 3.0, 0.75}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.4723665527410147, 0.049787068367863944, 6.14421235332821E-6, 6.14421235332821E-6, 0.049787068367863944, 0.4723665527410147}, pc.getGibbsPotential(), epsilon);

            pc.calculateWithoutEpsilon(new double[] {-4, -2, -1, 1, 2, 4}, new double[] {3, 2});
            assertEquals(10.5, pc.getSumPotential(), epsilon);
            assertArrayEquals(new double[] {0.25, 1.0, 4.0, 4.0, 1.0, 0.25}, pc.getPotential(), epsilon);
            assertArrayEquals(new double[] {0.7788007830714049, 0.36787944117144233, 0.01831563888873418, 0.01831563888873418, 0.36787944117144233, 0.7788007830714049}, pc.getGibbsPotential(), epsilon);
        }
    }
}
