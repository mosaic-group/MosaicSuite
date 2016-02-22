package mosaic.bregman;


import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import mosaic.bregman.ColocalizationAnalysis.ColocResult;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Outdata;
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
import mosaic.core.utils.ShellCommand;
import mosaic.utils.ArrayOps;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.ImgUtils;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;


public class BLauncher {
    private static final Logger logger = Logger.getLogger(BLauncher.class);
    
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

    // Maximum norm, it fix the range of the normalization, useful for video normalization has to be done on all frame video, 
    // filled when the plugins is called with the options min=... max=...
    public static double norm_max = 0.0;
    public static double norm_min = 0.0;
    public static Parameters iParameters = new Parameters();
    
    private ColocResult resAB;
    private ColocResult resBA;
//    private int sth_hcount = 0; // WTF this var is what for?
    private final String choice1[] = { "Automatic", "Low layer", "Medium layer", "High layer" };
    private final String choice2[] = { "Poisson", "Gauss" };
    private final Vector<String> pf = new Vector<String>();

    private int ni, nj, nz;
    private int iOutputImgScale = 1;
    
    private final static int NumOfInputChannels = 2;
    private ImagePlus[] inputImages = new ImagePlus[NumOfInputChannels];
    private double[][][][] images = new double[NumOfInputChannels][][][];
    private boolean[][][][] cellMasks = new boolean[NumOfInputChannels][][][];
    private boolean[][][] overallCellMaskBinary;
    // Create empty elements - they are later access by index - not nice but later elements are accessed by get() and they must exist.
    private ArrayList<ArrayList<Region>> regionslist = new ArrayList<ArrayList<Region>>() {
        private static final long serialVersionUID = 1L;
        { for (int i = 0; i < NumOfInputChannels; i++) add(null); }
    };
    private short[][][][] regions = new short[NumOfInputChannels][][][];
    
    private final ImagePlus[] out_soft_mask = new ImagePlus[NumOfInputChannels];
    private final ImagePlus[] out_over = new ImagePlus[NumOfInputChannels];
    private final ImagePlus[] out_disp = new ImagePlus[NumOfInputChannels];
    private final ImagePlus[] out_label = new ImagePlus[NumOfInputChannels];
    private final ImagePlus[] out_label_gray = new ImagePlus[NumOfInputChannels];
    
    public Vector<String> getProcessedFiles() {
        return pf;
    }
    
    /**
     * Launch the Segmentation
     * @param aPath path to image file or directory with image files
     */
    public BLauncher(String aPath) {
        final File inputFile = new File(aPath);
        File[] files = (inputFile.isDirectory()) ? inputFile.listFiles() : new File[] {inputFile};
        Arrays.sort(files);
        
        PrintWriter out = null;
        for (final File f : files) {
            // If it is the directory/Rscript/hidden/csv file then skip it
            if (f.isDirectory() == true || f.getName().equals("R_analysis.R") || f.getName().startsWith(".") || f.getName().endsWith(".csv")) {
                continue;
            }
            pf.add(MosaicUtils.removeExtension(f.getName()));

            ImagePlus aImp = MosaicUtils.openImg(f.getAbsolutePath());
            String outFilename= (files.length == 1) ? aImp.getTitle() : "stitch";
            out = segmentOneFile(out, aImp, outFilename);
        }
        if (out != null) {
            out.close();
        }

        // Try to run the R script
        try {
            ShellCommand.exeCmdNoPrint("Rscript " + new File(aPath).getAbsolutePath() + File.separator + "R_analysis.R");
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    public BLauncher(ImagePlus aImage) {
        PrintWriter out = segmentOneFile(null, aImage, aImage.getTitle());
        if (out != null) {
            out.close();
        }
    }

    private PrintWriter segmentOneFile(PrintWriter out, ImagePlus aImp, String outFilename) {
        for (int i = 0; i < NumOfInputChannels; i++){
            out_soft_mask[i] = null;
            out_over[i] = null;
            out_disp[i] = null;
            out_label[i] = null;
            out_label_gray[i] = null;
        }
        for (int frame = 1; frame <= aImp.getNFrames(); frame++) {
            aImp.setPosition(aImp.getChannel(), aImp.getSlice(), frame);      

            segmentAndColocalize(aImp);
            displayResult(aImp.getTitle());
            try {
                out = writeImageDataCsv(out, MosaicUtils.ValidFolderFromImage(aImp), aImp.getTitle(), outFilename, frame - 1);
            }
            catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        // Write a file info output
        if (iParameters.save_images) {
            saveAllImages(MosaicUtils.ValidFolderFromImage(aImp));
        }
        return out;
    }

    /**
     * Display results
     * @param separate true if you do not want separate the images
     */
    private void displayResult(String aTitle) {
        
        final int factor = iOutputImgScale;
        int fz = (nz > 1) ? factor : 1;

        for (int channel = 0; channel < iParameters.nchannels; ++channel) {
            if (iParameters.dispoutline) {
                final ImagePlus img = generateOutlineOverlay(regions[channel], images[channel]);
                showUpdatedImgs(channel, img, createTitle(aTitle, channel + 1, "_outline_overlay_c"), iParameters.dispoutline, out_over);
            }
            if (iParameters.dispint) {
                ImagePlus img = generateIntensitiesImg(regionslist.get(channel), nz * fz, ni * factor, nj * factor);
                showUpdatedImgs(channel, img, createTitle(aTitle, channel + 1, "_intensities_c"), iParameters.dispint, out_disp);
            }
            if (iParameters.displabels || iParameters.dispcolors) {
                ImagePlus img = generateRegionImg(regions[channel], regionslist.get(channel).size(), "");
                showUpdatedImgs(channel, img, createTitle(aTitle, channel + 1, "_seg_c"), iParameters.displabels, out_label);
            }
            if (iParameters.dispcolors) {
                final ImagePlus img = generateLabelsGray(channel);
                showUpdatedImgs(channel, img, createTitle(aTitle, channel + 1, "_mask_c"), iParameters.dispcolors, out_label_gray);
            }
            if (iParameters.dispSoftMask) {
                out_soft_mask[channel].setTitle(createTitle(aTitle, channel + 1, "_soft_mask_c"));
                out_soft_mask[channel].show();
            }
        }
    }

    /**
     * Save all images
     * @param path where to save
     */
    private void saveAllImages(String path) {
        for (int i = 0; i < iParameters.nchannels; i++) {
            if (out_over[i] != null) {
                final String savepath = path + File.separator + out_over[i].getTitle() + ".zip";
                IJ.saveAs(out_over[i], "ZIP", savepath);
            }
            if (out_disp[i] != null) {
                final String savepath = path + File.separator + out_disp[i].getTitle() + ".zip";
                IJ.saveAs(out_disp[i], "ZIP", savepath);
            }
            if (out_label[i] != null) {
                final String savepath = path + File.separator + out_label[i].getTitle() + ".zip";
                IJ.saveAs(out_label[i], "ZIP", savepath);
            }
            if (out_label_gray[i] != null) {
                final String savepath = path + File.separator + out_label_gray[i].getTitle() + ".zip";
                IJ.saveAs(out_label_gray[i], "ZIP", savepath);
            }
            if (out_soft_mask[i] != null) {
                final String savepath = path + File.separator + out_soft_mask[i].getTitle() + ".tiff";
                IJ.saveAsTiff(out_soft_mask[i], savepath);
            }
        }
    }

    /**
     * Write the CSV ImageData file information
     *
     * @param path directory where to save
     * @param filename name of the file processed
     * @param outfilename output file (extension is removed)
     * @param hcount frame output
     * @return true if success, false otherwise
     * @throws FileNotFoundException
     */
    private PrintWriter writeImageDataCsv(PrintWriter out, String path, String filename, String outfilename, int hcount) throws FileNotFoundException {
        if (out == null) {
            out = new PrintWriter(path + File.separator + MosaicUtils.removeExtension(outfilename) + "_ImagesData" + ".csv");

            // write the header
            if (iParameters.nchannels == 2) {
                out.print("File" + ";" + "Image ID" + ";" + "Objects ch1" + ";" + "Mean size in ch1" + ";" + "Mean surface in ch1" + ";" + "Mean length in ch1" + ";" + "Objects ch2" + ";"
                        + "Mean size in ch2" + ";" + "Mean surface in ch2" + ";" + "Mean length in ch2" + ";" + "Colocalization ch1 in ch2 (signal based)" + ";"
                        + "Colocalization ch2 in ch1 (signal based)" + ";" + "Colocalization ch1 in ch2 (size based)" + ";" + "Colocalization ch2 in ch1 (size based)" + ";"
                        + "Colocalization ch1 in ch2 (objects numbers)" + ";" + "Colocalization ch2 in ch1 (objects numbers)" + ";" + "Mean ch2 intensity of ch1 objects" + ";"
                        + "Mean ch1 intensity of ch2 objects" + ";" + "Pearson correlation" + ";" + "Pearson correlation inside cell masks");
            }
            else {
                out.print("File" + ";" + "Image ID" + ";" + "Objects ch1" + ";" + "Mean size in ch1" + ";" + "Mean surface in ch1" + ";" + "Mean length in ch1");
            }
            out.println();
            out.flush();
            
            out.print("%Parameters:" + " " + "background removal " + " " + iParameters.removebackground + " " + "window size " + iParameters.size_rollingball + " " + "stddev PSF xy " + " "
                    + round(iParameters.sigma_gaussian, 5) + " " + "stddev PSF z " + " " + round(iParameters.sigma_gaussian / iParameters.zcorrec, 5) + " "
                    + "Regularization " + iParameters.lreg_[0] + " " + iParameters.lreg_[1] + " " + "Min intensity ch1 " + iParameters.min_intensity + " " + "Min intensity ch2 "
                    + iParameters.min_intensityY + " " + "subpixel " + iParameters.subpixel + " " + "Cell mask ch1 " + iParameters.usecellmaskX + " " + "mask threshold ch1 "
                    + iParameters.thresholdcellmask + " " + "Cell mask ch2 " + iParameters.usecellmaskY + " " + "mask threshold ch2 " + iParameters.thresholdcellmasky + " " + "Intensity estimation "
                    + choice1[iParameters.mode_intensity] + " " + "Noise model " + choice2[iParameters.noise_model] + ";");
            out.println();
            out.flush();
        }

        final double meanSA = meansurface(regionslist.get(0));
        final double meanLA = meanLength(regionslist.get(0));
        if (iParameters.nchannels == 2) {
            // ================= Colocalization analysis ===============================================
            double colocAB = round(resAB.colocsegABsignal, 4);
            double colocABnumber = round(resAB.colocsegABnumber, 4);
            double colocABsize = round(resAB.colocsegABsize, 4);
            double colocA = round(resAB.colocsegA, 4);
            double colocBA = round(resBA.colocsegABsignal, 4);
            double colocBAnumber = round(resBA.colocsegABnumber, 4);
            double colocBAsize = round(resBA.colocsegABsize, 4);
            double colocB = round(resBA.colocsegA, 4);

            double[] temp = new SamplePearsonCorrelationCoefficient(inputImages[0], inputImages[1], iParameters.usecellmaskX, iParameters.thresholdcellmask, iParameters.usecellmaskY, iParameters.thresholdcellmasky).run();
            double corr = temp[0];
            double corr_mask = temp[1];
            
            final double meanSB = meansurface(regionslist.get(1));
            final double meanLB = meanLength(regionslist.get(1));

            out.print(filename + ";" + hcount + ";" + regionslist.get(0).size() + ";" + round(meanSize(regionslist.get(0)), 4) + ";" + round(meanSA, 4) + ";"
                    + round(meanLA, 4) + ";" + +regionslist.get(1).size() + ";" + round(meanSize(regionslist.get(1)), 4) + ";" + round(meanSB, 4) + ";"
                    + round(meanLB, 4) + ";" + colocAB + ";" + colocBA + ";" + colocABsize + ";" + colocBAsize + ";" + colocABnumber + ";" + colocBAnumber + ";" + colocA + ";"
                    + colocB + ";" + round(corr, 4) + ";" + round(corr_mask, 4));
        }
        else {
            out.print(filename + ";" + hcount + ";" + regionslist.get(0).size() + ";" + round(meanSize(regionslist.get(0)), 4) + ";" + round(meanSA, 4) + ";" + round(meanLA, 4));
        }
        out.println();
        out.flush();
        
        return out;
    }

    private void segmentAndColocalize(ImagePlus aImage) {
        if (aImage == null) {
            IJ.error("No image to process");
            return;
        }
        if (aImage.getType() == ImagePlus.COLOR_RGB) {
            IJ.error("This is a color image and is not supported, convert into 8-bit , 16-bit or float");
            return;
        }

        // Image info
        ni = aImage.getWidth();
        nj = aImage.getHeight();
        nz = aImage.getNSlices();
        final int currentFrame = aImage.getFrame();
        iParameters.nchannels = aImage.getNChannels();
        final String title = aImage.getTitle();

        // Segmentation
        for (int channel = 0; channel < iParameters.nchannels; channel++) {
            inputImages[channel] =  ImgUtils.extractImage(aImage, currentFrame, channel + 1 /* 1-based */);
            images[channel] = ImgUtils.ImgToZXYarray(inputImages[channel]);
            ArrayOps.normalize(images[channel]);
            logger.debug("------------------- Segmentation of [" + title + "] channel: " + channel + ", frame: " + currentFrame);
            segment(channel, currentFrame);
            logger.debug("------------------- End of Segmentation ---------------------------");
        }
        // Postprocessing 
        String savepath = MosaicUtils.ValidFolderFromImage(aImage);
        final String filename_without_ext = MosaicUtils.removeExtension(title); 
        
        // Choose the Rscript coloc format
        if (iParameters.nchannels == 2) CSVOutput.occ = CSVOutput.oc[2];
        final CSV<? extends Outdata<Region>> IpCSV = CSVOutput.getCSV();
        
        if (iParameters.nchannels == 1) {
            if (iParameters.save_images) {
                final Vector<? extends Outdata<Region>> obl = CSVOutput.getObjectsList(regionslist.get(0), currentFrame - 1);
                IpCSV.setMetaInformation("background", savepath + File.separator + title);
                CSVOutput.occ.converter.Write(IpCSV, savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv", obl, CSVOutput.occ.outputChoose, (currentFrame - 1 != 0));
            }
        }
        if (iParameters.nchannels == 2) {
            computeOverallMask(nz, ni, nj);
            applyMask();
            
            final int factor2 = iOutputImgScale;
            int fz2 = (nz > 1) ? factor2 : 1;

            ImagePlus colocImg = generateColocImg(regionslist.get(0), regionslist.get(1), ni * factor2, nj * factor2, nz * fz2);
            colocImg.show();
            
            if (iParameters.save_images) {
                IJ.saveAs(colocImg, "ZIP", MosaicUtils.removeExtension(MosaicUtils.ValidFolderFromImage(aImage) + title) + "_coloc.zip");
                
                // ================= Colocalization analysis ===============================================
                // TODO: It must be currently done here since it updates data for all regions 
                //       which are saved below to CSV files. It must be refactored...
                ColocalizationAnalysis ca = new ColocalizationAnalysis(nz, ni, nj, (nz > 1) ? factor2 : 1, factor2, factor2);
                ca.addRegion(regionslist.get(0), images[0]);
                ca.addRegion(regionslist.get(1), images[1]);
                resAB = ca.calculate(0, 1);
                resBA = ca.calculate(1, 0);
                regions[0] = ca.getLabeledRegion(0);
                regions[1] = ca.getLabeledRegion(1);
                
                // =================================

                // Write channel 1
                Vector<? extends Outdata<Region>> obl = CSVOutput.getObjectsList(regionslist.get(0), currentFrame - 1);
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + title);
                final String output1 = savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv";
                boolean append = (new File(output1).exists()) ? true : false;
                CSVOutput.occ.converter.Write(IpCSV, output1, obl, CSVOutput.occ.outputChoose, append);
                
                // Write channel 2
                obl = CSVOutput.getObjectsList(regionslist.get(1), currentFrame - 1);
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + title);
                final String output2 = savepath + File.separator + filename_without_ext + "_ObjectsData_c2" + ".csv";
                CSVOutput.occ.converter.Write(IpCSV, output2, obl, CSVOutput.occ.outputChoose, append);
            }
        }
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

    private ImagePlus generateLabelsGray(int channel) {
        final ImagePlus img = out_label[channel].duplicate();
        IJ.run(img, "Grays", "");
        return img;
    }

    private String createTitle(String aTitle, int channel, final String outName) {
        return aTitle.substring(0, aTitle.length() - 4) + outName + (channel - 1 + 1);
    }
    
    private void showUpdatedImgs(int channel, final ImagePlus img, final String title, final boolean show, final ImagePlus[] imgs) {
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
        final ImagePlus img = inputImages[channel];
        /* Search for maximum and minimum value, normalization */
        double minNorm = norm_min;
        double maxNorm = norm_max;
        if (norm_max == 0) {
            MinMax<Double> mm = ImgUtils.findMinMax(img);
            minNorm = mm.getMin();
            maxNorm = mm.getMax();
        }
        if (iParameters.usecellmaskX && channel == 0) {
            generateMask(channel, img, minNorm, maxNorm, iParameters.thresholdcellmask);
        }
        if (iParameters.usecellmaskY && channel == 1) {
            generateMask(channel, img, minNorm, maxNorm, iParameters.thresholdcellmasky);
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
        double max = mm.getMax();
        double min = mm.getMin();
        if (iParameters.livedisplay && iParameters.removebackground) {
            final ImagePlus noBackgroundImg = img.duplicate();
            noBackgroundImg.setTitle("Background reduction channel " + (channel + 1));
            noBackgroundImg.changes = false;
            noBackgroundImg.setDisplayRange(min, max);
            noBackgroundImg.show();
        }
        /* Overload min/max after background subtraction */
        if (norm_max != 0) {
            max = norm_max;
            // if we are removing the background we have no idea which is the minumum across 
            // all the movie so let be conservative and put min = 0.0 for sure cannot be < 0
            min = (iParameters.removebackground) ? 0.0 : norm_min;
        }
        
        double minIntensity = (channel == 0) ? iParameters.min_intensity : iParameters.min_intensityY;

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

        iOutputImgScale = rg.iLabeledRegions[0].length / ni;
        regionslist.set(channel, rg.iRegionsList);
        regions[channel] = rg.iLabeledRegions;
        logger.debug("------------------- Found " + rg.iRegionsList.size() + " object(s) in channel " + channel);
        // =============================
        if (iParameters.dispSoftMask) {
            out_soft_mask[channel] = ImgUtils.ZXYarrayToImg(rg.iSoftMask, "Mask" + ((channel == 0) ? "X" : "Y"));
        }
        ImagePlus maskImg = generateMaskImg(rg.iAllMasks); 
        if (maskImg != null) {maskImg.setTitle("Mask Evol");maskImg.show();}
    }

    private void generateMask(int channel, final ImagePlus img, double min, double max, double maskThreshold) {
        ImagePlus maskImg = new ImagePlus();
        maskImg.setTitle("Cell mask channel " + (channel + 1));
        cellMasks[channel] = createBinaryCellMask(maskThreshold * (max - min) + min, img, channel, maskImg);
        if (iParameters.livedisplay) {
            maskImg.show();
        }
    }
    
    // ============================================ MASKS and tools ==============================
    
    private void computeOverallMask(int nz, int ni, int nj) {
        final boolean mask[][][] = new boolean[nz][ni][nj];

        for (int z = 0; z < nz; z++) {
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    if (iParameters.usecellmaskX && iParameters.usecellmaskY) {
                        mask[z][i][j] = cellMasks[0][z][i][j] && cellMasks[1][z][i][j];
                    }
                    else if (iParameters.usecellmaskX) {
                        mask[z][i][j] = cellMasks[0][z][i][j];
                    }
                    else if (iParameters.usecellmaskY) {
                        mask[z][i][j] = cellMasks[1][z][i][j];
                    }
                    else {
                        mask[z][i][j] = true;
                    }

                }
            }
        }
        overallCellMaskBinary = mask;
    }
    
    private void applyMask() {
        for (int channel = 0; channel < iParameters.nchannels; channel++) {
            final ArrayList<Region> maskedRegion = new ArrayList<Region>();
            for (Region r : regionslist.get(channel)) {
                if (isInside(r)) {
                    maskedRegion.add(r);
                }
            }
            regionslist.set(channel, maskedRegion);
        }
    }
    
    private boolean isInside(Region r) {
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

    private double meansurface(ArrayList<Region> aRegionsList) {
        double totalPerimeter = 0;
        for (Region r : aRegionsList) {
            totalPerimeter += r.perimeter;
        }

        return (totalPerimeter / aRegionsList.size());
    }

    private double meanSize(ArrayList<Region> aRegionsList) {
        double totalSize = 0;
        for (Region r : aRegionsList) {
            totalSize += r.iPixels.size();
        }

        return (totalSize / aRegionsList.size()) / (Math.pow(iOutputImgScale, 2));
    }
    
    private double meanLength(ArrayList<Region> aRegionsList) {
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
                }
            }
            final ByteProcessor bp = new ByteProcessor(ni, nj);
            bp.setPixels(mask);
            maskStack.addSlice("", bp);
        }
        final ImagePlus maskImg = (aOutputImage == null) ? new ImagePlus("Cell mask channel " + (aChannel + 1)) : aOutputImage;
        maskImg.setStack(maskStack);
        IJ.run(maskImg, "Invert", "stack");
        
        // "Fill Holes" is using Prefs.blackBackground global setting. We need false here.
        boolean tempBlackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        IJ.run(maskImg, "Fill Holes", "stack");
        ij.Prefs.blackBackground = tempBlackbackground;
        
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
    
    // ==================================== Image generation ==========================
    
    /**
     * @return Generated image with regions with of given size.
     */
    private ImagePlus generateColocImg(ArrayList<Region> aRegionsListA, ArrayList<Region> aRegionsListB, int iWidth, int iHeigth, int iDepth) {
        final int[][] imagecolor = new int[iDepth][iWidth * iHeigth];
        setRgb(imagecolor, aRegionsListA, /* green */ 1, iWidth);
        setRgb(imagecolor, aRegionsListB, /* red */ 2, iWidth);

        // Merge them into one Color image
        ImageStack is = new ImageStack(iWidth, iHeigth);
        for (int z = 0; z < iDepth; z++) {
            final ColorProcessor colorProc = new ColorProcessor(iWidth, iHeigth, imagecolor[z]);
            is.addSlice(colorProc);
        }
        
        return new ImagePlus("Colocalization", is);
    }
    
    /**
     * Draws regions with provided color
     * @param pixels array of [z][x*y] size
     * @param aRegions regions to draw
     * @param aColor color R=2, G=1, B=0
     * @param aWidth width of [x*y]
     */
    private void setRgb(int[][] pixels, ArrayList<Region> aRegions, int aColor, int aWidth) {
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
        // TODO: This generates images with set color space but.. mostly it is white on screen. After converting to
        //       RGB it looks nice. Should be somehow fixed.
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
    private ImagePlus generateIntensitiesImg(ArrayList<Region> aRegions, int aDepth, int aWidth, int aHeight) {
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
}
