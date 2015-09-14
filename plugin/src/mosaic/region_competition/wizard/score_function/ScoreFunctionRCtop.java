package mosaic.region_competition.wizard.score_function;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.io.Opener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import mosaic.plugins.Region_Competition;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.PointCM;
import mosaic.region_competition.Settings;
import mosaic.region_competition.wizard.PickRegion;
	
public class ScoreFunctionRCtop implements ScoreFunction
{	
	String[] file;
	
	IntensityImage i[];
	LabelImageRC l[];
	Algorithm al;
	Settings s;
	
	private PointCM pntMod[][];
	
	public Settings createSettings(Settings s, double pop[])
	{
		Settings st = new Settings(s);
	
		st.m_RegionMergingThreshold = (float) pop[0];
//		st.m_RegionMergingCoeff = (int) pop[0];
		
		return st;
	}
		
	public ScoreFunctionRCtop(IntensityImage i_[], LabelImageRC l_[], Settings s_)
	{
		i = i_;
		l = l_;
		
		s = s_;
		file = new String[l.length];
		pntMod = new PointCM[l.length][];
	}

	public LabelImageRC getLabel(int im)
	{
		return l[im];
	}
	
	
	public void setCMModel(PointCM [] mod,int i)
	{
		pntMod[i] = mod;
	}
	
	
	public double Topo(LabelImageRC l, PointCM pntMod[], double pop[])
	{
		int off[] = l.getDimensions().clone();
		Arrays.fill(off, 0);
		new RegionIterator(l.getDimensions(),l.getDimensions(),off);
		
		PointCM Reg[] = l.createCMModel();
		
		Vector<PointCM> pntModV = new Vector<PointCM>(Arrays.asList(pntMod));
		Vector<PointCM> RegV = new Vector<PointCM>(Arrays.asList(Reg));
		
		// Reorder
		
		double Min = Double.MAX_VALUE;
		PointCM pMin1 = null;
		PointCM pMin2 = null;
		PointCM reoMod2[] = new PointCM[Reg.length];
		PointCM reoMod1[] = new PointCM[pntMod.length];
		int k = 0;
		
		while (pntModV.size() != 0 && RegV.size() != 0)
		{
			Min = Double.MAX_VALUE;
			for (PointCM pCM : pntModV)
			{
				for (PointCM pReg : RegV)
				{
					double d = pReg.p.distance(pCM.p);
					if (d < Min)
					{
						Min = d;
						pMin1 = pCM;
						pMin2 = pReg;
					}
				}
			}
			
			reoMod1[k] = pMin1;
			reoMod2[k] = pMin2;
			k++;
			
			pntModV.remove(pMin1);
			RegV.remove(pMin2);
		}
			
		// Calculate

		double ret = 0.0;
		
		for (int i = 0 ; i < reoMod1.length && i < reoMod2.length ; i++)
		{
			ret += reoMod1[i].p.distance(reoMod2[i].p);
		}
		
		double expo = 1.0 / l.getDim();
		ret += Math.abs(reoMod1.length - reoMod2.length)*Math.pow(l.getSize()/pntMod.length,expo);
		
		if (reoMod1.length - reoMod2.length > 0)
			ret += Math.abs(reoMod1.length - reoMod2.length) * 1000.0 * pop[0];
		else
			ret += Math.abs(reoMod1.length - reoMod2.length) * 500 * (1.0 - pop[0]);
		
		return ret;
	}
		
	@Override
	public double valueOf(double[] x) 
	{	
		double result = 0.0;
			
		s.m_RegionMergingThreshold = (float) x[0];
		
		// write the settings
		Region_Competition.getConfigHandler().SaveToFile(IJ.getDirectory("temp")+"RC_top"+x[0], s);
			
		for (int im = 0 ; im < i.length ; im++)
		{
			IJ.run(i[im].imageIP,"Region Competition","config="+IJ.getDirectory("temp")+"RC_top"+x[0] + "  " + "output=" +IJ.getDirectory("temp")+"RC_top"+x[0]+".tif" + " normalize=false");
			
			// Read Label Image
		
			Opener o = new Opener();
			file[im] = new String(IJ.getDirectory("temp")+"RC_top"+x[0]+".tif");
			ImagePlus ip = o.openImage(file[im]);

			l[im].initWithIP(ip);
			l[im].createStatistics(i[im]);
				
			// Scoring
		 		
			result = Topo(l[im],pntMod[im],x);
				
//			result += 10.0/Math.abs(a1 - a2);
		}
		
		return result;
	}

	@Override
	public boolean isFeasible(double[] x) 
	{
		int minSz = Integer.MAX_VALUE;
		for (LabelImageRC lbt : l)
		{
			for (int d : lbt.getDimensions())
			{
				if (d < minSz)
				{
					minSz = d;
				}
			}
		}
			
		if (x[0] >= 1.0 || x[0] <= 0.0)
			return false;
			
		return true;
	}

	@Override
	public void incrementStep() 
	{
		
	}

	@Override
	public void show() 
	{
	
		for (int im = 0 ;  im < l.length ; im++)
			l[im].show("init", 255);
		
	}

	@Override
	public double[] getAMean(Settings s) 
	{
		double [] aMean = new double [1];
		
		aMean[0] = s.m_RegionMergingThreshold;
		
		return aMean;
	}
		
	@Override
	public int getNImg() 
	{
		return l.length;
	}

	@Override
	public TypeImage getTypeImage() {
		return TypeImage.FILENAME;
	}

	@Override
	public ImagePlus[] getImagesIP() {
		return null;
	}

	@Override
	public String[] getImagesString() {
		return file;
	}
	
	
	private class DivideBtn implements ActionListener
	{

		LabelImageRC ip[];
		ImagePlus ipp[];
		
		DivideBtn(LabelImageRC ip_[], ImagePlus ipp_[])
		{
			ip = ip_;
			ipp = ipp_;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) 
		{
			
			for (int i = 0 ; i < ipp.length ; i++)
			{
				Roi r = ipp[i].getRoi();
				
				Line lr = (Line)r;

				int xp = 0;
				int yp = 0;
				
				int x1 = lr.x1;
				int x2 = lr.x2;
				
				int y1 = lr.y1;
				int y2 = lr.y2;
				
				// Draw the line 2D
				
				if (x1 >= x2)
					xp = -1;
				else
					xp = 1;
				
				if (y1 >= y2)
					yp = -1;
				else
					yp = 1;
				
				int deltax = x2 - x1;	
				int deltay = y2 - y1;
				
				Point p = new Point(2);
				double deltaerr = Math.abs((double)deltay/deltax);
				double error = 0;
				
				p.x[1] = y1;
				p.x[0] = x1;
				
				int Col[] = new int [3];
				
				while (p.x[0] != x2 && p.x[1] != y2)
				{
					ip[i].setLabel(p, 0);
					Col[0] = 0;
					
					ipp[i].getProcessor().putPixel(p.x[0], p.x[1], Col);
					error = error + deltaerr;
					
					while (error >= 0.5)
					{
						p.x[1]+=yp;
						error = error - 1.0;
						ip[i].setLabel(p, 0);
						ipp[i].getProcessor().putPixel(p.x[0], p.x[1], Col);
					}
					p.x[0]+=xp;
				}
				
				ipp[i].updateAndDraw();
			}
		}
		
	}
	
	private class MergeBtn implements ActionListener
	{
		LabelImageRC ip[];
		ImagePlus ipp[];
		PickRegion pr[];
		
		MergeBtn(LabelImageRC ip_[])
		{
			ip = ip_;
			pr = new PickRegion[ip_.length];
			ipp = new ImagePlus[ip_.length];
			for (int i = 0 ; i < ip_.length ; i++)
			{
				ipp[i] = ip[i].convert("Topo-fix", 255);
				ipp[i].show();
				pr[i] = new PickRegion(ipp[i]);
			}
		}
		
		ImagePlus [] getImagePlus()
		{
			return ipp;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) 
		{
			int from;
			int to;
			
			int Col[] = new int [3];
			
			
			for (int i = 0 ; i < ip.length ; i++)
			{
				Vector<Point> pC = pr[i].getClick();
				for (int j = 0 ; j < pC.size() ; j += 2)
				{
					from = ip[i].getLabel(pC.get(j));
					to = ip[i].getLabel(pC.get(j+1));
					
					RegionIterator itImg = new RegionIterator(ip[i].getDimensions());
					
					Col[0] = to;
					while (itImg.hasNext())
					{
						int k = itImg.next();
						Point p = itImg.getPoint();
						if (ip[i].getLabelAbs(k) == from)
						{
							ip[i].setLabel(k, to);
							ipp[i].getProcessor().putPixel(p.x[0], p.x[1], Col);
						}
					}
				}
				pC.clear();
				ipp[i].updateAndDraw();
			}
		}
	}
	
	JDialog frm;
	
	public void MergeAndDivideWin(LabelImageRC ip[])
	{
		lock = new Object();
		frm = new JDialog();
		
		frm.addWindowListener( new WindowAdapter() 
		{
			@Override
			public void windowClosing(WindowEvent e) 
			{
				synchronized(lock)
				{
					lock.notify();
				}
			}
		});
		
		frm.setTitle("Merge and divide");
		frm.setBounds(100, 100, 200, 140);
		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		frm.setContentPane(contentPane);
		contentPane.setLayout(new GridBagLayout());
		
		// Divide message
		JPanel contentDivide = new JPanel();
		contentDivide.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentDivide.setLayout(new GridLayout(0, 2, 0, 0));
		JLabel lblNewLabel = new JLabel("Draw a lines and divide: ");
		contentDivide.add(lblNewLabel);
		JButton btnDivide = new JButton("Divide");
		contentDivide.add(btnDivide);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		contentPane.add(contentDivide,c);
		
		// Merge message
		JPanel contentMerge = new JPanel();
		contentMerge.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentMerge.setLayout(new GridLayout(0, 2, 0, 0));
		lblNewLabel = new JLabel("Merge regions: ");
		contentMerge.add(lblNewLabel);
		JButton btnMerge = new JButton("Merge");
		contentMerge.add(btnMerge);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		contentPane.add(contentMerge,c);
		
		MergeBtn btnL = new MergeBtn(ip);
		
		btnMerge.addActionListener(btnL);
		btnDivide.addActionListener(new DivideBtn(ip,btnL.getImagePlus()));
		
		frm.setVisible(true);
		
		waitClose();
		
		for (int i = 0 ; i < ip.length ; i++)
		{
			ip[i].deleteParticles();
			ip[i].connectedComponents();
		}
	}
	
	Object lock;
	
	void waitClose()
	{
		synchronized(lock)
		{
			try {
				lock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
