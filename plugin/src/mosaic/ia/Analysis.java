package mosaic.ia;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.measure.ResultsTable;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

import javax.vecmath.Point3d;

import mosaic.ia.nn.DistanceCalculations;
import mosaic.ia.nn.DistanceCalculationsCoords;
import mosaic.ia.nn.DistanceCalculationsImage;
import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.ImageProcessUtils;
import mosaic.ia.utils.PlotUtils;
import mosaic.paramopt.cma.CMAEvolutionStrategy;
import weka.estimators.KernelEstimator;

public class Analysis 
{
	private ImagePlus[] impList;
	private ImagePlus X, Y;
	private ImagePlus mask, genMask;
	private Point3d[] particleXSetCoordUnfiltered;
	private Point3d[] particleYSetCoordUnfiltered;

	private int dgrid_size = 1000;
	private double[] q_D_grid, NN_D_grid;
	private KernelEstimator kep; // kep for NN
	private double x1, y1, x2, y2, z1, z2;
	private int cmaReRunTimes;
	private boolean showAllRerunResults = false;
	private int bestFitnessindex=0;

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

	private double[] D; // Nearest neighbour;
	private int potentialType; // 1 is step, 2 is hernquist, 5 is nonparam
	private double[][] best;
	private double[] allFitness;
	private double kernelWeightq,kernelWeightp;
	
	private double[] q, nnObserved, dgrid;

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

	private double minD, maxD, meanD;
	private boolean isImage; // to distinguish b/wimage and coords
	// private Vector<Point> Xcoords, Ycoords;

	public void setImageList(ImagePlus X, ImagePlus Y) {
		impList = new ImagePlus[2];
		impList[0] = Y;
		impList[1] = X;
	}

	public Analysis(ImagePlus X, ImagePlus Y) {
		this.X = X;
		this.Y = Y;
		isImage = true;
		// System.out.println("1:"+X.getWidth());
	}

	public boolean isMaskSet() {
		if (mask != null)
			return true;
		return false;
	}

	public Analysis(Point3d[] Xcoords, Point3d[] Ycoords) {
		this.particleXSetCoordUnfiltered = Xcoords;
		this.particleYSetCoordUnfiltered = Ycoords;

		isImage = false;
		// System.out.println("1:"+X.getWidth());
	}

	public void setPotentialType(int potentialType) {
		this.potentialType = potentialType;
	}

	public String getMaskTitle() {
		return mask.getTitle();
	}

	public boolean getIsImage() {
		return isImage;
	}

	public boolean calcDist(double gridSize) {

		boolean ret;
		DistanceCalculations dci;
		if (isImage == true) {
			dci = new DistanceCalculationsImage(X, Y, mask, gridSize,kernelWeightq,dgrid_size);
			dci.calcDistances();

		} else {
			dci = new DistanceCalculationsCoords(particleXSetCoordUnfiltered,
					particleYSetCoordUnfiltered, genMask, x1, y1, z1, x2, y2,
					z2, gridSize,kernelWeightq,dgrid_size);
			dci.calcDistances();
		}
		D = dci.getD();
		
		
		dgrid=dci.getqOfD()[0];
		q=dci.getqOfD()[1];
		q_D_grid=q;	//q_D_grid is supposed to be not normalized - will be normalized again in CMAObj... but its OK.
		System.out.println("p set to:"+kernelWeightp);
//		System.out.println("Weka weight:"+IAPUtils.calcWekaWeights(D));
		kep = IAPUtils.createkernelDensityEstimator(D, kernelWeightp);
		
	//	ke = IAPUtils.createkernelDensityEstimator(dci.getDGrid(), kernelWeightq);
		//fillQofD_grid(dci.getDGrid());
		generateKernelDensityforD();
		ret = true;
		if (ret == true) {
			//q = normalize(q_D_grid);
			nnObserved = IAPUtils.normalize(NN_D_grid);
			plotQP(dgrid, q, nnObserved);
			PlotUtils.histPlotDoubleArray_imageJ("ObservedDistances", D,IAPUtils.getOptimBins(D, 8, (int)D.length/8));
			double[] minMaxMean = IAPUtils.getMinMaxMeanD(D);
			minD = minMaxMean[0];
			maxD = minMaxMean[1];
			meanD = minMaxMean[2];
			System.out.println("min d" + minD);
		}
		
		//plot also Q ratio
		double SumNormQ [] =new double[dgrid.length];
		double QD [] =new double[D.length];
		double n [] =new double[D.length];
		double [] D1=new double [D.length];
		D1=D;
		Arrays.sort(D1);
		SumNormQ[0]=q[0];
	//	ImageProcessUtils.saveArraytoFile(new File("/Users/arun/Documents/matlab/nonParam/q.txt"), q);
		//ImageProcessUtils.saveArraytoFile(new File("/Users/arun/Documents/matlab/nonParam/d.txt"), dgrid);
		//ImageProcessUtils.saveArraytoFile(new File("/Users/arun/Documents/matlab/nonParam/Dfile.txt"), D);
		for(int i=1;i<dgrid.length;i++)
			SumNormQ[i]=SumNormQ[i-1]+q[i];
		
		
		for(int i=0;i<D.length;i++)
		{
			for(int j=1;j<dgrid.length;j++)
			{
				if(dgrid[j-1]<=D1[i] && dgrid[j]>D1[i])
					QD[i]=IAPUtils.linearInterpolation(SumNormQ[j-1], dgrid[j-1], SumNormQ[j], dgrid[j], D1[i]);
					//(SumNormQ[j-1]+SumNormQ[j])/2;
				//	QD[i]=(SumNormQ[j-1]);
					
			}
			
			for(int k=0;k<D.length;k++)
			{
				if(D1[k]<=D1[i])
					n[i]++;
					
			}
			
			
			
				
		}
		IAPUtils.updateMacheps(); // for hyp testing...
		/*PlotUtils.plotDoubleArrayPts("dVSQNorm","d","Normalized cumulative Q", D1, QD);
		PlotUtils.histPlotDoubleArray_imageJ("QDsum", QD);
		PlotUtils.plotDoubleArrayPts("nVSQNorm","n (Number of d_i < d)","Normalized cumulative Q", n, QD);
		PlotUtils.plotDoubleArrayPts("nVSQNorm","Threshold","n (Number of d_i < d)", D1,n);
		
		for(int j=0;j<5;j++)
		{
		double v= Math.exp(j*.01+.51);
		for(int i=0;i<D.length;i++)
		{
			likRatio[i]= Math.pow(Math.pow(v, n[i]/D.length)/(1+((v-1)*QD[i])),D.length);
		}
		PlotUtils.plotDoubleArrayPts("LikelihoodRatio for strength="+(j*.01+.51), "Threshold", "Likelihood ratio for strength="+(j*.01+.51), D1, likRatio);
	
		}*/
		IJ.showMessage("Suggested Kernel wt(p): "+IAPUtils.calcWekaWeights(D));
		
		return ret;
	}
	
	private void generateKernelDensityforD() // just to run kernel density
	{

		// double min=Double.MAX_VALUE;
	
		NN_D_grid = new double[dgrid.length];
		NN_D_grid[0] = kep.getProbability(dgrid[0]);
		for (int i = 1; i < dgrid.length; i++) {
			NN_D_grid[i] = kep.getProbability(dgrid[i]);
		}
		
	}


//	private void fillQofD_grid(float D_grid[]) // just to run kernel density
//	{
//
//		// double min=Double.MAX_VALUE;
//		double min = 0;
//		double max = 0;
//
//		for (int i = 0; i < D_grid.length; i++) {
//
//			if (D_grid[i] > max)
//				max = D_grid[i];
//		}
//		// updateKernelforNonParam(min,max);
//		dgrid = new double[dgrid_size];
//		dgrid[0] = 0;
//
//		double bin_size = (max - min) / dgrid.length;
//		System.out.println("Grid bin size" + bin_size);
//		System.out.println("Grid bins length" + dgrid.length);
//
//		q_D_grid = new double[dgrid.length];
//		NN_D_grid = new double[dgrid.length];
//		q_D_grid[0] = ke.getProbability(dgrid[0]); // how does this work?
//													// q_D_grid is a histogram.
//													// how do we give the bin
//													// size to ke?
//		NN_D_grid[0] = kep.getProbability(dgrid[0]);
//		double sumProbability = 0;
//		for (int i = 1; i < dgrid.length; i++) {
//			dgrid[i] = dgrid[i - 1] + bin_size;
//			q_D_grid[i] = ke.getProbability(dgrid[i]); // how does this work?
//														// q_D_grid is a
//														// histogram. how do we
//														// give the bin size to
//														// ke?
//			NN_D_grid[i] = kep.getProbability(dgrid[i]);
//			sumProbability = q_D_grid[i] + sumProbability;
//		}
//		System.out.println("Sum of q_D grid: " + sumProbability);
//		
//	}

	public boolean calcMask() {

		genMask = new ImagePlus();
		if (Y != null) {
			genMask = ImageProcessUtils.generateMask(Y);
			return true;
		}
		// System.out.println(genMask.getType());
		return false;
	}

	public boolean applyMask() {
		if (genMask == null)
			return false;
		// new Macro_Runner().run("Convert to Mask");
		genMask.updateImage();

		System.out.println("Mask size is same");
		mask = genMask;
		return true;

	}

	public boolean loadMask() {
		// open file dialog, open image, test if it is binary, set
		// genMask=loaded image

		ImagePlus tempMask = new ImagePlus();
		tempMask = ImageProcessUtils.openImage("Open Mask", "");
		if (tempMask == null) {
			IJ.showMessage("Filetype not recognized");
			return false;
		}
		tempMask.show("Mask loaded" + tempMask.getTitle());

		if (tempMask.getType() != ImagePlus.GRAY8) {
			IJ.showMessage("ERROR: Loaded mask not 8 bit gray");

			return false;
		}

		if (isImage)
			if (tempMask.getHeight() != Y.getHeight()
					|| tempMask.getWidth() != Y.getWidth()
					|| tempMask.getNSlices() != Y.getNSlices()) {
				IJ.showMessage("ERROR: Loaded mask size does not match with image size");
				return false;
			}

		genMask = tempMask;
		return true;

	}

	public boolean resetMask() {
		mask = null;

		return true;
	}

	/**
	 * 
	 */
	public boolean cmaOptimization() {
		// PlotUtils.plotDoubleArray("CDF",iam.getDgrid(),
		// IAPUtils.calculateCDF(iam.getQ_D_grid()));

		CMAMosaicObjectiveFunction fitfun = new CMAMosaicObjectiveFunction(
				dgrid, q_D_grid, D, potentialType,IAPUtils.normalize(NN_D_grid));
		/*
		 * double [] P1=fitfun.likelihood(initGuess);
		 * PlotUtils.plotDoubleArray("Loglikelihood", iam.getD(), P1); return
		 * true;
		 */

		
		if (potentialType == PotentialFunctions.NONPARAM)
			best = new double[cmaReRunTimes][PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
		else
			best = new double[cmaReRunTimes][2];
		allFitness = new double[cmaReRunTimes];
		double bestFitness=Double.MAX_VALUE;
		ResultsTable rt = new ResultsTable();
		/*Analyzer.getResultsTable();
		if (rt == null) {
		        rt = new ResultsTable();
		        Analyzer.setResultsTable(rt);
		}
		int rowNums=rt.getCounter();
		for(int i=0;i<rowNums;i++)
			rt.deleteRow(i);
		rt.updateResults();*/
		boolean diffFitness=false;
		// cma.options.stopTolUpXFactor=10000;
		for (int k = 0; k < cmaReRunTimes; k++) {
			CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
			cma.options.writeDisplayToFile = 0;
			cma.readProperties(); // read options, see file
									// CMAEvolutionStrategy.properties
			cma.options.stopFitness = 1e-12; // optional setting
			cma.options.stopTolFun = 1e-15;
			Random rn = new Random(System.nanoTime());
			if (potentialType == PotentialFunctions.NONPARAM) {
				cma.setDimension(PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1);
				double[] initialX = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
				double[] initialsigma = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
				for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
					initialX[i] = meanD * rn.nextDouble();
					initialsigma[i] = initialX[i] / 3;
				}
				cma.setInitialX(initialX);
				cma.setInitialStandardDeviations(initialsigma);
			}

			else if (potentialType == PotentialFunctions.STEP) {
				cma.setDimension(2);

				double[] initialX = new double[2];

				double[] initialsigma = new double[2];
				initialX[0] = rn.nextDouble() * 5; // epsilon. average strength
													// of 5

				if (meanD != 0) {
					initialX[1] = rn.nextDouble() * meanD;
					initialsigma[1] = initialX[1] / 3;
					//
				} else {
					initialX[1] = 0;
					initialsigma[1] = 1E-3;
				}

				// cma.setInitialX(initialX);
				initialsigma[0] = initialX[0] / 3;

				cma.setTypicalX(initialX);
				cma.setInitialStandardDeviations(initialsigma);

			} else {

				cma.setDimension(2);
				double[] initialX = new double[2];

				double[] initialsigma = new double[2];
				initialX[0] = rn.nextDouble() * 5; // epsilon. average strength
													// of 5

				if (meanD != 0) {
					initialX[1] = rn.nextDouble() * meanD;
					initialsigma[1] = initialX[1] / 3;
					//
				} else {
					initialX[1] = 0;
					initialsigma[1] = 1E-3;
				}

				// cma.setInitialX(initialX);
				initialsigma[0] = initialX[0] / 3;

				cma.setTypicalX(initialX);
				cma.setInitialStandardDeviations(initialsigma);

			}

			// cma.setInitialX(l, u);

			// cma.options.lowerStandardDeviations=new double[]{1e-5,1e-5};
			// initialize cma and get fitness array to fill in later
			double[] fitness = cma.init(); // new
											// double[cma.parameters.getPopulationSize()];

			// initial output to files
			// cma.writeToDefaultFilesHeaders(0); // 0 == overwrites old files

			// iteration loop

			while (cma.stopConditions.getNumber() == 0) {

				// --- core iteration step ---
				double[][] pop = cma.samplePopulation(); // get a new population
															// of solutions
				for (int i = 0; i < pop.length; ++i) { // for each candidate
														// solution i
					// a simple way to handle constraints that define a convex
					// feasible domain
					// (like box constraints, i.e. variable boundaries) via
					// "blind re-sampling"
					// assumes that the feasible domain is convex, the optimum
					// is
					while (!fitfun.isFeasible(pop[i]))
						// not located on (or very close to) the domain
						// boundary,
						pop[i] = cma.resampleSingle(i); // initialX is feasible
														// and
														// initialStandardDeviations
														// are
														// sufficiently small to
														// prevent
														// quasi-infinite
														// looping here
					// compute fitness/objective value
					fitness[i] = fitfun.valueOf(pop[i]); // fitfun.valueOf() is
															// to be minimized
				}
				cma.updateDistribution(fitness); // pass fitness array to update
													// search distribution
				// --- end core iteration step ---

				// output to files and console
				// cma.writeToDefaultFiles();
				int outmod = 150;
				if (cma.getCountIter() % (15 * outmod) == 1)
					cma.printlnAnnotation(); // might write file as well
				if (cma.getCountIter() % outmod == 1)
					cma.println();
			}
			// evaluate mean value as it is the best estimator for the optimum
			cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates
																	// the best
																	// ever
																	// solution

			// final output
			// cma.writeToDefaultFiles(1);
			cma.println();
			cma.println("Terminated due to");
			for (String s : cma.stopConditions.getMessages())
				cma.println("  " + s);
			cma.println("best function value " + cma.getBestFunctionValue()
					+ " at evaluation " + cma.getBestEvaluationNumber());
			// double [] best;

					allFitness[k] = cma.getBestFunctionValue();
			if(allFitness[k]<bestFitness)
			{
				if(k>0 && bestFitness-allFitness[k]>allFitness[k]*.00001)
					diffFitness=true;
				bestFitness=allFitness[k];
				bestFitnessindex=k;
				
			}
			best[k] = cma.getBestX();
			rt.incrementCounter();
			if(potentialType!=PotentialFunctions.NONPARAM)
			{
			
			rt.addValue("Strength", best[k][0]);
			rt.addValue("Threshold/Scale", best[k][1]);
			}
			rt.addValue("Residual", allFitness[k]);
			
			
		}
		
		rt.updateResults();
		rt.show("Results");
		
		if(diffFitness)
		{
			
				IJ.showMessage("Warning: Optimization returned different results for reruns. The results may not be accurate. Displaying the parameters and the plots corr. to best fitness.");
		}
		
		
		// System.out.println("Best parameters: Threshold, Sigma, Epsilon"+best[0]+" "+best[1]+" "+best[2]);
	
		if (!showAllRerunResults) { //show only best
			double[] fitPotential = fitfun.getPotential(best[bestFitnessindex]);
			fitfun.l2Norm(best[bestFitnessindex]); 		//to calc pgrid for best params
			Plot plot = new Plot("Estimated potential", "distance",
					"Potential value", fitfun.getD_grid(), fitPotential);
			double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
			for (int i = 0; i < fitPotential.length; i++) {
				if (fitPotential[i] < min)
					min = fitPotential[i];
				if (fitPotential[i] > max)
					max = fitPotential[i];
			}

			plot.setLimits(fitfun.getD_grid()[0] - 1,
					fitfun.getD_grid()[fitfun.getD_grid().length - 1], min, max);
			plot.setColor(Color.BLUE);
			plot.setLineWidth(2);
			DecimalFormat format = new DecimalFormat("#.####E0");
	
			
			if (potentialType == PotentialFunctions.NONPARAM) {
				String estim = "";
				double[] dp = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
				double minW = Double.MAX_VALUE, maxW = Double.MIN_VALUE;
				for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
					dp[i] = PotentialFunctions.dp[i];
					// kp[i]=kp[i]+ //print kp
					// System.out.print("w["+i+"]="+best[i]+" ");
					estim = estim + "w[" + i + "]=" + best[bestFitnessindex][i] + " ";
					if (best[bestFitnessindex][i] < minW)
						minW = best[bestFitnessindex][i];
					if (best[bestFitnessindex][i] > maxW)
						maxW = best[bestFitnessindex][i];
				}
				System.out.println(estim);
				Plot plotWeight = new Plot("Estimated Nonparam weights for best fitness:",
						"Support", "Weight", new double[1], new double[1]);
				plot.addLabel(.65, .3,
						"Residual: " +format.format(allFitness[bestFitnessindex]));

				plotWeight.setLimits(dp[0],
						dp[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1 - 1],
						minW, maxW);
				plotWeight.addPoints(dp, best[bestFitnessindex], Plot.CROSS);
				plotWeight.setColor(Color.RED);

				plotWeight.setLineWidth(2);
				plotWeight.show();
				// IJ.showMessage(estim);
			} else if (potentialType == PotentialFunctions.STEP) {
				best[bestFitnessindex][0] = Math.abs(best[bestFitnessindex][0]);// epsil
				best[bestFitnessindex][1] = Math.abs(best[bestFitnessindex][1]);
				//IJ.showMessage("Estimated parameters for best fitness: Epsilon, Threshold:"
			//			+ best[0] + " " + best[bestFitnessindex][1]);
				System.out.println("Best parameters: Epsilon, Threshold:"
						+ best[0] + " " + best[bestFitnessindex][1]);

				plot.addLabel(.65, .3, "Strength: " + format.format(best[bestFitnessindex][0]));
				plot.addLabel(.65, .4, "Threshold: " + format.format(best[bestFitnessindex][1]));
				plot.addLabel(.65, .5,
						"Residual: " + format.format(allFitness[bestFitnessindex]));

			} else {
				best[bestFitnessindex][0] = Math.abs(best[bestFitnessindex][0]);
				best[bestFitnessindex][1] = Math.abs(best[bestFitnessindex][1]);
		//		IJ.showMessage("Estimated parameters for best fitness:  Epsilon, Sigma:"
			//			+ best[bestFitnessindex][0] + " " + best[bestFitnessindex][1]);
				System.out.println("Best parameters:  Epsilon, Sigma:"
						+ best[bestFitnessindex][0] + " " + best[bestFitnessindex][1]);
				plot.addLabel(.65, .3, "Strength: " + format.format(best[bestFitnessindex][0]));
				plot.addLabel(.65, .4, "Scale: " + format.format(best[bestFitnessindex][1]));
				plot.addLabel(.65, .5,
						"Residual: " + format.format(allFitness[bestFitnessindex]));
			}
			System.out.println("N= " + D.length);
			plot.show();
			double[] P_grid = fitfun.getPGrid();
		/*	int count = 0;
			for (int i = 0; i < D.length; i++) {
				if (D[i] <= best[bestFitnessindex][1]) {
					count++;
				}

			}
			System.out.println("Number of d< sigma: " + count);*/

			P_grid = IAPUtils.normalize(P_grid);

			plotQPNN(dgrid, P_grid, q, nnObserved,best[bestFitnessindex][0],best[bestFitnessindex][1],allFitness[bestFitnessindex]);

		}
		
	/*	for(int i=0;i<cmaReRunTimes;i++)
		{
			System.out.println("Best parameters:  Epsilon, Sigma:"
					+ best[i][0] + " " + best[i][1]);
	
		}*/

		return true;

	}

	

	private void plotQP(double[] d, double[] q, double[] nn) {
		String xlabel ="Distance";
		if(isImage)
		{
			xlabel=xlabel+" ("+X.getCalibration().getUnit()+")";
			
		}
		Plot plot = new Plot("Result: Estimated distance distributions", xlabel,
				"Probability density", d, nn);
		double max = 0;
		for (int i = 0; i < q.length; i++) {
			if (q[i] > max)
				max = q[i];
		}
		for (int i = 0; i < nn.length; i++) {
			if (nn[i] > max)
				max = nn[i];
		}
		plot.setLimits(d[0], d[d.length - 1], 0, max);
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
		plot.addLabel(.75, .3, "q(d): Context");
		
	
		plot.show();

	}

	private void plotQPNN(double[] d, double[] p, double[] q, double[] nn, double strength, double sigma, double fitness) {
		String xlabel="Distance";
		if(isImage)
		{
			xlabel=xlabel+" ("+X.getCalibration().getUnit()+")";
			
		}
		Plot plot = new Plot("Distance distributions", xlabel,
				"Probability density", d, nn);

		double max = 0;
		for (int i = 0; i < q.length; i++) {
			if (q[i] > max)
				max = q[i];
		}
		for (int i = 0; i < nn.length; i++) {
			if (nn[i] > max)
				max = nn[i];
		}
		for (int i = 0; i < p.length; i++) {
			if (p[i] > max)
				max = p[i];
		}
		plot.setLimits(d[0], d[d.length - 1], 0, max);
		plot.setColor(Color.BLUE);
		// plot.setLimits(0, 1, 0, 10);
		plot.setLineWidth(2);
		plot.addLabel(.65, .2, "----  ");
		plot.draw();
		plot.setColor(Color.black);
		plot.addLabel(.7, .2, "Observed dist");
		plot.draw();

		plot.setColor(Color.red);
		plot.addPoints(d, q, PlotWindow.LINE);
		plot.addLabel(.65, .3, "----  ");
		plot.draw();
		plot.setColor(Color.black);
		plot.draw();
		plot.addLabel(.7, .3, "q(d): Context");

		plot.setColor(Color.green);
		plot.addPoints(d, p, PlotWindow.LINE);
		// plot.setLimits(-1, xMax, yMin, yMax);
		plot.addLabel(.65, .4, "----  ");
		plot.setColor(Color.black);
		plot.draw();
		plot.addLabel(.7, .4, "p(d): Model fit");
		DecimalFormat format = new DecimalFormat("#.####E0");
		if(potentialType==PotentialFunctions.STEP)
		{
			plot.addLabel(.65, .6, "Strength: " + format.format(best[bestFitnessindex][0]));
			plot.addLabel(.65, .7, "Threshold: " + format.format(best[bestFitnessindex][1]));
			plot.addLabel(.65, .8,
					"Residual: " + format.format(allFitness[bestFitnessindex]));
		}
		else if(potentialType==PotentialFunctions.NONPARAM)
		{
			plot.addLabel(.65, .6,
					"Residual: " + format.format(allFitness[bestFitnessindex]));
		}	
		else
		{
			plot.addLabel(.65, .6, "Strength: " + format.format(best[bestFitnessindex][0]));
			plot.addLabel(.65, .7, "Scale: " + format.format(best[bestFitnessindex][1]));
			plot.addLabel(.65, .8,
					"Residual: " + format.format(allFitness[bestFitnessindex]));
		}
		
/*		//calculate l1norm
		double [] l1norm=new double[nn.length];
		double [] l2norm=new double[nn.length];
		double l2Sum=0, l1Sum=0;
		for(int i=0;i<l1norm.length;i++)
		{
			l1norm[i]=Math.abs(nn[i]-p[i]);
			l2norm[i]=(nn[i]-p[i])*(nn[i]-p[i]);
			l1Sum=l1Sum+l1norm[i];
			l2Sum=l2Sum+l2norm[i];
		}
			
		System.out.println("L1Norm:"+ l1Sum);

		System.out.println("L2Norm:"+ l2Sum);	*/
		/*plot.setColor(Color.magenta);
		plot.addPoints(d, l1norm, PlotWindow.LINE);
		// plot.setLimits(-1, xMax, yMin, yMax);
		plot.addLabel(.7, .5, "----  ");
		plot.setColor(Color.black);
		plot.draw();
		plot.addLabel(.75, .5, "Residual: L1Norm");


		plot.show();

		plot.setColor(Color.darkGray);
		plot.addPoints(d, l2norm, PlotWindow.LINE);
		// plot.setLimits(-1, xMax, yMin, yMax);
		plot.addLabel(.7, .6, "----  ");
		plot.setColor(Color.black);
		plot.draw();
		plot.addLabel(.75, .6, "Residual: L2Norm");
*/

		plot.show();
		
		

	}

	public boolean hypTest(int monteCarloRunsForTest, double alpha) {
		if (best == null)
		{
			IJ.showMessage("Error: Run estimation first");
		
		
		}
		else if(potentialType==PotentialFunctions.NONPARAM)	{
			IJ.showMessage("Hypothesis test is not applicable for Non Parametric potential \n since it does not have 'strength' parameter");
			return false;
		}
			
		System.out.println("Running test with " + monteCarloRunsForTest
				+ " and " + alpha);
		HypothesisTesting ht = new HypothesisTesting(
				IAPUtils.calculateCDF(q_D_grid), dgrid, D, best[bestFitnessindex],
				potentialType, monteCarloRunsForTest, alpha);
		ht.rankTest();
	//	ht.nonParametricTest();
		return true;
	}

	public void setCmaReRunTimes(int cmaReRunTimes) {
		this.cmaReRunTimes = cmaReRunTimes;
	}

	public boolean isShowAllRerunResults() {
		return showAllRerunResults;
	}

	public void setShowAllRerunResults(boolean showAllRerunResults) {
		this.showAllRerunResults = showAllRerunResults;
	}

	public double getKernelWeightq() {
		return kernelWeightq;
	}

	public void setKernelWeightq(double kernelWeightq) {
		this.kernelWeightq = kernelWeightq;
	}
	
	public double getKernelWeightp() {
		return kernelWeightp;
	}

	public void setKernelWeightp(double kernelWeightp) {
		this.kernelWeightp = kernelWeightp;
	}
	
	
	

}
