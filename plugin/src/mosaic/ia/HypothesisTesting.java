package mosaic.ia;

import ij.IJ;
import ij.gui.Plot;

import java.awt.Color;
import java.util.Arrays;
import java.util.Random;

import org.math.array.LinearAlgebra;
import org.math.array.StatisticSample;

import Jama.Matrix;



import weka.estimators.KernelEstimator;

import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.PlotUtils;

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
	private double Tob,Uob;
	private int binNum=20;
	private double binSpace;
	private double [] Tl;
	private double [][] Td1,Td2;
	//private double [] tempTd;
	private double [] TdMean;
	private double [][]  TdCov, TdInvCov;
	private double [] U;
	
	
	public HypothesisTesting(double[] cDFGrid, double[] dGrid,double[] D,
			double[] params, int type, int K, double alpha) {
		super();
		CDFGrid = cDFGrid;
		DGrid = dGrid;
	//	System.out.println("CDFGrid size:"+cDFGrid.length+" Dgrid size"+dGrid.length);
		this.N = dGrid.length;
		this.params = params;
		this.type = type;
		this.D=D;
		this.K=K;
		this.alpha=alpha;
	//	PlotUtils.plotDoubleArray("CDF", dGrid, cDFGrid);
	}
	
	
	private void displayResultNonParam()
	{
		int no=100;
		KernelEstimator kde=IAPUtils.createkernelDensityEstimator(U, .01);
		double [] Ulinspace=new double[no],Udens=new double[no];
		double Udiff=(U[U.length-1]-U[0])/no;
		double sum=0,max=0,min=Double.MAX_VALUE;
		for(int i=0;i<no;i++)
		{
			Ulinspace[i]=U[0]+Udiff*i;
			Udens[i]=kde.getProbability(Ulinspace[i]);
			sum=sum+Udens[i];
		}
	//	PlotUtils.histPlotDoubleArray("Statistic:T",T);
		for(int i=0;i<no;i++)
		{
		
			Udens[i]=Udens[i]/sum;
			if(Udens[i]>max)
				max=Udens[i];
			if(Udens[i]<min)
				min=Udens[i];
		}
		Plot plot = new Plot("Result: NonParam Hypothesis testing","U","Probability density",Ulinspace,Udens);
		
		plot.setLimits(Math.min(Ulinspace[0],Uob),Math.max(Ulinspace[Ulinspace.length-1],Uob)+Udiff*50, min, max);
		
		plot.setColor(Color.BLUE);
       // plot.setLimits(0, 1, 0, 10);
        plot.setLineWidth(2);
        plot.addLabel(.7, .2, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .2, "H0: True - Random");
        plot.draw();
        double [] Tx={Uob,Uob};
        double [] Ty={0, max};
        
        plot.setColor(Color.RED);
        plot.addPoints(Tx, Ty, Plot.LINE);
        plot.setLineWidth(2);
        plot.addLabel(.7, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .3, "H0: True - Estimated");
        plot.draw();
        plot.show();
        System.out.println("Uob at plot stage:"+Uob);
	}
	
	

	 
	/*
	 public boolean nonParametricTest(){
	
		
		binNum=IAPUtils.getOptimBins(D, 8, (int)Math.floor(D.length/8));
		System.out.println("Bin no for nonparametric: "+binNum);
		//generate thresholds
		Tl=new double[binNum];
	//	tempTd=new double[binNum];
		
		Td1=new double[K][Tl.length]; 
		Td2=new double[K][Tl.length]; 
		TdMean=new double[binNum];

		
		binSpace= (DGrid[DGrid.length-1]-DGrid[0])/binNum;
		Tl[0]=DGrid[0];
		for (int i=1;i<Tl.length;i++ )
			Tl[i]=Tl[i-1]+binSpace;
		
		
		//generate N samples of d from Q ,  MCx2 times.
	
		Td1=calculateTd();
		
		
		
	/*	for(int i=0;i<K;i++)
		{
			//generate d
			
			
			for(int j=0;j<Tl.length;j++)
				System.out.print("Td1["+i+"]["+j+"]:"+Td1[i][j]+",");
			System.out.println(";");

		}
		
	
		
			Td2=calculateTd();
			
		
		//System.out.println("1st term of Td1: "+Td1[0][0]);
		//	RealMatrix rmTd1= new RealMatrixImpl(Td1);
		//System.out.println("1st term of rmTd1: "+rmTd1.getEntry(0, 0));
		for(int i=0;i<Td1.length;i++){
			for(int j=0;j<Td1[0].length;j++)
				System.out.print(Td1[i][j]+",");
			System.out.println("");
		}
			System.out.println("Mean:");
			for(int i=0;i<Tl.length;i++)
			{
				for(int j=0;j<K;j++)
				{
					TdMean[i]=TdMean[i]+Td1[j][i];
					
				}
				TdMean[i]=TdMean[i]/K;
				System.out.print(TdMean[i]);
				
			}
			System.out.println("");
			PlotUtils.histPlotDoubleArray_imageJ("TdMean:nonparam", TdMean,binNum);
	    
	        TdCov = new double[Tl.length][Tl.length];
	   
	        double c;
	    
	        for (int i = 0; i < Tl.length; i++) {
	            for (int j = 0; j < Tl.length; j++) {
	                c = 0;
	           
	            
	                for (int k = 0; k < K; k++)
	                {
	                    c += (Td1[k][i] - TdMean[i]) * (Td1[k][j] - TdMean[j]);
	                }
	                TdCov[i][j] = c /(K - 1);
	            }
	        }

	    
	
		TdInvCov= IAPUtils.pseudoInverse(new Matrix(TdCov)).getArray();

		
		U=new double[K];
		double [] dT=new double[Tl.length];
		for(int i=0;i<K;i++)
		{
			
			for(int j=0;j<Tl.length;j++)
			{
				dT[j]=TdMean[j]-Td2[i][j];
			}
		//	U[i]=MatrixUtils.createColumnRealMatrix(dT).multiply(TdInvCov.multiply(MatrixUtils.createRowRealMatrix(dT))).getEntry(0, 0); 
			U[i]=LinearAlgebra.times(dT,LinearAlgebra.times(TdInvCov,dT))[0];
					
			
		}
		
		PlotUtils.histPlotDoubleArray_imageJ("dT:nonparam:q:1sample", dT,binNum);
		Arrays.sort(U);
		//now for D
		double [] tempTD=new double[Tl.length];
	
		for(int i=0;i<D.length;i++)
		{
			for(int j=0;j<Tl.length-1;j++){
				if(D[i]>= Tl[j] && D[i]< Tl[j+1])
				{
					tempTD[j]++;
					break;
				}
				
			}
		}
		

		for(int j=0;j<Tl.length;j++)
		{
			dT[j]=TdMean[j]-tempTD[j];
		}
		PlotUtils.histPlotDoubleArray_imageJ("dT:nonparam:p", dT,binNum);
//		double U1=MatrixUtils.createColumnRealMatrix(dT).multiply(TdInvCov.multiply(MatrixUtils.createRowRealMatrix(dT))).getEntry(0, 0); 
		Uob=LinearAlgebra.times(dT,LinearAlgebra.times(TdInvCov,dT))[0];
		//rank
		int i;
		for(i=0;i<K;i++)
		{
			if(Uob<=U[i])
				break;
		}
		
		if(i>(int)((1-alpha)*K) )
		{
			System.out.println("NonParametric: Null hypothesis rejected, rank: "+i+" out of "+K);
			IJ.showMessage("NonParametric with "+binNum+" bins: Null hypothesis: No interaction - Rejected, rank: "+i+" out of "+K+"MC runs with alpha= "+alpha);
			//displayResultNonParam();
			System.out.println("NullMax:"+U[U.length-1]+" observed U:"+Uob);
			return true;
		}	
		else
		{
			IJ.showMessage("NonParametric with "+binNum+" bins:  Null hypothesis accepted, rank: "+i+" out of "+K+" MC runs with alpha= "+alpha);
			System.out.println("NonParametric: Null hypothesis: No interaction - Accepted, rank: "+i+" out of "+K+" MC runs with alpha= "+alpha);
	//		displayResultNonParam();
			System.out.println("NullMax:"+U[U.length-1]+" observed U:"+Uob);
			return false;
		}
		
	}
	*/
	

	public boolean rankTest(){
		
		
		
		calculateT();
		PotentialCalculator pcOb=new PotentialCalculator(D, params, type);
		pcOb.calculateWOEpsilon();
	//	PlotUtils.plotDoubleArray("Estimated Pot", D, pcOb.getPotential());
		Tob= -1*pcOb.getSumPotential();
		
	//double [] oneToK=new double[K];
	//	for(i=0;i<K;i++)
		//{
			//oneToK[i]=(double)i;
		//}
		double maxT=Double.MIN_VALUE,minT=Double.MAX_VALUE;
		
		
		for(int i=0;i<K;i++)
		{
			if(minT>T[i])
				minT=T[i];
			if(maxT<T[i])
				maxT=T[i];
			
		}
		int i=0;
		for(i=0;i<K;i++)
		{
			if(Tob<=T[i])
				break;
		}
		
	//	PlotUtils.histPlotDoubleArray("T", T);
	//	PlotUtils.plotDoubleArray("T", oneToK, T);
		System.out.println("MinT: "+minT+" maxT: "+maxT);
		System.out.println("T obs: "+Tob+" found at rank: "+i);
		if(i>(int)((1-alpha)*K))
		{
			System.out.println("Null hypothesis rejected, rank: "+i+" out of "+K);
			IJ.showMessage("Null hypothesis: No interaction - Rejected, rank: "+i+" out of "+K+"MC runs with alpha= "+alpha);
			//displayResult();
			return true;
		}	
		else
		{
			IJ.showMessage("Null hypothesis accepted, rank: "+i+" out of "+K+" MC runs with alpha= "+alpha);
			System.out.println("Null hypothesis: No interaction - Accepted, rank: "+i+" out of "+K+" MC runs with alpha= "+alpha);
		//	displayResult();
			return false;
		}
		
			
	}

	private void calculateT()
	{
		DRand=new double[D.length];
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
		KernelEstimator kde=IAPUtils.createkernelDensityEstimator(T, .01);
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
        plot.show();
        
	}
	
	private double [][] calculateTd()
	{
		double [][] tempTd=new double[K][Tl.length];
		for(int k=0;k<K;k++)
		{
			//generate d
			generateRandomD();
			for(int i=0;i<DRand.length;i++)
			{
				{
				for(int j=0;j<Tl.length-1;j++){
					if(DRand[i]>= Tl[j] && DRand[i]< Tl[j+1]){
						tempTd[k][j]++;
						break;
						
						}
					}
				}
			}
		}
		return tempTd;
	}
	
	
	private double calculateTk()
	{
		generateRandomD();
		PotentialCalculator pc=new PotentialCalculator(DRand, params, type);
		pc.calculateWOEpsilon();
	//	PlotUtils.plotDoubleArray("Null Hyp Pot", D, pc.getPotential());
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
//		System.out.println("CDFGrid[0]: "+CDFGrid[0]);
	//	System.out.println("CDFGrid[N-1]: "+CDFGrid[N-1]);
		
		for(int i=0;i<D.length;)
		{
		R=rn.nextDouble();
		if(R>=CDFGrid[0]) /// to make sure that random value will be gte the least in cdf
		{
			DRand[i]=findD(R);
			i++;
		}
	//	System.out.print(R+":"+DRand[i]+",");
		}
	//	System.out.println(");
		

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
		//System.out.println("R: "+R+" CDF[0]"+CDFGrid[0]);
		return IAPUtils.linearInterpolation(DGrid[i], CDFGrid[i],DGrid[i+1], CDFGrid[i+1], R);
	}
	
	

	
	

}
