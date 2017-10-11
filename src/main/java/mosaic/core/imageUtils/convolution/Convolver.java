package mosaic.core.imageUtils.convolution;

import ij.ImageStack;
import ij.process.FloatProcessor;

public class Convolver {
    private double[][][] iData;
    
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
        iWidth = aData[0][0].length;
        iHeight = aData[0].length;
        iDepth = aData.length;
        iData = new double[iDepth][iHeight][iWidth];
        copy(aData);
    }
    
    public Convolver(double[][] aData) {
        iWidth = aData[0].length;
        iHeight = aData.length;
        iDepth = 1;
        iData = new double[iDepth][iHeight][iWidth];
        double[][][] temp = new double[][][] {aData};
        copy(temp);
    }

    public Convolver(Convolver aConvolver) {
        this(aConvolver.getWidth(), aConvolver.getHeight(), aConvolver.getDepth());
        copy(aConvolver.iData);
    }
    
    private void copy(double[][][] aData) {
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
    
    public void load(ImageStack aStack) {
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
    
    public void convolvex(Convolver aSrcConv, Kernel1D aKernel) {
        int hw = aKernel.halfwidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int k = -hw; k <= hw; ++k) {
                        int xc = x + k; if (xc < 0) xc = 0; else if (xc >= iWidth) xc = iWidth - 1;
                        iData[z][y][x] += aSrcConv.iData[z][y][xc]*aKernel.k[k+hw];
                    }
                }
            }
        }
    }
    
    public void convolvey(Convolver aSrcConv, Kernel1D aKernel) {
        int hw = aKernel.halfwidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int k = -hw; k <= hw; ++k) {
                        int yc = y + k; if (yc < 0) yc = 0; else if (yc >= iHeight) yc = iHeight - 1;
                        iData[z][y][x] += aSrcConv.iData[z][yc][x]*aKernel.k[k+hw];
                    }
                }
            }
        }
    }
    
    public void convolvez(Convolver aSrcConv, Kernel1D aKernel) {
        int hw = aKernel.halfwidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int k = -hw; k <= hw; ++k) {
                        int zc = z + k; if (zc < 0) zc = 0; else if (zc >= iDepth) zc = iDepth - 1;
                        iData[z][y][x] += aSrcConv.iData[zc][y][x]*aKernel.k[k+hw];
                    }
                }
            }
        }
    }
    
    public void convolvexy(Convolver aSrcConv, Kernel2D aKernel) {
        int hw = aKernel.halfwidth;
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    iData[z][y][x] = 0;
                    for (int m = -hw; m <= hw; ++m) {
                        int yc = y + m; if (yc < 0) yc = 0; else if (yc >= iHeight) yc = iHeight - 1;
                        for (int k = -hw; k <= hw; ++k) {
                            int xc = x + k; if (xc < 0) xc = 0; else if (xc >= iWidth) xc = iWidth - 1;
                            iData[z][y][x] += aSrcConv.iData[z][yc][xc]*aKernel.k[m+hw][k+hw];
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
