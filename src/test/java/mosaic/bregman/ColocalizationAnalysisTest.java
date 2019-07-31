package mosaic.bregman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ij.ImagePlus;
import mosaic.bregman.ColocalizationAnalysis.ChannelPair;
import mosaic.bregman.ColocalizationAnalysis.ColocResult;
import mosaic.bregman.ColocalizationAnalysis.RegionColoc;
import mosaic.bregman.GUI.ColocalizationGUI;
import mosaic.bregman.segmentation.Pix;
import mosaic.bregman.segmentation.Region;
import mosaic.test.framework.CommonBase;

public class ColocalizationAnalysisTest extends CommonBase {

    @Test
    public void testSimpleTwoRegions() {
        // Prepare regions and other stuff
        Pix[] r1pix = new Pix[] {new Pix(0, 1, 1)};
        Region r1 = new Region(1, Arrays.asList(r1pix));
        r1.intensity = 3;
        r1.realSize = 1;
        
        Pix[] r2pix = new Pix[] {new Pix(0, 1, 1), new Pix(0, 1, 2), new Pix(0, 2, 1), new Pix(0, 2, 2)};
        Region r2 = new Region(2, Arrays.asList(r2pix));
        r2.intensity = 5;
        r2.realSize = 4;
        
        ChannelPair[] cp = new ChannelPair[] {new ChannelPair(0, 1), new ChannelPair(1, 0)};
        ColocalizationAnalysis ca = new ColocalizationAnalysis(1, 1, 1);
        
        List<List<Region>> regions = new ArrayList<List<Region>>();
        List<Region> list1 = new ArrayList<Region>(); list1.add(r1);
        List<Region> list2 = new ArrayList<Region>(); list2.add(r2);
        regions.add(list1); regions.add(list2);
        
        short[][][][] labels = new short[][][][] {{{{0, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}}},
                                                  {{{0, 0, 0, 0}, {0, 1, 1, 0}, {0, 1, 1, 0}, {0, 0, 0, 0}}}};
        double[][][][] img  = new double[][][][] {{{{0, 0, 0, 0}, {0, 9, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}}},
                                                  {{{0, 0, 0, 0}, {0, 2, 2, 0}, {0, 2, 2, 0}, {0, 0, 0, 0}}}};
        
        // Tested method                                                  
        Map<ChannelPair, ColocResult> calculateAll = ca.calculateAll(Arrays.asList(cp), regions, labels, img);
        
        assertTrue("Number of analysis", calculateAll.size() == 2);
        ColocResult res01 = calculateAll.get(new ChannelPair(0, 1));
        assertTrue("Result for pair (0, 1)", res01 != null);
        ColocResult res10 = calculateAll.get(new ChannelPair(1, 0));
        assertTrue("Result for pair (1, 0)", res10 != null);
        
        double eps = 1e-6;
        assertEquals(1.0, res01.channelColoc.colocSignal, eps);
        assertEquals(1.0, res01.channelColoc.colocNumber, eps);
        assertEquals(1.0, res01.channelColoc.colocSize, eps);
        assertEquals(2.0, res01.channelColoc.coloc,eps);
        
        assertEquals(0.25, res10.channelColoc.colocSignal, eps);
        assertEquals(0.0, res10.channelColoc.colocNumber, eps);
        assertEquals(0.25, res10.channelColoc.colocSize, eps);
        assertEquals(2.25, res10.channelColoc.coloc,eps);
        
        Map<Integer, RegionColoc> regionsColoc01 = res01.regionsColoc;
        Map<Integer, RegionColoc> regionsColoc10 = res10.regionsColoc;
        assertEquals(1, regionsColoc01.size());
        RegionColoc rc01 = regionsColoc01.get(1);
        assertTrue("Region with label 1",  rc01 != null);
        assertEquals(1, regionsColoc10.size());
        RegionColoc rc10 = regionsColoc10.get(2);
        assertTrue("Region with label 2", rc10 != null);
        
        assertEquals(1.0, rc01.overlapFactor, eps);
        assertEquals(4.0, rc01.colocObjectsAverageArea, eps);
        assertEquals(5.0, rc01.colocObjectsAverageIntensity, eps);
        assertEquals(2.0, rc01.colocObjectIntensity, eps);
        assertEquals(true, rc01.singleRegionColoc);
        
        assertEquals(0.25, rc10.overlapFactor, eps);
        assertEquals(1.0, rc10.colocObjectsAverageArea, eps);
        assertEquals(3.0, rc10.colocObjectsAverageIntensity, eps);
        assertEquals(2.25, rc10.colocObjectIntensity, eps);
        assertEquals(true, rc10.singleRegionColoc);
    }

    
    @Test
    public void testGUI() {
        ImagePlus iInputImage = loadImagePlus("/Users/gonciarz/1/3C.tif");
        Parameters iParameters = new Parameters();
        iParameters.usecellmaskY=true;
        iParameters.thresholdcellmasky = 0.5;
        final ColocalizationGUI gds = new ColocalizationGUI(iInputImage, iParameters);
        gds.run();
        System.out.println(iParameters);
    }
}
