package mosaic.filamentSegmentation.GUI;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import mosaic.filamentSegmentation.SegmentationFunctions;
import mosaic.filamentSegmentation.SegmentationFunctions.FilamentXyCoordinates;
import mosaic.plugins.FilamentSegmentation.VisualizationLayer;
import mosaic.utils.ImgUtils;
import mosaic.utils.math.CubicSmoothingSpline;


/**
 * Generates ImagePlus with filaments draw over the original input data.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class OutputImageWindow {
    private final ImagePlus iOutputColorImg;
    private final ImagePlus iInputImg;
    
    /**
     * @param aInputImg Title for output image.
     */
    public OutputImageWindow(ImagePlus aInputImg, String aTitle) {
        iInputImg = aInputImg;
        iOutputColorImg = ImgUtils.createNewEmptyImgPlus(iInputImg, aTitle, 1, 1, ImgUtils.OutputType.RGB);     
    }
    
    /**
     * @return Returns generated ImagePlus
     */
    public ImagePlus getImagePlus() {
        return iOutputColorImg;
    }
    
    /**
     * Prepares output window with drawn filaments
     * @param iFilamentsData input data {@link mosaic.plugins.FilamentSegmentation#iFilamentsData check here for details of aFilamentsData structure}
     * @param iVisualizationLayer 
     */
    public void showFilaments(final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> iFilamentsData, VisualizationLayer iVisualizationLayer) {
        copyOrigImagesToOutputImg();
        drawFilaments(iFilamentsData, iVisualizationLayer);
    }
    
    private void drawFilaments(final Map<Integer, Map<Integer, List<CubicSmoothingSpline>>> iFilamentsData, VisualizationLayer iVisualizationLayer) {
        ImageStack stack = iOutputColorImg.getStack();
        Overlay overlay = new Overlay();
        
        for (int frame : iFilamentsData.keySet()) {
            Map<Integer, List<CubicSmoothingSpline>> ms  = iFilamentsData.get(frame);
            ImageProcessor ip = stack.getProcessor(frame);
            
            // for every image take all filaments
            for (Entry<Integer, List<CubicSmoothingSpline>> e : ms.entrySet()) {
                
                // and draw them one by one
                for (final CubicSmoothingSpline css : e.getValue()) {
                    FilamentXyCoordinates coordinates = SegmentationFunctions.generateAdjustedXyCoordinatesForFilament(css);
                    if (iVisualizationLayer == VisualizationLayer.OVERLAY) {
                        drawFilamentsOnOverlay(overlay, frame, coordinates);
                    }
                    else if (iVisualizationLayer == VisualizationLayer.IMAGE) {
                        drawFilamentsOnImage(ip, e, coordinates);
                    }
                }
            }
        }
        
        if (iVisualizationLayer == VisualizationLayer.OVERLAY) {
            iOutputColorImg.setOverlay(overlay);
        }
    }

    private void copyOrigImagesToOutputImg() {
        ImageStack stack = iOutputColorImg.getStack();
        for (int sn = 1; sn <= stack.getSize(); ++sn) {
            ImageProcessor ip = stack.getProcessor(sn);
            int noOfChannels = iInputImg.getStack().getProcessor(sn).getNChannels();
    
            if (noOfChannels != 1) {
                for (int c = 0; c <= 2; ++c) {
                    ip.setPixels(c, iInputImg.getStack().getProcessor(sn).toFloat(c, null));
                }
            }
            else {
                // In case when input image is gray then copy it to all output channels (R,G,B)
                for (int c = 0; c <= 2; ++c) {
                    ip.setPixels(c, iInputImg.getStack().getProcessor(sn).toFloat(0, null));   
                }
            }
        }
    }
    
    private void drawFilamentsOnImage(ImageProcessor aIp, Entry<Integer, List<CubicSmoothingSpline>> aFilamentsForOneImg, FilamentXyCoordinates coordinates) {
        int[] pixels = (int[]) aIp.getPixels();
        int w = aIp.getWidth(), h = aIp.getHeight();
        int color = 0;
        if (iInputImg.getStack().getProcessor(1).getNChannels() != 1) {
            // Create best color depending on channel (which is possibly RGB)
            if (aFilamentsForOneImg.getKey() == 0) color = 255 << 8;
            else if (aFilamentsForOneImg.getKey() == 1) color = 255 << 0;
            else if (aFilamentsForOneImg.getKey() == 2) color = 255 << 16;
        } 
        else {
            // gold-yellow in case of gray images
            color = (255 << 16) + (193 << 8) + 37;
        }
        for (int i = 0; i < coordinates.x.size(); ++i) {
            int xp = (int) (coordinates.x.get(i));
            int yp = (int) (coordinates.y.get(i));
            if (xp < 0 || xp >= w - 1 || yp < 0 || yp >= h - 1) continue;
            pixels[yp * w + xp] = color;
        }
    }

    private void drawFilamentsOnOverlay(Overlay aOverlay, int aSliceNumber, FilamentXyCoordinates coordinates) {
        Roi r = new PolygonRoi(coordinates.x.getArrayYXasFloats()[0], coordinates.y.getArrayYXasFloats()[0], Roi.POLYLINE);
        r.setPosition(aSliceNumber);
        aOverlay.add(r);
    }
}
