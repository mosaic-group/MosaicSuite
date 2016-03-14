package mosaic.bregman;


import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.plugin.Resizer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mosaic.bregman.ColocalizationAnalysis.ChannelColoc;
import mosaic.bregman.ColocalizationAnalysis.ChannelPair;
import mosaic.bregman.ColocalizationAnalysis.ColocResult;
import mosaic.bregman.ColocalizationAnalysis.RegionColoc;
import mosaic.bregman.Files.FileInfo;
import mosaic.bregman.Files.FileType;
import mosaic.bregman.output.ImageColoc;
import mosaic.bregman.output.ImageData;
import mosaic.bregman.output.ObjectsColoc;
import mosaic.bregman.output.ObjectsData;
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
import mosaic.utils.SysOps;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;

public class SquasshLauncher {
    private static final Logger logger = Logger.getLogger(SquasshLauncher.class);
    
    // Global normalization, if not provided (set to 0) min/max will be searched for provided file.
    private final double iGlobalNormalizationMin;
    private final double iGlobalNormalizationMax;
    
    private final Parameters iParameters;

    private int ni;
    private int nj;
    private int nz;
    private int iNumOfChannels = -1;
    private int iOutputImgScale = 1;
    
    private ImagePlus[] iInputImages;
    private double[][][][] iNormalizedImages;
    private boolean[][][][] iCellMasks;
    private boolean[][][] iOoverallCellMask;
    private List<List<Region>> iRegionsList;
    private short[][][][] iLabeledRegions;
    private double[][][][] iSoftMasks;
    private ImagePlus[] iOutSoftMasks;
    private ImagePlus[] iOutOutlines;
    private ImagePlus[] iOutIntensities;
    private int iMaxNumberOfRegionsFound = 0;
    private ImagePlus[] iOutLabeledRegionsColor;
    private ImagePlus[] iOutLabeledRegionsGray;
    private ImagePlus[] iOutColoc;
    
    private List<ChannelPair> iAnalysisPairs;
    
    private Set<FileInfo> iSavedFilesInfo = new LinkedHashSet<FileInfo>();
    private void addSavedFile(FileType aFI, String aFileName) {
        iSavedFilesInfo.add(new FileInfo(aFI, SysOps.removeRedundantSeparators(aFileName)));
    } 
    public Set<FileInfo> getSavedFiles() { return iSavedFilesInfo; }
    
    public SquasshLauncher(ImagePlus aImage, Parameters aParameters, String aOutputDir, double aNormalizationMin, double aNormalizationMax) {
        this(aImage, aParameters, aOutputDir, aNormalizationMin, aNormalizationMax, null);
    }
    
    /**
     * Launch the Segmentation for provided image. Calculate colocalization, generate images and save all files according to settings provided in aParameters/aOutputDir.
     * @param aImage image to be segmented
     */
    public SquasshLauncher(ImagePlus aImage, Parameters aParameters, String aOutputDir, double aNormalizationMin, double aNormalizationMax, List<ChannelPair> aAnalysisPairs) {
        iParameters = aParameters;
        iGlobalNormalizationMin = aNormalizationMin;
        iGlobalNormalizationMax = aNormalizationMax;
        
        if (aImage == null) {
            IJ.error("No image to process");
            return;
        }
        if (aImage.getType() == ImagePlus.COLOR_RGB) {
            IJ.error("This is a color image and is not supported, convert into 8-bit , 16-bit or float");
            return;
        }
        
        // Image info
        final String title = aImage.getTitle();
        ni = aImage.getWidth();
        nj = aImage.getHeight();
        nz = aImage.getNSlices();
        final int numOfFrames = aImage.getNFrames();
        final int numOfChannels = aImage.getNChannels();
        logger.debug("Segmenting Image: [" + title + "] Dims(x/y/z): "+ ni + "/" + nj + "/" + nz + " NumOfFrames: " + numOfFrames + " NumOfChannels: " + numOfChannels);
        
        // Initiate structures
        iNumOfChannels = numOfChannels;
        iInputImages = new ImagePlus[iNumOfChannels];
        iNormalizedImages = new double[iNumOfChannels][][][];
        iCellMasks = new boolean[iNumOfChannels][][][];
        iRegionsList = new ArrayList<List<Region>>(iNumOfChannels);
        for (int i = 0; i < iNumOfChannels; i++) iRegionsList.add(null);
        iLabeledRegions = new short[iNumOfChannels][][][];
        iSoftMasks = new double[iNumOfChannels][][][];
        iOutSoftMasks = new ImagePlus[iNumOfChannels];
        iOutOutlines = new ImagePlus[iNumOfChannels];
        iOutIntensities = new ImagePlus[iNumOfChannels];
        iOutLabeledRegionsColor = new ImagePlus[iNumOfChannels];
        iOutLabeledRegionsGray = new ImagePlus[iNumOfChannels];
        
        iAnalysisPairs = computeValidChannelsPairsForImage(aAnalysisPairs, iNumOfChannels);
        
        iOutColoc = new ImagePlus[iAnalysisPairs.size()];
        
        String outFileName = SysOps.removeExtension(title);
        for (int frame = 1; frame <= numOfFrames; frame++) {
            aImage.setPosition(aImage.getChannel(), aImage.getSlice(), frame);      

            runSegmentation(aImage, frame, title);
            
            displayAndUpdateImages(outFileName);
            if (iParameters.save_images) {
                writeImageDataCsv(aOutputDir, title, outFileName, frame - 1);
                writeObjectDataCsv(aOutputDir, title, outFileName, frame - 1);
                
                // Compute and apply colocalization mask
                for (int i = 0; i < iNumOfChannels; i++) generateMasks(i, iInputImages[i]);
                computeOverallMask(nz, ni, nj);
                List<List<Region>> maskedRegionList = applyMask();
                final int factor2 = iOutputImgScale;
                ColocalizationAnalysis ca = new ColocalizationAnalysis((nz > 1) ? factor2 : 1, factor2, factor2);
                mosaic.utils.Debug.print("LOOP", frame, maskedRegionList.size(), maskedRegionList);
                for (int i = 0; i < iNumOfChannels; i++) {
                    mosaic.utils.Debug.print(i, maskedRegionList.get(i).size(), maskedRegionList.get(i));
                }
                Map<ChannelPair, ColocResult> allColocs = ca.calculateAll(iAnalysisPairs, maskedRegionList, iLabeledRegions, iNormalizedImages);
                writeImageColoc(aOutputDir, title, outFileName, frame - 1, allColocs);
                writeObjectsColocCsv(aOutputDir, title, outFileName, frame - 1, allColocs);
            }
        }

        if (iParameters.save_images) {
            saveAllImages(aOutputDir);
        }
        
        logger.info("Saved files:");
        for (FileInfo f : iSavedFilesInfo) {
            logger.info("            " + f);
        }
    }
    
    private List<ChannelPair> computeValidChannelsPairsForImage(List<ChannelPair> aAnalysisPairs, int aNumOfChannels) {
        // Always crate new container since later some of its elements are removed
        // If it would be done on input parameter it would impact processing of multiple images.
        
        List<ChannelPair> iAnalysisPairs = new ArrayList<ChannelPair>();
        if (aAnalysisPairs != null) { 
            iAnalysisPairs.addAll(aAnalysisPairs);
        }
        else {
            // If not provided add all comparisons for channels 0, 1, 2
            final int numOfChannels = 3;
            for (int c1 = 0; c1 < numOfChannels; c1++)
                for (int c2 = c1; c2 < numOfChannels; c2++) {
                    if (c1 != c2) {
                        iAnalysisPairs.add(new ChannelPair(c1, c2));
                        iAnalysisPairs.add(new ChannelPair(c2, c1));
                    }
                }
        }
        
        // Remove all pairs not applicable for current image (user can define channel pairs 
        // arbitrarily - it is needed for batch processing multiple files with possibly different 
        // diemnsions).
        for (Iterator<ChannelPair> iterator = iAnalysisPairs.iterator(); iterator.hasNext(); ) {
            ChannelPair cp = iterator.next();
            if (cp.ch1 >= aNumOfChannels || cp.ch2 >= aNumOfChannels) {
                iterator.remove();
            }
        }
        
        return iAnalysisPairs;
    }
    
    /**
     * Display results
     * @param separate true if you do not want separate the images
     */
    private void displayAndUpdateImages(String aTitle) {
        
        final int factor = iOutputImgScale;
        int fz = (nz > 1) ? factor : 1;

        for (int channel = 0; channel < iNumOfChannels; ++channel) {
            if (iParameters.dispoutline) {
                final ImagePlus img = generateOutlineOverlay(iLabeledRegions[channel], iNormalizedImages[channel]);
                updateImages(channel, img, Files.createTitleWithExt(FileType.Outline, aTitle, channel + 1), iParameters.dispoutline, iOutOutlines);
            }
            if (iParameters.dispint) {
                ImagePlus img = generateIntensitiesImg(iRegionsList.get(channel), nz * fz, ni * factor, nj * factor);
                updateImages(channel, img, Files.createTitleWithExt(FileType.Intensity, aTitle, channel + 1), iParameters.dispint, iOutIntensities);
            }
            if (iParameters.displabels || iParameters.dispcolors) {
                iMaxNumberOfRegionsFound = (iMaxNumberOfRegionsFound < iRegionsList.get(channel).size()) ? iRegionsList.get(channel).size() : iMaxNumberOfRegionsFound;
                ImagePlus img = generateRegionImg(iLabeledRegions[channel], iMaxNumberOfRegionsFound, "");
                updateImages(channel, img, Files.createTitleWithExt(FileType.Segmentation, aTitle, channel + 1), iParameters.displabels, iOutLabeledRegionsColor);
                if (iParameters.dispcolors) {
                    final ImagePlus imgGray = generateLabelsGray(img);
                    updateImages(channel, imgGray, Files.createTitleWithExt(FileType.Mask, aTitle, channel + 1), iParameters.dispcolors, iOutLabeledRegionsGray);
                }
            }
            if (iParameters.dispSoftMask) {
                ImagePlus img = ImgUtils.ZXYarrayToImg(iSoftMasks[channel], "Mask" + ((channel == 0) ? "X" : "Y"));
                updateImages(channel, img, Files.createTitleWithExt(FileType.SoftMask, aTitle, channel + 1), iParameters.dispSoftMask, iOutSoftMasks);
            }
        }
        
        // Channel colloc image is created only for one pair, the second opposite should be skipped (i.e. only for pair (0,1) and not for (1,0)
        Set<ChannelPair> processedChannels = new HashSet<ChannelPair>();
        for (int i = 0; i < iAnalysisPairs.size(); ++i) {
            ChannelPair cp = iAnalysisPairs.get(i);
            if (processedChannels.add(cp)) {
                // Add also opposite pair
                processedChannels.add(new ChannelPair(cp.ch2, cp.ch1));
                ImagePlus img = generateColocImage(cp);
                updateImages(i, img, Files.createTitleWithExt(FileType.Colocalization, aTitle + "_ch_" + cp.ch1 + "_" + cp.ch2 + "_" ), true, iOutColoc);
            }
        }
    }

    /**
     * Save all images
     * @param aOutputPath where to save
     */
    private void saveAllImages(String aOutputPath) {
        final String savePath = aOutputPath + File.separator;
        for (int i = 0; i < iNumOfChannels; i++) {
            if (iOutOutlines[i] != null) {
                String fileName = savePath + iOutOutlines[i].getTitle();
                IJ.save(iOutOutlines[i], fileName);
                addSavedFile(FileType.Outline, fileName);
            }
            if (iOutIntensities[i] != null) {
                String fileName = savePath + iOutIntensities[i].getTitle();
                IJ.save(iOutIntensities[i], fileName);
                addSavedFile(FileType.Intensity, fileName);
            }
            if (iOutLabeledRegionsColor[i] != null) {
                String fileName = savePath + iOutLabeledRegionsColor[i].getTitle();
                IJ.save(iOutLabeledRegionsColor[i], fileName);
                addSavedFile(FileType.Segmentation, fileName);
            }
            if (iOutLabeledRegionsGray[i] != null) {
                String fileName = savePath + iOutLabeledRegionsGray[i].getTitle();
                IJ.save(iOutLabeledRegionsGray[i], fileName);
                addSavedFile(FileType.Mask, fileName);
            }
            if (iOutSoftMasks[i] != null) {
                String fileName = savePath + iOutSoftMasks[i].getTitle();
                IJ.save(iOutSoftMasks[i], fileName);
                addSavedFile(FileType.SoftMask, fileName);
            }
        }
        for (ImagePlus ip : iOutColoc) {
            if (ip != null) {
                String fileName = savePath + ip.getTitle();
                IJ.save(ip, fileName);
                addSavedFile(FileType.Colocalization, fileName);
            }
        }
    }

    private void runSegmentation(ImagePlus aImage, int aCurrentFrame, String aTitle) {
        for (int channel = 0; channel < iNumOfChannels; channel++) {
            logger.debug("------------------- Segmentation of [" + aTitle + "] channel: " + channel + ", frame: " + aCurrentFrame);
            iInputImages[channel] =  ImgUtils.extractImage(aImage, aCurrentFrame, channel + 1 /* 1-based */, true /* make copy */);
            iNormalizedImages[channel] = ImgUtils.ImgToZXYarray(iInputImages[channel]);
            ArrayOps.normalize(iNormalizedImages[channel]);
            segment(channel, aCurrentFrame);
            logger.debug("------------------- End of Segmentation ---------------------------");
        }
    }

    private ImagePlus generateColocImage(ChannelPair aChannelPair) {
        final int factor2 = iOutputImgScale;
        int fz2 = (nz > 1) ? factor2 : 1;

        return generateColocImg(iRegionsList.get(aChannelPair.ch1), iRegionsList.get(aChannelPair.ch2), ni * factor2, nj * factor2, nz * fz2);
    }
    
    private ImagePlus generateOutlineOverlay(short[][][] regions, double[][][] aImage) {
        final ImagePlus regionsOutlines = generateRegionOutlinesImg(regions);
        final ImagePlus image = generateImage(aImage);
        final int dz = regions.length;
        final int di = regions[0].length;
        final int dj = regions[0][0].length;
        final ImagePlus scaledImg = scaleImage(image, dz, di, dj);
        return RGBStackMerge.mergeChannels(new ImagePlus[] {regionsOutlines, scaledImg}, false);
    }

    private ImagePlus generateLabelsGray(ImagePlus aImage) {
        final ImagePlus img = aImage.duplicate();
        IJ.run(img, "Grays", "");
        img.resetDisplayRange();
        return img;
    }
    
    private void updateImages(int channel, final ImagePlus img, final String title, final boolean show, final ImagePlus[] imgs) {
        if (imgs[channel] != null) {
            MosaicUtils.MergeFrames(imgs[channel], img);
        }
        else {
            imgs[channel] = img;
            imgs[channel].setTitle(title);
        }

        if (show) {
            // this force the update of the image
            imgs[channel].setStack(imgs[channel].getStack());
            imgs[channel].show();
        }
    }

    private void segment(int channel, int frame) {
        final ImagePlus img = iInputImages[channel];

        if (iParameters.removebackground) {
            for (int z = 0; z < nz; z++) {
                img.setSlice(z + 1);
                final BackgroundSubtracter bs = new BackgroundSubtracter();
                bs.rollingBallBackground(img.getProcessor(), iParameters.size_rollingball, false, false, false, true, true);
            }
        }
        
        double[][][] image = ImgUtils.ImgToZXYarray(img);
        
        // Calculate min/max. If we are removing the background we have no idea which is the minimum across 
        // all the frames so let be conservative and put min = 0.0 (for sure cannot be < 0).
        double min = (iParameters.removebackground) ? 0.0 : iGlobalNormalizationMin;
        double max = iGlobalNormalizationMax;
        if (iGlobalNormalizationMax == 0) {
            MinMax<Double> mm = ImgUtils.findMinMax(img);
            min = mm.getMin();
            max = mm.getMax();
        }
        
        double minIntensity = (channel == 0) ? iParameters.min_intensity : iParameters.min_intensityY;
        // TODO: Temporary solution. When GUI/Paramters will work with more than two channels it must be removed. 
        //       Currently for channels > 1, settings for channel 1 are chosen.
        int tempChannel = channel;
        if (channel > 1) channel = 1;
        SegmentationParameters sp = new SegmentationParameters(
                iParameters.nthreads,
                ((iParameters.subpixel) ? ((nz > 1) ? 2 : 4) : 1),
                iParameters.lreg_[channel],
                minIntensity,
                iParameters.exclude_z_edges,
                IntensityMode.values()[iParameters.mode_intensity],
                NoiseModel.values()[iParameters.noise_model],
                iParameters.sigma_gaussian,
                iParameters.sigma_gaussian / iParameters.zcorrec,
                iParameters.min_region_filter_intensities );
        //  ============== SEGMENTATION
        SquasshSegmentation rg = new SquasshSegmentation(image, sp, min, max);
        if (iParameters.patches_from_file == null) {
            rg.run();
        }
        else {
            rg.runWithProvidedMask(generateMaskFromPatches(iParameters.patches_from_file, nz, ni, nj, frame));
        }
        channel = tempChannel;
        iOutputImgScale = rg.iLabeledRegions[0].length / ni;
        
        iLabeledRegions[channel] = rg.iLabeledRegions; 
        iRegionsList.set(channel, rg.iRegionsList);
        
        logger.debug("------------------- Found " + rg.iRegionsList.size() + " object(s) in channel " + channel);
        // =============================
        iSoftMasks[channel] = rg.iSoftMask;
        ImagePlus maskImg = generateMaskImg(rg.iAllMasks); 
        if (maskImg != null) {maskImg.setTitle("Mask Evol");maskImg.show();}
    }
    
    // ============================================ MASKS and tools ==============================
    
    private void generateMasks(int channel, final ImagePlus img) {
        double minNorm = iGlobalNormalizationMin;
        double maxNorm = iGlobalNormalizationMax;
        if (iGlobalNormalizationMax == 0) {
            MinMax<Double> mm = ImgUtils.findMinMax(img);
            minNorm = mm.getMin();
            maxNorm = mm.getMax();
        }
        if (iParameters.usecellmaskX && channel == 0) {
            iCellMasks[channel] = generateMask(channel, img, minNorm, maxNorm, iParameters.thresholdcellmask);
        }
        if (iParameters.usecellmaskY && channel == 1) {
            iCellMasks[channel] = generateMask(channel, img, minNorm, maxNorm, iParameters.thresholdcellmasky);
        }
    }
    
    private boolean[][][] generateMask(int channel, final ImagePlus img, double min, double max, double maskThreshold) {
        ImagePlus mask = createBinaryCellMask(img, "Cell mask channel " + (channel + 1), maskThreshold * (max - min) + min);
        if (iParameters.livedisplay) {
            mask.show();
        }
        
        return ImgUtils.imgToZXYbinaryArray(mask);
    }
    
    private void computeOverallMask(int nz, int ni, int nj) {
        iOoverallCellMask = new boolean[nz][ni][nj];

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (iParameters.usecellmaskX && iParameters.usecellmaskY) {
                        iOoverallCellMask[z][i][j] = iCellMasks[0][z][i][j] && iCellMasks[1][z][i][j];
                    }
                    else if (iParameters.usecellmaskX) {
                        iOoverallCellMask[z][i][j] = iCellMasks[0][z][i][j];
                    }
                    else if (iParameters.usecellmaskY) {
                        iOoverallCellMask[z][i][j] = iCellMasks[1][z][i][j];
                    }
                    else {
                        iOoverallCellMask[z][i][j] = true;
                    }

                }
            }
        }
    }
    
    private List<List<Region>> applyMask() {
        List<List<Region>> maskedRegionList = new ArrayList<List<Region>>();
        for (int channel = 0; channel < iNumOfChannels; channel++) {
            final ArrayList<Region> maskedRegion = new ArrayList<Region>();
            for (Region r : iRegionsList.get(channel)) {
                if (isInside(r)) {
                    maskedRegion.add(r);
                }
            }
            maskedRegionList.add(maskedRegion);
        }
        return maskedRegionList;
    }
    
    private boolean isInside(Region r) {
        final int factor2 = iOutputImgScale;
        int fz2 = (iOoverallCellMask.length > 1) ? factor2 : 1;

        double size = r.iPixels.size();
        int inside = 0;
        for (Pix px : r.iPixels) {
            if (iOoverallCellMask[px.pz / fz2][px.px / factor2][px.py / factor2]) {
                inside++;
            }
        }
        return ((inside / size) > 0.1);
    }
    
    // ============================== Mask from particles ======================================
    
    private double[][][] generateMaskFromPatches(String aFileName, int nz, int ni, int nj, int aFrame) {
        final CSV<Particle> csv = new CSV<Particle>(Particle.class);
        csv.setCSVPreferenceFromFile(aFileName);
        Vector<Particle> pt = csv.Read(aFileName, new CsvColumnConfig(Particle.ParticleDetection_map, Particle.ParticleDetectionCellProcessor));
   
        // Get the particle related inly to one frames
        final Vector<Particle> pt_f = getPart(pt, aFrame - 1);
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
    private Vector<Particle> getPart(Vector<Particle> part, int frame) {
        final Vector<Particle> pp = new Vector<Particle>();

        // get the particle related to one frame
        for (Particle p : part) {
            if (p.getFrame() == frame) {
                pp.add(p);
            }
        }

        return pp;
    }
    
    private void drawParticles(double[][][] aOutputMask, Vector<Particle> aParticles, int aRadius) {
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
    
    // ============================================ Regions analysis ==================================

    private double meanSurface(List<Region> aRegionsList) {
        double totalPerimeter = 0;
        for (Region r : aRegionsList) {
            totalPerimeter += r.perimeter;
        }

        return (totalPerimeter / aRegionsList.size());
    }

    private double meanSize(List<Region> aRegionsList) {
        double totalSize = 0;
        for (Region r : aRegionsList) {
            totalSize += r.iPixels.size();
        }

        return (totalSize / aRegionsList.size()) / (Math.pow(iOutputImgScale, 2));
    }
    
    private double meanLength(List<Region> aRegionsList) {
        double totalLength = 0;
        for (Region r : aRegionsList) {
            totalLength += r.length;
        }
        
        return (totalLength / aRegionsList.size());
    }

    // ====================================== TOOLS ================================
    
    // Round y to z-places after comma
    private double round(double y, final int z) {
        final double factor = Math.pow(10,  z);
        y *= factor;
        y = (int) y;
        y /= factor;
        return y;
    }
    
    public static ImagePlus createBinaryCellMask(ImagePlus aInputImage, String aTitle, double aThreshold) {
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
                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(mask);
            maskStack.addSlice("", bp);
        }
        final ImagePlus maskImg = new ImagePlus(aTitle, maskStack);
        IJ.run(maskImg, "Invert", "stack");
        
        // "Fill Holes" is using Prefs.blackBackground global setting. We need false here.
        boolean tempBlackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        IJ.run(maskImg, "Fill Holes", "stack");
        ij.Prefs.blackBackground = tempBlackbackground;
        
        IJ.run(maskImg, "Open", "stack");
        IJ.run(maskImg, "Invert", "stack");
        maskImg.changes = false;
        
        return maskImg;
    }
    // ==================================== Image generation ==========================
    
    /**
     * @return Generated image with regions with of given size.
     */
    private ImagePlus generateColocImg(List<Region> aRegionsListA, List<Region> aRegionsListB, int iWidth, int iHeigth, int iDepth) {
        final int[][] imagecolor = new int[iDepth][iWidth * iHeigth];
        setRgb(imagecolor, aRegionsListA, /* green */ 1, iWidth);
        setRgb(imagecolor, aRegionsListB, /* red */ 2, iWidth);

        // Merge them into one Color image
        ImageStack is = new ImageStack(iWidth, iHeigth);
        for (int z = 0; z < iDepth; z++) {
            final ColorProcessor colorProc = new ColorProcessor(iWidth, iHeigth, imagecolor[z]);
            is.addSlice(colorProc);
        }
        
        ImagePlus result = new ImagePlus("Colocalization", is);
        result.setDimensions(1, iDepth, 1);
        return result;
    }
    
    /**
     * Draws regions with provided color
     * @param pixels array of [z][x*y] size
     * @param aRegions regions to draw
     * @param aColor color R=2, G=1, B=0
     * @param aWidth width of [x*y]
     */
    private void setRgb(int[][] pixels, List<Region> aRegions, int aColor, int aWidth) {
        int shift = 8 * aColor;
        for (final Region r : aRegions) {
            for (final Pix p : r.iPixels) {
                pixels[p.pz][p.px + p.py * aWidth] = pixels[p.pz][p.px + p.py * aWidth] | 0xff << shift;
            }
        }
    }
    
    /**
     * @return ImagePlus generated from all provided masks (each mask is one new frame)
     */
    private ImagePlus generateMaskImg(List<float[][][]> aAllMasks) {
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
    
    /**
     * @return ImagePlus generated for all provided regions, each region has its own color.
     */
    private ImagePlus generateRegionImg(short[][][] aLabeledRegions, int aMaxRegionNumber, final String aTitle) {
        final int depth = aLabeledRegions.length;
        final int width = aLabeledRegions[0].length;
        final int height = aLabeledRegions[0][0].length;
        final int minPixelValue = 0;
        final int maxPixelValue = Math.max(aMaxRegionNumber, 255);

        ImageStack is = new ImageStack(width, height);
        for (int z = 0; z < depth; z++) {
            final short[] pixels = new short[width * height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    pixels[j * width + i] = aLabeledRegions[z][i][j];
                }
            }
            final ShortProcessor sp = new ShortProcessor(width, height, pixels, null);
            sp.setMinAndMax(minPixelValue, maxPixelValue);
            is.addSlice("", sp);
        }

        is.setColorModel(generateColorModel(aMaxRegionNumber));
        
        return new ImagePlus(aTitle, is);
    }

    /**
     * @return IndexColorModel with provided number of colors. Each color is maximally different
     *         from the others and has maximum brightness and saturation.
     */
    private IndexColorModel generateColorModel(int aNumOfColors) {
        int numOfColors = aNumOfColors > 255 ? 255 : aNumOfColors;
        final byte[] r = new byte[256];
        final byte[] g = new byte[256];
        final byte[] b = new byte[256];
        // Set all to white:
        for (int i = 0; i < 256; ++i) {
            r[i] = g[i] = b[i] = (byte) 255;
        }
        // Set 0 to black:
        r[0] = g[0] = b[0] = 0;
        for (int i = 1; i <= numOfColors; ++i) {
            Color c = Color.getHSBColor((float)(i - 1) / numOfColors, 1.0f, 1.0f);
            r[i] = (byte) c.getRed();
            g[i] = (byte) c.getGreen();
            b[i] = (byte) c.getBlue();
        }
        return new IndexColorModel(8, 256, r, g, b);
    }
    
    /**
     * @return ImagePlus type Byte from array [z][x][y]
     */
    private ImagePlus generateImage(double[][][] image) {
        final int nz = image.length;
        final int ni = image[0].length;
        final int nj = image[0][0].length;
        
        ImageStack is = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final byte[] pixels = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    pixels[j * ni + i] = (byte) (255 * image[z][i][j]);
                }
            }

            final ByteProcessor bp = new ByteProcessor(ni, nj, pixels);
            is.addSlice("", bp);
        }
        
        return new ImagePlus("Image", is);
    }

    /**
     * @return scaled input image with provided output resolution
     */
    private ImagePlus scaleImage(ImagePlus aInputImg, int aNewZ, int aNewX, int aNewY) {
        ImagePlus outputImg = new Resizer().zScale(aInputImg, aNewZ, ImageProcessor.NONE);
        final ImageStack imgS2 = new ImageStack(aNewX, aNewY);
        for (int z = 0; z < aNewZ; z++) {
            outputImg.setSliceWithoutUpdate(z + 1);
            outputImg.getProcessor().setInterpolationMethod(ImageProcessor.NONE);
            imgS2.addSlice("", outputImg.getProcessor().resize(aNewX, aNewY, false));
        }
        outputImg.setStack(imgS2);
        return outputImg;
    }

    /**
     * @return image with generated outlines of regions
     */
    private ImagePlus generateRegionOutlinesImg(short[][][] aLabeledRegions) {
        final int dz = aLabeledRegions.length;
        final int di = aLabeledRegions[0].length;
        final int dj = aLabeledRegions[0][0].length;
        ImageStack is = new ImageStack(di, dj);
        for (int z = 0; z < dz; z++) {
            final byte[] mask_bytes = new byte[di * dj];
            for (int i = 0; i < di; i++) {
                for (int j = 0; j < dj; j++) {
                    if (aLabeledRegions[z][i][j] == 0) {
                        mask_bytes[j * di + i] = (byte) 255;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(di, dj, mask_bytes);
            final BinaryProcessor bip = new BinaryProcessor(bp);
            bip.outline();
            bip.invert();
            is.addSlice("", bp);
        }
        return new ImagePlus("regionsOutlines", is);
    }
    
    /**
     * @return ImagePlus with intensities image with provided dimensions
     */
    private ImagePlus generateIntensitiesImg(List<Region> aRegions, int aDepth, int aWidth, int aHeight) {
        short[][] pixels = new short [aDepth][aWidth * aHeight];
        for (final Region r : aRegions) {
            for (final Pix p : r.iPixels) {
                pixels[p.pz][p.px + p.py * aWidth] = (short)r.intensity;
            }
        }
        ImageStack is = new ImageStack(aWidth, aHeight);
        for (int z = 0; z < aDepth; z++) {
            final ShortProcessor sp = new ShortProcessor(aWidth, aHeight, pixels[z], null);
            is.addSlice("", sp);
        }
        
        return new ImagePlus("Intensities", is);
    }
    
// ================================================ NEW OUTPUT 
    
    // ------------------------------------------------------------------
    
    List<ObjectsData> getObjectsData(List<Region> regions, String aFile, int frame, int channel) {
        List<ObjectsData> res = new ArrayList<ObjectsData>();
        
        for (Region r : regions) {
            ObjectsData id = new ObjectsData(aFile,
                                             frame, 
                                             channel, 
                                             r.iLabel, 
                                             r.getcx(),
                                             r.getcy(),
                                             r.getcz(),
                                             (float)round(r.getrsize(), 3), // TODO: Nasty way did by old impl. - currently kept to have same results but should be changed.
                                             r.getperimeter(),
                                             r.getlength(),
                                             r.getintensity());
            res.add(id);
        }
        
        return res;
    }
    
    private void writeObjectDataCsv(String aOutputPath, String aTitle, String aOutFileName, int aCurrentFrame) {
        String outFileName = aOutputPath + Files.createTitleWithExt(FileType.ObjectsDataNew, aOutFileName);
        final CSV<ObjectsData> csv = new CSV<ObjectsData>(ObjectsData.class);
        csv.setDelimiter(';');
        csv.setMetaInformation("background", aOutputPath + aTitle);
        
        for (int ch = 0; ch < iNumOfChannels; ch++) {
            boolean shouldAppend = !(aCurrentFrame == 0 && ch == 0);
            csv.Write(outFileName, getObjectsData(iRegionsList.get(ch), aTitle, aCurrentFrame, ch), ObjectsData.ColumnConfig, shouldAppend);
            addSavedFile(FileType.ObjectsDataNew, outFileName);
        }
    }
    
    // ------------------------------------------------------------------
    
    List<ObjectsColoc> getObjectsColoc(String aFile, int frame, int channel, int channelColoc, Map<Integer, RegionColoc> regionsColoc) {
        List<ObjectsColoc> res = new ArrayList<ObjectsColoc>();
        
        for (Integer label : regionsColoc.keySet()) {
            RegionColoc regionColoc = regionsColoc.get(label);
            ObjectsColoc id = new ObjectsColoc(aFile,
                                               frame, 
                                               channel, 
                                               label, 
                                               channelColoc,
                                               regionColoc.overlapFactor,
                                               regionColoc.colocObjectsAverageArea,
                                               regionColoc.colocObjectsAverageIntensity,
                                               regionColoc.colocObjectIntensity,
                                               regionColoc.singleRegionColoc);
            res.add(id);
        }
        
        return res;
    }
    
    private void writeObjectsColocCsv(String aOutputPath, String aTitle, String aOutFileName, int aCurrentFrame, Map<ChannelPair, ColocResult> allColocs) {
        String outFileName = aOutputPath + Files.createTitleWithExt(FileType.ObjectsColocNew, aOutFileName);
        final CSV<ObjectsColoc> csv = new CSV<ObjectsColoc>(ObjectsColoc.class);
        csv.setDelimiter(';');
        csv.setMetaInformation("background", aOutputPath + aTitle);
        
        boolean shouldAppend = aCurrentFrame != 0;
        for (ChannelPair cp : iAnalysisPairs) {
            Map<Integer, RegionColoc> regionsColoc = allColocs.get(cp).regionsColoc;
            csv.Write(outFileName, getObjectsColoc(aTitle, aCurrentFrame, cp.ch1, cp.ch2, regionsColoc), ObjectsColoc.ColumnConfig, shouldAppend);
            shouldAppend = true;
            addSavedFile(FileType.ObjectsColocNew, outFileName);
        }
    }
    
    // ------------------------------------------------------------------

    List<ImageColoc> getImageColoc(ChannelColoc aColoc, double[] aPearson, String aFile, int frame, int channel1, int channel2) {
        List<ImageColoc> res = new ArrayList<ImageColoc>();
        
        ImageColoc id = new ImageColoc(aFile,
                                       frame, 
                                       channel1, 
                                       channel2,
                                       round(aColoc.colocSignal, 4),
                                       round(aColoc.colocSize, 4),
                                       round(aColoc.colocNumber, 4),
                                       round(aColoc.coloc, 4),
                                       round(aPearson[0], 4),
                                       round(aPearson[1], 4));
        res.add(id);
        
        return res;
    }

    private void writeImageColoc(String aOutputPath, String aTitle, String aOutFileName, int aCurrentFrame, Map<ChannelPair, ColocResult> allColocs) {
        String outFileName = aOutputPath + Files.createTitleWithExt(FileType.ImageColocNew, aOutFileName);
        final CSV<ImageColoc> csv = new CSV<ImageColoc>(ImageColoc.class);
        csv.setDelimiter(';');
        csv.setMetaInformation("background", aOutputPath + aTitle);
        
        boolean shouldAppend = !(aCurrentFrame == 0);
        for (ChannelPair cp : iAnalysisPairs) {
            ChannelColoc resAB = allColocs.get(cp).channelColoc;
            double[] pearsonResult = new SamplePearsonCorrelationCoefficient(iInputImages[cp.ch1], iInputImages[cp.ch2], iParameters.usecellmaskX, iParameters.thresholdcellmask, iParameters.usecellmaskY, iParameters.thresholdcellmasky).run(); 
            csv.Write(outFileName, getImageColoc(resAB, pearsonResult, aTitle, aCurrentFrame, cp.ch1, cp.ch2), ImageColoc.ColumnConfig, shouldAppend);
            shouldAppend = true;
            addSavedFile(FileType.ImageColocNew, outFileName);
        }
    }

    // ------------------------------------------------------------------
    
    List<ImageData> getImagesData(List<Region> regions, String aFile, int frame, int channel) {
        List<ImageData> res = new ArrayList<ImageData>();
        
        ImageData id = new ImageData(aFile,
                                     frame, 
                                     channel, 
                                     regions.size(), 
                                     round(meanSize(regions), 4),
                                     round(meanSurface(regions), 4),
                                     round(meanLength(regions), 4));
        res.add(id);
        return res;
    }
    
    private void writeImageDataCsv(String aOutputPath, String aTitle, String aOutFileName, int aCurrentFrame) {
        String outFileName = aOutputPath + Files.createTitleWithExt(FileType.ImagesDataNew, aOutFileName);
        final CSV<ImageData> csv = new CSV<ImageData>(ImageData.class);
        csv.setDelimiter(';');
        csv.setMetaInformation("background", aOutputPath + aTitle);
        
        for (int ch = 0; ch < iNumOfChannels; ch++) {
            boolean shouldAppend = !(aCurrentFrame == 0 && ch == 0);
            csv.Write(outFileName, getImagesData(iRegionsList.get(ch), aTitle, aCurrentFrame, ch), ImageData.ColumnConfig, shouldAppend);
            addSavedFile(FileType.ImagesDataNew, outFileName);
        }
    }
}
