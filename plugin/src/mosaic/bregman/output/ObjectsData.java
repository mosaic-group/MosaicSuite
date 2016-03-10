package mosaic.bregman.output;

import org.apache.commons.lang3.ArrayUtils;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

import mosaic.utils.io.csv.CsvColumnConfig;

public class ObjectsData extends ImageBase {
    protected static final String[] ObjectsDataMap = ArrayUtils.addAll(ImageBaseMap, new String[] { "Id", "X", "Y", "Z", "Size", "Perimeter", "Length", "Intensity" });
    protected static final CellProcessor[] ObjectsDataProc = ArrayUtils.addAll(ImageBaseProc, new CellProcessor[] { new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble() });
    public static final CsvColumnConfig ColumnConfig = new CsvColumnConfig(ObjectsDataMap, ObjectsDataProc);
    
    protected int id;
    protected double x;
    protected double y;
    protected double z; 
    protected double size;
    protected double perimeter; 
    protected double length;
    protected double intensity;
    
    public ObjectsData(String file, int f, int c, int i, double x, double y, double z, double si, double per, double le, double in) {
        setFilename(file);
        setFrame(f);
        setChannel(c);
        setId(i);
        setX(x);
        setY(y);
        setZ(z);
        setSize(si);
        setPerimeter(per);
        setLength(le);
        setIntensity(in);
    }
    
    // Auto generated getters/setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getZ() {
        return z;
    }
    
    public void setZ(double z) {
        this.z = z;
    }
    
    public double getSize() {
        return size;
    }
    
    public void setSize(double size) {
        this.size = size;
    }
    
    public double getPerimeter() {
        return perimeter;
    }
    
    public void setPerimeter(double perimeter) {
        this.perimeter = perimeter;
    }
    
    public double getLength() {
        return length;
    }
    
    public void setLength(double length) {
        this.length = length;
    }
    
    public double getIntensity() {
        return intensity;
    }
    
    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }
}
