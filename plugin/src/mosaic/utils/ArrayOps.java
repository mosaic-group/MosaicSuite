package mosaic.utils;

public class ArrayOps {
    
    static public class MinMax<T> {
        private final T min;
        private final T max;
    
        MinMax(T aMin, T aMax) {
            min = aMin;
            max = aMax;
        }
    
        public T getMin() {
            return min;
        }
    
        public T getMax() {
            return max;
        }
    }

    static public MinMax<Float> findMinMax(final float[][] aArray) {
        final int arrayW = aArray[0].length;
        final int arrayH = aArray.length;
    
        // Find min and max value of image
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                final float pix = aArray[y][x];
                if (pix < min) {
                    min = pix;
                }
                if (pix > max) {
                    max = pix;
                }
            }
        }
    
        return new MinMax<Float>(min, max);
    }

    static public MinMax<Double> findMinMax(final double[][] aArray) {
        final int arrayW = aArray[0].length;
        final int arrayH = aArray.length;
    
        // Find min and max value of image
        double min = Float.MAX_VALUE;
        double max = -Float.MAX_VALUE;
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                final double pix = aArray[y][x];
                if (pix < min) {
                    min = pix;
                }
                if (pix > max) {
                    max = pix;
                }
            }
        }
    
        return new MinMax<Double>(min, max);
    }
    
    static public MinMax<Float> findMinMax(final float[] aArray) {
        final int len = aArray.length;
    
        // Find min and max value of image
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int x = 0; x < len; ++x) {
            final float pix = aArray[x];
            if (pix < min) {
                min = pix;
            }
            if (pix > max) {
                max = pix;
            }
        }
    
        return new MinMax<Float>(min, max);
    }

    static public MinMax<Double> findMinMax(final double[] aArray) {
        final int len = aArray.length;
        
        // Find min and max value of image
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int x = 0; x < len; ++x) {
            final double pix = aArray[x];
            if (pix < min) {
                min = pix;
            }
            if (pix > max) {
                max = pix;
            }
        }
    
        return new MinMax<Double>(min, max);
    }
    
    /**
     * Normalize values in array to 0..1 range
     * @param aArray
     */
    static public void normalize(final float[][] aArray) {
        // Find min and max value of image
        MinMax<Float> minMax = findMinMax(aArray);
        float min = minMax.getMin();
        float max = minMax.getMax();
    
        // Normalize with found values
        final int arrayW = aArray[0].length;
        final int arrayH = aArray.length;
        if (max != min) {
            for (int y = 0; y < arrayH; ++y) {
                for (int x = 0; x < arrayW; ++x) {
                    aArray[y][x] = (aArray[y][x] - min) / (max - min);
                }
            }
        }
    }

    /**
     * Normalize values in array to 0..1 range
     * @param aArray
     */
    static public void normalize(final double[][] aArray) {
        // Find min and max value of image
        MinMax<Double> minMax = findMinMax(aArray);
        double min = minMax.getMin();
        double max = minMax.getMax();
    
        // Normalize with found values
        final int arrayW = aArray[0].length;
        final int arrayH = aArray.length;
        if (max != min) {
            for (int y = 0; y < arrayH; ++y) {
                for (int x = 0; x < arrayW; ++x) {
                    aArray[y][x] = (aArray[y][x] - min) / (max - min);
                }
            }
        }
    }
    
    /**
     * Normalize values in array to 0..1 range
     * @param aArray
     */
    static public void normalize(final float[] aArray) {
        // Find min and max value of image
        MinMax<Float> minMax = findMinMax(aArray);
        float min = minMax.getMin();
        float max = minMax.getMax();
    
        // Normalize with found values
        final int len = aArray.length;
        if (max != min) {
            for (int x = 0; x < len; ++x) {
                aArray[x] = (aArray[x] - min) / (max - min);
            }
        }
    }

    /**
     * Normalize values in array to 0..1 range
     * @param aArray
     */
    static public void normalize(final double[] aArray) {
        // Find min and max value of image
        MinMax<Double> minMax = findMinMax(aArray);
        double min = minMax.getMin();
        double max = minMax.getMax();
    
        // Normalize with found values
        final int len = aArray.length;
        if (max != min) {
            for (int x = 0; x < len; ++x) {
                aArray[x] = (aArray[x] - min) / (max - min);
            }
        }
    }
    
    /**
     * Converts range of values in array computing for each element:  (elem * aMultiply + aShift)
     * Technically it makes opposite operation than normalization.
     * @param aArray - array to be converted
     * @param aMultiply - multiplication factor
     * @param aShift - shift value
     */
    static public void convertRange(final float[][] aArray, float aMultiply, float aShift) {
        final int arrayW = aArray[0].length;
        final int arrayH = aArray.length;
    
        // Normalize with found values
        for (int y = 0; y < arrayH; ++y) {
            for (int x = 0; x < arrayW; ++x) {
                aArray[y][x] = aArray[y][x] * aMultiply + aShift;
            }
        }
    }
    
    static public void fillArray(short[][][] aArray, short aValue) {
        int lenX = aArray.length;
        int lenY = aArray[0].length;
        int lenZ = aArray[0][0].length;
        
        for (int x = 0; x < lenX; ++x) {
            for (int y = 0; y < lenY; ++y) {
                for (int z = 0; z < lenZ; ++z) {
                    aArray[x][y][z] = aValue;
                }
            }
        }
    }
}
