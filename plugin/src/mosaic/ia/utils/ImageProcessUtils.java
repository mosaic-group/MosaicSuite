package mosaic.ia.utils;


import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.vecmath.Point3d;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.plugin.Macro_Runner;
import ij.process.ImageStatistics;
import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.GUIhelper;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicUtils;
import mosaic.ia.nn.KDTreeNearestNeighbor;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;


public class ImageProcessUtils {

    /**
     * Detect the particle in a stack from an image using particle tracker
     * @param aInputImage Image
     * @return Vector of particle detected
     */
    public static Vector<Particle> detectParticlesinStack(ImagePlus aInputImage) {
        switch (aInputImage.getType()) {
            case ImagePlus.GRAY8: {
                return ImageProcessUtils.<UnsignedByteType> detectParticlesInStack(aInputImage);
            }
            case ImagePlus.GRAY16: {
                return ImageProcessUtils.<UnsignedShortType> detectParticlesInStack(aInputImage);
            }
            case ImagePlus.GRAY32: {
                return ImageProcessUtils.<FloatType> detectParticlesInStack(aInputImage);
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
        System.out.println("No of N slices: " + aInputImg.getNSlices());
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
        MyFrame frame = new MyFrame(MosaicUtils.GetSubStackCopyInFloat(stack, first_slice, first_slice + aInputImg.getNSlices() - 1), 
                                    getFrameNumberFromSlice(aInputImg.getCurrentSlice(), aInputImg.getNSlices()) - 1, 
                                    1);
        
        Vector<Particle> detectedParticles = featurePointDetector.featurePointDetection(frame.getOriginalImageStack());
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

    public static double[] KDTreeDistCalc(Point3d[] X, Point3d[] Y) {
        System.out.println("Size of X:" + X.length);
        System.out.println("Size of Y:" + Y.length);
        final KDTreeNearestNeighbor kdtnn = new KDTreeNearestNeighbor(Y);
        return kdtnn.getNNDistances(X);
    }

    public static ImagePlus generateMask(ImagePlus aImage) {
        final ImagePlus mask = new Duplicator().run(aImage);
        mask.show();
        new Macro_Runner().run("JAR:src/mosaic/plugins/scripts/GenerateMask_.ijm");
        mask.changes = false;

        return mask;
    }

    public static ImagePlus openImage() {
        return new Opener().openImage("");
    }

    // Wrapper used for reading data for Point3d class, it provides 'interface' methods for CSV reader
    public static class Point3dCsvReadWrapper extends Point3d {
        private static final long serialVersionUID = 1L;
        
        public void setXX(double v) {this.x = v;}
        public double getXX() {return this.x;}
        public void setYY(double v) {this.y = v;}
        public double getYY() {return this.y;}
        public void setZZ(double v) {this.z = v;}
        public double getZZ() {return this.z;}
        
        public static CsvColumnConfig getConfig2D() { return new CsvColumnConfig(new String[]{"XX","YY"}, new CellProcessor[] { new ParseDouble(), new ParseDouble() }); }
        public static CsvColumnConfig getConfig3D() { return new CsvColumnConfig(new String[]{"XX","YY","ZZ"}, new CellProcessor[] { new ParseDouble(), new ParseDouble(), new ParseDouble() }); }
    }
    
    public static Point3d[] openCsvFile(String aTitle) {
        // Let user choose a input file
        final OpenDialog od = new OpenDialog(aTitle);
        if (od.getDirectory() == null || od.getFileName() == null) {
            return null;
        }
        final File file = new File(od.getDirectory() + od.getFileName());
        if (!file.exists()) {
            IJ.showMessage("There is no file [" + file.getName() + "]");
            return null;
        }
        
        // Read it and handle errors
        CSV<Point3dCsvReadWrapper>csv = new CSV<Point3dCsvReadWrapper>(Point3dCsvReadWrapper.class);
        int numOfCols = csv.setCSVPreferenceFromFile(file.getAbsolutePath());
        if (numOfCols <= 1 || numOfCols > 3) {
            IJ.showMessage("CSV file should have 2 or 3 columns of data with comma or semicolon delimieters and no header!");
            return null;
        }
        CsvColumnConfig ccc = numOfCols == 2 
                              ? Point3dCsvReadWrapper.getConfig2D()
                              : Point3dCsvReadWrapper.getConfig3D();
        final Vector<Point3dCsvReadWrapper> outdst = csv.Read(file.getAbsolutePath(), ccc, true);
        if (outdst.isEmpty()) {
            IJ.showMessage("Incorrect CSV file chosen [" + file.getName() + "]\nCSV file should have 2 or 3 columns of data with comma or semicolon delimieters and no header!\n");
            return null;
        }
        
        // Return as a array
        return outdst.toArray(new Point3d[outdst.size()]);
    }
}
