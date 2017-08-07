package mosaic.region_competition.RC;

import java.io.File;

import org.apache.log4j.Logger;

import ij.ImagePlus;
import ij.io.Opener;
import mosaic.core.cluster.ClusterGUI;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.psf.GeneratePSF;
import mosaic.core.utils.MosaicUtils;
import mosaic.plugins.Region_Competition;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.plugins.Region_Competition.InitializationType;
import mosaic.utils.ImgUtils;

/**
 * Moved here from Region_Competition. 
 * TODO: Should be refactored!!!
 */
public class ClusterModeRC {
    private static final Logger logger = Logger.getLogger(ClusterModeRC.class);

    public static void runClusterMode(ImagePlus aImp, ImagePlus labelImage, Settings iSettings, String[] out) {
        // The only modification to old implementation:
        String labelImageFilename = ImgUtils.getImageAbsolutePath(aImp);
        String inputImageFilename = ImgUtils.getImageAbsolutePath(labelImage);
        // -------
        mosaic.utils.Debug.print("CLUSTER", inputImageFilename, labelImageFilename);
        logger.info("Running RC on cluster");
        
        // We run on cluster - saving config file
        Region_Competition.getConfigHandler().SaveToFile("/tmp/settings.dat", iSettings);

        final ClusterGUI cg = new ClusterGUI();
        ClusterSession ss = cg.getClusterSession();
        ss.setInputParameterName("text1");
        ss.setSlotPerProcess(1);
        File[] fileslist = null;

        // Check if we selected a directory
        if (aImp == null) {
            final File fl = new File(inputImageFilename);
            final File fl_l = new File(labelImageFilename);
            if (fl.isDirectory() == true) {
                // we have a directory

                String opt = getOptions(fl, iSettings);
                if (iSettings.labelImageInitType == InitializationType.File) {
                    // upload label images

                    ss = cg.getClusterSession();
                    fileslist = fl_l.listFiles();
                    final File dir = new File("label");
                    ss.upload(dir, fileslist);
                    opt += " text2=" + ss.getClusterDirectory() + File.separator + dir.getPath();
                }

                fileslist = fl.listFiles();

                ss = ClusterSession.processFiles(fileslist, "Region Competition", opt + " show_and_save_statistics", out, cg);
            }
            else if (fl.isFile()) {
                String opt = getOptions(fl, iSettings);
                if (iSettings.labelImageInitType == InitializationType.File) {
                    // upload label images
                    ss = cg.getClusterSession();
                    fileslist = new File[1];
                    fileslist[0] = fl_l;
                    ss.upload(fileslist);
                    opt += " text2=" + ss.getClusterDirectory() + File.separator + fl_l.getName();
                }

                ss = ClusterSession.processFile(fl, "Region Competition", opt + " show_and_save_statistics", out, cg);
            }
            else {
                ss = ClusterSession.getFinishedJob(out, "Region Competition", cg);
            }
        }
        else {
            // It is an image
            String opt = getOptions(aImp, iSettings);

            if (iSettings.labelImageInitType == InitializationType.File) {
                // upload label images

                ss = cg.getClusterSession();
                ss.splitAndUpload(labelImage, new File("label"), null);
                opt += " text2=" + ss.getClusterDirectory() + File.separator + "label" + File.separator + ss.getSplitAndUploadFilename(0);
            }

            ss = ClusterSession.processImage(aImp, "Region Competition", opt + " show_and_save_statistics", out, cg);
        }

        // Get output format and Stitch the output in the output selected
        final File f = ClusterSession.processJobsData(ImgUtils.getImageDirectory(aImp));

        if (aImp != null) {
            MosaicUtils.StitchCSV(ImgUtils.getImageDirectory(aImp), out, ImgUtils.getImageDirectory(aImp) + File.separator + aImp.getTitle());
        }
        else {
            MosaicUtils.StitchCSV(f.getParent(), out, null);
        }
    }
    
    /**
     * Return the dimension of a file
     * @param f file
     * @return
     */
    private static int getDimension(File f) {
        final Opener o = new Opener();
        final ImagePlus ip = o.openImage(f.getAbsolutePath());

        return getDimension(ip);
    }

    /**
     * Return the dimension of an image
     * @param aImp image
     * @return
     */
    private static int getDimension(ImagePlus aImp) {
        if (aImp.getNSlices() == 1) {
            return 2;
        }
        return 3;
    }

    private static String getOptions(File f, Settings settings) {
        final int d = getDimension(f);
        return generateParameters(d, settings);
    }


    private static String getOptions(ImagePlus aImp, Settings settings) {
        final int d = getDimension(aImp);
        return generateParameters(d, settings);
    }

    private static String generateParameters(final int aDimension, Settings settings) {
        String par = "Dimensions=" + aDimension + " ";
        
        // if deconvolving create a PSF generator window
        if (settings.m_EnergyFunctional == EnergyFunctionalType.e_DeconvolutionPC) {
            final GeneratePSF psf = new GeneratePSF();
            psf.generate(aDimension);
            par += psf.getParameters();
        }
        return par;
    }
}
