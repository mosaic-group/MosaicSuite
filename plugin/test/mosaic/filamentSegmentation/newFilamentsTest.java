package mosaic.filamentSegmentation;


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.Test;

import Skeletonize3D_.Skeletonize3D_;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;
import mosaic.bregman.segmentation.SegmentationParameters;
import mosaic.bregman.segmentation.SquasshSegmentation;
import mosaic.test.framework.CommonBase;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.GraphUtils;
import mosaic.utils.math.GraphUtils.IntVertex;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import mosaic.utils.math.Matrix.MFunc;


public class newFilamentsTest extends CommonBase {
    private static final Logger logger = Logger.getLogger(newFilamentsTest.class);

    static public class Result {

        Result(Matrix aM, UndirectedGraph<IntVertex, DefaultEdge> aG, GraphPath<IntVertex, DefaultEdge> aP) {
            m = aM;
            graph = aG;
            path = aP;
        }

        Matrix m;
        UndirectedGraph<IntVertex, DefaultEdge> graph;
        GraphPath<IntVertex, DefaultEdge> path;
    }

    private Result runLongestShortestPaths(Matrix aImageMatrix) {
        UndirectedGraph<IntVertex, DefaultEdge> g = GraphUtils.matrixToGraph(aImageMatrix, true);
        UndirectedGraph<IntVertex, DefaultEdge> gMst = GraphUtils.minimumSpanningTree(g);

        GraphPath<IntVertex, DefaultEdge> path = findPathBoosted(gMst);

        Matrix result = aImageMatrix.copy().zeros();
        if (path == null) return new Result(result, null, null);
        for (DefaultEdge e : path.getEdgeList()) {
            result.set(gMst.getEdgeSource(e).getLabel(), 255);
            result.set(gMst.getEdgeTarget(e).getLabel(), 255);
        }

        return new Result(result, gMst, path);
    }

    private void graphShow(Set<IntVertex> aVertex, int w, int h, String aTitle) {
        
        Matrix result = new Matrix(h, w);
        for (IntVertex v : aVertex) {
            result.set(v.getLabel(), 255);
        }
        
        matrixToImage(result, aTitle);
    }
    
    private GraphPath<IntVertex, DefaultEdge> findPath(UndirectedGraph<IntVertex, DefaultEdge> gMst) {
        FloydWarshallShortestPaths<IntVertex, DefaultEdge> paths = new FloydWarshallShortestPaths<>(gMst);
        GraphPath<IntVertex, DefaultEdge> path = findLongestPath(gMst.vertexSet().iterator().next(), paths);
        
        return path;
    }
    
    private GraphPath<IntVertex, DefaultEdge> findPathBoosted(UndirectedGraph<IntVertex, DefaultEdge> gMst) {
        WeightedGraph<IntVertex, DefaultWeightedEdge> gs = GraphUtils.simplifySimipleUndirectedGraph(gMst);
        
        for (DefaultWeightedEdge edge : gs.edgeSet()) {
            logger.debug(edge + " " + gs.getEdgeWeight(edge));
        }
        
        FloydWarshallShortestPaths<IntVertex, DefaultWeightedEdge> paths = new FloydWarshallShortestPaths<>(gs);
        GraphPath<IntVertex, DefaultWeightedEdge> path = findLongestPath(gs.vertexSet().iterator().next(), paths);
        if (path == null) return null;
        DijkstraShortestPath<IntVertex, DefaultEdge> dijkstraShortestPath = new DijkstraShortestPath<>(gMst, path.getStartVertex(), path.getEndVertex());
        
        return dijkstraShortestPath.getPath();
    }

    private <E, T extends DefaultEdge> GraphPath<E, T> findLongestPath(E v, FloydWarshallShortestPaths<E, T> paths) {
        List<GraphPath<E, T>> sp = paths.getShortestPaths(v);
        
        double len = 0;
        E end = null;
        for (GraphPath<E, T> p : sp) {
            double w = getPathWeight(p);
            if (w > len) {
                len = w;
                end = p.getEndVertex();
            }
        }
        List<GraphPath<E, T>> sp2 = paths.getShortestPaths(end);
        len = 0;
        E end2 = null;
        GraphPath<E, T> plongest = null;
        for (GraphPath<E, T> p : sp2) {
            double w = getPathWeight(p);
            if (w > len) {
                len = w;
                end2 = p.getEndVertex();
                plongest = p;
            }
        }
        logger.debug("Longest path from shortest: " + len + " " + end + ":" + end2);
        logger.debug(plongest);
        return plongest;
    }
    
     <E, T extends DefaultEdge> double getPathWeight(GraphPath<E, T> p) {
        double sum = 0;
        
        List<T> edgeList = p.getEdgeList();
        Graph<E, T> graph = p.getGraph();
        
        for (T e : edgeList) sum += graph.getEdgeWeight(e);
        
        return sum;
    }

    int w;
    int h;

    @Test
    public void fil() {
        // Input file
        String input = null;
        input = "/Users/gonciarz/test/short.tif";
        input = "/Users/gonciarz/test/elispe.tif";
        input = "/Users/gonciarz/test/Crop44.tif";
        input = "/Users/gonciarz/test/DF_5.tif";
        input = "/Users/gonciarz/test/xyz.tif";
        input = "/Users/gonciarz/test/test.tif";
        input = "/Users/gonciarz/test/zyx.tif";
        input = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/filamentsTest.tif";
        input = "/Users/gonciarz/test/cross.tif";
        input = "/Users/gonciarz/test/multi.tif";
        input = "/Users/gonciarz/test/spiral.tif";
        input = "/Users/gonciarz/test/maskFila.tif";
        input = "/Users/gonciarz/test/single.tif";
        input = "/Users/gonciarz/test/Crop_12-12.tif";
        input = "/Users/gonciarz/test/many.tif";
        input = "/Users/gonciarz/test/sample.jpg";
        input = "/Users/gonciarz/test/longlong.tif";
        boolean toBeSegmented = true;

        // Load input
        ImagePlus ip4 = loadImagePlus(input);
        ip4.setTitle("INPUT");
        ip4.show();

        w = ip4.getWidth();
        h = ip4.getHeight();

        // Input is to be segmented or ready mask?
        ImagePlus ip1 = toBeSegmented ? runSquassh(ip4) : ip4.duplicate();

        // Remove holes
        ip1 = binarizeImage(ip1);
//         ip1 = removeHoles(ip1);

        // Run distance transfrom
        // ImagePlus dist = runDistanceTransform(ip1.duplicate());
        ImagePlus dist = ip1.duplicate();

        // Convert to matrix
        final Matrix imgMatrix = convertToMatrix(w, h, dist);

        // Find connected components
        Matrix[] mm = createSeperateMatrixForEachRegion(imgMatrix);

        List<CSS> css = new ArrayList<CSS>();
        // Process each connected component
        for (int i = 0; i < mm.length; i++) {
            Matrix m = mm[i];

            Matrix best = thresholdRegion(m, true);
            final ByteProcessor bp = skeletonize(w, h, best);

            // Matrix with skeleton
            final double[][] img2 = new double[h][w];
            ImgUtils.ImgToYX2Darray(bp.convertToFloatProcessor(), img2, 1.0);
            Matrix skelMatrix = new Matrix(img2);
            logger.info("Longest...");
            // Create graph
            // Do some operations on graph
            // create image from graph
            Result result = runLongestShortestPaths(skelMatrix);
            mm[i] = result.m;
            logger.info("Splines...");
            // GENERATE SPLINES
            GraphPath<IntVertex, DefaultEdge> path = result.path;
            if (path == null) continue;
            int len = path.getEdgeList().size();
            logger.debug("LENGHT: " + len);
            int max = len/5;
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
                final int x = pt / h;
                final int y = pt % h;
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
            
            CSS cssResult = calcSplines(xz, yz, tz);
            css.add(cssResult);
        }

        // Merge results and show them
        Matrix out = mergeMatrices(mm);
        FloatProcessor floatProcessor = matrixToImage(out, "Matrix Image");
        ImagePlus xyz = showFilamentsOnInputImage(input, w, h, floatProcessor);
        draw(xyz, css);
        sleep(25000);
    }

    private CSS calcSplines( final double[] xz, final double[] yz, final double[] tz) {
        double maxErr = 1;
        CubicSmoothingSpline cssX = null;
        CubicSmoothingSpline cssY = null;
        if (xz.length == 2) {
            // No fitting needed for just two points. Fit the (almost) straight regression line.
            cssX = new CubicSmoothingSpline(tz, xz, 1e-15);
            cssY = new CubicSmoothingSpline(tz, yz, 1e-15);
        }else {
            int step = 1;
            double direction = 1;
            double p = 0;
            while (step < 50) {
                p += direction * 1.0/Math.pow(2, step);
                logger.debug("Current P: " + p + " step: " + step);
                cssX = new CubicSmoothingSpline(tz, xz, p);
                cssY = new CubicSmoothingSpline(tz, yz, p);

                double max = 0;
                for (int i = 0; i < tz.length; i++) {
                    double xv = cssX.getValue(tz[i]);
                    double yv = cssY.getValue(tz[i]);
                    xv = Math.abs(xv - xz[i]);
                    yv = Math.abs(yv - yz[i]);
                    if (max < xv) max = xv; if (max < yv) max = yv;
                    if (max > maxErr) break;
                }
                if (max > 0.5 * maxErr && max <= maxErr) { logger.info("FOUND!");break;};
                direction = (max <= maxErr) ? -1 : 1;
                step += 1;
            }
        }
        CSS cssOut = new CSS();
        cssOut.cssX = cssX;
        cssOut.cssY = cssY;
        
        return cssOut;
    }
    
    static class CSS {

        CubicSmoothingSpline cssX;
        CubicSmoothingSpline cssY;
    }

    static public class FilamentXyCoordinates {

        public Matrix x;
        public Matrix y;

        protected FilamentXyCoordinates(Matrix aXvalues, Matrix aYvalues) {
            x = aXvalues;
            y = aYvalues;
        }
    }

    static public FilamentXyCoordinates generateXyCoordinatesForFilament(final CubicSmoothingSpline cssX, final CubicSmoothingSpline cssY) {
        // Generate x,y coordinates for current filament

        double start = cssX.getKnot(0) - 0;
        double stop = cssX.getKnot(cssX.getNumberOfKNots() - 1) + 0;
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

    /**
     * Generates (x,y) coordinates from given cubic smoothing spline
     * 
     * @param css - input spline
     * @return
     */
    static public FilamentXyCoordinates generateAdjustedXyCoordinatesForFilament(final CSS css) {
        FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(css.cssX, css.cssY);
        coordinates.x.sub(0);
        coordinates.y.sub(0);

        return coordinates;
    }

    private void draw(ImagePlus outImg, List<CSS> cssd) {
        Overlay overlay = new Overlay();

        // for every image take all filaments
        for (CSS css : cssd) {
            logger.debug("Drawing....");
            FilamentXyCoordinates coordinates = generateAdjustedXyCoordinatesForFilament(css);
            drawFilamentsOnOverlay(overlay, 0, coordinates);
        }

        outImg.setOverlay(overlay);
    }

    private void drawFilamentsOnOverlay(Overlay aOverlay, int aSliceNumber, FilamentXyCoordinates coordinates) {
        Roi r = new PolygonRoi(coordinates.x.getArrayYXasFloats()[0], coordinates.y.getArrayYXasFloats()[0], Roi.POLYLINE);
        r.setPosition(aSliceNumber);
        r.setStrokeColor(Color.RED);
        r.setStrokeWidth(0.25);
        aOverlay.add(r);
    }

    private ByteProcessor skeletonize(final int w, final int h, Matrix best) {
        byte[] maskBytes = new byte[w * h];
        for (int x = 0; x < best.getData().length; x++)
            maskBytes[x] = best.getData()[x] > 0 ? (byte) 255 : (byte) 0;
        final ByteProcessor bp = new ByteProcessor(w, h, maskBytes);

        // And skeletonize
        final ImagePlus skeleton = new ImagePlus("Skeletonized", bp);
        Skeletonize3D_ skel = new Skeletonize3D_();
        skel.setup("", skeleton);
        skel.run(skeleton.getProcessor());
//        ImagePlus skelImg = new ImagePlus("Skeleton", bp);
//        skelImg.show();
        return bp;
    }

    private ImagePlus binarizeImage(ImagePlus aImage) {
        new ImageConverter(aImage).convertToGray8();
        byte[] pixels = (byte[]) aImage.getProcessor().getPixels();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (pixels[i] == 0) ? (byte) 0 : (byte) 255;
        }
        return aImage;
    }

    private Matrix thresholdRegion(Matrix m, boolean aOnlyRegionized) {
        double[] data = m.getData();
        SortedSet<Double> sortedSet = new TreeSet<Double>();
        for (double d : data)
            sortedSet.add(d);
        logger.debug(sortedSet.size() + " " + sortedSet.first() + " " + sortedSet.last());

        // find max threshold
        double previous = sortedSet.first();
        Matrix best = Matlab.logical(m, 0);
        if (!aOnlyRegionized) for (double th : sortedSet) {
            Matrix l = Matlab.logical(m, th);
            Map<Integer, List<Integer>> c = Matlab.bwconncomp(l, true);
            logger.debug(th + " " + c.size() + " " + c.keySet());
            if (c.size() == 1) {
                previous = th;
                best = l;
            }
            else
                break;
        }
        logger.debug("MAX th: " + previous);
        return best;
    }

    private ImagePlus showFilamentsOnInputImage(String input, final int w, final int h, FloatProcessor floatProcessor) {
        ImagePlus xyz = loadImagePlus(input);
        xyz.setTitle("FILAMENTS");
        byte[] pixelsOut = (byte[]) xyz.getProcessor().getPixels();
        float[] pixelsIn = (float[]) floatProcessor.getPixels();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                // pixelsOut[x + y * w] = pixelsIn[x + y * w] > 0 ? (byte) 0 : pixelsOut[x + y * w];
            }
        }
        xyz.show();
        return xyz;
    }

    private FloatProcessor matrixToImage(Matrix aImageMatrix, String aTitle) {
        final double[][] result = aImageMatrix.getArrayYX();
        FloatProcessor floatProcessor = new FloatProcessor(result[0].length, result.length);
        ImagePlus ipout = new ImagePlus(aTitle, floatProcessor);
        ImgUtils.YX2DarrayToImg(result, floatProcessor, 1.0);
        ipout.show();
        return floatProcessor;
    }

    private Matrix mergeMatrices(Matrix[] mm) {
        Matrix out = mm[0];
        for (int i = 1; i < mm.length; i++)
            out.add(mm[i]);
        return out;
    }

    private Matrix[] createSeperateMatrixForEachRegion(final Matrix aImageMatrix) {
        Matrix logical = Matlab.logical(aImageMatrix, 0);
        Map<Integer, List<Integer>> cc = Matlab.bwconncomp(logical, true);
        logger.debug("Connected components " + cc.size());

        // Create sepreate matrix for each connected component
        Matrix[] mm = new Matrix[cc.size()];
        int idx = 0;
        for (List<Integer> p : cc.values()) {
            Matrix m = aImageMatrix.copy().zeros();
            for (int i : p)
                m.set(i, aImageMatrix.get(i));
            mm[idx] = m;
            idx++;
        }
        return mm;
    }

    private Matrix convertToMatrix(final int w, final int h, ImagePlus aIamge) {
        FloatProcessor fp = aIamge.getProcessor().convertToFloatProcessor();
        final double[][] img = new double[h][w];
        ImgUtils.ImgToYX2Darray(fp, img, 1.0);
        final Matrix imgMatrix = new Matrix(img);
        return imgMatrix;
    }

    private ImagePlus runDistanceTransform(ImagePlus aImage) {
        boolean tempBlackBackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = true;
        final EDM filtEDM = new EDM();
        filtEDM.setup("Exact Euclidean Distance Transform (3D)", aImage);
        filtEDM.run(aImage.getProcessor());
        ij.Prefs.blackBackground = tempBlackBackground;
        aImage.setTitle("Distance Transform");
        aImage.show();
        return aImage;
    }

    private ImagePlus removeHoles(ImagePlus aImage) {
        IJ.run(aImage, "Invert", "stack");
        // "Fill Holes" is using Prefs.blackBackground global setting. We need false here.
        boolean tempBlackbackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = false;
        IJ.run(aImage, "Fill Holes", "stack");
        ij.Prefs.blackBackground = tempBlackbackground;

        IJ.run(aImage, "Invert", "stack");
        aImage.setTitle("Holes removed");
        aImage.resetDisplayRange();
        aImage.show();
        return aImage;
    }

    private ImagePlus runSquassh(ImagePlus aImage) {
        double[][][] img3 = ImgUtils.ImgToZXYarray(aImage);
        SegmentationParameters sp = new SegmentationParameters(4, 1, 0.0125, 0.0125, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.GAUSS, 1, 1, 0, 5);
        ImageStatistics statistics = aImage.getStatistics();
        SquasshSegmentation ss = new SquasshSegmentation(img3, sp, statistics.histMin, statistics.histMax);
        ss.run();
        ImagePlus ip1 = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(ss.iLabeledRegions), "Output");
        new ImageConverter(ip1).convertToGray8();
        return ip1;
    }
}
