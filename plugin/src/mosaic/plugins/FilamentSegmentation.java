package mosaic.plugins;

import ij.process.FloatProcessor;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import mosaic.filamentSegmentation.SegmentationAlgorithm;
import mosaic.filamentSegmentation.SegmentationAlgorithm.NoiseType;
import mosaic.filamentSegmentation.SegmentationAlgorithm.PsfType;
import mosaic.math.CubicSmoothingSpline;
import mosaic.math.MFunc;
import mosaic.math.Matlab;
import mosaic.math.Matrix;
import mosaic.plugins.utils.Convert;
import mosaic.plugins.utils.Debug;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.PlugInFloatBase;

public class FilamentSegmentation extends PlugInFloatBase {
	// Distance between control points 
	private int iCoefficientStep;

	
	static Map<Integer, List<CubicSmoothingSpline>> m = new TreeMap<Integer, List<CubicSmoothingSpline>>();;
	static synchronized void addNewFindings(List<CubicSmoothingSpline> css, Integer number) {
	    m.put(number, css);
	}
	protected void segmentation(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
		// Get dimensions of input image
        final int originalWidth = aOrigImg.getWidth();
        final int originalHeight = aOrigImg.getHeight();

        float[][] img = new float[originalHeight][originalWidth];

        // Convert to array
        ImgUtils.ImgToYX2Darray(aOrigImg, img, 1.0f);
        //MinMax<Float> minMax = ImgUtils.findMinMax(img);
        
        
        
        double[][] id = Convert.toDouble(img);

        // TODO: segmentation goes here...
        // ==============================================================================
        
//        System.out.println("function jav()");
//        System.out.println("figure(1); hold off");
//        System.out.println("view([90 0])");
//        System.out.println("end");
        
//        BSplineSurface imgS = new BSplineSurface(img, (float)1, (float)width, (float)1, (float)height, 2, (float)20).showMatlabCode(2.5f, 2.5f);
//        BSplineSurface phi = generatePhi(88, 73, scale);
//        System.out.println("hold on");
//        BSplineSurface psi = generatePsi(88, 73, scale);

//        Matrix m = new Matrix(id);
        
//        // Sobel Filter`
//        Matrix m1 = Matlab.imfilterSymmetric(m, new Matrix(new double[][]{{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}}));
//        Matrix m2 = Matlab.imfilterSymmetric(m, new Matrix(new double[][]{{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}}));
//        m1.elementMult(m1);
//        m2.elementMult(m2);
//        m1.add(m2);
//        m1.sqrt();
//        id = m1.getArrayYX();
       
//        // Laplace filter
//        Matrix m1  = Matlab.imfilterSymmetric(m, new Matrix(new double[][] {{1, 1 ,1}, {1, -8, 1}, {1, 1, 1}}));
//        id = m1.getArrayYX();
        
//      // Laplacian of a Gaussian LoG filter
//        Matrix m1  = Matlab.imfilterSymmetric(m, new Matrix(new double[][] {{0,0,-1,0,0}, {0,-1,-2,-1,0},{-1,-2,16,-2,-1}, {0,-1,-2,-1,0}, {0,0,-1,0,0}}));
//        id = m1.getArrayYX();
//        int h = id.length; int w = id[0].length;
//        double [][] result = new double[h][w];
//        for (int y = 0; y < h-1; ++y) {
//            for (int x = 0; x < w-1; ++x) {
//                double val = 100;
//                boolean plu = false;
//                boolean min = false;
//                if (id[y][x] >=0) plu = true; else min = true;
//                if (id[y+1][x] >=0) plu = true; else min = true;
//                if (id[y][x+1] >=0) plu = true; else min = true;
//                if (id[y+1][x+1] >=0) plu = true; else min = true;
//                
//                if (min && plu) val = 0;
//                result[y][x] = val;
//            }
//        }
//        id = result;
//        System.out.println("Hello2");
//        
//        Matrix im = new Matrix(new double[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
//        Matrix ma = new Matrix(new double[][] {{9, 8, 7}, {6, 5, 4}, {3, 2, 1}});
//        Glm glm = new GlmGaussian();
//        RegionStatisticsSolver rss = new RegionStatisticsSolver(im, ma, glm, im, 2);
//        Matrix mi = rss.calculate().getModelImage();
//        System.out.println(mi);
//        System.out.println(glm.nllMean(im, mi, glm.priorWeights(im)));
        
        
//        double[][][] temp1 = new double[1][3][3];
//        double[][][] temp2 = new double[1][3][3];
//        double[][][] temp3 = new double[1][3][3];
//        double[][][] image = {{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}};
//        double[][][] mu = {{{1, 2 ,3}, {4, 5, 6}, {7, 8, 9}}};
//        double[][][] mask  = {{{0, 1, 1}, {1, 1 , 1}, {1, 1, 1}}};
//        double[][][] weights = {{{1, 1, 1} ,{ 1,1,1} , {1,1,1}}};
//        Parameters p = new Parameters();
//        p.nz=1;
//        p.ni=3;
//        p.nj=3;
//        Analysis.p.PSF = new GaussPSF<DoubleType>(3,DoubleType.class);
//        RegionStatisticsSolver rss = new RegionStatisticsSolver(temp1, temp2, mask, image, weights, 10, p);
//        rss.eval(mask);
//        System.out.println(rss.betaMLEout);
//        System.out.println(rss.betaMLEin);
        
//        System.out.println("function l()");
//        System.out.println("figure(1);");
//        System.out.println("view([90 0])");
//        new BSplineSurface(img, 1, width, 1, height, 2, 1).showMatlabCode(2, 2);
//        System.out.println("end");

        
        SegmentationAlgorithm sa = new SegmentationAlgorithm(id, 
                                                             NoiseType.GAUSSIAN, 
                                                             PsfType.GAUSSIAN, 
                                                             new Dimension(1, 1), 
                                /* subpixel sumpling */      1, 
                                /* scale */                  iCoefficientStep, 
                                /* regularizer term */       0.0001,
                                                             100);
        
        List<CubicSmoothingSpline> ps = sa.performSegmentation();
        addNewFindings(ps, aOrigImg.getSliceNumber());
        
        System.out.println("SLICE NUMBER: " + aOrigImg.getSliceNumber());
        
        for (CubicSmoothingSpline css : ps) {
            final CubicSmoothingSpline css1 = css;
            double start = css1.getKnot(0);
            double stop = css1.getKnot(css1.getNumberOfKNots() - 1);
            
            final Matrix x  = Matlab.linspace(start, stop, 1000);
            Matrix y = x.copy().process(new MFunc() { 
                @Override
                public double f(double aElement, int aRow, int aCol) {
                    return css1.getValue(x.get(aRow, aCol));
                }
            });
            
            for (int i = 0; i < x.size(); ++i) {
                id[(int)y.get(i)][(int)x.get(i)] = 255;
            }
            

        }
        
        // ==============================================================================

        img = Convert.toFloat(id);

        // Convert array to Image with converting back range of values
        //ImgUtils.convertRange(img, minMax.getMax() - minMax.getMin(), minMax.getMin());
        ImgUtils.YX2DarrayToImg(img, aOutputImg, 1.0f);
        

	}
	@Override
    protected void postprocess() {
	    
        System.out.println(m.size());
        for (Entry<Integer, List<CubicSmoothingSpline>> e : m.entrySet()) {
            Debug.print(e.getKey(), e.getValue().size());
            if (e.getValue().size() > 4) System.out.println(e.getValue());
        }
	};
	
	@Override
	protected boolean showDialog() {
		// TODO: this data should be handled in dialog window, hard-coded in a meantime
		
		// Should take values from 0..4 -> distance between control points is then 2**scale => 1..16
		iCoefficientStep = 1;
		
		return true;
	}

	@Override
	protected boolean setup(String aArgs) {
		return true;
	}

	@Override
	protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
		segmentation(aOutputImg, aOrigImg);
	}

}
