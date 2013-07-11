package mosaic.region_competition.wizard;

import mosaic.region_competition.Settings;

/**
 * @author      Incardona Pietro <incardon@mpi-cbg.de>
 * @version     1.0
 * @since       2013-04-29
 */

/**
 * Region Competition Wizard.
 */

public class RCWizard
{
	RCWWin w;
	
	public void StartWizard(Settings s)
	{
		w = new RCWWin();
		w.start(s);
	}
}
