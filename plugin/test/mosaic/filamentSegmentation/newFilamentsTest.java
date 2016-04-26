package mosaic.filamentSegmentation;


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
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
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import mosaic.utils.math.Matrix.MFunc;


public class newFilamentsTest extends CommonBase {

    @Test
    public void testGraph() {
        Matrix img = new Matrix(new double[][] { { 0, 1, 1, 0, 0 }, { 0, 0, 1, 1, 0 }, { 0, 1, 0, 1, 0 }, { 0, 0, 0, 1, 1 } });
        runMst(img);

    }

    static public class MstResult {

        MstResult(Matrix aM, UndirectedGraph<Vertex, DefaultEdge> aG, GraphPath<Vertex, DefaultEdge> aP) {
            m = aM;
            graph = aG;
            path = aP;
        }

        Matrix m;
        UndirectedGraph<Vertex, DefaultEdge> graph;
        GraphPath<Vertex, DefaultEdge> path;
    }

    private MstResult runMst(Matrix aImageMatrix) {
        List<List<Integer>> conn = findAllElementsOfObject(aImageMatrix, true);
        UndirectedGraph<Vertex, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);

        for (int i = 0; i < conn.size(); ++i) {
            int idx = conn.get(i).get(0);
            Vertex v = new Vertex(idx);
            g.addVertex(v);

            for (int z = 1; z < conn.get(i).size(); z++) {
                int idxz = conn.get(i).get(z);
                Vertex vz = new Vertex(idxz);
                g.addVertex(vz);
                g.addEdge(v, vz);
            }
        }

        // KruskalMinimumSpanningTree<Vertex, DefaultEdge> mst = new KruskalMinimumSpanningTree<>(g);
        // UndirectedGraph<Vertex, DefaultEdge> gMst = new SimpleGraph<>(DefaultEdge.class);
        // for (Vertex v : g.vertexSet()) gMst.addVertex(v);
        // for (DefaultEdge e : mst.getEdgeSet()) gMst.addEdge(g.getEdgeSource(e), g.getEdgeTarget(e));

        UndirectedGraph<Vertex, DefaultEdge> gMst = g;

        FloydWarshallShortestPaths<Vertex, DefaultEdge> paths = new FloydWarshallShortestPaths<>(gMst);
        GraphPath<Vertex, DefaultEdge> path = findLongestPath(gMst.vertexSet().iterator().next(), paths);
        Matrix result = aImageMatrix.copy().zeros();
        if (path == null) return new MstResult(result, null, null);
        for (DefaultEdge e : path.getEdgeList()) {
            result.set(gMst.getEdgeSource(e).x, 255);
            result.set(gMst.getEdgeTarget(e).x, 255);
        }

        return new MstResult(result, gMst, path);
    }

    private GraphPath<Vertex, DefaultEdge> findLongestPath(Vertex v, FloydWarshallShortestPaths<Vertex, DefaultEdge> paths) {
        List<GraphPath<Vertex, DefaultEdge>> sp = paths.getShortestPaths(v);
        double len = 0;
        Vertex end = null;
        for (GraphPath<Vertex, DefaultEdge> p : sp) {
            if (p.getWeight() > len) {
                len = p.getWeight();
                end = p.getEndVertex();
            }
        }
        List<GraphPath<Vertex, DefaultEdge>> sp2 = paths.getShortestPaths(end);
        len = 0;
        Vertex end2 = null;
        GraphPath<Vertex, DefaultEdge> plongest = null;
        for (GraphPath<Vertex, DefaultEdge> p : sp2) {
            if (p.getWeight() > len) {
                len = p.getWeight();
                end2 = p.getEndVertex();
                plongest = p;
            }
        }
        System.out.println(len + " " + end + ":" + end2);
        System.out.println(plongest);
        return plongest;
    }

    static class Vertex {

        Vertex(int num) {
            x = num;
        }

        int x;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + x;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Vertex other = (Vertex) obj;
            if (x != other.x) return false;
            return true;
        }

        @Override
        public String toString() {
            return "{" + x + "}";
        }
    }

    private static List<List<Integer>> findAllElementsOfObject(Matrix aMat, boolean aIs8connected) {
        aMat = Matlab.logical(aMat, 0); // make 0s and 1s
        // List of found elements belonging to one componnent
        final List<List<Integer>> elements = new ArrayList<List<Integer>>();

        final double[][] aM = aMat.getArrayXY();
        final int aWidth = aM.length;
        final int aHeight = aM[0].length;

        int aStartXpoint = -1;
        int aStartYpoint = -1;
        // Go through whole array
        for (int x = 0; x < aWidth; ++x) {
            for (int y = 0; y < aHeight; ++y) {
                if (aM[x][y] != 0) {
                    aStartXpoint = x;
                    aStartYpoint = y;
                }
            }
        }
        if (aStartXpoint < 0) return elements;

        int aLabel = 1234;

        // List of elements to be visited
        final List<Integer> q = new ArrayList<Integer>();

        // Initialize list with entry point
        q.add(aStartXpoint * aHeight + aStartYpoint);

        // Iterate until all elements of component are visited
        while (!q.isEmpty()) {
            // Get first element on the list and remove it
            final int id = q.remove(0);
            final int x = id / aHeight;
            final int y = id % aHeight;

            // Mark pixel and add it to element's container
            aM[x][y] = aLabel;
            List<Integer> ll = new ArrayList<Integer>();
            ll.add(id);
            elements.add(ll);

            // Check all neighbours of currently processed pixel
            // (do some additional logic to skip point itself and to handle 4/8
            // base connectivity)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        final int indX = x + dx;
                        final int indY = y + dy;
                        if (indX >= 0 && indX < aWidth && indY >= 0 && indY < aHeight) {
                            if (aIs8connected || (dy * dx == 0)) {
                                if (aM[indX][indY] == 1) {
                                    final int idx = indX * aHeight + indY;
                                    ll.add(idx);
                                    if (!q.contains(idx)) {
                                        // If element was not visited yet put it
                                        // on list
                                        q.add(idx);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return elements;
    }

    @Test
    public void fil() {
        // Input file
        String input = null;
        input = "/Users/gonciarz/test/zyx.tif";
        input = "/Users/gonciarz/test/xyz.tif";
        input = "/Users/gonciarz/test/short.tif";
        input = "/Users/gonciarz/test/test.tif";
        input = "/Users/gonciarz/test/Crop_12-12.tif";
        input = "/Users/gonciarz/test/multi.tif";
        input = "/Users/gonciarz/test/cross.tif";
        input = "/Users/gonciarz/test/DF_5.tif";
        input = "/Users/gonciarz/test/Crop44.tif";
        input = "/Users/gonciarz/test/sample.jpg";
        input = "/Users/gonciarz/test/spiral.tif";
        input = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/filamentsTest.tif";
        input = "/Users/gonciarz/test/many.tif";
        input = "/Users/gonciarz/test/maskFila.tif";
        input = "/Users/gonciarz/test/elispe.tif";
        boolean toBeSegmented = false;

        // Load input
        ImagePlus ip4 = loadImagePlus(input);
        ip4.setTitle("INPUT");
        ip4.show();

        final int w = ip4.getWidth();
        final int h = ip4.getHeight();

        // Input is to be segmented or ready mask?
        ImagePlus ip1 = toBeSegmented ? runSquassh(ip4) : ip4.duplicate();

        // Remove holes
        ip1 = binarizeImage(ip1);
        // ip1 = removeHoles(ip1);

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

            // Create graph
            // Do some operations on graph
            // create image from graph
            MstResult result = runMst(skelMatrix);
            mm[i] = result.m;

            // GENERATE SPLINES
            GraphPath<Vertex, DefaultEdge> path = result.path;
            if (path == null) continue;
            int len = path.getEdgeList().size();
            System.out.println("LENGHT: " + len);
            int max = 20;
            double step = (double) len / (max);
            if (step < 1) step = 1;
            List<Integer> pts = new ArrayList<Integer>();
            pts.add(path.getStartVertex().x);
            List<DefaultEdge> edgeList = path.getEdgeList();
            for (double currentIdx = 0; currentIdx < edgeList.size(); currentIdx += step) {
                if (currentIdx == 0) continue;
                pts.add(result.graph.getEdgeTarget(edgeList.get((int) currentIdx)).x);
            }
            pts.add(path.getEndVertex().x);

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
            System.out.println("INDEX: " + i);
            double smoothing = 0.5;
            final CubicSmoothingSpline cssX = new CubicSmoothingSpline(tz, xz, smoothing);
            final CubicSmoothingSpline cssY = new CubicSmoothingSpline(tz, yz, smoothing);
            // mosaic.utils.Debug.print(cssX, cssY, css);
            CSS cssOut = new CSS();
            cssOut.cssX = cssX;
            cssOut.cssY = cssY;
            css.add(cssOut);
        }

        // Merge results and show them
        Matrix out = mergeMatrices(mm);
        FloatProcessor floatProcessor = matrixToImage(w, h, out);
        ImagePlus xyz = showFilamentsOnInputImage(input, w, h, floatProcessor);
        draw(xyz, css);
        sleep(25000);
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

        double start = cssX.getKnot(0) - 10;
        double stop = cssX.getKnot(cssX.getNumberOfKNots() - 1) + 10;
        mosaic.utils.Debug.print("ST/ST", start, stop);
        final Matrix t = Matlab.linspace(start, stop, 100);
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
            System.out.println("Drawing....");
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
        ImagePlus skelImg = new ImagePlus("Skeleton", bp);
        skelImg.show();
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
        System.out.println(sortedSet.size() + " " + sortedSet.first() + " " + sortedSet.last());

        // find max threshold
        double previous = sortedSet.first();
        Matrix best = Matlab.logical(m, 0);
        if (!aOnlyRegionized) for (double th : sortedSet) {
            Matrix l = Matlab.logical(m, th);
            Map<Integer, List<Integer>> c = Matlab.bwconncomp(l, true);
            System.out.println(th + " " + c.size() + " " + c.keySet());
            if (c.size() == 1) {
                previous = th;
                best = l;
            }
            else
                break;
        }
        System.out.println("MAX th: " + previous);
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

    private FloatProcessor matrixToImage(final int w, final int h, Matrix aImageMatrix) {
        final double[][] result = aImageMatrix.getArrayYX();
        FloatProcessor floatProcessor = new FloatProcessor(w, h);
        ImagePlus ipout = new ImagePlus("Output", floatProcessor);
        ImgUtils.YX2DarrayToImg(result, floatProcessor, 1.0);
        ipout.setTitle("Matrix Image");
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
        System.out.println("Connected components " + cc.size());

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
