package mosaic.core.ipc;

import java.io.File;
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


public class InterPluginCSV<E extends ICSVGeneral> {
    private class ExtParam {
        ExtParam(String p1, String p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        String p1;
        String p2;
    }

    private class CommentExtendedCSV implements CommentMatcher {

        @Override
        public boolean isComment(String s) {
            if (s.startsWith("%")) {
                String[] pr = s.split(":");

                if (pr.length == 2) {
                    fld_r.add(new ExtParam(pr[0].substring(1), pr[1].trim()));
                }

                return true;
            }
            return false;
        }

    }

    final private Class<E> p;
    private CsvPreference c;
    final private Vector<ExtParam> fld;
    final private Vector<ExtParam> fld_r;

    /**
     * Constructor, require the class type of the Generic parameter, example for
     * InterPluginCSV<T> you have to pass T.class
     * @param p_
     *            T.class
     */
    public InterPluginCSV(Class<E> p_) {
        p = p_;
        CsvPreference.Builder bld = new CsvPreference.Builder('"', ',', "\n");
        bld.skipComments(new CommentExtendedCSV());
        c = bld.build();
        fld = new Vector<ExtParam>();
        fld_r = new Vector<ExtParam>();
    }

    /**
     * Set Meta information
     * 
     * @param String meta information
     * @param Value of the meta information
     */
    public void setMetaInformation(String parameter, String Value) {
        ExtParam prm = new ExtParam(parameter, Value);
        fld.add(prm);
    }

    /**
     * Get Meta information
     * 
     * @param String meta information
     * @return Value of the meta information
     */
    public String getMetaInformation(String parameter) {
        for (int i = 0; i < fld.size(); i++) {
            if (fld.get(i).p1.equals(parameter)) {
                return fld.get(i).p2;
            }
        }
    
        for (int i = 0; i < fld_r.size(); i++) {
            if (fld_r.get(i).p1.equals(parameter)) {
                return fld_r.get(i).p2;
            }
        }
    
        return null;
    }

    /**
     * Remove Meta information
     * 
     * @param String meta information
     * @param Value of the meta information
     */
    public void removeMetaInformation(String parameter) {
        // search for the Meta information and remove it

        for (int i = 0; i < fld.size(); i++) {
            if (fld.get(i).p1.equals(parameter)) {
                fld.remove(i);
                break;
            }
        }

        // search for the read meta-information and remove it
        for (int i = 0; i < fld_r.size(); i++) {
            if (fld_r.get(i).p1.equals(parameter)) {
                fld_r.remove(i);
                break;
            }
        }
    }

    /**
     * Delete all previously set meta information
     */
    public void clearMetaInformation() {
        fld.clear();
    }

    /**
     * Trying to figure out the best setting to read the CSV file
     * 
     * @param CsvFilename
     */
    public void setCSVPreferenceFromFile(String CsvFilename) {
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), c);

            @SuppressWarnings("unused")
            E element = p.newInstance();

            String[] map = beanReader.getHeader(true); // ignore the header

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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
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
            beanWriter = new CsvDozerBeanWriter(new FileWriter(CsvFilename, append), c);

            E element = null;
            try {
                element = p.newInstance();
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
                for (int i = 0; i < fld.size(); i++) {
                    beanWriter.writeComment("%" + fld.get(i).p1 + ":" + fld.get(i).p2);
                }

                // write read meta information if specified
                for (int i = 0; i < fld_r.size(); i++) {
                    beanWriter.writeComment("%" + fld_r.get(i).p1 + ":" + fld_r.get(i).p2);
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
                    element = p.newInstance();
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
        CsvPreference.Builder bld = new CsvPreference.Builder('"', d, "\n");
        bld.skipComments(new CommentExtendedCSV());
        c = bld.build();
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
            element = p.newInstance();
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
                element = p.newInstance();
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
     * Stitch the CSV files all together in the directory dir/dir_p[] save the
     * result in output_file + dir_p[] "*" are substituted by "_"
     * 
     * @param dir_p list of directories
     * @param dir Base
     * @param output_file stitched file
     * @param Class<T> internal data for conversion
     * @return true if success, false otherwise
     */
    public static <T extends ICSVGeneral> boolean Stitch(String dir_p[], File dir, File output_file, MetaInfo ext[], Class<T> cls) {
        boolean first = true;
        InterPluginCSV<?> csv = new InterPluginCSV<T>(cls);

        for (int j = 0; j < dir_p.length; j++) {
            File[] fl = new File(dir + File.separator + dir_p[j].replace("*", "_")).listFiles();
            if (fl == null)
                continue;
            int nf = fl.length;

            String str[] = new String[nf];

            for (int i = 1; i <= nf; i++) {
                if (fl[i - 1].getName().endsWith(".csv"))
                    str[i - 1] = fl[i - 1].getAbsolutePath();
            }

            if (ext != null) {
                for (int i = 0; i < ext.length; i++)
                    csv.setMetaInformation(ext[i].par, ext[i].value);
            }

            if (first == true) {
                // if it is the first time set the file preference from the first file
                first = false;

                csv.setCSVPreferenceFromFile(str[0]);
            }

            csv.Stitch(str, output_file + dir_p[j]);
        }

        return true;
    }

    @SuppressWarnings("unchecked") OutputChoose ReadGeneral(String CsvFilename, Vector<E> out) {
        OutputChoose occ = new OutputChoose();
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), c);
    
            E element = p.newInstance();
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
    
            beanReader.configureBeanMapping(element.getClass(), map);
    
            occ.map = map;
            occ.cel = c;
    
            while ((element = (E) beanReader.read(element.getClass(), c)) != null) {
                out.add(element);
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
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
    @SuppressWarnings("unchecked") void Readv(String CsvFilename, Vector<E> out, OutputChoose occ) {
        ICsvDozerBeanReader beanReader = null;
        try {
            beanReader = new CsvDozerBeanReader(new FileReader(CsvFilename), c);
    
            E element = p.newInstance();
    
            beanReader.getHeader(true); // ignore the header
            beanReader.configureBeanMapping(element.getClass(), occ.map);
    
            while ((element = (E) beanReader.read(element.getClass(), occ.cel)) != null) {
                out.add(element);
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
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
    }

    /**
     * Stitch CSV files in one with an unknown (but equal between files) format
     * (the first CSV format file drive the output conversion)
     * 
     * @param csvs files to stitch
     * @param Sttch output stitched file
     * @return
     */
    private boolean Stitch(String csvs[], String Sttch) {
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
}
