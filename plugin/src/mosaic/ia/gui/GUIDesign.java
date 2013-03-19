package mosaic.ia.gui;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import javax.swing.SwingConstants;

import javax.swing.JLabel;
import javax.swing.JComboBox;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import org.eclipse.wb.swing.FocusTraversalOnArray;

import sun.security.provider.SystemSigner;

import java.awt.Component;
import javax.swing.JInternalFrame;
import java.awt.ComponentOrientation;
import java.awt.GridBagLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Dimension;
import javax.swing.JTextArea;
import java.awt.SystemColor;
import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.DefaultComboBoxModel;
import javax.swing.border.Border;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Vector;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.ia.Analysis;
import mosaic.ia.PotentialFunctions;
import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.ImageProcessUtils;
import net.miginfocom.swing.MigLayout;
import javax.swing.SpringLayout;
import javax.vecmath.Point3d;

public class GUIDesign implements ActionListener  {

	public JFrame frmInteractionAnalysis;
	private String [] items={"Hernquist","Step","Linear type 1","Linear type 2","Plummer","Non-parametric"};
	//private JTextField textField;
	//private JTextField textField_1;

	private double gridSize=.5;
	private ImagePlus imgx,imgy;
	private JComboBox jcb;
	private int potentialType=PotentialFunctions.HERNQUIST;
	private Point3d [] Xcoords, Ycoords;
	
	 private Analysis a;
	 private int monteCarloRunsForTest=1000;
	 private int numReRuns=10;
private double qkernelWeight=.001;
private double pkernelWeight=1;

		private double alpha=.05;
		private JFormattedTextField mCRuns, numSupport,smoothnessNP,gridSizeInp,reRuns,kernelWeightq, kernelWeightp;
		
		private JFormattedTextField alphaField;
	
	    
	    private JButton browseY, browseX, btnCalculateDistances,estimate,test,genMask, applyMask, loadMask, resetMask,btnLoadCsvFileX,btnLoadCsvFileY;
	    private Border blackBorder;
	    private JTextArea textArea;
	  
	 
	    JLabel lblsupportPts,lblSmoothness;
	    private JFormattedTextField txtXmin;
	    private JFormattedTextField txtYmin;
	    private JFormattedTextField txtZmin;
	    private JFormattedTextField txtXmax;
	    private JFormattedTextField txtYmax;
	    private JFormattedTextField txtZmax;
	    private double xmin=Double.MAX_VALUE,ymin=Double.MAX_VALUE,zmin=Double.MAX_VALUE,xmax=Double.MAX_VALUE,ymax=Double.MAX_VALUE,zmax=Double.MAX_VALUE;
	    private JLabel lblKernelWeightq,lblKernelWeightp;
	
	  
	 //   private int state=GUIStates.LOAD_IMAGES;
	    
	/**
	 * Launch the application.
	 */
	/*public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUIDesign window = new GUIDesign();
					window.frmInteractionAnalysis.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}*/

	/**
	 * Create the application.
	 */
	public GUIDesign() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmInteractionAnalysis = new JFrame();
		blackBorder=BorderFactory.createLineBorder(Color.black);
	    browseX = new JButton("Open");
	
		
		browseY = new JButton("Open");
		
		JPanel panel_5 = new JPanel();
		panel_5.setBorder(blackBorder);
		JPanel panel_7 = new JPanel();
		panel_7.setBorder(blackBorder);
		JPanel panel_6 = new JPanel();
		panel_6.setBorder(blackBorder);
		textArea = new JTextArea("Please refer to and cite: " +
		        "Jo A. Helmuth et al., Beyond co-localization:\n  inferring spatial " +
		        "interactions between sub-cellular structures from  \n microscopy images. " +
		        "BMC Bioinformatics, 11:372, 2010 " +
		        "\nFreely downloadable from:\n http://www.biomedcentral.com/1471-2105/11/372");
		textArea.setBackground(UIManager.getColor("Button.background"));
		
		GroupLayout gl_panel_6 = new GroupLayout(panel_6);
		gl_panel_6.setHorizontalGroup(
			gl_panel_6.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel_6.createSequentialGroup()
					.addContainerGap()
					.addComponent(textArea, GroupLayout.PREFERRED_SIZE, 430, Short.MAX_VALUE)
					.addContainerGap())
		);
		gl_panel_6.setVerticalGroup(
			gl_panel_6.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_6.createSequentialGroup()
					.addComponent(textArea, GroupLayout.PREFERRED_SIZE, 88, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(9, Short.MAX_VALUE))
		);
		panel_6.setLayout(gl_panel_6);
		
/*		JLabel lblImageX = new JLabel("Image X");
		
		JLabel lblImageY = new JLabel("Reference image Y");
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGap(98)
					.addComponent(lblImageX)
					.addGap(74)
					.addComponent(browseX, GroupLayout.PREFERRED_SIZE, 176, GroupLayout.PREFERRED_SIZE))
				.addGroup(gl_panel.createSequentialGroup()
					.addGap(33)
					.addComponent(lblImageY)
					.addGap(74)
					.addComponent(browseY, GroupLayout.PREFERRED_SIZE, 176, GroupLayout.PREFERRED_SIZE))
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGap(6)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(5)
							.addComponent(lblImageX))
						.addComponent(browseX))
					.addGap(6)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(5)
							.addComponent(lblImageY))
						.addComponent(browseY)))
		);
		panel.setLayout(gl_panel);*/
		
		JLabel lblHypothesisTesting = new JLabel("Hypothesis testing");
		
		JLabel lblMonteCarloRuns = new JLabel("Monte carlo runs");
		
		mCRuns = new JFormattedTextField();
		mCRuns.setHorizontalAlignment(SwingConstants.CENTER);
		mCRuns.setText(""+monteCarloRunsForTest);
		mCRuns.setColumns(10);
		
		JLabel lblSignificanceLevel = new JLabel("Significance level");
		
		alphaField = new JFormattedTextField();
		alphaField.setHorizontalAlignment(SwingConstants.CENTER);
		alphaField.setText(""+alpha);
		alphaField.setColumns(10);
		alphaField.addActionListener(this);
		
		 test = new JButton("Test");
		GroupLayout gl_panel_7 = new GroupLayout(panel_7);
		gl_panel_7.setHorizontalGroup(
			gl_panel_7.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_7.createSequentialGroup()
					.addGap(24)
					.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
						.addComponent(lblMonteCarloRuns)
						.addComponent(lblSignificanceLevel))
					.addGroup(gl_panel_7.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_panel_7.createSequentialGroup()
							.addGap(21)
							.addComponent(lblHypothesisTesting, GroupLayout.PREFERRED_SIZE, 129, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(152, Short.MAX_VALUE))
						.addGroup(gl_panel_7.createSequentialGroup()
							.addGap(148)
							.addGroup(gl_panel_7.createParallelGroup(Alignment.TRAILING)
								.addComponent(alphaField)
								.addComponent(mCRuns))
							.addGap(23))))
				.addGroup(gl_panel_7.createSequentialGroup()
					.addGap(101)
					.addComponent(test, GroupLayout.PREFERRED_SIZE, 233, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(104, Short.MAX_VALUE))
		);
		gl_panel_7.setVerticalGroup(
			gl_panel_7.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_7.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblHypothesisTesting)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE)
						.addComponent(mCRuns, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblMonteCarloRuns))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblSignificanceLevel)
						.addComponent(alphaField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
					.addComponent(test)
					.addContainerGap())
		);
		panel_7.setLayout(gl_panel_7);
		
		JLabel lblPotentialEstimation = new JLabel("Potential estimation");
		
		 jcb = new JComboBox(items);
		//jcb.setModel(new DefaultComboBoxModel(potentialOptions));
		jcb.addActionListener(this);
		
		 estimate = new JButton("Estimate");
		estimate.setActionCommand("Estimate");
		
		JLabel lblPotentialShape = new JLabel("Potential:");
		
		numSupport = new JFormattedTextField();

		numSupport.setHorizontalAlignment(SwingConstants.CENTER);

		numSupport.setText(""+PotentialFunctions.NONPARAM_WEIGHT_SIZE);
		numSupport.setColumns(10);
		numSupport.setEnabled(false);
		numSupport.addActionListener(this);
		
		smoothnessNP = new JFormattedTextField();

		smoothnessNP.setHorizontalAlignment(SwingConstants.CENTER);

		smoothnessNP.setText(""+PotentialFunctions.NONPARAM_SMOOTHNESS);
		smoothnessNP.setColumns(10);
		smoothnessNP.setEnabled(false);
		smoothnessNP.addActionListener(this);
		
		 lblsupportPts = new JLabel("#Support pts:");
		lblsupportPts.setEnabled(false);
		
		 lblSmoothness = new JLabel("Smoothness:");
		lblSmoothness.setEnabled(false);
		
		JLabel lblRepeatEstimation = new JLabel("Repeat estimation:");
		
		reRuns = new JFormattedTextField();
		reRuns.setColumns(10);
		reRuns.setText(numReRuns+"");
		reRuns.addActionListener(this);
		GroupLayout gl_panel_5 = new GroupLayout(panel_5);
		gl_panel_5.setHorizontalGroup(
			gl_panel_5.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel_5.createSequentialGroup()
					.addGap(107)
					.addComponent(estimate, GroupLayout.PREFERRED_SIZE, 221, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(92, Short.MAX_VALUE))
				.addGroup(gl_panel_5.createSequentialGroup()
					.addGap(161)
					.addComponent(lblPotentialEstimation)
					.addContainerGap(133, Short.MAX_VALUE))
				.addGroup(gl_panel_5.createSequentialGroup()
					.addGroup(gl_panel_5.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_5.createSequentialGroup()
							.addGap(10)
							.addComponent(lblsupportPts)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(numSupport, GroupLayout.PREFERRED_SIZE, 74, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
							.addComponent(lblSmoothness)
							.addGap(18)
							.addComponent(smoothnessNP, GroupLayout.PREFERRED_SIZE, 71, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel_5.createSequentialGroup()
							.addContainerGap()
							.addComponent(lblPotentialShape)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(jcb, GroupLayout.PREFERRED_SIZE, 126, GroupLayout.PREFERRED_SIZE)
							.addGap(18)
							.addComponent(lblRepeatEstimation)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(reRuns, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		gl_panel_5.setVerticalGroup(
			gl_panel_5.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel_5.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblPotentialEstimation)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_5.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblPotentialShape)
						.addComponent(jcb, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblRepeatEstimation)
						.addComponent(reRuns, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_5.createParallelGroup(Alignment.BASELINE)
						.addComponent(smoothnessNP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblsupportPts)
						.addComponent(numSupport, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblSmoothness))
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(estimate)
					.addContainerGap())
		);
		panel_5.setLayout(gl_panel_5);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		//tabbedPane.setEnabled(false);
		
		JTabbedPane tabbedPane_1 = new JTabbedPane(JTabbedPane.TOP);
		
		JPanel panel_3 = new JPanel();
		tabbedPane_1.addTab("Apply mask", null, panel_3, null);
		tabbedPane_1.setEnabledAt(0, true);
		SpringLayout sl_panel_3 = new SpringLayout();
		panel_3.setLayout(sl_panel_3);
		
		 genMask = new JButton("Generate");
		 genMask.setToolTipText("Generate a new mask from the opened Reference image");
		 sl_panel_3.putConstraint(SpringLayout.NORTH, genMask, 16, SpringLayout.NORTH, panel_3);
		 sl_panel_3.putConstraint(SpringLayout.WEST, genMask, 0, SpringLayout.WEST, panel_3);
		 sl_panel_3.putConstraint(SpringLayout.EAST, genMask, 123, SpringLayout.WEST, panel_3);
		 panel_3.add(genMask);
		 genMask.addActionListener(this);
		 
		 loadMask = new JButton("Load");
		 loadMask.setToolTipText("Load an existing mask from a file");
		 sl_panel_3.putConstraint(SpringLayout.NORTH, loadMask, 0, SpringLayout.NORTH, genMask);
		 sl_panel_3.putConstraint(SpringLayout.WEST, loadMask, 2, SpringLayout.EAST, genMask);
		 panel_3.add(loadMask);
		 loadMask.addActionListener(this);
		 
		 applyMask = new JButton("Apply");
		 applyMask.setToolTipText("Apply the open mask");
		 sl_panel_3.putConstraint(SpringLayout.EAST, applyMask, -107, SpringLayout.EAST, panel_3);
		 sl_panel_3.putConstraint(SpringLayout.EAST, loadMask, -6, SpringLayout.WEST, applyMask);
		 sl_panel_3.putConstraint(SpringLayout.NORTH, applyMask, 0, SpringLayout.NORTH, genMask);
		 panel_3.add(applyMask);
		 applyMask.addActionListener(this);
		 
		 
		 resetMask = new JButton("Reset");
		 resetMask.setToolTipText("Reset mask ");
		 sl_panel_3.putConstraint(SpringLayout.WEST, resetMask, 6, SpringLayout.EAST, applyMask);
		 sl_panel_3.putConstraint(SpringLayout.NORTH, resetMask, 0, SpringLayout.NORTH, genMask);
		 sl_panel_3.putConstraint(SpringLayout.EAST, resetMask, -10, SpringLayout.EAST, panel_3);
		 panel_3.add(resetMask);
		 resetMask.addActionListener(this);
		JPanel panel_4 = new JPanel();
		
		 btnCalculateDistances = new JButton("Calculate distances");
		 
		 gridSizeInp = new JFormattedTextField();
		 gridSizeInp.setHorizontalAlignment(SwingConstants.CENTER);

		 gridSizeInp.setText(""+gridSize);
		 gridSizeInp.setColumns(10);
		 gridSizeInp.addActionListener(this);
		 
		 JLabel lblGridSize = new JLabel("Grid spacing:");
		 
		 lblKernelWeightq = new JLabel("Kernel wt(q):");
		 
		 kernelWeightq=new JFormattedTextField();
		 kernelWeightq.setText(qkernelWeight+"");
		 kernelWeightq.addActionListener(this);
		 
		 lblKernelWeightp = new JLabel("Kernel wt(p):");
		 
		 kernelWeightp= new JFormattedTextField();
		 kernelWeightp.setText("0.1");
		 GroupLayout gl_panel_4 = new GroupLayout(panel_4);
		 gl_panel_4.setHorizontalGroup(
		 	gl_panel_4.createParallelGroup(Alignment.LEADING)
		 		.addGroup(gl_panel_4.createSequentialGroup()
		 			.addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING)
		 				.addGroup(gl_panel_4.createSequentialGroup()
		 					.addContainerGap()
		 					.addComponent(lblGridSize)
		 					.addPreferredGap(ComponentPlacement.RELATED)
		 					.addComponent(gridSizeInp, GroupLayout.PREFERRED_SIZE, 44, GroupLayout.PREFERRED_SIZE)
		 					.addPreferredGap(ComponentPlacement.RELATED)
		 					.addComponent(lblKernelWeightq)
		 					.addPreferredGap(ComponentPlacement.RELATED)
		 					.addComponent(kernelWeightq, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE)
		 					.addPreferredGap(ComponentPlacement.RELATED)
		 					.addComponent(lblKernelWeightp, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		 					.addPreferredGap(ComponentPlacement.RELATED)
		 					.addComponent(kernelWeightp, GroupLayout.PREFERRED_SIZE, 49, GroupLayout.PREFERRED_SIZE)
		 					.addPreferredGap(ComponentPlacement.RELATED))
		 				.addGroup(gl_panel_4.createSequentialGroup()
		 					.addGap(113)
		 					.addComponent(btnCalculateDistances, GroupLayout.PREFERRED_SIZE, 209, GroupLayout.PREFERRED_SIZE)))
		 			.addGap(0))
		 );
		 gl_panel_4.setVerticalGroup(
		 	gl_panel_4.createParallelGroup(Alignment.TRAILING)
		 		.addGroup(gl_panel_4.createSequentialGroup()
		 			.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		 			.addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
		 				.addComponent(lblGridSize)
		 				.addComponent(gridSizeInp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
		 				.addComponent(lblKernelWeightq)
		 				.addComponent(kernelWeightq, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
		 				.addComponent(lblKernelWeightp)
		 				.addComponent(kernelWeightp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		 			.addGap(21)
		 			.addComponent(btnCalculateDistances))
		 );
		 panel_4.setLayout(gl_panel_4);
		 kernelWeightp.addActionListener(this);
		 
		 btnCalculateDistances.addActionListener(this);
		
		JPanel panel_2 = new JPanel();
		tabbedPane.addTab("Load images", null, panel_2, null);
		
		JLabel label = new JLabel("Image X");
		
		browseX = new JButton("Open X");
		browseX.addActionListener(this);
		
		JLabel label_1 = new JLabel("Reference image Y");
		browseY = new JButton("Open Y");
		browseY.addActionListener(this);
		GroupLayout gl_panel_2 = new GroupLayout(panel_2);
		gl_panel_2.setHorizontalGroup(
			gl_panel_2.createParallelGroup(Alignment.LEADING)
				.addGap(0, 438, Short.MAX_VALUE)
				.addGroup(gl_panel_2.createSequentialGroup()
					.addGap(98)
					.addComponent(label)
					.addGap(74)
					.addComponent(browseX, GroupLayout.PREFERRED_SIZE, 176, GroupLayout.PREFERRED_SIZE))
				.addGroup(gl_panel_2.createSequentialGroup()
					.addGap(33)
					.addComponent(label_1)
					.addGap(74)
					.addComponent(browseY, GroupLayout.PREFERRED_SIZE, 176, GroupLayout.PREFERRED_SIZE))
		);
		gl_panel_2.setVerticalGroup(
			gl_panel_2.createParallelGroup(Alignment.LEADING)
				.addGap(0, 75, Short.MAX_VALUE)
				.addGroup(gl_panel_2.createSequentialGroup()
					.addGap(6)
					.addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_2.createSequentialGroup()
							.addGap(5)
							.addComponent(label))
						.addComponent(browseX))
					.addGap(6)
					.addGroup(gl_panel_2.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_2.createSequentialGroup()
							.addGap(5)
							.addComponent(label_1))
						.addComponent(browseY)))
		);
		panel_2.setLayout(gl_panel_2);
		
		JPanel panel_8 = new JPanel();
		tabbedPane.addTab("Load coordinates", null, panel_8, null);
		
		btnLoadCsvFileX = new JButton("Open");
		btnLoadCsvFileX.setHorizontalAlignment(SwingConstants.RIGHT);
		btnLoadCsvFileX.addActionListener(this);
		
		JLabel lblCsvFileOf = new JLabel("X Coordinates");
		lblCsvFileOf.setHorizontalAlignment(SwingConstants.LEFT);
		
		JLabel label_2 = new JLabel("Y (reference) Coordinates");
		
		btnLoadCsvFileY = new JButton("Open");
		btnLoadCsvFileY.addActionListener(this);
		
		txtXmin = new JFormattedTextField();
		txtXmin.setText("xmin");
		txtXmin.setColumns(10);
		txtXmin.addActionListener(this);
		
		txtYmin = new JFormattedTextField();
		txtYmin.setText("ymin");
		txtYmin.setColumns(10);
		txtYmin.addActionListener(this);
		
		txtZmin = new JFormattedTextField();
		txtZmin.setText("zmin");
		txtZmin.setColumns(10);
		txtZmin.addActionListener(this);
		
		txtXmax = new JFormattedTextField();
		txtXmax.setText("xmax");
		txtXmax.setColumns(10);
		txtXmax.addActionListener(this);
		
		txtYmax = new JFormattedTextField();
		txtYmax.setText("ymax");
		txtYmax.setColumns(10);
		txtYmax.addActionListener(this);
		
		txtZmax = new JFormattedTextField();
		txtZmax.setText("zmax");
		txtZmax.setColumns(10);
		txtZmax.addActionListener(this);
		GroupLayout gl_panel_8 = new GroupLayout(panel_8);
		gl_panel_8.setHorizontalGroup(
			gl_panel_8.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_8.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel_8.createParallelGroup(Alignment.LEADING)
						.addComponent(lblCsvFileOf)
						.addComponent(label_2))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_8.createParallelGroup(Alignment.LEADING, false)
						.addGroup(gl_panel_8.createSequentialGroup()
							.addComponent(btnLoadCsvFileY)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtXmax, 0, 0, Short.MAX_VALUE))
						.addGroup(gl_panel_8.createSequentialGroup()
							.addComponent(btnLoadCsvFileX)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtXmin, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)))
					.addGroup(gl_panel_8.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_8.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtYmin, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE))
						.addGroup(Alignment.TRAILING, gl_panel_8.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txtYmax, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE)))
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGroup(gl_panel_8.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_panel_8.createSequentialGroup()
							.addComponent(txtZmin, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
							.addContainerGap())
						.addGroup(gl_panel_8.createSequentialGroup()
							.addComponent(txtZmax, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE)
							.addGap(11))))
		);
		gl_panel_8.setVerticalGroup(
			gl_panel_8.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_8.createSequentialGroup()
					.addGap(5)
					.addGroup(gl_panel_8.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblCsvFileOf)
						.addComponent(btnLoadCsvFileX)
						.addComponent(txtXmin, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(txtYmin, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(txtZmin, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_8.createParallelGroup(Alignment.BASELINE)
						.addComponent(label_2)
						.addComponent(btnLoadCsvFileY)
						.addComponent(txtXmax, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(txtYmax, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(txtZmax, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(13))
		);
		panel_8.setLayout(gl_panel_8);
		GroupLayout groupLayout = new GroupLayout(frmInteractionAnalysis.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(6)
					.addComponent(tabbedPane))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(6)
					.addComponent(tabbedPane_1))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(17)
					.addComponent(panel_6))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(17)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
						.addComponent(panel_5, Alignment.LEADING)
						.addComponent(panel_7, Alignment.LEADING)
						.addComponent(panel_4, Alignment.LEADING)))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(6)
					.addComponent(tabbedPane)
					.addGap(6)
					.addComponent(tabbedPane_1,100,100,100)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel_4)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel_5)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel_7)
					.addGap(6)
					.addComponent(panel_6))
		);
		frmInteractionAnalysis.getContentPane().setLayout(groupLayout);
		frmInteractionAnalysis.pack();
		estimate.addActionListener(this);
		test.addActionListener(this);
		mCRuns.addActionListener(this);
		
	}
	
	public void actionPerformed(ActionEvent e) {
		
		// TODO Auto-generated method stub
		if(e.getSource()==browseX)
		{
		//	 imgx=new ImagePlus();
			//a=null; garbage collection OK?
			 imgx=ImageProcessUtils.openImage("Open Image X", "");
			 if(imgx==null)
			 {
				 IJ.showMessage("Cancelled/Filetype not recognized");
				 return;
			 }
			 imgx.show("Image X");
			 
			 browseX.setText(imgx.getTitle());
			 if(imgy!=null && imgx!=null)
			 {
			 if(!checkIfImagesAreRightSize())	
				{
					System.out.println("Distance calc: different image sizes");
					IJ.showMessage("Error: Image sizes/scale/unit do not match");
					return;	
				}
				a=new Analysis(imgx,imgy);
				a.setCmaReRunTimes(numReRuns);
				a.setKernelWeightq(qkernelWeight);
				a.setKernelWeightp(pkernelWeight);
				System.out.println("p set to:"+pkernelWeight);
			 }
			 return;
		}
		if(e.getSource()==browseY)
		{
		//	a=null;
		//	imgy=new ImagePlus();
			 imgy=ImageProcessUtils.openImage("Open Image Y", "");
			 
			 if(imgy==null)
			 {
				 IJ.showMessage("Cancelled/Filetype not recognized");
				 return;
			 }
			 imgy.show("Image Y");
			 browseY.setText(imgy.getTitle());
		//	 System.out.println("ImageY size"+imgy.getWidth()+"x"+imgy.getHeight());
			 if(imgy!=null && imgx!=null)
			 {
			 if(!checkIfImagesAreRightSize())	
				{
					System.out.println("Distance calc: different image sizes");
					IJ.showMessage("Error: Image sizes/scale do not match");
					return;	
				}
				a=new Analysis(imgx,imgy);
				a.setCmaReRunTimes(numReRuns);
				a.setKernelWeightq(qkernelWeight);
				a.setKernelWeightp(pkernelWeight);
				System.out.println("p set to:"+pkernelWeight);
			 }
			 return;
		}
		
		if(e.getSource()==btnLoadCsvFileX)
		{
			
		//	IJ.showMessage("TestX");
			
			
	
		//	Problem: how to make masks, how to calculate q(d), etc
			Xcoords=ImageProcessUtils.openCSVFile("Open CSV file for image X", "");
			if(Xcoords==null)
			{
				IJ.showMessage("Error: Wrong CSV format");
				return;
			}
			System.out.println("Loaded X with size"+Xcoords.length);
			if(Xcoords!=null && Ycoords!=null)
			{
				a=new Analysis(Xcoords,Ycoords);
				a.setCmaReRunTimes(numReRuns);
				a.setKernelWeightq(qkernelWeight);
				a.setKernelWeightp(pkernelWeight);
			}
			return;
		}
		
		if(e.getSource()==btnLoadCsvFileY)
		{
			
		//	MyFrame myframe=new MyFrame();
			
			Ycoords=ImageProcessUtils.openCSVFile("Open CSV file for image Y", "");
			if(Ycoords==null)
			{
				IJ.showMessage("Error: Wrong CSV format");
				return;
			}
			
			System.out.println("Loaded Y with size"+Ycoords.length);
		//	ImageProcessUtils.openCSVFile("Open CSV file for image Y", "");
			//a.loadYCoordinates();
			if(Xcoords!=null && Ycoords!=null)
			{
				a=new Analysis(Xcoords,Ycoords);
				a.setCmaReRunTimes(numReRuns);
				a.setKernelWeightq(qkernelWeight);
				a.setKernelWeightp(pkernelWeight);
			}
			return;
		}
		
		if(a==null)
		{
			IJ.showMessage("Load images/coordinates first");
			return;
		}
		if(e.getSource()==jcb)
		{
			
			JComboBox cb = (JComboBox)e.getSource();
	        String selected = (String)cb.getSelectedItem();
	        System.out.println("Selected: "+selected);
	        if(selected==items[5])
		      {	
		    	  //  IJ.showMessage("Nonparametric estimation - feature under testing - may not be perfect");
				
				
			//	System.out.println("Min:"+minMax[0]+" Max:"+minMax[1]+"Mean:"+minMax[2]);
					potentialType=PotentialFunctions.NONPARAM;
					numSupport.setEnabled(true);
					smoothnessNP.setEnabled(true);
					lblSmoothness.setEnabled(true);
					lblsupportPts.setEnabled(true);
					
		      }
	        else
	        {
	        	numSupport.setEnabled(false);
				smoothnessNP.setEnabled(false);
				lblSmoothness.setEnabled(false);
				lblsupportPts.setEnabled(false);
	      if(selected==items[1]){
			potentialType=PotentialFunctions.STEP;
			//System.out.println("Step's gonna work");
	      }
	      if(selected==items[0])
	      {
				potentialType=PotentialFunctions.HERNQUIST;
			//	System.out.println("HERNQUIST gonna work");
	      }
	      if(selected==items[2])
	      {
				potentialType=PotentialFunctions.L1;
			//	System.out.println("HERNQUIST gonna work");
	      }
	      if(selected==items[3])
	      {
				potentialType=PotentialFunctions.L2;
			//	System.out.println("HERNQUIST gonna work");
	      }
	      if(selected==items[4])
	      {
				potentialType=PotentialFunctions.PlUMMER;
			//	System.out.println("HERNQUIST gonna work");
	      }
	        }
	      
	//      if(selected==items[6])
	  //    {
		//		potentialType=PotentialFunctions.COULOMB;
	  //    }

	      
			a.setPotentialType(potentialType);
	      return;
		}
/*		if(e.getSource()==kernelWeightq)
		{
			qkernelWeight=Double.parseDouble(e.getActionCommand());
			a.setKernelWeightq(qkernelWeight);
			
		}
		if(e.getSource()==kernelWeightp)
		{
			pkernelWeight=Double.parseDouble(e.getActionCommand());
			System.out.println("p set to:"+pkernelWeight);
			a.setKernelWeightp();
			
		}*/
		if(e.getSource()==btnCalculateDistances)
		{
			xmin=Double.parseDouble(txtXmin.getText());
			ymin=Double.parseDouble(txtYmin.getText());
			zmin=Double.parseDouble(txtZmin.getText());
			xmax=Double.parseDouble(txtXmin.getText());
			ymax=Double.parseDouble(txtYmin.getText());
			zmax=Double.parseDouble(txtZmin.getText());
			
			gridSize=Double.parseDouble(gridSizeInp.getText());
			a.setKernelWeightq(Double.parseDouble(kernelWeightq.getText()));
			a.setKernelWeightp(Double.parseDouble(kernelWeightp.getText()));
			
		//	a.setImageList(imgx, imgy);
			
			if(!a.getIsImage())
			{
				if(xmin>Double.MAX_VALUE-1|| xmax>Double.MAX_VALUE-1|| ymin> Double.MAX_VALUE-1 || ymax> Double.MAX_VALUE-1 || zmin>Double.MAX_VALUE-1 || zmax>Double.MAX_VALUE-1
						|| xmax<xmin || ymax<ymin || zmax<zmin)
				{
					IJ.showMessage("Error: boundary values are not correct");
					return;
				}
				a.setX1(xmin);
				a.setX2(xmax);
				a.setY1(ymin);
				a.setY2(ymax);
				a.setZ1(zmin);
				a.setZ2(zmax);
			
			}
			
			
			if(!a.calcDist(gridSize))
				IJ.showMessage("No X and Y images/coords loaded. Cannot calculate distance");
		
			return;
		}
		
/*		if(e.getSource()==reRuns)
		{
			numReRuns=Integer.parseInt(e.getActionCommand());
			a.setCmaReRunTimes(numReRuns);
		}*/
		
		if(e.getSource()==estimate)
		{
			numReRuns= Integer.parseInt(reRuns.getText());
			a.setCmaReRunTimes(numReRuns);
			
			System.out.println("Estimating with potential type:"+potentialType);
			if(potentialType==PotentialFunctions.NONPARAM)
				PotentialFunctions.initializeNonParamWeights(a.getMinD(), a.getMaxD());
			 a.setPotentialType(potentialType); // for the first time
		    // a.test();
			if(!a.cmaOptimization())
				IJ.showMessage("Error: Calculate distances first!");
			return;
		}
		
		if(e.getSource()==test)
		{
			monteCarloRunsForTest = Integer.parseInt(mCRuns.getText());
			alpha = Double.parseDouble(alphaField.getText());
			
			if(!a.hypTest(monteCarloRunsForTest,alpha))
				IJ.showMessage("Error: Run estimation first");
			return;
		}
		
/*		if(e.getSource()==gridSizeInp)
		{
			gridSize=Double.parseDouble(e.getActionCommand());
			
			System.out.println("Grid size changed to:"+gridSize);

			return;
		}*/
		if(e.getSource()==numSupport)
		{
			PotentialFunctions.NONPARAM_WEIGHT_SIZE=Integer.parseInt(e.getActionCommand());
			
			
			System.out.println("Weight size changed to:"+PotentialFunctions.NONPARAM_WEIGHT_SIZE);
			
			return;
		}
		if(e.getSource()==smoothnessNP)
		{
			PotentialFunctions.NONPARAM_SMOOTHNESS=Double.parseDouble(e.getActionCommand());
			System.out.println("Smoothness:"+PotentialFunctions.NONPARAM_SMOOTHNESS);
			return;
		}
/*		if(e.getSource()==mCRuns)
		{
			monteCarloRunsForTest=Integer.parseInt(e.getActionCommand());
			return;
		}*/
/*		if(e.getSource()==txtXmin)
		{
			xmin=Double.parseDouble(e.getActionCommand());
			System.out.println("xmin:"+xmin);
			
			return;
		}
		if(e.getSource()==txtYmin)
		{
			ymin=Double.parseDouble(e.getActionCommand());
			System.out.println("ymin:"+ymin);
			return;
		}
		if(e.getSource()==txtZmin)
		{
			zmin=Double.parseDouble(e.getActionCommand());
			System.out.println("zmin:"+zmin);
			return;
		}
		if(e.getSource()==txtXmax)
		{
			xmax=Double.parseDouble(e.getActionCommand());
			System.out.println("xmax:"+xmax);
			return;
		}
		if(e.getSource()==txtYmax)
		{
			ymax=Double.parseDouble(e.getActionCommand());
			System.out.println("ymax:"+ymax);
			return;
		}
		if(e.getSource()==txtZmax)
		{
			zmax=Double.parseDouble(e.getActionCommand());
			System.out.println("zmax:"+zmax);
			return;
		}*/
		
/*		if(e.getSource()==alphaField)
		{
			alpha=Double.parseDouble(e.getActionCommand());
			return;
		}*/
		if(e.getSource()==genMask)
		{
			if(!a.getIsImage())
			{
				IJ.showMessage("Cannot generate mask for coordinates. Load a mask instead");
				return;
			}
			try{
			
			if(!a.calcMask())
				IJ.showMessage("Image Y is null: Cannot generate mask");
			}
			catch(NullPointerException npe)
			{
				System.out.println("NPE caught");
				IJ.showMessage("Image Y is null: Cannot generate mask");
			}
			return;
		}
		
		if(e.getSource()==applyMask)
		{
			
			if(a.applyMask()==true)
				IJ.showMessage("Mask set to:"+a.getMaskTitle());
			else
				IJ.showMessage("No mask to apply! Load/Generate a mask.");
			return;
		}
		if(e.getSource()==loadMask)
		{
			
			a.loadMask();
			return;
		}
		
		if(e.getSource()==resetMask)
		{
			
			a.resetMask();
			IJ.showMessage("Mask reset to Null");
			return;
		}
		
		

	}
		

		public  ImagePlus [] returnImages()
		{
			ImagePlus [] imgList = new ImagePlus[2];
			imgList[0]=imgy;
			imgList[1]=imgx;
			return imgList;
		}
		private boolean checkIfImagesAreRightSize()
		{
			Calibration imgxc =imgx.getCalibration();
			Calibration imgyc =imgy.getCalibration();
			if((imgx.getWidth()==imgy.getWidth())&&(imgx.getHeight()==imgy.getHeight())&&(imgx.getStackSize()==imgy.getStackSize())&& (imgxc.pixelDepth==imgyc.pixelDepth) && (imgxc.pixelHeight==imgyc.pixelHeight)&&(imgxc.pixelWidth==imgyc.pixelWidth) &&(imgxc.getUnit().equals(imgyc.getUnit())) )
					return true;
			else {
				System.out.println(imgx.getWidth()+","+imgy.getWidth()+","+imgx.getHeight()+","+imgy.getHeight()+","+imgx.getStackSize()+","+imgy.getStackSize()+","+imgxc.pixelDepth+","+imgyc.pixelDepth+","+imgxc.pixelHeight+","+imgyc.pixelHeight+","+imgxc.pixelWidth+","+imgyc.pixelWidth+","+imgxc.getUnit()+","+imgyc.getUnit());
				
					return false;
					
			}
		}
	public JTextArea getTextArea() {
		return textArea;
	}
}
