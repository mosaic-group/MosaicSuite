package mosaic.bregman;

import java.util.ArrayList;
import java.util.List;

import mosaic.bregman.segmentation.Pix;
import mosaic.bregman.segmentation.Region;

public class ColocalizationAnalysis {
    List<double[][][]> iImages = new ArrayList<double[][][]>();
    List<List<Region>> iRegions = new ArrayList<List<Region>>();
    List<short[][][]> iLabeledRegions = new ArrayList<short[][][]>();
    int iX, iY, iZ;
    int iScaleX, iScaleY, iScaleZ;
    
    ColocalizationAnalysis(int aZsize, int aXsize, int aYsize, int aScaleZ, int aScaleX, int aScaleY) {
        iX = aXsize;
        iY = aYsize;
        iZ = aZsize;
        iScaleX = aScaleX;
        iScaleY = aScaleY;
        iScaleZ = aScaleZ;
    }
    
    public void addRegion(ArrayList<Region> aRegion, double[][][] aImage) {
        iRegions.add(aRegion);
        iLabeledRegions.add(labelRegions(aRegion));
        iImages.add(aImage);
    }
    
    public short[][][] getLabeledRegion(int aIndex) {
        return iLabeledRegions.get(aIndex);
    }
    
    public class ColocResult {
        double colocsegABsignal;
        double colocsegABnumber; 
        double colocsegABsize;
        double colocsegA;
    }
    
    public ColocResult calculate(int a1stRegionIndex, int a2ndRegionIndex) {
        
        final List<Region> region1 = iRegions.get(a1stRegionIndex);
        final List<Region> region2 = iRegions.get(a2ndRegionIndex);
        final short[][][] labeledRegion2 = iLabeledRegions.get(a2ndRegionIndex);
        final double[][][] imageRegion2 = iImages.get(a2ndRegionIndex);

        int objectsCount = region1.size();
        double totalsignal = 0;
        double colocsignal = 0;
        double totalsize = 0;
        double colocsize = 0;
        double sum = 0;
        double objectscoloc = 0;
        for (Region r : region1) {
            regionColocData(r, region2, labeledRegion2, imageRegion2);
            
            colocsignal += r.realSize * r.intensity * r.overlapFactor;
            totalsignal += r.realSize * r.intensity;
            colocsize += r.realSize * r.overlapFactor;
            totalsize += r.realSize;
            sum += r.colocObjectIntensity;
            if (r.overlapFactor > Region.ColocThreshold) objectscoloc++;
        }
        
        double colocSignalBasedOfRegion2InRegion1 =  colocsignal / totalsignal;
        double colocSizeBasedOfRegion2InRegion1 =  colocsize / totalsize;
        double colocObjectsNumberOfRegion2InRegion1 =  objectscoloc / objectsCount;
        double meanRegion2ObjectsIntensityInRegion1 = sum / objectsCount;
              
        ColocResult cr = new ColocResult();
        cr.colocsegABsignal = colocSignalBasedOfRegion2InRegion1; 
        cr.colocsegABnumber =  colocObjectsNumberOfRegion2InRegion1; 
        cr.colocsegABsize = colocSizeBasedOfRegion2InRegion1;
        cr.colocsegA = meanRegion2ObjectsIntensityInRegion1;
        
        return cr;
    }    
    
    short[][][] labelRegions(ArrayList<Region> aRegions) {
        short[][][] regions = new short[iZ * iScaleZ][iX * iScaleX][iY * iScaleY];

        short label = 1;
        for (Region r : aRegions) {
            for (Pix p : r.iPixels) {
                regions[p.pz][p.px][p.py] = label;
            }
            label++;
        }
        
        return regions;
    }
    
    private void regionColocData(Region a1stRegion, List<Region> a2ndRegion, short[][][] a2ndLabeledRegions, double[][][] a2ndRegionImage) {
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
        a1stRegion.colocObjectIntensity = (sum / count);
        a1stRegion.overlapFactor = ((float) colocCount) / count;
        a1stRegion.singleRegionColoc = singleRegionColoc;
        if (colocCount != 0) {
            a1stRegion.colocObjectsAverageArea = (float) (sizeColoc / colocCount) / (iScaleX * iScaleY * iScaleZ);
            a1stRegion.colocObjectsAverageIntensity = (float) (intensityColoc) / colocCount;
        }
    }
}
