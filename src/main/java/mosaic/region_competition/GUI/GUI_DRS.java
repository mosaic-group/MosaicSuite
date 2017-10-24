package mosaic.region_competition.GUI;


import java.awt.Button;
import java.awt.Choice;
import java.awt.FileDialog;
import java.awt.Font;
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

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Roi;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.plugins.Region_Competition.InitializationType;
import mosaic.plugins.Region_Competition.RegularizationType;
import mosaic.region_competition.PluginSettingsDRS;

/**
 * TODO: ALL this GUI stuff must be rewritten. It is now too patched and over-complicated.
 */
public class GUI_DRS  {
    private static final Logger logger = Logger.getLogger(GUI_RC.class);
    
    // Input stuff
    final protected PluginSettingsDRS iSettings;
    final private ImagePlus iInputImg;
    
    // Main GUI window variables
    GenericDialog iMainDialogWin;
    protected static final String TextDefaultInputImage = "Input Image: \n\n" + "Drop image here,\n" + "insert Path to file,\n" + "or press Button below";
    protected static final String TextDefaultLabelImage = "Drop Label Image here, or insert Path to file";
    private Choice iInputImageChoice;
    private Choice iLabelImageChoice;
    private boolean iShowNormalized = true;
    
    // Intermediate variables handling image inputs from Choise and TextArea elements
    private String iInputImageFromTextArea;
    private String iInputImageTitleFromChoice;
    private String iInputLabelImageFromTextArea;
    private String iInputLabelImageTitleFromChoice;
    
    // Configuration handling
    boolean isConfigurationValid = false;
    boolean isConfigurationReadAlready = false;
    
    /**
     * Create main GUI of DRS plugin
     */
    public GUI_DRS(PluginSettingsDRS aSettings, ImagePlus aInputImg) {
        iSettings = aSettings;
        iInputImg = aInputImg;
        
        final String[] strings = new String[] { "Normalize_input_image"};
        if (IJ.isMacro() == true) {
            // TODO: Used in cluster mode probalby.. Must be checked if can be converted to regular window.
            logger.info("GUI - macro mode");
            // Must be 'false' since headless mode of Fiji cannot handle this window.
            iMainDialogWin = new GenericDialog("DRS MACRO MODE");
            // in case of script just add two argument for parsing them
            iMainDialogWin.addStringField("text1", "");
            iMainDialogWin.addStringField("text2", "");
            iMainDialogWin.addCheckbox(strings[0], true);
            return;
        }
        
        logger.info("GUI - regular mode");
        
        iMainDialogWin = new CustomDialog("Discrete Region Sampling");
        final Font bf = new Font(null, Font.BOLD, 12);
        
        iMainDialogWin.addMessage("Input and Label image", bf);
        iMainDialogWin.addTextAreas(TextDefaultInputImage, TextDefaultLabelImage, 5, 30);
        new TextAreaListener(this, iMainDialogWin.getTextArea1(), TextDefaultInputImage);
        new TextAreaListener(this, iMainDialogWin.getTextArea2(), TextDefaultLabelImage);

        Panel p = new Panel();
        Button b = new Button("Open Input Image");
        b.addActionListener(new FileOpenerActionListener(iMainDialogWin, iMainDialogWin.getTextArea1()));
        p.add(b);
        b = new Button("Open Label Image");
        b.addActionListener(new FileOpenerActionListener(iMainDialogWin, iMainDialogWin.getTextArea2()));
        p.add(b);
        iMainDialogWin.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));
        addOpenedImageChooser();

        iMainDialogWin.addMessage("Segmentation parameters", bf);
        p = new Panel();
        b = new Button("Parameters");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createParametersDialog();
            }
        });
        p.add(b);
        iMainDialogWin.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));
        
        p = new Panel();
        b = new Button("Reset Settings");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                iSettings.copy(new PluginSettingsDRS());
            }
        });
        p.add(b);
        iMainDialogWin.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));

        iMainDialogWin.addMessage("Output and processing settings", bf);
        final boolean[] bools = new boolean[] { iShowNormalized };
        iMainDialogWin.addCheckboxGroup(1, strings.length, strings, bools);
        
        p = new Panel();
        p.add(new JLabel("<html>Please refer to and cite:<br><br>" + 
                         "J. Cardinale, G. Paul, and I. F. Sbalzarini.<br>" + 
                         "Discrete region competition for unknown numbers of connected regions.<br>" + 
                         "IEEE Trans. Image Process., 21(8):3531–3545, 2012.</html>"));
        iMainDialogWin.addPanel(p);
    }

    /**
     * Create parameters dialog
     */
    protected void createParametersDialog() {
        GenericDialog gd = new GenericDialog("Region Competition Parameters");
        final Font bf = new Font(null, Font.BOLD, 12);
        
        int gridy = 1;
        final int gridx = 2;

        gd.addMessage("Energy and initialization settings", bf);
        // Energy Functional
        final EnergyFunctionalType[] energyValues = EnergyFunctionalType.values();
        final String[] energyItems = new String[energyValues.length];
        for (int i = 0; i < energyValues.length; ++i) {
            energyItems[i] = energyValues[i].name();
        }
        gd.addChoice("E_data", energyItems, iSettings.m_EnergyFunctional.name());
        Choice choiceEnergy = (Choice) gd.getChoices().lastElement();
        {
            Button optionButton = new Button("Options");
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = gridx;
            c.gridy = gridy++;
            c.anchor = GridBagConstraints.EAST;
            gd.add(optionButton, c);

            optionButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String energy = choiceEnergy.getSelectedItem();
                    SettingsBaseGUI energyGUI = EnergyGUI.factory(iSettings, energy);
                    energyGUI.createDialog();
                    energyGUI.showDialog();
                    energyGUI.processDialog();
                }
            });
        }

        // Regularization
        final RegularizationType[] regularizationValues = RegularizationType.values();
        final String[] regularizationItems = new String[regularizationValues.length];
        for (int i = 0; i < regularizationValues.length; ++i) {
            regularizationItems[i] = regularizationValues[i].name();
        }
        gd.addChoice("E_length", regularizationItems, iSettings.regularizationType.name());
        
        Choice choiceRegularization = (Choice) gd.getChoices().lastElement();
        {
            Button optionButton = new Button("Options");
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.EAST;
            c.gridx = gridx;
            c.gridy = gridy++;
            gd.add(optionButton, c);

            optionButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String type = choiceRegularization.getSelectedItem();
                    final SettingsBaseGUI gui = RegularizationGUI.factory(iSettings, type);
                    gui.createDialog();
                    gui.showDialog();
                    gui.processDialog();
                }
            });
        }

        // Label Image Initialization
        final InitializationType[] initTypes = InitializationType.values();
        final String[] initializationItems = new String[initTypes.length];
        for (int i = 0; i < initTypes.length; ++i) {
            initializationItems[i] = initTypes[i].name();
        }
        gd.addChoice("Initialization", initializationItems, iSettings.labelImageInitType.name());
        
        // save reference to this choice, so we can handle it
        Choice initializationChoice = (Choice) gd.getChoices().lastElement();

        Button optionButton = new Button("Options");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridx;
        c.gridy = gridy++;
        c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton, c);

        optionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final String type = initializationChoice.getSelectedItem();
                final SettingsBaseGUI gui = InitializationGUI.factory(iSettings, type);
                gui.createDialog();
                gui.showDialog();
                gui.processDialog();
            }
        });
        
        gd.addMessage("\nGeneral settings", bf);
        gd.addNumericField("Lambda E_length", iSettings.m_EnergyContourLengthCoeff, 4, 8, "");
        gd.addNumericField("Max_Iterations", iSettings.m_MaxNbIterations, 0, 8, "");
        gd.addCheckboxGroup(1, 4, new String[] { "Fusion", "Fission", "Handles" }, new boolean[] { iSettings.m_AllowFusion, iSettings.m_AllowFission, iSettings.m_AllowHandles });

        gd.addNumericField("Burn-in factor [0-1]", iSettings.burnInFactor, 4, 8, "");
        gd.addNumericField("Off-boundary probability [0-1]", iSettings.offBoundarySampleProbability, 4, 8, "");
        gd.addCheckboxGroup(1, 2, new String[] { "biased proposal", "pair proposal" }, new boolean[] { iSettings.useBiasedProposal, iSettings.usePairProposal });
        gd.showDialog();

        // On OK, read parameters
        if (gd.wasOKed()) {
            // Energy Choice
            final String energy = gd.getNextChoice();
            iSettings.m_EnergyFunctional = EnergyFunctionalType.valueOf(energy);
            final EnergyGUI eg = EnergyGUI.factory(iSettings, iSettings.m_EnergyFunctional);
            eg.createDialog();
            eg.processDialog();
            
            // Regularization Choice
            final String regularization = gd.getNextChoice();
            iSettings.regularizationType = RegularizationType.valueOf(regularization);
            iSettings.m_EnergyContourLengthCoeff = (float) gd.getNextNumber();
            iSettings.m_MaxNbIterations = (int) gd.getNextNumber();
            
            // Initialization
            final String initialization = gd.getNextChoice();
            final InitializationType type = InitializationType.valueOf(initialization);
            iSettings.labelImageInitType = type;
            final InitializationGUI ig = InitializationGUI.factory(iSettings, iSettings.labelImageInitType);
            ig.createDialog();
            ig.processDialog();
            
            // Topological constraints
            iSettings.m_AllowFusion = gd.getNextBoolean();
            iSettings.m_AllowFission = gd.getNextBoolean();
            iSettings.m_AllowHandles = gd.getNextBoolean();
            
            // DRS settings
            iSettings.burnInFactor = (float)gd.getNextNumber();
            iSettings.offBoundarySampleProbability = (float)gd.getNextNumber();
            iSettings.useBiasedProposal = gd.getNextBoolean();
            iSettings.usePairProposal = gd.getNextBoolean();
        }
    }
    
    public void showDialog() {
        isConfigurationReadAlready = false;
        iMainDialogWin.showDialog();
        
    }
    public boolean configurationValid() {
        if (!isConfigurationReadAlready) {
            isConfigurationValid = processInput();
        }
        return isConfigurationValid;
    }

    private void addOpenedImageChooser() {
        // Create array of all opened images wiht first default empty ("") entry
        final int[] ids = WindowManager.getIDList();
        int numOpenedImg = (ids != null) ? ids.length : 0;
        final String[] names = new String[numOpenedImg + 1];
        names[0] = "";
        if (ids != null) {
            for (int i = 0; i < numOpenedImg; ++i) {
                names[i + 1] = WindowManager.getImage(ids[i]).getTitle();
            }
        }

        // Input Image Choice - initially empty or if input img given then set to it.
        iMainDialogWin.addChoice("InputImage", names, names[0]);
        iInputImageChoice = (Choice) iMainDialogWin.getChoices().lastElement();
        if (iInputImg != null) {
            iInputImageChoice.select(iInputImg.getTitle());
        }

        // Label Image
        iMainDialogWin.addChoice("LabelImage", names, names[0]);
        iLabelImageChoice = (Choice) iMainDialogWin.getChoices().lastElement();

        // select previously opened image (comparing to input image)
        if (numOpenedImg >= 2 && iInputImg != null) {
            WindowManager.putBehind();
            iLabelImageChoice.select(WindowManager.getCurrentImage().getTitle());
            WindowManager.toFront(iInputImg.getWindow());
        }
    }

    /**
     * Reads out the values from the input mask
     * and saves them into the settings object. <br>
     * Input Processing (valid values)
     */
    protected boolean processInput() {
        if (iMainDialogWin.wasCanceled()) {
            return false;
        }
        
        if (IJ.isMacro() == true) {
            iInputImageFromTextArea = iMainDialogWin.getNextString();
            iInputLabelImageFromTextArea = iMainDialogWin.getNextString();
            iShowNormalized = iMainDialogWin.getNextBoolean();
            return true;
        }

        iInputImageFromTextArea = iMainDialogWin.getTextArea1().getText();
        if (iInputImageFromTextArea == null || iInputImageFromTextArea.isEmpty() || iInputImageFromTextArea.equals(TextDefaultInputImage)) {
            // TODO: set text to [] due to a bug in GenericDialog, if bug gets fixed, this will cause problems!
            iMainDialogWin.getTextArea1().setText("[]");
            iInputImageFromTextArea = null;
        }

        iInputLabelImageFromTextArea = iMainDialogWin.getTextArea2().getText();
        if (iInputLabelImageFromTextArea == null || iInputLabelImageFromTextArea.isEmpty() || iInputLabelImageFromTextArea.equals(TextDefaultLabelImage)) {
            // TODO set text to [] due to a bug in GenericDialog if bug gets fixed, this will cause problems!
            // (cannot macro read boolean after empty text field)
            iMainDialogWin.getTextArea2().setText("[]");
            iInputLabelImageFromTextArea = null;
        }

        // Input/Label Image Choice
        iInputImageTitleFromChoice = iMainDialogWin.getNextChoice();
        iInputLabelImageTitleFromChoice = iMainDialogWin.getNextChoice();
        
        iShowNormalized = iMainDialogWin.getNextBoolean();
        
        return true;
    }

    public ImagePlus getInputImage() {
        final String file = iInputImageFromTextArea;
        ImagePlus inputImage = WindowManager.getImage(iInputImageTitleFromChoice);
        return getImage(file, inputImage, iInputImg);
    }

    public ImagePlus getInputLabelImage() {
        final String fileName = iInputLabelImageFromTextArea;
        final ImagePlus choiceIP = WindowManager.getImage(iInputLabelImageTitleFromChoice);
        return getImage(fileName, choiceIP, /* no default image */ null);
    }
    
    public boolean getNormalize() {
        return iShowNormalized;
    }

    private ImagePlus getImage(final String file, ImagePlus inputImage, ImagePlus aDefault) {
        ImagePlus ip = null;
        
        // 1. Try to read file from text areas (drag & drop)
        if (file != null && !file.isEmpty()) {
            ip = IJ.openImage(file);
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

    void setInputImageChoiceEmpty() {
        if (iInputImageChoice != null) {
            iInputImageChoice.select(0);
        }
    }

    void setLabelImageChoiceEmpty() {
        if (iLabelImageChoice != null) {
            iLabelImageChoice.select(0);
        }
    }

    /**
     * Some extenstions for NonBlockingGenericDialog in order to perform additional checks before user's "OK" is accepted.
     * TODO: This is of course temporary solution - must be changed to something handling user interaction much more intuitively
     */
    private class CustomDialog extends NonBlockingGenericDialog {
        private static final long serialVersionUID = 1L;

        protected CustomDialog(String title) {
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
                  if (!isConfigurationReadAlready) {
                      isConfigurationValid = processInput();
                      isConfigurationReadAlready = true;
                  }
                  if (iSettings.labelImageInitType == InitializationType.ROI_2D) {
                      Roi roi = null;
                      ImagePlus iChosenInputImage = getInputImage();
                      if (iChosenInputImage == null) {
                          IJ.showMessage("Before starting Region Competition plugin please open image with selected ROI(s).");
                          
                          // Because there is no input image and parameters from GenericDialgo cannot be read again then dialog must 
                          // be close. Pretend that "Cancel" button is pressed.
                          isConfigurationValid = false;
                          iMainDialogWin.actionPerformed(new ActionEvent(cancel, 0, "Cancel pressed"));
                          return;
                      }
                      roi = iChosenInputImage.getRoi();
                      if (roi == null) {
                          IJ.showMessage("You have chosen initialization type to ROI.\nPlease add ROI region(s) to plugin input image (" + iChosenInputImage.getTitle() + ").");
                          iChosenInputImage.show();
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
               */
              void itIsOK(ActionEvent e) {
                  ok.removeActionListener(this);
                  ok.addActionListener(iMainDialogWin);
                  iMainDialogWin.actionPerformed(e);
              }
          });
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
        private final GUI_DRS gd;
        
        protected TextAreaListener(GUI_DRS gd, TextArea ta, String defaulftTxt) {
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
                            elem = "";
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
                if (defaultText.equals(GUI_RC.TextDefaultLabelImage)) {
                    gd.setLabelImageChoiceEmpty();
                }
                if (defaultText.equals(GUI_RC.TextDefaultInputImage)) {
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
        
        protected FileOpenerActionListener(GenericDialog aParentDialog, TextArea aAreaForPath) {
            gd = aParentDialog;
            ta = aAreaForPath;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            final FileDialog fd = new FileDialog(gd);
            fd.setVisible(true);
            if (fd.getFile() != null && fd.getDirectory() != null) {
                ta.setText(fd.getDirectory() + fd.getFile());
            }
        }
    }
}

