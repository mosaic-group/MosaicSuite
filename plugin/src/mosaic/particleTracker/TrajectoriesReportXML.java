package mosaic.particleTracker;

import java.io.File;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import mosaic.core.detection.Particle;
import mosaic.plugins.ParticleTracker3DModular_;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class creates XML report with information:
 * <li> configuration of plugin (kernel radius, link range...)
 * <li> source frame information (resolution, number of frames...)
 * <li> detected trajectories (all trajectory analysis and data)
 * 
 */
public class TrajectoriesReportXML {
    private Document iReport;
    private ParticleTracker3DModular_ iTracker;
    
    public TrajectoriesReportXML (String aFileName, ParticleTracker3DModular_ aTracker) {
        iTracker = aTracker;
        
        try {
            // Create new xml document
            iReport = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            
            // Fill it with data
            generateReport();
            
            // Finalize and save results
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(iReport);
            StreamResult result = new StreamResult(new File(aFileName));
            transformer.transform(source, result);
            
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    private void generateReport() {
        
        Element rootElement = iReport.createElement("ParticleTracker");
        iReport.appendChild(rootElement);
        
        // NOTE: -------------------------------------------------------------------------
        // To add css support just uncomment following lines. Of course first css must be 
        // created to nicely present xml content.
        // -------------------------------------------------------------------------------
        // Node css = iReport.createProcessingInstruction
        //         ("xml-stylesheet", "type=\"text/css\" href=\"report.css\"");
        // iReport.insertBefore(css, rootElement);
        
        generateConfiguration(rootElement);
        generateFramesInfo(rootElement);
        generateTrajectoriesInfo(rootElement);
    }
    
    private void generateConfiguration(Element aParent) {
        Element conf = addElement(aParent, "Configuration");
        
        addElementWithAttr(conf, "KernelRadius", "value", iTracker.getRadius());
        addElementWithAttr(conf, "CutoffRadius", "value", iTracker.getCutoffRadius());
        addElementWithAttr(conf, "Threshold", "mode", iTracker.getThresholdMode(), "value", iTracker.getThresholdValue());
        addElementWithAttr(conf, "Displacement", "value", iTracker.displacement);
        addElementWithAttr(conf, "Linkrange", "value", iTracker.linkrange);
    }
    
    private void generateFramesInfo(Element aParent) {
        Element conf = addElement(aParent, "FramesInfo");
        
        addElementWithAttr(conf, "Width", "value", iTracker.getWidth());
        addElementWithAttr(conf, "Height", "value", iTracker.getHeight());
        addElementWithAttr(conf, "NumberOfSlices", "value", iTracker.getNumberOfSlices());
        addElementWithAttr(conf, "NumberOfFrames", "value", iTracker.getNumberOfFrames());
        addElementWithAttr(conf, "GlobalMinimum", "value", iTracker.getGlobalMinimum());
        addElementWithAttr(conf, "GlobalMaximum", "value", iTracker.getGlobalMaximum());
    }
    
    private void generateTrajectoriesInfo(Element aParent) {
        Element traj = addElement(aParent, "Trajectories");
        
        Iterator<Trajectory> iter = iTracker.all_traj.iterator();
        while (iter.hasNext()) {
            addTrajectory(traj, iter.next());
        }
    }
    
    private void addTrajectory(Element aParent, Trajectory aTrajectory) {
        Element traj = addElementWithAttr(aParent, "Trajectory", "ID", aTrajectory.serial_number);
        
        generateTrajectoryAnalysis(traj, aTrajectory);
        generateTrajectoryData(traj, aTrajectory);
    }

    private void generateTrajectoryData(Element aParent, Trajectory aTrajectory) {
        Element trajData = addElement(aParent, "TrajectoryData");
        
        for (Particle p : aTrajectory.existing_particles) {
            Element frame = addElementWithAttr(trajData, "Frame", "number", p.getFrame());

            Element coordinates = addElement(frame, "Coordinates");
            coordinates.setAttribute("x", "" + p.getx());
            coordinates.setAttribute("y", "" + p.gety());
            coordinates.setAttribute("z", "" + p.getz());
            
            Element intensity = addElement(frame, "IntensityMoments");
            intensity.setAttribute("m0", "" + p.m0);
            intensity.setAttribute("m1", "" + p.m1);
            intensity.setAttribute("m2", "" + p.m2);
            intensity.setAttribute("m3", "" + p.m3);
            intensity.setAttribute("m4", "" + p.m4);
            
            addElementWithAttr(frame, "NonParticleDiscriminationScore", "value", p.score);
        }
    }

    private void generateTrajectoryAnalysis(Element aParent, Trajectory aTrajectory) {
        Element trajAnalysis = addElement(aParent, "TrajectoryAnalysis");
        TrajectoryAnalysis ta = new TrajectoryAnalysis(aTrajectory);
        if (ta.calculateAll() == TrajectoryAnalysis.SUCCESS) {
            addElementWithAttr(trajAnalysis, "MSS", "slope", "" + ta.getMSSlinear(), "yAxisIntercept", "" + ta.getMSSlinearY0());
            addElementWithAttr(trajAnalysis, "MSD", "slope", "" + ta.getGammasLogarithmic()[1], "yAxisIntercept", "" + ta.getGammasLogarithmicY0()[1]);
            addElementWithAttr(trajAnalysis, "DiffusionCoefficient", "D2", "" + ta.getDiffusionCoefficients()[1]);
        }
        else {
            addElementWithAttr(trajAnalysis, "MSS", "slope", "", "yAxisIntercept", "");
            addElementWithAttr(trajAnalysis, "MSD", "slope", "", "yAxisIntercept", "");
            addElementWithAttr(trajAnalysis, "DiffusionCoefficient", "D2", "");
        }
    }
    
    private Element addElement(Element aParent, String aName) {
        Element el = iReport.createElement(aName);
        aParent.appendChild(el);
        return el;
    }
    
    private Element addElementWithAttr(Element aParent, String aName, String aAttribute, int aValue) {
        return addElementWithAttr(aParent, aName, aAttribute, "" + aValue);
    }
    
    private Element addElementWithAttr(Element aParent, String aName, String aAttribute, double aValue) {
        return addElementWithAttr(aParent, aName, aAttribute, "" + aValue);
    }
        
    private Element addElementWithAttr(Element aParent, String aName, String aAttribute, String aValue) {
        Element el = iReport.createElement(aName);
        el.setAttribute(aAttribute, aValue);
        aParent.appendChild(el);
        
        return el;
    }
    
    private Element addElementWithAttr(Element aParent, String aName, String aAttribute1, String aValue1, String aAttribute2, String aValue2) {
        Element el = iReport.createElement(aName);
        el.setAttribute(aAttribute1, aValue1);
        el.setAttribute(aAttribute2, aValue2);
        aParent.appendChild(el);
        
        return el;
    }
}
