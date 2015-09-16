package mosaic.bregman;


import ij.gui.GenericDialog;
//import ij.gui.NonBlockingGenericDialog;

import java.awt.TextField;


public class GenericDialogCustom extends GenericDialog {

    /**
     * Default serial version UID
     */
    private static final long serialVersionUID = 1L;

    public GenericDialogCustom(String title) {
        super(title);
    }

    public TextField getField(int n) {
        return (TextField) numberField.elementAt(n);
    }
}
