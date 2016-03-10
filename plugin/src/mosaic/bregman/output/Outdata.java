package mosaic.bregman.output;

import mosaic.bregman.segmentation.Region;

public interface Outdata {
    void setData(Region r); 
    public void setFrame(int fr);
}
