package mosaic.plugins;

import java.awt.Button;
import java.awt.Panel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.ipc.OutputChoose;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.GUI.ParticleTrackerHelp;
import net.imglib2.img.display.imagej.ImageJFunctions;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

/**
 * 
 * Plugin to detect spot
 * 
 * @author Pietro Incardona
 *
 */


public class Spot_detection implements PlugInFilter
{

	String dir;
	FeaturePointDetector detector;
	ImagePlus aImp;
	ImageStack stack;
	int slices_number;
	int frames_number;
	MyFrame frames[];
	
	private void fillSize(Vector<Particle> pt, int radius)
	{
		for (int i = 0 ; i < pt.size() ; i++)
		{
			pt.get(i).m0 = radius;
		}
	}
	
	/**
	 * 
	 * Run the plugin
	 * 
	 * @param Image where to run the spot detection
	 * 
	 */
	
	@Override
	public void run(ImageProcessor arg0) 
	{
		// TODO Auto-generated method stub
		
		frames = new MyFrame[frames_number];
		
		// process all frames
		for (int frame_i = 0, file_index = 0; frame_i < frames_number; frame_i++, file_index++) 
		{
			// sequence of images mode:
			// construct each frame from the corresponding image
			MyFrame current_frame = new MyFrame(MosaicUtils.GetSubStackInFloat(stack, (frame_i) * slices_number + 1, (frame_i + 1) * slices_number), frame_i, 1);
			
			// Detect feature points in this frame
			IJ.showStatus("Detecting Particles in Frame " + (frame_i+1) + "/" + frames_number);				
			detector.featurePointDetection(current_frame);
			frames[current_frame.frame_number] = current_frame;
			IJ.freeMemory();
		} // for
		
		Particle.initCSV();
	
		InterPluginCSV<Particle> P_csv = new InterPluginCSV<Particle>(Particle.class);
		
		Vector<Particle> pt = new Vector<Particle>();
		
		for (int i = 0 ; i < frames.length ; i++)
		{
			int old_i = pt.size();
			
			pt.addAll(frames[i].getParticles());
			
			int new_i = pt.size();
			
			// Set the frame number for the particles
			
			for (int j = old_i ; j < new_i ; j++)
			{
				pt.get(j).setFrame(i);
			}
		}
		
		fillSize(pt,detector.getRadius());
		
		dir = MosaicUtils.ValidFolderFromImage(aImp);
		if (dir == null)
			dir = IJ.getDirectory("Choose output directory");
		
		OutputChoose oc = new OutputChoose();
		
		oc.map = Particle.ParticleDetection_map;
		oc.cel = Particle.ParticleDetectionCellProcessor;
		
		P_csv.Write(dir + aImp.getTitle() + "det.csv", pt , oc , false);
	}

	@Override
	public int setup(String arg0, ImagePlus original_imp) 
	{
		/* get user defined params and set more initial params accordingly 	*/	

		aImp = original_imp;
		GenericDialog gd = new GenericDialog("Spot detection...");
		
		// initialize ImageStack stack

		if (original_imp == null)
		{
			IJ.error("There is no image");
			return DONE;
		}
			
		stack = original_imp.getStack();

		// get global minimum and maximum
		StackStatistics stack_stats = new StackStatistics(original_imp);
		float global_max = (float)stack_stats.max;
		float global_min = (float)stack_stats.min;
		slices_number = original_imp.getNSlices();
		frames_number = original_imp.getNFrames();
		
		detector = new FeaturePointDetector(global_max, global_min);
		
		detector.addUserDefinedParametersDialog(gd);
		
		gd.showDialog();
		
		if (gd.wasCanceled())
		{
			return DONE;
		}
		
		detector.getUserDefinedParameters(gd);
		
		return DOES_ALL;
	}
	
}