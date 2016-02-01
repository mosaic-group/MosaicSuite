package mosaic.bregman.output;


import mosaic.bregman.GUI.GUIOutputChoose;
import mosaic.bregman.segmentation.Region;
import mosaic.utils.io.csv.CSV;


public class SquasshOutputChoose extends GUIOutputChoose {
    /* Class to produce InterPluginsCSV of internal type */
    Class<CSV<? extends Outdata<Region>>> CSVFactory;
    /* Internal type class factory */
    Class<? extends Outdata<Region>> classFactory;
    public ConvertAndWrite<? extends Outdata<Region>> converter;
    char delimiter;
}
