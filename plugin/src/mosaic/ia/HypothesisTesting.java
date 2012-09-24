package mosaic.ia;

import ij.IJ;
import ij.gui.Plot;

import java.awt.Color;
import java.util.Arrays;
import java.util.Random;

import weka.estimators.KernelEstimator;

import mosaic.ia.utils.IAPUtils;

public class HypothesisTesting {
	private double [] CDFGrid;
	private double [] DGrid;
	private double [] D;
	private double [] DRand;
	private int N;
	private double [] T;
	private double [] params; //same convention
	private int type;
	private int K;
	private double alpha;
	private double Tob;
	
	public HypothesisTesting(double[] cDFGrid, double[] dGrid,double[] D, int N,
			double[] params, int type, int K, double alpha) {
		super();
		CDFGrid = cDFGrid;
		DGrid = dGrid;
	//	System.out.println("CDFGrid size:"+cDFGrid.length+" Dgrid size"+dGrid.length);
		this.N = N;
		this.params = params;
		this.type = type;
		this.D=D;
		this.K=K;
		this.alpha=alpha;
	//	PlotUtils.plotDoubleArray("CDF", dGrid, cDFGrid);
	}
	
	public boolean rankTest(){
		calculateT();
		PotentialCalculator pcOb=new PotentialCalculator(D, params, type);
		pcOb.calculateWOEpsilon();
		Tob= -1*pcOb.getSumPotential();
		int i=0;
	//double [] oneToK=new double[K];
	//	for(i=0;i<K;i++)
		//{
			//oneToK[i]=(double)i;
		//}
		for(i=0;i<K;i++)
		{
			if(Tob<=T[i])
				break;
		}
		
	//	PlotUtils.histPlotDoubleArray("T", T);
	//	PlotUtils.plotDoubleArray("T", oneToK, T);
		System.out.println("T obs: "+Tob);
		if(i>(int)((1-alpha)*K))
		{
			System.out.println("Null hypothesis rejected, rank: "+i+" out of "+K);
			IJ.showMessage("Null hypothesis: No interaction - Rejected, rank: "+i+" out of "+K+"MC runs with alpha= "+alpha);
			displayResult();
			return true;
		}	
		else
		{
			IJ.showMessage("Null hypothesis accepted, rank: "+i+" out of "+K+" MC runs with alpha= "+alpha);
			System.out.println("Null hypothesis: No interaction - Accepted, rank: "+i+" out of "+K+" MC runs with alpha= "+alpha);
			displayResult();
			return false;
		}
		
			
	}

	private void calculateT()
	{
		DRand=new double[N];
		T=new double[K];
		
		for(int i=0;i<K;i++)
		{
			//System.out.println("Calculating TK, K= "+i);
			T[i]=calculateTk();
		}
		
		Arrays.sort(T);
		
	}
	
	private void displayResult()
	{
		int no=100;
		KernelEstimator kde=IAPUtils.createkernelDensityEstimator(T, 1);
		double [] Tlinspace=new double[no],Tdens=new double[no];
		double Tdiff=(T[T.length-1]-T[0])/no;
		double sum=0,max=0,min=Double.MAX_VALUE;
		for(int i=0;i<no;i++)
		{
			Tlinspace[i]=T[0]+Tdiff*i;
			Tdens[i]=kde.getProbability(Tlinspace[i]);
			sum=sum+Tdens[i];
		}
	//	PlotUtils.histPlotDoubleArray("Statistic:T",T);
		for(int i=0;i<no;i++)
		{
		
			Tdens[i]=Tdens[i]/sum;
			if(Tdens[i]>max)
				max=Tdens[i];
			if(Tdens[i]<min)
				min=Tdens[i];
		}
		Plot plot = new Plot("Result: Hypothesis testing","T","Probability density",Tlinspace,Tdens);
		
		plot.setLimits(Math.min(Tlinspace[0],Tob),Math.max(Tlinspace[Tlinspace.length-1],Tob)+Tdiff*50, min, max);
		
		plot.setColor(Color.BLUE);
       // plot.setLimits(0, 1, 0, 10);
        plot.setLineWidth(2);
        plot.addLabel(.7, .2, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .2, "H0: True - Random");
        plot.draw();
        double [] Tx={Tob,Tob};
        double [] Ty={0, max};
        plot.setColor(Color.RED);
        plot.addPoints(Tx, Ty, Plot.LINE);
        plot.setLineWidth(2);
        plot.addLabel(.7, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .3, "H0: True - Estimated");
        plot.draw();
     
        
	}
	
	
	private double calculateTk()
	{
		generateRandomD();
		PotentialCalculator pc=new PotentialCalculator(DRand, params, type);
		pc.calculateWOEpsilon();
		/*for(int i=0;i<N;i++)
		{
			
			System.out.print("D: "+DRand[i]+" Potential: "+pc.getPotential()[i]);
		}
		System.out.println(" ");*/
		return -1*pc.getSumPotential();
		
	}
	
	private void generateRandomD()
	{
		
		// not erasing contents of DRAND
		Random rn = new Random(System.nanoTime());
		double R=0;
		for(int i=0;i<N;i++)
		{
		while((R=rn.nextDouble())<CDFGrid[0]); /// to make sure that random value will be gte the least in cdf
		DRand[i]=findD(R);
	//	System.out.print(R+":"+DRand[i]+",");
		}
		//System.out.println(" ");
		

	}
	
	private double  findD(double R)
	{
		int i;
		for(i=0;i<CDFGrid.length-1;i++)
		{
			if(R>= CDFGrid[i] && R < CDFGrid[i+1])
			{
				break;
				
			}
		}
		//System.out.println("CDFGrid size:"+CDFGrid.length+" Dgrid size"+DGrid.length+"current i:"+i);
		//System.out.println("R: "+R+" CDF[length-1]"+CDFGrid[0]);
		return IAPUtils.linearInterpolation(DGrid[i], CDFGrid[i],DGrid[i+1], CDFGrid[i+1], R);
	}
	
	

	
	

}
