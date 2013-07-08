package mosaic.region_competition.wizard.score_function;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Roi;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import mosaic.plugins.Region_Competition;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Point;
import mosaic.region_competition.PointCM;
import mosaic.region_competition.RegionIterator;
import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.CurvatureBasedFlow;
import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.wizard.PickRegion;
	
public class ScoreFunctionRCtop implements ScoreFunction
{	
	String[] file;
	private double smooth[];
	private int Area[];
	
	IntensityImage i[];
	LabelImage l[];
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
		
	public ScoreFunctionRCtop(IntensityImage i_[], LabelImage l_[], Settings s_)
	{
		i = i_;
		l = l_;
		
		s = s_;
		file = new String[l.length];
		pntMod = new PointCM[l.length][];
	}

	public LabelImage getLabel(int im)
	{
		return l[im];
	}
	
	public void setSmooth(double a[])
	{
		smooth = a;
	}
		
	public void setArea(int[] sizeA)
	{
		Area = sizeA;
	}
	
	public void setCMModel(PointCM [] mod,int i)
	{
		pntMod[i] = mod;
	}
	
	
	public double Topo(LabelImage l, PointCM pntMod[])
	{
		int off[] = l.getDimensions().clone();
		Arrays.fill(off, 0);
		HashMap<Integer,PointCM> Reg = new HashMap<Integer,PointCM>();
		RegionIterator img = new RegionIterator(l.getDimensions(),l.getDimensions(),off);
		
		while (img.hasNext())
		{
			Point p = img.getPoint();
			int i = img.next();
			if (l.dataLabel[i] != 0)
			{
				int id = Math.abs(l.dataLabel[i]);
				
				PointCM pCM = Reg.get(id);
				
				pCM.count++;
				pCM.p.add(p);
				
				// Get the module of the curvature flow
			}
		}
		
		for (PointCM pCM : Reg.values())
		{
			pCM.p.div(pCM.count);
		}
		
		// Reorder
		
		double Min = Double.MAX_VALUE;
		PointCM pMin = null;
		PointCM reoMod[] = new PointCM[Reg.size()];
		int k = 0;
		
		for (PointCM pCM : pntMod)
		{
			if (Reg.size() != 0)
			{
				for (PointCM pReg : Reg.values())
				{
					double d = pReg.p.distance(pCM.p);
					if (d < Min)
					{
						Min = d;
						pMin = pReg;
					}
				}
				reoMod[k] = pMin;
				k++;
			}
		}
			
		// Calculate

		double ret = 0.0;
		
		for (int i = 0 ; i < reoMod.length || i < pntMod.length ; i++)
		{
			ret += reoMod[k].p.distance(pntMod[k].p);
		}
		
		double expo = 1.0 / (double)l.getDim();
		ret += Math.abs(reoMod.length - pntMod.length)*Math.pow(l.getSize()/pntMod.length,expo);
		
		return ret;
	}
		
	@Override
	public double valueOf(double[] x) 
	{	
		double result = 0.0;
			
		s.m_RegionMergingThreshold = (float) x[0];
		
		// write the settings
		
		try {
			Region_Competition.SaveConfigFile(IJ.getDirectory("temp")+"RC_top"+x[0], s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		for (int im = 0 ; im < i.length ; im++)
		{
			IJ.run(i[im].imageIP,"Region Competition","config="+IJ.getDirectory("temp")+"RC_top"+x[0] + "  " + "output=" +IJ.getDirectory("temp")+"RC_top"+x[0]+".tif");
			
			// Read Label Image
		
			Opener o = new Opener();
			file[im] = new String(IJ.getDirectory("temp")+"RC_smo"+x[0]+".tif");
			ImagePlus ip = o.openImage(file[im]);

			l[im].initWithIP(ip);
			l[im].createStatistics(i[im]);
				
			// Scoring
		 		
			int count = 0;
			double a1 = 0.0;
			double a2 = 0.0;
			
			result = Topo(l[im],pntMod[im]);
				
//			result += 10.0/Math.abs(a1 - a2);
		}
			
		return result;
	}

	@Override
	public boolean isFeasible(double[] x) 
	{
		int minSz = Integer.MAX_VALUE;
		for (LabelImage lbt : l)
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
			
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void incrementStep() 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void show() 
	{
		// TODO Auto-generated method stub
	
		for (int im = 0 ;  im < l.length ; im++)
			l[im].show("init", 10);
		
	}

	@Override
	public double[] getAMean(Settings s) 
	{
		// TODO Auto-generated method stub
		double [] aMean = new double [1];
		
		aMean[0] = s.m_RegionMergingThreshold;
		
		return aMean;
	}
		
	@Override
	public int getNImg() 
	{
		// TODO Auto-generated method stub
		return l.length;
	}

	@Override
	public TypeImage getTypeImage() {
		// TODO Auto-generated method stub
		return TypeImage.FILENAME;
	}

	@Override
	public ImagePlus[] getImagesIP() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getImagesString() {
		// TODO Auto-generated method stub
		return file;
	}
	
	
	class DivideBtn implements ActionListener
	{

		LabelImage ip[];
		ImagePlus ipp[];
		
		DivideBtn(LabelImage ip_[])
		{
			ip = ip_;
			ipp = new ImagePlus[ip.length];
			for (int i = 0 ; i < ip_.length ; i++)
			{
				ipp[i] = ip[i].convert("Topo-fix", 255);
				ipp[i].show();
			}
		}
		
		DivideBtn(LabelImage ip_[], ImagePlus ipp_[])
		{
			ip = ip_;
			ipp = ipp_;
		}
		
		ImagePlus [] getImagePlus()
		{
			return ipp;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) 
		{
			// TODO Auto-generated method stub
			
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
	
	class MergeBtn implements ActionListener
	{
		LabelImage ip[];
		ImagePlus ipp[];
		PickRegion pr[];
		
		MergeBtn(LabelImage ip_[])
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
			
			// TODO Auto-generated method stub
			
			for (int i = 0 ; i < ip.length ; i++)
			{
				Vector<Point> pC = pr[i].getClick();
				for (int j = 0 ; j < pC.size() ; j += 2)
				{
					from = ip[i].getLabel(pC.get(j));
					to = ip[i].getLabel(pC.get(j+1));
					
					for (int k = 0 ; k < ip[i].getSize() ; k++)
					{
						if (ip[i].getLabel(k) == from)
							ip[i].setLabel(k, to);
					}
				}
			}
		}
		
	}
	
	JDialog frm;
	
	public void MergeAndDivideWin(LabelImage ip[])
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
		
		frm.show();
		
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
}
