package mosaic.core.utils;


import static org.junit.Assert.fail;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import io.scif.SCIFIOService;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import mosaic.core.GUI.ProgressBarWin;
import mosaic.core.cluster.ClusterSession;
import mosaic.plugins.utils.PlugInFilterExt;
import mosaic.utils.io.csv.CSV;
import net.imglib2.img.Img;

import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;


/**
 * This class expose static member to test every plugins in our Mosaic Toolsuite
 *
 * @author Pietro Incardona
 */
public class MosaicTest {

    public static String getTestEnvironment() {
        return IJ.getDirectory("temp") + File.separator + "test" + File.separator;
    }

    private static void prepareTestEnvironment(ProgressBarWin wp, ImgTest tmp) {
        wp.SetStatusMessage("Testing... " + new File(tmp.base).getName());

        // Save on tmp and reopen
        final String tmp_dir = getTestEnvironment();

        // Remove everything there
        try {
            ShellCommand.exeCmdNoPrint("rm -rf " + tmp_dir);
        }
        catch (final IOException e3) {
            e3.printStackTrace();
        }
        catch (final InterruptedException e3) {
            e3.printStackTrace();
        }

        // make the test dir
        try {
            ShellCommand.exeCmd("mkdir " + tmp_dir);
        }
        catch (final IOException e2) {
            e2.printStackTrace();
        }
        catch (final InterruptedException e2) {
            e2.printStackTrace();
        }

        // For unknown reason IJ.save kill macro options
        final String options = Macro.getOptions();

        for (int i = 0; i < tmp.img.length; i++) {
            final String temp_img = tmp_dir + tmp.img[i].substring(tmp.img[i].lastIndexOf(File.separator) + 1);
            IJ.save(MosaicUtils.openImg(tmp.img[i]), temp_img);
        }

        Macro.setOptions(options);

        // copy the config file
        try {
            for (int i = 0; i < tmp.setup_files.length; i++) {
                String str = new String();
                str = IJ.getDirectory("temp") + File.separator + tmp.setup_files[i].substring(tmp.setup_files[i].lastIndexOf(File.separator) + 1);
                ShellCommand.exeCmdNoPrint("cp -r " + tmp.setup_files[i] + " " + str);
            }
        }
        catch (final IOException e1) {
            e1.printStackTrace();
        }
        catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Get a the path that contain the file
     * example:
     * cs = /some_path/image_A
     * /some_path/image_B
     * /some_other_path/image_C
     * fl = image_C
     * the function return 2
     *
     * @param cs List of paths
     * @param fl name of the file
     * @return String of the JobID
     */
    private static int getIDfromFileList(String path[], String fl) {
        for (int k = 0; k < path.length; k++) {
            if (new File(path[k]).getName().contains(fl)) {
                return k;
            }
        }

        return -1;
    }

    private static <T> void processResult(PlugInFilterExt BG, ImgTest tmp, ProgressBarWin wp, Class<T> cls) {
        // Save on tmp and reopen
        final String tmp_dir = getTestEnvironment();

        // Check if there are job directories
        final String[] cs = ClusterSession.getJobDirectories(0, tmp_dir);
        final String[] csr = ClusterSession.getJobDirectories(0, tmp.base);
        if (cs != null && cs.length != 0) {

            // Check if result_imgs has the same number
            if (csr.length % cs.length != 0) {
                fail("Error: Image result does not match the result");
            }

            // replace the result dir with the job id
            for (int i = 0; i < cs.length; i++) {
                final String fr[] = MosaicUtils.readAndSplit(cs[i] + File.separator + "JobID");
                final String fname = MosaicUtils.removeExtension(fr[2]);
                final String JobID = fr[0];

                final int id = getIDfromFileList(tmp.result_imgs_rel, fname);

                tmp.result_imgs_rel[id] = tmp.result_imgs_rel[id].replace("*", JobID);

            }

            for (int i = 0; i < csr.length; i++) {
                final String fr[] = MosaicUtils.readAndSplit(csr[i] + File.separator + "JobID");
                final String fname = MosaicUtils.removeExtension(fr[2]);
                final String JobID = fr[0];

                final int id = getIDfromFileList(tmp.result_imgs, fname);

                tmp.result_imgs[id] = tmp.result_imgs[id].replace("*", JobID);
            }

            // same things for csv
            for (int i = 0; i < cs.length; i++) {
                final String fr[] = MosaicUtils.readAndSplit(cs[i] + File.separator + "JobID");
                final String fname = MosaicUtils.removeExtension(fr[2]);
                final String JobID = fr[0];

                final int id = getIDfromFileList(tmp.csv_results_rel, fname);

                tmp.csv_results_rel[id] = tmp.csv_results_rel[id].replace("*", JobID);
            }

            for (int i = 0; i < csr.length; i++) {
                final String fr[] = MosaicUtils.readAndSplit(csr[i] + File.separator + "JobID");
                final String fname = MosaicUtils.removeExtension(fr[2]);
                final String JobID = fr[0];

                final int id = getIDfromFileList(tmp.csv_results, fname);

                tmp.csv_results[id] = tmp.csv_results[id].replace("*", JobID);
            }
        }

        int cnt = 0;

        final ImgOpener imgOpener = new ImgOpener(new Context(SCIFIOService.class, AppService.class, StatusService.class));
        // By default ImgOpener produces a lot of logs, this is one of the ways
        // to switch it off.
        imgOpener.log().setLevel(0);

        for (final String rs : tmp.result_imgs) {
            // open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg)
            // is automatically determined. For a small image that fits in memory,
            // this should open as an ArrayImg.
            Img<?> image = null;
            Img<?> image_rs = null;
            try {
                wp.SetStatusMessage("Checking... " + new File(rs).getName());
                image = imgOpener.openImgs(rs).get(0);
                String filename = null;
                filename = tmp_dir + File.separator + tmp.result_imgs_rel[cnt];

                // open the result image
                image_rs = imgOpener.openImgs(filename).get(0);
            }
            catch (final ImgIOException e) {
                e.printStackTrace();
            }
            catch (final java.lang.UnsupportedOperationException e) {
                e.printStackTrace();
                fail("Error: Image " + rs + " does not match the result");
            }

            // compare
            if (MosaicUtils.compare(image, image_rs) == false) {
                fail("Error: Image " + rs + " does not match the result");
            }

            cnt++;
        }

        // Close all images
        if (BG != null) {
            BG.closeAll();
        }

        // Open csv
        cnt = 0;

        for (final String rs : tmp.csv_results) {
            wp.SetStatusMessage("Checking... " + new File(rs).getName());

            final CSV<T> iCSVsrc = new CSV<T>(cls);

            String filename = null;
            filename = tmp_dir + File.separator + tmp.csv_results_rel[cnt];

            iCSVsrc.setCSVPreferenceFromFile(filename);
            final Vector<T> outsrc = iCSVsrc.Read(filename);

            final CSV<T> iCSVdst = new CSV<T>(cls);
            iCSVdst.setCSVPreferenceFromFile(rs);
            final Vector<T> outdst = iCSVdst.Read(rs);

            if (outsrc.size() != outdst.size() || outsrc.size() == 0) {
                fail("Error: CSV outout does not match");
            }

            for (int i = 0; i < outsrc.size(); i++) {
                if (outsrc.get(i).equals(outdst.get(i)) == false) {
                    // Maybe the order is changed
                    int j = 0;
                    for (j = 0; j < outdst.size(); j++) {
                        if (outsrc.get(i).equals(outdst.get(j)) == true) {
                            break;
                        }
                    }

                    if (j == outdst.size()) {
                        fail("Error: CSV output does not match");
                    }
                }
            }

            cnt++;
        }
    }

    /**
     * Test the plugin filter
     *
     * @param BG plugins filter filter
     * @param testset String that indicate the test to use (all the test are in
     *            Jtest_data folder)
     * @param Class<T> Class for reading csv files used for CSV class
     */
    public static <T> void testPlugin(PlugInFilterExt BG, String testset, Class<T> cls) {
        // Set the plugin in test mode
        BG.setIsOnTest(true);

        // Save on tmp and reopen
        final String tmp_dir = getTestEnvironment();
        final ProgressBarWin wp = new ProgressBarWin();

        // if macro options is different from "" or null filter the tests
        final String test_set = MosaicUtils.parseString("test", Macro.getOptions());
        if (test_set != null && test_set.length() != 0 && test_set.startsWith(testset) == false) {
            return;
        }

        // Get all test images
        final ImgTest imgT[] = MosaicUtils.getTestImages(testset, test_set);

        if (imgT == null) {
            fail("No Images to test");
            return;
        }

        // String
        final String original_options = Macro.getOptions();

        // for each image
        for (final ImgTest tmp : imgT) {
            prepareTestEnvironment(wp, tmp);

            // append the macro set options with the test specific options
            final String plugin_options = original_options + " " + tmp.options.replace("*", tmp.base);
            Macro.setOptions(plugin_options);

            // Create a plugin filter
            int rt = 0;
            if (tmp.img.length == 1) {
                final String temp_img = tmp_dir + tmp.img[0].substring(tmp.img[0].lastIndexOf(File.separator) + 1);
                final ImagePlus img = MosaicUtils.openImg(temp_img);
                img.show();

                rt = BG.setup(plugin_options, img);
            }
            else {
                rt = BG.setup(plugin_options, null);
            }

            if (rt != tmp.setup_return) {
                fail("Setup error expecting: " + tmp.setup_return + " getting: " + rt);
            }

            // run the filter
            BG.run(null);

            processResult(BG, tmp, wp, cls);
        }

        Macro.setOptions(original_options);
        wp.dispose();
    }
}
