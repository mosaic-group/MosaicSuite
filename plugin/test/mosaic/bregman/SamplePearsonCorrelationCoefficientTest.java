package mosaic.bregman;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;


public class SamplePearsonCorrelationCoefficientTest {

    @Test
    public void test1_2D() {
        final float[][] imgA = {{1, 0, 0, 0, 0},
                                {0, 1, 0, 0, 0},
                                {0, 0, 1, 0, 0},
                                {0, 0, 0, 1, 0},
                                {0, 0, 0, 0, 1}};
        ImagePlus ipA = new ImagePlus("", new FloatProcessor(imgA));
        final float[][] imgB = {{0, 0, 0, 0, 1},
                                {0, 0, 0, 1, 0},
                                {0, 0, 1, 0, 0},
                                {0, 1, 0, 0, 0},
                                {1, 0, 0, 0, 0}};
        ImagePlus ipB = new ImagePlus("", new FloatProcessor(imgB));
        
        double[] result = new SamplePearsonCorrelationCoefficient(ipA, ipB, false, 0, false, 0).run();
        assertArrayEquals(new double[]{0, 0}, result, 1e-6);
    }
    
    @Test
    public void test2_2D() {
        final float[][] imgA = {{0, 0, 0, 0, 1},
                                {0, 0, 0, 1, 0},
                                {0, 0, 1, 0, 0},
                                {0, 1, 0, 0, 0},
                                {1, 0, 0, 0, 0}};
        ImagePlus ipA = new ImagePlus("", new FloatProcessor(imgA));
        final float[][] imgB = {{0, 0, 0, 0, 1},
                                {0, 0, 0, 1, 0},
                                {0, 0, 1, 0, 0},
                                {0, 1, 0, 0, 0},
                                {1, 0, 0, 0, 0}};
        ImagePlus ipB = new ImagePlus("", new FloatProcessor(imgB));
        
        double[] result = new SamplePearsonCorrelationCoefficient(ipA, ipB, false, 0, false, 0).run();
        assertArrayEquals(new double[]{1, 1}, result, 1e-6);
    }
    
    @Test
    public void test3_2D() {
        final float[][] imgA = {{0, 0, 0, 0, 0},
                                {0, 1, 1, 1, 0},
                                {0, 1, 0, 1, 0},
                                {0, 1, 1, 1, 0},
                                {0, 0, 0, 0, 0}};
        ImagePlus ipA = new ImagePlus("", new FloatProcessor(imgA));
        final float[][] imgB = {{0, 0, 0, 0, 0},
                                {0, 1, 1, 1, 0},
                                {0, 1, 0, 1, 0},
                                {0, 1, 1, 1, 0},
                                {0, 0, 0, 0, 0}};
        ImagePlus ipB = new ImagePlus("", new FloatProcessor(imgB));
        
        double[] result = new SamplePearsonCorrelationCoefficient(ipA, ipB, true, 0.5, true, 0.25).run();
        assertArrayEquals(new double[]{1, 1}, result, 1e-6);
    }
    
    @Test
    public void test1_3D() {
        final float[][][] imgA = {{{1, 0, 0},
                                   {0, 0, 0},
                                   {0, 0, 0}},
                
                                  {{0, 0, 0},
                                   {0, 1, 0},
                                   {0, 0, 0}},
                                  
                                  {{0, 0, 0},
                                   {0, 0, 0},
                                   {0, 0, 1}} };
        ImageStack is = new ImageStack(3, 3);
        for (int i = 0; i < 3; i++) is.addSlice(new FloatProcessor(imgA[i]));
        ImagePlus ipA = new ImagePlus("", is);
        ImagePlus ipB = new ImagePlus("", is);
        
        double[] result = new SamplePearsonCorrelationCoefficient(ipA, ipB, false, 0, false, 0).run();
        assertArrayEquals(new double[]{1, 1}, result, 1e-6);
    }
    
    @Test
    public void test2_3D() {
        final float[][][] imgA = {{{0, 0, 0},
                                   {0, 0, 0},
                                   {1, 0, 0}},
                
                                  {{0, 0, 0},
                                   {0, 1, 0},
                                   {0, 0, 0}},
                                  
                                  {{0, 0, 1},
                                   {0, 0, 0},
                                   {0, 0, 0}} };
        ImageStack isa = new ImageStack(3, 3);
        for (int i = 0; i < 3; i++) isa.addSlice(new FloatProcessor(imgA[i]));
        ImagePlus ipA = new ImagePlus("", isa);
        final float[][][] imgB =  {{{0, 0, 1},
                                    {0, 0, 0},
                                    {0, 0, 0}},
                        
                                   {{0, 0, 0},
                                    {0, 1, 0},
                                    {0, 0, 0}},
                                   
                                   {{0, 0, 0},
                                    {0, 0, 0},
                                    {1, 0, 0}} };
        ImageStack isb = new ImageStack(3, 3);
        for (int i = 0; i < 3; i++) isb.addSlice(new FloatProcessor(imgB[i]));
        ImagePlus ipB = new ImagePlus("", isb);
        
        double[] result = new SamplePearsonCorrelationCoefficient(ipA, ipB, false, 0, false, 0).run();
        assertArrayEquals(new double[]{0.25, 0.25}, result, 1e-6);
    }
}
