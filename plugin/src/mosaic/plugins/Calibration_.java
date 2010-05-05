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
import javax.vecmath.*;

import mosaic.core.Particle;
import mosaic.detection.FeaturePointDetector;
import mosaic.detection.MyFrame;
import mosaic.detection.PreviewCanvas;
import mosaic.detection.PreviewInterface;

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
	
	Vector<Vector3f> shifts ;
	Vector<Vector3f> shiftPositions;
	
	
	@Override
	public void run(String arg) {
		allocateTwoImages();
		detect();
		linkParticles();
		calculateShifts();
		regression();
	}

	
	
	private void regression() {
		
		
	}



	private void calculateShifts() {
		Vector<Particle> particlesA, particlesB;
		particlesA = frames[0].getParticles();
		particlesB = frames[1].getParticles();
		Vector<Vector3f> shifts = new Vector<Vector3f>();
		Vector<Vector3f> shiftPositions = new Vector<Vector3f>();
		Vector<Particle> shiftPart = new Vector<Particle>();
		
		
		for (Particle parA : particlesA)
		{
			if (parA.next[0] >= 0) {
			Particle parB = particlesB.get(parA.next[0]);
			shifts.add(new Vector3f(parB.x -parA.x,parB.y -parA.y, parB.z -parA.z));
			shiftPositions.add(new Vector3f(parA.x,parA.y,parA.z));
			shiftPart.add(parB);
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
	
	
	/**
	 * Second phase of the algorithm - 
	 * <br>Identifies points corresponding to the 
	 * same physical particle in subsequent frames and links the positions into trajectories
	 * <br>The length of the particles next array will be reset here according to the current linkrange
	 * <br>Adapted from Ingo Oppermann implementation
	 */
	private void linkParticles() {

		int m, i, j, k, nop, nop_next, n;
		int ok, prev, prev_s, x = 0, y = 0, curr_linkrange;
		int[] g;
		double min, z, max_cost;
		double[] cost;
		Vector<Particle> p1, p2;

		// set the length of the particles next array according to the linkrange
		// it is done now since link range can be modified after first run
		for (int fr = 0; fr<frames.length; fr++) {
			for (int pr = 0; pr<frames[fr].getParticles().size(); pr++) {
				frames[fr].getParticles().elementAt(pr).next = new int[linkrange];
			}
		}
		curr_linkrange = this.linkrange;

		/* If the linkrange is too big, set it the right value */
		if(frames_number < (curr_linkrange + 1))
			curr_linkrange = frames_number - 1;

		max_cost = this.displacement * this.displacement;

		for(m = 0; m < frames_number - curr_linkrange; m++) {
			nop = frames[m].getParticles().size();
			for(i = 0; i < nop; i++) {
				frames[m].getParticles().elementAt(i).special = false;
				for(n = 0; n < this.linkrange; n++)
					frames[m].getParticles().elementAt(i).next[n] = -1;
			}

			for(n = 0; n < curr_linkrange; n++) {
				max_cost = (double)(n + 1) * this.displacement * (double)(n + 1) * this.displacement;

				nop_next = frames[m + (n + 1)].getParticles().size();

				/* Set up the cost matrix */
				cost = new double[(nop + 1) * (nop_next + 1)];

				/* Set up the relation matrix */
				g = new int[(nop + 1) * (nop_next + 1)];

				/* Set g to zero */
				for (i = 0; i< g.length; i++) g[i] = 0;

				p1 = frames[m].getParticles();
				p2 = frames[m + (n + 1)].getParticles();
				//    			p1 = frames[m].particles;
				//    			p2 = frames[m + (n + 1)].particles;


				/* Fill in the costs */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						cost[coord(i, j, nop_next + 1)] = 
							(p1.elementAt(i).x - p2.elementAt(j).x)*(p1.elementAt(i).x - p2.elementAt(j).x) + 
							(p1.elementAt(i).y - p2.elementAt(j).y)*(p1.elementAt(i).y - p2.elementAt(j).y) + 
							(p1.elementAt(i).z - p2.elementAt(j).z)*(p1.elementAt(i).z - p2.elementAt(j).z) + 
							(p1.elementAt(i).m0 - p2.elementAt(j).m0)*(p1.elementAt(i).m0 - p2.elementAt(j).m0) + 
							(p1.elementAt(i).m2 - p2.elementAt(j).m2)*(p1.elementAt(i).m2 - p2.elementAt(j).m2);
					}
				}

				for(i = 0; i < nop + 1; i++)
					cost[coord(i, nop_next, nop_next + 1)] = max_cost;
				for(j = 0; j < nop_next + 1; j++)
					cost[coord(nop, j, nop_next + 1)] = max_cost;
				cost[coord(nop, nop_next, nop_next + 1)] = 0.0;

				/* Initialize the relation matrix */
				for(i = 0; i < nop; i++) { // Loop over the x-axis
					min = max_cost;
					prev = 0;
					for(j = 0; j < nop_next; j++) { // Loop over the y-axis
						/* Let's see if we can use this coordinate */
						ok = 1;
						for(k = 0; k < nop + 1; k++) {
							if(g[coord(k, j, nop_next + 1)] == 1) {
								ok = 0;
								break;
							}
						}
						if(ok == 0) // No, we can't. Try the next column
							continue;

						/* This coordinate is OK */
						if(cost[coord(i, j, nop_next + 1)] < min) {
							min = cost[coord(i, j, nop_next + 1)];
							g[coord(i, prev, nop_next + 1)] = 0;
							prev = j;
							g[coord(i, prev, nop_next + 1)] = 1;
						}
					}

					/* Check if we have a dummy particle */
					if(min == max_cost) {
						g[coord(i, prev, nop_next + 1)] = 0;
						g[coord(i, nop_next, nop_next + 1)] = 1;
					}
				}

				/* Look for columns that are zero */
				for(j = 0; j < nop_next; j++) {
					ok = 1;
					for(i = 0; i < nop + 1; i++) {
						if(g[coord(i, j, nop_next + 1)] == 1)
							ok = 0;
					}

					if(ok == 1)
						g[coord(nop, j, nop_next + 1)] = 1;
				}

				/* The relation matrix is initilized */

				/* Now the relation matrix needs to be optimized */
				min = -1.0;
				while(min < 0.0) {
					min = 0.0;
					prev = 0;
					prev_s = 0;
					for(i = 0; i < nop + 1; i++) {
						for(j = 0; j < nop_next + 1; j++) {
							if(i == nop && j == nop_next)
								continue;

							if(g[coord(i, j, nop_next + 1)] == 0 && 
									cost[coord(i, j, nop_next + 1)] <= max_cost) {
								/* Calculate the reduced cost */

								// Look along the x-axis, including
								// the dummy particles
								for(k = 0; k < nop + 1; k++) {
									if(g[coord(k, j, nop_next + 1)] == 1) {
										x = k;
										break;
									}
								}

								// Look along the y-axis, including
								// the dummy particles
								for(k = 0; k < nop_next + 1; k++) {
									if(g[coord(i, k, nop_next + 1)] == 1) {
										y = k;
										break;
									}
								}

								/* z is the reduced cost */
								if(j == nop_next)
									x = nop;
								if(i == nop)
									y = nop_next;

								z = cost[coord(i, j, nop_next + 1)] + 
								cost[coord(x, y, nop_next + 1)] - 
								cost[coord(i, y, nop_next + 1)] - 
								cost[coord(x, j, nop_next + 1)];
								if(z > -1.0e-10)
									z = 0.0;
								if(z < min) {
									min = z;
									prev = coord(i, j, nop_next + 1);
									prev_s = coord(x, y, nop_next + 1);
								}
							}
						}
					}

					if(min < 0.0) {
						g[prev] = 1;
						g[prev_s] = 1;
						g[coord(prev / (nop_next + 1), prev_s % (nop_next + 1), nop_next + 1)] = 0;
						g[coord(prev_s / (nop_next + 1), prev % (nop_next + 1), nop_next + 1)] = 0;
					}
				}

				/* After optimization, the particles needs to be linked */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						if(g[coord(i, j, nop_next + 1)] == 1)
							p1.elementAt(i).next[n] = j;
					}
				}
			}

			if(m == (frames_number - curr_linkrange - 1) && curr_linkrange > 1)
				curr_linkrange--;
		}

		/* At the last frame all trajectories end */
		for(i = 0; i < frames[frames_number - 1].getParticles().size(); i++) {
			frames[frames_number - 1].getParticles().elementAt(i).special = false;
			for(n = 0; n < this.linkrange; n++)
				frames[frames_number - 1].getParticles().elementAt(i).next[n] = -1;
		}
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
