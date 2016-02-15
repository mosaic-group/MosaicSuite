package mosaic.bregman;


import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.bregman.segmentation.Pix;
import mosaic.bregman.segmentation.Region;
import mosaic.bregman.segmentation.SegmentationParameters;
import mosaic.bregman.segmentation.SegmentationParameters.IntensityMode;
import mosaic.bregman.segmentation.SegmentationParameters.NoiseModel;
import mosaic.bregman.segmentation.SquasshSegmentation;
import mosaic.core.detection.Particle;
import mosaic.core.imageUtils.MaskOnSpaceMapper;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.masks.BallMask;
import mosaic.core.utils.MosaicUtils;
import mosaic.utils.ArrayOps;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.ImgUtils;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;


public class Analysis {
    private static final Logger logger = Logger.getLogger(Analysis.class);
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
    static final int NumOfInputImages = 2;
    static ImagePlus[] inputImages = new ImagePlus[NumOfInputImages];
    static double[][][][] images = new double[NumOfInputImages][][][];
    static boolean[][][] cellMaskABinary;
    static boolean[][][] cellMaskBBinary;
    private static boolean[][][] overallCellMaskBinary;
    public static int iOutputImgScale = 1;
    public static Parameters iParameters = new Parameters();
    static public int frame;

    // Maximum norm, it fix the range of the normalization, useful for video normalization has to be done on all frame video, 
    // filled when the plugins is called with the options min=... max=...
    public static double norm_max = 0.0;
    public static double norm_min = 0.0;

    // Create empty elements - they are later access by index - not nice but later elements are accessed by get() 
    // and they must exist.
    private static ArrayList<ArrayList<Region>> regionslist = new ArrayList<ArrayList<Region>>() {
        private static final long serialVersionUID = 1L;
        { for (int i = 0; i < NumOfInputImages; i++) add(null); }
    };
    private static short[][][][] regions = new short[NumOfInputImages][][][];
    final static ImagePlus out_soft_mask[] = new ImagePlus[NumOfInputImages];
    
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
        Analysis.regions[aRegionNum] = regions;
    }

    static void loadChannels(ImagePlus img2, int aNumOfChannels) {
        final int currentFrame = img2.getFrame();
        setupChannel(img2, currentFrame, 0);
        if (aNumOfChannels > 1) setupChannel(img2, currentFrame, 1);
    }

    private static void setupChannel(ImagePlus img2, final int currentFrame, int channel) {
        inputImages[channel] =  ImgUtils.extractImage(img2, currentFrame, channel + 1 /* 1-based */);
        images[channel] = ImgUtils.ImgToZXYarray(inputImages[channel]);
        ArrayOps.normalize(images[channel]);
    }

    static void segment(int channel) {
        final ImagePlus img = inputImages[channel];
        currentImage = img.getTitle();
                /* Search for maximum and minimum value, normalization */
                double min, max;
                int ni = img.getWidth();
                int nj = img.getHeight();
                int nz = img.getNSlices();
                if (Analysis.norm_max == 0) {
                    MinMax<Double> mm = ImgUtils.findMinMax(img);
                    min = mm.getMin();
                    max = mm.getMax();
                }
                else {
                    min = Analysis.norm_min;
                    max = Analysis.norm_max;
                }
                if (iParameters.usecellmaskX && channel == 0) {
                    ImagePlus maskImg = new ImagePlus();
                    maskImg.setTitle("Cell mask channel 1");
                    cellMaskABinary = createBinaryCellMask(Analysis.iParameters.thresholdcellmask * (max - min) + min, img, channel, maskImg);
                    if (iParameters.livedisplay) {
                        maskImg.show();
                    }
                }
                if (iParameters.usecellmaskY && channel == 1) {
                    ImagePlus maskImg = new ImagePlus();
                    maskImg.setTitle("Cell mask channel 2");
                    cellMaskBBinary = createBinaryCellMask(Analysis.iParameters.thresholdcellmasky * (max - min) + min, img, channel, maskImg);
                    if (iParameters.livedisplay) {
                        maskImg.show();
                    }
                }
        
                if (iParameters.removebackground) {
                    for (int z = 0; z < nz; z++) {
                        img.setSlice(z + 1);
                        ImageProcessor ip = img.getProcessor();
                        final BackgroundSubtracter bs = new BackgroundSubtracter();
                        bs.rollingBallBackground(ip, iParameters.size_rollingball, false, false, false, true, true);
                    }
                }
        
                double[][][] image = ImgUtils.ImgToZXYarray(img);
                MinMax<Double> mm = ArrayOps.findMinMax(image);
                max = mm.getMax();
                min = mm.getMin();
                
                if (iParameters.livedisplay && iParameters.removebackground) {
                    final ImagePlus noBackgroundImg = img.duplicate();
                    noBackgroundImg.setTitle("Background reduction channel " + (channel + 1));
                    noBackgroundImg.changes = false;
                    noBackgroundImg.setDisplayRange(min, max);
                    noBackgroundImg.show();
                }
        
                /* Overload min/max after background subtraction */
                if (Analysis.norm_max != 0) {
                    max = Analysis.norm_max;
                    // if we are removing the background we have no idea which is the minumum across 
                    // all the movie so let be conservative and put min = 0.0 for sure cannot be < 0
                    min = (iParameters.removebackground) ? 0.0 : Analysis.norm_min;
                }
                double minIntensity = (channel == 0) ? iParameters.min_intensity : iParameters.min_intensityY;
                
                SegmentationParameters sp = new SegmentationParameters(
                                                            iParameters.nthreads,
                                                            ((Analysis.iParameters.subpixel) ? ((nz > 1) ? 2 : 4) : 1),
                                                            iParameters.lreg_[channel],
                                                            minIntensity,
                                                            iParameters.exclude_z_edges,
                                                            IntensityMode.values()[iParameters.mode_intensity],
                                                            NoiseModel.values()[iParameters.noise_model],
                                                            iParameters.sigma_gaussian,
                                                            iParameters.sigma_gaussian / iParameters.zcorrec,
                                                            iParameters.min_region_filter_intensities );
                
                //  ============== SEGMENTATION
                logger.debug("------------------- Segmentation of [" + currentImage + "] channel: " + channel + ", frame: " + frame);
                SquasshSegmentation rg = new SquasshSegmentation(image, sp, min, max);
                if (iParameters.patches_from_file == null) {
                    rg.run();
                }
                else {
                    rg.runWithProvidedMask(generateMaskFromPatches(nz, ni, nj));
                }
                
                iOutputImgScale = rg.iLabeledRegions[0].length / ni;
                regionslist.set(channel, rg.iRegionsList);
                regions[channel] = rg.iLabeledRegions;
                logger.debug("------------------- Found " + rg.iRegionsList.size() + " object(s) in channel " + channel);
                // =============================
                IJ.log(rg.iRegionsList.size() + " objects found in " + ((channel == 0) ? "X" : "Y") + ".");
                if (iParameters.dispSoftMask) {
        //            if (out_soft_mask[channel] == null) {
                        out_soft_mask[channel] = new ImagePlus();
        //            }
                    rg.iSoftMask.setTitle("Mask" + ((channel == 0) ? "X" : "Y"));
                    MosaicUtils.MergeFrames(out_soft_mask[channel], rg.iSoftMask);
                    out_soft_mask[channel].setStack(out_soft_mask[channel].getStack());
                }
                ImagePlus maskImg = generateMaskImg(rg.iAllMasks); 
                if (maskImg != null) {maskImg.setTitle("Mask Evol");maskImg.show();}
                System.out.println("END ==============");
    }
    
    public static ImagePlus generateMaskImg(List<float[][][]> aAllMasks) {
        if (aAllMasks.size() == 0) return null;
        
        final float[][][] firstImg = aAllMasks.get(0);
        int iWidth = firstImg[0].length;
        int iHeigth = firstImg[0][0].length;
        int iDepth = firstImg.length;
        
        final ImageStack stack = new ImageStack(iWidth, iHeigth);
        
        for (float[][][] img : aAllMasks)
        for (int z = 0; z < iDepth; z++) {
            stack.addSlice("", new FloatProcessor(img[z]));
        }
      
        final ImagePlus img = new ImagePlus();
        img.setStack(stack);
        img.changes = false;
        img.setDimensions(1, iDepth, aAllMasks.size());
        return img;
    }
    
    private static double[][][] generateMaskFromPatches(int nz, int ni, int nj) {
        final CSV<Particle> csv = new CSV<Particle>(Particle.class);
        csv.setCSVPreferenceFromFile(iParameters.patches_from_file);
        Vector<Particle> pt = csv.Read(iParameters.patches_from_file, new CsvColumnConfig(Particle.ParticleDetection_map, Particle.ParticleDetectionCellProcessor));
   
        // Get the particle related inly to one frames
        final Vector<Particle> pt_f = getPart(pt, frame - 1);
        double[][][] mask = new double[nz][ni][nj];
        // create a mask Image
        drawParticles(mask, pt_f, (int) 3.0);
        
        return mask;
    }
    
    /**
     * Get the particles related to one frame
     *
     * @param part particle vector
     * @param frame frame number
     * @return a vector with particles related to one frame
     */
    private static Vector<Particle> getPart(Vector<Particle> part, int frame) {
        final Vector<Particle> pp = new Vector<Particle>();

        // get the particle related to one frame
        for (Particle p : part) {
            if (p.getFrame() == frame) {
                pp.add(p);
            }
        }

        return pp;
    }
    
    private static void drawParticles(double[][][] aOutputMask, Vector<Particle> aParticles, int aRadius) {
        final int xyzDims[] = new int[] {aOutputMask[0].length, aOutputMask[0][0].length, aOutputMask.length};

        // Create a circle Mask and an iterator
        final BallMask cm = new BallMask(aRadius, /* num od dims */ 3);
        final MaskOnSpaceMapper ballIter = new MaskOnSpaceMapper(cm, xyzDims);

        for (Particle particle : aParticles) {
            // Draw the sphere
            ballIter.setMiddlePoint(new Point((int) (particle.iX), (int) (particle.iY), (int) (particle.iZ)));
            while (ballIter.hasNext()) {
                final Point p = ballIter.nextPoint();
                final int x = p.iCoords[0];
                final int y = p.iCoords[1];
                final int z = p.iCoords[2];
                aOutputMask[z][x][y] = 1.0f;
            }
        }
    }
    
    
    // ============================================ Colocation analysis ==================================

    static double[] pearson_corr() {
        return new SamplePearsonCorrelationCoefficient(inputImages[0], inputImages[1], iParameters.usecellmaskX, iParameters.thresholdcellmask, iParameters.usecellmaskY, iParameters.thresholdcellmasky).run();
    }
    
    static double colocsegA() {
        return coloc(regionslist.get(0), images[1]);
    }

    static double colocsegB() {
        return coloc(regionslist.get(1), images[0]);
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
        int nz = regions.length;
        final int osxy = iOutputImgScale;
        for (Pix p : r.iPixels) {
            int valcoloc = regions[p.pz][p.px][p.py];
            if (valcoloc > 0) {
                countcoloc++;
                if (previousvalcoloc != 0 && valcoloc != previousvalcoloc) {
                    oneColoc = false;
                }
                intColoc += regionlist.get(valcoloc - 1).intensity;
                sizeColoc += regionlist.get(valcoloc - 1).iPixels.size();
                previousvalcoloc = valcoloc;
            }
            count++;
        }
        final double colocthreshold = 0.5;
        r.colocpositive = ((double) countcoloc) / count > colocthreshold;
        r.overlap = ((float) countcoloc) / count;
        if (countcoloc != 0) {
        r.over_size = (nz == 1) ? (float) (sizeColoc / countcoloc) / (osxy * osxy)
                                : (float) (sizeColoc / countcoloc) / (osxy * osxy * osxy);

        r.over_int = (float) (intColoc) / countcoloc;
        }
        else {
            r.over_size = 0;
            r.over_int = 0;
        }
        r.singlec = oneColoc;

        return r.colocpositive;
    }

    private static double regionsum(Region r, double[][][] image) {
        final int factor2 = iOutputImgScale;
        int fz2 = (image.length > 1) ? factor2 : 1;

        int count = 0;
        double sum = 0;
        for (Pix p : r.iPixels) {
            sum += image[p.pz / fz2][p.px / factor2][p.py / factor2];
            count++;
        }

        r.coloc_o_int = (sum / count);
        return (sum / count);
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

        if (Analysis.iParameters.subpixel) {
            return (totalsize / objects) / (Math.pow(iOutputImgScale, 2));
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
            totalsize += r.iPixels.size();
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
        final int factor2 = iOutputImgScale;
        int fz2 = (overallCellMaskBinary.length > 1) ? factor2 : 1;

        double size = 0;
        int inside = 0;
        for (Pix px : r.iPixels) {
            if (overallCellMaskBinary[px.pz / fz2][px.px / factor2][px.py / factor2]) {
                inside++;
            }
            size++;
        }
        return ((inside / size) > 0.1);
    }

    static void computeOverallMask(int nz, int ni, int nj) {
        final boolean mask[][][] = new boolean[nz][ni][nj];

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (iParameters.usecellmaskX && iParameters.usecellmaskY) {
                        mask[z][i][j] = cellMaskABinary[z][i][j] && cellMaskBBinary[z][i][j];
                    }
                    else if (iParameters.usecellmaskX) {
                        mask[z][i][j] = cellMaskABinary[z][i][j];
                    }
                    else if (iParameters.usecellmaskY) {
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

    static void setRegionsLabels(ArrayList<Region> regionslist, short[][][] regions, int nz, int ni, int nj) {
        final int factor2 = iOutputImgScale;
        int fz2 = (nz > 1) ? factor2 : 1;
        for (int z = 0; z < nz * fz2; z++) {
            for (int i = 0; i < ni * factor2; i++) {
                for (int j = 0; j < nj * factor2; j++) {
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
        for (Pix px : r.iPixels) {
            regions[px.pz][px.px][px.py] = label;
        }
    }
    
    static boolean[][][] createBinaryCellMask(double aThreshold, ImagePlus aInputImage, int aChannel, ImagePlus aOutputImage) {
        int ni = aInputImage.getWidth();
        int nj = aInputImage.getHeight();
        int nz = aInputImage.getNSlices();
        final ImageStack maskStack = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            aInputImage.setSlice(z + 1);
            ImageProcessor ip = aInputImage.getProcessor();
            final byte[] mask = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (ip.getPixelValue(i, j) > aThreshold) {
                        mask[j * ni + i] = (byte) 255;
                    }
                    else {
                        mask[j * ni + i] = 0;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(mask);
            maskStack.addSlice("", bp);
        }
        final ImagePlus maskImg = (aOutputImage == null) ? new ImagePlus("Cell mask channel " + (aChannel + 1)) : aOutputImage;
        maskImg.setStack(maskStack);
        IJ.run(maskImg, "Invert", "stack");
        IJ.run(maskImg, "Fill Holes", "stack");
        IJ.run(maskImg, "Open", "stack");
        IJ.run(maskImg, "Invert", "stack");

        final boolean[][][] cellmask = new boolean[nz][ni][nj];
        for (int z = 0; z < nz; z++) {
            maskImg.setSlice(z + 1);
            ImageProcessor ip = maskImg.getProcessor();
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    cellmask[z][i][j] = ip.getPixelValue(i, j) != 0;
                }
            }
        }
        
        return cellmask;
    }
}
