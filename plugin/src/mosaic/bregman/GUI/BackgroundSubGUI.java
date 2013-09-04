package mosaic.bregman.GUI;


import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import mosaic.bregman.Analysis;
import mosaic.bregman.GenericDialogCustom;
import mosaic.core.GUI.HelpGUI;
import mosaic.core.GUI.ParticleTrackerHelp;
import ij.ImagePlus;
import ij.gui.GenericDialog;


public class BackgroundSubGUI 
{
	
	class BackgroundSubHelp extends HelpGUI implements ActionListener
	{
		public JDialog frame;
		//Initialize Buttons
		private JPanel panel;
		private JButton Close;
		private Font header = new Font(null, Font.BOLD,14);


		public BackgroundSubHelp(int x, int y )
		{		
			frame = new JDialog();
			frame.setTitle("Background Sub Help");
			frame.setSize(500, 220);
			frame.setLocation(x+500, y-50);
			frame.setModal(true);
			//frame.toFront();
			//frame.setResizable(false);
			//frame.setAlwaysOnTop(true);

			panel= new JPanel(new FlowLayout(FlowLayout.LEFT,10,5));
			panel.setPreferredSize(new Dimension(500, 220));

			JPanel pref= new JPanel(new GridBagLayout());
//			pref.setPreferredSize(new Dimension(555, 550));
//			pref.setSize(pref.getPreferredSize());
			
			setPanel(pref);
			setHelpTitle("Background Subtraction");
			String desc = new String("Reduce background fluorescence using the rolling ball algorithm " +
									"by selecting “Remove Background“ and entering the window edge-length " +
									"in units of pixels. This length should be large enough so that" + 
									"a square with that edge length cannot fit inside the objects to be detected," +
									" but is smaller than the length scale of background variations");
			createField("Background subtraction window size",desc,null);

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
	
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;

	public BackgroundSubGUI()
	{
	}


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);
		
		final GenericDialog  gd = new GenericDialog("Background subtractor options");
		
		gd.setInsets(-10,0,3);
		gd.addMessage("Background subtractor",bf);
		
		Panel p = new Panel();
		Button help_b = new Button("help");
		
		p.add(help_b);
		
		help_b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				Point p =gd.getLocationOnScreen();
				
				BackgroundSubHelp pth = new BackgroundSubHelp(p.x,p.y);
				
			}});
		
		gd.addPanel(p);
		
		gd.addCheckbox("Remove background", Analysis.p.removebackground);
		
		gd.addNumericField("rolling ball window size (in pixels)", Analysis.p.size_rollingball,0);
		
		gd.showDialog();
		if (gd.wasCanceled()) return;

		//general options	
		Analysis.p.removebackground=gd.getNextBoolean();
		//IJ.log("rem back:" +  Analysis.p.removebackground);
		Analysis.p.size_rollingball=(int) gd.getNextNumber();
		//Analysis.p.usePSF=gd.getNextBoolean();
		
	}

}
