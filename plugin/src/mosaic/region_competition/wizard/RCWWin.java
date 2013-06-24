package mosaic.region_competition.wizard;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.GridLayout;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTabbedPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import mosaic.paramopt.cma.CMAEvolutionStrategy;
import mosaic.paramopt.cma.fitness.IObjectiveFunction;
import mosaic.plugins.Region_Competition;
import mosaic.region_competition.Algorithm;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.LabelInformation;
import mosaic.region_competition.Point;
import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.initializers.InitializationType;
import mosaic.region_competition.initializers.MaximaBubbles;
import mosaic.region_competition.Settings;
import mosaic.region_competition.RCMean;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;

public class RCWWin extends JDialog implements MouseListener, Runnable
{

	enum segType
	{
		Tissue,
		Cell,
		Other
	}
	
	private JPanel contentPane;
	private JComboBox b1;
	private ImagePlus img[];
	private segType sT;
	
	public void start()
	{
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RCWWin frame = new RCWWin();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private int Ask(String title,String message)
	{
		GenericDialog ask = new GenericDialog(title);
		ask.addNumericField(message, 1, 1);
		ask.showDialog();
		
		if(ask.wasCanceled())
			return -1;
		else
			return (int) ask.getNextNumber();
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) 
	{
		EventQueue.invokeLater(new Runnable() 
		{
			public void run() 
			{
				try 
				{
					RCWWin frame = new RCWWin();
					frame.setVisible(true);
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		});
	}

	private String[] GetROI()
	{
		RoiManager manager = RoiManager.getInstance();
		if (manager == null)
		    manager = new RoiManager();
	    Hashtable<String, Roi> table = (Hashtable<String, Roi>)manager.getROIs();
	    int i = 0;
	    String[] md = new String[table.size()];
	    for (String label : table.keySet())
	    {
	    	int slice = manager.getSliceNumber(label);
	        Roi roi = table.get(label);
	        md[i] = roi.getName();
	        i++;
	    }
	    return md;
	}
	

	private static JButton getButtonSubComponent(Container container) 
	{
	    if (container instanceof JButton) 
	    {
	         return (JButton) container;
	    }
	    else
	    {
	         Component[] components = container.getComponents();
	         for (Component component : components) 
	         {
	            if (component instanceof Container) 
	            {
	               return getButtonSubComponent((Container)component);
	            }
	         }
	    }
	    return null;
	 }
	
	/**
	 * Create the frame.
	 */
	public RCWWin()
	{
//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(0, 2, 0, 0));
		
		JLabel lblNewLabel = new JLabel("What you are segmenting: ");
		contentPane.add(lblNewLabel);
		
		JComboBox comboBox = new JComboBox(segType.values());
		contentPane.add(comboBox);
		comboBox.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				sT = (segType)((JComboBox)arg0.getSource()).getSelectedItem();
			}
		});
		
		JLabel lblNewLabel_1 = new JLabel("<html>Do you have a Point spread <br>function image ?</html>");
		contentPane.add(lblNewLabel_1);
		
		JButton btnNewButton = new JButton("Browse");
		contentPane.add(btnNewButton);
		
		JLabel lblNewLabel_2 = new JLabel("<html>Select a region of interest <br>with the imageJ selection<br> tool</html>");
		contentPane.add(lblNewLabel_2);
		
		JComboBox comboBox_1 = new JComboBox();;
	    
	    String[] md = GetROI();
	    
		comboBox_1.setBackground(Color.YELLOW);
		contentPane.add(comboBox_1);
		comboBox_1.setModel((new DefaultComboBoxModel(md)));
		
		b1 = comboBox_1;
		
		// Add a refresh for the action listener
		
		getButtonSubComponent(comboBox_1).addMouseListener(this);
		
		// OK and Cancel button
		
		JButton btnOKButton = new JButton("OK");
		contentPane.add(btnOKButton);
		btnOKButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				ComputePar();
			}
		});

		JButton btnCancelButton = new JButton("Cancel");
		contentPane.add(btnCancelButton);
		
	}
	
	void ComputePar()
	{
		int i = 0;
		
		// Get if is Tissue or Cell
		
		
		
		// Get the regions
		
		RoiManager manager = RoiManager.getInstance();
	    Hashtable<String, Roi> table = (Hashtable<String, Roi>)manager.getROIs();
	    String[] md = new String[table.size()];
	    img = new ImagePlus[table.size()];
	    for (String label : table.keySet())
	    {
	    	int slice = manager.getSliceNumber(label);
	        Roi roi = table.get(label);
	        
	        Rectangle b = roi.getBounds();
	        img[i] = new ImagePlus(roi.getName(),ij.WindowManager.getImage(roi.getImageID()).getProcessor());
	        ImageProcessor ip = img[i].getProcessor();
	        ip.setRoi(b.x,b.y,b.width,b.height);
	        img[i].setProcessor(null,ip.crop());
	        i++;
	    }
		
		// Start computation thread
		
	    Thread t = new Thread(this,"Compute parameters");
	    t.start();
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) 
	{
		String[] md = GetROI();
		b1.setModel((new DefaultComboBoxModel(md)));
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {}

	void runRC(ImagePlus img, Settings s)
	{
		IJ.run("Region Competition", "config=");
	}
	
	// Score function try to find out the best initialization on all area selected
	
	interface ScoreFunction extends IObjectiveFunction
	{
		abstract void incrementStep();
		abstract void show();
	}
	
	class ScoreFunctionInit implements ScoreFunction
	{
		int off[];
		int inc_step[];
		int r_t = 8;
		int rad = 8;
		
		IntensityImage i[];
		LabelImage l[];
		
		ScoreFunctionInit(IntensityImage i_[], LabelImage l_[],int r_t_,int rad_)
		{
			i = i_;
			l = l_;
			r_t = r_t_;
			rad = rad_;
			off = new int [i_.length];
			inc_step = new int [i_.length];
		}

		public void incrementStep()
		{
			for (int j = 0 ; j < off.length ; j++)
			{
				off[j] += inc_step[j];
			}
		}
		
		void setObject(int i, int off_)
		{
			off[i] = off_;
			if (2*off_ / 5 >= 1)
				inc_step[i] = 2*off_ / 5;
			else
				inc_step[i] = 1;
		}
		
		int [] getObject()
		{
			return off;
		}
		
		@Override
		public double valueOf(double[] x) 
		{
			double sigma = x[0];
			double tol = x[1];
			
			double result = 0.0;
			
			for (int im = 0 ; im < i.length ; im++)
			{
				l[im].initZero();
				MaximaBubbles b = new MaximaBubbles(i[im],l[im],rad,sigma,tol,r_t);
				b.initFloodFilled();
				int c = l[im].createStatistics(i[im]);
				HashMap<Integer, LabelInformation> Map = l[im].getLabelMap();
				Map.remove(0);  // remove background
				for (LabelInformation lb : Map.values())
				{
					result += 4.0*Math.abs((double)lb.count - l[im].getSize()/4.0/off[im])/l[im].getSize();
				}
				result += Math.abs(c - (off[im]+1)) /**l[im].getSize()*/;
				
//				l[im].show("test",10);
			}
			
			return result;
		}

		@Override
		public boolean isFeasible(double[] x) 
		{
			if (x[0] <= 0.0 || x[1] <= 0.0 )
				return false;
			
			if (x[0] >= 20.0)
				return false;
			
			// TODO Auto-generated method stub
			return true;
		}
		
		public void show()
		{
			for (int im = 0 ;  im < l.length ; im++)
				l[im].show("init", 10);
		}
	}
	
	class RCThread implements Runnable
	{
		Settings s;
		ImageProcessor img;
		Region_Competition rg;
		
		RCThread(ImageProcessor img_, Settings set)
		{
			img = img_;
			s = set;
		}
		
		Region_Competition getRC()
		{
			return rg;
		}
		
		@Override
		public void run() 
		{
			// TODO Auto-generated method stub
		
			rg = new Region_Competition(img,s);
			rg.runP();
		}
		
	}
	
	class PickRegion implements MouseListener, ChangeListener
	{
		JSlider slid;
		JTextArea text;
		ImageCanvas canvas;
		RCThread t;
		int id = -1;
		
		PickRegion(RCThread th, JSlider js, JTextArea tx)
		{
			ImageWindow win = th.getRC().getStackImPlus().getWindow();
			canvas = win.getCanvas();
			canvas.addMouseListener(this);
			slid = js;
			text = tx;
			t = th;
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent e) 
		{
			// TODO Auto-generated method stub
			
			int x = e.getX();
			int y = e.getY();
			int offscreenX = canvas.offScreenX(x);
			int offscreenY = canvas.offScreenY(y);
			
			ImagePlus img = t.getRC().getStackImPlus();
			
			LabelImage lb = t.getRC().getLabelImage();
			int size[] = lb.getDimensions();
			
			id = lb.getLabel(offscreenX+offscreenY*size[0]);
			Double r_FG = lb.getLabelMap().get(id).mean;
			text.setText(r_FG.toString());
			slid.setValue((int)(r_FG * 1000.0));
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void stateChanged(ChangeEvent arg0) {
			// TODO Auto-generated method stub
			
			text.setText(String.valueOf(slid.getValue()/1000.0));
			
			if (id != -1)
			{
				LabelImage lb = t.getRC().getLabelImage();
				double bg = lb.getLabelMap().get(0).mean;
				
				if (bg < slid.getValue()/1000.0 || id == 0)
				{
					RCMean rcm = new RCMean(id,slid.getValue()/1000.0);
			
					t.getRC().getAlgorithm().Process(rcm);
				}
			}
		}
		
	}
	
	JDialog g;
	
	LabelImage RCPainter(ImagePlus aImg, Settings s)
	{
		double result = 0.0;
		
		s.RC_free = true;
		s.m_EnergyContourLengthCoeff = (float) 0.001;
		s.m_RegionMergingThreshold = (float) 0.0;
		
		// Create the drawing window
		
		g = new JDialog();
		
		g.setTitle("RC Painter");
		g.setBounds(100, 100, 150, 100);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		g.setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(0, 1, 0, 0));
		
		JLabel lblNewLabel = new JLabel("Pick a region and move the cursor");
		contentPane.add(lblNewLabel);
		
		JSlider slid = new JSlider();
		slid.setMaximum(1000);
		slid.setMinimum(0);
		
		JTextArea text = new JTextArea("0.5");
		contentPane.add(slid);
		contentPane.add(text);
		
		RCThread RCT = new RCThread(aImg.getProcessor(),s);
		Thread t = new Thread(RCT);
		t.start();
		
		// Delay we have to start
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PickRegion pc = new PickRegion(RCT,slid,text);
		
        slid.addChangeListener(pc);
		
		g.addMouseListener(pc);
		
		g.show();
		
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return RCT.getRC().getLabelImage();
		
		//
		
//		t.stop();
	}
	
	// Score function try to find out the best segmentation with PC on all area selected
	// use setPS() to select PS
	
	class ScoreFunctionRC implements ScoreFunction
	{
		private int off[];
		private double aTol;
		private double sigma;
		
		IntensityImage i[];
		LabelImage l[];
		Algorithm al;
		
		ScoreFunctionRC(IntensityImage i_[], LabelImage l_[], double sigma_, double aTol_, int off_[])
		{
			i = i_;
			l = l_;
			
			aTol = aTol_;
			sigma = sigma_;
			
			off = off_;
		}

		@Override
		public double valueOf(double[] x) 
		{	
			double result = 0.0;
			
			Settings s = new Settings();
			s.m_RegionMergingThreshold = (float) x[0];
			s.m_EnergyContourLengthCoeff = (float) x[1];
			s.m_CurvatureMaskRadius = (float) x[2];
			if (s.m_GaussPSEnergyRadius > 2.0)
				s.m_EnergyFunctional = EnergyFunctionalType.e_PS;
			else
				s.m_EnergyFunctional = EnergyFunctionalType.e_PC;
			s.m_GaussPSEnergyRadius = (int) x[3];
			s.m_BalloonForceCoeff = (float)x[4];
			s.l_Sigma = sigma;
			s.l_Tolerance = aTol;
			s.labelImageInitType = InitializationType.LocalMax;
			
			// write the settings
			
			try {
				Region_Competition.SaveConfigFile(IJ.getDirectory("temp")+"RC_"+x[0]+"_"+x[1]+"_"+x[2], s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for (int im = 0 ; im < i.length ; im++)
			{
				IJ.run(i[im].imageIP,"Region Competition","config="+IJ.getDirectory("temp")+"RC_"+x[0]+"_"+x[1]+"_"+x[2] + "  " + "output=" +IJ.getDirectory("temp")+"RC_"+x[0]+"_"+x[1]+"_"+x[2]+".tif");
				
				// Read Label Image
				
				Opener o = new Opener();
				ImagePlus ip = o.openImage(IJ.getDirectory("temp")+"RC_"+x[0]+"_"+x[1]+"_"+x[2]+".tif");

				l[im].initWithIP(ip);
				l[im].createStatistics(i[im]);
				
				// Scoring
				
				result += Math.abs(l[im].getLabelMap().size()-off[im]);
				
				int count = 0;
				double a1 = 0.0;
				double a2 = 0.0;
				
				Collection<LabelInformation> li = l[im].getLabelMap().values();
				
				
				a1 = ((LabelInformation)li.toArray()[0]).mean;
				
				for (int i = 1 ; i < li.toArray().length ; i++)
				{
					a2 += ((LabelInformation)li.toArray()[i]).mean*((LabelInformation)li.toArray()[i]).count;
					count += ((LabelInformation)li.toArray()[i]).count;
				}
				a2 /= count;
				
				result += 10.0/Math.abs(a1 - a2);
			}
			
			return result;
		}

		@Override
		public boolean isFeasible(double[] x) 
		{
			if (x[0] <= 0.0 || x[1] <= 0.0 || x[2] <= 0.0 || x[3] <= 0.0 || x[4] <= 0.0)
				return false;
			
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void incrementStep() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void show() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	CMAEvolutionStrategy OptimizeWithCMA(ScoreFunction fi,double aMean[], double aDev[], String Question, boolean debug)
	{
		CMAEvolutionStrategy solver = null;
		boolean restart = true;
		int restart_it = 0;
		while (restart == true && restart_it < 5)
		{
			
			solver = new CMAEvolutionStrategy();
			solver.readProperties();
			solver.setDimension(aMean.length);
			solver.setInitialX(aMean);
			solver.setInitialStandardDeviations(aDev);
			solver.options.stopFitness = 0.3;
		
			// initialize cma and get fitness array to fill in later
		
			double[] fitness = solver.init();
		
			// initial output file
		
			solver.writeToDefaultFilesHeaders(0);
		
			// iteration loop
		
			int n_it = 0;
		
			while (solver.stopConditions.getNumber() == 0)
			{
				double [][] pop = solver.samplePopulation();
			
				for (int i = 0 ; i < pop.length ; i++)
				{
					while (!fi.isFeasible(pop[i]))
					{
						solver.resampleSingle(i);
					}
				
					fitness[i] = fi.valueOf(pop[i]);
				}
				for (int i = 0 ; i < pop.length ; i++)
				{
					System.out.println("Pop " + i);
					for (int j = 0 ; j < pop[i].length ; j++)
					{
						System.out.println(pop[i][j]);
					}
					System.out.println("fitness " + fitness[i]);
				}
			
				if (debug == true)
				{
					System.out.println("Press Enter to continue");
					try{System.in.read();}
					catch (Exception e) {}
				}
			
				solver.updateDistribution(fitness);
			
				solver.writeToDefaultFiles();
				int outmod = 150;
				if (solver.getCountIter() % (15*outmod) == 1)
				{solver.printlnAnnotation();}
				if (solver.getCountIter() % outmod == 1)
				{solver.println();}
			
				n_it++;
				if (n_it >= 40)
					break;
				
				System.out.println(n_it);
			}
			// evaluate mean value as it is the best estimator for the optimum
			solver.setFitnessOfMeanX(fi.valueOf(solver.getBestX()));
		
			System.out.println((solver.getBestX())[0]);
			System.out.println((solver.getBestX())[1]);
		
			fi.valueOf(solver.getMeanX());
			fi.show();
		
			// final output
		
			solver.writeToDefaultFiles(1);
			solver.println();
			solver.println("Terminated due to");
			for (String sc : solver.stopConditions.getMessages())
			{solver.println("    " + sc);}
			solver.println("best function value " + solver.getBestFunctionValue() +
					 " at evaluation " + solver.getBestEvaluationNumber());
		
			
			int ans = Ask("Question",Question);
			if (ans == 1)
				restart = false;
			else
			{
				restart = true;
				fi.incrementStep();
			}
			
			restart_it++;
			
		}
		
		return solver;
	}
	
	@Override
	public void run() 
	{
		// Start with standard settings
		
		Settings s = new Settings();
		s.labelImageInitType = InitializationType.LocalMax;
		
		// if is a Tissue produce pow(2,d) < r < pow(3,d) regions (region tol 8)
		// if is a Cell produce 1 region (region tol 16)
		
		// Convert ImagePlus into Intensity image
		
		int dim = 2;
		int sigma = 2;
		double tol = 0.005;
		int r_t = 8;
		int rad = 8;
		
		if (sT == segType.Tissue)
		{
			sigma = 2;
			tol = 0.005;
			r_t = 8;
			rad = 8;
		}
		else if (sT == segType.Cell)
		{
			sigma = 4;
			tol = 0.01;
			r_t = 0;
			rad = 0;
		}
		
		IntensityImage in[] = new IntensityImage[img.length];
		LabelImage lb[] = new LabelImage[img.length];
		
		for (int i = 0 ; i < img.length ; i++)
		{
			in[i] = new IntensityImage(img[i]);
			lb[i] = new LabelImage(in[i].getDimensions());
		}
		
		// Set initialization local minima
		
		ScoreFunctionInit fi = null;
		
		if (sT == segType.Cell || sT == segType.Other)
		{
			for (int i = 0 ; i < img.length ; i++)
			{
				in[i] = new IntensityImage(img[i]);
				fi = new ScoreFunctionInit(in,lb,r_t,rad);
				lb[i] = new LabelImage(in[i].getDimensions());
				in[i].imageIP.show();
				fi.setObject(i,Ask("Question", "How many object do you see ?"));
			}
			
		}
		else 
		{fi = new ScoreFunctionInit(in,lb,r_t,rad);}
		
		double aMean[] = new double[2];
		double aDev[] = new double[2];
		aMean[0] = sigma;
		aMean[1] = tol;
		aDev[0] = 0.5;
		aDev[1] = 0.005;
		
		CMAEvolutionStrategy solver = OptimizeWithCMA(fi,aMean,aDev,"Check whenever all objects has at least 1 or more region (Yes = 1)",false);
		
		// we work on E_lenght and R_k E_merge  PS_radius Ballon
		
		double LM[] = solver.getMeanX();
		
		aMean = new double [5];
		aDev = new double [5];
	
		s.l_Sigma = LM[0];
		s.l_Tolerance = LM[1];
		s.labelImageInitType = InitializationType.LocalMax;
		s.m_EnergyFunctional = EnergyFunctionalType.e_PC;
		s.m_CurvatureMaskRadius = 4;
//		s.m_ConstantOutwardFlow = (float) 0.04;
		
/*		img[0].show();
		lb[0] = RCPainter(img[0],s);
		
		ImagePlus lb_m = lb[0].createMeanImage();
		lb_m.show();*/
		
		aMean[0] = 0.04;
		aMean[1] = 0.02;
		aMean[2] = 4.0;
		aMean[3] = 1.0;
		aMean[4] = 0.0;
		
		aDev[0] = 0.05;
		aDev[1] = 0.01;
		aDev[2] = 0.2;
		aDev[3] = 3.0;
		aDev[4] = 0.01;
		
		ScoreFunctionRC fiRC = new ScoreFunctionRC(in,lb,LM[0],LM[1],fi.getObject());
		
		solver = OptimizeWithCMA(fiRC,aMean,aDev,"Check whenever the segmentation is reasonable", true);
		
			// run Region Competition on all images
		
			// Filter out with ROI
		
			// Scoring
		
			// N(nr) + a/(Iman - Imin) * N
		
			// Selection*/
		
	}
}
