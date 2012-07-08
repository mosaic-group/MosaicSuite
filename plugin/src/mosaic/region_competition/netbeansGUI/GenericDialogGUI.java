package mosaic.region_competition.netbeansGUI;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JTextArea;

import mosaic.plugins.Region_Competition;
import mosaic.region_competition.EnergyFunctionalType;
import mosaic.region_competition.Settings;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

/**
 * Adapts GenericDialog from ImageJ for our purposes
 */
public class GenericDialogGUI implements InputReadable
{
	private Settings settings;
	private GenericDialog gd;
	private ImagePlus aImp; // active ImagePlus (argument of Plugin)
	
	private String filenameInput;		// image to be processed
	private String filenameLabelImage;	// initialization
	
	private String inputImageTitle;
	private String labelImageTitle;
	
	private LabelImageInitType labelImageInitType;

	private int kbest = 1;
	private boolean useStack = true;
	private boolean showStatistics = false;
	private boolean useRegularization;
	private boolean useOldRegionIterator=false;
	
	static final String EnergyFunctional = "EnergyFunctional";
	static final String e_CV = "e_CV";
	static final String e_GaussPS = "e_GaussPS";
	
	static final String Regularization = "Regularization";
	static final String No_Regularization="No Regularization";
	static final String Sphere_Regularization="Sphere Regularization";
	static final String Sphere_Regularization_OLD="Sphere Regularization OLD";
	
	static final String Initialization = "Initialization";
	static final String Rectangular_Initialization="Rectangular Initialization";
	static final String Random_Ellipses_Initialization="Random Ellipses Initialization";
	static final String User_Defined_Initialization="User Defined Initialization";
	static final String Bubbles_Initialization="Bubbles Initialization";
	static final String File_Initalization="File Initalization";
	
	static final String TextDefaultInputImage="Input Image: \n\n" +
			"Drop image here,\n" +
			"insert Path to file,\n" +
			"or press Button below";
	static final String TextDefaultLabelImage="Drop Label Image here, or insert Path to file";
	
	
	public GenericDialogGUI(Region_Competition region_Competition)
	{
		
//		EmptyGenericDialog gde = new EmptyGenericDialog("Region Competition");
//		if(gde!= null)
//		{
//			gde.showDialog();
//			gde.recordValues();
//			return;
//		}
		
		this.settings=region_Competition.settings;
		aImp = region_Competition.getOriginalImPlus();
		gd = new GenericDialog("Region Competition");
		
		
		// components: 
		final TextField tfBalloonForce;
		final Choice choiceEnergy;
		
		
		
		// energy
		
		String[] energyItems = {
				e_CV,
				e_GaussPS, 
				};
		//TODO default choice
		gd.addChoice(EnergyFunctional, energyItems, energyItems[0]);
		choiceEnergy = (Choice)gd.getChoices().lastElement();
		
		
		// Regularization
		
		String[] regularizationItems= {
				No_Regularization,
				Sphere_Regularization, 
				Sphere_Regularization_OLD, 
				};
		gd.addChoice(Regularization, regularizationItems, regularizationItems[1]);
		
		
		
		// Label Image Initialization
		
		String[] initializationItems= {
				Rectangular_Initialization, 
				Random_Ellipses_Initialization, 
				User_Defined_Initialization, 
				Bubbles_Initialization, 
				File_Initalization, 
				};
		
		// default choice
		String defaultInit = Bubbles_Initialization;
		if(aImp!=null && aImp.getRoi()!=null)
		{
			defaultInit=User_Defined_Initialization;
		}
		gd.addChoice(Initialization, initializationItems, defaultInit);
		
		
//		save reference to this choice, so we can handle it
		initializationChoice = (Choice)gd.getChoices().lastElement();
//		lastInitChoice=initializationChoice.getSelectedItem();
		
		// Numeric Fields
		
		gd.addNumericField("m_CurvatureMaskRadius", settings.m_CurvatureMaskRadius, 0);
		gd.addNumericField("m_EnergyContourLengthCoeff", settings.m_EnergyContourLengthCoeff, 4);
		gd.addNumericField("merge_Threshold", settings.m_RegionMergingThreshold, 4);

		
		gd.addNumericField("m_MaxNbIterations", settings.m_MaxNbIterations, 0);
		
		gd.addNumericField("Balloon_force", settings.m_BalloonForceCoeff, 4);
		tfBalloonForce=(TextField)gd.getNumericFields().lastElement();
		
		
		// File path text areas
		
		gd.addTextAreas(TextDefaultInputImage, 
						TextDefaultLabelImage, 10, 30);
		new TextAreaListener(this, gd.getTextArea1(), TextDefaultInputImage);
		new TextAreaListener(this, gd.getTextArea2(), TextDefaultLabelImage);
		
		
		// File opener Buttons
		
		Panel p = new Panel();
			Button b = new Button("Open Input Image");
			b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea1()));
			p.add(b);
			
			b = new Button("Open Label Image");
			b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea2()));
			p.add(b);
			
		gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));
//		gd.addPanel(p);
//		gd.add(b);
		
		
		addOpenedImageChooser();
		
		gd.addCheckbox("Show_Statistics", showStatistics);
		gd.addCheckbox("Show_Stack", useStack);
		gd.addNumericField("kbest", 0, 0);
		
		
		// components 2
		
		choiceEnergy.addItemListener(new ItemListener() 
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				System.out.println(e.getItem());
				if(((Choice)e.getSource()).getSelectedItem().equals(e_GaussPS))
				{
					tfBalloonForce.setText("0.01");
				}
				else if(e.getItem().equals(e_CV))
				{
					tfBalloonForce.setText("0.0");
				}
			}
		});
		
		addWheelListeners();
	}
	
	public void showDialog()
	{
		gd.showDialog();
	}
	
	
	// Choice for open images in IJ
	
	private int nOpenedImages = 0;
	Choice choiceLabelImage;
	Choice choiceInputImage;
	private void addOpenedImageChooser()
	{
		nOpenedImages = 0;
		int[] ids = WindowManager.getIDList();
		
		if(ids!=null){
			nOpenedImages = ids.length;
		}
		
		// show opened image titles
//		for(int id: ids)
//		{
//			ImagePlus ip = WindowManager.getImage(id);
//			System.out.println("ID: "+id+" "+ip.getTitle());
//		}
		
		String[] names = new String[nOpenedImages+1];
		names[0]="";
		for(int i = 0; i<nOpenedImages; i++)
		{
			names[i+1] = WindowManager.getImage(ids[i]).getTitle();
		}
		
//		if(nOpenedImages>0)
		{
			// Input Image
			gd.addChoice("InputImage", names, names[0]);
			choiceInputImage = (Choice)gd.getChoices().lastElement();
			if(aImp!=null){
				String title = aImp.getTitle();
				choiceInputImage.select(title);
			}
			
			// Label Image
			gd.addChoice("LabelImage", names, names[0]);
			choiceLabelImage = (Choice)gd.getChoices().lastElement();
			
			// add listener to change labelImage initialization to file
			choiceLabelImage.addItemListener(new ItemListener() 
			{
				@Override
				public void itemStateChanged(ItemEvent e)
				{
					Choice choice = (Choice)e.getSource();
					int idx = choice.getSelectedIndex();
					
					if(idx>0){
						setInitToFileInput();
					}
				}
			});
		}
		
	}
	
	private void readOpenedImageChooser()
	{
//		if(nOpenedImages>0)
		if(true)
		{
			inputImageTitle = gd.getNextChoice();
			labelImageTitle = gd.getNextChoice();
		}
		else
		{
			inputImageTitle = null;
			labelImageTitle = null;
		}
	}
	
	/**
	 * Adds MouseWheelListeners to each NumericField in GenericDialog
	 */
	private void addWheelListeners()
	{
		Vector<TextField> v = gd.getNumericFields();
		for(TextField tf:v)
		{
			tf.addMouseWheelListener(new NumericFieldWheelListener(tf));
		}
	}
	
	
	/**
	 * Reads out the values from the input mask 
	 * and saves them into the settings object. <br>
	 * Input Processing (valid values)
	 */
	@Override
	public boolean processInput()
	{
		
		if(gd.wasCanceled())
			return false;
		
		boolean success = true;
		
		// Energy Choice
		
		String energy = gd.getNextChoice();
		if(energy.equals(e_CV))
		{
			settings.m_EnergyFunctional = EnergyFunctionalType.e_CV;
		} 
		else if (energy.equals(e_GaussPS))
		{
			settings.m_EnergyFunctional = EnergyFunctionalType.e_GaussPS;
		}
		else
		{
			success = false;
		}

		
		
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
		settings.m_RegionMergingThreshold = (float)gd.getNextNumber();
		settings.m_MaxNbIterations = (int)gd.getNextNumber();
		
		settings.m_BalloonForceCoeff = (float)gd.getNextNumber();
		
		// Initial Choice
		String initialization = gd.getNextChoice();
		
		//TODO put this into enum
		if(initialization.equals(Rectangular_Initialization))
		{
			labelImageInitType=LabelImageInitType.Rectangle;
		}
		else if(initialization.equals(Random_Ellipses_Initialization))
		{
			labelImageInitType=LabelImageInitType.Ellipses;
		}
		else if(initialization.equals(User_Defined_Initialization))
		{
			labelImageInitType=LabelImageInitType.UserDefinedROI;
		}
		else if(initialization.equals(Bubbles_Initialization))
		{
			labelImageInitType=LabelImageInitType.Bubbles;
		}
		else if(initialization.equals(File_Initalization))
		{
			labelImageInitType=LabelImageInitType.File;
		}
		else
		{
			IJ.log("No valid LabelImage Choice");
			labelImageInitType=null;
			success = false;
		}
		
		//only record valid inputs
		filenameInput=gd.getTextArea1().getText();
		if(filenameInput==null || filenameInput.isEmpty() || filenameInput.equals(TextDefaultInputImage))
		{
			//TODO 
			// set text to [] due to a bug in GenericDialog
			// if bug gets fixed, this will cause problems!
			gd.getTextArea1().setText("[]");
		}
		else
		{
			String s = filenameInput.replace('\\', '/');
			gd.getTextArea1().setText(s);
		}
		filenameInput=gd.getNextText();

		
		filenameLabelImage=gd.getTextArea2().getText();
		if(filenameLabelImage==null || filenameLabelImage.isEmpty() || filenameLabelImage.equals(TextDefaultLabelImage))
		{
			//TODO IJ BUG
			// set text to [] due to a bug in GenericDialog
			// (cannot macro read boolean after empty text field)
			// if bug gets fixed, this will cause problems!
			gd.getTextArea2().setText("[]");
		}
		else
		{
			String s = filenameLabelImage.replace('\\', '/');
			gd.getTextArea2().setText(s);
		}
		filenameLabelImage=gd.getNextText();
		
		// TODO IJ BUG
		if(filenameLabelImage.equals("[]")){
			filenameLabelImage="";
		}
		
		
		
		readOpenedImageChooser();
		
		showStatistics=gd.getNextBoolean();
		useStack=gd.getNextBoolean();
		
		kbest = (int)gd.getNextNumber();
		
		return success;
	}


	@Override
	public LabelImageInitType getLabelImageInitType()
	{
		return labelImageInitType;
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
		return kbest;
	}


	@Override
	public boolean useRegularization()
	{
		return useRegularization;
	}

	@Override
	public int getNumIterations()
	{
		return settings.m_MaxNbIterations;
	}

	////////////////////////////
	
	private Choice initializationChoice; 	// reference to the awt.Choice for initialization
//	private String lastInitChoice; 			// choice before File_choice was set automatically
	
	void setInitToFileInput()
	{
//		lastInitChoice=initializationChoice.getSelectedItem();
		initializationChoice.select(File_Initalization);
	}
//	void setInitToLastChoice()
//	{
//		System.out.println("change to last="+lastInitChoice);
//		initializationChoice.select(lastInitChoice);
//	}
	
	void setInputImageChoiceEmpty()
	{
		if(choiceInputImage != null)
		{
			choiceInputImage.select(0);
		}
	}
	
	void setLabelImageChoiceEmpty()
	{
		if(choiceLabelImage != null)
		{
			choiceLabelImage.select(0);
		}
	}
	

	@Override
	public ImagePlus getInputImage()
	{
		return WindowManager.getImage(inputImageTitle);
	}
	
	
	@Override
	public ImagePlus getLabelImage()
	{
		return WindowManager.getImage(labelImageTitle);
	}


}


/**
 * DropTargetListener for TextArea, so one can drag&drop a File into the textArea 
 * and the file source gets written in. 
 * Usage: Just create a new instance, 
 * with TextArea and DefaultText (Text shown in TextArea) in constructor
 */
class TextAreaListener implements DropTargetListener, TextListener, FocusListener //, MouseListener
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
//		ta.addMouseListener(this);
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
						
						IJ.open(filename);
						
						// Print out the file path
						System.out.println("File path is '" + filename + "'.");
					}
				}
				else if (flavor.isRepresentationClassInputStream())
				{
					JTextArea ta = new JTextArea();
					ta.read(new InputStreamReader((InputStream)transferable.getTransferData(flavor)), "from system clipboard");
					
					String dndString = ta.getText().trim();
					StringTokenizer tokenizer = new StringTokenizer(dndString);
					String elem="";
					while(tokenizer.hasMoreElements())
					{
						elem = (String)tokenizer.nextToken();
						if(elem.startsWith("file"))
							break;
						else
							elem="";
					}

					textArea.setText(elem);
					ta.setText(null);
					break;
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
	public void dragOver(DropTargetDragEvent dtde){
		System.out.println("dragOver "+dtde);
	}
	
	@Override
	public void dragExit(DropTargetEvent dte){
		System.out.println("dragExit "+dte);
	}
	
	@Override
	public void dragEnter(DropTargetDragEvent dtde){
		System.out.println("dragEnter "+dtde);
	}

	// TextListener
	
	@Override
	public void textValueChanged(TextEvent e)
	{
		// Change input choice to file if text in textfield was changed explicitly
		
		String text = textArea.getText();
		if(text.isEmpty() || text.equals(defaultText))
		{
			// changed to default, do nothing
		}
		else
		{
			// there was a non-default change in the textfield. 
			// set input choice to file if it was TextArea for labelImage 
			if(defaultText.equals(gd.TextDefaultLabelImage))
			{
				gd.setInitToFileInput();
				gd.setLabelImageChoiceEmpty();
			}
			if(defaultText.equals(gd.TextDefaultInputImage))
			{
				gd.setInputImageChoiceEmpty();
			}
		}
//		System.out.println("tf changed to: "+textArea.getText());
	}


	@Override
	public void focusGained(FocusEvent e)
	{
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

/**
 * MouseWheelListener for NumericField <br>
 * Value in NumericField grows/shrinks by factor fac per MouseWheel "click"
 * Field is assumed to be floating point. 
 */
class NumericFieldWheelListener implements MouseWheelListener
{
	double fac = 0.1;
	TextField tf;
	
	public NumericFieldWheelListener(TextField tf)
	{
		this.tf=tf;
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int n = e.getWheelRotation();
		
		boolean inc=false;
		double f = 1.0;
		if(n>0){
			f = 1-fac;
		}
		else{
			f = 1+fac;
			inc=true;
		}
		
		n=Math.abs(n);
		
		double val = Double.valueOf(tf.getText());
		
		boolean isInteger=false;
		if(val==Math.floor(val))
			isInteger=true;
		
		
		System.out.println(val);
		for(int i=0; i<n; i++)
		{
			val*=f;
		}
		if(isInteger && !inc)
			val=Math.floor(val);
		if(isInteger && inc)
			val=Math.ceil(val);
		if(inc && val==0)
			val=1;
		tf.setText(Double.toString(val));
		System.out.println("wheeee "+val);
	}
	
}






