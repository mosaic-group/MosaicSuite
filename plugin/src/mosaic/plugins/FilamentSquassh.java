package mosaic.plugins;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;

import Skeletonize3D_.Skeletonize3D_;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import mosaic.bregman.segmentation.SegmentationParameters;
import mosaic.bregman.segmentation.SquasshSegmentation;
import mosaic.filamentSegmentation.GaussPsf;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.CubicSmoothingSpline.FittingStrategy;
import mosaic.utils.math.CubicSmoothingSpline.Spline;
import mosaic.utils.math.GraphUtils;
import mosaic.utils.math.GraphUtils.IntVertex;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import mosaic.utils.math.Matrix.MFunc;
import mosaic.utils.math.Polynomial;


/**
 * Implementation of filament segmentation plugin.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentSquassh extends PlugInFloatBase { // NO_UCD
    private static final Logger logger = Logger.getLogger(FilamentSquassh.class);
    
    // Segmentation parameters
    final private static double MaximumSplineError = 0.5;
    final private static double PSF = 2;
    final private static double LengthOfRefineLine = 4.5;
    final private static int NumOfStepsBetweenFilamentDataPoints = 1; // >= 1
    
    final private static boolean Debug = true;
    
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
    private final Map<Integer, Map<Integer, List<CSS>>> iFilamentsData = new TreeMap<Integer, Map<Integer, List<CSS>>>();
    private synchronized void addNewFinding(Integer aFrameNumber, Integer aChannelNumber, List<CSS> aCubicSpline) {
        if (iFilamentsData.get(aFrameNumber) == null) {
            iFilamentsData.put(aFrameNumber, new TreeMap<Integer, List<CSS>>());
        }
        iFilamentsData.get(aFrameNumber).put(aChannelNumber, aCubicSpline);
    }

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
        List<CSS> filaments = perfromSegmentation(new ImagePlus("", aOrigImg), /* segment */ true);

        // Save results and update output image
        addNewFinding(aOrigImg.getSliceNumber(), aChannelNumber, filaments);
    }
    
    @Override
    protected boolean showDialog() {
        // TODO: Create dialog.
        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        setResultDestination(ResultOutput.NONE);
        iProcessedImg = iInputImg.duplicate();
        iProcessedImg.setTitle("filaments_" + iInputImg.getTitle());
        
        return true;
    }
    
    @Override
    protected void postprocessBeforeShow() {
        // Show all segmentation results
        drawFilaments(iFilamentsData);
        showPlot(iFilamentsData);
    }

    private void drawFilaments(final Map<Integer, Map<Integer, List<CSS>>> iFilamentsData) {
        Overlay overlay = new Overlay();
        iProcessedImg.setOverlay(overlay);

        for (int frame : iFilamentsData.keySet()) {
            Map<Integer, List<CSS>> ms  = iFilamentsData.get(frame);
            // for every image take all filaments
            for (Entry<Integer, List<CSS>> e : ms.entrySet()) {

                // and draw them one by one
                for (final CSS css : e.getValue()) {
                    logger.debug("Drawing....");
                    final CSS css1 = css;
                    FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(css1.cssX, css1.cssY, css1.tMin, css1.tMax);
                    if (css.color != Color.RED || css.color == Color.RED && Debug) drawFilamentsOnOverlay(overlay, coordinates, css.color, frame);

                    if (Debug) {
                        for (int i = 0 ; i < css.xt.length; i++) {
                            if (css.color == Color.RED) {
                                Roi r = new ij.gui.EllipseRoi(css.xinit[i]-0.1, css.yinit[i]-0.1, css.xinit[i]+0.1, css.yinit[i]+0.1, 1);
                                r.setPosition(frame);
                                r.setStrokeColor(Color.BLACK);
                                overlay.add(r);
                            }
                            else {
                                Roi r = new ij.gui.EllipseRoi(css.xt[i]-0.1, css.yt[i]-0.1, css.xt[i]+0.1, css.yt[i]+0.1, 1);
                                r.setPosition(frame);
                                r.setStrokeColor(Color.BLUE);
                                r.setFillColor(Color.BLUE);
                                overlay.add(r);
                            }
                        }
                    }
                }
                if (Debug) drawPerpendicularLines(e.getValue(), iProcessedImg, frame);
            }
        }
    }
    
    protected void showPlot(final Map<Integer, Map<Integer, List<CSS>>> aFilamentsData) {
            PlotWindow.noGridLines = false; // draw grid lines
            final Plot plot = new Plot("FIL PLOT", "X", "Y");
            plot.setLimits(0, iInputImg.getWidth() - 1, iInputImg.getHeight() - 1, 0);
            
            // Plot data
            plot.setColor(Color.blue);
            for (final Map<Integer, List<CSS>> ms : aFilamentsData.values()) {
                for (final List<CSS> ps : ms.values()) {
                    int count = 0;
                    for (final CSS css1 : ps) {
                        if (css1.color == Color.RED) continue; 
                        // Mix colors - it is good for spotting changes for single filament
                        switch(count) {
                            case 0: plot.setColor(Color.BLUE);break;
                            case 1: plot.setColor(Color.RED);break;
                            case 2: plot.setColor(Color.GREEN);break;
                            case 3: plot.setColor(Color.BLACK);break;
                            case 4: plot.setColor(Color.CYAN);break;
                            default:plot.setColor(Color.MAGENTA);break;
                        }
                        count = (++count % 5); // Keeps all values in 0-5 range
                        
                        // Put stuff on plot
                        FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(css1.cssX, css1.cssY, css1.tMin, css1.tMax);
                        plot.addPoints(coordinates.x.getData(), coordinates.y.getData(), PlotWindow.LINE);
                    }
                }
            }

            plot.show();
    }
    
    // ================================= Implementation ============
    
    public List<CSS> perfromSegmentation(ImagePlus aInputImg, boolean aSegmentFirst) {
        // Input is to be segmented or ready mask?
        ImagePlus segmentedImg = aSegmentFirst ? runSquassh(aInputImg) : aInputImg.duplicate();

        // Remove holes in regions and put every region into seperate matrix
        ImagePlus binarizedImg = ImgUtils.binarizeImage(segmentedImg);
        ImagePlus regionsImg = ImgUtils.removeHoles(binarizedImg);
        final Matrix imgMatrix = ImgUtils.imageToMatrix(regionsImg);
        Matrix[] regionsMatrices = createSeperateMatrixForEachRegion(imgMatrix);
    
        List<CSS> css = new ArrayList<CSS>();
        int height = aInputImg.getHeight();
        // Process each connected component
        for (int regionIdx = 0; regionIdx < regionsMatrices.length; regionIdx++) {
            logger.debug("Processing region wiht index: " + regionIdx);
            Matrix currentRegion = Matlab.logical(regionsMatrices[regionIdx], 0);
            Matrix skeleton = skeletonize(currentRegion);
            PathResult longestPath = runLongestShortestPaths(skeleton);

            // Generate cubic smoothing splines for longest path
            List<Integer> allPoints = generatePoints(longestPath);
            if (allPoints.size() == 0) continue;
            
            // Generate coordinates for creating parametric splines x(t) and y(t)
            final double[] xt = new double[allPoints.size()];
            final double[] yt = new double[allPoints.size()];
            final double[] t = new double[allPoints.size()];
            generateParametricCoordinates(height, allPoints, xt, yt, t);
    
            // Calculate first "rough" spline which fits longest shortest path in skeleton
            CSS cssResult = calcSplines(xt, yt, t, 2 * MaximumSplineError);
            css.add(cssResult);
    
            refine(cssResult, aInputImg, regionsImg);
    
            CSS cssOut = new CSS();
            cssOut.cssX = new CubicSmoothingSpline(t, xt, FittingStrategy.MaxSinglePointValue, MaximumSplineError * 2, MaximumSplineError);
            cssOut.cssY = new CubicSmoothingSpline(t, yt, FittingStrategy.MaxSinglePointValue, MaximumSplineError * 2, MaximumSplineError);
            System.out.println("sp: " + cssOut.cssY.getSmoothingParameter() + " " + cssOut.cssX.getSmoothingParameter());
            cssOut.xt = xt;
            cssOut.yt = yt;
            cssOut.t = t;
            cssOut.tMin = t[0];
            cssOut.tMax = t[t.length - 1];
            cssOut.color = Color.GREEN;
            css.add(cssOut);
    
            CubicSmoothingSpline xs = cssOut.cssX;
            CubicSmoothingSpline ys = cssOut.cssY;
            aInputImg.getProcessor().setInterpolate(true);
            ImageProcessor.setUseBicubic(true);
            double sum = 0;
            for (int ti = 0; ti < xs.getNumberOfKNots(); ti++) {
                double tvv = xs.getKnot(ti);
                double xvv = xs.getValue(tvv);
                double yvv = ys.getValue(tvv);
                double pixelValue = aInputImg.getProcessor().getInterpolatedValue(xvv - 0.5, yvv - 0.5);
                sum += pixelValue;
            }
            
            double avgIntensity =  sum / xs.getNumberOfKNots();
            double boundaryValue = avgIntensity * valueOfGaussAtStepPoint(PSF);
            regionsImg.getProcessor().setInterpolate(true);
            ImageProcessor.setUseBicubic(true);
            for (double tn = 0; tn > -100; tn -= 0.001) {
                double xvv = xs.getValue(tn);
                double yvv = ys.getValue(tn);
                double pixelValue = aInputImg.getProcessor().getInterpolatedValue(xvv-0.5, yvv-0.5);
                if (pixelValue >= boundaryValue && regionsImg.getProcessor().getInterpolatedValue(xvv - 0.5, yvv -0.5) >=128) cssOut.tMin = tn;
                else {break;}
            }
            double tMaximum = xs.getKnot(xs.getNumberOfKNots() - 1);
            for (double tn = tMaximum; tn < tMaximum + 100 ; tn += 0.001) {
                double xvv = xs.getValue(tn);
                double yvv = ys.getValue(tn);
                double pixelValue = aInputImg.getProcessor().getInterpolatedValue(xvv-0.5, yvv-0.5);
                if (pixelValue >= boundaryValue && regionsImg.getProcessor().getInterpolatedValue(xvv-0.5, yvv -0.5) >= 128) cssOut.tMax = tn;
                else{break;}
            }
        }
    
        return css;
    }

    private void generateParametricCoordinates(int height, List<Integer> allPoints, final double[] xt, final double[] yt, final double[] t) {
        for (int i = 0; i < allPoints.size(); ++i) {
            Integer point = allPoints.get(i);
            // Index of a point is a absolute index of image - find coordinates
            final int x = point / height;
            final int y = point % height;
            
            // Adjust to point initially in a middle of pixel (+ 0.5)
            xt[i] = x + 0.5;
            yt[i] = y + 0.5;
            
            // Generate parametric value in range 0-100. It is much easier to fit spline into such range than for example 0-1. 
            // In range 0-100 reaction on smoothing parameter is more linear in its range (0-1).
            t[i] = (100 * ((double) i) / (allPoints.size() - 1));
        }
    }

    private List<Integer> generatePoints(PathResult longestPath) {
        List<Integer> pts = new ArrayList<Integer>();
        GraphPath<IntVertex, DefaultEdge> path = longestPath.path;
        if (path != null) {
            UndirectedGraph<IntVertex, DefaultEdge> graph = longestPath.graph;
            List<DefaultEdge> edgeList = path.getEdgeList();
            for (int idx = 0; idx < edgeList.size(); idx += NumOfStepsBetweenFilamentDataPoints) {
                pts.add(graph.getEdgeSource(edgeList.get(idx)).getLabel());
            }
            // Make sure that very last point is also there
            pts.add(path.getEndVertex().getLabel());
        }
        return pts;
    }

    static public class PathResult {
        PathResult(UndirectedGraph<IntVertex, DefaultEdge> aGraph, GraphPath<IntVertex, DefaultEdge> aPath) { graph = aGraph; path = aPath; }

        UndirectedGraph<IntVertex, DefaultEdge> graph;
        GraphPath<IntVertex, DefaultEdge> path;
    }

    private PathResult runLongestShortestPaths(Matrix aImageMatrix) {
        Graph<IntVertex, DefaultEdge> g = GraphUtils.matrixToGraph(aImageMatrix, true);
        UndirectedGraph<IntVertex, DefaultEdge> gMst = GraphUtils.minimumSpanningTree(g);
        WeightedGraph<IntVertex, DefaultWeightedEdge> gs = GraphUtils.simplifySimipleUndirectedGraph(gMst);
        GraphPath<IntVertex, DefaultWeightedEdge> path1 = GraphUtils.findLongestShortestPath(gs);
        GraphPath<IntVertex, DefaultEdge> path = null;
        if (path1 != null) {
            DijkstraShortestPath<IntVertex, DefaultEdge> dijkstraShortestPath = new DijkstraShortestPath<IntVertex, DefaultEdge>(gMst, path1.getStartVertex(), path1.getEndVertex());
            path = dijkstraShortestPath.getPath();
        }

        return new PathResult(gMst, path);
    }

    private void refine(CSS c, ImagePlus xyz, ImagePlus binary) {
        c.xinit=c.xt.clone();
        c.yinit=c.yt.clone();
        for (int num = 0; num < c.t.length; num += 1) {
            double t = c.t[num];
            double x = c.xt[num];
            double y = c.yt[num];
            Spline sx = c.cssX.getSplineForValue(t);
            Spline sy = c.cssY.getSplineForValue(t);
            Polynomial dx = sx.equation.getDerivative(1);
            Polynomial dy = sy.equation.getDerivative(1);

            double dxv = dx.getValue(t - sx.shift);
            double dyv = dy.getValue(t - sy.shift);

            double alpha;
            if (dxv == 0) alpha = Math.PI/2;
            else if (dyv == 0) alpha = 0;
            else alpha = Math.atan(dyv/dxv);

            double x1 = (x) - LengthOfRefineLine * Math.cos(alpha - Math.PI/2);
            double x2 = (x) + LengthOfRefineLine * Math.cos(alpha - Math.PI/2);
            double y1 = (y) - LengthOfRefineLine * Math.sin(alpha - Math.PI/2);
            double y2 = (y) + LengthOfRefineLine * Math.sin(alpha - Math.PI/2);

            // TODO: Handle situation when two parts of regions are too close that refinement line crossing both.
            int inter=(int)LengthOfRefineLine * 2 * 20 + 1;
            double dxi = (x2 - x1)/(inter - 1);
            double dyi = (y2 - y1)/(inter - 1);
            double sumx = 0, sumy = 0, wx = 0, wy = 0;
            double px = x1, py = y1;
            xyz.getProcessor().setInterpolate(true);
            ImageProcessor.setUseBicubic(true);
            double shift = 0.5; // shift for bicubic interpolation making it symmetric in respect to middle of pixel.
            
            for (int i = 0; i < inter; ++i) {
                double pixelValue = xyz.getProcessor().getInterpolatedValue((float)px - shift, (float)py - shift);
                if (binary.getProcessor().getPixelValue((int)(px), (int)(py)) == 0) pixelValue = 0;
                wx += pixelValue;
                wy += pixelValue;
                sumx += px * pixelValue;
                sumy += py * pixelValue;
                px += dxi;
                py += dyi;
            }
            if (wx == 0 || wy == 0) continue;

            c.xt[num] = (sumx/wx);
            c.yt[num] = (sumy/wy);
        }
    }

    private CSS calcSplines( final double[] aXvalues, final double[] aYvalues, final double[] aTvalues, double aMaxSinglePointError) {
        CSS cssOut = new CSS();
        cssOut.cssX = new CubicSmoothingSpline(aTvalues, aXvalues, FittingStrategy.MaxSinglePointValue, aMaxSinglePointError);
        cssOut.cssY = new CubicSmoothingSpline(aTvalues, aYvalues, FittingStrategy.MaxSinglePointValue, aMaxSinglePointError);
        cssOut.xt = aXvalues;
        cssOut.yt = aYvalues;
        cssOut.t = aTvalues;
        cssOut.tMin = aTvalues[0];
        cssOut.tMax = aTvalues[aTvalues.length - 1];
        return cssOut;
    }

    private void drawPerpendicularLines(List<CSS> css, ImagePlus xyz, int aFrame) {
        Overlay overlay = xyz.getOverlay();
        for (int idx = 0; idx < css.size(); idx++) {
            CSS c = css.get(idx);
            if (c.color != Color.RED) continue;
            for (int num = 0; num < c.t.length; num += 1) {
                double x = c.xinit[num];
                double y = c.yinit[num];
                double t = c.t[num];
                Spline sx = c.cssX.getSplineForValue(t);
                Spline sy = c.cssY.getSplineForValue(t);
                Polynomial dx = sx.equation.getDerivative(1);
                Polynomial dy = sy.equation.getDerivative(1);

                double dxv = dx.getValue(t - sx.shift);
                double dyv = dy.getValue(t - sy.shift);

                double alpha;
                if (dxv == 0) alpha = Math.PI/2;
                else if (dyv == 0) alpha = 0;
                else alpha = Math.atan(dyv/dxv);
                double x1 = x - LengthOfRefineLine * Math.cos(alpha - Math.PI/2);
                double x2 = x + LengthOfRefineLine * Math.cos(alpha - Math.PI/2);
                double y1 = y - LengthOfRefineLine * Math.sin(alpha - Math.PI/2);
                double y2 = y + LengthOfRefineLine * Math.sin(alpha - Math.PI/2);

                Roi r = new Line(x1, y1, x2, y2);
                r.setPosition(aFrame);
                r.setStrokeColor(Color.WHITE);
                overlay.add(r);
            }
        }
        xyz.draw();
    }

    static class CSS {
        CubicSmoothingSpline cssX;
        CubicSmoothingSpline cssY;
        double[] xt;
        double[] yt;
        double[] xinit;
        double[] yinit;
        double[] t;
        double tMin;
        double tMax;
        Color color = Color.RED;
    }

    static public class FilamentXyCoordinates {
        public Matrix x;
        public Matrix y;

        protected FilamentXyCoordinates(Matrix aXvalues, Matrix aYvalues) {
            x = aXvalues;
            y = aYvalues;
        }
    }

    static public FilamentXyCoordinates generateXyCoordinatesForFilament(final CubicSmoothingSpline cssX, final CubicSmoothingSpline cssY, double start, double stop) {
        // Generate x,y coordinates for current filament
        mosaic.utils.Debug.print("ST/ST", start, stop);
        logger.debug("NUM OF POINTS BSPLINE: "  + cssX.getNumberOfKNots() * 2);
        final Matrix t = Matlab.linspace(start, stop, cssX.getNumberOfKNots() * 2);
        Matrix x = t.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return cssX.getValue(t.get(aRow, aCol));
            }
        });
        Matrix y = t.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return cssY.getValue(t.get(aRow, aCol));
            }
        });

        return new FilamentXyCoordinates(x, y);
    }

    private void drawFilamentsOnOverlay(Overlay aOverlay, FilamentXyCoordinates coordinates, Color color, int aFrame) {
        Roi r = new PolygonRoi(coordinates.x.getArrayYXasFloats()[0], coordinates.y.getArrayYXasFloats()[0], Roi.POLYLINE);
        r.setPosition(aFrame);
        r.setStrokeColor(color);
        r.setStrokeWidth(0.3);
        aOverlay.add(r);
    }

    private Matrix skeletonize(Matrix best) {
        // Create ByteProcessor image
        byte[] maskBytes = new byte[best.size()];
        for (int x = 0; x < best.getData().length; x++) {
            maskBytes[x] = best.getData()[x] != 0 ? (byte) 255 : (byte) 0;
        }
        final ByteProcessor bp = new ByteProcessor(best.numCols(), best.numRows(), maskBytes);

        // And skeletonize
        final ImagePlus skeleton = new ImagePlus("Skeletonized", bp);
        Skeletonize3D_ skel = new Skeletonize3D_();
        skel.setup("", skeleton);
        skel.run(skeleton.getProcessor());
        
        // Return as a matrix
        return  ImgUtils.imageToMatrix(skeleton);
    }

    private Matrix[] createSeperateMatrixForEachRegion(final Matrix aImageMatrix) {
        Matrix logical = Matlab.logical(aImageMatrix, 0);
        Map<Integer, List<Integer>> cc = Matlab.bwconncomp(logical, true);
        logger.debug("Number of found connected components " + cc.size());

        // Create separate matrix for each connected component
        Matrix[] matrices = new Matrix[cc.size()];
        int idx = 0;
        for (List<Integer> p : cc.values()) {
            Matrix m = aImageMatrix.newSameSize();
            for (int i : p) {
                m.set(i, aImageMatrix.get(i));
            }
            matrices[idx++] = m;
        }
        return matrices;
    }

    private ImagePlus runSquassh(ImagePlus aImage) {
        double[][][] img3 = ImgUtils.ImgToZXYarray(aImage);
        SegmentationParameters sp = new SegmentationParameters(2, 1, 0.006125, 0.006125, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.GAUSS, PSF, 1, 0, 5);
        ImageStatistics statistics = aImage.getStatistics();
        SquasshSegmentation ss = new SquasshSegmentation(img3, sp, statistics.histMin, statistics.histMax);
        ss.run();
        ImagePlus ip1 = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(ss.iLabeledRegions), "Output");
        new ImageConverter(ip1).convertToGray8();
        return ip1;
    }

    private double valueOfGaussAtStepPoint(double aSigma) {
        // Make reasonable large number of points in respect to sigma to keep quality
        final int NumOfPsfPoints = (int)aSigma * 20;
        
        // Make sure that we have odd number of points (this is required by later code).
        int len = (NumOfPsfPoints / 2) * 2 + 1;
        
        //  Generate gauss with provided sigma
        double[] gauss = GaussPsf.generateKernel(len, 1, aSigma)[0];
        
        // Generate step function
        double[] step = new double[len  * 2];
        for (int i = len; i < len * 2; i++) step[i] = 1;

        // Convolve exactly at step point (for n = len)
        double value = 0;
        int n = len;
        for (int m = -len/2; m <= len/2; m++) {
            value += step[n - m] * gauss[m + len/2 /* moves -n/2..n/2 to 0..n */];
        }
        
        return value;
    }
}
