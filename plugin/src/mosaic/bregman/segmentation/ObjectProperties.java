package mosaic.bregman.segmentation;


import Skeletonize3D_.Skeletonize3D_;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import mosaic.core.psf.psf;
import mosaic.utils.ArrayOps;
import mosaic.utils.Debug;
import mosaic.utils.ArrayOps.MinMax;
import net.imglib2.type.numeric.real.DoubleType;


class ObjectProperties implements Runnable {
    // Input parameters
    private final double[][][] iImage;
    private final Region iRegion;
    private final short[][][] iSingleRegion;
    private  psf<DoubleType> iPsf;
    private final double iBetaMleOut;
    private final double iBetaMleIn;

    private final int nx, ny, nz;// size of full oversampled work zone
    private final int osxy, osz; // oversampling
    private int sx, sy, sz;// size for object
    private int cx, cy, cz;// offset of patch
    
    
    ObjectProperties(double[][][] aImage, Region aRegion, short[][][] aSingleRegion, psf<DoubleType> aPsf, double aBetaMleOut, double aBetaMleIn, int nx, int ny,  int nz, int osxy, int osz) {
        iImage = aImage;
        iRegion = aRegion;
        iSingleRegion = aSingleRegion;
        iPsf = aPsf;
        iBetaMleOut = aBetaMleOut;
        iBetaMleIn = aBetaMleIn;
        
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.osxy = osxy;
        this.osz = osz;
        
        calculatePatchGeometry(aRegion);
    }

    @Override
    public void run() {
        double[][][] patch = fillPatch(iImage);
        MinMax<Double> patchMinMax = ArrayOps.normalize(patch);
        double[][][] mask = fillMask(iRegion);
        double cin = estimateIntensity(mask, patch);
        iRegion.intensity = cin * (patchMinMax.getMax() - patchMinMax.getMin()) + patchMinMax.getMin();
        iRegion.rsize = (float) Tools.round((iRegion.iPixels.size()) / ((float) osxy * osxy * osz), 3);

        // Probably some stuff for saving images - recalculations etc.
        calculateRegionCenter(iRegion);
        iRegion.perimeter = calculatePerimeter(iRegion, iSingleRegion);
        calculateLength(iRegion, iSingleRegion);
    }

    private double[][][] fillPatch(double[][][] image) {
        double[][][] patchResult = new double[sz][sx][sy];
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    patchResult[z][i][j] = image[(cz + z) / osz][(cx + i) / osxy] [(cy + j) / osxy];
                }
            }
        }
        return patchResult;
    }

    private double[][][] fillMask(Region r) {
        double[][][] result = new double[sz][sx][sy];
        
        for (Pix p : r.iPixels) {
            int rz = (p.pz - cz);
            int rx = (p.px - cx);
            int ry = (p.py - cy);
            result[rz][rx][ry] = 1;
        }
        
        return result;
    }

    private double estimateIntensity(double[][][] aMask, double[][][] aPatch) {
        double[][][][] temp = new double[3][sz][sx][sy];
        Tools.normalizeAndConvolveMask(temp[2], aMask, iPsf, temp[0], temp[1]);
        RegionStatisticsSolver RSS = new RegionStatisticsSolver(temp[0], temp[1], aPatch, null, 10, iBetaMleOut, iBetaMleIn);
        RSS.eval(temp[2] /* convolved mask */);
        return RSS.betaMLEin;
    }

    private void calculateRegionCenter(Region aRegion) {
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (Pix p : aRegion.iPixels) {
            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
        }
        int count = aRegion.iPixels.size();
    
        aRegion.cx = (float) (sumx / count) / osxy;
        aRegion.cy = (float) (sumy / count) / osxy;
        aRegion.cz = (float) (sumz / count) / osz;
    }

    private double calculatePerimeter(Region aRegion, short[][][] aRegions) {
        if (sz == 1) {
            return regionPerimeter2D(aRegion, aRegions);
        }
        else {
            return regionPerimeter3D(aRegion, aRegions);
        }
    }

    private double regionPerimeter2D(Region aRegion, short[][][] aRegions) {
        int numOfFreeEdges = 0;
    
        for (Pix p : aRegion.iPixels) {
            // not on edges of image
            if (p.px != 0 && p.px != nx - 1 && p.py != 0 && p.py != ny - 1) {
                if (aRegions[p.pz][p.px - 1][p.py] == 0) numOfFreeEdges++;
                if (aRegions[p.pz][p.px + 1][p.py] == 0) numOfFreeEdges++;
                if (aRegions[p.pz][p.px][p.py - 1] == 0) numOfFreeEdges++;
                if (aRegions[p.pz][p.px][p.py + 1] == 0) numOfFreeEdges++;
            }
            else {
                numOfFreeEdges++;
            }
        }
    
        return (double)numOfFreeEdges / (osxy);
    }

    private double regionPerimeter3D(Region aRegion, short[][][] aRegions) {
        int numOfFreeEdges = 0;
    
        for (Pix p : aRegion.iPixels) {
            // not on edges of image
            if (p.px != 0 && p.px != nx - 1 && p.py != 0 && p.py != ny - 1 && p.pz != 0 && p.pz != nz - 1) {
                if (aRegions[p.pz][p.px - 1][p.py] == 0) numOfFreeEdges++;
                if (aRegions[p.pz][p.px + 1][p.py] == 0) numOfFreeEdges++;
                if (aRegions[p.pz][p.px][p.py - 1] == 0) numOfFreeEdges++;
                if (aRegions[p.pz][p.px][p.py + 1] == 0) numOfFreeEdges++;
                if (aRegions[p.pz + 1][p.px][p.py] == 0) numOfFreeEdges++;
                if (aRegions[p.pz - 1][p.px][p.py] == 0) numOfFreeEdges++;
            }
            else {
                numOfFreeEdges++;
            }
        }
    
        return (double)numOfFreeEdges / (osxy * osz);
    }

    private void calculateLength(Region aRegion, short[][][] aRegions) {
        final ImageStack is = new ImageStack(sx, sy);
        for (int k = 0; k < sz; k++) {
            final byte[] maskBytes = new byte[sx * sy];
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (aRegions[cz + k][cx + i][cy + j] > 0) {
                        maskBytes[j * sx + i] = (byte) 255;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(sx, sy, maskBytes);
            is.addSlice(bp);
        }
        final ImagePlus skeleton = new ImagePlus("Skeletonized", is);
        
        Skeletonize3D_ skel = new Skeletonize3D_();
        skel.setup("", skeleton);
        skel.run(skeleton.getProcessor());

        regionlength(aRegion, skeleton);
    }

    private void regionlength(Region r, ImagePlus skel) {
        int length = 0;
        final ImageStack is = skel.getStack();
    
        for (Pix v : r.iPixels) {
            // count number of pixels in skeleton
            if (is.getProcessor(v.pz - cz + 1).getPixel(v.px - cx, v.py - cy) != 0) {
                length++;
            }
        }
    
        r.length = ((double) length) / (osxy * osz);
    }

    private void calculatePatchGeometry(Region aRegion) {
        Pix[] mm = aRegion.getMinMaxCoordinates();
        Pix min = mm[0]; Pix max = mm[1];
        int xmin = min.px;
        int ymin = min.py;
        int zmin = min.pz;
        int xmax = max.px;
        int ymax = max.py;
        int zmax = max.pz;
        
        
        final int margin = 5;
        final int zmargin = 2;
        xmin = Math.max(0, xmin - margin);
        xmax = Math.min(nx, xmax + margin + 1);
        ymin = Math.max(0, ymin - margin);
        ymax = Math.min(ny, ymax + margin + 1);
        zmin = Math.max(0, zmin - zmargin);
        zmax = Math.min(nz, zmax + zmargin + 1);

        sx = (xmax - xmin);
        sy = (ymax - ymin);

        cx = xmin;
        cy = ymin;

        sz = (zmax - zmin);
        cz = zmin;
        mosaic.utils.Debug.print(Debug.getArrayDims(iImage));
        mosaic.utils.Debug.print(Debug.getArrayDims(iSingleRegion));
        mosaic.utils.Debug.print(min, max, sx, sy, sz, cx, cy, cz, osxy, osz);
    }
}
