

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.plugin.filter.Convolver;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;
import ij.text.TextWindow;
import ij.gui.ImageCanvas;
import ij.io.FileInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import Jama.Matrix;

/**
 * <h2>PFTracking3D</h2>
 * <h3>An ImageJ Plugin base class for tracking in 4D fluorescence microscopy</h3>
 * 
 *  <p>The provided algorithm serves to model complex models with (hopefully) a lot of a-priori 
 *  knowledge of the dynamics. If you're able to crop a object to track, you might use this algorithm with 
 *  a random walk model with large variances in the dynamics distribution. The algorithm is able to handle
 *  very low signal to noise ratios (a feature point can be tracked in a 2D image till a SNR of 2.5; for 3D images the SNR
 *  might be even smaller).
 *  If you have many objects close to each other doing large and unknown movements, this is not the right choice.
 * 
 *  <p>Correctly overwriting the abstract methods should track any object. Basically, the dimensions of your state
 *  vector have to be set, and from there on, a expected image from such a state have to be created in the
 *  <code>generateIdealImage</code> method. Further you should define the proposal distribution by overwriting the method
 *  <code>drawFromProposalDistribution</code>. As an example of implementation, have a look on the <code>LinearMovementFPTracker_3D</code>
 *  class or FPTracker_3D class. 
 *  
 *  <p>The data structures provide support for multiple objects. Note that multi target tracking (MTT) only
 *  does not work properly with this algorithm if the objects are too near from each other.
 *  
 *  <p>For further informations please consider the appropriate tutorial.
 *  
 * <p> The tracks can be processed on 8-bit, 16-bit and 32-bit but no color images.
 * 
 * <p><b>Disclaimer</b>
 * <br>IN NO EVENT SHALL THE ETH BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, 
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND
 * ITS DOCUMENTATION, EVEN IF THE ETH HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * THE ETH SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. 
 * THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE ETH HAS NO 
 * OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.<p>
 * 
 * @version 1.0.03 Feb, 2009 (requires: Java 5 or higher)
 * @author Janick Cardinale - PhD student at the <a href="http://www.cbl.ethz.ch/">Computational Biophysics Lab<a>, ETH Zurich
 */

public abstract class PFTracking3D implements  PlugInFilter, CMAES.CMAESProblem{
	protected static enum STATE_OF_FILTER {WAITING, INIT, READY_TO_RUN, VISUALIZING, CORRECTING};
	protected String RESULT_FILE_SUFFIX = "_mtTracker_results.txt";
	protected String INIT_FILE_SUFFIX = "_mtTracker_initValues.txt";
	protected String COV_FILE_SUFFIX = "_mtTracker_covMatrix.txt";
	protected String PARAM_FILE_SUFFIX = "_mtTracker_params.txt";
	protected STATE_OF_FILTER mStateOfFilter = STATE_OF_FILTER.WAITING;
	
	/*
	 * Parameters
	 */
	protected int mNbThreads = 8;
	protected int mNbParticles = 200;	
//	protected int mRepSteps = 10;
	protected int mNbMCMCmoves = 60;
	protected int mNbMCMCburnInMoves = 30;//mNbMCMCmoves / 2;
	protected int mResamplingThreshold = mNbParticles / 5;
	protected float mBackground = 10f;
	protected String[] mDimensionsDescription;
	protected float mSigmaPSFxy = 110f;//219;
	protected float mSigmaPSFz = 333f;//in nm
	protected long mSeed = 8889;
	protected int mWavelengthInNm = 515;
	protected float mNA = 1.2f;
	protected float mn = 1.3f;
	protected int mTrackTillFrame = 0;
	protected int mGaussBlurRadius = 1;
	protected float mCovMatrixBackwardTimeHorizon = 1f; //approx 37% of information is older than x generation (except for x = 1)
	protected boolean mEMCCDMode = false;
	protected int mGain = 1200;
	protected int mEMStages = 500;
	protected float mEMStageSigma = 0.002f;

	/*
	 * Options
	 */
	protected boolean mDoUseTheoreticalPSF = false;
	protected boolean mDoPrintStates = true;
	protected boolean mDoGaussianBlur = false;
	

	/*
	 * Monitoring variables 
	 */
	protected boolean mDoMonitorIdealImage = false; //TODO: entfernen!
	protected boolean mDoMonitorParticles = true;
	protected ImageStack mIdealImageMonitorStack; 


	//
	// frequently used members(for simplicity and speed up)
	//
	/**
	 * Starts with 1..NFrames
	 */
	ImagePlus mPSF = null;
	protected int mStartingFrame = 1;
	protected int mHeight, mWidth, mNSlices, mNFrames;
	protected float mPxWidthInNm, mPxDepthInNm;
	protected Random mRandomGenerator = new Random(mSeed);
	protected ImagePlus mOriginalImagePlus;
	protected ImagePlus mZProjectedImagePlus;
	protected int mCurrentFrameIndex = 1; // for CMA integration
	protected ImageStack mCurrentFrame; // for CMA integration
	protected Vector<FeatureObject> mFeatureObjects = new Vector<FeatureObject>();
	private boolean mFirstDialog = true;
	protected boolean mUsePSFMap = false;
	/**
	 * The plugins setup method, invoked by imageJ
	 */
	public int setup(String aArgs, ImagePlus aImp) 
	{
		if(IJ.versionLessThan("1.38u")) {
			return DONE;
		}
		if(aImp == null) {
			IJ.showMessage("Please open an image to track first.");
			return DONE;
		}		
		
		if(aImp.getNFrames() < 2){
			IJ.run("Properties...");
		}
		
		while(true) {
			String vUnit = aImp.getCalibration().getUnit();
			if(vUnit.equals("nm")) {
				mPxWidthInNm = (float)aImp.getCalibration().pixelWidth;
				mPxDepthInNm = (float)aImp.getCalibration().pixelDepth;
				break;
			} else if(vUnit.equals(IJ.micronSymbol+"m")) {
				mPxWidthInNm = (float)aImp.getCalibration().pixelWidth * 1000;
				mPxDepthInNm = (float)aImp.getCalibration().pixelDepth * 1000;
				break;
			} else if(vUnit.equals("mm")) {
				mPxWidthInNm = (float)aImp.getCalibration().pixelWidth * 1000000;
				mPxDepthInNm = (float)aImp.getCalibration().pixelDepth * 1000000;
				break;
			}
			IJ.showMessage("Please enter the pixel sizes in nm, " + IJ.micronSymbol + "m or mm");
			IJ.run("Properties...");
		}

		//
		// Init members
		//
		mOriginalImagePlus = aImp;
		mHeight = mOriginalImagePlus.getHeight();
		mWidth = mOriginalImagePlus.getWidth();
		mNFrames = mOriginalImagePlus.getNFrames();
		mNSlices = mOriginalImagePlus.getNSlices();
		mTrackTillFrame = mNFrames;
		mStartingFrame = sliceToFrame(mOriginalImagePlus.getCurrentSlice());
		mRandomGenerator = new Random(mSeed);		
		if(mDoUseTheoreticalPSF){
			mSigmaPSFxy = (0.21f * mWavelengthInNm / mNA);
			mSigmaPSFz = (0.66f * mWavelengthInNm * mn / (mNA*mNA));
		}
		
		mDimensionsDescription = getMDimensionsDescription();
		
//		StackStatistics vSS = new StackStatistics(mOriginalImagePlus);
//		mBackground = (float)vSS.dmode;
		
		mUsePSFMap = usePSFMap(mOriginalImagePlus);
		if (!getUserDefinedParams()) return DONE;
		doZProjection();
		initMonitoring();
		initVisualization();
		
		initPlugin();
		
		if(mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN)
			return DONE;
		return DOES_8G + DOES_16 + DOES_32 + STACK_REQUIRED; //+ PARALLELIZE_STACKS; 
	}

	protected boolean usePSFMap(ImagePlus aOrigIP) {
		FileInfo vFI = aOrigIP.getOriginalFileInfo();
		File vPSFFile = new File(vFI.directory, "PSF.tif");
		if(vPSFFile.exists()){
//			System.out.println("file exists");
			try{
				mPSF = IJ.openImage(vFI.directory.concat("/PSF.tif"));
				return true;
			} catch(Exception e){
//				System.out.println("exceptaion raised:" + e.getMessage());
				return false;				
			}
		}
		return false;
	}
	
	public void run(ImageProcessor ip) 
	{
		if(	mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN ) {
			IJ.showMessage("No valid initialization. No calculation started");
			return;
		}
		//
		//DEBUG Likelihood plotter
		//
		if(false) {
			//CONFIG
			int vFrameToProcess = 3;
			int vNSamplesPerDim = 80; 
			mNbParticles = vNSamplesPerDim*vNSamplesPerDim;//*vNSamplesPerDim;
			mNFrames = 3; //heap space issue
			FeatureObject vFO = new FeatureObject(new float[]{
					4342.222f,3244.6897f,3699.7817f,
					3492.6204f,3020.8813f,3669.8538f,
					2840.188f,2373.5547f,3718.037f,
					99.25847f,107.99541f,70.88709f},vFrameToProcess);
			float vAS = 2840.188f - 60f; float vAInc = 1f; //7px x 160 nm
			float vBS = 2373.5547f - 80f; float vBInc = 1f;
			int vADim = 6;
			int vBDim = 7;
			
			//ENDCONFIG
			
			
			mFeatureObjects.add(vFO);
			//create particles
			createParticles(vFO,vFrameToProcess);

			float vA = vAS;
			float vB = vBS;
			for(int vPC = 0; vPC < mNbParticles; vPC++){
				vFO.mParticles[vPC][vADim] = vA;
				vFO.mParticles[vPC][vBDim] = vB;
				
				vA += vAInc;
				if((vPC+1)%vNSamplesPerDim==0) {
					vA = vAS;
					vB += vBInc;
				}
			}
			 
			updateParticleWeights(getAFrameCopy(mOriginalImagePlus, vFrameToProcess), vFO.mParticles);
			normalizeWeights(vFO.mParticles);
			//write the weights in a file
			BufferedWriter vW = null;
			try {
				vW = new BufferedWriter(new FileWriter(getTextFile("_TOPLOT.txt")));
				int vPCounter = 0;
				for(float [] vP : vFO.mParticles) {
					vW.write(vP[vADim] + "\t" + vP[vBDim] + "\t" + vP[12] + "\t" + vP[13] + "\n");
					vPCounter++;
//					if(vPCounter%vNSamplesPerDim == 0) {
//						vW.newLine();
//					}
				}
				
			}catch(IOException aIOE) {
				aIOE.printStackTrace();
				return;
			}
			finally {
				try { vW.close(); } catch(IOException aIOE) { 
					aIOE.printStackTrace();
					return;
				}
			}
			return;
		}
		
		//
		//END DEBUG
		//
		if (!showParameterDialog()) return;
		
		mStartingFrame = mZProjectedImagePlus.getCurrentSlice();
		
		initVisualization();
		
		runParticleFilter(mOriginalImagePlus);

		//
		// save or write data to screen
		//
		if(mDoPrintStates){
			if(mOriginalImagePlus.getOriginalFileInfo() == null){
				printStatesToWindow("Tracking states", mFeatureObjects);
			} else {
				writeResultFile(getTextFile(RESULT_FILE_SUFFIX));
				writeCovMatrixFile(getTextFile(COV_FILE_SUFFIX));
				writeParamFile(getTextFile(PARAM_FILE_SUFFIX));
			}
		}

		visualizeMonitors();
		mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
	}

	/**
	 * This method enables derived classes to initialize a new trackable object with a starting state vector.
	 * Each time this method is invoked by the derived class, a new object to track is added. Further
	 * the initialization state of the new object is set. Note that the current slice in the z projected
	 * window is the accordingly frame to the init state in the argument.
	 * @param aState
	 */
	public void registerNewObject(float[] aState) {
		mFeatureObjects.add(new FeatureObject(aState, mZProjectedImagePlus.getCurrentSlice()));
	}
	
	/**
	 * The method first checks if there are already results found in the same directory as the movie is
	 * saved. If yes, the visualizing mode is used. If not it is checked if there is a initialization file
	 * in this directory. If yes, the tracker automatically begins; this may be used to first initialize several files and
	 * then let them track in a macro. If no initialization was found, the method tries to automatically
	 * initialize using a method one may override <code>autoInitFilter</code>.
	 */
	protected void initPlugin() 
	{
		//
		// Check for a result file
		//
		FileInfo vFI = mOriginalImagePlus.getOriginalFileInfo();
		if(vFI != null && !(vFI.directory == "") ) {//|| vFI.fileName == "")) {
			//
			// Check if there are results to visualize
			//
			File vResFile = getTextFile(RESULT_FILE_SUFFIX);
			File vCovFile = getTextFile(COV_FILE_SUFFIX);
			if(vResFile.exists()) {
				//read out all the stuff; parameter too? no
				if(readResultFile(vResFile)) {
					mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
					if(vCovFile.exists()){
						readCovarianceFile(vCovFile);
					}
					return;
				}
			}		

			//
			// Check if there are init values
			///
			File vInitFile = getTextFile(INIT_FILE_SUFFIX);

			if(vInitFile.exists()) {
				//read out all the stuff; parameter too? no
				if(readInitFile(vInitFile)) {				
					mStateOfFilter = STATE_OF_FILTER.READY_TO_RUN;
					return;
				}
			}
		}
		//
		// try to init automatically
		//
		ImageStack vInitStack = getAFrameCopy(mOriginalImagePlus, mStartingFrame);
		if(autoInitFilter(vInitStack)) {
			/* Save the state vector after initialisation(the first frame is finished) */
			for(FeatureObject vFO : mFeatureObjects) {
				for(int vKeyFrame : vFO.mInitStates.keySet()) {
					vFO.mStateVectorsMemory[vKeyFrame] = copyStateVector(vFO.mInitStates.get(vKeyFrame));
				}
			}
			//we propose the initialisation but do not run
			mStateOfFilter = STATE_OF_FILTER.VISUALIZING;

			mZProjectedImagePlus.setSlice(sliceToFrame(mOriginalImagePlus.getCurrentSlice()));
			mZProjectedImagePlus.repaintWindow();
			return;
		}
		mStateOfFilter = STATE_OF_FILTER.WAITING;
	}

	protected void initVisualization()
	{
		// generate the previewCanvas - while generating it the drawing will be done 
		DrawCanvas vDrawCanvas = new DrawCanvas(mZProjectedImagePlus);

		// display the image and canvas in a stackWindow  
		new TrajectoryStackWindow(mZProjectedImagePlus, vDrawCanvas);
	}

	private void setCovMatrixAtFrameToIdentity(FeatureObject aFO, int aF) {
		int vDimOfState = aFO.getDimension();
		for(int vI = 0; vI < vDimOfState; vI++) {
			for (int vJ = 0; vJ < vDimOfState; vJ++) {
				aFO.mCovMatrix[aF][vI][vJ] = 0;
				if(vI == vJ) {
					aFO.mCovMatrix[aF][vI][vJ] = 1;
				}
			}
		}
	}

	/**
	 * To override. If youre successful, create for each object to track a <code>FeatureObject</code> and add this to 
	 * the <code>FeatureObjects</code> Vector. 
	 * @return true if successful.
	 */
	protected boolean autoInitFilter(ImageStack aImageStack)
	{
		return false;
	}

	protected void doZProjection()
	{
		ImageStack vZProjectedStack = new ImageStack(mWidth, mHeight);
		ZProjector vZProjector = new ZProjector(mOriginalImagePlus);
		vZProjector.setMethod(ZProjector.SUM_METHOD);//.MAX_METHOD);
		for(int vC = 0; vC < mOriginalImagePlus.getNFrames(); vC++){
			vZProjector.setStartSlice(vC * mNSlices + 1);
			vZProjector.setStopSlice((vC + 1) * mNSlices);
			vZProjector.doProjection();
			vZProjectedStack.addSlice("", vZProjector.getProjection().getProcessor());
		}
		mZProjectedImagePlus = new ImagePlus("Z-Projected " + mOriginalImagePlus.getTitle(), vZProjectedStack);
//		vZProjectedImage.show();
	}

	private void runParticleFilter(ImagePlus aImagePlus)
	{	
		for(int vFrameIndex = mStartingFrame; vFrameIndex <= mNFrames && vFrameIndex <= mTrackTillFrame; vFrameIndex++){
			IJ.showProgress(vFrameIndex,aImagePlus.getNFrames());	
			IJ.showStatus("Particle Filter in progress at frame: " + vFrameIndex);
		
			//
			// preprocess the frame
			//
			ImageStack vCurrentFloatFrame = getAFrameCopy(aImagePlus, vFrameIndex);
			if(mDoGaussianBlur) {
				vCurrentFloatFrame = padImageStack3D(vCurrentFloatFrame, mGaussBlurRadius);
				vCurrentFloatFrame = GaussBlur3D(vCurrentFloatFrame, mGaussBlurRadius);
				vCurrentFloatFrame = cropImageStack3D(vCurrentFloatFrame, mGaussBlurRadius);
			}
			//
			// begin processing the frame
			//
			// save a copy of the original sigma to restore it afterwards
//			float[] vSigmaOfRWSave = new float[mSigmaOfRandomWalk.length];
//			for(int vI = 0; vI < mSigmaOfRandomWalk.length; vI++) {
//				vSigmaOfRWSave[vI] = mSigmaOfRandomWalk[vI];
//			}
			for(FeatureObject vFO : mFeatureObjects) {
				//DEBUG test single particle
				if(false){
					int vNbParticles = 200;
					float[][] vParticles = new float[vNbParticles][];
					for(int i = 0; i < vNbParticles; i++){
						vParticles[0+i] = new float[]{1505.8484f,3077.6335f,2335.8364f,2296.7537f,3140.6672f,2391.5461f,3791.4932f,3779.231f,2344.6138f,50+i,61.033524f,68.07729f,0f,0f};
//						vParticles[10+i] = new float[]{1.8344e3f, 3.0877e3f-50+10*i, 2.4096e3f, 0.857e3f, 0.0015e3f, 0.0001e3f, 1.6362e3f, 0.0001e3f, 0.0003e3f, 0.1192e3f, 0.1192e3f, 0.1192e3f, 0f, 0f};
//						vParticles[20+i] = new float[]{1.8344e3f, 3.0877e3f, 2.4096e3f-50+10*i, 0.8557e3f, 0.0015e3f, 0.0001e3f, 1.6362e3f, 0.0001e3f, 0.0003e3f, 0.1192e3f, 0.1192e3f, 0.1192e3f, 0f, 0f};
//						vParticles[30+i] = new float[]{1.8344e3f, 3.0877e3f, 2.4096e3f, 0.8557e3f-50+10*i, 0.0015e3f, 0.0001e3f, 1.6362e3f, 0.0001e3f, 0.0003e3f, 0.1192e3f, 0.1192e3f, 0.1192e3f, 0f, 0f};
//						vParticles[40+i] = new float[]{1.8344e3f, 3.0877e3f, 2.4096e3f, 0.8557e3f, 0.0015e3f, 0.0001e3f, 1.6362e3f-50+10*i, 0.0001e3f, 0.0003e3f, 0.1192e3f, 0.1192e3f, 0.1192e3f, 0f, 0f};
					}
					float[][] vStackProcs = new float[mNSlices][];
					float vPxWidthInNm = getPixelWidthInNm();
					float vPxDepthInNm = getPixelDepthInNm();
					for(int vZ = 0; vZ < mNSlices; vZ++){
						vStackProcs[vZ] = (float[])vCurrentFloatFrame.getProcessor(vZ+1).getPixels();
					}
					boolean[][][] vBitmap = generateParticlesIntensityBitmap_3D(vParticles, mWidth, mHeight, mNSlices);
					for(int i = 0; i < vNbParticles; i++) {
						vParticles[i][12] = calculateLogLikelihood_3D(vStackProcs, generateIdealImage_3D(mWidth, mHeight, mNSlices, vParticles[i], (int)mBackground, vPxWidthInNm, vPxDepthInNm), 
								vBitmap);
					}
					printParticleSet(vParticles,"");
					return;
				}
				//END DEBUG
				
				
				//check if this object is already running/aborted or has a valid initialisation at the current frame
				if(vFO.checkObjectForInitializationAtFrame(vFrameIndex)) {
					//if yes, particles have to be newly created for this object!
					createParticles(vFO, vFrameIndex);
				}
				
				//abort if criterion is fullfilled, do not abort if this is an initstate(or is already aborted).
				if(!vFO.isAborted() && !vFO.checkObjectForInitializationAtFrame(vFrameIndex) && testForAbort(vFO, vFrameIndex-1) ){
					vFO.abort(); //set the flag
				}

				if(vFO.isAborted()) {
					continue; // if the flag is set, continue with the next object
				}

				float[] vNewState = new float[vFO.getDimension()];

				drawNewParticles(vFO.mParticles); //draw the particles at the appropriate position.

				updateParticleWeights(vCurrentFloatFrame,vFO.mParticles);
				normalizeWeights(vFO.mParticles);


				if(mDoMonitorParticles){
					//copy the particles history before resampling
					vFO.mParticleHistory[vFrameIndex-1][0] = copyParticleVector(vFO.mParticles);
				}
//				if(mDoResampling){
//				//TODO: what if vRepStep == 0? -->we probably lost the object
//				if(!resample(vFO.mParticles)) {//further iterations are wrong! 
////				System.out.println("Break at frame " + vFrameIndex + " at repstep " + vRepStep +".");
//				System.out.println("no resampling at frame " + vFrameIndex);
//				break;
//				}
//				}
					
				//To get a good initialization for the mcmc moves, a resampling is appropriate here
//				forcedResample(vFO.mParticles);
				performMCMCMovesWithResampling(vCurrentFloatFrame, vFO, vFrameIndex);
				
				float[][] vFinalParticleSet = integrateMCMCHistoryToParticleSet(vFO, vFrameIndex, mNbMCMCburnInMoves+1, mNbMCMCmoves);
				//TODO: The next step is wrong. it does not yield the recursive scheme of the sequential mc->to do bridging steps
//				calculateWeigthsFromLogLikelihoods(vFinalParticleSet);
				
				//the next normalization is allowed since all particles are scaled with the same factor; 
				//a normalization on a subset of the particles will do the inverse operation:
//				normalizeWeights(vFinalParticleSet);
				
//				estimateStateVector(vNewState, vFinalParticleSet);
				estimateStateVectorFromPositions(vNewState, vFinalParticleSet);
//				estimateStateVectorFromPositions(vNewState, vFO.mParticles);
				
				//after resampling, we estimate covariance matrix from the particles positions
//				forcedResample(vFinalParticleSet);
//				if(vFrameIndex==2){
//					printParticleSet(vFinalParticleSet);
//				}
				
//				vFO.mCovMatrix[vFrameIndex - 1] = estimateCovMatrixFromParticleWeights(vFinalParticleSet);
				//since the particle positions represent the likelihood distr after metropolis hastings algo, 
				//we estimate the covariance matrix from their positions:
				vFO.mCovMatrix[vFrameIndex - 1] = estimateCovMatrixFromParticlePositions(vFinalParticleSet);
				doStatistics(vFinalParticleSet, vFrameIndex);
				
//				vFO.mCovMatrix[vFrameIndex - 1] = estimateCovMatrixFromParticlePositions(vFO.mParticles);
//				updateCovMatrix(vFrameIndex, vFO, mCovMatrixBackwardTimeHorizon, true);
				
				//save the new state
				vFO.mStateVectorsMemory[vFrameIndex - 1] = copyStateVector(vNewState);
				
				//VISUALIZATION FOR DEBUGGING ETC.
//				createParticleVisualization(vFO.mParticleHistory[vFrameIndex-1][0]);
//				createParticleIntensityVisualization(vFO.mParticleHistory[vFrameIndex-1][0]);
			}
			//At the end of the frame, free memory
			IJ.freeMemory();
		}
		//
//		//DEBUG: write particles
//		//
		if(false)
		try {
			int vObjectC = 0;
			for(FeatureObject vFO : mFeatureObjects){
				vObjectC++;
				
				for(int vF = 2; vF < 3; vF++){
					int vRSC = 0;
					for(float[][] vRepStepPs : vFO.mParticleHistory[vF]) {
						vRSC++;
						BufferedWriter vBW = new BufferedWriter(new FileWriter(getTextFile("_O" + vObjectC+"_F" + vF + "_MCMCs" + vRSC + ".txt")));
						if(vRepStepPs == null) {
							continue;
						}
						for(float[] vP : vRepStepPs) {
							String vLine = "";
							for(float vV : vP) {
								vLine += vV + "\t";
							}
							vLine += "\n";
							vBW.write(vLine);
						}
						vBW.close();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected boolean createParticleVisualization(float[][] aSetOfParticles) {
		return false;
	}
	
	protected boolean createParticleIntensityVisualization(float[][] aSetOfParticles) {
		return false;
	}

	private void printParticleSet(float[][] aParticles, String aSuffix) {
		try {
			BufferedWriter vBW = new BufferedWriter(new FileWriter(getTextFile("_"+aSuffix+"_DEBUG.txt")));
			for(float[] vP : aParticles) {
				String vLine = "";
				for(float vV : vP) {
					vLine += vV + "\t";
				}
				vLine += "\n";
				vBW.write(vLine);
			}
			vBW.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	private void runParticleFilter(ImagePlus aImagePlus)
//	{	
//		for(int vFrameIndex = mStartingFrame; vFrameIndex <= mNFrames && vFrameIndex <= mTrackTillFrame; vFrameIndex++){
//			IJ.showProgress(vFrameIndex,aImagePlus.getNFrames());	
//			IJ.showStatus("Particle Filter in progress at frame: " + vFrameIndex);
//		
//			//
//			// preprocess the frame
//			//
//			ImageStack vCurrentFloatFrame = getAFrameCopy(aImagePlus, vFrameIndex);
//			if(mDoGaussianBlur) {
//				vCurrentFloatFrame = padImageStack3D(vCurrentFloatFrame, mGaussBlurRadius);
//				vCurrentFloatFrame = GaussBlur3D(vCurrentFloatFrame, mGaussBlurRadius);
//				vCurrentFloatFrame = cropImageStack3D(vCurrentFloatFrame, mGaussBlurRadius);
//			}
//			//
//			// begin processing the frame
//			//
//			// save a copy of the original sigma to restore it afterwards
//			float[] vSigmaOfRWSave = new float[mSigmaOfRandomWalk.length];
//			for(int vI = 0; vI < mSigmaOfRandomWalk.length; vI++) {
//				vSigmaOfRWSave[vI] = mSigmaOfRandomWalk[vI];
//			}
//			for(FeatureObject vFO : mFeatureObjects) {
//				//check if this object is already running/aborted or has a valid initialisation at the current frame
//				if(vFO.checkObjectForInitializationAtFrame(vFrameIndex)) {
//					//if yes, particles have to be newly created for this object!
//					createParticles(vFO, vFrameIndex);
//				}
//				
//				//abort if criterion is fullfilled, do not abort if this is an initstate(or is already aborted).
//				if(!vFO.isAborted() && !vFO.checkObjectForInitializationAtFrame(vFrameIndex) && testForAbort(vFO, vFrameIndex-1) ){
//					vFO.abort();
//				}
//				
//				if(vFO.isAborted()) {
//					continue;
//				}
//				
//				float[] vNewState = new float[vFO.getDimension()];
//				
//				for(int vRepStep = 0; vRepStep < mRepSteps; vRepStep++) {
//					if(vRepStep == 0) {
//						drawNewParticles(vFO.mParticles); //draw the particles at the appropriate position.
//					}else {
//						scaleSigmaOfRW(1f/(float)Math.pow(3, vRepStep));//(1f - (float)vRepStep / (float)mRepSteps);
//						drawParticlesForOptimization(vFO.mParticles);
//					}
//
//					updateParticleWeights(vCurrentFloatFrame,vFO.mParticles);
//					normalizeWeights(vFO.mParticles);
//					estimateStateVector(vNewState, vFO.mParticles);
//					
//					if(mDoMonitorParticles){
//						//copy the particles history before resampling
//						vFO.mParticleMonitor[vFrameIndex-1][vRepStep] = copyParticleVector(vFO.mParticles);
//					}
//					if(mDoResampling){
//						//TODO: what if vRepStep == 0? -->we probably lost the object
//						if(!resample(vFO.mParticles)) {//further iterations are wrong! 
////								System.out.println("Break at frame " + vFrameIndex + " at repstep " + vRepStep +".");
//								break;
//						}
//					}
//					
//					//after resampling, estimate(update) the covariance matrix
////					if(vRepStep == 2)//test
////						updateCovMatrix(vFrameIndex - 1, vFO, mCovMatrixBackwardTimeHorizon, true);// vRepStep == 0);
//					
//					if(!mDoLikelihoodOptimization) {
//						break; //do not repeat the filter on the first frame
//					}
//				}
//				//save the new state
//				vFO.mStateVectorsMemory[vFrameIndex - 1] = copyStateVector(vNewState);
//				
//				updateCovMatrix(vFrameIndex - 1, vFO, mCovMatrixBackwardTimeHorizon, true);
//			}
//			
//			
//			
//			//restore the sigma vector
//			for(int vI = 0; vI < mSigmaOfRandomWalk.length; vI++) {
//				mSigmaOfRandomWalk[vI] = vSigmaOfRWSave[vI];
//			}
////			if(IJ.escapePressed()) { //TODO: doesnt work!
////				break;
////			}
//			//At the end of the frame, free memory
//			IJ.freeMemory();
//		}
//		
//		//
//		//DEBUG: write particles
//		//
//		if(false)
//		try {
//			int vObjectC = 0;
//			for(FeatureObject vFO : mFeatureObjects){
//				vObjectC++;
//				BufferedWriter vBW = new BufferedWriter(new FileWriter(getTextFile("_particlesOfObject" + vObjectC + ".txt")));
//				for(int vF = 0; vF < mNFrames; vF++){
//					
//					int vRSC = 0;
//					vBW.write("Frame " + vF + "\n-----------\n");
//					for(float[][] vRepStepPs : vFO.mParticleMonitor[vF]) {
//						vRSC++;
//						if(vRepStepPs == null) {
//							continue;
//						}
//						vBW.write("RepStep " + vRSC+"\n---------------\n"); 
//						for(float[] vP : vRepStepPs) {
//							
//							
//							String vLine = "";
//							for(float vV : vP) {
//								vLine += vV + "\t";
//							}
//							vLine += "\n";
//							vBW.write(vLine);
//						}
//					}
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
//	private void runParticleFilter(ImagePlus aImagePlus)
//	{	
//		for(int vFrameIndex = mStartingFrame; vFrameIndex <= mNFrames && vFrameIndex <= mTrackTillFrame; vFrameIndex++){
//			mCurrentFrameIndex = vFrameIndex;
//			IJ.showProgress(vFrameIndex,aImagePlus.getNFrames());	
//			IJ.showStatus("Particle Filter in progress at frame: " + vFrameIndex);
//		
//			//
//			// preprocess the frame
//			//
//			ImageStack vCurrentFloatFrame = getAFrameCopy(aImagePlus, vFrameIndex);
//			if(mDoGaussianBlur) {
//				vCurrentFloatFrame = padImageStack3D(vCurrentFloatFrame, mGaussBlurRadius);
//				vCurrentFloatFrame = GaussBlur3D(vCurrentFloatFrame, mGaussBlurRadius);
//				vCurrentFloatFrame = cropImageStack3D(vCurrentFloatFrame, mGaussBlurRadius);
//			}
//			mCurrentFrame = vCurrentFloatFrame;
//			//
//			// begin processing the frame
//			//
//			// save a copy of the original sigma to restore it afterwards
//			for(FeatureObject vFO : mFeatureObjects) {
//				v
//				//check if this object is already running/aborted or has a valid initialisation at the current frame
//				if(vFO.checkObjectForInitializationAtFrame(vFrameIndex)) {
//					//if yes, particles have to be newly created for this object!
//					createParticles(vFO, vFrameIndex);
//				}
//				if(vFO.isAborted()) {
//					continue;
//				}
//				
//				float[] vNewState = new float[vFO.getDimension()];
//				
////				drawNewParticles(vFO.mParticles); //draw the particles at the appropriate position.
//				// or use CMA optimize the function and collect the particles on its way.
//				
//				CMAES vCMA = new CMAES(this,laststate,covmatrix,1);
//				//Die samples von CMA definieren eine neue proposal distribution die wir gleichzeigit
//				//schon gesampelt und evaluiert haben - entspricht also unserem neuen particle set! Das Problem
//				//q kennt x_k nicht, nur x_k^i !!
//				//q kennt die neue messung, ignoriert aber die particles
//				//und deren weights schon mal. Der prior ist gaussian. Frage: wie
//				//wird der rekursive approach beibehalten, wie kann etwas multimodal bleiben?! auf der anderen
//				//seite muss es reichen, die particles schon berechnet zu haben. es muss also gleichzeitig unser
//				//neues particle set darstellen; nicht nur die proposal distr q. 
//				
//				updateParticleWeights(vCurrentFloatFrame, vFO.mParticles);
//				normalizeWeights(vFO.mParticles);
//
//				//TODO: this should actually not be in the history -> adapt the artificial +1 in the
//				//TODO: mcmc-step index.
//				//save a copy of the particles (actually unnecessary but useful to debug)
//				vFO.mParticleHistory[vFrameIndex - 1][0] = copyParticleVector(vFO.mParticles);
//
//				if(mDoResampling){
//					if(!resample(vFO.mParticles)) {
//						System.out.println("No resampling at frame " + vFrameIndex + ".");
//					}
//				}
//
//				//after resampling, estimate(update) the covariance matrix (used for mcmc proposal density
//				updateCovMatrix(vFrameIndex - 1, vFO, mCovMatrixBackwardTimeHorizon, true);// vRepStep == 0);
//
////				performMCMCMoves(vCurrentFloatFrame, vFO, vFrameIndex);
//			
//				normalizeWeights(vFO.mParticles);
//				
//				estimateStateVector(vNewState, vFO.mParticles);
//				
//				//save the new state
//				vFO.mStateVectorsMemory[vFrameIndex - 1] = copyStateVector(vNewState);
//			}
//			
////			if(IJ.escapePressed()) { //TODO: doesnt work!
////				break;
////			}
//			//At the end of the frame, free memory
//			IJ.freeMemory();
//		}
//		
//		//
//		//DEBUG: write particles
//		//
//
//		try {
//			int vObjectC = 0;
//			for(FeatureObject vFO : mFeatureObjects){
//				vObjectC++;
//				for(int vF = 0; vF < mNFrames; vF++){
//					for(int vMCMCStep = 0; vMCMCStep < mNbMCMCmoves; vMCMCStep++) {
//						BufferedWriter vBW = new BufferedWriter(new FileWriter(getTextFile("_particles_o" + vObjectC + "_f" + vF + "_m" + vMCMCStep + ".txt")));
//						for(int vP = 0; vP < mNbParticles; vP++) {
//							for(float vV : vFO.mParticleHistory[vF][vMCMCStep][vP])
//								vBW.write(vV + "\t");
//							vBW.newLine();
//						}
//						vBW.close();
//					}
//				}
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	private void updateCovMatrix(float[][] aMatrixToUpdate, float[][] aMatrix, float aBackwardTimeHorizon) {
		
			scaleMatrix(aMatrix, 1f / aBackwardTimeHorizon);
			scaleMatrix(aMatrixToUpdate,  1f - (1f / aBackwardTimeHorizon));
			addMatrixBtoA(aMatrixToUpdate, aMatrix);
		
	}
	
	private void scaleMatrix(float[][] aMatrix, float aScale) {
		for(int vI = 0; vI < aMatrix.length; vI++) {
			for(int vJ = 0; vJ < aMatrix[vI].length; vJ++) {
				aMatrix[vI][vJ] *= aScale;
			}
		}
	}
	
	private void addMatrixBtoA(float[][] aMatrixA, float[][] aMatrixB) {
		if(aMatrixA.length != aMatrixB.length || aMatrixA[0].length != aMatrixB[0].length) {
			throw new IllegalArgumentException();
		}
		for(int vI = 0; vI < aMatrixA.length; vI++) {
			for(int vJ = 0; vJ < aMatrixA[vI].length; vJ++) {
				aMatrixA[vI][vJ] += aMatrixB[vI][vJ];
			}
		}
	}
	
	/**
	 * Enables the derived classes to do further statistics on the final particle set.
	 * @param aSetOfParticles
	 * @param aFrameIndex
	 */
	protected void doStatistics(float[][] aSetOfParticles, int aFrameIndex) {}
	
	/**
	 * Calculates a weighted covariance matrix from a set of particles. This should be the same as
	 * <code>estimateCovMatrixFromParticlePositions</code> but without resampling before.
	 * @param aPartilces normalized set of particles
	 * @return
	 */
	protected float[][] estimateCovMatrixFromParticleWeights(float[][] aPartilces) {		
		int vDimOfState = aPartilces[0].length - 2;
		float[][] vCov = new float[vDimOfState][vDimOfState];
		//
		//	calculate the weighted mean.
		//
		float[] vWeightedMean = new float[vDimOfState];
		for (float[] vParticle : aPartilces) {
			for(int vI = 0; vI < vDimOfState; vI++) {
				vWeightedMean[vI] += vParticle[vI] * vParticle[vDimOfState + 1];
			}
		}
		
		//update the matrix
		for (float[] vParticle : aPartilces){
			float[] vResidual = new float[vDimOfState];
			for(int vI = 0; vI < vDimOfState; vI++) {
				vResidual[vI] = vParticle[vI] - vWeightedMean[vI];
			}
			for(int vY = 0; vY < vDimOfState; vY++) {
				for(int vX = 0; vX < vDimOfState; vX++) {
					vCov[vY][vX] += vParticle[vDimOfState + 1] *  vResidual[vY] * vResidual[vX];
				}
			}
		}
		
		return vCov;
	}
	
	
	protected float[][] estimateCovMatrixFromParticlePositions(float[][] aPartilces) {		
		int vDimOfState = aPartilces[0].length - 2;
		int vNbParticles = aPartilces.length;
		float[][] vCov = new float[vDimOfState][vDimOfState];
		//
		//	calculate the weighted mean.
		//
		float[] vWeightedMean = new float[vDimOfState];
		for (float[] vParticle : aPartilces) {
			for(int vI = 0; vI < vDimOfState; vI++) {
				vWeightedMean[vI] += vParticle[vI] / (float)vNbParticles;// * vParticle[vDimOfState + 1];//falsch!
			}
		}
		
		//update the matrix
		for (float[] vParticle : aPartilces){
			float[] vResidual = new float[vDimOfState];
			for(int vI = 0; vI < vDimOfState; vI++) {
				vResidual[vI] = vParticle[vI] - vWeightedMean[vI];
			}
			for(int vY = 0; vY < vDimOfState; vY++) {
				for(int vX = 0; vX < vDimOfState; vX++) {
					vCov[vY][vX] += /*vParticle[vDimOfState + 1] * */ vResidual[vY] * vResidual[vX];
				}
			}
		}
		//normalize
		float vNorm = 1f/(float)(vNbParticles-1);
		for(int vY = 0; vY < vDimOfState; vY++) {
			for(int vX = 0; vX < vDimOfState; vX++) {
				vCov[vY][vX] *= vNorm;
			}
		}
		return vCov;
	}

//	protected void estimateCovMatrix(int aFrameIndex) {
//	for(FeatureObject vFO : mFeatureObjects){
//	int vDimOfState = vFO.mStateVector.length;
//			//first, normalize the weights from all the particles of a particular frame 
//			//(with all the optimization steps)
//			float[][] vLikelihoods = new float[mRepSteps][mNbParticles];
//			float vMax = Float.NEGATIVE_INFINITY;
//			for (int vJ = 0; vJ < mRepSteps; vJ++) {
//				for (int vI = 0; vI < mNbParticles; vI++) {		
//					float vNewV = vFO.mParticleMonitor.elementAt(aFrameIndex).elementAt(vJ).elementAt(vI)[vDimOfState];
//					if(vNewV > vMax) {
//						vMax = vNewV;
//					}
//				}
//			}
//			//
//			//	Store the particles in the vLikelihoods array and 
//			//	calculate the weighted mean.
//			//
//			float vNormalizer = 0f;
//			float[] vWeightedMean = new float[vDimOfState];
//			for (int vR = 0; vR < mRepSteps; vR++) {
//				for (int vP = 0; vP < mNbParticles; vP++) {	
//					float[] vParticle = vFO.mParticleMonitor.elementAt(aFrameIndex).elementAt(vR).elementAt(vP);
//					vLikelihoods[vR][vP] = (float)Math.exp(vParticle[vDimOfState]-vMax);
//					vNormalizer += vLikelihoods[vR][vP];
//				}
//			}
//			//
//			//	Normalize the weights and calculate the mean.
//			//
//			for (int vR = 0; vR < mRepSteps; vR++) {
//				for (int vP = 0; vP < mNbParticles; vP++) {	
//					for(int vI = 0; vI < vDimOfState; vI++) {
//						float[] vParticle = vFO.mParticleMonitor.elementAt(aFrameIndex).elementAt(vR).elementAt(vP);
//						vWeightedMean[vI] = vLikelihoods[vR][vP] * vParticle[vI] / vNormalizer;
//					}
//				}
//			}
//			
//			//update the matrix
//			for (int vR = 0; vR < mRepSteps; vR++) {
//				for (int vP = 0; vP < mNbParticles; vP++) {
//					float[] vParticle = vFO.mParticleMonitor.elementAt(aFrameIndex).elementAt(vR).elementAt(vP);
//					for(int vI = 0; vI < vDimOfState; vI++) {
//						vParticle[vI] -= vWeightedMean[vI];
//					}
//					for(int vY = 0; vY < vDimOfState; vY++) {
//						for(int vX = 0; vX < vDimOfState; vX++) {
//							vFO.mCovMatrix[aFrameIndex][vY][vX] += vLikelihoods[vR][vP] * vParticle[vY] * vParticle[vX];
//						}
//					}
//				}
//			}
//		}
//	}

//	/**
//	 * If one uses multiple iterations on a frame, this method scales the search radius 'sigma'. 
//	 * @param vScaler
//	 */
//	protected void scaleSigmaOfRW(float vScaler) 
//	{
//		for(int vI = 0; vI < mSigmaOfRandomWalk.length; vI++) {
//			mSigmaOfRandomWalk[vI] *= vScaler;
//		}
//	}

	/**
	 * The method creates particles at a certain frame. If there are already particles, they are overwritten. 
	 * It is necessary that the <code>FeatureObject</code> instance has an initialization entry at this frame. 
	 * @param aFO
	 * @param aFrameIndex
	 */
	private void createParticles(FeatureObject aFO, int aFrameIndex)
	{
		int vDimOfState = aFO.getDimension();
		float[] vState = aFO.mInitStates.get(aFrameIndex);
		if(vState == null)
			throw new IllegalArgumentException();
		
		for(int vPIndex = 0; vPIndex < mNbParticles; vPIndex++) {
			float[] vProposal = aFO.mParticles[vPIndex];
			for(int vI = 0; vI < vDimOfState; vI++) {
				vProposal[vI] = vState[vI];
			}
//			Init the weight as a last dimension
			vProposal[vDimOfState] = 1f; //not 0!
			vProposal[vDimOfState + 1] = 1f; //not 0!
		}
	}

	protected void initMonitoring()
	{
		if(mDoMonitorIdealImage) {					
			mIdealImageMonitorStack = new ImageStack(mWidth, mHeight);				
		}		
	}

	private void visualizeMonitors()
	{
		if(mDoMonitorIdealImage){
			ImagePlus vIdealImagePlus = new ImagePlus("Summed up ideal image", mIdealImageMonitorStack);
			new StackWindow(vIdealImagePlus);
		}

		if(mDoMonitorParticles){
			ImageStack vStack = new ImageStack(mWidth, mHeight);
			for(int vF = 0; vF < mOriginalImagePlus.getNFrames()*mNbMCMCmoves;vF++){//mRepSteps; vF++){
				vStack.addSlice("", new ByteProcessor(mWidth, mHeight));
			}
			ImagePlus vParticlesImage = new ImagePlus("Particles", vStack);
			new StackWindow(vParticlesImage, new ParticleMonitorCanvas(vParticlesImage));
			
//			for(FeatureObject vFO : mFeatureObjects)
//				new StackWindow(new ImagePlus("Particles",burnParticlesToImage(20, vFO)));
		}
	}

	/**
	 * Draws new particles
	 * @param aParticlesToRedraw The particle set to redraw.
	 */
	private void drawNewParticles(float[][] aParticlesToRedraw)
	{		
		//invoke this method here to not repeat it for every particle, pass it by argument
		float vPxW = getPixelWidthInNm();
		float vPxD = getPixelDepthInNm();
		for(float[] vParticle : aParticlesToRedraw){
			drawFromProposalDistribution(vParticle,vPxW, vPxD);
		}
	}
	
	/**
	 * performs a resampling on the given particle set
	 * @param aParticles the particle set with normalized weights
	 */
	private void forcedResample(float[][] aParticles) {
		int vDimOfState = aParticles[0].length - 2;
		int vNbParticles = aParticles.length;
		float VNBPARTICLES_1 = 1f/(float)vNbParticles;
		double[] vC = new double[vNbParticles + 1];
		vC[0] = 0;
		for(int vInd = 1; vInd <= vNbParticles; vInd++){
			vC[vInd] = vC[vInd-1] + aParticles[vInd-1][vDimOfState + 1];
		}

		double vU = mRandomGenerator.nextFloat()*VNBPARTICLES_1;

		float[][] vParticlesCopy = copyParticleVector(aParticles);
		int vI = 0;
		for(int vParticleCounter = 0; vParticleCounter < vNbParticles; vParticleCounter++){
			while(vU > vC[vI]){
				if(vI < vNbParticles) //this can happen due to numerical reasons...
					vI++;
				if(vI >= vNbParticles) //...so we also have to consider this case
					break;
			}
			for(int vK = 0; vK <= vDimOfState; vK++){ //copy all entires, even the likelihoods!
				aParticles[vParticleCounter][vK] = vParticlesCopy[vI-1][vK];
			}
			aParticles[vParticleCounter][vDimOfState + 1] = VNBPARTICLES_1;
			vU += VNBPARTICLES_1;
		}
	}
	/**
	 * 
	 * @param aParticles set of parameters to resample.
	 * @return true if resampling was performed, false if not.
	 */
	private boolean resample(float[][] aParticles)
	{		
		//
		// First check if the threshold is smaller than Neff
		//
		float vNeff = calculateNeff(aParticles);
		
		if(vNeff > mResamplingThreshold) {
			return false; 
		}
		System.out.println("Resampling");
		forcedResample(aParticles);

		return true;
	}
	
	/**
	 * Calculates the Kullback-Leibler Distance of two normal distributions with same mean.
	 * @param sigma1: Covaraince Matrix of first Gaussian
	 * @param sigma2: Covariance Matrix of second Gaussian.
	 * @return Kullback Leibler Distance of two Gaussians with same mean.
	 */
	private float calculateKLDistance(float[][] aSigma1, float[][] aSigma2) {
		float vDist = 0;	
		int vN = aSigma1.length;
		double[][] vSigma1array = new double[vN][vN];
		double[][] vSigma2array = new double[vN][vN];
		for(int vI = 0; vI < vN; vI++) {
			for(int vJ = 0; vJ < vN; vJ++) {
				vSigma1array[vI][vJ] = aSigma1[vI][vJ];
				vSigma2array[vI][vJ] = aSigma2[vI][vJ];
			}
		}
		Matrix vS1 = new Matrix(vSigma1array);
		Matrix vS2 = new Matrix(vSigma2array);
		
		
		vDist = (float)(Math.log(vS2.det()/vS1.det()) + vS2.inverse().times(vS1).trace() - vN);				
		return vDist;
	}
	
	private float calculateNeff(float[][] aParticles) 
	{
		int vDimOfState = aParticles[0].length - 2;
		float vNeff = 0;
		for(float[] vParticle : aParticles){
			vNeff += vParticle[vDimOfState + 1] * vParticle[vDimOfState + 1];
		}
		return 1f/vNeff;
	}

	/**
	 * Merges all the particles of the MCMC History from mcmc step <code>aFrom</code> to step <code>aTo-1</code>. Note that it is not
	 * a copy but only the pointers to the history of the particles in the <code>FeatureObject</code>. 
	 * @param aFO
	 * @param aFrom: where to start in the history (i = aFrom)
	 * @param aTo: where to end in the history (i <= aTo)
	 * @param aFrameIndex The index begins with 1.
	 */
	private float[][] integrateMCMCHistoryToParticleSet(FeatureObject aFO, int aFrameIndex, int aFrom, int aTo){
		float[][] vRes = new float[(aTo-aFrom+1) * mNbParticles][];
		int vPC = 0;
		for(int vI = aFrom; vI <= aTo; vI++) {
			for(float[] vP : aFO.mParticleHistory[aFrameIndex-1][vI]){
				vRes[vPC] = vP;
				vPC++;
			}
		}
		return vRes;
	}
	/**
	 * 
	 * @param aFrameIS ImageStack to calculate the weights of the particles
	 * @param aFO The FeatureObject where the particles will perform mcmc moves
	 * @param aFrameIndex The frame-index (1..N) of this current frame(to read out proposaldensity from covmatrix)
	 */
	private void performMCMCMovesWithResampling(ImageStack aFrameIS, FeatureObject aFO, int aFrameIndex) {
		int vDimOfState = aFO.getDimension();
		int vNbNotAccepted = 0;
		int vNbAccepted = 0;
		int vIterationsAfterAnnealing = 0;
		float[][] vEstimatedCovMatrixOfLastMCMCStep = null;
		//
		//The annealing is a part of the burn in phase. If the annealing does not end before the burn in phase is over, 
		//a warning is written to standard out. Annealing stops if no resampling happens ->see resampling threshold.
		//
		boolean vAnnealingPhase = true; 
		
		//
		// Generate a new set of particles (move according to proposal density)
		//
		//TODO: vIntensityBitmap was already calculated.
		boolean[][][] vIntensityBitmap = generateParticlesIntensityBitmap_3D(aFO.mParticles, mWidth, mHeight, mNSlices);
		
		//
		//Poss 1
		//
//		aFO.mCovMatrix[aFrameIndex-1] = getInitialCovarianceMatrix();
		//
		//Poss 2 (old)
		//
//		for(int vI = 0; vI < aFO.mCovMatrix[aFrameIndex-1].length; vI++){
//			aFO.mCovMatrix[aFrameIndex-1][vI][vI] = mSigmaOfRandomWalk[vI]*mSigmaOfRandomWalk[vI];
//		}
		//
		// Poss 3
		//
		aFO.mCovMatrix[aFrameIndex-1] = estimateCovMatrixFromParticlePositions(aFO.mParticles);
		System.out.println("Frame " + aFrameIndex + "\n-------------------\n");
		for(int vMCMCStep = 0; vMCMCStep < mNbMCMCmoves; vMCMCStep++) {
			float[][] vProposalSet = new float[aFO.mParticles.length][vDimOfState+2];
			
			
			System.out.println("Neff = " + calculateNeff(aFO.mParticles));
//			forcedResample(aFO.mParticles);
			/*
			if(vAnnealingPhase){
				if(resample(aFO.mParticles)) {	
					if(vMCMCStep >= mNbMCMCburnInMoves) {
						System.out.println("Warning: In Frame " + aFrameIndex + " burn in endet before resampling threshold: Track lost or burn-in too short.");
						vAnnealingPhase = false;
					}
					if(vMCMCStep > 1) { //after 2nd iteration, cool down the matrix
						//poss
						//if the variances of the user defined proposal dist was too loose, then thight it.
//						System.out.println("annealing matrix...");//						
//						scaleMatrix(aFO.mCovMatrix[aFrameIndex-1], 1f/3f); //with factor .25, sigma is decreased by factor 0.5
						//
						//Poss
//						System.out.println("Scaling matrix in annealing phase, s = "+ (float)Math.pow(vNbAccepted / (vNbNotAccepted * .25f),2));
//						scaleMatrix(aFO.mCovMatrix[aFrameIndex-1], (float)Math.pow(vNbAccepted / (vNbNotAccepted * .25f),2));
						//
						//Poss
//						System.out.println("annealing phase, do nothing, s = "+ (float)Math.pow(vNbAccepted / (vNbNotAccepted * .25f),2));
						//
						//Poss
						updateCovMatrix(aFrameIndex, aFO, 2, false);
					}
				} 
				else {
					System.out.println("no resampling at iteration: " + (vMCMCStep) + "/" + (mNbMCMCmoves));
					System.out.println("annealing ended. Setting proposal to initial distribution...");
					aFO.mCovMatrix[aFrameIndex-1] = getInitialCovarianceMatrix();
					vAnnealingPhase = false;
				}
			}
			*/
			float[][] mcmcHistory = integrateMCMCHistoryToParticleSet(aFO, aFrameIndex, Math.max(0, vMCMCStep-0), vMCMCStep);
			float[][] vEstimatedCovFromHist = estimateCovMatrixFromParticlePositions(mcmcHistory);
			if(vAnnealingPhase) {
				
				if(vMCMCStep >= mNbMCMCburnInMoves) {
//					IJ.write("Particle Filter: Warning in frame " + aFrameIndex+". There was no convergence in the annealing phase.");
					vAnnealingPhase = false;
				}
				//the resampling we have to do while annealing and also in the last annealing iteration!
				forcedResample(aFO.mParticles);
				if(vMCMCStep > 0) { //after first iteration
//					updateCovMatrix(aFrameIndex,aFO,2,false);
					
					updateCovMatrix(aFO.mCovMatrix[aFrameIndex-1], vEstimatedCovFromHist,2);
				}
			}
			
						
			float[][] vEstimatedMatrix = estimateCovMatrixFromParticlePositions(aFO.mParticles);
			
			
			float vKLDist = Float.MAX_VALUE;
			float vKLDist2 = Float.MAX_VALUE;
			try{
				vKLDist = calculateKLDistance(aFO.mCovMatrix[aFrameIndex-1], vEstimatedMatrix);
			}catch(RuntimeException vRE) {
				//TODO: not important
			}
			try{
				vKLDist2 = calculateKLDistance(aFO.mCovMatrix[aFrameIndex-1], vEstimatedCovFromHist);
			}catch(RuntimeException vRE) {
				//TODO: not important
			}
			
			if(Math.abs(vKLDist) < 1. && vAnnealingPhase) {
				vAnnealingPhase = false;
//				scaleMatrix(aFO.mCovMatrix[aFrameIndex-1], 2);
				System.out.println("annealing ended due to KL Distance.");
			}
			
			System.out.println("matrix used:\n"+matrixToString(aFO.mCovMatrix[aFrameIndex-1]));
			System.out.println("estimated cov matrix from particle set: \n" + matrixToString(vEstimatedMatrix));
			System.out.println("D_KL(Proposal || particles): " + vKLDist);
			System.out.println("D_KL(Proposal || mcmcHistory): " + vKLDist2);
			
//			//DEBUG
//			aFO.mCovMatrix[aFrameIndex-1][3][3] = 0;
//			//DEBUG end
			
			if(!vAnnealingPhase) {
				float vKLDist3 = Float.MAX_VALUE;				
				try{
					vKLDist3 = calculateKLDistance(vEstimatedMatrix, vEstimatedCovMatrixOfLastMCMCStep);
				}catch(RuntimeException vRE) {
					//TODO: not important
				}
				System.out.println("D_KL( new estimate || old estimate) = " + vKLDist3);
			}
			vEstimatedCovMatrixOfLastMCMCStep = copyCovarianceMatrix(vEstimatedMatrix);
			
//			if(!vAnnealingPhase) {
//				updateCovMatrix(aFrameIndex,aFO,2,false);				
//			}						

			
//			if(!vAnnealingPhase) {
//				vIterationsAfterAnnealing++;
//				if(vIterationsAfterAnnealing < 3) {
//					float vScaler =(float)Math.pow(vNbAccepted / (vNbNotAccepted * .3f),1); 
//					System.out.println("Scaling matrix for first real use...factor = "+ vScaler);
//					scaleMatrix(aFO.mCovMatrix[aFrameIndex-1], vScaler);
//					if(vIterationsAfterAnnealing == 2) { //already done, shortcut!
//						vMCMCStep = mNbMCMCburnInMoves;
//						System.out.println("Setting movestep to" + vMCMCStep);
//					}					
//				} else {
//					System.out.println("real MCMC move: vMCMCStep = " + vMCMCStep);
//					//calculate kullack-leibler.
//					
//					float[][] aTempSet = integrateMCMCHistoryToParticleSet(aFO, aFrameIndex, mNbMCMCburnInMoves+1, vMCMCStep);
//					float vKLDist2 = calculateKLDistance(estimateCovMatrixFromParticlePositions(aTempSet), vEstimatedMatrix);
//					System.out.println("D_KL(real estimate || current particle set) = " + vKLDist2);
//				}				
//			}
			/*
			if(!vAnnealingPhase) {				
				if(vMCMCStep == mNbMCMCburnInMoves){
					//--------------------------------------------------------------------------------------------------------------
					System.out.println("Scaling matrix for first real use...factor = "+ (float)Math.pow(vNbAccepted / (vNbNotAccepted * .25f),2));
					scaleMatrix(aFO.mCovMatrix[aFrameIndex-1], (float)Math.pow(vNbAccepted / (vNbNotAccepted * .25f),2));
					//--------------------------------------------------------------------------------------------------------------
					
					//--------------------------------------------------------------------------------------------------------------
//					System.out.println("Reestimate matrix for first real use...");
//					aFO.mCovMatrix[aFrameIndex-1] = estimateCovMatrixFromParticlePositions(aFO.mParticles);
					//--------------------------------------------------------------------------------------------------------------
					
					//set the collected statistics back to zero, they are used as a error detector of the scaling then
					vNbAccepted = 0;
					vNbNotAccepted = 0;
				}
				else if(vMCMCStep > mNbMCMCburnInMoves){
					//do controlling:
					if(vNbNotAccepted == 0 || vNbAccepted == 0 ){
						//scaling went wrong; set back to users choice
						System.out.println("scaling went wrong, setting back to initial proposal distribution...");
						aFO.mCovMatrix[aFrameIndex-1] = getInitialCovarianceMatrix();
					} else {
						System.out.println("controlling: everything fine: accepted = " + vNbAccepted +", rejected = " + vNbNotAccepted);
						vNbAccepted = 0;
						vNbNotAccepted = 0;						
					}
				}

			}
			 */
			for(int vP = 0; vP < aFO.mParticles.length; vP++) {
				//copy particles and move particles according to mcmc-proposal density
				for(int vI = 0; vI < vDimOfState; vI++) {
					vProposalSet[vP][vI] = aFO.mParticles[vP][vI] + (float)mRandomGenerator.nextGaussian() * (float)Math.sqrt(aFO.mCovMatrix[aFrameIndex-1][vI][vI]);
				}
//				for(int vI = 0; vI < vDimOfState; vI++) {
//					vProposalSet[vP][vI] = aFO.mParticles[vP][vI] + (float)mRandomGenerator.nextGaussian() * mSigmaOfRandomWalk[vI];
//				}
				
				//do also copy the likelihood and weight. 
				vProposalSet[vP][vDimOfState] = aFO.mParticles[vP][vDimOfState];
				vProposalSet[vP][vDimOfState+1] = aFO.mParticles[vP][vDimOfState+1];
			}

			//
			// Update the weights (if necessary also the weights of already existing particles)
			//
			boolean[][][] vNewIntensityBitmap = generateParticlesIntensityBitmap_3D(vProposalSet, mWidth, mHeight, mNSlices);
			if(!isSubset(vIntensityBitmap, vNewIntensityBitmap)) {
				orOperation(vIntensityBitmap, vNewIntensityBitmap);
				
				if(vAnnealingPhase) {
					//if we are in the annealing phase we can update the particle weights here since a Resampling procedure
					//was perfermed before (all the particles have the same weight).
					updateParticleWeights(aFrameIS, aFO.mParticles, vIntensityBitmap);
				} else{
					//if we are not in the annealing phase we just calculate the likelihoods and do not update the weights.
					calculateLogLikelihoods(aFrameIS, aFO.mParticles, vIntensityBitmap);
				}
			}
			if(vAnnealingPhase) {
				//if we are in the annealing phase we can update the particle weights here since a Resampling procedure
				//was perfermed before (all the particles have the same weight). We must overwrite the weights in order
				//to prepare for the next resampling procedure.
				updateParticleWeights(aFrameIS, vProposalSet, vIntensityBitmap);
			} else{
				//if we are not in the annealing phase we just calculate the likelihoods and do not update the weights.
				calculateLogLikelihoods(aFrameIS, vProposalSet, vIntensityBitmap);
			}
			
			//
			//decision to move based on likelihoods only:
			//
			vNbAccepted = 0;
			vNbNotAccepted = 0;
			for(int vI = 0; vI < vProposalSet.length; vI++) {
//				float vA = vProposalSet[vI][vDimOfState + 1] / aFO.mParticles[vI][vDimOfState+1];
				//Do not just use the weight due to numerical reason: they are close to 0 
				float vLogLikOld = aFO.mParticles[vI][vDimOfState];// - Math.max(aFO.mParticles[vI][vDimOfState], vProposalSet[vI][vDimOfState]);
				float vLogLikNew = vProposalSet[vI][vDimOfState];// - Math.max(aFO.mParticles[vI][vDimOfState], vProposalSet[vI][vDimOfState]);
//				float vNormalizer = vLogLikOld + vLogLikNew;
//				vLogLikNew -= vNormalizer;
//				vLogLikOld -= vNormalizer;
				
				//since we are operating on particles with equal weights and the proposal distribution is the same as the 
				//prior, we can only use the likelihood to decide if the move happens or not.
				float vA = (float)Math.exp(vLogLikNew - vLogLikOld);
				
				boolean vMove = false;
				if(vA >= 1) {
					vMove = true;
					vNbAccepted++;
				} else {
					if(vA >= mRandomGenerator.nextFloat()) {
						vMove = true;
						vNbAccepted++;
					} else {
						vMove = false;
						vNbNotAccepted++;
					}
				}
				float[] vWinner = null;
				if(vMove) {
					vWinner = vProposalSet[vI];
					aFO.mParticles[vI] = vWinner;
				} else {
					vWinner = aFO.mParticles[vI];
				}
				
				//
				//update history
				//
//				aFO.mParticleHistory.elementAt(aFrameIndex).elementAt(vI).setElementAt(copyStateVector(vWinner), vMCMCStep);
			}
			normalizeWeights(aFO.mParticles);
			aFO.mParticleHistory[aFrameIndex - 1][vMCMCStep + 1] = copyParticleVector(aFO.mParticles);
			System.out.println("Frame " + aFrameIndex + ", MCMCstep " + vMCMCStep +": Accepted: " + vNbAccepted + ", not accepted: " + vNbNotAccepted);
		}
	}

	/**
	 * Tests if all true values in the 3D bitmap <code>aSubset</code> is a subset of all
	 * true values in the 3D bitmap <code>aArray</code>.
	 */
	private boolean isSubset(boolean[][][] aArray, boolean[][][] aSubset) {
		int vLX = aArray[0][0].length;
		int vLY = aArray[0].length;
		int vLZ = aArray.length;
		for(int vZ = 0; vZ < vLZ; vZ++) {
			for(int vY = 0; vY < vLY; vY++) {
				for(int vX = 0; vX < vLX; vX++) {
					if(aSubset[vZ][vY][vX]) {
						if(!aArray[vZ][vY][vX]) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Performs the or operation on 3D bitmaps. The result is stored in the first bitmap.
	 * @param aArray The result
	 * @param aOperand The array to OR
	 */
	private void orOperation(boolean[][][] aArray, boolean[][][] aOperand) {
		int vLX = aArray[0][0].length;
		int vLY = aArray[0].length;
		int vLZ = aArray.length;
		for(int vZ = 0; vZ < vLZ; vZ++) {
			for(int vY = 0; vY < vLY; vY++) {
				for(int vX = 0; vX < vLX; vX++) {
					if(!aArray[vZ][vY][vX]) {
						aArray[vZ][vY][vX] = aOperand[vZ][vY][vX];
					}
				}
			}
		}
	}
	
	
	private void updateParticleWeights(ImageStack aObservationStack, float[][] aSetOfParticles) {
		boolean[][][] vBitmap = generateParticlesIntensityBitmap_3D(aSetOfParticles, mWidth, mHeight, mNSlices);
		updateParticleWeights(aObservationStack, aSetOfParticles, vBitmap);
	}
	
		
	private void updateParticleWeights(ImageStack aObservationStack, float[][] aSetOfParticles, boolean[][][] aROIBitmap)
	{
		calculateLogLikelihoods(aObservationStack, aSetOfParticles, aROIBitmap);
		calculateWeigthsFromLogLikelihoods(aSetOfParticles);
	}		
	
	/**
	 * Updates the weights of the particles in the sequential Monte Carlo scheme. Note, that first the likelihood entries
	 * have to be updated.
	 * @param aParticleSet
	 */
	private void calculateWeigthsFromLogLikelihoods(float[][] aParticleSet){
		int vDimOfState = aParticleSet[0].length - 2;
		float vMaxLogLikelihood = Float.NEGATIVE_INFINITY;
		
		//
		// Search max to maintain numerical stability
		//
		for(int vI = 0; vI < mNbParticles; vI++) {				
			if(aParticleSet[vI][vDimOfState] > vMaxLogLikelihood){
				vMaxLogLikelihood = aParticleSet[vI][vDimOfState];
			}
		}
		
		// sum up the likelihoods
		float vLogLikSum = 0;
		for(float[] vParticle : aParticleSet){
			vLogLikSum += Math.exp(vParticle[vDimOfState] - vMaxLogLikelihood);
		}
		vLogLikSum = (float) Math.log(vLogLikSum);
		
		//
		// Iterate again and update the weights with normalized likelihoods
		//
		for(float[] vParticle : aParticleSet){
			vParticle[vDimOfState + 1] = vParticle[vDimOfState + 1] * (float)Math.exp(vParticle[vDimOfState] - vLogLikSum - vMaxLogLikelihood);
		}
	}

	/**
	 * The log-likelihood entries of aSetOfParticles will be updated/calculated.
	 * @param aObservationStack
	 * @param aSetOfParticles
	 * @param aROIBitmap
	 * @see updateParticleWeights
	 */
	private void calculateLogLikelihoods(ImageStack aObservationStack, float[][] aSetOfParticles, boolean[][][] aROIBitmap) {
		//
		// Calculate the likelihoods for each particle and save the biggest one
		//
		boolean[][][] vBitmap = aROIBitmap;
		Thread[] vThreads = new Thread[mNbThreads];
		for(int vT = 0; vT < mNbThreads; vT++){
			vThreads[vT] = new ParallelizedLikelihoodCalculator(aObservationStack, vBitmap, aSetOfParticles);
		}
		for(int vT = 0; vT < mNbThreads; vT++){
			vThreads[vT].start();
		}
		//wait for the threads to end.
		for(int vT = 0; vT < mNbThreads; vT++){
			try{
				vThreads[vT].join();
			}catch(InterruptedException aIE) {
				IJ.showMessage("Not all particles calculated, the tracking might be wrong.");
			}
		}
	}

	private void normalizeWeights(float[][] aSetOfParticles) {
		float vSumOfWeights = 0;
		int vDim = aSetOfParticles[0].length - 2;
		float vNbP = (float)aSetOfParticles.length;
		for(float[] vP : aSetOfParticles) {
			vSumOfWeights += vP[vDim + 1];
		}
		if(vSumOfWeights == 0.0f) { //can happen if the winning particle before had a weight of 0.0
			for(float[] vParticle : aSetOfParticles){
				vParticle[vDim + 1] = 1.0f / vNbP;
			}
		} else {
			for(float[] vParticle : aSetOfParticles){
				vParticle[vDim + 1] /= vSumOfWeights;
			}
		}
	}


	/**
	 * Estimates all state vectors from the particles and their weights	 
	 */
	private void estimateStateVector(float[] aStateVec, float[][] aParticles)
	{
		int vDim = aStateVec.length;
		/* Set the old state to 0*/
		for(int vI = 0; vI < vDim; vI++)
			aStateVec[vI] = 0f;

		for(float[] vParticle : aParticles){
			for(int vD = 0; vD < vDim; vD++) {
				aStateVec[vD] += vParticle[vDim + 1] * vParticle[vD];
			}
		}
	}
	
	/**
	 * Estimates all state vectors from the particles (without weights, only according to their position)
	 */
	private void estimateStateVectorFromPositions(float[] aStateVec, float[][] aParticles)
	{
		int vDim = aStateVec.length;
		float vW = 1f/(float)aParticles.length;
		/* Set the old state to 0*/
		for(int vI = 0; vI < vDim; vI++)
			aStateVec[vI] = 0f;

		for(float[] vParticle : aParticles){
			for(int vD = 0; vD < vDim; vD++) {
				aStateVec[vD] += vW * vParticle[vD];
			}
		}
	}

	public double objectiveFunction(double[] aSample) {
		getCurrentFrameIndex();
		
		float[] vSample = new float[aSample.length];
		for(int vI = 0; vI < aSample.length; vI++) {
			vSample[vI] = (float)aSample[vI];
		}
		//calculate ideal image
		float[][][] vIdealImage = generateIdealImage_3D(mWidth, 
				mHeight,
				mNSlices,
				vSample,
				(int)(mBackground + .5),
				mPxWidthInNm,
				mPxDepthInNm);	
		
		float[][] vStackProcs = new float[mNSlices][mHeight * mWidth];
		                                    
		for(int vZ = 0; vZ < mNSlices; vZ++){
			vStackProcs[vZ] = (float[])mCurrentFrame.getProcessor(vZ+1).getPixels();
		}
		//calculate likelihood
		if(!mEMCCDMode) {
			return calculateLogLikelihood_3D_CMA(vStackProcs, vIdealImage);
		}
		return calculateEMCCDLogLikelihood3D_CMA(vStackProcs, vIdealImage);
		
	}

	protected boolean [][][] mIntensityBitmap = null;
	/**
	 * Override this method to speed up the algorithm (useful in 3D images). 
	 * It may be useful to fill the member <code>mIntensityBitmap</code> in the
	 * <code>generateIdealImage_3D(...)</code> method and return it here.
	 * @param aSetOfParticles
	 * @return
	 */
	protected boolean[][][] generateParticlesIntensityBitmap_3D(float[][] aSetOfParticles, int aW, int aH, int aS) 
	{
		if(mIntensityBitmap == null) {
			mIntensityBitmap = new boolean[aS][aH][aW];
			for(int vZ = 0; vZ < aS;vZ++) {
				for(int vY = 0; vY < aH; vY++) {
					for(int vX = 0; vX < aW; vX ++) {
						mIntensityBitmap[vZ][vY][vX] = true;
					}
				}
			}
		}
		return mIntensityBitmap;
	}

	/**
	 * This method should be overrided. It declares an initial guess for the covariance matrix of the gaussian MCMC proposal distribution.
	 * Or, since this distribution should in the optimal case be the same as the posterior distr,
	 * this can also declare a initial guess of the accuracy of the final result.
	 * @param aFO
	 * @return
	 */
//	protected abstract float[][] getInitialCovarianceMatrix(); //TODO: raus.
	
	/**
	 * A test for the derived class to abort the tracking till a new initialization state
	 * is found at a later frame (or till end). The test is only  fullfilled, if the tracker
	 * was running.
	 * For example the <code>mStateVectorsMemory[aFrameIndex-1]</code> is a defined array.
	 * for meaningful entries.
	 * @param vFO The feature object to look at.
	 * @param aFrameIndex The frame index of the last tracked position beginning with 1.
	 * @return is there a reason to abort(like meaningless state vector at <code>aFrameIndex</code>.
	 */
	protected boolean testForAbort(FeatureObject vFO, int aFrameIndex){
		return false;
	}
	/**
	 * Generates an artificial 3D image. Override this method. Do not forget to add background(minimal value of a voxel is 1.0).
	 * @param aW: The width of the image to generate
	 * @param aH: The height of the image to generate
	 * @param aS: The number of slices of the image to generate
	 * @param aParticle: the particle that describes the state
	 * @param aBackground: background intensity to add
	 * @param aPxDepthInNm
	 * @param aPxWidthInNm
	 * @return a 3D image
	 */	
	protected abstract float[][][] generateIdealImage_3D(int aW, int aH, int aS, float[] aParticle, int aBackground, float aPxWidthInNm, float aPxDepthInNm);
	/**
	 * The plugin can draw on the canvas.
	 * @param aG: The graphics object to draw on.
	 * @param aMaginification: The magnification factor used by the user.
	 * @param aActiveFrame: The frame currently selected by the user.
	 */
	protected abstract void paintOnCanvas(Graphics aG, float[] aState, double aMagnification);

	/**
	 * Calculates the likelihood by multipling the poissons marginals around a particle given a image(optimal image)
	 * @param aImagePlus: The observed image
	 * @param aFrame: The frame index 1<=n<=NSlices(to read out the correct substack from aStack
	 * @param aGivenImage: a intensity array, the 'measurement'
	 * @param aBitmap: Pixels which are not set to true in the bitmap are pulled out(proportionality)
	 * @return the likelihood for the image given 
	 */
	private float calculateLogLikelihood_3D(float[][] aObservation, float[][][] aGivenImage, boolean[][][] aBitmap) //ImageStack aImageStack){
	{
		float vLogLikelihood = 0;		
		//we need all processors anyway. Profiling showed that the method getProcessor needs a lot of time. Store them
		//in an Array.
		
//		long vTime1 = System.currentTimeMillis();
		for(int vZ = 0; vZ < mNSlices; vZ++){
			for(int vY = 0; vY < mHeight; vY++){
				for(int vX = 0; vX < mWidth; vX++){			
					if(aBitmap[vZ][vY][vX]){
						vLogLikelihood += -aGivenImage[vZ][vY][vX] + (float)aObservation[vZ][vY*mWidth+vX] * (float)Math.log(aGivenImage[vZ][vY][vX]);
//						if(Float.isNaN(vLogLikelihood)){
//							System.out.println("NAN at vz = " + vZ +", vY = " + vY + ", vX = " + vX);
//						}
					}
				}
			}
		}
//		System.out.println("used time for loglik = " + (System.currentTimeMillis() - vTime1));
		return vLogLikelihood;
	}
	/**
	 * 
	 * @param aObservation: the observation image 
	 * @param aGivenImage: the given image, i.e. the ideal image or the expectation
	 * @param aBitmap
	 * @return
	 */
	private float calculateEMCCDLogLikelihood3D(float[][] aObservation, float[][][] aGivenImage, boolean[][][] aBitmap) 
	{
		float vLogLikelihood = 0;		
		//the squared excess noise factor
		float vENF2 = 1f;
		if(mGain >= 2) {
			//TODO: the if-statement is too expensive here.
			vENF2 = 2f - 1f/mGain + mEMStageSigma * mEMStages * (1f - 1f/mGain) / (float)Math.log(mGain);
		} 
		for(int vZ = 0; vZ < mNSlices; vZ++){
			for(int vY = 0; vY < mHeight; vY++){
				for(int vX = 0; vX < mWidth; vX++){			
					if(aBitmap[vZ][vY][vX]){
						vLogLikelihood += -(Math.pow(aObservation[vZ][vY*mWidth+vX] - mGain*aGivenImage[vZ][vY][vX],2)/
								(2f * vENF2 * mGain * mGain * aGivenImage[vZ][vY][vX])); 
						if(Float.isNaN(vLogLikelihood)){
							System.out.println("NAN at (vz = " + vZ +", vY = " + vY + ", vX = " + vX + "); aObservation = " + aObservation[vZ][vY*mWidth+vX] + ", aGivenImage = " + aGivenImage[vZ][vY][vX]);
						}
					}
				}
			}
		}
		return vLogLikelihood;
	}
	
	private float calculateLogLikelihood_3D_CMA(float[][] aObservation, float[][][] aGivenImage) //ImageStack aImageStack){
	{
		float vLogLikelihood = 0;		
		for(int vZ = 0; vZ < mNSlices; vZ++){
			for(int vY = 0; vY < mHeight; vY++){
				for(int vX = 0; vX < mWidth; vX++){			
					vLogLikelihood += -aGivenImage[vZ][vY][vX] + (float)aObservation[vZ][vY*mWidth+vX] * (float)Math.log(aGivenImage[vZ][vY][vX]);
				}
			}
		}
		return vLogLikelihood;
	}
	private float calculateEMCCDLogLikelihood3D_CMA(float[][] aObservation, float[][][] aGivenImage) 
	{
		float vLogLikelihood = 0;		
		float vENF2 = 1f;//the squared excess noise factor
		if(mGain >= 2) {
			//TODO: the if-statement is too expensive here.
			vENF2 = 2f - 1f/mGain + mEMStageSigma * mEMStages * (1f - 1f/mGain) / (float)Math.log(mGain);
		} 
		for(int vZ = 0; vZ < mNSlices; vZ++){
			for(int vY = 0; vY < mHeight; vY++){
				for(int vX = 0; vX < mWidth; vX++){			
					vLogLikelihood += -(Math.pow(aObservation[vZ][vY*mWidth+vX] - mGain*aGivenImage[vZ][vY][vX],2)/
							(2f * vENF2 * mGain * mGain * aGivenImage[vZ][vY][vX])); 
					if(Float.isNaN(vLogLikelihood)){
						System.out.println("NAN at (vz = " + vZ +", vY = " + vY + ", vX = " + vX + "); aObservation = " + aObservation[vZ][vY*mWidth+vX] + ", aGivenImage = " + aGivenImage[vZ][vY][vX]);
					}
				}
			}
		}
		return vLogLikelihood;
	}
	
	/**
	 * Override this method to include the apriori knowledge of the dynamics of your system.
	 * @param aParticle A possible state. 
	 * @param aPxWidthInNm Pixel width in nano meter to not use the getter function since it is called very often.
	 * @param aPxDepthInNm Pixel depth in nano meter to not use the getter function since it is called very often.
	 */
	abstract protected void drawFromProposalDistribution(float[] aParticle, float aPxWidthInNm, float aPxDepthInNm);

	/**
	 * Converts a slice index to a frame index using the parameters of the image defined by the user.
	 * @param aSlice
	 * @return frame index beginning with 1.
	 */
	protected int sliceToFrame(int aSlice){
		if(aSlice < 1)
			System.err.println("wrong argument in particle filter in SliceToFrame: < 1" );
		return (int)(aSlice-1)/mOriginalImagePlus.getNSlices() + 1;
	}

//	/**
//	 * Override this model if you don't want to use CMA.
//	 * @param aParticles
//	 */
//	protected void drawParticlesForOptimization(float[][] aParticles) {
//		float vPxW = getPixelWidthInNm();
//		float vPxD = getPixelDepthInNm();
//		//TODO: integrate CMA
//		for(float[] vP : aParticles) {
//			randomWalkProposal(vP, vPxW, vPxD);
//		}
//	}
//	/**
//	 * Draws new particles, the last 2 entries of the argument are supposed to be the likelihood resp the weight 
//	 * and remain unchanged.
//	 * @param aParticle A Array with state vector entries + a weight entry(remains unchanged)
//	 */
//	private void randomWalkProposal(float[] aParticle, float aPxWidthInNm, float aPxDepthInNm){
//		for(int aI = 0; aI < aParticle.length - 2; aI++) {
//			aParticle[aI] += (float)mRandomGenerator.nextGaussian() * mSigmaOfRandomWalk[aI];
//		}
//	}	

	
	
	/**
	 * Checks if the movie/image is saved somewhere and returns the possible result/init/cov file at the same place.
	 * @return null if the file(image) is not yet saved, else it returns the new textfile(even if it does not exist).
	 */
	protected File getTextFile(String aSuffix) 
	{
		FileInfo vFI = mOriginalImagePlus.getOriginalFileInfo();
		if(vFI == null) {
			return null;
		}
		String vResFileName = new String(vFI.fileName);
		int vLastInd = vResFileName.lastIndexOf(".");
		if(vLastInd != -1)
			vResFileName = vResFileName.substring(0, vLastInd);
		vResFileName = vResFileName.concat(aSuffix);

		File vResFile = new File(vFI.directory, vResFileName);
		return vResFile;
	}

	protected boolean writeTextFile(StringBuffer aText, File aFile) {
		BufferedWriter vW = null;
		try {
			vW = new BufferedWriter(new FileWriter(aFile));
			vW.write(aText.toString()); 
		}catch(IOException aIOE) {
			aIOE.printStackTrace();
			return false;
		}
		finally {
			try { vW.close(); } catch(IOException aIOE) { 
				aIOE.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * Writes the results to the result file.
	 * @see getResultFile()
	 * @param aFile
	 * @return true if successful, false if not.
	 */
	protected boolean writeCovMatrixFile(File aFile)
	{
		StringBuffer vText = new StringBuffer();
		for(FeatureObject vFO : mFeatureObjects) {
			int vDimOfState = vFO.getDimension();
			for(int vF = 0; vF < mNFrames; vF++){
				vText.append("Frame " + (vF + 1) + ":\n\n");
				for(int vI = 0; vI < vDimOfState; vI++) {
					vText.append("\n");
					for(int vJ = 0; vJ < vDimOfState; vJ++) {
						vText.append(vFO.mCovMatrix[vF][vI][vJ] + "\t");
					}
				}
				vText.append("\n\n");
			}
		}
		return writeTextFile(vText,aFile);
	}

	/**
	 * Writes the initialization to disk. Using this information, batch processing movies can be done using macros.
	 * @param aFile
	 * @return true if successful, false if not.
	 */
	protected boolean writeInitFile(File aFile)
	{
		StringBuffer vText = new StringBuffer();
		for(FeatureObject vFO : mFeatureObjects) {
			for(Integer vInitFrame : vFO.mInitStates.keySet()) {
				String vLine = vInitFrame + " ";
				float[] vStateOfInitFrame = vFO.mInitStates.get(vInitFrame);
				for(int vI = 0; vI < vStateOfInitFrame.length; vI++) {
					vLine += vStateOfInitFrame[vI] + " ";
				}
				vText.append(vLine);
				vText.append("\n");
			}
			vText.append("\n");
		}
		return writeTextFile(vText, aFile);
	}

	/**
	 * Writes the results to the result file.
	 * @see getResultFile()
	 * @param aFile
	 * @return true if successful, false if not.
	 */
	protected boolean writeResultFile(File aFile)
	{
		StringBuffer vText =  new StringBuffer();
		vText.append(generateOutputString(mFeatureObjects,",",true));

		return writeTextFile(vText, aFile);
	}

	protected boolean writeParamFile(File aFile) {
		StringBuffer vText = new StringBuffer();
		vText.append(generateParamString());
		return writeTextFile(vText, aFile);
	}

	/**
	 * Reads the init file. The parameters read are then stored in the statevector member. 
	 * @param aFile
	 * @see getInitFile()
	 * @return true if successful. false if not.
	 */
	protected boolean readInitFile(File aFile) 
	{
		BufferedReader vR = null;
		try {
			vR = new BufferedReader(new FileReader(aFile));
		}
		catch(FileNotFoundException aFNFE) {
			return false;
		}
		String vLine;
		boolean vObjectFound = false;
		boolean vCreateNewObject = true;
		try {
			while((vLine = vR.readLine()) != null) {
				if(vLine.startsWith("#")) continue; //comment
				if(vLine.matches("(\\s)*")) {
					//empty line, this means a new object has to be generated
					if(vObjectFound) {
						//new object
						vCreateNewObject = true;
					} else {
						//empty line in the beginning of the document
						continue;
					}
				}
				Pattern vPattern = Pattern.compile("(,|\\||;|\\s)");
				String [] vPieces = vPattern.split(vLine);
				int vInitFrameNb;
//				if (vPieces.length < mStateVectors.firstElement().length) continue; //not the right line
				try {
					vInitFrameNb = Integer.parseInt(vPieces[0]);
				}catch(NumberFormatException aNFE) {
					continue;
				}
				float[] vState = new float[vPieces.length - 1];
				for (int i = 1; i < vPieces.length; i++) {
					float vValue = 0f;
					try {
						vValue =  Float.parseFloat(vPieces[i]);
						vState[i-1] = vValue;
					}catch(NumberFormatException aNFE) {
						continue; //perhaps there is another matching line
					}
				}
				if(vCreateNewObject) {
					vObjectFound = true; 
					mFeatureObjects.add(new FeatureObject(vState,vInitFrameNb));
				} else {
					mFeatureObjects.lastElement().addInitStateAtFrame(vState, vInitFrameNb);
				}
			}
		}catch(IOException aIOE) {
			aIOE.printStackTrace();
			return false;
		}
		finally{
			try {
				vR.close();
			}catch(IOException aIOE) {
				aIOE.printStackTrace();
				return false;
			}
		}

		return true;
	}

	/**
	 * Reads the result file. The parameters read are then stored in the statevectormemory member. 
	 * @param aFile
	 * @see getInitFile()
	 * @return true if successful. false if not.
	 */
	protected boolean readResultFile(File aFile) 
	{
		BufferedReader vR = null;
		try {
			vR = new BufferedReader(new FileReader(aFile));
		}
		catch(FileNotFoundException aFNFE) {
			return false;
		}
		String vLine;
		int vFrameMem = Integer.MAX_VALUE;
		FeatureObject vNewObject = null;
		try {
			while((vLine = vR.readLine()) != null) {
				if(vLine.startsWith("#")) continue; //ignore
				if(vLine.matches("(\\s)*")) continue; //empty line
				Pattern vPattern = Pattern.compile("(,|\\||;|\\s)");
				String[] vPieces = vPattern.split(vLine);
				
				if(vPieces[0].equalsIgnoreCase("frame")){
					mDimensionsDescription = new String[vPieces.length-1];
					for (int vP = 1; vP < vPieces.length; vP++) {
						mDimensionsDescription[vP-1] = vPieces[vP];
					}
				}

				int vFrame = 0;
				try {
					vFrame = Integer.parseInt(vPieces[0])-1;
					if(vFrame < 0){
						continue; //we just ignore this case
					}
				}catch(NumberFormatException aNFE) {
					continue;
				}
				
				float[] vState = new float[vPieces.length-1];
				for (int vP = 1; vP < vPieces.length; vP++) {
					float vValue = 0f;
					try {
						vValue =  Float.parseFloat(vPieces[vP]);
						vState[vP-1] = vValue;
					}catch(NumberFormatException aNFE) {
						continue;//perhaps there is another matching line
					}
				}
				
				//if the frame number of this line is smaller than the last one we begin with a new object
				if(vFrame < vFrameMem) {
					vFrameMem = -1;
					vNewObject = new FeatureObject(vState.length);
					mFeatureObjects.add(vNewObject);
				}
				else {
					vFrameMem = vFrame;
				}
				vNewObject.mStateVectorsMemory[vFrame] = vState;
			}
		}catch(IOException aIOE) {
			aIOE.printStackTrace();
			return false;
		}
		finally{
			try {
				vR.close();
			}catch(IOException aIOE) {
				aIOE.printStackTrace();
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Reads the covariance file. The parameters read are then stored in the statevectormemory member. 
	 * @param aFile
	 * @see getInitFile()
	 * @return true if successful. false if not.
	 */
	protected boolean readCovarianceFile(File aFile) 
	{
		BufferedReader vR = null;
		try {
			vR = new BufferedReader(new FileReader(aFile));
		}
		catch(FileNotFoundException aFNFE) {
			return false;
		}
		String vLine;
		int vFrameMem = Integer.MAX_VALUE;
		FeatureObject vCurrentObject = null;
		int vObjectCounter = 0;
		int vCurrentCovarianceLine = 0;
		int vFrame = 0;
		try {
			while((vLine = vR.readLine()) != null) {
				if(vLine.startsWith("#")) continue; //ignore
				if(vLine.matches("(\\s)*")) continue; //empty line
				Pattern vPattern = Pattern.compile("(,|\\||;|\\s|:)");
				String[] vPieces = vPattern.split(vLine);
				
				
				if(vPieces[0].equalsIgnoreCase("frame")){
					//then, the next pattern is the frame number
					try {
						vFrame = Integer.parseInt(vPieces[1])-1;
						vCurrentCovarianceLine = 0;
						continue; 
					}catch(NumberFormatException aNFE) {
						continue;
					}
				}

				//if the frame number of this line is smaller than the last one we begin with a new object
				if(vFrame < vFrameMem) {
					vFrameMem = -1;
					vCurrentObject = mFeatureObjects.elementAt(vObjectCounter);
					vObjectCounter++;
					
				}
				else {
					vFrameMem = vFrame;
				}
				
				for (int vPx = 0; vPx < vCurrentObject.getMDim(); vPx++) {
					try {
						vCurrentObject.mCovMatrix[vFrame][vCurrentCovarianceLine][vPx] =  Float.parseFloat(vPieces[vPx]);

					}catch(NumberFormatException aNFE) {
						continue;//perhaps there is another matching line
					}
					
				}
				vCurrentCovarianceLine++;
				
			}
		}catch(IOException aIOE) {
			aIOE.printStackTrace();
			return false;
		}
		finally{
			try {
				vR.close();
			}catch(IOException aIOE) {
				aIOE.printStackTrace();
				return false;
			}
		}

		return true;
	}

	protected String generateParamString() {
		String vOut = new String(
		"particles = " + mNbParticles + "\n" +
		"mcmc moves = " + mNbMCMCmoves + "\n" +
		"burn in moves = " + mNbMCMCburnInMoves + "\n" +
		"resampling threshold = " + mResamplingThreshold + "\n" +		
		"background = " + mBackground + "\n" +
		"seed = " + mSeed + "\n");

		if(mDoGaussianBlur) {
			vOut += "use gaussian blurring = on\n" + 
			"radius for blurring = " + mGaussBlurRadius + "\n";
		} else {
			vOut += "use gaussian blurring = off\n";
		}
		if(mUsePSFMap) {
			vOut += "psf-map used.";
		} else {
			if(mDoUseTheoreticalPSF) {
				vOut += "use theoretical psf = on\n" +
				"wavelength = " + mWavelengthInNm + "\n" +
				"numercal apparture = " + mNA + "\n" +
				"refractive index of medium = " + mn + "\n" +
				"--> sigmaPSFxy = " + mSigmaPSFxy + "\n" +
				"--> sigmaPSFz = " + mSigmaPSFz + "\n";
			} else {
				vOut += "use theoretical psf = off\n" +
				"sigmaPSFxy = " + mSigmaPSFxy + "\n" +
				"sigmaPSFz = " + mSigmaPSFz + "\n";
			}
		}
		if(mEMCCDMode) {
			vOut += "EM mode = on \n" +
			"EM gain = " + mGain + "\n" +
			"EM stages in pipeline = " + mEMStages + "\n" +
			"sigma per stage = " + mEMStageSigma + "\n";
		} else {
			vOut += "EM mode = off \n";
		}
		return vOut;
	}
	/**
	 * This method may be overrided.
	 * @param aStateVectorsMem
	 * @return the string to print in a file or in a window(if the movie is not saved).
	 */
	protected String generateOutputString(Vector<FeatureObject> aFOs, String aDelimiter, boolean aPrintHeader) 
	{
		String vOut = "";

		if(aPrintHeader){
			vOut += "frame" + aDelimiter;
				for(String vDim : mDimensionsDescription) {
					vOut += vDim + aDelimiter;
				}
		}		
		for(FeatureObject vFO : aFOs){
			for(int vF = 0; vF < mNFrames; vF++){
				vOut += "\n" + (vF+1) + aDelimiter;

				for(float vV : vFO.mStateVectorsMemory[vF]){
					vOut += vV + aDelimiter;
				}
			}
			vOut += "\n";
		}
		return vOut;
	}
	

	/**
	 * Generates a text window to display all the information in <code>aStateVectorsMem</code>
	 * @param aTitle Title of the window
	 * @param aStateVectorsMem
	 */
	protected void printStatesToWindow(String aTitle, Vector<FeatureObject> aFOs)
	{
		String vData = generateOutputString(aFOs,"\t",true);
		int vFirstLineEndIndex = vData.indexOf("\n");
		if(vFirstLineEndIndex < 0) {
			IJ.showMessage("Empty.");
			return;
		}
		new TextWindow(aTitle, vData.substring(0, vFirstLineEndIndex), 
				vData.substring(vFirstLineEndIndex + 1, vData.length()), 400, 400);
	}

	/**
	 * Reads in the parameters used by the base class. 
	 * @return True, if cancelled false.
	 */
	protected boolean getUserDefinedParams() {
		
		GenericDialog vGenericDialog = new GenericDialog("Particle filtering parameters",IJ.getInstance());
		if(mFirstDialog){
			vGenericDialog.addNumericField("# of particles ~ quality", mNbParticles, 0);
		}
		
		//vGenericDialog.addNumericField("Background Intensity", mBackground, 0);
		if(mUsePSFMap){
			vGenericDialog.addMessage("PSF map found.");
		}else {			
			if(mDoUseTheoreticalPSF){
				vGenericDialog.addNumericField("Wavelength in nm", mWavelengthInNm, 0);
				vGenericDialog.addNumericField("Numerical apparture", mNA, 2);
				vGenericDialog.addNumericField("refractive index(medium)", mn, 2);
			} else {
				vGenericDialog.addNumericField("sigmaPSFxy in nm", mSigmaPSFxy, 0);
				vGenericDialog.addNumericField("sigmaPSFz in nm", mSigmaPSFz, 0);
			}
		}
		vGenericDialog.addNumericField("Track till frame", mTrackTillFrame, 0);
		vGenericDialog.addMessage("_____________");
		vGenericDialog.addCheckbox("Electron multiplying mode", mEMCCDMode);
		vGenericDialog.addNumericField("Linear gain", mGain, 0);
		vGenericDialog.addNumericField("#EM-Stages in camera", mEMStages, 0);
		vGenericDialog.addMessage("_____________");
//		vGenericDialog.addCheckbox("Filter noise", mDoGaussianBlur);
		vGenericDialog.showDialog();

		if(vGenericDialog.wasCanceled())
			return false;
		
		if(mFirstDialog){
			mNbParticles = (int)vGenericDialog.getNextNumber();
		}
		mResamplingThreshold = mNbParticles / 2;
		
		//mBackground = (int)vGenericDialog.getNextNumber();
		
		setMBackground(mBackground);
		if(!mUsePSFMap){
			if(mDoUseTheoreticalPSF){
				mWavelengthInNm = (int)vGenericDialog.getNextNumber();
				mNA = (float)vGenericDialog.getNextNumber();
				mn = (float)vGenericDialog.getNextNumber();
			} else{
				mSigmaPSFxy = (int)vGenericDialog.getNextNumber();
				mSigmaPSFz = (float)vGenericDialog.getNextNumber();
			}
		}
		mTrackTillFrame = (int)vGenericDialog.getNextNumber();
		mEMCCDMode = vGenericDialog.getNextBoolean();
		mGain = (int)vGenericDialog.getNextNumber();
		mEMStages = (int)vGenericDialog.getNextNumber();
		//		mDoGaussianBlur = vGenericDialog.getNextBoolean();
		
		StackStatistics vSS = new StackStatistics(mOriginalImagePlus);
		mBackground = (float)vSS.dmode;
		if(mEMCCDMode) {
			mBackground /= mGain;
		}
		
		mFirstDialog = false;		
		
		return true;
	}
	
	/**
	 * Shows the dialog to enter the specific parameters.
	 * @return false if cancelled, else true.
	 */
	protected boolean showParameterDialog() 
	{
		return true;
	}
	
	/**
	 * Returns a copy of a float covariance matrix
	 * @param aMatrix to copy
	 * @return the copy
	 */
	public static float[][] copyCovarianceMatrix(float[][] aMatrix){
		int vN = aMatrix.length;
		float[][] vCopy = new float[vN][vN];
		for(int vI = 0; vI < vN; vI++) {
			for(int vJ = 0; vJ < vN; vJ++) {
				vCopy[vI][vJ] = aMatrix[vI][vJ];
			}
		}
		return vCopy;
	}

	/**
	 * Copies a <code>Vector&lt;float[]&gt</code> data structure. Used to copy the state vector here.
	 * @param aOrig
	 * @return the copy.
	 */
	public static Vector<float []> copyStateVectors(Vector<float[]> aOrig){
		Vector<float[]> vResVector = new Vector<float[]>(aOrig.size());
		for(float[] vA : aOrig){
			float[] vResA = new float[vA.length];
			for(int vI = 0; vI < vA.length; vI++){
				vResA[vI] = vA[vI];
			}			
			vResVector.add(vResA);
		}
		return vResVector;
	}
	/**
	 * Copies a <code>float[]</code> data structure. Used to copy the state vector here.
	 * @param aOrig
	 * @return the copy.
	 */
	public static float[] copyStateVector(float[] aOrig){
		float[] vResA = new float[aOrig.length];
		for(int vI = 0; vI < aOrig.length; vI++){
			vResA[vI] = aOrig[vI];
		}			
		return vResA;
	}

	/**
	 * Copies a <code>Vector&lt;float[]&gt;</code> data structure. Used to copy(not clone) the particle vector here.
	 * It is assumed that no entry contains a <code>null</code> entry.
	 * @param aOrig
	 * @return the copy.
	 */
	public static float[][] copyParticleVector(float[][] aOrig){
		int vY = aOrig.length;
		int vX = aOrig[0].length;
		float[][] vResVector = new float[vY][vX];
		
		for(int vIy = 0; vIy < vY; vIy++){
			for(int vIx = 0; vIx < vX; vIx++){
				vResVector[vIy][vIx] = aOrig[vIy][vIx]; 
			}
		}
		return vResVector;
	}

	/**
	 * Recursively searches the brightest voxel in the neighborhood. Might be used for the initialization.
	 * @param aStartX
	 * @param aStartY
	 * @param aStartZ begins with 1...nslices
	 * @param aImageStack
	 * @return a int array with 3 entries: x,y and z coordinate.
	 */
	public static int[] searchLocalMaximumIntensityWithSteepestAscent(int aStartX, int aStartY, int aStartZ, ImageStack aImageStack) {
		int[] vRes = new int[]{aStartX, aStartY, aStartZ};
		float vMaxValue = aImageStack.getProcessor(aStartZ).getPixelValue(aStartX, aStartY);
		for(int vZi = -1; vZi < 2; vZi++){
			if(aStartZ + vZi > 0 && aStartZ + vZi <= aImageStack.getSize()) {			
				for(int vXi = -1; vXi < 2; vXi++){
					for(int vYi = -1; vYi < 2; vYi++){
						if(aImageStack.getProcessor(aStartZ+vZi).getPixelValue(aStartX+vXi, aStartY+vYi) > vMaxValue) {
							vMaxValue = aImageStack.getProcessor(aStartZ+vZi).getPixelValue(aStartX+vXi, aStartY+vYi);
							vRes[0] = aStartX + vXi;
							vRes[1] = aStartY + vYi;
							vRes[2] = aStartZ + vZi;
						}
					}
				}
			}
		}
		if(vMaxValue > aImageStack.getProcessor(aStartZ).getPixelValue(aStartX, aStartY))
			return searchLocalMaximumIntensityWithSteepestAscent(vRes[0], vRes[1], vRes[2], aImageStack);
		return vRes;
	}

	/**
	 * Add the intensities of 2 2D arrays.
	 * @param aResult here the first image is stored in.
	 * @param aImageToAdd a Image that is added to <code>aResult</code>
	 */
	public static void addImage(float[][] aResult, float[][] aImageToAdd){
		int vIMax = Math.min(aResult.length, aImageToAdd.length);
		int vJMax = Math.min(aResult[0].length, aImageToAdd[0].length);
		for(int vI = 0; vI < vIMax; vI++) {
			for(int vJ = 0; vJ < vJMax; vJ++)
				aResult[vI][vJ] += aImageToAdd[vI][vJ];
		}
	}

	/**
	 * Sets all values in the array to <code>aValue</code>
	 * @param aArray
	 * @param aValue
	 */
	public static void initArrayToValue(float[][] aArray, float aValue){		
		for(int vI = 0; vI < aArray.length; vI++) {
			for(int vJ = 0; vJ < aArray[0].length; vJ++)
				aArray[vI][vJ] = aValue;
		}
	}
	
	/**
	 * Convolves an image stack(without convolving the image edges) with a Gaussian surface.
	 * @param aIS The imageStack to convolve
	 * @param aRadius the sigma of the gaussian density function used to generate the kernel.
	 * @return A convolved copy of the imagestack.
	 */
	private ImageStack GaussBlur3D(ImageStack aIS, float aRadius) {
		ImageStack vRestoredIS = new ImageStack(aIS.getWidth(), aIS.getHeight());	
		float[] vKernel = GenerateNormalizedGaussKernel(aRadius);
		int aKernelRadius = vKernel.length / 2;
		int nSlices = aIS.getSize();
		int vWidth = aIS.getWidth();
		for(int i = 1; i <= nSlices; i++){
			ImageProcessor vRestoredProcessor = aIS.getProcessor(i).duplicate();
			Convolver vConvolver = new Convolver();
			// no need to normalize the kernel - its already normalized
			vConvolver.setNormalize(false);
			vConvolver.convolve(vRestoredProcessor, vKernel, vKernel.length , 1);  
			vConvolver.convolve(vRestoredProcessor, vKernel, 1 , vKernel.length);  
			vRestoredIS.addSlice(null, vRestoredProcessor);
		}
		//2D mode, abort here; the rest is unnecessary
		if(aIS.getSize() == 1) {
			return vRestoredIS;
		}			
	
		aKernelRadius = vKernel.length / 2;
		//to speed up the method, store the processor in an array (not invoke getProcessor()):
		float[][] vOrigProcessors = new float[nSlices][];
		float[][] vRestoredProcessors = new float[nSlices][];
		for(int s = 0; s < nSlices; s++) {
			vOrigProcessors[s] = (float[])vRestoredIS.getProcessor(s + 1).getPixelsCopy();
			vRestoredProcessors[s] = (float[])vRestoredIS.getProcessor(s + 1).getPixels();
		}
		//begin convolution with 1D gaussian in 3rd dimension:
		for(int vY = aKernelRadius; vY < aIS.getHeight() - aKernelRadius; vY++){
        	for(int vX = aKernelRadius; vX < aIS.getWidth() - aKernelRadius; vX++){
        		for(int vS = aKernelRadius + 1; vS <= vRestoredIS.getSize() - aKernelRadius; vS++) {
        			float vSum = 0;
        			for(int i = -aKernelRadius; i <= aKernelRadius; i++) {	        				
        				vSum += vKernel[i + aKernelRadius] * vOrigProcessors[vS + i - 1][vY*vWidth+vX];
        			}
        			vRestoredProcessors[vS-1][vY*vWidth+vX] = vSum;
        		}
        	}
        }
		return vRestoredIS;
	}

	/**
	 * Calculates and returns a discrete Gaussian shape with a sigma equal to <code>aRadius</code>.
	 * @param aRadius equal to sigma
	 * @return float array containing the discrete, normalized Gaussian density function.
	 */
	public float[] GenerateNormalizedGaussKernel(float aRadius){
    	int vL = (int)aRadius * 3 * 2 + 1;
    	if(vL < 3) vL = 3;
		float[] vKernel = new float[vL];
		int vM = vKernel.length/2;
		for(int vI = 0; vI < vM; vI++){
			vKernel[vI] = (float)(1f/(2f*Math.PI*aRadius*aRadius) * Math.exp(-(float)((vM-vI)*(vM-vI))/(2f*aRadius*aRadius)));
			vKernel[vKernel.length - vI - 1] = vKernel[vI];
		}
		vKernel[vM] = (float)(1f/(2f*Math.PI*aRadius*aRadius));

		//normalize the kernel numerically:
		float vSum = 0;
		for(int vI = 0; vI < vKernel.length; vI++){
			vSum += vKernel[vI];
		}
		float vScale = 1.0f/vSum;
		for(int vI = 0; vI < vKernel.length; vI++){
			vKernel[vI] *= vScale;
		}
		return vKernel;
	}
	
	/**
	 * crops a 3D image at all of sides of the imagestack cube. 
	 * @param aIS a frame to crop
	 * @param aRadius how many slices/pixel to crop at each edge of the cube.
	 * @see pad ImageStack3D
	 * @return the cropped image
	 */
	public ImageStack cropImageStack3D(ImageStack aIS, int aRadius) {
		int width = aIS.getWidth();
		int newWidth = width - 2*aRadius;
		ImageStack cropped_is = new ImageStack(aIS.getWidth()-2*aRadius, aIS.getHeight()-2*aRadius);
		for(int s = aRadius + 1; s <= aIS.getSize()-aRadius; s++) {
			FloatProcessor cropped_proc = new FloatProcessor(aIS.getWidth()-2*aRadius, aIS.getHeight()-2*aRadius);
			float[] croppedpx = (float[])cropped_proc.getPixels();
			float[] origpx = (float[])aIS.getProcessor(s).getPixels();
			int offset = aRadius*width;
			for(int i = offset, j = 0; j < croppedpx.length; i++, j++) {
				croppedpx[j] = origpx[i];		
				if(j%newWidth == 0 || j%newWidth == newWidth - 1) {
					i+=aRadius;
				}
			}
			cropped_is.addSlice("", cropped_proc);
		}
		return cropped_is;
	}
	
	 /**
	  * Before convolving, the image is padded such that no artifacts occure at the edge of an image.
	  * @param aIS a frame (not a movie!)
	  * @param aRadius how many pixel/slices to add at each edge of the cube(frame).
	  * @see cropImageStack3D(ImageStack)
	  * @return the padded imagestack to (w+2*r, h+2r, s+2r) by copying the last pixel row/line/slice
	  */
	public ImageStack padImageStack3D(ImageStack aIS, int aRadius) {
		int width = aIS.getWidth();
		int newWidth = width + 2*aRadius;
		ImageStack padded_is = new ImageStack(aIS.getWidth() + 2*aRadius, aIS.getHeight() + 2*aRadius);
		for(int s = 0; s < aIS.getSize(); s++){
			FloatProcessor padded_proc = new FloatProcessor(aIS.getWidth() + 2*aRadius, aIS.getHeight() + 2*aRadius);
			float[] paddedpx = (float[])padded_proc.getPixels();
			float[] origpx = (float[])aIS.getProcessor(s+1).getPixels();
			//first r pixel lines
			for(int i = 0; i < aRadius*newWidth; i++) {
				if(i%newWidth < aRadius) { 			//right corner
					paddedpx[i] = origpx[0];
					continue;
				}
				if(i%newWidth >= aRadius + width) {
					paddedpx[i] = origpx[width-1];	//left corner
					continue;
				}
				paddedpx[i] = origpx[i%newWidth-aRadius];
			}
			
			//the original pixel lines and left & right edges				
			for(int i = 0, j = aRadius*newWidth; i < origpx.length; i++,j++) {
				int xcoord = i%width;
				if(xcoord==0) {//add r pixel rows (left)
					for(int a = 0; a < aRadius; a++) {
						paddedpx[j] = origpx[i];
						j++;
					}
				}
				paddedpx[j] = origpx[i];
				if(xcoord==width-1) {//add r pixel rows (right)
					for(int a = 0; a < aRadius; a++) {
						j++;
						paddedpx[j] = origpx[i];
					}
				}
			}
			
			//last r pixel lines
			int lastlineoffset = origpx.length-width;
			for(int j = (aRadius+aIS.getHeight())*newWidth, i = 0; j < paddedpx.length; j++, i++) {
				if(i%width == 0) { 			//left corner
					for(int a = 0; a < aRadius; a++) {
						paddedpx[j] = origpx[lastlineoffset];
						j++;
					}
//					continue;
				}
				if(i%width == width-1) {	
					for(int a = 0; a < aRadius; a++) {
						paddedpx[j] = origpx[lastlineoffset+width-1];	//right corner
						j++;
					}
//					continue;
				}
				paddedpx[j] = origpx[lastlineoffset+i%width];
			}
			//if we are on the top or bottom of the stack, add r slices
			if(s == 0 || s == aIS.getSize() - 1) {
				for(int i = 0; i < aRadius; i++) {
					padded_is.addSlice("", padded_proc.duplicate());
				}
			} 
			padded_is.addSlice("", padded_proc);

		}
		return padded_is;
	}
	
	/**
	 * Returns a copy of a single frame. Note that the properties of the ImagePlus have to be correct
	 * @param aMovie
	 * @param aFrameNumber beginning with 1...#frames
	 * @return The frame copy.
	 */
	public static ImageStack getAFrameCopy(ImagePlus aMovie, int aFrameNumber)
	{
		if(aFrameNumber > aMovie.getNFrames() || aFrameNumber < 1) {
			throw new IllegalArgumentException("frame number = " + aFrameNumber);
		}
		int vS = aMovie.getNSlices();
		return getSubStackFloatCopy(aMovie.getStack(), (aFrameNumber-1) * vS + 1, aFrameNumber * vS);
	}
	
	/**
	 * Rerurns a copy of a substack (i.e.frames)
	 * @param aImageStack: the stack to crop	
	 * @param aStartPos: 1 <= aStartPos <= aImageStack.size()
	 * @param aEndPos: 1 <= aStartPos <= aEndPos <= aImageStack.size()
	 * @return a Copy of the supstack
	 */
	public static ImageStack getSubStackFloatCopy(ImageStack aImageStack, int aStartPos, int aEndPos){
		ImageStack res = new ImageStack(aImageStack.getWidth(), aImageStack.getHeight());
		if(!(aStartPos < 1 || aEndPos < 0)){
			for(int vI = aStartPos; vI <= aEndPos; vI++) {
				res.addSlice(aImageStack.getSliceLabel(vI), aImageStack.getProcessor(vI).convertToFloat().duplicate());
			}
		}
		return res;
	}

	/**
	 * 
	 * @param aImageStack: the stack to crop	
	 * @param aStartPos: 1 <= aStartPos <= aImageStack.size()
	 * @param aEndPos: 1 <= aStartPos <= aEndPos <= aImageStack.size()
	 * @return
	 */
	public static ImageStack getSubStackFloat(ImageStack aImageStack, int aStartPos, int aEndPos){
		ImageStack res = new ImageStack(aImageStack.getWidth(), aImageStack.getHeight());
		if(!(aStartPos < 1 || aEndPos < 0)){
			for(int vI = aStartPos; vI <= aEndPos; vI++) {
				res.addSlice(aImageStack.getSliceLabel(vI), aImageStack.getProcessor(vI).convertToFloat());
			}
		}
		return res;
	}

	protected void addBackgroundToImage(float[][][] aImage, float aBackground) 
	{
		for(float[][] vSlice : aImage) {
			for(float[] vRow : vSlice) {
				for(int vI = 0; vI < vRow.length; vI++) {
					vRow[vI] += aBackground;
				}
			}
		}
	}
	
	protected void addFeaturePointTo3DImage(float[][][] aImage, Point3D aPoint, float aIntensity, float aPxWidthInNm, float aPxDepthInNm, float aGhostImage[][]) {
		if(mUsePSFMap) {
			addPSF(aImage, aPoint, aIntensity, aPxWidthInNm, aPxDepthInNm, aGhostImage);
		} else {
			addGaussBlob(aImage, aPoint, aIntensity, aPxWidthInNm, aPxDepthInNm, aGhostImage);
		}
	}
	
	protected void addGaussBlob(float[][][] aImage, Point3D aPoint, float aIntensity, float aPxWidthInNm, float aPxDepthInNm, float[][] aGhostImage) {
		float vVarianceXYinPx = mSigmaPSFxy * mSigmaPSFxy / (aPxWidthInNm * aPxWidthInNm);
		float vVarianceZinPx = mSigmaPSFz * mSigmaPSFz / (aPxDepthInNm * aPxDepthInNm);
		float vMaxDistancexy = 4*mSigmaPSFxy / aPxWidthInNm;
		float vMaxDistancez = 4*mSigmaPSFz / aPxDepthInNm; //in pixel!

		Point3D vPointInPx = new Point3D(aPoint.mX / aPxWidthInNm, aPoint.mY / aPxWidthInNm, aPoint.mZ / aPxDepthInNm);
		
		int vXStart, vXEnd, vYStart, vYEnd, vZStart, vZEnd;
		if(vPointInPx.mX + .5f - (vMaxDistancexy + .5f) < 0) vXStart = 0; else vXStart = (int)(vPointInPx.mX + .5f) - (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mY + .5f - (vMaxDistancexy + .5f) < 0) vYStart = 0; else vYStart = (int)(vPointInPx.mY + .5f) - (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mZ + .5f - (vMaxDistancez + .5f)  < 0) vZStart = 0; else vZStart = (int)(vPointInPx.mZ + .5f) - (int)(vMaxDistancez + .5f);
		if(vPointInPx.mX + .5f + (vMaxDistancexy + .5f) >= mWidth) vXEnd = mWidth - 1; else vXEnd = (int)(vPointInPx.mX + .5f) + (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mY + .5f + (vMaxDistancexy + .5f) >= mHeight) vYEnd = mHeight - 1; else vYEnd = (int)(vPointInPx.mY + .5f) + (int)(vMaxDistancexy + .5f);
		if(vPointInPx.mZ + .5f + (vMaxDistancez + .5f)  >= mNSlices) vZEnd = mNSlices - 1; else vZEnd = (int)(vPointInPx.mZ + .5f) + (int)(vMaxDistancez + .5f);

		for(int vZ = vZStart; vZ <= vZEnd; vZ++) {
			for(int vY = vYStart; vY <= vYEnd; vY++){
				for(int vX = vXStart; vX <= vXEnd; vX++){
					aImage[vZ][vY][vX] += (float) (aIntensity  
							* Math.exp(-(Math.pow(vX - vPointInPx.mX, 2) + Math.pow(vY - vPointInPx.mY, 2)) / (2 * vVarianceXYinPx))
							* Math.exp(-Math.pow(vZ - vPointInPx.mZ, 2) / (2 * vVarianceZinPx)));
				}
			}
		}	
	}
	protected void addPSF(float[][][] aPixelArray, Point3D aPoint, float aIntensity, float aPxWidthInNm, float aPxDepthInNm, float[][] aGhostImage) {
    	//the origin in the PSF map is at (offset, 0)
    	int vZOffsetPSFCoord = mPSF.getWidth() / 2;
    	
    	//calculate the maximal distance of the influence of the PSF.
    	int vZMaxInPx = (int) ((mPSF.getWidth() / 2) * (mPSF.getCalibration().pixelWidth / aPxDepthInNm));
    	int vRMaxInPx = (int) (mPSF.getHeight()      * (mPSF.getCalibration().pixelWidth / aPxWidthInNm));
    	
    	float vPX = aPoint.mX/aPxWidthInNm;
    	float vPY = aPoint.mY/aPxWidthInNm;
    	float vPZ = aPoint.mZ/aPxDepthInNm;
    	
    	//for speedup, get the pixelarray:
    	float[][] vPSFPixelArray = mPSF.getProcessor().getFloatArray();
    	int vRDim = mPSF.getHeight();
    	int vZDim = mPSF.getWidth();
    	//for all the pixel in the given influence region...
    	for(int vZ = -vZMaxInPx; vZ <= vZMaxInPx; vZ++) {
    		for(int vY = -vRMaxInPx; vY <= vRMaxInPx; vY++) {
    			for(int vX = -vRMaxInPx; vX <= vRMaxInPx; vX++) {
    				//check if we are in the image that is created:
    				if((int)vPZ+vZ < 0 || (int)vPZ+vZ >= mNSlices ||
    						(int)vPY+vY < 0 || (int)vPY+vY >= mHeight || 
    						(int)vPX+vX < 0 || (int)vPX+vX >= mWidth) {
    					continue;
    				}
    				//calculate the distance of the true position aX to the voxel to be filled
    				float vDistX = ((float)Math.floor(vPX) + vX + .5f) - vPX; 
    				float vDistY = ((float)Math.floor(vPY) + vY + .5f) - vPY;
    				
    				float vDistRInPx = (float) Math.sqrt(vDistX*vDistX + vDistY*vDistY); 
    				float vDistZInPx = (float) (Math.floor(vPZ) + vZ +.5f) - vPZ;
    				
    				//check, if the distance is to the true position is not too large.
    				if(vDistRInPx > vRMaxInPx) {
    					continue;
    				}
    				
    				//convert the distances to the coordinates in the PSF map
    				int vPSFCoordinateR = (int) (vDistRInPx *(aPxWidthInNm/mPSF.getCalibration().pixelHeight)+.5f);
    				int vPSFCoordinateZ = (int) (vZOffsetPSFCoord + vDistZInPx * (aPxDepthInNm/mPSF.getCalibration().pixelWidth)+.5f);
    				float vPSFValue = 0;
    				if(vPSFCoordinateR < vRDim && vPSFCoordinateZ < vZDim){
    					vPSFValue = vPSFPixelArray[vPSFCoordinateZ][vPSFCoordinateR];
    				}
//    				if(Float.isNaN(vPSFValue)) {
//    					System.out.println("stop it");
//    				}
    				aPixelArray[(int)vPZ+vZ][(int)vPY+vY][(int)vPX+vX] += (aIntensity)*vPSFValue;
        		}
    		}
    	}
    	
    }
	
	protected class FeatureObject {
		protected float[][] mStateVectorsMemory;
		protected float[][] mParticles;
		protected float[][][] mCovMatrix;
//		protected float[] mStateVector;
		protected boolean mRunning = false;
		private int mDim = 0;
		/**
		 * Maps frame-indices to init state vectors.
		 */
		Hashtable<Integer, float[]> mInitStates = new Hashtable<Integer, float[]>();
		/**
		 * This variable serves only for debugging/visualizing aspects in the case of repsteps(not mcmc)
		 * Frames, Optimizationframes, Particles, Particle(x,y,Intensity,likelihood, weight)*/
		//float[][][][] mParticleMonitor;
//		Vector<Vector<Vector<float[]>>> mParticleMonitor = null;
		/**
		 * initialize the history of the mcmc moves per frame. 
		 * Within a frame, the history represents thus the density of the posterior distribution.
		 * Entries of the datastructure: Frame, Particles, History of particle(mcmc), values
		 */
//		Vector<Vector<Vector<float[]>>> mParticleHistory = null;
		float[][][][] mParticleHistory = null;
		
		public FeatureObject(float[] aInitStateVector, int aCorrespondingFrame) {
			setMDim(aInitStateVector.length);
			mInitStates.put(aCorrespondingFrame, aInitStateVector);
			initDataStructures();
			//also put the initialization to the state vectors memory, so it gets visualized
			mStateVectorsMemory[aCorrespondingFrame - 1] = aInitStateVector;
			mZProjectedImagePlus.getCanvas().repaint();
		}
		
		public FeatureObject(int aDimension) {
			setMDim(aDimension);
			mCovMatrix = new float[mNFrames][getMDim()][getMDim()];
			initDataStructures();
			mZProjectedImagePlus.getCanvas().repaint();
		}
		
		private void initDataStructures(){
			mStateVectorsMemory = new float[mNFrames][getMDim()];
			mParticles = new float[mNbParticles][getMDim()+2];
			mCovMatrix = new float[mNFrames][getMDim()][getMDim()];
			mParticleHistory = new float[mNFrames][mNbMCMCmoves+1][mNbParticles][getMDim()+2];
		//	mParticleMonitor = new float[mNFrames][mRepSteps][mNbParticles][mDim+2];
			
		}
		public void addInitStateAtFrame(float[] aInitStateVector, int aCorrespondingFrame) {
			mInitStates.put(aCorrespondingFrame, aInitStateVector);
			mStateVectorsMemory[aCorrespondingFrame - 1] = aInitStateVector;
		}
		
		public void abort() {
			mRunning = false;
		}
		
		/**
		 * @param aFrame
		 * @return true if there are is a initialization entry for this object stored.
		 */
		public boolean checkObjectForInitializationAtFrame(int aFrame) {
			if(mInitStates.containsKey(aFrame)) {
				mRunning = true;
				return true;
			}
			return false;
		}

		public boolean isAborted() {
			return !mRunning;
		}
		public int getDimension() {
			return getMDim();
		}

		public void setMDim(int mDim) {
			this.mDim = mDim;
		}

		public int getMDim() {
			return mDim;
		}
	}

	//private int mControllingParticleIndex = 0;
	private AtomicInteger mControllingParticleIndex = new AtomicInteger(0);

	@SuppressWarnings("serial")
	private class DrawCanvas extends ImageCanvas {
		public DrawCanvas(ImagePlus aImagePlus){
			super(aImagePlus);
		}
		public void paint(Graphics aG){
			super.paint(aG);
			int vFrame = mZProjectedImagePlus.getCurrentSlice();
			for(FeatureObject vFO : mFeatureObjects){
//				if(vFO.mStateVectorsMemory.elementAt(vFrame-1) != null)
				paintOnCanvas(aG, vFO.mStateVectorsMemory[vFrame-1],  magnification);
			}
		}
	}

	@SuppressWarnings("serial")
	private class ParticleMonitorCanvas extends ImageCanvas {
		ImagePlus mImagePlus;
		public ParticleMonitorCanvas(ImagePlus aImagePlus){
			super(aImagePlus);
			mImagePlus = aImagePlus;
		}

		public void paint(Graphics aG){
			super.paint(aG);
			try{
				int vCurrentFrame = (mImagePlus.getCurrentSlice()-1) / (mNbMCMCmoves+1);
				int vCurrentOptStep = (mImagePlus.getCurrentSlice()-1) % (mNbMCMCmoves+1);
//				int vCurrentFrame = (mImagePlus.getCurrentSlice()-1) / mRepSteps;
//				int vCurrentOptStep = (mImagePlus.getCurrentSlice()-1) % mRepSteps;
				for(FeatureObject vFO : mFeatureObjects) {
//					for(float[] vParticle : vFO.mParticleMonitor[vCurrentFrame][vCurrentOptStep]){
//						paintParticleOnCanvas(aG, vParticle, magnification);
//					}
					for(float[] vParticle : vFO.mParticleHistory[vCurrentFrame][vCurrentOptStep]){
						paintParticleOnCanvas(aG, vParticle, magnification);
					}
				}
			}
			catch(java.lang.NullPointerException vE){
				//do nothing
			}
		}
		
		
	}
	
	public ImageStack burnParticlesToImage(int aZoomFactor, FeatureObject aFO){
		if(aFO.mParticleHistory == null){
			return null;
		}
		
		ImagePlus vIP = new ImagePlus();
		ImageStack vResultStackIS = new ImageStack(mWidth*aZoomFactor, mHeight*aZoomFactor);
		BufferedImage vImage;
		int vSubFrameIndex = 0;
		int vFrameIndex = 0;
		for(float[][][] vFrame : aFO.mParticleHistory) {
			vFrameIndex++;
			for(float[][] vSubframe : vFrame){
				vSubFrameIndex++;
				File vParticleOutputFile = new File("C:\\Users\\janigo\\Desktop\\particlemovie\\particles_t" + vSubFrameIndex + ".png");
//				vImage = new BufferedImage(mWidth*aZoomFactor, mHeight*aZoomFactor, BufferedImage.TYPE_INT_ARGB);
//				vImage.setData(mZProjectedImagePlus.getStack().getProcessor(vFrameIndex).convertToRGB().resize(mWidth*aZoomFactor,mHeight*aZoomFactor).getBufferedImage().getData());
				vImage = mZProjectedImagePlus.getStack().getProcessor(vFrameIndex).resize(mWidth*aZoomFactor,mHeight*aZoomFactor).convertToRGB().getBufferedImage();
				for(float[] vParticle : vSubframe){
					paintParticleOnCanvas(vImage.getGraphics(), vParticle, aZoomFactor);
				}
				try {
					ImageIO.write(vImage, "png", vParticleOutputFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				vIP.setImage(vImage);
				vResultStackIS.addSlice("", vIP.getProcessor());
			}
		}
		return vResultStackIS;
	}
	/**
	 * To override if you would like to visualize the particles in a separate window.
	 * @param aParticle
	 * @param aMagnification
	 */
	protected void paintParticleOnCanvas(Graphics aG, float[] aParticle, double aMagnification) 
	{
		return;
	}

	private class ParallelizedLikelihoodCalculator extends Thread {
		//
		// Most of this members are used to speed up the algorithm, so that they have not to be
		// evaluated for each particle but only for each thread.
		//
		float[][] mParticles;
		ImageStack mObservedImage;
		float[][] mStackProcs;
		boolean[][][] mBitmap;
		float mPxWidthInNm,mPxDepthInNm;
		/**
		 * Calculates likelihoods for particles given a image and writes them in the result array. 
		 * There are two options: 1. LeastSquares likelihoods(negative!) and 2. Poisson LOG(!) Likelihoods.
		 * DO FIRST CONSTRUCT ALL THREADS BEFORE RUNNING THE FIRST ONE!
		 * @param aImageStack: The frame to operate on, i.e. the observed image
		 * @param aParticles: the particles to score
		 *
		 */
		public ParallelizedLikelihoodCalculator(ImageStack aImageStack, boolean[][][] aBitmap,  float[][] aParticles){
			mParticles = aParticles;
			mObservedImage = aImageStack;
			mBitmap = aBitmap;
			// The next line is only ok if the threads are first constructed and run afterwards!
			//mControllingParticleIndex = 0;
			mControllingParticleIndex.set(0);
			
			//this members speeds the algorithm drastically up since we only have to 
			//invoke getProcessor() once per thread instead of for each particle.
			mStackProcs = new float[mNSlices][];
			mPxWidthInNm = getPixelWidthInNm();
			mPxDepthInNm = getPixelDepthInNm();
			for(int vZ = 0; vZ < mNSlices; vZ++){
				mStackProcs[vZ] = (float[])mObservedImage.getProcessor(vZ+1).getPixels();
			}
		}

		public void run() {
			int vI;
			while((vI = getNewParticleIndex()) != -1 ) {
				//calculate ideal image
				float[][][] vIdealImage = generateIdealImage_3D(mWidth, 
						mHeight,
						mNSlices,
						mParticles[vI],
						(int)(mBackground + .5),
						mPxWidthInNm,
						mPxDepthInNm);	
				
				//calculate likelihood and store at second last position
				if(!mEMCCDMode) {
					mParticles[vI][mParticles[vI].length - 2] = calculateLogLikelihood_3D(mStackProcs, vIdealImage, mBitmap);
				} else {
					mParticles[vI][mParticles[vI].length - 2] = calculateEMCCDLogLikelihood3D(mStackProcs, vIdealImage, mBitmap);
				}
			}
			
			
		}

		synchronized int getNewParticleIndex(){
			if(mControllingParticleIndex.intValue() < mNbParticles &&
					mControllingParticleIndex.intValue() >= 0){				
				return mControllingParticleIndex.incrementAndGet() - 1;
			}
			mControllingParticleIndex.set(-1);
			return -1;
		}
	}
	/**
	 * Method to override optionally. The body of the method in the base class is empty.
	 * @param aEvent
	 */
	protected void mousePressed(int aX, int aY){}
	/**
	 * Method to override optionally. The body of the method in the base class is empty.
	 * @param aEvent
	 */
	protected void mouseClicked(int aX, int aY){}
	/**
	 * Method to override optionally. The body of the method in the base class is empty.
	 * @param aEvent
	 */
	protected void mouseEntered(MouseEvent aEvent){}
	/**
	 * Method to override optionally. The body of the method in the base class is empty.
	 * @param aEvent
	 */
	protected void mouseExited(MouseEvent aEvent){}
	/**
	 * Method to override optionally. The body of the method in the base class is empty.
	 * @param aEvent
	 */
	protected void mouseReleased(int aX, int aY){}
	/**
	 * May be overrided optionally. The implementation of the base class runs the algorithm.
	 */
	protected void calcFromHereButtonPressed()
	{
		mStartingFrame = mZProjectedImagePlus.getCurrentSlice();
		//relaunch the filter from the current slice on with the current initialization if there is one
		if(mStateOfFilter != STATE_OF_FILTER.VISUALIZING && mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN){
			IJ.showMessage("No initialization done yet.");
			return;
		}				
		// restart calculation at any frame with valid state at that frame.
		if(mStateOfFilter == STATE_OF_FILTER.VISUALIZING){ 
			int vFrameOfInitialization = mZProjectedImagePlus.getCurrentSlice();
			for(FeatureObject vFO : mFeatureObjects) {
				//for all objects that are defined in that frame, new initializations are generated. 
				float[] vState = vFO.mStateVectorsMemory[vFrameOfInitialization-1];
//				if((vState = vFO.mStateVectorsMemory.elementAt(vFrameOfInitialization - 1)) != null) {
				vFO.mInitStates.put(vFrameOfInitialization, copyStateVector(vState));
//				}
//				else {
//					IJ.showMessage("One or more objects do not have a valid value in this frame. Please reinitialize.");
//					return;
//				}
			}
			mStateOfFilter = STATE_OF_FILTER.READY_TO_RUN;
			PFTracking3D.this.run(null);
			return;
		}				
		//start from a new initialization 
		if(mStateOfFilter == STATE_OF_FILTER.READY_TO_RUN) {
			//we're sure that there is a correct initialization at a certain frame
			
			PFTracking3D.this.run(new FloatProcessor(1,1));
			mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
			return;
		}
	}
	
	/**
	 * 
	 * @param aFrameToStart (1 <= aFrameToEnd <= mNFrames)
	 * @param aFrameToEnd (1 <= aFrameToEnd <= mNFrames)
	 */
	protected void deleteResults(int aFrameToStart, int aFrameToEnd) {
		for (FeatureObject vFO : mFeatureObjects) {
			for(int vF = aFrameToStart-1; vF < aFrameToEnd; vF++) {
				for(int vI = 0; vI < vFO.mStateVectorsMemory[vF].length; vI++) {
					vFO.mStateVectorsMemory[vF][vI] = 0.0f;
				}
				for(int vI = 0; vI < vFO.mCovMatrix[vF].length;vI++) {
					for(int vJ = 0; vJ < vFO.mCovMatrix[vF][0].length;vJ++) {
						vFO.mCovMatrix[vF][vI][vJ] = 0.0f;
					}
				}
			}
		}
		writeResultFile(getTextFile(getRESULT_FILE_SUFFIX()));
		writeCovMatrixFile(getTextFile(COV_FILE_SUFFIX));
		mZProjectedImagePlus.repaintWindow();
	}
	
	protected void deleteRestOfResultsButtonPressed() {
		deleteResults(mZProjectedImagePlus.getCurrentSlice(), mNFrames);
	}
	
	protected void deleteUntilHereButtonPressed() {
		deleteResults(1, mZProjectedImagePlus.getCurrentSlice());
	}
	
	protected void deleteThisFrameButtonPressed() {
		deleteResults(mZProjectedImagePlus.getCurrentSlice(), 
				mZProjectedImagePlus.getCurrentSlice());
	}
	
	/**
	 * May be overrided optionally. The implementation of the base class sets the state of the
	 * plugin to <code>INIT</code>.
	 */
	protected void initializeWithMouseButtonPressed()
	{
		mStateOfFilter = STATE_OF_FILTER.INIT;
	}
	/**
	 * May be overrided optionally. The implementation of the base class saves the initialization values
	 * in a separate file. This enables batch processing.
	 */
	protected void saveInitButtonPressed()
	{
		if(mStateOfFilter != STATE_OF_FILTER.VISUALIZING && mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN){					
			IJ.showMessage("No initialization done yet. Please initialize.");
			return;
		}
		if(mOriginalImagePlus.getOriginalFileInfo() == null) {
			IJ.showMessage("It seems that the movie was not saved. Save the movie in a directory with 'write' permission first.");
			return;
		}
		//write out the init positions in a file in the same directory

		writeInitFile(getTextFile(INIT_FILE_SUFFIX));
	}
	/**
	 * May be overrided optionally. The implementation of the base class deletes all values in RAM 
	 * and on the Disk(Init-value file and result file). Afterwards, a new initialization has to be done.
	 */
	protected void deleteAllButtonPressed()
	{
		mFeatureObjects.clear();
		if(getTextFile(INIT_FILE_SUFFIX) != null)
			getTextFile(INIT_FILE_SUFFIX).delete();
		if(getTextFile(RESULT_FILE_SUFFIX) != null)
			getTextFile(RESULT_FILE_SUFFIX).delete();
		if(getTextFile(COV_FILE_SUFFIX) != null)
			getTextFile(COV_FILE_SUFFIX).delete();
		mStateOfFilter = STATE_OF_FILTER.WAITING;
		mZProjectedImagePlus.repaintWindow();
	}
	
	/**
	 * May be overrided optionally. The implementation of the base class rewrites the result file on the
	 * HD.
	 */
	protected void saveCorrectionButtonPressed()
	{
		if(mOriginalImagePlus.getOriginalFileInfo() == null) {
			IJ.showMessage("It seems that the movie was not saved. Save the movie in a directory with 'write' permission first.");
			return;
		}
		//write out the positions in the res file
		writeResultFile(getTextFile(getRESULT_FILE_SUFFIX()));
	}
	
	private int getCurrentFrameIndex(){
		return mCurrentFrameIndex;
	}
	
	@SuppressWarnings("serial")
	private class TrajectoryStackWindow extends StackWindow implements ActionListener, MouseListener{
//		private static final long serialVersionUID = 1L;
		private Button mCalcFromHereButton;
		private Button mMouseInitializationButton;
		private Button mSaveInitButton;
		private Button mDeleteAllButton;
		private Button mSaveCorrectionButton;
		private Button mChangeParametersButton;
		private Button mShowResultsButton;
		private Button mDeleteRestButton;
		private Button mDeleteThisFrameButton;
		private Button mDeleteUntilHereButton;


		/**
		 * Constructor.
		 * <br>Creates an instance of TrajectoryStackWindow from a given <code>ImagePlus</code>
		 * and <code>ImageCanvas</code> and a creates GUI panel.
		 * <br>Adds this class as a <code>MouseListener</code> to the given <code>ImageCanvas</code>
		 * @param aImagePlus
		 * @param aImageCanvas
		 */
		private TrajectoryStackWindow(ImagePlus aImagePlus, ImageCanvas aImageCanvas) 
		{
			super(aImagePlus, aImageCanvas);
			aImageCanvas.addMouseListener(this);
			addPanel();
		}

		/**
		 * Adds a Panel with filter options button in it to this window 
		 */
		private void addPanel() 
		{
			Panel vButtonPanel = new Panel(new GridLayout(4,3));
			mChangeParametersButton = new Button("Change parameters...");
			mCalcFromHereButton = new Button("Calculate!");
			mSaveInitButton = new Button("Save init position");
			mDeleteAllButton = new Button("Delete all");
			mSaveCorrectionButton = new Button("Write data to disk");
			mMouseInitializationButton = new Button("Initialize with mouse");
			mShowResultsButton = new Button("Show Results");
			mDeleteRestButton = new Button("Delete Results from here on");
			mDeleteUntilHereButton = new Button("Delete Results until here");
			mDeleteThisFrameButton	= new Button("Delete Results of this frame");
			mCalcFromHereButton.addActionListener(this);
			mSaveInitButton.addActionListener(this);
			mDeleteAllButton.addActionListener(this);
			mMouseInitializationButton.addActionListener(this);
			mSaveCorrectionButton.addActionListener(this);
			mChangeParametersButton.addActionListener(this);
			mShowResultsButton.addActionListener(this);
			mDeleteRestButton.addActionListener(this);
			mDeleteThisFrameButton.addActionListener(this);
			mDeleteUntilHereButton.addActionListener(this);

			vButtonPanel.add(mCalcFromHereButton);
			vButtonPanel.add(mSaveInitButton);
			vButtonPanel.add(mSaveCorrectionButton);
			vButtonPanel.add(mDeleteRestButton);
			vButtonPanel.add(mDeleteAllButton);
			vButtonPanel.add(mDeleteUntilHereButton);
			vButtonPanel.add(mDeleteThisFrameButton);
			vButtonPanel.add(mMouseInitializationButton);
			vButtonPanel.add(mSaveCorrectionButton);
			vButtonPanel.add(mChangeParametersButton);
			vButtonPanel.add(mShowResultsButton);
			add(vButtonPanel);
			pack();
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			Point loc = getLocation();
			Dimension size = getSize();
			if (loc.y+size.height>screen.height)
				getCanvas().zoomOut(0, 0);
		}

		/** 
		 * Defines the action taken upon an <code>ActionEvent</code> triggered from buttons
		 * that have class <code>TrajectoryStackWindow</code> as their action listener:
		 * <br><code>Button filter_length</code>
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public synchronized void actionPerformed(ActionEvent aEvent) 
		{
			Object vButton = aEvent.getSource();

			//
			// Show result window
			//
			if (vButton == mShowResultsButton) {
				PFTracking3D.this.printStatesToWindow("Results", mFeatureObjects);
			}
			
			//
			// Change param button
			//
			if (vButton == mChangeParametersButton) { 
				PFTracking3D.this.getUserDefinedParams();
			}
			
			//
			// Calculate Button
			//
			if (vButton == mCalcFromHereButton) { 
				PFTracking3D.this.calcFromHereButtonPressed();
			}

			//
			// Save init button
			//
			if (vButton == mSaveInitButton) {
				PFTracking3D.this.saveInitButtonPressed();
			}

			//
			// Save results button
			//
			if (vButton == mSaveCorrectionButton) {
				PFTracking3D.this.saveCorrectionButtonPressed();
			}

			//
			// Clear Button
			//
			if(vButton == mDeleteAllButton) {
				PFTracking3D.this.deleteAllButtonPressed();
			}

			//
			// initialze with mouse button
			//
			if(vButton == mMouseInitializationButton) {
				PFTracking3D.this.initializeWithMouseButtonPressed();
			}
			
			if(vButton == mDeleteRestButton) {
				PFTracking3D.this.deleteRestOfResultsButtonPressed();
			}
			
			if(vButton == mDeleteUntilHereButton) {
				PFTracking3D.this.deleteUntilHereButtonPressed();
			}
			
			if(vButton == mDeleteThisFrameButton) {
				PFTracking3D.this.deleteThisFrameButtonPressed();
			}

			// generate an updated view with the ImagePlus in this window according to the new filter
//			generateView(this.imp);
		}

		/** 
		 * Defines the action taken upon an <code>MouseEvent</code> triggered by left-clicking 
		 * the mouse anywhere in this <code>TrajectoryStackWindow</code>
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public synchronized void mousePressed(MouseEvent aE) {
			PFTracking3D.this.mousePressed(this.ic.offScreenX(aE.getPoint().x),this.ic.offScreenY(aE.getPoint().y));
		}
		
		public void mouseReleased(MouseEvent aE) {
			PFTracking3D.this.mouseReleased(this.ic.offScreenX(aE.getPoint().x),this.ic.offScreenY(aE.getPoint().y));
		}

		public void mouseClicked(MouseEvent aE) {
			PFTracking3D.this.mouseClicked(this.ic.offScreenX(aE.getPoint().x),this.ic.offScreenY(aE.getPoint().y));
		}


		public void mouseEntered(MouseEvent aE) {
			PFTracking3D.this.mouseEntered(aE);		
		}


		public void mouseExited(MouseEvent aE) {
			PFTracking3D.this.mouseExited(aE);			
		}
	} // CustomStackWindow inner class

	public STATE_OF_FILTER getMStateOfFilter() {
		return mStateOfFilter;
	}

	public void setMStateOfFilter(STATE_OF_FILTER stateOfFilter) {
		mStateOfFilter = stateOfFilter;
	}

	public int getMNbThreads() {
		return mNbThreads;
	}

	public void setMNbThreads(int nbThreads) {
		mNbThreads = nbThreads;
	}	

	public int getMNbParticles() {
		return mNbParticles;
	}

	public void setMNbParticles(int nbParticles) {
		mNbParticles = nbParticles;
	}

//	public int getMRepSteps() {
//		return mRepSteps;
//	}
//
//	public void setMRepSteps(int repSteps) {
//		mRepSteps = repSteps;
//	}

	public int getMResamplingThreshold() {
		return mResamplingThreshold;
	}

	public void setMResamplingThreshold(int resamplingThreshold) {
		mResamplingThreshold = resamplingThreshold;
	}

	public float getMBackground() {
		return mBackground;
	}

	public void setMBackground(float background) {
		mBackground = background;
	}

	abstract public String[] getMDimensionsDescription();
	

	public void setMDimensionsDescription(String[] dimensionsDescription) {
		mDimensionsDescription = dimensionsDescription;
	}

	public long getMSeed() {
		return mSeed;
	}

	public void setMSeed(long seed) {
		mSeed = seed;
	}	

	public boolean isMDoPrintStates() {
		return mDoPrintStates;
	}

	public void setMDoPrintStates(boolean doPrintStates) {
		mDoPrintStates = doPrintStates;
	}

	public boolean isMDoMonitorIdealImage() {
		return mDoMonitorIdealImage;
	}

	public void setMDoMonitorIdealImage(boolean doMonitorIdealImage) {
		mDoMonitorIdealImage = doMonitorIdealImage;
	}

	public boolean isMDoMonitorParticles() {
		return mDoMonitorParticles;
	}

	public void setMDoMonitorParticles(boolean doMonitorParticles) {
		mDoMonitorParticles = doMonitorParticles;
	}

	public ImageStack getMIdealImageMonitorProcessor() {
		return mIdealImageMonitorStack;
	}

	public void setMIdealImageMonitorProcessor(ImageStack idealImageMonitorProcessor) {
		mIdealImageMonitorStack = idealImageMonitorProcessor;
	}

	public int getMWidth() {
		return mWidth;
	}

	public void setMWidth(int width) {
		mWidth = width;
	}

	public ImagePlus getMZProjectedImagePlus() {
		return mZProjectedImagePlus;
	}

	public void setMZProjectedImagePlus(ImagePlus projectedImagePlus) {
		mZProjectedImagePlus = projectedImagePlus;
	}

	public String getRESULT_FILE_SUFFIX() {
		return RESULT_FILE_SUFFIX;
	}

	public String getINIT_FILE_SUFFIX() {
		return INIT_FILE_SUFFIX;
	}

	public int getMHeight() {
		return mHeight;
	}

	public int getMNSlices() {
		return mNSlices;
	}

	public int getMNFrames() {
		return mNFrames;
	}

	public ImagePlus getMOriginalImagePlus() {
		return mOriginalImagePlus;
	}

	public float getMSigmaPSFxy() {
		return mSigmaPSFxy;
	}

	public void setMSigmaPSFxy(float sigmaPSFxy) {
		mSigmaPSFxy = sigmaPSFxy;
	}

	public float getMSigmaPSFz() {
		return mSigmaPSFz;
	}

	public void setMSigmaPSFz(float sigmaPSFz) {
		mSigmaPSFz = sigmaPSFz;
	}

	public float getMNA() {
		return mNA;
	}

	public void setMNA(float mna) {
		mNA = mna;
	}

	public float getMn() {
		return mn;
	}

	public void setMn(float mn) {
		this.mn = mn;
	}

	public float getPixelWidthInNm() {
		return mPxWidthInNm;
	}

	public float getPixelDepthInNm() {
		return mPxDepthInNm;
	}		
	
	public String matrixToString(float[][] aMatrix){
		String vS = "";
		NumberFormat nfFormat = NumberFormat.getInstance();
		for(float[] vL : aMatrix) {
			for(float vV : vL) {
				vS += nfFormat.format(vV) + "\t";
			}
			vS += "\n";
		}
		return vS;
	}

	public class Line3D{
		Point3D mA, mB;
		public Line3D() {
			this(new Point3D(), new Point3D());
		}
		public Line3D(Point3D aStartPoint, Point3D aEndPoint) {
			mA = aStartPoint;
			mB = aEndPoint;
		}
		public Point3D getMA() {
			return mA;
		}
		public void setMA(Point3D ma) {
			mA = ma;
		}
		public Point3D getMB() {
			return mB;
		}
		public void setMB(Point3D mb) {
			mB = mb;
		}

		public float getDistanceToLine(Point3D aP) {
			//|(p-a)x(b-a)| / |b-a| , u = b-a
			Point3D vU = mB.clone().subtract(mA);
			return aP.clone().subtract(mA).cross(vU).getLength() / vU.getLength();
		}

		public float getDistanceToSegment(Point3D aPoint) {
			if(aPoint.clone().subtract(mA).normalize().scalarProduct(mB.clone().subtract(mA).normalize()) < 0) {
				return aPoint.clone().subtract(mA).getLength();
			}
			if(aPoint.clone().subtract(mB).normalize().scalarProduct(mA.clone().subtract(mB).normalize()) > 0) {
				return aPoint.clone().subtract(mB).getLength();
			}
			return getDistanceToLine(aPoint);
		}
		/**
		 *
		 * @return a Line from the closest point to (0,0,0) to the point most far away.
		 */
		public Line3D getBoundingBox() {
			return new Line3D(
					new Point3D(Math.min(mA.mX, mB.mX), Math.min(mA.mY, mB.mY), Math.min(mA.mZ, mB.mZ)), 
					new Point3D(Math.max(mA.mX, mB.mX), Math.max(mA.mY, mB.mY), Math.max(mA.mZ, mB.mZ)));
		}
	}

	public class Point3D{
		public float mX,mY,mZ;
		public Point3D () {
			this(0f, 0f, 0f);
		}
		public Point3D (float aX, float aY, float aZ){
			mX = aX;
			mY = aY;
			mZ = aZ;
		}
		public Point3D clone() {
			return new Point3D(mX, mY, mZ);
		}
		public Point3D add(Point3D aB) {
			mX += aB.mX;
			mY += aB.mY;
			mZ += aB.mZ;
			return this;
		}
		/**
		 * this = this - aB
		 * @param aB
		 */
		public Point3D subtract(Point3D aB) {
			mX -= aB.mX;
			mY -= aB.mY;
			mZ -= aB.mZ;
			return this;
		}
		public float scalarProduct(Point3D aB) {
			return mX * aB.mX + mY * aB.mY + mZ * aB.mZ;
		}
		public Point3D cross(Point3D aB) {
			mX = mY * aB.mZ - mZ * aB.mY;
			mY = mZ * aB.mX - mX * aB.mZ;
			mZ = mX * aB.mY - mY * aB.mX;
			return this;
		}
		public float getLength() {
			return (float)Math.sqrt(scalarProduct(this));
		}
		public Point3D normalize(){
			float vL = getLength();
			mX /= vL;
			mY /= vL;
			mZ /= vL;
			return this;
		}
		public float getMX() {
			return mX;
		}
		public void setMX(float mx) {
			mX = mx;
		}
		public float getMY() {
			return mY;
		}
		public void setMY(float my) {
			mY = my;
		}
		public float getMZ() {
			return mZ;
		}
		public void setMZ(float mz) {
			mZ = mz;
		}
		
	}
}

