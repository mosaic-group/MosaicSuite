package mosaic.region_competition.wizard;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
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
import mosaic.region_competition.Settings;
import mosaic.region_competition.initializers.MaximaBubbles;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

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
					result += 4.0*Math.abs((double)lb.count - l[im].getSize()/4.0)/l[im].getSize()/off[im];
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
	
	// Score function try to find out the best segmentation with PC on all area selected
	// use setPS() to select PS
	
	class ScoreFunctionRC implements ScoreFunction
	{
		private double aTol;
		private double sigma;
		
		IntensityImage i[];
		LabelImage l[];
		Algorithm al;
		
		ScoreFunctionRC(IntensityImage i_[], LabelImage l_[], double aTol_, double sigma_)
		{
			i = i_;
			l = l_;
			
			aTol = aTol_;
			sigma = sigma_;
		}

		@Override
		public double valueOf(double[] x) 
		{	
			double result = 0.0;
			
			Settings s = new Settings();
			s.m_RegionMergingThreshold = (float) x[0];
			s.m_EnergyContourLengthCoeff = (float) x[1];
			s.m_CurvatureMaskRadius = (float) x[2];
			s.l_Sigma = sigma;
			s.l_Tolerance = aTol;
			
			// write the settings
			
			try {
				Region_Competition.SaveConfigFile(IJ.getDirectory("temp")+"RC_"+x[0]+"_"+x[1]+"_"+x[2], s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for (int im = 0 ; im < i.length ; im++)
			{
				IJ.run("Region_Competition","config="+IJ.getDirectory("temp")+"RC_"+x[0]+"_"+x[1]+"_"+x[2] + "  " + "output=" +IJ.getDirectory("temp")+"RC_"+x[0]+"_"+x[1]+"_"+x[2]+".tif");
				
				// Read Label Image
				
				// Scoring
				
				result += Math.abs(l[im].getLabelMap().size()-1)*l[im].getSize();
				
				double a1 = 0.0;
				double a2 = 0.0;
				
				Collection<LabelInformation> li = l[im].getLabelMap().values();
				
				
				a1 = ((LabelInformation)li.toArray()[0]).mean;
				a2 = ((LabelInformation)li.toArray()[1]).mean;
				
				result += Math.abs(a1 - a2)/(i[im].imageIP.getStatistics().max - i[im].imageIP.getStatistics().min);
			}
			
			return result;
		}

		@Override
		public boolean isFeasible(double[] x) 
		{
			if (x[0] <= 0.0 || x[1] <= 0.0 || x[2] <= 0.0)
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
	
	CMAEvolutionStrategy OptimizeWithCMA(ScoreFunction fi,double aMean[], double aDev[], String Question)
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
				System.out.println("Pop 0");
				System.out.println(pop[0][0]);
				System.out.println(pop[0][1]);
				System.out.println(fitness[0]);
				System.out.println("Pop 1");
				System.out.println(pop[1][0]);
				System.out.println(pop[1][1]);
				System.out.println(fitness[1]);
				System.out.println("Pop 2");
				System.out.println(pop[2][0]);
				System.out.println(pop[2][1]);
				System.out.println(fitness[2]);
				System.out.println("Pop 3");
				System.out.println(pop[3][0]);
				System.out.println(pop[3][1]);
				System.out.println(fitness[3]);
				System.out.println("Pop 4");
				System.out.println(pop[4][0]);
				System.out.println(pop[4][1]);
				System.out.println(fitness[4]);
			
//				System.out.println("Press Enter to continue");
//				try{System.in.read();}
//				catch (Exception e) {}		
			
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
			solver.setFitnessOfMeanX(fi.valueOf(solver.getMeanX()));
		
			System.out.println((solver.getMeanX())[0]);
			System.out.println((solver.getMeanX())[1]);
		
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
		{/*fi = new ScoreFunctionInit(in,lb,r_t,rad, (int) Math.pow(2,in[0].getDim()));*/}
		
		double aMean[] = new double[2];
		double aDev[] = new double[2];
		aMean[0] = sigma;
		aMean[1] = tol;
		aDev[0] = 0.5;
		aDev[1] = 0.005;
		
		CMAEvolutionStrategy solver = OptimizeWithCMA(fi,aMean,aDev,"Check whenever all objects has at least 1 or more region (Yes = 1)");
		
		// we work on E_lenght and R_k E_merge  // we fix on PC
		
		double LM[] = solver.getMeanX();
		
		aMean = new double [3];
		aDev = new double [3];
		
		aMean[0] = 0.04;
		aMean[2] = 0.02;
		aMean[1] = 4.0;
		
		aDev[0] = 0.005;
		aDev[1] = 0.001;
		aDev[2] = 0.2;
		
		ScoreFunctionRC fiRC = new ScoreFunctionRC(in,lb,LM[0],LM[1]);
		
		solver = OptimizeWithCMA(fiRC,aMean,aDev,"Check whenever the segmentation is reasonable");
		
			// run Region Competition on all images
		
			// Filter out with ROI
		
			// Scoring
		
			// N(nr) + a/(Iman - Imin) * N
		
			// Selection
		
	}
}
