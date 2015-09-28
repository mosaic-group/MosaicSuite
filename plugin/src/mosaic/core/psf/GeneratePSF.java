package mosaic.core.psf;


import ij.IJ;
import ij.gui.GenericDialog;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;

import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.SerializedDataFile;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;


/**
 * This class generate PSF images from the list of all implemented
 * PSF. or from a File
 *
 * @see psfList.java
 *      Just create the a new GeneratePSF class and call generate
 * @author Pietro Incardona
 */

class PSFSettings implements Serializable {
    private static final long serialVersionUID = 3757876543608904166L;

    String clist;
}

public class GeneratePSF {

    private PSFSettings settings = new PSFSettings();

    private int sz[];

    private Choice PSFc;

    private psf<FloatType> psfc;
    protected TextField dimF;

    /**
     * Get the parameters for the Psf
     *
     * @param dim Dimension of the PSF
     * @param psf String that identify the PSF like "Gauss ... "
     */

    private void selectPSF(int dim, String psf) {
        if (dim == 0) {
            IJ.error("Dimension must be a valid integer != 0");
        }
        psfc = psfList.factory(psf, dim, FloatType.class);
        psfc.getParamenters();
    }

    /**
     * Get the parameters for the PSF
     *
     * @param dim dimension of the psf
     */

    protected void selectPSF(int dim) {
        final String psf = PSFc.getSelectedItem();

        if (dim == 0) {
            IJ.error("Dimension must be a valid integer != 0");
        }
        psfc = psfList.factory(psf, dim, FloatType.class);
        psfc.getParamenters();
    }

    /**
     * Generate a 2D array image from the PSF
     *
     * @param psf
     * @return 2D array image
     */

    static <T extends RealType<T>> double[][] generateImage2DAsDoubleArray(psf<T> psf) {
        if (psf.getSuggestedImageSize().length != 2) {
            return null;
        }

        final int sz[] = psf.getSuggestedImageSize();

        final double[][] img = new double[sz[0]][sz[1]];

        final int[] mid = new int[sz.length];

        for (int i = 0; i < mid.length; i++) {
            mid[i] = sz[i] / 2;
        }

        // If is file psf

        int old_mid[] = null;
        if (psf.isFile() == false) {
            old_mid = psf.getCenter();
            psf.setCenter(mid);
        }

        //

        final int loc[] = new int[sz.length];

        // Create an img

        for (int i = 0; i < sz[0]; i++) {
            for (int j = 0; j < sz[1]; j++) {
                loc[0] = i;
                loc[1] = j;
                psf.setPosition(loc);
                final double f = psf.get().getRealFloat();
                img[i][j] = f;
            }
        }

        // Reset the center to previous one

        psf.setCenter(old_mid);

        return img;
    }

    static <T extends RealType<T>> float[][] generateImage2DAsFloatArray(psf<T> psf) {
        if (psf.getSuggestedImageSize().length != 2) {
            return null;
        }

        final int sz[] = psf.getSuggestedImageSize();

        final float[][] img = new float[sz[0]][sz[1]];

        final int[] mid = new int[sz.length];

        for (int i = 0; i < mid.length; i++) {
            mid[i] = sz[i] / 2;
        }

        // If is file psf

        int old_mid[] = null;
        if (psf.isFile() == false) {
            old_mid = psf.getCenter();
            psf.setCenter(mid);
        }

        //

        final int loc[] = new int[sz.length];

        // Create an imglib2

        for (int i = 0; i < sz[0]; i++) {
            for (int j = 0; j < sz[1]; j++) {
                loc[0] = i;
                loc[1] = j;
                psf.setPosition(loc);
                final float f = psf.get().getRealFloat();
                img[i][j] = f;
            }
        }

        // Reset the center to previous one

        psf.setCenter(old_mid);

        return img;
    }

    /**
     * Generate a 3D array image from the PSF
     *
     * @param psf
     * @return 3D array image
     */

    static <T extends RealType<T>> double[][][] generateImage3DAsDoubleArray(psf<T> psf) {
        if (psf.getSuggestedImageSize().length != 3) {
            return null;
        }

        final int sz[] = psf.getSuggestedImageSize();

        final double[][][] img = new double[sz[2]][sz[1]][sz[0]];

        final int[] mid = new int[sz.length];

        for (int i = 0; i < mid.length; i++) {
            mid[i] = sz[i] / 2;
        }

        // If is file psf

        int old_mid[] = null;
        if (psf.isFile() == false) {
            old_mid = psf.getCenter();
            psf.setCenter(mid);
        }

        //

        final int loc[] = new int[sz.length];

        // Create an imglib2

        for (int i = 0; i < sz[0]; i++) {
            for (int j = 0; j < sz[1]; j++) {
                for (int k = 0; k < sz[2]; k++) {
                    loc[0] = i;
                    loc[1] = j;
                    loc[2] = k;
                    psf.setPosition(loc);
                    final float f = psf.get().getRealFloat();
                    img[k][j][i] = f;
                }
            }
        }

        // Reset the center to previous one

        psf.setCenter(old_mid);

        return img;
    }

    static <T extends RealType<T>> float[][][] generateImage3DAsFloatArray(psf<T> psf) {
        if (psf.getSuggestedImageSize().length != 3) {
            return null;
        }

        final int sz[] = psf.getSuggestedImageSize();

        final float[][][] img = new float[sz[0]][sz[1]][sz[2]];

        final int[] mid = new int[sz.length];

        for (int i = 0; i < mid.length; i++) {
            mid[i] = sz[i] / 2;
        }

        // If is file psf

        int old_mid[] = null;
        if (psf.isFile() == false) {
            old_mid = psf.getCenter();
            psf.setCenter(mid);
        }

        //

        final int loc[] = new int[sz.length];

        // Create an imglib2

        for (int i = 0; i < sz[2]; i++) {
            for (int j = 0; j < sz[1]; j++) {
                for (int k = 0; k < sz[0]; k++) {
                    loc[0] = i;
                    loc[1] = j;
                    loc[2] = k;
                    psf.setPosition(loc);
                    final float f = psf.get().getRealFloat();
                    img[k][j][i] = f;
                }
            }
        }

        // Reset the center to previous one

        psf.setCenter(old_mid);

        return img;
    }

    /**
     * Return a generated PSF image. A GUI is shown ti give the user
     * the possibility to choose size of the image PSF function parameters
     *
     * @return An image representing the PSF
     */

    public Img<FloatType> generate(int dim) {
        settings.clist = psfList.psfList[0];
        settings = getConfigHandler().LoadFromFile(IJ.getDirectory("temp") + File.separator + "psf_settings.dat", PSFSettings.class, settings);

        final GenericDialog gd = new GenericDialog("PSF Generator");
        gd.addNumericField("Dimensions ", dim, 0);

        if (IJ.isMacro() == false) {
            dimF = (TextField) gd.getNumericFields().lastElement();

            gd.addChoice("PSF: ", psfList.psfList, settings.clist);
            PSFc = (Choice) gd.getChoices().lastElement();
            {
                final Button optionButton = new Button("Options");
                final GridBagConstraints c = new GridBagConstraints();
                final int gridx = 2;
                int gridy = 1;
                c.gridx = gridx;
                c.gridy = gridy++;
                c.anchor = GridBagConstraints.EAST;
                gd.add(optionButton, c);

                optionButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final int dim = Integer.parseInt(dimF.getText());
                        selectPSF(dim);
                    }
                });
            }
        }
        else {
            gd.addChoice("PSF: ", psfList.psfList, settings.clist);
        }

        gd.showDialog();

        // if Batch system

        final String choice = gd.getNextChoice();
        if (IJ.isMacro() == true) {
            dim = (int) gd.getNextNumber();
            selectPSF(dim, choice);
        }

        // psf not selected

        if (psfc == null) {
            dim = (int) gd.getNextNumber();
            selectPSF(dim, choice);
        }

        // get the dimension

        sz = psfc.getSuggestedImageSize();
        if (sz == null) {
            return null;
        }

        // center on the middle of the image

        final int[] mid = new int[sz.length];

        for (int i = 0; i < mid.length; i++) {
            mid[i] = sz[i] / 2;
        }

        // If is file psf

        if (psfc.isFile() == false) {
            psfc.setCenter(mid);
        }

        //

        final int loc[] = new int[sz.length];

        // Create an imglib2

        final ImgFactory<FloatType> imgFactory = new ArrayImgFactory<FloatType>();
        final Img<FloatType> PSFimg = imgFactory.create(sz, new FloatType());

        final Cursor<FloatType> cft = PSFimg.cursor();

        while (cft.hasNext()) {
            cft.next();
            cft.localize(loc);
            psfc.setPosition(loc);
            final float f = psfc.get().getRealFloat();
            cft.get().set(f);
        }

        // Save settings
        settings.clist = choice;
        getConfigHandler().SaveToFile(IJ.getDirectory("temp") + File.separator + "psf_settings.dat", settings);

        return PSFimg;
    }

    public String getParameters() {
        return psfc.getStringParameters();
    }

    /**
     * Returns handler for (un)serializing PSFSettings objects.
     */
    private static DataFile<PSFSettings> getConfigHandler() {
        return new SerializedDataFile<PSFSettings>();
    }
}
