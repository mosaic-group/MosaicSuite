package mosaic.bregman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mosaic.bregman.segmentation.Pix;
import mosaic.bregman.segmentation.Region;

class ColocalizationAnalysis {
    private List<double[][][]> iImages = new ArrayList<double[][][]>();
    private List<List<Region>> iRegions = new ArrayList<List<Region>>();
    private List<short[][][]> iLabeledRegions = new ArrayList<short[][][]>();
    private int iScaleX, iScaleY, iScaleZ;
    
    ColocalizationAnalysis(int aScaleZ, int aScaleX, int aScaleY) {
        iScaleX = aScaleX;
        iScaleY = aScaleY;
        iScaleZ = aScaleZ;
    }
    
    /**
     * Add new region.
     */
    void addRegion(ArrayList<Region> aRegion, short[][][] aLabeledRegion, double[][][] aImage) {
        iRegions.add(aRegion);
        iLabeledRegions.add(aLabeledRegion);
        iImages.add(aImage);
    }
    
//    ChannelColoc calculate(int a1stRegionIndex, int a2ndRegionIndex) {
//        
//        final List<Region> region1 = iRegions.get(a1stRegionIndex);
//        final List<Region> region2 = iRegions.get(a2ndRegionIndex);
//        final short[][][] labeledRegion2 = iLabeledRegions.get(a2ndRegionIndex);
//        final double[][][] imageRegion2 = iImages.get(a2ndRegionIndex);
//
//        return calculateChannelColoc(region1, region2, labeledRegion2, imageRegion2);
//    }
//
//    private ChannelColoc calculateChannelColoc(final List<Region> region1, final List<Region> region2, final short[][][] labeledRegion2, final double[][][] imageRegion2) {
//        int objectsCount = region1.size();
//        double totalsignal = 0;
//        double colocsignal = 0;
//        double totalsize = 0;
//        double colocsize = 0;
//        double sum = 0;
//        double objectscoloc = 0;
//        for (Region r : region1) {
//            regionColocData(r, region2, labeledRegion2, imageRegion2);
//            
//            colocsignal += r.realSize * r.intensity * r.overlapFactor;
//            totalsignal += r.realSize * r.intensity;
//            colocsize += r.realSize * r.overlapFactor;
//            totalsize += r.realSize;
//            sum += r.colocObjectIntensity;
//            if (r.overlapFactor > Region.ColocThreshold) objectscoloc++;
//        }
//        
//        double colocSignalBasedOfRegion2InRegion1 =  colocsignal / totalsignal;
//        double colocSizeBasedOfRegion2InRegion1 =  colocsize / totalsize;
//        double colocObjectsNumberOfRegion2InRegion1 =  objectscoloc / objectsCount;
//        double meanRegion2ObjectsIntensityInRegion1 = sum / objectsCount;
//              
//        ChannelColoc cr = new ChannelColoc();
//        cr.colocsegABsignal = colocSignalBasedOfRegion2InRegion1; 
//        cr.colocsegABnumber =  colocObjectsNumberOfRegion2InRegion1; 
//        cr.colocsegABsize = colocSizeBasedOfRegion2InRegion1;
//        cr.colocsegA = meanRegion2ObjectsIntensityInRegion1;
//        
//        return cr;
//    }    
//    
//    private void regionColocData(Region a1stRegion, List<Region> a2ndRegion, short[][][] a2ndLabeledRegions, double[][][] a2ndRegionImage) {
//        int colocCount = 0;
//        int previousColocLabel = 0;
//        boolean singleRegionColoc = true;
//        double intensityColoc = 0;
//        double sizeColoc = 0;
//        
//        double sum = 0;
//        for (Pix p : a1stRegion.iPixels) {
//            sum += a2ndRegionImage[p.pz / iScaleZ][p.px / iScaleX][p.py / iScaleY];
//            int label = a2ndLabeledRegions[p.pz][p.px][p.py];
//            if (label > 0) {
//                colocCount++;
//                if (previousColocLabel != 0 && label != previousColocLabel) {
//                    singleRegionColoc = false;
//                }
//                Region region2 = a2ndRegion.get(label - 1);
//                intensityColoc += region2.intensity;
//                sizeColoc += region2.iPixels.size();
//                previousColocLabel = label;
//            }
//        }
//        int count = a1stRegion.iPixels.size();
//        a1stRegion.colocObjectIntensity = (sum / count);
//        a1stRegion.overlapFactor = ((float) colocCount) / count;
//        a1stRegion.singleRegionColoc = singleRegionColoc;
//        if (colocCount != 0) {
//            a1stRegion.colocObjectsAverageArea = (float) (sizeColoc / colocCount) / (iScaleX * iScaleY * iScaleZ);
//            mosaic.utils.Debug.print("NORMAL: " ,sizeColoc / colocCount,iScaleX , iScaleY , iScaleZ );
//            a1stRegion.colocObjectsAverageIntensity = (float) (intensityColoc) / colocCount;
//        }
//    }
    
    RegionColoc regionColocData2(Region a1stRegion, List<Region> a2ndRegion, short[][][] a2ndLabeledRegions, double[][][] a2ndRegionImage) {
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
                sizeColoc += region2.iPixels.size();
                previousColocLabel = label;
            }
        }
        int count = a1stRegion.iPixels.size();
        RegionColoc regionColoc = new RegionColoc();
        regionColoc.colocObjectIntensity = (sum / count);
        regionColoc.overlapFactor = ((float) colocCount) / count;
        regionColoc.singleRegionColoc = singleRegionColoc;
        if (colocCount != 0) {
            regionColoc.colocObjectsAverageArea = (float) (sizeColoc / colocCount) / (iScaleX * iScaleY * iScaleZ);
            mosaic.utils.Debug.print("NEW  : " ,sizeColoc / colocCount,iScaleX , iScaleY , iScaleZ );
            regionColoc.colocObjectsAverageIntensity = (float) (intensityColoc) / colocCount;
        }
        return regionColoc;
    }
    
    private ColocResult calculateChannelColoc2(final List<Region> region1, final List<Region> region2, final short[][][] labeledRegion2, final double[][][] imageRegion2) {
        final float ColocThreshold = 0.5f;
        
        int objectsCount = region1.size();
        double totalsignal = 0;
        double colocsignal = 0;
        double totalsize = 0;
        double colocsize = 0;
        double sum = 0;
        double objectscoloc = 0;
        Map<Integer, RegionColoc> colocs = new TreeMap<Integer, RegionColoc>();
        for (Region r : region1) {
            RegionColoc cd = regionColocData2(r, region2, labeledRegion2, imageRegion2);
            colocs.put(r.iLabel, cd);
            
            colocsignal += r.realSize * r.intensity * cd.overlapFactor;
            totalsignal += r.realSize * r.intensity;
            colocsize += r.realSize * cd.overlapFactor;
            totalsize += r.realSize;
            sum += cd.colocObjectIntensity;
            if (cd.overlapFactor > ColocThreshold) objectscoloc++;
        }
        
        double colocSignalBasedOfRegion2InRegion1 =  colocsignal / totalsignal;
        double colocSizeBasedOfRegion2InRegion1 =  colocsize / totalsize;
        double colocObjectsNumberOfRegion2InRegion1 =  objectscoloc / objectsCount;
        double meanRegion2ObjectsIntensityInRegion1 = sum / objectsCount;
              
        ChannelColoc cr = new ChannelColoc();
        cr.colocsegABsignal = colocSignalBasedOfRegion2InRegion1; 
        cr.colocsegABnumber =  colocObjectsNumberOfRegion2InRegion1; 
        cr.colocsegABsize = colocSizeBasedOfRegion2InRegion1;
        cr.colocsegA = meanRegion2ObjectsIntensityInRegion1;
        
        ColocResult colRes = new ColocResult();
        colRes.channelColoc = cr;
        colRes.regionsColoc = colocs;
        return colRes;
    }
    
    class ChannelColoc {
        double colocsegABsignal;
        double colocsegABnumber; 
        double colocsegABsize;
        double colocsegA;
        
        @Override
        public String toString() {
            return "[" + colocsegABsignal + ", " + colocsegABnumber + ", " + colocsegABsize + ", " + colocsegA + "]";
        }
    }
    
    class RegionColoc {
        public float overlapFactor = 0.0f;
        public float colocObjectsAverageArea = 0.0f;
        public float colocObjectsAverageIntensity = 0.0f;
        public double colocObjectIntensity = 0.0;
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
            return "" + regionsColoc + "\n" + channelColoc;
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

    static Map<ColocalizationAnalysis.ChannelPair, ColocResult> map = new HashMap<ColocalizationAnalysis.ChannelPair, ColocResult>();
    
//    private ChannelColoc[] runColocalizationAnalysis(int aChannel1, int aChannel2) {
//        // Run colocalization
//        final int factor2 = iOutputImgScale;
//        ColocalizationAnalysis ca = new ColocalizationAnalysis(nz, ni, nj, (nz > 1) ? factor2 : 1, factor2, factor2);
//        ca.addRegion(maskedRegionList.get(aChannel1), iLabeledRegions[aChannel1], iNormalizedImages[aChannel1]);
//        ca.addRegion(maskedRegionList.get(aChannel2), iLabeledRegions[aChannel2], iNormalizedImages[aChannel2]);
//        ChannelColoc resAB = ca.calculate(0, 1);
//        ChannelColoc resBA = ca.calculate(1, 0);
//
//        return new ChannelColoc[] {resAB, resBA};
//    }
//    
    public Map<ColocalizationAnalysis.ChannelPair, ColocResult> calculateAll(List<ColocalizationAnalysis.ChannelPair> aChannelPairs, ArrayList<ArrayList<Region>> aRegions, short[][][][] aLabeledRegions, double[][][][] aNormalizedImages, int iOutputImgScale) {
        final int nz = aNormalizedImages[0].length;
        final int factor2 = iOutputImgScale;
        
        ColocalizationAnalysis ca = new ColocalizationAnalysis((nz > 1) ? factor2 : 1, factor2, factor2);
        for (ColocalizationAnalysis.ChannelPair cp : aChannelPairs) {
            ColocResult calculateChannelColocAB = ca.calculateChannelColoc2(aRegions.get(cp.ch1), aRegions.get(cp.ch2), aLabeledRegions[cp.ch2], aNormalizedImages[cp.ch2]);
            map.put(cp, calculateChannelColocAB);
            ColocResult calculateChannelColocBA = ca.calculateChannelColoc2(aRegions.get(cp.ch2), aRegions.get(cp.ch1), aLabeledRegions[cp.ch1], aNormalizedImages[cp.ch1]);
            
            map.put(new ColocalizationAnalysis.ChannelPair(cp.ch2, cp.ch1), calculateChannelColocBA);
        }
        
        return map;
    }
}
