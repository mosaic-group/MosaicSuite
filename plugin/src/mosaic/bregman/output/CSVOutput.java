package mosaic.bregman.output;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Vector;

import mosaic.bregman.segmentation.Region;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;

import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;


/**
 * Here we define all the possible csv output format for Squassh
 *
 * @author Pietro Incardona
 */
public class CSVOutput {
    public static final String[] Region3DTrack_map         = new String[] { "Frame", "x", "y", "z", "Size", "Intensity", "Surface" };
    private static final String[] Region3DRScript_map      = new String[] { "Image_ID", "Object_ID", "Size", "Perimeter", "Length", "Intensity", "Coord_X", "Coord_Y", "Coord_Z" };
    private static final String[] Region3DColocRScript_map = new String[] { "Image_ID", "Object_ID", "Size", "Perimeter", "Length", "Intensity", "Overlap_with_ch", "Coloc_object_size",
                                                                            "Coloc_object_intensity", "Single_Coloc", "Coloc_image_intensity", "Coord_X", "Coord_Y", "Coord_Z" };

    private static CellProcessor[] Region3DTrackCellProcessor;
    private static CellProcessor[] Region3DRScriptCellProcessor;
    private static CellProcessor[] Region3DColocRScriptCellProcessor;

    /**
     * Get CellProcessor for Region3DTrack objects
     */
    public static CellProcessor[] getRegion3DTrackCellProcessor() {
        return new CellProcessor[] { new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), };
    }

    /**
     * Init CSV structure
     */
    @SuppressWarnings("unchecked")
    public static void initCSV(int oc_s) {
        Region3DTrackCellProcessor = getRegion3DTrackCellProcessor();
        Region3DRScriptCellProcessor = new CellProcessor[] { new ParseInt(), new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), };
        Region3DColocRScriptCellProcessor = new CellProcessor[] { new ParseInt(), new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(),
                new ParseDouble(), new ParseDouble(), new ParseBool(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), };

        oc = new SquasshOutputChoose[3];

        oc[0] = new SquasshOutputChoose();
        oc[0].name = new String("Format for region tracking)");
        oc[0].outputChoose = new CsvColumnConfig(Region3DTrack_map, Region3DTrackCellProcessor);
        oc[0].classFactory = Region3DTrack.class;
        oc[0].CSVFactory = (Class<CSV<? extends Outdata>>) new CSV<Region3DTrack>(Region3DTrack.class).getClass();
        oc[0].converter = new ConvertAndWrite<Region3DTrack>(Region3DTrack.class);
        oc[0].delimiter = ',';
        
        oc[1] = new SquasshOutputChoose();
        oc[1].name = new String("Format for R script");
        oc[1].outputChoose = new CsvColumnConfig(Region3DRScript_map, Region3DRScriptCellProcessor);
        oc[1].classFactory = Region3DRScript.class;
        oc[1].CSVFactory = (Class<CSV<? extends Outdata>>) new CSV<Region3DRScript>(Region3DRScript.class).getClass();
        oc[1].converter = new ConvertAndWrite<Region3DRScript>(Region3DRScript.class);
        oc[1].delimiter = ';';
        
        oc[2] = new SquasshOutputChoose();
        oc[2].name = new String("Format for R coloc script");
        oc[2].outputChoose = new CsvColumnConfig(Region3DColocRScript_map, Region3DColocRScriptCellProcessor);
        oc[2].classFactory = Region3DColocRScript.class;
        oc[2].CSVFactory = (Class<CSV<? extends Outdata>>) new CSV<Region3DColocRScript>(Region3DColocRScript.class).getClass();
        oc[2].converter = new ConvertAndWrite<Region3DColocRScript>(Region3DColocRScript.class);
        oc[2].delimiter = ';';

        occ = (oc_s == -1) ? oc[0] : oc[oc_s];
    }

    /**
     * Get a vector of objects with the selected format,
     * in particular convert the Region arraylist into
     * objects vector implementing Outdata and a particular output format.
     * The array created can be given to CSV to write a CSV file
     *
     * @param v ArrayList of Region objects
     * @return Vector of object of the selected format
     */
    public static Vector<? extends Outdata> getVector(ArrayList<Region> v) {
        return CSVOutput.occ.converter.getVector(v);
    }

    public static Vector<? extends Outdata> getObjectsList(ArrayList<Region> vec, int frame) {
        final Vector<? extends Outdata> v = getVector(vec);
        for (int i = 0; i < v.size(); i++) {
            v.get(i).setFrame(frame);
        }

        return v;
    }
    
    /**
     * Get an CSV object with the selected format
     * @return CSV
     */
    public static CSV<? extends Outdata> getCSV() {
        try {
            final Constructor<CSV<? extends Outdata>> c = occ.CSVFactory.getDeclaredConstructor(occ.classFactory.getClass());
            final CSV<? extends Outdata> csv = c.newInstance(occ.classFactory.newInstance().getClass());
            csv.setDelimiter(occ.delimiter);
            return csv;
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (final NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (final SecurityException e) {
            e.printStackTrace();
        }
        catch (final IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (final InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SquasshOutputChoose occ;
    public static SquasshOutputChoose oc[];
}
