package mosaic.utils.io.serialize;


public interface  DataFile <T> {
    /**
     * Serialize given object to aSerializedFileName.
     * @param aSerializedFileName
     * @param aObject2Save
     * @return true on success, false otherwise
     */
    public boolean SaveToFile(String aSerializedFileName, T aObject2Save);
    
    /**
     * Loads serialized object from given aSerializedFileName 
     * @param aSerializedFileName
     * @return object on success or null otherwise
     */
    public T LoadFromFile(String aSerializedFileName, Class<T> aClazz);
    
    /**
     * Loads serialized object from given aSerializedFileName 
     * @param aSerializedFileName
     * @param aClazz Class type
     * @param aDefaultValue - value to be returned if loading from file failed
     * @return object on success or null otherwise
     */
    public T LoadFromFile(String aSerializedFileName, Class<T> aClazz, T aDefaultValue);
}
