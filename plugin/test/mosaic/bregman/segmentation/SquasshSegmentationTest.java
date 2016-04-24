package mosaic.bregman.segmentation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

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
import ij.process.ImageProcessor;
import mosaic.filamentSegmentation.SegmentationFunctions.FilamentXyCoordinates;
import mosaic.test.framework.CommonBase;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;


public class SquasshSegmentationTest extends CommonBase {

    @Test
    public void testGenerateSeriesOfSquassh() {
//        String path = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/Crop_12-12.tif";
//        ImagePlus ip = loadImagePlus(path);
//        
//        double[][][] img = ImgUtils.ImgToZXYarray(ip);
//        short[][] blank = new short[img[0].length][img[0][0].length];
//        
//        int numOfSteps = 256;
//        short[][][] out = new short[numOfSteps][][];
//        for (int i = 0; i < numOfSteps; i++) {
//            System.out.println(i * (1.0/(numOfSteps)));
//            SegmentationParameters sp = new SegmentationParameters(4, 1, 0.1, i * (1.0/(numOfSteps-1)), true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.GAUSS, 1, 1, 0);
//            SquasshSegmentation ss = new SquasshSegmentation(img, sp, 0, 255);
//            ss.run();
//            out[i] = (ss.iRegionsList.size() > 0) ? ss.iLabeledRegions[0] : blank;
//        }
//        ImagePlus outImg = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(out), "Output");
//        IJ.saveAsTiff(outImg, "/tmp/out.tif");
    }

    @Test
    public void test1ctrl2() {
//        String path = "/Users/gonciarz/Documents/MOSAIC/work/repo/MosaicSuite/plugin/Jtest_data/Squassh/ScriptR/1 Ctrl 2.tif";
//        ImagePlus ipl = loadImagePlus(path);
//        System.out.println(ImgUtils.getImageInfo(ipl));
//        ImagePlus ip = setupChannel(ipl, 1, 2);
//        double[][][] img = ImgUtils.ImgToZXYarray(ip);
//        MinMax<Double> mm = ImgUtils.findMinMax(ip);
//        SegmentationParameters sp = new SegmentationParameters(4, 1, 0.2, 0.03, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.POISSON, 0.63, 1, 0);
//        SquasshSegmentation ss = new SquasshSegmentation(img, sp, mm.getMin(), mm.getMax());
//        ss.run();
//        ImagePlus outImg = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(ss.iLabeledRegions), "Output");
//        IJ.saveAsTiff(outImg, "/tmp/out.tif");
//        try {
//            Thread.sleep(30000);
//        }
//        catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }
    
//    @Test
//    public void test2() {
//        int size = 6 ; int from = size/3; int to = 2*size/3;
//        int count = 0;
//        double[][] img = new double[size][size];
//        for (int x = from; x < to; x++)
//            for (int y = from; y < to; y++)
//                {img[x][y] = 0.1; count++;}
//        System.out.println(generateAsciiImage(img));
//        SegmentationParameters sp = new SegmentationParameters(1, 1, 0.3, 0.01, true, SegmentationParameters.IntensityMode.HIGH, SegmentationParameters.NoiseModel.POISSON, 0.1, 0.1, 0);
//        SquasshSegmentation ss = new SquasshSegmentation(new double[][][] {img}, sp, 0, 0.1);
//        ss.run();
//        
//        System.out.println(generateAsciiImage(ConvertArray.toDouble(ss.iLabeledRegions)[0]));
//        System.out.println(ss.iRegionsList.get(0).iPixels.size() + " vs " + count);
//    }
//    
//    private String generateAsciiImage(double[][] aImage) {
//        System.out.println(Debug.getArrayDims(aImage));
//        int sizeX = aImage.length;
//        int sizeY = aImage[0].length;
//        StringBuilder sb = new StringBuilder();
//        for (int y = 0; y < sizeY; ++y) {
//            for (int x = 0; x < sizeX; ++x) {
//                sb.append(Tools.round(aImage[x][y], 1) + " ");
//            }
//            sb.append('\n');
//        }
//        String result = sb.toString();
//        return result;
//    }
    
    @Test
    public void testGray() {
//        String path1 = "/tmp/test1.tif";
//        ImagePlus ip1 = loadImagePlus(path1);
//        String path2 = "/tmp/test2.tif";
//        ImagePlus ip2 = loadImagePlus(path2);
//        int channels = 2;
//        ImagePlus[] ips = new ImagePlus[] {ip1, ip2};
//        ImageStack is = new ImageStack(ip1.getWidth(), ip1.getHeight());
//        for (int f = 0; f < ip1.getNFrames();f++)
//        for (int z = 0; z < ip1.getNSlices();z++)
//        for (int c = 0; c < channels; c++) {
//            int sidx = ip1.getStackIndex(1, z + 1, f + 1);
//            mosaic.utils.Debug.print(sidx, c, z, f);
//            is.addSlice(ips[c].getStack().getProcessor(sidx));
//        }
//        
//        ip1.setStack(is);
//        ip1.setDimensions(2, 1, 3);
//        ip1.setOpenAsHyperStack(true);
//        ip1.show();
//      try {
//      Thread.sleep(30000);
//      }
//      catch (InterruptedException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }
    }
    
    static class Vertex {
        Vertex(int num, String aName) { x = num; name = aName; }
        int x;
        String name;
        
        
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
            return "{"+x+", "+name+"}";
        }
    }
    
    @Test
    public void filament() {
        UndirectedGraph<Vertex, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);

        final int num = 10;
        Vertex[] vl = new Vertex[num + 1];
        for (int i = 0; i <= num; ++i) {
            Vertex v = new Vertex(i, "v"+i);
            vl[i] = v; 
            g.addVertex(v);
        }
        
        // add edges to create a circuit
        g.addEdge(vl[1], vl[4]);
        g.addEdge(vl[2], vl[3]);
        g.addEdge(vl[3], vl[4]);
        g.addEdge(vl[4], vl[5]);
        g.addEdge(vl[6], vl[5]);
        g.addEdge(vl[6], vl[7]);
        g.addEdge(vl[6], vl[8]);
        g.addEdge(vl[6], vl[9]);
        g.addEdge(vl[10], vl[9]);
        
        g.addEdge(vl[2], vl[0]);
        
        FloydWarshallShortestPaths<Vertex, DefaultEdge> paths = new FloydWarshallShortestPaths<>(g);
//        System.out.println(paths.getDiameter());
//        System.out.println(paths.getShortestPathsCount());
        for (Vertex v : vl) findLongestPath(v, paths);
        
        KruskalMinimumSpanningTree<Vertex, DefaultEdge> mst = new KruskalMinimumSpanningTree<>(g);
//        System.out.println(mst.getSpanningTreeCost());
    }

    private void findLongestPath(Vertex v, FloydWarshallShortestPaths<Vertex, DefaultEdge> paths) {
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
    }
    
    @Test
    public void fil() {
        String input = null;
        input = "/Users/gonciarz/test/spiral.tif";
        input = "/Users/gonciarz/test/sample.jpg";
        input = "/Users/gonciarz/test/Crop_12-12.tif";
        input = "/Users/gonciarz/test/xyz.tif";
//        ImagePlus ip2 = loadImagePlus("/Users/gonciarz/test/mask.tif");
//      ImagePlus ip2 = loadImagePlus("/Users/gonciarz/test/sample.jpg");
      ImagePlus ip2 = loadImagePlus(input);
        ip2.show();
        
        new ImageConverter(ip2).convertToGray8();
        double[][][] img3 = ImgUtils.ImgToZXYarray(ip2);
        short[][] blank = new short[img3[0].length][img3[0][0].length];
        SegmentationParameters sp = new SegmentationParameters(4, 1, 0.025, 0.05, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.GAUSS, 1, 1, 0, 2);
        SquasshSegmentation ss = new SquasshSegmentation(img3, sp, 0, 255);
        ss.run();
        ImagePlus ip1 = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(ss.iLabeledRegions), "Output");
        new ImageConverter(ip1).convertToGray8();
        
        // RUn distance transfrom
        ImagePlus dist = ip1.duplicate();
        boolean tempBlackBackground = ij.Prefs.blackBackground;
        ij.Prefs.blackBackground = true;
        final EDM filtEDM = new EDM();
        filtEDM.setup("Exact Euclidean Distance Transform (3D)", dist);
        filtEDM.run(dist.getProcessor());
        ij.Prefs.blackBackground = tempBlackBackground;
        dist.show();
        
        // Convert to array
        FloatProcessor fp  = dist.getProcessor().convertToFloatProcessor();
        final int w = dist.getWidth();
        final int h = dist.getHeight();
        final double[][] img = new double[h][w];
        ImgUtils.ImgToYX2Darray(fp, img, 1.0);
        final Matrix imgMatrix = new Matrix(img);
        
        // Find connected components
        Matrix logical = Matlab.logical(imgMatrix, 0);
        Map<Integer, List<Integer>> cc = Matlab.bwconncomp(logical, true);
        System.out.println("Connected components " + cc.size());
        
        // Create sepreate matrix for each connected component
        Matrix[] mm = new Matrix[cc.size()]; int idx = 0;
        for (List<Integer> p : cc.values()) {
            Matrix m = imgMatrix.copy().zeros();
            for (int i : p) m.set(i, imgMatrix.get(i));
            mm[idx] = m;
            idx++;
        }
        
        //Process each connected component
        for (int i = 0; i < mm.length; i ++) {
            Matrix m = mm[i];
            // Find all levels of elements
            double[] data = m.getData();
            SortedSet<Double> sortedSet = new TreeSet<Double>();
            for (double d : data) sortedSet.add(d);
            System.out.println(sortedSet.size() + " "  + sortedSet.first() + " " + sortedSet.last());
            
            // find max threshold
            double previous = sortedSet.first();
            Matrix best = Matlab.logical(m, 0);
//            for (double th : sortedSet) {
//                Matrix l = Matlab.logical(m, th);
//                Map<Integer, List<Integer>> c = Matlab.bwconncomp(l, true);
//                System.out.println(th + " " + c.size() +  " " + c.keySet());
//                if (c.size() == 1) {previous = th; best  = l;}
//                else break;
//            }
            
            System.out.println("MAX th: " + previous);
            
            // Convert to byte processor
            byte[] maskBytes = new byte[w * h];
            System.out.println("Connecting componenets");
            System.out.println(best.getData().length + " " + (w*h));
            for (int x  = 0; x < best.getData().length; x++) maskBytes[x] = best.getData()[x] > 0 ? 1 : (byte)0;
            System.out.println("Connecting componenets2");
            final ByteProcessor bp = new ByteProcessor(w, h, maskBytes);
            
            // And skeletonize
            final ImagePlus skeleton = new ImagePlus("Skeletonized", bp);
            Skeletonize3D_ skel = new Skeletonize3D_();
            skel.setup("", skeleton);
            skel.run(skeleton.getProcessor());

            // Put as a final 
            final double[][] img2 = new double[h][w];
            ImgUtils.ImgToYX2Darray(bp.convertToFloatProcessor(), img2, 1.0);
            final Matrix imgMatrix2 = new Matrix(img2);
            mm[i] = imgMatrix2;
        }
        
        // Merge results
        Matrix out = mm[0];
        for (int i = 1; i < mm.length; i++) out.add(mm[i]);
        
        // Update output with reslt
        final double[][] result = out.getArrayYX();
        FloatProcessor floatProcessor = new FloatProcessor(w,h);
        ImagePlus ipout = new ImagePlus("Output", floatProcessor);
        ImgUtils.YX2DarrayToImg(result, floatProcessor, 1.0);
        ipout.show();
        
        ImagePlus xyz = loadImagePlus(input);
        xyz.setTitle("FILAMENTS");
        byte[] pixelsOut = (byte[]) xyz.getProcessor().getPixels();
        float[] pixelsIn = (float[]) floatProcessor.getPixels();
        
        for (int x = 0; x < w; x++) 
            for (int y = 0; y < h ; y++ ) {
                pixelsOut[x + y * w] = pixelsIn[x + y * w] > 0 ? (byte) 0  : pixelsOut[x + y * w];
            }
        xyz.show();
        
        sleep(1200000);
    }



}
