package mosaic.regions.GUI;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import mosaic.regions.RegionsUtils.EnergyFunctionalType;
import mosaic.regions.RegionsUtils.InitializationType;
import mosaic.regions.RegionsUtils.RegularizationType;
import mosaic.regions.DRS.PluginSettingsDRS;
import mosaic.utils.ImgUtils;

/**
 * This class creates GUI for DRS. 
 * It implements logic of GUI like verifying/setting/getting data from GUI. 
 * GUI components are created in a GuiDrsBase. This is quite simple case and that is why none of well
 * known patterns is not followed here.
 * The only "hack" which is used is that it is creating blocking non-modal dialog which is needed for that 
 * particular plugin in ImageJ/Fiji environment (it allows to mark new ROI on input image while blocks execution
 * of plugin until OK/Cancel is clicked).
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class GuiDrs extends GuiDrsBase {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(GuiDrs.class);
    
    // Defines energy types handled by DRS
    // NOTE: order of added energies matter! If changed it must be also done in GUI in energy cards.
    final static ArrayList<EnergyFunctionalType> EnergyTypes = new ArrayList<EnergyFunctionalType>() {
        private static final long serialVersionUID = 1L;

    {
        add(EnergyFunctionalType.e_PC_Gauss);
        add(EnergyFunctionalType.e_PC);
        add(EnergyFunctionalType.e_PS);
    }};

    // input data for GUI + some needed variables
    final PluginSettingsDRS iSettings;
    final ImagePlus iImage;
    boolean iOkPressed = false;
    final Object iBlockingLock = new Object();

    // ----- Constructor and main function used for testing during changes -----
    private GuiDrs() {
        this(new PluginSettingsDRS(), null);
    }
    public static void main(String[] args) {
        GuiDrs dd = new GuiDrs();
        dd.showDialog();
    }
    // -------------------------------------------------------------------------
    
    /**
     * @param aSettings - a input settings for GUI, will be updated according to users choices
     * @param aImage - an input image for plugin, it is used for verification only (in case of ROI init chosen)
     */
    public GuiDrs(PluginSettingsDRS aSettings, ImagePlus aImage) {
        super(EnergyTypes);
        logger.debug("DRS GUI created");
        iSettings = aSettings;
        iImage = aImage;
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Discrete Region Sampling [" + (iImage != null ? iImage.getTitle() : "<no image provided>") + "]");
        
        // Take care about minimum size (add 5% in each direction - window just looks better)
        pack();
        Dimension d = new Dimension(getWidth()*105/100, getHeight()*105/100);
        getContentPane().setPreferredSize(d);
        pack();

        setModal(false);
//        setResizable(false);
        
        // Place window in a middle of screen
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - getHeight()) / 2);
        setLocation(x, y);

        // Update shown data and show window
        transferDataToGui();
        
        // Unlock in case when closed other than via buttons.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                super.windowClosing(ev);
                synchronized(iBlockingLock) {iBlockingLock.notify();}
            }
        });
    }
    
    /**
     * Adds currently opened files to initEnergy list. In case if there is a file which was 
     * previously used as a initialization - it will be automatically selected.
     */
    void addFilesToInitFileChooser() {
        final int[] ids = WindowManager.getIDList();
        if (ids != null) {
            final String[] names = new String[ids.length];
            for (int i = 0; i < ids.length; ++i) {
                names[i] = WindowManager.getImage(ids[i]).getTitle();
            }
            fileInitList.setListData(names);
            
            // Try to set old/saved file as selected if currently opened
            if (iSettings.initFileName != null) {
                int idx = Arrays.binarySearch(names, iSettings.initFileName);
                if (idx < 0) {iSettings.initFileName = null;}
                else {fileInitList.setSelectedIndex(idx);}
            }
        }
    }

    /**
     * Shows window and blocks until closed.
     */
    public void showDialog() {
        setVisible(true);
        try {
            // Trick for blocking execution of with non-modal dialog. It is needed since allows 
            // user to make a ROI selection in input image (if not provided before running plugin).
            // It will be unblocked with OK/Cancel buttons.
            synchronized(iBlockingLock) {iBlockingLock.wait();}
        } catch (InterruptedException e) {}
    }
    
    /**
     * @return true if cancel was clicked
     */
    public boolean wasCanceled() {
        return !iOkPressed;
    }
    
    /**
     * @return true if OK was clicked
     */
    public boolean wasOKed() {
        return iOkPressed;
    }
    
    /**
     * Resets setting to initial state
     */
    @Override
    protected void resetSettings() {
        logger.debug("Settings reseted.");
        iSettings.copy(new PluginSettingsDRS());
        transferDataToGui();
    }

    @Override
    protected void ok() {
        logger.debug("OK clicked");
        if (!getDataFromGui()) return;
        iOkPressed = true;
        dispose();
        synchronized(iBlockingLock) {iBlockingLock.notify();}
    }

    @Override
    protected void cancel() {
        logger.debug("Cancel clicked");
        dispose();
        synchronized(iBlockingLock) {iBlockingLock.notify();}
    }

    /**
     * Sets all GUI values to those from settings
     */
    private void transferDataToGui() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {

                // Energy Data panel
                dataEnergyCombo.setSelectedIndex(EnergyTypes.indexOf(iSettings.energyFunctional));
                psEenergyRadius.setText("" + iSettings.energyPsGaussEnergyRadius);
                psEnergyBetaBalloon.setText("" + iSettings.energyPsBalloonForceCoeff);

                // Regularization energy panel
                lengthEnergyCombo.setSelectedIndex(iSettings.regularizationType.ordinal());
                lambdaRegularization.setText("" + iSettings.energyContourLengthCoeff);
                sphereRegularizationRadiusK.setText("" + iSettings.energyCurvatureMaskRadius);

                // Initialization panel
                initializationCombo.setSelectedIndex(iSettings.initType.ordinal());
                rectangleInitBoxFillRatio.setText("" + iSettings.initBoxRatio);
                bubblesInitRadius.setText("" + iSettings.initBubblesRadius);
                bubblesInitPadding.setText("" + iSettings.initBubblesDisplacement);
                localMaxInitRadius.setText("" + iSettings.initLocalMaxBubblesRadius);
                localMaxInitSigma.setText("" + iSettings.initLocalMaxGaussBlurSigma);
                localMaxInitTolerance.setText("" + iSettings.initLocalMaxTolerance);
                localMaxInitMinRegionSize.setText("" + iSettings.initLocalMaxMinimumRegionSize);

                // Other Settings panel
                numOfIterations.setText("" + iSettings.maxNumOfIterations);
                chckbxFussion.setSelected(iSettings.allowFusion);
                chckbxFission.setSelected(iSettings.allowFission);
                chckbxHandles.setSelected(iSettings.allowHandles);

                offBoundaryProbability.setText("" + iSettings.offBoundarySampleProbability);
                burnInFactor.setText("" + iSettings.burnInFactor);

                chckbxPairProposal.setSelected(iSettings.usePairProposal);
                chckbxBiasedProposal.setSelected(iSettings.useBiasedProposal);

                chckbxShowLabelImage.setSelected(iSettings.showLabelImage);
                chckbxSaveLabelImage.setSelected(iSettings.saveLabelImage);
                chckbxShowProbabilityImage.setSelected(iSettings.showProbabilityImage);
                chckbxSaveProbabilityImage.setSelected(iSettings.saveProbabilityImage);
                
                lblStatus.setText("Have a nice day!");
                
                addFilesToInitFileChooser();
            }
        });
    }
    
    /**
     * Takes data from GUI and updates iSettings. Verifies if data is correct and values are in given range.
     * In case of ROI or File initialization it also checks if file is provided or if ROI is chosen.
     * @return true - if configuration is valid
     */
    private boolean getDataFromGui() {

        try {
            // Energy Data panel
            iSettings.energyFunctional = EnergyTypes.get(dataEnergyCombo.getSelectedIndex());
            iSettings.energyPsGaussEnergyRadius = (int) getValue(lblPsEnergyRadius.getText(), psEenergyRadius.getText(), 1, Double.POSITIVE_INFINITY, true);
            iSettings.energyPsBalloonForceCoeff = (float) getValue(lblPsEnergyBetaEBalloon.getText(), psEnergyBetaBalloon.getText(), 0, Double.POSITIVE_INFINITY, false);

            // Regularization energy panel
            iSettings.regularizationType = RegularizationType.getEnum(lengthEnergyCombo.getSelectedItem().toString());
            iSettings.energyContourLengthCoeff = (float) getValue(lblLambdaRegularization.getText(), lambdaRegularization.getText(), 0, Double.POSITIVE_INFINITY, false);
            iSettings.energyCurvatureMaskRadius = (int) getValue(lblSphereReguralizationRK.getText(), sphereRegularizationRadiusK.getText(), 1, Double.POSITIVE_INFINITY, true);
            
            // Initialization panel
            iSettings.initType = InitializationType.getEnum(initializationCombo.getSelectedItem().toString());
            iSettings.initBoxRatio = getValue(lblBoxFillRatio.getText(), rectangleInitBoxFillRatio.getText(), 0, 1, false);
            iSettings.initBubblesRadius = (int) getValue(lblBubbleRadius.getText(), bubblesInitRadius.getText(), 1, Double.POSITIVE_INFINITY, true);
            iSettings.initBubblesDisplacement = (int) getValue(lblBubblePadding.getText(), bubblesInitPadding.getText(), 1, Double.POSITIVE_INFINITY, true);
            iSettings.initLocalMaxBubblesRadius = (int) getValue(lblMaxInitRadius.getText(), localMaxInitRadius.getText(), 1, Double.POSITIVE_INFINITY, true);
            iSettings.initLocalMaxGaussBlurSigma = getValue(lblMaxInitSigma.getText(), localMaxInitSigma.getText(), 0, Double.POSITIVE_INFINITY, false);
            iSettings.initLocalMaxTolerance = getValue(lblMaxInitTolerance.getText(), localMaxInitTolerance.getText(), 0, 1, false);
            iSettings.initLocalMaxMinimumRegionSize = (int) getValue(lblMaxInitMinimumRegionSize.getText(), localMaxInitMinRegionSize.getText(), 1, Double.POSITIVE_INFINITY, true);

            // Other Settings panel
            iSettings.maxNumOfIterations = (int) getValue(lblNumberOfIterations.getText(), numOfIterations.getText(), 1, Double.POSITIVE_INFINITY, true);
            
            iSettings.allowFusion = chckbxFussion.isSelected();
            iSettings.allowFission = chckbxFission.isSelected();
            iSettings.allowHandles = chckbxHandles.isSelected();
            
            iSettings.offBoundarySampleProbability = (float) getValue(lblOffboundaryProbability.getText(), offBoundaryProbability.getText(), 0, 1, false);
            iSettings.burnInFactor = (float) getValue(lblBurninFactor.getText(), burnInFactor.getText(), 0, 1, false);
            iSettings.usePairProposal = chckbxPairProposal.isSelected();
            iSettings.useBiasedProposal = chckbxBiasedProposal.isSelected();

            iSettings.showLabelImage = chckbxShowLabelImage.isSelected();
            iSettings.saveLabelImage = chckbxSaveLabelImage.isSelected();
            if (iSettings.saveLabelImage) {
                final String directory = ImgUtils.getImageDirectory(iImage);
                if (directory == null) {
                    throw new IllegalArgumentException("\"" + chckbxSaveLabelImage.getText() +"\" - directory for saving is taken from input image, but it is not saved yet");
                }
            }
            
            iSettings.showProbabilityImage = chckbxShowProbabilityImage.isSelected();
            iSettings.saveProbabilityImage = chckbxSaveProbabilityImage.isSelected();
            if (iSettings.saveProbabilityImage) {
                final String directory = ImgUtils.getImageDirectory(iImage);
                if (directory == null) {
                    throw new IllegalArgumentException("\"" + chckbxSaveProbabilityImage.getText() +"\" - directory for saving is taken from input image, but it is not saved yet");
                }
            }
            
            checkRoi();
            checkFile();
        } 
        catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    lblStatus.setText(e.getMessage());
                }
            });
            return false;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                lblStatus.setText("Have a nice day!");
            }
        });
        return true;
    }
    
    /**
     * Checks if ROI is selected in input image.
     */
    private void checkRoi()  throws IllegalArgumentException {
        if (iSettings.initType == InitializationType.ROI_2D) {
            Roi roi = null;
            ImagePlus iChosenInputImage = iImage;
            roi = iChosenInputImage.getRoi();
            if (roi == null) {
                iChosenInputImage.show();
                throw new IllegalArgumentException("Please add ROI region(s) to plugin input image (" + iChosenInputImage.getTitle() + ").");
            }
        }
    }
    
    /**
     * Checks if file is selected from file list.
     */
    private void checkFile() throws IllegalArgumentException {
        if (iSettings.initType == InitializationType.File && fileInitList.isSelectionEmpty()) {
            throw new IllegalArgumentException("Please select file with regions for this type of initialization");
        }
    }
    
    /**
     * @param aFieldName - name of GUI field being processed (for help message)
     * @param aStrValue - string representing number
     * @param aMinValue - minimum expected value or INF if to be skipped
     * @param aMaxValue - maximum expected value or INF if to be skipped
     * @param aIntExpected - should value be integer
     * @return double value of input string
     * @throws IllegalArgumentException - wiht explaining message why getting value failed
     */
    private double getValue(String aFieldName, String aStrValue, double aMinValue, double aMaxValue, boolean aIntExpected)  throws IllegalArgumentException {
        double d = convertToDouble(aStrValue);
        if (Double.isNaN(d)) {
            throw new IllegalArgumentException("Field \"" + aFieldName + "\" has incorrect value [" + aStrValue + "]");
        }
        else if (aIntExpected && !(d == Math.floor(d) && !Double.isInfinite(d))) {
            throw new IllegalArgumentException("Field \"" + aFieldName + "\" should have integer values only but current value is: " + d);
        }
        else if (d < aMinValue || d > aMaxValue) {
            String errorStr = "Field \"" + aFieldName + "\" should be ";
            if (Double.isInfinite(aMaxValue) && !Double.isInfinite(aMinValue)) {
                if (aIntExpected) errorStr += ">=" + (int)aMinValue;
                else errorStr += ">=" + aMinValue;
            }
            else if (!Double.isInfinite(aMaxValue) && Double.isInfinite(aMinValue)) {
                if (aIntExpected) errorStr += "<=" + (int) aMinValue;
                else errorStr += "<=" + aMinValue;
            }
            else {
                if (aIntExpected) errorStr += "in " + (int) aMinValue + "-" + (int) aMaxValue +" range";
                else errorStr += "in " + aMinValue + "-" + aMaxValue +" range";
            }
            if (aIntExpected) errorStr += ", but current value is: " + (int) d;
            else errorStr += ", but current value is: " + d;
            throw new IllegalArgumentException(errorStr);            
        }
        return d;
    }
    
    private double convertToDouble(String aValue) {
        try {
            return Double.parseDouble(aValue);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public ImagePlus getInputLabelImage() {
        if (!fileInitList.isSelectionEmpty()) {
            iSettings.initFileName = fileInitList.getSelectedValue();
            return  WindowManager.getImage(fileInitList.getSelectedValue());
        }
        return null;
    }
    
    @Override
    protected void updateFileInitList() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                addFilesToInitFileChooser();
            }
        });
    }
}
