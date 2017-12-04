package mosaic.regions.GUI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import mosaic.regions.RegionsUtils.EnergyFunctionalType;
import mosaic.regions.RegionsUtils.InitializationType;
import mosaic.regions.RegionsUtils.RegularizationType;


public abstract class GuiDrsBase extends JDialog implements ItemListener, ActionListener {
    private static final long serialVersionUID = 1L;
    
    protected JPanel basePanel;
    protected JPanel buttonsPanel;
    protected JPanel statusPanel;
    protected JPanel energiesPanel;
    protected JPanel otherSettingsPanel;
    protected JPanel dataEnergyPanel;
    protected JPanel regularizationEnergyPanel;
    protected JPanel initializationPanel;
    protected JButton btnResetSettings;
    protected JButton btnOk;
    protected JButton btnCancel;
    protected JPanel dataEnergyCards;
    protected JComboBox<String> dataEnergyCombo;
    protected JPanel psEnergyPanel;
    protected JPanel pcEnergyPanel;
    protected JPanel pcGaussEnergyPanel;
    protected JPanel lengthEnergyCards;
    protected JComboBox<String> lengthEnergyCombo;
    protected JTextField lambdaRegularization;
    protected JLabel lblLambdaRegularization;
    protected JPanel sphereRegularizationPanel;
    protected JPanel approxReguralizationPanel;
    protected JPanel noneReguralizationPanel;
    protected JPanel initCards;
    protected JComboBox<String> initializationCombo;
    protected JPanel rectangleInitPanel;
    protected JPanel bubblesInitPanel;
    protected JPanel localMaxInitPanel;
    protected JPanel roiInitiPanel;
    protected JPanel fileInitPanel;
    protected JLabel lblNumberOfIterations;
    protected JTextField numOfIterations;
    protected JLabel lblGeometricConstrains;
    protected JCheckBox chckbxFussion;
    protected JCheckBox chckbxFission;
    protected JCheckBox chckbxHandles;
    protected JLabel lblMcmcSettings;
    protected JTextField burnInFactor;
    protected JTextField offBoundaryProbability;
    protected JCheckBox chckbxBiasedProposal;
    protected JCheckBox chckbxPairProposal;
    protected JLabel lblOffboundaryProbability;
    protected JLabel lblBurninFactor;
    protected JLabel lblOutputSettings;
    protected JCheckBox chckbxShowLabelImage;
    protected JCheckBox chckbxSaveLabelImage;
    protected JCheckBox chckbxShowProbabilityImage;
    protected JCheckBox chckbxSaveProbabilityImage;
    protected JLabel lblNewLabel_1;
    protected JSeparator separator;
    protected JSeparator separator_1;
    protected JSeparator separator_2;
    protected JLabel lblNoOptionsAvailable;
    protected JLabel lblNoOptionsAvailable_1;
    protected JTextField psEenergyRadius;
    protected JTextField psEnergyBetaBalloon;
    protected JLabel lblPsEnergyRadius;
    protected JLabel lblPsEnergyBetaEBalloon;
    protected JLabel lblNoOptionsAvailable_2;
    protected JLabel lblNoOptionsAvailable_3;
    protected JTextField sphereRegularizationRadiusK;
    protected JLabel lblSphereReguralizationRK;
    protected JTextField rectangleInitBoxFillRatio;
    protected JLabel lblBoxFillRatio;
    protected JTextField bubblesInitRadius;
    protected JLabel lblBubbleRadius;
    protected JTextField bubblesInitPadding;
    protected JLabel lblBubblePadding;
    protected JTextField localMaxInitRadius;
    protected JLabel lblMaxInitRadius;
    protected JTextField localMaxInitSigma;
    protected JLabel lblMaxInitSigma;
    protected JTextField localMaxInitTolerance;
    protected JLabel lblMaxInitTolerance;
    protected JTextField localMaxInitMinRegionSize;
    protected JLabel lblMaxInitMinimumRegionSize;
    protected JLabel lblNoOptionsAvailable_4;
    protected JList<String> fileInitList;
    protected JLabel lblInitFile;
    protected JLabel lblStatus;
    protected JPanel citationPanel;
    protected JTextArea txtCitationInfo;

    private String fileInitPanelId = null;
    
    /**
     * Create the dialog.
     */
    public GuiDrsBase(List<EnergyFunctionalType> aDataEnergies) {
        
        basePanel = new JPanel();
        getContentPane().add(basePanel, BorderLayout.CENTER);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[]{0, 0};
        gbl_panel.rowHeights = new int[]{0, 20, 20, 0};
        gbl_panel.columnWeights = new double[]{1.0, 1.0};
        gbl_panel.rowWeights = new double[]{1.0, 0.1, 0.0, 1.0};
        basePanel.setLayout(gbl_panel);
        
        energiesPanel = new JPanel();
        GridBagConstraints gbc_energiesPanel = new GridBagConstraints();
        gbc_energiesPanel.insets = new Insets(0, 0, 5, 5);
        gbc_energiesPanel.fill = GridBagConstraints.BOTH;
        gbc_energiesPanel.gridx = 0;
        gbc_energiesPanel.gridy = 0;
        basePanel.add(energiesPanel, gbc_energiesPanel);
        GridBagLayout gbl_energiesPanel = new GridBagLayout();
        gbl_energiesPanel.columnWidths = new int[] {0};
        gbl_energiesPanel.rowHeights = new int[] {0, 30, 0, 30, 0};
        gbl_energiesPanel.columnWeights = new double[]{1.0};
        gbl_energiesPanel.rowWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0};
        energiesPanel.setLayout(gbl_energiesPanel);
        
        dataEnergyPanel = new JPanel();
        dataEnergyPanel.setBorder(new TitledBorder(null, "Energy Data", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_dataEnergyPanel = new GridBagConstraints();
        gbc_dataEnergyPanel.insets = new Insets(0, 0, 5, 0);
        gbc_dataEnergyPanel.fill = GridBagConstraints.BOTH;
        gbc_dataEnergyPanel.gridx = 0;
        gbc_dataEnergyPanel.gridy = 0;
        energiesPanel.add(dataEnergyPanel, gbc_dataEnergyPanel);
        GridBagLayout gbl_dataEnergyPanel = new GridBagLayout();
        gbl_dataEnergyPanel.columnWidths = new int[]{0, 0};
        gbl_dataEnergyPanel.rowHeights = new int[]{0, 0, 0};
        gbl_dataEnergyPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gbl_dataEnergyPanel.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        dataEnergyPanel.setLayout(gbl_dataEnergyPanel);
        
        dataEnergyCombo = new JComboBox<>();
        dataEnergyCombo.addItemListener(this);
        GridBagConstraints gbc_dataEnergyCombo = new GridBagConstraints();
        gbc_dataEnergyCombo.insets = new Insets(0, 0, 5, 0);
        gbc_dataEnergyCombo.fill = GridBagConstraints.HORIZONTAL;
        gbc_dataEnergyCombo.gridx = 0;
        gbc_dataEnergyCombo.gridy = 0;
        dataEnergyPanel.add(dataEnergyCombo, gbc_dataEnergyCombo);
        
        String pcGaussId = aDataEnergies.get(0).toString();
        String pcId = aDataEnergies.get(1).toString();
        String psId = aDataEnergies.get(2).toString();
        
        dataEnergyCombo.addItem(pcGaussId);
        dataEnergyCombo.addItem(pcId);
        dataEnergyCombo.addItem(psId);
        
        dataEnergyCards = new JPanel();
        dataEnergyCards.setBorder(new TitledBorder(null, "energy options", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, null));
        GridBagConstraints gbc_dataEnergyCards = new GridBagConstraints();
        gbc_dataEnergyCards.fill = GridBagConstraints.BOTH;
        gbc_dataEnergyCards.gridx = 0;
        gbc_dataEnergyCards.gridy = 1;
        dataEnergyPanel.add(dataEnergyCards, gbc_dataEnergyCards);
        dataEnergyCards.setLayout(new CardLayout(0, 0));
        
        pcGaussEnergyPanel = new JPanel();
        dataEnergyCards.add(pcGaussEnergyPanel, pcGaussId);
        pcGaussEnergyPanel.setLayout(new BorderLayout(0, 0));
        
        lblNoOptionsAvailable_1 = new JLabel("No options available");
        lblNoOptionsAvailable_1.setHorizontalAlignment(SwingConstants.CENTER);
        pcGaussEnergyPanel.add(lblNoOptionsAvailable_1, BorderLayout.CENTER);
        
        pcEnergyPanel = new JPanel();
        dataEnergyCards.add(pcEnergyPanel, pcId);
        pcEnergyPanel.setLayout(new BorderLayout(0, 0));
        
        lblNoOptionsAvailable = new JLabel("No options available");
        lblNoOptionsAvailable.setHorizontalAlignment(SwingConstants.CENTER);
        pcEnergyPanel.add(lblNoOptionsAvailable, BorderLayout.CENTER);
        
        psEnergyPanel = new JPanel();
        dataEnergyCards.add(psEnergyPanel, psId);
        GridBagLayout gbl_psEnergyPanel = new GridBagLayout();
        gbl_psEnergyPanel.columnWidths = new int[] {0, 0};
        gbl_psEnergyPanel.rowHeights = new int[] {0, 0};
        gbl_psEnergyPanel.columnWeights = new double[]{0.0, 1.0};
        gbl_psEnergyPanel.rowWeights = new double[]{0.0, 0.0};
        psEnergyPanel.setLayout(gbl_psEnergyPanel);
        
        lblPsEnergyRadius = new JLabel("Radius");
        GridBagConstraints gbc_lblPsEnergyRadius = new GridBagConstraints();
        gbc_lblPsEnergyRadius.insets = new Insets(0, 0, 5, 5);
        gbc_lblPsEnergyRadius.anchor = GridBagConstraints.EAST;
        gbc_lblPsEnergyRadius.gridx = 0;
        gbc_lblPsEnergyRadius.gridy = 0;
        psEnergyPanel.add(lblPsEnergyRadius, gbc_lblPsEnergyRadius);
        
        psEenergyRadius = new JTextField();
        psEenergyRadius.setHorizontalAlignment(SwingConstants.CENTER);
        psEenergyRadius.setText("12");
        GridBagConstraints gbc_psEenergyRadius = new GridBagConstraints();
        gbc_psEenergyRadius.fill = GridBagConstraints.HORIZONTAL;
        gbc_psEenergyRadius.insets = new Insets(0, 0, 5, 0);
        gbc_psEenergyRadius.gridx = 1;
        gbc_psEenergyRadius.gridy = 0;
        psEnergyPanel.add(psEenergyRadius, gbc_psEenergyRadius);
        psEenergyRadius.setColumns(10);
        
        lblPsEnergyBetaEBalloon = new JLabel("Beta E Balloon");
        GridBagConstraints gbc_lblPsEnergyBetaEBalloon = new GridBagConstraints();
        gbc_lblPsEnergyBetaEBalloon.insets = new Insets(0, 0, 0, 5);
        gbc_lblPsEnergyBetaEBalloon.anchor = GridBagConstraints.EAST;
        gbc_lblPsEnergyBetaEBalloon.gridx = 0;
        gbc_lblPsEnergyBetaEBalloon.gridy = 1;
        psEnergyPanel.add(lblPsEnergyBetaEBalloon, gbc_lblPsEnergyBetaEBalloon);
        
        psEnergyBetaBalloon = new JTextField();
        psEnergyBetaBalloon.setHorizontalAlignment(SwingConstants.CENTER);
        psEnergyBetaBalloon.setText("0.123");
        GridBagConstraints gbc_psEnergyBetaBalloon = new GridBagConstraints();
        gbc_psEnergyBetaBalloon.fill = GridBagConstraints.HORIZONTAL;
        gbc_psEnergyBetaBalloon.gridx = 1;
        gbc_psEnergyBetaBalloon.gridy = 1;
        psEnergyPanel.add(psEnergyBetaBalloon, gbc_psEnergyBetaBalloon);
        psEnergyBetaBalloon.setColumns(10);
        
        regularizationEnergyPanel = new JPanel();
        regularizationEnergyPanel.setBorder(new TitledBorder(null, "Energy Length (regularization)", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_regularizationEnergyPanel = new GridBagConstraints();
        gbc_regularizationEnergyPanel.insets = new Insets(0, 0, 5, 0);
        gbc_regularizationEnergyPanel.fill = GridBagConstraints.BOTH;
        gbc_regularizationEnergyPanel.gridx = 0;
        gbc_regularizationEnergyPanel.gridy = 2;
        energiesPanel.add(regularizationEnergyPanel, gbc_regularizationEnergyPanel);
        GridBagLayout gbl_regularizationEnergyPanel = new GridBagLayout();
        gbl_regularizationEnergyPanel.columnWidths = new int[]{0, 0, 0};
        gbl_regularizationEnergyPanel.rowHeights = new int[]{0, 0, 0, 0};
        gbl_regularizationEnergyPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        gbl_regularizationEnergyPanel.rowWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
        regularizationEnergyPanel.setLayout(gbl_regularizationEnergyPanel);
        
        lengthEnergyCombo = new JComboBox<>();
        lengthEnergyCombo.addItemListener(this);
        GridBagConstraints gbc_lengthEnergyCombo = new GridBagConstraints();
        gbc_lengthEnergyCombo.gridwidth = 2;
        gbc_lengthEnergyCombo.insets = new Insets(0, 0, 5, 0);
        gbc_lengthEnergyCombo.fill = GridBagConstraints.HORIZONTAL;
        gbc_lengthEnergyCombo.gridx = 0;
        gbc_lengthEnergyCombo.gridy = 0;
        regularizationEnergyPanel.add(lengthEnergyCombo, gbc_lengthEnergyCombo);
        
        String sphereRegularizationId = RegularizationType.values()[0].toString();
        String approxReguralizationId = RegularizationType.values()[1].toString();
        String noneReguralizationId = RegularizationType.values()[2].toString();
        
        lengthEnergyCombo.addItem(sphereRegularizationId);
        lengthEnergyCombo.addItem(approxReguralizationId);
        lengthEnergyCombo.addItem(noneReguralizationId);
        
        lblLambdaRegularization = new JLabel("Lambda Regularization");
        GridBagConstraints gbc_lblLambdaRegularization = new GridBagConstraints();
        gbc_lblLambdaRegularization.insets = new Insets(0, 0, 5, 5);
        gbc_lblLambdaRegularization.anchor = GridBagConstraints.EAST;
        gbc_lblLambdaRegularization.gridx = 0;
        gbc_lblLambdaRegularization.gridy = 1;
        regularizationEnergyPanel.add(lblLambdaRegularization, gbc_lblLambdaRegularization);
        
        lambdaRegularization = new JTextField();
        lambdaRegularization.setHorizontalAlignment(SwingConstants.CENTER);
        lambdaRegularization.setText("0");
        GridBagConstraints gbc_textField = new GridBagConstraints();
        gbc_textField.insets = new Insets(0, 0, 5, 0);
        gbc_textField.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField.gridx = 1;
        gbc_textField.gridy = 1;
        regularizationEnergyPanel.add(lambdaRegularization, gbc_textField);
        lambdaRegularization.setColumns(10);
        
        lengthEnergyCards = new JPanel();
        lengthEnergyCards.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "energy options", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0)));
        GridBagConstraints gbc_lengthEnergyCards = new GridBagConstraints();
        gbc_lengthEnergyCards.gridwidth = 2;
        gbc_lengthEnergyCards.fill = GridBagConstraints.BOTH;
        gbc_lengthEnergyCards.gridx = 0;
        gbc_lengthEnergyCards.gridy = 2;
        regularizationEnergyPanel.add(lengthEnergyCards, gbc_lengthEnergyCards);
        lengthEnergyCards.setLayout(new CardLayout(0, 0));
        
        sphereRegularizationPanel = new JPanel();
        lengthEnergyCards.add(sphereRegularizationPanel, sphereRegularizationId);
        GridBagLayout gbl_sphereRegularizationPanel = new GridBagLayout();
        gbl_sphereRegularizationPanel.columnWidths = new int[] {0, 0};
        gbl_sphereRegularizationPanel.rowHeights = new int[] {0};
        gbl_sphereRegularizationPanel.columnWeights = new double[]{0.0, 1.0};
        gbl_sphereRegularizationPanel.rowWeights = new double[]{0.0};
        sphereRegularizationPanel.setLayout(gbl_sphereRegularizationPanel);
        
        lblSphereReguralizationRK = new JLabel("R k");
        GridBagConstraints gbc_lblSphereReguralizationRK = new GridBagConstraints();
        gbc_lblSphereReguralizationRK.insets = new Insets(0, 0, 0, 5);
        gbc_lblSphereReguralizationRK.anchor = GridBagConstraints.EAST;
        gbc_lblSphereReguralizationRK.gridx = 0;
        gbc_lblSphereReguralizationRK.gridy = 0;
        sphereRegularizationPanel.add(lblSphereReguralizationRK, gbc_lblSphereReguralizationRK);
        
        sphereRegularizationRadiusK = new JTextField();
        sphereRegularizationRadiusK.setHorizontalAlignment(SwingConstants.CENTER);
        sphereRegularizationRadiusK.setText("0");
        GridBagConstraints gbc_sphereRegularizationRadiusK = new GridBagConstraints();
        gbc_sphereRegularizationRadiusK.fill = GridBagConstraints.HORIZONTAL;
        gbc_sphereRegularizationRadiusK.gridx = 1;
        gbc_sphereRegularizationRadiusK.gridy = 0;
        sphereRegularizationPanel.add(sphereRegularizationRadiusK, gbc_sphereRegularizationRadiusK);
        sphereRegularizationRadiusK.setColumns(10);
        
        approxReguralizationPanel = new JPanel();
        lengthEnergyCards.add(approxReguralizationPanel, approxReguralizationId);
        approxReguralizationPanel.setLayout(new BorderLayout(0, 0));
        
        lblNoOptionsAvailable_3 = new JLabel("No options available");
        lblNoOptionsAvailable_3.setHorizontalAlignment(SwingConstants.CENTER);
        approxReguralizationPanel.add(lblNoOptionsAvailable_3, BorderLayout.CENTER);
        
        noneReguralizationPanel = new JPanel();
        lengthEnergyCards.add(noneReguralizationPanel, noneReguralizationId);
        noneReguralizationPanel.setLayout(new BorderLayout(0, 0));
        
        lblNoOptionsAvailable_2 = new JLabel("No options available");
        lblNoOptionsAvailable_2.setHorizontalAlignment(SwingConstants.CENTER);
        noneReguralizationPanel.add(lblNoOptionsAvailable_2, BorderLayout.CENTER);
        
        initializationPanel = new JPanel();
        initializationPanel.setBorder(new TitledBorder(null, "Initialization", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_initializationPanel = new GridBagConstraints();
        gbc_initializationPanel.fill = GridBagConstraints.BOTH;
        gbc_initializationPanel.gridx = 0;
        gbc_initializationPanel.gridy = 4;
        energiesPanel.add(initializationPanel, gbc_initializationPanel);
        GridBagLayout gbl_initializationPanel = new GridBagLayout();
        gbl_initializationPanel.columnWidths = new int[]{0, 0};
        gbl_initializationPanel.rowHeights = new int[]{0, 0, 0};
        gbl_initializationPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gbl_initializationPanel.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        initializationPanel.setLayout(gbl_initializationPanel);
        
        initializationCombo = new JComboBox<>();
        initializationCombo.addItemListener(this);
        GridBagConstraints gbc_initializationCombo = new GridBagConstraints();
        gbc_initializationCombo.insets = new Insets(0, 0, 5, 0);
        gbc_initializationCombo.fill = GridBagConstraints.HORIZONTAL;
        gbc_initializationCombo.gridx = 0;
        gbc_initializationCombo.gridy = 0;
        initializationPanel.add(initializationCombo, gbc_initializationCombo);
        
        String rectangleInitPanelId = InitializationType.values()[0].toString();
        String bubblesInitPanelId = InitializationType.values()[1].toString();
        String localMaxInitPanelId = InitializationType.values()[2].toString();
        String roiInitPanelId = InitializationType.values()[3].toString();
        fileInitPanelId = InitializationType.values()[4].toString();
        
        initializationCombo.addItem(rectangleInitPanelId);
        initializationCombo.addItem(bubblesInitPanelId);
        initializationCombo.addItem(localMaxInitPanelId);
        initializationCombo.addItem(roiInitPanelId);
        initializationCombo.addItem(fileInitPanelId);
        
        initCards = new JPanel();
        initCards.setBorder(new TitledBorder(null, "initialization options / input", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, null));
        GridBagConstraints gbc_initCards = new GridBagConstraints();
        gbc_initCards.fill = GridBagConstraints.BOTH;
        gbc_initCards.gridx = 0;
        gbc_initCards.gridy = 1;
        initializationPanel.add(initCards, gbc_initCards);
        initCards.setLayout(new CardLayout(0, 0));
        
        rectangleInitPanel = new JPanel();
        initCards.add(rectangleInitPanel, rectangleInitPanelId);
        GridBagLayout gbl_rectangleInitPanel = new GridBagLayout();
        gbl_rectangleInitPanel.columnWidths = new int[] {0, 0};
        gbl_rectangleInitPanel.rowHeights = new int[] {0};
        gbl_rectangleInitPanel.columnWeights = new double[]{0.0, 1.0};
        gbl_rectangleInitPanel.rowWeights = new double[]{0.0};
        rectangleInitPanel.setLayout(gbl_rectangleInitPanel);
        
        lblBoxFillRatio = new JLabel("Box fill ratio");
        GridBagConstraints gbc_lblBoxFillRatio = new GridBagConstraints();
        gbc_lblBoxFillRatio.insets = new Insets(0, 0, 0, 5);
        gbc_lblBoxFillRatio.anchor = GridBagConstraints.EAST;
        gbc_lblBoxFillRatio.gridx = 0;
        gbc_lblBoxFillRatio.gridy = 0;
        rectangleInitPanel.add(lblBoxFillRatio, gbc_lblBoxFillRatio);
        
        rectangleInitBoxFillRatio = new JTextField();
        rectangleInitBoxFillRatio.setHorizontalAlignment(SwingConstants.CENTER);
        rectangleInitBoxFillRatio.setText("0.8");
        GridBagConstraints gbc_rectangleInitBoxFillRatio = new GridBagConstraints();
        gbc_rectangleInitBoxFillRatio.fill = GridBagConstraints.HORIZONTAL;
        gbc_rectangleInitBoxFillRatio.gridx = 1;
        gbc_rectangleInitBoxFillRatio.gridy = 0;
        rectangleInitPanel.add(rectangleInitBoxFillRatio, gbc_rectangleInitBoxFillRatio);
        rectangleInitBoxFillRatio.setColumns(10);
        
        bubblesInitPanel = new JPanel();
        initCards.add(bubblesInitPanel, bubblesInitPanelId);
        GridBagLayout gbl_bubblesInitPanel = new GridBagLayout();
        gbl_bubblesInitPanel.columnWidths = new int[] {0, 0};
        gbl_bubblesInitPanel.rowHeights = new int[] {0, 0};
        gbl_bubblesInitPanel.columnWeights = new double[]{0.0, 1.0};
        gbl_bubblesInitPanel.rowWeights = new double[]{0.0, 0.0};
        bubblesInitPanel.setLayout(gbl_bubblesInitPanel);
        
        lblBubbleRadius = new JLabel("Bubble Radius");
        GridBagConstraints gbc_lblBubbleRadius = new GridBagConstraints();
        gbc_lblBubbleRadius.insets = new Insets(0, 0, 5, 5);
        gbc_lblBubbleRadius.anchor = GridBagConstraints.EAST;
        gbc_lblBubbleRadius.gridx = 0;
        gbc_lblBubbleRadius.gridy = 0;
        bubblesInitPanel.add(lblBubbleRadius, gbc_lblBubbleRadius);
        
        bubblesInitRadius = new JTextField();
        bubblesInitRadius.setHorizontalAlignment(SwingConstants.CENTER);
        bubblesInitRadius.setText("10");
        GridBagConstraints gbc_bubblesInitRadius = new GridBagConstraints();
        gbc_bubblesInitRadius.insets = new Insets(0, 0, 5, 0);
        gbc_bubblesInitRadius.fill = GridBagConstraints.HORIZONTAL;
        gbc_bubblesInitRadius.gridx = 1;
        gbc_bubblesInitRadius.gridy = 0;
        bubblesInitPanel.add(bubblesInitRadius, gbc_bubblesInitRadius);
        bubblesInitRadius.setColumns(10);
        
        lblBubblePadding = new JLabel("Bubble Padding");
        GridBagConstraints gbc_lblBubblePadding = new GridBagConstraints();
        gbc_lblBubblePadding.insets = new Insets(0, 0, 0, 5);
        gbc_lblBubblePadding.anchor = GridBagConstraints.EAST;
        gbc_lblBubblePadding.gridx = 0;
        gbc_lblBubblePadding.gridy = 1;
        bubblesInitPanel.add(lblBubblePadding, gbc_lblBubblePadding);
        
        bubblesInitPadding = new JTextField();
        bubblesInitPadding.setHorizontalAlignment(SwingConstants.CENTER);
        bubblesInitPadding.setText("10");
        GridBagConstraints gbc_bubblesInitPadding = new GridBagConstraints();
        gbc_bubblesInitPadding.fill = GridBagConstraints.HORIZONTAL;
        gbc_bubblesInitPadding.gridx = 1;
        gbc_bubblesInitPadding.gridy = 1;
        bubblesInitPanel.add(bubblesInitPadding, gbc_bubblesInitPadding);
        bubblesInitPadding.setColumns(10);
        
        localMaxInitPanel = new JPanel();
        initCards.add(localMaxInitPanel, localMaxInitPanelId);
        GridBagLayout gbl_localMaxInitPanel = new GridBagLayout();
        gbl_localMaxInitPanel.columnWidths = new int[] {0, 0};
        gbl_localMaxInitPanel.rowHeights = new int[] {0, 0, 0, 0};
        gbl_localMaxInitPanel.columnWeights = new double[]{0.0, 1.0};
        gbl_localMaxInitPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0};
        localMaxInitPanel.setLayout(gbl_localMaxInitPanel);
        
        lblMaxInitRadius = new JLabel("Radius");
        GridBagConstraints gbc_lblMaxInitRadius = new GridBagConstraints();
        gbc_lblMaxInitRadius.insets = new Insets(0, 0, 5, 5);
        gbc_lblMaxInitRadius.anchor = GridBagConstraints.EAST;
        gbc_lblMaxInitRadius.gridx = 0;
        gbc_lblMaxInitRadius.gridy = 0;
        localMaxInitPanel.add(lblMaxInitRadius, gbc_lblMaxInitRadius);
        
        localMaxInitRadius = new JTextField();
        localMaxInitRadius.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbc_localMaxInitRadius = new GridBagConstraints();
        gbc_localMaxInitRadius.insets = new Insets(0, 0, 5, 0);
        gbc_localMaxInitRadius.fill = GridBagConstraints.HORIZONTAL;
        gbc_localMaxInitRadius.gridx = 1;
        gbc_localMaxInitRadius.gridy = 0;
        localMaxInitPanel.add(localMaxInitRadius, gbc_localMaxInitRadius);
        localMaxInitRadius.setColumns(10);
        
        lblMaxInitSigma = new JLabel("Sigma");
        GridBagConstraints gbc_lblMaxInitSigma = new GridBagConstraints();
        gbc_lblMaxInitSigma.insets = new Insets(0, 0, 5, 5);
        gbc_lblMaxInitSigma.anchor = GridBagConstraints.EAST;
        gbc_lblMaxInitSigma.gridx = 0;
        gbc_lblMaxInitSigma.gridy = 1;
        localMaxInitPanel.add(lblMaxInitSigma, gbc_lblMaxInitSigma);
        
        localMaxInitSigma = new JTextField();
        localMaxInitSigma.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbc_localMaxInitSigma = new GridBagConstraints();
        gbc_localMaxInitSigma.insets = new Insets(0, 0, 5, 0);
        gbc_localMaxInitSigma.fill = GridBagConstraints.HORIZONTAL;
        gbc_localMaxInitSigma.gridx = 1;
        gbc_localMaxInitSigma.gridy = 1;
        localMaxInitPanel.add(localMaxInitSigma, gbc_localMaxInitSigma);
        localMaxInitSigma.setColumns(10);
        
        lblMaxInitTolerance = new JLabel("Tolerance");
        GridBagConstraints gbc_lblMaxInitTolerance = new GridBagConstraints();
        gbc_lblMaxInitTolerance.insets = new Insets(0, 0, 5, 5);
        gbc_lblMaxInitTolerance.anchor = GridBagConstraints.EAST;
        gbc_lblMaxInitTolerance.gridx = 0;
        gbc_lblMaxInitTolerance.gridy = 2;
        localMaxInitPanel.add(lblMaxInitTolerance, gbc_lblMaxInitTolerance);
        
        localMaxInitTolerance = new JTextField();
        localMaxInitTolerance.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbc_localMaxInitTolerance = new GridBagConstraints();
        gbc_localMaxInitTolerance.insets = new Insets(0, 0, 5, 0);
        gbc_localMaxInitTolerance.fill = GridBagConstraints.HORIZONTAL;
        gbc_localMaxInitTolerance.gridx = 1;
        gbc_localMaxInitTolerance.gridy = 2;
        localMaxInitPanel.add(localMaxInitTolerance, gbc_localMaxInitTolerance);
        localMaxInitTolerance.setColumns(10);
        
        lblMaxInitMinimumRegionSize = new JLabel("Min. region size");
        GridBagConstraints gbc_lblMaxInitRegionTolerance = new GridBagConstraints();
        gbc_lblMaxInitRegionTolerance.insets = new Insets(0, 0, 5, 5);
        gbc_lblMaxInitRegionTolerance.gridx = 0;
        gbc_lblMaxInitRegionTolerance.gridy = 3;
        localMaxInitPanel.add(lblMaxInitMinimumRegionSize, gbc_lblMaxInitRegionTolerance);
        
        localMaxInitMinRegionSize = new JTextField();
        localMaxInitMinRegionSize.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbc_localMaxInitRegionTolerance = new GridBagConstraints();
        gbc_localMaxInitRegionTolerance.insets = new Insets(0, 0, 5, 0);
        gbc_localMaxInitRegionTolerance.fill = GridBagConstraints.HORIZONTAL;
        gbc_localMaxInitRegionTolerance.gridx = 1;
        gbc_localMaxInitRegionTolerance.gridy = 3;
        localMaxInitPanel.add(localMaxInitMinRegionSize, gbc_localMaxInitRegionTolerance);
        localMaxInitMinRegionSize.setColumns(10);
        
        roiInitiPanel = new JPanel();
        initCards.add(roiInitiPanel, roiInitPanelId);
        roiInitiPanel.setLayout(new BorderLayout(0, 0));
        
        lblNoOptionsAvailable_4 = new JLabel("No options available");
        lblNoOptionsAvailable_4.setHorizontalAlignment(SwingConstants.CENTER);
        roiInitiPanel.add(lblNoOptionsAvailable_4, BorderLayout.CENTER);
        
        fileInitPanel = new JPanel();
        initCards.add(fileInitPanel, fileInitPanelId);
        GridBagLayout gbl_fileInitPanel = new GridBagLayout();
        gbl_fileInitPanel.columnWidths = new int[] {0};
        gbl_fileInitPanel.rowHeights = new int[] {0, 0};
        gbl_fileInitPanel.columnWeights = new double[]{1.0};
        gbl_fileInitPanel.rowWeights = new double[]{0.0, 1.0};
        fileInitPanel.setLayout(gbl_fileInitPanel);
        
        lblInitFile = new JLabel("Choose input file:");
        GridBagConstraints gbc_lblInitFile = new GridBagConstraints();
        gbc_lblInitFile.insets = new Insets(0, 0, 5, 0);
        gbc_lblInitFile.gridx = 0;
        gbc_lblInitFile.gridy = 0;
        fileInitPanel.add(lblInitFile, gbc_lblInitFile);
        
        fileInitList = new JList<>();
        JScrollPane sp = new JScrollPane(fileInitList);
        fileInitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileInitList.setLayoutOrientation(JList.VERTICAL);
        fileInitList.setVisibleRowCount(4);
        GridBagConstraints gbc_fileInitList = new GridBagConstraints();
        gbc_fileInitList.fill = GridBagConstraints.BOTH;
        gbc_fileInitList.gridx = 0;
        gbc_fileInitList.gridy = 1;
        fileInitPanel.add(sp, gbc_fileInitList);
        
        otherSettingsPanel = new JPanel();
        otherSettingsPanel.setBorder(new TitledBorder(null, "Other Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_otherSettingsPanel = new GridBagConstraints();
        gbc_otherSettingsPanel.insets = new Insets(0, 0, 5, 0);
        gbc_otherSettingsPanel.fill = GridBagConstraints.BOTH;
        gbc_otherSettingsPanel.gridx = 1;
        gbc_otherSettingsPanel.gridy = 0;
        basePanel.add(otherSettingsPanel, gbc_otherSettingsPanel);
        GridBagLayout gbl_otherSettingsPanel = new GridBagLayout();
        gbl_otherSettingsPanel.columnWidths = new int[] {0, 0};
        gbl_otherSettingsPanel.rowHeights = new int[] {0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 20, 0, 0, 0};
        gbl_otherSettingsPanel.columnWeights = new double[]{1.0, 1.0};
        gbl_otherSettingsPanel.rowWeights = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0};
        otherSettingsPanel.setLayout(gbl_otherSettingsPanel);
        
        lblNewLabel_1 = new JLabel("Algorithm Settings");
        lblNewLabel_1.setFont(new Font("Lucida Grande", Font.BOLD, 13));
        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
        gbc_lblNewLabel_1.gridwidth = 2;
        gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
        gbc_lblNewLabel_1.gridx = 0;
        gbc_lblNewLabel_1.gridy = 0;
        otherSettingsPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
        
        lblNumberOfIterations = new JLabel("Number of Iterations");
        GridBagConstraints gbc_lblNumberOfIterations = new GridBagConstraints();
        gbc_lblNumberOfIterations.anchor = GridBagConstraints.WEST;
        gbc_lblNumberOfIterations.insets = new Insets(0, 0, 5, 5);
        gbc_lblNumberOfIterations.gridx = 0;
        gbc_lblNumberOfIterations.gridy = 1;
        otherSettingsPanel.add(lblNumberOfIterations, gbc_lblNumberOfIterations);
        
        numOfIterations = new JTextField();
        numOfIterations.setHorizontalAlignment(SwingConstants.CENTER);
        numOfIterations.setText("100");
        GridBagConstraints gbc_numOfIterations = new GridBagConstraints();
        gbc_numOfIterations.insets = new Insets(0, 0, 5, 0);
        gbc_numOfIterations.fill = GridBagConstraints.HORIZONTAL;
        gbc_numOfIterations.gridx = 1;
        gbc_numOfIterations.gridy = 1;
        otherSettingsPanel.add(numOfIterations, gbc_numOfIterations);
        numOfIterations.setColumns(10);
        
        separator = new JSeparator();
        GridBagConstraints gbc_separator = new GridBagConstraints();
        gbc_separator.fill = GridBagConstraints.HORIZONTAL;
        gbc_separator.gridwidth = 2;
        gbc_separator.insets = new Insets(0, 0, 5, 0);
        gbc_separator.gridx = 0;
        gbc_separator.gridy = 2;
        otherSettingsPanel.add(separator, gbc_separator);
        
        lblGeometricConstrains = new JLabel("Geometric Constrains");
        lblGeometricConstrains.setFont(new Font("Lucida Grande", Font.BOLD, 13));
        GridBagConstraints gbc_lblGeometricConstrains = new GridBagConstraints();
        gbc_lblGeometricConstrains.gridwidth = 2;
        gbc_lblGeometricConstrains.insets = new Insets(0, 0, 5, 0);
        gbc_lblGeometricConstrains.gridx = 0;
        gbc_lblGeometricConstrains.gridy = 3;
        otherSettingsPanel.add(lblGeometricConstrains, gbc_lblGeometricConstrains);
        
        chckbxFussion = new JCheckBox("Fussion");
        GridBagConstraints gbc_chckbxFussion = new GridBagConstraints();
        gbc_chckbxFussion.anchor = GridBagConstraints.WEST;
        gbc_chckbxFussion.gridwidth = 2;
        gbc_chckbxFussion.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxFussion.gridx = 0;
        gbc_chckbxFussion.gridy = 4;
        otherSettingsPanel.add(chckbxFussion, gbc_chckbxFussion);
        
        chckbxFission = new JCheckBox("Fission");
        GridBagConstraints gbc_chckbxFission = new GridBagConstraints();
        gbc_chckbxFission.anchor = GridBagConstraints.WEST;
        gbc_chckbxFission.gridwidth = 2;
        gbc_chckbxFission.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxFission.gridx = 0;
        gbc_chckbxFission.gridy = 5;
        otherSettingsPanel.add(chckbxFission, gbc_chckbxFission);
        
        chckbxHandles = new JCheckBox("Handles");
        GridBagConstraints gbc_chckbxHandles = new GridBagConstraints();
        gbc_chckbxHandles.anchor = GridBagConstraints.WEST;
        gbc_chckbxHandles.gridwidth = 2;
        gbc_chckbxHandles.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxHandles.gridx = 0;
        gbc_chckbxHandles.gridy = 6;
        otherSettingsPanel.add(chckbxHandles, gbc_chckbxHandles);
        
        separator_1 = new JSeparator();
        GridBagConstraints gbc_separator_1 = new GridBagConstraints();
        gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
        gbc_separator_1.gridwidth = 2;
        gbc_separator_1.insets = new Insets(0, 0, 5, 0);
        gbc_separator_1.gridx = 0;
        gbc_separator_1.gridy = 7;
        otherSettingsPanel.add(separator_1, gbc_separator_1);
        
        lblMcmcSettings = new JLabel("MCMC settings");
        lblMcmcSettings.setFont(new Font("Lucida Grande", Font.BOLD, 13));
        GridBagConstraints gbc_lblMcmcSettings = new GridBagConstraints();
        gbc_lblMcmcSettings.gridwidth = 2;
        gbc_lblMcmcSettings.insets = new Insets(0, 0, 5, 0);
        gbc_lblMcmcSettings.gridx = 0;
        gbc_lblMcmcSettings.gridy = 8;
        otherSettingsPanel.add(lblMcmcSettings, gbc_lblMcmcSettings);
        
        lblBurninFactor = new JLabel("burn-in factor [0-1]");
        GridBagConstraints gbc_lblBurninFactor = new GridBagConstraints();
        gbc_lblBurninFactor.insets = new Insets(0, 0, 5, 5);
        gbc_lblBurninFactor.anchor = GridBagConstraints.WEST;
        gbc_lblBurninFactor.gridx = 0;
        gbc_lblBurninFactor.gridy = 9;
        otherSettingsPanel.add(lblBurninFactor, gbc_lblBurninFactor);
        
        burnInFactor = new JTextField();
        burnInFactor.setHorizontalAlignment(SwingConstants.CENTER);
        burnInFactor.setText("0.1");
        GridBagConstraints gbc_burnInFactor = new GridBagConstraints();
        gbc_burnInFactor.insets = new Insets(0, 0, 5, 0);
        gbc_burnInFactor.fill = GridBagConstraints.HORIZONTAL;
        gbc_burnInFactor.gridx = 1;
        gbc_burnInFactor.gridy = 9;
        otherSettingsPanel.add(burnInFactor, gbc_burnInFactor);
        burnInFactor.setColumns(10);
        
        lblOffboundaryProbability = new JLabel("off-boundary probability [0-1]");
        GridBagConstraints gbc_lblOffboundaryProbability = new GridBagConstraints();
        gbc_lblOffboundaryProbability.insets = new Insets(0, 0, 5, 5);
        gbc_lblOffboundaryProbability.anchor = GridBagConstraints.WEST;
        gbc_lblOffboundaryProbability.gridx = 0;
        gbc_lblOffboundaryProbability.gridy = 10;
        otherSettingsPanel.add(lblOffboundaryProbability, gbc_lblOffboundaryProbability);
        
        offBoundaryProbability = new JTextField();
        offBoundaryProbability.setHorizontalAlignment(SwingConstants.CENTER);
        offBoundaryProbability.setText("0.2");
        GridBagConstraints gbc_offBoundaryProbability = new GridBagConstraints();
        gbc_offBoundaryProbability.insets = new Insets(0, 0, 5, 0);
        gbc_offBoundaryProbability.fill = GridBagConstraints.HORIZONTAL;
        gbc_offBoundaryProbability.gridx = 1;
        gbc_offBoundaryProbability.gridy = 10;
        otherSettingsPanel.add(offBoundaryProbability, gbc_offBoundaryProbability);
        offBoundaryProbability.setColumns(10);
        
        chckbxPairProposal = new JCheckBox("pair proposal");
        GridBagConstraints gbc_chckbxPairProposal = new GridBagConstraints();
        gbc_chckbxPairProposal.anchor = GridBagConstraints.WEST;
        gbc_chckbxPairProposal.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxPairProposal.gridx = 0;
        gbc_chckbxPairProposal.gridy = 11;
        otherSettingsPanel.add(chckbxPairProposal, gbc_chckbxPairProposal);
        
        chckbxBiasedProposal = new JCheckBox("biased proposal");
        GridBagConstraints gbc_chckbxBiasedProposal = new GridBagConstraints();
        gbc_chckbxBiasedProposal.anchor = GridBagConstraints.WEST;
        gbc_chckbxBiasedProposal.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxBiasedProposal.gridx = 0;
        gbc_chckbxBiasedProposal.gridy = 12;
        otherSettingsPanel.add(chckbxBiasedProposal, gbc_chckbxBiasedProposal);
        
        separator_2 = new JSeparator();
        GridBagConstraints gbc_separator_2 = new GridBagConstraints();
        gbc_separator_2.fill = GridBagConstraints.HORIZONTAL;
        gbc_separator_2.gridwidth = 2;
        gbc_separator_2.insets = new Insets(0, 0, 5, 5);
        gbc_separator_2.gridx = 0;
        gbc_separator_2.gridy = 13;
        otherSettingsPanel.add(separator_2, gbc_separator_2);
        
        lblOutputSettings = new JLabel("Output Settings");
        lblOutputSettings.setFont(new Font("Lucida Grande", Font.BOLD, 13));
        GridBagConstraints gbc_lblOutputSettings = new GridBagConstraints();
        gbc_lblOutputSettings.insets = new Insets(0, 0, 5, 0);
        gbc_lblOutputSettings.gridwidth = 2;
        gbc_lblOutputSettings.gridx = 0;
        gbc_lblOutputSettings.gridy = 14;
        otherSettingsPanel.add(lblOutputSettings, gbc_lblOutputSettings);
        
        chckbxShowLabelImage = new JCheckBox("show label image");
        GridBagConstraints gbc_chckbxShowLabelImage = new GridBagConstraints();
        gbc_chckbxShowLabelImage.fill = GridBagConstraints.HORIZONTAL;
        gbc_chckbxShowLabelImage.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxShowLabelImage.gridx = 0;
        gbc_chckbxShowLabelImage.gridy = 15;
        otherSettingsPanel.add(chckbxShowLabelImage, gbc_chckbxShowLabelImage);
        
        chckbxSaveLabelImage = new JCheckBox("save label image");
        GridBagConstraints gbc_chckbxSaveLabelImage = new GridBagConstraints();
        gbc_chckbxSaveLabelImage.fill = GridBagConstraints.HORIZONTAL;
        gbc_chckbxSaveLabelImage.insets = new Insets(0, 0, 5, 0);
        gbc_chckbxSaveLabelImage.gridx = 1;
        gbc_chckbxSaveLabelImage.gridy = 15;
        otherSettingsPanel.add(chckbxSaveLabelImage, gbc_chckbxSaveLabelImage);
        
        chckbxShowProbabilityImage = new JCheckBox("show probability image");
        GridBagConstraints gbc_chckbxShowProbabilityImage = new GridBagConstraints();
        gbc_chckbxShowProbabilityImage.fill = GridBagConstraints.HORIZONTAL;
        gbc_chckbxShowProbabilityImage.insets = new Insets(0, 0, 0, 5);
        gbc_chckbxShowProbabilityImage.gridx = 0;
        gbc_chckbxShowProbabilityImage.gridy = 16;
        otherSettingsPanel.add(chckbxShowProbabilityImage, gbc_chckbxShowProbabilityImage);
        
        chckbxSaveProbabilityImage = new JCheckBox("save probability image");
        GridBagConstraints gbc_chckbxSaveProbabilityImage = new GridBagConstraints();
        gbc_chckbxSaveProbabilityImage.fill = GridBagConstraints.HORIZONTAL;
        gbc_chckbxSaveProbabilityImage.gridx = 1;
        gbc_chckbxSaveProbabilityImage.gridy = 16;
        otherSettingsPanel.add(chckbxSaveProbabilityImage, gbc_chckbxSaveProbabilityImage);
        
        statusPanel = new JPanel();
        statusPanel.setBorder(new TitledBorder(null, "Status", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_statusPanel = new GridBagConstraints();
        gbc_statusPanel.gridwidth = 2;
        gbc_statusPanel.insets = new Insets(0, 0, 5, 0);
        gbc_statusPanel.fill = GridBagConstraints.BOTH;
        gbc_statusPanel.gridx = 0;
        gbc_statusPanel.gridy = 1;
        basePanel.add(statusPanel, gbc_statusPanel);
        
        lblStatus = new JLabel("Status");
        statusPanel.add(lblStatus);
        
        buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new TitledBorder(null, "Control", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_buttonsPanel = new GridBagConstraints();
        gbc_buttonsPanel.insets = new Insets(0, 0, 5, 0);
        gbc_buttonsPanel.gridwidth = 2;
        gbc_buttonsPanel.fill = GridBagConstraints.BOTH;
        gbc_buttonsPanel.gridx = 0;
        gbc_buttonsPanel.gridy = 2;
        basePanel.add(buttonsPanel, gbc_buttonsPanel);
        GridBagLayout gbl_buttonsPanel = new GridBagLayout();
        gbl_buttonsPanel.columnWidths = new int[] {0, 30, 0, 0};
        gbl_buttonsPanel.rowHeights = new int[] {0};
        gbl_buttonsPanel.columnWeights = new double[]{1.0, 0.0, 1.0, 1.0};
        gbl_buttonsPanel.rowWeights = new double[]{1.0};
        buttonsPanel.setLayout(gbl_buttonsPanel);
        
        btnResetSettings = new JButton("Reset Settings");
        btnResetSettings.addActionListener(this);
        GridBagConstraints gbc_btnResetSettings = new GridBagConstraints();
        gbc_btnResetSettings.fill = GridBagConstraints.BOTH;
        gbc_btnResetSettings.insets = new Insets(0, 0, 0, 5);
        gbc_btnResetSettings.gridx = 0;
        gbc_btnResetSettings.gridy = 0;
        buttonsPanel.add(btnResetSettings, gbc_btnResetSettings);
        
        btnOk = new JButton("OK");
        btnOk.addActionListener(this);
        GridBagConstraints gbc_btnOk = new GridBagConstraints();
        gbc_btnOk.fill = GridBagConstraints.BOTH;
        gbc_btnOk.insets = new Insets(0, 0, 0, 5);
        gbc_btnOk.gridx = 2;
        gbc_btnOk.gridy = 0;
        buttonsPanel.add(btnOk, gbc_btnOk);
        
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this);
        GridBagConstraints gbc_btnCancel = new GridBagConstraints();
        gbc_btnCancel.fill = GridBagConstraints.BOTH;
        gbc_btnCancel.gridx = 3;
        gbc_btnCancel.gridy = 0;
        buttonsPanel.add(btnCancel, gbc_btnCancel);
        
        citationPanel = new JPanel();
        citationPanel.setBorder(new TitledBorder(null, "Citation info", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_citationPanel = new GridBagConstraints();
        gbc_citationPanel.gridwidth = 2;
        gbc_citationPanel.insets = new Insets(0, 0, 0, 5);
        gbc_citationPanel.fill = GridBagConstraints.BOTH;
        gbc_citationPanel.gridx = 0;
        gbc_citationPanel.gridy = 3;
        basePanel.add(citationPanel, gbc_citationPanel);
        citationPanel.setLayout(new BorderLayout(0, 0));
        
        txtCitationInfo = new JTextArea();
        txtCitationInfo.setEditable(false);
        txtCitationInfo.setBackground(SystemColor.window);
        txtCitationInfo.setText("Lorem ipsum... Lorem ipsum.");
        txtCitationInfo.setColumns(10);
        citationPanel.add(txtCitationInfo, BorderLayout.CENTER);
    
        addToolTipTexts();
    }
    
    private void addToolTipTexts() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                dataEnergyCombo.setToolTipText("<html>This is the Energy that depends on the input image:<br>PC Gauss - Piecewise constant with Gauss correction<br>PC - Piecewise constant<br>PS - Piecewise smooth</html>");
                psEenergyRadius.setToolTipText("Radius of the sphere where the mean of the intensity is calculated");
                psEnergyBetaBalloon.setToolTipText("Beta controls the strenght of the Balloon force");

                lengthEnergyCombo.setToolTipText("<html>From a practical point of view, this energy locally reduces concavity, tips , and globally the length of the countor region.<br> Sphere Reguralization - curvature regularization<br>Approximative - Countor length regularization<br>None - do not use any lenght regularization</html>");
                lambdaRegularization.setToolTipText("Is a scaling factor for the Energy length, this term in general is sensitive to the shape of the region [0, inf)");
                sphereRegularizationRadiusK.setToolTipText("Radius of the hypersphere in the curvature regularization");

                initializationCombo.setToolTipText("<html>Choose one of a methods to initialize regions:<br>Rectangle - creates rectangle in a center of image<br>Bubbles - creates a grid of bubbles<br>Local maximum - creates regions around local maxima of input image<br>ROI 2D - region is taken from ROI defined in input image<br>File with regions - regions are taken from provided file</html>");
                rectangleInitBoxFillRatio.setToolTipText("ratio is the division of the side of the rectangle with the side of image on each dimensions");
                bubblesInitRadius.setToolTipText("radius of single bubble");
                bubblesInitPadding.setToolTipText("distance between bubble centers");
                localMaxInitRadius.setToolTipText("if minimum region size is not reached then bubble with this radius is created around maximum");
                localMaxInitSigma.setToolTipText("sigma for gauss pre-smoothing before finding local maxima");
                localMaxInitTolerance.setToolTipText("tolerance in range 0-1 (0 no tolerance at all, 1 full tolerance) of points around maxima points");
                localMaxInitMinRegionSize.setToolTipText("if number of found points around maixmum is less then minimum region size then sphere bubble with provided radius is drawn in maximum point");

                numOfIterations.setToolTipText("Number of iteration that algorithm will perform [1, inf)");
                chckbxFussion.setToolTipText("Allow fusion of regions");
                chckbxFission.setToolTipText("Allow fission of region");
                chckbxHandles.setToolTipText("Allow handles (holes) inside a region");

                offBoundaryProbability.setToolTipText("Probability of off-boundary sampling [0 - 1]");
                burnInFactor.setToolTipText("Burn-in phase factor of all iterations [0 - 1]");

                chckbxPairProposal.setToolTipText("In each iteration one move corresponds to a neighboring pair of particles");
                chckbxBiasedProposal.setToolTipText("enables the biased-proposal mode in order to propose smooth shapes");

                chckbxShowLabelImage.setToolTipText("Shows last generated (in last iteration) label image");
                chckbxSaveLabelImage.setToolTipText("Saves last generated (in last iteration) label image");
                chckbxShowProbabilityImage.setToolTipText("Shows probability image crated from all iterations after burn-in phase");
                chckbxSaveProbabilityImage.setToolTipText("Saves probability image crated from all iterations after burn-in phase");
            }});
    }
    

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == initializationCombo) {
            do_initializationCombo_itemStateChanged(e);
        }
        if (e.getSource() == lengthEnergyCombo) {
            do_lengthEnergyCombo_itemStateChanged(e);
        }
        if (e.getSource() == dataEnergyCombo) {
            do_dataEnergyCombo_itemStateChanged(e);
        }
    }
    
    protected void do_dataEnergyCombo_itemStateChanged(ItemEvent e) {
        if (dataEnergyCards != null) {
            CardLayout cl = (CardLayout) dataEnergyCards.getLayout();
            cl.show(dataEnergyCards, (String)e.getItem());
        }
    }
    
    protected void do_lengthEnergyCombo_itemStateChanged(ItemEvent e) {
        if (lengthEnergyCards != null) {
            CardLayout cl = (CardLayout) lengthEnergyCards.getLayout();
            cl.show(lengthEnergyCards, (String)e.getItem());
        }
    }
    protected void do_initializationCombo_itemStateChanged(ItemEvent e) {
        if (initCards != null) {
            CardLayout cl = (CardLayout) initCards.getLayout();
            cl.show(initCards, (String)e.getItem());
            // Update files when file init chosen
            if (((String)e.getItem()).equals(fileInitPanelId)) {
                updateFileInitList();
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCancel) {
            cancel();
        }
        if (e.getSource() == btnOk) {
            ok();
        }
        if (e.getSource() == btnResetSettings) {
            resetSettings();
        }
    }
    
    abstract protected void updateFileInitList();
    abstract protected void resetSettings();
    abstract protected void ok();
    abstract protected void cancel();
}
