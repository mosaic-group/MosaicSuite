package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.StackWindow;
import ij.measure.Measurements;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;

import java.awt.Button;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;


/**
 * <h2>BackgroundSubtractor</h2>
 * <h3>An ImageJ Plugin for removing background in fluorescent images</h3>
 * <p> This plugin creates a local histogram around a certain position in an image.
 * <br> The most frequent intensity is supposed to be background and is then
 * <br> subtracted from the image. 
 * <p> The tool processes 8-bit, 16-bit and 32-bit but no color images.
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
 * @version 1.0. September, 2007 (requires: Java 5 or higher)
 * @author Janick Cardinale - Master student at the <a href="http://www.cbl.ethz.ch/">Computational Biophysics Lab<a>, ETH Zurich
 */
public class BackgroundSubtractor2_  implements  PlugInFilter, ActionListener{
	//
	// parameters
	//
	public int mLength = 20; //autodetected in setup() and maybe changed in shodDialog()
	public int mBins = 255;
	public int mStepSize = mLength / 2; //Init in showDialog()
	private float mBinSize = 0f;
	public float mGaussBlurRadius = mStepSize; //Init in showDialog()
	public boolean mShowBackgroundImage = true;
	public boolean mDoAll = false;
	
	//
	// members just for simplicity 
	//
	private static final int BYTE=0, SHORT=1, FLOAT=2;
	private ImageStack mBackgroundImageStack = null;
	private ImagePlus mOriginalImagePlus = null;
	private int mHeight = 0;
	private int mWidth = 0;
	private boolean mAPICall = false;
	
	public int setup(String aArgs, ImagePlus aImagePlus){
		if(IJ.versionLessThan("1.36x"))
			return DONE;
		if (aImagePlus == null) {
			IJ.showMessage("Load an image first.");
			return DONE;
		}
		mOriginalImagePlus = aImagePlus;
		mHeight = mOriginalImagePlus.getHeight();
		mWidth = mOriginalImagePlus.getWidth();
//		mLength = AutoDetectParameters(aImagePlus.getProcessor());
		try {
			mLength = Integer.parseInt(aArgs);
		}catch(NumberFormatException aE) {
			//nothing			
		}
		int vWhatToDo = 2;
		while(vWhatToDo == 2){			
			vWhatToDo = showDialog();
			if(vWhatToDo == 0)
				return DONE;
			if(vWhatToDo == 1) //actually unnecessary
				break;
		}		
		if(mShowBackgroundImage) {
			mBackgroundImageStack = new ImageStack(aImagePlus.getWidth(), aImagePlus.getHeight());
		}
		int vReturnValue = IJ.setupDialog(aImagePlus, DOES_8G + DOES_16 + DOES_32);
		if((vReturnValue & DOES_STACKS) != 0) {
			mDoAll = true;
			vReturnValue -= DOES_STACKS;
		}
		return vReturnValue;
	}

	/**
	 * Subtracts the background for the processor in the argument.
	 */
	public void run(ImageProcessor aImageProcessor){
		int vType;
        if (aImageProcessor instanceof ByteProcessor)
        	vType = BYTE;
        else if (aImageProcessor instanceof ShortProcessor)
        	vType = SHORT;
        else if (aImageProcessor instanceof FloatProcessor)
        	vType = FLOAT;
        else {
        	IJ.showMessage("Wrong image type");
        	return;
        }
        
        if(!mAPICall && mLength >= .5 * Math.min(mWidth, mHeight)) {
        	if(!IJ.showMessageWithCancel("Unsure parameter", "Your square length parameter seems to be too big. \n " + 
        			"The background image will be dominated by the edge of your image." + 
        			"\nClick cancel to abort.")) {
        		return;
        	}
        }
        int vStartSlice = 0;
        int vStopSlice = 0;
        if(!mAPICall) {
        	vStartSlice = 1;
        	vStopSlice = mOriginalImagePlus.getStackSize();
        	if(!mDoAll) {
        		vStartSlice = mOriginalImagePlus.getCurrentSlice();
        		vStopSlice = vStartSlice;        	
        	}
        }else {
        	vStartSlice = 1;
        	vStopSlice = 1;
        }
        
        for(int vS = vStartSlice; vS <= vStopSlice; vS++) {
        	ImageProcessor vImageProcessor = null;
        	ImageProcessor vOrigImageProcessor = null;
        	if(!mAPICall){
        		IJ.showProgress(vS - vStartSlice + 1, vStopSlice - vStartSlice + 1);
        		vOrigImageProcessor = mOriginalImagePlus.getStack().getProcessor(vS);

        		vImageProcessor = vOrigImageProcessor;
        	}else {
        		vOrigImageProcessor = aImageProcessor;
        		vImageProcessor = aImageProcessor;
        	}
//      	vImageProcessor.setCalibrationTable(null);
        	vImageProcessor = vImageProcessor.convertToFloat();

        	int vWidth =  vImageProcessor.getWidth();
        	int vHeight =  vImageProcessor.getHeight();
        	
        	FloatProcessor vBackgroundProcessor = new FloatProcessor(vWidth, vHeight);
        	vImageProcessor.resetMinAndMax();
        	mBinSize = (float)( vImageProcessor.getMax() -  vImageProcessor.getMin()) / mBins;
        	if(mBinSize == 0f)
        		mBinSize = 1f;//only the minmum value will be subtracted->image = 0
        	
        	//
        	// Begin with sampling the expensive BG function
        	//
        	for(int vY = 0; vY < vHeight; vY = vY + mStepSize){
        		for(int vX = 0; vX < vWidth; vX = vX + mStepSize){  
        			vBackgroundProcessor.setf(vX, vY, GetBackgroundIntensityAt(vX, vY, vImageProcessor));
        		}
        	}
        	//
        	// Sample also at the last pixel row and column
        	//
        	for(int vX = 0; vX < vWidth; vX = vX + mStepSize){  
        		vBackgroundProcessor.setf(vX, mHeight - 1, GetBackgroundIntensityAt(vX, mHeight - 1, vImageProcessor));
        	}
        	for(int vY = 0; vY < mHeight; vY = vY + mStepSize){  
        		vBackgroundProcessor.setf(mWidth - 1, vY, GetBackgroundIntensityAt(mWidth - 1, vY, vImageProcessor));
        	}
        	vBackgroundProcessor.setf(mWidth - 1, mHeight - 1, GetBackgroundIntensityAt(mWidth - 1, mHeight - 1, vImageProcessor));
        	//
        	// shade(linear interpolate) the rows
        	//
        	for(int vY = 0; vY < vHeight + mStepSize - 1; vY = vY + mStepSize){
        		if(vY >= vHeight)
        			vY = vHeight - 1; //reset to the last row if necessary
        		for(int vX = 0; vX < vWidth; vX++){ 
        			int vA = vX % mStepSize; 
        			if(vA == 0)
        				continue;
        			int vRightPixelIndex = (vX - vA + mStepSize < vWidth) ? vX - vA + mStepSize : vWidth - 1;
        			float vShadedIntensity = (((float)(mStepSize - vA) / (float)mStepSize) * vBackgroundProcessor.getPixelValue(vX - vA, vY) +
        					((float)vA / (float)(mStepSize)) * vBackgroundProcessor.getPixelValue(vRightPixelIndex, vY));
        			vBackgroundProcessor.setf(vX, vY, vShadedIntensity);
        		}
        	}
        	//
        	// shade the columns
        	//
        	for(int vY = 0; vY < vHeight; vY++){
        		int vB = vY % mStepSize; 
        		if(vB == 0)
    				continue;
        		int vBottomPixelIndex = (vY - vB + mStepSize < vHeight) ? vY - vB + mStepSize : vHeight - 1;
        		for(int vX = 0; vX < vWidth + mStepSize - 1; vX = vX + mStepSize){ 
        			if(vX >= mWidth) 
        				vX = mWidth - 1;
        			float vShadedIntensity = (((float)(mStepSize - vB) / (float)mStepSize) * vBackgroundProcessor.getPixelValue(vX, vY - vB) +
        					((float)vB / (float)(mStepSize)) * vBackgroundProcessor.getPixelValue(vX, vBottomPixelIndex));
        			vBackgroundProcessor.setf(vX, vY, vShadedIntensity);
        		}
        	}
        	//
        	// shade the rest of the pixels
        	//
        	for(int vY = 0; vY < vHeight; vY++){
        		int vB = vY % mStepSize;
        		if(vB == 0)
    				continue;
        		int vBottomPixelIndex = (vY - vB + mStepSize < vHeight) ? vY - vB + mStepSize : vHeight - 1;
        		for(int vX = 0; vX < vWidth; vX++){ 
        			int vA = vX % mStepSize;
        			if(vA == 0)
        				continue;
        			int vRightPixelIndex = (vX - vA + mStepSize < vWidth) ? vX - vA + mStepSize : vWidth - 1;
        			float vShadedIntensityInX = (((float)(mStepSize - vA) / (float)mStepSize) * vBackgroundProcessor.getPixelValue(vX - vA, vY) +
        					((float)vA / (float)(mStepSize)) * vBackgroundProcessor.getPixelValue(vRightPixelIndex, vY));
        			float vShadedIntensityInY = (((float)(mStepSize - vB) / (float)mStepSize) * vBackgroundProcessor.getPixelValue(vX, vY  - vB) +
        					((float)vB / (float)(mStepSize)) * vBackgroundProcessor.getPixelValue(vX, vBottomPixelIndex));
        			vBackgroundProcessor.setf(vX, vY, (vShadedIntensityInX + vShadedIntensityInY) / 2f);
        		}
        	}
        	
        	
//        	//should we do the blurring after shading???
        	float[] vKernel = GenerateGaussKernel(mGaussBlurRadius);
        	vBackgroundProcessor.convolve(vKernel, vKernel.length, 1);
        	vBackgroundProcessor.convolve(vKernel, 1, vKernel.length);

        	if(mShowBackgroundImage) {
        		mBackgroundImageStack.addSlice("", vBackgroundProcessor);        		
        	}
        	
        	float[] vPixels = (float[])vImageProcessor.getPixels();
        	float[] vBackgroundPixels = (float[])vBackgroundProcessor.getPixels();
        	for(int vI = 0; vI < vPixels.length; vI++){
        		vPixels[vI] -= vBackgroundPixels[vI];
        		if(vPixels[vI] < 0){ //what to do?
        			vPixels[vI] = 0;
        		}
        	}

        	switch (vType) {
        	case BYTE:
        		vImageProcessor = vImageProcessor.convertToByte(false);
        		vOrigImageProcessor.insert(vImageProcessor,0,0);
        		break;
        	case SHORT:
        		vImageProcessor = vImageProcessor.convertToShort(false);
        		vOrigImageProcessor.insert(vImageProcessor,0, 0);
        		break;
        	case FLOAT:
        		vOrigImageProcessor.insert(vImageProcessor, 0, 0);
        		break;
        	}
        }
        if(mShowBackgroundImage) {
        	new StackWindow(new ImagePlus("BG of " + mOriginalImagePlus.getTitle(), mBackgroundImageStack));
        }
	}
	
	/**
	 * Same as <code>SubtractBackground(ImagePlus aImagePlus, int aRadius)</code> but only for a ImageProcessor.
	 * @param aImageProcessor The image to process.
	 * @param aSideLength 
	 */
	public void SubtractBackground(ImageProcessor aImageProcessor, int aSideLength) {
		mAPICall = true;
		mHeight = aImageProcessor.getHeight();
		mWidth = aImageProcessor.getWidth();
		mDoAll = false;
		mShowBackgroundImage = false;
//		mBackgroundImageStack = new ImageStack(mWidth, mHeight);//temp
		mLength = aSideLength;
		mStepSize = mLength/2;
		mGaussBlurRadius = mStepSize;
		run(aImageProcessor);
	}

	/**
	 * Creates a histogram of the sliding window around the pixel at (aX, aY) and returns 
	 * the most occuring intensity in the sliding window.
	 * @param aX
	 * @param aY
	 * @param aImageProcessor
	 * @return
	 */
	public float GetBackgroundIntensityAt(int aX, int aY, ImageProcessor aImageProcessor) {
		int[] vHistogramm = new int[mBins+1];
		int vPointerOnMax = 0;
		float vHistoStartIntensity = (float)aImageProcessor.getMin();
//		if(vHistoStartIntensity < 0) {
//			//TODO: The filter will stop by a IndexOutOfBoundsException. Important is that it'll stop.
//			IJ.showMessage("Error in BackgroundSubtractor", "The image contains below 0 values.");
//		}
		for(int vSliderIndY = aY - mLength; vSliderIndY <= aY + mLength; vSliderIndY++) {
			for(int vSliderIndX = aX - mLength; vSliderIndX <= aX + mLength; vSliderIndX++) {
//				TODO: the following edge-handling stuff could be in a separate loop to increase performance a little bit.
				int vPX = vSliderIndX;
				int vPY = vSliderIndY;
				if(vSliderIndX < 0) {
//					vPX *= -1; //this would mirror at the edge of the image 
					vPX = 0; //this replicates the last/first row/column at the edge of the image
				}
				if(vSliderIndY < 0) {
//					vPY *= -1;
					vPY = 0;
				}
				if(vSliderIndX >= mWidth) {
//					vPX = 2*mWidth - 2 - vSliderIndX;
					vPX = mWidth - 1;
				}
				if(vSliderIndY >= mHeight) {
//					vPY = 2*mHeight - 2 - vSliderIndY;
					vPY = mHeight - 1;
				}
				
				float vV = aImageProcessor.getPixelValue(vPX, vPY);
				if(vPointerOnMax == 334)
					System.out.println("Stop");
				if(++vHistogramm[(int)((vV-vHistoStartIntensity)/mBinSize)] > vHistogramm[vPointerOnMax])
					vPointerOnMax = (int)((vV-vHistoStartIntensity)/mBinSize);
			}
		}
		return vPointerOnMax * mBinSize + vHistoStartIntensity;
	}
	
	/**
	 * Generates a one dimensional kernel. Note that the kernel is not normalized.
	 * @param aRadius
	 * @return
	 */
	public float[] GenerateGaussKernel(float aRadius){
		float[] vKernel = new float[3 * (int)aRadius * 2 + 1];
		int vM = vKernel.length / 2;
		for(int vI = 0; vI < vM; vI++){
			vKernel[vI] = (float)(1f/(2f*Math.PI * aRadius * aRadius) * Math.exp(-(float)((vM-vI)*(vM-vI))/(2f * aRadius * aRadius)));
			vKernel[vKernel.length - vI - 1] = vKernel[vI];
		}
		vKernel[vM] = (float)(1f/(2f*Math.PI * aRadius * aRadius));
		return vKernel;
	}
	
//	private int AddColumnToHistogramm(int[] aHistogramm, float aHistogrammStartIntensity, float[] aColumn, int aPointer){
//		for(float vV : aColumn){
//			if(++aHistogramm[(int)((vV-aHistogrammStartIntensity) / mBinSize)] > aHistogramm[aPointer])
//				aPointer = (int)((vV-aHistogrammStartIntensity) / mBinSize);
//		}
//		return aPointer;
//	}
//
//	private int RemoveColumnFromHistogramm(int[] aHistogramm, float aHistogrammStartIntensity, float[] aColumn, int aPointer){
//		for(float vV : aColumn){
//			aHistogramm[(int)((vV - aHistogrammStartIntensity) / mBinSize)]--;
//		}
//		aPointer = 0;
//		for(int vI = 0; vI < aHistogramm.length; vI++){
//			if (aHistogramm[vI] > aHistogramm[aPointer]){
//				aPointer = vI;
//			}
//		}
//		return aPointer;
//	}

	private Button mAutoParamButton;
	private boolean mProposeButtonClicked = false;
	GenericDialog mParameterDialog = null;
//	TextField mLengthTextField = null;
	/**
	 * @return 0 if Dialog was cancelled, 1 if the propose button was clicked, 2 if ok button was clicked.
	 */
	public int showDialog() {
		mParameterDialog = new GenericDialog("Background subtractor...");
		mParameterDialog.addNumericField("Length of sliding window (pixel)", mLength, 0);
//		mLengthTextField = new TextField("" + mLength);
		mAutoParamButton = new Button("Propose length parameter");
		mAutoParamButton.addActionListener(this);
//		mParameterDialog.addNumericField("Square Length", mLength, 0);
		
		Panel vLengthParamPanel = new Panel();
		vLengthParamPanel.setLayout(new GridBagLayout());
//		vLengthParamPanel.add(new Label("Square Length"));
//		vLengthParamPanel.add(mLengthTextField);
		vLengthParamPanel.add(mAutoParamButton);
		
		mParameterDialog.addPanel(vLengthParamPanel);
		mParameterDialog.addCheckbox("Show Background picture", false);
		mParameterDialog.showDialog();
		
		if (mParameterDialog.wasCanceled()) {			
			return 0;
		}
		if (mProposeButtonClicked) {
			mProposeButtonClicked = false;
			return 2;
		}
		
		mLength = (int)mParameterDialog.getNextNumber();
//		mLength = Integer.parseInt(mLengthTextField.getText());
		mShowBackgroundImage = mParameterDialog.getNextBoolean();
		
		mStepSize = mLength / 2;
		mGaussBlurRadius = mStepSize;
		
		return 1;
	}	
	
	public synchronized void actionPerformed(ActionEvent e) {
		Object vSource = e.getSource();
		if (vSource == mAutoParamButton) {
			mLength = AutoDetectParameters(mOriginalImagePlus.getProcessor());
//			mLengthTextField.setText("" + mLength);
//			mLengthTextField.repaint();
//			mParameterDialog.repaint();
			mParameterDialog.dispose();
			mProposeButtonClicked = true;
		}		
	}
	
	/**
	 * The method tries to detect the length paramter using the image processor in the argument.
	 * First, a automatic threshold should select the objects, afterwards, the length of a line fitting
	 * in the biggest area selected by the threshold is returned.
	 * @param aImageProcessor
	 * @return length of the paramter.
	 */
	private int AutoDetectParameters(ImageProcessor aImageProcessor) {
		
		ImageProcessor vInitProcessor = aImageProcessor.convertToFloat();
		float vThreshold  = (float)aImageProcessor.getMinThreshold();
		float[] vPixels = (float[])vInitProcessor.getPixels();			
		
		if (aImageProcessor.getMinThreshold() == ImageProcessor.NO_THRESHOLD){
			//TODO: catch the stackoverlow
			ImageStatistics vStats = ImageStatistics.getStatistics(vInitProcessor, Measurements.MIN_MAX /*+ Measurements.AREA + Measurements.MODE*/, null);
			vThreshold = vInitProcessor.getAutoThreshold(vStats.histogram);
			vThreshold = (float)(vInitProcessor.getMin()+ (vThreshold/255.0)*(vInitProcessor.getMax()-vInitProcessor.getMin()));//scale up
		}
		
		//convert the threshold if a calibration table is used
		if(aImageProcessor.getCalibrationTable() != null) {
			vThreshold = aImageProcessor.getCalibrationTable()[(int)vThreshold];
		}
		
//		TODO: why does resetMinAndMax reset the  minThreshold to NO_THRESHOLD ??
		vInitProcessor.resetMinAndMax();
		int vMaxPointer = 0;
		boolean[] vBitmap = null;
		
		vBitmap = new boolean[vPixels.length];
		for(int vP = 0; vP < vPixels.length; vP++){
			if(vPixels[vP] > vThreshold) {
				vBitmap[vP] = true;
			}
			else {
				vBitmap[vP] = false;
			}
		}
		Vector<Vector<Integer>> vAreas = null;
		try {
			 vAreas = SearchAreasInBitmap(vBitmap);
		}catch (StackOverflowError aSOE) { //TODO: not that nice solution.
			IJ.showMessage("The parameter detection failed.\n" + 
					"Try again with the minimum threshold set.\n" + 
					"(Menu Image\\Adjust\\Threshold)");
			return mLength;
		}
		for(int vI = 1; vI < vAreas.size(); vI++){
			if(vAreas.elementAt(vI).size() > vAreas.elementAt(vMaxPointer).size()) {
				vMaxPointer = vI;
			}
		}
//		for the largest area, get the boundary
		Vector<Integer> vBoundary = SearchBoundary(vBitmap, vAreas.elementAt(vMaxPointer));
		float vMaxDist = 0;
		for(int vI = 0; vI < vBoundary.size(); vI++){
			for(int vJ = vI + 1; vJ < vBoundary.size(); vJ++){
				int vX1 = vBoundary.elementAt(vI) % mWidth;
				int vY1 = vBoundary.elementAt(vI) / mWidth;
				int vX2 = vBoundary.elementAt(vJ) % mWidth;
				int vY2 = vBoundary.elementAt(vJ) / mWidth;
				float vD = (vX1 - vX2) * (vX1 - vX2) + (vY1 -vY2) * (vY1 - vY2);
				if(vD > vMaxDist) {
					vMaxDist = vD;
				}
			}
		}
		vMaxDist = (float)Math.sqrt(vMaxDist);
		return (int)(vMaxDist * 2 + 1) + 1;
		
	}
	/**
	 * Recursively adds adjoining pixel to areas in the returned vector.
	 * @param aBitmap
	 * @return A vector with adjoining areas. A area is a vector containing the index of the pixels.
	 */
	private Vector<Vector<Integer>> SearchAreasInBitmap(boolean[] aBitmap) {
		Vector<Vector<Integer>> vAreas = new Vector<Vector<Integer>>();
		boolean[] vAlreadyVisited = new boolean[aBitmap.length];
		for(int vP = 0; vP < aBitmap.length; vP++){
			IJ.showProgress(vP,aBitmap.length);
			if(aBitmap[vP] && !vAlreadyVisited[vP]){
				vAreas.add(SearchArea(aBitmap, vAlreadyVisited, vP));
			}
		}
		return vAreas;
	}
	/**
	 * Searches for area.
	 * @param aBitmap
	 * @param aAlreadyVisitedMask
	 * @param aPixel
	 * @see SearchAreasInBitmap
	 * @return a Area.
	 */
	private Vector<Integer> SearchArea(boolean[] aBitmap, boolean[] aAlreadyVisitedMask, int aPixel) {
		if(!aBitmap[aPixel] || aAlreadyVisitedMask[aPixel])
			return new Vector<Integer>();
		int vWidth = mOriginalImagePlus.getWidth();
		int vHeight = mOriginalImagePlus.getHeight();
		Vector<Integer> vArea = new Vector<Integer>();
		vArea.add(aPixel);
		aAlreadyVisitedMask[aPixel] = true;
		if(aPixel % vWidth != vWidth - 1 && aBitmap[aPixel + 1] && !aAlreadyVisitedMask[aPixel + 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + 1));
		}
		if(aPixel % vWidth != vWidth -1 && aPixel > vWidth && aBitmap[aPixel - vWidth + 1] && ! aAlreadyVisitedMask[aPixel - vWidth + 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - vWidth + 1));
		}
		if(aPixel > vWidth && aBitmap[aPixel - vWidth] && !aAlreadyVisitedMask[aPixel - vWidth]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - vWidth));
		}
		if(aPixel % vWidth != 0 && aPixel > vWidth && aBitmap[aPixel - vWidth - 1] && ! aAlreadyVisitedMask[aPixel - vWidth - 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - vWidth - 1));
		}
		if(aPixel % vWidth != 0 && aBitmap[aPixel - 1] && !aAlreadyVisitedMask[aPixel - 1]) {
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel - 1));
		}
		if(aPixel % vWidth != 0 && aPixel < (vHeight-1)*vWidth && aBitmap[aPixel + vWidth - 1] && !aAlreadyVisitedMask[aPixel + vWidth - 1]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + vWidth - 1));
		}
		if(aPixel < (vHeight-1)*vWidth && aBitmap[aPixel + vWidth] && !aAlreadyVisitedMask[aPixel + vWidth]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + vWidth));
		}
		if(aPixel % vWidth != vWidth -1 && aPixel < (vHeight-1)*vWidth && aBitmap[aPixel + vWidth + 1] && !aAlreadyVisitedMask[aPixel + vWidth + 1]){
			vArea.addAll(SearchArea(aBitmap, aAlreadyVisitedMask, aPixel + vWidth + 1));
		}
		return vArea;
	}
	/**
	 * Searches the boundary in an area.
	 * @see SearchAreasInBitmap
	 */
	private Vector<Integer> SearchBoundary(boolean[] aBitmap, Vector<Integer> aArea) {
		Vector<Integer> vB = new Vector<Integer>();
		int vWidth = mOriginalImagePlus.getWidth();
		int vHeight = mOriginalImagePlus.getHeight();
		for(int vP : aArea){
			//
			// Get the neighbours, first handle the boundaries of the image
			//
			//corners
			if(vP == 0 || vP == vWidth-1 || vP == (vHeight-1)*vWidth || vP == (vHeight-1)*vWidth + vWidth - 1) {
				vB.add(vP);
				continue;
			}
			//right
			if(vP % vWidth == vWidth - 1) {
				if(!aBitmap[vP-vWidth] || !aBitmap[vP+vWidth]){
					vB.add(vP);
				}
				continue;
			}
			//top
			if(vP < vWidth) {
				if(!aBitmap[vP-1] || !aBitmap[vP+1]){
					vB.add(vP);
				}
				continue;
			}
			//left
			if(vP % vWidth == 0) {
				if(!aBitmap[vP-vWidth] || !aBitmap[vP+vWidth]){
					vB.add(vP);
				}
				continue;
			}
			//bottom
			if(vP > (vHeight - 1) * vWidth) {
				if(!aBitmap[vP-1] || !aBitmap[vP+1]){
					vB.add(vP);
				}
				continue;
			}
			//not boundary
			int vNeighbourCode = 0;
			if(!aBitmap[vP + 1]) vNeighbourCode += 1;
			if(!aBitmap[vP + 1 - vWidth]) vNeighbourCode += 2;
			if(!aBitmap[vP - vWidth]) vNeighbourCode += 4;
			if(!aBitmap[vP - 1 - vWidth]) vNeighbourCode += 8;
			if(!aBitmap[vP - 1]) vNeighbourCode += 16;
			if(!aBitmap[vP - 1 + vWidth]) vNeighbourCode += 32;
			if(!aBitmap[vP + vWidth]) vNeighbourCode += 64;
			if(!aBitmap[vP + vWidth + 1]) vNeighbourCode += 128;

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
}