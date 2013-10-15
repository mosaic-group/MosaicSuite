package mosaic.core.GUI;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;


public class ProgressBarWin extends JDialog
{
	private static final long serialVersionUID = 147834134785813L;
	JPanel contentPane;
	JProgressBar Prog_s;
	JLabel Status;
	private Object lock;
	
	public void SetStatusMessage(String Message)
	{
		Status.setText(Message);
	}
	
	public void SetProgress(int p)
	{
		Prog_s.setString(((Integer)p).toString() + " %");
		Prog_s.setValue(p);
	}
	
	double GetProgress()
	{
		return Prog_s.getValue();
		
	}
	
	
	public ProgressBarWin()
	{
		
		lock = new Object();
		
		setTitle("Processing...");
		setBounds(100, 100, 400, 100);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new GridBagLayout());
		
		// Status message
		JPanel contentMessage = new JPanel();
		contentMessage.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentMessage.setLayout(new GridLayout(0, 2, 0, 0));
		JLabel lblNewLabel = new JLabel("Status: ");
		contentMessage.add(lblNewLabel);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		contentPane.add(contentMessage,c);
		
		lblNewLabel = new JLabel("Warming up");
		Status = lblNewLabel;
		Status.setSize(new Dimension(50,300));
		contentMessage.add(lblNewLabel);
		
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		JProgressBar pbar = new JProgressBar();
		pbar.setStringPainted(true);
		Prog_s = pbar;
		contentPane.add(pbar,c);
		
	}
}
