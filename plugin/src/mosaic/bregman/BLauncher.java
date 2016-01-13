package mosaic.bregman;


import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.plugin.Resizer;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Outdata;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.utils.io.csv.CSV;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.ShortType;


public class BLauncher {
    private double colocAB;
    private double colocABnumber;
    private double colocABsize;
    private double colocBA;
    private double colocBAnumber;
    private double colocBAsize;
    private double colocA;
    private double colocB;

    private int hcount = 0;
    private ImagePlus aImp;

    private final Vector<ImagePlus> ip = new Vector<ImagePlus>();

    private final String choice1[] = { "Automatic", "Low layer", "Medium layer", "High layer" };
    private final String choice2[] = { "Poisson", "Gauss" };

    private final Vector<String> pf = new Vector<String>();

    public Vector<String> getProcessedFiles() {
        return pf;
    }

    /**
     * Launch the Segmentation + Analysis from path
     *
     * @param path
     */
    public BLauncher(String path) {
        final boolean processdirectory = (new File(path)).isDirectory();
        if (processdirectory) {

            PrintWriter out = null;

            // Get all files
            final File dir = new File(path);
            final File fl[] = dir.listFiles();

            // Order the file by name
            Arrays.sort(fl);

            // Check if we have more than one frame
            for (final File f : fl) {
                if (f.isDirectory() == true) {
                    continue;
                }

                // If it is the Rscript continue
                if (f.getName().equals("R_analysis.R")) {
                    continue;
                }

                // It is an hidden file
                if (f.getName().startsWith(".") == true) {
                    continue;
                }

                // It is a csv file
                if (f.getName().endsWith(".csv") == true) {
                    continue;
                }

                // Attempt to open a file

                try {
                    aImp = MosaicUtils.openImg(f.getAbsolutePath());
                    pf.add(MosaicUtils.removeExtension(f.getName()));
                }
                catch (final java.lang.UnsupportedOperationException e) {
                    continue;
                }
                Headless_file();

                displayResult(true);

                System.out.println("Display result (save images = " + Analysis.p.save_images + ")");

                // Write a file info output
                if (Analysis.p.save_images) {
                    saveAllImages(MosaicUtils.ValidFolderFromImage(aImp));

                    String outFilename= "stitch";
                    if (fl.length == 1) {
                        outFilename = aImp.getTitle();
                    }

                    try {
                        out = writeImageDataCsv(out, MosaicUtils.ValidFolderFromImage(aImp), aImp.getTitle(), outFilename, hcount - 1);
                    }
                    catch (final FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (out != null) {
                out.close();
                out = null;
            }

            // Try to run the R script
            try {
                ShellCommand.exeCmdNoPrint("Rscript " + dir.getAbsolutePath() + File.separator + "R_analysis.R");
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            // Open the image and process
            aImp = MosaicUtils.openImg(path);
            start(aImp);
        }
    }

    public BLauncher(ImagePlus aImp_) {
        // start processing
        start(aImp_);
    }

    private void start(ImagePlus aImp_) {
        aImp = aImp_;
        PrintWriter out = null;

        // Check if we have more than one frame
        for (int f = 1; f <= aImp.getNFrames(); f++) {
            Analysis.frame = f;
            aImp.setPosition(aImp.getChannel(), aImp.getSlice(), f);
            Headless_file();

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
        if (Analysis.p.save_images) {
            saveAllImages(MosaicUtils.ValidFolderFromImage(aImp));
        }

        if (out != null) {
            out.close();
        }
    }

    /**
     * Display results
     *
     * @param separate true if you do not want separate the images
     */
    private void displayResult(boolean sep) {
        final int factor = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
        int fz;
        if (Analysis.p.nz > 1) {
            fz = factor;
        }
        else {
            fz = 1;
        }

        System.out.println("Separate: " + sep);

        if (Analysis.p.dispoutline) {
            displayoutline(Analysis.regions[0], Analysis.imagea, Analysis.p.nz * fz, Analysis.p.ni * factor, Analysis.p.nj * factor, 1, sep);
            if (Analysis.p.nchannels == 2) {
                displayoutline(Analysis.regions[1], Analysis.imageb, Analysis.p.nz * fz, Analysis.p.ni * factor, Analysis.p.nj * factor, 2, sep);
            }
        }
        if (Analysis.p.dispint) {
            displayintensities(Analysis.regionslist[0], Analysis.p.nz * fz, Analysis.p.ni * factor, Analysis.p.nj * factor, 1, sep);
            if (Analysis.p.nchannels == 2) {
                displayintensities(Analysis.regionslist[1], Analysis.p.nz * fz, Analysis.p.ni * factor, Analysis.p.nj * factor, 2, sep);
            }
        }
        if (Analysis.p.displabels || Analysis.p.dispcolors) {
            displayRegionsCol(Analysis.regions[0], 1, Analysis.regionslist[0].size(), sep);
            if (Analysis.p.nchannels == 2) {
                displayRegionsCol(Analysis.regions[0], 2, Analysis.regionslist[0].size(), sep);
            }
        }
        if (Analysis.p.dispcolors) {
            displayRegionsLab(1, sep);
            if (Analysis.p.nchannels == 2) {
                displayRegionsLab(2, sep);
            }
        }
        if (Analysis.p.dispSoftMask) {
            Analysis.out_soft_mask[0].setTitle(getSoftMask(0));
            Analysis.out_soft_mask[0].show();
            if (Analysis.p.nchannels == 2) {
                Analysis.out_soft_mask[1].setTitle(getSoftMask(1));
                Analysis.out_soft_mask[1].show();
            }
        }
    }

    /**
     * Get the outline filename
     *
     * @return the outline name
     */
    private String getOutlineName(int i) {
        return Analysis.currentImage.substring(0, Analysis.currentImage.length() - 4) + "_outline_overlay_c" + (i + 1);
    }

    /**
     * Get the outline filename
     *
     * @return the outline filename
     */
    private String getIntensitiesName(int i) {
        return Analysis.currentImage.substring(0, Analysis.currentImage.length() - 4) + "_intensities" + "_c" + (i + 1);
    }

    /**
     * Get the Intensities filename
     *
     * @return the intensities filename
     */
    private String getSegmentationName(int i) {
        return Analysis.currentImage.substring(0, Analysis.currentImage.length() - 4) + "_seg_c" + (i + 1);
    }

    /**
     * Get the Mask filename
     *
     * @param i
     * @return the mask filename
     */
    private String getMaskName(int i) {
        return Analysis.currentImage.substring(0, Analysis.currentImage.length() - 4) + "_mask_c" + (i + 1);
    }

    private String getSoftMask(int i) {
        return Analysis.currentImage.substring(0, Analysis.currentImage.length() - 4) + "_soft_mask_c" + (i + 1);
    }

    /**
     * Save all images
     *
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
        for (int i = 0; i < Analysis.out_soft_mask.length; i++) {
            final String savepath = path + File.separator + getSoftMask(i) + ".tiff";
            if (Analysis.out_soft_mask[i] != null) {
                IJ.saveAsTiff(Analysis.out_soft_mask[i], savepath);
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
            if (Analysis.p.nchannels == 2) {
                out.print("File" + ";" + "Image ID" + ";" + "Objects ch1" + ";" + "Mean size in ch1" + ";" + "Mean surface in ch1" + ";" + "Mean length in ch1" + ";" + "Objects ch2" + ";"
                        + "Mean size in ch2" + ";" + "Mean surface in ch2" + ";" + "Mean length in ch2" + ";" + "Colocalization ch1 in ch2 (signal based)" + ";"
                        + "Colocalization ch2 in ch1 (signal based)" + ";" + "Colocalization ch1 in ch2 (size based)" + ";" + "Colocalization ch2 in ch1 (size based)" + ";"
                        + "Colocalization ch1 in ch2 (objects numbers)" + ";" + "Colocalization ch2 in ch1 (objects numbers)" + ";" + "Mean ch2 intensity of ch1 objects" + ";"
                        + "Mean ch1 intensity of ch2 objects" + ";" + "Pearson correlation" + ";" + "Pearson correlation inside cell masks");

                out.println();
                out.flush();
            }
            else {
                out.print("File" + ";" + "Image ID" + ";" + "Objects ch1" + ";" + "Mean size in ch1" + ";" + "Mean surface in ch1" + ";" + "Mean length in ch1");
                out.println();
                out.flush();
            }
            out.print("%Parameters:" + " " + "background removal " + " " + Analysis.p.removebackground + " " + "window size " + Analysis.p.size_rollingball + " " + "stddev PSF xy " + " "
                    + mosaic.bregman.Tools.round(Analysis.p.sigma_gaussian, 5) + " " + "stddev PSF z " + " " + mosaic.bregman.Tools.round(Analysis.p.sigma_gaussian / Analysis.p.zcorrec, 5) + " "
                    + "Regularization " + Analysis.p.lreg_[0] + " " + Analysis.p.lreg_[1] + " " + "Min intensity ch1 " + Analysis.p.min_intensity + " " + "Min intensity ch2 "
                    + Analysis.p.min_intensityY + " " + "subpixel " + Analysis.p.subpixel + " " + "Cell mask ch1 " + Analysis.p.usecellmaskX + " " + "mask threshold ch1 "
                    + Analysis.p.thresholdcellmask + " " + "Cell mask ch2 " + Analysis.p.usecellmaskY + " " + "mask threshold ch2 " + Analysis.p.thresholdcellmasky + " " + "Intensity estimation "
                    + choice1[Analysis.p.mode_intensity] + " " + "Noise model " + choice2[Analysis.p.noise_model] + ";");
            out.println();
            out.flush();
        }

        if (Analysis.p.nchannels == 2) {
            double corr_mask, corr;
            double[] temp = Analysis.pearson_corr();
            corr = temp[0];
            corr_mask = temp[1];

            final double meanSA = Analysis.meansurface(Analysis.regionslist[0]);
            final double meanSB = Analysis.meansurface(Analysis.regionslist[1]);

            final double meanLA = Analysis.meanlength(Analysis.regionslist[0]);
            final double meanLB = Analysis.meanlength(Analysis.regionslist[1]);

            out.print(filename + ";" + hcount + ";" + Analysis.regionslist[0].size() + ";" + mosaic.bregman.Tools.round(Analysis.meansize(Analysis.regionslist[0]), 4) + ";" + mosaic.bregman.Tools.round(meanSA, 4) + ";"
                    + mosaic.bregman.Tools.round(meanLA, 4) + ";" + +Analysis.regionslist[1].size() + ";" + mosaic.bregman.Tools.round(Analysis.meansize(Analysis.regionslist[1]), 4) + ";" + mosaic.bregman.Tools.round(meanSB, 4) + ";"
                    + mosaic.bregman.Tools.round(meanLB, 4) + ";" + colocAB + ";" + colocBA + ";" + colocABsize + ";" + colocBAsize + ";" + colocABnumber + ";" + colocBAnumber + ";" + colocA + ";"
                    + colocB + ";" + mosaic.bregman.Tools.round(corr, 4) + ";" + mosaic.bregman.Tools.round(corr_mask, 4));
            out.println();
            out.flush();
        }
        else {
            final double meanSA = Analysis.meansurface(Analysis.regionslist[0]);
            final double meanLA = Analysis.meanlength(Analysis.regionslist[0]);

            out.print(filename + ";" + hcount + ";" + Analysis.regionslist[0].size() + ";" + mosaic.bregman.Tools.round(Analysis.meansize(Analysis.regionslist[0]), 4) + ";" + mosaic.bregman.Tools.round(meanSA, 4) + ";"
                    + mosaic.bregman.Tools.round(meanLA, 4));
            out.println();
            out.flush();
        }
        return out;
    }

    private void Headless_file() {
        try {
            ImagePlus img = aImp;
            if (img == null) {
                IJ.error("No image to process");
                return;
            }

            if (img.getType() == ImagePlus.COLOR_RGB) {
                IJ.error("This is a color image and is not supported, convert into 8-bit , 16-bit or float");
                return;
            }

            Analysis.p.nchannels = img.getNChannels();

            bcolocheadless(img);
            IJ.log("");
        }
        catch (final Exception e) {
            e.printStackTrace();
            System.err.println("Error launcher file processing: " + e.getMessage());
        }
    }

    static Vector<? extends Outdata<Region>> getObjectsList(int f, int channel) {
        final Vector<? extends Outdata<Region>> v = CSVOutput.getVector(Analysis.regionslist[channel]);

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
        double Ttime = 0;
        final long lStartTime = new Date().getTime(); // start time

        boolean tempBlackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        Analysis.p.nchannels = img2.getNChannels();

        Analysis.loadChannels(img2, Analysis.p.nchannels);

        if (Analysis.p.nz > 1) {
            Analysis.p.max_nsb = 151;
            Analysis.p.interpolation = 2;
        }
        else {
            Analysis.p.max_nsb = 151;
            Analysis.p.interpolation = 4;
        }

        int nni, nnj, nnz;
        nni = Analysis.imgA.getWidth();
        nnj = Analysis.imgA.getHeight();
        nnz = Analysis.imgA.getNSlices();

        Analysis.p.ni = nni;
        Analysis.p.nj = nnj;
        Analysis.p.nz = nnz;

        Analysis.segmentA();

        if (Analysis.p.nchannels == 2) {
            Analysis.segmentB();
        }

        // TODO : why is it needed to reassign p.ni ...??
        Analysis.p.ni = Analysis.imgA.getWidth();
        Analysis.p.nj = Analysis.imgA.getHeight();
        Analysis.p.nz = Analysis.imgA.getNSlices();

        if (Analysis.p.nchannels == 2) {
            Analysis.computeOverallMask();
            Analysis.regionslist[0] = Analysis.removeExternalObjects(Analysis.regionslist[0]);
            Analysis.regionslist[1] = Analysis.removeExternalObjects(Analysis.regionslist[1]);

            Analysis.setRegionsLabels(Analysis.regionslist[0], Analysis.regions[0]);
            Analysis.setRegionsLabels(Analysis.regionslist[1], Analysis.regions[1]);
            final int factor2 = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
            int fz2 = (Analysis.p.nz > 1) ? factor2 : 1;

            final MasksDisplay md = new MasksDisplay(Analysis.p.ni * factor2, Analysis.p.nj * factor2, Analysis.p.nz * fz2, Analysis.p.nlevels, Analysis.p.cl, Analysis.p);
            md.displaycoloc(MosaicUtils.ValidFolderFromImage(img2) + img2.getTitle(), Analysis.regionslist[0], Analysis.regionslist[1], ip);

            if (Analysis.p.save_images) {
                // Calculate colocalization quantities
                colocAB = mosaic.bregman.Tools.round(Analysis.colocsegAB(), 4);
                colocBA = mosaic.bregman.Tools.round(Analysis.colocsegBA(), 4);
                colocABnumber = mosaic.bregman.Tools.round(Analysis.colocsegABnumber(), 4);
                colocABsize = mosaic.bregman.Tools.round(Analysis.colocsegABsize(), 4);
                colocBAnumber = mosaic.bregman.Tools.round(Analysis.colocsegBAnumber(), 4);
                colocBAsize = mosaic.bregman.Tools.round(Analysis.colocsegBAsize(), 4);
                colocA = mosaic.bregman.Tools.round(Analysis.colocsegA(), 4);
                colocB = mosaic.bregman.Tools.round(Analysis.colocsegB(), 4);

                String savepath = MosaicUtils.ValidFolderFromImage(aImp);
                final String filename_without_ext = img2.getTitle().substring(0, img2.getTitle().lastIndexOf("."));
                final String output1 = new String(savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv");
                final String output2 = new String(savepath + File.separator + filename_without_ext + "_ObjectsData_c2" + ".csv");

                boolean append = (new File(output1).exists()) ? true : false;

                // Write channel 2

                // Choose the Rscript coloc format
                CSVOutput.occ = CSVOutput.oc[2];
                Vector<? extends Outdata<Region>> obl = getObjectsList(hcount, 1);

                final CSV<? extends Outdata<Region>> IpCSV = CSVOutput.getCSV();
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, output2, obl, CSVOutput.occ.outputChoose, append);

                // Write channel 1
                obl = getObjectsList(hcount, 0);
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, output1, obl, CSVOutput.occ.outputChoose, append);
            }

            hcount++;
        }

        if (Analysis.p.nchannels == 1) {
            if (Analysis.p.save_images) {
                String savepath = null;
                savepath = MosaicUtils.ValidFolderFromImage(aImp);

                final Vector<? extends Outdata<Region>> obl = getObjectsList(hcount, 0);
                final String filename_without_ext = img2.getTitle().substring(0, img2.getTitle().lastIndexOf("."));

                final CSV<? extends Outdata<Region>> IpCSV = CSVOutput.getCSV();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv", obl, CSVOutput.occ.outputChoose, (hcount != 0));
            }

            hcount++;
        }
        ij.Prefs.blackBackground = tempBlackbackground;

        final long lEndTime = new Date().getTime(); // start time
        final long difference = lEndTime - lStartTime; // check different
        Ttime += difference;
        IJ.log("Done. Total Time: " + Ttime / 1000 + "s");
    }

    private final ImagePlus out_over[] = new ImagePlus[2];

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
        ImageStack objS;
        final ImagePlus objcts = new ImagePlus();

        // build stack and imageplus for objects
        objS = new ImageStack(di, dj);
        System.out.println(regions.length + " " + regions[0].length + " " + regions[0][0].length);
        for (int z = 0; z < dz; z++) {
            final byte[] mask_bytes = new byte[di * dj];
            for (int i = 0; i < di; i++) {
                for (int j = 0; j < dj; j++) {
                    if (regions[z][i][j] > 0) {
                        mask_bytes[j * di + i] = 0;
                    }
                    else {
                        mask_bytes[j * di + i] = (byte) 255;
                    }
                }
            }

            final ByteProcessor bp = new ByteProcessor(di, dj);
            bp.setPixels(mask_bytes);
            objS.addSlice("", bp);

        }
        objcts.setStack("Objects", objS);

        // build image in bytes

        // build stack and imageplus for the image
        ImageStack imgS = new ImageStack(Analysis.p.ni, Analysis.p.nj);
        for (int z = 0; z < Analysis.p.nz; z++) {
            final byte[] mask_bytes = new byte[Analysis.p.ni * Analysis.p.nj];
            for (int i = 0; i < Analysis.p.ni; i++) {
                for (int j = 0; j < Analysis.p.nj; j++) {
                    mask_bytes[j * Analysis.p.ni + i] = (byte) (255 * image[z][i][j]);
                }
            }

            final ByteProcessor bp = new ByteProcessor(Analysis.p.ni, Analysis.p.nj);
            bp.setPixels(mask_bytes);
            imgS.addSlice("", bp);
        }

        ImagePlus img = new ImagePlus("Image", imgS);

        final Resizer re = new Resizer();
        img = re.zScale(img, dz, ImageProcessor.NONE);
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

        final ImagePlus tab[] = new ImagePlus[2];
        tab[0] = objcts;
        tab[1] = img;
        final ImagePlus over = RGBStackMerge.mergeChannels(tab, false);

        // if we have already an outline overlay image merge the frame
        if (sep == false) {
            updateImages(out_over, over, getOutlineName(channel - 1), Analysis.p.dispoutline, channel);
        }
        else {
            ip.add(over);
            out_over[channel - 1] = over;
            over.show();
            over.setTitle(getOutlineName(channel - 1));
        }
    }

    private final ImagePlus out_disp[] = new ImagePlus[2];

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
            for (final Pix p : r.pixels) {
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
            updateImages(out_disp, intensities2, getIntensitiesName(channel - 1), Analysis.p.dispint, channel);
        }
        else {
            ip.add(intensities2);
            out_disp[channel - 1] = intensities2;
            intensities2.show();
            intensities2.setTitle(getIntensitiesName(channel - 1));
        }
    }

    private final ImagePlus out_label[] = new ImagePlus[2];
    private ImagePlus label = null;

    private static IndexColorModel backgroundAndSpectrum(int maximum) {
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

    private final String chan_s[] = { "X", "Y" };

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
            final ShortProcessor sp = new ShortProcessor(width, height);
            sp.setPixels(mask_short);
            sp.setMinAndMax(min, max);
            labS.addSlice("", sp);
        }

        labS.setColorModel(backgroundAndSpectrum(Math.min(max_r, 255)));
        
        label = new ImagePlus("Regions " + chan_s[channel - 1], labS);

        if (sep == false) {
            updateImages(out_label, label, getSegmentationName(channel - 1), Analysis.p.dispcolors, channel);
        }
        else {
            out_label[channel - 1] = label;
            ip.add(label);
            label.show();
            label.setTitle(getSegmentationName(channel - 1));
        }
    }

    private final ImagePlus out_label_gray[] = new ImagePlus[2];

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
            updateImages(out_label_gray, label_, getMaskName(channel - 1), Analysis.p.displabels, channel);
        }
        else {
            out_label_gray[channel - 1] = label_;
            ip.add(label_);
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
            ip.add(ips);
            ipd[channel - 1].setTitle(title);
        }

        if (disp) {
            // this force the update of the image
            ipd[channel - 1].setStack(ipd[channel - 1].getStack());
            ipd[channel - 1].show();
        }
    }

    public void closeAllImages() {
        for (ImagePlus img : ip) {
            img.close();
        }
    }
}
