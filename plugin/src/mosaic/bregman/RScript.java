package mosaic.bregman;


import ij.IJ;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;


class RScript {

    PrintWriter Script;
    String ObjectsC1DataFile;
    String ObjectsC2DataFile;
    String ImagesDataFile;
    int NumberOfGroups;
    int[] ImagesPerGroup;
    String[] GroupNames;
    String Ch1Name;
    String Ch2Name;

    // generate file

    // parameters data

    public RScript(String path, String file1, String file2, String file3, int nbgroups, int[] nbimages, String[] groupnames, String ch1, String ch2) {
        try {
            Script = new PrintWriter(path + File.separator + "R_analysis.R");
        }
        catch (Exception e) {// Catch exception if any
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

        try {
            // works when plugin running from .jar plugin file
            IJ.log("RSCRIPT..........");
            InputStream in = this.getClass().getResourceAsStream("/src/mosaic/plugins/scripts/Rscript.r");
            Scanner scanner = new Scanner(in);
            String content = scanner.useDelimiter("\\Z").next();

            Script.print(content);
            Script.flush();
            Script.close();
            scanner.close();
        }
        catch (Exception e) {
            System.err.println("Error generating R Script " + e.getMessage());
        }

    }

}
