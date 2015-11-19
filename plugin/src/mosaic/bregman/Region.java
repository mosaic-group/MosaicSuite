package mosaic.bregman;


import java.util.ArrayList;


/* An inner class to make the results list sortable. */
public class Region implements Comparable<Region> {

    boolean colocpositive = false;

    Region(int value, int points) {
        this.value = value;
        this.points = points;
    }

    Region(int points) {
        this.points = points;
    }

    ArrayList<Pix> pixels = new ArrayList<Pix>();
    final int points;
    float rsize;
    int value;
    double perimeter;
    double length;
    Region rvoronoi;
    double intensity;
    float cx, cy, cz;
    float overlap;
    float over_int;
    float over_size;
    boolean singlec;
    double coloc_o_int;

    @Override
    public int compareTo(Region otherRegion) {
        return (value < otherRegion.value) ? 1 : ((value > otherRegion.value) ? -1 : 0);
    }

    public double getcx() {
        return cx;
    }

    public double getcy() {
        return cy;
    }

    public double getcz() {
        return cz;
    }

    public double getintensity() {
        return intensity;
    }

    public double getrsize() {
        return rsize;
    }

    public double getperimeter() {
        return perimeter;
    }

    public double getlength() {
        return length;
    }

    public double getoverlap_with_ch() {
        return overlap;
    }

    public double getcoloc_object_size() {
        return over_size;
    }

    public double getcoloc_object_intensity() {
        return over_int;
    }

    public boolean getsingle_coloc() {
        return singlec;
    }

    public double getcoloc_image_intensity() {
        return coloc_o_int;
    }
}
