package mosaic.bregman.outputNew;

import org.apache.commons.lang3.ArrayUtils;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

import mosaic.utils.io.csv.CsvColumnConfig;

public class ImageData extends ImageBase {
    protected static final String[] ImageDataMap = ArrayUtils.addAll(ImageBaseMap, new String[] { "NumOfObjects", "MeanSize", "MeanSurface", "MeanLength" });
    protected static final CellProcessor[] ImageDataProc = ArrayUtils.addAll(ImageBaseProc, new CellProcessor[] { new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble() });
    public static final CsvColumnConfig ColumnConfig = new CsvColumnConfig(ImageDataMap, ImageDataProc);
    
    protected int numOfObjects;
    protected double meanSize; 
    protected double meanSurface;
    protected double meanLength;
    
    public ImageData(String aFileName, int f, int c, int n, double si, double su, double le) {
        setFilename(aFileName);
        setFrame(f);
        setChannel(c);
        setNumOfObjects(n);
        setMeanSize(si);
        setMeanSurface(su);
        setMeanLength(le);
    }
    
    // Auto generated getters/setters
    
    public int getNumOfObjects() {
        return numOfObjects;
    }
    
    public void setNumOfObjects(int numOfObjects) {
        this.numOfObjects = numOfObjects;
    }
    
    public double getMeanSize() {
        return meanSize;
    }
    
    public void setMeanSize(double meanSize) {
        this.meanSize = meanSize;
    }
    
    public double getMeanSurface() {
        return meanSurface;
    }
    
    public void setMeanSurface(double meanSurface) {
        this.meanSurface = meanSurface;
    }
    
    public double getMeanLength() {
        return meanLength;
    }
    
    public void setMeanLength(double meanLength) {
        this.meanLength = meanLength;
    }
}
