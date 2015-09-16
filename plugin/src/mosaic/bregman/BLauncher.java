package mosaic.bregman;


/*
 * Colocalization analysis class
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.plugin.Resizer;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

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

import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Outdata;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.ShellCommand;
import mosaic.io.csv.CSV;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.ShortType;


class BLauncher {

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
    private Tools Tools;

    private Vector<ImagePlus> ip = new Vector<ImagePlus>();

    private String choice1[] = { "Automatic", "Low layer", "Medium layer", "High layer" };
    private String choice2[] = { "Poisson", "Gauss" };

    double colocsegAB = 0;
    double colocsegBA = 0;

    private Vector<String> pf = new Vector<String>();

    Vector<String> getProcessedFiles() {
        return pf;
    }

    /**
     * Launch the Segmentation + Analysis from path
     * 
     * @param path
     */
    BLauncher(String path) {
        boolean processdirectory = (new File(path)).isDirectory();
        if (processdirectory) {

            PrintWriter out = null;

            // Get all files
            File dir = new File(path);
            File fl[] = dir.listFiles();

            // Order the file by name
            Arrays.sort(fl);

            // Check if we have more than one frame
            for (File f : fl) {
                if (f.isDirectory() == true) continue;

                // If it is the Rscript continue
                if (f.getName().equals("R_analysis.R")) continue;

                // It is an hidden file
                if (f.getName().startsWith(".") == true) continue;

                // It is a csv file
                if (f.getName().endsWith(".csv") == true) continue;

                // Attempt to open a file

                try {
                    aImp = MosaicUtils.openImg(f.getAbsolutePath());
                    pf.add(MosaicUtils.removeExtension(f.getName()));
                }
                catch (java.lang.UnsupportedOperationException e) {
                    continue;
                }
                Headless_file();

                displayResult(true);

                System.out.println("Display result (save images = " + Analysis.p.save_images + ")");

                // Write a file info output

                if (Analysis.p.save_images) {
                    saveAllImages(MosaicUtils.ValidFolderFromImage(aImp));

                    try {
                        if (fl.length != 1)
                            out = writeImageDataCsv(out, MosaicUtils.ValidFolderFromImage(aImp), aImp.getTitle(), "stitch", hcount - 1);
                        else
                            out = writeImageDataCsv(out, MosaicUtils.ValidFolderFromImage(aImp), aImp.getTitle(), aImp.getTitle(), 0);
                    }
                    catch (FileNotFoundException e) {
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
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            // Open the image and process
            aImp = MosaicUtils.openImg(path);
            start(aImp);
        }
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
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // Display results

            displayResult(false);
        }

        // Write a file info output
        if (Analysis.p.save_images) {
            saveAllImages(MosaicUtils.ValidFolderFromImage(aImp));
        }

        if (out != null) out.close();
    }

    BLauncher(ImagePlus aImp_) {
        // start processing
        start(aImp_);
    }

    /**
     * Display results
     * 
     * @param separate true if you do not want separate the images
     */
    private void displayResult(boolean sep) {
        int factor = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
        int fz;
        if (Analysis.p.nz > 1)
            fz = factor;
        else
            fz = 1;

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
            ;
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
            String savepath = path + File.separator + getOutlineName(i) + ".zip";
            if (out_over[i] != null) IJ.saveAs(out_over[i], "ZIP", savepath);
        }

        for (int i = 0; i < out_disp.length; i++) {
            String savepath = path + File.separator + getIntensitiesName(i) + ".zip";
            if (out_disp[i] != null) IJ.saveAs(out_disp[i], "ZIP", savepath);
        }

        for (int i = 0; i < out_label.length; i++) {
            String savepath = path + File.separator + getSegmentationName(i) + ".zip";
            if (out_label[i] != null) IJ.saveAs(out_label[i], "ZIP", savepath);
        }

        for (int i = 0; i < out_label_gray.length; i++) {
            String savepath = path + File.separator + getMaskName(i) + ".zip";
            if (out_label_gray[i] != null) IJ.saveAs(out_label_gray[i], "ZIP", savepath);
        }
        for (int i = 0; i < Analysis.out_soft_mask.length; i++) {
            String savepath = path + File.separator + getSoftMask(i) + ".tiff";
            if (Analysis.out_soft_mask[i] != null) IJ.saveAsTiff(Analysis.out_soft_mask[i], savepath);
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
            String ffo = MosaicUtils.removeExtension(outfilename);

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
            double[] temp;
            temp = Analysis.pearson_corr();
            corr = temp[0];
            corr_mask = temp[1];

            double meanSA = Analysis.meansurface(Analysis.regionslist[0]);
            double meanSB = Analysis.meansurface(Analysis.regionslist[1]);

            double meanLA = Analysis.meanlength(Analysis.regionslist[0]);
            double meanLB = Analysis.meanlength(Analysis.regionslist[1]);

            out.print(filename + ";" + hcount + ";" + Analysis.na + ";" + mosaic.bregman.Tools.round(Analysis.meana, 4) + ";" + mosaic.bregman.Tools.round(meanSA, 4) + ";"
                    + mosaic.bregman.Tools.round(meanLA, 4) + ";" + +Analysis.nb + ";" + mosaic.bregman.Tools.round(Analysis.meanb, 4) + ";" + mosaic.bregman.Tools.round(meanSB, 4) + ";"
                    + mosaic.bregman.Tools.round(meanLB, 4) + ";" + colocAB + ";" + colocBA + ";" + colocABsize + ";" + colocBAsize + ";" + colocABnumber + ";" + colocBAnumber + ";" + colocA + ";"
                    + colocB + ";" + mosaic.bregman.Tools.round(corr, 4) + ";" + mosaic.bregman.Tools.round(corr_mask, 4));
            out.println();
            out.flush();
        }
        else {
            double meanSA = Analysis.meansurface(Analysis.regionslist[0]);
            double meanLA = Analysis.meanlength(Analysis.regionslist[0]);

            out.print(filename + ";" + hcount + ";" + Analysis.na + ";" + mosaic.bregman.Tools.round(Analysis.meana, 4) + ";" + mosaic.bregman.Tools.round(meanSA, 4) + ";"
                    + mosaic.bregman.Tools.round(meanLA, 4));
            out.println();
            out.flush();
        }
        return out;
    }

    private void Headless_file() {
        try {
            ImagePlus img = null;

            /* Get Image directory */
            img = aImp;

            if (img == null) {
                IJ.error("No image to process");
            }

            if (img.getType() == ImagePlus.COLOR_RGB) {
                IJ.error("This is a color image and is not supported, convert into 8-bit , 16-bit or float");
                return;
            }

            Analysis.p.nchannels = img.getNChannels();

            bcolocheadless(img);
            IJ.log("");
        }
        catch (Exception e) {// Catch exception if any
            e.printStackTrace();
            System.err.println("Error launcher file processing: " + e.getMessage());
        }
    }

    /*
     * It segment the image and give co-localization analysis result
     * for a 2 channels image
     * @param img2 Image to segment and analyse
     */
    private void bcolocheadless(ImagePlus img2) {
        double Ttime = 0;
        long lStartTime = new Date().getTime(); // start time

        Analysis.p.blackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        Analysis.p.nchannels = img2.getNChannels();


        if (Analysis.p.nchannels == 2) {
            Analysis.load2channels(img2);
        }

        if (Analysis.p.nchannels == 1) {
            Analysis.load1channel(img2);
        }
        if (Analysis.p.mode_voronoi2) {
            if (Analysis.p.nz > 1) {
                Analysis.p.max_nsb = 151;
                Analysis.p.interpolation = 2;
            }
            else {
                Analysis.p.max_nsb = 151;
                Analysis.p.interpolation = 4;
            }
        }

        int nni, nnj, nnz;
        nni = Analysis.imgA.getWidth();
        nnj = Analysis.imgA.getHeight();
        nnz = Analysis.imgA.getNSlices();

        Analysis.p.ni = nni;
        Analysis.p.nj = nnj;
        Analysis.p.nz = nnz;

        Tools = new Tools(nni, nnj, nnz);
        Analysis.iTools = Tools;

        Analysis.segmentA();

        if (Analysis.p.nchannels == 2) {
            Analysis.segmentb();
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
            int factor2 = Analysis.p.oversampling2ndstep * Analysis.p.interpolation;
            int fz2;
            if (Analysis.p.nz > 1)
                fz2 = factor2;
            else
                fz2 = 1;

            MasksDisplay md = new MasksDisplay(Analysis.p.ni * factor2, Analysis.p.nj * factor2, Analysis.p.nz * fz2, Analysis.p.nlevels, Analysis.p.cl, Analysis.p);
            md.displaycoloc(MosaicUtils.ValidFolderFromImage(img2) + img2.getTitle(), Analysis.regionslist[0], Analysis.regionslist[1], ip);

            Analysis.na = Analysis.regionslist[0].size();
            Analysis.nb = Analysis.regionslist[1].size();

            Analysis.meana = Analysis.meansize(Analysis.regionslist[0]);
            Analysis.meanb = Analysis.meansize(Analysis.regionslist[1]);

            if (Analysis.p.save_images) {
                // Write object 2 list
                String savepath = null;
                savepath = MosaicUtils.ValidFolderFromImage(aImp);

                // Calculate colocalization quantities

                colocAB = mosaic.bregman.Tools.round(Analysis.colocsegAB(hcount), 4);
                colocBA = mosaic.bregman.Tools.round(Analysis.colocsegBA(hcount), 4);
                colocABnumber = mosaic.bregman.Tools.round(Analysis.colocsegABnumber(), 4);
                colocABsize = mosaic.bregman.Tools.round(Analysis.colocsegABsize(hcount), 4);
                colocBAnumber = mosaic.bregman.Tools.round(Analysis.colocsegBAnumber(), 4);
                colocBAsize = mosaic.bregman.Tools.round(Analysis.colocsegBAsize(hcount), 4);
                colocA = mosaic.bregman.Tools.round(Analysis.colocsegA(null), 4);
                colocB = mosaic.bregman.Tools.round(Analysis.colocsegB(null), 4);

                String filename_without_ext = img2.getTitle().substring(0, img2.getTitle().lastIndexOf("."));
                String output1 = new String(savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv");
                String output2 = new String(savepath + File.separator + filename_without_ext + "_ObjectsData_c2" + ".csv");

                boolean append = false;

                if (new File(output1).exists())
                    append = true;
                else
                    append = false;

                // Write channel 2

                // Choose the Rscript coloc format
                CSVOutput.occ = CSVOutput.oc[2];
                Vector<? extends Outdata<Region>> obl = Analysis.getObjectsList(hcount, 1);

                CSV<? extends Outdata<Region>> IpCSV = CSVOutput.getCSV();
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, output2, obl, CSVOutput.occ.outputChoose, append);

                // Write channel 1

                obl = Analysis.getObjectsList(hcount, 0);
                IpCSV.clearMetaInformation();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, output1, obl, CSVOutput.occ.outputChoose, append);
            }

            Analysis.doingbatch = false;
            hcount++;
        }

        if (Analysis.p.nchannels == 1) {
            Analysis.na = Analysis.regionslist[0].size();
            Analysis.meana = Analysis.meansize(Analysis.regionslist[0]);
            if (Analysis.p.save_images) {
                String savepath = null;
                savepath = MosaicUtils.ValidFolderFromImage(aImp);

                boolean append = false;

                if (hcount == 0)
                    append = false;
                else
                    append = true;

                Vector<? extends Outdata<Region>> obl = Analysis.getObjectsList(hcount, 0);

                String filename_without_ext = img2.getTitle().substring(0, img2.getTitle().lastIndexOf("."));

                CSV<? extends Outdata<Region>> IpCSV = CSVOutput.getCSV();
                IpCSV.setMetaInformation("background", savepath + File.separator + img2.getTitle());
                CSVOutput.occ.converter.Write(IpCSV, savepath + File.separator + filename_without_ext + "_ObjectsData_c1" + ".csv", obl, CSVOutput.occ.outputChoose, append);
            }

            hcount++;

        }
        ij.Prefs.blackBackground = Analysis.p.blackbackground;

        long lEndTime = new Date().getTime(); // start time

        long difference = lEndTime - lStartTime; // check different
        Ttime += difference;
        IJ.log("Done. Total Time: " + Ttime / 1000 + "s");
    }

    private ImagePlus out_over[] = new ImagePlus[2];

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
        ImagePlus objcts = new ImagePlus();

        // build stack and imageplus for objects
        objS = new ImageStack(di, dj);

        for (int z = 0; z < dz; z++) {
            byte[] mask_bytes = new byte[di * dj];
            for (int i = 0; i < di; i++) {
                for (int j = 0; j < dj; j++) {
                    if (regions[z][i][j] > 0)
                        mask_bytes[j * di + i] = 0;
                    else
                        mask_bytes[j * di + i] = (byte) 255;
                }
            }

            ByteProcessor bp = new ByteProcessor(di, dj);
            bp.setPixels(mask_bytes);
            objS.addSlice("", bp);

        }
        objcts.setStack("Objects", objS);

        // build image in bytes
        ImageStack imgS;
        ImagePlus img = new ImagePlus();

        // build stack and imageplus for the image
        imgS = new ImageStack(Analysis.p.ni, Analysis.p.nj);

        for (int z = 0; z < Analysis.p.nz; z++) {
            byte[] mask_bytes = new byte[Analysis.p.ni * Analysis.p.nj];
            for (int i = 0; i < Analysis.p.ni; i++) {
                for (int j = 0; j < Analysis.p.nj; j++) {
                    mask_bytes[j * Analysis.p.ni + i] = (byte) (255 * image[z][i][j]);
                }
            }

            ByteProcessor bp = new ByteProcessor(Analysis.p.ni, Analysis.p.nj);
            bp.setPixels(mask_bytes);

            imgS.addSlice("", bp);
        }

        img.setStack("Image", imgS);

        // resize z
        Resizer re = new Resizer();
        img = re.zScale(img, dz, ImageProcessor.NONE);
        // img.duplicate().show();
        ImageStack imgS2 = new ImageStack(di, dj);
        for (int z = 0; z < dz; z++) {
            img.setSliceWithoutUpdate(z + 1);
            img.getProcessor().setInterpolationMethod(ImageProcessor.NONE);
            imgS2.addSlice("", img.getProcessor().resize(di, dj, false));
        }
        img.setStack(imgS2);

        for (int z = 1; z <= dz; z++) {
            BinaryProcessor bip = new BinaryProcessor((ByteProcessor) objcts.getStack().getProcessor(z));
            bip.outline();
            bip.invert();
        }

        ImagePlus tab[] = new ImagePlus[2];
        tab[0] = objcts;
        tab[1] = img;
        ImagePlus over = RGBStackMerge.mergeChannels(tab, false);

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

    private ImagePlus out_disp[] = new ImagePlus[2];

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
        RandomAccess<ShortType> ra = imgInt.randomAccess();

        int pos[] = new int[3];

        // for each region draw the region with specified intensity
        for (Region r : regionslist) {
            // for each pixel in the region
            for (Pix p : r.pixels) {
                pos[0] = p.px;
                pos[1] = p.py;
                pos[2] = p.pz;
                ra.setPosition(pos);
                ra.get().set((short) r.intensity);
            }
        }

        ImagePlus intensities = ImageJFunctions.wrap(imgInt, "Intensities");

        // copy to imageJ1
        ImagePlus intensities2 = intensities.duplicate();

        if (sep == false)
            updateImages(out_disp, intensities2, getIntensitiesName(channel - 1), Analysis.p.dispint, channel);
        else {
            ip.add(intensities2);
            out_disp[channel - 1] = intensities2;
            intensities2.show();
            intensities2.setTitle(getIntensitiesName(channel - 1));
        }
    }

    private ImagePlus out_label[] = new ImagePlus[2];
    private ImagePlus label = null;

    private static IndexColorModel backgroundAndSpectrum(int maximum) {
        if (maximum > 255) maximum = 255;
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];
        // Set all to white:
        for (int i = 0; i < 256; ++i) {
            reds[i] = greens[i] = blues[i] = (byte) 255;
        }
        // Set 0 to black:
        reds[0] = greens[0] = blues[0] = 0;
        float divisions = maximum;
        Color c;
        for (int i = 1; i <= maximum; ++i) {
            float h = (i - 1) / divisions;
            c = Color.getHSBColor(h, 1f, 1f);
            reds[i] = (byte) c.getRed();
            greens[i] = (byte) c.getGreen();
            blues[i] = (byte) c.getBlue();
        }
        return new IndexColorModel(8, 256, reds, greens, blues);
    }

    private String chan_s[] = { "X", "Y" };

    /**
     * Display regions colors
     * 
     * @param regions label image
     * @param channel number of the channel
     * @param max_r max number of region
     * @param sep = true to separate
     */
    private void displayRegionsCol(short[][][] regions, int channel, int max_r, boolean sep) {
        int width = regions[0].length;
        int height = regions[0][0].length;
        int depth = regions.length;

        // ImageStack out_label_stack = out_label[channel].getStack();

        ImageStack labS;
        label = new ImagePlus();

        // build stack and imageplus
        labS = new ImageStack(width, height);

        int min = 0;
        int max = Math.max(max_r, 255);
        for (int z = 0; z < depth; z++) {
            short[] mask_short = new short[width * height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    mask_short[j * width + i] = regions[z][i][j];
                }
            }
            ShortProcessor sp = new ShortProcessor(width, height);
            sp.setPixels(mask_short);
            sp.setMinAndMax(min, max);
            labS.addSlice("", sp);
        }

        labS.setColorModel(backgroundAndSpectrum(Math.min(max_r, 255)));
        label.setStack("Regions " + chan_s[channel - 1], labS);

        if (sep == false)
            updateImages(out_label, label, getSegmentationName(channel - 1), Analysis.p.dispcolors, channel);
        else {
            out_label[channel - 1] = label;
            ip.add(label);
            label.show();
            label.setTitle(getSegmentationName(channel - 1));
        }
    }

    private ImagePlus out_label_gray[] = new ImagePlus[2];

    /**
     * Display regions labels
     * 
     * @param channel
     * @param sep = true if you want to separate
     */
    private void displayRegionsLab(int channel, boolean sep) {
        ImagePlus label_ = label.duplicate();

        IJ.run(label_, "Grays", "");

        if (sep == false)
            updateImages(out_label_gray, label_, getMaskName(channel - 1), Analysis.p.displabels, channel);
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

    /**
     * Close all images
     */
    void closeAll() {
        for (int i = 0; i < ip.size(); i++) {
            ip.get(i).close();
        }
    }
}
