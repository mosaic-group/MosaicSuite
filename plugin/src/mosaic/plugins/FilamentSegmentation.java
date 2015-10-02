package mosaic.plugins;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ij.macro.Interpreter;
import ij.process.FloatProcessor;
import mosaic.filamentSegmentation.SegmentationAlgorithm;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.filamentSegmentation.GUI.ConfigDialog;
import mosaic.filamentSegmentation.GUI.FilamentResultsTable;
import mosaic.filamentSegmentation.GUI.OutputImageWindow;
import mosaic.filamentSegmentation.GUI.PlotDialog;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.utils.math.CubicSmoothingSpline;

/**
 * Implementation of filament segmentation plugin.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentSegmentation extends PlugInFloatBase { // NO_UCD
    // Segmentation parameters
    private NoiseType iNoiseType;
    private PsfType iPsfType;
    private Dimension iPsfDimension;
    private double iSubpixelSampling;
    private int iCoefficientStep;
    private double iRegularizerTerm;
    private int iNumberOfIterations;

    // Layer for visualization filaments
    public static enum VisualizationLayer {
        OVERLAY, IMAGE
    }
    VisualizationLayer iVisualizationLayer;
    
    // Synchronized map used to collect segmentation data from all plugin threads
    //
    // Map <
    //     frameNumber, 
    //     Map <
    //          channelNumber,
    //          listOfCubicSplines
    //         >
    //     >   
    // TreeMap is intentionally used to have always sorted data so output for example to
    // results table is nice and clean.
    private final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> iFilamentsData = new TreeMap<Integer, Map<Integer, List<CubicSmoothingSpline>>>();
    private synchronized void addNewFinding(Integer aFrameNumber, Integer aChannelNumber, List<CubicSmoothingSpline> aCubicSpline) {
        if (iFilamentsData.get(aFrameNumber) == null) {
            iFilamentsData.put(aFrameNumber, new TreeMap<Integer, List<CubicSmoothingSpline>>());
        }
        iFilamentsData.get(aFrameNumber).put(aChannelNumber, aCubicSpline);
    }

    // Output image with marked filaments
    private OutputImageWindow iOutputColorImg;

    /**
     * Segmentation procedure for plugin
     * @param aOutputImg - <not used>
     * @param aOrigImg - input image (to be segmented)
     * @param aChannelNumber - channel number used for drawing output image.
     */
    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
        // Get dimensions of input image
        final int originalWidth = aOrigImg.getWidth();
        final int originalHeight = aOrigImg.getHeight();

        // Convert to array
        final double[][] img = new double[originalHeight][originalWidth];
        ImgUtils.ImgToYX2Darray(aOrigImg, img, 1.0f);

        // --------------- SEGMENTATION --------------------------------------------
        final SegmentationAlgorithm sa = new SegmentationAlgorithm(img,
                                                                   iNoiseType,
                                                                   iPsfType,
                                                                   iPsfDimension,
                                      /* subpixel sampling */      iSubpixelSampling,
                                      /* scale */                  iCoefficientStep,
                                      /* regularizer term */       iRegularizerTerm,
                                                                   iNumberOfIterations);
        final List<CubicSmoothingSpline> filaments = sa.performSegmentation();

        // Save results and update output image
        addNewFinding(aOrigImg.getSliceNumber(), aChannelNumber, filaments);
    }
    
    @Override
    protected void postprocessBeforeShow() {
        // Show all segmentation results
        iOutputColorImg.showFilaments(iFilamentsData, iVisualizationLayer);
        
        PlotDialog pd = new PlotDialog("All filaments from " + iInputImg.getTitle(), iInputImg.getWidth() - 1, iInputImg.getHeight() - 1);
        pd.createPlotWithAllCalculetedSplines(iFilamentsData).show();
        
        if (!Interpreter.isBatchMode()) {
            FilamentResultsTable frt = new FilamentResultsTable("Filaments segmentation results of " + iInputImg.getTitle(), iFilamentsData);
            frt.show();
        }
    }

    @Override
    protected boolean showDialog() {
        ConfigDialog cd = new ConfigDialog();
        if (!cd.getConfiguration()) {
            return false;
        }
        
        // Get segmentation paramters
        iNoiseType = cd.getNoiseType();
        iPsfType = cd.getPsfType();
        iPsfDimension = cd.getPsfDimension();
        iSubpixelSampling = cd.getSubpixelSampling();
        iCoefficientStep = cd.getCoefficientStep();
        iRegularizerTerm = cd.getRegularizerTerm();
        iNumberOfIterations = cd.getNumberOfIterations();
        iVisualizationLayer = cd.getVisualizationLayer();

        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        // Generate new RGB ImagePlus and set it as a output/processed image;
        setResultDestination(ResultOutput.NONE);
        iOutputColorImg = new OutputImageWindow(iInputImg, "segmented_" + iInputImg.getTitle());
        setProcessedImg(iOutputColorImg.getImagePlus());

        return true;
    }
}
