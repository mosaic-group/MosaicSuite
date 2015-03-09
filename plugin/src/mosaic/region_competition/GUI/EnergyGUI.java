package mosaic.region_competition.GUI;

import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.EnergyFunctionalType;

public abstract class EnergyGUI extends GUImeMore
{
	protected EnergyGUI(Settings settings)
	{
		super(settings);
	}
	
	public static EnergyGUI factory(Settings settings, EnergyFunctionalType type)
	{
		EnergyGUI result = null; 
		
		switch(type)
		{
			case e_PS:
			{
				result = new PS_GUI(settings);
				break;
			}
			case e_DeconvolutionPC:
			{
				result = new DefaultEnergyGUI();
				break;
			}
			default:
			{
				result = new DefaultEnergyGUI();
				break;
			}
		}
		return result;
	}
	
	public static EnergyGUI factory(Settings settings, String energy)
	{
		EnergyFunctionalType type = EnergyFunctionalType.valueOf(energy);
		return factory(settings, type);
	}
}
	
	
class PS_GUI extends EnergyGUI
{
	public PS_GUI(Settings settings)
	{
		super(settings);
	}

	@Override
	public void createDialog()
	{	
		gd.setTitle("Gauss PS Options");	
		gd.addNumericField("Radius", settings.m_GaussPSEnergyRadius, 0);
		gd.addNumericField("Beta E_Balloon", settings.m_BalloonForceCoeff, 4);
	}

	@Override
	public void process()
	{	
		settings.m_GaussPSEnergyRadius = (int)gd.getNextNumber();
		settings.m_BalloonForceCoeff = (float)gd.getNextNumber();
	}
}

class DefaultEnergyGUI extends EnergyGUI
{

	protected DefaultEnergyGUI()
	{
		super(null);
	}

	@Override
	public void createDialog()
	{
		gd = getNoGUI();
	}

	@Override
	public void process(){}
	
}




