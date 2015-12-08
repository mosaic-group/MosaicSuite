package mosaic.ia.gui;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.vecmath.Point3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Macro_Runner;
import ij.process.ImageProcessor;
import mosaic.ia.Analysis;
import mosaic.ia.Analysis.Result;
import mosaic.ia.Potential;
import mosaic.ia.Potential.PotentialType;
import mosaic.ia.utils.FileUtils;

public class InteractionAnalysisGUI implements ActionListener {

    private Analysis iAnalysis;
    
    private JButton help;

    private JButton browseY, browseX;
    private ImagePlus imgx, imgy;
    private JButton btnLoadCsvFileX, btnLoadCsvFileY;
    private Point3d[] Xcoords, Ycoords;
    private JFormattedTextField txtXmin, txtYmin, txtZmin, txtXmax,txtYmax, txtZmax;
    private JButton genMask, loadMask, resetMask;
    private ImagePlus genMaskIP;

    private JFormattedTextField gridSizeInp;
    private JFormattedTextField  kernelWeightq;
    private JFormattedTextField  kernelWeightp;
    private JButton btnCalculateDistances;
    
    private final String[] items = { "Hernquist", "Step", "Linear type 1", "Linear type 2", "Plummer", "Non-parametric" };
    private JComboBox<String> potentialComboBox;
    private PotentialType potentialType = PotentialType.HERNQUIST;
    private JFormattedTextField reRuns;
    private JFormattedTextField numSupport, smoothnessNP;
    private JButton estimate;
    
    private JFormattedTextField mCRuns;
    private JFormattedTextField alphaField;
    private JButton test;
    
    private JTabbedPane tabbedPane;
    private JLabel lblsupportPts, lblSmoothness;
    
    /**
     * Create the application.
     */
    public InteractionAnalysisGUI() {
        JFrame frmInteractionAnalysis = new JFrame("Interaction Analysis");
        Border blackBorder = BorderFactory.createLineBorder(Color.black);
        help = new JButton("help");
        help.addActionListener(this);
        
        final JPanel panel_help = new JPanel();
        final JPanel panel_5 = new JPanel();
        panel_5.setBorder(blackBorder);
        final JPanel panel_7 = new JPanel();
        panel_7.setBorder(blackBorder);
        final JPanel panel_6 = new JPanel();
        panel_6.setBorder(blackBorder);
        JTextArea textArea = new JTextArea("Please refer to and cite: J. A. Helmuth, G. Paul, and I. F. Sbalzarini.\n" + "Beyond co-localization: inferring spatial interactions between sub-cellular \n"
                + "structures from microscopy images. BMC Bioinformatics, 11:372, 2010.\n\n" + "A. Shivanandan, A. Radenovic, and I. F. Sbalzarini. MosaicIA: an ImageJ/Fiji\n"
                + "plugin for spatial pattern and interaction analysis. BMC Bioinformatics, \n" + "14:349, 2013. ");
        
        textArea.setBackground(UIManager.getColor("Button.background"));
        
        final GroupLayout gl_panel_help = new GroupLayout(panel_help);
        gl_panel_help.setHorizontalGroup(gl_panel_help.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_help.createSequentialGroup().addGap(198).addComponent(help, GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE).addGap(198)));
        
        gl_panel_help.setVerticalGroup(gl_panel_help.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_help.createSequentialGroup().addComponent(help, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap(9, Short.MAX_VALUE)));
        panel_help.setLayout(gl_panel_help);
        
        final GroupLayout gl_panel_6 = new GroupLayout(panel_6);
        gl_panel_6.setHorizontalGroup(gl_panel_6.createParallelGroup(Alignment.LEADING).addComponent(textArea, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE));
        gl_panel_6.setVerticalGroup(gl_panel_6.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_6.createSequentialGroup().addComponent(textArea, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap(9, Short.MAX_VALUE)));
        panel_6.setLayout(gl_panel_6);
        
        final JLabel lblHypothesisTesting = new JLabel("Hypothesis testing");
        
        final JLabel lblMonteCarloRuns = new JLabel("Monte carlo runs");
        
        mCRuns = new JFormattedTextField();
        mCRuns.setHorizontalAlignment(SwingConstants.CENTER);
        mCRuns.setText("1000");
        mCRuns.setColumns(10);
        
        final JLabel lblSignificanceLevel = new JLabel("Significance level");
        
        alphaField = new JFormattedTextField();
        alphaField.setHorizontalAlignment(SwingConstants.CENTER);
        alphaField.setText("0.05");
        alphaField.setColumns(10);
        alphaField.addActionListener(this);
        
        test = new JButton("Test");
        final GroupLayout gl_panel_7 = new GroupLayout(panel_7);
        gl_panel_7.setHorizontalGroup(gl_panel_7
                .createParallelGroup(Alignment.TRAILING)
                .addGroup(
                        gl_panel_7
                                .createSequentialGroup()
                                .addGap(24)
                                .addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING).addComponent(lblMonteCarloRuns).addComponent(lblSignificanceLevel))
                                .addGroup(
                                        gl_panel_7
                                                .createParallelGroup(Alignment.TRAILING)
                                                .addGroup(gl_panel_7.createSequentialGroup().addGap(21).addComponent(lblHypothesisTesting).addContainerGap(252, Short.MAX_VALUE))
                                                .addGroup(
                                                        gl_panel_7
                                                                .createSequentialGroup()
                                                                .addGap(175)
                                                                .addGroup(
                                                                        gl_panel_7.createParallelGroup(Alignment.TRAILING, false).addComponent(mCRuns, Alignment.LEADING)
                                                                                .addComponent(alphaField, Alignment.LEADING)).addGap(23))))
                .addGroup(gl_panel_7.createSequentialGroup().addContainerGap(244, Short.MAX_VALUE).addComponent(test).addGap(233)));
        gl_panel_7.setVerticalGroup(gl_panel_7.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_7
                        .createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblHypothesisTesting)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE).addComponent(mCRuns, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE).addComponent(lblMonteCarloRuns))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                                gl_panel_7.createParallelGroup(Alignment.BASELINE).addComponent(lblSignificanceLevel)
                                        .addComponent(alphaField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(ComponentPlacement.RELATED, 11, Short.MAX_VALUE).addComponent(test).addContainerGap()));
        panel_7.setLayout(gl_panel_7);
        
        final JLabel lblPotentialEstimation = new JLabel("Potential estimation");
        
        potentialComboBox = new JComboBox<String>(items);
        potentialComboBox.addActionListener(this);
        
        estimate = new JButton("Estimate");
        estimate.setActionCommand("Estimate");
        
        final JLabel lblPotentialShape = new JLabel("Potential:");
        
        numSupport = new JFormattedTextField();
        
        numSupport.setHorizontalAlignment(SwingConstants.CENTER);
        
        numSupport.setText("" + Potential.NONPARAM_WEIGHT_SIZE);
        numSupport.setColumns(10);
        numSupport.setEnabled(false);
        numSupport.addActionListener(this);
        
        smoothnessNP = new JFormattedTextField();
        
        smoothnessNP.setHorizontalAlignment(SwingConstants.CENTER);
        
        smoothnessNP.setText("" + Potential.NONPARAM_SMOOTHNESS);
        smoothnessNP.setColumns(10);
        smoothnessNP.setEnabled(false);
        smoothnessNP.addActionListener(this);
        
        lblsupportPts = new JLabel("#Support pts:");
        lblsupportPts.setEnabled(false);
        
        lblSmoothness = new JLabel("Smoothness:");
        lblSmoothness.setEnabled(false);
        
        final JLabel lblRepeatEstimation = new JLabel("Repeat estimation:");
        
        reRuns = new JFormattedTextField();
        reRuns.setColumns(10);
        reRuns.setHorizontalAlignment(SwingConstants.CENTER);
        reRuns.setText("10");
        reRuns.addActionListener(this);
        
        final GroupLayout gl_panel_5 = new GroupLayout(panel_5);
        gl_panel_5.setHorizontalGroup(gl_panel_5
                .createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_panel_5.createSequentialGroup().addGap(107).addComponent(estimate).addContainerGap(92, Short.MAX_VALUE))
                .addGroup(gl_panel_5.createSequentialGroup().addGap(161).addComponent(lblPotentialEstimation).addContainerGap(133, Short.MAX_VALUE))
                .addGroup(
                        gl_panel_5
                                .createSequentialGroup()
                                .addGroup(
                                        gl_panel_5
                                                .createParallelGroup(Alignment.LEADING)
                                                .addGroup(
                                                        gl_panel_5.createSequentialGroup().addGap(10).addComponent(lblsupportPts).addPreferredGap(ComponentPlacement.RELATED).addComponent(numSupport)
                                                                .addPreferredGap(ComponentPlacement.RELATED, 51, Short.MAX_VALUE).addComponent(lblSmoothness).addGap(18).addComponent(smoothnessNP))
                                                .addGroup(
                                                        gl_panel_5.createSequentialGroup().addContainerGap().addComponent(lblPotentialShape).addPreferredGap(ComponentPlacement.RELATED)
                                                                .addComponent(potentialComboBox).addGap(18).addComponent(lblRepeatEstimation).addPreferredGap(ComponentPlacement.RELATED).addComponent(reRuns)))
                                .addContainerGap()));
        gl_panel_5.setVerticalGroup(gl_panel_5.createParallelGroup(Alignment.TRAILING).addGroup(
                gl_panel_5.createSequentialGroup().addContainerGap().addComponent(lblPotentialEstimation).addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl_panel_5.createParallelGroup(Alignment.BASELINE).addComponent(lblPotentialShape).addComponent(potentialComboBox).addComponent(lblRepeatEstimation).addComponent(reRuns))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl_panel_5.createParallelGroup(Alignment.BASELINE).addComponent(smoothnessNP).addComponent(lblsupportPts).addComponent(numSupport).addComponent(lblSmoothness))
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(estimate).addContainerGap()));
        panel_5.setLayout(gl_panel_5);
        
        tabbedPane = new JTabbedPane(SwingConstants.TOP);
        final JTabbedPane tabbedPane_1 = new JTabbedPane(SwingConstants.TOP);
        
        final JPanel panel_3 = new JPanel();
        tabbedPane_1.addTab("Apply mask", null, panel_3, null);
        tabbedPane_1.setEnabledAt(0, true);
        
        genMask = new JButton("Generate");
        genMask.setToolTipText("Generate a new mask from the opened Reference image");
        panel_3.add(genMask);
        genMask.addActionListener(this);
        
        loadMask = new JButton("Load");
        loadMask.setToolTipText("Load an existing mask from a file");
        panel_3.add(loadMask);
        loadMask.addActionListener(this);
        
        resetMask = new JButton("Reset");
        resetMask.setToolTipText("Reset mask ");
        panel_3.add(resetMask);
        resetMask.addActionListener(this);
        final JPanel panel_4 = new JPanel();
        
        btnCalculateDistances = new JButton("Calculate distances");
        btnCalculateDistances.setAlignmentX(SwingConstants.CENTER);
        
        gridSizeInp = new JFormattedTextField();
        gridSizeInp.setHorizontalAlignment(SwingConstants.CENTER);
        
        gridSizeInp.setText("0.5");
        gridSizeInp.setColumns(6);
        
        final JLabel lblGridSize = new JLabel("Grid spacing:");
        
        JLabel lblKernelWeightq = new JLabel("Kernel wt(q):");
        kernelWeightq = new JFormattedTextField();
        kernelWeightq.setHorizontalAlignment(SwingConstants.CENTER);
        kernelWeightq.setText("0.001");
        kernelWeightq.setColumns(6);
        
        JLabel lblKernelWeightp = new JLabel("Kernel wt(p):");
        kernelWeightp = new JFormattedTextField();
        kernelWeightp.setHorizontalAlignment(SwingConstants.CENTER);
        kernelWeightp.setText("35.9");
        kernelWeightp.setColumns(6);
        
        final GroupLayout gl_panel_4 = new GroupLayout(panel_4);
        gl_panel_4.setHorizontalGroup(gl_panel_4.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_4
                        .createSequentialGroup()
                        .addGroup(
                                gl_panel_4
                                        .createParallelGroup(Alignment.LEADING)
                                        .addGroup(
                                                gl_panel_4.createSequentialGroup().addComponent(lblGridSize).addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(gridSizeInp)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(lblKernelWeightq)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(kernelWeightq)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(lblKernelWeightp)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(kernelWeightp)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE))
                                        .addGroup(
                                                gl_panel_4.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(btnCalculateDistances)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE))).addGap(0)));
        gl_panel_4.setVerticalGroup(gl_panel_4.createParallelGroup(Alignment.TRAILING).addGroup(
                gl_panel_4
                        .createSequentialGroup()
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(
                                gl_panel_4.createParallelGroup(Alignment.BASELINE).addComponent(lblGridSize).addComponent(gridSizeInp).addComponent(lblKernelWeightq).addComponent(kernelWeightq)
                                        .addComponent(lblKernelWeightp).addComponent(kernelWeightp)).addGap(21).addComponent(btnCalculateDistances)));
        panel_4.setLayout(gl_panel_4);
        kernelWeightp.addActionListener(this);
        
        btnCalculateDistances.addActionListener(this);
        
        final JPanel panel_2 = new JPanel();
        tabbedPane.addTab("Load images", null, panel_2, null);
        
        final JLabel label = new JLabel("Image X");
        
        browseX = new JButton("Open X");
        browseX.addActionListener(this);
        
        final JLabel label_1 = new JLabel("Reference image Y");
        browseY = new JButton("Open Y");
        browseY.addActionListener(this);
        final GroupLayout gl_panel_2 = new GroupLayout(panel_2);
        gl_panel_2.setHorizontalGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addGap(0, 438, Short.MAX_VALUE)
                .addGroup(gl_panel_2.createSequentialGroup().addGap(98).addComponent(label).addGap(74).addComponent(browseX))
                .addGroup(gl_panel_2.createSequentialGroup().addGap(33).addComponent(label_1).addGap(74).addComponent(browseY)));
        gl_panel_2.setVerticalGroup(gl_panel_2
                .createParallelGroup(Alignment.LEADING)
                .addGap(0, 75, Short.MAX_VALUE)
                .addGroup(
                        gl_panel_2.createSequentialGroup().addGap(6)
                                .addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addGroup(gl_panel_2.createSequentialGroup().addGap(5).addComponent(label)).addComponent(browseX)).addGap(6)
                                .addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addGroup(gl_panel_2.createSequentialGroup().addGap(5).addComponent(label_1)).addComponent(browseY))));
        panel_2.setLayout(gl_panel_2);
        
        final JPanel panelCsvCoordinates = new JPanel();
        tabbedPane.addTab("Load coordinates", null, panelCsvCoordinates, null);
        final GroupLayout glCsvCoordinates = new GroupLayout(panelCsvCoordinates);
        final JLabel lblCsvFileX = new JLabel("X Coordinates");
        lblCsvFileX.setHorizontalAlignment(SwingConstants.LEFT);
        final JLabel lblCsvFileY = new JLabel("Y (reference) Coordinates");
        btnLoadCsvFileX = new JButton("Open");
        btnLoadCsvFileX.setHorizontalAlignment(SwingConstants.RIGHT);
        btnLoadCsvFileX.addActionListener(this);
        btnLoadCsvFileY = new JButton("Open");
        btnLoadCsvFileY.addActionListener(this);
        txtXmin = createTextField("0");
        txtYmin = createTextField("0");
        txtZmin = createTextField("0");
        txtXmax = createTextField("10");
        txtYmax = createTextField("10");
        txtZmax = createTextField("10");
        JLabel empty = new JLabel("");
        JLabel x = new JLabel("x");
        JLabel y = new JLabel("y");
        JLabel z = new JLabel("z");
        JLabel min = new JLabel("min");
        JLabel max = new JLabel("max");
        glCsvCoordinates.setHorizontalGroup(glCsvCoordinates.createSequentialGroup()
                .addGap(11)
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(empty)
                        .addComponent(lblCsvFileX)
                        .addComponent(lblCsvFileY))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(empty)
                        .addComponent(btnLoadCsvFileX)
                        .addComponent(btnLoadCsvFileY))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(x)
                        .addComponent(txtXmin)
                        .addComponent(txtXmax))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(y)
                        .addComponent(txtYmin)
                        .addComponent(txtYmax))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(z)
                        .addComponent(txtZmin)
                        .addComponent(txtZmax))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(empty)
                        .addComponent(min)
                        .addComponent(max))
                .addGap(11));
        glCsvCoordinates.setVerticalGroup(glCsvCoordinates.createSequentialGroup()
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addGap(5).addComponent(empty).addComponent(empty).addComponent(x).addComponent(y).addComponent(z).addComponent(empty))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addGap(5).addComponent(lblCsvFileX).addComponent(btnLoadCsvFileX).addComponent(txtXmin).addComponent(txtYmin).addComponent(txtZmin).addComponent(min))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addGap(5).addComponent(lblCsvFileY).addComponent(btnLoadCsvFileY).addComponent(txtXmax).addComponent(txtYmax).addComponent(txtZmax).addComponent(max))
        );
        panelCsvCoordinates.setLayout(glCsvCoordinates);
        
        final GroupLayout groupLayout = new GroupLayout(frmInteractionAnalysis.getContentPane());
        groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(Alignment.LEADING)
                                        .addGroup(
                                                groupLayout.createSequentialGroup().addGap(17).addComponent(panel_4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(tabbedPane_1, GroupLayout.PREFERRED_SIZE, 546, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(
                                                groupLayout
                                                        .createSequentialGroup()
                                                        .addGap(17)
                                                        .addGroup(
                                                                groupLayout.createParallelGroup(Alignment.TRAILING, false).addComponent(panel_help, Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                                                                        .addComponent(panel_6, Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                                                                        .addComponent(panel_7, Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                                                                        .addComponent(panel_5, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, 536, GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(9, Short.MAX_VALUE)));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout.createSequentialGroup().addContainerGap().addComponent(panel_help, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()
                        .addComponent(tabbedPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(tabbedPane_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_7, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_6, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()));
        frmInteractionAnalysis.getContentPane().setLayout(groupLayout);
        frmInteractionAnalysis.pack();
        estimate.addActionListener(this);
        test.addActionListener(this);
        mCRuns.addActionListener(this);
        frmInteractionAnalysis.setVisible(true);
    }

    private JFormattedTextField createTextField(String aValue) {
        JFormattedTextField ftf = new JFormattedTextField();
        ftf.setText(aValue);
        ftf.setColumns(6);
        ftf.addActionListener(this);
        return ftf;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == help) {
            new InteractionAnalysisHelpGUI(0, 0);
        }
        else if (e.getSource() == browseX) {
            imgx = FileUtils.openImage();
            if (imgx == null) {
                IJ.showMessage("Cancelled/Filetype not recognized");
                return;
            }
            imgx.show("Image X");
            browseX.setText(imgx.getTitle());
        }
        else if (e.getSource() == browseY) {
            imgy = FileUtils.openImage();
            if (imgy == null) {
                IJ.showMessage("Cancelled/Filetype not recognized");
                return;
            }
            imgy.show("Image Y");
            browseY.setText(imgy.getTitle());
        }
        else if (e.getSource() == btnLoadCsvFileX) {
            Xcoords = FileUtils.openCsvFile("Open CSV file for image X");
            setMinMaxCoordinates();
        }
        else if (e.getSource() == btnLoadCsvFileY) {
            Ycoords = FileUtils.openCsvFile("Open CSV file for image Y");
            setMinMaxCoordinates();
        }
        else if (e.getSource() == potentialComboBox) {
            final String selected = (String) potentialComboBox.getSelectedItem();
            System.out.println("Selected: " + selected);
            if (selected == items[5]) {
                enableNoparamControls(true);
                potentialType = PotentialType.NONPARAM;

            }
            else {
                enableNoparamControls(false);
                if (selected == items[0]) {
                    potentialType = PotentialType.HERNQUIST;
                }
                else if (selected == items[1]) {
                    potentialType = PotentialType.STEP;
                }
                else if (selected == items[2]) {
                    potentialType = PotentialType.L1;
                }
                else if (selected == items[3]) {
                    potentialType = PotentialType.L2;
                }
                else if (selected == items[4]) {
                    potentialType = PotentialType.PlUMMER;
                }
            }
        }
        else if (e.getSource() == btnCalculateDistances) {
            double gridSize = Double.parseDouble(gridSizeInp.getText());
            double qkernelWeight = Double.parseDouble(kernelWeightq.getText());
            double pkernelWeight = Double.parseDouble(kernelWeightp.getText());
            iAnalysis = new Analysis();
            boolean result = false;
            float[][][] mask3d = genMaskIP != null ? imageTo3Darray(genMaskIP) : null;
            if (tabbedPane.getSelectedIndex() == 1) {
                // coordinates
                double xmin = Double.parseDouble(txtXmin.getText());
                double ymin = Double.parseDouble(txtYmin.getText());
                double zmin = Double.parseDouble(txtZmin.getText());
                double xmax = Double.parseDouble(txtXmax.getText());
                double ymax = Double.parseDouble(txtYmax.getText());
                double zmax = Double.parseDouble(txtZmax.getText());
                if (xmax < xmin || ymax < ymin || zmax < zmin) {
                    IJ.showMessage("Error: boundary values are not correct");
                    return;
                }
                
                if (Xcoords != null && Ycoords != null) {
                    iAnalysis = new Analysis();
                    System.out.println("[Point3d] p set to:" + pkernelWeight);
                }
                System.out.println("Boundary:" + xmin + "," + xmax + ";" + ymin + "," + ymax + ";" + zmin + "," + zmax);
                result = iAnalysis.calcDist(gridSize, qkernelWeight, pkernelWeight, mask3d, Xcoords, Ycoords, xmin, xmax, ymin, ymax, zmin, zmax);
            }
            else {
                // image
                if (imgy != null && imgx != null) {
                    if (!checkIfImagesAreRightSize()) {
                        System.out.println("Distance calc: different image sizes");
                        IJ.showMessage("Error: Image sizes/scale/unit do not match");
                    }
                    else {
                        iAnalysis = new Analysis();
                        System.out.println("[ImagePlus] p set to:" + pkernelWeight);
                        result = iAnalysis.calcDist(gridSize, qkernelWeight, pkernelWeight, mask3d, imgx, imgy);
                    }
                }
            }

            if (!result) {
                IJ.showMessage("No X and Y images/coords loaded. Cannot calculate distance");
            }
        }
        else if (e.getSource() == estimate) {
            Potential.NONPARAM_WEIGHT_SIZE = Integer.parseInt(numSupport.getText());
            Potential.NONPARAM_SMOOTHNESS = Double.parseDouble(smoothnessNP.getText());
            int numReRuns = Integer.parseInt(reRuns.getText());

            System.out.println("Estimating with potential type:" + potentialType);
            iAnalysis.setPotentialType(potentialType); // for the first time
            List<Result> results = new ArrayList<Result>();
            if (!iAnalysis.cmaOptimization(results, numReRuns)) {
                IJ.showMessage("Error: Calculate distances first!");
            }
            else {
                if (!Interpreter.batchMode) {
                    final ResultsTable rt = new ResultsTable();
                    for (Analysis.Result r : results) {
                        rt.incrementCounter();
                        if (potentialType != PotentialType.NONPARAM) {
                            rt.addValue("Strength", r.iStrength);
                            rt.addValue("Threshold/Scale", r.iThresholdScale);
                        }
                        rt.addValue("Residual", r.iResidual);
                    }
                    rt.updateResults();
                    rt.show("Results");
                }
            }
        }
        else if (e.getSource() == test) {
            int monteCarloRunsForTest = Integer.parseInt(mCRuns.getText());
            double alpha = Double.parseDouble(alphaField.getText());

            if (!iAnalysis.hypothesisTesting(monteCarloRunsForTest, alpha)) {
                IJ.showMessage("Error: Run estimation first");
            }
        }
        else if (e.getSource() == numSupport) {
            Potential.NONPARAM_WEIGHT_SIZE = Integer.parseInt(e.getActionCommand());
            System.out.println("Weight size changed to:" + Potential.NONPARAM_WEIGHT_SIZE);
        }
        else if (e.getSource() == smoothnessNP) {
            Potential.NONPARAM_SMOOTHNESS = Double.parseDouble(e.getActionCommand());
            System.out.println("Smoothness:" + Potential.NONPARAM_SMOOTHNESS);
        }
        else if (e.getSource() == genMask) {
            try {
                if (tabbedPane.getSelectedIndex() == 1) {
                    IJ.showMessage("Cannot generate mask for coordinates. Load a mask instead");
                    return;
                }

                if (!generateMask()) {
                    IJ.showMessage("Image Y is null: Cannot generate mask");
                }
            }
            catch (final NullPointerException npe) {
                System.out.println("NPE caught");
                IJ.showMessage("Image Y is null: Cannot generate mask");
            }
        }
        else if (e.getSource() == loadMask) {
            if (loadMask() == true) {
                IJ.showMessage("Mask set to:" + getMaskTitle());
            }
        }
        else if (e.getSource() == resetMask) {
            resetMask();
            IJ.showMessage("Mask reset to Null");
        }
        else if (iAnalysis == null) {
            IJ.showMessage("Load images/coordinates first");
        }
    }

    private void enableNoparamControls(boolean aShow) {
        numSupport.setEnabled(aShow);
        smoothnessNP.setEnabled(aShow);
        lblSmoothness.setEnabled(aShow);
        lblsupportPts.setEnabled(aShow);
    }

    private void setMinMaxCoordinates() {
            double x1 = Double.MAX_VALUE;
            double y1 = Double.MAX_VALUE;
            double z1 = Double.MAX_VALUE;
            double x2 = -Double.MAX_VALUE; 
            double y2 = -Double.MAX_VALUE;
            double z2 = -Double.MAX_VALUE;
            boolean isSet = false;
            
            Point3d[][] coordinates = {Xcoords, Ycoords}; 
            for (Point3d[] coords : coordinates) {
                if (coords != null) {
                    for (Point3d p : coords) {
                        if (p.x < x1) x1 = p.x;
                        if (p.x > x2) x2 = p.x;
                        if (p.y < y1) y1 = p.y;
                        if (p.y > y2) y2 = p.y;
                        if (p.z < z1) z1 = p.z;
                        if (p.z > z2) z2 = p.z;
                    }
                    isSet = true;
                }
            }
            if (isSet) {
                txtXmin.setText(Math.floor(x1) + "");
                txtXmax.setText(Math.ceil(x2) + "");
                txtYmin.setText(Math.floor(y1) + "");
                txtYmax.setText(Math.ceil(y2) + "");
                txtZmin.setText(Math.floor(z1) + "");
                txtZmax.setText(Math.ceil(z2) + "");
            }
    }

    private boolean checkIfImagesAreRightSize() {
        final Calibration imgxc = imgx.getCalibration();
        final Calibration imgyc = imgy.getCalibration();
        if ((imgx.getWidth() == imgy.getWidth()) && 
            (imgx.getHeight() == imgy.getHeight()) && 
            (imgx.getStackSize() == imgy.getStackSize()) && 
            (imgxc.pixelDepth == imgyc.pixelDepth) &&
            (imgxc.pixelHeight == imgyc.pixelHeight) && 
            (imgxc.pixelWidth == imgyc.pixelWidth) && 
            (imgxc.getUnit().equals(imgyc.getUnit()))) 
        {
            return true;
        }
        else {
            System.out.println(imgx.getWidth() + "," + imgy.getWidth() + "," + imgx.getHeight() + "," + imgy.getHeight() + "," + imgx.getStackSize() + "," + imgy.getStackSize() + ","
                    + imgxc.pixelDepth + "," + imgyc.pixelDepth + "," + imgxc.pixelHeight + "," + imgyc.pixelHeight + "," + imgxc.pixelWidth + "," + imgyc.pixelWidth + "," + imgxc.getUnit() + ","
                    + imgyc.getUnit());

            return false;
        }
    }
    
    public boolean generateMask() {
        genMaskIP = new ImagePlus();
        if (imgy != null) {
            final ImagePlus genMaskIP = new Duplicator().run(imgy);
            genMaskIP.show();
            new Macro_Runner().run("JAR:src/mosaic/plugins/scripts/GenerateMask_.ijm");
            genMaskIP.changes = false;
            System.out.println("Generated mask: " + genMaskIP.getType());
            return true;
        }
        return false;
    }

    public boolean loadMask() {
        ImagePlus tempMask = FileUtils.openImage();
        if (tempMask == null) {
            IJ.showMessage("Filetype not recognized");
            return false;
        }
        else if (tempMask.getType() != ImagePlus.GRAY8) {
            IJ.showMessage("ERROR: Loaded mask not 8 bit gray");
            return false;
        }
        else if (!(tabbedPane.getSelectedIndex() == 1)) {
            if (tempMask.getHeight() != imgy.getHeight() || tempMask.getWidth() != imgy.getWidth() || tempMask.getNSlices() != imgy.getNSlices()) {
                IJ.showMessage("ERROR: Loaded mask size does not match with image size");
                return false;
            }
        }
    
        tempMask.show("Mask loaded" + tempMask.getTitle());
        genMaskIP = tempMask;
        genMaskIP.updateImage();
        return true;
    }

    private static float[][][] imageTo3Darray(ImagePlus image) {
        final ImageStack is = image.getStack();
        final float[][][] image3d = new float[is.getSize()][is.getWidth()][is.getHeight()];

        for (int k = 0; k < is.getSize(); k++) {
            ImageProcessor imageProc = is.getProcessor(k + 1);
            image3d[k] = imageProc.getFloatArray();
        }
        
        return image3d;
    }
    
    public boolean resetMask() {
        genMaskIP = null;
        return true;
    }
    
    public String getMaskTitle() {
        return genMaskIP.getTitle();
    }
}
