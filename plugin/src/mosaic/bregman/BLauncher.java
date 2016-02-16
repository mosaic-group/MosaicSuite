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
import mosaic.utils.Debug;
import mosaic.utils.ImgUtils;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.ShortType;


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
    private int sth_hcount = 0; // WTF this var is what for?
//    private ImagePlus aImp;
    private final String choice1[] = { "Automatic", "Low layer", "Medium layer", "High layer" };
    private final String choice2[] = { "Poisson", "Gauss" };
    private final Vector<String> pf = new Vector<String>();

    private int ni, nj, nz;
    
    private int currentFrame;
    private String currentImageTitle = "currentImage";
    private int iOutputImgScale = 1;
    
    private final static int NumOfInputImages = 2;
    private ImagePlus[] inputImages = new ImagePlus[NumOfInputImages];
    private double[][][][] images = new double[NumOfInputImages][][][];
    private boolean[][][][] cellMasks = new boolean[NumOfInputImages][][][];
    private boolean[][][] overallCellMaskBinary;
    // Create empty elements - they are later access by index - not nice but later elements are accessed by get() and they must exist.
    private ArrayList<ArrayList<Region>> regionslist = new ArrayList<ArrayList<Region>>() {
        private static final long serialVersionUID = 1L;
        { for (int i = 0; i < NumOfInputImages; i++) add(null); }
    };
    private short[][][][] regions = new short[NumOfInputImages][][][];
    private final ImagePlus out_soft_mask[] = new ImagePlus[NumOfInputImages];
    
    private final ImagePlus out_over[] = new ImagePlus[NumOfInputImages];
    private final ImagePlus out_disp[] = new ImagePlus[NumOfInputImages];
    private final ImagePlus out_label[] = new ImagePlus[NumOfInputImages];
    private final ImagePlus out_label_gray[] = new ImagePlus[NumOfInputImages];
    private ImagePlus label = null;
    
    public Vector<String> getProcessedFiles() {
        return pf;
    }
    
    /**
     * Launch the Segmentation
     * @param aPath path to image file or directory with image files
     */
    public BLauncher(String aPath) {
        final boolean processdirectory = (new File(aPath)).isDirectory();
        if (processdirectory) {
            // Get all files
            final File files[] = new File(aPath).listFiles();
            Arrays.sort(files);

            PrintWriter out = null;
            for (final File f : files) {
                // If it is the directory/Rscript/hidden/csv file then skip it
                if (f.isDirectory() == true || f.getName().equals("R_analysis.R") || f.getName().startsWith(".") || f.getName().endsWith(".csv")) {
                    continue;
                }

                // Attempt to open a file
                ImagePlus aImp = MosaicUtils.openImg(f.getAbsolutePath());
                pf.add(MosaicUtils.removeExtension(f.getName()));
                
                Headless_file(aImp);

                displayResult(true);
                // Write a file info output
                if (iParameters.save_images) {
                    saveAllImages(MosaicUtils.ValidFolderFromImage(aImp));

                    String outFilename= "stitch";
                    if (files.length == 1) {
                        outFilename = aImp.getTitle();
                    }

                    try {
                        out = writeImageDataCsv(out, MosaicUtils.ValidFolderFromImage(aImp), aImp.getTitle(), outFilename, sth_hcount - 1);
                    }
                    catch (final FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
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
        else {
            ImagePlus aImp = MosaicUtils.openImg(aPath);
            start(aImp);
        }
    }

    public BLauncher(ImagePlus aImp_) {
        start(aImp_);
    }

    private void start(ImagePlus aImp_) {
        ImagePlus aImp = aImp_;
        PrintWriter out = null;

        // Check if we have more than one frame
        for (int f = 1; f <= aImp.getNFrames(); f++) {
            currentFrame = f;
            aImp.setPosition(aImp.getChannel(), aImp.getSlice(), f);
            Headless_file(aImp);

            try {
                out = writeImageDataCsv(out, MosaicUtils.ValidFolderFromImage(aImp), aImp.getTitle(), aImp.getTitle(), f - 1);
            }
            catch (final FileNotFoundException e) {
                e.printStackTrace();
            }

            // Display results
            displayResult(false);
        }

        // Write a file info output
        if (iParameters.save_images) {
            saveAllImages(MosaicUtils.ValidFolderFromImage(aImp));
        }

        if (out != null) {
            out.close();
        }
    }

    /**
     * Display results
     * @param separate true if you do not want separate the images
     */
    private void displayResult(boolean sep) {
        
        final int factor = iOutputImgScale;
        int fz = (nz > 1) ? factor : 1;

        if (iParameters.dispoutline) {
                displayoutline(regions[0], images[0], nz * fz, ni * factor, nj * factor, 1, sep);
            if (iParameters.nchannels == 2) {
                displayoutline(regions[1], images[1], nz * fz, ni * factor, nj * factor, 2, sep);
            }
        }
        if (iParameters.dispint) {
            displayintensities(regionslist.get(0), nz * fz, ni * factor, nj * factor, 1, sep);
            if (iParameters.nchannels == 2) {
                displayintensities(regionslist.get(1), nz * fz, ni * factor, nj * factor, 2, sep);
            }
        }
        if (iParameters.displabels || iParameters.dispcolors) {
            displayRegionsCol(regions[0], 1, regionslist.get(0).size(), sep);
            if (iParameters.nchannels == 2) {
                displayRegionsCol(regions[0], 2, regionslist.get(0).size(), sep);
            }
        }
        if (iParameters.dispcolors) {
            displayRegionsLab(1, sep);
            if (iParameters.nchannels == 2) {
                displayRegionsLab(2, sep);
            }
        }
        if (iParameters.dispSoftMask) {
            out_soft_mask[0].setTitle(getSoftMask(0));
            out_soft_mask[0].show();
            if (iParameters.nchannels == 2) {
                out_soft_mask[1].setTitle(getSoftMask(1));
                out_soft_mask[1].show();
            }
        }
    }

    /**
     * Get the outline filename
     *
     * @return the outline name
     */
    private String getOutlineName(int i) {
        return currentImageTitle.substring(0, currentImageTitle.length() - 4) + "_outline_overlay_c" + (i + 1);
    }

    /**
     * Get the outline filename
     *
     * @return the outline filename
     */
    private String getIntensitiesName(int i) {
        return currentImageTitle.substring(0, currentImageTitle.length() - 4) + "_intensities" + "_c" + (i + 1);
    }

    /**
     * Get the Intensities filename
     *
     * @return the intensities filename
     */
    private String getSegmentationName(int i) {
        return currentImageTitle.substring(0, currentImageTitle.length() - 4) + "_seg_c" + (i + 1);
    }

    /**
     * Get the Mask filename
     *
     * @param i
     * @return the mask filename
     */
    private String getMaskName(int i) {
        return currentImageTitle.substring(0, currentImageTitle.length() - 4) + "_mask_c" + (i + 1);
    }

    private String getSoftMask(int i) {
        return currentImageTitle.substring(0, currentImageTitle.length() - 4) + "_soft_mask_c" + (i + 1);
    }

    /**
     * Save all images
     * @param path where to save
     */
    private void saveAllImages(String path) {
        // Save images
        for (int i = 0; i < out_over.length; i++) {
            final String savepath = path + File.separator + getOutlineName(i) + ".zip";
            if (out_over[i] != null) {
                IJ.saveAs(out_over[i], "ZIP", savepath);
            }
        }

        for (int i = 0; i < out_disp.length; i++) {
            final String savepath = path + File.separator + getIntensitiesName(i) + ".zip";
            if (out_disp[i] != null) {
                IJ.saveAs(out_disp[i], "ZIP", savepath);
            }
        }

        for (int i = 0; i < out_label.length; i++) {
            final String savepath = path + File.separator + getSegmentationName(i) + ".zip";
            if (out_label[i] != null) {
                IJ.saveAs(out_label[i], "ZIP", savepath);
            }
        }

        for (int i = 0; i < out_label_gray.length; i++) {
            final String savepath = path + File.separator + getMaskName(i) + ".zip";
            if (out_label_gray[i] != null) {
                IJ.saveAs(out_label_gray[i], "ZIP", savepath);
            }
        }
        for (int i = 0; i < out_soft_mask.length; i++) {
            
            // TODO: Added temporarily to since soft mask for channel 2 is not existing ;
            if (i > 0) break;
            
            final String savepath = path + File.separator + getSoftMask(i) + ".tiff";
            if (out_soft_mask[i] != null) {
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
            // Remove extension from filename
            final String ffo = MosaicUtils.removeExtension(outfilename);

            out = new PrintWriter(path + File.separator + ffo + "_ImagesData" + ".csv");

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
            out.print(filename + ";" + hcount + ";" + regionslist.get(0).size() + ";" + round(meanSize(regionslist.get(0)), 4) + ";" + round(meanSA, 4) + ";"
                    + round(meanLA, 4));
        }
        out.println();
        out.flush();
        
        return out;
    }

    private void Headless_file(ImagePlus aImp) {
        try {
            if (aImp == null) {
                IJ.error("No image to process");
                return;
            }
            if (aImp.getType() == ImagePlus.COLOR_RGB) {
                IJ.error("This is a color image and is not supported, convert into 8-bit , 16-bit or float");
                return;
            }

            iParameters.nchannels = aImp.getNChannels();
            bcolocheadless(aImp);
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private Vector<? extends Outdata<Region>> getObjectsList(int f, int channel) {
        final Vector<? extends Outdata<Region>> v = CSVOutput.getVector(regionslist.get(channel));

        // Set frame
        for (int i = 0; i < v.size(); i++) {
            v.get(i).setFrame(f);
        }

        // Convert it back to original type.
        return v;
    }
    
    /*
     * It segment the image and give co-localization analysis result
     * for a 2 channels image
     * @param img2 Image to segment and analyse
     */
    private void bcolocheadless(ImagePlus img2) {
        boolean tempBlackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        iParameters.nchannels = img2.getNChannels();

        loadChannels(img2, iParameters.nchannels);
        
        ni = inputImages[0].getWidth();
        nj = inputImages[0].getHeight();
        nz = inputImages[0].getNSlices();

        segment(0);
        if (iParameters.nchannels == 2) {
            segment(1);
        }

        String savepath = MosaicUtils.ValidFolderFromImage(img2);
        final String filename_without_ext = img2.getTitle().substring(0, img2.getTitle().lastIndexOf("."));
        
        // Choose the Rscript coloc format
        if (iParameters.nchannels == 2) CSVOutput.occ = CSVOutput.oc[2];
        
        final CSV<? extends Outdata<Region>> IpCSV = CSVOutput.getCSV();
        
        if (iParameters.nchannels == 1) {
            if (iParameters.save_images) {
                final Vector<? extends Outdata<Region>> obl = getObjectsList(sth_hcount, 0);
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv", obl, CSVOutput.occ.outputChoose, (sth_hcount != 0));
            }
            
            sth_hcount++;
        }
        if (iParameters.nchannels == 2) {
            computeOverallMask(nz, ni, nj);
            regionslist.set(0, removeExternalObjects(regionslist.get(0)));
            regionslist.set(1, removeExternalObjects(regionslist.get(1)));
            
            // ========= new analysis
            
            mosaic.utils.Debug.print("SIZES: ", Debug.getArrayDims(regions[0]), nz, ni, nj);
            final int factor2 = iOutputImgScale;
            int fz2 = (nz > 1) ? factor2 : 1;

            ImagePlus colocImg = generateColocImg(regionslist.get(0), regionslist.get(1), ni * factor2, nj * factor2, nz * fz2);
            colocImg.show();
            
            if (iParameters.save_images) {
                // Handle colocation image
                IJ.run(colocImg, "RGB Color", "");
                IJ.saveAs(colocImg, "ZIP", MosaicUtils.removeExtension(MosaicUtils.ValidFolderFromImage(img2) + img2.getTitle()) + "_coloc.zip");
                
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
                final String output1 = new String(savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv");
                final String output2 = new String(savepath + File.separator + filename_without_ext + "_ObjectsData_c2" + ".csv");
                boolean append = (new File(output1).exists()) ? true : false;

                // Write channel 1
                Vector<? extends Outdata<Region>> obl = getObjectsList(sth_hcount, 0);
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, output1, obl, CSVOutput.occ.outputChoose, append);
                
                // Write channel 2
                obl = getObjectsList(sth_hcount, 1);
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, output2, obl, CSVOutput.occ.outputChoose, append);
            }

            sth_hcount++;
        }

        ij.Prefs.blackBackground = tempBlackbackground;
    }

    private ImagePlus generateColocImg(ArrayList<Region> aRegionsListA, ArrayList<Region> aRegionsListB, int iWidth, int iHeigth, int iDepth) {
        final byte[] imagecolor = new byte[iDepth * iWidth * iHeigth * 3];

        // set green pixels
        for (final Region r : aRegionsListA) {
            for (final Pix p : r.iPixels) {
                final int t = p.pz * iWidth * iHeigth * 3 + p.px * iHeigth * 3;
                imagecolor[t + p.py * 3 + 1] = (byte) 255;
            }
        }

        // set red pixels
        for (final Region r : aRegionsListB) {
            for (final Pix p : r.iPixels) {
                final int t = p.pz * iWidth * iHeigth * 3 + p.px * iHeigth * 3;
                imagecolor[t + p.py * 3 + 0] = (byte) 255;
            }
        }

        // Merge them into one Color image
        final int[] tabt = new int[3];
        ImageStack imgcolocastack = new ImageStack(iWidth, iHeigth);
        for (int z = 0; z < iDepth; z++) {
            final ColorProcessor colorProc = new ColorProcessor(iWidth, iHeigth);
            for (int i = 0; i < iWidth; i++) {
                final int t = z * iWidth * iHeigth * 3 + i * iHeigth * 3;
                for (int j = 0; j < iHeigth; j++) {
                    tabt[0] = imagecolor[t + j * 3 + 0] & 0xFF;
                    tabt[1] = imagecolor[t + j * 3 + 1] & 0xFF;
                    tabt[2] = imagecolor[t + j * 3 + 2] & 0xFF;
                    colorProc.putPixel(i, j, tabt);
                }
            }
            imgcolocastack.addSlice("Colocalization", colorProc);
        }
        ImagePlus iColocImg = new ImagePlus("Colocalization", imgcolocastack);
        
        return iColocImg;
    }

    /**
     * Display outline overlay segmentation
     *
     * @param regions mask with intensities
     * @param image image
     * @param dz z size
     * @param di x size
     * @param dj y size
     * @param channel
     * @param sep true = doea not fuse with the separate outline
     */
    private void displayoutline(short[][][] regions, double[][][] image, int dz, int di, int dj, int channel, boolean sep) {
        mosaic.utils.Debug.print("displayoutline", channel, dz, di, dj);
        // build stack and imageplus for objects
        ImageStack objS = new ImageStack(di, dj);
        for (int z = 0; z < dz; z++) {
            final byte[] mask_bytes = new byte[di * dj];
            for (int i = 0; i < di; i++) {
                for (int j = 0; j < dj; j++) {
                    if (regions[z][i][j] == 0) {
                        mask_bytes[j * di + i] = (byte) 255;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(di, dj, mask_bytes);
            objS.addSlice("", bp);
        }
        final ImagePlus objcts = new ImagePlus("Objects", objS);

        ImageStack imgS = new ImageStack(ni, nj);
        for (int z = 0; z < nz; z++) {
            final byte[] mask_bytes = new byte[ni * nj];
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    mask_bytes[j * ni + i] = (byte) (255 * image[z][i][j]);
                }
            }

            final ByteProcessor bp = new ByteProcessor(ni, nj, mask_bytes);
            imgS.addSlice("", bp);
        }
        ImagePlus img = new ImagePlus("Image", imgS);

        img = new Resizer().zScale(img, dz, ImageProcessor.NONE);
        final ImageStack imgS2 = new ImageStack(di, dj);
        for (int z = 0; z < dz; z++) {
            img.setSliceWithoutUpdate(z + 1);
            img.getProcessor().setInterpolationMethod(ImageProcessor.NONE);
            imgS2.addSlice("", img.getProcessor().resize(di, dj, false));
        }
        img.setStack(imgS2);

        for (int z = 1; z <= dz; z++) {
            final BinaryProcessor bip = new BinaryProcessor((ByteProcessor) objcts.getStack().getProcessor(z));
            bip.outline();
            bip.invert();
        }

        final ImagePlus tab[] = new ImagePlus[] {objcts, img};
        final ImagePlus over = RGBStackMerge.mergeChannels(tab, false);

        // if we have already an outline overlay image merge the frame
        if (sep == false) {
            updateImages(out_over, over, getOutlineName(channel - 1), iParameters.dispoutline, channel);
        }
        else {
            out_over[channel - 1] = over;
            over.show();
            over.setTitle(getOutlineName(channel - 1));
        }
    }

    /**
     * Display intensity result
     *
     * @param regionslist Regions
     * @param dz image size z
     * @param di image size x
     * @param dj image size y
     * @param channel
     * @param imagecolor
     * @param sep = true if you want to separate
     */
    private void displayintensities(ArrayList<Region> regionslist, int dz, int di, int dj, int channel, boolean sep) {
        // build stack and imageplus
        final ImgFactory<ShortType> imgFactory = new ArrayImgFactory<ShortType>();

        // create an 3d-Img
        final Img<ShortType> imgInt = imgFactory.create(new long[] { di, dj, dz }, new ShortType());
        final RandomAccess<ShortType> ra = imgInt.randomAccess();

        final int pos[] = new int[3];

        // for each region draw the region with specified intensity
        for (final Region r : regionslist) {
            // for each pixel in the region
            for (final Pix p : r.iPixels) {
                pos[0] = p.px;
                pos[1] = p.py;
                pos[2] = p.pz;
                ra.setPosition(pos);
                ra.get().set((short) r.intensity);
            }
        }

        final ImagePlus intensities = ImageJFunctions.wrap(imgInt, "Intensities");

        // copy to imageJ1
        final ImagePlus intensities2 = intensities.duplicate();

        if (sep == false) {
            updateImages(out_disp, intensities2, getIntensitiesName(channel - 1), iParameters.dispint, channel);
        }
        else {
            out_disp[channel - 1] = intensities2;
            intensities2.show();
            intensities2.setTitle(getIntensitiesName(channel - 1));
        }
    }

    private IndexColorModel backgroundAndSpectrum(int maximum) {
        if (maximum > 255) {
            maximum = 255;
        }
        final byte[] reds = new byte[256];
        final byte[] greens = new byte[256];
        final byte[] blues = new byte[256];
        // Set all to white:
        for (int i = 0; i < 256; ++i) {
            reds[i] = greens[i] = blues[i] = (byte) 255;
        }
        // Set 0 to black:
        reds[0] = greens[0] = blues[0] = 0;
        final float divisions = maximum;
        Color c;
        for (int i = 1; i <= maximum; ++i) {
            final float h = (i - 1) / divisions;
            c = Color.getHSBColor(h, 1f, 1f);
            reds[i] = (byte) c.getRed();
            greens[i] = (byte) c.getGreen();
            blues[i] = (byte) c.getBlue();
        }
        return new IndexColorModel(8, 256, reds, greens, blues);
    }

    /**
     * Display regions colors
     *
     * @param regions label image
     * @param channel number of the channel
     * @param max_r max number of region
     * @param sep = true to separate
     */
    private void displayRegionsCol(short[][][] regions, int channel, int max_r, boolean sep) {
        final int width = regions[0].length;
        final int height = regions[0][0].length;
        final int depth = regions.length;

        // build stack and imageplus
        ImageStack labS = new ImageStack(width, height);
        final int min = 0;
        final int max = Math.max(max_r, 255);
        for (int z = 0; z < depth; z++) {
            final short[] mask_short = new short[width * height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    mask_short[j * width + i] = regions[z][i][j];
                }
            }
            final ShortProcessor sp = new ShortProcessor(width, height, mask_short, null);
            sp.setMinAndMax(min, max);
            labS.addSlice("", sp);
        }

        labS.setColorModel(backgroundAndSpectrum(Math.min(max_r, 255)));
        final String chan_s[] = { "X", "Y" };
        label = new ImagePlus("Regions " + chan_s[channel - 1], labS);

        if (sep == false) {
            updateImages(out_label, label, getSegmentationName(channel - 1), iParameters.dispcolors, channel);
        }
        else {
            out_label[channel - 1] = label;
            label.show();
            label.setTitle(getSegmentationName(channel - 1));
        }
    }

    /**
     * Display regions labels
     *
     * @param channel
     * @param sep = true if you want to separate
     */
    private void displayRegionsLab(int channel, boolean sep) {
        final ImagePlus label_ = label.duplicate();

        IJ.run(label_, "Grays", "");

        if (sep == false) {
            updateImages(out_label_gray, label_, getMaskName(channel - 1), iParameters.displabels, channel);
        }
        else {
            out_label_gray[channel - 1] = label_;
            label_.show();
            label_.setTitle(getMaskName(channel - 1));
        }
    }

    /**
     * Update the images array, merging frames and display it
     *
     * @param ipd array of images
     * @param ips image
     * @param title of the image
     * @param channel Channel id array (id-1)
     */
    private void updateImages(ImagePlus ipd[], ImagePlus ips, String title, boolean disp, int channel) {
        if (ipd[channel - 1] != null) {
            MosaicUtils.MergeFrames(ipd[channel - 1], ips);
        }
        else {
            ipd[channel - 1] = ips;
            ipd[channel - 1].setTitle(title);
        }

        if (disp) {
            // this force the update of the image
            ipd[channel - 1].setStack(ipd[channel - 1].getStack());
            ipd[channel - 1].show();
        }
    }
    
    // Round y to z-places after comma
    private double round(double y, final int z) {
        final double factor = Math.pow(10,  z);
        y *= factor;
        y = (int) y;
        y /= factor;
        return y;
    }
    
    private void loadChannels(ImagePlus img2, int aNumOfChannels) {
        final int currentFrame = img2.getFrame();
        setupChannel(img2, currentFrame, 0);
        if (aNumOfChannels > 1) setupChannel(img2, currentFrame, 1);
    }

    private void setupChannel(ImagePlus img2, final int currentFrame, int channel) {
        inputImages[channel] =  ImgUtils.extractImage(img2, currentFrame, channel + 1 /* 1-based */);
        images[channel] = ImgUtils.ImgToZXYarray(inputImages[channel]);
        ArrayOps.normalize(images[channel]);
    }
    

    private void segment(int channel) {
        final ImagePlus img = inputImages[channel];
        currentImageTitle = img.getTitle();
                /* Search for maximum and minimum value, normalization */
                double min, max;
                if (norm_max == 0) {
                    MinMax<Double> mm = ImgUtils.findMinMax(img);
                    min = mm.getMin();
                    max = mm.getMax();
                }
                else {
                    min = norm_min;
                    max = norm_max;
                }
                if (iParameters.usecellmaskX && channel == 0) {
                    ImagePlus maskImg = new ImagePlus();
                    maskImg.setTitle("Cell mask channel 1");
                    cellMasks[0] = createBinaryCellMask(iParameters.thresholdcellmask * (max - min) + min, img, channel, maskImg);
                    if (iParameters.livedisplay) {
                        maskImg.show();
                    }
                }
                if (iParameters.usecellmaskY && channel == 1) {
                    ImagePlus maskImg = new ImagePlus();
                    maskImg.setTitle("Cell mask channel 2");
                    cellMasks[1] = createBinaryCellMask(iParameters.thresholdcellmasky * (max - min) + min, img, channel, maskImg);
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
                logger.debug("------------------- Segmentation of [" + currentImageTitle + "] channel: " + channel + ", frame: " + currentFrame);
                SquasshSegmentation rg = new SquasshSegmentation(image, sp, min, max);
                if (iParameters.patches_from_file == null) {
                    rg.run();
                }
                else {
                    rg.runWithProvidedMask(generateMaskFromPatches(nz, ni, nj));
                }
                
                iOutputImgScale = rg.iLabeledRegions[0].length / ni;
                regionslist.set(channel, rg.iRegionsList);
                int num = 0;
                for (Region r  : rg.iRegionsList) {
                    System.out.println("=======> " + (num++) + " label: " + r.iLabel);
                }
                regions[channel] = rg.iLabeledRegions;
                logger.debug("------------------- Found " + rg.iRegionsList.size() + " object(s) in channel " + channel);
                // =============================
                IJ.log(rg.iRegionsList.size() + " objects found in " + ((channel == 0) ? "X" : "Y") + ".");
                if (iParameters.dispSoftMask) {
                    out_soft_mask[channel] = ImgUtils.ZXYarrayToImg(rg.iSoftMask, "Mask" + ((channel == 0) ? "X" : "Y"));
                }
                ImagePlus maskImg = generateMaskImg(rg.iAllMasks); 
                if (maskImg != null) {maskImg.setTitle("Mask Evol");maskImg.show();}
                System.out.println("END ==============");
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
    
    private ArrayList<Region> removeExternalObjects(ArrayList<Region> aRegionsList) {
        final ArrayList<Region> newregionlist = new ArrayList<Region>();
        for (Region r : aRegionsList) {
            if (isInside(r)) {
                newregionlist.add(r);
            }
        }

        return newregionlist;
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
    
    private double[][][] generateMaskFromPatches(int nz, int ni, int nj) {
        final CSV<Particle> csv = new CSV<Particle>(Particle.class);
        csv.setCSVPreferenceFromFile(iParameters.patches_from_file);
        Vector<Particle> pt = csv.Read(iParameters.patches_from_file, new CsvColumnConfig(Particle.ParticleDetection_map, Particle.ParticleDetectionCellProcessor));
   
        // Get the particle related inly to one frames
        final Vector<Particle> pt_f = getPart(pt, currentFrame - 1);
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
    
    // ============================================ Colocation analysis ==================================

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

    // ======================================
    
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
