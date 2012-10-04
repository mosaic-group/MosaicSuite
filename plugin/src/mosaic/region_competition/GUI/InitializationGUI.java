package mosaic.region_competition.GUI;

import mosaic.region_competition.Settings;
import mosaic.region_competition.initializers.InitializationType;

public abstract class InitializationGUI extends GUImeMore
{

	protected InitializationGUI(Settings settings)
	{
		super(settings);
	}

	public static InitializationGUI factory(Settings settings, InitializationType type)
	{
		InitializationGUI result = null; 
		
		switch(type)
		{
			case Bubbles:
			{
				result = new BubblesInitGUI(settings);
				break;
			}
			case Rectangle: 
			{
				result = new BoxInitGUI(settings);
				break;
			}
			default:
			{
				result = new DefaultInitGUI();
				break;
			}
		}
		return result;
	}
	
	public static InitializationGUI factory(Settings settings, String s)
	{
		InitializationType type = InitializationType.valueOf(s);
		InitializationGUI result = factory(settings, type);
		return result;
	}
	
	
}

class BubblesInitGUI extends InitializationGUI
{
	protected BubblesInitGUI(Settings settings)
	{
		super(settings);
		gd.setTitle("It's bubble Time");
	}

	@Override
	public void createDialog()
	{
		gd.addNumericField("Bubble_Radius", 5, 0);
		gd.addNumericField("Bubble_Padding", 15, 0);
		
	}

	@Override
	public void process()
	{
		if(gd.wasCanceled())
			return;
		
		double rad = gd.getNextNumber();
		double padding = gd.getNextNumber();
		
		//TODO save to settings
		
	}
}



class BoxInitGUI extends InitializationGUI
{
	protected BoxInitGUI(Settings settings)
	{
		super(settings);
		gd.setTitle("Box Initialization");
	}

	@Override
	public void createDialog()
	{
		gd.addNumericField("Box fill ratio", 0.8, 2);
		
	}

	@Override
	public void process()
	{
		if(gd.wasCanceled())
			return;
		
		double ratio = gd.getNextNumber();
		
		//TODO save to settings
		
	}
}

class DefaultInitGUI extends InitializationGUI
{

	protected DefaultInitGUI()
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



