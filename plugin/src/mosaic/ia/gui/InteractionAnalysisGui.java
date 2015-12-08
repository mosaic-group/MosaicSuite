package mosaic.ia.gui;


import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.vecmath.Point3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Macro_Runner;
import ij.process.ImageProcessor;
import mosaic.ia.Analysis;
import mosaic.ia.Analysis.Result;
import mosaic.ia.Potential;
import mosaic.ia.Potential.PotentialType;
import mosaic.ia.utils.FileUtils;


public class InteractionAnalysisGui extends InteractionAnalysisGuiBase {

    private Analysis iAnalysis;
    
    private ImagePlus iImgX, iImgY;
    private Point3d[] iCsvX, iCsvY;
    private ImagePlus iMaskImg;
    
    // Order must be same as declared in PotentialType enum
    protected static final String[] PotentialList = { "Step", "Hernquist", "Linear type 1", "Linear type 2", "Plummer", "Non-parametric" };

    public InteractionAnalysisGui() {
        super(PotentialList);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == help) {
            new InteractionAnalysisHelpGui(0, 0);
        }
        else if (e.getSource() == loadImgX) {
            iImgX = loadImage("Image X", loadImgX);
        }
        else if (e.getSource() == loadImgY) {
            iImgY = loadImage("Image Y", loadImgY);
        }
        else if (e.getSource() == loadCsvX) {
            iCsvX = loadCsv("Open CSV file for image X");
        }
        else if (e.getSource() == loadCsvY) {
            iCsvY = loadCsv("Open CSV file for image Y");
        }
        else if (e.getSource() == potentialComboBox) {
            enableNoparamControls(getPotential() == PotentialType.NONPARAM ? true : false);
        }
        else if (e.getSource() == calculateDistances) {
            calculateDistances();
        }
        else if (e.getSource() == estimate) {
            estimatePotential();
        }
        else if (e.getSource() == testHypothesis) {
            testHypothesis();
        }
        else if (e.getSource() == generateMask) {
            try {
                if (tabbedPane.getSelectedIndex() == 1) {
                    IJ.showMessage("Cannot generate mask for coordinates. Load a mask instead");
                    return;
                }

                if (!generateMask()) {
                    IJ.showMessage("Image Y is null: Cannot generate mask");
                }
                else {
                    maskPane.setTitleAt(0, "Mask: <generated>");
                }
            }
            catch (final NullPointerException npe) {
                IJ.showMessage("Image Y is null: Cannot generate mask");
            }
        }
        else if (e.getSource() == loadMask) {
            if (loadMask() == true) {
                maskPane.setTitleAt(0, "Mask: " + getMaskTitle());
            }
        }
        else if (e.getSource() == resetMask) {
            resetMask();
            maskPane.setTitleAt(0, "Mask: <empty>");
        }
    }

    private void testHypothesis() {
        int monteCarloRunsForTest = Integer.parseInt(monteCarloRuns.getText());
        double alpha = Double.parseDouble(alphaField.getText());

        if (!iAnalysis.hypothesisTesting(monteCarloRunsForTest, alpha)) {
            IJ.showMessage("Error: Run estimation first");
        }
    }

    private void estimatePotential() {
        Potential.NONPARAM_WEIGHT_SIZE = Integer.parseInt(numOfSupportPoints.getText());
        Potential.NONPARAM_SMOOTHNESS = Double.parseDouble(smoothness.getText());
        System.out.println("Weight size changed to:" + Potential.NONPARAM_WEIGHT_SIZE);
        System.out.println("Smoothness:" + Potential.NONPARAM_SMOOTHNESS);

        int numReRuns = Integer.parseInt(reRuns.getText());
        PotentialType potentialType = getPotential();
        System.out.println("Estimating with potential type:" + potentialType);
        iAnalysis.setPotentialType(potentialType); // for the first time
        List<Result> results = new ArrayList<Result>();
        if (!iAnalysis.cmaOptimization(results, numReRuns)) {
            IJ.showMessage("Error: Calculate distances first!");
        }
        else {
            if (!Interpreter.batchMode) {
                final ResultsTable rt = new ResultsTable();
                for (Analysis.Result r : results) {
                    rt.incrementCounter();
                    if (potentialType != PotentialType.NONPARAM) {
                        rt.addValue("Strength", r.iStrength);
                        rt.addValue("Threshold/Scale", r.iThresholdScale);
                    }
                    rt.addValue("Residual", r.iResidual);
                }
                rt.updateResults();
                rt.show("Results");
            }
        }
    }

    private void calculateDistances() {
        double gridDelta = Double.parseDouble(gridSize.getText());
        double qkernelWeight = Double.parseDouble(kernelWeightQ.getText());
        double pkernelWeight = Double.parseDouble(kernelWeightP.getText());
        iAnalysis = new Analysis();
        float[][][] mask3d = iMaskImg != null ? imageTo3Darray(iMaskImg) : null;
        if (tabbedPane.getSelectedIndex() == 1) {
            // coordinates
            double xmin = Double.parseDouble(xMin.getText());
            double ymin = Double.parseDouble(yMin.getText());
            double zmin = Double.parseDouble(zMin.getText());
            double xmax = Double.parseDouble(xMax.getText());
            double ymax = Double.parseDouble(yMax.getText());
            double zmax = Double.parseDouble(zMax.getText());
            if (xmax < xmin || ymax < ymin || zmax < zmin) {
                IJ.showMessage("Error: boundary values are not correct");
                return;
            }
            
            if (iCsvX != null && iCsvY != null) {
                iAnalysis = new Analysis();
                System.out.println("[Point3d] p set to:" + pkernelWeight);
                System.out.println("Boundary:" + xmin + "," + xmax + ";" + ymin + "," + ymax + ";" + zmin + "," + zmax);
                iAnalysis.calcDist(gridDelta, qkernelWeight, pkernelWeight, mask3d, iCsvX, iCsvY, xmin, xmax, ymin, ymax, zmin, zmax);
            }
            else {
                IJ.showMessage("Load X and Y coordinates first.");
            }
        }
        else {
            // image
            if (iImgY != null && iImgX != null) {
                if (!checkIfImagesAreSameSize()) {
                    System.out.println("Distance calc: different image sizes");
                    IJ.showMessage("Error: Image sizes/scale/unit do not match");
                }
                else {
                    iAnalysis = new Analysis();
                    System.out.println("[ImagePlus] p set to:" + pkernelWeight);
                    System.out.println("X: " + iImgX.toString());
                    System.out.println("Y: " + iImgY.toString());
                    System.out.println("Mask: " + iMaskImg.toString());
                    System.out.println(mask3d.length + " " + mask3d[0].length + " " + mask3d[0][0].length);
                    iAnalysis.calcDist(gridDelta, qkernelWeight, pkernelWeight, mask3d, iImgX, iImgY);
                }
            }
            else {
                IJ.showMessage("Load X and Y images first.");
            }
        }
    }

    private Point3d[] loadCsv(String aDialogTitle) {
        Point3d[] coords = FileUtils.openCsvFile(aDialogTitle);
        setMinMaxCoordinates();
        return coords;
    }

    private void setMinMaxCoordinates() {
            double x1 = Double.MAX_VALUE;
            double y1 = Double.MAX_VALUE;
            double z1 = Double.MAX_VALUE;
            double x2 = -Double.MAX_VALUE; 
            double y2 = -Double.MAX_VALUE;
            double z2 = -Double.MAX_VALUE;
            boolean isSet = false;
            
            Point3d[][] coordinates = {iCsvX, iCsvY}; 
            for (Point3d[] coords : coordinates) {
                if (coords != null) {
                    for (Point3d p : coords) {
                        if (p.x < x1) x1 = p.x;
                        if (p.x > x2) x2 = p.x;
                        if (p.y < y1) y1 = p.y;
                        if (p.y > y2) y2 = p.y;
                        if (p.z < z1) z1 = p.z;
                        if (p.z > z2) z2 = p.z;
                    }
                    isSet = true;
                }
            }
            if (isSet) {
                xMin.setText(Math.floor(x1) + "");
                xMax.setText(Math.ceil(x2) + "");
                yMin.setText(Math.floor(y1) + "");
                yMax.setText(Math.ceil(y2) + "");
                zMin.setText(Math.floor(z1) + "");
                zMax.setText(Math.ceil(z2) + "");
            }
    }

    private boolean checkIfImagesAreSameSize() {
        final Calibration imgxc = iImgX.getCalibration();
        final Calibration imgyc = iImgY.getCalibration();
        if ((iImgX.getWidth() == iImgY.getWidth()) && 
            (iImgX.getHeight() == iImgY.getHeight()) && 
            (iImgX.getStackSize() == iImgY.getStackSize()) && 
            (imgxc.pixelDepth == imgyc.pixelDepth) &&
            (imgxc.pixelHeight == imgyc.pixelHeight) && 
            (imgxc.pixelWidth == imgyc.pixelWidth) && 
            (imgxc.getUnit().equals(imgyc.getUnit()))) 
        {
            return true;
        }
        else {
            System.out.println(iImgX.getWidth() + "," + iImgY.getWidth() + "," + iImgX.getHeight() + "," + iImgY.getHeight() + "," + iImgX.getStackSize() + "," + iImgY.getStackSize() + ","
                    + imgxc.pixelDepth + "," + imgyc.pixelDepth + "," + imgxc.pixelHeight + "," + imgyc.pixelHeight + "," + imgxc.pixelWidth + "," + imgyc.pixelWidth + "," + imgxc.getUnit() + ","
                    + imgyc.getUnit());

            return false;
        }
    }
    
    public boolean generateMask() {
        if (iImgY != null) {
            final ImagePlus genMaskIP = new Duplicator().run(iImgY);
            genMaskIP.show();
            new Macro_Runner().run("JAR:src/mosaic/plugins/scripts/GenerateMask_.ijm");
            genMaskIP.changes = false;
            System.out.println("Generated mask: " + genMaskIP.getType());
            iMaskImg = genMaskIP;
            return true;
        }
        return false;
    }

    public boolean loadMask() {
        ImagePlus tempMask = FileUtils.openImage();
        if (tempMask == null) {
            IJ.showMessage("Filetype not recognized");
            return false;
        }
        else if (tempMask.getType() != ImagePlus.GRAY8) {
            IJ.showMessage("ERROR: Loaded mask not 8 bit gray");
            return false;
        }
        else if (!(tabbedPane.getSelectedIndex() == 1)) {
            if (tempMask.getHeight() != iImgY.getHeight() || tempMask.getWidth() != iImgY.getWidth() || tempMask.getNSlices() != iImgY.getNSlices()) {
                IJ.showMessage("ERROR: Loaded mask size does not match with image size");
                return false;
            }
        }
    
        tempMask.show("Mask loaded" + tempMask.getTitle());
        iMaskImg = tempMask;
        iMaskImg.updateImage();
        return true;
    }

    ImagePlus loadImage(String aTitle, JButton aLoadImgButton) {
        ImagePlus tempImg = FileUtils.openImage();
        if (tempImg == null) {
            IJ.showMessage("Cancelled/Filetype not recognized");
            return null;
        }
        tempImg.show(aTitle);
        aLoadImgButton.setText(tempImg.getTitle());
        
        return tempImg;
    }
    
    public boolean resetMask() {
        iMaskImg = null;
        return true;
    }

    public String getMaskTitle() {
        return iMaskImg.getTitle();
    }

    public PotentialType getPotential() { 
        final String selected = PotentialList[potentialComboBox.getSelectedIndex()];
        PotentialType pt = PotentialType.values()[Arrays.asList(PotentialList).indexOf(selected)];
        System.out.println("Selected potential: [" + selected + "] / " + pt);
        
        return pt;
    }
    
    private static float[][][] imageTo3Darray(ImagePlus image) {
        final ImageStack is = image.getStack();
        final float[][][] image3d = new float[is.getSize()][is.getWidth()][is.getHeight()];

        for (int k = 0; k < is.getSize(); k++) {
            ImageProcessor imageProc = is.getProcessor(k + 1);
            image3d[k] = imageProc.getFloatArray();
        }
        
        return image3d;
    }
}
