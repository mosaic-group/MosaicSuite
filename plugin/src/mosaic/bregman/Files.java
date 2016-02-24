package mosaic.bregman;


public class Files {

    // This is the output local
    public final static String outSuffixesLocal[] = {"*_ObjectsData_c1.csv", "*_ObjectsData_c2.csv", 
                                          "*_mask_c1.zip", "*_mask_c2.zip", 
                                          "*_ImagesData.csv", 
                                          "*_outline_overlay_c1.zip", "*_outline_overlay_c2.zip",
                                          "*_intensities_c1.zip", "*_intensities_c2.zip", 
                                          "*_seg_c1.zip", "*_seg_c2.zip", 
                                          "*_soft_mask_c1.tiff", "*_soft_mask_c2.tiff", 
                                          "*_coloc.zip" };
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
    public enum Type {
        Outline("_outline_overlay_c", "zip", true), 
        Intensity("_intensities_c", "zip", true), 
        Segmentation("_seg_c", "zip", true), 
        Mask("_mask_c", "zip", true), 
        SoftMask("_soft_mask_c", "tiff", true), 
        Colocalization("_coloc", "zip", false), 
        ObjectsData("_ObjectsData_c", "csv", true), 
        ImagesData("_ImagesData", "csv", false);
        
        public String suffix;
        public String ext;
        public boolean hasChannelInfo;
        Type(String aSuffix, String aExt, boolean aHasChannelInfo) {suffix = aSuffix; ext = aExt; hasChannelInfo = aHasChannelInfo;}
        
        @Override
        public String toString() {return "[" + name() + ", "+ suffix + " / " + ext + "]";}
    }
    
    public static class FileInfo {
        FileInfo(Type aType, String aName) {type = aType; name = aName;}
        Type type;
        String name;
        
        @Override
        public String toString() {return "[" + type + ", " + name + "]";}
    }
    
    public static String createTitle(Type aType, String aTitle) {
        if (aType.hasChannelInfo) throw new RuntimeException("Channel info not provided! [" + aTitle + " " + aType +"]");
        return aTitle + aType.suffix;
    }

    public static String createTitle(Type aType, String aTitle, int aChannel) {
        if (!aType.hasChannelInfo) throw new RuntimeException("Channel provided but this type does not require it! [" + aTitle + " " + aType +"] + " + aChannel + "]");
        return aTitle + aType.suffix + aChannel;
    }
    
    public static String createTitleWithExt(Type aType, String aTitle) {
        return addExt(aType, createTitle(aType, aTitle));
    }

    public static String createTitleWithExt(Type aType, String aTitle, int aChannel) {
        return addExt(aType, createTitle(aType, aTitle, aChannel));
    }
    
    private static String addExt(Type aType, String aTitle) {
        return aTitle + "." + aType.ext;
    }
}
