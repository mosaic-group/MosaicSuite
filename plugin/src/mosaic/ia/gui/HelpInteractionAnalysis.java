package mosaic.ia.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import mosaic.core.GUI.HelpGUI;

public class HelpInteractionAnalysis extends HelpGUI implements ActionListener
{
	public JFrame frame;
	//Initialize Buttons
	private JPanel panel;
	private JButton Close;
	private Font header = new Font(null, Font.BOLD,14);


	public HelpInteractionAnalysis(int x, int y )
	{		
		frame = new JFrame("Interaction Analysis Help");
		frame.setSize(555, 780);
		frame.setLocation(x+500, y-50);
		//frame.toFront();
		//frame.setResizable(false);
		//frame.setAlwaysOnTop(true);

		panel= new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
		panel.setPreferredSize(new Dimension(575, 720));

		JPanel pref= new JPanel(new GridBagLayout());
//		pref.setPreferredSize(new Dimension(555, 550));
//		pref.setSize(pref.getPreferredSize());
		
		setPanel(pref);
		setHelpTitle("Interaction Analysis ");
		createTutorial("http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf");
		createArticle("http://www.biomedcentral.com/content/pdf/1471-2105-11-372.pdf#page=1&zoom=100");
		createSection("Images",null);
		String desc = new String("The images can be selected by clicking on " +
								"Open X and Open Y. Keep in mind that Y should be the reference set of points. ");
		createField("OpenX/Y ",desc,"http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf#page=4&zoom=150,0,-370");
		desc = new String("A region mask can be created to limit the analysis to a specific region. " +
				"Regions with pixel intensity > 0 will be the analyzed");
		createField("Mask Apply/Generate/Load",desc,"http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf#page=4&zoom=150,0,-470");
		desc = new String("Kernel wt(q) and Kernel wt(p) are weights for the kernel density estimator, for q(d) and p(d) " +
							"respectively . It is inversely related to the smoothness of the function");
		createField("Distance distribution",desc,"http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf#page=7&zoom=150,0,-230");
		createSection("Parameter estimation","http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf#page=7&zoom=150,0,-710");
		desc = new String("Select the potential shape");
		createField("Potential",desc,"http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf#page=9&zoom=150,0,-110");
		desc = new String("Repeat the estimation several time");
		createField("Repeat estimation",desc,"http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf#page=8&zoom=150,0,-610");
		createSection("Hypotesis","http://mosaic.mpi-cbg.de/Downloads/IAPManual.pdf#page=11&zoom=150,0,0");
		
		//JPanel panel = new JPanel(new BorderLayout());

		panel.add(pref);
		//panel.add(label, BorderLayout.NORTH);


		frame.add(panel);

		//frame.repaint();

		frame.setVisible(true);
		//frame.requestFocus();
		//frame.setAlwaysOnTop(true);

		//			JOptionPane.showMessageDialog(frame,
		//				    "Eggs are not supposed to be green.\n dsfdsfsd",
		//				    "A plain message",
		//				    JOptionPane.PLAIN_MESSAGE);

	}
	
	public void actionPerformed(ActionEvent ae) 
	{
		Object source = ae.getSource();	// Identify Button that was clicked


		if(source == Close)
		{
			//IJ.log("close called");
			frame.dispose();				
		}


	}
	
}
