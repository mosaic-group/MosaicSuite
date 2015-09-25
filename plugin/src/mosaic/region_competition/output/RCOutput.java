package mosaic.region_competition.output;


/**
 * Region Competition internal data conversion
 *
 * @author Pietro Incardona
 */

public class RCOutput {

    int Frame;
    double x;
    double y;
    double z;
    double mean;
    double size;
    double var;

    public void setImage_ID(int Image_ID_) {
        Frame = Image_ID_;
    }

    public void setSize(double Size_) {
        size = Size_;
    }

    public void setsize(double Size_) // NO_UCD (unused code)
    {
        size = Size_;
    }

    public void setx(double Coord_X_) // NO_UCD (unused code)
    {
        x = Coord_X_;
    }

    public void sety(double Coord_Y_) // NO_UCD (unused code)
    {
        y = Coord_Y_;
    }

    public void setz(double Coord_Z_) // NO_UCD (unused code)
    {
        z = Coord_Z_;
    }

    public void setFrame(int fr) {
        Frame = fr;
    }

    public void setIntensity(double intensity) {
        mean = intensity;
    }

    public void setmean(double intensity) // NO_UCD (unused code)
    {
        mean = intensity;
    }

    public int getObject_ID() {
        return 0;
    }

    public void setlabel(int Object_ID_) { // NO_UCD (unused code)
        setObject_ID(Object_ID_);
    }

    public void setObject_ID(@SuppressWarnings("unused") int Object_ID_) {
    }

    public int getlabel() { // NO_UCD (unused code)
        return getObject_ID();
    }

    public void setCoord_X(double Coord_X_) {
        x = Coord_X_;
    }

    public double getCoord_Y() {
        return y;
    }

    public double getCoord_Z() {
        return z;
    }

    public double getCoord_X() {
        return x;
    }

    public double getIntensity() {
        return mean;
    }

    public double getmean() // NO_UCD (unused code)
    {
        return mean;
    }

    public int getImage_ID() {
        return Frame;
    }

    public void setCoord_Y(double Coord_Y_) {
        y = Coord_Y_;
    }

    public void setCoord_Z(double Coord_Z_) {
        z = Coord_Z_;
    }

    public double getSize() {
        return size;
    }

    public double getsize() // NO_UCD (unused code)
    {
        return size;
    }

    public void setData(RCOutput rco) {
        Frame = rco.Frame;
        x = rco.x;
        y = rco.y;
        z = rco.z;
        mean = rco.mean;
        size = rco.size;
        var = rco.var;
    }

    @Override
    public boolean equals(Object rc) {
        RCOutput rco = (RCOutput) rc;

        if (x != rco.x) {
            return false;
        }
        if (y != rco.y) {
            return false;
        }
        y = rco.y;
        if (z != rco.z) {
            z = rco.z;
        }
        if (mean != rco.mean) {
            mean = rco.mean;
        }
        if (size != rco.size) {
            size = rco.size;
        }
        // TODO: It is not set anywhere in RCOutput
        if (var != rco.var) {
            var = rco.var;
        }

        return true;
    }
}
