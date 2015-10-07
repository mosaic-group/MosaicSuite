package mosaic.region_competition.output;

/**
 * Region Competition CSV output format
 */
public class RCOutput {

    // All fields of CSV file
    private int Id;
    private int Image_ID;
    private int label;
    private double size;
    private double mean;
    private double variance;
    private double Coord_X; 
    private double Coord_Y;
    private double Coord_Z;
    
    // Below are getters/setters used by CSV.java for reading and writing
    // They are accessed via reflection.
    
    public int getId() {
        return Id;
    }
    
    public void setId(int id) {
        Id = id;
    }
    
    public int getImage_ID() {
        return Image_ID;
    }
    
    public void setImage_ID(int image_ID) {
        Image_ID = image_ID;
    }
    
    public int getlabel() {
        return label;
    }
    
    public void setlabel(int label) {
        this.label = label;
    }
    
    public double getsize() {
        return size;
    }
    
    public void setsize(double size) {
        this.size = size;
    }
    
    public double getmean() {
        return mean;
    }
    
    public void setmean(double mean) {
        this.mean = mean;
    }
    
    public double getvariance() {
        return variance;
    }
    
    public void setvariance(double variance) {
        this.variance = variance;
    }
    
    public double getCoord_X() {
        return Coord_X;
    }
    
    public void setCoord_X(double coord_X) {
        Coord_X = coord_X;
    }
    
    public double getCoord_Y() {
        return Coord_Y;
    }
    
    public void setCoord_Y(double coord_Y) {
        Coord_Y = coord_Y;
    }
    
    public double getCoord_Z() {
        return Coord_Z;
    }
    
    public void setCoord_Z(double coord_Z) {
        Coord_Z = coord_Z;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(Coord_X);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(Coord_Y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(Coord_Z);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + Id;
        result = prime * result + Image_ID;
        result = prime * result + label;
        temp = Double.doubleToLongBits(mean);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(size);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(variance);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RCOutput other = (RCOutput) obj;
        if (Double.doubleToLongBits(Coord_X) != Double.doubleToLongBits(other.Coord_X)) return false;
        if (Double.doubleToLongBits(Coord_Y) != Double.doubleToLongBits(other.Coord_Y)) return false;
        if (Double.doubleToLongBits(Coord_Z) != Double.doubleToLongBits(other.Coord_Z)) return false;
        if (Id != other.Id) return false;
        if (Image_ID != other.Image_ID) return false;
        if (label != other.label) return false;
        if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean)) return false;
        if (Double.doubleToLongBits(size) != Double.doubleToLongBits(other.size)) return false;
        if (Double.doubleToLongBits(variance) != Double.doubleToLongBits(other.variance)) return false;
        return true;
    }
}
