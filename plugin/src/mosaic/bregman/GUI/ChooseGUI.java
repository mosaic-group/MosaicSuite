package mosaic.bregman.GUI;


import java.io.File;
import java.util.List;

import ij.gui.GenericDialog;


public class ChooseGUI {
    /**
     * Create a Choose Window
     * @param aTitle Window title
     * @param aMessage to show in the selection window
     * @param aSelectOptions all the options
     * @return chosen String
     */
    public String chooseString(String aTitle, String aMessage, String aSelectOptions[]) {
        if (aSelectOptions.length == 0) return null;

        final GenericDialog gd = new GenericDialog(aTitle);
        gd.addChoice(aMessage, aSelectOptions, aSelectOptions[0]);
        gd.showDialog();

        if (gd.wasCanceled()) {
            return null;
        }

        return gd.getNextChoice();
    }

    /**
     * Create a Choose Window
     * @param aTitle Window title
     * @param aMessage to show in the selection window
     * @param aSelectOptions all the options
     * @return chosen File
     */
    public File chooseFile(String aTitle, String aMessage, List<File> aSelectOptions) {
        if (aSelectOptions.size() == 0) return null;

        final String ad[] = new String[aSelectOptions.size()];
        for (int i = 0; i < aSelectOptions.size(); i++) {
            ad[i] = aSelectOptions.get(i).getAbsolutePath();
        }
        
        String c = chooseString(aTitle, aMessage, ad); 
        if (c == null) return null;
        
        return new File(c);
    }
}
