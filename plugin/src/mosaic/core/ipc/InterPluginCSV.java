package mosaic.core.ipc;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Vector;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.io.dozer.CsvDozerBeanReader;
import org.supercsv.io.dozer.CsvDozerBeanWriter;
import org.supercsv.io.dozer.ICsvDozerBeanReader;
import org.supercsv.io.dozer.ICsvDozerBeanWriter;
import org.supercsv.prefs.CsvPreference;


public class InterPluginCSV<E> {
    private class CommentExtendedCSV implements CommentMatcher {
        @Override
        public boolean isComment(String s) {
            // Comment style:
            // %parameter:value
            if (s.startsWith("%")) {
                String[] pr = s.split(":");

                if (pr.length == 2) {
                    iMetaInfosRead.add(new MetaInfo(pr[0].substring(1), pr[1].trim()));
                }
                return true;
            }
            return false;
        }
    }

    final private Class<E> iClazz;
    private CsvPreference iCsvPreference;
    final private Vector<MetaInfo> iMetaInfos;
    final private Vector<MetaInfo> iMetaInfosRead;

    /**
     * @param aClazz E.class
     */
    public InterPluginCSV(Class<E> aClazz) {
        iClazz = aClazz;
        iMetaInfos = new Vector<MetaInfo>();
        iMetaInfosRead = new Vector<MetaInfo>();
        setCsvPreference(',');
    }

    /**
     * Set Meta information
     * @param aParameter meta information
     * @param aValue of the meta information
     */
    public void setMetaInformation(String aParameter, String aValue) {
        setMetaInformation(new MetaInfo(aParameter, aValue));
    }
    
    /**
     * Set Meta information
     * @param aMetaInfo - meta information
     */
    public void setMetaInformation(MetaInfo aMetaInfo) {
        iMetaInfos.add(aMetaInfo);
    }

    /**
     * Get Meta information
     * 
     * @param parameter - Name of meta information parameter
     * @return Value of the meta information
     */
    public String getMetaInformation(String parameter) {
        for (MetaInfo mi : iMetaInfos) {
            if (mi.parameter.equals(parameter)) return mi.value;
        }
        for (MetaInfo mi : iMetaInfosRead) {
            if (mi.parameter.equals(parameter)) return mi.value;
        }
    
        return null;
    }

    /**
     * Remove Meta information
     * 
     * @param parameter - Name of meta information parameter
     */
    public void removeMetaInformation(String parameter) {
        for (int i = 0; i < iMetaInfos.size(); i++) {
            if (iMetaInfos.get(i).parameter.equals(parameter)) {
                iMetaInfos.remove(i);
                break;
            }
        }

        for (int i = 0; i < iMetaInfosRead.size(); i++) {
            if (iMetaInfosRead.get(i).parameter.equals(parameter)) {
                iMetaInfosRead.remove(i);
                break;
            }
        }
    }

    /**
     * Delete all previously set meta information
     */
    public void clearMetaInformation() {
        iMetaInfos.clear();
    }

    /**
     * Trying to figure out the best setting to read the CSV file
     * 
     * @param aCsvFilename
     */
    public void setCSVPreferenceFromFile(String aCsvFilename) {
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(aCsvFilename), iCsvPreference);

            String[] map = beanReader.getHeader(true);

            if (map.length == 1) {
                if (map[0].split(";").length > 1) {
                    setDelimiter(';');
                    return;
                }

                if (map[0].split(" ").length > 1) {
                    setDelimiter(' ');
                    return;
                }
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (beanReader != null) {
                try {
                    beanReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Read a General CSV file result is returned as a vector of Generics
     * 
     * @param CsvFilename Name of the filename to open
     * @return out output vector
     */
    public Vector<E> Read(String CsvFilename) {
        Vector<E> out = new Vector<E>();
        ReadGeneral(CsvFilename, out);

        return out;
    }

    /**
     * Read a CSV file result is returned as a vector of Generics
     * 
     * @param CsvFilename Name of the filename to open
     * @param OutputChoose Output choose
     * @return out output vector
     */
    public Vector<E> Read(String CsvFilename, OutputChoose occ) {
        Vector<E> out = new Vector<E>();
        Readv(CsvFilename, out, occ);

        return out;
    }

    /**
     * Write a CSV file, perform an automatical conversion from the element
     * inside vector and the internal type, setData(T) where T is the type of
     * the sigle element in the out vector has to be implemented in the POJO
     * Class (aka internal type)
     * 
     * @param CsvFilename Filename
     * @param out Input file of object
     * @param occ Output choose
     */
    public void Write(String CsvFilename, Vector<?> out, OutputChoose occ, boolean append) {
        if (out.size() == 0)
            return;

        Class<?> cl = out.get(0).getClass();

        ICsvDozerBeanWriter beanWriter = null;
        try {
            beanWriter = new CsvDozerBeanWriter(new FileWriter(CsvFilename, append), iCsvPreference);

            E element = null;
            try {
                element = iClazz.newInstance();
            } catch (InstantiationException e1) {
                e1.printStackTrace();
                return;
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
                return;
            }

            // configure the mapping from the fields to the CSV columns
            beanWriter.configureBeanMapping(element.getClass(), occ.map);

            // write the header and metainformation
            if (append == false) {
                beanWriter.writeHeader(occ.map);

                // Write meta information
                for (int i = 0; i < iMetaInfos.size(); i++) {
                    beanWriter.writeComment("%" + iMetaInfos.get(i).parameter + ":" + iMetaInfos.get(i).value);
                }

                // write read meta information if specified
                for (int i = 0; i < iMetaInfosRead.size(); i++) {
                    beanWriter.writeComment("%" + iMetaInfosRead.get(i).parameter + ":" + iMetaInfosRead.get(i).value);
                }
            }

            // write the beans
            Method m = null;

            try {
                m = element.getClass().getMethod("setData", cl);
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
                System.err.println("Error in order to Write on CSV the base class has to implement Outdata<>");
                return;
            } catch (SecurityException e1) {
                e1.printStackTrace();
                return;
            }

            for (int i = 0; i < out.size(); i++) {
                try {
                    element = iClazz.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    m.invoke(element, out.get(i));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                beanWriter.write(element, occ.cel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (beanWriter != null) {
                try {
                    beanWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Set delimiter
     * 
     * @param d delimiter
     */
    public void setDelimiter(char d) {
        setCsvPreference(d);
    }

    /**
     * Create a vector of the internal type from an array of unknown type
     * 
     * @param ar
     * @return
     */
    public Vector<?> getVector(ArrayList<?> ar) {
        Vector<E> v = new Vector<E>();

        if (ar.size() == 0)
            return v;

        Class<?> car = ar.get(0).getClass();
        E element = null;
        Method m = null;

        try {
            element = iClazz.newInstance();
            m = element.getClass().getMethod("setData", car);
        } catch (NoSuchMethodException e1) {
            e1.printStackTrace();
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < ar.size(); i++) {
            try {
                element = iClazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            try {
                m.invoke(element, ar.get(i));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            v.add(element);
        }

        return v;
    }



    /**
     * Stitch CSV files in one with an unknown (but equal between files) format
     * (the first CSV format file drive the output conversion)
     * 
     * @param csvs files to stitch
     * @param Sttch output stitched file
     * @return
     */
    public boolean Stitch(String csvs[], String Sttch) {
        if (csvs.length == 0)
            return false;
        Vector<E> out = new Vector<E>();
    
        OutputChoose occ = ReadGeneral(csvs[0], out);
        if (occ == null)
            return false;
    
        for (int i = 1; i < csvs.length; i++) {
            Readv(csvs[i], out, occ);
        }
    
        Write(Sttch, out, occ, false);
    
        return true;
    }

    OutputChoose ReadGeneral(String CsvFilename, Vector<E> out) {
        OutputChoose occ = new OutputChoose();
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), iCsvPreference);

            String[] map = beanReader.getHeader(true); // ignore the header

            // number of column

            if (map == null) // we cannot get the header
                return null;

            CellProcessor c[] = new CellProcessor[map.length];
            ProcessorGeneral pc = new ProcessorGeneral();

            for (int i = 0; i < c.length; i++) {
                try {
                    map[i] = pc.getMap(map[i].replace(" ", "_"));
                    c[i] = (CellProcessor) pc.getClass().getMethod("getProcessor" + map[i].replace(" ", "_")).invoke(pc);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    // getProcessor from above getMethod is not existing
                    // figure out what to do with this. Anyway this situation
                    // seems to be handled below.
                    //
                    // e.printStackTrace();

                    c[i] = null;
                    map[i] = "Nothing";
                    continue;
                }
                map[i] = map[i].replace(" ", "_");
            }

            beanReader.configureBeanMapping(iClazz, map);

            occ.map = map;
            occ.cel = c;

            E element = null;
            while ((element = beanReader.read(iClazz, c)) != null) {
                out.add(element);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            if (beanReader != null) {
                try {
                    beanReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return occ;
    }

    /**
     * Read a Csv file given a format occ (combination of map and Cell
     * processor)
     * 
     * @param CsvFilename csv filename
     * @param out output vector
     * @param occ format choosen
     */
    void Readv(String CsvFilename, Vector<E> out, OutputChoose occ) {
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), iCsvPreference);

            beanReader.getHeader(true); // ignore the header
            beanReader.configureBeanMapping(iClazz, occ.map);

            E element = null;
            while ((element = beanReader.read(iClazz, occ.cel)) != null) {
                out.add(element);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (beanReader != null) {
                try {
                    beanReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setCsvPreference(char aDelimiter) {
        CsvPreference.Builder bld = new CsvPreference.Builder('"', aDelimiter, "\n");
        bld.skipComments(new CommentExtendedCSV());
        iCsvPreference = bld.build();
    }
}
