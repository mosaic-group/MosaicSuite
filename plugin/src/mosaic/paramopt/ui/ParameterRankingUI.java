package mosaic.paramopt.ui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mosaic.paramopt.IParameterRankingController;

// OPEN TODOS IN THIS CLASS:
// TODO: Add a method getSelectionMessage which returns the correct message
//       for how many images need to be selected.
// TODO: Use getSelectionMessage when creating the info message of step 4.

public class ParameterRankingUI extends JFrame implements IParameterRankingUI,
		MouseListener, MouseWheelListener, MouseMotionListener, KeyListener{

	private static final long serialVersionUID = 1L;
	
	// Grouping and layout panels
	private JPanel contentPane = null;
	private JPanel centerPanel = null;
	private JPanel loadingPanel = null;
	private JPanel topPanel = null;
	private JPanel bottomPanel = null;
	private JPanel controlsPanel = null;
	private JPanel buttonsPanel = null;
	private JPanel spacePanel = null;
	// Components of the loading panel
	private JLabel loadingMessage = null;
	private JProgressBar loadingProgress = null;
	// Step labels and descriptions
	private JLabel step4Label = null;
	private JLabel step4DescrLabel = null;
	private JLabel step5Label = null;
	private JLabel step5DescrLabel = null;
	// Action buttons and description labels
	private JButton nextButton = null;
	private JButton repeatButton = null;
	private JButton finishButton = null;
	private JLabel nextLabel = null;
	private JLabel repeatLabel = null;
	private JLabel finishLabel = null;
	// Components for the zoom control
	private JLabel opacityLabel = null;
	private JSlider opacitySlider = null;
	private JLabel opacityValueLabel = null;
	private JLabel sliceLabel = null;
	private JSlider sliceSlider = null;
	private JLabel sliceValueLabel = null;
	private JLabel zoomLabel = null;
	private JSlider zoomSlider = null;
	private JLabel zoomValueLabel = null;
	// Info icon and text
	private JLabel infoIconLabel = null;
	private JLabel infoLabel = null;
	// Button to clear the current selection
	private JButton clearSelectionButton = null;
	// The canvases for the images
	private final ImageCanvas[] images;
	
	// The range of zoom values
	private final double[] zoomValues = {1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0,
			12.0, 16.0, 24.0, 32.0};
	private int lastX, lastY;
	private boolean draging = false;
	
	// The controller associated with this UI
	private final IParameterRankingController controller;
	// The number of images that this window contains
	private int imageCount;
	// The minimal number of images which need to be ranked before the
	// optimization can be started
	private final int minRanked;
	// The maximal number of images which are to be ranked.
	private final int maxRanked;
	// The count of currently ranked images.
	private int rankCount = 0;
	// The current progress of the optimization
	private int currentProgress = 0;
	
	
	
	/**
	 * Creates the frame with the specified number of images and sets the
	 * number of images which have to be selected to the specified value.
	 * 
	 * @param imageCount
	 *            the number of images which are to be displayed in this frame
	 * @param minSelection
	 *            the minimal number of images which need to be selected in
	 *            order to start the optimization
	 * @param maxSelection
	 *            the maximal number of images which can be selected for the 
	 *            optimization
	 * @param ctrl
	 *            the controller interface which is used as target for the 
	 *            method invocations in the buttons
	 */
	public ParameterRankingUI(int imageCount, int minRanked,
			int maxRanked, IParameterRankingController ctrl) {
		super("Test");
		this.imageCount = imageCount;
		this.minRanked = minRanked;
		this.maxRanked = maxRanked;
		images = new ImageCanvas[imageCount];
		initialize();
		controller = ctrl;
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(600, 600);
		this.setMinimumSize(new Dimension(750,500));
		this.setContentPane(getJContentPane());
		this.setTitle("Parameter Optimization - Ranking");
		this.addKeyListener(this);
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (contentPane == null) {
			contentPane = new JPanel();
			contentPane.setLayout(new BorderLayout());
			
			// Create the loading panel
			getLoadingPanel();
			
			// Create and add the center, top and bottom panels.
			contentPane.add(getCenterPanel(), BorderLayout.CENTER);
			contentPane.add(getTopPanel(), BorderLayout.NORTH);
			contentPane.add(getBottomPanel(), BorderLayout.SOUTH);
		}
		return contentPane;
	}

	/**
	 * This method initializes CenterPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getCenterPanel() {
		if (centerPanel == null) {
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(3);
			centerPanel = new JPanel();
			
			for (int i = 0; i < imageCount; i++) {
				images[i] = new ImageCanvas(null, i);
				images[i].addMouseListener(this);
				images[i].addMouseWheelListener(this);
				images[i].addMouseMotionListener(this);
				images[i].addKeyListener(this);
				centerPanel.add(images[i]);
				if (i == 0)
					images[i].setZoomIndicatorEnabled(true);
			}
			centerPanel.setLayout(gridLayout);
			centerPanel.setBorder(new LineBorder(Color.LIGHT_GRAY, 2) );
		}
		return centerPanel;
	}

	/**
	 * This method initializes TopPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getTopPanel() {
		if (topPanel == null) {
			// Create step4Label
			GridBagConstraints gbcStep4Label = new GridBagConstraints();
			gbcStep4Label.gridx = 0;
			gbcStep4Label.gridy = 0;
			gbcStep4Label.anchor = GridBagConstraints.NORTHWEST;
			gbcStep4Label.insets = new Insets(5,5,5,10);
			step4Label = new JLabel();
			step4Label.setText("Step 4:");
			step4Label.setForeground(Color.BLUE);
			step4Label.setFont(step4Label.getFont().deriveFont(Font.BOLD, 16));
			
			// Create step4DescriptionLabel
			GridBagConstraints gbcStep4DescrLabel = new GridBagConstraints();
			gbcStep4DescrLabel.gridx = 1;
			gbcStep4DescrLabel.gridy = 0;
			gbcStep4DescrLabel.fill = GridBagConstraints.HORIZONTAL;
			gbcStep4DescrLabel.weightx = 1.0;
			gbcStep4DescrLabel.anchor = GridBagConstraints.NORTHWEST;
			gbcStep4DescrLabel.insets = new Insets(9,5,5,5);
			step4DescrLabel = new JLabel();
			step4DescrLabel.setText("<html>Rank " + getSelectionMessage() +
					" of the images below by holding the <b>shift</b>-key " +
					"and clicking on them.</html>");
			Font font = step4DescrLabel.getFont();
			step4DescrLabel.setFont(font.deriveFont(Font.PLAIN));
			step4DescrLabel.setForeground(Color.BLUE);
			
			// Create infoIconLabel
			GridBagConstraints gbcInfoIconLabel = new GridBagConstraints();
			gbcInfoIconLabel.gridx = 2;
			gbcInfoIconLabel.gridy = 0; 
			ImageIcon infoIcon = new ImageIcon(getClass()
					.getResource("mosaic/pictures/info.png"));
			infoIconLabel = new JLabel(infoIcon);
			// Add mouse listener to show and hide the info text.
			infoIconLabel.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {
					infoLabel.setVisible(true);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					infoLabel.setVisible(false);
				}
			});
			
			// Create infoLabel
			GridBagConstraints gbcInfoLabel = new GridBagConstraints();
			gbcInfoLabel.gridx = 1;
			gbcInfoLabel.gridy = 1;
			gbcInfoLabel.gridwidth = 2;
			gbcInfoLabel.fill = GridBagConstraints.HORIZONTAL;
			gbcInfoLabel.insets = new Insets(0,5,0,5);
			infoLabel = new JLabel();
			infoLabel.setVisible(false);
			infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN));
			infoLabel.setText("<html><font color='#999999'>You can use the " +
					"zoom and pan controls, change the slice with the mouse " +
					"wheel or change the overlay opacity by holding the " +
					"shift-key and scrolling the mouse wheel.</html>");
			
			// Create topPanel and add the components
			topPanel = new JPanel();
			topPanel.setLayout(new GridBagLayout());
			topPanel.add(step4Label, gbcStep4Label);
			topPanel.add(step4DescrLabel, gbcStep4DescrLabel);
			topPanel.add(infoIconLabel, gbcInfoIconLabel);
			topPanel.add(infoLabel, gbcInfoLabel);
		}
		return topPanel;
	}

	/**
	 * This method initializes BottomPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getBottomPanel() {
		if (bottomPanel == null) {
			// Create the GridBagConstraints for controlsPanel
			GridBagConstraints gbcControlsPanel = new GridBagConstraints();
			gbcControlsPanel.gridx = 0;
			gbcControlsPanel.fill = GridBagConstraints.HORIZONTAL;
			gbcControlsPanel.weightx = 0.0;
			gbcControlsPanel.anchor = GridBagConstraints.NORTHWEST;

			// Create the GridBagConstraints for spacePanel
			GridBagConstraints gbcSpacePanel = new GridBagConstraints();
			gbcSpacePanel.gridx = 1;
			gbcSpacePanel.gridy = 0;
			gbcSpacePanel.weightx = 1.0;

			// Create the GridBagConstraints for buttonsPanel
			GridBagConstraints gbcButtonsPanel = new GridBagConstraints();
			gbcButtonsPanel.gridx = 2;
			
			// Create bottomPanel and add the components
			bottomPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] {210,0,300};
			bottomPanel.setLayout(layout);
			bottomPanel.add(getControlsPanel(), gbcControlsPanel);
			bottomPanel.add(getSpacePanel(), gbcSpacePanel);
			bottomPanel.add(getButtonsPanel(), gbcButtonsPanel);
		}
		return bottomPanel;
	}

	/**
	 * This method initializes ControlsPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getControlsPanel() {
		if (controlsPanel == null) {
			// Create opacityLabel, opacitySlider and opacityValueLabel
			GridBagConstraints gbcOpacityLabel = new GridBagConstraints();
			gbcOpacityLabel.gridx = 0;
			gbcOpacityLabel.gridy = 1;
			gbcOpacityLabel.ipadx = 5;
			gbcOpacityLabel.anchor = GridBagConstraints.NORTHEAST;
			opacityLabel = new JLabel();
			opacityLabel.setText("Opacity:");
			GridBagConstraints gbcOpacitySlider = new GridBagConstraints();
			gbcOpacitySlider.gridx = 1;
			gbcOpacitySlider.gridy = 1;
			gbcOpacitySlider.fill = GridBagConstraints.HORIZONTAL;
			gbcOpacitySlider.weightx = 1.0;
			gbcOpacitySlider.ipady = 5;
			GridBagConstraints gbcOpacityValueLabel = new GridBagConstraints();
			gbcOpacityValueLabel.gridx = 2;
			gbcOpacityValueLabel.gridy = 1;
			gbcOpacityValueLabel.anchor = GridBagConstraints.NORTH;
			gbcOpacityValueLabel.insets = new Insets(2,5,0,0);
			opacityValueLabel = new JLabel();
			opacityValueLabel.setFont(opacityValueLabel.getFont()
					.deriveFont(Font.BOLD, 11));
			opacityValueLabel.setText("0.0");

			// Create sliceLabel, sliceSlider and sliceValueLabel
			GridBagConstraints gbcSliceLabel = new GridBagConstraints();
			gbcSliceLabel.gridx = 0;
			gbcSliceLabel.gridy = 2;
			gbcSliceLabel.ipadx = 5;
			gbcSliceLabel.anchor = GridBagConstraints.NORTHEAST;
			sliceLabel = new JLabel();
			sliceLabel.setText("Slice:");
			GridBagConstraints gbcSliceSlider = new GridBagConstraints();
			gbcSliceSlider.gridx = 1;
			gbcSliceSlider.gridy = 2;
			gbcSliceSlider.ipady = 5;
			gbcSliceSlider.fill = GridBagConstraints.HORIZONTAL;
			gbcSliceSlider.weightx = 1.0;
			GridBagConstraints gbcSliceValueLabel = new GridBagConstraints();
			gbcSliceValueLabel.gridx = 2;
			gbcSliceValueLabel.gridy = 2;
			gbcSliceValueLabel.anchor = GridBagConstraints.NORTH;
			gbcSliceValueLabel.insets = new Insets(2,5,0,0);
			sliceValueLabel = new JLabel();
			sliceValueLabel.setFont(sliceValueLabel.getFont().deriveFont(Font.BOLD, 11));
			sliceValueLabel.setText("1");

			// Create zoomLabel, zoomSlider and zoomValueLabel
			GridBagConstraints gbcZoomLabel = new GridBagConstraints();
			gbcZoomLabel.gridx = 0;
			gbcZoomLabel.gridy = 3;
			gbcZoomLabel.ipadx = 5;
			gbcZoomLabel.anchor = GridBagConstraints.NORTHEAST;
			zoomLabel = new JLabel();
			zoomLabel.setText("Zoom:");
			GridBagConstraints gbcZoomSlider = new GridBagConstraints();
			gbcZoomSlider.fill = GridBagConstraints.HORIZONTAL;
			gbcZoomSlider.gridx = 1;
			gbcZoomSlider.gridy = 3;
			gbcZoomSlider.weightx = 1.0;
			GridBagConstraints gbcZoomValueLabel = new GridBagConstraints();
			gbcZoomValueLabel.gridx = 2;
			gbcZoomValueLabel.gridy = 3;
			gbcZoomValueLabel.anchor = GridBagConstraints.NORTH;
			gbcZoomValueLabel.insets = new Insets(2,5,0,0);
			zoomValueLabel = new JLabel();
			zoomValueLabel.setFont(zoomValueLabel.getFont().deriveFont(Font.BOLD, 11));
			zoomValueLabel.setText("1.0x");

			// Create controlsPanel and add the components
			controlsPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] {50,100,35};
			controlsPanel.setLayout(layout);
			controlsPanel.add(zoomLabel, gbcZoomLabel);
			controlsPanel.add(getZoomSlider(), gbcZoomSlider);
			controlsPanel.add(zoomValueLabel, gbcZoomValueLabel);
			controlsPanel.add(opacityLabel, gbcOpacityLabel);
			controlsPanel.add(getOpacitySlider(), gbcOpacitySlider);
			controlsPanel.add(opacityValueLabel, gbcOpacityValueLabel);
			controlsPanel.add(sliceLabel, gbcSliceLabel);
			controlsPanel.add(getSliceSlider(), gbcSliceSlider);
			controlsPanel.add(sliceValueLabel, gbcSliceValueLabel);
		}
		return controlsPanel;
	}

	/**
	 * This method initializes ButtonsPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getButtonsPanel() {
		if (buttonsPanel == null) {
			GridBagConstraints gbcClearSelectionButton = new GridBagConstraints();
			gbcClearSelectionButton.anchor = GridBagConstraints.NORTHWEST;
			gbcClearSelectionButton.gridx = 3;
			gbcClearSelectionButton.gridy = 0;
			gbcClearSelectionButton.gridwidth = 1;
			GridBagConstraints gbcStep5DescrLabel = new GridBagConstraints();
			gbcStep5DescrLabel.gridx = 1;
			gbcStep5DescrLabel.gridy = 0;
			gbcStep5DescrLabel.gridwidth = 2;
			gbcStep5DescrLabel.anchor = GridBagConstraints.SOUTHWEST;
			gbcStep5DescrLabel.insets = new Insets(5,5,6,5);
			step5DescrLabel = new JLabel();
			Font font1 = step5DescrLabel.getFont();
			font1 = font1.deriveFont(Font.PLAIN);
			step5DescrLabel.setFont(font1);
			step5DescrLabel.setForeground(Color.BLUE);
			step5DescrLabel.setText("Select the next action.");
			GridBagConstraints gbcStep5Label = new GridBagConstraints();
			gbcStep5Label.gridx = 0;
			gbcStep5Label.gridy = 0;
			gbcStep5Label.insets = new Insets(5,5,5,10);
			step5Label = new JLabel();
			step5Label.setForeground(Color.BLUE);
			Font font2 = step5Label.getFont();
			step5Label.setFont(font2.deriveFont(Font.BOLD, 16));
			step5Label.setText("Step 5:");
			GridBagConstraints gbcNextLabel = new GridBagConstraints();
			gbcNextLabel.gridx = 0;
			gbcNextLabel.gridy = 1;
			gbcNextLabel.gridwidth = 2;
			gbcNextLabel.anchor = GridBagConstraints.EAST;
			nextLabel = new JLabel();
			nextLabel.setForeground(Color.LIGHT_GRAY);
			nextLabel.setText("Optimize with the current selection > ");
			GridBagConstraints gbcRepeatLabel = new GridBagConstraints();
			gbcRepeatLabel.gridx = 0;
			gbcRepeatLabel.gridy = 3;
			gbcRepeatLabel.gridwidth = 2;
			gbcRepeatLabel.anchor = GridBagConstraints.EAST;
			repeatLabel = new JLabel();
			repeatLabel.setForeground(Color.LIGHT_GRAY);
			repeatLabel.setText("Repeat the optimization step ignoring the selection > ");
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 3;
			gridBagConstraints3.gridy = 3;
			gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
			GridBagConstraints gbcFinishLabel = new GridBagConstraints();
			gbcFinishLabel.gridx = 0;
			gbcFinishLabel.gridy = 4;
			gbcFinishLabel.gridwidth = 2;
			gbcFinishLabel.anchor = GridBagConstraints.EAST;
			finishLabel = new JLabel();
			finishLabel.setText("Finish optimization and apply the selected parameter > ");
			finishLabel.setForeground(Color.LIGHT_GRAY);
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 3;
			gridBagConstraints1.gridy = 4;
			gridBagConstraints1.anchor = GridBagConstraints.EAST;
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = 1;
			gridBagConstraints.anchor = GridBagConstraints.EAST;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			buttonsPanel = new JPanel();
			buttonsPanel.setLayout(new GridBagLayout());
			buttonsPanel.add(getFinishButton(), gridBagConstraints1);
			buttonsPanel.add(getNextButton(), gridBagConstraints);
			buttonsPanel.add(getRepeatButton(), gridBagConstraints3);
			buttonsPanel.add(finishLabel, gbcFinishLabel);
			buttonsPanel.add(repeatLabel, gbcRepeatLabel);
			buttonsPanel.add(nextLabel, gbcNextLabel);
			buttonsPanel.add(step5Label, gbcStep5Label);
			buttonsPanel.add(step5DescrLabel, gbcStep5DescrLabel);
			buttonsPanel.add(getClearSelectionButton(), gbcClearSelectionButton);
		}
		return buttonsPanel;
	}

	/**
	 * This method initializes FinishButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getFinishButton() {
		if (finishButton == null) {
			finishButton = new JButton("Finish");
			// Disable the finish button until an image has been selected.
			finishButton.setEnabled(false);
			// Add a mouse listener to highlight the help text on mouse over.
			finishButton.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {
					// Highlight the help text if the button is enabled.
					if (finishButton.isEnabled())
						finishLabel.setForeground(Color.BLACK);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					finishLabel.setForeground(Color.LIGHT_GRAY);
				}
				
			});
			// Add an action listener to handle clicks on the button.
			finishButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onFinishButtonClick();
				}
			});
		}
		return finishButton;
	}

	/**
	 * This method initializes NextButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getNextButton() {
		if (nextButton == null) {
			nextButton = new JButton("Next");
			// Disable the button until the required number of images has been
			// selected.
			nextButton.setEnabled(false);
			// Add a mouse listener to highlight the help text on mouse over.
			nextButton.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {
					// Highlight the help text if the button is enabled.
					if (nextButton.isEnabled())
						nextLabel.setForeground(Color.BLACK);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					nextLabel.setForeground(Color.LIGHT_GRAY);
				}
				
			});
			// Add an action listener to handle clicks on the button.
			nextButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onNextButtonClick();
				}
			});
		}
		return nextButton;
	}

	/**
	 * This method initializes RepeatButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getRepeatButton() {
		if (repeatButton == null) {
			repeatButton = new JButton("Repeat");
			repeatButton.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {
					if (repeatButton.isEnabled())
						repeatLabel.setForeground(Color.BLACK);
				}

				@Override
				public void mouseExited(MouseEvent e) {
					repeatLabel.setForeground(Color.LIGHT_GRAY);
				}
				
			});
			// Add an action listener to handle clicks on the button.
			repeatButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onRepeatButtonClick();
				}
			});
		}
		return repeatButton;
	}

	/**
	 * This method initializes OpacitySlider	
	 * 	
	 * @return javax.swing.JSlider	
	 */
	private JSlider getOpacitySlider() {
		if (opacitySlider == null) {
			opacitySlider = new JSlider();
			opacitySlider.setPaintTicks(false);
			opacitySlider.setMinorTickSpacing(1);
			opacitySlider.setMinimum(0);
			opacitySlider.setMaximum(10);
			opacitySlider.setValue(0);
			opacitySlider.setSnapToTicks(true);
			Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
			JLabel label0 = new JLabel("0.0");
			label0.setFont(label0.getFont().deriveFont(Font.PLAIN, 10));
			JLabel label5 = new JLabel("0.5");
			label5.setFont(label0.getFont());
			JLabel label10 = new JLabel("1.0");
			label10.setFont(label0.getFont());
			labels.put(0, label0);
			labels.put(5, label5);
			labels.put(10, label10);
			opacitySlider.setLabelTable(labels);
			opacitySlider.setPaintLabels(true);
			opacitySlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					float value = (float) opacitySlider.getValue() 
							/ (float) opacitySlider.getMaximum();
					for (ImageCanvas ic : images) {
						if (ic != null)
							ic.setOverlayOpacity(value);
					}
					opacityValueLabel.setText(Float.toString(value));
				}
			});
		}
		return opacitySlider;
	}

	/**
	 * This method initializes SliceSlider	
	 * 	
	 * @return javax.swing.JSlider	
	 */
	private JSlider getSliceSlider() {
		if (sliceSlider == null) {
			sliceSlider = new JSlider();
			sliceSlider.setMinimum(1);
			sliceSlider.setMaximum(1);
			sliceSlider.setValue(1);
			sliceSlider.setSnapToTicks(true);
			sliceSlider.setMinorTickSpacing(1);
			sliceSlider.setPaintLabels(true);
			sliceSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					int value = sliceSlider.getValue();
					for (ImageCanvas ic : images) {
						if (ic != null)
							ic.setSlice(value);
					}
					sliceValueLabel.setText(Integer.toString(value));
				}
			});
		}
		return sliceSlider;
	}

	/**
	 * This method initializes ZoomSlider	
	 * 	
	 * @return javax.swing.JSlider	
	 */
	private JSlider getZoomSlider() {
		if (zoomSlider == null) {
			zoomSlider = new JSlider();
			zoomSlider.setMinimum(0);
			zoomSlider.setMaximum(zoomValues.length - 1);
			zoomSlider.setValue(0);
			zoomSlider.setSnapToTicks(true);
			Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
			JLabel labelMin = new JLabel(((int) zoomValues[0]) + "x");
			Font font = labelMin.getFont().deriveFont(Font.PLAIN, 10);
			labelMin.setFont(font);
			JLabel labelMax = new JLabel(
					((int) zoomValues[zoomValues.length - 1]) + "x");
			labelMax.setFont(font);
			labels.put(0, labelMin);
			labels.put(zoomValues.length - 1, labelMax);
			zoomSlider.setLabelTable(labels);
			zoomSlider.setPaintLabels(true);
			zoomSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					double value = zoomValues[zoomSlider.getValue()];
					zoomValueLabel.setText(value + "x");
					for (ImageCanvas ic : images) {
						ic.setMagnification(value);
						ic.invalidate();
					}
				}
			});
		}
		return zoomSlider;
	}

	/**
	 * This method initializes clearSelectionButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getClearSelectionButton() {
		if (clearSelectionButton == null) {
			clearSelectionButton = new JButton("clear selection");
			clearSelectionButton.setEnabled(false);
			// Add an action listener to handle clicks on the button.
			clearSelectionButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					clearRanks();
				}
			});
		}
		return clearSelectionButton;
	}
	
	/**
	 * This method initializes spacePanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getSpacePanel() {
		if (spacePanel == null) {
			spacePanel = new JPanel();
			spacePanel.setLayout(new GridBagLayout());
		}
		return spacePanel;
	}
	
	private Component getLoadingPanel() {
		if (loadingPanel == null) {
			loadingPanel = new JPanel(new GridBagLayout());
			
			// Create a progress bar and layout constraints for it. 
			GridBagConstraints gbcLoadingProgress = new GridBagConstraints();
			gbcLoadingProgress.gridx = 0;
			gbcLoadingProgress.gridy = 1;
			loadingProgress = new JProgressBar();
			// Set the range of the progress bar.
			loadingProgress.setMinimum(0);
			loadingProgress.setMaximum(imageCount);
			
			// Create a label for the loading message and constraints for it.
			GridBagConstraints gbcLoadingMessage = new GridBagConstraints();
			gbcLoadingMessage.gridx = 0;
			gbcLoadingMessage.gridy = 0;
			loadingMessage = new JLabel("");
			
			// Add the label and the progress bar to the panel.
			loadingPanel.add(loadingMessage, gbcLoadingMessage);
			loadingPanel.add(loadingProgress, gbcLoadingProgress);
		}
		return loadingPanel;
	}
	

	
	/**************************************************************************
	 *                         RANKING UI INTERFACE                           *
	 **************************************************************************/
	
	@Override
	public void setOverlay(ImagePlus imp) {
		for (ImageCanvas ic : images) {
			ic.setOverlayImage(imp);
		}
	}
	
	/**
	 * Sets the maximum for the slice number and updates the components which
	 * represent the slice number.
	 * 
	 * @param max the number of slices which are available
	 */
	public void setSliceMaximum(int max) {
		// Only allow values greater than 0.
		if (max < 1)
			return;
		// Set the upper bound of the slider.
		getSliceSlider().setMaximum(max);
		if (sliceSlider.getValue() > max)
			sliceSlider.setValue(max);
		if (max == 1) {
			// There's only one slice so we don't need to display the slice
			// controls.
			sliceLabel.setVisible(false);
			sliceSlider.setVisible(false);
			sliceValueLabel.setVisible(false);
		} else {
			Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
			JLabel labelMin = new JLabel("1");
			Font font = labelMin.getFont().deriveFont(Font.PLAIN, 10);
			labelMin.setFont(font);
			JLabel labelMax = new JLabel(Integer.toString(max));
			labelMax.setFont(font);
			labels.put(1, labelMin);
			labels.put(max, labelMax);
			sliceSlider.setLabelTable(labels);
			sliceLabel.setVisible(true);
			sliceSlider.setVisible(true);
			sliceValueLabel.setVisible(true);
		}
	}

	/**
	 * Sets the ImagePlus resource of the specified image.
	 * 
	 * @param index
	 *            the index of the image whose resource is to be set
	 * @param img the resource which is to be set
	 */
	public void setImage(int index, ImagePlus imp) {
		// Check if index is valid.
		if (index < 0 || index >= imageCount)
			return;
		// Set the image resource on the specified image.
		images[index].setImage(imp);
	}
	
	/**
	 * Sets the progress of the optimization step.
	 * 
	 * @param progress
	 *            the current progress between 0 and the number of images in 
	 *            this UI
	 */
	public void setProgress(int progress) {
		if (progress <= 0) {
			// The optimization process has been started so display the loading
			// panel.
			currentProgress = 0;
			startLoading();
		} else if (progress >= imageCount) {
			// The optimization is complete so display the center panel
			currentProgress = imageCount;
			startSelection();
		} else {
			currentProgress = progress;
			updateLoadingStatus();
		}
	}
	
	/**
	 * Closes the UI by disposing it.
	 */
	@Override
	public void close() {
		this.dispose();
	}
	
	/**************************************************************************
	 *                           INTERNAL METHODS                             *
	 **************************************************************************/
	
	private void startLoading() {
		// Disable the controls and all buttons
		setControlsEnabled(false);
		repeatButton.setEnabled(false);
		nextButton.setEnabled(false);
		finishButton.setEnabled(false);
		clearSelectionButton.setEnabled(false);
		
		// Update the status of the loading message and the progress bar.
		updateLoadingStatus();
		
		// Remove the center panel and add the loading panel to the center of 
		// the content pane.
		contentPane.remove(centerPanel);
		contentPane.add(loadingPanel, BorderLayout.CENTER);
		contentPane.updateUI();
	}
	
	private void startSelection() {
		// Clear the ranks for the next round.
		clearRanks();
		
		// Enable the controls and the repeat button
		setControlsEnabled(true);
		repeatButton.setEnabled(true);
		
		// Remove the loading panel and add the center panel to the center of 
		// the content pane.
		contentPane.remove(loadingPanel);
		contentPane.add(centerPanel, BorderLayout.CENTER);
		contentPane.updateUI();
	}
	
	private void updateLoadingStatus() {
		loadingProgress.setValue(currentProgress);
		loadingMessage.setText("creating image " + (currentProgress + 1) 
				+ " of " + imageCount + "...");
	}

	private void setControlsEnabled(boolean enabled) {
		opacitySlider.setEnabled(enabled);
		zoomSlider.setEnabled(enabled);
		sliceSlider.setEnabled(enabled);
	}
	
	private void selectImage(int index) {
		if (images[index].getRank() <= 0 && rankCount < maxRanked) {
			setRankOnImage(++rankCount, index);
			repeatButton.setEnabled(false);
			if (rankCount == minRanked) {
				// Now enough images have been ranked to do the next
				// optimization
				nextButton.setEnabled(true);
			}
			if (rankCount == 1)
				// Its the first image which has been selected so enable the 
				// clear button.
				clearSelectionButton.setEnabled(true);
				finishButton.setEnabled(true);
		}
	}
	
	/**
	 * Sets the rank of all images to 0 which means that they have not been
	 * selected.
	 */
	private void clearRanks() {
		// Set all ranks to 0.
		for (ImageCanvas ic : images)
			ic.setRank(0);
		rankCount = 0;
		finishButton.setEnabled(false);
		nextButton.setEnabled(false);
		repeatButton.setEnabled(true);
	}

	/**
	 * Sets the rank to be drawn on the image with the specified index.
	 * 
	 * @param rank
	 *            the rank which is to be set on the image
	 * @param index
	 *            the index of the image on which it is to be set
	 */
	private void setRankOnImage(int rank, int index) {
		// Check if index is valid.
		if (index < 0 || index >= imageCount)
			return;
		// Set rank on the image with given index.
		images[index].setRank(rank);
	}
	
	private int[] getRanks() {
		final int[] ranks = new int[imageCount];
		for (int i = 0; i < imageCount; i++)
			ranks[i] = images[i].getRank();
		return ranks;
	}
	
	private String getSelectionMessage() {
		if (minRanked == maxRanked) {
			// Just a specific number of images can be selected.
			return "<b>" + minRanked + "</b>";
		} else if (maxRanked == imageCount) {
			// All images can be selected.
			return "<b>" + minRanked + "</b> or more images";
		} else {
			// A specified range of images can be selected.
			return "between <b>" + minRanked + "</b> and <b>" + maxRanked
					+ "</b>";
		}
	}
	
	/**************************************************************************
	 *                             BUTTON CLICKS                              *
	 **************************************************************************/
	
	private void onNextButtonClick() {
		if (controller != null) {
			new Thread(new Runnable() {
				public void run() {
					controller.nextOptimization(getRanks());
				}
			}).start();
		}
	}
	
	private void onRepeatButtonClick() {
		if (controller != null)
			new Thread(new Runnable() {
				public void run() {
					controller.repeatOptimization();
				}
			}).start();
	}

	private void onFinishButtonClick() {
		if (controller != null) {
			new Thread(new Runnable() {
				public void run() {
					controller.finishOptimization(getRanks());
				}
			}).start();
		}
	}
		
	/**************************************************************************
	 *                           MOUSE INTERFACES                             *
	 **************************************************************************/
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {
		int rotation = event.getWheelRotation();
		if((event.getModifiers() & MouseWheelEvent.SHIFT_MASK) != 0) {
			// Add rotation to the value of the opacity slider.
			opacitySlider.setValue(opacitySlider.getValue() + rotation);
		} else {
			// Add rotation to the value of the slice slider.
			sliceSlider.setValue(sliceSlider.getValue() + rotation);
		}
	}
	
	// TODO: Write JavaDoc for this method!
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getSource() instanceof ImageCanvas) {
			ImageCanvas canvas = (ImageCanvas) e.getSource();
			int toolID = Toolbar.getToolId();
			int flags = e.getModifiers();
			if ((flags & Event.SHIFT_MASK) != 0) {
				// Shift key is pressed so selection mode is active
				selectImage(canvas.getId());
			} else if (IJ.spaceBarDown() || (toolID == Toolbar.HAND)) { 
				draging = true;
			} else if (toolID == Toolbar.MAGNIFIER) {
				double imageX = canvas.screenToImageX(e.getX());
				double imageY = canvas.screenToImageY(e.getY());
				for (ImageCanvas ic : images)
					ic.setFocus(imageX, imageY);
				// Zoom in or out depending on the flags.
				if ((flags & (Event.ALT_MASK|Event.META_MASK|Event.CTRL_MASK))!=0)
					zoomSlider.setValue(zoomSlider.getValue() - 1);
				else
					zoomSlider.setValue(zoomSlider.getValue() + 1);
			}
		}
		lastX = e.getX();
		lastY = e.getY();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (e.getSource() instanceof ImageCanvas) {
			ImageCanvas ic = (ImageCanvas) e.getSource();
			ic.setHighlighted(false);
			ic.repaint();
		}
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
		if (e.getSource() instanceof ImageCanvas) {
			ImageCanvas ic = (ImageCanvas) e.getSource();
			ic.setHighlighted(true);
			ic.repaint();
		}
	}
	
	// TODO: Write JavaDoc for this method!
	public void mouseDragged(MouseEvent e) {
		synchronized(this) {
			int toolID = Toolbar.getToolId();
			if (IJ.spaceBarDown() || (toolID == Toolbar.HAND)) {
				if (draging) {
					int panX = lastX - e.getX();
					int panY = lastY - e.getY();
					for (ImageCanvas ic : images)
						ic.panBy(panX, panY);
					lastX = e.getX();
					lastY = e.getY();
				}
			}
		}
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {
		IJ.setKeyDown(e.getKeyCode());
//		IJ.log("keyPressed");
	}

	@Override
	public void keyReleased(KeyEvent e) {
		IJ.setKeyUp(e.getKeyCode());
	}

}
