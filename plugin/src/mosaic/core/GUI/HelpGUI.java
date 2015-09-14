package mosaic.core.GUI;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * Class to create help windows + link to respective documentation
 * 
 * @author Pietro Incardona
 *
 */

public class HelpGUI
{
	JPanel pref;
	
	int gridx = 0;
	int gridy = 0;
	
	public HelpGUI()
	{
	}
	
	protected void setPanel(JPanel pref_)
	{
		pref = pref_;
	}
	
	protected void setHelpTitle(String title)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = gridx;
		c.gridy = gridy;
		c.gridwidth = 2;
		JLabel label = new JLabel();
		label.setText("<html>"
				+ "<h1> " + title + " </h1>"
				+ "</html>");
		pref.add(label,c);
		
		gridy++;
	}
	
	protected void createArticle(final String link)
	{
		if (link == null)
			return;
			
		GridBagConstraints c = new GridBagConstraints();
		
		JLabel label = new JLabel();
		JButton bt_a = new JButton("<html><font color=\"blue\">click here</font></hmtl>");
		bt_a.setBorderPainted(false);
		bt_a.setOpaque(false);
		bt_a.setBackground(Color.GRAY);
		bt_a.setToolTipText(link);
		bt_a.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				
				try {
					open(new URI(link));
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				
			}});
		
		label.setText("<html>Article: </html>");
		c.gridx = 0;
		c.gridy = gridy;
		c.anchor = GridBagConstraints.CENTER ;
		pref.add(label,c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		pref.add(bt_a,c);
		
		gridy++;
	}
	
	protected void createTutorial(final String link)
	{
		if (link == null)
			return;
			
		GridBagConstraints c = new GridBagConstraints();
		
		JLabel label = new JLabel();
		JButton bt_t = new JButton("<html><font color=\"blue\">click here</font></hmtl>");
		bt_t.setBorderPainted(false);
		bt_t.setOpaque(false);
		bt_t.setBackground(Color.GRAY);
		bt_t.setToolTipText(link);
		bt_t.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				
				try {
					open(new URI(link));
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				
			}
		});
		pref.add(label,c);
		
		label.setText("<html>Tutorial: </html>");
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 1;
		pref.add(label,c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		pref.add(bt_t,c);
		
		gridy++;
	}
	
	protected void createSection(String sc, final String link)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		
		if (link == null)
		{
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			JLabel label = new JLabel();
			label.setText("<html><h1> " + sc + " </h1>");
			pref.add(label,c);
			
			gridy++;
		}
		else
		{
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 1;
			JLabel label = new JLabel();
			label.setText("<html><h1> " + sc + " </h1>");
			pref.add(label,c);
			
			JButton bt_s = new JButton("<html><font color=\"blue\">more info</font></hmtl>");
			bt_s.setBorderPainted(false);
			bt_s.setOpaque(false);
			bt_s.setBackground(Color.GRAY);
			bt_s.setToolTipText(link);
			bt_s.addActionListener(new ActionListener() 
			{
				@Override
				public void actionPerformed(ActionEvent arg0) 
				{
					
					try {
						open(new URI(link));
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
					
				}
			});
			c.gridx = 1;
			pref.add(bt_s,c);
			
			gridy++;
		}
	}
	
	protected void createField(String fld, String desc, final String link)
	{
		GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 2;
		JLabel label = new JLabel();
		label.setText("<html>"
				+ "<h2> <font color=\"red\"> " + fld + " </font></h2>"
				+ "<div style=\"width:400px\">" + desc + "</div>");
		pref.add(label,c);
		
		gridy++;
		
		if (link != null)
		{
			JButton bt_pr = new JButton("<html><font color=\"blue\">more info</font></hmtl>");
			bt_pr.setBorderPainted(false);
			bt_pr.setOpaque(false);
			bt_pr.setBackground(Color.GRAY);
			bt_pr.setToolTipText(link);
			bt_pr.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent arg0) 
					{
						
						try {
							open(new URI(link));
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
						
					}});
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			pref.add(bt_pr,c);
		}
			
		gridy++;
	}
	
	private static void open(URI uri) 
	{
		if (Desktop.isDesktopSupported()) 
		{
			try 
			{
				Desktop.getDesktop().browse(uri);
		    }
			catch (IOException e)
			{ /* TODO: error handling */ }
		}
		else 
		{ /* TODO: error handling */ }
	}
}
