package mosaic.region_competition.GUI;


import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;

import java.awt.Button;
import java.awt.Choice;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
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
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import mosaic.plugins.Region_Competition;
import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.energies.RegularizationType;
import mosaic.region_competition.initializers.InitializationType;
import mosaic.region_competition.wizard.RCWWin;


/**
 * Adapts GenericDialog from ImageJ for our purposes
 */
public class GenericDialogGUI implements InputReadable {

    Settings settings;
    private GenericDialog gd;
    private GenericDialog gd_p;
    private ImagePlus aImp; // active ImagePlus (argument of Plugin)

    private String filenameInput; // image to be processed
    private String filenameLabelImage; // initialization

    private String inputImageTitle;
    private String labelImageTitle;

    private int kbest = 1;
    private boolean showAndSaveStatistics = true;
    private boolean showNormalized = false;
    private boolean useStack = true;
    public boolean keepAllFrames = true; // keep result of last segmentation iteratior?
    // private boolean useRegularization;
    private boolean useCluster = false;

    static final String EnergyFunctional = "E_data";
    EnergyGUI energyGUI;
    static final String Regularization = "E_length";
    RegularizationGUI regularizationGUI;

    private static final String Initialization = "Initialization";

    static final String TextDefaultInputImage = "Input Image: \n\n" + "Drop image here,\n" + "insert Path to file,\n" + "or press Button below";
    static final String TextDefaultLabelImage = "Drop Label Image here, or insert Path to file";

    static final String emptyOpenedImage = "";

    /**
     * Create main plugins dialog
     */

    public GenericDialogGUI(Settings aSettings, ImagePlus aImg) {
        settings = aSettings; // region_Competition.settings;
        aImp = aImg; // region_Competition.getOriginalImPlus();

        if (IJ.isMacro() == true) {
            // Create a window with text1 and text2

            gd = new GenericDialog("Region Competition");

            // in case of script just add two argument for parsing them

            gd.addStringField("text1", "");
            gd.addStringField("text2", "");

            gd.addCheckbox("Show_and_save_Statistics", true);

            return;
        }

        gd = new NonBlockingGenericDialog("Region Competition");

        // File path text areas

        gd.addTextAreas(TextDefaultInputImage, TextDefaultLabelImage, 5, 30);

        new TextAreaListener(this, gd.getTextArea1(), TextDefaultInputImage);
        new TextAreaListener(this, gd.getTextArea2(), TextDefaultLabelImage);

        // File opener Buttons

        Panel p = new Panel();

        // if script button are pointless

        Button b = new Button("Open Input Image");
        b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea1()));
        p.add(b);

        b = new Button("Open Label Image");
        b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea2()));
        p.add(b);

        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));

        // Parameter opener Buttons

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

        gd.addNumericField("kbest", 0, 0);

        String[] strings = new String[] { "Show_Progress", "Keep_Frames", "Show_Normalized", "Show_and_save_Statistics", };

        boolean[] bools = new boolean[] { useStack, keepAllFrames, showNormalized, showAndSaveStatistics, };

        gd.addCheckboxGroup(2, strings.length, strings, bools);

        // Parameter opener Buttons

        p = new Panel();

        b = new Button("Wizard");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                RCWWin w = new RCWWin();
                w.start(settings);
            }
        });
        p.add(b);

        b = new Button("Reset");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                settings = new Settings();
                Region_Competition.getConfigHandler().SaveToFile("/tmp/rc_settings.dat", settings);
            }
        });
        p.add(b);

        gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));

        gd.addCheckbox("Process on computer cluster", false);

        // Introduce a label with reference

        JLabel labelJ = new JLabel("<html>Please refer to and cite:<br><br>" + "J. Cardinale, G. Paul, and I. F. Sbalzarini. Discrete region competition<br>"
                + " for unknown numbers of connected regions. IEEE Trans.<br>" + " Image Process., 21(8):3531–3545, 2012. " + "</html>");
        p = new Panel();
        p.add(labelJ);
        gd.addPanel(p);

        addWheelListeners();
    }

    /**
     * Create parameters dialog
     */

    void CreateParametersDialog() {
        gd_p = new GenericDialog("Region Competition Parameters");

        Button optionButton;
        GridBagConstraints c;
        int gridy = 0;
        int gridx = 2;

        // components:
        final Choice choiceEnergy;
        final Choice choiceRegularization;

        // Energy Functional
        EnergyFunctionalType[] energyValues = EnergyFunctionalType.values();
        String[] energyItems = new String[energyValues.length];
        for (int i = 0; i < energyItems.length; i++) {
            energyItems[i] = energyValues[i].name();
        }

        gd_p.addChoice(EnergyFunctional, energyItems, settings.m_EnergyFunctional.name());
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
                    String energy = choiceEnergy.getSelectedItem();
                    energyGUI = EnergyGUI.factory(settings, energy);
                    energyGUI.createDialog();
                    energyGUI.showDialog();
                    energyGUI.processDialog();
                }
            });
        }

        // Regularization

        RegularizationType[] regularizationValues = RegularizationType.values();
        int n = regularizationValues.length;
        String[] regularizationItems = new String[n];
        for (int i = 0; i < n; i++) {
            regularizationItems[i] = regularizationValues[i].name();
        }
        gd_p.addChoice(Regularization, regularizationItems, settings.regularizationType.name());
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
                    String type = choiceRegularization.getSelectedItem();
                    RegularizationGUI gui = RegularizationGUI.factory(settings, type);
                    gui.createDialog();
                    gui.showDialog();
                    gui.processDialog();

                }
            });
        }

        // Label Image Initialization

        InitializationType[] initTypes = InitializationType.values();
        String[] initializationItems = new String[initTypes.length];

        for (int i = 0; i < initializationItems.length; i++) {
            initializationItems[i] = initTypes[i].name();
        }

        // default choice
        String defaultInit = settings.labelImageInitType.name();

        gd_p.addChoice(Initialization, initializationItems, defaultInit);
        // save reference to this choice, so we can handle it
        initializationChoice = (Choice) gd_p.getChoices().lastElement();
        // lastInitChoice=initializationChoice.getSelectedItem();

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
                    String type = initializationChoice.getSelectedItem();
                    InitializationGUI gui = InitializationGUI.factory(settings, type);
                    gui.createDialog();
                    gui.showDialog();
                    gui.processDialog();
                }
            });
        }

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

    @Override
    public void showDialog() {
        gd.showDialog();
    }

    // Choice for open images in IJ

    private int nOpenedImages = 0;
    Choice choiceLabelImage;
    Choice choiceInputImage;

    private void addOpenedImageChooser() {
        nOpenedImages = 0;
        int[] ids = WindowManager.getIDList();

        String[] names = new String[nOpenedImages + 1];
        names[0] = emptyOpenedImage;
        if (ids != null) {
            nOpenedImages = ids.length;
            for (int i = 0; i < nOpenedImages; i++) {
                ImagePlus ip = WindowManager.getImage(ids[i]);
                names[i + 1] = ip.getTitle();
            }
        }


        // if (nOpenedImages>0)
        {
            // Input Image
            gd.addChoice("InputImage", names, names[0]);
            choiceInputImage = (Choice) gd.getChoices().lastElement();
            if (aImp != null) {
                String title = aImp.getTitle();
                choiceInputImage.select(title);
            }

            // Label Image
            gd.addChoice("LabelImage", names, names[0]);
            choiceLabelImage = (Choice) gd.getChoices().lastElement();

            // select second image
            if (nOpenedImages >= 2 && aImp != null) {
                WindowManager.putBehind();
                String title = WindowManager.getCurrentImage().getTitle();
                choiceLabelImage.select(title);
                WindowManager.toFront(aImp.getWindow());
            }

            // add listener to change labelImage initialization to file
            choiceLabelImage.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    Choice choice = (Choice) e.getSource();
                    int idx = choice.getSelectedIndex();

                    if (idx > 0) {
                        setInitToFileInput();
                    }
                }
            });
        }

    }

    private void readOpenedImageChooser() {
        // if (nOpenedImages>0)
        inputImageTitle = gd.getNextChoice();
        labelImageTitle = gd.getNextChoice();
    }

    /**
     * Adds MouseWheelListeners to each NumericField in GenericDialog
     */
    private void addWheelListeners() {
        @SuppressWarnings("unchecked")
        Vector<TextField> v = gd.getNumericFields();
        for (TextField tf : v) {
            tf.addMouseWheelListener(new NumericFieldWheelListener(tf));
        }
    }

    /**
     * Reads out the values from the parameters mask
     * and saves them into the settings object. <br>
     * Input Processing (valid values)
     */

    private boolean processParameters() {
        boolean success = true;

        if (gd_p.wasCanceled()) {
            return false;
        }

        // Energy Choice

        String energy = gd_p.getNextChoice();
        settings.m_EnergyFunctional = EnergyFunctionalType.valueOf(energy);
        EnergyGUI eg = EnergyGUI.factory(settings, settings.m_EnergyFunctional);
        eg.createDialog();
        eg.processDialog();

        // Regularization Choice

        String regularization = gd_p.getNextChoice();
        settings.regularizationType = RegularizationType.valueOf(regularization);

        settings.m_EnergyContourLengthCoeff = (float) gd_p.getNextNumber();
        settings.m_RegionMergingThreshold = (float) gd_p.getNextNumber();
        settings.m_MaxNbIterations = (int) gd_p.getNextNumber();
        settings.m_OscillationThreshold = gd_p.getNextNumber();

        // Initialization
        String initialization = gd_p.getNextChoice();
        InitializationType type = InitializationType.valueOf(initialization);
        settings.labelImageInitType = type;
        InitializationGUI ig = InitializationGUI.factory(settings, settings.labelImageInitType);
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
    @Override
    public boolean processInput() {

        if (gd.wasCanceled()) {
            return false;
        }

        boolean success = true;

        if (IJ.isMacro() == true) {
            filenameInput = gd.getNextString();
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
        }
        // else
        // {
        // String s = filenameInput.replace('\\', '/');
        // filenameInput = s;
        // }

        // IJ Macro

        filenameLabelImage = gd.getTextArea2().getText();
        if (filenameLabelImage == null || filenameLabelImage.isEmpty() || filenameLabelImage.equals(TextDefaultLabelImage)) {
            // TODO IJ BUG
            // set text to [] due to a bug in GenericDialog
            // (cannot macro read boolean after empty text field)
            // if bug gets fixed, this will cause problems!
            if (IJ.isMacro() == false) {
                gd.getTextArea2().setText("[]");
            }
        }
        // else
        // {
        // String s = filenameLabelImage.replace('\\', '/');
        // filenameLabelImage = s;
        // }

        // TODO IJ BUG
        if (filenameLabelImage.equals("[]")) {
            filenameLabelImage = "";
        }

        readOpenedImageChooser();
        kbest = (int) gd.getNextNumber();

        useStack = gd.getNextBoolean();
        keepAllFrames = gd.getNextBoolean();
        showNormalized = gd.getNextBoolean();
        showAndSaveStatistics = gd.getNextBoolean();

        useCluster = gd.getNextBoolean();

        return success;
    }

    @Override
    public InitializationType getLabelImageInitType() {
        return settings.labelImageInitType;
    }

    @Override
    public String getLabelImageFilename() {
        return filenameLabelImage;
    }

    @Override
    public String getInputImageFilename() {
        return filenameInput;
    }

    @Override
    public boolean useCluster() {
        return useCluster;
    }

    @Override
    public boolean showAllFrames() {
        return this.keepAllFrames;
    }

    @Override
    public boolean showAndSaveStatistics() {
        return showAndSaveStatistics;
    }

    @Override
    public int getKBest() {
        return kbest;
    }

    @Override
    public int getNumIterations() {
        return settings.m_MaxNbIterations;
    }

    // //////////////////////////

    Choice initializationChoice; // reference to the awt.Choice for initialization

    // private String lastInitChoice; // choice before File_choice was set automatically

    void setInitToFileInput() {
        // lastInitChoice=initializationChoice.getSelectedItem();
        initializationChoice.select(InitializationType.File.name());
    }

    // void setInitToLastChoice()
    // {
    // System.out.println("change to last="+lastInitChoice);
    // initializationChoice.select(lastInitChoice);
    // }

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

    @Override
    public ImagePlus getInputImage() {
        return WindowManager.getImage(inputImageTitle);
    }

    @Override
    public ImagePlus getLabelImage() {
        return WindowManager.getImage(labelImageTitle);
    }

}

/**
 * DropTargetListener for TextArea, so one can drag&drop a File into the textArea
 * and the file source gets written in.
 * Usage: Just create a new instance,
 * with TextArea and DefaultText (Text shown in TextArea) in constructor
 */
class TextAreaListener implements DropTargetListener, TextListener, FocusListener // , MouseListener
{

    TextArea textArea;
    String defaultText;
    GenericDialogGUI gd;

    public TextAreaListener(GenericDialogGUI gd, TextArea ta, String defaulftTxt) {
        this.gd = gd;
        this.textArea = ta;
        this.defaultText = defaulftTxt;
        new DropTarget(ta, this);
        ta.addTextListener(this);
        ta.addFocusListener(this);
        // ta.addMouseListener(this);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void drop(DropTargetDropEvent event) {
        boolean failed = true;
        String filename;

        // Accept copy drops
        event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

        // Get the transfer which can provide the dropped item data
        Transferable transferable = event.getTransferable();

        // Get the data formats of the dropped item
        DataFlavor[] flavors = transferable.getTransferDataFlavors();

        // Loop through the flavors
        for (DataFlavor flavor : flavors) {

            try {
                // If the drop items are files
                if (flavor.isFlavorJavaFileListType()) {
                    // Get all of the dropped files
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) transferable.getTransferData(flavor);
                    // Loop them through
                    for (File file : files) {
                        filename = file.getPath();
                        textArea.setText(filename);

                        // IJ.open(filename);

                        // Print out the file path
                        System.out.println("File path is '" + filename + "'.");
                        failed = false;
                    }
                }
                else if (flavor.isRepresentationClassInputStream()) {
                    JTextArea ta = new JTextArea();
                    ta.read(new InputStreamReader((InputStream) transferable.getTransferData(flavor)), "from system clipboard");

                    String dndString = ta.getText().trim();
                    StringTokenizer tokenizer = new StringTokenizer(dndString);
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
            catch (Exception e) {
                // Print out the error stack
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
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
                String token = st.nextToken().trim();
                if (token.startsWith("#") || token.isEmpty()) {
                    // comment line, by RFC 2483
                    continue;
                }

                textArea.setText(token);

                // Print out the file path
                System.out.println("File path is '" + token + "'.");
            }
        }

        // Inform that the drop is complete
        event.dropComplete(true);
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        System.out.println("dragOver " + dtde);
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        System.out.println("dragExit " + dte);
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        System.out.println("dragEnter " + dtde);
    }

    // TextListener

    @Override
    public void textValueChanged(TextEvent e) {
        // Change input choice to file if text in textfield was changed explicitly

        String text = textArea.getText();
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
        // System.out.println("tf changed to: "+textArea.getText());
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (textArea.getText().equals(defaultText)) {
            // delete defaultText on focus gain to allow input
            textArea.setText("");
        }
        else {
            // do nothing if text was edited
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (textArea.getText().isEmpty()) {
            // there was no input. recover default text on focus lost
            textArea.setText(defaultText);
        }
        else {
            // do nothing if text was edited
        }
    }

}

/**
 * Opens a FileDialog and writes the path of the file into the TextArea
 * (so GenericDialog can read from it and do the Macro parsing) <br>
 * If FileDialog is canceled, TextArea is not changed.
 */
class FileOpenerActionListener implements ActionListener {

    GenericDialog gd;
    TextArea ta;

    public FileOpenerActionListener(GenericDialog gd, TextArea ta) {
        this.gd = gd;
        this.ta = ta;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FileDialog fd = new FileDialog(gd);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();

        if (file != null && dir != null) {
            ta.setText(dir + file);
        }
    }
}

/**
 * MouseWheelListener for NumericField <br>
 * Value in NumericField grows/shrinks by factor fac per MouseWheel "click"
 * Field is assumed to be floating point.
 */
class NumericFieldWheelListener implements MouseWheelListener {

    double fac = 0.1;
    TextField tf;

    public NumericFieldWheelListener(TextField tf) {
        this.tf = tf;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int n = e.getWheelRotation();

        boolean inc = false;
        double f = 1.0;
        if (n > 0) {
            f = 1 - fac;
        }
        else {
            f = 1 + fac;
            inc = true;
        }

        n = Math.abs(n);

        double val = Double.valueOf(tf.getText());

        boolean isInteger = false;
        if (val == Math.floor(val)) {
            isInteger = true;
        }

        System.out.println(val);
        for (int i = 0; i < n; i++) {
            val *= f;
        }
        if (isInteger && !inc) {
            val = Math.floor(val);
        }
        if (isInteger && inc) {
            val = Math.ceil(val);
        }
        if (inc && val == 0) {
            val = 1;
        }
        tf.setText(Double.toString(val));
        System.out.println("wheeee " + val);
    }

}
