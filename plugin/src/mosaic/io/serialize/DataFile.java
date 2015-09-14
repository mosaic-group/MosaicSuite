package mosaic.io.serialize;


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
}
