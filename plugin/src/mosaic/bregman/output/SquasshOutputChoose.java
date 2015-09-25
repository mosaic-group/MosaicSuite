package mosaic.bregman.output;


import mosaic.bregman.Region;
import mosaic.core.GUI.GUIOutputChoose;
import mosaic.utils.io.csv.CSV;


public class SquasshOutputChoose extends GUIOutputChoose {

    /* Class to produce InterPluginsCSV of internal type */
    public Class<CSV<? extends Outdata<Region>>> CSVFactory;

    /* Internal type class factory */
    public Class<? extends Outdata<Region>> classFactory;

    public ConvertAndWrite<? extends Outdata<Region>> converter;

    char delimiter;
}
