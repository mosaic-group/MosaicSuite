package mosaic.plugins;

import mosaic.detection.FeaturePointDetector;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.StackStatistics;

public class Calibration_ implements PlugIn {
	
	ImagePlus imgA, imgB;
	FeaturePointDetector detector;
	
	@Override
	public void run(String arg) {
		allocateTwoImages();
		detect();
		pair();
		regression();
	}

	
	
	private void regression() {
		// TODO Auto-generated method stub
		
	}



	private void pair() {
		// TODO Auto-generated method stub
		
	}



	private void detect() {
		// TODO Do we use the same detector for both images or two different detectors?
		// get global minimum and maximum 
		StackStatistics stack_statsA = new StackStatistics(imgA);
		StackStatistics stack_statsB = new StackStatistics(imgB);
		float global_max = Math.max((float)stack_statsA.max, (float)stack_statsB.max);
		float global_min = Math.min((float)stack_statsA.min, (float)stack_statsB.min);
		detector = new FeaturePointDetector(global_max, global_min);
		
		GenericDialog gd = new GenericDialog("Particle detection...", IJ.getInstance());
		detector.addUserDefinedParametersDialog(gd);
		gd.showDialog();
		detector.getUserDefinedParameters(gd);
		
		
		
	}



	/**
	 * Initializes imgA and imgB so that 2 images are available.
	 * @return
	 */
	boolean allocateTwoImages() {
		// Get 2 Images ready and store them in imgA and imgB.
		int[] windowList = WindowManager.getIDList();
		
		if (windowList == null) {
			// No images open, have to open 2 images.
			imgA = IJ.openImage();
			imgA.show();
			imgB = IJ.openImage();
			imgB.show();
			return true;
		} else if (windowList.length == 1) {
			// One image open, have to open another one.
			imgB = IJ.openImage();
			imgB.show();
			imgA = WindowManager.getImage(windowList[0]);
			return true;
		} else if (windowList.length > 2) {
			// Select images
			// TODO
			return false;
		} else {
			// Two image open.
			imgA = WindowManager.getImage(windowList[0]);
			imgB = WindowManager.getImage(windowList[1]);
			return true;
		}
	}
}
