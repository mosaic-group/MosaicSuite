package mosaic.core.cluster;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
		
		// Set coded profile
		
		ClusterProfile[] cp = new ClusterProfile[1];
		cp[0] = new MadMaxProfile();
		cp_sel = cp[0];
		
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
		
		JTextField tx_u = new JTextField();
		contentPane.add(tx_u);
		
		lblNewLabel_2 = new JLabel("Password");
		contentPane.add(lblNewLabel_2);
		
		JPasswordField tx_p = new JPasswordField();
		contentPane.add(tx_p);
		
			
		// OK and Cancel button
			
		JButton btnOKButton = new JButton("OK");
		contentPane.add(btnOKButton);
		btnOKButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				cl = new ClusterSession(cp_sel);
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
