package ij.plugin;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;

public class CMAES {
	//parameters with setters and getters
	private int mNbOfThreads = 4;			//number of threads that process parallel
	private int mMaxNbOfIterations = 20;	//termination criterium
	private int mLambda = 50;				//the number of population; the number offspring
	private int mMu = 20;					//the size of the parents (number of selected search points in population)
	private long mSeed = 8;					//the seed for the random generator
	private boolean mDoRecordHistory; 		//handles if the result of all objective evaluations should be stored. 
	
	//variables
	private float mCSigma;					//learning rate for the step size control
	private float mDSigma;					//dumping parameter for the step size control
	private float mCC;						//learning rate for cumulation for rank one update
	private float mMuCov;					//weighting between rank one and rank-mu update
	private float mCCov;					//learning rate for CMA
	private float mSigma;					//the step size
	private float mMuEff;					//the variance effective selection mass
	private Matrix[] mX;					//the samples
	private double[] mW;					//the weight of the samples
	private int[] mMuBestWeightsIndices = new int[mLambda];
	private int mG; 						//generation counter
	private Matrix mCovMatrix;				//covariance matrix
	private EigenvalueDecomposition mED;	//the eigenvalue decomposition
	private Matrix mMean;					//weighted mean of the samples resp the mean parameter of the normal dist
	private Matrix[] mY;					//'normalized' sample: y = (x-mean)/sample
	private Matrix[] mZ;					//variable to speed up
	private Matrix mYw; 					//step of distribution mean disregarding the step size
	private Matrix mZw;						//variable to speed up
	private Matrix mPSigma;					//conjugated evolution path
	private Matrix mPC;						//evolution path
	private boolean mReturnMean = false;	//return the weighted mean or the best sample?
	private CMAESProblem mProblem;			//A reference to the class that implemented the function to optimize
	private int mDim;						//the dimension of the problem
	private Random mRandomGenerator = new Random(mSeed);
	private double[][][] mXHistory; 			//stores all the states evaluated. First index corresponds to the iteration.
	private double[][] mWeightHistory;		//stores the fitness value evaluation. First index indexes the iteration.
	
	/**
	 * Crates an instance of a CMA-ES optimizer. The initial covariance matrix is set to the identity and the step 
	 * size is set to 1. 
	 * @param aCMAESProblem An instance of an class implementing the interface CMAES-Problem.
	 * @param aMean the initial start point of the algorithm.
	 * @see CMAES (CMAESProblem aCMAESProblem, double[] aMean, double[][] aCovMatrix, float aSigma)
	 */
	public CMAES (CMAESProblem aCMAESProblem, double[] aMean) {
		this(aCMAESProblem, aMean, null, 1);
	}

	/**
	 * Other parameters like the number of the offspring or the seed can be set via setters.
	 * @param aCMAESProblem An instance of an class implementing the interface CMAES-Problem.
	 * @param aMean An instance of an class implementing the interface CMAES-Problem.
	 * @param aCovMatrix The initial covariance matrix.
	 * @param aSigma The initial step size.
	 * @see CMAES (CMAESProblem aCMAESProblem, double[] aMean) {
	 */
	public CMAES (CMAESProblem aCMAESProblem, double[] aMean, double[][] aCovMatrix, float aSigma) {
		mDim = aMean.length;
		mProblem = aCMAESProblem;
		mX = new Matrix[mLambda];
		mW = new double[mLambda];
		mSigma = aSigma;
		mDoRecordHistory = true;
		//
		// initialize the members
		//
		if(mDoRecordHistory) {
			mXHistory = new double[mMaxNbOfIterations][mLambda][mDim];
			mWeightHistory = new double[mMaxNbOfIterations][mLambda];
		}
		//
		// construct mean
		//
		mMean = new Matrix(mDim, 1);
		for(int vI = 0; vI < mDim; vI++) {
			mMean.set(vI, 0, aMean[vI]);
		}

		//
		// construct cov matrix
		//
		if(aCovMatrix == null){
			mCovMatrix = Matrix.identity(mDim, mDim);
		}
		else {
			mCovMatrix = new Matrix(aCovMatrix);
		}
	}

	public interface CMAESProblem{		
		public double objectiveFunction(double[] aSample);
	}
	
	private double[] getVectorFromMatrix(Matrix aMatrix, int aI) {
		double[][] vMatrix = aMatrix.getArray();
		double[] vRes = new double[vMatrix.length];
		for(int vI = 0; vI < vMatrix.length; vI++) {
			vRes[vI] = vMatrix[vI][aI];
		}
		return vRes;
	}
	
	public double[] optimzeObjectiveFunction() {
		for(mG = 0; mG < mMaxNbOfIterations; mG++) {
			//calculate the eigenvalue decomposition 
			mED = new EigenvalueDecomposition(mCovMatrix);			
			//sample new population
			sampleNewPopulation(mED.getV(), mED.getD());
			//calculate the weights
			calculateTheWeights();
			//store the evaluated fitness
			if(mDoRecordHistory) {
				storeHistory(mG);
			}
			//normalize the weights
			normalizeTheWeights(mW);
			//selection (sort the weights)
			getBestIndices();
			//recombination (calculate new mean and y_w)
			calculateStepOfDistributionMean();
			calculateMean();
			//calculate strategy parameters
			updateMuEff();
			updateStrategyParameter();
			//step size update 
			updateStepSize();
			//covariance matrix update
			updateCovMatrix();
			
		}
		//return the mean or the best fit.
		if(mReturnMean) {
			return getVectorFromMatrix(mMean, 0);
		}
		return getVectorFromMatrix(mMean, 0); //TODO:return best fit.
	}
	
	private void sampleNewPopulation(Matrix vB, Matrix vD) {
		mY = new Matrix[mLambda];
		mZ = new Matrix[mLambda];
		for(int vS = 0; vS < mLambda; vS++) {			
			for(int vI = 0; vI < mDim; vI++) {
				double vR = mRandomGenerator.nextGaussian();
				mY[vS].set(vI, 0, vR);
				mZ[vS].set(vI, 1, vR);
			}
			mY[vS] = vB.times(vD).times(mY[vS]);
			mX[vS] = mMean.plus(mY[vS].times(mSigma));
		}
	}

	private void calculateTheWeights() {
		Thread[] vThreads = new Thread[mNbOfThreads];
		mAtomicCounter.set(0);
		for(int vT = 0; vT < mNbOfThreads; vT++){
			vThreads[vT] = new ParallelizedObjectiveFunctionCalculator();
		}
		for(int vT = 0; vT < mNbOfThreads; vT++){
			vThreads[vT].start();
		}
		//wait for the threads to end.
		for(int vT = 0; vT < mNbOfThreads; vT++){
			try{
				vThreads[vT].join();
			}catch(InterruptedException aIE) {
				aIE.printStackTrace();
			}
		}
	}
	
	AtomicInteger mAtomicCounter;
	class ParallelizedObjectiveFunctionCalculator extends Thread{
		@Override
		public void run() {
			int vI;
			while((vI = mAtomicCounter.getAndIncrement()) < mLambda) {
				mW[vI] = mProblem.objectiveFunction(getVectorFromMatrix(mX[vI], 0));				
			}
		}
	}
	
	private void normalizeTheWeights(double[] aWeights) {
		double vSum = 0;
		for(double vW : aWeights) {
			vSum += vW;
		}
		for(int vI = 0; vI < aWeights.length; vI++) {
			aWeights[vI] /= vSum;
		}
	}
	
	private void getBestIndices() {		
		boolean[] vSelected = new boolean[mLambda];
		for(int vM = 0; vM < mMu; vM++) {
			int vMaxIndex = 0;
			for(int vI = 1; vI < mLambda; vI++) {
				if(mW[vI] > mW[vMaxIndex] && vSelected[vI] == false)
					vMaxIndex = vI;
			}
			vSelected[vMaxIndex] = true;
			mMuBestWeightsIndices[vM] = vMaxIndex;
		}
	}
	
	private void calculateStepOfDistributionMean() {
		mYw = new Matrix(mDim,1);
		for(int vM = 0; vM < mMu; vM++) {
			int vI = mMuBestWeightsIndices[vM];
			mYw.plusEquals(mY[vI].times(mW[vI]));
		}
	}
	
	private void calculateMean() {
		mMean.plusEquals(mYw.times(mSigma));
	}
	
	private void updateMuEff() {
		//calculate mu_eff = Sum(w_i)^(-1)
		mMuEff = 0;
		mZw = new Matrix(mDim, 1);
		for(int vM = 0; vM < mMu; vM++) {
			int vI = mMuBestWeightsIndices[vM];
			mMuEff += mW[vI] * mW[vI];	
			mZw.plusEquals(mZ[vI].times(mW[vI]));
		}
		mMuEff = 1.0f / mMuEff;
	}
	
	private void updateStrategyParameter() {
		//step size parameter
		mCSigma = (mMuEff + 2.0f) / (mDim + mMuEff + 2.0f);
		mDSigma = 1f + 2f * (float)Math.max(0f, Math.sqrt((mMuEff -1) / (mDim + 1)) - 1) + mCSigma;
		//cov matrix parameter
		mCC = 4f /(mDim + 4f);
		mMuCov = mMuEff;
		float vSqrt2 = 1.4142135624f;
		mCCov = 2f/(mMuCov * (mDim + vSqrt2) * (mDim + vSqrt2));
		mCCov += (1 - 1f / mMuCov) * Math.min(1f, (2f*mMuEff - 1f) / ((mDim+2)*(mDim+2) + mMuEff));
	}
	
	private void updateStepSize() {
		//calculate p_sigma
		double vCoeff = Math.sqrt(mCSigma * (2 - mCSigma) * mMuEff);
		mPSigma = mPSigma.times(1 - mCC).plus(mED.getV().times(mZw).times(vCoeff));
		mSigma = mSigma * (float)Math.exp((mCSigma / mDSigma) * (getNormalRandomVectorLengthE() - 1.0));
	}
	
	private void updateCovMatrix() {
		//
		// calculate the Heaviside function to avoid cov matrix problem if the step size is too small.
		//
		int hSigma = 0;
		double vA = Math.sqrt(mPSigma.transpose().times(mPSigma).trace()) / Math.sqrt(1 - Math.pow((1 - mCSigma), 2 * (mG + 1)));
		double vB = (1.4 + 2.0/(mDim + 1.0)) * getNormalRandomVectorLengthE();
		if(vA < vB) {
			hSigma = 1;
		}
		//
		// calculate pc
		//
		mPSigma = mPSigma.times(1 - mCC);
		if(hSigma == 1) {
			double vCoeff = Math.sqrt(mCC * (2 - mCC) * mMuEff);
			mPSigma.plusEquals(mYw.times(vCoeff));
		}
		//
		// update the covariance matrix
		//
		//second summand in formula (43)
		Matrix vSecond;
		if(hSigma == 1) {
			vSecond = mPC.times(mPC.transpose()).plus(mCovMatrix).times(mCCov / mMuCov);
		} else {
			vSecond = mPC.times(mPC.transpose()).times(mCCov / mMuCov);
		}
		//the third summand in formula (43)
		Matrix vWeightedSum = new Matrix(mDim, mDim);
		for(int vM = 0; vM < mMu; vM++) {
			int vI = mMuBestWeightsIndices[vM];
			vWeightedSum.plusEquals(mY[vI].times(mY[vI].transpose()).times(mW[vI]));
		}
		vWeightedSum.times(mCCov * (1.0f - (1.0f / mMuCov)));
		//the first summand in formula (43)
		mCovMatrix.timesEquals(1 - mCCov);
		//add second and third summand
		mCovMatrix.plusEquals(vSecond.plus(vWeightedSum));
	}
	
	private void storeHistory(int mG) {
		for(int vI = 0; vI < mLambda; vI++) {
			mWeightHistory[mG][vI] = mW[vI];
			mXHistory[mG][vI] = getVectorFromMatrix(mX[vI], 0);
		}
	}
	
	private double getNormalRandomVectorLengthE() {
		return Math.sqrt(mDim) * (1.0 - 1.0 /(4.0 * mDim) + 1.0 /(21.0 * mDim * mDim));
	}

	/**
	 * @return the final weights (of the last generation).
	 * @see getStates()
	 */
	public double[] getWeights() {
		return mW;
	}
	
	/**
	 * @return return the particles of the last generation. The according weights can be
	 * accessed via the method <code>getWeights</code>.
	 */
	public double[][] getStates() {
		double[][] vRes = new double[mLambda][mDim];
		for(int vI = 0; vI < mLambda; vI++) {
			vRes[vI] = getVectorFromMatrix(mX[vI], 0);
		}
		return vRes;
	}

	public int getMNbOfThreads() {
		return mNbOfThreads;
	}

	/**
	 * Sets the number of threads that are started to use while the fitness evaluations.
	 * @param nbOfThreads
	 */
	public void setMNbOfThreads(int nbOfThreads) {
		mNbOfThreads = nbOfThreads;
	}

	public int getMMaxNbOfIterations() {
		return mMaxNbOfIterations;
	}

	/**
	 * The number of iterations if no other termination criteria is achieved
	 * @param maxNbOfIterations
	 */
	public void setMMaxNbOfIterations(int maxNbOfIterations) {
		mMaxNbOfIterations = maxNbOfIterations;
	}

	/**
	 * @return The size of the population.
	 */
	public int getMLambda() {
		return mLambda;
	}

	/**
	 * @param lambda The size of the population.
	 */
	public void setMLambda(int lambda) {
		mLambda = lambda;
	}

	/**
	 * @return the size of the parents (number of selected search points in population)
	 */
	public int getMMu() {
		return mMu;
	}

	/**
	 * @param mu the size of the parents (number of selected search points in population)
	 */
	public void setMMu(int mu) {
		mMu = mu;
	}

	public boolean isMDoRecordHistory() {
		return mDoRecordHistory;
	}
	/**
	 * For a large amount of particles evaluated saving copies to all particles might use too much memory. Default is true.
	 * @param doRecordHistory
	 */
	public void setMDoRecordHistory(boolean doRecordHistory) {
		mDoRecordHistory = doRecordHistory;
	}
	
	/**
	 * Returns all states evaluated. The corrsponding weights can be accessed via method <code>getWeightsHistory()</code>.
	 * @return all states evaluated. The first index refers to the iteration, the second to the particle and the last to the states entries.
	 * @see getWeightsHistory()
	 * @see setMDoRecordHistory()
	 */
	public double[][][] getStatesHistory() {
		return mXHistory;
	}
	/**
	 * Returns the evaluated, unnormalized fitnessfunctions of each particles. The values of the particles can be accessed via the method
	 * <code>getStatesHistory()</code>.
	 * @return The fintenss function of the particles. The first index corresponds to the generation and the second to the particle index.
	 * @see getStatesHistory().
	 */
	public double[][] getWeightsHistory() {
		return mWeightHistory;
	}
}
