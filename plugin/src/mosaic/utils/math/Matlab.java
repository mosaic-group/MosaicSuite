package mosaic.utils.math;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mosaic.utils.math.Interpolation.InterpolationMode;
import mosaic.utils.math.Interpolation.InterpolationType;

public class Matlab {
    /**
     * Generates linear spaced numbers in array (as in Matlab)
     *
     * @param aMin
     * @param aMax
     * @param aNoOfSteps
     * @return
     */
    public static double[] linspaceArray(double aMin, double aMax, int aNoOfSteps) {
        if (aNoOfSteps < 1) {
            return null;
        }

        final double[] result = new double[aNoOfSteps];
        if (aNoOfSteps > 1) {
            final double step = (aMax - aMin) / (aNoOfSteps - 1);
            for (int i = 0; i < aNoOfSteps - 1; ++i) {
                result[i] = aMin + i * step;
            }
        }

        result[aNoOfSteps - 1] = aMax;

        return result;
    }

    /**
     * Generates linear spaced vector (as in Matlab)
     *
     * @param aMin
     * @param aMax
     * @param aNoOfSteps
     * @return
     */
    public static Matrix linspace(double aMin, double aMax, int aNoOfSteps) {
        return Matrix.mkRowVector(linspaceArray(aMin, aMax, aNoOfSteps));
    }

    /**
     * Generates spaced array (as in Matlab double start:step:stop operation)
     *
     * @param aStart
     * @param aStep
     * @param aNoOfSteps
     * @return
     */
    public static double[] regularySpacedArray(double aStart, double aStep, double aStop) {
        if (aStep == 0 || !((aStart > aStop) ^ (aStep > 0))) {
            return null;
        }

        final int noOfSteps = (int) ((aStop - aStart) / aStep) + 1;
        final double[] result = new double[noOfSteps];

        double val = aStart;
        for (int i = 0; i < noOfSteps; ++i) {
            result[i] = val;
            val += aStep;
        }

        return result;
    }

    /**
     * Generates spaced vector (as in Matlab double start:step:stop operation)
     *
     * @param aMin
     * @param aMax
     * @param aStep
     * @return
     */
    public static Matrix regularySpacedVector(double aMin, double aStep, double aMax) {
        return Matrix.mkRowVector(regularySpacedArray(aMin, aStep, aMax));
    }

    /**
     * Generates two matrices as Matlab's command 'meshgrid'
     *
     * @param aVector1
     *            - row values
     * @param aVector2
     *            - col values
     * @return
     */
    public static Matrix[] meshgrid(Matrix aVector1, Matrix aVector2) {
        // Adjust data but do not change users input - if needed make a copy.
        if (aVector1.isColVector()) {
            aVector1 = aVector1.copy().transpose();
        }
        if (aVector2.isRowVector()) {
            aVector2 = aVector2.copy().transpose();
        }

        final int r = aVector2.numRows();
        final int c = aVector1.numCols();

        final Matrix m1 = new Matrix(r, c);
        final Matrix m2 = new Matrix(r, c);

        for (int i = 0; i < r; ++i) {
            m1.insert(aVector1, i, 0);
        }
        for (int i = 0; i < c; ++i) {
            m2.insert(aVector2, 0, i);
        }

        return new Matrix[] { m1, m2 };
    }

    /**
     * Implementation of matlab's imfilter for 'symmetric' boundary options
     *
     * @param aImg - input image
     * @param aFilter - filter to be used
     * @return - filtered image (aImg is not changed)
     */
    public static Matrix imfilterSymmetric(Matrix aImg, Matrix aFilter) {
        final Matrix result = new Matrix(aImg.numRows(), aImg.numCols());

        final int filterRows = aFilter.numRows();
        final int filterCols = aFilter.numCols();
        final int filterRowMiddle = ((filterRows + 1) / 2) - 1;
        final int filterColMiddle = ((filterCols + 1) / 2) - 1;
        final double[][] filter = aFilter.getArrayYX();
        final int imageRows = aImg.numRows();
        final int imageCols = aImg.numCols();
        final double[][] image = aImg.getArrayYX();

        for (int r = 0; r < imageRows; ++r) {
            for (int c = 0; c < imageCols; ++c) {
                // (r,c) - element of image to be calculated
                double sum = 0.0;
                for (int fr = 0; fr < filterRows; ++fr) {
                    for (int fc = 0; fc < filterCols; ++fc) {
                        // Calculate image coordinates for (fr, fc) filter element
                        // This is symmetric filter so values outside the bounds of the array
                        // are computed by mirror-reflecting the array across the array border.
                        int imr = r - filterRowMiddle + fr;
                        int imc = c - filterColMiddle + fc;
                        do {
                            // Intentionally not if..else(d). For very long filters
                            // imr/imc can be smaller that 0 and after falling into one
                            // case they can be immediatelly bigger than imageRows/Cols
                            // that is also a reason for loop here since it may require
                            // several loops. (Usually filters are smaller than image so it
                            // is not a case but still left here to fully comply with Matlab
                            // version).
                            if (imr < 0) {
                                imr = -imr - 1;
                            }
                            if (imr >= imageRows) {
                                imr = (imageRows - 1) - (imr - (imageRows - 1) - 1);
                            }
                            if (imc < 0) {
                                imc = -imc - 1;
                            }
                            if (imc >= imageCols) {
                                imc = (imageCols - 1) - (imc - (imageCols - 1) - 1);
                            }
                        } while (!(imr >= 0 && imr < imageRows && imc >= 0 && imc < imageCols));

                        // After finding coordinates just compute next part of filter sum.
                        sum += filter[fr][fc] * image[imr][imc];
                    }
                }
                result.set(r, c, sum);
            }
        }

        return result;
    }

    /**
     * Implementation of matlab's imfilter for 'conv' boundary options
     *
     * @param aImg - input image
     * @param aFilter - filter to be used
     * @return - filtered image (aImg is not changed)
     */
    public static Matrix imfilterConv(Matrix aImg, Matrix aFilter) {
        final Matrix result = new Matrix(aImg.numRows(), aImg.numCols());

        final int filterRows = aFilter.numRows();
        final int filterCols = aFilter.numCols();
        final int filterRowMiddle = ((filterRows + 2) / 2) - 1;
        final int filterColMiddle = ((filterCols + 2) / 2) - 1;
        final double[][] filter = aFilter.getArrayYX();
        final int imageRows = aImg.numRows();
        final int imageCols = aImg.numCols();
        final double[][] image = aImg.getArrayYX();

        for (int r = 0; r < imageRows; ++r) {
            for (int c = 0; c < imageCols; ++c) {
                // (r,c) - element of image to be calculated
                double sum = 0.0;
                for (int fr = 0; fr < filterRows; ++fr) {
                    for (int fc = 0; fc < filterCols; ++fc) {
                        // Calculate image coordinates for (fr, fc) filter
                        // element
                        final int imr = r + filterRowMiddle - fr;
                        final int imc = c + filterColMiddle - fc;
                        double imgVal = 0;
                        if (imr >= 0 && imr < imageRows && imc >= 0 && imc < imageCols) {
                            imgVal = image[imr][imc];
                        }

                        // After finding coordinates just compute next part of
                        // filter sum.
                        sum += imgVal * filter[fr][fc];
                    }
                }
                result.set(r, c, sum);
            }
        }

        return result;
    }

    /**
     * Implementation of 'imresize' Matlab function for bicubic interpolation
     *
     * @param aM
     *            Input image
     * @param scale
     *            scale of image
     * @return scaled image
     */
    public static Matrix imresize(Matrix aM, double scale) {
        final int w = aM.numCols();
        final int h = aM.numRows();
        final int nw = (int) Math.ceil(w * scale);
        final int nh = (int) Math.ceil(h * scale);

        return imresize(aM, nw, nh);
    }

    /**
     * Implementation of 'imresize' Matlab function for bicubic interpolation
     *
     * @param aM Input image
     * @param aNewWidth
     * @param aNewHeight
     * @return scaled image
     */
    public static Matrix imresize(Matrix aM, int aNewWidth, int aNewHeight) {
        Matrix result = null;
        if (aM.numCols() != aNewWidth || aM.numRows() != aNewHeight) {
            final double[][] output = Interpolation.resize(aM.getArrayYX(),
                    aNewHeight, aNewWidth,
                    InterpolationType.BICUBIC,
                    InterpolationMode.MATLAB);

            result = new Matrix(output);
        } else {
            result = aM.copy();
        }

        return result;
    }

    /**
     * Implementation of 'bwconncomp' Matlab's command for finding connected
     * components in binary images
     *
     * @param aInputImg
     *            Matrix with values 0 and 1
     * @param aIs8connected
     *            if set to true then 8 based connectivity is used, otherwise 4
     *            based connectivity
     * @return map with all found components altogheter with pixel indices
     *         (Matlab style of numbering). Labels (which are keys) starts from
     *         number 2.
     */
    public static Map<Integer, List<Integer>> bwconncomp(Matrix aInputImg, boolean aIs8connected) {
        final double[][] img = aInputImg.getArrayXY();
        final int width = img.length;
        final int height = img[0].length;

        // Number for first label
        int labelIndex = 2;

        // Contains all found regions with element indices
        final Map<Integer, List<Integer>> connectedComponents = new HashMap<Integer, List<Integer>>();

        // Go through whole array
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (img[x][y] == 1) {
                    // Every time first pixel of new object is found do graph
                    // searching
                    connectedComponents.put(labelIndex,
                            findAllElementsOfObject(img, x, y, width, height, labelIndex, aIs8connected));

                    // Increase label value and continue searching
                    labelIndex++;
                }
            }
        }

        return connectedComponents;
    }

    /**
     * Do graph based searching for all pixels of connected component
     *
     * @return list with all pixel indices (Matlab's style of inexing top-down
     *         then right and again)
     */
    private static List<Integer> findAllElementsOfObject(double[][] aM, int aStartXpoint, int aStartYpoint, int aWidth,
            int aHeight, int aLabel, boolean aIs8connected) {
        // List of found elements belonging to one componnent
        final List<Integer> elements = new ArrayList<Integer>();

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
            elements.add(id);

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

    /**
     * Generates new Matrix with elements of aMatrix >aTreshold set to 1 and elements below are set to 0
     * @param aMatrix
     * @param aTreshold
     * @return
     */
    public static Matrix logical(final Matrix aMatrix, final double aTreshold) {
        final Matrix result = aMatrix.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return aElement > aTreshold ? 1 : 0;
            }
        });

        return result;
    }
}
