package mosaic.region_competition.GUI;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Dimension;
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
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JTextArea;

import mosaic.plugins.Region_Competition;
import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.energies.RegularizationType;
import mosaic.region_competition.initializers.InitializationType;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

/**
 * Adapts GenericDialog from ImageJ for our purposes
 */
public class GenericDialogGUI implements InputReadable
{
	private Region_Competition MVC;
	
	private Settings settings;
	private GenericDialog gd;
	private GenericDialog gd_p;
	private ImagePlus aImp; // active ImagePlus (argument of Plugin)
	
	private String filenameInput;		// image to be processed
	private String filenameLabelImage;	// initialization
	
	private String inputImageTitle;
	private String labelImageTitle;
	
	private int kbest = 1;
	private boolean showStatistics = false;
	private boolean showNormalized = false;
	private boolean useStack = true;
	public boolean keepAllFrames = true;	// keep result of last segmentation iteratior?
	private boolean show3DResult = false;
//	private boolean useRegularization;
	private boolean useOldRegionIterator=false;
	
	static final String EnergyFunctional = "EnergyFunctional";
	EnergyGUI energyGUI;
	static final String Regularization = "Regularization";
	RegularizationGUI regularizationGUI;
	
	private static final String Initialization = "Initialization";
	
	static final String TextDefaultInputImage="Input Image: \n\n" +
			"Drop image here,\n" +
			"insert Path to file,\n" +
			"or press Button below";
	static final String TextDefaultLabelImage="Drop Label Image here, or insert Path to file";
	
	static final String emptyOpenedImage = "";
	
	/**
	* Create main plugins dialog
	*/
	
	public GenericDialogGUI(Region_Competition region_Competition)
	{
		
//		EmptyGenericDialog gde = new EmptyGenericDialog("Region Competition");
//		if(gde!= null)
//		{
//			gde.showDialog();
//			gde.recordValues();
//			return;
//		}
		this.MVC = region_Competition;
		this.settings=region_Competition.settings;
		aImp = region_Competition.getOriginalImPlus();
		gd = new GenericDialog("Region Competition");
		
		
		// File path text areas
		
		gd.addTextAreas(TextDefaultInputImage, 
						TextDefaultLabelImage, 5, 30);
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
		
		// Parameter opener Buttons
		
		p = new Panel();
		b = new Button("Parameters");
		b.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CreateParametersDialog(MVC);
			}
		});
		p.add(b);
		gd.addPanel(p, GridBagConstraints.CENTER, new Insets(0, 25, 0, 0));
		
//		gd.addPanel(p);
//		gd.add(b);
		
		
		addOpenedImageChooser();
		
		gd.addNumericField("kbest", 0, 0);
		
		String[] strings = new String[]{
				"Show_Progress",
				"Keep_Frames", 
				"Show_3D_Result", 
				"Show_Normalized", 
				"Show_Statistics", 
				};
		
		boolean[] bools = new boolean[]{
				useStack, 
				keepAllFrames, 
				show3DResult, 
				showNormalized, 
				showStatistics, 
				};
		
		gd.addCheckboxGroup(2, strings.length, 
				strings, bools);
		
		
		 
//		{
//			b = new Button("delete saved");
//			b.addActionListener(new ActionListener() {
//				
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					String fileName = MVC.savedSettings;
//					File file = new File(fileName);
//					file.delete();
//				}
//			});
//			gd.add(b);
//		}
		
		addWheelListeners();
	}
	
	/**
	* Create parameters dialog
	*/
	
	private void CreateParametersDialog(Region_Competition region_Competition)
	{
		
//		EmptyGenericDialog gde = new EmptyGenericDialog("Region Competition");
//		if(gde!= null)
//		{
//			gde.showDialog();
//			gde.recordValues();
//			return;
//		}

		gd_p = new GenericDialog("Region Competition Parameters");
//		gd_p.setMinimumSize(new Dimension(600, 600));
		
		Button optionButton;
		GridBagConstraints c;
		int gridy=0;
		int gridx=2;
		
		// components: 
		final TextField tfBalloonForce;
		final Choice choiceEnergy;
		final Choice choiceRegularization;
		
		
		// Energy Functional
		
		EnergyFunctionalType[] energyValues = EnergyFunctionalType.values();
		String[] energyItems = new String[energyValues.length];
		for(int i=0; i<energyItems.length; i++)
		{
			energyItems[i]=energyValues[i].name();
		}
		
		
		gd_p.addChoice(EnergyFunctional, energyItems, settings.m_EnergyFunctional.name());
		choiceEnergy = (Choice)gd.getChoices().lastElement();
		{
			optionButton = new Button("Additional Options");
			c = new GridBagConstraints();
			c.gridx=gridx; c.gridy=gridy++; c.anchor = GridBagConstraints.EAST;
			gd_p.add(optionButton,c);
			
			optionButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					String energy = choiceEnergy.getSelectedItem();
					energyGUI = EnergyGUI.factory(settings, energy);
					energyGUI.createDialog();
					energyGUI.showDialog();
					energyGUI.processDialog();
				}
			});
		}
		
		
		
		// Regularization
		
		RegularizationType[] regularizationValues = RegularizationType.values();
		int n = regularizationValues.length;
		String[] regularizationItems = new String[n];
		for(int i=0; i<n; i++)
		{
			regularizationItems[i]=regularizationValues[i].name();
		}
		gd_p.addChoice(Regularization, regularizationItems, settings.regularizationType.name());
		choiceRegularization = (Choice)gd_p.getChoices().lastElement();
		
		{
			optionButton = new Button("Additional Options");
			c = new GridBagConstraints(); c.anchor = GridBagConstraints.EAST;
			c.gridx=gridx; c.gridy=gridy++;
			gd_p.add(optionButton,c);
			
			optionButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e)
				{
					String type = choiceRegularization.getSelectedItem();
					RegularizationGUI gui = RegularizationGUI.factory(settings, type);
					gui.createDialog();
					gui.showDialog();
					gui.processDialog();
									
				}
			});
		}
		
		
		// Label Image Initialization
		
		InitializationType[] initTypes = InitializationType.values();
		String[] initializationItems = new String[initTypes.length];
		
		for(int i=0; i<initializationItems.length; i++)
		{
			initializationItems[i]=initTypes[i].name();
		}
		
		// default choice
		String defaultInit = settings.labelImageInitType.name();
//		if(aImp!=null && aImp.getRoi()!=null)
//		{
//			defaultInit=User_Defined_Initialization;
//		}
		gd_p.addChoice(Initialization, initializationItems, defaultInit);
//		save reference to this choice, so we can handle it
		initializationChoice = (Choice)gd.getChoices().lastElement();
//		lastInitChoice=initializationChoice.getSelectedItem();
		
		{
			optionButton = new Button("Additional Options");
			c = new GridBagConstraints();
			c.gridx=gridx; c.gridy=gridy++; c.anchor = GridBagConstraints.EAST;
			gd.add(optionButton,c);
			
			optionButton.addActionListener(new ActionListener() 
			{
				
				@Override
				public void actionPerformed(ActionEvent e)
				{
					IJ.showMessage("Only for demo purposes, values will not be saved");
					String type = initializationChoice.getSelectedItem();
					InitializationGUI gui = InitializationGUI.factory(settings, type);
					gui.createDialog();
					gui.showDialog();
					gui.processDialog();
				}
			});
		}		
		
		gd_p.addCheckboxGroup(1, 4, new String[]{
				"Fusion", 
				"Fission", 
				"Handles", 
				"Shrink_First"},
				new boolean[]{
				settings.m_AllowFusion, 
				settings.m_AllowFission, 
				settings.m_AllowHandles, 
				settings.shrinkFirstOnly});
		
		
		// Numeric Fields
		
		gd_p.addNumericField("Curvature_Radius", settings.m_CurvatureMaskRadius, 0);
		gd_p.addNumericField("Contour_Length_Coeff", settings.m_EnergyContourLengthCoeff, 4);
		gd_p.addNumericField("Merge_Threshold", settings.m_RegionMergingThreshold, 4);

		
		gd_p.addNumericField("Max_Iterations", settings.m_MaxNbIterations, 0);
		
		gd_p.addNumericField("PS_Radius", settings.m_GaussPSEnergyRadius, 0);
		gd_p.addNumericField("Balloon_Force", settings.m_BalloonForceCoeff, 4);
		tfBalloonForce=(TextField)gd_p.getNumericFields().lastElement();
		
		gd_p.showDialog();
		
		// Dialog destroyed
		// On OK, read parameters
		
		if (gd_p.wasOKed())
			processParameters();
			
			
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
		
		
		String[] names = new String[nOpenedImages+1];
		names[0]=emptyOpenedImage;
		for(int i = 0; i<nOpenedImages; i++)
		{
			ImagePlus ip = WindowManager.getImage(ids[i]);
			names[i+1] = ip.getTitle();
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
			
			// select second image
			if(nOpenedImages>=2 && aImp!= null)
			{
				WindowManager.putBehind();
				String title = WindowManager.getCurrentImage().getTitle();
				choiceLabelImage.select(title);
				WindowManager.toFront(aImp.getWindow());
			}
			
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
	 * Reads out the values from the parameters mask 
	 * and saves them into the settings object. <br>
	 * Input Processing (valid values)
	 */
	
	public boolean processParameters()
	{
		boolean success = true;
		
		if(gd_p.wasCanceled())
			return false;
		
		// Energy Choice
		
		String energy = gd_p.getNextChoice();
		settings.m_EnergyFunctional=EnergyFunctionalType.valueOf(energy);
		EnergyGUI eg = EnergyGUI.factory(settings, settings.m_EnergyFunctional);
		eg.createDialog();
		eg.processDialog();
		
		// Regularization Choice
		
		String regularization = gd_p.getNextChoice();
		settings.regularizationType = RegularizationType.valueOf(regularization);

		settings.m_CurvatureMaskRadius=(int)gd_p.getNextNumber();
		settings.m_EnergyContourLengthCoeff=(float)gd_p.getNextNumber();
		settings.m_RegionMergingThreshold = (float)gd_p.getNextNumber();
		settings.m_MaxNbIterations = (int)gd_p.getNextNumber();
		
		settings.m_GaussPSEnergyRadius = (int)gd_p.getNextNumber();
		settings.m_BalloonForceCoeff = (float)gd_p.getNextNumber();
		
		
		// Initialization
		String initialization = gd_p.getNextChoice();
		InitializationType type = InitializationType.valueOf(initialization);
		settings.labelImageInitType = type;
		InitializationGUI ig = InitializationGUI.factory(settings, settings.labelImageInitType);
		ig.createDialog();
		ig.processDialog();
		
		// Topological constraints
		
		settings.m_AllowFusion = gd_p.getNextBoolean();
		settings.m_AllowFission = gd_p.getNextBoolean();
		settings.m_AllowHandles = gd_p.getNextBoolean();
		settings.shrinkFirstOnly = gd_p.getNextBoolean();
		
		return success;
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
				
		// Input Files
		
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
		kbest = (int)gd.getNextNumber();
		
		useStack=gd.getNextBoolean();
		keepAllFrames = gd.getNextBoolean();
		show3DResult = gd.getNextBoolean();
		showNormalized = gd.getNextBoolean();
		showStatistics = gd.getNextBoolean();
		
		
		return success;
	}


	@Override
	public InitializationType getLabelImageInitType()
	{
		return settings.labelImageInitType;
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
	public boolean showNormalized()
	{
		return showNormalized;
	}
	
	@Override
	public boolean useStack()
	{
		return useStack;
	}

	@Override
	public boolean showAllFrames()
	{
		return this.keepAllFrames;
	}

	@Override
	public boolean show3DResult()
	{
		return show3DResult;
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
		initializationChoice.select(InitializationType.File.name());
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
						
//						IJ.open(filename);
						
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






