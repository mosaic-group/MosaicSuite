package mosaic.bregman;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mosaic.bregman.segmentation.Pix;
import mosaic.bregman.segmentation.Region;

class ColocalizationAnalysis {
    private int iScaleX, iScaleY, iScaleZ;
    
    ColocalizationAnalysis(int aScaleZ, int aScaleX, int aScaleY) {
        iScaleX = aScaleX;
        iScaleY = aScaleY;
        iScaleZ = aScaleZ;
    }
    
    Map<ChannelPair, ColocResult> calculateAll(List<ChannelPair> aChannelPairs, List<List<Region>> aRegions, short[][][][] aLabeledRegions, double[][][][] aNormalizedImages) {
        Map<ChannelPair, ColocResult> results = new HashMap<ChannelPair, ColocResult>();
        for (ChannelPair cp : aChannelPairs) {
            ColocResult channelColoc = calculateChannelColoc(aRegions.get(cp.ch1), aRegions.get(cp.ch2), aLabeledRegions[cp.ch2], aNormalizedImages[cp.ch2]);
            results.put(cp, channelColoc);
        }
        
        return results;
    }

    private RegionColoc regionColocData(Region a1stRegion, List<Region> a2ndRegion, short[][][] a2ndLabeledRegions, double[][][] a2ndRegionImage) {
        int colocCount = 0;
        int previousColocLabel = 0;
        boolean singleRegionColoc = true;
        double intensityColoc = 0;
        double sizeColoc = 0;
        double sum = 0;
        
        for (Pix p : a1stRegion.iPixels) {
            sum += a2ndRegionImage[p.pz / iScaleZ][p.px / iScaleX][p.py / iScaleY];
            int label = a2ndLabeledRegions[p.pz][p.px][p.py];
            if (label > 0) {
                colocCount++;
                if (previousColocLabel != 0 && label != previousColocLabel) {
                    singleRegionColoc = false;
                }
                Region region2 = a2ndRegion.get(label - 1);
                intensityColoc += region2.intensity;
                sizeColoc += region2.realSize;
                previousColocLabel = label;
            }
        }
        int count = a1stRegion.iPixels.size();
        RegionColoc regionColoc = new RegionColoc();
        regionColoc.colocObjectIntensity = (sum / count);
        regionColoc.overlapFactor = ((float) colocCount) / count;
        regionColoc.singleRegionColoc = singleRegionColoc;
        if (colocCount != 0) {
            regionColoc.colocObjectsAverageArea = (float) (sizeColoc / colocCount);
            regionColoc.colocObjectsAverageIntensity = (float) (intensityColoc) / colocCount;
        }
        return regionColoc;
    }
    
    private ColocResult calculateChannelColoc(final List<Region> aRegions1, final List<Region> aRegions2, final short[][][] aLabeledRegions2, final double[][][] aImageRegions2) {
        final float ColocThreshold = 0.5f;
        
        int objectsCount = aRegions1.size();
        double totalsignal = 0;
        double colocsignal = 0;
        double totalsize = 0;
        double colocsize = 0;
        double sum = 0;
        double objectscoloc = 0;
        Map<Integer, RegionColoc> regionColocs = new TreeMap<Integer, RegionColoc>();
        for (Region r : aRegions1) {
            RegionColoc regionColoc = regionColocData(r, aRegions2, aLabeledRegions2, aImageRegions2);
            regionColocs.put(r.iLabel, regionColoc);
            
            colocsignal += r.realSize * r.intensity * regionColoc.overlapFactor;
            totalsignal += r.realSize * r.intensity;
            colocsize += r.realSize * regionColoc.overlapFactor;
            totalsize += r.realSize;
            sum += regionColoc.colocObjectIntensity;
            if (regionColoc.overlapFactor > ColocThreshold) objectscoloc++;
        }
        
        ChannelColoc channelColoc = new ChannelColoc();
        channelColoc.colocSignal = colocsignal / totalsignal; 
        channelColoc.colocNumber =  objectscoloc / objectsCount; 
        channelColoc.colocSize = colocsize / totalsize;
        channelColoc.coloc = sum / objectsCount;
        
        ColocResult colRes = new ColocResult();
        colRes.channelColoc = channelColoc;
        colRes.regionsColoc = regionColocs;
        return colRes;
    }
    
    class ChannelColoc {
        // what is average intensity/signal of overlapped part of regions? (first to second channel) (normalized to 0-1) 
        double colocSignal;
        // what is average number of objects that colocalize over threshold with second channel? (normalized to 0-1)
        double colocNumber;
        // what is average size of overlapped parts of regions? (normalized to 0-1)
        double colocSize;
        // what is average image in second channel? (signal from overlapping area)
        double coloc;
        
        @Override
        public String toString() {
            return "[" + colocSignal + ", " + colocNumber + ", " + colocSize + ", " + coloc + "]";
        }
    }
    
    class RegionColoc {
        // how big area of region overlaps region(s) in second channel?
        public float overlapFactor = 0.0f;
        // what is average area/size of region(s) from second channel that overlaps with this region?
        public float colocObjectsAverageArea = 0.0f;
        // what is average intensity of region(s) from second channel that overlaps with this region?
        public float colocObjectsAverageIntensity = 0.0f;
        // what is average intensity of image area in second channel covered by this region?
        public double colocObjectIntensity = 0.0;
        // does this object colocalize only with one region from second channel?
        public boolean singleRegionColoc = false;
        
        @Override
        public String toString() {
            return "[" + overlapFactor + ", " + colocObjectsAverageArea + ", " + colocObjectsAverageIntensity + ", " + colocObjectIntensity + ", " + singleRegionColoc + "]";
        }
    }
    
    class ColocResult {
        // Map region to coloc results: <Region ID, RegionColoc>
        Map<Integer, RegionColoc> regionsColoc;
        ChannelColoc channelColoc;
        
        @Override
        public String toString() {
            return "" + regionsColoc + "\n" + channelColoc + "\n";
        }
    }
    
    public static class ChannelPair {
        ChannelPair(int aCh1, int aCh2) { ch1 = aCh1; ch2 = aCh2; }
        int ch1;
        int ch2;
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ch1;
            result = prime * result + ch2;
            return result;
        }
    
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            ChannelPair other = (ChannelPair) obj;
            if (ch1 != other.ch1) return false;
            if (ch2 != other.ch2) return false;
            return true;
        }
    
        @Override
        public String toString() {
            return "{" + ch1 + ", " + ch2 + "}";
        }
    }
}
