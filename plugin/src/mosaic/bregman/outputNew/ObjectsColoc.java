package mosaic.bregman.outputNew;

import org.apache.commons.lang3.ArrayUtils;
import org.supercsv.cellprocessor.FmtBool;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

import mosaic.utils.io.csv.CsvColumnConfig;

public class ObjectsColoc extends ImageBase {
    protected static final String[] ObjectsColocMap = ArrayUtils.addAll(ImageBaseMap, new String[] { "Id", "ChannelColoc", "Overlap", "AvgColocObjSize", "AvgColocObjIntensity", "ColocImageIntensity", "SingleColoc" });
    protected static final CellProcessor[] ObjectsColocProc = ArrayUtils.addAll(ImageBaseProc, new CellProcessor[] { new ParseInt(), new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new FmtBool("true", "false") });
    public static final CsvColumnConfig ColumnConfig = new CsvColumnConfig(ObjectsColocMap, ObjectsColocProc);
    
    protected int id; 
    protected int channelColoc; 
    protected double overlap;
    protected double avgColocObjSize;
    protected double avgColocObjIntensity;
    protected double colocImageIntensity;
    protected boolean singleColoc;
    
    public ObjectsColoc(String aFileName, int f, int c, int i, int cc, double over, double objsi, double objin, double imint, boolean sc) {
        setFilename(aFileName);
        setFrame(f);
        setChannel(c);
        setId(i);
        setChannelColoc(cc);
        setOverlap(over);
        setAvgColocObjSize(objsi);
        setAvgColocObjIntensity(objin);
        setColocImageIntensity(imint);
        setSingleColoc(sc);
    }

    // Auto generated getters/setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    
    public int getChannelColoc() {
        return channelColoc;
    }

    
    public void setChannelColoc(int channelColoc) {
        this.channelColoc = channelColoc;
    }
    
    public double getOverlap() {
        return overlap;
    }

    public void setOverlap(double overlap) {
        this.overlap = overlap;
    }

    public double getAvgColocObjSize() {
        return avgColocObjSize;
    }

    public void setAvgColocObjSize(double avgColocObjSize) {
        this.avgColocObjSize = avgColocObjSize;
    }

    public double getAvgColocObjIntensity() {
        return avgColocObjIntensity;
    }
    
    public void setAvgColocObjIntensity(double avgColocObjIntensity) {
        this.avgColocObjIntensity = avgColocObjIntensity;
    }

    public double getColocImageIntensity() {
        return colocImageIntensity;
    }

    public void setColocImageIntensity(double colocImageIntensity) {
        this.colocImageIntensity = colocImageIntensity;
    }
    
    public boolean getSingleColoc() {
        return singleColoc;
    }

    public void setSingleColoc(boolean singleColoc) {
        this.singleColoc = singleColoc;
    }
}
