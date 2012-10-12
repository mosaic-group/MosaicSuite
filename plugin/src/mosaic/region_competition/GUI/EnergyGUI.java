package mosaic.region_competition.GUI;

import java.awt.Button;
import java.awt.Panel;
import java.awt.TextField;

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
				result = new Deconvolution_GUI(settings);
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

class Deconvolution_GUI extends EnergyGUI
{
	public Deconvolution_GUI(Settings settings)
	{
		super(settings);
	}

	@Override
	public void createDialog()
	{	
		gd.setTitle("Deconvolution Options");
		
		gd.addTextAreas(settings.m_PSFImg, null, 1, 20);
		Button b = new Button("Open PSF Image");
		b.addActionListener(new FileOpenerActionListener(gd, gd.getTextArea1()));
		gd.add(b);
		gd.addCheckbox("Generate", settings.m_UseGaussianPSF);
	}

	@Override
	public void process()
	{
		String filenameInput=gd.getTextArea1().getText();
		if(filenameInput==null || filenameInput.isEmpty())
		{
			//TODO 
			// set text to [] due to a bug in GenericDialog
			// if bug gets fixed, this will cause problems!
			settings.m_UseGaussianPSF = true;
		}
		else
		{
			settings.m_UseGaussianPSF = gd.getNextBoolean();
			settings.m_PSFImg = filenameInput.replace('\\', '/');
		}
		settings.m_GaussPSEnergyRadius = (int)gd.getNextNumber();
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




