package mosaic.bregman;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import ij.IJ;


public class RScript {

    private PrintWriter Script;
    private final String ObjectsC1DataFile;
    private final String ObjectsC2DataFile;
    private final String ImagesDataFile;
    private final int NumberOfGroups;
    private final int[] ImagesPerGroup;
    private final String[] GroupNames;
    private final String Ch1Name;
    private final String Ch2Name;

    public RScript(String path, String file1, String file2, String file3, int nbgroups, int[] nbimages, String[] groupnames, String ch1, String ch2) {
        try {
            Script = new PrintWriter(path + File.separator + "R_analysis.R");
        }
        catch (final Exception e) {// Catch exception if any
            System.err.println("Error creating R Script file " + e.getMessage());
        }

        ObjectsC1DataFile = file1;
        ObjectsC2DataFile = file2;
        ImagesDataFile = file3;
        NumberOfGroups = nbgroups;
        ImagesPerGroup = nbimages;
        GroupNames = groupnames;
        Ch1Name = ch1;
        Ch2Name = ch2;
    }

    public void writeScript() {

        Script.print("#R_analysis v1.5 ");
        Script.println();
        Script.println();

        Script.print("###Mandatory parameters###################################################################### ");
        Script.println();
        Script.println();

        Script.print("##file names ");
        Script.println();

        Script.print("file_1=\"" + ObjectsC1DataFile + "\"  #(objA channel)");
        Script.println();

        Script.print("file_2=\"" + ObjectsC2DataFile + "\"  #(objB channel)");
        Script.println();

        Script.print("file_3=\"" + ImagesDataFile + "\"  #(images mean results)");
        Script.println();
        Script.println();

        Script.print("#Data Properties ");
        Script.println();

        Script.print("NR=" + NumberOfGroups);
        Script.println();

        Script.print("NCR=c(");

        Script.print("" + ImagesPerGroup[0]);
        for (int i = 1; i < NumberOfGroups; i++) {
            Script.print("," + ImagesPerGroup[i]);
        }
        Script.print(") #Number of images per group (should have as many values as number of groups)");

        Script.println();
        Script.println();

        // display names
        Script.print("#display parameters ");
        Script.println();

        Script.print("objA=\"" + Ch1Name + "\" 	#ch1 name");
        Script.println();
        Script.print("objB=\"" + Ch2Name + "\" 	#ch2 name");
        Script.println();

        Script.print("ConditionsNames=c(");

        Script.print("\"" + GroupNames[0] + "\"");
        for (int i = 1; i < NumberOfGroups; i++) {
            Script.print("," + "\"" + GroupNames[i] + "\"");
        }
        Script.print(") #group names (should have as many names as number of groups)");

        Script.println();
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
            Script.flush();
            Script.close();
            scanner.close();
        }
        catch (final Exception e) {
            System.err.println("Error generating R Script " + e.getMessage());
        }
        finally {
            try {
                if (in != null) in.close();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}
