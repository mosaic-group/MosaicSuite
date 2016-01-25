package mosaic.bregman;


import java.util.ArrayList;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mosaic.core.utils.MosaicUtils;
import mosaic.utils.Debug;


public class Analysis {
    // This is the output for cluster
    public final static String out[] = {"*_ObjectsData_c1.csv", "*_ObjectsData_c2.csv", 
                                        "*_mask_c1.zip", "*_mask_c2.zip", 
                                        "*_ImagesData.csv", 
                                        "*_outline_overlay_c1.zip", "*_outline_overlay_c2.zip",
                                        "*_intensities_c1.zip", "*_intensities_c2.zip", 
                                        "*_seg_c1.zip", "*_seg_c2.zip", 
                                        "*_coloc.zip", 
                                        "*_soft_mask_c1.tiff", "*_soft_mask_c2.tiff", 
                                        "*.tif" };

    // This is the output local
    public final static String out_w[] = {"*_ObjectsData_c1.csv", "*_ObjectsData_c2.csv", 
                                          "*_mask_c1.zip", "*_mask_c2.zip", 
                                          "*_ImagesData.csv", 
                                          "*_outline_overlay_c1.zip", "*_outline_overlay_c2.zip",
                                          "*_intensities_c1.zip", "*_intensities_c2.zip", 
                                          "*_seg_c1.zip", "*_seg_c2.zip", 
                                          "*_soft_mask_c1.tiff", "*_soft_mask_c2.tiff", 
                                          "*_coloc.zip" };

    static String currentImage = "currentImage";
    static ImagePlus imgA;
    private static ImagePlus imgB;
    private static byte[][][] maskA;
    private static byte[][][] maskB;
    static double[][][] imagea;
    static double[][][] imageb;
    static boolean[][][] cellMaskABinary;
    static boolean[][][] cellMaskBBinary;
    private static boolean[][][] overallCellMaskBinary;

    public static Parameters iParams = new Parameters();
    static int frame;

    // Maximum norm, it fix the range of the normalization, useful for video normalization has to be done on all frame video, 
    // filled when the plugins is called with the options min=... max=...
    public static double norm_max = 0.0;
    public static double norm_min = 0.0;

    // Create empty elements - they are later access by index - not nice but later elements are accessed by get() 
    // and they must exist. TODO: This should be refactored.
    private static ArrayList<ArrayList<Region>> regionslist = new ArrayList<ArrayList<Region>>() {
        private static final long serialVersionUID = 1L;
        { add(null); add(null); }
    };
    private static short[][][][] regions = new short[2][][][];
    
    final static ImagePlus out_soft_mask[] = new ImagePlus[2];
    
    public static ArrayList<Region> getRegionslist(int aRegionNum) {
        return regionslist.get(aRegionNum);
    }
    
    public static void setRegionslist(ArrayList<Region> regionslist, int aRegionNum) {
        Analysis.regionslist.set(aRegionNum, regionslist);
    }
    
    public static short[][][] getRegions(int aRegionNum) {
        return regions[aRegionNum];
    }
    
    public static void setRegions(short[][][] regions, int aRegionNum) {
        Debug.print("setRegions", Debug.getStack(3, 2));
        Analysis.regions[aRegionNum] = regions;
    }

    static void loadChannels(ImagePlus img2, int aNumOfChannels) {
        iParams.ni = img2.getWidth();
        iParams.nj = img2.getHeight();
        iParams.nz = img2.getNSlices();
        final int currentFrame = img2.getFrame();
        final int bits = img2.getBitDepth();

        setupChannel1(img2, currentFrame, bits);
        if (aNumOfChannels > 1) setupChannel2(img2, currentFrame, bits);
    }

    private static void setupChannel1(ImagePlus img2, final int currentFrame, final int bits) {
        final ImageStack img_s = generateImgStack(img2, currentFrame, bits, 1);
        imgA = new ImagePlus();
        imgA.setStack(img2.getTitle(), img_s);
        imagea = setImage(imgA);
    }

    private static void setupChannel2(ImagePlus img2, final int currentFrame, final int bits) {
        final ImageStack img_s = generateImgStack(img2, currentFrame, bits, 2);
        imgB = new ImagePlus();
        imgB.setStack(img2.getTitle(), img_s);
        imageb = setImage(imgB);
    }

    private static double[][][] setImage(ImagePlus aImage) {
        double[][][] image = new double[iParams.nz][iParams.ni][iParams.nj];

        double max = 0;
        double min = Double.POSITIVE_INFINITY;

        for (int z = 0; z < iParams.nz; z++) {
            aImage.setSlice(z + 1);
            ImageProcessor imp = aImage.getProcessor();
            for (int i = 0; i < iParams.ni; i++) {
                for (int j = 0; j < iParams.nj; j++) {
                    image[z][i][j] = imp.getPixel(i, j);
                    if (image[z][i][j] > max) {
                        max = image[z][i][j];
                    }
                    if (image[z][i][j] < min) {
                        min = image[z][i][j];
                    }
                }
            }
        }

        for (int z = 0; z < iParams.nz; z++) {
            for (int i = 0; i < iParams.ni; i++) {
                for (int j = 0; j < iParams.nj; j++) {
                    image[z][i][j] = (image[z][i][j] - min) / (max - min);
                }
            }
        }
        
        return image;
    }
    
    private static ImageStack generateImgStack(ImagePlus img2, final int currentFrame, final int bits, int channel) {
        final ImageStack img_s = new ImageStack(iParams.ni, iParams.nj);

        for (int z = 0; z < iParams.nz; z++) {
            img2.setPosition(channel, z + 1, currentFrame);
            ImageProcessor impt = (bits == 32) ? img2.getProcessor().convertToShort(false) : img2.getProcessor();
            img_s.addSlice("", impt);
        }
        
        return img_s;
    }

    static double[] pearson_corr() {
        return new SamplePearsonCorrelationCoefficient(imgA, imgB, iParams.usecellmaskX, iParams.thresholdcellmask, iParams.usecellmaskY, iParams.thresholdcellmasky).run();
    }

    static void segmentA() {
        segmentImg(imgA, 0);
    }
    
    static void segmentB() {
        segmentImg(imgB, 1);
    }
    
    private static void segmentImg(final ImagePlus img, final int channel) {
        // PSF only working in two region problem
        // 3D only working for two problem
        
        currentImage = img.getTitle();

        TwoRegions rg = new TwoRegions(img, iParams, channel);
        rg.run();

        if (iParams.dispSoftMask) {
            // TODO: Added temporarily to since soft mask for channel 2 is not existing ;
            if (channel > 1) return;
            
            if (out_soft_mask[channel] == null) {
                out_soft_mask[channel] = new ImagePlus();
            }

            MosaicUtils.MergeFrames(out_soft_mask[channel], rg.out_soft_mask[channel]);
            out_soft_mask[channel].setStack(out_soft_mask[channel].getStack());
        }
    }

    static void compute_connected_regions_a() {
        final FindConnectedRegions fcr = processConnectedRegions(iParams.min_intensity, maskA);
        regions[0] = fcr.getLabeledRegions();
        regionslist.set(0, fcr.getFoundRegions());
    }

    static void compute_connected_regions_b() {
        final FindConnectedRegions fcr = processConnectedRegions(iParams.min_intensityY, maskB);
        regions[1] = fcr.getLabeledRegions();
        regionslist.set(1, fcr.getFoundRegions());
    }
    
    private static FindConnectedRegions processConnectedRegions(double intensity, byte[][][] mask) {
        final ImagePlus mask_im = new ImagePlus();
        final ImageStack mask_ims = new ImageStack(iParams.ni, iParams.nj);

        for (int z = 0; z < iParams.nz; z++) {
            final byte[] mask_bytes = new byte[iParams.ni * iParams.nj];
            for (int i = 0; i < iParams.ni; i++) {
                for (int j = 0; j < iParams.nj; j++) {
                    mask_bytes[j * iParams.ni + i] = mask[z][i][j];
                }
            }
            final ByteProcessor bp = new ByteProcessor(iParams.ni, iParams.nj);
            bp.setPixels(mask_bytes);
            mask_ims.addSlice("", bp);
        }

        mask_im.setStack("", mask_ims);
        final FindConnectedRegions fcr = new FindConnectedRegions(mask_im);
        fcr.run(-1 /* no maximum size */, iParams.minves_size, (float) (255 * intensity));
        
        return fcr;
    }

    static double colocsegA() {
        return coloc(regionslist.get(0), imageb);
    }

    static double colocsegB() {
        return coloc(regionslist.get(1), imagea);
    }
    
    private static double coloc(final ArrayList<Region> currentRegion, double[][][] image) {
        int objects = currentRegion.size();
        double sum = 0;
        for (Region r : currentRegion) {
            sum += regionsum(r, image);
        }
        return sum / objects;
    }

    static double colocsegABnumber() {
        return colocNumber(regionslist.get(0));
    }

    static double colocsegBAnumber() {
        return colocNumber(regionslist.get(1));
    }

    private static double colocNumber(final ArrayList<Region> currentRegion) {
        int objects = currentRegion.size();
        int objectscoloc = 0;
        for (Region r : currentRegion) {
            if (r.colocpositive) {
                objectscoloc++;
            }
        }

        return ((double) objectscoloc) / objects;
    }
    
    static double colocsegAB() {
        return colocRegions(regionslist.get(0), regionslist.get(1), regions[1]);
    }

    static double colocsegBA() {
        return colocRegions(regionslist.get(1), regionslist.get(0), regions[0]);
    }

    private static double colocRegions(final ArrayList<Region> currentRegionList, final ArrayList<Region> secondRegionList, final short[][][] region) {
        double totalsignal = 0;
        double colocsignal = 0;
        for (Region r : currentRegionList) {
            regioncoloc(r, secondRegionList, region);
            totalsignal += r.rsize * r.intensity;
            colocsignal += r.rsize * r.intensity * r.overlap;
        }

        return (colocsignal / totalsignal);
    }

    static double colocsegABsize() {
        return colocRegionsSize(regionslist.get(0), regionslist.get(1), regions[1]);
    }

    static double colocsegBAsize() {
        return colocRegionsSize(regionslist.get(1), regionslist.get(0), regions[0]);
    }

    private static double colocRegionsSize(final ArrayList<Region> currentRegionList, final ArrayList<Region> secondRegionList, final short[][][] region) {
        double totalsize = 0;
        double colocsize = 0;
        for (Region r : currentRegionList) {
            regioncoloc(r, secondRegionList, region);
            totalsize += r.rsize;
            colocsize += r.rsize * r.overlap;
        }

        return (colocsize / totalsize);
    }

    private static boolean regioncoloc(Region r, ArrayList<Region> regionlist, short[][][] regions) {
        int count = 0;
        int countcoloc = 0;
        int previousvalcoloc = 0;
        boolean oneColoc = true;
        double intColoc = 0;
        double sizeColoc = 0;
        final int osxy = Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation;
        for (Pix p : r.pixels) {
            int valcoloc = regions[p.pz][p.px][p.py];
            if (valcoloc > 0) {
                countcoloc++;
                if (previousvalcoloc != 0 && valcoloc != previousvalcoloc) {
                    oneColoc = false;
                }
                intColoc += regionlist.get(valcoloc - 1).intensity;
                sizeColoc += regionlist.get(valcoloc - 1).points;
                previousvalcoloc = valcoloc;
            }
            count++;
        }

        r.colocpositive = ((double) countcoloc) / count > iParams.colocthreshold;
        r.overlap = (float) Tools.round(((double) countcoloc) / count, 3);
        r.over_size = (float) Tools.round((sizeColoc) / countcoloc, 3);
        if (iParams.nz == 1) {
            r.over_size = (float) Tools.round(r.over_size / (osxy * osxy), 3);
        }
        else {
            r.over_size = (float) Tools.round(r.over_size / (osxy * osxy * osxy), 3);
        }

        r.over_int = (float) Tools.round((intColoc) / countcoloc, 3);
        r.singlec = oneColoc;

        return r.colocpositive;
    }

    static void SetRegionsObjsVoronoi(ArrayList<Region> regionlist, ArrayList<Region> regionsvoronoi, float[][][] ri) {
        for (Region r : regionlist) {
            int x = r.pixels.get(0).px;
            int y = r.pixels.get(0).py;
            int z = r.pixels.get(0).pz;
            r.rvoronoi = regionsvoronoi.get((int) ri[z][x][y]);
        }
    }
    
    private static double regionsum(Region r, double[][][] image) {
        final int factor2 = Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation;
        int fz2 = (Analysis.iParams.nz > 1) ? factor2 : 1;

        int count = 0;
        double sum = 0;
        for (Pix p : r.pixels) {
            sum += image[p.pz / fz2][p.px / factor2][p.py / factor2];
            count++;
        }

        r.coloc_o_int = (sum / count);
        return (sum / count);
    }

    static void regionCenter(Region r) {
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (Pix p : r.pixels) {
            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
        }
        int count = r.pixels.size();

        r.cx = (float) (sumx / count);
        r.cy = (float) (sumy / count);
        r.cz = (float) (sumz / count);
        if (Analysis.iParams.subpixel) {
            r.cx = r.cx / (Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation);
            r.cy = r.cy / (Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation);
            r.cz = r.cz / (Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation);
        }
    }

    static void setMaskA(double[][][] mask) {
        maskA = new byte[iParams.nz][iParams.ni][iParams.nj];
        copyScaledMask(maskA, mask);
    }

    static void setMaskB(double[][][] mask) {
        maskB = new byte[iParams.nz][iParams.ni][iParams.nj];
        copyScaledMask(maskB, mask);
    }
    
    private static void copyScaledMask(byte[][][] aDestination, double[][][] aSource) {
        for (int z = 0; z < iParams.nz; z++) {
            for (int i = 0; i < iParams.ni; i++) {
                for (int j = 0; j < iParams.nj; j++) {
                    aDestination[z][i][j] = (byte) ((int) (255 * aSource[z][i][j]));
                }
            }
        }
    }

    static double meansurface(ArrayList<Region> aRegionsList) {
        final int objects = aRegionsList.size();
        double totalsize = 0;
        for (Region r : aRegionsList) {
            totalsize += r.perimeter;
        }

        return (totalsize / objects);
    }

    static double meansize(ArrayList<Region> aRegionsList) {
        final int objects = aRegionsList.size();
        double totalsize = totalsize(aRegionsList);

        if (Analysis.iParams.subpixel) {
            return (totalsize / objects) / (Math.pow(Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation, 2));
        }
        else {
            return (totalsize / objects);
        }
    }
    
    static double meanlength(ArrayList<Region> aRegionsList) {
        final int objects = aRegionsList.size();
        double totalsize = 0;
        for (Region r : aRegionsList) {
            totalsize += r.length;
        }
        
        return (totalsize / objects);
    }

    private static double totalsize(ArrayList<Region> aRegionsList) {
        double totalsize = 0;
        for (Region r : aRegionsList) {
            totalsize += r.points;
        }
        
        return totalsize;
    }

    static ArrayList<Region> removeExternalObjects(ArrayList<Region> aRegionsList) {
        final ArrayList<Region> newregionlist = new ArrayList<Region>();
        for (Region r : aRegionsList) {
            if (isInside(r)) {
                newregionlist.add(r);
            }
        }

        return newregionlist;
    }

    private static boolean isInside(Region r) {
        final int factor2 = Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation;
        int fz2 = (Analysis.iParams.nz > 1) ? factor2 : 1;

        double size = 0;
        int inside = 0;
        for (Pix px : r.pixels) {
            if (overallCellMaskBinary[px.pz / fz2][px.px / factor2][px.py / factor2]) {
                inside++;
            }
            size++;
        }
        return ((inside / size) > 0.1);
    }

    static void computeOverallMask() {
        final boolean mask[][][] = new boolean[iParams.nz][iParams.ni][iParams.nj];

        for (int z = 0; z < iParams.nz; z++) {
            for (int i = 0; i < iParams.ni; i++) {
                for (int j = 0; j < iParams.nj; j++) {
                    if (iParams.usecellmaskX && iParams.usecellmaskY) {
                        mask[z][i][j] = cellMaskABinary[z][i][j] && cellMaskBBinary[z][i][j];
                    }
                    else if (iParams.usecellmaskX) {
                        mask[z][i][j] = cellMaskABinary[z][i][j];
                    }
                    else if (iParams.usecellmaskY) {
                        mask[z][i][j] = cellMaskBBinary[z][i][j];
                    }
                    else {
                        mask[z][i][j] = true;
                    }

                }
            }
        }
        overallCellMaskBinary = mask;
    }

    static void setRegionsLabels(ArrayList<Region> regionslist, short[][][] regions) {
        final int factor2 = Analysis.iParams.oversampling2ndstep * Analysis.iParams.interpolation;
        int fz2 = (Analysis.iParams.nz > 1) ? factor2 : 1;
        for (int z = 0; z < iParams.nz * fz2; z++) {
            for (int i = 0; i < iParams.ni * factor2; i++) {
                for (int j = 0; j < iParams.nj * factor2; j++) {
                    regions[z][i][j] = 0;
                }
            }
        }

        short index = 1;
        for (Region r : regionslist) {
            setRegionLabel(r, regions, index++);
        }
    }

    private static void setRegionLabel(Region r, short[][][] regions, short label) {
        for (Pix px : r.pixels) {
            regions[px.pz][px.px][px.py] = label;
        }
    }
}
