package mosaic.regions;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.psf.GeneratePSF;
import mosaic.core.utils.MosaicUtils;
import mosaic.regions.energies.E_CV;
import mosaic.regions.energies.E_CurvatureFlow;
import mosaic.regions.energies.E_Deconvolution;
import mosaic.regions.energies.E_Gamma;
import mosaic.regions.energies.E_KLMergingCriterion;
import mosaic.regions.energies.E_PC_Gauss;
import mosaic.regions.energies.E_PS;
import mosaic.regions.energies.Energy.ExternalEnergy;
import mosaic.regions.energies.Energy.InternalEnergy;
import mosaic.regions.energies.ImageModel;
import mosaic.regions.initializers.BoxInitializer;
import mosaic.regions.initializers.BubbleInitializer;
import mosaic.regions.initializers.MaximaBubbles;
import mosaic.utils.ImgUtils;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;


/**
 * Region Competion plugin implementation
 * 
 * @author Stephan Semmler, ETH Zurich
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public abstract class RegionsUtils {
    public static enum InitializationType {
        Rectangle("Rectangle"), 
        Bubbles("Bubbles"), 
        LocalMax("Local Maximum"), 
        ROI_2D("ROI 2D"), 
        File("File with regions");
        
        private final String iName;
        private InitializationType(final String aName) {
            iName = aName;
        }
        @Override
        public String toString() {
            return iName;
        }
        
        static public InitializationType getEnum(String aString) {
            for (InitializationType t : InitializationType.values()) {
                if (t.toString().equals(aString)) return t;
            }
            // If a aString is not a descriptive string of enum then try enum name
            return InitializationType.valueOf(aString);
        }
    }

    public static enum EnergyFunctionalType {
        e_PC("PC"), 
        e_PS("PS"), 
        e_DeconvolutionPC("Deconvolution PC"), 
        e_PC_Gauss("PC Gauss");
        
        private final String iName;
        private EnergyFunctionalType(final String aName) {
            iName = aName;
        }
        @Override
        public String toString() {
            return iName;
        }
        
        static public EnergyFunctionalType getEnum(String aString) {
            for (EnergyFunctionalType t : EnergyFunctionalType.values()) {
                if (t.toString().equals(aString)) return t;
            }
            // If a aString is not a descriptive string of enum then try enum name
            return EnergyFunctionalType.valueOf(aString);
        }
    }

    public static enum RegularizationType {
        Sphere_Regularization("Sphere Reguralization"), 
        Approximative("Approximative"), 
        None("None");
        
        private final String iName;
        private RegularizationType(final String aName) {
            iName = aName;
        }
        @Override
        public String toString() {
            return iName;
        }
        
        static public RegularizationType getEnum(String aString) {
            for (RegularizationType t : RegularizationType.values()) {
                if (t.toString().equals(aString)) return t;
            }
            // If a aString is not a descriptive string of enum then try enum name
            return RegularizationType.valueOf(aString);
        }
    }
    
    /**
     * Initialize the energy function
     * @return 
     */
    public static ImageModel initEnergies(IntensityImage intensityImage, LabelImage labelImage, Calibration inputImageCalibration, EnergyFunctionalType m_EnergyFunctional, float m_RegionMergingThreshold, int m_GaussPSEnergyRadius, float m_BalloonForceCoeff, RegularizationType regularizationType, float m_CurvatureMaskRadius, float m_EnergyContourLengthCoeff) {
        ExternalEnergy e_data;
        ExternalEnergy e_merge = null;
        switch (m_EnergyFunctional) {
            case e_PC: {
                e_data = new E_CV();
                e_merge = new E_KLMergingCriterion(LabelImage.BGLabel, m_RegionMergingThreshold);
                break;
            }
            case e_PS: {
                e_data = new E_PS(labelImage, intensityImage, m_GaussPSEnergyRadius, m_BalloonForceCoeff, m_RegionMergingThreshold);
                break;
            }
            case e_DeconvolutionPC: {
                final GeneratePSF gPsf = new GeneratePSF();
                Img<FloatType> image_psf = gPsf.generate(labelImage.getDepth());
                
                // Normalize PSF to overall sum equal 1.0
                final double Vol = MosaicUtils.volume_image(image_psf);
                MosaicUtils.rescale_image(image_psf, (float) (1.0f / Vol));

                e_data = new E_Deconvolution(intensityImage, image_psf);
                break;
            }
            case e_PC_Gauss: {
                e_data = new E_PC_Gauss();
                e_merge = new E_KLMergingCriterion(LabelImage.BGLabel, m_RegionMergingThreshold);
                break;
            }
            default: {
                final String s = "Unsupported Energy functional";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
        }

        InternalEnergy e_length;
        switch (regularizationType) {
            case Sphere_Regularization: {
                e_length = new E_CurvatureFlow(labelImage, (int)m_CurvatureMaskRadius, inputImageCalibration);
                break;
            }
            case Approximative: {
                e_length = new E_Gamma(labelImage);
                break;
            }
            case None: {
                e_length = null;
                break;
            }
            default: {
                final String s = "Unsupported Regularization";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
        }

        return new ImageModel(e_data, e_length, e_merge, m_EnergyContourLengthCoeff);
    }

    public static IntensityImage initInputImage(ImagePlus inputImageChosenByUser, boolean normalize_ip, int iPadSize) {
        // We should have a image or...
        if (inputImageChosenByUser != null) {
            int c = inputImageChosenByUser.getNChannels();
            int f = inputImageChosenByUser.getNFrames();
            if (c != 1 || f != 1) {
                String s = "Plugin is not able to segment correctly multichannel or multiframe images.\n" +
                        "Current input file info: number of channels=" + c +
                        "number of frames=" + f + "\nPlease use as a input only 2D or 3D single image.";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
            ImagePlus workImg = inputImageChosenByUser;
            ImageStack padedIs = ImgUtils.pad(inputImageChosenByUser.getStack(), iPadSize, inputImageChosenByUser.getNDimensions() > 2);
            workImg = inputImageChosenByUser.duplicate();
            workImg.setStack(padedIs);
            inputImageChosenByUser.show();
            return new IntensityImage(workImg, normalize_ip);
        }
        // ... we have failed to load anything
        IJ.noImage();
        return null;
    }

    public static LabelImage initLabelImage(IntensityImage intensityImage, ImagePlus inputImageChosenByUser, ImagePlus inputLabelImageChosenByUser, int iPadSize, InitializationType labelImageInitType, double l_BoxRatio, int m_BubblesRadius, int m_BubblesDispl, double l_Sigma, double l_Tolerance, int l_BubblesRadius, int l_RegionTolerance) {
        LabelImage labelImage = new LabelImage(intensityImage.getDimensions());

        switch (labelImageInitType) {
            case ROI_2D: {
                if (inputImageChosenByUser.getRoi() == null) {
                    // Can happen only in macro mode since GUI is doing check on existance of ROI
                    throw new RuntimeException("No ROI found in input image");
                }
                labelImage.initLabelsWithRoi(inputImageChosenByUser.getRoi());
                labelImage.initBorder();
                labelImage.connectedComponents();
                break;
            }
            case Rectangle: {
                final BoxInitializer bi = new BoxInitializer(labelImage);
                bi.initialize(l_BoxRatio);
                break;
            }
            case Bubbles: {
                final BubbleInitializer bi = new BubbleInitializer(labelImage);
                bi.initialize(m_BubblesRadius, m_BubblesDispl);
                break;
            }
            case LocalMax: {
                final MaximaBubbles mb = new MaximaBubbles(intensityImage, labelImage, l_Sigma, l_Tolerance, l_BubblesRadius, l_RegionTolerance);
                mb.initialize();
                break;
            }
            case File: {
                if (inputLabelImageChosenByUser != null && inputLabelImageChosenByUser.getWidth() == inputImageChosenByUser.getWidth() &&  inputLabelImageChosenByUser.getHeight() == inputImageChosenByUser.getHeight()) {
                    ImagePlus labelImg = inputLabelImageChosenByUser;
                    ImageStack padedIs = ImgUtils.pad(inputLabelImageChosenByUser.getStack(), iPadSize, inputLabelImageChosenByUser.getNDimensions() > 2);
                    labelImg = inputLabelImageChosenByUser.duplicate();
                    labelImg.setStack(padedIs);
                    labelImage.initWithImg(labelImg);
                    labelImage.initBorder();
                    labelImage.connectedComponents();
                }
                else {
                    final String msg = "No valid label image given.";
                    IJ.showMessage(msg);
                    return null;
                }

                break;
            }
            default: {
                // was aborted
                throw new RuntimeException("No valid input option in User Input. Abort");
            }
        }
        return labelImage;
    }
}
