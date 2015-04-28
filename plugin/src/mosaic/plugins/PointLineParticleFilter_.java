package mosaic.plugins; 


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.io.FileInfo;
//import ij.plugin.ZProjector;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.text.TextWindow;

import java.awt.Button;
import java.awt.Color;
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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * @author Janick Cardinale, 2007, ETHZ
 *
 */

public class PointLineParticleFilter_ implements  PlugInFilter{
	public static enum STATE_OF_FILTER {WAITING, INIT_STEP1, INIT_STEP2, INIT_STEP3, READY_TO_RUN, RUNNING, VISUALIZING, CORRECTING};
	public static final String RESULT_FILE_SUFFIX = "_mtTracker_results.txt";
	public static final String INIT_FILE_SUFFIX = "_mtTracker_initValues.txt";
	private STATE_OF_FILTER mStateOfFilter = STATE_OF_FILTER.WAITING;
	/*
	 * Parameters
	 */
	public int mNbThreads = 2; //= nb of cores
	public boolean mLSLikelihood = false;
	public int mNbParticles = 200;	
	public int mRepSteps = 5;
	public float mResamplingThreshold = mNbParticles*mRepSteps/2;//mResamplingThreshold = 10; //mNbParticles/2;//TODO:etwas einfallen lassen...bzw. untersuchen
	public float mBackground = 10f;//Achtung: Minimum 1 ->L�sung ausdenken -> ok
	public float mSigmaOfRandomWalk = 1f;//in PIxel
//	public float[] mSigmaOfRandomWalkA = new float[]{
//			mSigmaOfRandomWalk,
//			mSigmaOfRandomWalk,
//			mSigmaOfRandomWalk,
//			mSigmaOfRandomWalk * 2f / (2.0f * (float)Math.PI),
//			mSigmaOfRandomWalk,
//			mSigmaOfRandomWalk * 2f/ (2.0f * (float)Math.PI),
//			mSigmaOfRandomWalk,
//			mSigmaOfRandomWalk};
	public float mScaleSigma = 1/3f; //.6 f�r die iterations auf 1 frame //1f //1.15 //1.3
	public float[] mSigmaOfRandomWalkA = new float[]{
			3f * mScaleSigma,
			3f* mScaleSigma,
			3f* mScaleSigma,
			.3f* mScaleSigma,
			6f* mScaleSigma,
			.3f*mScaleSigma,//mSigmaOfRandomWalk * 2f/ (2.0f * (float)Math.PI) * mScaleSigma,
			.1f* mScaleSigma,
			.1f* mScaleSigma};
	public float mSigmaPSF = 1.23f; //TODO:should better be in the unit of the image (ex.um) and scaled for at least z(perhaps also for y
	public float mRestIntensity = 0f;
	public int mLikelihoodRadius = 3 * ((int)mSigmaPSF+1);
	public float mZResolution = 0.01f;//TODO: units?
	public long mSeed = 88888888;
	/*
	 * Init parameters
	 */
	public float mGaussBlurRadius = mSigmaPSF; //in etwa gleich PSF
	public int mMaxBins = 256;//not used 
	public int mInitParticleFilterIterations = 10;
	
	/*
	 * Monitors (debugging)
	 */
	public boolean mDoMonitorIdealImage = false;
	public boolean mDoMonitorLikelihood = false;
	public boolean mDoMonitorParticles = false;
	public FloatProcessor mIdealImageMonitorProcessor; 
	public FloatProcessor mLikelihoodMonitorProcessor;
	/** - Frames, FeaturePoint, Particles, Particle(x,y,Intensity,weight)*/
	public Vector<Vector<Vector<float[]>>> mParticleMonitor = new Vector<Vector<Vector<float[]>>>(); //init gr�sse?	
	
	/*
	 * Options
	 */
	public boolean mDoResampling = true;
	public boolean mDoPrintStates = true;
	public boolean mDoPrintInitStates = true;

	/*
	 * frequently used members(for simplicity)
	 */
	/** Starts with 1..NFrames*/
	private int mFrameOfInitialization = 1;
	private int mHeight, mWidth;
	private int mNFrames;
	private int mNSlices;
	private Random mRandomGenerator;
	private ImagePlus mOriginalImagePlus;
	private ImagePlus mZProjectedImagePlus;
	private float mSigmaScaler = 1f;
	/**
	 * Dimensions of a line state vector<br>
	 * <li> [0]: x position of the Line<br>
	 * <li> [1]: y position of the Line<br>
	 * <li> [2]: Length of the Line<br>
	 * <li> [3]: polar angle of the Line<br>
	 * <li> [4]: Distance from the endpoint of the line(x+cos([3]),y+sin([3])) to the point<br>
	 * <li> [5]: angle: between the point and the line at (x+cos([3]),y+sin([3]))<br>
	 * <li> [6]: the intensity of the line<br>
	 * <li> [7]: the intensity of the point<br>
	 */
	public Vector<float[]> mStateVectors = new Vector<float []>(); //m�glich in Array zu packen

	/**
	 * Stores the state vectors for each frame. The first frame is at position 0.
	 * The states are completed by <code>mZCoordinates</code>.
	 */
	public Vector<Vector<float[]>> mStateVectorsMemory = new Vector<Vector<float[]>>(); //gr�sse->evt in setup() initialisieren
	/**
	 * Completes the state vectors stored in <code>mStateVectorsMemory</code> with the restored z-coordinate.
	 * Dimensions of variable: Frames -> Objects -> z-coordinates[of line start, of line end(nearer to tip), of tip]
	 */
	public Vector<Vector<float[]>> mZCoordinates = new Vector<Vector<float[]>>();
	/**
	 * Dimensions of the particles vector: Each feature point has a vector of particles. Each vector of particles
	 * contain a float array with dimensions:<br>
	 * <li> [0]: x mid-position of the Line<br>
	 * <li> [1]: y mid-position of the Line<br>
	 * <li> [2]: Length of the Line<br>
	 * <li> [3]: polar angle of the Line<br>
	 * <li> [4]: Distance from the endpoint of the line(x+L/2*cos([3]),y+L/2*sin([3])) to the point<br>
	 * <li> [5]: angle: between the point and the line at (x+L/2*cos([3]),y+L/2*sin([3]))<br>
	 * <li> [6]: the intensity of the line<br>
	 * <li> [7]: the intensity of the point<br>
	 * <li> [8]: the particles weight (always at position 'length - 1'), a weight equal to -1 means that this particle is not valuable.<br>
	 */
	public Vector<Vector<float[]>> mParticles = new Vector<Vector<float[]>>(); //gr�sse?
	/**
	 * A scalar to quantify the track for each frame
	 */
	public Vector<Float> mQualityMeasure = new Vector<Float>();

	/**
	 * The plugins setup method, invoked by imageJ
	 */
	public int setup(String arg, ImagePlus aImp) {
		if(aImp == null) {
			IJ.showMessage("This plugin needs a movie.");	
			return DONE;
		}
		mOriginalImagePlus = aImp;		
		mHeight = mOriginalImagePlus.getHeight();
		mWidth = mOriginalImagePlus.getWidth();
		mNFrames = mOriginalImagePlus.getNFrames();
		mNSlices = mOriginalImagePlus.getNSlices();
		mStateVectorsMemory.setSize(mOriginalImagePlus.getNFrames());
		mZCoordinates.setSize(mOriginalImagePlus.getNFrames());
		mQualityMeasure.setSize(mOriginalImagePlus.getNFrames());

		initMonitoring();
		if(aImp.getNFrames() < 2){
			//nothing to track
			IJ.showMessage("The image only contains one frame, check your image properties");			
			return DONE;
		}
		if (!GetUserDefinedParams()) return DONE;

		/*
		 * first, do the z-projection
		 */
		ImageStack vZProjectedStack = new ImageStack(mWidth, mHeight);
		ZProjector vZProjector = new ZProjector(mOriginalImagePlus);
		vZProjector.setMethod(ZProjector.MAX_METHOD);
		for(int vC = 0; vC < mOriginalImagePlus.getNFrames(); vC++){
			vZProjector.setStartSlice(vC * mNSlices + 1);
			vZProjector.setStopSlice((vC + 1) * mNSlices);
			vZProjector.doProjection();
			vZProjectedStack.addSlice("", vZProjector.getProjection().getProcessor());
		}
		mZProjectedImagePlus = new ImagePlus("Z-Projected " + mOriginalImagePlus.getTitle(), vZProjectedStack);
//		vZProjectedImage.show();
		mFrameOfInitialization = SliceToFrame(mOriginalImagePlus.getCurrentSlice());
		initVisualization();
		
		/*
		 * Possible modes for this plugin: 
		 * - init already done(saved in a file) -> ready to run
		 * - init done automatically
		 * - init done by hand or with threshold
		 */
		initPlugin();
		if(mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN)
			return DONE;
		return DOES_8G + DOES_16 + DOES_32 + NO_CHANGES + STACK_REQUIRED;  
	}

	public void run(ImageProcessor ip) {
		/*
		 * Monitors
		 */
		if (mDoMonitorLikelihood){		
			CalculateLikelihoodMonitorProcessor(mZProjectedImagePlus.getStack().getProcessor(17).convertToFloat());
			return;
		}
//		if(false) {
//			float[][] vF = generateIdealImage(60, 60, new float[]{19.91f, 30.7f, 8.8f, 0.46f, 9.25f, -0.8f, 65, 65}, 10);
//			new ImagePlus("Ideal image", new FloatProcessor(vF)).show();
//			return;
//		}
		
//		if (mOriginalImagePlus.getNFrames() == 1)
//		if(!IJ.showMessageWithCancel("MT Track", "Your current image only contains one frame. " +
//		"Please check your image properties. Continue?"))
//		return;
		if(mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN && mStateOfFilter != STATE_OF_FILTER.VISUALIZING){
			IJ.showMessage("No valid initialization. No calculation started");
			return;
		}
		
		/* we're ready, init the filter */
		initParticleFilter(mZProjectedImagePlus.getStack().getProcessor(mFrameOfInitialization).convertToFloat().duplicate());
		
		/* Be sure that the init state also is saved */
		mStateVectorsMemory.setElementAt(CopyStateVector(mStateVectors), mFrameOfInitialization-1);
		runParticleFilter(mZProjectedImagePlus);

		/*
		 * Restore the z coordinate
		 */
		restoreZCoord();

				
		/*
		 * save or write data to screen
		 */
		if(mDoPrintStates){
			if(mOriginalImagePlus.getOriginalFileInfo() == null){
				PrintStatesToWindow("Tracking states", mStateVectorsMemory, mZCoordinates, mQualityMeasure);
			} else {
				writeResultFile(getResultFile());
			}
		}
		VisualizeMonitors();
		
	}

	private void initPlugin() {
		/*
		 * Check for a result file
		 */
		FileInfo vFI = mOriginalImagePlus.getOriginalFileInfo();
		if(vFI != null && !(vFI.directory == "" || vFI.fileName == "")) {
			/*
			 * Check if there are results to visualize
			 */


			File vResFile = getResultFile();

			if(vResFile.exists()) {
				//read out all the stuff; parameter too? no
				if(readResultFile(vResFile)) {
					mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
					return;
				}
			}		

			/*
			 * Check if there are init values
			 */
			File vInitFile = getInitFile();

			if(vInitFile.exists()) {
				//read out all the stuff; parameter too? no
				if(readInitFile(vInitFile)) {				
					mStateOfFilter = STATE_OF_FILTER.READY_TO_RUN;
					return;
				}
			}
		}
		/*
		 * try to init automatically
		 */
		ImageProcessor vInitProcessor = mZProjectedImagePlus.getStack().getProcessor(mFrameOfInitialization).convertToFloat().duplicate();
		if(setupStateVectorFromImageProcessor(vInitProcessor)) {
			/* Save the state vector after initialisation(the first frame is finished) */
			mStateVectorsMemory.set(mFrameOfInitialization - 1, CopyStateVector(mStateVectors));
			//we propose the initialisation but do not run
			mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
			
			mZProjectedImagePlus.setSlice(SliceToFrame(mOriginalImagePlus.getCurrentSlice()));
			mZProjectedImagePlus.repaintWindow();
			return;
		}
		mStateOfFilter = STATE_OF_FILTER.WAITING;
	}
	
	public File getResultFile() {
		FileInfo vFI = mOriginalImagePlus.getOriginalFileInfo();
		String vResFileName = new String(vFI.fileName);
		int vLastInd = vResFileName.lastIndexOf(".");
		if(vLastInd != -1)
			vResFileName = vResFileName.substring(0, vLastInd);
		vResFileName = vResFileName.concat(RESULT_FILE_SUFFIX);
		
		File vResFile = new File(vFI.directory, vResFileName);
		return vResFile;
	}
	
	public File getInitFile() {
		FileInfo vFI = mOriginalImagePlus.getOriginalFileInfo();
		String vFileName = new String(vFI.fileName);
		int vLastInd = vFileName.lastIndexOf(".");
		vLastInd = vFileName.lastIndexOf(".");
		if(vLastInd != -1)
			vFileName = vFileName.substring(0, vLastInd);
		vFileName = vFileName.concat(INIT_FILE_SUFFIX);
		
		File vInitFile = new File(vFI.directory, vFileName);
		return vInitFile;
			
	}
	
	private boolean writeResultFile(File aFile){
		BufferedWriter vW = null;
		try {
			vW = new BufferedWriter(new FileWriter(aFile));
			vW.write(GenerateOutputString(mStateVectorsMemory, mZCoordinates, mQualityMeasure));
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
	
	private boolean writeInitFile(File aFile){
		BufferedWriter vW = null;
		try {
			vW = new BufferedWriter(new FileWriter(aFile));
			String vS = mFrameOfInitialization + " ";
			for(int vI = 0; vI < mStateVectors.elementAt(0).length; vI++) {
				if(mStateVectors.elementAt(0) == null) {
					return false;
				}
				vS += mStateVectors.elementAt(0)[vI] + " ";
			}
			vW.write(vS);
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
	
	private boolean readResultFile(File aFile) {
		BufferedReader vR = null;
		try {
			vR = new BufferedReader(new FileReader(aFile));
		}
		catch(FileNotFoundException aFNFE) {
			return false;
		}
		String vLine;
		try {
			while((vLine = vR.readLine()) != null) {
				if(vLine.startsWith("#")) continue; //comment
				if(vLine.matches("(\\s)*")) continue; //empty line
				Pattern vPattern = Pattern.compile("(,|\\||;|\\s)");
				String [] vPieces = vPattern.split(vLine);
				if (vPieces.length < 9) continue; //not the right line
				int vFrame = 0;
				try {
					vFrame = Integer.parseInt(vPieces[0])-1;
					if(vFrame < 0){
						IJ.showMessage("Warning", "Unproper result file");
						continue;
					}
				}catch(NumberFormatException aNFE) {
					continue;
				}
				Vector<float[]> vFrameStates = new Vector<float[]>();
				float[] vFrameState = new float[8];
				
				for (int i = 1; i < 9/*vPieces.length*/; i++) {
					float vValue = 0f;
					try {
						vValue =  Float.parseFloat(vPieces[i]);
						vFrameState[i-1] = vValue;
					}catch(NumberFormatException aNFE) {
						continue;//perhaps there is another matching line
					}
				}
				vFrameStates.add(vFrameState);
				mStateVectorsMemory.add(vFrame, vFrameStates);
				
				Vector<float[]> vFrameZCoords = new Vector<float[]>();
				float[] vFrameZCoord = new float[3];
				for (int i = 9; i < 12/*vPieces.length*/; i++) {
					float vValue = 0f;
					try {
						vValue =  Float.parseFloat(vPieces[i]);
						vFrameZCoord[i-9] = vValue;
					}catch(NumberFormatException aNFE) {
						continue;//perhaps there is another matching line
					}
				}
				vFrameZCoords.add(vFrameZCoord);
				mZCoordinates.add(vFrame,vFrameZCoords);
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
	
	private boolean readInitFile(File aFile) {
		BufferedReader vR = null;
		try {
			vR = new BufferedReader(new FileReader(aFile));
		}
		catch(FileNotFoundException aFNFE) {
			return false;
		}
		String vLine;
//		boolean[] vSuperviserArray = new boolean[8];
		try {
			while((vLine = vR.readLine()) != null) {
				if(vLine.startsWith("#")) continue; //comment
				if(vLine.matches("(\\s)*")) continue; //empty line
				Pattern vPattern = Pattern.compile("(,|\\||;|\\s)");
				String [] vPieces = vPattern.split(vLine);
				if (vPieces.length < 9) continue; //not the right line
				try {
					mFrameOfInitialization = Integer.parseInt(vPieces[0]);
				}catch(NumberFormatException aNFE) {
					continue;
				}
				float[] vState = new float[8];
				for (int i = 1; i < 9/*vPieces.length*/; i++) {
					float vValue = 0f;
					try {
						vValue =  Float.parseFloat(vPieces[i]);
						vState[i-1] = vValue;
					}catch(NumberFormatException aNFE) {
						continue;//perhaps there is another matching line
					}
				}
				mStateVectors.add(vState);
//				int vIndOfEq = vLine.indexOf("=");
//				String vName = vLine.substring(0, vIndOfEq);
//				String vValueString = vLine.substring(vIndOfEq, vLine.length()-1);
//				float vValue = 0f;
//				try {
//					vValue =  Float.parseFloat(vValueString);
//				}catch(NumberFormatException aNFE) {
//					continue;
//				}
//				if(vName.toLowerCase().matches("^(\\s)*x(\\s)*")){
//					mStateVectors.elementAt(0)[0] = vValue;
//					vSuperviserArray[0] = true;
//				}else if(vName.toLowerCase().matches("^(\\s)*y(\\s)*")) {
//					mStateVectors.elementAt(0)[1] = vValue;
//					vSuperviserArray[1] = true;
//				}else if(vName.toLowerCase().matches("^(\\s)*alpha(\\s)*")) {
//					mStateVectors.elementAt(0)[2] = vValue;
//					vSuperviserArray[2] = true;
//				}else if(vName.toLowerCase().matches("^(\\s)*l(\\s)*")) {
//					mStateVectors.elementAt(0)[3] = vValue;
//					vSuperviserArray[3] = true;
//				}else if(vName.toLowerCase().matches("^(\\s)*beta(\\s)*")) {
//					mStateVectors.elementAt(0)[4] = vValue;
//					vSuperviserArray[4] = true;
//				}else if(vName.toLowerCase().matches("^(\\s)*d(\\s)*")) {
//					mStateVectors.elementAt(0)[5] = vValue;
//					vSuperviserArray[5] = true;
//				}else if(vName.toLowerCase().matches("^(\\s)*ILine(\\s)*")) {
//					mStateVectors.elementAt(0)[6] = vValue;
//					vSuperviserArray[6] = true;
//				}else if(vName.toLowerCase().matches("^(\\s)*IPoint(\\s)*")) {
//					mStateVectors.elementAt(0)[7] = vValue;
//					vSuperviserArray[7] = true;
//				}
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
//		/*
//		 * Check if all state vector variables have a value
//		 */
//		for(boolean vB : vSuperviserArray) {
//			if(vB == false)
//				return false;
//		}
		
		return true;
	}

	private void restoreZCoord() {

		//iterate through the frames
		int vFrameC = 0;
		for(Vector<float[]> vFrame : mStateVectorsMemory) {
			IJ.showStatus("restore z coordinate at frame: " + vFrameC);
			vFrameC++;
			if(vFrameC == 118)
				System.out.println("gugus at frame: " + vFrameC);
			if(vFrame == null)
				continue;
			Vector<float[]> vZsOfFrame = new Vector<float[]>();
			for(float[] vStateOfFP : vFrame) {
				float vPointX = vStateOfFP[0] + (vStateOfFP[2]/2f) * (float)Math.cos(vStateOfFP[3]) + vStateOfFP[4] * (float)Math.cos(vStateOfFP[3] + vStateOfFP[5]);
				float vPointY = vStateOfFP[1] + (vStateOfFP[2]/2f) * (float)Math.sin(vStateOfFP[3]) + vStateOfFP[4] * (float)Math.sin(vStateOfFP[3] + vStateOfFP[5]);
				Line2D.Float vLine = new Line2D.Float(
						vStateOfFP[0] - (vStateOfFP[2]/2f)*(float)Math.cos(vStateOfFP[3]), 
						vStateOfFP[1] - (vStateOfFP[2]/2f)*(float)Math.sin(vStateOfFP[3]), 
						vStateOfFP[0] + (vStateOfFP[2]/2f)*(float)Math.cos(vStateOfFP[3]),
						vStateOfFP[1] + (vStateOfFP[2]/2f)*(float)Math.sin(vStateOfFP[3]));
				vZsOfFrame.add(new float[]{
						restoreZAt(vLine.x1, vLine.y1, vStateOfFP[7],vFrameC),
						restoreZAt(vLine.x2, vLine.y2, vStateOfFP[7],vFrameC),
						restoreZAt(vPointX, vPointY, vStateOfFP[6],vFrameC)});
			}
			mZCoordinates.add(vFrameC-1, vZsOfFrame);
		}
	}

	private float restoreZAt(float aX, float aY, float aIntensity, int aFrameIndex) {
		if(aX < 0 || aX > mWidth || aY < 0 || aY > mHeight) {
			return -1f;
		}
		//correction: find the right direction
		float[] vInterpolatedData = interpolateRay(mOriginalImagePlus.getStack(), aX, aY, aFrameIndex);
		float vMaxSliceInd = getMaxIndex(vInterpolatedData);
		float[] vIdealImage = generateIdealImage1D(mNSlices, vMaxSliceInd + mZResolution, aIntensity, mBackground);
//		float vLeftLogLik = calculateZCoordLogLikelihood(vInterpolatedData, 
//				vIdealImage, 
//				aFrameIndex);
		vIdealImage = generateIdealImage1D(mNSlices, vMaxSliceInd - mZResolution, aIntensity, mBackground);
//		float vRightLogLik = calculateZCoordLogLikelihood(vInterpolatedData, 
//				vIdealImage, 
//				aFrameIndex);
		vIdealImage = generateIdealImage1D(mNSlices, vMaxSliceInd, aIntensity, mBackground);
//		float vOldLogLik = calculateZCoordLogLikelihood(vInterpolatedData, 
//				vIdealImage, 
//				aFrameIndex);

//		float vCoeff = vLeftLogLik > vRightLogLik ? 1f : -1f;
//		float vNewLogLik;
//
//		float vOldPos = vMaxSliceInd;
		/*
		 * Z Coordinate likelihood plots
		 */
		//TODO: not steepest descent, sample on different qualities and in the end steepest desc.
//		if(aFrameIndex == 10) {
		float[] vRes = new float[(mNSlices + 2)* (int)(1f/mZResolution+.5f) + 1];
		int vCounter = 0;
		float vMaxPos = 1;
		float vMaxLogLik = Float.NEGATIVE_INFINITY;
//		long vtime1 = System.currentTimeMillis();
		for(float vStep = 1; vStep <= mNSlices; vStep += mZResolution) {
			vIdealImage = generateIdealImage1D(mNSlices, vStep, aIntensity, mBackground);
			float vloglik = calculateZCoordLogLikelihood(
					vInterpolatedData, 
					vIdealImage, 
					aFrameIndex);
			vRes[vCounter] = vloglik;
			vCounter++;
			if(vloglik > vMaxLogLik) {
				vMaxLogLik = vloglik;
				vMaxPos = vStep;
			}
		}
		return vMaxPos;
//		System.out.println("time for .01 accuracy: " + (System.currentTimeMillis() - vtime1));
//		String vS = "";
//		for(float vF : vRes)
//		vS += vF + "\n";
//		new TextWindow("sampled z likelihoods", vS, 200, 600);
//		}
//		while(true) {
//		vIdealImage = CalculateIdealImage1D(mNSlices, vOldPos + mZResolution * vCoeff, aIntensity, mBackground);
//		vNewLogLik = calculateZCoordLogLikelihood(vInterpolatedData, 
//		vIdealImage, 
//		aFrameIndex);

//		if(vNewLogLik < vOldLogLik)
//		break;
//		vOldLogLik = vNewLogLik;
//		vOldPos += mZResolution * vCoeff;
//		if(vOldPos < 0 || vOldPos > mNSlices+1) {
//		//may happen if the search direction is wrong
//		return -1;
//		}
//		}
//		return vOldPos;
	}

	private int getMaxIndex(float[] aArray) {
		int vMaxIndex = 0;
		for(int vI = 1; vI < aArray.length; vI++) {
			if(aArray[vMaxIndex] < aArray[vI]) {
				vMaxIndex = vI;
			}
		}
		return vMaxIndex;
	}

	private void runParticleFilter(ImagePlus aImagePlus){	
		/*
		 * Copy the particles of the first frame if necessary(they were created while initialization of the filter)
		 */
		if(mDoMonitorParticles){
			mParticleMonitor.add(CopyParticleVector(mParticles));
		}

		for(int vFrameIndex = mFrameOfInitialization; vFrameIndex <= mNFrames; vFrameIndex++){
//			long vTimeForAFrame = System.currentTimeMillis();
			ImageProcessor vOriginalFloatProcessor = aImagePlus.getStack().getProcessor(vFrameIndex).convertToFloat();
			IJ.showProgress(vFrameIndex,aImagePlus.getNFrames());	
			IJ.showStatus("Particle Filter in progress at frame: " + vFrameIndex);
			mSigmaScaler = 1f;
			for(int vRepStep = 0; vRepStep < mRepSteps; vRepStep++) {
				DrawNewParticles(mParticles);
				mSigmaScaler = 1f - (float)vRepStep / (float)mRepSteps;
				if(mDoMonitorParticles){
					mParticleMonitor.add(CopyParticleVector(mParticles));
				}

				UpdateParticleWeights(vOriginalFloatProcessor);

				EstimateStateVectors(mStateVectors, mParticles);

				QualityMeasure(mParticles, vFrameIndex);//does the same calculation as in resample

				if(mDoResampling){
					Resample(mParticles);
				}
			}
			//save the new states
			mStateVectorsMemory.set(vFrameIndex-1, CopyStateVector(mStateVectors));
//			System.out.println("Time for frame " + vFrameIndex + ": " + (System.currentTimeMillis() - vTimeForAFrame));
		}
	}

	/**
	 * Has to be invoked after setting up the state vector
	 * @param aInitProcessor
	 */
	private void initParticleFilter(ImageProcessor aInitProcessor){
		/*
		 * - set up state vector
		 * - create particles
		 * - filter the initialized values
		 */
		CreateParticles(mStateVectors, mParticles);
		filterTheInitialization(aInitProcessor);
	}

	private boolean setupStateVectorFromImageProcessor(ImageProcessor aInitProcessor)
	{
		//mStateVectors.add(new float[]{7, 20, 7, -(float)Math.PI/4f, 7, 0f, 50});
//		ImageProcessor vInitProcessor = mOriginalImagePlus.getStack().getProcessor(mFrameOfInitialization).convertToFloat().duplicate();

		//
		// Gaussian Blur
		//
//		float[] vKernel = new float[(int)(2*3*(int)mGaussBlurRadius+1)];
//		CalculateGaussKernel(vKernel);
//		vInitProcessor.convolve(vKernel, vKernel.length, 1);
//		vInitProcessor.convolve(vKernel, 1, vKernel.length);
//		System.out.println("The blurring kernel: ");
//		for(float vF : vKernel) {
//		System.out.print(vF + ", ");
//		}
//		new ImageWindow(new ImagePlus("after blurring convolution", vInitProcessor));

		//
		// Threshold
		//
		float vThreshold  = (float)mOriginalImagePlus.getProcessor().getMinThreshold();


		if (vThreshold == ImageProcessor.NO_THRESHOLD){
			ImageStatistics vStats = ImageStatistics.getStatistics(aInitProcessor, Measurements.MIN_MAX /*+ Measurements.AREA + Measurements.MODE*/, null);
			vThreshold = aInitProcessor.getAutoThreshold(vStats.histogram);
			vThreshold = (float)(aInitProcessor.getMin()+ (vThreshold/255.0)*(aInitProcessor.getMax()-aInitProcessor.getMin()));//scale up
		}

		//convert the threshold if a calibration table is used
		if(mOriginalImagePlus.getProcessor().getCalibrationTable() != null) {
			vThreshold = mOriginalImagePlus.getProcessor().getCalibrationTable()[(int)vThreshold];
		}
//		System.out.println("thrshld: " + vThreshold);
//		vInitProcessor.resetMinAndMax();
//		int vBins = (int)(vInitProcessor.getMax()+1) - (int)(vInitProcessor.getMin());
//		if(vBins > mMaxBins)
//		vBins = mMaxBins;
//		int[] vHistogramm = new int[vBins];

//		for(int vP = 0; vP < vPixels.length; vP++)
//		vHistogramm[(int)(vPixels[vP]*(vBins-1)/vInitProcessor.getMax())]++;

//		int vThreshold = vInitProcessor.getAutoThreshold(vHistogramm);
//		int vThreshold = vInitProcessor.convertToShort(false).getAutoThreshold();

		Vector<Vector<Integer>> vAreas = null;
		int vMaxPointer = 0;
		int v2ndPointer = 0;
		boolean[] vBitmap = null;
		float[] vPixels = (float[])aInitProcessor.getPixels();
		vBitmap = new boolean[vPixels.length];
		for(int vP = 0; vP < vPixels.length; vP++){
			if(vPixels[vP] > vThreshold) {
				vBitmap[vP] = true;
			}
			else {
				vBitmap[vP] = false;
			}
		}
		//
		// 	TEST 
		//
//		int thrs = vInitProcessor.convertToShort(false).getAutoThreshold();
//		double[] vDoubleTestPixels = new double[vBitmap.length];
//		for(int vT = 0; vT < vBitmap.length; vT++){
//		if(vBitmap[vT]){
//		vDoubleTestPixels[vT] = 1;
//		}else{
//		vDoubleTestPixels[vT] = 0;
//		}
//		}
//		System.out.println("Threshold: " + vThreshold + "Thrs from converted image: " + thrs);
//		FloatProcessor vTestProcessor = new FloatProcessor(mWidth, mHeight, vDoubleTestPixels);
//		new ImageWindow(new ImagePlus("Bitmap", vTestProcessor));
		//
		//	END TEST
		//

		//
		// Search 2 largest areas over threshold
		//
		try{
			vAreas = SearchAreasInBitmap(vBitmap);
		}
		catch(StackOverflowError aE) {
			//in this case the threshold was wrong
			return false;
		}
		if(vAreas.size() < 2) {
//			IJ.showMessage("Initialization failed.\n(Threshold = " + vThreshold + ")");
			return false;
		}
		//search the 2 largest areas
		vMaxPointer = 0;
		v2ndPointer = 1;

		if(vAreas.elementAt(vMaxPointer).size() < vAreas.elementAt(v2ndPointer).size() ) {
			vMaxPointer = 1;
			v2ndPointer = 0;
		}
		for(int vIndex = 2; vIndex < vAreas.size(); vIndex++) {
			if(vAreas.elementAt(vIndex).size() > vAreas.elementAt(v2ndPointer).size()){
				if(vAreas.elementAt(vIndex).size() > vAreas.elementAt(vMaxPointer).size()){
					v2ndPointer = vMaxPointer;
					vMaxPointer = vIndex;
				}else{
					v2ndPointer = vIndex;
				}
			}
		}
		//for the largest area, get the boundary
		Vector<Integer> vBoundary = SearchBoundary(vBitmap, vAreas.elementAt(vMaxPointer));
		//
		// 	TEST 
		//
//		FloatProcessor vTestProcessor2 = new FloatProcessor(mWidth, mHeight);
//		float[] vTestPixels = (float[])vTestProcessor2.getPixels();
//		for(int vI : vBoundary){
//		vTestPixels[vI] = 255;
//		}
//		new ImageWindow(new ImagePlus("boundary",vTestProcessor2));
		//
		//	END TEST
		//
		float vMaxDist = 0;
		int vLx1 = 0, vLy1 = 0, vLx2 = 0, vLy2 = 0; // the line start and end points
		for(int vI = 0; vI < vBoundary.size(); vI++){
			for(int vJ = vI + 1; vJ < vBoundary.size(); vJ++){
				int vX1 = vBoundary.elementAt(vI) % mWidth;
				int vY1 = (int)(vBoundary.elementAt(vI) / mWidth);
				int vX2 = vBoundary.elementAt(vJ) % mWidth;
				int vY2 = (int)(vBoundary.elementAt(vJ) / mWidth);
				float vD = (vX1 - vX2) * (vX1 - vX2) + (vY1 -vY2) * (vY1 - vY2);
				if(vD > vMaxDist) {
					vLx1 = vX1;
					vLy1 = vY1;
					vLx2 = vX2;
					vLy2 = vY2;
					vMaxDist = vD;
				}
			}
		}
		//the second largest area
		float vPx = 0, vPy = 0;
		float vInensitySum = 0;		
		for(int vP : vAreas.elementAt(v2ndPointer)){
			vInensitySum += vPixels[vP];			
		}
		for(int vP : vAreas.elementAt(v2ndPointer)){
			vPx += (float)(vP % mWidth) * vPixels[vP] / vInensitySum;
			vPy += (float)(vP / mWidth) * vPixels[vP] / vInensitySum;
		}
		//
		// Read out the intensities(the maximum intensity of a pixel)
		//
//		float[] vOrigPixels = (float[])mOriginalImagePlus.getStack().getProcessor(mFrameOfInitialization).convertToFloat().getPixels();
//		float vPointIntensity = 0f;
//		float vLineIntensity = 0f; 

//		for(int vP : vAreas.elementAt(vMaxPointer)){
//		if(vOrigPixels[vP] > vLineIntensity)
//		vLineIntensity = vOrigPixels[vP];
//		}
//		for(int vP : vAreas.elementAt(v2ndPointer)){
//		if(vOrigPixels[vP] > vPointIntensity){
//		vPointIntensity = vOrigPixels[vP];
//		}
//		}
		
		setupStateVectorFromPoints(vLx1, vLy1, vLx2, vLy2, vPx, vPy, aInitProcessor);
		
		return true;
	}

	private void setupStateVectorFromPoints(float aSPB1x, float aSPB1y, float aSPB2x, 
			float aSPB2y, float aTipx, float aTipy, ImageProcessor aImageProcessor) 
	{
		mStateVectors.clear();
		//
		// Read out the intensities
		//
		float vPointIntensity = aImageProcessor.getf((int)Math.round(aSPB1x), (int)Math.round(aSPB1y));
		float vLineIntensity = (aImageProcessor.getf((int)Math.round(aSPB1x), (int)Math.round(aSPB1y)) +
							aImageProcessor.getf((int)Math.round(aSPB1x), (int)Math.round(aSPB1y))) /2f;
		//
		// Set up the state vector with streight foreward calculations
		//
		float vLineLength = (float)Math.sqrt((aSPB1x -aSPB2x)*(aSPB1x -aSPB2x) + (aSPB1y - aSPB2y)*(aSPB1y - aSPB2y));
		float vDistToL1 = (float)Math.sqrt((aTipx - aSPB1x) * (aTipx - aSPB1x) + (aTipy - aSPB1y) * (aTipy - aSPB1y));
		float vDistToL2 = (float)Math.sqrt((aTipx - aSPB2x) * (aTipx - aSPB2x) + (aTipy - aSPB2y) * (aTipy - aSPB2y));
		if(vDistToL1 > vDistToL2) {
			float vAlpha = (float)Math.acos((aSPB2x - aSPB1x)/vLineLength);
			if(aSPB2y - aSPB1y < 0) // check the quadrant
				vAlpha = 2f*(float)Math.PI - vAlpha;			
			float vBeta = (float)Math.acos((aTipx - aSPB2x)/vDistToL2);
			if(aTipy - aSPB2y < 0)
				vBeta = 2f*(float)Math.PI - vBeta;
			vBeta -= vAlpha;
			mStateVectors.add(new float[]{
					aSPB1x + vLineLength/2f * (float)Math.cos(vAlpha), 
					aSPB1y + vLineLength/2f * (float)Math.sin(vAlpha), 
					vLineLength, vAlpha, vDistToL2, vBeta, vLineIntensity - mBackground, vPointIntensity - mBackground});
		}else{
			float vAlpha = (float)Math.acos((aSPB1x - aSPB2x)/vLineLength);
			if(aSPB1y - aSPB2y < 0) // check the quadrant
				vAlpha = 2f*(float)Math.PI - vAlpha;			
			float vBeta = (float)Math.acos((aTipx - aSPB1x)/vDistToL1);
			if(aTipy - aSPB1y < 0)
				vBeta = 2f*(float)Math.PI - vBeta;
			vBeta -= vAlpha;
			mStateVectors.add(new float[]{
					aSPB2x + vLineLength/2f * (float)Math.cos(vAlpha),
					aSPB2y + vLineLength/2f * (float)Math.sin(vAlpha), 
					vLineLength, vAlpha, vDistToL1, vBeta, vLineIntensity - mBackground, vPointIntensity - mBackground });
		}
		
		
	}
	private void filterTheInitialization(ImageProcessor aImageProcessor) {

		//
		// Preprocess the data several times on the first processor
		//
		Vector<Vector<float[]>> vInitStatesMemory = new Vector<Vector<float[]>>();
		vInitStatesMemory.add(CopyStateVector(mStateVectors));
		for(int vR = 0; vR < mInitParticleFilterIterations; vR++){
			IJ.showProgress(vR, mInitParticleFilterIterations);
			
			DrawNewParticles(mParticles);

			UpdateParticleWeights(aImageProcessor.convertToFloat());			

			EstimateStateVectors(mStateVectors, mParticles);

			Resample(mParticles);

			vInitStatesMemory.add(CopyStateVector(mStateVectors));
		}
		
		if(mDoPrintInitStates)
			PrintStatesToWindow("Initialization states",vInitStatesMemory,null,null);

	}

	private Vector<Vector<Integer>> SearchAreasInBitmap(boolean[] aBitmap) {
		Vector<Vector<Integer>> vAreas = new Vector<Vector<Integer>>();
		boolean[] vAlreadyVisited = new boolean[aBitmap.length];
		for(int vP = 0; vP < aBitmap.length; vP++){
			if(aBitmap[vP] && !vAlreadyVisited[vP]){
				vAreas.add(SearchArea(aBitmap, vAlreadyVisited, vP));
			}
		}
		return vAreas;
	}

	private Vector<Integer> SearchArea(boolean[] aBitmap, boolean[] aAlreadyVisitedMask, int aPixel) {
		if(!aBitmap[aPixel] || aAlreadyVisitedMask[aPixel])
			return new Vector<Integer>();

		Vector<Integer> vArea = new Vector<Integer>();
		vArea.add(aPixel);
		aAlreadyVisitedMask[aPixel] = true;
		if(aPixel % mWidth != mWidth - 1 && aBitmap[aPixel + 1] && !aAlreadyVisitedMask[aPixel + 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + 1));
		}
		if(aPixel % mWidth != mWidth -1 && aPixel > mWidth && aBitmap[aPixel - mWidth + 1] && ! aAlreadyVisitedMask[aPixel - mWidth + 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - mWidth + 1));
		}
		if(aPixel > mWidth && aBitmap[aPixel - mWidth] && !aAlreadyVisitedMask[aPixel - mWidth]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - mWidth));
		}
		if(aPixel % mWidth != 0 && aPixel > mWidth && aBitmap[aPixel - mWidth - 1] && ! aAlreadyVisitedMask[aPixel - mWidth - 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - mWidth - 1));
		}
		if(aPixel % mWidth != 0 && aBitmap[aPixel - 1] && !aAlreadyVisitedMask[aPixel - 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - 1));
		}
		if(aPixel % mWidth != 0 && aPixel < (mHeight-1)*mWidth && aBitmap[aPixel + mWidth - 1] && !aAlreadyVisitedMask[aPixel + mWidth - 1]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + mWidth - 1));
		}
		if(aPixel < (mHeight-1)*mWidth && aBitmap[aPixel + mWidth] && !aAlreadyVisitedMask[aPixel + mWidth]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + mWidth));
		}
		if(aPixel % mWidth != mWidth -1 && aPixel < (mHeight-1)*mWidth && aBitmap[aPixel + mWidth + 1] && !aAlreadyVisitedMask[aPixel + mWidth + 1]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + mWidth + 1));
		}
		return vArea;
	}

	private Vector<Integer> SearchBoundary(boolean[] aBitmap, Vector<Integer> aArea) {
		Vector<Integer> vB = new Vector<Integer>();
		for(int vP : aArea){
			//
			// Get the neighbours, first handle the boundaries of the image
			//
			//corners
			if(vP == 0 || vP == mWidth-1 || vP == (mHeight-1)*mWidth || vP == (mHeight-1)*mWidth + mWidth - 1) {
				vB.add(vP);
			}
			//right
			if(vP % mWidth == mWidth - 1) {
				if(!aBitmap[vP-mWidth] || !aBitmap[vP+mWidth]){
					vB.add(vP);
				}
				continue;
			}
			//top
			if(vP < mWidth) {
				if(!aBitmap[vP-1] || !aBitmap[vP+1]){
					vB.add(vP);
				}
				continue;
			}
			//left
			if(vP % mWidth == 0) {
				if(!aBitmap[vP-mWidth] || !aBitmap[vP+mWidth]){
					vB.add(vP);
				}
				continue;
			}
			//bottom
			if(vP > (mHeight - 1) * mWidth) {
				if(!aBitmap[vP-1] || !aBitmap[vP+1]){
					vB.add(vP);
				}
				continue;
			}
			//not boundary
			int vNeighbourCode = 0;
			if(!aBitmap[vP + 1]) vNeighbourCode += 1;
			if(!aBitmap[vP + 1 - mWidth]) vNeighbourCode += 2;
			if(!aBitmap[vP - mWidth]) vNeighbourCode += 4;
			if(!aBitmap[vP - 1 - mWidth]) vNeighbourCode += 8;
			if(!aBitmap[vP - 1]) vNeighbourCode += 16;
			if(!aBitmap[vP - 1 + mWidth]) vNeighbourCode += 32;
			if(!aBitmap[vP + mWidth]) vNeighbourCode += 64;
			if(!aBitmap[vP + mWidth + 1]) vNeighbourCode += 128;

			if((vNeighbourCode & 15) == 15  ||
					(vNeighbourCode & 30) == 30  ||
					(vNeighbourCode & 60) == 60  ||
					(vNeighbourCode & 120) == 120  ||
					(vNeighbourCode & 240) == 240  ||
					(vNeighbourCode & 225) == 225  ||
					(vNeighbourCode & 195) == 195  ||
					(vNeighbourCode & 135) == 135  ) {
				vB.add(vP);
			}

		}
		return vB;
	}

	private void CreateParticles(Vector<float[]> aStateVectors, Vector<Vector<float[]>> aParticles){
		aParticles.clear();
		if(aStateVectors.isEmpty()) {				
			IJ.error("Not nice argument in method CreateParticles in ParticleFilter_");
			return;
		}
		for(float[] vState : aStateVectors) {
			Vector<float[]> vParticleVector = new Vector<float[]>(mNbParticles);
			for(int vIndex = 0; vIndex < mNbParticles; vIndex++) {		
				float[] vProposal = new float[vState.length + 1];
				for(int vI = 0; vI < vState.length; vI++) 
					vProposal[vI] = vState[vI];
//				Init the weight as a last dimension
				vProposal[vState.length] = 1f; //not 0!

				//Draw a the new proposal
				DrawFromProposalDistribution(vProposal); 
				//add the new particle
				vParticleVector.add(vProposal);					
			}
			aParticles.add(vParticleVector);
		}		
	}

	private void initMonitoring()
	{
		if(mDoMonitorIdealImage) {					
			mIdealImageMonitorProcessor = new FloatProcessor(mWidth, mHeight);				
		}
		if(mDoMonitorLikelihood){
			mLikelihoodMonitorProcessor = new FloatProcessor(mWidth, mHeight);
		}
	}

	private void VisualizeMonitors()
	{
		if(mDoMonitorIdealImage){
			ImagePlus vIdealImagePlus = new ImagePlus("Summed up ideal image", mIdealImageMonitorProcessor);
			new ImageWindow(vIdealImagePlus);
		}
		if(mDoMonitorLikelihood){
			ImagePlus vIdealImagePlus = new ImagePlus("Likelihood", mLikelihoodMonitorProcessor);
			new ImageWindow(vIdealImagePlus);
		}
		if(mDoMonitorParticles){
			ImageStack vStack = new ImageStack(mWidth, mHeight);
			int[] vBlackImage = new int[mWidth * mHeight];			
			for(int vI = 0; vI < mWidth*mHeight; vI++){
				vBlackImage[vI] = 0;
			}
			for(int vF = 0; vF < mOriginalImagePlus.getNFrames(); vF++){
				vStack.addSlice("", vBlackImage);
			}

			ImagePlus vParticlesImage = new ImagePlus("Particles", vStack);

			new StackWindow(vParticlesImage, new ParticleMonitorCanvas(vParticlesImage) );
		}
	}

	/**
	 * Draws new particles for all the feature points
	 *
	 */
	private void DrawNewParticles(Vector<Vector<float[]>> aParticlesToRedraw){
		for(Vector<float[]> vFeaturePointParticles : aParticlesToRedraw){
			for(float[] vParticle : vFeaturePointParticles){
				DrawFromProposalDistribution(vParticle);				
			}
		}
	}

	private void QualityMeasure(Vector<Vector<float[]>> aParticles, int aFrameIndex) {
		for(Vector<float[]> vFeaturePointParticles : aParticles){ //should only be one here
			int vDimOfState = aParticles.elementAt(0).elementAt(0).length - 1;
			float vNeff = 0;
			for(float[] vParticle : vFeaturePointParticles){
				vNeff += vParticle[vDimOfState] * vParticle[vDimOfState];
			}
			vNeff = 1/vNeff;
			mQualityMeasure.setElementAt(vNeff, aFrameIndex-1);
		}
	}

	private void Resample(Vector<Vector<float[]>> aParticles){		
		for(Vector<float[]> vFeaturePointParticles : aParticles){
			/*
			 * First check if the threshold is smaller than Neff
			 */
			int vDimOfState = aParticles.elementAt(0).elementAt(0).length - 1;
			float vNeff = 0;
			for(float[] vParticle : vFeaturePointParticles){
				vNeff += vParticle[vDimOfState] * vParticle[vDimOfState];
			}
			vNeff = 1/vNeff;
			
			if(vNeff > mResamplingThreshold) {
				System.out.println("no resampling");
				return; //we won't do the resampling
			}
			/*
			 * Begin resampling
			 */
//			System.out.println("Resampling");
			float VNBPARTICLES_1 = 1f/(float)mNbParticles;
			double[] vC = new double[mNbParticles + 1];
			vC[0] = 0;
			for(int vInd = 1; vInd <= mNbParticles; vInd++){
				vC[vInd] = vC[vInd-1] + vFeaturePointParticles.elementAt(vInd-1)[vDimOfState];
			}

			double vU = mRandomGenerator.nextFloat()*VNBPARTICLES_1;

			Vector<float[]> vFPParticlesCopy = CopyStateVector(vFeaturePointParticles);
			int vI = 0;
			for(int vParticleCounter = 0; vParticleCounter < mNbParticles; vParticleCounter++){
				while(vU > vC[vI])
					if(vI < mNbParticles) //this can happen due to numerical reasons
						vI++;
				for(int vK = 0; vK < vDimOfState; vK++){
					vFeaturePointParticles.elementAt(vParticleCounter)[vK] = vFPParticlesCopy.elementAt(vI-1)[vK];
				}
				vFeaturePointParticles.elementAt(vParticleCounter)[vDimOfState] = VNBPARTICLES_1;
				vU += VNBPARTICLES_1;
			}
		}
	}






	private float CalculateLSLikelihood(ImageProcessor aObservationImage, float[][] aIdealImage) {
		float[] vPixels = (float[])aObservationImage.getPixels();
		float vSum = 0f;
		for(int vY = 0; vY < mHeight; vY++){
			for(int vX = 0; vX < mWidth; vX++) {
				float vDist = (vPixels[vY * mWidth + vX] - aIdealImage[vY][vX]);
				vSum += vDist * vDist;
			}
		}
		return -vSum;
	}

	private void UpdateParticleWeights(ImageProcessor aObservationImage){
		//bitmap?????
		

		for(Vector<float[]> vFeatureParticles : mParticles){
			float vSumOfWeights = 0f;

			/*
			 * Calculate the likelihoods for each particle 
			 */
			float[] vLikelihoods = new float[mNbParticles];


			Thread[] vThreads = new Thread[mNbThreads];
			for(int vT = 0; vT < mNbThreads; vT++){
				vThreads[vT] = new ParallelizedLikelihoodCalculator(aObservationImage,vLikelihoods,vFeatureParticles);
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
			/*
			 * Search the maximum to normalize the weights afterwards
			 */
			float vLikelihoodNormalizer = 0f;
			if(!mLSLikelihood) {
				vLikelihoodNormalizer = Float.NEGATIVE_INFINITY;
			}

			for(int vI = 0; vI < vFeatureParticles.size(); vI++) {
				if(mLSLikelihood){
					if(vLikelihoods[vI] < vLikelihoodNormalizer) {//the LS likelihoods are negative
						vLikelihoodNormalizer = vLikelihoods[vI];
					}
				}
				else {
					if(vLikelihoods[vI] > vLikelihoodNormalizer) //the poisson-likelihoods are logarithmic, can be negative
						vLikelihoodNormalizer = vLikelihoods[vI];
				}
			}
			/*
			 * Iterate again and update the weights
			 */
			int vI = 0;
			int vDimOfState = vFeatureParticles.elementAt(0).length - 1;
			if(mLSLikelihood) {
				for(float[] vParticle : vFeatureParticles){
					
					vLikelihoods[vI] -= vLikelihoodNormalizer;
					vParticle[vDimOfState] = vParticle[vDimOfState] * vLikelihoods[vI];
					vSumOfWeights += vParticle[vDimOfState];	
					vI++;
				}
			} else {
				for(float[] vParticle : vFeatureParticles){	
//					System.out.println(vLikelihoods[vI]);
					vLikelihoods[vI] -= vLikelihoodNormalizer;					
					vParticle[vDimOfState] = vParticle[vDimOfState] * (float)Math.exp(vLikelihoods[vI]);
					vSumOfWeights += vParticle[vDimOfState];						
					vI++;
				}
			}

			/*
			 * Iterate again and normalize the weights
			 */
			if(vSumOfWeights == 0.0f) { //can happen if the winning particle before had a weight of 0.0
				for(float[] vParticle : vFeatureParticles){
					vParticle[vDimOfState] = 1.0f / (float)mNbParticles;
				}
			}
			else {
				for(float[] vParticle : vFeatureParticles){
					vParticle[vDimOfState] /= vSumOfWeights;
				}
			}
		}
	}


	private void CalculateLikelihoodMonitorProcessor(ImageProcessor aObservationImage)
	{
		IJ.showStatus("Calculating likelihood image for monitoring");

		PrintWriter vPW = null;
		try{
			BufferedWriter vBW = new BufferedWriter(new FileWriter("c:\\likelihoodsWrong.txt"));
			vPW = new PrintWriter(vBW);
			vBW.close();
		}catch (IOException aIOE){
			IJ.error("unable to write to file");
		}
		
		
		if(!mLSLikelihood){
			boolean[][] vBitmap = new boolean[mHeight][mWidth];
			for(int vX = 0; vX < mWidth; vX++)
				for(int vY = 0; vY < mHeight; vY++)
					vBitmap[vY][vX] = true;

//			float[] vPixels = (float[])mLikelihoodMonitorProcessor.getPixels();
			String vS = "";

//			float[][] vLogLikelihoods = new float[200][200];
			/**
			 * movie 884, frame 17: values 19.91	30.7	8.8	0.46	9.25	-0.8 63 63
			 * 

			 */
			float vX_tip = 32.56f;
			float vY_tip = 29.55f;
			
			for(float vL = 6f ; vL < 10f; vL = vL + 4f/100f){	
				vPW.write("\n");
				
				float vSPB2x = 19.91f + vL/2f * (float)Math.cos(0.56);
				float vSPB2y = 30.70f + vL/2f * (float)Math.sin(0.56);
				
				float vD = (float)Math.sqrt((vX_tip-vSPB2x)*(vX_tip-vSPB2x) + (vY_tip-vSPB2y)*(vY_tip-vSPB2y)); 
				float vBeta = (float)Math.asin((vY_tip - 30.7f - vL/2f*Math.sin(0.46))/vD)-0.46f;
				
				for(float vI = 20f ; vI < 100f; vI = vI + 80f/100f){
					float[][] vIdealImage = generateIdealImage(aObservationImage.getWidth(), 
							aObservationImage.getHeight(),
							new float[]{19.91f, 30.7f, vL, 0.46f, vD, vBeta, vI, 25.3f},
							10);	
					vS = vL + "," + vI + "," + vD + "," + vBeta + "," + calculateLogLikelihood(aObservationImage,vIdealImage,vBitmap);//vIdealImage[vY][vX];
					vS += "\n";
					vPW.write(vS);
//					vLogLikelihoods[vYCounter][vXCounter] = calculateLogLikelihood(aObservationImage,vIdealImage,vBitmap);
				}
			}

//			int vXMaxIndex = 0;
//			int vYMaxIndex = 0;
//			for(int vY = 0; vY < vLogLikelihoods.length; vY++) {
//				for(int vX = 0; vX < vLogLikelihoods[0].length; vX++) {
//					if(vLogLikelihoods[vY][vX] > vLogLikelihoods[vYMaxIndex][vXMaxIndex]){
//						vXMaxIndex = vX;
//						vYMaxIndex = vY;
//					}
//				}
//			}
//			float vMaxValue = vLogLikelihoods[vYMaxIndex][vXMaxIndex];		                                
//			for(int vY = 0; vY < vLogLikelihoods.length; vY++) {
//				for(int vX = 0; vX < vLogLikelihoods[0].length; vX++) {
//					vLogLikelihoods[vY][vX] -= vMaxValue;				
//				}
//			}
//			WriteArrayToFile(vLogLikelihoods, "C:\\loglikelihoods_param_d_beta.txt");
//			new TextWindow("Likelihoods", vS, 200, 800);
			//System.out.print("\n");
//			new ImageWindow(new ImagePlus("D-Beta-LogLikelihoods",new FloatProcessor(vLogLikelihoods)));
//			float[][] vLikelihoods = new float[vLogLikelihoods.length][vLogLikelihoods[0].length];
//			for(int vY = 0; vY < vLogLikelihoods.length; vY++) {
//				for(int vX = 0; vX < vLogLikelihoods[0].length; vX++) {
//					vLikelihoods[vY][vX] = (float)Math.exp(vLogLikelihoods[vY][vX]);				
//				}
//			}
//			new ImageWindow(new ImagePlus("xyLogLikelihoods",new FloatProcessor(vLogLikelihoods)));
//			new ImageWindow(new ImagePlus("xyLikelihoods",new FloatProcessor(vLikelihoods)));
			vPW.close();
		}
		else {
			float[][] vLSLikelihoods = new float[200][200];
			int vXCounter = 0;
			int vYCounter = 0;
			for(float vY = 19.5f; vY < 21.5; vY = vY + 0.01f){
//				IJ.showProgress((vY-19.5f)/2f);
				for(float vX = 5f ; vX < 7f; vX = vX + .01f){

					float[][] vIdealImage = generateIdealImage(aObservationImage.getWidth(), 
							aObservationImage.getHeight(),
							new float[]{vX, vY, 9.03f, 5.43f, 7.72f, -0.49f, 43.68f, 41.51f},//optimal values: 6.22	20.63	9.03	5.43	7.72	-0.49	43.68	41.51
							1);	
//					vS += vX + " " + calculateLogLikelihood(aObservationImage,vIdealImage,vBitmap);//vIdealImage[vY][vX];
//					vS += "\n";
					vLSLikelihoods[vYCounter][vXCounter] = CalculateLSLikelihood(aObservationImage,vIdealImage);
					vXCounter++;
				}
				vYCounter++;
				vXCounter = 0;
			}
			new ImageWindow(new ImagePlus("xy LeastSquares Likelihoods",new FloatProcessor(vLSLikelihoods)));
		}
	}	

	/**
	 * Estimates all state vectors from the particles and their weights	 
	 */
	private void EstimateStateVectors(Vector<float[]> aStateVectors, Vector<Vector<float[]>> aParticles){
		for(int vFPIndex = 0; vFPIndex < aStateVectors.size(); vFPIndex++){
			float[] vState = aStateVectors.get(vFPIndex);
			Vector<float[]> vFeaturePointParticles = aParticles.get(vFPIndex);

			/* Set the old state to 0*/
			for(int vI = 0; vI < vState.length; vI++)
				vState[vI] = 0f;

			for(float[] vParticle : vFeaturePointParticles){
				for(int vDim = 0; vDim < vState.length; vDim++) {
					vState[vDim] += vParticle[vState.length] * vParticle[vDim];
				}
			}
		}
	}


	/**
	 * @param aW The Width of the image to generate
	 * @param aH The Height of the image to generate
	 * @param aBackground The Background of the image to generate
	 * @param aParticle A particle for which the image is generated
	 * @return a Image with a gaussian blob as an intensity at position of the particle
	 */
//	boolean mTestFirst = true;
	private float[][] generateIdealImage(int aW, int aH, float[] aParticle, int aBackground){
		float[] tempArray = null;
		if(mDoMonitorIdealImage)
			tempArray = (float[])mIdealImageMonitorProcessor.getPixels();
		float vIdealImage[][] = new float[aH][aW];
		//float vBackground = mBackground;
		for(int vY = 0; vY < aH; vY++){
			for(int vX = 0; vX < aW; vX++){
				vIdealImage[vY][vX] = mBackground;		//????????????to slow!	
//				if(mDoMonitorIdealImage)
//				tempArray[vY*mWidth+vX] += 140f;
			}
			//Arrays.fill(vIdealImage[vY], 0);
		}

		int vLeft, vRight, vTop, vBottom;
		float vPointX = aParticle[0] + (aParticle[2]/2f) * (float)Math.cos(aParticle[3]) + aParticle[4] * (float)Math.cos(aParticle[3] + aParticle[5]);
		float vPointY = aParticle[1] + (aParticle[2]/2f) * (float)Math.sin(aParticle[3]) + aParticle[4] * (float)Math.sin(aParticle[3] + aParticle[5]);
		//calculate the boundaries for the gaussian blob to increase performance
		//int mLikelihoodRadius = 5;//5 IS OK, larger doesn't make sense ->result is 0.0		
		if(vPointY + .5f - mLikelihoodRadius < 0) vBottom = 0; else vBottom = (int)(vPointY + .5f) - mLikelihoodRadius;
		if(vPointX + .5f - mLikelihoodRadius < 0) vLeft = 0; else vLeft = (int)(vPointX + .5f) - mLikelihoodRadius;
		if(vPointY + .5f + mLikelihoodRadius >= aH) vTop = aH - 1; else vTop = (int)(vPointY + .5f) + mLikelihoodRadius;
		if(vPointX + .5f  + mLikelihoodRadius >= aW) vRight = aW - 1; else vRight = (int)(vPointX + .5f) + mLikelihoodRadius;

		for(int vY = vBottom; vY <= vTop; vY++){
			for(int vX = vLeft; vX <= vRight; vX++){
				vIdealImage[vY][vX] += /*aBackground +*/ (float) (aParticle[7] * Math.pow(Math.E, 
						-(Math.pow(vX - vPointX + .5f, 2) + Math.pow(vY - vPointY + .5f, 2)) / (2 * mSigmaPSF * mSigmaPSF)));//TODO: 2 oder 4 *sigma^2 ??
				if(mDoMonitorIdealImage){
					tempArray[vY*mWidth+vX] += vIdealImage[vY][vX];
				}
			}
		}			

		//
		// Add the line(s)
		//
		Line2D.Float vLine = new Line2D.Float(
				aParticle[0] - (aParticle[2]/2f)*(float)Math.cos(aParticle[3]), 
				aParticle[1] - (aParticle[2]/2f)*(float)Math.sin(aParticle[3]), 
				aParticle[0] + (aParticle[2]/2f)*(float)Math.cos(aParticle[3]),
				aParticle[1] + (aParticle[2]/2f)*(float)Math.sin(aParticle[3]));
		
		Line2D.Float vRestIntensityLine = new Line2D.Float(
				aParticle[0] + (aParticle[2]/2f)*(float)Math.cos(aParticle[3]),
				aParticle[1] + (aParticle[2]/2f)*(float)Math.sin(aParticle[3]),
				vPointX,
				vPointY
		);
		for(int vY = 0; vY < aH; vY++){
			for(int vX = 0; vX < aW; vX++){
				Point2D.Float vPoint = new Point2D.Float(vX + .5f, vY + .5f);
				float vDist;
				if((vDist = (float)vLine.ptSegDist(vPoint)) < 3 * mSigmaPSF || vDist <= 3){
					vIdealImage[vY][vX] +=  (float) (aParticle[6] * Math.pow(Math.E, 
							- vDist*vDist / (2 * mSigmaPSF * mSigmaPSF)));
				}
				/**
				 * test for tubulin
				 */
				if((vDist = (float)vRestIntensityLine.ptSegDist(vPoint)) < 3 * mSigmaPSF || vDist <= 3){
					vIdealImage[vY][vX] +=  (float) (aParticle[7]* mRestIntensity * Math.pow(Math.E, 
							- vDist*vDist / (2 * mSigmaPSF * mSigmaPSF)));
				}
//				if(mDoMonitorIdealImage){
//					tempArray[vY*mWidth+vX] += vIdealImage[vY][vX];
//				}
			}
		}	

//		if(mTestFirst){
//			mTestFirst = false;
//			new ImageWindow(new ImagePlus("Single ideal image",new FloatProcessor(vIdealImage)));
//		}
		return vIdealImage;
	}

	/**
	 * 
	 * @param aLength: length of the 1D blob i.e. #of slices
	 * @param aPosition: 1 <= aPosition <= nSlices
	 * @param aIntensity: maximal intensity value
	 * @param aBackground: >= 1
	 * @return a Gaussian blob at position <code>aPosition</code>
	 */
	private float[] generateIdealImage1D(int aLength, float aPosition, float aIntensity, float aBackground) {
		float[] vIdealImage = new float[aLength];
		float vZScale = (float)(mOriginalImagePlus.getCalibration().pixelDepth / mOriginalImagePlus.getCalibration().pixelWidth);//TODO:do that in the setup
		float vSigmaPSF = mSigmaPSF / vZScale;
		for(int vX = 0; vX < aLength; vX++) {
			vIdealImage[vX] += aBackground + (float) (aIntensity * Math.pow(Math.E, 
					-(Math.pow(vX - aPosition, 2)) / (2 * vSigmaPSF * vSigmaPSF)));
		}
		return vIdealImage;
	}

	/**
	 * Calculates the likelihood by multipling the poissons marginals around a particle given a image(optimal image)
	 * @param aImageProc: The observed image
	 * @param aGivenImage: a intensity array, the 'measurement'
	 * @param aX: The particles x-coordinate
	 * @param aY: The particles y-coordinate
	 * @param aBitmap: Pixels which are not set to true in the bitmap are pulled out(proportionality)
	 * @return the likelihood for the image given 
	 */
	
	private float calculateLogLikelihood(ImageProcessor aImageProc, float[][] aGivenImage, boolean[][] aBitmap) {//ImageStack aImageStack){
		
		float vLogLikelihood = 0;	
//			mBW.write("new likelihoodcalculation\n");
			for(int vY = 0; vY < mHeight; vY++){
				for(int vX = 0; vX < mWidth; vX++){			
					if(aBitmap[vY][vX]){					
						vLogLikelihood += -aGivenImage[vY][vX] + (float)aImageProc.getf(vX, vY) * (float)Math.log(aGivenImage[vY][vX]);
//						mBW.write(vLogLikelihood + " " + aGivenImage[vY][vX] + " " + aImageProc.get(vX, vY)+"\n");
					}
				}
			}		
		return vLogLikelihood;
		
	}
	/**
	 * Same as <code>calculateLogLikelihood</code> but only 1 dimensional (for z) and no support for a bitmap.
	 * @param data
	 * @param aGivenImage1D
	 * @param aFrameIndex 1<= aFrameIndex <= mNFrames
	 * @return the logLikelihood a Image given another image according to a poisson distribution.
	 */
	private float calculateZCoordLogLikelihood(float[] data, float[] aGivenImage1D, int aFrameIndex){
		float vLogLikelihood = 0f;
		for(int vS = 0; vS < mNSlices; vS++) {
			vLogLikelihood += -aGivenImage1D[vS] + data[vS] * (float)Math.log(aGivenImage1D[vS]);
		}
		return vLogLikelihood;
	}
	
	/**
	 * 
	 * @param aState
	 * @param aX
	 * @param aY
	 * @return 1,2 or 3 for SPB1, SPB2 resp. Tip
	 */
	public int getNearestBioobject(float[] aState, float aX, float aY){
		Point2D.Float vClickP = new Point.Float(aX,aY);
		Point2D.Float[] vPoints = getPointsFromState(aState);
		float vD1 = (float)vClickP.distance(vPoints[0]);
		float vD2 = (float)vClickP.distance(vPoints[1]);
		float vDT = (float)vClickP.distance(vPoints[2]);
		float vMinDist = Math.min(vD1, vD2);
		vMinDist = Math.min(vMinDist, vDT);
		if(vMinDist == vD1)
			return 1;
		if(vMinDist == vD2)
			return 2;
		if(vMinDist == vDT)
			return 3;
		return 0;
	}

	public Point2D.Float[] getPointsFromState(float[] aState) {
		Point2D.Float[] vRes = new Point2D.Float[3];
		vRes[0] = new Point2D.Float((float)(aState[0] - (aState[2]/2f) * Math.cos(aState[3])),(float)(aState[1] - (aState[2]/2f) * Math.sin(aState[3])));
		vRes[1] = new Point2D.Float((float)(aState[0] + (aState[2]/2f) * Math.cos(aState[3])),(float)(aState[1] + (aState[2]/2f) * Math.sin(aState[3])));
		vRes[2] = new Point2D.Float(vRes[1].x + aState[4] * (float)Math.cos(aState[5] + aState[3]), vRes[1].y + aState[4] * (float)Math.sin(aState[5] + aState[3]));
		return vRes;
		
	}
	
	private float[] interpolateRay(ImageStack aIS, float aX, float aY, int aFrameIndex) {
		float[] vRes = new float[mNSlices];
		float vXOff = aX - (int)aX;
		float vYOff = aY - (int)aY;
		int vXIndexOff = 1;
		int vYIndexOff = 1;
		float vCoeffX = (1f - vXOff) + .5f;
		float vCoeffY = (1f - vYOff) + .5f;
		if(vXOff < .5) {
			vXIndexOff *= -1;
			vCoeffX = .5f + vXOff;
		}

		if(vYOff < .5) {
			vYIndexOff *= -1;
			vCoeffY = .5f + vYOff;
		}
		ImageProcessor vProc = null;	
		if(aY + vYIndexOff >= mHeight || aY + vYIndexOff < 0)
			vYIndexOff = 0;
		if(aX + vXIndexOff >= mWidth || aX + vXIndexOff < 0)
			vXIndexOff = 0;
		for(int vS = 0; vS < mNSlices; vS++) {			
			vProc = aIS.getProcessor((aFrameIndex-1)*mNSlices+(vS+1));
			vRes[vS] = vCoeffX * vCoeffY * vProc.getf((int)aX, (int)aY)
			+ vCoeffX * (1-vCoeffY) * vProc.getf((int)aX, (int)aY + vYIndexOff)
			+ (1-vCoeffX) * vCoeffY * vProc.getf((int)aX + vXIndexOff, (int)aY)
			+ (1-vCoeffX) * (1-vCoeffY) * vProc.getf((int)aX + vXIndexOff, (int)aY + vYIndexOff);
		}
		return vRes;
	}

	/**
	 * Draws new particles, the last entry of the argument is supposed to be the weight and remains unchanged.
	 * @param aParticle A Array with state vector entries + a weight entry(remains unchanged)
	 */
	private void DrawFromProposalDistribution(float[] aParticle) {
		RandomWalkProposal(aParticle);
	}

	/**
	 * @param aParticle Some state vector with a weight as a last element appended.
	 */
	private void RandomWalkProposal(float[] aParticle){
		//float vVariance = mSigmaOfRandomWalk * mSigmaOfRandomWalk;
		for(int vI = 0; vI < aParticle.length - 1 ; vI++){
			aParticle[vI] = aParticle[vI] + (float)mRandomGenerator.nextGaussian() * (mSigmaScaler  * mSigmaOfRandomWalkA[vI]);
		}
		//the intensities must not be < 0
		if(aParticle[6] < mBackground) aParticle[6] = mBackground + 1;
		if(aParticle[7] < mBackground) aParticle[7] = mBackground + 1;
		//the length should not be < 0 //TODO: truncated gaussians
		if(aParticle[2] < 0) aParticle[2] *= -1;
		//hold the angles between 0 and 2PI
		aParticle[3] = (float)(aParticle[3] % (Math.PI*2));
		aParticle[5] = (float)(aParticle[5] % (Math.PI*2));
		
		//validate(just a check that the tip all 3 objects do not leave the image space)
		//TODO:etwas schlauer...k�nnte lange dauern
//		float vSPB2x = aParticle[0] + (aParticle[2]/2f)*(float)Math.cos(aParticle[3]);
//		float vSPB2y = aParticle[1] + (aParticle[2]/2f)*(float)Math.sin(aParticle[3]);
//		float vTipx = vSPB2x + aParticle[4]*(float)Math.cos(aParticle[3] + aParticle[5]);
//		float vTipy = vSPB2y + aParticle[4]*(float)Math.sin(aParticle[3] + aParticle[5]);
//		if(vTipx < 0 || vTipx > mWidth || vTipy < 0 || vTipy > mHeight){
//			
//		}
		
	}	

	private void initVisualization(){
//		generate the previewCanvas - while generating it the drawing will be done 
		DrawCanvas vDrawCanvas = new DrawCanvas(mZProjectedImagePlus);
		
		// display the image and canvas in a stackWindow  
		new TrajectoryStackWindow(mZProjectedImagePlus, vDrawCanvas);
//		vDrawCanvas.zoomIn(mWidth/2, mHeight/2);
//		vZWin.repaint();
		

	}

	@SuppressWarnings("serial")
	private class DrawCanvas extends ImageCanvas {
		public DrawCanvas(ImagePlus aImagePlus){			
			super(aImagePlus);
		}
		public void paint(Graphics aG){
			//double vMagnificationMemory = this.getMagnification();			
			super.paint(aG);
			PaintLines(aG);
//			this.addMouseListener(this);

		}

//		public void mouseClicked(MouseEvent aEvent) {
////		super.mouseClicked(aEvent);
//		IJ.showMessage("mouse clicked");

//		}

		private void PaintLines(Graphics aG){			
			int vCurrentFrame = mZProjectedImagePlus.getCurrentSlice();
			if(mStateVectorsMemory.elementAt(vCurrentFrame - 1) != null) {
				try{
					for(float[] vState : mStateVectorsMemory.elementAt(vCurrentFrame - 1)){
						aG.setColor(Color.MAGENTA);
						int vBodyX = (int)Math.round(vState[0]*magnification);
						int vBodyY = (int)Math.round(vState[1]*magnification);
						aG.drawLine(vBodyX - 5, vBodyY, vBodyX + 5, vBodyY);
						aG.drawLine(vBodyX, vBodyY - 5, vBodyX, vBodyY + 5);						
						aG.setColor(Color.yellow);
						aG.drawLine(
								(int)((vState[0] - (vState[2]/2f) * Math.cos(vState[3])) * magnification + 0.5f), 
								(int)((vState[1] - (vState[2]/2f) * Math.sin(vState[3])) * magnification + 0.5f), 
								(int)((vState[0] + (vState[2]/2f) * Math.cos(vState[3])) * magnification + 0.5f), 
								(int)((vState[1] + (vState[2]/2f) * Math.sin(vState[3])) * magnification + 0.5f));
						aG.setColor(Color.cyan);
						int vPointX = (int)((vState[0] + (vState[2]/2f) * Math.cos(vState[3]) + vState[4] * Math.cos(vState[5] + vState[3])) * magnification + 0.5f);
						int vPointY = (int)((vState[1] + (vState[2]/2f) * Math.sin(vState[3]) + vState[4] * Math.sin(vState[5] + vState[3])) * magnification + 0.5f);
						aG.drawLine(
								(int)((vState[0] + (vState[2]/2f) * Math.cos(vState[3]))  * magnification + 0.5f), 
								(int)((vState[1] + (vState[2]/2f) * Math.sin(vState[3])) * magnification + 0.5f), 
								vPointX, 
								vPointY);
						aG.setColor(Color.green);
						aG.drawLine(vPointX - 5, vPointY, vPointX + 5, vPointY);
						aG.drawLine(vPointX, vPointY - 5, vPointX, vPointY + 5);
					}
				}catch(java.lang.NullPointerException aE){
					//nothing
				}
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

		public void paint(Graphics g){
			super.paint(g);
			try{
				int vFeaturePointInd = 1;
				for(Vector<float[]> vFeaturePoint : mParticleMonitor.elementAt(mImagePlus.getCurrentSlice()-1)){
					g.setColor(new Color(vFeaturePointInd*1000));
					vFeaturePointInd++;
					for (float[] vParticle : vFeaturePoint) {
						g.setColor(Color.yellow);
						g.drawRect((int)(magnification * vParticle[0] + .5f),(int) (magnification * vParticle[1] + .5f), 1, 1);
						g.setColor(Color.cyan);
						g.drawRect((int)(magnification * (vParticle[0] + vParticle[2] * Math.cos(vParticle[3]))+ .5f),
								(int) (magnification * (vParticle[1] + vParticle[2] * Math.sin(vParticle[3])) + .5f), 1, 1);
						g.setColor(Color.green);
						g.drawRect((int)(magnification * (vParticle[0] + vParticle[2] * Math.cos(vParticle[3]) + vParticle[4] * Math.cos(vParticle[5] + vParticle[3])) + .5f),
								(int) (magnification * (vParticle[1] + vParticle[2] * Math.sin(vParticle[3]) + vParticle[4] * Math.sin(vParticle[5] + vParticle[3])) + .5f), 1, 1);
					}
				}
			}
			catch(java.lang.NullPointerException vE){
				//do nothing
			}
		}
	}

	private int SliceToFrame(int aSlice){
		if(aSlice < 1)
			System.err.println("error in particle filter in SliceToFrame: < 1" );
		return (int)(aSlice-1)/mOriginalImagePlus.getNSlices() + 1;
	}


	private Vector<float []> CopyStateVector(Vector<float[]> aOrig){
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

	private Vector<Vector<float[]>> CopyParticleVector(Vector<Vector<float[]>> aOrig){
		Vector<Vector<float[]>> vResVector = new Vector<Vector<float[]>>(aOrig.size());
		for(Vector<float[]> vP : aOrig){
			vResVector.add(CopyStateVector(vP));
		}
		return vResVector;
	}

	private void PrintStatesToWindow(String aTitle, Vector<Vector<float[]>> aStateVectorsMem, 
			Vector<Vector<float[]>> aZVector, Vector<Float> aQualityMeasure){
		new TextWindow(aTitle, GenerateOutputString(aStateVectorsMem, aZVector, aQualityMeasure), 400, 400);
	}

	/**
	 * 
	 * @param aStateVectorsMem a full or sparse vector of states
	 * @param aZVector may be null
	 * @param aQualityMeasure may be null
	 * @return a table of all results
	 */
	private String GenerateOutputString(Vector<Vector<float[]>> aStateVectorsMem, 
			Vector<Vector<float[]>> aZVector, Vector<Float> aQualityMeasure){
		
		String vOut = "# Nb of particles: " + mNbParticles;
		vOut += "\n# RandomWalk Sigma: ";
		for(int vI = 0; vI < mSigmaOfRandomWalkA.length; vI++)
			vOut += mSigmaOfRandomWalkA[vI]+",";
		vOut += "\n# Background Intensity: " + mBackground;
		vOut += "\n# Sigma of PSF: " + mSigmaPSF;
		vOut += "\n# Restintesity: " + mRestIntensity;
		vOut += "\n# mSeed: " + mSeed;
		vOut += "\n# output: frame,x,y,L1,Alpha,L2,Beta,Intensity of Line,Intensity of Point,z of SPB1,z of SPB2,zTip\n";
		int vNObjects = 0;
		for(int vI = 0; vI < aStateVectorsMem.size(); vI++) {
			if(aStateVectorsMem.elementAt(vI) != null) {
				vNObjects = aStateVectorsMem.elementAt(vI).size();
				break;
			}
		}
		for(int vI = 0; vI < vNObjects; vI++){
			int vFrameC = 0;
			for(Vector<float[]> vFrame : aStateVectorsMem){
				if(vFrame == null) {
					vFrameC++;
					continue;
				}
				vFrameC++;
				vOut += "\n" + vFrameC + ",";
				for(float vV :vFrame.elementAt(vI)){
					vOut += vV + ",";
				}
				if(aZVector != null) {
					for(float aZ: aZVector.elementAt(vFrameC-1).elementAt(vI)) {
						vOut += aZ + ",";
					}
				}
				if(aQualityMeasure != null && aQualityMeasure.elementAt(vFrameC-1) != null) {
					vOut += aQualityMeasure.elementAt(vFrameC-1);
				}
			}
		}
		return vOut;
	}
	
	public void CalculateGaussKernel(float[] aKernel){
		int vM = aKernel.length/2;
		for(int vI = 0; vI < vM; vI++){
			aKernel[vI] = (float)(1f/(Math.sqrt(2f*Math.PI)*mGaussBlurRadius) * 
					Math.exp(-(float)((vM-vI)*(vM-vI))/(2f*mGaussBlurRadius*mGaussBlurRadius)));
			aKernel[aKernel.length - vI - 1] = aKernel[vI];
		}
		aKernel[vM] = (float)(1f/(Math.sqrt(2f*Math.PI)*mGaussBlurRadius));		
	}

	public void WriteArrayToFile(float[][] aArray, String aFileName){
		PrintWriter vPW = null;
		try{
			BufferedWriter vBW = new BufferedWriter(new FileWriter(aFileName));
			vPW = new PrintWriter(vBW);
			for(int vI = 0; vI < aArray.length; vI++){
				for(int vJ = 0; vJ < aArray[0].length; vJ++){
					vPW.println(vI + " " + vJ + " " + aArray[vI][vJ]);
				}
			}
		}catch (IOException aIOE){
			IJ.error("unable to write to file");
		}
		finally {
			vPW.close();
		}
	}

	boolean GetUserDefinedParams() {
		GenericDialog vGenericDialog = new GenericDialog("Line-Point Tracker Params",IJ.getInstance());
		vGenericDialog.addNumericField("# of particles ~ quality", mNbParticles, 0);
		vGenericDialog.addNumericField("Sigma of the PSF", mSigmaPSF, 2);		
		vGenericDialog.addNumericField("Background Intensity", mBackground, 0);
		vGenericDialog.addNumericField("Restintensity", mRestIntensity, 2);
		vGenericDialog.addMessage("Sigmas of the state vector entries in pixel: ");

		vGenericDialog.addNumericField("x pos", mSigmaOfRandomWalkA[0], 2);
		vGenericDialog.addNumericField("y pos", mSigmaOfRandomWalkA[1], 2);
		vGenericDialog.addNumericField("Length", mSigmaOfRandomWalkA[2], 2);
		vGenericDialog.addNumericField("alpha", mSigmaOfRandomWalkA[3], 2);
		vGenericDialog.addNumericField("Distance", mSigmaOfRandomWalkA[4], 2);
		vGenericDialog.addNumericField("beta", mSigmaOfRandomWalkA[5], 2);
		vGenericDialog.addNumericField("Line Intensity", mSigmaOfRandomWalkA[6], 2);
		vGenericDialog.addNumericField("Point Intensity", mSigmaOfRandomWalkA[7], 2);

		vGenericDialog.addCheckbox("Print_results", mDoPrintStates);

		vGenericDialog.addMessage("Parameters for the initialization");
		vGenericDialog.addNumericField("PF Iterations on 1st frame", mInitParticleFilterIterations, 0);
		vGenericDialog.addCheckbox("Print_init results(for testing only): ", mDoPrintInitStates);

		vGenericDialog.showDialog();

		mNbParticles = (int)vGenericDialog.getNextNumber();
		mSigmaPSF = (float)vGenericDialog.getNextNumber();
		mBackground = (int)vGenericDialog.getNextNumber();
		mRestIntensity = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[0] = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[1] = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[2] = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[3] = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[4] = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[5] = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[6] = (float)vGenericDialog.getNextNumber();
		mSigmaOfRandomWalkA[7] = (float)vGenericDialog.getNextNumber();
		mDoPrintStates = vGenericDialog.getNextBoolean();
		mInitParticleFilterIterations = (int)vGenericDialog.getNextNumber();
		mDoPrintInitStates = vGenericDialog.getNextBoolean();

		mGaussBlurRadius = mSigmaPSF;
		mLikelihoodRadius = 3 * ((int)mSigmaPSF+1);
		mRandomGenerator = new Random(mSeed);

		if(vGenericDialog.wasCanceled())
			return false;

		return true;
	}

//	private AtomicInteger mControllingParticleIndex = new AtomicInteger(0);
	private int mControllingParticleIndex = 0;
	/**
	 * 
	 * @author Janigo
	 */
	private class ParallelizedLikelihoodCalculator extends Thread {
		float[] mResultArray;//we only write to the array, there should be no conflicts
		Vector<float[]> mParticles;
		ImageProcessor mObservedImage;
		boolean[][] mBitmap;
		/**
		 * Calculates likelihoods for particles given a image and writes them in the result array. 
		 * There are two options: 1. LeastSquares likelihoods(negative!) and 2. Poisson LOG(!) Likelihoods.
		 * DO FIRST CONSTRUCT ALL THREADS BEFORE RUNNING THE FIRST ONE!
		 * @param aImageProcessor: The image to operate on, i.e. the observed image
		 * @param aRusultArray: the results are written in this array in the same order the particles are in <code>aParticles</code>
		 * @param aParticles: the particles to score
		 *
		 */
		public ParallelizedLikelihoodCalculator(ImageProcessor aImageProcessor, float[] aRusultArray, Vector<float[]> aParticles){
			mResultArray = aRusultArray;
			mParticles = aParticles;
			mObservedImage = aImageProcessor;
			mBitmap = new boolean[mHeight][mWidth]; 
			for(int vY = 0; vY < mHeight; vY++)
				for(int vX = 0; vX < mWidth; vX++)
					mBitmap[vY][vX] = true;
			// The next line is only ok if the threads are first constructed and run afterwards!
			mControllingParticleIndex = 0;

		}

		public void run() {
			int vI;
			while((vI = getNewParticleIndex()) != -1 ) {
//				if(vI == 1) {
//				System.out.println("run stop");
//				}
				//get the particle
			
				float[] vParticle = mParticles.elementAt(vI);						
					
				//calculate ideal image
				float[][] vIdealImage = generateIdealImage(mWidth, 
						mHeight,
						vParticle,
						(int)(mBackground + .5));	//should be auto-threshold of the next image!
				//calculate likelihood
				if(mLSLikelihood) { 
					mResultArray[vI] = CalculateLSLikelihood(mObservedImage, vIdealImage);
				}else {
					mResultArray[vI] = calculateLogLikelihood(mObservedImage, vIdealImage, mBitmap);
				}
			}
		}

		synchronized int getNewParticleIndex(){
			if(mControllingParticleIndex < mNbParticles && mControllingParticleIndex >= 0){
				mControllingParticleIndex++;
				return mControllingParticleIndex - 1;
			}
			mControllingParticleIndex = -1;
			return -1;
		}
				
	}	

	private class TrajectoryStackWindow extends StackWindow implements ActionListener, MouseListener{

        private static final long serialVersionUID = 1L;
        //		private static final long serialVersionUID = 1L;
		private Button mCalcFromHereButton;
		private Button mMouseInitializationButton;
		private Button mSaveInitButton;
		private Button mClearDataButton;
		private Button mSaveCorrectionButton;

		private Point mSPB1, mSPB2, mTip;
		int mPointToCorrect = 0;
		
		/**
		 * Constructor.
		 * <br>Creates an instance of TrajectoryStackWindow from a given <code>ImagePlus</code>
		 * and <code>ImageCanvas</code> and a creates GUI panel.
		 * <br>Adds this class as a <code>MouseListener</code> to the given <code>ImageCanvas</code>
		 * @param aImagePlus
		 * @param aImageCanvas
		 */
		private TrajectoryStackWindow(ImagePlus aImagePlus, ImageCanvas aImageCanvas) {
			
			super(aImagePlus, aImageCanvas);
			aImageCanvas.addMouseListener(this);
			addPanel();
		}

		/**
		 * Adds a Panel with filter options button in it to this window 
		 */
		private void addPanel() {
			Panel vButtonPanel = new Panel(new GridLayout(4,2));
			mCalcFromHereButton = new Button("Calculate");
			mSaveInitButton = new Button("Save init position");
			mClearDataButton = new Button("Delete all");
			mSaveCorrectionButton = new Button("Save corrections");
			mMouseInitializationButton = new Button("Initialize with mouse");
			mCalcFromHereButton.addActionListener(this);
			mSaveInitButton.addActionListener(this);
			mClearDataButton.addActionListener(this);
			mMouseInitializationButton.addActionListener(this);
			mSaveCorrectionButton.addActionListener(this);
			
			vButtonPanel.add(mCalcFromHereButton);
			vButtonPanel.add(mSaveInitButton);
			vButtonPanel.add(mSaveCorrectionButton);
			vButtonPanel.add(mClearDataButton);
			vButtonPanel.add(mMouseInitializationButton);
			vButtonPanel.add(mSaveCorrectionButton);
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
		public synchronized void actionPerformed(ActionEvent aEvent) {
			Object vButton = aEvent.getSource();
			
			/*
			 * Calculate Button
			 */
			if (vButton == mCalcFromHereButton) { 
				//relaunch the filter from the current slice on with the current initialization if there is one
//				mFrameOfInitialization = this.getImagePlus().getCurrentSlice();
				if(mStateOfFilter != STATE_OF_FILTER.VISUALIZING && mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN){
					IJ.showMessage("No initialization done yet.");
					return;
				}				
				if(mStateOfFilter == STATE_OF_FILTER.VISUALIZING){ //restart from corrected frame
					if(mStateVectorsMemory.elementAt(mZProjectedImagePlus.getCurrentSlice()-1) != null) {
						//first setup the state vector then run
						mStateVectors = CopyStateVector(mStateVectorsMemory.elementAt(mZProjectedImagePlus.getCurrentSlice() - 1));
						mFrameOfInitialization = mZProjectedImagePlus.getCurrentSlice();
						PointLineParticleFilter_.this.run(new FloatProcessor(1,1));
					}
				}				
				if(mStateOfFilter == STATE_OF_FILTER.READY_TO_RUN) {
					//we're sure that there is a correct initialization at a certain frame
					PointLineParticleFilter_.this.run(new FloatProcessor(1,1));
				}
			}
			
			/*
			 * Save init button
			 */
			if (vButton == mSaveInitButton) {
				if(mStateOfFilter != STATE_OF_FILTER.VISUALIZING && mStateOfFilter != STATE_OF_FILTER.READY_TO_RUN){					
					IJ.showMessage("No initialization done yet.");
					return;
				}
				if(mOriginalImagePlus.getOriginalFileInfo() == null) {
					IJ.showMessage("The movie has no origin. Save it first.");
					return;
				}
				mFrameOfInitialization = this.getImagePlus().getCurrentSlice();
				//write out the init positions in a file in the same directory
							
				writeInitFile(getInitFile());
				
			}
			
			/*
			 * Save results button
			 */
			if (vButton == mSaveCorrectionButton) {
				if(mOriginalImagePlus.getOriginalFileInfo() == null) {
					IJ.showMessage("The movie has no origin. Save it first.");
					return;
				}
				
				//write out the positions in the res file
							
				writeResultFile(getResultFile());
				
			}
			
			/*
			 * Clear Button
			 */
			if(vButton == mClearDataButton) {
				//clear the memory vectors(also zmem) and repaint the window.
				mStateVectorsMemory.clear();
				mZCoordinates.clear();	
				mStateVectors.clear();
				mQualityMeasure.clear();
				mStateVectorsMemory.setSize(mNFrames);
				mZCoordinates.setSize(mNFrames);
				mQualityMeasure.setSize(mNFrames);
				getInitFile().delete();
				getResultFile().delete();
				mStateOfFilter = STATE_OF_FILTER.WAITING;
				mZProjectedImagePlus.repaintWindow();
			}
			
			/*
			 * initialze with mouse button
			 */
			if(vButton == mMouseInitializationButton) {
				IJ.setTool(Toolbar.LINE);
				mStateOfFilter = STATE_OF_FILTER.INIT_STEP1;
			}
			
			// generate an updated view with the ImagePlus in this window according to the new filter
//			generateView(this.imp);
		}

		/** 
		 * Defines the action taken upon an <code>MouseEvent</code> triggered by left-clicking 
		 * the mouse anywhere in this <code>TrajectoryStackWindow</code>
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public synchronized void mousePressed(MouseEvent e) {
			switch(mStateOfFilter) {
			case VISUALIZING:
				//correction mode	//TODO: correction mode	
				if(IJ.shiftKeyDown() && mStateVectorsMemory.elementAt(mZProjectedImagePlus.getCurrentSlice()-1) != null){
					mStateOfFilter = STATE_OF_FILTER.CORRECTING;
					int vX = this.ic.offScreenX(e.getPoint().x);
					int vY = this.ic.offScreenX(e.getPoint().y);
					mPointToCorrect = getNearestBioobject(mStateVectorsMemory.elementAt(mZProjectedImagePlus.getCurrentSlice()-1).elementAt(0),vX,vY);
					System.out.println("nearest Point: " + mPointToCorrect);
				}
				break;
			case INIT_STEP1:
				//SPB1 set
				mStateOfFilter = STATE_OF_FILTER.INIT_STEP2;
				mSPB1 = new Point(this.ic.offScreenX(e.getPoint().x),this.ic.offScreenY(e.getPoint().y));
				break;
			case INIT_STEP2:
				//see mouseReleased
				break;
			case INIT_STEP3:
				mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
				//Tip set
				mTip = new Point(this.ic.offScreenX(e.getPoint().x),this.ic.offScreenY(e.getPoint().y));
				//set up the state vector and store it
				mFrameOfInitialization = this.getImagePlus().getCurrentSlice();
				setupStateVectorFromPoints(mSPB1.x, mSPB1.y, mSPB2.x, mSPB2.y, mTip.x, mTip.y, mZProjectedImagePlus.getProcessor());
				mStateVectorsMemory.setElementAt(CopyStateVector(mStateVectors), mFrameOfInitialization-1);
				mZProjectedImagePlus.repaintWindow();
				break;		
			default:
			    break;
			}	

		}

		public void mouseClicked(MouseEvent e) {
			// Auto-generated method stub
		}


		public void mouseEntered(MouseEvent arg0) {
			// Auto-generated method stub			
		}


		public void mouseExited(MouseEvent arg0) {
			// Auto-generated method stub			
		}

		public void mouseReleased(MouseEvent e) {
			// Auto-generated method stub			
			if(mStateOfFilter == STATE_OF_FILTER.INIT_STEP2) {
				mStateOfFilter = STATE_OF_FILTER.INIT_STEP3;
				mSPB2 = new Point(this.ic.offScreenX(e.getPoint().x),this.ic.offScreenY(e.getPoint().y));
				IJ.setTool(Toolbar.POINT);
			}
			if(mStateOfFilter == STATE_OF_FILTER.CORRECTING) {
				mStateOfFilter = STATE_OF_FILTER.VISUALIZING;
				Point2D.Float[] vPoints = getPointsFromState(mStateVectorsMemory.elementAt(mZProjectedImagePlus.getCurrentSlice()-1).elementAt(0));
				Point vClicked = new Point(this.ic.offScreenX(e.getPoint().x),this.ic.offScreenY(e.getPoint().y));
				if(mPointToCorrect == 1) {
					setupStateVectorFromPoints(vClicked.x, vClicked.y, vPoints[1].x, vPoints[1].y, vPoints[2].x, vPoints[2].y, mZProjectedImagePlus.getProcessor());
				}
				if(mPointToCorrect == 2) {
					setupStateVectorFromPoints(vPoints[0].x, vPoints[0].y, vClicked.x, vClicked.y, vPoints[2].x, vPoints[2].y, mZProjectedImagePlus.getProcessor());
				}
				if(mPointToCorrect == 3) {
					setupStateVectorFromPoints(vPoints[0].x, vPoints[0].y, vPoints[1].x, vPoints[1].y, vClicked.x, vClicked.y, mZProjectedImagePlus.getProcessor());
				}
				mStateVectorsMemory.setElementAt(CopyStateVector(mStateVectors), mZProjectedImagePlus.getCurrentSlice()-1);
				mZProjectedImagePlus.repaintWindow();
				
				//TODO: save automatically?
			}
		}

	} // CustomStackWindow inner class
}

