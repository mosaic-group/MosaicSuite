package mosaic.bregman.segmentation;


public class Pix {

    public final int pz;
    public final int px;
    public final int py;

    Pix(int z, int x, int y) {
        this.pz = z;
        this.px = x;
        this.py = y;
    }
    
    @Override
    public String toString() {
        return "[" + pz + ", " + px + ", " + py + "]";
    }
}
