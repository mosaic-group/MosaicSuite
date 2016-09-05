package mosaic.region_competition.GUI;


import java.awt.Button;
import java.awt.Choice;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.Opener;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.plugins.Region_Competition.InitializationType;
import mosaic.plugins.Region_Competition.RegularizationType;
import mosaic.region_competition.Settings;
import mosaic.region_competition.wizard.RCWWin;

/**
 * TODO: ALL this GUI stuff must be rewritten. It is now too patched and over-complicated.
 */
public class GenericDialogGUI  {

    protected Settings settings;
    
    /**
     * Some extenstions for NonBlockingGenericDialog in order to perform additional checks before user's "OK" is accepted.
     * 
     * TODO: This is of course temporary solution - must be changed to something handling user interaction much more intuitively
     */
    private class CustomDialog extends NonBlockingGenericDialog {
        private static final long serialVersionUID = 1L;

        public CustomDialog(String title) {
            super(title);
        }
        
        /**
         * During additional setup of GenericDialog standard keyListener for OK button is exchanged in order
         * to perform additional checking(s). If everything is OK same ActionEvent is passed to GenericDialog.
         * If something is wrong than proper message is shown to user and depending on problem - dialog is closed or 
         * stays open.
         */
        @Override
        protected void setup() {
          Button[] buttons = getButtons();
          final Button ok = buttons[0]; // first button -> OK
          final Button cancel = buttons[1]; // second button -> Cancel
          ok.removeActionListener(this);
          ok.addActionListener(new ActionListener() {
              
              @Override
              public void actionPerformed(ActionEvent e) {
                  if (!areInputParametersRead) {
                      configurationResult = processInput();
                      areInputParametersRead = true;
                  }
                  if (settings.labelImageInitType == InitializationType.ROI_2D) {
                      Roi roi = null;
                      if (inputImage == null) {
                          IJ.showMessage("Before starting Region Competition plugin please open image with selected ROI(s).");
                          
                          // Because there is no input image and parameters from GenericDialgo cannot be read again then dialog must 
                          // be close. Pretend that "Cancel" button is pressed.
                          configurationResult = false;
                          gd.actionPerformed(new ActionEvent(cancel, 0, "Cancel pressed"));
                          return;
                      }
                      else {
                          roi = inputImage.getRoi();
                      }
                      if (roi == null) {
                          IJ.showMessage("You have chosen initialization type to ROI.\nPlease add ROI region(s) to plugin input image (" + inputImage.getTitle() + ").");
                          inputImage.show();
                          // Generic dialog stays opened. User may select region or/and change initialization and try to click "OK" again.
                      }
                      else {
                          // Everything is fine. ROI is found.
                          itIsOK(e);
                      }
                  }
                  else {
                      // Nothing to do just proxy an event.
                      itIsOK(e);
                  }
              }
              
              /**
               * Change back to standard listener for OK button and passes given (original) ActionEvent.
               * @param e
               */
              void itIsOK(ActionEvent e) {
                  ok.removeActionListener(this);
                  ok.addActionListener(gd);
                  gd.actionPerformed(e);
              }
          });
        }
    }
    
    GenericDialog gd;
    private GenericDialog gd_p;

    private String filenameInput;
    private String filenameLabelImage;

    private String inputImageTitle;
    private String labelImageTitle;

    private boolean showAndSaveStatistics = true;
    private boolean showNormalized = false;
    private boolean keepAllFrames = true; // keep result of last segmentation iteratior?
    private boolean useCluster = false;

    static final String TextDefaultInputImage = "Input Image: \n\n" + "Drop image here,\n" + "insert Path to file,\n" + "or press Button below";
    static final String TextDefaultLabelImage = "Drop Label Image here, or insert Path to file";

    final ImagePlus aImp; // active ImagePlus (argument of Plugin)
    /**
     * Create main plugins dialog
     */
    public GenericDialogGUI(Settings aSettings, ImagePlus aImg) {
        settings = aSettings; // region_Competition.settings;
        aImp = aImg; // region_Competition.getOriginalImPlus();

        
        if (IJ.isMacro() == true) {
            // Must be 'false' since headless mode of Fiji cannot handle this window.
            keepAllFrames = false;
            System.out.println("MACRO MODE");
            gd = new GenericDialog("MACRO MODE");
            // in case of script just add two argument for parsing them
            gd.addStringField("text1", "");
            gd.addStringField("text2", "");
            gd.addCheckbox("Show_and_save_Statistics", true);
            return;
        }
        System.out.println("Regular GUI");
        gd = new CustomDialog("Region Competition");

        gd.addTextAreas(TextDefaultInputImage, TextDefaultLabelImage, 5, 30);

        new TextAreaListener(this, gd.getTextArea1(), TextDefaultInputImage);
        new TextAreaListener(this, gd.getTextArea2(), TextDefaultLabelImage);

        Panel p = new Panel();

        Button b = new Button("Open Input Image");
        b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea1()));
        p.add(b);

        b = new Button("Open Label Image");
        b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea2()));
        p.add(b);

        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));

        p = new Panel();

        b = new Button("Parameters");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CreateParametersDialog();
            }
        });
        p.add(b);

        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));

        addOpenedImageChooser();

        final String[] strings = new String[] { "Keep_Frames", "Show_Normalized", "Show_and_save_Statistics", };

        final boolean[] bools = new boolean[] { keepAllFrames, showNormalized, showAndSaveStatistics, };

        gd.addCheckboxGroup(2, strings.length, strings, bools);

        // Parameter opener Buttons
        p = new Panel();

        b = new Button("Wizard");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final RCWWin w = new RCWWin();
                w.start(settings);
            }
        });
        p.add(b);

        b = new Button("Reset");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                settings.copy(new Settings());
            }
        });
        p.add(b);

        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));

        gd.addCheckbox("Process on computer cluster", false);

        final JLabel labelJ = new JLabel("<html>Please refer to and cite:<br><br>" + "J. Cardinale, G. Paul, and I. F. Sbalzarini. Discrete region competition<br>"
                + " for unknown numbers of connected regions. IEEE Trans.<br>" + " Image Process., 21(8):3531–3545, 2012. " + "</html>");
        p = new Panel();
        p.add(labelJ);
        gd.addPanel(p);
    }

    /**
     * Create parameters dialog
     */
    protected void CreateParametersDialog() {
        gd_p = new GenericDialog("Region Competition Parameters");

        Button optionButton;
        GridBagConstraints c;
        int gridy = 0;
        final int gridx = 2;

        // components:
        final Choice choiceEnergy;
        final Choice choiceRegularization;

        // Energy Functional
        final EnergyFunctionalType[] energyValues = EnergyFunctionalType.values();
        final String[] energyItems = new String[energyValues.length];
        for (int i = 0; i < energyItems.length; i++) {
            energyItems[i] = energyValues[i].name();
        }

        gd_p.addChoice("E_data", energyItems, settings.m_EnergyFunctional.name());
        choiceEnergy = (Choice) gd_p.getChoices().lastElement();
        {
            optionButton = new Button("Options");
            c = new GridBagConstraints();
            c.gridx = gridx;
            c.gridy = gridy++;
            c.anchor = GridBagConstraints.EAST;
            gd_p.add(optionButton, c);

            optionButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    final String energy = choiceEnergy.getSelectedItem();
                    EnergyGUI energyGUI = EnergyGUI.factory(settings, energy);
                    energyGUI.createDialog();
                    energyGUI.showDialog();
                    energyGUI.processDialog();
                }
            });
        }

        // Regularization
        final RegularizationType[] regularizationValues = RegularizationType.values();
        final int n = regularizationValues.length;
        final String[] regularizationItems = new String[n];
        for (int i = 0; i < n; i++) {
            regularizationItems[i] = regularizationValues[i].name();
        }
        gd_p.addChoice("E_length", regularizationItems, settings.regularizationType.name());
        choiceRegularization = (Choice) gd_p.getChoices().lastElement();

        {
            optionButton = new Button("Options");
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.EAST;
            c.gridx = gridx;
            c.gridy = gridy++;
            gd_p.add(optionButton, c);

            optionButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    final String type = choiceRegularization.getSelectedItem();
                    final RegularizationGUI gui = RegularizationGUI.factory(settings, type);
                    gui.createDialog();
                    gui.showDialog();
                    gui.processDialog();

                }
            });
        }

        // Label Image Initialization
        final InitializationType[] initTypes = InitializationType.values();
        final String[] initializationItems = new String[initTypes.length];

        for (int i = 0; i < initializationItems.length; i++) {
            initializationItems[i] = initTypes[i].name();
        }

        // default choice
        final String defaultInit = settings.labelImageInitType.name();

        gd_p.addChoice("Initialization", initializationItems, defaultInit);
        // save reference to this choice, so we can handle it
        initializationChoice = (Choice) gd_p.getChoices().lastElement();

        optionButton = new Button("Options");
        c = new GridBagConstraints();
        c.gridx = gridx;
        c.gridy = gridy++;
        c.anchor = GridBagConstraints.EAST;
        gd_p.add(optionButton, c);

        optionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final String type = initializationChoice.getSelectedItem();
                final InitializationGUI gui = InitializationGUI.factory(settings, type);
                gui.createDialog();
                gui.showDialog();
                gui.processDialog();
            }
        });

        gd_p.addCheckboxGroup(1, 4, new String[] { "Fusion", "Fission", "Handles" }, new boolean[] { settings.m_AllowFusion, settings.m_AllowFission, settings.m_AllowHandles });

        // Numeric Fields
        gd_p.addNumericField("Lambda E_length", settings.m_EnergyContourLengthCoeff, 4);
        gd_p.addNumericField("Theta E_merge", settings.m_RegionMergingThreshold, 4);

        gd_p.addNumericField("Max_Iterations", settings.m_MaxNbIterations, 0);
        gd_p.addNumericField("Oscillation threshold (Convergence)", settings.m_OscillationThreshold, 4);

        gd_p.showDialog();

        // Dialog destroyed
        // On OK, read parameters
        if (gd_p.wasOKed()) {
            processParameters();
        }
    }
    boolean configurationResult = false;
    boolean areInputParametersRead = false;
    
    public void showDialog() {
        areInputParametersRead = false;
        gd.showDialog();
        
    }
    public boolean configurationValid() {
        if (!areInputParametersRead) {
            configurationResult = processInput();
        }
        return configurationResult;
    }
    
    // Choice for open images in IJ
    private int nOpenedImages = 0;
    private Choice choiceLabelImage;
    private Choice choiceInputImage;

    private void addOpenedImageChooser() {
        nOpenedImages = 0;
        final int[] ids = WindowManager.getIDList();
        if (ids != null) nOpenedImages = ids.length;
        final String[] names = new String[nOpenedImages + 1];

        names[0] = "";
        if (ids != null) {
            for (int i = 0; i < nOpenedImages; i++) {
                final ImagePlus ip = WindowManager.getImage(ids[i]);
                names[i + 1] = ip.getTitle();
            }
        }
        
        // if (nOpenedImages>0)
        {
            // Input Image
            gd.addChoice("InputImage", names, names[0]);
            choiceInputImage = (Choice) gd.getChoices().lastElement();
            if (aImp != null) {
                final String title = aImp.getTitle();
                choiceInputImage.select(title);
            }

            // Label Image
            gd.addChoice("LabelImage", names, names[0]);
            choiceLabelImage = (Choice) gd.getChoices().lastElement();

            // select second image
            if (nOpenedImages >= 2 && aImp != null) {
                WindowManager.putBehind();
                final String title = WindowManager.getCurrentImage().getTitle();
                choiceLabelImage.select(title);
                WindowManager.toFront(aImp.getWindow());
            }

            // add listener to change labelImage initialization to file
            choiceLabelImage.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    final Choice choice = (Choice) e.getSource();
                    final int idx = choice.getSelectedIndex();

                    if (idx > 0) {
                        setInitToFileInput();
                    }
                }
            });
        }

    }

    private void readOpenedImageChooser() {
        inputImageTitle = gd.getNextChoice();
        labelImageTitle = gd.getNextChoice();
    }

    /**
     * Reads out the values from the parameters mask
     * and saves them into the settings object. <br>
     * Input Processing (valid values)
     */
    private boolean processParameters() {
        final boolean success = true;

        if (gd_p.wasCanceled()) {
            return false;
        }

        // Energy Choice
        final String energy = gd_p.getNextChoice();
        settings.m_EnergyFunctional = EnergyFunctionalType.valueOf(energy);
        final EnergyGUI eg = EnergyGUI.factory(settings, settings.m_EnergyFunctional);
        eg.createDialog();
        eg.processDialog();

        // Regularization Choice
        final String regularization = gd_p.getNextChoice();
        settings.regularizationType = RegularizationType.valueOf(regularization);

        settings.m_EnergyContourLengthCoeff = (float) gd_p.getNextNumber();
        settings.m_RegionMergingThreshold = (float) gd_p.getNextNumber();
        settings.m_MaxNbIterations = (int) gd_p.getNextNumber();
        settings.m_OscillationThreshold = gd_p.getNextNumber();

        // Initialization
        final String initialization = gd_p.getNextChoice();
        final InitializationType type = InitializationType.valueOf(initialization);
        settings.labelImageInitType = type;
        final InitializationGUI ig = InitializationGUI.factory(settings, settings.labelImageInitType);
        ig.createDialog();
        ig.processDialog();

        // Topological constraints
        settings.m_AllowFusion = gd_p.getNextBoolean();
        settings.m_AllowFission = gd_p.getNextBoolean();
        settings.m_AllowHandles = gd_p.getNextBoolean();

        return success;
    }

    /**
     * Reads out the values from the input mask
     * and saves them into the settings object. <br>
     * Input Processing (valid values)
     */
    public boolean processInput() {

        if (gd.wasCanceled()) {
            return false;
        }
        
        
        if (IJ.isMacro() == true) {
            filenameInput = gd.getNextString();
            inputImage = getInputImageFile();
            filenameLabelImage = gd.getNextString();
            showAndSaveStatistics = gd.getNextBoolean();

            return true;
        }

        // Input Files

        // only record valid inputs

        filenameInput = gd.getTextArea1().getText();
        if (filenameInput == null || filenameInput.isEmpty() || filenameInput.equals(TextDefaultInputImage)) {
            // TODO
            // set text to [] due to a bug in GenericDialog
            // if bug gets fixed, this will cause problems!
            gd.getTextArea1().setText("[]");
            filenameInput = null;
        }

        // IJ Macro

        filenameLabelImage = gd.getTextArea2().getText();
        if (filenameLabelImage == null || filenameLabelImage.isEmpty() || filenameLabelImage.equals(TextDefaultLabelImage)) {
            // TODO IJ BUG
            // set text to [] due to a bug in GenericDialog
            // if bug gets fixed, this will cause problems!
            // (cannot macro read boolean after empty text field)
            if (IJ.isMacro() == false) {
                gd.getTextArea2().setText("[]");
            }
            filenameLabelImage = null;
        }

        readOpenedImageChooser();
        inputImage = getInputImageFile();
        
        keepAllFrames = gd.getNextBoolean();
        showNormalized = gd.getNextBoolean();
        showAndSaveStatistics = gd.getNextBoolean();

        useCluster = gd.getNextBoolean();
        
        return true;
    }
    ImagePlus inputImage;
    /**
     * @return The filepath as String or empty String if no file was chosen.
     */
    public String getLabelImageFilename() {
        return filenameLabelImage;
    }

    public String getInputImageFilename() {
        return filenameInput;
    }

    public boolean useCluster() {
        return useCluster;
    }

    public boolean showAllFrames() {
        return this.keepAllFrames;
    }

    public boolean showAndSaveStatistics() {
        return showAndSaveStatistics;
    }

    public ImagePlus getInputImageFile() {
        final String file = getInputImageFilename();
        ImagePlus inputImage = getInputImageInternal();
        ImagePlus ip = getImage(file, inputImage, aImp);
        
        return ip;
    }
    public ImagePlus getInputImage() {
        return inputImage;
    }
    public ImagePlus getInputLabelImage() {
        final String fileName = getLabelImageFilename();
        final ImagePlus choiceIP = getLabelImageInternal();
        
        ImagePlus ip = getImage(fileName, choiceIP, /* no default image */ null);
        
        return ip;
        
    }
    
    private ImagePlus getImage(final String file, ImagePlus inputImage, ImagePlus aDefault) {
        ImagePlus ip = null;
        
        // 1. Try to read file from text areas (drag & drop)
        if (file != null && !file.isEmpty()) {
            final Opener o = new Opener();
            ip = o.openImage(file);
            if (ip != null) {
                final FileInfo fi = ip.getFileInfo();
                fi.directory = file.substring(0, file.lastIndexOf(File.separator));
                ip.setFileInfo(fi);
            }
        }
        
        // 2. If still null then try to get image from choice/comboboxes  
        if (ip == null) {
            ip = inputImage;
        }

        // 3. Still null, just use given default image
        if (ip == null) {
            ip = aDefault;
        }

        return ip;
    }
    ///////////////////////////

    protected Choice initializationChoice; // reference to the awt.Choice for initialization

    void setInitToFileInput() {
        // TODO: I have no idea what is going on here. It is called from main window
        //       but it is trying to set sth in parameters window which is not created. Just adding
        //       check for null 
        if (initializationChoice != null) initializationChoice.select(InitializationType.File.name());
    }

    void setInputImageChoiceEmpty() {
        if (choiceInputImage != null) {
            choiceInputImage.select(0);
        }
    }

    void setLabelImageChoiceEmpty() {
        if (choiceLabelImage != null) {
            choiceLabelImage.select(0);
        }
    }

    /**
     * @return Reference to the input image. Type depends on Implementation.
     */
    public ImagePlus getInputImageInternal() {
        return WindowManager.getImage(inputImageTitle);
    }

    public ImagePlus getLabelImageInternal() {
        return WindowManager.getImage(labelImageTitle);
    }
}

/**
 * DropTargetListener for TextArea, so one can drag&drop a File into the textArea
 * and the file source gets written in.
 * Usage: Just create a new instance,
 * with TextArea and DefaultText (Text shown in TextArea) in constructor
 */
class TextAreaListener implements DropTargetListener, TextListener, FocusListener
{
    private final TextArea textArea;
    private final String defaultText;
    private final GenericDialogGUI gd;

    protected TextAreaListener(GenericDialogGUI gd, TextArea ta, String defaulftTxt) {
        this.gd = gd;
        this.textArea = ta;
        this.defaultText = defaulftTxt;
        new DropTarget(ta, this);
        ta.addTextListener(this);
        ta.addFocusListener(this);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {}

    @Override
    public void drop(DropTargetDropEvent event) {
        boolean failed = true;
        boolean done = false;
        String filename;

        // Accept copy drops
        event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

        // Get the transfer which can provide the dropped item data
        final Transferable transferable = event.getTransferable();

        // Get the data formats of the dropped item
        final DataFlavor[] flavors = transferable.getTransferDataFlavors();

        // Loop through the flavors
        for (final DataFlavor flavor : flavors) {

            try {
                // If the drop items are files
                if (flavor.isFlavorJavaFileListType() && !done) {
                    // Get all of the dropped files
                    
                    // To transfer a list of files to/from Java (and the underlying platform) a DataFlavor of this type/subtype 
                    // and representation class of java.util.List is used. Each element of the list is required/guaranteed t
                    // o be of type java.io.File
                    // Explicitly ignoring warning and casting to List<File>
                    @SuppressWarnings("unchecked")
                    final List<File> files = (List<File>) transferable.getTransferData(flavor);
                    for (final File file : files) {
                        filename = file.getPath();
                        textArea.setText(filename);
                        textArea.validate();

                        failed = false;
                        done = true;
                    }
                }
                else if (flavor.isRepresentationClassInputStream() && !done) {
                    final JTextArea ta = new JTextArea();
                    ta.read(new InputStreamReader((InputStream) transferable.getTransferData(flavor)), "from system clipboard");

                    final String dndString = ta.getText().trim();
                    final StringTokenizer tokenizer = new StringTokenizer(dndString);
                    String elem = "";
                    while (tokenizer.hasMoreElements()) {
                        elem = tokenizer.nextToken();
                        if (elem.startsWith("file")) {
                            break;
                        }
                        else {
                            elem = "";
                        }
                    }
                    textArea.setText(elem);
                    ta.setText(null);
                    break;
                }

            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }

        if (failed == true) {
            // This is for Linux, ( and maybe OSX )
            String data = null;

            DataFlavor nixFileDataFlavor;
            try {
                nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
                data = (String) transferable.getTransferData(nixFileDataFlavor);
            }
            catch (final ClassNotFoundException e) {
                e.printStackTrace();
            }
            catch (final UnsupportedFlavorException e) {
                e.printStackTrace();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }

            for (final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
                final String token = st.nextToken().trim();
                if (token.startsWith("#") || token.isEmpty()) {
                    // comment line, by RFC 2483
                    continue;
                }

                textArea.setText(token);
            }
        }

        // Inform that the drop is complete
        event.dropComplete(true);
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {}

    @Override
    public void dragExit(DropTargetEvent dte) {}

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {}

    @Override
    public void textValueChanged(TextEvent e) {
        // Change input choice to file if text in textfield was changed explicitly

        final String text = textArea.getText();
        if (text.isEmpty() || text.equals(defaultText)) {
            // changed to default, do nothing
        }
        else {
            // there was a non-default change in the textfield.
            // set input choice to file if it was TextArea for labelImage
            if (defaultText.equals(GenericDialogGUI.TextDefaultLabelImage)) {
                gd.setInitToFileInput();
                gd.setLabelImageChoiceEmpty();
            }
            if (defaultText.equals(GenericDialogGUI.TextDefaultInputImage)) {
                gd.setInputImageChoiceEmpty();
            }
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (textArea.getText().equals(defaultText)) {
            // delete defaultText on focus gain to allow input
            textArea.setText("");
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (textArea.getText().isEmpty()) {
            // there was no input. recover default text on focus lost
            textArea.setText(defaultText);
        }
    }
}

/**
 * Opens a FileDialog and writes the path of the file into the TextArea
 * (so GenericDialog can read from it and do the Macro parsing) <br>
 * If FileDialog is canceled, TextArea is not changed.
 */
class FileOpenerActionListener implements ActionListener {

    private final GenericDialog gd;
    private final TextArea ta;

    protected FileOpenerActionListener(GenericDialog gd, TextArea ta) {
        this.gd = gd;
        this.ta = ta;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final FileDialog fd = new FileDialog(gd);
        fd.setVisible(true);
        final String dir = fd.getDirectory();
        final String file = fd.getFile();

        if (file != null && dir != null) {
            ta.setText(dir + file);
        }
    }
}
