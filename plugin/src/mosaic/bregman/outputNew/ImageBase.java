package mosaic.bregman.outputNew;

import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;

public class ImageBase {
    protected static final String[] ImageBaseMap = new String[] { "FileName", "Frame", "Channel" };
    protected static final CellProcessor[] ImageBaseProc = new CellProcessor[] { new Optional(), new ParseInt(), new ParseInt() };
    
    protected String filename = "<noFileName>";
    protected int frame;
    protected int channel;
    
    // Auto generated getters/setters
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getFrame() {
        return frame;
    }
    
    public void setFrame(int frame) {
        this.frame = frame;
    }
    
    public int getChannel() {
        return channel;
    }
    
    public void setChannel(int channel) {
        this.channel = channel;
    }
}
