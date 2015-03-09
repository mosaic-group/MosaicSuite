package mosaic.plugins;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import ij.io.FileInfo;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Pattern;

public class ManualMTTracker_3D implements  PlugInFilter{
	protected static enum STATE {WAITING, READY_FOR_POINT_1, READY_FOR_POINT_2, READY_FOR_POINT_3};
	STATE mState = STATE.WAITING;
	protected static final String RESULT_FILE_SUFFIX = "_ManualMtTracking_results.txt";
	int mHeight, mWidth, mNFrames, mNSlices, mNPoints;
	ImagePlus mOriginalImagePlus, mZProjectedImagePlus;
	Vector<Vector<float[]>> mPoints = new Vector<Vector<float[]>>();
	TrajectoryStackWindow mMainWindow;
	
	public int setup(String aArgs, ImagePlus aImp) {
		mOriginalImagePlus = aImp;
		mHeight = mOriginalImagePlus.getHeight();
		mWidth = mOriginalImagePlus.getWidth();
		mNFrames = mOriginalImagePlus.getNFrames();
		mNSlices = mOriginalImagePlus.getNSlices();
		mNPoints = 3;
		if(mNFrames <= 1) {
			IJ.run("Properties...");
			mNFrames = mOriginalImagePlus.getNFrames();
			mNSlices = mOriginalImagePlus.getNSlices();
		}
		mPoints.setSize(mNFrames);
		doZProjection();
		//
		// If there is data available already...
		//
		File vFile = getResultFile();
		if(vFile != null && vFile.exists()) {
			readResultFile(getResultFile());
		}
		//
		// The window and the canvas:
		//
		DrawCanvas vDrawCanvas = new DrawCanvas(mZProjectedImagePlus);
		// display the image and canvas in a stackWindow  
		mMainWindow = new TrajectoryStackWindow(mZProjectedImagePlus, vDrawCanvas);
		
//		//
//		// temp
//		//
//		for(int vF = 0; vF < mNFrames; vF++) {
//			if(mPoints.elementAt(vF) == null)
//				continue;
//			for(float[] vP : mPoints.elementAt(vF)) {
//				vP[2] = calculateExpectedZPositionAt((int)(vP[0]+.5f), (int)(vP[1]+.5f), mOriginalImagePlus.getStack(), vF*mNSlices+1, (vF+1)*mNSlices);
//			}
//		}
//		writeResultFile(getResultFile());
//		IJ.showMessage("restoring z finished, data wrote to disk.");
		//
		//	finish
		//
		return DOES_16 + DOES_32 + STACK_REQUIRED + NO_CHANGES + DONE;
	}
	
	public void run(ImageProcessor aIP) 
	{
		
	}
	
	private void doZProjection()
	{
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
	}
	/**
	 * The method assumes that the object is well in focus. From the brightest slice and its 4 neighbours
	 * the expectation is calculated.
	 * @param aX
	 * @param aY
	 * @param aIS
	 * @param aStartSlice
	 * @param aStopSlice
	 * @return Expected value from all slices xs with aStartSlice <= xs <= aStartslices
	 */
	private float calculateExpectedZPositionAt(int aX, int aY, ImageStack aIS, int aStartSlice, int aStopSlice) 
	{
		float vMaxInt = 0;
		int vMaxSlice = 0;
		for(int vZ = aStartSlice; vZ <= aStopSlice; vZ++) {
			float vThisInt;
			if((vThisInt = aIS.getProcessor(vZ).getf(aX, aY)) > vMaxInt) {
				vMaxInt =  vThisInt;
				vMaxSlice = vZ;
			}
			
		}
		float vSumOfIntensities = 0f;
		float vRes = 0f;
		int vStartSlice = Math.max(aStartSlice, vMaxSlice-2);
		int vStopSlice = Math.min(aStopSlice, vMaxSlice+2);
		for(int vZ = vStartSlice; vZ <= vStopSlice; vZ++) {
			vSumOfIntensities += aIS.getProcessor(vZ).getf(aX, aY);
			vRes += (vZ - aStartSlice + 1) * aIS.getProcessor(vZ).getf(aX, aY);
		}
		return vRes / vSumOfIntensities;
	}
	
	public String generateDataString(String aSeparator,boolean aDoHeading) 
	{
		String vS = "";
		if(aDoHeading) {
			vS += generateHeaderString(aSeparator);
		}
		for(int vF = 0; vF < mNFrames; vF++) {
			if(mPoints.elementAt(vF) == null) {
				continue;
			}
			vS += "\n" + (vF + 1) + aSeparator;
			for(float[] vP : mPoints.elementAt(vF)) {
				if(vP == null) {
					continue;
				}
				for(float vV : vP) {
					vS += vV + aSeparator;
				}
			}
		}
		return vS;
	}
	
	public String generateHeaderString(String aSeparator) 
	{
		String vS = "#frame" + aSeparator + "SPB1_x" + aSeparator + "SPB1_y" + aSeparator + "SPB1_z" + 
		aSeparator + "SPB2_x" + aSeparator + "SPB2_y" + aSeparator + "SPB2_z" + 
		aSeparator + "Tip_x" + aSeparator + "Tip_y" + aSeparator + "Tip_z\n";
		return vS;
	}

	protected File getResultFile() 
	{
		FileInfo vFI = mOriginalImagePlus.getOriginalFileInfo();
		if(vFI == null) {
			return null;
		}
		String vResFileName = new String(vFI.fileName);
		int vLastInd = vResFileName.lastIndexOf(".");
		if(vLastInd != -1)
			vResFileName = vResFileName.substring(0, vLastInd);
		vResFileName = vResFileName.concat(RESULT_FILE_SUFFIX);

		File vResFile = new File(vFI.directory, vResFileName);
		return vResFile;
	}
	
	protected boolean writeResultFile(File aFile)
	{
		BufferedWriter vW = null;
		try {
			vW = new BufferedWriter(new FileWriter(aFile));
			vW.write(generateDataString("\t",true));
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
		try {
			while((vLine = vR.readLine()) != null) {
				if(vLine.startsWith("#")) continue; //ignore
				if(vLine.matches("(\\s)*")) continue; //empty line
				Pattern vPattern = Pattern.compile("(,|\\||;|\\s|\\t)");
				String [] vPieces = vPattern.split(vLine);
				//if (vPieces.length < mStateVectors.firstElement().length) continue; //not the right line
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
				float[] vFrameState = new float[vPieces.length-1];

				for (int i = 1; i < vPieces.length; i++) {
					float vValue = 0f;
					try {
						vValue =  Float.parseFloat(vPieces[i]);
						vFrameState[i-1] = vValue;
					}catch(NumberFormatException aNFE) {
						continue;//perhaps there is another matching line
					}
				}
				vFrameStates.add(new float[]{vFrameState[0],vFrameState[1],vFrameState[2]});
				vFrameStates.add(new float[]{vFrameState[3],vFrameState[4],vFrameState[5]});
				vFrameStates.add(new float[]{vFrameState[6],vFrameState[7],vFrameState[8]});
				mPoints.add(vFrame, vFrameStates);

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
	
	@SuppressWarnings("serial")
	private class DrawCanvas extends ImageCanvas {
		public DrawCanvas(ImagePlus aImagePlus){
			super(aImagePlus);
		}
		public void paint(Graphics aG){
			super.paint(aG);
			if(mPoints.elementAt(mZProjectedImagePlus.getCurrentSlice()-1) == null) {
				aG.setColor(Color.red);
				aG.drawString("no data", 5+1*(int)magnification, 12+1*(int)magnification);
				return;
			}
			Vector<float[]> mFramePoints = mPoints.elementAt(mZProjectedImagePlus.getCurrentSlice()-1);
			if(mFramePoints.elementAt(0) == null) {
				return;//should never happen
			}
			if(mFramePoints.size() < 2) {
				aG.setColor(Color.ORANGE);
				drawCross(aG, mFramePoints.elementAt(0));
				return;
			}
			aG.setColor(Color.ORANGE);
			aG.drawLine(
					(int)Math.round(mFramePoints.elementAt(0)[0]*magnification), 
					(int)Math.round(mFramePoints.elementAt(0)[1]*magnification),
					(int)Math.round(mFramePoints.elementAt(1)[0]*magnification),
					(int)Math.round(mFramePoints.elementAt(1)[1]*magnification));
			if(mFramePoints.size() < 3) {
				return;
			}
			aG.setColor(Color.RED);
			drawCross(aG, mFramePoints.elementAt(2));
			
		}
		private void drawCross(Graphics aG, float[] aPoint) {
			aG.drawLine(
					(int)Math.round(aPoint[0]*magnification)-5, 
					(int)Math.round(aPoint[1]*magnification),
					(int)Math.round(aPoint[0]*magnification)+5,
					(int)Math.round(aPoint[1]*magnification));
			aG.drawLine(
					(int)Math.round(aPoint[0]*magnification), 
					(int)Math.round(aPoint[1]*magnification)-5,
					(int)Math.round(aPoint[0]*magnification),
					(int)Math.round(aPoint[1]*magnification)+5);
		}
	}

	@SuppressWarnings("serial")
		private class TrajectoryStackWindow extends StackWindow implements ActionListener, MouseListener{
	//		private static final long serialVersionUID = 1L;
			private Button mDeleteThisFrameButton;
			private Button mShowResultsButton;
			private Button mStartStopButton;
			private Button mWriteToDiskButton;
			public Label mStatusTextArea;
			private Vector<float[]> mPointsOfAFrame;
	
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
//				mPointsOfAFrame = new Vector<float[]>(mNPoints);
				mStatusTextArea = new Label("Press Start/Pause button to start tracking.");
				addPanel();
			}
	
			/**
			 * Adds a Panel with filter options button in it to this window 
			 */
			private void addPanel() 
			{
				Panel vButtonPanel = new Panel(new GridLayout(2,2));
				mDeleteThisFrameButton = new Button("Delete this frame.");
				mShowResultsButton = new Button("Show results.");
				mStartStopButton = new Button("Start/Stop tracking");
				mWriteToDiskButton = new Button("Write data to disk.");
				
				mDeleteThisFrameButton.addActionListener(this);
				mShowResultsButton.addActionListener(this);
				mStartStopButton.addActionListener(this);
				mWriteToDiskButton.addActionListener(this);
	
				vButtonPanel.add(mStartStopButton);
				vButtonPanel.add(mShowResultsButton);
				vButtonPanel.add(mDeleteThisFrameButton);
				vButtonPanel.add(mWriteToDiskButton);
				add(vButtonPanel);
				add(mStatusTextArea);
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
				// write to disk Button
				//
				if (vButton == mWriteToDiskButton) { 
					File vFi = getResultFile();
					if(vFi != null && writeResultFile(getResultFile())) {
						IJ.showMessage("Data is stored on disk.");
					}else {
						IJ.showMessage("Unable to store the data to disk. Perhaps you didn't store the movie first.");
						new TextWindow("Manual Tracking Results", generateHeaderString("\t"),
								generateDataString("\t", false), 400, 400);
					}
					
					mState = STATE.WAITING;
					mStatusTextArea.setText("Tracking paused.");
				}

				//
				// Delete frame Button
				//
				if (vButton == mStartStopButton) { 
					if(mState == STATE.WAITING) {
						IJ.setTool(Toolbar.RECTANGLE);
						mState = STATE.READY_FOR_POINT_1;
						mStatusTextArea.setText("Begin with new SPB");
						mPointsOfAFrame = new Vector<float[]>(mNPoints);
					}else {
						mState = STATE.WAITING;
						mStatusTextArea.setText("Tracking paused.");
					}
				}
				//
				// Delete frame Button
				//
				if (vButton == mDeleteThisFrameButton) { 
					mPoints.setElementAt(null, mZProjectedImagePlus.getCurrentSlice()-1);
					mState = STATE.WAITING;
					mStatusTextArea.setText("Tracking paused.");
					mZProjectedImagePlus.repaintWindow();
				}
				
				//
				// show results Button
				//
				if (vButton == mShowResultsButton) { 
					mState = STATE.WAITING;
					mStatusTextArea.setText("Tracking paused.");
					new TextWindow("Manual Tracking Results", generateHeaderString("\t"),
							generateDataString("\t", false), 400, 400);
				}
	
			}
	
			/** 
			 * Defines the action taken upon an <code>MouseEvent</code> triggered by left-clicking 
			 * the mouse anywhere in this <code>TrajectoryStackWindow</code>
			 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
			 */
			public synchronized void mousePressed(MouseEvent aE) {
			}
			
			public void mouseReleased(MouseEvent aE) {
				if(mState == STATE.WAITING)
					return;
				
				int vCurrentSlice = mZProjectedImagePlus.getCurrentSlice();
				//
				// save the clicked point, if it is the last one, save it in the member
				//
				
				float[] vPoint = new float[3];
				vPoint[0] = aE.getX()/(float)getCanvas().getMagnification();
				vPoint[1] = aE.getY()/(float)getCanvas().getMagnification();
				vPoint[2] = calculateExpectedZPositionAt(
						(int)(vPoint[0]+.5f), (int)(vPoint[1]+.5f), 
						mOriginalImagePlus.getStack(),
						(vCurrentSlice-1)*mNSlices+1, vCurrentSlice*mNSlices); 
				
				mPointsOfAFrame.addElement(vPoint);
				mPoints.setElementAt(mPointsOfAFrame, vCurrentSlice-1);
				//
				// Update the state and the status text
 				//
				if(mState == STATE.READY_FOR_POINT_1) {					
					mStatusTextArea.setText("Click on the old SPB.");
					mState = STATE.READY_FOR_POINT_2;
				}else if(mState == STATE.READY_FOR_POINT_2) {
					mStatusTextArea.setText("Click on the tip.");
					mState = STATE.READY_FOR_POINT_3;
				}else if(mState == STATE.READY_FOR_POINT_3) {
//					mPoints.setElementAt(mPointsOfAFrame, vCurrentSlice-1);
					if(vCurrentSlice == mNFrames) {
						mState = STATE.WAITING;
						mStatusTextArea.setText("Tracking finished.");
						mMainWindow.showSlice(1);
						//
						// finished: try to write a file.
						//
						if(mOriginalImagePlus.getOriginalFileInfo() == null) {
							new TextWindow("Manual Tracking Results", generateHeaderString("\t"),
									generateDataString("\t", false), 400, 400);
						}else {
							if(writeResultFile(getResultFile())) {
								IJ.showMessage("Data is stored on disk.");
							}else {
								new TextWindow("Manual Tracking Results", generateHeaderString("\t"),
										generateDataString("\t", false), 400, 400);
							}
						}
					}else {
						mStatusTextArea.setText("Click on the new SPB.");
						mState = STATE.READY_FOR_POINT_1;
						mMainWindow.showSlice(vCurrentSlice+1);
						mPointsOfAFrame = new Vector<float[]>(mNPoints);
					}
				}
				mZProjectedImagePlus.repaintWindow();
			}
	
			public void mouseClicked(MouseEvent aE) {
			}

			public void mouseEntered(MouseEvent aE) {
			}
	
			public void mouseExited(MouseEvent aE) {
			}
		} // CustomStackWindow inner class
}
