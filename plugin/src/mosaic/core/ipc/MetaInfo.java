package mosaic.core.ipc;

public class MetaInfo {
    public MetaInfo(String aParameter, String aValue) {
        parameter = aParameter;
        value = aValue;
    }

    public MetaInfo() {
        this("", "");
    }


    public String parameter;
    public String value;
}
