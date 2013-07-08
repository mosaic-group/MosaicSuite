package mosaic.region_competition.wizard;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Settings;
import mosaic.region_competition.wizard.RCWWin.segType;


class RCProgressWin extends JFrame implements MouseListener
{
	JLabel score_l[];
	
	class ImageList
	{
		ImagePlus img_p[];
		ImageCanvas img_c[];
		double score;
		Settings s;
		ImageList()
		{
			score = Double.MAX_VALUE;
		}
	}
	
	enum StatusSel
	{
		RUNNING,
		STOP,
	};
	
	StatusSel sStat = StatusSel.RUNNING;
	int selection = -1;
	int nbest;
	int nImg;
	ImageList img[];
	JPanel contentPane;
	JPanel contentImage;
	JProgressBar Prog_s;
	JLabel Status;
	private Object lock;
	
	public void SetStatusMessage(String Message)
	{
		Status.setText(Message);
	}
	
	void SetProgress(int p)
	{
		Prog_s.setString(((Integer)p).toString() + " %");
		Prog_s.setValue(p);
	}
	
	double GetProgress()
	{
		return Prog_s.getValue();
		
	}
	
	private void RemoveComponent(int i,int j)
	{
		if (img[i].img_p[j] != null)
			contentImage.remove(img[i].img_c[j]);
	}
	
	private void AddComponent(int i,int j,Component c)
	{
		if (c == null)
			return;
		
		GridBagLayout layout = (GridBagLayout) contentImage.getLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = i;
		gbc.gridy = j;
		gbc.ipadx = 5;
		gbc.ipady = 5;
		contentImage.add(c,gbc);
	}
	
	void SetImage(double score, Settings st,ImagePlus new_img[])
	{
		int i = 0;
		
		for (ImageList l : img)
		{
			if (l.score > score)
			{break;}
			i++;
		}
		
		if (i >= img.length)
			return;
		
		// Remove all component
		
		for (int s = 0 ; s < img.length ; s++)
		{
			if (img[s] != null)
			{
				for (int j = 0 ; j < img[s].img_p.length ; j++)
				{
					RemoveComponent(s,j);
				}
			}
		}
		
		// scale all image back
		
		for (int s = i ; s < img.length-1 ; s++)
		{
			img[i+1] = img[i];
		}
		
		// Add new Image
		
		img[i] = new ImageList();

		img[i].img_c = new ImageCanvas[new_img.length];
		img[i].img_p = new_img;
		
		int y = 140;
		for (int j = 0 ; j < new_img.length ; j++)
		{
			y += img[i].img_p[j].getHeight() + 10;
			img[i].score = score;
			img[i].s = st;
			img[i].img_c[j] = new ImageCanvas(img[i].img_p[j]);
			AddComponent(i,j,img[i].img_c[j]);
			
			// Add listener
			
			img[i].img_c[j].addMouseListener(this);
		}
		
		// Add all components
		
		for (int s = 0 ; s < img.length ; s++)
		{
			for (int j = 0 ; j < img[s].img_p.length ; j++)
			{
				if (img[s].img_p[j] != null)
				{
					AddComponent(s,j,img[s].img_c[j]);
				}
			}
			
			if (img[s].score != Double.MAX_VALUE)
			{
				DecimalFormat df = new DecimalFormat("#.###");
				score_l[s].setText(df.format(img[s].score));
			}	
		}
		
		// redraw
		
		// calculate window width
		
		int x = 0;
		
		int mx = 0;
		for (int j = 0 ; j < img[i].img_p.length ; j++)
		{
			if (img[i].img_p[j].getWidth() > mx)
				mx = img[i].img_p[j].getWidth();
		}
		x = (mx+10) * nbest;
		if (x < 200)
			x = 200;
		
		setSize(x, y);
		Prog_s.setSize(new Dimension(x,50));
		Prog_s.revalidate();
		contentImage.revalidate();
		contentPane.revalidate();
		repaint();
	}
	
	Settings getSelection()
	{
		return img[selection].s;
	}
	
	void waitClose()
	{
		synchronized(lock)
		{
			try {
				lock.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	StatusSel getSelectionStatus()
	{
		return sStat;
	}
	
	void SetImage(double score, Settings s, String FileName[])
	{
		Opener o = new Opener();
		ImagePlus img_p[] = new ImagePlus[nImg];
		LabelImage img_l[] = new LabelImage[nImg];
		for (int j = 0 ; j < FileName.length ; j++)
		{
			int l[];
			img_p[j] = o.openImage(FileName[j]);
			if (img_p[j].getDimensions()[2] != 1)
			{
				l = new int [3];
				l[0] = img_p[j].getDimensions()[0];
				l[1] = img_p[j].getDimensions()[1];
				l[2] = img_p[j].getDimensions()[2];
			}
			else
			{
				l = new int [2];
				l[0] = img_p[j].getDimensions()[0];
				l[1] = img_p[j].getDimensions()[1];
			}
			img_l[j] = new LabelImage(l);
			img_l[j].initWithIP(img_p[j]);
			img_p[j] = img_l[j].convert("image", 255);
		}
		
		SetImage(score, s, img_p);
	}
	
	RCProgressWin(int nbest_, int nImg_)
	{
		nbest = nbest_;
		nImg = nImg_;
		
		lock = new Object();
		img = new ImageList [nbest];
		for (int i = 0 ; i < img.length ; i++)
		{
			img[i] = new ImageList();
			img[i].img_p = new ImagePlus[nImg];
		}
		
		setTitle("Processing...");
		setBounds(100, 100, 200, 140);
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
		contentMessage.add(lblNewLabel);
		
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		JProgressBar pbar = new JProgressBar();
		pbar.setStringPainted(true);
		Prog_s = pbar;
		contentPane.add(pbar,c);
		
		// Image area
		
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		contentImage = new JPanel();
		contentImage.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentImage.setLayout(new GridBagLayout());
		contentPane.add(contentImage,c);
		
		// score Label
		
		score_l = new JLabel[nbest];
		
		for (int i = 0 ; i < nbest ; i++)
		{
			score_l[i] = new JLabel("1.0");
			c.gridx = i;
			c.gridy = nImg;
			contentImage.add(score_l[i],c);
		}
		
		// Stop button
		
		c.gridx = 0;
		c.gridy = 3;
		c.anchor = GridBagConstraints.CENTER;
		JButton btnOKButton = new JButton("Stop");
		contentPane.add(btnOKButton,c);
		btnOKButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				synchronized(lock)
				{
					if (sStat == StatusSel.RUNNING)
						sStat = StatusSel.STOP;
					else
					{
						dispose();
						lock.notify();
					}
				}
			}
		});
	}

	@Override
	public void mouseClicked(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
		ImageCanvas c = (ImageCanvas) arg0.getComponent();
		
		int x = arg0.getX();
		int y = arg0.getY();
		int offscreenX = c.offScreenX(x);
		int offscreenY = c.offScreenY(y);
		
		GridBagConstraints bgc = ((GridBagLayout)contentImage.getLayout()).getConstraints(c);
		int lab = bgc.gridx;
		
		for (int i = 0 ; i < score_l.length ; i++)
		{
			if (i == lab)
			{
				score_l[lab].setOpaque(true);
				score_l[lab].setBackground(Color.red);
			}
			else
			{
				score_l[i].setBackground(Color.gray);
				score_l[i].setOpaque(false);
			}
		}
		
		selection = lab;
	}

	@Override
	public void mouseEntered(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) 
	{
		// TODO Auto-generated method stub
		
		
	}
}
