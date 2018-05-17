package mosaic.ia.gui;


import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;

import org.scijava.vecmath.Point3d;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.Macro_Runner;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import mosaic.ia.Analysis;
import mosaic.ia.Analysis.CmaResult;
import mosaic.ia.FileUtils;
import mosaic.ia.Potentials;
import mosaic.ia.Potentials.Potential;
import mosaic.ia.Potentials.PotentialType;


public class InteractionAnalysisGui extends InteractionAnalysisGuiBase {

    public static void main(String[] args) {
        ImagePlus image = IJ.openImage("/Users/gonciarz/4rois.tif");
        image.show();
//        OverlayCommands oc = new OverlayCommands();
//        oc.run("to");
        new InteractionAnalysisGui();
    }
    
    private Analysis iAnalysis;
    private ImagePlus iImgX;
    private ImagePlus iImgY;
    private Point3d[] iCsvX;
    private Point3d[] iCsvY;
    private ImagePlus iMaskImg;
    
    private List<Roi> iRoiData;
    
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
        if (iAnalysis == null) {
            IJ.showMessage("Error: Calculate distances first!");
            return;
        }
        int monteCarloRunsForTest = Integer.parseInt(monteCarloRuns.getText());
        double alpha = Double.parseDouble(alphaField.getText());

        iAnalysis.hypothesisTesting(monteCarloRunsForTest, alpha);
    }

    private void estimatePotential() {
        if (iAnalysis == null) {
            IJ.showMessage("Error: Calculate distances first!");
            return;
        }
        int numOfSupportPointsValue = Integer.parseInt(numOfSupportPoints.getText());
        double smoothnessValue = Double.parseDouble(smoothness.getText());
        System.out.println("Weight size changed to:" + numOfSupportPointsValue);
        System.out.println("Smoothness:" + smoothnessValue);

        int numReRuns = Integer.parseInt(reRuns.getText());
        Potential potential = Potentials.createPotential(getPotential(), iAnalysis.getMinDistance(), iAnalysis.getMaxDistance(), numOfSupportPointsValue, smoothnessValue);
        iAnalysis.setPotentialType(potential); // for the first time
        List<CmaResult> results = new ArrayList<CmaResult>();
        iAnalysis.cmaOptimization(results, numReRuns, false);
        mosaic.utils.Debug.print(results);
        if (!Interpreter.batchMode) {
            final ResultsTable rt = new ResultsTable();
            for (Analysis.CmaResult r : results) {
                rt.incrementCounter();
                if (getPotential() != PotentialType.NONPARAM) {
                    rt.addValue("Strength", r.iStrength);
                    rt.addValue("Threshold/Scale", r.iThresholdScale);
                }
                rt.addValue("Residual", r.iResidual);
            }
            rt.updateResults();
            rt.show("Results");
        }
    }
    
    enum Tabs {IMG, COORD, ROI}
    
    private void calculateDistances() {
        double gridDelta = Double.parseDouble(gridSize.getText());
        double qkernelWeight = Double.parseDouble(kernelWeightQ.getText());
        double pkernelWeight = Double.parseDouble(kernelWeightP.getText());
        float[][][] mask3d = iMaskImg != null ? imageTo3Darray(iMaskImg) : null;
        
        if (getTabType() == Tabs.COORD) {
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
                System.out.println("Boundary (x/y/z): " + xmin + " - " + xmax + "; " + ymin + " - " + ymax + "; " + zmin + " - " + zmax);
                iAnalysis = new Analysis();
                iAnalysis.calcDist(gridDelta, qkernelWeight, pkernelWeight, mask3d, iCsvX, iCsvY, xmin, xmax, ymin, ymax, zmin, zmax);
            }
            else {
                IJ.showMessage("Load X and Y coordinates first.");
            }
        }
        else if (getTabType() == Tabs.IMG) {
            // image
            if (iImgY != null && iImgX != null) {
                if (!checkIfImagesAreSameSize()) {
                    IJ.showMessage("Error: Image sizes/scale/unit do not match");
                }
                else {
                    iAnalysis = new Analysis();
                    iAnalysis.calcDist(gridDelta, qkernelWeight, pkernelWeight, mask3d, iImgX, iImgY);
                }
            }
            else {
                IJ.showMessage("Load X and Y images first.");
            }
        }
        else if (getTabType() == Tabs.ROI) {
            System.out.println("ROI:" + xRois.getSelectedValue() + " " + yRois.getSelectedValue());
            if (xRois.getSelectedIndex() >= 0 && yRois.getSelectedIndex() >= 0 && !xRois.getSelectedValue().equals(yRois.getSelectedValue())) {
                readSelectedRois();
                
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
                System.out.println("X=" + iCsvX.length + " Y=" + iCsvY.length);
                System.out.println("Boundary (x/y/z): " + xmin + " - " + xmax + "; " + ymin + " - " + ymax + "; " + zmin + " - " + zmax);
                
                iAnalysis = new Analysis();
                iAnalysis.calcDist(gridDelta, qkernelWeight, pkernelWeight, mask3d, iCsvX, iCsvY, xmin, xmax, ymin, ymax, zmin, zmax);                
            }
        }
        else {
            throw new RuntimeException("Unknown tab chosen in IA GUI");
        }
    }

    @Override
    public void roiInitChosen() {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp != null) {
            xRois.removeAll();
            yRois.removeAll();
            
            Set<String> types = new HashSet<>();
            
            // Try get ROIs from overlay
            Overlay overlay = imp.getOverlay();
            if (overlay != null) {
                iRoiData = new ArrayList<>();
                for (int i=0; i< overlay.size(); ++i) {
                    types.add(overlay.get(i).getName());
                    iRoiData.add(overlay.get(i));
                }
                xRois.setListData(types.toArray(new String[] {}));
                yRois.setListData(types.toArray(new String[] {}));
            }
            else {
                // if overlay is empty then try from RoiManager
                RoiManager rm = RoiManager.getInstance2();
                if (rm != null) {
                    iRoiData = new ArrayList<>();
                    for (int i = 0; i < rm.getCount(); ++i) {
                        types.add(rm.getRoi(i).getName());
                        iRoiData.add(rm.getRoi(i));
                    }
                    xRois.setListData(types.toArray(new String[] {}));
                    yRois.setListData(types.toArray(new String[] {}));
                }
            }
        }
    }
    
    /**
     * Initialiez "LoadROI" tab with data provided by user
     * @param rois map names of data sets and arrays of points
     */
    public void initRoi(Map<String, Point3d[]> rois) {
        ArrayList<String> names = new ArrayList<>();
        iRoiData = new ArrayList<>();
        for (Map.Entry<String, Point3d[]> e : rois.entrySet()) {
            names.add(e.getKey());
            for (Point3d p : e.getValue()) {
                Roi r = new Roi(p.x, p.y, 1, 1);
                r.setName(e.getKey());
                iRoiData.add(r);
            }
        }
        xRois.setListData(names.toArray(new String[] {}));
        yRois.setListData(names.toArray(new String[] {}));
        
        tabbedPane.setSelectedIndex(2);      
    }
    
    public void readSelectedRois() {
        List<Point3d> x = new ArrayList<Point3d>();
        List<Point3d> y = new ArrayList<Point3d>();
        
        for (int i = 0; i<iRoiData.size(); i++) {
            Roi roi = iRoiData.get(i);
            Rectangle bounds = roi.getBounds();
            double xp = bounds.getCenterX();
            double yp = bounds.getCenterY();
            double zp = roi.getZPosition();
            if (roi.getName().equals(xRois.getSelectedValue())) {
                x.add(new Point3d(xp, yp, zp));
            }
            else if (roi.getName().equals(yRois.getSelectedValue())) {
                y.add(new Point3d(xp, yp, zp));
            }
        }
        iCsvX = x.toArray(new Point3d[] {});
        iCsvY = y.toArray(new Point3d[] {});
        setMinMaxCoordinates();
    }

    private Tabs getTabType() {
        if (tabbedPane.getSelectedIndex() == 1) return Tabs.COORD;
        else if (tabbedPane.getSelectedIndex() == 0) return Tabs.IMG;
        else return Tabs.ROI;
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
        System.out.println(iImgX.getWidth() + "," + iImgY.getWidth() + "," + iImgX.getHeight() + "," + iImgY.getHeight() + "," + iImgX.getStackSize() + "," + iImgY.getStackSize() + ","
                + imgxc.pixelDepth + "," + imgyc.pixelDepth + "," + imgxc.pixelHeight + "," + imgyc.pixelHeight + "," + imgxc.pixelWidth + "," + imgyc.pixelWidth + "," + imgxc.getUnit() + ","
                + imgyc.getUnit());

        return false;
    }
    
    private void generateMask() {
        if (getTabType() != Tabs.IMG) {
            IJ.showMessage("Cannot generate mask for coordinates. Load a mask instead.");
        }
        else if (iImgY != null) {
            iMaskImg = new Duplicator().run(iImgY);
            iMaskImg.show();
            new Macro_Runner().run("JAR:GenerateMask_.ijm");
            iMaskImg.changes = false;
            maskPane.setTitleAt(0, "Mask: <generated>");
        }
        else {
            IJ.showMessage("Image Y is null: Cannot generate mask");
        }
    }

    private boolean loadMask() {
        ImagePlus tempMask = FileUtils.openImage();
        if (tempMask == null) {
            IJ.showMessage("Filetype not recognized");
            return false;
        }
        else if (tempMask.getType() != ImagePlus.GRAY8) {
            IJ.showMessage("ERROR: Loaded mask not 8 bit gray");
            return false;
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
    
    
    
    static Point3d[] filter(double maxx, double maxy, Point3d[] input) {
        ArrayList<Point3d> out = new ArrayList<>(input.length);
        for (Point3d p : input) {
            if (p.x <= maxx && p.y <= maxy) {
                out.add(p);
            }
        }
        return out.toArray(new Point3d[0]);
    }
}
