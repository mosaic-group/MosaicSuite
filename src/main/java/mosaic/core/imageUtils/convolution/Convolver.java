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
    
    public Convolver add(Convolver aConvolver) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] += aConvolver.iData[z][y][x];
                }
            }
        }
        return this;
    }
    
    public Convolver sub(Convolver aConvolver) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] -= aConvolver.iData[z][y][x];
                }
            }
        }
        return this;
    }
    
    public Convolver mul(Convolver aConvolver) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] *= aConvolver.iData[z][y][x];
                }
            }
        }
        return this;
    }
    
    public Convolver div(Convolver aConvolver) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] /= aConvolver.iData[z][y][x];
                }
            }
        }
        return this;
    }
    
    public Convolver mul(double aConst) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] *= aConst;
                }
            }
        }
        return this;
    }
    
    public Convolver sqrt() {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] = Math.sqrt(iData[z][y][x]);
                }
            }
        }
        return this;
    }
    
    public Convolver pow(double aPower) {
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                        iData[z][y][x] = Math.pow(iData[z][y][x], aPower);
                }
            }
        }
        return this;
    }
    
    public Convolver div(double aConst) {
        mul(1.0/aConst);
        return this;
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
    
    
    public void sobel2D(Convolver aSrcConv) {
        /*
         * Faster implementation of volume-based approach:
         * 
         * VolumeFloat v = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
         * v.load(img.getImageStack(), 0);
         * VolumeFloat dx = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
         * VolumeFloat dy = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
         * Kernel2D k = new Sobel();
         * dx.convolvex(v, k);
         * dy.convolvey(v, k);
         * dx.mul(dx);
         * dy.mul(dy);
         * dx.add(dy);
         * dx.sqrt();
         * float[][][] volumeImg = dx.getVolume();
         * ImageStack imageStack = dx.getImageStack();
         */
        Kernel2D aKernel = new Kernel2D() {{
            int width = 3;
            iHalfWidth = width / 2;
            k = new double[3][3];
            /* Sobel 2D:
                  -1  -2   -1
                  0   0     0
                  1   2     1
            */
            k[0][0] = -1.0/9; k[0][1] = 0; k[0][2] = 1.0/9;
            k[1][0] = -2.0/9; k[1][1] = 0; k[1][2] = 2.0/9;
            k[2][0] = -1.0/9; k[2][1] = 0; k[2][2] = 1.0/9;
        }};
        
        // Alias things for easier use
        double[][][] volume = aSrcConv.iData;
        double[][] kernel = aKernel.k;
        int kernelWidth = aKernel.iHalfWidth;
        
        // Image still can have depth for 2D. In such case each layer is treated as a seperate 2D image.
        for (int z = 0; z < iDepth; z++) {
            int vz = z + 0;
            for (int y = 0; y < iHeight; y++) {
                for (int x = 0; x < iWidth; x++) {
                    if (x >= kernelWidth && y >= kernelWidth && x < iWidth - kernelWidth && y < iHeight - kernelWidth) {
                        double dx = 0;
                        double dy = 0;
                        for (int l = -kernelWidth; l <= kernelWidth; l++) {
                            int kx = l + kernelWidth;
                            int vy = y + l;
                            for (int k = -kernelWidth; k <= kernelWidth; k++) {
                                int ky = k + kernelWidth;
                                int vx = x + k;
                                double val = volume[vz][vy][vx];
                                
                                dx += val * kernel[kx][ky];
                                dy += val * kernel[ky][kx];
                            }
                        }
                        iData[z][y][x] = (float) Math.sqrt(dx*dx + dy*dy);
                    }
                    else {
                        iData[z][y][x] = 0;
                    }
                }
            }
        }
    }
    
    public void sobel3D(Convolver aSrcConv) {
//      Implementation using original VolumeFloat does not work since valid(x,y) is used instead valid(x,y,z):
//      
//      VolumeFloat dx = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//      VolumeFloat dy = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//      VolumeFloat dz = new VolumeFloat(img.getWidth(), img.getHeight(), img.getNSlices());
//      Kernel3D k = new Sobel3D();
//      dx.convolvex(v, k);
//      dy.convolvey(v, k);
//      dz.convolvez(v, k);
//      dx.mul(dx);
//      dy.mul(dy);
//      dz.mul(dz);
//      dx.add(dy);
//      dx.add(dz);
//      dx.sqrt();
//        
        Kernel3D aKernel = new Kernel3D() {{
            int width = 3;
            iHalfWidth = width / 2;
            k = new double[3][3][3];
            /*  Sobel 3D
                  -2  0   2     -3  0   3    -2   0   2
                  -3  0   3     -6  0   6    -3   0   3
                  -2  0   2     -3  0   3    -2   0   2
            */
            k[0][0][0] = -2.0/27; k[0][0][1] = 0; k[0][0][2] = 2.0/27;
            k[0][1][0] = -3.0/27; k[0][1][1] = 0; k[0][1][2] = 3.0/27;
            k[0][2][0] = -2.0/27; k[0][2][1] = 0; k[0][2][2] = 2.0/27;
            k[1][0][0] = -3.0/27; k[1][0][1] = 0; k[1][0][2] = 3.0/27;
            k[1][1][0] = -6.0/27; k[1][1][1] = 0; k[1][1][2] = 6.0/27;
            k[1][2][0] = -3.0/27; k[1][2][1] = 0; k[1][2][2] = 3.0/27;
            k[2][0][0] = -2.0/27; k[2][0][1] = 0; k[2][0][2] = 2.0/27;
            k[2][1][0] = -3.0/27; k[2][1][1] = 0; k[2][1][2] = 3.0/27;
            k[2][2][0] = -2.0/27; k[2][2][1] = 0; k[2][2][2] = 2.0/27;
        }};
        
        // Alias things for easier use
        double[][][] volume = aSrcConv.iData;
        double[][][] kernel = aKernel.k;
        int kernelWidth = aKernel.iHalfWidth;
        
        for (int z = 0; z < iDepth; ++z) {
            for (int y = 0; y < iHeight; ++y) {
                for (int x = 0; x < iWidth; ++x) {
                    if (x >= kernelWidth && y >= kernelWidth && z >= kernelWidth && x < iWidth - kernelWidth && y < iHeight - kernelWidth && z < iDepth - kernelWidth) {
                        double dx = 0;
                        double dy = 0;
                        double dz = 0;
                        for (int m = -kernelWidth; m <= kernelWidth; ++m) {
                            int kx = m + kernelWidth;
                            int vz = z + 0+m;
                            for (int l = -kernelWidth; l <= kernelWidth; ++l) {
                                int ky = l + kernelWidth;
                                int vy = y + l;
                                for (int k = -kernelWidth; k <= kernelWidth; ++k) {
                                    int kz = k + kernelWidth;
                                    int vx = x + k;
                                    double val = volume[vz][vy][vx];
                                    
                                    dx += val * kernel[kx][ky][kz];
                                    dy += val * kernel[kx][kz][ky];
                                    dz += val * kernel[kz][ky][kx];
                                }
                            }
                        }
                        iData[z][y][x] = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
                    }
                    else {
                        iData[z][y][x] = 0;
                    }
                }        
            }
        }
    }
}
