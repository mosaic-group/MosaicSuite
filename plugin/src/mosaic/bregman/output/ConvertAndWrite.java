package mosaic.bregman.output;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import mosaic.bregman.Region;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvColumnConfig;

/**
 * Hopefully temporary class to perform some needed conversions and write output as CSV.
 * It remembers full type of objects so can perform some nasty conversions.
 *
 * @param <T>
 */
public class ConvertAndWrite<T> {
    Class<T> iClazz;

    ConvertAndWrite(Class<T> aClazz) {
        iClazz = aClazz;
    }

    @SuppressWarnings("unchecked")
    public void Write(CSV<? extends Outdata<Region>> aCsv, String aCsvFilename, Vector<?> aOutputData, CsvColumnConfig aOutputChoose, boolean aShouldAppend) {
        ((CSV<T>)aCsv).Write(aCsvFilename, (Vector<T>) aOutputData, aOutputChoose, aShouldAppend);
    }

    /**
     * Create a vector of the internal type from an array of unknown type
     * @param aData
     * @return
     */
    public Vector<T> getVector(List<?> aData) {
        Vector<T> v = new Vector<T>();
        if (aData.size() == 0) {
            return v;
        }

        try {

            Class<?> car = aData.get(0).getClass();
            Method m = iClazz.getMethod("setData", car);
            T element = null;
            for (int i = 0; i < aData.size(); i++) {
                element = iClazz.newInstance();
                m.invoke(element, aData.get(i));
                v.add(element);
            }

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return v;
    }
}
