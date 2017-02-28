package mosaic.region_competition.DRS;


import org.apache.log4j.Logger;

import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.region_competition.energies.ImageModel;

public class AlgorithmDRS {

    private static final Logger logger = Logger.getLogger(AlgorithmDRS.class);

    // Input for Algorithm
    private final LabelImage iLabelImage;
    private final IntensityImage iIntensityImage;
    private final ImageModel iImageModel;
    private final Settings iSettings;

    public AlgorithmDRS(IntensityImage aIntensityImage, LabelImage aLabelImage, ImageModel aModel, Settings aSettings) {
        logger.debug("DRS algorithm created");
        
        iLabelImage = aLabelImage;
        iIntensityImage = aIntensityImage;
        iImageModel = aModel;
        iSettings = aSettings;
        
        logger.debug(mosaic.utils.Debug.getString(iIntensityImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iLabelImage.getDimensions()));
        logger.debug(mosaic.utils.Debug.getString(iImageModel.toString()));
        logger.debug(mosaic.utils.Debug.getString(iSettings.toString()));
        
        // Initialize label image
        iLabelImage.initBoundary();
        iLabelImage.initContour();
        
        
    }

    public boolean performIteration() {
        boolean convergence = true;
        
        return convergence;
    }
    
    public int getBiggestLabel() {
        return 10;
    }
}
