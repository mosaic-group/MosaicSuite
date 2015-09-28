package mosaic.core.psf;


import ij.IJ;
import ij.gui.GenericDialog;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;

import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.SerializedDataFile;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;


/**
 * Class that produce Gaussian images
 *
 * @author Pietro Incardona
 * @param <T> Type of image to produce FloatType, Short .......
 */

class GaussPSFSettings implements Serializable {

    private static final long serialVersionUID = 1777976543628904166L;

    //float var[] = new float[16];
}

public class GaussPSF<T extends RealType<T>> implements psf<T> {

    private GaussPSFSettings settings = new GaussPSFSettings();

    private final RealType<T> pos[];
    private RealType<T> var[];
    private final RealType<T> offset[];
    private final Class<T> clCreator;
    private final double[][] sepDimD;
    private double[][] Image2DD;
    private double[][][] Image3DD;
    private float[][] Image2DF;
    private float[][][] Image3DF;

    /**
     * Create a Gaussian object
     * getParameters() create a GUI to get the required parameters
     * convolve() convolve the image with the PSF
     * GaussPSF implements also RandomAccess to get the PSF value on one
     * point.
     * PS if you want to generate a PSF image use GeneratePSF
     *
     * @see psf<T>
     * @see PSFGui
     * @param dim dimension
     * @param cl give the class of the parameter T
     */

    @SuppressWarnings("unchecked")
    public GaussPSF(int dim, Class<T> cl) {
        clCreator = cl;
        pos = (RealType<T>[]) Array.newInstance(cl, dim);
        var = (RealType<T>[]) Array.newInstance(cl, dim);
        offset = (RealType<T>[]) Array.newInstance(cl, dim);
        sepDimD = new double[dim][];

        try {

            for (int i = 0; i < dim; i++) {
                pos[i] = cl.newInstance();
                var[i] = cl.newInstance();
                offset[i] = cl.newInstance();
            }
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set variance of the Gaussian
     *
     * @param var_
     */

    public void setVar(RealType<T> var_[]) {
        var = var_;
    }

    @Override
    public RandomAccess<T> copyRandomAccess() {
        return this;
    }

    @Override
    public int getIntPosition(int i) {
        return (int) pos[i].getRealDouble();
    }

    @Override
    public long getLongPosition(int i) {
        return (int) pos[i].getRealDouble();
    }

    @Override
    public void localize(int[] loc) {
        for (int i = 0; i < loc.length; i++) {
            pos[i].setReal(loc[i]);
        }
    }

    @Override
    public void localize(long[] loc) {
        for (int i = 0; i < loc.length; i++) {
            pos[i].setReal(loc[i]);
        }
    }

    @Override
    public double getDoublePosition(int i) {
        return pos[i].getRealDouble();
    }

    @Override
    public float getFloatPosition(int i) {
        return pos[i].getRealFloat();
    }

    @Override
    public void localize(float[] loc) {
        for (int i = 0; i < loc.length; i++) {
            pos[i].setReal(loc[i]);
        }
    }

    @Override
    public void localize(double[] loc) {
        for (int i = 0; i < loc.length; i++) {
            pos[i].setReal(loc[i]);
        }
    }

    @Override
    public int numDimensions() {
        return pos.length;
    }

    @Override
    public void bck(int i) {
        pos[i].setReal(pos[i].getRealDouble() + 1.0);
    }

    @Override
    public void fwd(int i) {
        pos[i].setReal(pos[i].getRealDouble() + 1.0);
    }

    @Override
    public void move(Localizable arg) {
        for (int i = 0; i < arg.numDimensions(); i++) {
            pos[i].setReal(pos[i].getRealDouble() + arg.getDoublePosition(i));
        }
    }

    @Override
    public void move(int[] mv) {
        for (int i = 0; i < mv.length; i++) {
            pos[i].setReal(mv[i]);
        }
    }

    @Override
    public void move(long[] mv) {
        for (int i = 0; i < mv.length; i++) {
            pos[i].setReal(pos[i].getRealDouble() + mv[i]);
        }
    }

    @Override
    public void move(int i, int j) {
        pos[i].setReal(j);
    }

    @Override
    public void setPosition(Localizable arg) {
        for (int i = 0; i < arg.numDimensions(); i++) {
            pos[i].setReal(arg.getDoublePosition(i));
        }
    }

    @Override
    public void setPosition(int[] pos_) {
        for (int i = 0; i < pos.length; i++) {
            pos[i].setReal(pos_[i]);
        }
    }

    @Override
    public void setPosition(long[] pos_) {
        for (int i = 0; i < pos.length; i++) {
            pos[i].setReal(pos_[i]);
        }
    }

    @Override
    public void setPosition(int i, int j) {
        pos[i].setReal(j);
    }

    @Override
    public void setPosition(long i, int j) {
        pos[(int) i].setReal(j);
    }

    @Override
    public Sampler<T> copy() {
        return this;
    }

    @Override
    public T get() {
        double res = 1.0;

        for (int i = 0; i < pos.length; i++) {
            res *= 1.0 / Math.sqrt(2.0 * Math.PI) / var[i].getRealDouble()
                    * Math.exp(-(pos[i].getRealDouble() - offset[i].getRealDouble()) * (pos[i].getRealDouble() - offset[i].getRealDouble()) / (2.0 * var[i].getRealDouble() * var[i].getRealDouble()));
        }
        final RealType<T> rc = pos[0].createVariable();
        rc.setReal(res);
        T rt = null;
        try {
            rt = clCreator.newInstance();
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        rt.setReal(res);
        return rt;
    }

    @Override
    public void move(long i, int j) {
        pos[(int) i].setReal(pos[(int) i].getRealDouble() + j);
    }

    @Override
    public void getParamenters() {
        settings = getConfigHandler().LoadFromFile(IJ.getDirectory("temp") + File.separator + "psf_gauss_settings.dat", GaussPSFSettings.class, settings);

        final GenericDialog gd = new GenericDialog("Gauss PSF");

        if (pos.length >= 1) {
            gd.addNumericField("sigma_X", 1.0, 3);
        }
        if (pos.length >= 2) {
            gd.addNumericField("sigma_Y", 1.0, 3);
        }
        if (pos.length >= 3) {
            gd.addNumericField("sigma_Z", 1.0, 3);
        }

        for (int i = 0; i < pos.length - 3; i++) {
            gd.addNumericField("sigma_" + i, 1.0, 3);
        }

        for (int i = 0; i < pos.length - 3; i++) {
            gd.addNumericField("mean_" + i, 1.0, 3);
        }

        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        for (int i = 0; i < var.length; i++) {
            var[i].setReal(gd.getNextNumber());
        }

        getConfigHandler().SaveToFile(IJ.getDirectory("temp") + File.separator + "psf_gauss_settings.dat", settings);
    }

    @Override
    public <S extends RealType<S>> void convolve(RandomAccessibleInterval<S> img, S bound) {
        final double[] sigma = new double[img.numDimensions()];

        if (var == null) {
            for (int d = 0; d < sigma.length; ++d) {
                sigma[d] = 1.0;
            }
        }
        else {
            for (int d = 0; d < sigma.length; ++d) {
                sigma[d] = var[d].getRealDouble();
            }
        }

        final RandomAccessible<S> infiniteImg = Views.extendValue(img, bound);

        // Convolve with gaussian;

        try {
            Gauss3.gauss(sigma, infiniteImg, img);
        }
        catch (final IncompatibleTypeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCenter(int[] pos) {
        for (int i = 0; i < pos.length; i++) {
            this.offset[i].setReal(pos[i]);
        }
    }

    @Override
    public String getStringParameters() {
        String opt = new String();

        if (var.length == 2) {
            opt += "sigma_x=" + var[0] + " sigma_y=" + var[1] + " Dimensions=" + var.length;
        }
        else {
            opt += "sigma_x=" + var[0] + " sigma_y=" + var[1] + " sigma_z=" + var[2] + " Dimensions=" + var.length;
        }

        for (int i = 0; i < var.length - 3; i++) {
            opt += "sigma_" + i + "=" + var[i + 3];
        }

        return opt;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    /**
     * Returns handler for (un)serializing GaussPSFSettings objects.
     */
    private static DataFile<GaussPSFSettings> getConfigHandler() {
        return new SerializedDataFile<GaussPSFSettings>();
    }

    @Override
    public int[] getSuggestedImageSize() {
        final int sz[] = new int[pos.length];

        for (int i = 0; i < pos.length; i++) {
            final int szo = (int) (var[i].getRealDouble() * 8.0) + 1;
            if (szo % 2 == 0) {
                sz[i] = szo + 1;
            }
            else {
                sz[i] = szo;
            }
        }

        return sz;
    }

    @Override
    public void setSuggestedImageSize(int[] sz) {
        // not used
    }

    @Override
    public boolean isSeparable() {
        return true;
    }

    @Override
    public double[] getSeparableImageAsDoubleArray(int dim) {
        if (sepDimD[dim] == null) {
            final int sz[] = getSuggestedImageSize();
            final int mid[] = new int[sz.length];
            int[] old_mid = new int[sz.length];

            for (int i = 0; i < sz.length; i++) {
                mid[i] = sz[i] / 2;
            }
            old_mid = getCenter();
            setCenter(mid);

            sepDimD[dim] = new double[sz[dim]];

            for (int i = 0; i < sepDimD[dim].length; i++) {
                final double res = 1.0 / Math.sqrt(2.0 * Math.PI) / var[dim].getRealDouble()
                        * Math.exp(-(i - offset[dim].getRealDouble()) * (i - offset[dim].getRealDouble()) / (2.0 * var[dim].getRealDouble() * var[dim].getRealDouble()));

                sepDimD[dim][i] = res;
            }

            setCenter(old_mid);
        }
        return sepDimD[dim];
    }

    @Override
    public float[][][] getImage3DAsFloatArray() {
        if (Image3DF == null) {
            Image3DF = GeneratePSF.generateImage3DAsFloatArray(this);
        }
        return Image3DF;
    }

    @Override
    public double[][][] getImage3DAsDoubleArray() {
        if (Image3DD == null) {
            Image3DD = GeneratePSF.generateImage3DAsDoubleArray(this);
        }
        return Image3DD;
    }

    @Override
    public double[][] getImage2DAsDoubleArray() {
        if (Image2DD == null) {
            Image2DD = GeneratePSF.generateImage2DAsDoubleArray(this);
        }
        return Image2DD;
    }

    @Override
    public float[][] getImage2DAsFloatArray() {
        Image2DF = GeneratePSF.generateImage2DAsFloatArray(this);
        return Image2DF;
    }

    @Override
    public int[] getCenter() {
        final int ofs[] = new int[pos.length];

        for (int i = 0; i < ofs.length; i++) {
            ofs[i] = (int) offset[i].getRealDouble();
        }

        return ofs;
    }
}
