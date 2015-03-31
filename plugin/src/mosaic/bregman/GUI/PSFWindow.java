package mosaic.bregman.GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mosaic.bregman.GaussianPSFModel;
import mosaic.bregman.GenericDialogCustom;


public class PSFWindow implements ActionListener,PropertyChangeListener
{
	
	

	public double lem, lex, NA, n, pinhole, pix_xy, pix_z;
	public JDialog frame;
	//Initialize Buttons
	private JPanel panel;
	private JButton Close;
	private Font header = new Font(null, Font.BOLD,14);
	private boolean confocal = true;

	private String [] items={"Confocal Microscope", "Wide Field Fluorescence Microscope"};

	//jcb.setModel(new DefaultComboBoxModel(potentialOptions));

	NumberFormat nf = NumberFormat.getInstance(Locale.US);

	private JFormattedTextField Vlem= new JFormattedTextField(nf);
	private JFormattedTextField Vlex= new JFormattedTextField(nf);
	private JFormattedTextField VNA= new JFormattedTextField(nf);
	private JFormattedTextField Vn= new JFormattedTextField(nf);
	private JFormattedTextField Vpinhole= new JFormattedTextField(nf);
	private JFormattedTextField Vpix_xy= new JFormattedTextField(nf);
	private JFormattedTextField Vpix_z= new JFormattedTextField(nf);

	private  JComboBox<String> micr = new JComboBox<String>(items);
	private JButton estimate = new JButton("Compute PSF");

	private JLabel ref= new JLabel(
			"<html>"
					+"<div align=\"justify\">"
					+ "Gaussian PSF approximation."
					+"<br>"
					+"<br>"
					+"Model from: Gaussian approximations of fluorescence microscope point-spread function models. "
					+"B Zhang, J Zerubia, J C Olivo-Marin. Appl. Optics (46) 1819-29, 2007."
					+"</div>"
					+ "</html>");



	//	"Gaussian PSF model from : 'Gaussian approximations of fluorescence microscope point-spread function models. B Zhang, J Zerubia, J C Olivo-Marin. Appl. Optics (46) 1819-29, 2007.''");
	private JLabel tlem= new JLabel("Emission wavelength (nm)");
	private JLabel tlex= new JLabel("Excitation wavelength (nm)");
	private JLabel tNA = new JLabel("Numerical aperture");
	private JLabel tn = new JLabel("Refraction index");
	private JLabel tpinhole = new JLabel("Pinhole size (Airy units)");
	private JLabel tpix_xy = new JLabel("Lateral pixel size (nm)");
	private JLabel tpix_z = new JLabel("Axial pixel size (nm)");

	private JLabel result = new JLabel("");
	private GenericDialogCustom gd;

	public PSFWindow(int x, int y, GenericDialogCustom gd)
	{
		y=0;
		this.gd=gd;

		lem=520;lex=488;NA=1.3;n=1.46;pinhole=1;
		pix_xy=100;pix_z=400;
		
		Vlem.setValue(lem);Vlex.setValue(lex);
		VNA.setValue(NA);Vn.setValue(n);
		Vpinhole.setValue(pinhole);
		Vpix_xy.setValue(pix_xy);Vpix_z.setValue(pix_z);

		frame = new JDialog();
		frame.setModal(true);
		frame.setSize(300, 500);
		frame.setLocation(x+450, y+150);

		
		panel= new JPanel();
		panel.setPreferredSize(new Dimension(300, 500));
		panel.setSize(panel.getPreferredSize());
		panel.setLayout(null);
		
		JPanel pref= new JPanel(new BorderLayout());
		pref.setPreferredSize(new Dimension(280, 120));
		pref.setSize(pref.getPreferredSize());
		pref.add(ref);

		JPanel pres= new JPanel(new BorderLayout());
		pres.setPreferredSize(new Dimension(280, 80));
		pres.setSize(pres.getPreferredSize());
		pres.add(result);


		//JLabel label = new JLabel();
		//panel.add(label, BorderLayout.NORTH);				
		Vlem.setColumns(4);Vlex.setColumns(4);
		VNA.setColumns(4);Vn.setColumns(4);
		Vpinhole.setColumns(4);Vpix_xy.setColumns(4);
		Vpix_z.setColumns(4);
		
		Vlem.setHorizontalAlignment(SwingConstants.CENTER);Vlex.setHorizontalAlignment(SwingConstants.CENTER);
		VNA.setHorizontalAlignment(SwingConstants.CENTER);Vn.setHorizontalAlignment(SwingConstants.CENTER);
		Vpinhole.setHorizontalAlignment(SwingConstants.CENTER);Vpix_xy.setHorizontalAlignment(SwingConstants.CENTER);
		Vpix_z.setHorizontalAlignment(SwingConstants.CENTER);
		
		Vlem.setLocale(Locale.US);Vlex.setLocale(Locale.US);
		VNA.setLocale(Locale.US);Vn.setLocale(Locale.US);
		Vpinhole.setLocale(Locale.US);Vpix_xy.setLocale(Locale.US);
		Vpix_z.setLocale(Locale.US);

		//ref.setSize(ref.getPreferredSize());
		micr.setSize(micr.getPreferredSize());
		tlem.setSize(tlem.getPreferredSize());Vlem.setSize(Vlem.getPreferredSize());
		tlex.setSize(tlex.getPreferredSize());Vlex.setSize(Vlex.getPreferredSize());
		tNA.setSize(tNA.getPreferredSize());VNA.setSize(VNA.getPreferredSize());
		tn.setSize(tn.getPreferredSize());Vn.setSize(Vn.getPreferredSize());
		tpinhole.setSize(tpinhole.getPreferredSize());Vpinhole.setSize(Vpinhole.getPreferredSize());
		tpix_xy.setSize(tpix_xy.getPreferredSize());Vpix_xy.setSize(Vpix_xy.getPreferredSize());
		tpix_z.setSize(tpix_z.getPreferredSize());Vpix_z.setSize(Vpix_z.getPreferredSize());
		estimate.setSize(estimate.getPreferredSize());


			//			
			//			
			//			micr.setLocation(145,120);

		//rlevel.setLocation(145,120);
		//rlevell.setLocation(20,125);
		pref.setLocation(10,0);
		panel.add(pref);

		micr.setLocation(10,125);			
		tlem.setLocation(20,165);Vlem.setLocation(200,160);
		tlex.setLocation(20,195);Vlex.setLocation(200,190);
		tNA.setLocation(20,225);VNA.setLocation(200,220);
		tn.setLocation(20,255);Vn.setLocation(200,250);
		tpinhole.setLocation(20,285);Vpinhole.setLocation(200,280);
		tpix_xy.setLocation(20,315);Vpix_xy.setLocation(200,310);
		tpix_z.setLocation(20,345);Vpix_z.setLocation(200,340);
		estimate.setLocation(80,375);
		result.setLocation(10,405);

		panel.add(micr);
		panel.add(tlem);panel.add(Vlem);
		panel.add(tlex);panel.add(Vlex);
		panel.add(tNA);panel.add(VNA);
		panel.add(tn);panel.add(Vn);
		panel.add(tpinhole);panel.add(Vpinhole);

		panel.add(tpix_xy);panel.add(Vpix_xy);
		panel.add(tpix_z);panel.add(Vpix_z);
		
		panel.add(estimate);
		pres.setLocation(10,400);
		panel.add(pres);


		frame.add(panel);

		estimate.addActionListener(this);


		Vlem.addPropertyChangeListener(this);
		Vlex.addPropertyChangeListener(this);
		VNA.addPropertyChangeListener(this);
		Vn.addPropertyChangeListener(this);
		Vpinhole.addPropertyChangeListener(this);
		Vpix_xy.addPropertyChangeListener(this);
		Vpix_z.addPropertyChangeListener(this);

		micr.addActionListener(this);
		//

		frame.setVisible(true);
		//frame.requestFocus();
		//frame.setAlwaysOnTop(true);

		//			JOptionPane.showMessageDialog(frame,
		//				    "Eggs are not supposed to be green.\n dsfdsfsd",
		//				    "A plain message",
		//				    JOptionPane.PLAIN_MESSAGE);


	}

	public void propertyChange(PropertyChangeEvent e) 
	{
		Object source = e.getSource();
		if (source == Vlem) 
		{lem=(int) ((Number)Vlem.getValue()).doubleValue();}
		else if (source == Vlex) 
		{lex=((Number)Vlex.getValue()).doubleValue();}
		else if (source == VNA) 
		{NA=((Number)VNA.getValue()).doubleValue();}
		else if (source == Vn)
		{n=((Number)Vn.getValue()).doubleValue();}
		else if (source == Vpinhole) 
		{pinhole=((Number)Vpinhole.getValue()).doubleValue();}
		else if (source == Vpix_xy) 
		{pix_xy=((Number)Vpix_xy.getValue()).doubleValue();}
		else if (source == Vpix_z) 
		{pix_z=((Number)Vpix_z.getValue()).doubleValue();}
	}

	public void actionPerformed(ActionEvent ae)
	{
		Object source = ae.getSource();	// Identify Button that was clicked

		if(source == estimate)
		{

			GaussianPSFModel psf= new GaussianPSFModel(lem,lex,NA,pinhole,n);

			double sz,sx;
			if(confocal)
			{
				sz=1000*psf.axial_LSCM();
				sx=1000*psf.lateral_LSCM();
			}
			else
			{
				sz=1000*psf.axial_WFFM();
				sx=1000*psf.lateral_WFFM();
			}

			TextField tx =gd.getField(3);//field x
			TextField tz =gd.getField(4);//filed z

			tx.setText(String.format(Locale.US,"%.2f", sx/pix_xy));
			tz.setText(String.format(Locale.US,"%.2f", sz/pix_z));

			result.setText
			(	"<html>"
					+"<div align=\"justify\">"
					+ "Gaussian PSF stddev:"
					+"<br>"
					+"Lateral : "+String.format(Locale.US,"%.2f", sx) +" nm "
					+"Axial : "+String.format(Locale.US,"%.2f", sz) +" nm"
					+"<br>"
					+"("+String.format(Locale.US,"%.3f", sx/pix_xy)+", "+ String.format(Locale.US,"%.3f", sz/pix_z)+" in pixels)"
					+"</div>"
					+ "</html>");
		}

		if(source==micr)
		{
			JComboBox cb = (JComboBox)source;
			String selected = (String)cb.getSelectedItem();
			//System.out.println("Selected: "+selected);
			if(selected==items[1])
			{
				//widefield
				confocal=false;

			}
			if(selected==items[0])
			{
					//confocal
					confocal=true;
			}
		}	
	}
}

