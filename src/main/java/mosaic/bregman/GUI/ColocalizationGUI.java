package mosaic.bregman.GUI;


import java.awt.Checkbox;
import java.awt.Font;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import mosaic.bregman.Mask;
import mosaic.bregman.Parameters;
import mosaic.utils.ArrayOps.MinMax;
import mosaic.utils.Debug;
import mosaic.utils.ImgUtils;


public class ColocalizationGUI implements ItemListener, ChangeListener, TextListener {

    private ImagePlus iInputImage;
    private final Parameters iParameters;

    private boolean fieldval = false;
    private boolean sliderval = false;
    private boolean refreshing = false;

    private final double MinSLiderRange = 0.001;
    private final double MaxSliderRange = 1;
    private final double LogMinSliderRange = Math.log10(MinSLiderRange);
    private final double LogSliderSpan = Math.log10(MaxSliderRange) - Math.log10(MinSLiderRange);
    private final int MaxSliderSteps = 1000;
    
    List<Checkbox> checkboxes = new ArrayList<>();
    List<TextField> thresholdValues = new ArrayList<>();
    List<JSlider> thresholdSliders = new ArrayList<>();
    List<JSlider> zPositions = new ArrayList<>();
    int zInitialPosition = 0;
    List<ImagePlus> masks = new ArrayList<>();
    List<Boolean> masksInit = new ArrayList<>();
    List<Double> thresholds = new ArrayList<>();
    List<Boolean> enableMask = new ArrayList<>();    
    int numOfChannels = 0;
    
    
    public ColocalizationGUI(ImagePlus ch1, Parameters aParameters) {
        // If there is no input image then create dummy one just to let GUI to be created and closed (used for macro mode
        // so in reality GUI is even not shown).
        iInputImage = ch1 == null ? new ImagePlus() : ch1;
        
        iParameters = aParameters;
        numOfChannels = iInputImage.getNChannels();
    }

    public void run() {
        final Font bf = new Font(null, Font.BOLD, 12);
        final GenericDialog gd = new GenericDialog("Cell masks");
        gd.setInsets(-10, 0, 3);
        gd.addMessage("Cell masks (two channels images)", bf);        

        // TODO: parameters holds only values for two channels - here we update it for multichannle use, update parameters to hold any number of channels
        thresholds.add(iParameters.thresholdcellmask);
        thresholds.add(iParameters.thresholdcellmasky);
        for (int c = 2; c < numOfChannels; ++c) thresholds.add(0.0);
        enableMask.add(iParameters.usecellmaskX);
        enableMask.add(iParameters.usecellmaskY);
        for (int c = 2; c < numOfChannels; ++c) enableMask.add(false);

        zInitialPosition = iInputImage.getSlice();
        
        for (int channel = 1; channel <= numOfChannels; ++channel) {
            Checkbox cb = new Checkbox("Cell_mask_channel_" + channel, enableMask.get(channel - 1)); 
            checkboxes.add(cb);
            Panel p = new Panel();
            p.add(cb);
            gd.addPanel(p);
            cb.addItemListener(this);
            
            // TODO iParameters should handle multiple channels
            gd.addNumericField("Threshold_channel_" + channel + " (0 to 1)", thresholds.get(channel - 1), 4);
            TextField t = (TextField) gd.getNumericFields().lastElement();
            thresholdValues.add(t);
            t.addTextListener(this);
            
            p = new Panel();
            p.add(new JLabel("threshold value   "));
            JSlider s = new JSlider();
            p.add(s);
            gd.addPanel(p);
            thresholdSliders.add(s);
            s.setMinimum(0);
            s.setMaximum(MaxSliderSteps);
            s.setValue((int) logvalue(thresholds.get(channel - 1)));
            s.addChangeListener(this);
            
            p = new Panel();
            p.add(new JLabel("z position preview"));
            JSlider z = new JSlider();
            p.add(z);
            gd.addPanel(p);
            zPositions.add(z);
            final int nslices = iInputImage.getNSlices();
            if (nslices > 1) {
                z.setMinimum(1);
                z.setMaximum(nslices);
                z.setValue(1);
                z.addChangeListener(this);
            }
            else {
                z.setEnabled(false);
            }
            
            if (enableMask.get(channel - 1)) {
                ImagePlus mask = new ImagePlus();
                masks.add(mask);
                previewBinaryCellMask(new Double((t.getText())), iInputImage, mask, channel);
                masksInit.add(true);
            }
            else {
                masks.add(null);
                masksInit.add(false);
            }
        }
        
        gd.showDialog();
            
        // Cleanup
        for (ImagePlus ip : masks) {
            if (ip != null) ip.close();
        }
        iInputImage.setSlice(zInitialPosition);

        if (gd.wasCanceled()) return;

        // Set values in parameters
        // TODO: Make parameters able to handle n-channels
        Debug.print(enableMask);
        iParameters.usecellmaskX = enableMask.get(0);
        iParameters.usecellmaskY = enableMask.get(1);
        iParameters.thresholdcellmask = new Double((thresholdValues.get(0).getText()));
        iParameters.thresholdcellmasky =  new Double((thresholdValues.get(1).getText()));
    }

    private double expvalue(double slidervalue) {
        return (Math.pow(10, (slidervalue / MaxSliderSteps) * LogSliderSpan + LogMinSliderRange));
    }

    private double logvalue(double tvalue) {
        return (MaxSliderSteps * (Math.log10(tvalue) - LogMinSliderRange) / LogSliderSpan);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        final Object source = e.getSource(); // Identify checkbox that was clicked

        int channelIdx = -1;
        for (int i = 0; i < numOfChannels; ++i) {
            if (source == checkboxes.get(i)) {channelIdx = i; break;}
        }
        
        if (channelIdx >= 0) {
            final boolean b = checkboxes.get(channelIdx).getState();
            if (b) {
                if (masks.get(channelIdx) == null) {
                    masks.set(channelIdx, new ImagePlus());
                }
                previewBinaryCellMask(new Double((thresholdValues.get(channelIdx).getText())), iInputImage, masks.get(channelIdx), channelIdx + 1);
                masksInit.set(channelIdx, true);
                enableMask.set(channelIdx, true);
            }
            else {
                // hide and clean
                if (masks.get(channelIdx) != null) {
                    masks.get(channelIdx).hide();
                }
                masksInit.set(channelIdx, false);
                enableMask.set(channelIdx, false);
            }
        }
    }

    @Override
    public void textValueChanged(TextEvent e) {
        final Object source = e.getSource();

        int channelIdx = -1;
        for (int i = 0; i < numOfChannels; ++i) {
            if (source == thresholdValues.get(i)) {channelIdx = i; break;}
        }
        if (channelIdx >= 0 && !sliderval) {// prevents looped calls
            fieldval = true;
            TextField t = thresholdValues.get(channelIdx);
            
            double v = 0.0;
            try {
                v = new Double((t.getText()));
            }
            catch(Exception ex) {
                return;
            }
            final int vv = (int) (logvalue(v));
            JSlider ths = thresholdSliders.get(channelIdx);
            if (masksInit.get(channelIdx)) {
                if (!sliderval && !refreshing) {
                    previewBinaryCellMask(v, iInputImage, masks.get(channelIdx), channelIdx + 1);
                    ths.setValue(vv);
                }
            }
            else {
                if (!sliderval) {
                    ths.setValue(vv);
                }
            }
            
            fieldval = false;
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        final Object origin = e.getSource();
        sliderval = true;

        int channelIdx = -1;
        for (int i = 0; i < numOfChannels && thresholdSliders.size() > i; ++i) {
            if (origin == thresholdSliders.get(i)) {channelIdx = i; break;}
        }
        if (channelIdx >= 0) {
            if (!fieldval) {// prevents looped calls
                
                JSlider th = thresholdSliders.get(channelIdx);
                boolean initialized = masksInit.get(channelIdx);
                
                if (initialized && !th.getValueIsAdjusting()) {
                    final double value = th.getValue();
                    final double vv = expvalue(value);

                    if (!fieldval) {
                        thresholdValues.get(channelIdx).setText(String.format(Locale.US, "%.4f", vv));
                        previewBinaryCellMask(vv, iInputImage, masks.get(channelIdx), channelIdx + 1);
                    }
                    refreshing = false;
                }

                if ((initialized && th.getValueIsAdjusting()) || (!initialized)) {
                    final double value = th.getValue();
                    final double vv = expvalue(value);
                    if (!fieldval) {
                        thresholdValues.get(channelIdx).setText(String.format(Locale.US, "%.4f", vv));
                    }
                    refreshing = true;
                }
                
            }
        }
        
        channelIdx = -1;
        for (int i = 0; i < numOfChannels && thresholdSliders.size() > i; ++i) {
            if (origin == zPositions.get(i)) {channelIdx = i; break;}
        }
        if (channelIdx >= 0 && masksInit.get(channelIdx)) {
            JSlider z = zPositions.get(channelIdx);
            masks.get(channelIdx).setZ(z.getValue());
            iInputImage.setZ(z.getValue());
        }
        sliderval = false;
    }

    // compute and display cell mask
    private void previewBinaryCellMask(double threshold_i, ImagePlus img, ImagePlus maska_im, int channel) {
        int currentSlice = img.getSlice();
        
        MinMax<Double> mm = ImgUtils.findMinMax(img, channel, 1);
        double min = mm.getMin();
        double max = mm.getMax();
        
        double threshold = threshold_i * (max - min) + min;
        ImagePlus mask = Mask.createBinaryCellMask(img, "Cell mask channel " + channel, threshold, channel);
        maska_im.setStack(mask.getStack());
        maska_im.setTitle(mask.getTitle());
        maska_im.updateAndDraw();
        maska_im.show();
        
        // revert set slice
        img.setSlice(currentSlice);
    }
}
