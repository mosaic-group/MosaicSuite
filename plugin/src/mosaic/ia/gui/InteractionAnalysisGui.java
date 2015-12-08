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
    private static final String[] PotentialList = { "Step", "Hernquist", "Linear type 1", "Linear type 2", "Plummer", "Non-parametric" };

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
            iCsvX = FileUtils.openCsvFile("Open CSV file for image X");
            setMinMaxCoordinates();
        }
        else if (e.getSource() == loadCsvY) {
            iCsvY = FileUtils.openCsvFile("Open CSV file for image Y");
            setMinMaxCoordinates();
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
            generateMask();
        }
        else if (e.getSource() == loadMask) {
            loadMask();
        }
        else if (e.getSource() == resetMask) {
            resetMask();
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
        
        if (isCoordinatesTab()) {
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
                    IJ.showMessage("Error: Image sizes/scale/unit do not match");
                }
                else {
                    iAnalysis.calcDist(gridDelta, qkernelWeight, pkernelWeight, mask3d, iImgX, iImgY);
                }
            }
            else {
                IJ.showMessage("Load X and Y images first.");
            }
        }
    }

    private boolean isCoordinatesTab() {
        return tabbedPane.getSelectedIndex() == 1;
    }

    private void setMinMaxCoordinates() {
            double xmin = Double.MAX_VALUE;
            double ymin = Double.MAX_VALUE;
            double zmin = Double.MAX_VALUE;
            double xmax = -Double.MAX_VALUE; 
            double ymax = -Double.MAX_VALUE;
            double zmax = -Double.MAX_VALUE;
            boolean isSet = false;
            
            Point3d[][] coordinates = {iCsvX, iCsvY}; 
            for (Point3d[] coords : coordinates) {
                if (coords != null) {
                    for (Point3d p : coords) {
                        if (p.x < xmin) xmin = p.x;
                        if (p.x > xmax) xmax = p.x;
                        if (p.y < ymin) ymin = p.y;
                        if (p.y > ymax) ymax = p.y;
                        if (p.z < zmin) zmin = p.z;
                        if (p.z > zmax) zmax = p.z;
                    }
                    isSet = true;
                }
            }
            if (isSet) {
                xMin.setText(Math.floor(xmin) + "");
                xMax.setText(Math.ceil(xmax) + "");
                yMin.setText(Math.floor(ymin) + "");
                yMax.setText(Math.ceil(ymax) + "");
                zMin.setText(Math.floor(zmin) + "");
                zMax.setText(Math.ceil(zmax) + "");
            }
    }

    private boolean checkIfImagesAreSameSize() {
        final Calibration imgxc = iImgX.getCalibration();
        final Calibration imgyc = iImgY.getCalibration();
        if ((iImgX.getWidth() == iImgY.getWidth()) && (iImgX.getHeight() == iImgY.getHeight()) && 
            (iImgX.getStackSize() == iImgY.getStackSize()) && (imgxc.pixelDepth == imgyc.pixelDepth) &&
            (imgxc.pixelHeight == imgyc.pixelHeight) && (imgxc.pixelWidth == imgyc.pixelWidth) && 
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
    
    private void generateMask() {
        if (isCoordinatesTab()) {
            IJ.showMessage("Cannot generate mask for coordinates. Load a mask instead.");
        }
        else if (iImgY != null) {
            iMaskImg = new Duplicator().run(iImgY);
            iMaskImg.show();
            new Macro_Runner().run("JAR:src/mosaic/plugins/scripts/GenerateMask_.ijm");
            iMaskImg.changes = false;
            maskPane.setTitleAt(0, "Mask: <generated>");
        }
        else {
            IJ.showMessage("Image Y is null: Cannot generate mask");
        }
    }

    private boolean loadMask() {
        if (iImgY == null) {
            IJ.showMessage("ERROR: Load Image Y first");
            return false;
        }
        ImagePlus tempMask = FileUtils.openImage();
        if (tempMask == null) {
            IJ.showMessage("Filetype not recognized");
            return false;
        }
        else if (tempMask.getType() != ImagePlus.GRAY8) {
            IJ.showMessage("ERROR: Loaded mask not 8 bit gray");
            return false;
        }
        else if (!isCoordinatesTab()) {
            if (tempMask.getHeight() != iImgY.getHeight() || tempMask.getWidth() != iImgY.getWidth() || tempMask.getNSlices() != iImgY.getNSlices()) {
                IJ.showMessage("ERROR: Loaded mask size does not match with image size");
                return false;
            }
        }
    
        tempMask.show("Mask loaded" + tempMask.getTitle());
        iMaskImg = tempMask;
        iMaskImg.updateImage();
        maskPane.setTitleAt(0, "Mask: " + iMaskImg.getTitle());
        return true;
    }

    private ImagePlus loadImage(String aStatusBarMessage, JButton aLoadImgButton) {
        ImagePlus tempImg = FileUtils.openImage();
        if (tempImg == null) {
            IJ.showMessage("Cancelled/Filetype not recognized");
            return null;
        }
        tempImg.show(aStatusBarMessage);
        aLoadImgButton.setText(tempImg.getTitle());
        
        return tempImg;
    }
    
    private void resetMask() {
        iMaskImg = null;
        maskPane.setTitleAt(0, "Mask: <empty>");
    }

    private PotentialType getPotential() { 
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