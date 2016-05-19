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
    final private static double LengthOfRefineLine = 5;
    final private static int NumOfStepsBetweenFilamentDataPoints = 1;
    
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
                    drawFilamentsOnOverlay(overlay, coordinates, css.color, frame);

                    for (int i = 0 ; i < css.x.length; i++) {
                        Roi r = new ij.gui.EllipseRoi(css.x[i]-0.1, css.y[i]-0.1, css.x[i]+0.1, css.y[i]+0.1, 1);
                        r.setPosition(frame);
                        r.setStrokeColor(Color.BLUE);
                        r.setFillColor(Color.BLUE);
                        overlay.add(r);
                        if (css.color == Color.RED) {
                            r = new ij.gui.EllipseRoi(css.xinit[i]-0.1, css.yinit[i]-0.1, css.xinit[i]+0.1, css.yinit[i]+0.1, 1);
                            r.setPosition(frame);
                            r.setStrokeColor(Color.BLACK);
                            overlay.add(r);}
                    }
                }
                drawPerpendicularLines(e.getValue(), iProcessedImg, frame);
            }
        }
    }
    
    @Override
    protected void postprocessBeforeShow() {
        // Show all segmentation results
        drawFilaments(iFilamentsData);
        showPlot(iFilamentsData);
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
    
    // =================================
    
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

    public List<CSS> perfromSegmentation(ImagePlus aInputImg, boolean aSegmentFirst) {
        int width = aInputImg.getWidth();
        int height = aInputImg.getHeight();
        
        // Input is to be segmented or ready mask?
        ImagePlus ip1 = aSegmentFirst ? runSquassh(aInputImg) : aInputImg.duplicate();

        // Remove holes
        ip1 = ImgUtils.binarizeImage(ip1);
        ip1 = ImgUtils.removeHoles(ip1);

        final Matrix imgMatrix = ImgUtils.imageToMatrix(ip1);

        // Find connected components
        Matrix[] mm = createSeperateMatrixForEachRegion(imgMatrix);

        List<CSS> css = new ArrayList<CSS>();
        // Process each connected component
        for (int i = 0; i < mm.length; i++) {
            Matrix best = Matlab.logical(mm[i], 0);
            
            final ByteProcessor bp = skeletonize(width, height, best);
            // Matrix with skeleton
            final double[][] img2 = new double[height][width];
            ImgUtils.ImgToYX2Darray(bp.convertToFloatProcessor(), img2, 1);
            Matrix skelMatrix = new Matrix(img2);
            logger.info("Longest...");
            // Create graph
            // Do some operations on graph
            // create image from graph
            PathResult result = runLongestShortestPaths(skelMatrix);
            logger.info("Splines...");
            // GENERATE SPLINES
            GraphPath<IntVertex, DefaultEdge> path = result.path;
            if (path == null) continue;
            int len = path.getEdgeList().size();
            logger.debug("LENGHT: " + len);
            int max = len/NumOfStepsBetweenFilamentDataPoints;
            double step = (double) len / (max);
            if (step < 1) step = 1;
            List<Integer> pts = new ArrayList<Integer>();
            pts.add(path.getStartVertex().getLabel());
            List<DefaultEdge> edgeList = path.getEdgeList();
            logger.debug("STEP: " + step);
            for (double currentIdx = 0; currentIdx < edgeList.size(); currentIdx += step) {
                if (currentIdx == 0) continue;
                pts.add(result.graph.getEdgeTarget(edgeList.get((int) currentIdx)).getLabel());

            }
            pts.add(path.getEndVertex().getLabel());
            logger.debug("NUMOF: " + pts.size());
            final List<Double> xv = new ArrayList<Double>();
            final List<Double> yv = new ArrayList<Double>();
            final List<Double> tv = new ArrayList<Double>();

            int t = 0;
            for (int pt : pts) {
                final int x = pt / height;
                final int y = pt % height;
                xv.add(x + 0.5);
                yv.add(y + 0.5);
                tv.add(100 * ((double) t) / (pts.size() - 1));
                t++;
            }
            final double[] xz = ConvertArray.toDouble(xv);
            final double[] yz = ConvertArray.toDouble(yv);
            final double[] tz = ConvertArray.toDouble(tv);
            mosaic.utils.Debug.print(xz, yz, tz);
            logger.debug("INDEX: " + i);

            CSS cssResult = calcSplines(xz, yz, tz, MaximumSplineError * 2);
            css.add(cssResult);

            // REFINE
            refine3(cssResult, aInputImg, ip1);

            CSS cssOut = new CSS();
            cssOut.cssX = new CubicSmoothingSpline(tz, xz, FittingStrategy.MaxSinglePointValue, MaximumSplineError * 2, MaximumSplineError);
            cssOut.cssY = new CubicSmoothingSpline(tz, yz, FittingStrategy.MaxSinglePointValue, MaximumSplineError * 2, MaximumSplineError);
            System.out.println("sp: " + cssOut.cssY.getSmoothingParameter() + " " + cssOut.cssX.getSmoothingParameter());
            cssOut.x = xz;
            cssOut.y = yz;
            cssOut.t = tz;
            cssOut.tMin = tz[0];
            cssOut.tMax = tz[tz.length - 1];
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
            ip1.getProcessor().setInterpolate(true);
            ImageProcessor.setUseBicubic(true);
            for (double tn = 0; tn > -100; tn -= 0.001) {
                double xvv = xs.getValue(tn);
                double yvv = ys.getValue(tn);
                double pixelValue = aInputImg.getProcessor().getInterpolatedValue(xvv-0.5, yvv-0.5);
                if (pixelValue >= boundaryValue && ip1.getProcessor().getInterpolatedValue(xvv - 0.5, yvv -0.5) >=128) cssOut.tMin = tn;
                else {break;}
            }
            double tMaximum = xs.getKnot(xs.getNumberOfKNots() - 1);
            for (double tn = tMaximum; tn < tMaximum + 100 ; tn += 0.001) {
                double xvv = xs.getValue(tn);
                double yvv = ys.getValue(tn);
                double pixelValue = aInputImg.getProcessor().getInterpolatedValue(xvv-0.5, yvv-0.5);
                if (pixelValue >= boundaryValue && ip1.getProcessor().getInterpolatedValue(xvv-0.5, yvv -0.5) >= 128) cssOut.tMax = tn;
                else{break;}
            }
        }

        return css;
    }

    private void refine3(CSS c, ImagePlus xyz, ImagePlus binary) {
        c.xinit=c.x.clone();
        c.yinit=c.y.clone();
        for (int num = 0; num < c.t.length; num += 1) {
            double t = c.t[num];
            double x = c.x[num];//c.cssX.getValue(t);
            double y = c.y[num];//c.cssY.getValue(t);
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

            c.x[num] = (sumx/wx);
            c.y[num] = (sumy/wy);
        }
    }

    private CSS calcSplines( final double[] xz, final double[] yz, final double[] tz, double maxErr) {
        CSS cssOut = new CSS();
        cssOut.cssX = new CubicSmoothingSpline(tz, xz, FittingStrategy.MaxSinglePointValue, maxErr);
        cssOut.cssY = new CubicSmoothingSpline(tz, yz, FittingStrategy.MaxSinglePointValue, maxErr);
        cssOut.x = xz;
        cssOut.y = yz;
        cssOut.t = tz;
        cssOut.tMin = tz[0];
        cssOut.tMax = tz[tz.length - 1];
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
        double[] x;
        double[] y;
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

    private ByteProcessor skeletonize(final int w, final int h, Matrix best) {
        byte[] maskBytes = new byte[w * h];
        for (int x = 0; x < best.getData().length; x++)
            maskBytes[x] = best.getData()[x] != 0 ? (byte) 255 : (byte) 0;
            final ByteProcessor bp = new ByteProcessor(w, h, maskBytes);

            // And skeletonize
            final ImagePlus skeleton = new ImagePlus("Skeletonized", bp);
            Skeletonize3D_ skel = new Skeletonize3D_();
            skel.setup("", skeleton);
            skel.run(skeleton.getProcessor());
            return bp;
    }



    private Matrix[] createSeperateMatrixForEachRegion(final Matrix aImageMatrix) {
        Matrix logical = Matlab.logical(aImageMatrix, 0);
        Map<Integer, List<Integer>> cc = Matlab.bwconncomp(logical, true);
        logger.debug("Number of found connected components " + cc.size());

        // Create sepreate matrix for each connected component
        Matrix[] matrices = new Matrix[cc.size()];
        int idx = 0;
        for (List<Integer> p : cc.values()) {
            Matrix m = aImageMatrix.newSameSize();
            for (int i : p)
                m.set(i, aImageMatrix.get(i));
            matrices[idx] = m;
            idx++;
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
