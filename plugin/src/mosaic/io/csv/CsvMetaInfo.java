package mosaic.io.csv;

public class CsvMetaInfo {
    public String parameter;
    public String value;

    public CsvMetaInfo(String aParameter, String aValue) {
        parameter = aParameter;
        value = aValue;
    }

    public CsvMetaInfo() {
        this("", "");
    }

    @Override
    public String toString() {
        return "{[" + parameter + "][" + value + "]}";
    }
}
