package mosaic.core.cluster;

import ij.IJ;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

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

import mosaic.core.utils.ShellCommand;

/**
 * 
 * This is the GUI to choose the cluster profile
 * and to feed username and password to access the cluster
 * 
 * usage:
 * 
 * ClusterGUI cg = new ClusterGUI()
 * 
 * It also create a ClusterSession that you can get with getClusterSession
 * 
 * 
 * @author : Pietro Incardona
 * 
 */

public class ClusterGUI  extends JDialog
{
	private JPanel contentPane;
	private ClusterProfile cp_sel;
	private ClusterSession cl;
	
	private JTextField tx_u;
	private JPasswordField tx_p;
	private ClusterProfile[] cp = null;
	
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
		dir += File.separator + ".MosaicToolSuite" + File.separator + "clusterProfile";
		File cpf[] = new File(dir).listFiles();
		if (cpf != null)
		{
			cp = new ClusterProfile[cpf.length + 1];
			
			int cnt = 0;
			for (File tcpf : cpf)
			{
				cp[cnt] = new FileClusterProfile(tcpf);
				cnt++;
			}
		}
		else
			cp = new ClusterProfile[1];
		
		// Set coded profile
		
		cp[cp.length-1] = new MadMaxProfile();
		cp_sel = cp[cp.length-1];
		
		// Create a set of strings
		
		String CBcp[] = new String[cp.length];
		for (int i = 0 ; i < cp.length ; i++)
		{
			CBcp[i] = new String(cp[i].getProfileName());
		}
				
		//
		
		JComboBox comboBox_1 = new JComboBox(CBcp);
		comboBox_1.setSelectedIndex(cp.length - 1);
		comboBox_1.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				
				cp_sel = cp[((JComboBox)arg0.getSource()).getSelectedIndex()];
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
		btnCancelButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				cl = null;
				dispose();
			}
		});
		
		contentPane.add(btnCancelButton);
		
		setModal(true);
		setVisible(true);
		
	}
	
	/**
	 * 
	 * Return the created ClusterSession
	 * 
	 * @return the created cluster session
	 */
	
	public ClusterSession getClusterSession()
	{
		return cl;
	}
	
	/**
	 * 
	 * Get a list of all cluster profiles
	 * 
	 * @return A list of all cluster profiles
	 */
	
	static public ClusterProfile[] getClusterProfiles()
	{
		return getClusterProfiles(0);
	}
	
	/**
	 * 
	 * Get a list of all cluster profiles
	 * 
	 * @param s allocate at the end s free slot
	 * 
	 * @return A list of all cluster profiles
	 */
	
	static public ClusterProfile[] getClusterProfiles(int s)
	{
		String dir = getClusterProfileDir();
		File cpf[] = new File(dir).listFiles();
		if (cpf == null)
		{
			return new ClusterProfile[s];
		}
		
		ClusterProfile[] cp = new ClusterProfile[cpf.length + s];
		
		int cnt = 0;
		for (File tcpf : cpf)
		{
			cp[cnt] = new FileClusterProfile(tcpf);
			cnt++;
		}
		
		return cp;
	}
	
	/**
	 * 
	 * Create the Cluster profile directory if does not exist
	 * 
	 */
	
	static public void createClusterProfileDir()
	{
		if (new File(getClusterProfileDir()).exists() == true)
			return;
		
		String dir = IJ.getDirectory("home") + File.separator + ".MosaicToolSuite";
		try {
			ShellCommand.exeCmd("mkdir " + dir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		dir += File.separator + "clusterProfile";
		
		try {
			ShellCommand.exeCmd("mkdir " + dir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static public String getClusterProfileDir()
	{
		String dir = IJ.getDirectory("home");
		dir += File.separator + ".MosaicToolSuite" + File.separator + "clusterProfile";
		return dir;
	}
}
