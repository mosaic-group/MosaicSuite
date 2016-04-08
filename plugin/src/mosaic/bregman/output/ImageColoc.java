package mosaic.bregman.output;

import org.apache.commons.lang3.ArrayUtils;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;

import mosaic.utils.io.csv.CsvColumnConfig;

public class ImageColoc extends ImageBase {
    protected static final String[] ImageColocMap = ArrayUtils.addAll(ImageBaseMap, new String[] { "ChannelColoc", "ColocSignalBased", "ColocSizeBased", "ColocObjectsNumber", "MeanIntensity", "PearsonCorr", "PearsonCorrMasked" });
    protected static final CellProcessor[] ImageColocProc = ArrayUtils.addAll(ImageBaseProc, new CellProcessor[] { new ParseInt(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble(), new ParseDouble() });
    public static final CsvColumnConfig ColumnConfig = new CsvColumnConfig(ImageColocMap, ImageColocProc);
    
    protected int channelColoc;
    protected double colocSignalBased;
    protected double colocSizeBased;
    protected double colocObjectsNumber;
    protected double meanIntensity; 
    protected double pearsonCorr; 
    protected double pearsonCorrMasked;
    
    public ImageColoc(String aFileName, int f, int c, int cc, double csig, double csize, double cobj, double cint, double pc, double pcm) {
        setFilename(aFileName);
        setFrame(f);
        setChannel(c);
        setChannelColoc(cc);
        setColocSignalBased(csig);
        setColocSizeBased(csize);
        setColocObjectsNumber(cobj);
        setMeanIntensity(cint);
        setPearsonCorr(pc);
        setPearsonCorrMasked(pcm);
    }

    // Auto generated getters/setters
    
    public int getChannelColoc() {
        return channelColoc;
    }
    
    public void setChannelColoc(int channelColoc) {
        this.channelColoc = channelColoc;
    }

    public double getColocSignalBased() {
        return colocSignalBased;
    }

    public void setColocSignalBased(double colocSignalBased) {
        this.colocSignalBased = colocSignalBased;
    }
    
    public double getColocSizeBased() {
        return colocSizeBased;
    }

    public void setColocSizeBased(double colocSizeBased) {
        this.colocSizeBased = colocSizeBased;
    }

    public double getColocObjectsNumber() {
        return colocObjectsNumber;
    }

    public void setColocObjectsNumber(double colocObjectsNumber) {
        this.colocObjectsNumber = colocObjectsNumber;
    }
    
    public double getMeanIntensity() {
        return meanIntensity;
    }

    public void setMeanIntensity(double meanIntensity) {
        this.meanIntensity = meanIntensity;
    }
    
    public double getPearsonCorr() {
        return pearsonCorr;
    }

    public void setPearsonCorr(double pearsonCorr) {
        this.pearsonCorr = pearsonCorr;
    }

    public double getPearsonCorrMasked() {
        return pearsonCorrMasked;
    }

    public void setPearsonCorrMasked(double pearsonCorrMasked) {
        this.pearsonCorrMasked = pearsonCorrMasked;
    }

}
