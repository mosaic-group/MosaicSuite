package mosaic.bregman;

import java.io.File;
import java.util.Set;

import mosaic.utils.SysOps;

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
    
    public static void moveFilesToOutputDirs(Set<FileInfo> aSavedFiles, String aOutputDir) {
        for (FileInfo fi : aSavedFiles) {
            // TODO: currently stick to old directory naming but should be changed to sth better
            String name = "_" + fi.name.substring(fi.name.indexOf(fi.type.baseName()));
            final File dirName = new File(aOutputDir + File.separator + name);
            SysOps.moveFileToDir(new File(fi.name), dirName, true, true);
        }
    }

}
