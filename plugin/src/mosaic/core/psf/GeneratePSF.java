package mosaic.core.psf;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import mosaic.core.utils.RegionIterator;
import mosaic.plugins.Generate_PSF.Point3D;
import mosaic.region_competition.GUI.EnergyGUI;
import mosaic.region_competition.GUI.RegularizationGUI;

/**
 * 
 * This class generate PSF images from the list of all implemented
 * PSF.
 * 
 * @see psfList.java
 * 
 * Just create the a new GeneratePSF class and call generate
 * 
 * @author Pietro Incardona
 *
 */

public class GeneratePSF
{
	int sz[];
	
	Choice PSFc;
	
	psf<FloatType> psfc;
	TextField dimF;
	
	/**
	 * 
	 * Get the parameters for the Psf
	 * 
	 * @param dim Dimension of the PSF
	 * @param psf String that identify the PSF like "Gauss ... "
	 */
	
	private void selectPSF(int dim,String psf)
	{	
		if (dim == 0)
		{
			IJ.error("Dimension must be a valid integer != 0");
		}
		psfc = psfList.factory(psf,dim,FloatType.class);
		psfc.getParamenters();
	}
	
	/**
	 * 
	 * Get the parameters for the PSF
	 * 
	 * @param dim dimension of the psf
	 */
	
	private void selectPSF(int dim)
	{
		String psf = PSFc.getSelectedItem();
		
		if (dim == 0)
		{
			IJ.error("Dimension must be a valid integer != 0");
		}
		psfc = psfList.factory(psf,dim,FloatType.class);
		psfc.getParamenters();
	}
	
	/**
	 * 
	 * Return a generated PSF image. A GUI is shown ti give the user
	 * the possibility to choose size of the image PSF function parameters
	 * 
	 * @return An image representing the PSF
	 */
	
	public Img< FloatType > generate(int dim)
	{
		GenericDialog gd = new GenericDialog("PSF Generator");
		
		gd.addNumericField("Dimensions ", dim, 0);
		
		if (IJ.isMacro() == false)
		{
			dimF = (TextField) gd.getNumericFields().lastElement();
		
			gd.addChoice("PSF: ", psfList.psfList, psfList.psfList[0]);
			PSFc = (Choice)gd.getChoices().lastElement();
			{
				Button optionButton = new Button("Options");
				GridBagConstraints c = new GridBagConstraints();
				int gridx = 2;
				int gridy = 1;
				c.gridx=gridx;
				c.gridy=gridy++; c.anchor = GridBagConstraints.EAST;
				gd.add(optionButton,c);
			
				optionButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						int dim = Integer.parseInt(dimF.getText());
						selectPSF(dim);
					}
				});
			}
		}
		else
		{
			gd.addChoice("PSF: ", psfList.psfList, psfList.psfList[0]);
		}
		
		gd.showDialog();
		
		// if Batch system
		
		if (IJ.isMacro() == true)
		{
			dim = (int) gd.getNextNumber();
			selectPSF(dim,gd.getNextChoice());
		}
		
		// psf not selected
		
		if (psfc == null)
			return null;
		
		// get the dimension
		
		sz = psfc.getSuggestedSize();
		
		// center on the middle of the image
		
		int [] mid = new int[sz.length];
		
		for (int i = 0 ; i < mid.length ; i++)
		{
			mid[i] = sz[i]/2;
		}
		
		psfc.setCenter(mid);
		
		int loc[] = new int[sz.length]; 
		
		// Create an imglib2
		
		ImgFactory< FloatType > imgFactory = new ArrayImgFactory< FloatType >( );
		Img<FloatType> PSFimg = imgFactory.create( sz, new FloatType() );
		
		Cursor<FloatType> cft = PSFimg.cursor();
		
		while(cft.hasNext())
		{
			cft.next();
			cft.localize(loc);
			psfc.setPosition(loc);
			cft.get().set(psfc.get().getRealFloat());
		}
		
		return PSFimg;
	}
	
	public String getParameters()
	{
		return psfc.getStringParameters();
	}
}
