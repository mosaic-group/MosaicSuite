package mosaic.core.ipc;

import ij.IJ;

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
     * @return Value of the meta information or null if not found
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
     * Remove Meta Information
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
                    // TODO: dangerous! What if there is only one column but has spaces in name?
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
     * Reads a CSV file
     * 
     * @param aCsvFilename Name of the filename to open
     * @return container with values
     */
    public Vector<E> Read(String aCsvFilename) {
        return Read(aCsvFilename, null);
    }

    /**
     * Read a CSV file
     * 
     * @param aCsvFilename - Name of the filename to open
     * @param OutputChoose - output choose with defined colum names and processors
     * @return container with values
     */
    public Vector<E> Read(String aCsvFilename, OutputChoose aOutputChoose) {
        Vector<E> out = new Vector<E>();
        readData(aCsvFilename, out, aOutputChoose);

        return out;
    }

    /**
     * Write a CSV file, perform an automatic conversion from the element
     * inside vector and the internal type, setData(T) where T is the type of
     * the single element in the out vector has to be implemented in the POJO
     * Class (aka internal type)
     * 
     * @param CsvFilename Filename
     * @param out Input file of object
     * @param occ Output choose
     */
    public void Write(String aCsvFilename, Vector<?> aOutputData, OutputChoose aOutputChoose, boolean aShouldAppend) {
        if (aOutputData.size() == 0)
            return;

        Class<?> cl = aOutputData.get(0).getClass();

        ICsvDozerBeanWriter beanWriter = null;
        try {
            beanWriter = new CsvDozerBeanWriter(new FileWriter(aCsvFilename, aShouldAppend), iCsvPreference);

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
            beanWriter.configureBeanMapping(element.getClass(), aOutputChoose.map);

            // write the header and metainformation
            if (aShouldAppend == false) {
                beanWriter.writeHeader(aOutputChoose.map);

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

            for (int i = 0; i < aOutputData.size(); i++) {
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
                    m.invoke(element, aOutputData.get(i));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                beanWriter.write(element, aOutputChoose.cel);
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
     * @param aInputFileNames - files to stitch
     * @param aOutputFileName - output stitched file
     * @return true when succeed
     */
    public boolean Stitch(String aInputFileNames[], String aOutputFileName) {
        if (aInputFileNames.length == 0)
            return false;
        Vector<E> out = new Vector<E>();
    
        OutputChoose occ = readData(aInputFileNames[0], out, null);
        if (occ == null)
            return false;
    
        for (int i = 1; i < aInputFileNames.length; i++) {
            readData(aInputFileNames[i], out, occ);
        }
    
        Write(aOutputFileName, out, occ, false);
    
        return true;
    }

    /**
     * Read a CSV file
     * 
     * @param aCsvFilename - CSV filename
     * @param out output - container for output data
     * @param aOutputChoose - chosen output (if null, it will be generated from header)
     */
    OutputChoose readData(String aCsvFilename, Vector<E> aOutput, OutputChoose aOutputChoose) {
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(aCsvFilename), iCsvPreference);
    
            String[] map = beanReader.getHeader(true);
            if (map == null) return null; // we cannot get the header
            if (aOutputChoose == null) aOutputChoose = generateOutputChoose(map);

            beanReader.configureBeanMapping(iClazz, aOutputChoose.map);
    
            E element;
            while ((element = beanReader.read(iClazz, aOutputChoose.cel)) != null) {
                aOutput.add(element);
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
        
        return aOutputChoose;
    }

    private OutputChoose generateOutputChoose(String[] map) {
        OutputChoose occ = new OutputChoose();
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
                // Set handling to use stub method getNothing/setNothing
                IJ.log("Method not found: [getProcessor" + map[i].replace(" ", "_") + "]");
                c[i] = null;
                map[i] = "Nothing";
                continue;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            map[i] = map[i].replace(" ", "_");
        }
        occ.map = map;
        occ.cel = c;
        
        return occ;
    }

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
    
    private void setCsvPreference(char aDelimiter) {
        CsvPreference.Builder bld = new CsvPreference.Builder('"', aDelimiter, "\n");
        bld.skipComments(new CommentExtendedCSV());
        iCsvPreference = bld.build();
    }
}
