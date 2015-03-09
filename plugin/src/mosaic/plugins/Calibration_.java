package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.StackStatistics;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Vector;

import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.detection.PreviewCanvas;
import mosaic.core.detection.PreviewInterface;
import mosaic.core.particleLinking.ParticleLinkerBestOnePerm;
import mosaic.core.particleLinking.linkerOptions;


public class Calibration_ implements PlugIn, PreviewInterface {
	
	ImagePlus impA, impB;
	FeaturePointDetector detector;
	
	PreviewCanvas previewA, previewB;
	MyFrame[] frames = new MyFrame[2];;
	GenericDialog gd;
	
	/* user defined parameters for linking*/
	int linkrange = 1; 			// default
	double displacement = 10.0; 	// default
	int frames_number = 2;
	
	double[][] shiftsWithPosition;
	
	@Override
	public void run(String arg) {
		allocateTwoImages();
		detect();
		// changed by arun to affect janick's refactoring
		
		linkerOptions op = new linkerOptions();
		op.linkrange = linkrange;
		op.displacement = (float) displacement;
		op.force = false;
		op.straight_line = false;
		
		new ParticleLinkerBestOnePerm().linkParticles(frames, frames_number, op);
		//
		calculateShifts();
		regression();
	}

	private void regression() {
		
		// Commented by Arun.Was giving compile error, code not used.
	//	Regression$.MODULE$.regression(shiftsWithPosition);
	}



	private void calculateShifts() {
		Vector<Particle> particlesA, particlesB;
		particlesA = frames[0].getParticles();
		particlesB = frames[1].getParticles();
		shiftsWithPosition = new double[particlesA.size()][6];
		Vector<double[]> shifts = new Vector<double[]>();
		Vector<double[]> shiftPositions = new Vector<double[]>();
		Vector<Particle> shiftPart = new Vector<Particle>();
		
		
		for (Particle parA : particlesA)
		{
			int i = 0;
			double[] tempVec = new double[3];
			if (parA.next[0] >= 0) {
				Particle parB = particlesB.get(parA.next[0]);
				shifts.add(new double[]{parB.x -parA.x,parB.y -parA.y, parB.z -parA.z});
				shiftPositions.add(new double[]{parA.x,parA.y,parA.z});
				
				//TODO clean up: makes data structure scala compatible

				System.arraycopy(shifts.elementAt(i), 0, shiftsWithPosition[i], 0, 3);
				System.arraycopy(shiftPositions.elementAt(i), 0, shiftsWithPosition[i], 3, 3);
				
				shiftPart.add(parB);
				i++;
			}
		}
		previewA.shifts = shifts;
		previewA.shiftPositions = shiftPositions;
		previewA.particlesShiftedToDisplay = shiftPart;
	}


	/**
	 * Returns a * c + b
	 * @param a: y-coordinate
	 * @param b: x-coordinate
	 * @param c: width
	 * @return
	 */
	private int coord (int a, int b, int c) {
		return (((a) * (c)) + (b));
	}
	
	private void detect() {
		// TODO Do we use the same detector for both images or two different detectors?
		// get global minimum and maximum 
		StackStatistics stack_statsA = new StackStatistics(impA);
		StackStatistics stack_statsB = new StackStatistics(impB);
		float global_max = Math.max((float)stack_statsA.max, (float)stack_statsB.max);
		float global_min = Math.min((float)stack_statsA.min, (float)stack_statsB.min);
		detector = new FeaturePointDetector(global_max, global_min);
		
		gd = new GenericDialog("Particle detection...", IJ.getInstance());
		detector.addUserDefinedParametersDialog(gd);
		gd.addPanel(detector.makePreviewPanel(this, impA), GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));	        

		previewA = detector.generatePreviewCanvas(impA);
		previewB = detector.generatePreviewCanvas(impB);
		gd.showDialog();
		detector.getUserDefinedParameters(gd);
		// Detection done with preview. TODO
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
			impA = IJ.openImage();
			impA.show();
			impB = IJ.openImage();
			impB.show();
			return true;
		} else if (windowList.length == 1) {
			// One image open, have to open another one.
			impB = IJ.openImage();
			impB.show();
			impA = WindowManager.getImage(windowList[0]);
			return true;
		} else if (windowList.length > 2) {
			// Select images
			// TODO
			return false;
		} else {
			// Two image open.
			impA = WindowManager.getImage(windowList[0]);
			impB = WindowManager.getImage(windowList[1]);
			return true;
		}
	}
		
	@Override
	public void preview(ActionEvent e) {
		// do preview
		detector.preview(impA, previewA, gd);
		frames[0] = detector.getPreviewFrame();
		detector.preview(impB, previewB, gd);
		frames[1] = detector.getPreviewFrame();
		previewA.repaint();
		previewB.repaint();
		return;
	}

	@Override
	public void saveDetected(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private void showAbout() {
		IJ.showMessage("Calibration...",
				"TODO, shift the blame on the developper." //TODO     
		);
	}

}
