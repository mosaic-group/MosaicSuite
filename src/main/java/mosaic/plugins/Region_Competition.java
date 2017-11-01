package mosaic.plugins;


import java.awt.GraphicsEnvironment;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.psf.GeneratePSF;
import mosaic.core.utils.MosaicUtils;
import mosaic.region_competition.GUI.SegmentationProcessWindow;
import mosaic.region_competition.energies.E_CV;
import mosaic.region_competition.energies.E_CurvatureFlow;
import mosaic.region_competition.energies.E_Deconvolution;
import mosaic.region_competition.energies.E_Gamma;
import mosaic.region_competition.energies.E_KLMergingCriterion;
import mosaic.region_competition.energies.E_PC_Gauss;
import mosaic.region_competition.energies.E_PS;
import mosaic.region_competition.energies.Energy.ExternalEnergy;
import mosaic.region_competition.energies.Energy.InternalEnergy;
import mosaic.region_competition.energies.ImageModel;
import mosaic.region_competition.initializers.BoxInitializer;
import mosaic.region_competition.initializers.BubbleInitializer;
import mosaic.region_competition.initializers.MaximaBubbles;
import mosaic.utils.ImgUtils;
import mosaic.utils.SysOps;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;


/**
 * Region Competion plugin implementation
 * 
 * @author Stephan Semmler, ETH Zurich
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public abstract class Region_Competition {
    public enum InitializationType {
        Rectangle, Bubbles, LocalMax, ROI_2D, File
    }

    public enum EnergyFunctionalType {
        e_PC, e_PS, e_DeconvolutionPC, e_PC_Gauss
    }

    public enum RegularizationType {
        Sphere_Regularization, Approximative, None,
    }
    
    // Output file names
    protected final String[] outputFileNamesSuffixes = { "*_ObjectsData_c1.csv", "*_seg_c1.tif", "*_prob_c1.tif" };

    // Settings
//    private Settings iSettings = null;
    protected String outputSegmentedImageLabelFilename = null;
    protected boolean normalize_ip = true;
    protected boolean showGUI = true;

    // Images to be processed
    protected Calibration inputImageCalibration;
    protected ImagePlus originalInputImage;
    protected ImagePlus inputImageChosenByUser;
    protected ImagePlus inputLabelImageChosenByUser;
    protected int iPadSize = 1;
    
    // Algorithm and its input stuff
    protected LabelImage labelImage;
    protected IntensityImage intensityImage;
    protected ImageModel imageModel;
    
    // User interfaces
    protected SegmentationProcessWindow stackProcess;

    
    
    public void runDeep() {
        // ================= Run segmentation ==============================
        runIt();
        
        // ================= Save segmented image =========================
        //
        saveSegmentedImage();
        
        final boolean headless_check = GraphicsEnvironment.isHeadless();
        if (headless_check == false) {
            final String directory = ImgUtils.getImageDirectory(inputImageChosenByUser);
            final String fileNameNoExt = SysOps.removeExtension(inputImageChosenByUser.getTitle());
            MosaicUtils.reorganize(outputFileNamesSuffixes, fileNameNoExt, directory, 1);
        }
    }

    protected abstract void runIt();
    
    protected abstract void saveSegmentedImage();
    protected abstract String configFilePath();

    /**
     * Initialize the energy function
     */
    protected void initEnergies(EnergyFunctionalType m_EnergyFunctional, float m_RegionMergingThreshold, int m_GaussPSEnergyRadius, float m_BalloonForceCoeff, RegularizationType regularizationType, float m_CurvatureMaskRadius, float m_EnergyContourLengthCoeff) {
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
                Img<FloatType> image_psf = gPsf.generate(inputImageChosenByUser.getNSlices() == 1 ? 2 : 3);
                
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

        imageModel = new ImageModel(e_data, e_length, e_merge, m_EnergyContourLengthCoeff);
    }

    protected void initInputImage() {
        // We should have a image or...
        if (inputImageChosenByUser != null) {
            int c = inputImageChosenByUser.getNChannels();
            int f = inputImageChosenByUser.getNFrames();
            if (c != 1 || f != 1) {
                String s = "Region Competition is not able to segment correctly multichannel or multiframe images.\n" +
                        "Current input file info: number of channels=" + c +
                        "number of frames=" + f + "\nPlease use as a input only 2D or 3D single image.";
                IJ.showMessage(s);
                throw new RuntimeException(s);
            }
            ImagePlus workImg = inputImageChosenByUser;
            ImageStack padedIs = ImgUtils.pad(inputImageChosenByUser.getStack(), iPadSize, inputImageChosenByUser.getNDimensions() > 2);
            workImg = inputImageChosenByUser.duplicate();
            workImg.setStack(padedIs);
            intensityImage = new IntensityImage(workImg, normalize_ip);
            inputImageChosenByUser.show();
        }
        else {
            // ... we have failed to load anything
            IJ.noImage();
            throw new RuntimeException("Failed to load an input image.");
        }
    }

    protected void initLabelImage(InitializationType labelImageInitType, double l_BoxRatio, int m_BubblesRadius, int m_BubblesDispl, double l_Sigma, double l_Tolerance, int l_BubblesRadius, int l_RegionTolerance) {
        labelImage = new LabelImage(intensityImage.getDimensions());

        switch (labelImageInitType) {
            case ROI_2D: {
                initializeRoi(labelImage);
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
                if (inputLabelImageChosenByUser != null) {
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
                    throw new RuntimeException(msg);
                }

                break;
            }
            default: {
                // was aborted
                throw new RuntimeException("No valid input option in User Input. Abort");
            }
        }
    }

    /**
     * Initializes labelImage with ROI <br>
     */
    private void initializeRoi(final LabelImage labelImg) {
        labelImg.initLabelsWithRoi(inputImageChosenByUser.getRoi());
        labelImg.initBorder();
        labelImg.connectedComponents();
    }
}
