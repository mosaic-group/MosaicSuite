package temporary;

import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.StubProp;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class Thing extends StubProp implements ICSVGeneral {
    public static final String[] Thing_map = new String[] 
    { 
        "Frame",
        "x"
    };
    public static CellProcessor[] Thing_CellProcessor = new CellProcessor[] 
    {
        new ParseInt(),
        new ParseDouble()
    };
    
    int frame;
    double x;
    
    Thing(int aFrame, double aX) {frame = aFrame; x = aX;}
    Th
    @Override
    public int getFrame() {return frame;}
    
    @Override
    public double getx() {return x;}
    
    public void setData(Thing p)
    {
        this.frame = p.frame;
        this.x = p.x;
    }
}
