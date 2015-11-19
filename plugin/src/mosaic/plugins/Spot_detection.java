package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

import java.util.Vector;

import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.GUIhelper;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicUtils;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;

/**
* Spot detection plugin.
* @author Pietro Incardona
*/
public class Spot_detection implements PlugInFilter // NO_UCD
{
    ImagePlus iOriginalImp;
    FeaturePointDetector iDetector;

    @Override
    public void run(ImageProcessor aArgs)
    {
        int numOfSlices = iOriginalImp.getNSlices();
        int numOfFrames = iOriginalImp.getNFrames();
        MyFrame[] frames = new MyFrame[numOfFrames];

        // Detect points in all frames
        for (int frameIdx = 0; frameIdx < numOfFrames; ++frameIdx) {
            final ImageStack subStack = MosaicUtils.GetSubStackInFloat(iOriginalImp.getStack(), (frameIdx) * numOfSlices + 1, (frameIdx + 1) * numOfSlices);
            final MyFrame frame = new MyFrame(subStack, frameIdx, 1);

            // Detect feature points in current frame
            IJ.showStatus("Detecting Particles in Frame " + (frameIdx + 1) + "/" + numOfFrames);
            iDetector.featurePointDetection(frame);
            frames[frame.frame_number] = frame;
        }

        // Fill container with all found particles and set m0 for each.
        final Vector<Particle> particles = new Vector<Particle>();
        for (int i = 0 ; i < numOfFrames ; ++i) {
            final int currentFrameFromIdx= particles.size();
            particles.addAll(frames[i].getParticles());
            final int currentFrameToIdx = particles.size();

            // Set the frame number for the particles
            for (int j = currentFrameFromIdx; j < currentFrameToIdx ; ++j) {
                particles.get(j).m0 = iDetector.getRadius();
            }
        }

        // Get results directory
        String dir = MosaicUtils.ValidFolderFromImage(iOriginalImp);
        if (dir == null) {
            dir = IJ.getDirectory("Choose output directory for CSV file");
        }

        // Save results (particle data) in CSV file
        final CsvColumnConfig columnConfig = new CsvColumnConfig(Particle.ParticleDetection_map, 
                                                                 Particle.ParticleDetectionCellProcessor);
        final CSV<Particle> csv = new CSV<Particle>(Particle.class);
        csv.Write(dir + iOriginalImp.getTitle() + "det.csv", particles , columnConfig , false);
    }

    @Override
    public int setup(String aArgs, ImagePlus aInputImp)
    {
        // Check and save input for later processing
        if (aInputImp == null) {
            IJ.error("There is no image");
            return DONE;
        }
        iOriginalImp = aInputImp;

        // Get statistics and create detector
        final StackStatistics stack_stats = new StackStatistics(iOriginalImp);
        final float global_max = (float)stack_stats.max;
        final float global_min = (float)stack_stats.min;
        iDetector = new FeaturePointDetector(global_max, global_min);

        // GUI and user input
        final GenericDialog gd = new GenericDialog("Spot detection...");
        GUIhelper.addUserDefinedParametersDialog(gd, iDetector);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return DONE;
        }
        GUIhelper.getUserDefinedParameters(gd, iDetector);

        return DOES_ALL;
    }
}
