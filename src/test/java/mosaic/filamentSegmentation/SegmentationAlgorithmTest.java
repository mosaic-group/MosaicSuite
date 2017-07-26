package mosaic.filamentSegmentation;

import static mosaic.filamentSegmentation.SegmentationFunctions.calcualteFilamentLenght;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mosaic.filamentSegmentation.SegmentationAlgorithm.EnergyOutput;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.ThresholdFuzzyOutput;
import mosaic.test.framework.CommonBase;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.Matrix;

import org.junit.Test;

public class SegmentationAlgorithmTest extends CommonBase {

    // 7 x 11, 1 filament, without noise
    final static double[][] simpleImage1filament = new double[][] {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},
        {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};

    @Test
    public void testEnergyMinimalizationGaussian() {
        // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                {0.000835391192304, 0.000159760981411, 0.000027858657282, 0.000007054809837, 0.000004027545170, 0.000006613751729, 0.000018012910577, 0.000032308016440, 0.000022967049639, 0.000005206856028, 0.000000271174849, 0.000000002142306},
                {0.004095842143240, 0.009765748562265, 0.017570724331280, 0.036922001086026, 0.109254243908178, 0.275516653768248, 0.276483236630647, 0.113315867747481, 0.020963214965216, 0.000818548832104, 0.000005071073433, 0.000000004534639},
                {0.006148308227110, 0.023550680903919, 0.052931011990019, 0.182442435763957, 0.855185113350170, 0.926745390418214, 0.964736034322046, 0.364714332749404, 0.098897689261131, 0.006206561170430, 0.000015200708311, 0.000000005554006},
                {0.003695067045254, 0.023527210499101, 0.068793440905883, 0.275301317657238, 0.986097299826307, 0.810788258246835, 1.000000000000000, 0.424018675167101, 0.107374458826254, 0.004870201819178, 0.000009909413003, 0.000000004090425},
                {0.000907944581849, 0.006850568625155, 0.036207957297966, 0.173354203137122, 0.694105183038105, 0.920139932670724, 0.784857141072581, 0.271129155703490, 0.039035555537214, 0.000605440597962, 0.000001924620224, 0.000000001888410},
                {0.000109452650608, 0.000440095241694, 0.001855991433643, 0.008281857161975, 0.033693904392742, 0.076952671594455, 0.062809786526967, 0.014482681345568, 0.000890528480505, 0.000018394997140, 0.000000154527561, 0.000000000567373},
                {0.000008707734865, 0.000020176992884, 0.000047915212333, 0.000110180135964, 0.000216776641141, 0.000293411323752, 0.000199249672502, 0.000052043133283, 0.000005274830637, 0.000000258736103, 0.000000007170039, 0.000000000106686},
                {0.000000599048807, 0.000001618518243, 0.000003132534780, 0.000004291399280, 0.000003979899944, 0.000002327314131, 0.000000825373748, 0.000000182379067, 0.000000027234999, 0.000000003022869, 0.000000000252023, 0},
        };
        final double expectedMLEin = 1.018718614554530;
        final double expectedMLEout = 0.007211831449837;

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(3,2),
                /* subpixel sumpling */      1,
                /* scale */                  1,
                /* regularizer term */       0.0001,
                /* no of iterations */       2);

        // Tested function
        final EnergyOutput result = sa.minimizeEnergy();

        final double epsilon = 1e-14;
        assertTrue("Mask", new Matrix(expectedMask).compare(result.iMask, epsilon));
        assertEquals("MLEin", expectedMLEin, result.iRss.getBetaMLEin(), expectedMLEin / 1e10);
        assertEquals("MLEout", expectedMLEout, result.iRss.getBetaMLEout(), expectedMLEout / 1e10);
    }

    @Test
    public void testEnergyMinimalizationPoisson() {
        // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                {0.002151431276675, 0.017863024078500, 0.035141051186090, 0.041674555577106, 0.020671801887261, 0.003636574792184, 0.000728627874825, 0.000052948141890, 0.000001708551147, 0.000000048452751, 0.000000001399065},
                {0.005615948362650, 0.047015214491254, 0.085130452554489, 0.112536805743743, 1.000000000000000, 0.481977313480429, 0.117635796629687, 0.003505766069300, 0.000014948259154, 0.000000123954962, 0.000000002361925},
                {0.005437538746889, 0.108287706602201, 0.671334499471147, 0.540962130110285, 0.789857887040152, 0.488454098887705, 0.188975237684911, 0.078905482427536, 0.000064244867116, 0.000000173316152, 0.000000002408774},
                {0.001855976381033, 0.071826548617592, 0.873001557429969, 0.690054365391901, 0.401795704024668, 0.280109941273312, 0.173723785451328, 0.052759270199597, 0.000030742205419, 0.000000095446050, 0.000000001462322},
                {0.000261890754231, 0.003893460088327, 0.022521897475944, 0.017878636573558, 0.022364150960108, 0.067843414852357, 0.051254949139446, 0.000497736433905, 0.000002045183790, 0.000000022810663, 0.000000000534253},
                {0.000020589012074, 0.000139698617516, 0.000399582529310, 0.000403212427230, 0.000359919408533, 0.000297932693146, 0.000082334158689, 0.000003809793625, 0.000000110511418, 0.000000003602608, 0.000000000113043},
                {0.000001147263659, 0.000006364173912, 0.000020625178018, 0.000034000587505, 0.000025304753895, 0.000008200726999, 0.000001274549903, 0.000000117760117, 0.000000007962056, 0.000000000432285, 0},
        };
        final double expectedMLEin = 1.582424937234086;
        final double expectedMLEout = 4.979662008561890e-05;

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.POISSON,
                PsfType.NONE,
                0.5,
                new Dimension(1, 1),
                /* subpixel sumpling */      1,
                /* scale */                  0,
                /* regularizer term */       0.0001,
                /* no of iterations */       2);

        // Tested function
        final EnergyOutput result = sa.minimizeEnergy();

        final double epsilon = 1e-14;
        assertTrue("Mask", new Matrix(expectedMask).compare(result.iMask, epsilon));
        assertEquals("MLEin", expectedMLEin, result.iRss.getBetaMLEin(), expectedMLEin/1e-9);
        assertEquals("MLEout", expectedMLEout, result.iRss.getBetaMLEout(), expectedMLEout/1e-9);
    }

    @Test
    public void testEnergyMinimalizationPhaseContrast() {
        // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                {0.0012574, 0.018805, 0.058048, 0.078214, 0.24453, 0.059303, 0.0040948, 5.1499e-05, 7.4258e-07, 2.2028e-08, 1.1164e-09},
                {0.0042905, 0.047962, 0.14078, 0.35549, 0.70792, 0.14933, 0.022423, 0.00015753, 2.4228e-06, 6.4778e-08, 2.0537e-09},
                {0.0072685, 0.056632, 0.20871, 1, 0.1853, 0.089456, 0.092038, 0.0012018, 1.0064e-05, 1.2933e-07, 2.208e-09},
                {0.0035364, 0.048778, 0.20141, 0.96579, 0.15583, 0.049161, 0.047812, 0.00053588, 4.943e-06, 7.1299e-08, 1.3286e-09},
                {0.00024194, 0.0037146, 0.036377, 0.12608, 0.058528, 0.0067124, 0.0021934, 2.8212e-05, 5.0092e-07, 1.3511e-08, 4.5773e-10},
                {9.9742e-06, 8.3335e-05, 0.00065097, 0.0017016, 0.00085838, 0.00026324, 3.4532e-05, 1.2536e-06, 4.4746e-08, 2.0475e-09, 9.4118e-11},
                {7.4777e-07, 4.6069e-06, 2.087e-05, 3.3698e-05, 1.6726e-05, 5.5526e-06, 1.1589e-06, 1.0339e-07, 6.0908e-09, 3.3311e-10, 0}
        };
        final double expectedMLEin = 0.74455;
        final double expectedMLEout = 0.19467;

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.POISSON,
                PsfType.PHASE_CONTRAST,
                0.5,
                new Dimension(1, 1),
                /* subpixel sumpling */      1,
                /* scale */                  0,
                /* regularizer term */       0.0001,
                /* no of iterations */       1);

        // Tested function
        final EnergyOutput result = sa.minimizeEnergy();

        final double epsilon = 0.000005;

        assertTrue("Mask", new Matrix(expectedMask).compare(result.iMask, epsilon));
        assertEquals("MLEin", expectedMLEin, result.iRss.getBetaMLEin(), expectedMLEin/10000);
        assertEquals("MLEout", expectedMLEout, result.iRss.getBetaMLEout(), expectedMLEout/10000);
    }

    @Test
    public void testThreshold() {
        // Expected data taken from Matlab implementation
        final double[][] expectedMask = new double[][] {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        };
        final double[][] expectedThresholdedMask = new double[][] {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.353949199487381, 0.963959531864304, 0.970296763398045, 0.707563984435845, 0.339427117734516, 0, 0, 0},
                {0, 0, 0.853581652479588, 0.875996833679905, 0.717756931916987, 0.962762649768139, 1.000000000000000, 0.484340607708402, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        };

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  0,
                /* regularizer term */       0.0001,
                5);

        final EnergyOutput resultOfEnergyMinimalization = sa.minimizeEnergy();

        // Tested method
        final ThresholdFuzzyOutput resultOfThresholding = sa.ThresholdFuzzyVLS(resultOfEnergyMinimalization.iTotalEnergy);

        final double epsilon = 1e-13;
        assertTrue("Mask", new Matrix(expectedMask).compare(resultOfThresholding.iopt_MK, epsilon));
        assertTrue("Mask", new Matrix(expectedThresholdedMask).compare(resultOfThresholding.iH_f, epsilon));
    }

    @Test
    public void testPostprocess() {
        // Expected data taken from Matlab implementation
        final double[] expectedKnots = new double[] {3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        final double[] expectedValues = new double[] {3.5, 4.0, 3.5, 3.5, 3.5, 3.5};
        final double[] expectedWeights = new double[] {2.0, 1.0, 2.0, 2.0, 2.0, 2.0};
        final double expectedFilamentLength = 5.001569684364591;

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  0,
                /* regularizer term */       0.0002,
                150);

        final EnergyOutput resultOfEnergyMinimalization = sa.minimizeEnergy();
        final ThresholdFuzzyOutput resultOfThresholding = sa.ThresholdFuzzyVLS(resultOfEnergyMinimalization.iTotalEnergy);

        // Tested method
        final List<CubicSmoothingSpline> result = sa.generateFilamentInfo(resultOfThresholding);

        assertEquals("Number of found filaments", 1, result.size());
        assertArrayEquals(expectedKnots, result.get(0).getKnots(), 1e-10);
        assertArrayEquals(expectedWeights, result.get(0).getWeights(), 1e-10);
        assertArrayEquals(expectedValues, result.get(0).getValues(), 1e-10);
        assertEquals(expectedFilamentLength, calcualteFilamentLenght(result.get(0)), 1e-10);
    }

    @Test
    public void testGenerateFilamentInfo() {
        // Matrices below are taken from Matlab (both - input masks and expected result).
        final double[][] mask =
            {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
        final double[][] thresMask =
            {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0.997191249752063, 0.945125483826485, 0.984305990675775, 0.897802387979837, 0.876698313697055, 0.845814436027728, 0, 0, 0},
                {0, 0, 0.953055070622392, 1.000000000000000, 0.911067579852218, 0.944311435282975, 0.856591034437441, 0.858662532820259, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};

        final double[][] expectedCoefficients =
            {{-0.000373689547152, 0, -0.023263579094167, 3.610985795504279},
                {0.000321009902573, -0.001121068641457, -0.024384647735625, 3.587348526862959},
                {0.000111704110019, -0.000158038933738, -0.025663755310820, 3.562163820388450},
                {-0.000011035722485, 0.000177073396321, -0.025644720848237, 3.536453730253912},
                {-0.000047988742955, 0.000143966228865, -0.025323681223052, 3.510975047079510}};

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  0,
                /* regularizer term */       0.0002,
                150);
        final ThresholdFuzzyOutput params = sa.new ThresholdFuzzyOutput(new Matrix(mask), new Matrix(thresMask));

        // Tested method
        final List<CubicSmoothingSpline> result = sa.generateFilamentInfo(params);

        assertEquals("Number of found filaments", 1, result.size());
        assertTrue(new Matrix(expectedCoefficients).compare(new Matrix(result.get(0).getCoefficients()), 0.000000001));
    }

    @Test
    public void testGenerateFilamentInfo2() {
        // Matrices below are taken from Matlab (both - input masks and expected result).
        final double[][] mask =
            {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            };
        final double[][] thresMask =
            {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.451374356276127, 0.881030332987749, 0.966029088010224, 0.444633291317662, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0.564828630721963, 0.928075042656205, 0.889672601248121, 0.965710578835770, 0.554636912658168, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0.730021593484628, 0.998966316892269, 0.992493628015381, 0.974452939642755, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0.558430714491194, 0.988490888902313, 0.849176593042148, 0.577180548703722, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0.704680352605598, 0.981817799927692, 0.668429004225885, 0.407375187659188, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0.389860688089553, 0.838694732270388, 0.959601357487264, 0.643342233816376, 0.386763812952287, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.548426436506120, 0.959494632593251, 0.885123770679988, 0.593176963581349, 0.363655171657864, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0.421591758541020, 0.821231977180627, 0.979570543620863, 0.788190991055422, 0.722191221594935, 0.725630620802927, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0.643065275049463, 0.906065344154813, 0.963016871015114, 0.915056821273320, 0.799558944540624, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.792559812215735, 0.959902902615398, 0.826060116794134, 0.498764482315306, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.500858019175207, 0.791707143133260, 1.000000000000000, 0.651828495264204, 0.350100189835824, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.462349967374751, 0.736475615709141, 0.909554924727538, 0.990893366585723, 0.717455284702580, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.452605348152121, 0.773982204435407, 0.718177376938364, 0.825741245012937, 0.939408249041080, 0.653726110922819, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.665736085480708, 0.976461145102577, 0.441341888023675, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.430439044484251, 0.913521121299610, 0.943116004595476, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.452632186499626, 0.707442496910979, 0.994103464058208, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.424598776635331, 0.928180999184151, 0.795494327020329, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            };

        final double[][] expectedCoefficients =
            {
                {0.001199034765807, 0, 1.138098972231266, 3.287773349110843},
                {0.002163561102325, 0.003597104297420, 1.141696076528686, 4.427071356107915},
                {-0.000335295514421, 0.016578470911372, 1.182047226946268, 6.742160415173570},
                {-0.001818294899999, 0.015572584368110, 1.214198282225751, 7.940450817516790},
                {-0.004068811361988, 0.010117699668112, 1.239888566261973, 9.168403389210651},
                {0.002718558185936, -0.014295168503815, 1.231533628590569, 11.656100829511145},
                {-0.003326715449649, 0.010171855169606, 1.219163688587942, 15.295446269768783},
                {-0.000031951470110, 0.000191708820658, 1.229527252578208, 16.521455098076682},
            };

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  1,
                /* regularizer term */       0.0001,
                150);
        final ThresholdFuzzyOutput params = sa.new ThresholdFuzzyOutput(new Matrix(mask), new Matrix(thresMask));

        // Tested method
        final List<CubicSmoothingSpline> result = sa.generateFilamentInfo(params);

        assertEquals("Number of found filaments", 1, result.size());
        assertTrue(new Matrix(expectedCoefficients).compare(new Matrix(result.get(0).getCoefficients()), 1e-14));
    }

    @Test
    public void testLimitNumberOfPoints() {
        /*
         * Test limitNumberOfPoints() method in case when number of points is big.
         * Expected limited number of points (15) comparing to input points. First and last point must
         * match input points.
         */
        final int size = 30;
        final List<Integer> x = new ArrayList<Integer>();
        final List<Integer> y = new ArrayList<Integer>();
        for (int i = 0; i < size; ++i) {
            x.add(i);
            y.add(i);
        }

        final List<Double> xl = new ArrayList<Double>();
        final List<Double> yl = new ArrayList<Double>();
        final List<Double> wl = new ArrayList<Double>();

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  1,
                /* regularizer term */       0.0001,
                150);
        sa.generateOutputPointsAndWeights(xl, yl, wl, x, y);
        final List<Double> expectedXY = Arrays.asList(0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 15.0, 17.0, 19.0, 21.0, 23.0, 25.0, 27.0, 29.0);
        final List<Double> expectedW  = Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        assertEquals(expectedXY, xl);
        assertEquals(expectedXY, yl);
        assertEquals(expectedW , wl);
    }

    @Test
    public void testLimitNumberOfPointsSmall() {
        /*
         * Test limitNumberOfPoints() method in case when number of points is small.
         * Expected same output as input.
         */
        final int size = 19;
        final List<Integer> x = new ArrayList<Integer>();
        final List<Integer> y = new ArrayList<Integer>();
        final List<Double> expectedWeights = new ArrayList<Double>();
        final List<Double> expectedX = new ArrayList<Double>();
        final List<Double> expectedY = new ArrayList<Double>();
        for (int i = 0; i < size; ++i) {
            x.add(i);
            y.add(i);
            expectedWeights.add(1.0);
            expectedX.add((double) i);
            expectedY.add((double) i);
        }

        final List<Double> xl = new ArrayList<Double>();
        final List<Double> yl = new ArrayList<Double>();
        final List<Double> wl = new ArrayList<Double>();

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  1,
                /* regularizer term */       0.0001,
                150);
        sa.generateOutputPointsAndWeights(xl, yl, wl, x, y);

        // Small number of points, outputs should be equal to inputs
        assertEquals(expectedX, xl);
        assertEquals(expectedY, yl);
        assertEquals(expectedWeights , wl);
    }

    @Test
    public void testMergePointsWithSameX() {
        final List<Double> inputX = new ArrayList<Double>(Arrays.asList(1.0, 1.0, 2.0, 3.0, 3.0, 4.0, 5.0, 5.0));
        final List<Double> inputY = new ArrayList<Double>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 9.0));
        final List<Double> inputW = new ArrayList<Double>(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0));

        final List<Double> expectedX = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        final List<Double> expectedY = Arrays.asList(1.5, 3.0, 4.5, 6.0, 8.0);
        final List<Double> expectedW = Arrays.asList(2.0, 1.0, 2.0, 1.0, 3.0);

        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  1,
                /* regularizer term */       0.0001,
                150);
        sa.mergePointsWithSameX(inputX, inputY, inputW);


        assertEquals(expectedX, inputX);
        assertEquals(expectedY, inputY);
        assertEquals(expectedW , inputW);
    }

    @Test
    public void testGenerateCoordinatesOfFilament() {
        final double[][] region =
            {{0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
                {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
                {0,   0,   1,   1,   1,   1,   1,   1,   0,   0,   0},
                {0,   0,   1,   1,   1,   1,   1,   1,   0,   0,   0},
                {0,   0,   1,   1,   1,   1,   1,   0,   0,   0,   0},
                {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
                {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0}};
        final double[][] filament =
            {{0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
                {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
                {0,   0,   1,   1,   1, 0.5,   1, 0.5,   0,   0,   0},
                {0,   0,   1,   1,   1, 0.6,   1,   1,   0,   0,   0},
                {0,   0,   1,   1,   1, 0.5,   1,   0,   0,   0,   0},
                {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
                {0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0}};


        final SegmentationAlgorithm sa = new SegmentationAlgorithm(simpleImage1filament,
                NoiseType.GAUSSIAN,
                PsfType.GAUSSIAN,
                0.5,
                new Dimension(1,1),
                /* subpixel sumpling */      1,
                /* scale */                  0,
                /* regularizer term */       0.0002,
                150);

        // Tested method
        final List<Integer> x = new ArrayList<Integer>();
        final List<Integer> y = new ArrayList<Integer>();
        sa.generateCoordinatesOfFilament(new Matrix(region), new Matrix(filament), x, y);

        // Matlab's solution output matrix looks like:
        //     0     0     0     0     0     0     0     0     0     0     0
        //     0     0     0     0     0     0     0     0     0     0     0
        //     0     0     1     1     1     1     1     0     0     0     0
        //     0     0     0     0     0     1     0     1     0     0     0
        //     0     0     0     0     0     1     0     0     0     0     0
        //     0     0     0     0     0     0     0     0     0     0     0
        //     0     0     0     0     0     0     0     0     0     0     0
        final List<Integer> expectedX = Arrays.asList(3, 4, 5, 6, 6, 6, 7, 8);
        final List<Integer> expectedY = Arrays.asList(3, 3, 3, 3, 4, 5, 3, 4);

        assertEquals(expectedX, x);
        assertEquals(expectedY, y);
    }

}
