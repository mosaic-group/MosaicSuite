package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.process.ImageProcessor;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mosaic.bregman.Analysis;
import mosaic.bregman.Analysis.outputF;
import mosaic.bregman.GenericGUI;
import mosaic.bregman.Parameters;
import mosaic.bregman.output.CSVOutput;
import mosaic.core.psf.psf;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.SerializedDataFile;
import net.imglib2.type.numeric.real.DoubleType;

public class BregmanGLM_Batch implements Segmentation {
    private ImagePlus OriginalImagePlus = null;
    private String savedSettings;
    private GenericGUI window;
    boolean gui_use_cluster = false;

    @Override
    public int setup(String arg0, ImagePlus active_img) {
        if (MosaicUtils.checkRequirement() == false)
            return DONE;

        // init basic structure
        Analysis.init();

        // if is a macro get the arguments from macro arguments
        if (IJ.isMacro()) {
            arg0 = Macro.getOptions();
            if (arg0 == null)
                arg0 = "";
        }

        String dir = IJ.getDirectory("temp");
        savedSettings = dir + "spb_settings.dat";
        Analysis.p = getConfigHandler().LoadFromFile(savedSettings, Parameters.class, Analysis.p);

        String path = findMatchedString(arg0, "config");
        if (path != null) {
            Analysis.p = getConfigHandler().LoadFromFile(path, Parameters.class, Analysis.p);
        }
        
        String norm = findMatchedString(arg0, "min");
        if (norm != null) {
            Analysis.norm_min = Double.parseDouble(norm);
            System.out.println("min norm " + Analysis.norm_min);
        }
        
        norm = findMatchedString(arg0, "max");
        if (norm != null) {
            Analysis.norm_max = Double.parseDouble(norm);
            System.out.println("max norm " + Analysis.norm_max);
        }

        // Initialize CSV format
        CSVOutput.initCSV(Analysis.p.oc_s);

        this.OriginalImagePlus = active_img;

        // Check the argument
        boolean batch = GraphicsEnvironment.isHeadless();

        if (batch == true) Analysis.p.dispwindows = false;

        window = new GenericGUI(batch, active_img);
        window.setUseCluster(gui_use_cluster);
        window.run("", active_img);

        saveConfig(savedSettings, Analysis.p);

        System.out.println("Setting macro options: " + arg0);

        // Re-set the arguments
        Macro.setOptions(arg0);

        return DONE;
    }

    /**
     * Find argument for given parameter
     * @param aInputArgs - input string containing all parameters and arguments
     * @param aParameter - search parameter name 
     * @return arguments for given parameter
     */
    private String findMatchedString(String aInputArgs, String aParameter) {
        Pattern aParameterPattern = Pattern.compile(aParameter);
        Pattern pathp = Pattern.compile("[a-zA-Z0-9/_.-]+");
        Pattern spaces = Pattern.compile("[\\s]*=[\\s]*");
        Matcher matcher = aParameterPattern.matcher(aInputArgs);
        if (matcher.find()) {
            String sub = aInputArgs.substring(matcher.end());
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
    
    @Override
    public void run(ImageProcessor imp) {
    }

    /**
     * Returns handler for (un)serializing Parameters objects.
     */
    public static DataFile<Parameters> getConfigHandler() {
        return new SerializedDataFile<Parameters>();
    }

    /**
     * Saves Parameters objects with additional handling of unserializable PSF
     * object. TODO: It (PSF) should be verified and probably corrected.
     * 
     * @param aFullFileName - absolute path and file name
     * @param aParams - object to be serialized
     */
    public static void saveConfig(String aFullFileName, Parameters aParams) {
        // Nullify PSF since it is not Serializable
        psf<DoubleType> tempPsf = aParams.PSF;
        aParams.PSF = null;

        getConfigHandler().SaveToFile(aFullFileName, aParams);

        aParams.PSF = tempPsf;
    }

    // =================== Stuff below is used only by tests

    /**
     * Unfortunately where is not way to hide the GUI in test mode, set the
     * plugin to explicitly bypass the GUI
     */
    public void bypass_GUI() {
        GenericGUI.bypass_GUI = true;
    }

    public void setUseCluster(boolean bl) {
        gui_use_cluster = bl;
    }

    // =================== Implementation of PlugInFilterExt interface
    public static boolean test_mode;

    /**
     * Close all the file
     */
    @Override
    public void closeAll() {
        if (OriginalImagePlus != null) {
            OriginalImagePlus.close();
        }

        window.closeAll();
    }

    @Override
    public void setIsOnTest(boolean test) {
        test_mode = test;
    }

    @Override
    public boolean isOnTest() {
        return test_mode;
    }

    // =================== Implementation of Segmentation interface

    /**
     * Get Mask images name output
     * 
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getMask(ImagePlus aImp) {
        String[] gM = new String[2];
        gM[0] = new String(Analysis.out[outputF.MASK.getNumVal()].replace("*", "_") + File.separator
                + Analysis.out[outputF.MASK.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        gM[1] = new String(Analysis.out[outputF.MASK.getNumVal() + 1].replace("*", "_") + File.separator
                + Analysis.out[outputF.MASK.getNumVal() + 1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        return gM;
    }

    /**
     * Get CSV regions list name output
     * 
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getRegionList(ImagePlus aImp) {
        String[] gM = new String[4];
        gM[0] = new String(Analysis.out[outputF.OBJECT.getNumVal()].replace("*", "_") + File.separator
                + Analysis.out[outputF.OBJECT.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        gM[1] = new String(Analysis.out[outputF.OBJECT.getNumVal() + 1].replace("*", "_")
                + File.separator
                + Analysis.out[outputF.OBJECT.getNumVal() + 1].replace("*",
                        MosaicUtils.removeExtension(aImp.getTitle())));

        // This is produced if there is a stitch operation
        gM[2] = new String(MosaicUtils.removeExtension(aImp.getTitle())
                + Analysis.out[outputF.OBJECT.getNumVal()].replace("*", "_"));
        gM[3] = new String(MosaicUtils.removeExtension(aImp.getTitle())
                + Analysis.out[outputF.OBJECT.getNumVal() + 1].replace("*", "_"));

        return gM;
    }

    /**
     * Get name of the plugins
     */
    @Override
    public String getName() {
        return "Squassh";
    }
}
