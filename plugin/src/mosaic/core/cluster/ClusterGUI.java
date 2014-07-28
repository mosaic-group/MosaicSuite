package mosaic.core.cluster;

import ij.IJ;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/*
 * 
 * This is the GUI to choose the cluster profile
 * 
 */

public class ClusterGUI  extends JDialog
{
	private JPanel contentPane;
	private ClusterProfile cp_sel;
	private ClusterSession cl;
	
	private JTextField tx_u;
	private JPasswordField tx_p;
	
	public ClusterGUI()
	{
//			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 350, 150);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(0, 2, 0, 0));
			
		JLabel lblNewLabel = new JLabel("Cluster profile: ");
		contentPane.add(lblNewLabel);
		
		// Check for file profile
		
		String dir = IJ.getDirectory("home");
		dir += File.separator + ".MosaicToolsuite" + File.separator + "clusterProfile";
		File cpf[] = new File(dir).listFiles();
		ClusterProfile[] cp = new ClusterProfile[cpf.length + 1];
		
		int cnt = 0;
		for (File tcpf : cpf)
		{
			cp[cnt] = new FileClusterProfile(tcpf);
			cnt++;
		}
		
		// Set coded profile
		
		cp[cp.length-1] = new MadMaxProfile();
		cp_sel = cp[cp.length-1];
		
		//
		
		JComboBox comboBox_1 = new JComboBox(cp);
		comboBox_1.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				cp_sel = (ClusterProfile)((JComboBox)arg0.getSource()).getSelectedItem();
			}
		});
		
		contentPane.add(comboBox_1);
		
		JLabel lblNewLabel_2 = new JLabel("Username");
		contentPane.add(lblNewLabel_2);
		
		tx_u = new JTextField();
		contentPane.add(tx_u);
		
		lblNewLabel_2 = new JLabel("Password");
		contentPane.add(lblNewLabel_2);
		
		tx_p = new JPasswordField();
		contentPane.add(tx_p);
		
			
		// OK and Cancel button
			
		JButton btnOKButton = new JButton("OK");
		contentPane.add(btnOKButton);
		btnOKButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				cp_sel.setPassword(new String(tx_p.getPassword()));
				cp_sel.setUsername(new String(tx_u.getText()));
				cl = new ClusterSession(cp_sel);
				dispose();
			}
		});

		JButton btnCancelButton = new JButton("Cancel");
		contentPane.add(btnCancelButton);
		
		setModal(true);
		setVisible(true);
		
	}
	
	public ClusterSession getClusterSession()
	{
		return cl;
	}
}
