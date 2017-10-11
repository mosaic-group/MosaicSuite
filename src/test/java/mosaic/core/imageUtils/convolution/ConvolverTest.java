package mosaic.core.imageUtils.convolution;

import org.junit.Test;

public class ConvolverTest {

    @Test
    public void test() {
        double[][] d = new double [][] {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0}
        };
        
        Kernel2D k = new Kernel2D() {{
            k = new double[][] {{1,1,1}, {1,1,1}, {1,1,1}};
            halfwidth = 1;
        }};
        Convolver c = new Convolver(d);
        c.convolvexy(new Convolver(c), k);
        c.convolvexy(new Convolver(c), k);

        System.out.println("\n-----------\n");
        
        Kernel1D k2 = new Kernel1D() {{
            k = new double[] {1,1,1};
            halfwidth = 1;
        }};
        Convolver c2 = new Convolver(d);
        c2.convolvex(new Convolver(c2), k2);
        c2.convolvey(new Convolver(c2), k2);
        c2.convolvex(new Convolver(c2), k2);
        c2.convolvey(new Convolver(c2), k2);
    }
    

}
