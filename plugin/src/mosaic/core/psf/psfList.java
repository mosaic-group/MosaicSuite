package mosaic.core.psf;

import net.imglib2.type.numeric.RealType;

/**
 * 
 * This class contain a list a all available PSF
 * 
 * @author Pietro Incardona
 *
 */

public class psfList
{
	static public String psfList[] = {"Gauss","File"};
	
	/**
	 * 
	 * Create a specified PSF
	 * 
	 * @param f String
	 * @return psf
	 */
	
	static public <T extends RealType<T>> psf<T> factory(String f,int dim, Class<T> cl)
	{
		for (int i = 0 ; i <  psfList.length ; i++)
		{
			if (f.equals("Gauss"))
			{
				return new GaussPSF<T>(dim,cl);
			}
			if (f.equals("File"))
			{
				return new FilePSF<T>(cl);
			}
		}
		return null;
	}
};
