package mosaic.paramopt.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import mosaic.paramopt.IParameterSelectionController;

public class ParameterSelectionUI extends JFrame implements
		IParameterSelectionUI {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JPanel leftPanel = null;
	private JPanel centerPanel = null;
	private JSplitPane mainSplitPane = null;
	private JScrollPane tableScrollPane = null;
	private JScrollPane highlightScrollPane = null;
	private JLabel step1Label = null;
	private JButton macroSelectionButton = null;
	private JLabel step2Label = null;
	private JFileChooser fc = null;
	private final MacroConfigurationFileFilter macroConfigFilter = 
			new MacroConfigurationFileFilter();
	private final MacroFileFilter macroFilter = new MacroFileFilter();
	private IParameterSelectionController controller = null;
	private int selectedStrategy = 0;
	private int currentStep = 1;
	private JPanel headPanel = null;
	private JLabel step3Label = null;
	private JPanel bottomPanel = null;
	private JButton startButton = null;
	private JScrollPane advancedScrollPane = null;
	private JTable parameterTable = null;
	private JComboBox<String> strategyComboBox = null;
	private JPanel advancedPanel = null;
	private JLabel strategyLabel = null;
	private JCheckBox historyCheckBox = null;
	private JTextArea highlightArea = null;
	private JButton saveConfigurationButton = null;

	
	/**
	 * This is the default constructor
	 */
	public ParameterSelectionUI() {
		this(null);
	}
	
	/**
	 * Creates the frame with a reference to the given controller.
	 * 
	 * @param ctrl
	 *            the controller which is used as target for the method
	 *            invocations in the buttons
	 */
	public ParameterSelectionUI(IParameterSelectionController ctrl) {
		super("Configuration");
		controller = ctrl;
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(600, 400);
		this.setMinimumSize(new Dimension(600, 400));
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getLeftPanel(), BorderLayout.WEST);
			jContentPane.add(getCenterPanel(), BorderLayout.CENTER);
		}
		return jContentPane;
	}

	/**
	 * This method initializes leftPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getLeftPanel() {
		if (leftPanel == null) {
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.gridy = 3;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.gridwidth = 2;
			gridBagConstraints.gridx = 0;
			
			// Create step 1 label.
			GridBagConstraints gbcStep1Label = new GridBagConstraints();
			gbcStep1Label.gridx = 0;
			gbcStep1Label.gridy = 0;
			gbcStep1Label.fill = GridBagConstraints.HORIZONTAL;
			gbcStep1Label.weightx = 1.0;
			gbcStep1Label.insets = new Insets(5,5,5,5);
			step1Label = new JLabel();
			step1Label.setText("<html><b><font size=5>Step 1:</font></b> " +
					"Select the file containing the " +
					"macro code.</html>");
			step1Label.setFont(step1Label.getFont()
					.deriveFont(Font.PLAIN));
			step1Label.setForeground(Color.BLUE);
			
			// Create constraints for the macro file selection button.
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.gridy = 1;
			gridBagConstraints4.gridwidth = 2;
			gridBagConstraints4.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints4.insets = new Insets(5,5,20,5);
			
			// Create step 2 label.
			GridBagConstraints gbcStep2Label = new GridBagConstraints();
			gbcStep2Label.gridx = 0;
			gbcStep2Label.gridy = 2;
			gbcStep2Label.fill = GridBagConstraints.HORIZONTAL;
			gbcStep2Label.weightx = 1.0;
			gbcStep2Label.insets = new Insets(5,5,5,5);
			step2Label = new JLabel();
			step2Label.setFont(step2Label.getFont().deriveFont(Font.PLAIN));
			step2Label.setText("<html><b><font size=5>Step 2:</font></b> " +
					"Select the strategy and optionaly further " +
					"parameters.</html>");
			step2Label.setForeground(Color.LIGHT_GRAY);

			leftPanel = new JPanel();
			leftPanel.setPreferredSize(new Dimension(200,0));
			leftPanel.setLayout(new GridBagLayout());
			leftPanel.add(step1Label, gbcStep1Label);
			leftPanel.add(getMacroSelectionButton(), gridBagConstraints4);
			leftPanel.add(step2Label, gbcStep2Label);
			leftPanel.add(getAdvancedScrollPane(), gridBagConstraints);
		}
		return leftPanel;
	}

	/**
	 * This method initializes centerPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getCenterPanel() {
		if (centerPanel == null) {
			centerPanel = new JPanel();
			centerPanel.setLayout(new BorderLayout());
			centerPanel.add(getMainSplitPane(), BorderLayout.CENTER);
			centerPanel.add(getHeadPanel(), BorderLayout.NORTH);
			centerPanel.add(getBottomPanel(), BorderLayout.SOUTH);
		}
		return centerPanel;
	}

	/**
	 * This method initializes mainSplitPane	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getMainSplitPane() {
		if (mainSplitPane == null) {
			mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			mainSplitPane.setResizeWeight(0.5);
			mainSplitPane.setLeftComponent(getTableScrollPane());
			mainSplitPane.setRightComponent(getHighlightScrollPane());
			mainSplitPane.setEnabled(false);
		}
		return mainSplitPane;
	}

	/**
	 * This method initializes tableScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getTableScrollPane() {
		if (tableScrollPane == null) {
			tableScrollPane = new JScrollPane();
			tableScrollPane.setViewportView(getParameterTable());
		}
		return tableScrollPane;
	}

	/**
	 * This method initializes highlightScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getHighlightScrollPane() {
		if (highlightScrollPane == null) {
			highlightScrollPane = new JScrollPane(getHighlightPanel());
		}
		return highlightScrollPane;
	}

	private JTextArea getHighlightPanel() {
		if(highlightArea == null) {
			highlightArea = new JTextArea();
			highlightArea.setEditable(false);
		}
		return highlightArea;
	}
	
	/**
	 * This method initializes macroSelectionButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getMacroSelectionButton() {
		if (macroSelectionButton == null) {
			// Setup the file chooser for the open file and save file dialog.
			fc = new JFileChooser();
			// Create a filter for macro configurations
			fc.addChoosableFileFilter(macroConfigFilter);
			fc.addChoosableFileFilter(macroFilter);
			try {
				fc.setCurrentDirectory(new File(new File(".").getCanonicalPath()
						+ "/macros"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			macroSelectionButton = new JButton("select file...");
			macroSelectionButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					fc.setAcceptAllFileFilterUsed(true);
					fc.setFileFilter(macroFilter);
					// Display file selection dialog and switch on return value
					switch(fc.showOpenDialog(null)) {
						case JFileChooser.APPROVE_OPTION:
							// A file has been chosen
							File f = fc.getSelectedFile();
							// Check if it is a macro configuration file.
							if (macroConfigFilter.accept(f)) {
								// Load macro with configuration
								if (controller != null)
									controller.selectMacroConfigurationFile(f);
							} else {
								// Load macro without configuration
								if (controller != null)
									controller.selectMacroFile(f);
							}
							break;
						case JFileChooser.CANCEL_OPTION:
						case JFileChooser.ERROR_OPTION:
							// Selection has been canceled or an error occurred  
							// TODO: Check if it is necessary to do something 
							//       here!
							break;
					}
				}
			});
		}
		return macroSelectionButton;
	}
		
/**
	 * This method initializes headPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getHeadPanel() {
		if (headPanel == null) {
			GridBagConstraints gbcStep3Label = new GridBagConstraints();
			gbcStep3Label.gridx = 0;
			gbcStep3Label.gridy = 0;
			gbcStep3Label.fill = GridBagConstraints.HORIZONTAL;
			gbcStep3Label.weightx = 1.0;
			gbcStep3Label.insets = new Insets(5,5,5,5);
			step3Label = new JLabel();
			step3Label.setFont(step3Label.getFont().deriveFont(Font.PLAIN));
			step3Label.setText("<html><b><font size=5>Step 3:</font></b> " +
					"Enable the parameters which you would like to optimize " +
					"in the table below and adjust their bounds if necessary." +
					"</html>");
			step3Label.setForeground(Color.LIGHT_GRAY);
			headPanel = new JPanel();
			headPanel.setLayout(new GridBagLayout());
			headPanel.add(step3Label, gbcStep3Label);
		}
		return headPanel;
	}

	/**
	 * This method initializes bottomPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getBottomPanel() {
		if (bottomPanel == null) {
			GridBagConstraints gbcSaveConfigButton = new GridBagConstraints();
			gbcSaveConfigButton.gridx = 0;
			gbcSaveConfigButton.gridy = 0;
			gbcSaveConfigButton.insets = new Insets(5,5,5,5);
			GridBagConstraints gbcStartButton = new GridBagConstraints();
			gbcStartButton.gridx = 1;
			gbcStartButton.gridy = 0;
			gbcStartButton.weightx = 1.0;
			gbcStartButton.anchor = GridBagConstraints.EAST;
			gbcStartButton.insets = new Insets(5,5,5,5);
			bottomPanel = new JPanel();
			bottomPanel.setLayout(new GridBagLayout());
			bottomPanel.add(getStartButton(), gbcStartButton);
			bottomPanel.add(getSaveConfigurationButton(), gbcSaveConfigButton);
		}
		return bottomPanel;
	}

	/**
	 * This method initializes startButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getStartButton() {
		if (startButton == null) {
			startButton = new JButton("start optimization");
			startButton.setEnabled(false);
			startButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (controller != null)
						controller.startOptimization();
				}
			});
		}
		return startButton;
	}

	/**
	 * This method initializes saveConfigurationButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getSaveConfigurationButton() {
		if (saveConfigurationButton == null) {
			saveConfigurationButton = new JButton("save configuration");
			saveConfigurationButton.setEnabled(false);
			saveConfigurationButton.addActionListener(new ActionListener()  {
				@Override
				public void actionPerformed(ActionEvent e) {
					fc.setAcceptAllFileFilterUsed(false);
					fc.removeChoosableFileFilter(macroFilter);
					fc.setFileFilter(macroConfigFilter);
					boolean done = false;
					while (!done) {
						// Display file selection dialog and switch on return
						// value.
						switch(fc.showSaveDialog(null)) {
							case JFileChooser.APPROVE_OPTION:
								// A file has been chosen.
								if (fc.accept(fc.getSelectedFile())) {
									// The selected file has a valid macro name.
									controller.saveMacroConfigurationFile(fc
											.getSelectedFile());
									// Set done to true to leave the loop.
									done = true;
								}
								
								break;
							case JFileChooser.CANCEL_OPTION:
							case JFileChooser.ERROR_OPTION:
								// Selection has been canceled or an error
								// has occurred.
								done = true;
								break;
						}
					}
					// Add the file filter for macros again.
					fc.addChoosableFileFilter(macroFilter);
				}
			});
		}
		return saveConfigurationButton;
	}
	


	/**
	 * This method initializes advancedScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getAdvancedScrollPane() {
		if (advancedScrollPane == null) {
			advancedScrollPane = new JScrollPane();
			advancedScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
			advancedScrollPane.setViewportView(getAdvancedPanel());
			advancedScrollPane.setEnabled(false);
		}
		return advancedScrollPane;
	}

	/**
	 * This method initializes parameterTable	
	 * 	
	 * @return javax.swing.JTable	
	 */
	private JTable getParameterTable() {
		if (parameterTable == null) {
			parameterTable = new JTable();
			parameterTable.setEnabled(false);
			parameterTable.getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {
				
				public void valueChanged(ListSelectionEvent e) {
					int selected = parameterTable.getSelectedRow();
					int paramNr = controller.getLineNumberOfParameter(selected);
					setHighlightedLine(paramNr);
				}
			});
		}
		return parameterTable;
	}

	/**
	 * This method initializes strategyComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox<String> getStrategyComboBox() {
		if (strategyComboBox == null) {
			strategyComboBox = new JComboBox<String>();
			strategyComboBox.setEnabled(false);
			strategyComboBox.setPreferredSize(new Dimension(190,20));
			strategyComboBox.addItem("Select a strategy");
			if (controller != null) {
				List<String> strategies = controller.getStrategies();
				for (String strategy : strategies)
					strategyComboBox.addItem(strategy);
			}
			strategyComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// Get the index of the selected entry
					int selected = strategyComboBox.getSelectedIndex();
					// The 'select strategy' entry can not be selected after
					// an other strategy has been selected.
					if (selected > 0 && controller != null) {
						// Subtract 1 because the 'select strategy' entry is not
						// a strategy.
						controller.selectStrategy(selected - 1);
						selectedStrategy = selected;
					} else {
						strategyComboBox.setSelectedIndex(selectedStrategy);
					}
				}
			});
		}
		return strategyComboBox;
	}

	/**
	 * This method initializes advancedPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getAdvancedPanel() {
		if (advancedPanel == null) {
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.gridy = 2;
			gridBagConstraints3.anchor = GridBagConstraints.NORTHWEST;
			gridBagConstraints3.insets = new Insets(5,5,5,5);
			gridBagConstraints3.weighty = 1.0;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.gridy = 0;
			gridBagConstraints2.anchor = GridBagConstraints.WEST;
			gridBagConstraints2.insets = new Insets(5,5,0,5);
			strategyLabel = new JLabel();
			strategyLabel.setText("Strategy:");
			strategyLabel.setEnabled(false);
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridy = 1;
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.insets = new Insets(5,5,5,5);
			advancedPanel = new JPanel();
			advancedPanel.setLayout(new GridBagLayout());
			advancedPanel.add(getStrategyComboBox(), gridBagConstraints1);
			advancedPanel.add(strategyLabel, gridBagConstraints2);
			advancedPanel.add(getHistoryCheckBox(), gridBagConstraints3);
		}
		return advancedPanel;
	}

	/**
	 * This method initializes historyCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getHistoryCheckBox() {
		if (historyCheckBox == null) {
			historyCheckBox = new JCheckBox("export history (MATLAB)");
			historyCheckBox.setEnabled(false);
			historyCheckBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (controller != null)
						controller.setHistoryEnabled(historyCheckBox
								.isSelected());
				}
			});
		}
		return historyCheckBox;
	}
	
	/**************************************************************************
	 *                  PARAMETER SELECTION UI INTERFACE                      *
	 **************************************************************************/
	
	/**
	 * Sets the index of the strategy which is currently selected.
	 * 
	 * @param strategy
	 *            the index of the strategy which has to be selected
	 */
	public void setStrategy(int strategy) {
		if (strategy < 0 || strategy >= strategyComboBox.getItemCount() - 1)
			return;
		
		// If it is an new strategy selection then set it.
		if (strategyComboBox.getSelectedIndex() != strategy + 1)
			strategyComboBox.setSelectedIndex(strategy + 1);
		
		// If the current step was the configuration of the strategy parameters
		// then go on to the next step.
		if (currentStep == 2)
			setStep(3);
	}
	
	/**
	 * Sets the flag of whether the option to export a history file is active
	 * or not.
	 * 
	 * @param enabled
	 *            the new state of the history export flag 
	 */
	public void setExportHistory(boolean enabled) {
		// Set the check mark in the check box.
		historyCheckBox.setSelected(enabled);
	}
	
	/**
	 * Notifies the ui about a change to the parameters.
	 */
	public void notifyParameterChange() {
		if (controller != null) {
			parameterTable.setModel(controller.getParameterTableModel());
			parameterTable.updateUI();
			highlightArea.setText(controller.getMacro());
			if (strategyComboBox.getSelectedIndex() == 0)
				setStep(2);
			else
				setStep(3);
		}
		else {
			parameterTable.setModel(null);
			highlightArea.setText(null);
			setStep(1);
		}
	}
	
	/**
	 * Notifies the ui that about the status of the plug-in and if optimization
	 * can be started or not.
	 * 
	 * @param ready
	 *            a flag denoting if the optimization can be started or not
	 */
	public void notifyOptimizationready(boolean ready) {
		startButton.setEnabled(ready);
		if (ready)
			setStep(3);
	}
	
	@Override
	public void close() {
		this.dispose();
	}
	
	/**************************************************************************
	 *                 INTERNAL HELPER METHODS AND CLASSES                    *
	 **************************************************************************/

	private void setStep(int step) {
		if (currentStep == step)
			// The current step is already set to i.
			return;
		else if (step >= 1 && step <= 3) {
			// The new step i is a valid step different from the current step.
			setStepHighlight(currentStep, false);
			currentStep = step;
			setStepHighlight(currentStep, true);
		} else
			// i is not a valid step.
			return;
	}
	
	private void setStepHighlight(int step, boolean highlight) {
		// Determine the color to be set.
		Color col = Color.LIGHT_GRAY;
		if (highlight)
			col = Color.BLUE;
		
		switch(step) {
			case 1:
				step1Label.setForeground(col);
				break;
			case 2:
				step2Label.setForeground(col);
				// If step 2 is being highlighted then enable the advanced
				// controls scroll pane and the center panel.
				if (highlight) {
					strategyLabel.setEnabled(true);
					strategyComboBox.setEnabled(true);
					historyCheckBox.setEnabled(true);
					mainSplitPane.setEnabled(true);
					parameterTable.setEnabled(true);
				}
				break;
			case 3:
				step3Label.setForeground(col);
				strategyLabel.setEnabled(true);
				strategyComboBox.setEnabled(true);
				historyCheckBox.setEnabled(true);
				mainSplitPane.setEnabled(true);
				parameterTable.setEnabled(true);
				saveConfigurationButton.setEnabled(true);
				break;
			default:
				break;
		}
	}
	
	private class MacroConfigurationFileFilter extends FileFilter {
		
		@Override
		public boolean accept(File f) {
			return isValidFileName(f.getName()) || f.isDirectory();
		}
		
		@Override
		public String getDescription() {
			return "ImageJ Macro configuration (*.ijmconfig)";
		}
		
		private boolean isValidFileName(String name) {
			int pos = name.lastIndexOf(".");
			return pos > 0 ? name.substring(pos).equals(".ijmconfig") : false;
		}
	}
	
	private class MacroFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			boolean value = false;
			String name = f.getName();
			int pos = name.lastIndexOf(".");
			if (pos >= 0) {
				String ext = name.substring(pos);
				if (ext.equals(".ijm"))
					value = true;
				if (ext.equals(".txt"))
					value = true;
			}
			return value || f.isDirectory();
		}
		@Override
		public String getDescription() {
			return "ImageJ Macro (*.ijm, *.txt)";
		}
	}
	
	protected void setHighlightedLine(int lineNumber) {
		if (highlightArea == null || lineNumber < 1)
			return;
		// Get the character range of the selected line in the macro code.
		String text = highlightArea.getText();
		String[] lines = text.split("\n");
		
		if (lines.length < lineNumber)
			// There is no line with the specified number
			return;
		
		int from = 0;
		int to = lines[0].length();
		for (int i = 1 ; i < lineNumber; i++) {
			from = to + 1;
			to = from + lines[i].length();
		}
		
		Highlighter h = highlightArea.getHighlighter();
	    h.removeAllHighlights();
	    try {
			h.addHighlight(from, to, DefaultHighlighter.DefaultPainter);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	

}
