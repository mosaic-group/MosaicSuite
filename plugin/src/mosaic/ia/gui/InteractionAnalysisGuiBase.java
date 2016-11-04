package mosaic.ia.gui;


import java.awt.Color;
import java.awt.event.ActionListener;

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

/**
 * This class contain GUI creation stuff (probably back in time generated in NetBeans or sth).
 * It was shamelessly moved here to make GUI processing (subclass of it) more clear
 * and easier to maintain.
 */
abstract public class InteractionAnalysisGuiBase implements ActionListener {

    protected JButton help;
    protected JButton loadImgY, loadImgX;
    protected JButton loadCsvX, loadCsvY;
    protected JFormattedTextField xMin, yMin, zMin, xMax,yMax, zMax;
    protected JTabbedPane maskPane;
    protected JButton generateMask, loadMask, resetMask;
    protected JFormattedTextField gridSize;
    protected JFormattedTextField  kernelWeightQ;
    protected JFormattedTextField  kernelWeightP;
    protected JButton calculateDistances;
    protected JComboBox potentialComboBox;
    protected JFormattedTextField reRuns;
    protected JLabel numOfsupportPointsLabel, smoothnessLabel;
    protected JFormattedTextField numOfSupportPoints, smoothness;
    protected JButton estimate;
    protected JFormattedTextField monteCarloRuns;
    protected JFormattedTextField alphaField;
    protected JButton testHypothesis;
    protected JTabbedPane tabbedPane;
    
    /**
     * Create the application.
     */
    public InteractionAnalysisGuiBase(String[] potentials) {
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
        
        monteCarloRuns = new JFormattedTextField();
        monteCarloRuns.setHorizontalAlignment(SwingConstants.CENTER);
        monteCarloRuns.setText("1000");
        monteCarloRuns.setColumns(10);
        
        final JLabel lblSignificanceLevel = new JLabel("Significance level");
        
        alphaField = new JFormattedTextField();
        alphaField.setHorizontalAlignment(SwingConstants.CENTER);
        alphaField.setText("0.05");
        alphaField.setColumns(10);
        alphaField.addActionListener(this);
        
        testHypothesis = new JButton("Test");
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
                                                                        gl_panel_7.createParallelGroup(Alignment.TRAILING, false).addComponent(monteCarloRuns, Alignment.LEADING)
                                                                                .addComponent(alphaField, Alignment.LEADING)).addGap(23))))
                .addGroup(gl_panel_7.createSequentialGroup().addContainerGap(244, Short.MAX_VALUE).addComponent(testHypothesis).addGap(233)));
        gl_panel_7.setVerticalGroup(gl_panel_7.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_7
                        .createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblHypothesisTesting)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE).addComponent(monteCarloRuns, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE).addComponent(lblMonteCarloRuns))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(
                                gl_panel_7.createParallelGroup(Alignment.BASELINE).addComponent(lblSignificanceLevel)
                                        .addComponent(alphaField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(ComponentPlacement.RELATED, 11, Short.MAX_VALUE).addComponent(testHypothesis).addContainerGap()));
        panel_7.setLayout(gl_panel_7);
        
        final JLabel lblPotentialEstimation = new JLabel("Potential estimation");
        
        potentialComboBox = new JComboBox(potentials);
        potentialComboBox.setSelectedIndex(1);
        potentialComboBox.addActionListener(this);
        
        estimate = new JButton("Estimate");
        estimate.setActionCommand("Estimate");
        
        final JLabel lblPotentialShape = new JLabel("Potential:");
        
        numOfSupportPoints = new JFormattedTextField();
        
        numOfSupportPoints.setHorizontalAlignment(SwingConstants.CENTER);
        
        numOfSupportPoints.setText("41");
        numOfSupportPoints.setColumns(10);
        numOfSupportPoints.setEnabled(false);
        numOfSupportPoints.addActionListener(this);
        
        smoothness = new JFormattedTextField();
        
        smoothness.setHorizontalAlignment(SwingConstants.CENTER);
        
        smoothness.setText("0.1");
        smoothness.setColumns(10);
        smoothness.setEnabled(false);
        smoothness.addActionListener(this);
        
        numOfsupportPointsLabel = new JLabel("#Support pts:");
        numOfsupportPointsLabel.setEnabled(false);
        
        smoothnessLabel = new JLabel("Smoothness:");
        smoothnessLabel.setEnabled(false);
        
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
                                                        gl_panel_5.createSequentialGroup().addGap(10).addComponent(numOfsupportPointsLabel).addPreferredGap(ComponentPlacement.RELATED).addComponent(numOfSupportPoints)
                                                                .addPreferredGap(ComponentPlacement.RELATED, 51, Short.MAX_VALUE).addComponent(smoothnessLabel).addGap(18).addComponent(smoothness))
                                                .addGroup(
                                                        gl_panel_5.createSequentialGroup().addContainerGap().addComponent(lblPotentialShape).addPreferredGap(ComponentPlacement.RELATED)
                                                                .addComponent(potentialComboBox).addGap(18).addComponent(lblRepeatEstimation).addPreferredGap(ComponentPlacement.RELATED).addComponent(reRuns)))
                                .addContainerGap()));
        gl_panel_5.setVerticalGroup(gl_panel_5.createParallelGroup(Alignment.TRAILING).addGroup(
                gl_panel_5.createSequentialGroup().addContainerGap().addComponent(lblPotentialEstimation).addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl_panel_5.createParallelGroup(Alignment.BASELINE).addComponent(lblPotentialShape).addComponent(potentialComboBox).addComponent(lblRepeatEstimation).addComponent(reRuns))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(gl_panel_5.createParallelGroup(Alignment.BASELINE).addComponent(smoothness).addComponent(numOfsupportPointsLabel).addComponent(numOfSupportPoints).addComponent(smoothnessLabel))
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(estimate).addContainerGap()));
        panel_5.setLayout(gl_panel_5);
        
        tabbedPane = new JTabbedPane(SwingConstants.TOP);
        maskPane = new JTabbedPane(SwingConstants.TOP);
        
        final JPanel panel_3 = new JPanel();
        maskPane.addTab("Mask: <empty>", null, panel_3, null);
        maskPane.setEnabledAt(0, true);
        
        generateMask = new JButton("Generate");
        generateMask.setToolTipText("Generate a new mask from the opened Reference image");
        panel_3.add(generateMask);
        generateMask.addActionListener(this);
        
        loadMask = new JButton("Load");
        loadMask.setToolTipText("Load an existing mask from a file");
        panel_3.add(loadMask);
        loadMask.addActionListener(this);
        
        resetMask = new JButton("Reset");
        resetMask.setToolTipText("Reset mask ");
        panel_3.add(resetMask);
        resetMask.addActionListener(this);
        final JPanel panel_4 = new JPanel();
        
        calculateDistances = new JButton("Calculate distances");
        calculateDistances.setAlignmentX(SwingConstants.CENTER);
        
        gridSize = new JFormattedTextField();
        gridSize.setHorizontalAlignment(SwingConstants.CENTER);
        
        gridSize.setText("0.5");
        gridSize.setColumns(6);
        
        final JLabel lblGridSize = new JLabel("Grid spacing:");
        
        JLabel lblKernelWeightq = new JLabel("Kernel wt(q):");
        kernelWeightQ = new JFormattedTextField();
        kernelWeightQ.setHorizontalAlignment(SwingConstants.CENTER);
        kernelWeightQ.setText("0.001");
        kernelWeightQ.setColumns(6);
        
        JLabel lblKernelWeightp = new JLabel("Kernel wt(p):");
        kernelWeightP = new JFormattedTextField();
        kernelWeightP.setHorizontalAlignment(SwingConstants.CENTER);
        kernelWeightP.setText("35.9");
        kernelWeightP.setColumns(6);
        
        final GroupLayout gl_panel_4 = new GroupLayout(panel_4);
        gl_panel_4.setHorizontalGroup(gl_panel_4.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_4
                        .createSequentialGroup()
                        .addGroup(
                                gl_panel_4
                                        .createParallelGroup(Alignment.LEADING)
                                        .addGroup(
                                                gl_panel_4.createSequentialGroup().addComponent(lblGridSize).addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(gridSize)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(lblKernelWeightq)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(kernelWeightQ)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(lblKernelWeightp)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(kernelWeightP)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE))
                                        .addGroup(
                                                gl_panel_4.createSequentialGroup().addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE).addComponent(calculateDistances)
                                                        .addPreferredGap(ComponentPlacement.RELATED, 0, Short.MAX_VALUE))).addGap(0)));
        gl_panel_4.setVerticalGroup(gl_panel_4.createParallelGroup(Alignment.TRAILING).addGroup(
                gl_panel_4
                        .createSequentialGroup()
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(
                                gl_panel_4.createParallelGroup(Alignment.BASELINE).addComponent(lblGridSize).addComponent(gridSize).addComponent(lblKernelWeightq).addComponent(kernelWeightQ)
                                        .addComponent(lblKernelWeightp).addComponent(kernelWeightP)).addGap(21).addComponent(calculateDistances)));
        panel_4.setLayout(gl_panel_4);
        kernelWeightP.addActionListener(this);
        
        calculateDistances.addActionListener(this);
        
        final JPanel panel_2 = new JPanel();
        tabbedPane.addTab("Load images", null, panel_2, null);
        
        final JLabel label = new JLabel("Image X");
        
        loadImgX = new JButton("Open X");
        loadImgX.addActionListener(this);
        
        final JLabel label_1 = new JLabel("Reference image Y");
        loadImgY = new JButton("Open Y");
        loadImgY.addActionListener(this);
        final GroupLayout gl_panel_2 = new GroupLayout(panel_2);
        gl_panel_2.setHorizontalGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addGap(0, 438, Short.MAX_VALUE)
                .addGroup(gl_panel_2.createSequentialGroup().addGap(98).addComponent(label).addGap(74).addComponent(loadImgX))
                .addGroup(gl_panel_2.createSequentialGroup().addGap(33).addComponent(label_1).addGap(74).addComponent(loadImgY)));
        gl_panel_2.setVerticalGroup(gl_panel_2
                .createParallelGroup(Alignment.LEADING)
                .addGap(0, 75, Short.MAX_VALUE)
                .addGroup(
                        gl_panel_2.createSequentialGroup().addGap(6)
                                .addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addGroup(gl_panel_2.createSequentialGroup().addGap(5).addComponent(label)).addComponent(loadImgX)).addGap(6)
                                .addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING).addGroup(gl_panel_2.createSequentialGroup().addGap(5).addComponent(label_1)).addComponent(loadImgY))));
        panel_2.setLayout(gl_panel_2);
        
        final JPanel panelCsvCoordinates = new JPanel();
        tabbedPane.addTab("Load coordinates", null, panelCsvCoordinates, null);
        final GroupLayout glCsvCoordinates = new GroupLayout(panelCsvCoordinates);
        final JLabel lblCsvFileX = new JLabel("X Coordinates");
        lblCsvFileX.setHorizontalAlignment(SwingConstants.LEFT);
        final JLabel lblCsvFileY = new JLabel("Y (reference) Coordinates");
        loadCsvX = new JButton("Open");
        loadCsvX.setHorizontalAlignment(SwingConstants.RIGHT);
        loadCsvX.addActionListener(this);
        loadCsvY = new JButton("Open");
        loadCsvY.addActionListener(this);
        xMin = createTextField("0");
        yMin = createTextField("0");
        zMin = createTextField("0");
        xMax = createTextField("10");
        yMax = createTextField("10");
        zMax = createTextField("10");
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
                        .addComponent(loadCsvX)
                        .addComponent(loadCsvY))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(x)
                        .addComponent(xMin)
                        .addComponent(xMax))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(y)
                        .addComponent(yMin)
                        .addComponent(yMax))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(z)
                        .addComponent(zMin)
                        .addComponent(zMax))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(empty)
                        .addComponent(min)
                        .addComponent(max))
                .addGap(11));
        glCsvCoordinates.setVerticalGroup(glCsvCoordinates.createSequentialGroup()
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addGap(5).addComponent(empty).addComponent(empty).addComponent(x).addComponent(y).addComponent(z).addComponent(empty))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addGap(5).addComponent(lblCsvFileX).addComponent(loadCsvX).addComponent(xMin).addComponent(yMin).addComponent(zMin).addComponent(min))
                .addGroup(glCsvCoordinates.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addGap(5).addComponent(lblCsvFileY).addComponent(loadCsvY).addComponent(xMax).addComponent(yMax).addComponent(zMax).addComponent(max))
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
                                        .addGroup(groupLayout.createSequentialGroup().addContainerGap().addComponent(maskPane, GroupLayout.PREFERRED_SIZE, 546, GroupLayout.PREFERRED_SIZE))
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
                        .addComponent(maskPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_7, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel_6, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addContainerGap()));
        frmInteractionAnalysis.getContentPane().setLayout(groupLayout);
        frmInteractionAnalysis.pack();
        estimate.addActionListener(this);
        testHypothesis.addActionListener(this);
        monteCarloRuns.addActionListener(this);
        frmInteractionAnalysis.setVisible(true);
    }

    private JFormattedTextField createTextField(String aValue) {
        JFormattedTextField ftf = new JFormattedTextField();
        ftf.setText(aValue);
        ftf.setColumns(6);
        ftf.addActionListener(this);
        return ftf;
    }
    
    protected void enableNoparamControls(boolean aShow) {
        numOfSupportPoints.setEnabled(aShow);
        smoothness.setEnabled(aShow);
        smoothnessLabel.setEnabled(aShow);
        numOfsupportPointsLabel.setEnabled(aShow);
    }
}
