package mosaic.region_competition.wizard;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import mosaic.region_competition.wizard.RCWWin.segType;


class RCProgressWin extends JFrame
{
	class ImageList
	{
		ImagePlus img_p[];
		double score;
	}
	
	int nbest;
	int nImg;
	double Progress;
	ImageList img[];
	JPanel contentPane;
	JPanel contentImage;
	
	void SetProgressMessage(String Message)
	{
		
	}
	
	void SetProgress(double p)
	{
		
	}
	
	double GetProgress()
	{
		return 0;
		
	}
	
	void RemoveComponent(int i,int j)
	{
		Component cr = contentImage.getComponent(i*nImg+j);
		contentImage.remove(cr);
	}
	
	void AddComponent(int i,int j,Component c)
	{
		GridBagLayout layout = (GridBagLayout) contentImage.getLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = i;
		gbc.gridy = j;
		contentImage.add(c,gbc);
	}
	
	void SetImage(double score, String FileName[])
	{
		int i = 0;
		Opener o = new Opener();
		
		for (ImageList l : img)
		{
			if (l.score > score)
			{break;}
			i++;
		}
		
		// Remove previous image
		
		for (int s = i ; s < img.length ; s++)
		{
			for (int j = 0 ; j < FileName.length ; j++)
			{
/*				Component cr = contentImage.getComponent(i*FileName.length+j);
				GridBagLayout layout = contentImage.getLayout();
				GridBagConstraints gbc = layout.getConstraints(c);
				contentImage.remove(cr);
				
				layout.setConstraints(comp, constraints)
				contentImage.remove(img[i].img_p[j].getCanvas());*/
				
				RemoveComponent(s,j);
			}
		}
		
		// scale all image back
		
		for (int s = i ; s < img.length-1 ; s++)
		{
			img[i] = img[i+1];
		}
		
		// Add new Image
		

		for (int j = 0 ; j < FileName.length ; j++)
		{
			img[i].img_p[j] = o.openImage(FileName[j]);
			AddComponent(i,j,img[i].img_p[j].getCanvas());
		}
		
		// Add other images
		
		for (int s = i+1 ; s < img.length ; s++)
		{
			for (int j = 0 ; j < FileName.length ; j++)
			{
				AddComponent(img[i].img_p[j].getCanvas());
			}
		}
		
		// redraw
		
		contentImage.revalidate();
		contentImage.repaint();
	}
	
	RCProgressWin(int nbest, int nImg)
	{
		img = new ImageList [nbest];
		
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(0, 1, 0, 0));
		
		// Status message
		JPanel contentMessage = new JPanel();
		contentMessage.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentMessage.setLayout(new GridLayout(0, 2, 0, 0));
		JLabel lblNewLabel = new JLabel("Status: ");
		contentMessage.add(lblNewLabel);
		
		lblNewLabel = new JLabel("Warming up");
		contentMessage.add(lblNewLabel);
		
		JProgressBar pbar = new JProgressBar();
		contentPane.add(pbar);
		
		// Image area
		
		contentImage = new JPanel();
		contentImage.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentImage.setLayout(new GridBagLayout());
		contentPane.add(contentImage);
		
		// Stop button
		
		JButton btnOKButton = new JButton("Stop");
		contentPane.add(btnOKButton);
		btnOKButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				
			}
		});
	}
}
