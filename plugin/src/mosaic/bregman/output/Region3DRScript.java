package mosaic.bregman.output;


import mosaic.bregman.segmentation.Region;


public class Region3DRScript implements Outdata<Region> {

    private int Image_ID;
    private int Object_ID;
    private double Size;
    private double Perimeter;
    private double Length;
    private double Intensity;
    private double x;
    private double y;
    private double z;

    public void setImage_ID(int Image_ID_) {
        Image_ID = Image_ID_;
    }

    public void setlabel(int aID) { // NO_UCD (unused code)
        setImage_ID(aID);
    } // NO_UCD (unused code)

    public void setObject_ID(int Object_ID_) {
        Object_ID = Object_ID_;
    }

    public void setSize(double Size_) {
        Size = Size_;
    }

    public void setsize(double aSize) { // NO_UCD (unused code)
        setSize(aSize);
    } // NO_UCD (unused code)

    public void setPerimeter(double Perimeter_) {
        Perimeter = Perimeter_;
    }

    public void setLength(double Length_) {
        Length = Length_;
    }

    public void setIntensity(double Intensity_) {
        Intensity = Intensity_;
    }

    public void setmean(double aVal) { // NO_UCD (unused code)
        setIntensity(aVal);
    } // NO_UCD (unused code)

    public void setx(double Coord_X_) { // NO_UCD (unused code)
        x = Coord_X_;
    } // NO_UCD (unused code)

    public void sety(double Coord_Y_) { // NO_UCD (unused code)
        y = Coord_Y_;
    } // NO_UCD (unused code)

    public void setz(double Coord_Z_) { // NO_UCD (unused code)
        z = Coord_Z_;
    } // NO_UCD (unused code)

    public Region3DRScript() {
    }

    @Override
    public void setData(Region r) {

        Image_ID = 0;
        Object_ID = 0;
        Intensity = r.getintensity();
        Perimeter = r.getperimeter();
        Length = r.getlength();
        Size = r.getrsize();
        x = r.getcx();
        y = r.getcy();
        z = r.getcz();
    }

    public void setData(Region3DRScript r) {

        Image_ID = r.Image_ID;
        Object_ID = r.Object_ID;
        Size = r.Size;
        Perimeter = r.Perimeter;
        Length = r.Length;
        Intensity = r.Intensity;
        x = r.x;
        y = r.y;
        z = r.z;
    }

    @Override
    public boolean equals(Object r) {
        if (r instanceof Region3DRScript) {
            final Region3DRScript r_ = (Region3DRScript) r;

            if (Size != r_.Size) {
                return false;
            }
            if (Perimeter != r_.Perimeter) {
                return false;
            }
            if (Length != r_.Length) {
                return false;
            }
            if (Intensity != r_.Intensity) {
                return false;
            }
            if (x != r_.x) {
                return false;
            }
            if (y != r_.y) {
                return false;
            }
            if (z != r_.z) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        long doubleFieldBits = Double.doubleToLongBits(Intensity);
        int result = (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Perimeter);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Size);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Length);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(x);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(y);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(z);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        return result;
    }
    
    @Override
    public void setFrame(int fr) {
        Image_ID = fr;
    }

    public void setSurface(double Surface_) {
        Perimeter = Surface_;
    }

    public void setCoord_X(double Coord_X_) {
        x = Coord_X_;
    }

    public void setCoord_Y(double Coord_Y_) {
        y = Coord_Y_;
    }

    public void setCoord_Z(double Coord_Z_) {
        z = Coord_Z_;
    }

    public int getImage_ID() {
        return Image_ID;
    }

    public int getlabel() { // NO_UCD (unused code)
        return getImage_ID();
    } // NO_UCD (unused code)

    public int getObject_ID() {
        return Object_ID;
    }

    public double getCoord_X() {
        return x;
    }

    public double getCoord_Y() {
        return y;
    }

    public double getCoord_Z() {
        return z;
    }

    public double getIntensity() {
        return Intensity;
    }

    public double getmean() { // NO_UCD (unused code)
        return getIntensity();
    } // NO_UCD (unused code)

    public double getSize() {
        return Size;
    }

    public double getsize() { // NO_UCD (unused code)
        return getSize();
    } // NO_UCD (unused code)

    public double getPerimeter() {
        return Perimeter;
    }

    public double getLength() {
        return Length;
    }

    public double getx() { // NO_UCD (unused code)
        return x;
    } 

    public double gety() { // NO_UCD (unused code)
        return y;
    } 

    public double getz() { // NO_UCD (unused code)
        return z;
    } 

    public int getFrame() {
        return Image_ID;
    }

    public double getSurface() {
        return 0;
    }

    public void setFile(@SuppressWarnings("unused") String dummy) {
    }

    public String getFile() {
        return null;
    }
}
