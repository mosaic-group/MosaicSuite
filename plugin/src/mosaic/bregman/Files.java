package mosaic.bregman;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mosaic.utils.SysOps;
import mosaic.utils.io.csv.CSV;
import mosaic.utils.io.csv.CsvMetaInfo;

public class Files {

    // =============== OLD files definitions, will be kept as long as necessary =============================
    
    // This is the output for cluster
    public final static String outSuffixesCluster[] = {"*_ObjectsData_c1.csv", "*_ObjectsData_c2.csv", 
                                        "*_mask_c1.zip", "*_mask_c2.zip", 
                                        "*_ImagesData.csv", 
                                        "*_outline_overlay_c1.zip", "*_outline_overlay_c2.zip",
                                        "*_intensities_c1.zip", "*_intensities_c2.zip", 
                                        "*_seg_c1.zip", "*_seg_c2.zip", 
                                        "*_coloc.zip", 
                                        "*_soft_mask_c1.tiff", "*_soft_mask_c2.tiff", 
                                        "*.tif" };

    // ============= Below will be put new implementations =================
    interface BaseType {
        public String baseName();
        public String ext();
        public boolean hasChannelInfo();
    }
    
    public enum Type implements BaseType {
        Outline("_outline_overlay", "zip", true), 
        Intensity("_intensities", "zip", true), 
        Segmentation("_seg", "zip", true), 
        Mask("_mask", "zip", true), 
        SoftMask("_soft_mask", "tiff", true), 
        Colocalization("_coloc", "zip", false), 
        ObjectsData("_ObjectsData", "csv", true), 
        ImagesData("_ImagesData", "csv", false);
        
        private String baseName;
        private String ext;
        private boolean hasChannelInfo;
        
        @Override
        public String baseName() {return baseName;}
        @Override
        public String ext() {return ext;}
        @Override
        public boolean hasChannelInfo() {return hasChannelInfo;}
        
        Type(String aBaseName, String aExt, boolean aHasChannelInfo) {baseName = aBaseName; ext = aExt; hasChannelInfo = aHasChannelInfo;}
        
        @Override
        public String toString() {return "[" + name() + ", "+ baseName + " / " + ext + "]";}
    }
    
    public static class FileInfo {
        FileInfo(Type aType, String aName) {type = aType; name = aName;}
        Type type;
        String name;
        
        @Override
        public String toString() {return "[" + type + ", " + name + "]";}
    }
    
    public static String createTitle(Type aType, String aTitle) {
        if (aType.hasChannelInfo()) throw new RuntimeException("Channel info not provided! [" + aTitle + " " + aType +"]");
        return aTitle + aType.baseName();
    }

    public static String createTitle(Type aType, String aTitle, int aChannel) {
        if (!aType.hasChannelInfo()) throw new RuntimeException("Channel provided but this type does not require it! [" + aTitle + " " + aType +"], " + aChannel + "]");
        return aTitle + aType.baseName() + "_c" + aChannel;
    }
    
    public static String createTitleWithExt(Type aType, String aTitle) {
        return addExt(aType, createTitle(aType, aTitle));
    }

    public static String createTitleWithExt(Type aType, String aTitle, int aChannel) {
        return addExt(aType, createTitle(aType, aTitle, aChannel));
    }
    
    private static String addExt(Type aType, String aTitle) {
        return aTitle + "." + aType.ext();
    }
    
    public static String getTypeDir(Type aType) {
        return createTitleWithExt(aType, "_") + File.separator;
    }
    
    public static String getTypeDir(Type aType, int aChannel) {
        return createTitleWithExt(aType, "_", aChannel) + File.separator;
    }
    
    public static String getMovedFilePath(Type aType, String aTitle, int aChannel) {
        return Files.getTypeDir(aType, aChannel) + Files.createTitleWithExt(aType, aTitle, aChannel);
    }
    
    public static String getMovedFilePath(Type aType, String aTitle) {
        return Files.getTypeDir(aType) + Files.createTitleWithExt(aType, aTitle);
    }
    
    /**
     * @return full type name like "_ObjectsData_c1.csv" consisting from baseName, channel info (if exists) and extension
     */
    public static String getFullTypeName(FileInfo aFileInfo) {
        return aFileInfo.name.substring(aFileInfo.name.lastIndexOf(aFileInfo.type.baseName()));
    }
    
    /**
     * Input file:
     * /tmp/test/test2d_ObjectsData_c1.csv
     * with category: 
     * __ObjectsData_c1.csv
     * is being moved to directory:
     * /tmp/test/__ObjectsData_c1.csv/
     * to finally be at:
     * /tmp/test/__ObjectsData_c1.csv/test2d_ObjectsData_c1.csv 
     */
    public static Set<FileInfo> moveFilesToOutputDirs(Set<FileInfo> aSavedFiles, String aOutputDir) {
        Set<FileInfo> result = new LinkedHashSet<FileInfo>();
        for (FileInfo fi : aSavedFiles) {
            // TODO: currently stick to old directory naming but should be changed to sth better
            final File dirName = new File(aOutputDir + File.separator + "_" + getFullTypeName(fi));
            File fileToMove = new File(fi.name);
            SysOps.moveFileToDir(fileToMove, dirName, true, true);
            result.add(new FileInfo(fi.type, dirName + File.separator + fileToMove.getName()));
        }
        return result;
    }

    public static void stitchCsvFiles(Set<FileInfo> aSavedFiles, String aOutputDir, String aBackgroundValue) {
        // Find all files of "csv" type and categorize them
        Map<String, List<String>> m = new HashMap<String, List<String>>();
        for (FileInfo fi : aSavedFiles) {
            if (fi.type.ext().equals("csv")) {
                String typeName = getFullTypeName(fi);
                List<String> fileList = m.get(typeName);
                if (fileList == null) {
                    fileList = new ArrayList<String>();
                    m.put(typeName, fileList);
                }
                fileList.add(fi.name);
            }
        }
        
        // Go through each category of files and stitch them into one file.
        CsvMetaInfo metaInfo = (aBackgroundValue != null) ? new CsvMetaInfo("background", aBackgroundValue) : null;
        boolean firstFile = true;
        for (Entry<String, List<String>> e : m.entrySet()) {
            String[] currentFilesAbsPaths = e.getValue().toArray(new String[0]);
            Arrays.sort(currentFilesAbsPaths);
            
            // Set metainformation for csv
            final CSV<Object> csv = new CSV<Object>(Object.class);
            if (metaInfo != null) {
                csv.setMetaInformation(metaInfo);
            }
            
            // if it is the first time set the file preference from the first file
            if (firstFile == true) {
                firstFile = false;
                csv.setCSVPreferenceFromFile(currentFilesAbsPaths[0]);
            }

            csv.StitchAny(currentFilesAbsPaths,  aOutputDir + "stitch_" + e.getKey());
        }
    }
    
}
