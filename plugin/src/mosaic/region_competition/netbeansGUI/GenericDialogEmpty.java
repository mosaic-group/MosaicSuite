package mosaic.region_competition.netbeansGUI;

import ij.gui.GenericDialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;


public class GenericDialogEmpty extends GenericDialog
{
	List<GDElement> list;
	GenericDialog gd;
	
	public GenericDialogEmpty(String title)
	{
		super(title);
		gd = this;
		list = new LinkedList<GDElement>();
		
		
		// here do all the gd stuff.
		// later on, transfer data from new gui to these fields
		// and read it out by getNext...() for macro
//		gd.addNumericField("numfield1", 12, 5);
		
		
		
		
		
		// add the elements to the gd
		for(GDElement e : list)
		{
			e.add();
		}
		
		// build my own GUI
		
		JButton b = new JButton("ok");
		b.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				e.setSource(gd.getButtons()[0]);
				gd.actionPerformed(e);
			}
		});
		super.add(b);
		this.pack();
	}
	
	void recordValues()
	{
		// record the values for macro
		for(GDElement e : list)
		{
			e.recordedValue();
		}
	}
	
	/** 
	 * overrides adding of components in GenericDialog, so getting an empty dialog
	 */
	@Override
	public Component add(Component comp)
	{
		return comp;
	}
	
	/**
	 * 	the original add method 
	 */
	public Component addNew(Component comp)
	{
		return super.add(comp);
	}
	
	
	
}


