package mosaic.bregman;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ij.IJ;
import mosaic.bregman.ColocalizationAnalysis.ChannelPair;


public class RScript {
    public static final String ScriptName = "R_analysis.R";

    /*
     * Here is example of beginning part of script that must be generated by makeRScript method:
     * -----------------------------------------------------------------------------------------
        ### Input parameters ##########################################################
    
        # Input file names
        BaseDirecotry = "/Users/gonciarz/Documents/MOSAIC/work/tasks/SquasshRScript/"
        FileObjColoc = paste0(BaseDirecotry, "stitch__NEW_ObjectColoc.csv")
        FileObjData  = paste0(BaseDirecotry, "stitch__NEW_ObjectData.csv")
        FileImgColoc = paste0(BaseDirecotry, "stitch__NEW_ImageColoc.csv")
        
        # Group properties
        NumberOfGroups = 4
        NumberImagesPerEachGroup = c(3, 3, 3, 3) # (should have as many values as number of groups)
        GroupNames = c("Group1", "Group2", "Group3", "Group4") # (should have as many names as number of groups)
        
        # Channel properties
        NumberOfChannels = 2
        NamesOfChannels = c("ChannelA", "ChannelB")
        MinIntensitiesInChannels = c(0, 0)
        
        ChannelPairs = list(c(0,1), c(1,0))
    * -----------------------------------------------------------------------------------------
    */
    public static void makeRScript(String path, String aObjectsDataFile, String aObjectsColocFile, String aImagesColocFile, List<ChannelPair> aChannelPairs, int[] ImagesPerGroup, String[] GroupNames, String Ch1Name, String Ch2Name) {
        try {
            PrintWriter Script = new PrintWriter(path + File.separator + ScriptName);
            Script.println("#R_analysis v2.0");
            Script.println();
            
            Script.println("### Input parameters ##########################################################");
            Script.println();
            
            Script.println("# Input file names");
            Script.println("BaseDirecotry = \"" + path + "\"");
            Script.println("FileObjData = \"" + aObjectsDataFile + "\"");
            Script.println("FileObjColoc = \"" + aObjectsColocFile + "\"");
            Script.println("FileImgColoc = \"" + aImagesColocFile + "\"");
            Script.println();
            
            Script.println("# Group properties");
            Script.println("NumberOfGroups = " + GroupNames.length);
            Script.print("NumberImagesPerEachGroup = c(");
            Script.print("" + ImagesPerGroup[0]);
            for (int i = 1; i < ImagesPerGroup.length; i++) {
                Script.print("," + ImagesPerGroup[i]);
            }
            Script.println(")");
            
            Script.print("GroupNames = c(");
            Script.print("\"" + GroupNames[0] + "\"");
            for (int i = 1; i < GroupNames.length; i++) {
                Script.print("," + "\"" + GroupNames[i] + "\"");
            }
            Script.println(")");
            Script.println();
            
            List<ChannelPair> filteredPairs = filterChannelPairs(aChannelPairs);
            
            Script.println("# Channel properties");
            int numOfChannels = findNumOfChannels(filteredPairs);
            Script.println("NumberOfChannels = " + numOfChannels);
            Script.print("NamesOfChannels = c(");
            if (numOfChannels >= 1) Script.print("\"" + Ch1Name + "\"");
            if (numOfChannels >= 2) Script.print(", \"" + Ch2Name + "\"");
            for (int i = 2; i < numOfChannels; i++) {
                Script.print(", \"channel " + i + " name\"");
            }
            Script.println(")");
            Script.print("MinIntensitiesInChannels = c(");
            Script.print("0");
            for (int i = 1; i < numOfChannels; i++) {
                Script.print(", 0");
            }
            Script.println(")");
            Script.println();
            
            Script.print("ChannelPairs = list(");
            Script.print("c(" + filteredPairs.get(0).ch1 + "," + filteredPairs.get(0).ch2 + ")");
            for (int i = 1; i < filteredPairs.size(); i++) {
                Script.print(", c(" + filteredPairs.get(i).ch1 + "," + filteredPairs.get(i).ch2 + ")");
            }
            Script.println(")");
            Script.println();
            
            InputStream in = null;
            try {
                in = RScript.class.getResourceAsStream("Rscript.r");
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
    
    static private int findNumOfChannels(List<ChannelPair> aChannelPairs) {
        int max = 0;
        
        for (ChannelPair cp : aChannelPairs) {
            int tempMax = Math.max(cp.ch1, cp.ch2);
            if (tempMax > max) max = tempMax;
        }
        
        // +1 since channels are numbered from 0
        return max + 1;
    }
    
    static private List<ChannelPair> filterChannelPairs(List<ChannelPair> aChannelPairs) {
        List<ChannelPair> filteredPairs = new ArrayList<ChannelPair>();
        
        filteredPairs.addAll(aChannelPairs);
        for (ChannelPair cp : filteredPairs) {
            filteredPairs.remove(new ChannelPair(cp.ch2, cp.ch1));
        }
        
        // +1 since channels are numbered from 0
        return filteredPairs;
    }
}