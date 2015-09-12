package mosaic.io.serialize;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

/**
 * Class for storing objects using standard serialization mechanism.
 * @param <T> type of object to (un)serialize
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class SerializedDataFile<T extends Serializable> implements DataFile<T> {
    
    protected static final Logger logger = Logger.getLogger(SerializedDataFile.class);

    @Override
    public boolean SaveToFile(String aSerializedFileName, T aObject2Save) {
        logger.debug("SaveToFile ["+ aSerializedFileName +"]");
        FileOutputStream fileOutput = null;
        ObjectOutputStream objectOutput = null;
        try {
            fileOutput = new FileOutputStream(aSerializedFileName);
            objectOutput = new ObjectOutputStream(fileOutput);
            objectOutput.writeObject(aObject2Save);
            return true;
        } catch (FileNotFoundException e) {
            logger.debug("File [" + aSerializedFileName + "] cannot be written!");
            logger.error(ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
            logger.error("An error occured during writing serialized file [" + aSerializedFileName + "]");
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            try {
                if (objectOutput != null) objectOutput.close();
                if (fileOutput != null) fileOutput.close();
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked") // Needed for (T) casting
    @Override
    public T LoadFromFile(String aSerializedFileName, Class<T> aClazz) {
        logger.debug("LoadFromFile [" + aSerializedFileName + "]");

        FileInputStream fileInput = null;
        ObjectInputStream objectInput = null;

        try {
            fileInput = new FileInputStream(aSerializedFileName);
            objectInput = new ObjectInputStream(fileInput);
            Object readObj = null;
            T obj;
            try {
                readObj = objectInput.readObject();
                obj = aClazz.cast(readObj);
            } catch (ClassCastException e) {
                String readObjName = (readObj == null) ? "null" : readObj.getClass().getName();
                logger.error("Different type of object read [" + readObjName + "] vs. [" + aClazz.getName() + "]");
                logger.error(ExceptionUtils.getStackTrace(e));
                return null;
            }
            return obj;
        } catch (FileNotFoundException e) {
            logger.debug("File [" + aSerializedFileName + "] not found.");
            return null;
        } catch (Exception e) {
            logger.error("An error occured during reading serialized file [" + aSerializedFileName + "]");
            logger.error(ExceptionUtils.getStackTrace(e));
            return null;
        } finally {
            try {
                if (objectInput != null) objectInput.close();
                if (fileInput != null) fileInput.close();
            } catch (IOException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }
}
