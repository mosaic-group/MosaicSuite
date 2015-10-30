package mosaic.utils.io.serialize;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class for serializing objects using JSON mechanism.
 * @param <T> type of object to (un)serialize
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class JsonDataFile<T> implements DataFile<T> {

    private static final Logger logger = Logger.getLogger(JsonDataFile.class);

    @Override
    public boolean SaveToFile(String aSerializedFileName, T aObject2Save) {
        logger.debug("SaveToFile ["+ aSerializedFileName +"]");

        // Generates nicer json string than just "new Gson();" would do
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String json = gson.toJson(aObject2Save);

        FileOutputStream fileOutput = null;

        try {
            fileOutput = new FileOutputStream(aSerializedFileName);
            fileOutput.write(json.getBytes());
            return true;
        } catch (final FileNotFoundException e) {
            logger.debug("File [" + aSerializedFileName + "] cannot be written!");
            logger.error(ExceptionUtils.getStackTrace(e));
        } catch (final IOException e) {
            logger.error("An error occured during writing json file [" + aSerializedFileName + "]");
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            try {
                if (fileOutput != null) {
                    fileOutput.close();
                }
            } catch (final IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return false;
    }

    @Override
    public T LoadFromFile(String aSerializedFileName, Class<T> aClazz) {
        logger.debug("LoadFromFile [" + aSerializedFileName + "]");

        final Gson gson = new Gson();

        try {
            final FileInputStream fileInput = new FileInputStream(aSerializedFileName);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(fileInput));
            final StringBuilder json = new StringBuilder(); //Holds the text
            String aDataRow = "";
            while ((aDataRow = reader.readLine()) != null) {
                json.append(aDataRow);
            }
            reader.close();

            final T obj = gson.fromJson(json.toString(), aClazz);

            return obj;
        } catch (final FileNotFoundException e) {
            logger.debug("File [" + aSerializedFileName + "] not found.");
            return null;
        } catch (final Exception e) {
            logger.error("An error occured during reading json file [" + aSerializedFileName + "]");
            logger.error(ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    @Override
    public T LoadFromFile(String aSerializedFileName, Class<T> aClazz, T aDefaultValue) {
        final T temp = LoadFromFile(aSerializedFileName, aClazz);
        if (temp != null) {
            return temp;
        }
        
        return aDefaultValue;
    }
}
