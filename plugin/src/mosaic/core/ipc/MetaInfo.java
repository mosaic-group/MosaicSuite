package mosaic.core.ipc;

public class MetaInfo {
    public String parameter;
    public String value;

    public MetaInfo(String aParameter, String aValue) {
        parameter = aParameter;
        value = aValue;
    }

    public MetaInfo() {
        this("", "");
    }

    @Override
    public String toString() {
        return "{[" + parameter + "][" + value + "]}";
    }
}
