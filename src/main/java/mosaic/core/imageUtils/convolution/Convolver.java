package mosaic.core.imageUtils.convolution;

import ij.ImageStack;
import ij.process.FloatProcessor;

/**
 * Convolver class for 2D/3D data
 * This is very initial version handling only kernels with NxN size where 
 * N is odd (1, 3, 5, ...). 
 * It uses extended edge handling where pixels outside boundaries have values same
 * as pixels on edge.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 * TODO: extend convolution to any size of kernel 
 */
public class Convolver {
    private double[][][] iData; // format [z][y][x]
    
    private int iDepth;
    private int iHeight;
    private int iWidth;

    public Convolver(int aWidth, int aHeight, int aDepth) {
        iWidth = aWidth;
        iHeight = aHeight;
        iDepth = aDepth;
        iData = new double[iDepth][iHeight][iWidth];
    }

    public Convolver(double[][][] aData) {
        this(aData[0][0].length, aData[0].length, aData.length);
        copyData(aData);
    }
    
    public Convolver(double[][] aData) {
        this(new double[][][] {aData});
    }

    public Convolver(Convolver aConvolver) {
        this(aConvolver.iData);
    }
    
    private void copyData(double[][][] aData) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = aData[z][y][x];
                }
            }
        }
    }
    
    public ImageStack getImageStack() {
        ImageStack is = new ImageStack(iWidth, iHeight);
        for (int z = 0; z < iDepth; ++z) {
            FloatProcessor fp = new FloatProcessor(iWidth, iHeight);
            float[] p = (float[]) fp.getPixels();
            int offset = 0;
            for (int y= 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    p[offset++] = (float) iData[z][y][x];
                }
            }
            is.addSlice("slice" + z, fp);
        }
        return is;
    }
    
    public void initFromImageStack(ImageStack aStack) {
        for (int z = 0; z < aStack.getSize(); ++z) {
            float[] p = (float[]) aStack.getPixels(z + 1);
            int offset = 0;
            for (int y= 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = p[offset++];
                }
            }
        }  
    }
    
    public void sub(Convolver aConvolver) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] -= aConvolver.iData[z][y][x];
                }
            }
        }
    }
    
    public void mul(double aConst) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] *= aConst;
                }
            }
        }
    }
    
    public void div(double aConst) {
        mul(1.0/aConst);
    }
    
    public void x1D(Kernel1D aKernel) {
        x1D(new Convolver(this), aKernel);
    }

    public void y1D(Kernel1D aKernel) {
        y1D(new Convolver(this), aKernel);
    }

    public void z1D(Kernel1D aKernel) {
        z1D(new Convolver(this), aKernel);
    }
    
    public void xy2D(Kernel2D aKernel) {
        xy2D(new Convolver(this), aKernel);
    }
    
    public void xyz3D(Kernel3D aKernel) {
        xyz3D(new Convolver(this), aKernel);
    }

    public void x1D(Convolver aSrcConv, Kernel1D aKernel) {
        int hw = aKernel.iHalfWidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int k = -hw; k <= hw; ++k) {
                        int xc = x + k; if (xc < 0) xc = 0; else if (xc >= iWidth) xc = iWidth - 1;
                        iData[z][y][x] += aSrcConv.iData[z][y][xc] * aKernel.k[k+hw];
                    }
                }
            }
        }
    }
    
    public void y1D(Convolver aSrcConv, Kernel1D aKernel) {
        int hw = aKernel.iHalfWidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int k = -hw; k <= hw; ++k) {
                        int yc = y + k; if (yc < 0) yc = 0; else if (yc >= iHeight) yc = iHeight - 1;
                        iData[z][y][x] += aSrcConv.iData[z][yc][x] * aKernel.k[k+hw];
                    }
                }
            }
        }
    }
    
    public void z1D(Convolver aSrcConv, Kernel1D aKernel) {
        int hw = aKernel.iHalfWidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int k = -hw; k <= hw; ++k) {
                        int zc = z + k; if (zc < 0) zc = 0; else if (zc >= iDepth) zc = iDepth - 1;
                        iData[z][y][x] += aSrcConv.iData[zc][y][x] * aKernel.k[k+hw];
                    }
                }
            }
        }
    }
    
    public void xy2D(Convolver aSrcConv, Kernel2D aKernel) {
        int hw = aKernel.iHalfWidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int m = -hw; m <= hw; ++m) {
                        int yc = y + m; if (yc < 0) yc = 0; else if (yc >= iHeight) yc = iHeight - 1;
                        for (int k = -hw; k <= hw; ++k) {
                            int xc = x + k; if (xc < 0) xc = 0; else if (xc >= iWidth) xc = iWidth - 1;
                            iData[z][y][x] += aSrcConv.iData[z][yc][xc] * aKernel.k[m+hw][k+hw];
                        }
                    }
                }
            }
        }
    }
    
    public void xyz3D(Convolver aSrcConv, Kernel3D aKernel) {
        int hw = aKernel.iHalfWidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int n = -hw; n <= hw; ++n) {
                        int zc = z + n; if (zc < 0) zc = 0; else if (zc >= iDepth) zc = iDepth - 1;
                        for (int m = -hw; m <= hw; ++m) {
                            int yc = y + m; if (yc < 0) yc = 0; else if (yc >= iHeight) yc = iHeight - 1;
                            for (int k = -hw; k <= hw; ++k) {
                                int xc = x + k; if (xc < 0) xc = 0; else if (xc >= iWidth) xc = iWidth - 1;
                                iData[z][y][x] += aSrcConv.iData[zc][yc][xc] * aKernel.k[n+hw][m+hw][k+hw];
                            }
                        }
                    }
                }
            }
        }
    }

    public int getWidth() { return iWidth; }
    public int getHeight() { return iHeight; }
    public int getDepth() { return iDepth; }
    public double[][][] getData() { return iData; }
}
