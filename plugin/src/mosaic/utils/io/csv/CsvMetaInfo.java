package mosaic.utils.io.csv;

public class CsvMetaInfo {
    final public String parameter;
    final public String value;

    public CsvMetaInfo(String aParameter, String aValue) {
        parameter = aParameter;
        value = aValue;
    }

    @Override
    public String toString() {
        return "{[" + parameter + "][" + value + "]}";
    }
}
