package mosaic.bregman;

import java.awt.Checkbox;
import java.awt.TextField;
//import java.awt.event.ActionListener;
//import java.awt.event.AdjustmentListener;
//import java.awt.event.FocusListener;
//import java.awt.event.ItemListener;
//import java.awt.event.KeyListener;
//import java.awt.event.TextListener;
//import java.awt.event.WindowListener;

import ij.gui.GenericDialog;
//import ij.gui.NonBlockingGenericDialog;

public class GenericDialogCustom extends GenericDialog {

	public GenericDialogCustom(String title) 
	{
		super(title);
	}


	public TextField getField(int n)
	{
		return (TextField)numberField.elementAt(n);
	}

	public Checkbox getBox(int n)
	{
		return (Checkbox)checkbox.elementAt(n);
	}
	
	


}
