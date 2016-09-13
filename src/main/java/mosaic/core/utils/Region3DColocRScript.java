package mosaic.core.utils;


import mosaic.bregman.segmentation.Region;


public class Region3DColocRScript {
    private String fileName;
    private int Image_ID;
    private int Object_ID;
    private double Size;
    private double Perimeter;
    private double Length;
    private double Intensity;
    private double Overlap_with_ch;
    private double Coloc_object_size;
    private double Coloc_object_intensity;
    private boolean Single_Coloc;
    private double Coloc_image_intensity;
    private double x;
    private double y;
    private double z;

    public void setImage_ID(int Image_ID_) {
        Image_ID = Image_ID_;
    }

    public void setObject_ID(int Object_ID_) {
        Object_ID = Object_ID_;
    }

    public void setSize(double Size_) {
        Size = Size_;
    }

    public void setPerimeter(double Perimeter_) {
        Perimeter = Perimeter_;
    }

    public void setLength(double Length_) {
        Length = Length_;
    }

    public void setIntensity(double Intensity_) {
        Intensity = Intensity_;
    }

    public void setx(double Coord_X_) { // NO_UCD (unused code)
        x = Coord_X_;
    } // NO_UCD (unused code)

    public void sety(double Coord_Y_) { // NO_UCD (unused code)
        y = Coord_Y_;
    } // NO_UCD (unused code)

    public void setz(double Coord_Z_) { // NO_UCD (unused code)
        z = Coord_Z_;
    } // NO_UCD (unused code)

    public void setOverlap_with_ch(double Overlap_with_ch_) {
        Overlap_with_ch = Overlap_with_ch_;
    }

    public void setColoc_object_size(double cos) {
        Coloc_object_size = cos;
    }

    public void setColoc_object_intensity(double coi) {
        Coloc_object_intensity = coi;
    }

    public void setSingle_Coloc(String sc) {
        Single_Coloc = Boolean.getBoolean(sc);
    }

    public void setColoc_image_intensity(double cii) {
        Coloc_image_intensity = cii;
    }

    public Region3DColocRScript() {
    }

    public void setData(Region r) {
        fileName = "";
        Image_ID = 0;
        Object_ID = r.iLabel;
        Intensity = r.getintensity();
        Perimeter = r.getperimeter();
        Size = r.getrsize();
        Length = r.getlength();
//        Overlap_with_ch = r.getoverlap_with_ch();
//        Coloc_object_size = r.getcoloc_object_size();
//        Coloc_object_intensity = r.getcoloc_object_intensity();
//        Single_Coloc = r.getsingle_coloc();
//        Coloc_image_intensity = r.getcoloc_image_intensity();
        x = r.getcx();
        y = r.getcy();
        z = r.getcz();
    }

    public void setData(Region3DColocRScript r) {
        fileName = r.fileName;
        Image_ID = r.Image_ID;
        Object_ID = r.Object_ID;
        Size = r.Size;
        Perimeter = r.Perimeter;
        Length = r.Length;
        Intensity = r.Intensity;
        Overlap_with_ch = r.Overlap_with_ch;
        Coloc_object_size = r.Coloc_object_size;
        Coloc_object_intensity = r.Coloc_object_intensity;
        Single_Coloc = r.Single_Coloc;
        Coloc_image_intensity = r.Coloc_image_intensity;
        x = r.x;
        y = r.y;
        z = r.z;
    }

    @Override
    public boolean equals(Object r) {
        if (r instanceof Region3DColocRScript) {
            final Region3DColocRScript r_ = (Region3DColocRScript) r;

            if (Image_ID != r_.Image_ID) {
                return false;
            }
            if (Object_ID != r_.Object_ID) {
                return false;
            }
            if (Intensity != r_.Intensity) {
                return false;
            }
            if (Perimeter != r_.Perimeter) {
                return false;
            }
            if (Size != r_.Size) {
                return false;
            }
            if (Length != r_.Length) {
                return false;
            }
            if (Overlap_with_ch != r_.Overlap_with_ch) {
                return false;
            }
            if (Coloc_object_size != r_.Coloc_object_size) {
                return false;
            }
            if (Coloc_object_intensity != r_.Coloc_object_intensity) {
                return false;
            }
            if (Single_Coloc != r_.Single_Coloc) {
                return false;
            }
            if (Coloc_image_intensity != r_.Coloc_image_intensity) {
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
        int result = Object_ID;
        result = 31 * result + Image_ID;
        long doubleFieldBits = Double.doubleToLongBits(Intensity);
        result = (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Perimeter);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Size);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Length);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Overlap_with_ch);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Coloc_object_size);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(Coloc_object_intensity);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        result = 31 * result + (Single_Coloc ? 0 : 1);
        doubleFieldBits = Double.doubleToLongBits(Coloc_image_intensity);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(x);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(y);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        doubleFieldBits = Double.doubleToLongBits(z);
        result = 31 * result + (int)(doubleFieldBits ^ (doubleFieldBits >>> 32));
        return result;
    }
    
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

    public double getSize() {
        return Size;
    }

    public double getPerimeter() {
        return Perimeter;
    }

    public double getLength() {
        return Length;
    }

    public double getx() { // NO_UCD (unused code)
        return x;
    } // NO_UCD (unused code)

    public double gety() { // NO_UCD (unused code)
        return y;
    } // NO_UCD (unused code)

    public double getz() { // NO_UCD (unused code)
        return z;
    } // NO_UCD (unused code)

    public int getFrame() {
        return Image_ID;
    }

    public double getSurface() {
        return 0;
    }

    public double getOverlap_with_ch() {
        return Overlap_with_ch;
    }

    public double getColoc_object_size() {
        return Coloc_object_size;
    }

    public double getColoc_object_intensity() {
        return Coloc_object_intensity;
    }

    public String getSingle_Coloc() {
        return Boolean.toString(Single_Coloc);
    }

    public double getColoc_image_intensity() {
        return Coloc_image_intensity;
    }

    public void setFile(String aFileName) {
        fileName = aFileName;
    }

    public String getFile() {
        return fileName;
    }
}
