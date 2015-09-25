package mosaic.bregman;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

import java.util.Iterator;

import mosaic.core.psf.GaussPSF;
import net.imglib2.type.numeric.real.DoubleType;


class ObjectProperties implements Runnable {

    private double intmin, intmax;
    private final double[][][] image;
    private final Region region;
    private final double[][][][] temp1;
    private final double[][][][] temp2;
    private final double[][][][] temp3;
    private double cin;
    private double[][][] patch;
    private final short[][][] regions;
    private final int margin;
    private final int zmargin;
    private int sx, sy, sz;// size for object
    private final int nx, ny, nz;// size of full oversampled work zone
    private final Parameters p;
    private int cx, cy, cz;// coord of patch in full work zone (offset)
    private final int osxy, osz;
    private double[][][][] mask;// nregions nslices ni nj
    private final byte[] imagecolor_c1;

    public ObjectProperties(double[][][] im, Region reg, int nx, int ny, int nz, Parameters p1, int osxy, int osz, byte[] color_c1, short[][][] regs) {
        this.regions = regs;
        this.p = new Parameters(p1);
        this.image = im;
        this.region = reg;
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.imagecolor_c1 = color_c1;
        margin = 5;
        zmargin = 2;

        this.osxy = osxy;
        this.osz = osz;

        set_patch_geom(region);

        temp1 = new double[1][sz][sx][sy];
        temp2 = new double[1][sz][sx][sy];
        temp3 = new double[1][sz][sx][sy];
        // set size
        p.ni = sx;
        p.nj = sy;
        p.nz = sz;
        // set psf
        if (p.nz > 1) {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(3, DoubleType.class);
            final DoubleType[] var = new DoubleType[3];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            var[2] = new DoubleType(p.sigma_gaussian / p.zcorrec);
            psf.setVar(var);
            p.PSF = psf;
        }
        else {
            final GaussPSF<DoubleType> psf = new GaussPSF<DoubleType>(2, DoubleType.class);
            final DoubleType[] var = new DoubleType[2];
            var[0] = new DoubleType(p.sigma_gaussian);
            var[1] = new DoubleType(p.sigma_gaussian);
            psf.setVar(var);
            p.PSF = psf;
        }
    }

    @Override
    public void run() {
        fill_patch(image);
        normalize();
        fill_mask(region);
        estimate_int(mask[0]);
        region.intensity = cin * (intmax - intmin) + intmin;
        if (p.nz == 1) {
            region.rsize = round((region.pixels.size()) / ((float) osxy * osxy), 3);
        }
        else {
            region.rsize = round((region.pixels.size()) / ((float) osxy * osxy * osxy), 3);
        }

        if (p.dispint) {
            fill_ints();
        }

        if (p.save_images) {
            setIntensitiesandCenters(region, image);
            setPerimeter(region, regions);
            setlength(region, regions);
        }
    }

    private void fill_ints() {
        if (imagecolor_c1 == null) {
            return;
        }
        final int c1 = (int) Math.min(255, 255 * Math.sqrt(region.intensity)); // Green
        final int c0 = (int) Math.min(255, 255 * region.intensity); // Red
        final int c2 = (int) Math.min(255, 255 * Math.pow(region.intensity, 2)); // Blue

        for (final Iterator<Pix> it2 = region.pixels.iterator(); it2.hasNext();) {
            final Pix p = it2.next();
            // set correct color
            final int t = p.pz * nx * ny * 3 + p.px * ny * 3;
            imagecolor_c1[t + p.py * 3 + 0] = (byte) c0;
            imagecolor_c1[t + p.py * 3 + 1] = (byte) c1;
            imagecolor_c1[t + p.py * 3 + 2] = (byte) c2;
            // green
        }
    }

    private void fill_patch(double[][][] image) {
        this.patch = new double[sz][sx][sy];
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    this.patch[z][i][j] = image[(cz + z) / osz][(cx + i) / osxy][(cy + j) / osxy];
                }
            }
        }
    }

    private void estimate_int(double[][][] mask) {
        RegionStatisticsSolver RSS;

        RSS = new RegionStatisticsSolver(temp1[0], temp2[0], temp3[0], patch, 10, p);
        RSS.eval(mask);

        // cout=RSS.betaMLEout;
        cin = RSS.betaMLEin;
    }

    private void set_patch_geom(Region r) {
        int xmin, ymin, zmin, xmax, ymax, zmax;
        xmin = nx;
        ymin = ny;
        zmin = nz;
        xmax = 0;
        ymax = 0;
        zmax = 0;

        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix p = it.next();
            if (p.px < xmin) {
                xmin = p.px;
            }
            if (p.px > xmax) {
                xmax = p.px;
            }
            if (p.py < ymin) {
                ymin = p.py;
            }
            if (p.py > ymax) {
                ymax = p.py;
            }
            if (p.pz < zmin) {
                zmin = p.pz;
            }
            if (p.pz > zmax) {
                zmax = p.pz;
            }
        }

        xmin = Math.max(0, xmin - margin);
        xmax = Math.min(nx, xmax + margin + 1);

        ymin = Math.max(0, ymin - margin);
        ymax = Math.min(ny, ymax + margin + 1);

        if (nz > 1) {// if (zmax-zmin>0){
            // do whole column
            // zmin=0;
            // zmax=p.nz-1;
            // old one
            zmin = Math.max(0, zmin - zmargin);
            zmax = Math.min(nz, zmax + zmargin + 1);
        }

        this.sx = (xmax - xmin);// todo :correct :+1 : done
        this.sy = (ymax - ymin);// correct :+1

        cx = xmin;
        cy = ymin;

        if (nz == 1) {
            this.sz = 1;
            cz = 0;
        }
        else {
            this.sz = (zmax - zmin);
            cz = zmin;
        }
    }

    private void normalize() {
        intmin = Double.MAX_VALUE;
        intmax = 0;
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    if (patch[z][i][j] > intmax) {
                        intmax = patch[z][i][j];
                    }
                    if (patch[z][i][j] < intmin) {
                        intmin = patch[z][i][j];
                    }
                }
            }
        }

        // rescale between 0 and 1
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    patch[z][i][j] = (patch[z][i][j] - intmin) / (intmax - intmin);
                }
            }
        }

    }

    private void fill_mask(Region r) {
        this.mask = new double[1][sz][sx][sy];
        for (int z = 0; z < sz; z++) {
            for (int i = 0; i < sx; i++) {
                for (int j = 0; j < sy; j++) {
                    this.mask[0][z][i][j] = 0;
                }
            }
        }

        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix p = it.next();
            int rz, rx, ry;
            rz = (p.pz - cz);
            rx = (p.px - cx);
            ry = (p.py - cy);
            this.mask[0][rz][rx][ry] = 1;
        }
    }

    private void setIntensitiesandCenters(Region r, double[][][] image) {
        regionIntensityAndCenter(r, image);
    }

    private void setPerimeter(Region r, short[][][] regionsA) {
        if (p.nz == 1) {
            regionPerimeter(r, regionsA);
        }
        else {
            regionPerimeter3D(r, regionsA);
        }
    }

    private void regionPerimeter(Region r, short[][][] regionsA) {
        // 2 Dimensions only
        double pr = 0;
        // int rvalue= r.value;

        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            int edges = 0;
            final Pix v = it.next();
            // count number of free edges
            if (v.px != 0 && v.px != nx - 1 && v.py != 0 && v.py != ny - 1) {
                // not on edges of image
                if (regionsA[v.pz][v.px - 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px + 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py - 1] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py + 1] == 0) {
                    edges++;// !=rvalue
                }
            }
            else {
                edges++;
            }

            pr += edges; // real number of edges (should be used with the
            // subpixel)
        }

        r.perimeter = pr;

        if (Analysis.p.subpixel) {
            r.perimeter = pr / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
        }
    }

    private void regionPerimeter3D(Region r, short[][][] regionsA) {
        // 2 Dimensions only
        double pr = 0;
        // int rvalue= r.value;

        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            int edges = 0;
            final Pix v = it.next();
            // count number of free edges
            if (v.px != 0 && v.px != nx - 1 && v.py != 0 && v.py != ny - 1 && v.pz != 0 && v.pz != nz - 1) {// not
                // on
                // edges
                // of
                // image
                if (regionsA[v.pz][v.px - 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px + 1][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py - 1] == 0) {
                    edges++;
                }
                if (regionsA[v.pz][v.px][v.py + 1] == 0) {
                    edges++;
                }
                if (regionsA[v.pz + 1][v.px][v.py] == 0) {
                    edges++;
                }
                if (regionsA[v.pz - 1][v.px][v.py] == 0) {
                    edges++;
                }
            }
            else {
                edges++;
            }
            pr += edges;
        }

        r.perimeter = pr;
        if (osxy > 1) {
            if (p.nz == 1) {
                r.perimeter = pr / (osxy);
            }
            else {
                r.perimeter = pr / (osxy * osxy);
            }
        }
    }

    private void regionIntensityAndCenter(Region r, double[][][] image) {
        int count = 0;
        double sum = 0;
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix p = it.next();
            if (!Analysis.p.refinement) {
                sum += image[p.pz][p.px][p.py];
            }

            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
            count++;
        }

        if (!Analysis.p.refinement) {
            r.intensity = (sum / (count));
        }// done in refinement

        r.cx = (float) (sumx / count);
        r.cy = (float) (sumy / count);
        r.cz = (float) (sumz / count);

        if (Analysis.p.subpixel) {
            r.cx = r.cx / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
            r.cy = r.cy / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
            r.cz = r.cz / (Analysis.p.oversampling2ndstep * Analysis.p.interpolation);
        }
    }

    private void setlength(Region r, short[][][] regionsA) {
        // 2D only yet
        final ImagePlus skeleton = new ImagePlus();

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

        skeleton.setStack(is);

        // do voronoi in 2D on Z projection
        IJ.run(skeleton, "Skeletonize (2D/3D)", "");

        regionlength(r, skeleton);
    }

    private void regionlength(Region r, ImagePlus skel) {
        int length = 0;
        final ImageStack is = skel.getStack();

        for (final Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
            final Pix v = it.next();
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

    private float round(float y, int z) {
        // Special tip to round numbers to 10^-2
        y *= Math.pow(10, z);
        y = (int) y;
        y /= Math.pow(10, z);
        return y;
    }

}
