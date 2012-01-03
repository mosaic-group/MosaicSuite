package mosaic.region_competition;

import ij.gui.GenericDialog;
import java.awt.Button;
import java.awt.Panel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.UnsupportedLookAndFeelException;

public class UserDialog
{
	Settings settings;
	
	private JCheckBox cb;
	
	public boolean doUser;
	public boolean doRand;
	public boolean doRect;
	public boolean doFile;
	
	public String filename;

	String title;
	GenericDialog gd;
	
	public UserDialog(Settings s) 
	{
		this.settings=s;
		gd = new GenericDialog(title);
	}
	
	public void showNetbeans()
	{
		EclipsePanel p = new EclipsePanel(settings);
		p.show();
	}
	
	/**
	 * waits until button pressed
	 */
	public void showDialog()
	{
		
//		gd.addMessage("addMessage");
//		gd.addChoice("choice", new String[]{"choice 1", "choice 2"}, "choice 2");
//		gd.addCheckbox("yes or no", false);
//		gd.addSlider("myslider", -20, 42, 13);
		
		Panel panel = new Panel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		final Button bUser = new Button("UserDefined");
		bUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doUser=true;
				dispose();
			}
		});
		
		Button bRand = new Button("RandomEllipse");
		bRand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doRand=true;
				dispose();
			}
		});
		
		Button bRect = new Button("Rectangle");
		bRect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doRect=true;
				dispose();
			}
		});
		
		panel.add(bUser);
		panel.add(bRand);
		panel.add(bRect);
		
		cb = new JCheckBox("Use Regularization");
		cb.setSelected(settings.m_EnergyUseCurvatureRegularization);
		panel.add(cb);
		
		
		final JSpinner rad = new JSpinner();
		rad.setFocusable(true);
		rad.getModel().setValue(settings.m_CurvatureMaskRadius);

		rad.addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				System.out.println("wheeeeee "+e.getWheelRotation());
				Float val = (Float) rad.getValue();
				val-= e.getWheelRotation();
				rad.getModel().setValue(val);
				settings.m_CurvatureMaskRadius=val;
			}
		});
		panel.add(rad);
		

		final JSpinner slider = new JSpinner();
		slider.setFocusable(true);
		slider.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(1.0f), Float.valueOf(0.0010f)));
		slider.getModel().setValue(settings.m_EnergyContourLengthCoeff);
//		slider.addChangeListener(new ChangeListener() {
//			
//			@Override
//			public void stateChanged(ChangeEvent e) {
//				System.out.println("spinner changed to "+slider.getValue());
//				
//			}
//		});
		slider.addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(slider.isFocusOwner())
				{
					System.out.println("is focusownder");
				}
				System.out.println("wheeeeee "+e.getWheelRotation());
				Float val = (Float) slider.getValue();
				val-= e.getWheelRotation()/100.0f;
				slider.getModel().setValue(val);
				settings.m_EnergyContourLengthCoeff=val;
			}
		});
		panel.add(slider);
		
		
		
		
		final JTextArea tf = new JTextArea("Drop initial guess file here", 20, 20);
		DropTargetListener  dropLabelImage = new DropTargetListener() {
			
			@Override
			public void dropActionChanged(DropTargetDragEvent dtde){}
			
			@Override
			public void drop(DropTargetDropEvent event)
			{
				// Accept copy drops
				event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

				// Get the transfer which can provide the dropped item data
				Transferable transferable = event.getTransferable();

				// Get the data formats of the dropped item
				DataFlavor[] flavors = transferable.getTransferDataFlavors();

				// Loop through the flavors
				for(DataFlavor flavor : flavors) {

					try {
						// If the drop items are files
						if(flavor.isFlavorJavaFileListType()) {
							// Get all of the dropped files
							List<File> files = (List<File>)transferable.getTransferData(flavor);
							// Loop them through
							for(File file : files) {
								tf.setText(file.getPath());
								// Print out the file path
								System.out.println("File path is '" + file.getPath() + "'.");
								doFile=true;
								filename=file.getPath();
								dispose();
							}
						}
					} catch (Exception e) {
						// Print out the error stack
						e.printStackTrace();
					}
				}

				// Inform that the drop is complete
				event.dropComplete(true);
			}
			
			@Override
			public void dragOver(DropTargetDragEvent dtde){}
			
			@Override
			public void dragExit(DropTargetEvent dte){}
			
			@Override
			public void dragEnter(DropTargetDragEvent dtde){}
		};
		new DropTarget(tf, dropLabelImage);
		panel.add(tf);
		
		
		
//
		gd.add(panel);
		
//		JPanel p = new NetbeansPanel();
//		gd.add(p);
		
		gd.showDialog();
	}

	public boolean useRegularization()
	{
		return cb.isSelected();
	}
	
	void dispose()
	{
		gd.dispose();
	}
	
}

class EclipsePanel extends optionPanel
{
	private static final long serialVersionUID = -5147585508691693209L;

	Settings settings;
	
	public boolean doUser;
	public boolean doRand;
	public boolean doRect;
	public boolean doFile;
	
	public String filename;
	
	GenericDialog gd;
	
	public EclipsePanel(Settings s) 
	{
		super();
		this.settings=s;
		gd = new GenericDialog("Region Competition GUI");
		additionalInits();
		
		gd.add(this);
	}
	
	
	public void show()
	{
//		JFrame f = new JFrame();
//		f.add(this);
//		f.pack();
//		f.setVisible(true);
		
		gd.showDialog();
	}


	void additionalInits()
	{
		spinnerRad.setValue(settings.m_CurvatureMaskRadius);
		spinnerWeight.setValue(settings.m_EnergyContourLengthCoeff);
		checkRegularization.setSelected(settings.m_EnergyUseCurvatureRegularization);
		spinnerNEllipses.setValue(5);
		
		spinnerRad.addMouseWheelListener(new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				System.out.println("wheeeeee "+e.getWheelRotation());
				Float val = (Float) spinnerRad.getValue();
				val-= e.getWheelRotation();
				spinnerRad.getModel().setValue(val);
				settings.m_CurvatureMaskRadius=val;
			}
		});
	}
	
	
	void addLabelImageInputDrop()
	{
DropTargetListener  dropLabelImage = new DropTargetListener() {
			
			@Override
			public void dropActionChanged(DropTargetDragEvent dtde){}
			
			@Override
			public void drop(DropTargetDropEvent event)
			{
				// Accept copy drops
				event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

				// Get the transfer which can provide the dropped item data
				Transferable transferable = event.getTransferable();

				// Get the data formats of the dropped item
				DataFlavor[] flavors = transferable.getTransferDataFlavors();

				// Loop through the flavors
				for(DataFlavor flavor : flavors) {

					try {
						// If the drop items are files
						if(flavor.isFlavorJavaFileListType()) {
							// Get all of the dropped files
							List<File> files = (List<File>)transferable.getTransferData(flavor);
							// Loop them through
							for(File file : files) {
								jTextArea2.setText(file.getPath());
								// Print out the file path
								System.out.println("File path is '" + file.getPath() + "'.");
								doFile=true;
								filename=file.getPath();
								dispose();
							}
						}
					} catch (Exception e) {
						// Print out the error stack
						e.printStackTrace();
					}
				}

				// Inform that the drop is complete
				event.dropComplete(true);
			}
			
			@Override
			public void dragOver(DropTargetDragEvent dtde){}
			
			@Override
			public void dragExit(DropTargetEvent dte){}
			
			@Override
			public void dragEnter(DropTargetDragEvent dtde){}
		};
		new DropTarget(jTextArea1, dropLabelImage);
	}
	
	@Override
	void bInputEllipsesActionPerformed(ActionEvent evt) 
	{
		doUser=true;
		dispose();
	}
	
	@Override
	void bInputRectActionPerformed(ActionEvent evt) 
	{
		doRect=true;
		dispose();
	}
	
	@Override
	void bInputUserActionPerformed(ActionEvent evt) 
	{
		doRand=true;
		dispose();
	}
	
	@Override
    void bCancelActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }
    
	void dispose()
	{
		gd.dispose();
	}
	
}

class optionPanel extends javax.swing.JPanel {

    public optionPanel() 
    {
    	
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) 
        {
        	System.out.println(info.getName());
            if ("Windows Classic".equals(info.getName())) {
                try {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedLookAndFeelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                continue;
            }
        }
    	
    	
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        bInputRect = new javax.swing.JButton();
        spinnerRectRatio = new javax.swing.JSpinner();
        bInputEllipses = new javax.swing.JButton();
        spinnerNEllipses = new javax.swing.JSpinner();
        bInputUser = new javax.swing.JButton();
        checkRegularization = new javax.swing.JCheckBox();
        spinnerRad = new javax.swing.JSpinner();
        labelRad = new javax.swing.JLabel();
        spinnerWeight = new javax.swing.JSpinner();
        labelWeight = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        bCancel = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();

        bInputRect.setText("Rectangle");
        bInputRect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bInputRectActionPerformed(evt);
            }
        });

        spinnerRectRatio.setModel(new javax.swing.SpinnerNumberModel(0.75d, 0.0d, 1.0d, 0.01d));

        bInputEllipses.setText("Random Ellipses");
        bInputEllipses.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bInputEllipsesActionPerformed(evt);
            }
        });

        spinnerNEllipses.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(5), Integer.valueOf(0), null, Integer.valueOf(1)));

        bInputUser.setText("User ROI");
        bInputUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bInputUserActionPerformed(evt);
            }
        });

        checkRegularization.setText("use Regularization");

        spinnerRad.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(4), Integer.valueOf(0), null, Integer.valueOf(1)));

        labelRad.setText("Radius");

        spinnerWeight.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(0.04d), Double.valueOf(0.0d), null, Double.valueOf(0.01d)));

        labelWeight.setText("Weight");

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(100, 100));
        jScrollPane1.setWheelScrollingEnabled(false);

        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jTextArea1);

        bCancel.setText("Cancel");
        bCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bCancelActionPerformed(evt);
            }
        });

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPane2.setPreferredSize(new java.awt.Dimension(100, 100));
        jScrollPane2.setWheelScrollingEnabled(false);

        jTextArea2.setColumns(20);
        jTextArea2.setLineWrap(true);
        jTextArea2.setRows(5);
        jTextArea2.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jTextArea2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(spinnerWeight, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelWeight, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(spinnerRad, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelRad, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(checkRegularization, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bInputUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bInputEllipses, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bInputRect, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(spinnerRectRatio)
                    .addComponent(spinnerNEllipses, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE))
                .addGap(46, 46, 46)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(102, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(473, Short.MAX_VALUE)
                .addComponent(bCancel)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(85, 85, 85)
                            .addComponent(bCancel)
                            .addContainerGap())
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(spinnerRectRatio, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                                .addComponent(bInputRect))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(spinnerNEllipses, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(bInputEllipses, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(bInputUser)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(checkRegularization)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(spinnerRad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(labelRad))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(spinnerWeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(labelWeight))
                            .addGap(69, 69, 69)))))
        );
    }// </editor-fold>

    void bInputRectActionPerformed(java.awt.event.ActionEvent evt) {                                           
        // TODO add your handling code here:
    }                                          

    void bInputEllipsesActionPerformed(java.awt.event.ActionEvent evt) {                                               
        // TODO add your handling code here:
    }                                              

    void bInputUserActionPerformed(java.awt.event.ActionEvent evt) {                                           
        // TODO add your handling code here:
    }                                          

    void bCancelActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    // Variables declaration - do not modify
    javax.swing.JButton bCancel;
    javax.swing.JButton bInputEllipses;
    javax.swing.JButton bInputRect;
    javax.swing.JButton bInputUser;
    javax.swing.JCheckBox checkRegularization;
    javax.swing.JScrollPane jScrollPane1;
    javax.swing.JScrollPane jScrollPane2;
    javax.swing.JTextArea jTextArea1;
    javax.swing.JTextArea jTextArea2;
    javax.swing.JLabel labelRad;
    javax.swing.JLabel labelWeight;
    javax.swing.JSpinner spinnerNEllipses;
    javax.swing.JSpinner spinnerRad;
    javax.swing.JSpinner spinnerRectRatio;
    javax.swing.JSpinner spinnerWeight;
    // End of variables declaration
}




