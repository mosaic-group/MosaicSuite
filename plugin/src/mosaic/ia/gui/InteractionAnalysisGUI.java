package mosaic.ia.gui;


import ij.IJ;
import ij.ImagePlus;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;

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

import mosaic.ia.Analysis;
import mosaic.ia.PotentialFunctions;
import mosaic.ia.Analysis.Result;
import mosaic.ia.utils.ImageProcessUtils;


public class InteractionAnalysisGUI implements ActionListener {

    private JFrame frmInteractionAnalysis;
    private final String[] items = { "Hernquist", "Step", "Linear type 1", "Linear type 2", "Plummer", "Non-parametric" };

    private double gridSize = .5;
    private ImagePlus imgx, imgy;
    private JComboBox<String> potentialComboBox;
    private int potentialType = PotentialFunctions.HERNQUIST;
    private Point3d[] Xcoords, Ycoords;

    private Analysis iAnalysis;
    private int monteCarloRunsForTest = 1000;
    private int numReRuns = 10;
    private final double qkernelWeight = .001;
    private final double pkernelWeight = 1;

    private JButton help;
    private double alpha = .05;
    private JFormattedTextField mCRuns, numSupport, smoothnessNP, gridSizeInp, reRuns, kernelWeightq, kernelWeightp;

    private JFormattedTextField alphaField;
    private JTabbedPane tabbedPane;
    private JButton browseY, browseX, btnCalculateDistances, estimate, test, genMask, loadMask, resetMask, btnLoadCsvFileX, btnLoadCsvFileY;
    private Border blackBorder;
    private JTextArea textArea;

    private JLabel lblsupportPts, lblSmoothness;
    private JFormattedTextField txtXmin;
    private JFormattedTextField txtYmin;
    private JFormattedTextField txtZmin;
    private JFormattedTextField txtXmax;
    private JFormattedTextField txtYmax;
    private JFormattedTextField txtZmax;
    private double xmin = Double.MAX_VALUE, ymin = Double.MAX_VALUE, zmin = Double.MAX_VALUE, xmax = Double.MAX_VALUE, ymax = Double.MAX_VALUE, zmax = Double.MAX_VALUE;
    private JLabel lblKernelWeightq, lblKernelWeightp;
    private ImagePlus genMaskIP;
    /**
     * Create the application.
     */
    public InteractionAnalysisGUI() {
        initialize();
        frmInteractionAnalysis.setVisible(true);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmInteractionAnalysis = new JFrame("Interaction Analysis");
        blackBorder = BorderFactory.createLineBorder(Color.black);
        browseX = new JButton("Open");
        browseY = new JButton("Open");
        help = new JButton("help");
        help.addActionListener(this);

        final JPanel panel_help = new JPanel();
        final JPanel panel_5 = new JPanel();
        panel_5.setBorder(blackBorder);
        final JPanel panel_7 = new JPanel();
        panel_7.setBorder(blackBorder);
        final JPanel panel_6 = new JPanel();
        panel_6.setBorder(blackBorder);
        textArea = new JTextArea("Please refer to and cite: J. A. Helmuth, G. Paul, and I. F. Sbalzarini.\n" + "Beyond co-localization: inferring spatial interactions between sub-cellular \n"
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
        mCRuns.setText("" + monteCarloRunsForTest);
        mCRuns.setColumns(10);

        final JLabel lblSignificanceLevel = new JLabel("Significance level");

        alphaField = new JFormattedTextField();
        alphaField.setHorizontalAlignment(SwingConstants.CENTER);
        alphaField.setText("" + alpha);
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

        numSupport.setText("" + PotentialFunctions.NONPARAM_WEIGHT_SIZE);
        numSupport.setColumns(10);
        numSupport.setEnabled(false);
        numSupport.addActionListener(this);

        smoothnessNP = new JFormattedTextField();

        smoothnessNP.setHorizontalAlignment(SwingConstants.CENTER);

        smoothnessNP.setText("" + PotentialFunctions.NONPARAM_SMOOTHNESS);
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
        reRuns.setText(numReRuns + "");
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

        gridSizeInp.setText("" + gridSize);
        gridSizeInp.setColumns(6);

        final JLabel lblGridSize = new JLabel("Grid spacing:");

        lblKernelWeightq = new JLabel("Kernel wt(q):");

        kernelWeightq = new JFormattedTextField();
        kernelWeightq.setHorizontalAlignment(SwingConstants.CENTER);
        kernelWeightq.setText(qkernelWeight + "");
        kernelWeightq.setColumns(6);

        lblKernelWeightp = new JLabel("Kernel wt(p):");

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

        final JPanel panel_8 = new JPanel();
        tabbedPane.addTab("Load coordinates", null, panel_8, null);

        btnLoadCsvFileX = new JButton("Open");
        btnLoadCsvFileX.setHorizontalAlignment(SwingConstants.RIGHT);
        btnLoadCsvFileX.addActionListener(this);

        final JLabel lblCsvFileOf = new JLabel("X Coordinates");
        lblCsvFileOf.setHorizontalAlignment(SwingConstants.LEFT);

        final JLabel label_2 = new JLabel("Y (reference) Coordinates");

        btnLoadCsvFileY = new JButton("Open");
        btnLoadCsvFileY.addActionListener(this);

        txtXmin = new JFormattedTextField();
        txtXmin.setText("xmin");
        txtXmin.setColumns(6);
        txtXmin.addActionListener(this);

        txtYmin = new JFormattedTextField();
        txtYmin.setText("ymin");
        txtYmin.setColumns(6);
        txtYmin.addActionListener(this);

        txtZmin = new JFormattedTextField();
        txtZmin.setText("zmin");
        txtZmin.setColumns(6);
        txtZmin.addActionListener(this);

        txtXmax = new JFormattedTextField();
        txtXmax.setText("xmax");
        txtXmax.setColumns(6);
        txtXmax.addActionListener(this);

        txtYmax = new JFormattedTextField();
        txtYmax.setText("ymax");
        txtYmax.setColumns(6);
        txtYmax.addActionListener(this);

        txtZmax = new JFormattedTextField();
        txtZmax.setText("zmax");
        txtZmax.setColumns(6);
        txtZmax.addActionListener(this);
        final GroupLayout gl_panel_8 = new GroupLayout(panel_8);
        gl_panel_8.setHorizontalGroup(gl_panel_8.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_8
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(gl_panel_8.createParallelGroup(Alignment.LEADING).addComponent(lblCsvFileOf).addComponent(label_2))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                                gl_panel_8.createParallelGroup(Alignment.LEADING, false)
                                        .addGroup(gl_panel_8.createSequentialGroup().addComponent(btnLoadCsvFileY).addPreferredGap(ComponentPlacement.RELATED).addComponent(txtXmax))
                                        .addGroup(gl_panel_8.createSequentialGroup().addComponent(btnLoadCsvFileX).addPreferredGap(ComponentPlacement.RELATED).addComponent(txtXmin)))
                        .addGroup(
                                gl_panel_8.createParallelGroup(Alignment.LEADING).addGroup(gl_panel_8.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED).addComponent(txtYmin))
                                        .addGroup(Alignment.TRAILING, gl_panel_8.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED).addComponent(txtYmax)))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                                gl_panel_8.createParallelGroup(Alignment.TRAILING).addGroup(gl_panel_8.createSequentialGroup().addComponent(txtZmin).addContainerGap())
                                        .addGroup(gl_panel_8.createSequentialGroup().addComponent(txtZmax).addGap(11)))));
        gl_panel_8.setVerticalGroup(gl_panel_8.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_8
                        .createSequentialGroup()
                        .addGap(5)
                        .addGroup(
                                gl_panel_8.createParallelGroup(Alignment.BASELINE).addComponent(lblCsvFileOf).addComponent(btnLoadCsvFileX).addComponent(txtXmin).addComponent(txtYmin)
                                        .addComponent(txtZmin))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                                gl_panel_8.createParallelGroup(Alignment.BASELINE).addComponent(label_2).addComponent(btnLoadCsvFileY).addComponent(txtXmax).addComponent(txtYmax)
                                        .addComponent(txtZmax)).addGap(13)));
        panel_8.setLayout(gl_panel_8);
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

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == help) {
            new InteractionAnalysisHelpGUI(0, 0);
        }
        else if (e.getSource() == browseX) {
            imgx = ImageProcessUtils.openImage();
            if (imgx == null) {
                IJ.showMessage("Cancelled/Filetype not recognized");
                return;
            }
            imgx.show("Image X");
            browseX.setText(imgx.getTitle());
        }
        else if (e.getSource() == browseY) {
            imgy = ImageProcessUtils.openImage();
            if (imgy == null) {
                IJ.showMessage("Cancelled/Filetype not recognized");
                return;
            }
            imgy.show("Image Y");
            browseY.setText(imgy.getTitle());
        }
        else if (e.getSource() == btnLoadCsvFileX) {
            Xcoords = ImageProcessUtils.openCsvFile("Open CSV file for image X");
            setMinMaxCoordinates();
        }
        else if (e.getSource() == btnLoadCsvFileY) {
            Ycoords = ImageProcessUtils.openCsvFile("Open CSV file for image Y");
            setMinMaxCoordinates();
        }
        else if (e.getSource() == potentialComboBox) {
            final String selected = (String) potentialComboBox.getSelectedItem();
            System.out.println("Selected: " + selected);
            if (selected == items[5]) {
                potentialType = PotentialFunctions.NONPARAM;
                numSupport.setEnabled(true);
                smoothnessNP.setEnabled(true);
                lblSmoothness.setEnabled(true);
                lblsupportPts.setEnabled(true);

            }
            else {
                numSupport.setEnabled(false);
                smoothnessNP.setEnabled(false);
                lblSmoothness.setEnabled(false);
                lblsupportPts.setEnabled(false);
                if (selected == items[0]) {
                    potentialType = PotentialFunctions.HERNQUIST;
                }
                else if (selected == items[1]) {
                    potentialType = PotentialFunctions.STEP;
                }
                else if (selected == items[2]) {
                    potentialType = PotentialFunctions.L1;
                }
                else if (selected == items[3]) {
                    potentialType = PotentialFunctions.L2;
                }
                else if (selected == items[4]) {
                    potentialType = PotentialFunctions.PlUMMER;
                }
            }
        }
        else if (e.getSource() == btnCalculateDistances) {
            gridSize = Double.parseDouble(gridSizeInp.getText());
            double qkernel = Double.parseDouble(kernelWeightq.getText());
            double pkernel = Double.parseDouble(kernelWeightp.getText());
            iAnalysis = null;
            if (tabbedPane.getSelectedIndex() == 1) {
                // coordinates
                xmin = Double.parseDouble(txtXmin.getText());
                ymin = Double.parseDouble(txtYmin.getText());
                zmin = Double.parseDouble(txtZmin.getText());
                xmax = Double.parseDouble(txtXmax.getText());
                ymax = Double.parseDouble(txtYmax.getText());
                zmax = Double.parseDouble(txtZmax.getText());
                if (xmax < xmin || ymax < ymin || zmax < zmin) 
                {
                    IJ.showMessage("Error: boundary values are not correct");
                    return;
                }
                
                if (Xcoords != null && Ycoords != null) {
                    iAnalysis = new Analysis(Xcoords, Ycoords, genMaskIP, xmin, xmax, ymin, ymax, zmin, zmax);
                    System.out.println("[Point3d] p set to:" + pkernelWeight);
                }
                System.out.println("Boundary:" + xmin + "," + xmax + ";" + ymin + "," + ymax + ";" + zmin + "," + zmax);
            }
            else {
                // image
                if (imgy != null && imgx != null) {
                    if (!checkIfImagesAreRightSize()) {
                        System.out.println("Distance calc: different image sizes");
                        IJ.showMessage("Error: Image sizes/scale/unit do not match");
                    }
                    else {
                        iAnalysis = new Analysis(imgx, imgy, genMaskIP);
                        System.out.println("[ImagePlus] p set to:" + pkernelWeight);
                    }
                }
            }

            if (iAnalysis == null || !iAnalysis.calcDist(gridSize, qkernel, pkernel)) {
                IJ.showMessage("No X and Y images/coords loaded. Cannot calculate distance");
            }
        }
        else if (e.getSource() == estimate) {
            PotentialFunctions.NONPARAM_WEIGHT_SIZE = Integer.parseInt(numSupport.getText());
            PotentialFunctions.NONPARAM_SMOOTHNESS = Double.parseDouble(smoothnessNP.getText());
            numReRuns = Integer.parseInt(reRuns.getText());

            System.out.println("Estimating with potential type:" + potentialType);
            if (potentialType == PotentialFunctions.NONPARAM) {
                PotentialFunctions.initializeNonParamWeights(iAnalysis.getMinD(), iAnalysis.getMaxD());
            }
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
                        if (potentialType != PotentialFunctions.NONPARAM) {
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
            monteCarloRunsForTest = Integer.parseInt(mCRuns.getText());
            alpha = Double.parseDouble(alphaField.getText());

            if (!iAnalysis.hypothesisTesting(monteCarloRunsForTest, alpha)) {
                IJ.showMessage("Error: Run estimation first");
            }
        }
        else if (e.getSource() == numSupport) {
            PotentialFunctions.NONPARAM_WEIGHT_SIZE = Integer.parseInt(e.getActionCommand());
            System.out.println("Weight size changed to:" + PotentialFunctions.NONPARAM_WEIGHT_SIZE);
        }
        else if (e.getSource() == smoothnessNP) {
            PotentialFunctions.NONPARAM_SMOOTHNESS = Double.parseDouble(e.getActionCommand());
            System.out.println("Smoothness:" + PotentialFunctions.NONPARAM_SMOOTHNESS);
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

    private void setMinMaxCoordinates() {
            double x1 = Double.MAX_VALUE;
            double y1 = Double.MAX_VALUE;
            double z1 = Double.MAX_VALUE;
            double x2 = Double.MIN_VALUE; 
            double y2 = Double.MIN_VALUE;
            double z2 = Double.MIN_VALUE;
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
            genMaskIP = ImageProcessUtils.generateMask(imgy);
            System.out.println("Generated mask: " + genMaskIP.getType());
            return true;
        }
        return false;
    }

    public boolean loadMask() {
        ImagePlus tempMask = ImageProcessUtils.openImage();
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

    public boolean resetMask() {
        genMaskIP = null;
        return true;
    }
    
    public String getMaskTitle() {
        return genMaskIP.getTitle();
    }
}
