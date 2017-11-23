package mosaic.ia;


import java.util.List;
import java.util.Vector;

import org.scijava.vecmath.Point3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.process.ImageStatistics;
import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.GUIhelper;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicUtils;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;


public class DistanceCalculationsImage extends DistanceCalculations {

    private final ImagePlus X, Y;

    public DistanceCalculationsImage(ImagePlus X, ImagePlus Y, float[][][] mask, double gridSize, double kernelWeightq, double kernelWeightP) {
        this(X, Y, mask, gridSize, kernelWeightq, kernelWeightP, NumberOfDistPoints);
    }

    public DistanceCalculationsImage(ImagePlus X, ImagePlus Y, float[][][] mask, double gridSize, double kernelWeightq, double kernelWeightP, int aNumberOfDistPoints) {
        super(mask, gridSize, kernelWeightq, kernelWeightP, aNumberOfDistPoints);
        this.X = X;
        this.Y = Y;
        
        calcDistances();
    }

    private Point3d[] extractParticles(ImagePlus image) {
        final Calibration calibration = image.getCalibration();
        iZscale = calibration.pixelDepth;
        iXscale = calibration.pixelHeight;
        iYscale = calibration.pixelWidth;
        iYscale /= iXscale;
        iZscale /= iXscale;
        iXscale = 1.0;

        Vector<Particle> particle = detectParticlesinStack(image);
        System.out.println("Num of detected Particles: " + particle.size());
        return getFilteredAndScaledCoordinates(getCoordinates(particle));
    }

    private void calcDistances() {
        iParticlesX = extractParticles(X);
        iParticlesY = extractParticles(Y);
        System.out.println("Num of filtered Particles: (x/y): " + iParticlesX.length + " / " + iParticlesY.length);
        stateDensity(0, X.getWidth() - 1, 0, X.getHeight() - 1, 0, X.getNSlices() - 1);
    }
    
    /**
     * Detect the particle in a stack from an image using particle tracker
     * @param aInputImage Image
     * @return Vector of particle detected
     */
    public static Vector<Particle> detectParticlesinStack(ImagePlus aInputImage) {
        switch (aInputImage.getType()) {
            case ImagePlus.GRAY8: {
                return DistanceCalculationsImage.<UnsignedByteType> detectParticlesInStack(aInputImage);
            }
            case ImagePlus.GRAY16: {
                return DistanceCalculationsImage.<UnsignedShortType> detectParticlesInStack(aInputImage);
            }
            case ImagePlus.GRAY32: {
                return DistanceCalculationsImage.<FloatType> detectParticlesInStack(aInputImage);
            }
            default: {
                IJ.error("Incompatible image type convert to 8-bit 16-bit or Float type");
                return null;
            }
        }
    }

    /**
     * Detect the particle in a stack from an image using particle tracker
     * The generic parameter is the type of the image
     *
     * @param aInputImg Image
     * @return Vector of particle detected
     */
    private static <T extends RealType<T> & NativeType<T>> Vector<Particle> detectParticlesInStack(ImagePlus aInputImg) {
        // Show input image
        aInputImg.show();

        // Get parameters from user
        final ImageStatistics imageStat = aInputImg.getStatistics();
        final FeaturePointDetector featurePointDetector = new FeaturePointDetector((float) imageStat.max, (float) imageStat.min);
        final GenericDialog gd = new GenericDialog("Particle Detection...", IJ.getInstance());
        GUIhelper.addUserDefinedParametersDialog(gd, featurePointDetector);
        gd.showDialog();
        GUIhelper.getUserDefinedParameters(gd, featurePointDetector);
        
        // Detects particles in the current displayed frame according to the parameters currently set
        final ImageStack stack = aInputImg.getStack();
        final int first_slice = aInputImg.getCurrentSlice();
        ImageStack frameStack = MosaicUtils.getSubStackInFloat(stack, first_slice, first_slice + aInputImg.getNSlices() - 1, true /*duplicate*/);
        MyFrame frame = new MyFrame(getFrameNumberFromSlice(aInputImg.getCurrentSlice(), aInputImg.getNSlices()) - 1);
        
        Vector<Particle> detectedParticles = featurePointDetector.featurePointDetection(frameStack);
        frame.setParticles(detectedParticles);
        frame.setParticleRadius(featurePointDetector.getRadius());
        
        // Draw dots on the positions of the detected particles on the frame and shows it
        final Img<T> background = ImagePlusAdapter.wrap(aInputImg);
        final Img<ARGBType> detected = frame.createImage(background, aInputImg.getCalibration());
        ImageJFunctions.show(detected);

        return frame.getParticles();
    }

    /**
     * @param sliceIndex: 1..#slices
     * @return a frame index: 1..#frames
     */
    private static int getFrameNumberFromSlice(int sliceIndex, int slices_number) {
        return ((sliceIndex - 1) / slices_number + 1);
    }
    
    /**
     * @param aParticles
     * @return coordinates of the Particles as a array of Point3d
     */
    public static Point3d[] getCoordinates(List<Particle> aParticles) {
        final Point3d[] tempCoords = new Point3d[aParticles.size()];

        int i = 0;
        for (Particle p : aParticles) {
            tempCoords[i++] = new Point3d(p.iX, p.iY, p.iZ);
        }

        return tempCoords;
    }
}
