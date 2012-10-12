package mosaic.region_competition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventListener;

import mosaic.region_competition.energies.EnergyFunctionalType;
import mosaic.region_competition.energies.RegularizationType;
import mosaic.region_competition.initializers.InitializationType;

public class Settings implements Serializable
{
	private static final long serialVersionUID = 1777976540627904860L;
	
	public int m_MaxNbIterations = 300;
	public boolean shrinkFirstOnly = false;

	public EnergyFunctionalType m_EnergyFunctional = EnergyFunctionalType.e_PC;
	public int m_GaussPSEnergyRadius=8;

	public float m_RegionMergingThreshold = 0.02f;

	public float m_BalloonForceCoeff = 0.0f;
	public final float default_PSballoonForceCoeff = 0.05f;
	public float m_ConstantOutwardFlow = 0.0f;

	
	public RegularizationType regularizationType = RegularizationType.Sphere_Regularization;
	public float m_CurvatureMaskRadius = 8;
	public float m_EnergyContourLengthCoeff = 0.04f; // 0.04;//0.003f;
	public boolean m_RemoveNonSignificantRegions = true;
	public int m_AreaThreshold = 1;

	public int m_OscillationHistoryLength = 10;
	public float m_AcceptedPointsFactor = 1;
	public float m_AcceptedPointsReductionFactor = 0.5f;
	
	public InitializationType labelImageInitType = InitializationType.Bubbles;


//	private float m_SigmaOfLoGFilter = 2.f;
	// / Pushes the curve towards edges in the image
//	private float m_EnergyEdgeAttractionCoeff = 0; // /EXPERIMENTAL!;
//	private float m_EnergyShapePriorCoeff = 0;
	public float m_EnergyRegionCoeff = 1.0f;
//	private float m_EnergySphericityCoeff = 0;

	public boolean m_AllowFusion = true;
	public boolean m_AllowFission = true;
	public boolean m_AllowHandles = true;
//	private boolean m_UseRegionCompetition = true;
//	private boolean m_UseForbiddenRegion = false;
	public boolean m_UseGaussianPSF = true;
	public boolean m_UseShapePrior = false;
//	private boolean m_UseFastEvolution = false;
//	private int m_LocalLiEnergySigma = 5;
	
	public String m_PSFImg;
	
	// SettingsListener ////////////////////////////////
	// To update values on the fly
	
	public static interface SettingsListener extends EventListener
	{
		public void settingsChanged(Settings settings);
	}
	
	ArrayList<SettingsListener> listeners = new ArrayList<Settings.SettingsListener>();
	public void addSettingsListener(SettingsListener listener)
	{
		listeners.add(listener);
	}
	
	public void settingsChanged()
	{
		for(SettingsListener l : listeners)
		{
			l.settingsChanged(this);
		}
	}
	
}



