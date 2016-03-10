package mosaic.bregman.output;


import mosaic.bregman.GUI.GUIOutputChoose;
import mosaic.utils.io.csv.CSV;


public class SquasshOutputChoose extends GUIOutputChoose {
    /* Class to produce InterPluginsCSV of internal type */
    Class<CSV<? extends Outdata>> CSVFactory;
    /* Internal type class factory */
    Class<? extends Outdata> classFactory;
    public ConvertAndWrite<? extends Outdata> converter;
    char delimiter;
}
