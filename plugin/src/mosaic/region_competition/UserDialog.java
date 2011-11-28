package mosaic.region_competition;

import ij.gui.GenericDialog;

import java.awt.Button;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.JTextArea;
import javax.swing.JTextField;

public class UserDialog
{
	
	public boolean doUser;
	public boolean doRand;
	public boolean doRect;
	public boolean doFile;
	
	public String filename;

	String title;
	
	public UserDialog(String title) 
	{
		this.title=title;
	}
	
	/**
	 * waits until button pressed
	 */
	public void showDialog()
	{
		
		final GenericDialog gd = new GenericDialog(title);
		
		gd.addMessage("addMessage");
//		gd.addChoice("choice", new String[]{"choice 1", "choice 2"}, "choice 2");
//		gd.addCheckbox("yes or no", false);
//		gd.addSlider("myslider", -20, 42, 13);
		
		Panel panel = new Panel();
		
		Button bUser = new Button("UserDefined");
		Button bRand = new Button("RandomEllipse");
		Button bRect = new Button("Rectangle");
		
		bUser.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doUser=true;
				gd.dispose();
			}
		});
		
		bRand.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doRand=true;
				gd.dispose();
			}
		});
		
		bRect.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doRect=true;
				gd.dispose();
			}
		});
		
		panel.add(bUser);
		panel.add(bRand);
		panel.add(bRect);
		
		
		final JTextArea tf = new JTextArea("Drop initial guess file here", 20, 20);
		DropTargetListener  myDragDropListener = new DropTargetListener() {
			
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
								gd.dispose();
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
		
		new DropTarget(tf, myDragDropListener);
		
		panel.add(tf);
		
		
//
		gd.add(panel);
		
		
		gd.showDialog();
	}
}
