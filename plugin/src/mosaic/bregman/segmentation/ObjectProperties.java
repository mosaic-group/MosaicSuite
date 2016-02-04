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
    private final short[][][] iRegions;
    private  psf<DoubleType> iPsf;
    private final double iBetaMleOut;
    private final double iBetaMleIn;

    private final int nx, ny, nz;// size of full oversampled work zone
    private final int osxy, osz;
    private int sx, sy, sz;// size for object
    private int cx, cy, cz;// coord of patch in full work zone (offset)
    
    
    ObjectProperties(double[][][] aImage, Region aRegion, short[][][] aRegions, psf<DoubleType> aPsf, double aBetaMleOut, double aBetaMleIn, int nx, int ny,  int nz, int osxy, int osz) {
        iImage = aImage;
        iRegion = aRegion;
        iRegions = aRegions;
        iPsf = aPsf;
        iBetaMleOut = aBetaMleOut;
        iBetaMleIn = aBetaMleIn;
        
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.osxy = osxy;
        this.osz = osz;
        mosaic.utils.Debug.print(Debug.getArrayDims(iRegions), nx, ny, nz, osxy, osz);
        set_patch_geom(aRegion);
    }

    @Override
    public void run() {
        double[][][] patch = fillPatch(iImage);
        MinMax<Double> patchMinMax = ArrayOps.normalize(patch);
        double[][][] mask = fillMask(iRegion);
        double cin = estimateIntensity(mask, patch);
        iRegion.intensity = cin * (patchMinMax.getMax() - patchMinMax.getMin()) + patchMinMax.getMin();
        if (sz == 1) {
            iRegion.rsize = (float) Tools.round((iRegion.pixels.size()) / ((float) osxy * osxy), 3);
        }
        else {
            iRegion.rsize = (float) Tools.round((iRegion.pixels.size()) / ((float) osxy * osxy * osz), 3);
        }

        // Probably some stuff for saving images - recalculations etc.
        regionIntensityAndCenter(iRegion);
        setPerimeter(iRegion, iRegions);
        setlength(iRegion, iRegions);
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
        
        for (Pix p : r.pixels) {
            int rz = (p.pz - cz);
            int rx = (p.px - cx);
            int ry = (p.py - cy);
            result[rz][rx][ry] = 1;
        }
        
        return result;
    }

    private double estimateIntensity(double[][][] mask, double[][][] aPatch) {
        double[][][][] temp = new double[3][sz][sx][sy];
        Tools.normalizeAndConvolveMask(temp[2], mask, iPsf, temp[0], temp[1]);
        RegionStatisticsSolver RSS = new RegionStatisticsSolver(temp[0], temp[1], aPatch, null, 10, iBetaMleOut, iBetaMleIn);
        RSS.eval(temp[2] /* convolved mask */);
        return RSS.betaMLEin;
    }

    private void regionIntensityAndCenter(Region r) {
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (Pix p : r.pixels) {
            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
        }
        int count = r.pixels.size();
    
        r.cx = (float) (sumx / count);
        r.cy = (float) (sumy / count);
        r.cz = (float) (sumz / count);
    
        r.cx = r.cx / (osxy);
        r.cy = r.cy / (osxy);
        r.cz = r.cz / (osxy);
    }

    private void setPerimeter(Region r, short[][][] regionsA) {
        if (sz == 1) {
            regionPerimeter2D(r, regionsA);
        }
        else {
            regionPerimeter3D(r, regionsA);
        }
    }

    private void regionPerimeter2D(Region r, short[][][] regionsA) {
        double pr = 0;
    
        for (Pix v : r.pixels) {
            int numOfFreeEdges = 0;
            if (v.px != 0 && v.px != nx - 1 && v.py != 0 && v.py != ny - 1) {
                // not on edges of image
                if (regionsA[v.pz][v.px - 1][v.py] == 0) numOfFreeEdges++;
                if (regionsA[v.pz][v.px + 1][v.py] == 0) numOfFreeEdges++;
                if (regionsA[v.pz][v.px][v.py - 1] == 0) numOfFreeEdges++;
                if (regionsA[v.pz][v.px][v.py + 1] == 0) numOfFreeEdges++;
            }
            else {
                numOfFreeEdges++;
            }
            pr += numOfFreeEdges; // real number of edges (should be used with the subpixel)
        }
    
        r.perimeter = pr / (osxy);
    }

    private void regionPerimeter3D(Region r, short[][][] regionsA) {
        // 2 Dimensions only
        double pr = 0;
    
        for (Pix v : r.pixels) {
            int numOfFreeEdges = 0;
            // not on edges of image
            if (v.px != 0 && v.px != nx - 1 && v.py != 0 && v.py != ny - 1 && v.pz != 0 && v.pz != nz - 1) {
                if (regionsA[v.pz][v.px - 1][v.py] == 0) numOfFreeEdges++;
                if (regionsA[v.pz][v.px + 1][v.py] == 0) numOfFreeEdges++;
                if (regionsA[v.pz][v.px][v.py - 1] == 0) numOfFreeEdges++;
                if (regionsA[v.pz][v.px][v.py + 1] == 0) numOfFreeEdges++;
                if (regionsA[v.pz + 1][v.px][v.py] == 0) numOfFreeEdges++;
                if (regionsA[v.pz - 1][v.px][v.py] == 0) numOfFreeEdges++;
            }
            else {
                numOfFreeEdges++;
            }
            pr += numOfFreeEdges;
        }
    
        r.perimeter = pr;
        if (osxy > 1) {
            if (sz == 1) {
                r.perimeter = pr / (osxy);
            }
            else {
                r.perimeter = pr / (osxy * osxy);
            }
        }
    }

    private void setlength(Region r, short[][][] regionsA) {
        final ImageStack is = new ImageStack(sx, sy);
        for (int k = 0; k < sz; k++) {
            final byte[] mask_bytes = new byte[sx * sy];
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (regionsA[cz + k][cx + i][cy + j] > 0) {
                        mask_bytes[j * sx + i] = (byte) 255;
                    }
                    else {
                        mask_bytes[j * sx + i] = (byte) 0;
                    }
                }
            }
            final ByteProcessor bp = new ByteProcessor(sx, sy);
            bp.setPixels(mask_bytes);
            is.addSlice(bp);
        }
    
        final ImagePlus skeleton = new ImagePlus("", is);
    
        // do voronoi in 2D on Z projection
        Skeletonize3D_ skel = new Skeletonize3D_();
        skel.setup("", skeleton);
        skel.run(skeleton.getProcessor());
        
        regionlength(r, skeleton);
    }

    private void regionlength(Region r, ImagePlus skel) {
        int length = 0;
        final ImageStack is = skel.getStack();
    
        for (Pix v : r.pixels) {
            // count number of pixels in skeleton
            if (is.getProcessor(v.pz - cz + 1).getPixel(v.px - cx, v.py - cy) != 0) {
                length++;
            }
        }
    
        r.length = length;
        // osz assumed to me == osxy
        if (osxy > 1) {
            r.length = ((double) length) / osxy;
        }
    }

    private void set_patch_geom(Region r) {
        Pix[] mm = r.getMinMaxCoordinates();
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
        if (nz > 1) {
            zmin = Math.max(0, zmin - zmargin);
            zmax = Math.min(nz, zmax + zmargin + 1);
        }

        sx = (xmax - xmin);
        sy = (ymax - ymin);

        cx = xmin;
        cy = ymin;

        if (nz == 1) {
            sz = 1;
            cz = 0;
        }
        else {
            sz = (zmax - zmin);
            cz = zmin;
        }
    }
}
