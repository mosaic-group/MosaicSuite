package mosaic.plugins.utils;

import static org.junit.Assert.*;
import mosaic.utils.math.Matrix;

import org.junit.Test;

import static mosaic.plugins.utils.Interpolation.InterpolationType.*;
import static mosaic.plugins.utils.Interpolation.InterpolationMode.*;

public class InterpolationTest {

    @Test
    public void testBicubicMatlab() {
        // imresize([1 2 3 4; 5 6 7 8; 9 10 11 12; 13 14 15 16], [6 5], 'bicubic')
        double[][] expected = new double[][] {
                {0.728018518518519, 1.437018518518519, 2.268518518518519, 3.100018518518520, 3.809018518518518},
                {2.709499999999999, 3.418500000000000, 4.250000000000000, 5.081500000000000, 5.790500000000000},
                {5.626166666666661, 6.335166666666662, 7.166666666666663, 7.998166666666664, 8.707166666666664},
                {8.292833333333332, 9.001833333333334, 9.833333333333334, 10.664833333333336, 11.373833333333334},
                {11.209499999999998, 11.918499999999998, 12.750000000000000, 13.581500000000002, 14.290500000000002},
                {13.190981481481478, 13.899981481481477, 14.731481481481477, 15.562981481481481, 16.271981481481482}
        };
        
        double[][] input = {{1, 2, 3, 4},
                            {5, 6, 7, 8},
                            {9, 10, 11, 12},
                            {13, 14, 15, 16}};
        
        double[][] result = Interpolation.resize(input, 6, 5, BICUBIC, MATLAB);
        
        assertTrue(new Matrix(expected).compare(new Matrix(result), 1e-13));
    }
    
    @Test
    public void testBicubicMatlabShrink() {
        // imresize ([1 2 3 4; 2 3 4 5; 3 4 5 6; 4 5 6 7], [2 2 ])
        double[][] expected = new double[][] {
                {1.875000000000000, 4.000000000000000},
                {4.000000000000000, 6.125000000000000},
        };
        
        double[][] input = {{1, 2, 3, 4},
                            {2, 3, 4, 5},
                            {3, 4, 5, 6},
                            {4, 5, 6, 7}};

        double[][] result = Interpolation.resize(input, 2, 2, BICUBIC, MATLAB);

        assertTrue(new Matrix(expected).compare(new Matrix(result), 1e-13));
    }
    
    @Test
    public void testBilinearMatlab() {
        // imresize([1 2 3 4; 5 6 7 8; 9 10 11 12; 13 14 15 16], [6 5], 'bilinear')
        double[][] expected = new double[][] {
                {1.000000000000000, 1.700000000000000, 2.500000000000000, 3.300000000000000, 4.000000000000000},
                {3.000000000000000, 3.700000000000000, 4.500000000000000, 5.300000000000001, 6.000000000000000},
                {5.666666666666666, 6.366666666666666, 7.166666666666666, 7.966666666666667, 8.666666666666666},
                {8.333333333333332, 9.033333333333331, 9.833333333333332, 10.633333333333333, 11.333333333333332},
                {11.000000000000000, 11.699999999999999, 12.500000000000000, 13.300000000000001, 14.000000000000000},
                {13.000000000000000, 13.699999999999999, 14.500000000000000, 15.300000000000001, 16.000000000000000}
        };
        
        double[][] input = {{1, 2, 3, 4},
                            {5, 6, 7, 8},
                            {9, 10, 11, 12},
                            {13, 14, 15, 16}};
        
        double[][] result = Interpolation.resize(input, 6, 5, BILINEAR, MATLAB);
        
        assertTrue(new Matrix(expected).compare(new Matrix(result), 1e-13));
    }
    
    @Test
    public void testNearestMatlab() {
        // imresize([1 2 3 4; 5 6 7 8; 9 10 11 12; 13 14 15 16], [6 5], 'nearest')
        double[][] expected = new double[][] {
                {1, 2, 3, 3, 4},
                {5, 6, 7, 7, 8},
                {5, 6, 7, 7, 8},
                {9, 10, 11, 11, 12},
                {13, 14, 15, 15, 16},
                {13, 14, 15, 15, 16},
        };
        
        double[][] input = {{1, 2, 3, 4},
                            {5, 6, 7, 8},
                            {9, 10, 11, 12},
                            {13, 14, 15, 16}};
        
        double[][] result = Interpolation.resize(input, 6, 5, NEAREST, MATLAB);
        
        assertTrue(new Matrix(expected).compare(new Matrix(result), 1e-13));
    }
    
    @Test
    public void testBicubicNone() {
        double[][] expected = new double[][] {
                { 1.000, 1.438, 2.000, 2.500, 3.000, 3.563, 4.000}             
        };
        double[][] input = {{1, 2, 3, 4}};
    
        double[][] result = Interpolation.resize(input, 1, 7, BICUBIC, NONE);
                
        assertTrue(new Matrix(expected).compare(new Matrix(result), 1e-3));
    }
    
    @Test
    public void testBicubicSmart() {
        double[][] expected = new double[][] {
                { 1.000, 1.500, 2.000, 2.500, 3.000, 3.500, 4.000}             
        };        
        double[][] input = {{1, 2, 3, 4}};
    
        double[][] result = Interpolation.resize(input, 1, 7, BICUBIC, SMART);
        
        assertTrue(new Matrix(expected).compare(new Matrix(result), 1e-3));
    }
    
    @Test
    public void testBilinearNone() {
        double[][] expected = new double[][] {
                { 2.5 }             
        };        
        double[][] input = {{1, 2},{ 3, 4}};
    
        double[][] result = Interpolation.resize(input, 1, 1, BILINEAR, NONE);
        
        assertTrue(new Matrix(expected).compare(new Matrix(result), 1e-3));
    }
}
