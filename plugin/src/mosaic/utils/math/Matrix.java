package mosaic.utils.math;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import mosaic.utils.ConvertArray;

/**
 * Wrapper for ejml library with additional number of helpful functions and operations.
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
public class Matrix {

    private DenseMatrix64F iMatrix;

    /**
     * Only for private use. Keeps provided matrix reference (not copy!).
     *
     * @param aDM
     */
    private Matrix(DenseMatrix64F aDM) {
        iMatrix = aDM;
    }

    /**
     * Creates new Matrix with provided number rows and columns
     *
     * @param aRows
     * @param aCols
     */
    public Matrix(int aRows, int aCols) {
        iMatrix = new DenseMatrix64F(aRows, aCols);
    }

    /**
     * Creates new Matrix from copy of provided matrix
     *
     * @param aM
     */
    public Matrix(Matrix aM) {
        iMatrix = new DenseMatrix64F(aM.iMatrix);
    }

    /**
     * Creates new Matrix from provided values. Number of provided values 
     * should equal aNumOfRows * aNumOfCols (it is not checked!). 
     * Provided values are put in matrix row-major.
     * @param aNumOfRows
     * @param aNumOfCols
     * @param aValues
     */
    public Matrix(int aNumOfRows, int aNumOfCols, double... aValues) {
        iMatrix = new DenseMatrix64F(aNumOfRows, aNumOfCols, true, aValues);
    }
    
    /**
     * Creates new Matrix from provided 2D array first dimension is rows and
     * then cols ([r][c] or [y][x]).
     *
     * @param aArray
     */
    public Matrix(double[][] aArray) {
        iMatrix = new DenseMatrix64F(aArray);
    }

    /**
     * Creates new Matrix from provided 2D array first dimension is rows and
     * then cols ([r][c] or [y][x]).
     *
     * @param aArray
     */
    public Matrix(float[][] aArray) {
        iMatrix = new DenseMatrix64F(ConvertArray.toDouble(aArray));
    }

    /**
     * Creates new Matrix from provided 2D array. Setup of 2D array [rows][cols]
     * or [cols][rows] can be chosen.
     *
     * @param aArray
     * @param aIsXYmatrix
     *            if true then [cols][rows] setup expected. If false then
     *            [rows][cols] (default) is used.
     */
    public Matrix(double[][] aArray, boolean aIsXYmatrix) {
        this(aArray);
        if (aIsXYmatrix) {
            this.transpose();
        }
    }

    /**
     * Creates new Matrix from provided 2D array. Setup of 2D array [rows][cols]
     * or [cols][rows] can be chosen.
     *
     * @param aArray
     * @param aIsXYmatrix
     *            if true then [cols][rows] setup expected. If false then
     *            [rows][cols] (default) is used.
     */
    public Matrix(float[][] aArray, boolean aIsXYmatrix) {
        this(aArray);
        if (aIsXYmatrix) {
            this.transpose();
        }
    }

    /**
     * Creates row vector (Matrix 1xN) from given array or list of doubles
     * number
     *
     * @param aInput
     * @return
     */
    public static Matrix mkRowVector(double... aInput) {
        final DenseMatrix64F result = new DenseMatrix64F(1, aInput.length);
        result.setData(aInput);

        return new Matrix(result);
    }

    /**
     * Creates column vector (Matrix Nx1) from given array or list of doubles
     * number
     *
     * @param aInput
     * @return
     */
    public static Matrix mkColVector(double... aInput) {
        final DenseMatrix64F result = new DenseMatrix64F(aInput.length, 1);
        result.setData(aInput);

        return new Matrix(result);
    }

    /**
     * Returns copy of Matrix.
     *
     * @return
     */
    public Matrix copy() {
        return new Matrix(this);
    }

    /**
     * Return number of rows in matrix
     *
     * @return
     */
    public int numRows() {
        return iMatrix.numRows;
    }

    /**
     * Return number of columns in matrix
     *
     * @return
     */
    public int numCols() {
        return iMatrix.numCols;
    }

    /**
     * Return number of elements in matrix (cols * rows)
     *
     * @return
     */
    public int size() {
        return numRows() * numCols();
    }

    /**
     * Returns true if matrix has 1xN dimensions
     *
     * @return
     */
    public boolean isRowVector() {
        return this.size() > 0 && iMatrix.numRows == 1;
    }

    /**
     * Returns true if matrix has Nx1 dimensions
     *
     * @return
     */
    public boolean isColVector() {
        return this.size() > 0 && iMatrix.numCols == 1;
    }

    /**
     * Returns Matrix content as a 2D array of values [rows][cols]
     *
     * @param aMatrix
     *            input matrix
     * @return
     */
    public static double[][] getArrayYX(Matrix aMatrix) {
        final int r = aMatrix.iMatrix.numRows;
        final int c = aMatrix.iMatrix.numCols;
        final double[][] result = new double[r][c];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[ry][cx] = aMatrix.get(ry, cx);
            }
        }
        return result;
    }

    /**
     * Returns Matrix content as a 2D array of values [cols][rows]
     *
     * @param aMatrix
     *            input matrix
     * @return
     */
    public static double[][] getArrayXY(Matrix aMatrix) {
        final int r = aMatrix.iMatrix.numRows;
        final int c = aMatrix.iMatrix.numCols;
        final double[][] result = new double[c][r];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[cx][ry] = aMatrix.get(ry, cx);
            }
        }
        return result;
    }

    /**
     * Returns Matrix content as a 2D array of float values [rows][cols]
     *
     * @param aMatrix
     *            input matrix
     * @return
     */
    public static float[][] getArrayYXasFloats(Matrix aMatrix) {
        final int r = aMatrix.iMatrix.numRows;
        final int c = aMatrix.iMatrix.numCols;
        final float[][] result = new float[r][c];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[ry][cx] = (float) aMatrix.get(ry, cx);
            }
        }
        return result;
    }

    /**
     * Returns Matrix content as a 2D array of float values [cols][rows]
     *
     * @param aMatrix
     *            input matrix
     * @return
     */
    public static float[][] getArrayXYasFloats(Matrix aMatrix) {
        final int r = aMatrix.iMatrix.numRows;
        final int c = aMatrix.iMatrix.numCols;
        final float[][] result = new float[c][r];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[cx][ry] = (float) aMatrix.get(ry, cx);
            }
        }
        return result;
    }

    /**
     * Returns Matrix content as a 2D array of values [rows][cols]
     *
     * @return
     */
    public double[][] getArrayYX() {
        return getArrayYX(this);
    }

    /**
     * Returns Matrix content as a 2D array of values [cols][rows]
     *
     * @return
     */
    public double[][] getArrayXY() {
        return getArrayXY(this);
    }

    /**
     * Returns Matrix content as a 2D array of values [rows][cols]
     *
     * @return
     */
    public float[][] getArrayYXasFloats() {
        return getArrayYXasFloats(this);
    }

    /**
     * Returns Matrix content as a 2D array of values [cols][rows]
     *
     * @return
     */
    public float[][] getArrayXYasFloats() {
        return getArrayXYasFloats(this);
    }

    /**
     * Return array containing specified column
     */
    public double[] getArrayColumn(int aColumn) {
        final double[] column = new double[this.numRows()];
        for (int i = 0; i < this.numRows(); ++i) {
            column[i] = get(i, aColumn);
        }

        return column;
    }

    /**
     * Return array containing specified row
     */
    public double[] getArrayRow(int aRow) {
        final double[] row = new double[this.numCols()];
        for (int i = 0; i < this.numCols(); ++i) {
            row[i] = get(aRow, i);
        }

        return row;
    }

    /**
     * Return matrix containing specified column
     */
    public Matrix getColumn(int aColumn) {
        return mkColVector(getArrayColumn(aColumn));
    }

    /**
     * Return matrix containing specified row
     */
    public Matrix getRow(int aRow) {
        return mkRowVector(getArrayRow(aRow));
    }

    /**
     * Return matrix internal data. Should be used only for processing each element
     * of matrix since row/col structure is not guaranteed(!) and may change in future.
     */
    public double[] getData() {
        return this.iMatrix.data;
    }


    /**
     * Use MFunc function to process every element of Matrix. Output value of
     * aMf is used to set currently processed element.
     *
     * @param aMf
     * @return
     */
    public Matrix process(MFunc aMf) {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            iMatrix.data[i] = aMf.f(iMatrix.data[i], i / iMatrix.numCols, i % iMatrix.numCols);
        }
        return this;
    }

    /**
     * Use MFunc function to process every element of Matrix. Output value is
     * ignored and Matrix is not changed.
     *
     * @param aMf
     * @return
     */
    public Matrix processNoSet(MFunc aMf) {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            aMf.f(iMatrix.data[i], i / iMatrix.numCols, i % iMatrix.numCols);
        }
        return this;
    }

    /**
     * Multiplies two Matrices element-wise (same as .* operator in Matlab)
     *
     * @param aM
     * @return
     */
    public Matrix elementMult(Matrix aM) {
        CommonOps.elementMult(this.iMatrix, aM.iMatrix);
        return this;
    }

    /**
     * Multiplies two Matrices element-wise (same as .* operator in Matlab)
     *
     * @param aM
     * @return
     */
    public Matrix mult(Matrix aM) {
        final Matrix result = new Matrix(this.numRows(), aM.numCols());
        CommonOps.mult(this.iMatrix, aM.iMatrix, result.iMatrix);
        this.iMatrix = result.iMatrix;
        return this;
    }

    /**
     * Divides two Matrices element-wise (same as ./ operator in Matlab)
     *
     * @param aM
     * @return
     */
    public Matrix elementDiv(Matrix aM) {
        CommonOps.elementDiv(this.iMatrix, aM.iMatrix);
        return this;
    }

    /**
     * Adds two Matrices
     *
     * @param aM
     * @return
     */
    public Matrix add(Matrix aM) {
        CommonOps.add(this.iMatrix, aM.iMatrix, this.iMatrix);
        return this;
    }

    /**
     * Adds scalar to every element of Matrix
     *
     * @param aM
     * @return
     */
    public Matrix add(double aVal) {
        CommonOps.add(this.iMatrix, aVal);
        return this;
    }

    /**
     * Subtracts two Matrices
     *
     * @param aM
     * @return
     */
    public Matrix sub(Matrix aM) {
        CommonOps.sub(this.iMatrix, aM.iMatrix, this.iMatrix);
        return this;
    }
    
    /**
     * Adds scalar to every element of Matrix
     *
     * @param aM
     * @return
     */
    public Matrix sub(double aVal) {
        CommonOps.add(this.iMatrix, -aVal);
        return this;
    }

    /**
     * Scale (multiplies) each element of Matrix by given scalar
     *
     * @param aVal
     * @return
     */
    public Matrix scale(double aVal) {
        CommonOps.scale(aVal, this.iMatrix);
        return this;
    }

    /**
     * Return value of element at given row/col indexes (they starting from 0)
     *
     * @param aRow
     * @param aCol
     * @return
     */
    public double get(int aRow, int aCol) {
        return iMatrix.get(aRow, aCol);
    }

    /**
     * Sets value of element at given row/col indexes (they starting from 0)
     *
     * @param aRow
     * @param aCol
     * @param aVal
     *            value to be set
     * @return
     */
    public Matrix set(int aRow, int aCol, double aVal) {
        iMatrix.set(aRow, aCol, aVal);
        return this;
    }

    /**
     * Return value of element at given index (Matlab style - rows first)
     *
     * @param aIdx
     * @return
     */
    public double get(int aIdx) {
        return iMatrix.get(aIdx % iMatrix.numRows, aIdx / iMatrix.numRows);
    }

    /**
     * Sets value of element at given index (Matlab style)
     *
     * @param aIdx
     * @param aVal
     *            value to be set
     * @return
     */
    public Matrix set(int idx, double aVal) {
        iMatrix.set(idx % iMatrix.numRows, idx / iMatrix.numRows, aVal);
        return this;
    }

    /**
     * Transponse Matrix
     *
     * @return
     */
    public Matrix transpose() {
        CommonOps.transpose(this.iMatrix);
        return this;
    }

    /**
     * Fills every element of Matrix with given value
     *
     * @param aVal
     * @return
     */
    public Matrix fill(double aVal) {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            iMatrix.data[i] = aVal;
        }
        return this;
    }

    /**
     * Sets every element of Matrix with 1.0
     *
     * @return this
     */
    public Matrix ones() {
        fill(1.0);
        return this;
    }

    /**
     * Sets every element of Matrix with 0.0
     */
    public Matrix zeros() {
        fill(0.0);
        return this;
    }

    /**
     * Sets each element of matrix as its power of two
     */
    public Matrix pow2() {
        return pow(2);
    }

    /**
     * Sets each element of matrix as its power of aPower
     */
    public Matrix pow(int aPower) {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            iMatrix.data[i] = Math.pow(iMatrix.data[i], aPower);
        }
        return this;
    }

    /**
     * Sets each element of matrix as its square root
     */
    public Matrix sqrt() {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            iMatrix.data[i] = Math.sqrt(iMatrix.data[i]);
        }
        return this;
    }

    /**
     * Sets each element of matrix as its natural logarithm
     */
    public Matrix log() {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            iMatrix.data[i] = Math.log(iMatrix.data[i]);
        }
        return this;
    }

    /**
     * Sets each element of matrix to its inverse (1/x) value
     */
    public Matrix inv() {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            iMatrix.data[i] = 1 / iMatrix.data[i];
        }
        return this;
    }

    /**
     * Sets each element of Matrix as its negative value (multiplies by -1)
     */
    public Matrix negative() {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            iMatrix.data[i] = -iMatrix.data[i];
        }
        return this;
    }

    /**
     * Calculates sum of each element in the Matrix
     * @return
     */
    public double sum() {
        return CommonOps.elementSum(this.iMatrix);
    }

    /**
     * Normalize elements of Matrix to be in range (-1, 1)
     */
    public Matrix normalize() {
        final int len = iMatrix.data.length;
        double max = 0.0;
        for (int i = 0; i < len; ++i) {
            final double absValue = Math.abs(iMatrix.data[i]);
            if (max < absValue) {
                max = absValue;
            }
        }
        if (max > 0) {
            for (int i = 0; i < len; ++i) {
                iMatrix.data[i] = iMatrix.data[i] / max;
            }
        }
        return this;
    }

    /**
     * Normalize elements of Matrix to be in range (0, 1)
     */
    public Matrix normalizeInRange0to1() {
        final int len = iMatrix.data.length;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < len; ++i) {
            final double val = iMatrix.data[i];
            if (max < val) {
                max = val;
            }
            if (min > val) {
                min = val;
            }
        }
        // Normalize with found values
        if (max != min) {
            for (int i = 0; i < len; ++i) {
                iMatrix.data[i] = (iMatrix.data[i] - min) / (max - min);
            }
        }
        return this;
    }

    /**
     * Inserts aMatrix into position of (aRow, aCol). It must have correct size to
     * fit original matrix
     * @param aMatrix
     * @param aRow
     * @param aCol
     * @return
     */
    public Matrix insert(Matrix aMatrix, int aRow, int aCol) {
        CommonOps.insert(aMatrix.iMatrix, this.iMatrix, aRow, aCol);
        return this;
    }

    /**
     * Resize matrix by choosing elements with step (aStepRow, aStepCol)
     * starting from (aStartRow, aStartCol).
     *
     * @param aStartRow
     * @param aStartCol
     * @param aStepRow
     * @param aStepCol
     * @return
     */
    public Matrix resize(int aStartRow, int aStartCol, int aStepRow, int aStepCol) {
        if (aStartRow == 0 && aStartCol == 0 && aStepRow <= 1 && aStepCol <= 1) {
            return this;
        }

        final int cols = iMatrix.numCols;
        final int rows = iMatrix.numRows;

        final int newCols = (cols + 1 - aStartCol) / aStepCol;
        final int newRows = (rows + 1 - aStartRow) / aStepRow;

        final DenseMatrix64F result = new DenseMatrix64F(newRows, newCols);
        for (int r = aStartRow, rn = 0; r < rows; r += aStepRow, ++rn) {
            for (int c = aStartCol, cn = 0; c < cols; c += aStepCol, ++cn) {
                result.set(rn, cn, iMatrix.get(r, c));
            }
        }
        iMatrix = result;

        return this;
    }

    /**
     * Inserts given row vector into matrix at given row number
     * @param aRowMatrix
     * @param aRowNum
     * @return
     */
    public Matrix insertRow(Matrix aRowMatrix, int aRowNum) {
        if (aRowMatrix.numCols() != iMatrix.numCols) {
            throw new IllegalArgumentException("Dimensions of row vector must match matrix");
        }
        CommonOps.insert(aRowMatrix.iMatrix, iMatrix, aRowNum, 0);
        return this;
    }

    /**
     * Inserts given column vector into matrix at given coumn number
     * @param aColMatrix
     * @param aColNum
     * @return
     */
    public Matrix insertCol(Matrix aColMatrix, int aColNum) {
        if (aColMatrix.numRows() != iMatrix.numRows) {
            throw new IllegalArgumentException("Dimensions of row vector must match matrix");
        }
        CommonOps.insert(aColMatrix.iMatrix, iMatrix, 0, aColNum);
        return this;
    }

    /**
     * Compare matrices with given precision (<aEpsilon)
     * @param aMatrix
     * @param aEpsilon
     * @return
     */
    public boolean compare(Matrix aMatrix, double aEpsilon) {
        final int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            if (Math.abs(this.iMatrix.data[i] - aMatrix.iMatrix.data[i]) > aEpsilon) {
                // DEBUG: Uncomment below to see different element
                // =====================================================
                //                 System.out.println("["+ i/iMatrix.numCols +"][" + i%iMatrix.numCols + "] " +
                //                                    this.iMatrix.data[i] + " vs " + aMatrix.iMatrix.data[i] +
                //                                    " Diff: " + Math.abs(this.iMatrix.data[i] - aMatrix.iMatrix.data[i]));
                // =====================================================

                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return this.iMatrix.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final Matrix cmp = (Matrix) obj;
        if (cmp.numCols() != this.numCols() || cmp.numRows() != this.numRows()) {
            return false;
        }

        return compare(cmp, 0.0);
    }
    
    @Override
    public int hashCode() {
        return iMatrix.hashCode();
    }
}
