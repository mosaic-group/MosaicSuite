package mosaic.core.utils;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.utils.MosaicUtils.ToARGB;
import mosaic.plugins.BregmanGLM_Batch;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvMetaInfo;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;


class FloatToARGB implements ToARGB {

    private double min = 0.0;
    private double max = 255;

    @Override
    public ARGBType toARGB(Object data) {
        final ARGBType t = new ARGBType();

        float td = 0.0f;
        td = (float) (255 * (((RealType<?>) data).getRealFloat() - min) / max);
        t.set(ARGBType.rgba(td, td, td, 255.0f));

        return t;
    }

    @Override
    public void setMinMax(double min, double max) {
        this.min = min;
        this.max = max;
    }
}

class IntToARGB implements ToARGB {

    private double min = 0.0;
    private double max = 255;

    @Override
    public ARGBType toARGB(Object data) {
        final ARGBType t = new ARGBType();

        int td = 0;
        td = (int) (255 * (((IntegerType<?>) data).getInteger() - min) / max);
        t.set(ARGBType.rgba(td, td, td, 255));

        return t;
    }

    @Override
    public void setMinMax(double min, double max) {
        this.min = min;
        this.max = max;
    }
}

class ARGBToARGB implements ToARGB {

    @Override
    public ARGBType toARGB(Object data) {
        return (ARGBType) data;
    }

    @Override
    public void setMinMax(double min, double max) {
        // Do nothing
    }
}

public class MosaicUtils {
    static public class SegmentationInfo {
        public File RegionList;
    }

    ////// Conversion to ARGB from different Type ////////////////////////////
    public interface ToARGB {
        void setMinMax(double min, double max);
        ARGBType toARGB(Object data);
    }

    /**
     * From an Object return a generic converter to ARGB type and at the same
     * time set a re-normalization Useful when you have a Generic T, you can use
     * in the following way T data; ToARGB conv = getConvertion(data,cursor<T>)
     * conv.toARGB(data);
     *
     * @param data
     * @return
     */
    static public <T extends RealType<T>> ToARGB getConversion(Object data, Cursor<T> crs) {
        ToARGB conv = null;
        if (data instanceof RealType) {
            conv = new FloatToARGB();
        }
        else if (data instanceof IntegerType) {
            conv = new IntToARGB();
        }
        else if (data instanceof ARGBType) {
            conv = new ARGBToARGB();
        }
        else {
            throw new RuntimeException();
        }

        crs.next();

        // Get the min and max
        T min = crs.get().createVariable();
        T max = crs.get().createVariable();
        getMinMax(crs, min, max);

        // get conversion;
        conv.setMinMax(min.getRealDouble(), max.getRealDouble());

        return conv;
    }

    /**
     * Filter out from Possible candidate file the one chosen by the user If
     * only one file present nothing appear
     * @param PossibleFile Vector of possible File
     * @return Chosen file
     */
    static private File filter_possible(Vector<File> PossibleFile) {
        if (PossibleFile == null || PossibleFile.size() == 0) {
            return null;
        }

        if (PossibleFile.size() == 1) {
            return PossibleFile.get(0);
        }
        // Ask user to choose
        return new ChooseGUI().chooseFile("Choose segmentation", "Found multiple segmentations", PossibleFile);
    }

    /**
     * Check if there are segmentation information for the image
     * @param Image
     */
    static public boolean checkSegmentationInfo(ImagePlus aImp, String plugin) {
        final String Folder = ImgUtils.getImageDirectory(aImp);
        final Segmentation[] sg = getSegmentationPluginsClasses();

        // Get infos from possible segmentation
        for (int i = 0; i < sg.length; i++) {
            final String sR[] = sg[i].getRegionList(aImp);
            for (int j = 0; j < sR.length; j++) {
                final File fR = new File(Folder + sR[j]);
                mosaic.utils.Debug.print("checkSegmentationInfo", fR.getAbsoluteFile());
                if (fR.exists()) {
                    return true;
                }
            }

            // Check if there are Jobs directory if there are open a job selector
            // and search inside the selected directory
            final String[] jb = ClusterSession.getJobDirectories(0, Folder);

            // check if the jobID and filename match
            for (int k = 0; k < jb.length; k++) {
                final String[] fl = readAndSplit(jb[k] + File.separator + "JobID");

                if (fl[2].contains(aImp.getTitle()) && sg[i].getName().equals(fl[3])) {
                    if (plugin == null) {
                        return true;
                    }
                    if (sg[i].getName().equals(plugin)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get if there are segmentation information for the image
     * @param Image
     */
    static public SegmentationInfo getSegmentationInfo(ImagePlus aImp) {
        final String Folder = ImgUtils.getImageDirectory(aImp);
        final Segmentation[] sg = getSegmentationPluginsClasses();
        final SegmentationInfo sI = new SegmentationInfo();

        // Get infos from possible segmentation
        final Vector<File> PossibleFile = new Vector<File>();
        for (int i = 0; i < sg.length; i++) {
            String sR[] = sg[i].getRegionList(aImp);
            for (int j = 0; j < sR.length; j++) {
                final File fR = new File(Folder + sR[j]);

                if (fR.exists()) {
                    PossibleFile.add(fR);
                }
            }

            // Check if there are Jobs directory
            // if there are open a job selector
            // and search inside the selected directory
            final String[] jb = ClusterSession.getJobDirectories(0, Folder);

            for (int k = 0; k < jb.length; k++) {
                // check if the jobID and filename match
                final String[] fl = readAndSplit(jb[k] + File.separator + "JobID");

                if (fl[2].contains(aImp.getTitle()) && sg[i].getName().equals(fl[3])) {
                    // Get the region list
                    sR = sg[i].getRegionList(aImp);
                    for (int j = 0; j < sR.length; j++) {
                        final File fR = new File(jb[k] + File.separator + sR[j]);
                        if (fR.exists() == true) {
                            PossibleFile.add(fR);
                        }
                    }
                }
            }

            sI.RegionList = filter_possible(PossibleFile);

            // if not segmentation choosen
            if (sI.RegionList == null) {
                return null;
            }
        }
        
        return sI;
    }

    /**
     * Get segmentation classes
     * @return an array of the classes
     */
    static private Segmentation[] getSegmentationPluginsClasses() {
        return new Segmentation[] {new BregmanGLM_Batch()};
    }

    /**
     * This function merge the frames of the image a2 into a1
     * @param a1 Image a1
     * @param a2 Image a2
     */
    static public void MergeFrames(ImagePlus a1, ImagePlus a2) {
        if (a1 == null || a2 == null) return;
        
        // If a1 does not have an imageStack set it to a2 and return
        if (a1.getImageStack().getSize() == 0) {
            a1.setStack("Merge frames", a2.getImageStack().duplicate());
            a1.setDimensions(a2.getNChannels(), a2.getNSlices(), a2.getNFrames());
            return;
        }

        // merge slices channels frames
        final int hcount = a2.getNFrames() + a1.getNFrames();
        for (int k = 1; k <= a2.getNFrames(); k++) {
            for (int j = 1; j <= a2.getNSlices(); j++) {
                for (int i = 1; i <= a2.getNChannels(); i++) {
                    a2.setPosition(i, j, k);
                    a1.getImageStack().addSlice("", a2.getChannelProcessor().getPixels());
                }
            }
        }
        a1.getStack().setColorModel(a2.getStack().getColorModel());
        a1.setDimensions(a2.getNChannels(), a2.getNSlices(), hcount);
    }

    /**
     * Read a file and split the String by space
     * @param file
     * @return values array of String
     */
    public static String[] readAndSplit(String file) {
        // Z means: "The end of the input but for the final terminator, if any"
        String output = null;
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(file));
            output = scanner.useDelimiter("\\Z").next();
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } 
        finally {
            if (scanner != null) scanner.close();
        }

        return output.split(" ");
    }

    public static ImageStack getSubStackInFloat(ImageStack is, int startPos, int endPos, boolean duplicate) {
        if (startPos > endPos || startPos < 0 || endPos < 0) {
            return null;
        }
        final ImageStack subStack = new ImageStack(is.getWidth(), is.getHeight());
        for (int i = startPos; i <= endPos; ++i) {
            ImageProcessor fp = duplicate ? is.getProcessor(i).convertToFloat().duplicate() : is.getProcessor(i).convertToFloat();
            subStack.addSlice(is.getSliceLabel(i), fp);
        }
        return subStack;
    }

    /**
     * Writes the given <code>info</code> to given file information. <code>info</code> will be written to the beginning of the file,
     * overwriting older information If the file does not exists it will be
     * created. Any problem creating, writing to or closing the file will
     * generate an ImageJ error
     *
     * @param directory location of the file to write to
     * @param file_name file name to write to
     * @param info info the write to file
     * @return boolean (writing to file successful or error message)
     */
    public static boolean write2File(String directory, String file_name, String info) {
        try {
            final FileOutputStream fos = new FileOutputStream(new File(directory, file_name));
            final BufferedOutputStream bos = new BufferedOutputStream(fos);
            final PrintWriter print_writer = new PrintWriter(bos);
            print_writer.print(info);
            print_writer.close();
            return true;
        }
        catch (final IOException e) {
            IJ.error("" + e);
            return false;
        }
    }

    /**
     * Copy an image B as a subspace into an image A
     *
     * @param A Image A
     * @param B Image B
     * @param fix subspace on A, dim(A) - dim(B) == 1
     * @return
     */
    static public <T extends NativeType<T>> boolean copyEmbedded(Img<T> A, Img<T> B, int fix) {
        // Check that the image are != null and the images has a difference
        // in dimensionality of one

        if (A == null || B == null || (A.numDimensions() - B.numDimensions() != 1)) {
            return false;
        }

        final Cursor<T> img_c = B.cursor();
        final RandomAccessibleInterval<T> view = Views.hyperSlice(A, B.numDimensions(), fix);
        final Cursor<T> img_v = Views.iterable(view).cursor();

        while (img_c.hasNext()) {
            img_c.fwd();
            img_v.fwd();
            img_v.get().set(img_c.get());
        }

        return true;
    }

    /**
     * Get the frame from input ImagePlus
     * @param aImage Image
     * @param aFrame frame (start from 1)
     * @return ImagePlus
     */
    public static ImagePlus getImageFrame(ImagePlus aImage, int aFrame) {
        final ImageStack stack = aImage.getStack();
        final int imagesPerFrame = stack.getSize() / aImage.getNFrames();

        final ImageStack subStack = new ImageStack(aImage.getWidth(), aImage.getHeight());
        for (int j = 1; j <= imagesPerFrame; ++j) {
            subStack.addSlice("st" + j, stack.getProcessor((aFrame - 1) * imagesPerFrame + j));
        }

        return new ImagePlus("tmp", subStack);
    }

    /**
     * Get the ImagePlus slice from an ImagePlus
     * TODO: Must be investigated during particle tracker refactoring. This method probably does not do what it claims.
     * @param aImage Image
     * @param aChannel (channel start from 1)
     * @return An ImagePlus of the channel
     */
    public static ImagePlus getImageSlice(ImagePlus aImage, int aChannel) {
        if (aChannel == 0) {
            return null;
        }

        final ImageStack stack = aImage.getStack();
        final int imagesPerChannel = stack.getSize() / aImage.getNSlices();

        final ImageStack tmp_stk = new ImageStack(aImage.getWidth(), aImage.getHeight());
        for (int j = 0; j < imagesPerChannel; j++) {
            tmp_stk.addSlice("st" + j, stack.getProcessor((aChannel - 1) * imagesPerChannel + j + 1));
        }

        return new ImagePlus("tmp", tmp_stk);
    }

    /**
     * Reorganize the data in the directories inside sv, following the file
     * patterns specified Give output[] = {data*file1, data*file2} and base =
     * "_tmp_" it create two folder data_file1 and data_file2 (* is replaced
     * with _) and inside put all the file with pattern data_tmp_1file1 .....
     * data_tmp_Nfile1 in the first and data_tmp_1file2 ..... data_tmp_Nfile2 in
     * the second. If the folder is empty the folder is deleted
     *
     * @param output List of output patterns
     * @param base String of the image/data to substitute
     * @param sv base dir where the data are located
     * @param nf is N, the number of file, if nf == 0 the file pattern is
     *            data_tmp_file without number
     */
    public static void reorganize(String output[], String base, String sv, int nf) {
        // Crate directories
        for (int j = 0; j < output.length; j++) {
            final String dirName = sv + "/" + output[j].replace("*", "_");
            SysOps.createDir(dirName);
        }

        // Copy all existing files
        for (int j = 0; j < output.length; j++) {
            final String tmp = output[j];

            for (int k = 0; k < nf; k++) {
                String src = "";
                String dest = "";
                if (new File(sv + File.separator + tmp.replace("*", base)).exists()) {
                    src = sv + File.separator + tmp.replace("*", base);
                    dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base + tmp.replace("*", "");
                }
                else {
                    src = sv + File.separator + tmp.replace("*", base + (k + 1));
                    dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base + (k + 1) + tmp.replace("*", "");
                }

                SysOps.moveFile(src, dest, true /* quiet */);
            }
        }

        // check for all the folder created if empty delete it
        for (int j = 0; j < output.length; j++) {
            final String tmp = new String(output[j]);
            final String dirStr = sv + "/" + tmp.replace("*", "_");
            final File dir = new File(dirStr);
            if (dir.listFiles() != null && dir.listFiles().length == 0) {
                SysOps.removeDir(dir);
            }
        }
    }
    
    

    /**
     * Reorganize the data in the directories inside sv, following the file
     * patterns specified Give output[] = {data*file1, data*file2} and base_src
     * = "_src_" and base_dst = "_dst_" it create two folder data_file1 and
     * data_file2 (* is replaced with _) and inside put all the file with
     * pattern data_src_1file1 ..... data_src_Nfile1 in the first renaming to
     * data_dst_1file1 ..... data_dst_Nfile1 and data_src_1file2 .....
     * data_src_Nfile2 in the second renaming to data_dst_1file2 .....
     * data_dst_Nfile2 If the folder is empty the folder is deleted
     *
     * @param output List of output patterns
     * @param base String of the image/data to substitute
     * @param sv base dir where the data are located
     * @param nf is N, the number of file, if nf == 0 the file pattern is
     *            data_tmp_file without number
     */
    public static void reorganize(String output[], String base_src, String base_dst, String sv, int nf) {
        // Crate directories
        for (int j = 0; j < output.length; j++) {
            final String tmp = new String(output[j]);
            final String dirName = sv + "/" + tmp.replace("*", "_");
            SysOps.createDir(dirName);
        }

        for (int j = 0; j < output.length; j++) {
            final String tmp = new String(output[j]);
            for (int k = 0; k < nf; k++) {
                String src = "";
                String dest = "";
                if (new File(sv + File.separator + tmp.replace("*", base_src)).exists()) {
                    src = sv + File.separator + tmp.replace("*", base_src);
                    dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base_dst + tmp.replace("*", "");
                }
                else {
                    if (nf == 1) {
                        src = sv + File.separator + tmp.replace("*", base_src + "_" + (k + 1));
                        dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base_dst + tmp.replace("*", "");
                    }
                    else {
                        src = sv + File.separator + tmp.replace("*", base_src + "_" + (k + 1));
                        dest = sv + File.separator + tmp.replace("*", "_") + File.separator + base_dst + "_" + (k + 1) + tmp.replace("*", "");
                    }
                }

                SysOps.moveFile(src, dest, true /* quiet */);
            }
        }

        // check for all the folder created if empty delete it
        for (int j = 0; j < output.length; j++) {
            final String tmp = new String(output[j]);
            final String dirStr = sv + "/" + tmp.replace("*", "_");
            final File dir = new File(dirStr);
            if (dir.listFiles() != null && dir.listFiles().length == 0) {
                SysOps.removeDir(dir);
            }
        }
    }

    /**
     * Parse the options string to get an argument
     *
     * @param aParameterName - searched parameter name
     * @param aOptions - input string containing all parameters and arguments 
     *                   " ..... aParameterName = xxxxx ...... "
     * @return arguments for given parameter, null if the parameter does not exist
     */
    public static String parseString(final String aParameterName, final String aOptions) {
        if (aOptions == null || aParameterName == null) {
            return null;
        }

        final Pattern config = Pattern.compile(aParameterName);
        final Pattern spaces = Pattern.compile("[\\s]*=[\\s]*");
        final Pattern pathp = Pattern.compile("[a-zA-Z0-9/_.-:-]+");
        Matcher matcher = config.matcher(aOptions);
        if (matcher.find()) {
            String sub = aOptions.substring(matcher.end());
            matcher = spaces.matcher(sub);
            if (matcher.find()) {
                sub = sub.substring(matcher.end());
                matcher = pathp.matcher(sub);
                if (matcher.find()) {
                    return matcher.group(0);
                }
            }
        }

        return null;
    }
    
    /**
     * IJ method for parsing macro checkboxes. 
     * Returns true if aKey is in aOptions and not in a bracketed literal (e.g., "[literal]")
     * @param aKey name of checkbox field
     * @param aOptions macro options
     * @return true if exist
     */
    public static boolean parseCheckbox(final String aKey, final String aOptions) {
        String s1 = aOptions;
        String s2 = aKey + " ";
        if (s1.startsWith(s2))
            return true;
        s2 = " " + s2;
        int len1 = s1.length();
        int len2 = s2.length();
        boolean match, inLiteral=false;
        char c;
        for (int i=0; i<len1-len2+1; i++) {
            c = s1.charAt(i);
            if (inLiteral && c==']')
                inLiteral = false;
            else if (c=='[')
                inLiteral = true;
            if (c!=s2.charAt(0) || inLiteral || (i>1&&s1.charAt(i-1)=='='))
                continue;
            match = true;
            for (int j=0; j<len2; j++) {
                if (s2.charAt(j)!=s1.charAt(i+j))
                {match=false; break;}
            }
            if (match) return true;
        }
        return false;
    }
    
    /**
     * Given an imglib2 image return the dimensions as an array of long
     * @param img Image
     * @return array with the image dimensions
     */
    public static <T> long[] getImageDimensions(Img<T> img) {
        final long dimensions_l[] = new long[img.numDimensions()];
        img.dimensions(dimensions_l);
        
        return dimensions_l;
    }

    /**
     * Given an imglib2 image return the dimensions as an array of integer
     * @param img Image
     * @return array with the image dimensions
     */
    public static <T> int[] getImageIntDimensions(Img<T> img) {
        return ConvertArray.toInt(getImageDimensions(img));
    }

    /**
     * Get the minimal value and maximal value of an image
     * @param img Image
     * @param min minimum value
     * @param max maximum value
     */
    private static <T extends RealType<T>> void getMinMax(Cursor<T> cur, T min, T max) {
        // Set min and max
        min.setReal(Double.MAX_VALUE);
        max.setReal(Double.MIN_VALUE);

        // Get the min and max
        while (cur.hasNext()) {
            cur.fwd();
            if (cur.get().getRealDouble() < min.getRealDouble()) {
                min.setReal(cur.get().getRealDouble());
            }
            if (cur.get().getRealDouble() > max.getRealDouble()) {
                max.setReal(cur.get().getRealDouble());
            }
        }
    }

    /**
     * Filter out the csv output dir
     * @param dir Array of directories
     * @return CSV output dir
     */
    private static String[] getCSV(String[] dir) {
        final Vector<String> outcsv = new Vector<String>();
        for (String d : dir) {
            if (d.endsWith(".csv")) outcsv.add(d.replace("*", "_"));
        }
        
        mosaic.utils.Debug.print("CSV files:", outcsv);
        return outcsv.toArray(new String[0]);
    }

    /**
     * Stitch the CSV files all together in the directory aBaseDir/aDirs[] save the
     * result in aOutputCsvFile + aDirs[] "*" are substituted by "_"
     * @param aDirs list of directories
     * @param aBaseDir Base
     * @param aOutputCsvFile stitched file
     * @param aMetaInfo metainformation for csv file
     * @param Class<T> internal data for conversion
     */
    private static <T> void Stitch(String aDirs[], File aBaseDir, File aOutputCsvFile, CsvMetaInfo aMetaInfo[], Class<T> aClazz) {
        boolean firstFile = true;
        final CSV<T> csv = new CSV<T>(aClazz);

        for (int j = 0; j < aDirs.length; j++) {
            final String currentDir = aDirs[j];
            
            // Get absolute paths to all files in currentDir
            final File[] currentFiles = new File(aBaseDir + File.separator + currentDir.replace("*", "_")).listFiles();
            if (currentFiles == null) {
                continue;
            }
            final String currentFilesAbsPaths[] = new String[currentFiles.length];
            for (int i = 0; i < currentFiles.length; i++) {
                if (currentFiles[i].getName().endsWith(".csv")) {
                    currentFilesAbsPaths[i] = currentFiles[i].getAbsolutePath();
                }
            }
            Arrays.sort(currentFilesAbsPaths);
            mosaic.utils.Debug.print(currentDir, currentFiles);
            // Set metainformation for csv
            csv.clearMetaInformation();
            if (aMetaInfo != null) {
                for (CsvMetaInfo cmi : aMetaInfo) {
                    csv.setMetaInformation(cmi);
                }
            }
            
            // if it is the first time set the file preference from the first file
            if (firstFile == true) {
                firstFile = false;
                csv.setCSVPreferenceFromFile(currentFilesAbsPaths[0]);
            }

            try {
                csv.StitchAny(currentFilesAbsPaths,  aOutputCsvFile + currentDir);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stitch the CSV in the directory
     *
     * @param aBaseDir directory where search for files to stitch directory to stitch
     * @param output string array that list all the outputs produced by the plugin
     * @param background Set the background param string
     * @return true if it stitch all the file success
     */
    static public void StitchCSV(String aBaseDir, String[] output, String aBackgroundValue) {
        CsvMetaInfo mt[] = (aBackgroundValue != null) ? new CsvMetaInfo[] {new CsvMetaInfo("background", aBackgroundValue)} : null;
        final String[] outcsv = getCSV(output);
        Stitch(outcsv, new File(aBaseDir), new File(aBaseDir + File.separator + "stitch"), mt, Region3DColocRScript.class);
    }

    /**
     * Stitch the CSV in the Jobs directory
     * @param fl directory where search for JobsXXX directory to stitch the csv
     * @param output string array that list all the outputs produced by the plugin
     * @param background Set the backgrond param string
     */
    static public void StitchJobsCSV(String fl, String[] output, String bck) {
        for (String jobDir : ClusterSession.getJobDirectories(0, fl)) {
            StitchCSV(jobDir, output, bck);
        }
    }

    /**
     * Calculate the sum of all pixels
     */
    public static <T extends RealType<T>> double volume_image(Img<T> aImage) {
        double sum = 0.0;

        final Cursor<T> cursor = aImage.cursor();
        while (cursor.hasNext()) {
            cursor.fwd();
            sum += cursor.get().getRealDouble();
        }

        return sum;
    }

    /**
     * It rescales all the pixels of image by factor aScaleFactor
     * @param aImage Image
     * @param aScaleFactor factor to rescale
     */
    public static <T extends RealType<T>> void rescale_image(Img<T> aImage, float aScaleFactor) {
        final Cursor<T> cursor = aImage.cursor();

        while (cursor.hasNext()) {
            cursor.fwd();
            cursor.get().setReal(cursor.get().getRealFloat() * aScaleFactor);
        }
    }
}
