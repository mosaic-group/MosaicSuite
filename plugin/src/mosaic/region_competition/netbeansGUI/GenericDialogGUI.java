package mosaic.region_competition.netbeansGUI;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.util.List;
import java.util.Vector;

import mosaic.region_competition.Settings;
import ij.IJ;
import ij.gui.GenericDialog;

public class GenericDialogGUI implements InputReadable
{
	public Settings settings;
	GenericDialog gd;
	
	public String filenameInput;
	public String filenameLabelImage;
	
	private LabelImageInput labelImageInput;
	
	public boolean useStack = true;
	public boolean showStatistics = false;
	
	boolean useRegularization;
	public boolean useOldRegionIterator=false;
	
	static final String Regularization = "Regularization";
	static final String No_Regularization="No_Regularization";
	static final String Sphere_Regularization="Sphere_Regularization";
	static final String Sphere_Regularization_OLD="Sphere_Regularization_OLD";
	
	
	static final String Initialization = "Initialization";
	static final String Rectangular_Initialization="Rectangular_Initialization";
	static final String Random_Ellipses_Initialization="Random_Ellipses_Initialization";
	static final String User_Defined_Initialization="User_Defined_Initialization";
	static final String Bubbles_Initialization="Bubbles_Initialization";
	static final String File_Initalization="File_Initalization";
	
	static final String TextDefaultInputImage="Drop Input Image here, or insert Path to file";
	static final String TextDefaultLabelImage="Drop Label Image here, or insert Path to file";
	
	
//	private Choice initializationChoice;
	
	public GenericDialogGUI(Settings s)
	{
		this.settings=s;
		gd = new GenericDialog("Region Competition");
		
		String[] regularizationItems= {
				No_Regularization,
				Sphere_Regularization, 
				Sphere_Regularization_OLD};
		gd.addChoice(Regularization, regularizationItems, regularizationItems[1]);
		//save reference to this choice, so we can handle it
//		initializationChoice = (Choice)gd.getChoices().lastElement();
		
		String[] initializationItems= {
				Rectangular_Initialization, 
				Random_Ellipses_Initialization, 
				User_Defined_Initialization, 
				Bubbles_Initialization, 
				File_Initalization};
		gd.addChoice(Initialization, initializationItems, initializationItems[0]);
		
		gd.addNumericField("m_CurvatureMaskRadius", settings.m_CurvatureMaskRadius, 0);
		gd.addNumericField("m_EnergyContourLengthCoeff", settings.m_EnergyContourLengthCoeff, 4);
		
		
		gd.addTextAreas(TextDefaultInputImage, 
						TextDefaultLabelImage, 10, 30);
		new TextAreaListener(this, gd.getTextArea1(), TextDefaultInputImage);
		new TextAreaListener(this, gd.getTextArea2(), TextDefaultLabelImage);
		
//		TextArea ta = gd.getTextArea1();
//		addLabelImageInputDrop(ta);
		
		Panel p = new Panel();
			Button b = new Button("Open Input Image");
			b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea1()));
			p.add(b);
			
			b = new Button("Open Label Image");
			b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea2()));
			p.add(b);
		gd.addPanel(p);
		
//		gd.add(b);
		
		addWheelListeners();
		
		gd.showDialog();
	}
	
	private void addWheelListeners()
	{
		Vector<TextField> v = gd.getNumericFields();
		for(TextField tf:v)
		{
			tf.addMouseWheelListener(new NumericFieldWheelListener(tf));
		}
	}
	
//	void setLabelImageToFileInput()
//	{
//		//TODO this does not work
//		initializationChoice.select(File_Initalization);
////		int id=0;
////		int stateChange=0;
////		ItemEvent ie = new ItemEvent(initializationChoice, id, initializationChoice, stateChange);
//	}
	
//	private void addLabelImageInputDrop(TextArea ta)
//	{
//		DropTargetListener dropLabelImage = new DnDListener(ta);
//		new DropTarget(ta, dropLabelImage);
//	}
	
	/**
	 * Reads out the values from the input mask and saves them into the settings object. <br>
	 * Input Processing (valid values)
	 */
	@Override
	public boolean processInput()
	{
		
		if(gd.wasCanceled())
			return false;
		
		boolean success = true;
		
		// Regularization Choice
		
		String regularization = gd.getNextChoice();
		if(regularization.equals(No_Regularization))
		{
			useRegularization=false;
		}
		else
		{
			useRegularization=true;
			if(regularization.equals(Sphere_Regularization))
			{
				useOldRegionIterator=false;
			}
			else if(regularization.equals(Sphere_Regularization_OLD))
			{
				useOldRegionIterator=true;
			}
			else
			{
				IJ.log("No valid Regularization");
				success = false;
			}
		}
		settings.m_EnergyUseCurvatureRegularization=useRegularization;
		
		settings.m_CurvatureMaskRadius=(int)gd.getNextNumber();
		settings.m_EnergyContourLengthCoeff=(float)gd.getNextNumber();
		
		// Initial Choice
		String initialization = gd.getNextChoice();
		
		//TODO put this into enum
		if(initialization.equals(Rectangular_Initialization))
		{
			labelImageInput=LabelImageInput.Rectangle;
		}
		else if(initialization.equals(Random_Ellipses_Initialization))
		{
			labelImageInput=LabelImageInput.Ellipses;
		}
		else if(initialization.equals(User_Defined_Initialization))
		{
			labelImageInput=LabelImageInput.UserDefinedROI;
		}
		else if(initialization.equals(Bubbles_Initialization))
		{
			labelImageInput=LabelImageInput.Bubbles;
		}
		else if(initialization.equals(File_Initalization))
		{
			labelImageInput=LabelImageInput.File;
		}
		else
		{
			IJ.log("No valid LabelImage Choice");
			labelImageInput=null;
			success = false;
		}
		
		//only record valid inputs
		filenameInput=gd.getTextArea1().getText();
		if(filenameInput==null || filenameInput.isEmpty() || filenameInput.equals(TextDefaultInputImage))
		{
			//set text to empty string if nothing was edited. 
			gd.getTextArea1().setText("");
		}
		filenameInput=gd.getNextText();

		
		filenameLabelImage=gd.getTextArea2().getText();
		if(filenameLabelImage==null || filenameLabelImage.isEmpty() || filenameLabelImage.equals(TextDefaultLabelImage))
		{
			//set text to empty string if nothing was edited. 
			gd.getTextArea2().setText("");
		}
		filenameLabelImage=gd.getNextText();
		
		return success;
	}


	@Override
	public LabelImageInput getLabelImageInput()
	{
		return labelImageInput;
	}


	@Override
	public String getLabelImageFilename()
	{
		return filenameLabelImage;
	}


	@Override
	public String getInputImageFilename()
	{
		return filenameInput;
	}


	@Override
	public boolean useStack()
	{
		return useStack;
	}


	@Override
	public boolean showStatistics()
	{
		return showStatistics;
	}


	@Override
	public boolean useOldRegionIterator()
	{
		return useOldRegionIterator;
	}


	@Override
	public int getKBest()
	{
		//TODO kbest
		return 0;
	}


	@Override
	public boolean useRegularization()
	{
		return useRegularization;
	}



}


/**
 * DropTargetListener for TextArea, so one can drag&drop a File into the textArea 
 * and the filesource gets written in. 
 * Usage: Just create a new instance, 
 * with TextArea and DefaultText (Text shown in TextArea) in constructor
 */
class TextAreaListener implements DropTargetListener, TextListener, FocusListener
{
	TextArea textArea;
	String defaultText;
	GenericDialogGUI gd;
	
	public TextAreaListener(GenericDialogGUI gd, TextArea ta, String defaulftTxt)
	{
		this.gd=gd;
		this.textArea=ta;
		this.defaultText=defaulftTxt;
		new DropTarget(ta, this);
		ta.addTextListener(this);
		ta.addFocusListener(this);
	}

	
	@Override
	public void dropActionChanged(DropTargetDragEvent dtde){}
	
	@Override
	public void drop(DropTargetDropEvent event)
	{
		String filename;
		
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
						textArea.setText(filename);
						// Print out the file path
						System.out.println("File path is '" + filename + "'.");
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

	// TextListener
	
	@Override
	public void textValueChanged(TextEvent e)
	{
		String text = textArea.getText();
		if(text.isEmpty() || text.equals(defaultText))
		{
			//do nothing
		}
		else
		{
			// there was a non-default change in the textfield. 
			// set input choise to file if it was TextArea for labelImage 
//			if(defaultText.equals(gd.TextDefaultLabelImage))
//			{
//				gd.setLabelImageToFileInput();
//			}
		}
		System.out.println("test changed to: "+textArea.getText());
	}


	@Override
	public void focusGained(FocusEvent e)
	{
		System.out.println("focus gained");
		if(textArea.getText().equals(defaultText))
		{
			// delete defaultText on focus gain to allow input
			textArea.setText("");
		}
		else
		{
			// do nothing if text was edited
		}
	}


	@Override
	public void focusLost(FocusEvent e)
	{
		if(textArea.getText().isEmpty())
		{
			// there was no input. recover default text on focus lost
			textArea.setText(defaultText);
		}
		else
		{
			// do nothing if text was edited
		}
	}
	
}

/**
 * Opens a FileDialog and writes the path of the file into the TextArea 
 * (so GenericDialog can read from it and do the Macro parsing) <br>
 * If FileDialog is canceled, TextArea is not changed. 
 */
class FileOpenerActionListener implements ActionListener
{
	GenericDialog gd;
	TextArea ta;
	
	public FileOpenerActionListener(GenericDialog gd, TextArea ta)
	{
		this.gd=gd;
		this.ta=ta;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		FileDialog fd = new FileDialog(gd);
		fd.setVisible(true);
		String dir = fd.getDirectory();
		String file = fd.getFile();
		
		if(file!=null && dir!=null)
		{
			ta.setText(dir+file);
		}
	}
}


class NumericFieldWheelListener implements MouseWheelListener
{
	TextField tf;
	
	public NumericFieldWheelListener(TextField tf)
	{
		this.tf=tf;
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int n = e.getWheelRotation();
		
		double fac = 0.1;
		if(n>0)
			fac = 1-fac;
		else
			fac = 1+fac;
		
		n=Math.abs(n);
		
		double val = Double.valueOf(tf.getText());
		System.out.println(val);
		for(int i=0; i<n; i++)
		{
			val*=fac;
		}
		tf.setText(Double.toString(val));
		System.out.println("wheeee "+val);
		
	}
	
}




