package mosaic.region_competition.netbeansGUI;

import ij.gui.GenericDialog;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JSpinner;

import mosaic.region_competition.Settings;

public class UserDialog extends OptionPanel
{
	Settings settings;
	
	public boolean doRect;
	public boolean doRand;
	public boolean doUser;
	public boolean doFile;
	
	public boolean useStack = true;
	public boolean showStatistics = false;
	public boolean useOldRegionIterator=false;
//	public int kbest=5;
	
	public String filename;

	String title;
	GenericDialog gd;

	public JSpinner activeSpinner;

	
	public UserDialog(Settings s) 
	{
		super();
		this.settings=s;
		additionalInits();
		gd = new GenericDialog(title);
	}
	
	public void showNetbeans()
	{
		gd.add(this);
		gd.showDialog();		
	}
	

	void additionalInits()
	{
		spinnerRad.setValue(settings.m_CurvatureMaskRadius);
		spinnerWeight.setValue(settings.m_EnergyContourLengthCoeff);
		checkRegularization.setSelected(settings.m_EnergyUseCurvatureRegularization);
		spinnerNEllipses.setValue(5);
		
		if(!settings.m_EnergyUseCurvatureRegularization){
			comboRegularization.setSelectedIndex(0);
		} else {
			if(useOldRegionIterator){
				comboRegularization.setSelectedIndex(1);
			} else {
				comboRegularization.setSelectedIndex(2);
			}
		}
		
		addLabelImageInputDrop();
		addWheel();
	}
	
	void addWheel()
	{
		
//		spinnerWeight.addMouseWheelListener(new SpinnerWheelListener());
//		spinnerRad.addMouseWheelListener(new SpinnerWheelListener());
//		spinnerNEllipses.addMouseWheelListener(new SpinnerWheelListener());
//		spinnerRectRatio.addMouseWheelListener(new SpinnerWheelListener());
		
		Field[] fields = OptionPanel.class.getDeclaredFields();
		
		for(Field f : fields)
		{
			Object o;
			try
			{
				o = f.get(this);
				if(o instanceof JSpinner)
				{
					JSpinner spinner = (JSpinner)o;
					spinner.addMouseWheelListener(new SpinnerWheelListener());
				}
			}
			catch (IllegalArgumentException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
								filename=file.getPath();
								jTextArea1.setText(filename);
								// Print out the file path
								System.out.println("File path is '" + filename + "'.");
								doFile=true;
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
		doRand=true;
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
		doUser=true;
		dispose();
	}
	
	@Override
	void bCancelActionPerformed(java.awt.event.ActionEvent evt) {
		dispose();
    }
	
	@Override
	void comboRegularizationActionPerformed(ActionEvent evt)
	{
		JComboBox combo = (JComboBox)evt.getSource();	
		int i = combo.getSelectedIndex();
		
		if(i==0)
			checkRegularization.setSelected(false);
		if(i==1){
			checkRegularization.setSelected(true);
			useOldRegionIterator=true;
		}
		if(i==2){
			checkRegularization.setSelected(true);
			useOldRegionIterator=false;
		}
		
		System.out.println("combo action "+i);
	}
    
	
	public int getNEllipses()
	{
		return (Integer)spinnerNEllipses.getValue();
	}
	
	public boolean useRegularization()
	{
		return checkRegularization.isSelected();
	}
	
	public int getKBest()
	{
		return (Integer)spinnerKbest.getValue();
	}
	
	void writeBack()
	{
		settings.m_CurvatureMaskRadius=(Float)spinnerRad.getValue();
		settings.m_EnergyContourLengthCoeff = (Float)spinnerWeight.getValue();
		settings.m_EnergyUseCurvatureRegularization = checkRegularization.isSelected();
		
	}
	
	void dispose()
	{
		writeBack();
		gd.dispose();
	}
	
}

class SpinnerWheelListener implements MouseWheelListener
{

	JSpinner spinner;
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		spinner = (JSpinner)e.getComponent();
		
		//pos values
		int amount = e.getWheelRotation();
		for(int i=0; i<amount; i++)
		{
			Object next=spinner.getModel().getPreviousValue();
			if(next==null)
				break;
			spinner.setValue(next);
		}
		
		//neg values
		amount=-amount;
		for(int i=0; i<amount; i++)
		{
			Object next=spinner.getModel().getNextValue();
			if(next==null)
				break;
			spinner.setValue(next);
		}
		
		
	}
	
}

