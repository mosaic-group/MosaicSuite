package mosaic.region_competition.netbeansGUI;

import ij.gui.GenericDialog;

import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.TextField;
import java.util.List;


public abstract class GDElement<T>
{
	GenericDialog gd;
	
	String label;
	
	/**
	 * @param gd	GenericDialog
	 * @param list	List to record/read values IN ORDER. 
	 */
	public GDElement(String label, GenericDialog gd, List<GDElement> list)
	{
		this.label=label;
		this.gd = gd;
		list.add(this);
		add();
	}
	
	/**
	 * Adds the GenericDialogElement to the GenericDialog. 
	 * Called in the constructor, 
	 * so the (GenericDialog GUI element) is actually created. 
	 */
	abstract void add();
	

	/**
	 * Sets the value of the corresponding element in GenericDialog
	 * @param value
	 */
	public abstract void setValue(T value);
	
	/**
	 * Returns / records values for macros. 
	 * Calls getNextELEM() method of genericDialog. 
	 * For the macro functionality, the desired value has to be 
	 * <li> saved to the element BEFORE calling this function 
	 * <li> read out by readValue() AFTER this function has been called. 
	 */
	public abstract T recordedValue();
	
	/**
	 * Override this to save the value from macro to your data structure. <br>
	 * Get the value by calling recordedValue();
	 */
	public abstract void readValue();
	
	
	public static abstract class GDEChoice extends GDElement<String>
	{
		Choice choice;

		public GDEChoice(String label, GenericDialog gd, List<GDElement> list)
		{
			super(label, gd, list);
		}
		
		@Override
		void add()
		{
			gd.addChoice(label, null, null);
			
			choice = (Choice)gd.getChoices().lastElement();
		}

		@Override
		public void setValue(String value)
		{
			choice.addItem(value);
			choice.select(value);
		}

		@Override
		public String recordedValue()
		{
			return gd.getNextChoice();
		}
	}
	
	public static abstract class GDENum extends GDElement<Double>
	{
		TextField field;
		
		public GDENum(String label, GenericDialog gd, List<GDElement> list)
		{
			super(label, gd, list);
		}

		@Override
		void add()
		{
			gd.addNumericField(label, 0, 10);
			field = (TextField)gd.getNumericFields().lastElement();
		}
		
		@Override
		public void setValue(Double value)
		{
			field.setText(value.toString());
		}

		@Override
		public Double recordedValue()
		{
			return gd.getNextNumber();
		}
	}
	
	public static abstract class GDECheckbox extends GDElement<Boolean>
	{
		Checkbox field;

		public GDECheckbox(String label, GenericDialog gd, List<GDElement> list)
		{
			super(label, gd, list);
		}

		@Override
		void add()
		{
			int n = 0;
			if(gd.getCheckboxes()!=null)
				n=gd.getCheckboxes().size();
			
			gd.addCheckbox("checkbox_"+n, true);
			field = (Checkbox)gd.getCheckboxes().lastElement();
		}

		@Override
		public void setValue(Boolean value)
		{
			field.setState(value);
		}

		@Override
		public Boolean recordedValue()
		{
			return gd.getNextBoolean();
		}
	}
	
	public static abstract class GDEText extends GDElement<String>
	{
		TextField field;

		public GDEText(String label, GenericDialog gd, List<GDElement> list)
		{
			super(label, gd, list);
		}

		@Override
		void add()
		{
			int n = 0;
			if(gd.getStringFields()!=null)
				n=gd.getStringFields().size();
			gd.addStringField("string_"+n, "empty_string");
			
			field = (TextField)gd.getStringFields().lastElement();
		}

		@Override
		public void setValue(String value)
		{
			field.setText(value);
		}

		@Override
		public String recordedValue()
		{
			return gd.getNextString();
		}
	}
	
}