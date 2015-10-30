package mosaic.utils.io.csv;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.comment.CommentMatcher;
import org.supercsv.io.dozer.CsvDozerBeanReader;
import org.supercsv.io.dozer.CsvDozerBeanWriter;
import org.supercsv.io.dozer.ICsvDozerBeanReader;
import org.supercsv.io.dozer.ICsvDozerBeanWriter;
import org.supercsv.prefs.CsvPreference;


public class CSV<E> {
    protected static final Logger logger = Logger.getLogger(CSV.class);

    private final Class<E> iClazz;
    private CsvPreference iCsvPreference;
    private final Vector<CsvMetaInfo> iMetaInfos;
    final Vector<CsvMetaInfo> iMetaInfosRead;

    /**
     * @param aClazz E.class
     */
    public CSV(Class<E> aClazz) {
        iClazz = aClazz;
        iMetaInfos = new Vector<CsvMetaInfo>();
        iMetaInfosRead = new Vector<CsvMetaInfo>();
        setCsvPreference(',');
    }

    /**
     * Set Meta information
     * @param aParameter meta information
     * @param aValue of the meta information
     */
    public void setMetaInformation(String aParameter, String aValue) {
        setMetaInformation(new CsvMetaInfo(aParameter, aValue));
    }

    /**
     * Set Meta information
     * @param aMetaInfo - meta information
     */
    private void setMetaInformation(CsvMetaInfo aMetaInfo) {
        final String value = getMetaInformation(aMetaInfo.parameter);
        if (value != null) {
            logger.debug("MetaInfo " + aMetaInfo + " added, but same parameter with value [" + value + "] already exists!");
        }
        iMetaInfos.add(aMetaInfo);
    }

    /**
     * Get Meta information
     *
     * @param parameter - Name of meta information parameter
     * @return Value of the meta information or null if not found
     */
    public String getMetaInformation(String parameter) {
        String value = getMetaInformation(iMetaInfos, parameter);
        if (value == null) {
            value = getMetaInformation(iMetaInfosRead, parameter);
        }
        return value;
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

            final String[] map = beanReader.getHeader(true);
            // In case when header map is equal to 1 it is probable that current delimiter is not correct
            // Try to find better. (Of course there is always a chanse that there is only one column).
            if (map.length == 1) {
                if (map[0].split(";").length > 1) {
                    setDelimiter(';');
                }
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        finally {
            if (beanReader != null) {
                try {
                    beanReader.close();
                } catch (final IOException e) {
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
     * @param CsvColumnConfig - output choose with defined colum names and processors
     * @return container with values
     */
    public Vector<E> Read(String aCsvFilename, CsvColumnConfig aOutputChoose) {
        final Vector<E> out = new Vector<E>();
        readData(aCsvFilename, out, aOutputChoose);

        return out;
    }

    /**
     * Writes data to csv file.
     * @param aCsvFilename - full absolute path/name of output file
     * @param aOutputData - container with data to be written
     * @param aOutputChoose - names/processors of expected output
     * @param aShouldAppend - if appends, then header and metainformation is not written
     */
    public void Write(String aCsvFilename, List<E> aOutputData, CsvColumnConfig aOutputChoose, boolean aShouldAppend) {
        // Make sure that OutputChoose does not contain empty (null) values for header
        final List<String> map = new ArrayList<String>();
        final List<CellProcessor> cp = new ArrayList<CellProcessor>();
        boolean isErrorReported = false;
        for (int i = 0; i < aOutputChoose.fieldMapping.length; ++i) {
            if (aOutputChoose.fieldMapping[i] != null && !aOutputChoose.fieldMapping[i].equals("")) {
                map.add(aOutputChoose.fieldMapping[i]);
                cp.add(aOutputChoose.cellProcessors[i]);
            }
            else {
                if (!isErrorReported) {
                    logger.error("Empty or null [" + aOutputChoose.fieldMapping[i] + "] field declared for file [" + aCsvFilename + "]!");
                    logger.error(aOutputChoose);
                    isErrorReported = true;
                }
            }
        }
        final String[] mapString = map.toArray(new String[map.size()]);
        final CellProcessor[] cel = cp.toArray(new CellProcessor[cp.size()]);
        final CsvColumnConfig oc = new CsvColumnConfig(mapString, cel);

        writeData(aCsvFilename, aOutputData, oc, aShouldAppend);
    }

    /**
     * Set delimiter
     * @param d delimiter
     */
    public void setDelimiter(char d) {
        setCsvPreference(d);
    }

    /**
     * Stitch CSV files in one with an unknown (but equal between files) format
     * (the first CSV format file drive the output conversion)
     *
     * @param aInputFileNames - files to stitch
     * @param aOutputFileName - output stitched file
     * @return true when succeed
     */
    public boolean Stitch(String[] aInputFileNames, String aOutputFileName) {
        return Stitch(aInputFileNames, aOutputFileName, null);
    }

    /**
     * Stitch CSV files in one with an unknown (but equal between files) format
     * (the first CSV format file drive the output conversion)
     *
     * @param aInputFileNames - files to stitch
     * @param aOutputFileName - output stitched file
     * @return true when succeed
     */
    boolean Stitch(String[] aInputFileNames, String aOutputFileName, CsvColumnConfig aOutputChoose) {
        if (aInputFileNames.length == 0) {
            return false;
        }

        final Vector<E> out = new Vector<E>();
        final CsvColumnConfig occ = readData(aInputFileNames[0], out, aOutputChoose);
        if (occ == null) {
            return false;
        }

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
    private CsvColumnConfig readData(String aCsvFilename, Vector<E> aOutput, CsvColumnConfig aOutputChoose) {
        ICsvDozerBeanReader beanReader = null;
        try {
            logger.info("Reading file: [" + aCsvFilename + "]");
            beanReader = new CsvDozerBeanReader(new FileReader(aCsvFilename), iCsvPreference);

            final String[] map = beanReader.getHeader(true);
            if (map == null)
            {
                return null; // we cannot get the header
            }
            if (aOutputChoose == null) {
                aOutputChoose = generateOutputChoose(map);
            }

            beanReader.configureBeanMapping(iClazz, aOutputChoose.fieldMapping);

            E element;
            while ((element = beanReader.read(iClazz, aOutputChoose.cellProcessors)) != null) {
                aOutput.add(element);
            }

        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (beanReader != null) {
                try {
                    beanReader.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return aOutputChoose;
    }

    private void writeData(String aCsvFilename, List<E> aOutputData, CsvColumnConfig aOutputChoose, boolean aShouldAppend) {
        if (aOutputData.size() == 0) {
            return;
        }

        ICsvDozerBeanWriter beanWriter = null;
        try {
            logger.info("Writing file: [" + aCsvFilename + "], append: " + aShouldAppend);
            beanWriter = new CsvDozerBeanWriter(new FileWriter(aCsvFilename, aShouldAppend), iCsvPreference);
            beanWriter.configureBeanMapping(iClazz, aOutputChoose.fieldMapping);

            // write the header and meta information
            if (aShouldAppend == false) {
                beanWriter.writeHeader(aOutputChoose.fieldMapping);

                // Write meta information
                for (final CsvMetaInfo mi : iMetaInfos) {
                    beanWriter.writeComment("%" + mi.parameter + ":" + mi.value);
                }

                // write read meta information if specified
                for (final CsvMetaInfo mi : iMetaInfosRead) {
                    beanWriter.writeComment("%" + mi.parameter + ":" + mi.value);
                }
            }

            // write the beans
            try {

                for (final E element : aOutputData) {
                    beanWriter.write(element, aOutputChoose.cellProcessors);
                }

            } catch (final SecurityException e) {
                e.printStackTrace();
                return;
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            }

        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (beanWriter != null) {
                try {
                    beanWriter.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String getMetaInformation(List<CsvMetaInfo> aContainer, String aParameter) {
        for (final CsvMetaInfo mi : aContainer) {
            if (mi.parameter.equals(aParameter)) {
                return mi.value;
            }
        }
        return null;
    }

    /**
     * Generates OutputChoose from provided header keywords.
     * @param aHeaderKeywords - array of keywords strings
     * @return generated OutputChoose
     */
    private CsvColumnConfig generateOutputChoose(String[] aHeaderKeywords) {
        final CellProcessor c[] = new CellProcessor[aHeaderKeywords.length];

        for (int i = 0; i < c.length; i++) {
            try {
                aHeaderKeywords[i] = aHeaderKeywords[i].replace(" ", "_");
                c[i] = null;

                iClazz.getMethod("get" + aHeaderKeywords[i]);
            } catch (final NoSuchMethodException e) {
                // getProcessor from above get(MethodName) is not existing
                logger.info("Method not found: [" + "get" + aHeaderKeywords[i] + "], setting to default (ignore) setup. Class: " + iClazz.getName());
                c[i] = null;
                aHeaderKeywords[i] = null;
                continue;
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            } catch (final SecurityException e) {
                e.printStackTrace();
            }
        }

        final CsvColumnConfig occ = new CsvColumnConfig(aHeaderKeywords, c);
        logger.debug("Generated field mapping: " + occ);

        return occ;
    }

    private class CommentExtendedCSV implements CommentMatcher {
        protected CommentExtendedCSV() {}
        
        @Override
        public boolean isComment(String s) {
            // Comment style:
            // %parameter:value
            if (s.startsWith("%")) {
                final String[] pr = s.split(":");

                if (pr.length >= 2) {
                    // Skip first sign "%"
                    String param = pr[0].substring(1);
                    // Parameter value is all after ":". Trim it to remove unneeded spaces.
                    String value = s.substring(pr[0].length() + 1, s.length()).trim();
                    final CsvMetaInfo mi = new CsvMetaInfo(param, value);
                    final String currentValue = getMetaInformation(iMetaInfosRead, mi.parameter);
                    if (currentValue != null) {
                        logger.debug("MetaInfo " + mi + " added, but same parameter with value [" + value + "] already exists!");
                    }
                    iMetaInfosRead.add(mi);
                }
                return true;
            }
            return false;
        }
    }

    private void setCsvPreference(char aDelimiter) {
        final CsvPreference.Builder bld = new CsvPreference.Builder('"', aDelimiter, "\n");
        bld.skipComments(new CommentExtendedCSV());
        iCsvPreference = bld.build();
    }
}
