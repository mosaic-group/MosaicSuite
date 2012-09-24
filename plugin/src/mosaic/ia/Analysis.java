package mosaic.ia;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.ImageCalculator;
import ij.plugin.Macro_Runner;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Vector;

import javax.vecmath.Point3d;

import mosaic.ia.nn.DistanceCalculations;
import mosaic.ia.nn.DistanceCalculationsCoords;
import mosaic.ia.nn.DistanceCalculationsImage;

import weka.estimators.KernelEstimator;

import mosaic.core.detection.Particle;
import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.ImageProcessUtils;
import mosaic.paramopt.cma.CMAEvolutionStrategy;

public class Analysis {
	private ImagePlus[] impList;
	private ImagePlus X,Y;
	private ImagePlus mask, genMask,loadedMask;
	private Point3d[] particleXSetCoordUnfiltered;
	private Point3d[] particleYSetCoordUnfiltered;

	private int dgrid_size=1000;
	private double [] q_D_grid,NN_D_grid;
	private KernelEstimator ke,kep; //ke for q and kep for NN
	private double x1, y1, x2, y2, z1, z2;
	public double getX1() {
		return x1;
	}
	public void setX1(double x1) {
		this.x1 = x1;
	}
	
	public void setY1(double y1) {
		this.y1 = y1;
	}
	
	public void setX2(double x2) {
		this.x2 = x2;
	}
	
	public void setY2(double y2) {
		this.y2 = y2;
	}

	public void setZ1(double z1) {
		this.z1 = z1;
	}
	
	public void setZ2(double z2) {
		this.z2 = z2;
	}

	private double[] D; //Nearest neighbour;
	private int potentialType; //1 is step, 2 is hernquist, 5 is nonparam
	private double [] best;
	
	
	
	private double [] q,nnObserved,dgrid;
	
	
	public double getMinD() {
		return minD;
	}
	public void setMinD(double minD) {
		this.minD = minD;
	}
	public double getMaxD() {
		return maxD;
	}
	public void setMaxD(double maxD) {
		this.maxD = maxD;
	}
	public double getMeanD() {
		return meanD;
	}
	public void setMeanD(double meanD) {
		this.meanD = meanD;
	}

	private double minD,maxD,meanD;
	private boolean isImage; // to distinguish b/wimage and coords
//	private Vector<Point> Xcoords, Ycoords;
	public void setImageList(ImagePlus X, ImagePlus Y)
	{
		impList = new ImagePlus[2];
		impList[0]=Y;
		impList[1]=X;
	}
	public Analysis(ImagePlus X, ImagePlus Y)
	{
	this.X=X;
	this.Y=Y;
	isImage=true;
//	System.out.println("1:"+X.getWidth());	
	}
	public boolean isMaskSet()
	{
		if(mask!=null)
			return true;
		return false;
	}
	
	
	public Analysis(Point3d [] Xcoords, Point3d [] Ycoords)
	{
	this.particleXSetCoordUnfiltered=Xcoords;
	this.particleYSetCoordUnfiltered=Ycoords;
	
	isImage=false;
//	System.out.println("1:"+X.getWidth());	
	}
	
	
	public void setPotentialType(int potentialType)
	{
		this.potentialType=potentialType;
	}
	
	public String getMaskTitle()
	{
		return mask.getTitle();
	}

	public boolean getIsImage()
	{
		return isImage;
	}

	
	public boolean calcDist(double gridSize)
	{
		
		boolean ret;
		DistanceCalculations dci;
		if(isImage==true)
		{
			 dci=new DistanceCalculationsImage(X, Y, mask, gridSize);
			dci.calcDistances();
			
		}
		else
		{
				dci=new DistanceCalculationsCoords(particleXSetCoordUnfiltered, particleYSetCoordUnfiltered, genMask, x1,y1,z1,x2,y2,z2,gridSize);
			dci.calcDistances();
		}
		D=dci.getD();
		kep=IAPUtils.createkernelDensityEstimator(D,50);
		ke=IAPUtils.createkernelDensityEstimator(dci.getDGrid(),.01);
		fillQofD_grid(dci.getDGrid());
		ret=true;
		if(ret==true)
		{
			
		q=normalize(q_D_grid);
		nnObserved=normalize(NN_D_grid);
		plotQP(dgrid, q, nnObserved);
		double [] minMaxMean=IAPUtils.getMinMaxMeanD(D);
		minD=minMaxMean[0];
		maxD=minMaxMean[1];
		meanD=minMaxMean[2];
		System.out.println("min d"+minD);
		}
		
		return ret;
	}
	
	
	


	private void fillQofD_grid(float D_grid []) //just to run kernel density
	{
		
		//double min=Double.MAX_VALUE;
		double min=0;
		double max=0;
		
		for(int i=0;i<D_grid.length;i++)
		{
		
			if (D_grid[i]>max)
				max=D_grid[i];
		}
	//	updateKernelforNonParam(min,max);
		dgrid=new double[dgrid_size];
		dgrid[0]=0;
		
		double bin_size=(max-min)/dgrid.length;
		System.out.println("Grid bin size"+bin_size);
		System.out.println("Grid bins length"+dgrid.length);
		
		q_D_grid=new double[dgrid.length];
		NN_D_grid=new double[dgrid.length];
		q_D_grid[0]=ke.getProbability(dgrid[0]);  // how does this work? q_D_grid is a histogram. how do we give the bin size to ke?
		NN_D_grid[0]=kep.getProbability(dgrid[0]);
		double sumProbability=0;
		for(int i=1;i<dgrid.length;i++)
		{
			dgrid[i]=dgrid[i-1]+bin_size;
			q_D_grid[i]=ke.getProbability(dgrid[i]);  // how does this work? q_D_grid is a histogram. how do we give the bin size to ke?
			NN_D_grid[i]=kep.getProbability(dgrid[i]);
			sumProbability=q_D_grid[i]+sumProbability;
		}
		System.out.println("Sum of q_D grid: "+sumProbability);
	
		
	}
	
	

	

	
	public boolean calcMask()
	{

		genMask=new ImagePlus();
		if(Y!=null)
		{
			genMask=ImageProcessUtils.generateMask(Y);
			return true;
		}
	//	System.out.println(genMask.getType());
		return false;
	}
	
	public boolean applyMask()
	{
		if(genMask==null)
			return false;
	//	new Macro_Runner().run("Convert to Mask");
		genMask.updateImage();
		
		

		System.out.println("Mask size is same");
		mask=genMask;
		return true;
		
	}
	
	public boolean loadMask()
	{
		// open file dialog, open image, test if it is binary, set genMask=loaded image
		
		ImagePlus tempMask=new ImagePlus();
		tempMask=ImageProcessUtils.openImage("Open Mask", "");
		if(tempMask==null)
		 {
			 IJ.showMessage("Filetype not recognized");
			 return false;
		 }
		tempMask.show("Mask loaded"+tempMask.getTitle());
		
		if(tempMask.getType()!=ImagePlus.GRAY8)
		{
			IJ.showMessage("ERROR: Loaded mask not 8 bit gray");
			
			return false;
		}
		
		if(isImage)
			if(tempMask.getHeight()!=Y.getHeight() || tempMask.getWidth()!=Y.getWidth() || tempMask.getNSlices()!=Y.getNSlices())
			{
				IJ.showMessage("ERROR: Loaded mask size does not match with image size");
				return false;
			}
			
	
		genMask=tempMask;
		return true;
		
	}
	
	public boolean resetMask()
	{
		mask=null;
		
		return true;
	}
	
	/**
	 * 
	 */
	public boolean cmaOptimization()
	{
		//PlotUtils.plotDoubleArray("CDF",iam.getDgrid(), IAPUtils.calculateCDF(iam.getQ_D_grid()));
		
	
	
		
		CMAMosaicObjectiveFunction fitfun=new CMAMosaicObjectiveFunction(dgrid, q_D_grid, D,potentialType);
	/*	double [] P1=fitfun.likelihood(initGuess);
		PlotUtils.plotDoubleArray("Loglikelihood", iam.getD(), P1);
		return true;*/
		
		Random rn = new Random(System.nanoTime());
		CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
		cma.readProperties(); // read options, see file CMAEvolutionStrategy.properties
//		cma.options.stopTolUpXFactor=10000;
		if(potentialType==PotentialFunctions.NONPARAM)
		{
			cma.setDimension(PotentialFunctions.NONPARAM_WEIGHT_SIZE-1);
			double [] initialX=new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE-1];
			double [] initialsigma=new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE-1];
			for(int i=0;i<PotentialFunctions.NONPARAM_WEIGHT_SIZE-1;i++)
			{
				initialX[i]=meanD*rn.nextDouble();
				initialsigma[i]=initialX[i]/3;
			}
			cma.setInitialX(initialX);
			cma.setInitialStandardDeviations(initialsigma);
		}
		
		else if(potentialType==PotentialFunctions.STEP)
		{
			cma.setDimension(2); 

			double [] initialX=new double[2];

			double [] initialsigma=new double[2];
			initialX[0]=rn.nextDouble()*5; //epsilon. average strength of 5
			
			if(meanD!=0)
			{
				initialX[1]=rn.nextDouble()*meanD;
				initialsigma[1]=initialX[1]/3;
				//	
			}
			else
			{
				initialX[1]=0;
				initialsigma[1]=1E-3;
			}	
				
		//	cma.setInitialX(initialX);
			initialsigma[0]=initialX[0]/3;

			cma.setTypicalX(initialX);
			cma.setInitialStandardDeviations(initialsigma);
		
		}
		else
		{
			
			double l []={0d,0d};
			double u []={10.00d,10.00d};
			
			cma.setDimension(2); 
			double [] initialX=new double[2];

			double [] initialsigma=new double[2];
			initialX[0]=rn.nextDouble()*5; //epsilon. average strength of 5
			
			if(meanD!=0)
			{
				initialX[1]=rn.nextDouble()*meanD;
				initialsigma[1]=initialX[1]/3;
				//	
			}
			else
			{
				initialX[1]=0;
				initialsigma[1]=1E-3;
			}	
				
		//	cma.setInitialX(initialX);
			initialsigma[0]=initialX[0]/3;

			cma.setTypicalX(initialX);
			cma.setInitialStandardDeviations(initialsigma);
		
		}
		
		
		

	//	cma.setInitialX(l, u);
		cma.options.stopFitness = 1e-5;       // optional setting
		cma.options.stopTolFun= 1e-5;
		
	//	cma.options.lowerStandardDeviations=new double[]{1e-5,1e-5};
		// initialize cma and get fitness array to fill in later
		double[] fitness = cma.init();  // new double[cma.parameters.getPopulationSize()];

		// initial output to files
		//cma.writeToDefaultFilesHeaders(0); // 0 == overwrites old files

		// iteration loop
		while(cma.stopConditions.getNumber() == 0) {

            // --- core iteration step ---
			double[][] pop = cma.samplePopulation(); // get a new population of solutions
			for(int i = 0; i < pop.length; ++i) {    // for each candidate solution i
            	// a simple way to handle constraints that define a convex feasible domain  
            	// (like box constraints, i.e. variable boundaries) via "blind re-sampling" 
            	                                       // assumes that the feasible domain is convex, the optimum is  
				while (!fitfun.isFeasible(pop[i]))     //   not located on (or very close to) the domain boundary,  
					pop[i] = cma.resampleSingle(i);    //   initialX is feasible and initialStandardDeviations are  
                                                       //   sufficiently small to prevent quasi-infinite looping here
                // compute fitness/objective value	
				fitness[i] = fitfun.valueOf(pop[i]); // fitfun.valueOf() is to be minimized
			}
			cma.updateDistribution(fitness);         // pass fitness array to update search distribution
            // --- end core iteration step ---

			// output to files and console 
			//cma.writeToDefaultFiles();
			int outmod = 150;
			if (cma.getCountIter() % (15*outmod) == 1)
				cma.printlnAnnotation(); // might write file as well
			if (cma.getCountIter() % outmod == 1)
				cma.println(); 
		}
		// evaluate mean value as it is the best estimator for the optimum
		cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates the best ever solution 

		// final output
		//cma.writeToDefaultFiles(1);
		cma.println();
		cma.println("Terminated due to");
		for (String s : cma.stopConditions.getMessages())
			cma.println("  " + s);
		cma.println("best function value " + cma.getBestFunctionValue() 
				+ " at evaluation " + cma.getBestEvaluationNumber());
		//		double [] best;
				if (potentialType==PotentialFunctions.NONPARAM)
					best=new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE-1];
				else 
					best=new double[2];
				
	
		best=cma.getBestX();
	//	System.out.println("Best parameters: Threshold, Sigma, Epsilon"+best[0]+" "+best[1]+" "+best[2]);
		System.out.println("Best params:");

		Plot plot=new Plot("Estimated potential","distance","Potential value",fitfun.getD_grid(), fitfun.getPotential(best));
		plot.setColor(Color.BLUE);
	    plot.setLineWidth(2);
	    DecimalFormat format = new DecimalFormat("#.####");
	    
		if (potentialType==PotentialFunctions.NONPARAM)
		{	
			String estim="";
			double [] dp=new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE-1];
			double [] kp=new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE-1];
			double minW=Double.MAX_VALUE,maxW=Double.MIN_VALUE;
			for (int i=0;i<PotentialFunctions.NONPARAM_WEIGHT_SIZE-1;i++)
			{
				dp[i]=PotentialFunctions.dp[i];
	//			kp[i]=kp[i]+ //print kp
				//System.out.print("w["+i+"]="+best[i]+" ");
				estim=estim+"w["+i+"]="+best[i]+" ";
				if(best[i]<minW)
					minW=best[i];
				if(best[i]>maxW)
					maxW=best[i];
			}
			System.out.println(estim);
			Plot plotWeight=new Plot("Estimated Nonparam weights","Support","Weight",new double[1],new double[1]);
			plot.addLabel(.75, .3, "Best fitness: "+fitfun.loglikelhihoodSummed(best));
			
			plotWeight.setLimits(dp[0], dp[PotentialFunctions.NONPARAM_WEIGHT_SIZE-1-1], minW,maxW );
			plotWeight.addPoints(dp, best, Plot.CROSS);
			plotWeight.setColor(Color.RED);
 
			plotWeight.setLineWidth(2);
			plotWeight.show();
		//	IJ.showMessage(estim);
		}	
		else if(potentialType==PotentialFunctions.STEP)
		{	
			best[0]=Math.abs(best[0]);//epsil
			best[1]=Math.abs(best[1]);
			IJ.showMessage("Estimated parameters: Epsilon, Threshold:"+best[0]+" "+best[1]);
			System.out.println("Best parameters: Epsilon, Threshold:"+best[0]+" "+best[1]);
			
	        plot.addLabel(.75, .3, "Strength: "+format.format(best[0]));
	        plot.addLabel(.75, .4, "Threshold: "+format.format(best[1]));
	        plot.addLabel(.75, .5, "Best fitness: "+cma.getBestFunctionValue());
	        
		}
		else
		{
			best[0]=Math.abs(best[0]);
			best[1]=Math.abs(best[1]);
			IJ.showMessage("Estimated parameters:  Epsilon, Sigma:"+best[0]+" "+best[1]);
			System.out.println("Best parameters:  Epsilon, Sigma:"+best[0]+" "+best[1]);
			plot.addLabel(.75, .3, "Strength: "+format.format(best[0]));
	        plot.addLabel(.75, .4, "Scale: "+format.format(best[1]));
	        plot.addLabel(.75, .5, "Best fitness: "+cma.getBestFunctionValue());
		}
		plot.show();
		double [] P;
		double [] gibbs;
		P=fitfun.likelihood(best);
		double [] P_grid=fitfun.getPGrid();
		//double [] D_grid=fitfun.getD_grid();
	//	double [] D=fitfun.getD();
		gibbs=fitfun.getGibbsPotential(best);	
	//	PlotUtils.plotDoubleArray("P",fitfun.getD(), P);
		//PlotUtils.plotDoubleArray("PGrid",fitfun.getD_grid(), P_grid);
	/*	double g_sum=0;
		double coloc=0;
		double [] norm_gibbs=new double[gibbs.length];
		for(int i=0;i<gibbs.length;i++)
			g_sum=g_sum+gibbs[i];
		for(int i=0;i<gibbs.length;i++)
		{
			norm_gibbs[i]=gibbs[i]/g_sum;
		}
		
		//find the index of d_max in d_grid
		double d_max=D[D.length-1];
		int index=0;
		for(int i=0;i<D_grid.length;i++)
		{
			if(D_grid[i]>d_max) //first value where
			{
				index=i;
				break;
			}	
		}
		
		for(int i=0;i<index;i++)
			coloc=coloc+gibbs[i];
		System.out.println("Sum of fit probability: "+coloc);*/
		
		
	//	PlotUtils.plotDoubleArray("Gibbs",fitfun.getD_grid(), gibbs);
		
	//	calculateZ(gibbs,fitfun.getD_grid(),iam.getQ_D_grid());
		// normalize fit probability
		
		
		
		P_grid=normalize(P_grid);
		
	/*	double [] dGrid=fitfun.getD_grid();
		int thresholdInGrid=0;
		for(int i=0;i<P_grid.length-1;i++)
		{
			if(dGrid[i]<best[1]&&dGrid[i+1]>=best[1]){
				thresholdInGrid=i;
				break;
			}
		}
		System.out.println("Threshold's position in dGrid"+thresholdInGrid);
		sum=0;
		for(int i=0;i<thresholdInGrid;i++)
			sum=sum+P_grid[i];
		System.out.println("Found coloc: "+sum);*/
		
		plotQPNN(dgrid,P_grid,q,nnObserved);
	/*	P_grid=returnNormLogofArray(P_grid);
		q=returnNormLogofArray(q);
		nnObserved=returnNormLogofArray(nnObserved);
		plotQPNN(d,P_grid,q,nnObserved);*/
        return true;
		
	}
	
	private double [] normalize(double [] array)
	{
		
		double sum=0;
		double [] retarray=new double[array.length];
		for(int i=0;i<array.length;i++)
			sum=sum+array[i];
		for(int i=0;i<array.length;i++)
			retarray[i]=array[i]/sum;
		return retarray;
	}
	
	
	
	private void plotQP(double [] d, double [] q, double [] nn)
	{
	Plot plot = new Plot("Result: Distance distributions","Distance","Probability density",d,nn);
		
		
		plot.setColor(Color.BLUE);
       // plot.setLimits(0, 1, 0, 10);
        plot.setLineWidth(2);
        plot.addLabel(.7, .2, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .2, "Observed dist");
        plot.draw();
        
        
        plot.setColor(Color.red);
        plot.addPoints(d, q, PlotWindow.LINE);
        plot.addLabel(.7, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.75, .3, "q(d): Random");
        
        
        plot.show();
		
	}
	
	private void plotQPNN(double [] d, double [] p, double [] q, double [] nn)
	{
	Plot plot = new Plot("Distance distributions","Distance","Probability density",d,nn);
		
		
		plot.setColor(Color.BLUE);
       // plot.setLimits(0, 1, 0, 10);
        plot.setLineWidth(2);
        plot.addLabel(.7, .2, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.addLabel(.75, .2, "Observed dist");
        plot.draw();
        
        
        plot.setColor(Color.red);
        plot.addPoints(d, q, PlotWindow.LINE);
        plot.addLabel(.7, .3, "----  ");
        plot.draw();
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.75, .3, "q(d): Random");
        
        plot.setColor(Color.green);
        plot.addPoints(d, p, PlotWindow.LINE);
        //plot.setLimits(-1, xMax, yMin, yMax);
        plot.addLabel(.7, .4, "----  ");
        plot.setColor(Color.black);
        plot.draw();
        plot.addLabel(.75, .4, "p(d): Model fit");
    
        
        plot.show();
		
	}
	

	public boolean hypTest(int monteCarloRunsForTest,double alpha)
	{
		if(best==null)
			return false;
		System.out.println("Running test with "+monteCarloRunsForTest+" and "+alpha);
		HypothesisTesting ht=new HypothesisTesting(IAPUtils.calculateCDF(q_D_grid), dgrid, D, D.length, best, potentialType, monteCarloRunsForTest, alpha);
		ht.rankTest();
		return true;
	}
	

	

}
