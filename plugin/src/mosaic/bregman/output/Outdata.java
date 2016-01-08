package mosaic.bregman.output;


public interface Outdata<E> {
    void setData(E r); // NO_UCD (unused code)

    // TODO: This is added temporarily to "simulate" all interface ICSVGeneral
    // After Regions3D*.java classes are cleaned up it should go away (possibly wiht Outdata itself).
    public void setFrame(int fr);
}
