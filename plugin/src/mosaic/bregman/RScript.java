package mosaic.bregman;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import ij.IJ;


public class RScript {

    public RScript(String path, String ObjectsC1DataFile, String ObjectsC2DataFile, String ImagesDataFile, int NumberOfGroups, int[] ImagesPerGroup, String[] GroupNames, String Ch1Name, String Ch2Name) {
        try {
            PrintWriter Script = new PrintWriter(path + File.separator + "R_analysis.R");
            Script.println("#R_analysis v1.5 ");
            Script.println();
            
            Script.println("###Mandatory parameters###################################################################### ");
            Script.println();
            
            Script.println("##file names ");
            Script.println("file_1=\"" + ObjectsC1DataFile + "\"  #(objA channel)");
            Script.println("file_2=\"" + ObjectsC2DataFile + "\"  #(objB channel)");
            Script.println("file_3=\"" + ImagesDataFile + "\"  #(images mean results)");
            Script.println();
            
            Script.println("#Data Properties ");
            Script.println("NR=" + NumberOfGroups);
            
            Script.print("NCR=c(");
            Script.print("" + ImagesPerGroup[0]);
            for (int i = 1; i < NumberOfGroups; i++) {
                Script.print("," + ImagesPerGroup[i]);
            }
            Script.println(") #Number of images per group (should have as many values as number of groups)");
            Script.println();
            
            // display names
            Script.println("#display parameters ");
            Script.println("objA=\"" + Ch1Name + "\"         #ch1 name");
            Script.println("objB=\"" + Ch2Name + "\"         #ch2 name");
            
            Script.print("ConditionsNames=c(");
            Script.print("\"" + GroupNames[0] + "\"");
            for (int i = 1; i < NumberOfGroups; i++) {
                Script.print("," + "\"" + GroupNames[i] + "\"");
            }
            Script.println(") #group names (should have as many names as number of groups)");
            Script.println();
            
            Script.flush();
            
            InputStream in = null;
            try {
                in = getClass().getResourceAsStream("Rscript.r");
                if (in == null) {
                    IJ.log("RSCRIPT generation has not succeed (cannot find Rscript.r resource)");
                }
                final Scanner scanner = new Scanner(in);
                final String content = scanner.useDelimiter("\\Z").next();
                
                Script.print(content);
                scanner.close();
            }
            catch (final Exception e) {
                System.err.println("Error generating R Script " + e.getMessage());
            }
            finally {
                Script.flush();
                Script.close();
                try {
                    if (in != null) in.close();
                }
                catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (final Exception e) {// Catch exception if any
            System.err.println("Error creating R Script file " + e.getMessage());
        }
    }
}
