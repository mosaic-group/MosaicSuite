package mosaic.filamentSegmentation;


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;

import Skeletonize3D_.Skeletonize3D_;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import mosaic.bregman.segmentation.SegmentationParameters;
import mosaic.bregman.segmentation.SquasshSegmentation;
import mosaic.plugins.FilamentSquassh;
import mosaic.test.framework.CommonBase;
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


public class newFilamentsTest extends CommonBase {
    private static final Logger logger = Logger.getLogger(newFilamentsTest.class);

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
            DijkstraShortestPath<IntVertex, DefaultEdge> dijkstraShortestPath = new DijkstraShortestPath<>(gMst, path1.getStartVertex(), path1.getEndVertex());
            path = dijkstraShortestPath.getPath();
        }

        return new PathResult(gMst, path);
    }
    @Test
    public void fil2() {
        String input = "/Users/gonciarz/test/one.tif";
        input = "/Users/gonciarz/test/test1.tif";
        input = "/Users/gonciarz/test/test2.tif";
        input = "/Users/gonciarz/test/snr4.tif";
        input = "/Users/gonciarz/test/many.tif";
        FilamentSquassh fs = new FilamentSquassh();
        ImagePlus inputImg = loadImagePlus(input);
        inputImg.show();
        fs.setup("", inputImg);
        fs.run(inputImg.getProcessor());
        fs.setup("final", inputImg);
        sleep(14500);
    }
    
    @Test
    public void fil() {
        // Input file
        String input = null;
        input = "/Users/gonciarz/test/test.tif";
        input = "/Users/gonciarz/test/cc.tif";
        input = "/Users/gonciarz/test/xyz.tif";
        input = "/Users/gonciarz/test/single.tif";
        input = "/Users/gonciarz/test/one.tif";
        input = "/Users/gonciarz/test/DF_5.tif";
        input = "/Users/gonciarz/test/v.tif";
        input = "/Users/gonciarz/test/line.tif";
        input = "/Users/gonciarz/test/spiral.tif";
        input = "/Users/gonciarz/test/longlong.tif";
        input = "/Users/gonciarz/test/smile.tif";
        input = "/Users/gonciarz/test/35_55_psf4.tif";
        input = "/Users/gonciarz/test/triangle.tif";
        input = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/filamentsTest.tif";
        input = "/Users/gonciarz/test/cross.tif";
        input = "/Users/gonciarz/test/elispe.tif";
        input = "/Users/gonciarz/test/short.tif";
        input = "/Users/gonciarz/test/gradNoise.tif";
        input = "/Users/gonciarz/test/smallG.tif";
        input = "/Users/gonciarz/test/grad.tif";
        input = "/Users/gonciarz/test/maskFila.tif";
        input = "/Users/gonciarz/test/Crop_12-12.tif";
        input = "/Users/gonciarz/test/multi.tif";
        input = "/Users/gonciarz/test/many.tif";
        input = "/Users/gonciarz/test/Crop44.tif";
        input = "/Users/gonciarz/test/test.tif";
        input = "/Users/gonciarz/test/256.tif";
        input = "/Users/gonciarz/test/zyx.tif";
        input = "/Users/gonciarz/test/curve.tif";
        input = "/Users/gonciarz/test/sample.jpg";
        input = "/Users/gonciarz/test/snr4.tif";
        input = "/Users/gonciarz/test/test2.tif";
        input = "/Users/gonciarz/test/test1.tif";
        boolean toBeSegmented = false;

        // Load input
        ImagePlus ip4 = loadImagePlus(input);
        ip4.setTitle("INPUT");
        ip4.show();

        int w = ip4.getWidth();
        int h = ip4.getHeight();
        // Input is to be segmented or ready mask?
        ImagePlus ip1 = toBeSegmented ? runSquassh(ip4) : ip4.duplicate();

        // Remove holes
        ip1 = binarizeImage(ip1);
        ip1 = removeHoles(ip1);

        // Run distance transfrom
        // ImagePlus dist = runDistanceTransform(ip1.duplicate());
        ImagePlus dist = ip1.duplicate();

        // Convert to matrix
        final Matrix imgMatrix = convertToMatrix(dist);

        // Find connected components
        Matrix[] mm = createSeperateMatrixForEachRegion(imgMatrix);

        ImagePlus xyz1 = loadImagePlus(input);

        List<CSS> css = new ArrayList<CSS>();
        // Process each connected component
        for (int i = 0; i < mm.length; i++) {
            Matrix best = Matlab.logical(mm[i], 0);
            
            final ByteProcessor bp = skeletonize(w, h, best);
            // Matrix with skeleton
            final double[][] img2 = new double[h][w];
            ImgUtils.ImgToYX2Darray(bp.convertToFloatProcessor(), img2, 0.5);
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
            int max = len/pointsStep;
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

            CSS cssResult = calcSplines(xz, yz, tz, maxErr * 2);
            css.add(cssResult);

            // REFINE
            refine3(cssResult, ip4, ip1);

            CSS cssOut = new CSS();
            cssOut.cssX = new CubicSmoothingSpline(tz, xz, FittingStrategy.MaxSinglePointValue, maxErr * 2, maxErr);
            cssOut.cssY = new CubicSmoothingSpline(tz, yz, FittingStrategy.MaxSinglePointValue, maxErr * 2, maxErr);
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
            ip4.getProcessor().setInterpolate(true);
            ImageProcessor.setUseBicubic(true);
            double sum = 0;
            for (int ti = 0; ti < xs.getNumberOfKNots(); ti++) {
                double tvv = xs.getKnot(ti);
                double xvv = xs.getValue(tvv);
                double yvv = ys.getValue(tvv);
                double pixelValue = ip4.getProcessor().getInterpolatedValue(xvv - 0.5, yvv - 0.5);
                System.out.print(pixelValue + " ");
                sum += pixelValue;
            }
            System.out.println();
            
            double avgIntensity =  sum / xs.getNumberOfKNots();
            double boundaryValue = avgIntensity * valueOfGauss(psf);
            ip1.getProcessor().setInterpolate(true);
            ImageProcessor.setUseBicubic(true);
            for (double tn = 0; tn > -100; tn -= 0.001) {
                double xvv = xs.getValue(tn);
                double yvv = ys.getValue(tn);
                double pixelValue = ip4.getProcessor().getInterpolatedValue(xvv-0.5, yvv-0.5);
                if (pixelValue >= boundaryValue && ip1.getProcessor().getInterpolatedValue(xvv - 0.5, yvv -0.5) >=128) cssOut.tMin = tn;
                else {System.out.println("BREAK @ " + xvv + "," + yvv);break;}
            }
            double tMaximum = xs.getKnot(xs.getNumberOfKNots() - 1);
            for (double tn = tMaximum; tn < tMaximum + 100 ; tn += 0.001) {
                double xvv = xs.getValue(tn);
                double yvv = ys.getValue(tn);
                double pixelValue = ip4.getProcessor().getInterpolatedValue(xvv-0.5, yvv-0.5);
                if (pixelValue >= boundaryValue && ip1.getProcessor().getInterpolatedValue(xvv-0.5, yvv -0.5) >= 128) cssOut.tMax = tn;
                else{System.out.println("BREAK @ " + xvv + "," + yvv);break;}
            }
        }

        // Merge results and show them
        xyz1.setTitle("FILAMENTS");
        xyz1.show();
        ImagePlus xyz = xyz1;
        draw(xyz, css);
        drawPerpendicularLines(css, xyz);
        IJ.save(xyz, "/tmp/processed.tif");
        sleep(2500);
    }

    final private static int pointsStep = 1;
    final private static double maxErr = 0.625;
    final private static double psf = 2;
    final private static double lenOfRefineLine = 3;
    
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

            double x1 = (x) - lenOfRefineLine * Math.cos(alpha - Math.PI/2);
            double x2 = (x) + lenOfRefineLine * Math.cos(alpha - Math.PI/2);
            double y1 = (y) - lenOfRefineLine * Math.sin(alpha - Math.PI/2);
            double y2 = (y) + lenOfRefineLine * Math.sin(alpha - Math.PI/2);

            // TODO: Handle situation when two parts of regions are too close that refinement line crossing both.
            int inter=(int)lenOfRefineLine * 2 * 20 + 1;
            double dxi = (x2 - x1)/(inter - 1);
            double dyi = (y2 - y1)/(inter - 1);
            double sumx = 0, sumy = 0, wx = 0, wy = 0;
            double px = x1, py = y1;
            xyz.getProcessor().setInterpolate(true);
            ImageProcessor.setUseBicubic(true);
            double shift = 0.5; // shift for bicubic interpolation making it symmetric in respect to middle of pixel.
            
            if (num < 30) mosaic.utils.Debug.print("NUM=0 ----------------", x, y, px, py, alpha);
            for (int i = 0; i < inter; ++i) {
                mosaic.utils.Debug.print((int)(px - shift), (int)(py - shift));
                double pixelValue = xyz.getProcessor().getInterpolatedValue((float)px - shift, (float)py - shift);
                if (binary.getProcessor().getPixelValue((int)(px), (int)(py)) == 0) pixelValue = 0;
                wx += pixelValue;
                wy += pixelValue;
                System.out.println(px + "," + py + " - " +  pixelValue);
                sumx += px * pixelValue;
                sumy += py * pixelValue;
                px += dxi;
                py += dyi;
            }
            if (wx == 0 || wy == 0) continue;

            c.x[num] = (sumx/wx);
            c.y[num] = (sumy/wy);
            if (num == 0) mosaic.utils.Debug.print("COORD: ", x, y, c.x[num], c.y[num]);
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

    private void drawPerpendicularLines(List<CSS> css, ImagePlus xyz) {
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
                double x1 = x - lenOfRefineLine * Math.cos(alpha - Math.PI/2);
                double x2 = x + lenOfRefineLine * Math.cos(alpha - Math.PI/2);
                double y1 = y - lenOfRefineLine * Math.sin(alpha - Math.PI/2);
                double y2 = y + lenOfRefineLine * Math.sin(alpha - Math.PI/2);

                Roi r = new Line(x1, y1, x2, y2);
                r.setPosition(0);
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

    private void draw(ImagePlus outImg, List<CSS> cssd) {
        Overlay overlay = new Overlay();

        // for every image take all filaments
        for (CSS css : cssd) {
            logger.debug("Drawing....");
            final CSS css1 = css;
            FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(css1.cssX, css1.cssY, css1.tMin, css1.tMax);
            drawFilamentsOnOverlay(overlay, 0, coordinates, css.color);
            
            for (int i = 0 ; i < css.x.length; i++) {
                Roi r = new ij.gui.EllipseRoi(css.x[i]-0.1, css.y[i]-0.1, css.x[i]+0.1, css.y[i]+0.1, 1);
                r.setStrokeColor(Color.BLUE);
                r.setFillColor(Color.BLUE);
                overlay.add(r);
                if (css.color == Color.RED) {
                r = new ij.gui.EllipseRoi(css.xinit[i]-0.1, css.yinit[i]-0.1, css.xinit[i]+0.1, css.yinit[i]+0.1, 1);
                r.setStrokeColor(Color.BLACK);
                overlay.add(r);}
            }
        }

        outImg.setOverlay(overlay);
    }

    private void drawFilamentsOnOverlay(Overlay aOverlay, int aSliceNumber, FilamentXyCoordinates coordinates, Color color) {
        Roi r = new PolygonRoi(coordinates.x.getArrayYXasFloats()[0], coordinates.y.getArrayYXasFloats()[0], Roi.POLYLINE);
        r.setPosition(aSliceNumber);
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
//            ImagePlus skelImg = new ImagePlus("Skeleton", bp);
//            skelImg.show();
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

    private Matrix convertToMatrix(ImagePlus aIamge) {
        FloatProcessor fp = aIamge.getProcessor().convertToFloatProcessor();
        int w = aIamge.getWidth();
        int h = aIamge.getHeight();
        final double[][] img = new double[h][w];
        ImgUtils.ImgToYX2Darray(fp, img, 1.0);
        final Matrix imgMatrix = new Matrix(img);
        return imgMatrix;
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
        SegmentationParameters sp = new SegmentationParameters(4, 1, 0.006125, 0.006125, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.GAUSS, psf, 1, 0, 5);
        ImageStatistics statistics = aImage.getStatistics();
        SquasshSegmentation ss = new SquasshSegmentation(img3, sp, statistics.histMin, statistics.histMax);
        ss.run();
        ImagePlus ip1 = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(ss.iLabeledRegions), "Output");
        new ImageConverter(ip1).convertToGray8();
        return ip1;
    }

    @Test
    public void gauss() {
        System.out.println(valueOfGauss(4));
    }
    private double valueOfGauss(double aSigma) {
        // Make reasonable large number of points to keep quality
        final int NumOfPsfPoints = (int)aSigma * 20;
        
        // Make sure that we have odd number of points (this is required by later code).
        int len = (NumOfPsfPoints / 2) * 2 + 1;
        
        //  Generate gauss with provided sigma
        double[] gauss = GaussPsf.generateKernel(len, 1, aSigma)[0];
        
        
        // Generate step function
        double[] step = new double[len  * 2];
        for (int i = len; i < len * 2; i++) step[i] = 1;

        // Convolve exactly at step point (for n = len)
        double sum = 0;
        int n = len;
        for (int m = -len/2; m <= len/2; m++) {
            sum += step[n - m] * gauss[m + len/2 /* moves -n/2..n/2 to 0..n */];
        }
        
        return sum;
    }
}
