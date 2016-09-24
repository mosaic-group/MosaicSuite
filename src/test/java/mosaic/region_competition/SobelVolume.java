package mosaic.region_competition;

import volume.Kernel;
import volume.Kernel2D;
import volume.Kernel3D;
import volume.VolumeFloat;

/**
Class responsible for calculating Sobel filter for 2D/3D images.
 */
public class SobelVolume extends VolumeFloat
{
    private static final long serialVersionUID = 1L;

    public SobelVolume(int width, int height, int depth) {
        super(width, height, depth, 1.0, 1.0, 1.0);
    }
    
    public void sobel2D(VolumeFloat aVolume, Kernel2D aKernel) {
        int depthoffset = InitParams(aVolume, aKernel);
        if (depthoffset < 0) return;
        
        // Alias things for easier use
        float[][][] volume = aVolume.v;
        double[][] kernel = aKernel.k;
        int kernelWidth = aKernel.halfwidth;
        
        // Image still can have depth for 2D. In such case each layer is treated as a seperate 2D image.
        for (int z = 0; z < depth; z++) {
            int vz = z + depthoffset;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (valid(x, y)) {
                        double dx = 0;
                        double dy = 0;
                        for (int l = -kernelWidth; l <= kernelWidth; l++) {
                            int kx = l + kernelWidth;
                            int vy = y + l;
                            for (int k = -kernelWidth; k <= kernelWidth; k++) {
                                int ky = k + kernelWidth;
                                int vx = x + k;
                                float val = volume[vz][vy][vx];
                                
                                dx += val * kernel[kx][ky];
                                dy += val * kernel[ky][kx];
                            }
                        }
                        v[z][y][x] = (float) Math.sqrt(dx*dx + dy*dy);
                    }
                    else {
                        v[z][y][x] = 0;
                    }
                }
            }
        }
    }

    public void sobel3D(VolumeFloat aVolume, Kernel3D aKernel) {
        int depthoffset = InitParams(aVolume, aKernel);
        if (depthoffset < 0) return;
        
        // Alias things for easier use
        float[][][] volume = aVolume.v;
        double[][][] kernel = aKernel.k;
        int kernelWidth = aKernel.halfwidth;
        
        for (int z = 0; z < depth; ++z) {
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    if (valid(x, y, z)) {
                        double dx = 0;
                        double dy = 0;
                        double dz = 0;
                        for (int m = -kernelWidth; m <= kernelWidth; ++m) {
                            int kx = m + kernelWidth;
                            int vz = z + depthoffset+m;
                            for (int l = -kernelWidth; l <= kernelWidth; ++l) {
                                int ky = l + kernelWidth;
                                int vy = y + l;
                                for (int k = -kernelWidth; k <= kernelWidth; ++k) {
                                    int kz = k + kernelWidth;
                                    int vx = x + k;
                                    float val = volume[vz][vy][vx];
                                    
                                    dx += val * kernel[kx][ky][kz];
                                    dy += val * kernel[kx][kz][ky];
                                    dz += val * kernel[kz][ky][kx];
                                }
                            }
                        }
                        v[z][y][x] = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
                    }
                    else {
                        v[z][y][x] = 0;
                    }
                }        
            }
        }
    }
    
    /**
     * Method from VolumeFloat - unfortunately it is private so it needed to be copied here
     * @param aVolume - input volume for convolution
     * @param aKernel - input kernel for convolution
     * @return
     */
    private int InitParams(VolumeFloat aVolume, Kernel aKernel) {   
        if (width < aVolume.getWidth() || height < aVolume.getHeight() || depth > aVolume.getDepth()) {   
            throw new RuntimeException("SobelVolume: convolution volume wrong size.");
        }
        
        if (aVolume.getEdge() > edge) edge = aVolume.getEdge();
        if (aKernel.halfwidth > edge) edge = aKernel.halfwidth;
         
        return (depth < aVolume.getDepth()) ? (aVolume.getDepth() - depth) / 2 : 0;
    }
}
