package mosaic.core.imageUtils.convolution;

import org.junit.Test;

import ij.ImagePlus;
import mosaic.regions.DRS.SobelVolume;
import mosaic.test.framework.CommonBase;
import mosaic.utils.ConvertArray;

public class ConvolverTest extends CommonBase {

    @Test
    public void test2D() {
        double[][] d = new double [][] {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 2, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0}
        };
        
        Kernel2D k = new Kernel2D() {{
            k = new double[][] {{1,1,1}, {1,1,1}, {1,1,1}};
            iHalfWidth = 1;
        }};
        Convolver c = new Convolver(d);
        c.xy2D(new Convolver(c), k);
        c.xy2D(new Convolver(c), k);
        
        Kernel1D k2 = new Kernel1D() {{
            k = new double[] {1,1,1};
            iHalfWidth = 1;
        }};
        Convolver c2 = new Convolver(d);
        c2.x1D(new Convolver(c2), k2);
        c2.y1D(new Convolver(c2), k2);
        c2.x1D(new Convolver(c2), k2);
        c2.y1D(new Convolver(c2), k2);
        
        CommonBase.compareArrays(c.getData(), c2.getData());
    }
    
    @Test
    public void test3D() {
        double[][][] d = new double [][][] {
                    {{1, 0, 0},
                     {0, 2, 0},
                     {0, 0, 3}},
                    {{1, 2, 3},
                     {1, 2, 3},
                     {1, 2, 3}},
                    {{1, 0, 0},
                     {0, 2, 0},
                     {0, 0, 3}},
            };
        
        Kernel3D k = new Kernel3D() {{
            k = new double[][][] { {{1,1,1}, {1,1,1}, {1,1,1}}, {{1,1,1}, {1,1,1}, {1,1,1}}, {{1,1,1}, {1,1,1}, {1,1,1}}};
            iHalfWidth = 1;
        }};
        Convolver c = new Convolver(d);
        c.xyz3D(new Convolver(c), k);
        c.xyz3D(k);
        
        Kernel1D k2 = new Kernel1D() {{
            k = new double[] {1,1,1};
            iHalfWidth = 1;
        }};
        Convolver c2 = new Convolver(d);
        c2.x1D(new Convolver(c2), k2);
        c2.y1D(new Convolver(c2), k2);
        c2.z1D(new Convolver(c2), k2);
        c2.x1D(k2);
        c2.y1D(k2);
        c2.z1D(k2);
        
        CommonBase.compareArrays(c.getData(), c2.getData());
    }
    
    @Test
    public void sobel2D() {
        double[][] d = new double [][] {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 2, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0}
        };

        Convolver c = new Convolver(d);
        SobelVolume sv = new SobelVolume(new ImagePlus("", c.getImageStack()));
        sv.sobel2D();
        
        c.sobel2D(new Convolver(c));
        
        CommonBase.compareArrays(c.getData(), ConvertArray.toDouble(sv.v));
    }
    
    @Test
    public void sobel3D() {
        double[][][] d = new double [][][] {
            {{1, 0, 0},
             {0, 2, 0},
             {0, 0, 3}},
            {{1, 2, 3},
             {1, 2, 3},
             {1, 2, 3}},
            {{1, 0, 0},
             {0, 2, 0},
             {0, 0, 3}},
    };

        Convolver c = new Convolver(d);
        SobelVolume sv = new SobelVolume(new ImagePlus("", c.getImageStack()));
        sv.sobel3D();
        
        c.sobel3D(new Convolver(c));
        
        CommonBase.compareArrays(c.getData(), ConvertArray.toDouble(sv.v));
    }
}
