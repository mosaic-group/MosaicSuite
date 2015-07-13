package mosaic.math;
import mosaic.plugins.utils.Convert;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Wrapper for ejml library.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
public class Matrix {
	private DenseMatrix64F iMatrix;
	
	private Matrix(DenseMatrix64F aDM) {iMatrix = aDM;}
	public Matrix(int aRows, int aCols) {
		iMatrix = new DenseMatrix64F(aRows, aCols);
	}
	public Matrix(Matrix aM) {
		iMatrix = new DenseMatrix64F(aM.iMatrix);
	}
	//[y][x] or [r][c]
	public Matrix(double[][] aArray) {
		iMatrix = new DenseMatrix64F(aArray);
	}
   public Matrix(float[][] aArray) {
        iMatrix = new DenseMatrix64F(Convert.toDouble(aArray));
    }
	public Matrix(double[][] aArray, boolean aIsXYmatrix) {
		this(aArray);
		if (aIsXYmatrix) {
			this.transpose();
		}
	}
   public Matrix(float[][] aArray, boolean aIsXYmatrix) {
        this(aArray);
        if (aIsXYmatrix) {
            this.transpose();
        }
    }
	// Factory --------------
	public static Matrix mkRowVector(double... aInput) {
		DenseMatrix64F result = new DenseMatrix64F(1, aInput.length);
		result.setData(aInput);
		
		return new Matrix(result); 
	}
	
	public static Matrix mkColVector(double... aInput) {
		DenseMatrix64F result = new DenseMatrix64F(aInput.length, 1);
		result.setData(aInput);
		
		return new Matrix(result); 
	}
	
	public Matrix copy() {return new Matrix(this);}
	
	public static Matrix[] meshgrid(Matrix aVector1, Matrix aVector2) {
		CommonOps.transpose(aVector2.iMatrix);
		int r = aVector2.iMatrix.numRows; int c = aVector1.iMatrix.numCols;
		DenseMatrix64F m1 = new DenseMatrix64F(r, c);
		DenseMatrix64F m2 = new DenseMatrix64F(r, c);

		for (int i = 0; i < r; ++i) CommonOps.insert(aVector1.iMatrix, m1, i, 0);
		for (int i = 0; i < c; ++i) CommonOps.insert(aVector2.iMatrix, m2, 0, i);
		
		return new Matrix[] {new Matrix(m1), new Matrix(m2)};
	}
    //[y][x] or [r][c]
    public static double[][] getArrayYX(Matrix aMatrix) {
        int r = aMatrix.iMatrix.numRows; int c = aMatrix.iMatrix.numCols;
        double [][] result = new double[r][c];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[ry][cx] = aMatrix.get(ry, cx);
            }
        }
        return result;
    }
    //[x][y] or [c][r]
    public static double[][] getArrayXY(Matrix aMatrix) {
        int r = aMatrix.iMatrix.numRows; int c = aMatrix.iMatrix.numCols;
        double [][] result = new double[c][r];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[cx][ry] = aMatrix.get(ry, cx);
            }
        }
        return result;
    }
    //[y][x] or [r][c]
    public static float[][] getArrayYXasFloats(Matrix aMatrix) {
        int r = aMatrix.iMatrix.numRows; int c = aMatrix.iMatrix.numCols;
        float [][] result = new float[r][c];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[ry][cx] = (float)aMatrix.get(ry, cx);
            }
        }
        return result;
    }
    //[x][y] or [c][r]
    public static float[][] getArrayXYasFloats(Matrix aMatrix) {
        int r = aMatrix.iMatrix.numRows; int c = aMatrix.iMatrix.numCols;
        float [][] result = new float[c][r];
        for (int ry = 0; ry < r; ++ry) {
            for (int cx = 0; cx < c; ++cx) {
                result[cx][ry] = (float)aMatrix.get(ry, cx);
            }
        }
        return result;
    }
	// -------------------------
	public Matrix process(MFunc aMf) {
		int len = iMatrix.data.length;
		for (int i = 0; i < len; ++i) iMatrix.data[i] = aMf.f(iMatrix.data[i], i/iMatrix.numCols, i % iMatrix.numCols);
		return this;
	}
	public Matrix processNoSet(MFunc aMf) {
		int len = iMatrix.data.length;
		for (int i = 0; i < len; ++i) aMf.f(iMatrix.data[i], i/iMatrix.numCols, i % iMatrix.numRows);
		return this;
	}
	
	public Matrix elementMult(Matrix aM) {
		CommonOps.elementMult(this.iMatrix, aM.iMatrix);
		return this;
	}
	
	public Matrix elementDiv(Matrix aM) {
		CommonOps.elementDiv(this.iMatrix, aM.iMatrix);
		return this;
	}
	
	public Matrix add(Matrix aM) {
		CommonOps.add(this.iMatrix, aM.iMatrix, this.iMatrix);
		return this;
	}
	public Matrix add(double aVal) {
		CommonOps.add(this.iMatrix, aVal);
		return this;
	}
	public Matrix sub(Matrix aM) {
		CommonOps.sub(this.iMatrix, aM.iMatrix, this.iMatrix);
		return this;
	}
	public Matrix scale(double aVal) {
		CommonOps.scale(aVal, this.iMatrix);	
		return this;
	}
	public double get(int r, int c) {
		return iMatrix.get(r, c);
	}
	public Matrix set(int r, int c, double aVal) {
		iMatrix.set(r, c, aVal);
		return this;
	}
	public double get(int idx) {
        return iMatrix.get(idx % iMatrix.numRows, idx / iMatrix.numRows);
    }
    public Matrix set(int idx, double aVal) {
        iMatrix.set(idx % iMatrix.numRows, idx / iMatrix.numRows, aVal);
        return this;
    }
    
	public int numRows() {
		return iMatrix.numRows;
	}
	public int numCols() {
		return iMatrix.numCols;
	}
	public int size() {return numRows() * numCols();}
	public Matrix fill(double aVal) {
		int len = iMatrix.data.length;
		for (int i = 0; i < len; ++i) iMatrix.data[i] = aVal;
		return this;
	}
	public Matrix transpose() {
		CommonOps.transpose(this.iMatrix);
		return this;
	}
	public Matrix ones() {
		fill(1.0);
		return this;
	}
	public Matrix zeros() {
		fill(0.0);
		return this;
	}
	// helpers
	public Matrix pow2() {
		int len = iMatrix.data.length;
		for (int i = 0; i < len; ++i) iMatrix.data[i] = Math.pow(iMatrix.data[i],2);
		return this;
	}
	public Matrix sqrt() {
		int len = iMatrix.data.length;
		for (int i = 0; i < len; ++i) iMatrix.data[i] = Math.sqrt(iMatrix.data[i]);
		return this;
	}
	public Matrix log() {
		int len = iMatrix.data.length;
		for (int i = 0; i < len; ++i) iMatrix.data[i] = Math.log(iMatrix.data[i]);
		return this;
	}
   public Matrix inv() {
        int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) iMatrix.data[i] = 1/iMatrix.data[i];
        return this;
    }
   public Matrix normalize() {
       int len = iMatrix.data.length;
       double max = 0.0;
       for (int i = 0; i < len; ++i) {
           double absValue = Math.abs(iMatrix.data[i]);
           if (max < absValue) max = absValue;
       }
       if (max > 0) {
           for (int i = 0; i < len; ++i) iMatrix.data[i] = iMatrix.data[i]/max;
       }
       return this;
   }
   
   public Matrix normalizeInRange0to1() {
       int len = iMatrix.data.length;
       double min = Double.MAX_VALUE;
       double max = Double.MIN_VALUE;
       for (int i = 0; i < len; ++i) {
           double val = iMatrix.data[i];
           if (max < val) max = val;
           if (min > val) min = val;
       }
       // Normalize with found values
       if (max != min) {
           for (int i = 0; i < len; ++i) iMatrix.data[i] = (iMatrix.data[i]  - min) / (max - min);;
       }
       return this;
   }
   
	public double[][] getArrayYX() {
		return getArrayYX(this);
	}
	public double[][] getArrayXY() {
		return getArrayXY(this);
	}
   public float[][] getArrayYXasFloats() {
        return getArrayYXasFloats(this);
    }
    public float[][] getArrayXYasFloats() {
        return getArrayXYasFloats(this);
    }
	public Matrix insert(Matrix aMatrix, int aRow, int aCol) {
		CommonOps.insert(aMatrix.iMatrix, this.iMatrix, aRow, aCol);
		return this;
	}
	
	public double sum() {
		return CommonOps.elementSum(this.iMatrix);
	}
	public Matrix resize(int aStartRow, int aStartCol, int aStepRow, int aStepCol) {
	    if (aStartRow == 0 && aStartCol == 0 && aStepRow == 1 && aStepCol == 1) return this;
	    
	    int cols = iMatrix.numCols;
	    int rows = iMatrix.numRows;
	    
	    int newCols = (cols + 1 - aStartCol) / aStepCol;
	    int newRows = (rows + 1 - aStartRow) / aStepRow;
	    
	    DenseMatrix64F result = new DenseMatrix64F(newRows, newCols);
	    for (int r = aStartRow, rn = 0; r < rows; r += aStepRow, ++rn) {
	        for (int c = aStartCol, cn = 0; c < cols; c += aStepCol, ++cn) {
	            result.set(rn, cn, iMatrix.get(r, c));
	        }
	    }
	    iMatrix = result;
	    
	    return this;
	}
	
	public boolean isRowVector() {
	    return iMatrix.numRows == 1;
	}
	
	public boolean isColVector() {
	        return iMatrix.numCols == 1;
	}
	
	public Matrix insertRow(Matrix aRowMatrix, int aRowNum) {
	    CommonOps.insert(aRowMatrix.iMatrix, iMatrix, aRowNum, 0);
	    return this;
	}
	
   public Matrix insertCol(Matrix aColMatrix, int aColNum) {
        CommonOps.insert(aColMatrix.iMatrix, iMatrix, 0, aColNum);
        return this;
    }
	
	@Override
	public
	String toString() {
		return this.iMatrix.toString();
	}
	
	// =================================
	public double[] getData() {
	    return this.iMatrix.data;
	}
	
    public boolean compare(Matrix aMatrix, double aEpsilon) {
        int len = iMatrix.data.length;
        for (int i = 0; i < len; ++i) {
            if (Math.abs(this.iMatrix.data[i] - aMatrix.iMatrix.data[i]) > aEpsilon) {
                System.out.println("["+ i/iMatrix.numCols +"][" + i%iMatrix.numCols + "] " + this.iMatrix.data[i] + " vs " + aMatrix.iMatrix.data[i] + " Diff: " + Math.abs(this.iMatrix.data[i] - aMatrix.iMatrix.data[i]));
                return false;
            }
        }
        return true;
    }
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == this) return true;
	    if (obj == null || obj.getClass() != this.getClass()) return false;
	    Matrix cmp = (Matrix)obj;
	    if (cmp.numCols() != this.numCols() || cmp.numRows() != this.numRows()) return false;
	    
        return compare(cmp, 0.0);
	}
}
