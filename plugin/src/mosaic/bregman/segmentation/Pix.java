package mosaic.bregman.segmentation;


public class Pix {

    public int pz;
    public int px;
    public int py;

    public Pix(int z, int x, int y) {
        this.pz = z;
        this.px = x;
        this.py = y;
    }
    
    @Override
    public String toString() {
        return "[" + pz + ", " + px + ", " + py + "]";
    }
}
