package mosaic.plugins;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import net.imglib2.Cursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;


public class ColorSubstitution implements  ExtendedPlugInFilter // NO_UCD
{
    <T extends RealType<T> & NativeType<T> >
    void substitute(ImagePlus imp, double col_from, double col_to)
    {
        final Img<T> image = ImagePlusAdapter.wrap( imp );
        final Cursor<T> cur = image.cursor();

        while (cur.hasNext()) {
            cur.next();
            if (cur.get().getRealFloat() == col_from) {
                cur.get().setReal(col_to);
            }
        }

        imp.repaintWindow();
    }

    @Override
    public int setup(String arg, ImagePlus imp)
    {
        return DOES_8G + DOES_16 + DOES_32;
    }

    @Override
    public int showDialog(ImagePlus imp, String arg1, PlugInFilterRunner arg2) {
        final GenericDialog gd = new GenericDialog("Color substitution");
        gd.addNumericField("Change color value from:", 0, 3);
        gd.addNumericField("                   to:  ", 0, 3);
        gd.showDialog();

        if (gd.wasCanceled() == true) {
            return DONE;
        }

        final double col_from = gd.getNextNumber();
        final double col_to = gd.getNextNumber();

        substitute(imp,col_from,col_to);

        return DONE;
    }

    @Override
    public void run(ImageProcessor ip) {
        // Nothing to do here
    }

    @Override
    public void setNPasses(int arg0) {
        // Nothing to do here
    }
}
