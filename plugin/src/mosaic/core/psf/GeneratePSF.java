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
	 * Return a generated PSF image. A GUI is shown ti give the user
	 * the possibility to choose size of the image PSF function parameters
	 * 
	 * @return An image representing the PSF
	 */
	
	public Img< FloatType > generate()
	{
		GenericDialog gd = new GenericDialog("PSF Generator");
		
		gd.addNumericField("Dimensions ", 2, 0);
		dimF = (TextField) gd.getNumericFields().lastElement();
		
		gd.addChoice("PSF: ", psfList.psfList, psfList.psfList[0]);
		PSFc = (Choice)gd.getChoices().lastElement();
		{
			Button optionButton = new Button("Options");
			GridBagConstraints c = new GridBagConstraints();
			int gridx = 0;
			int gridy = 1;
			c.gridx=gridx;
			c.gridy=gridy++; c.anchor = GridBagConstraints.EAST;
			gd.add(optionButton,c);
			
			optionButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					String psf = PSFc.getSelectedItem();
					
					int dim = Integer.parseInt(dimF.getText());
					if (dim == 0)
					{
						IJ.error("Dimension must be a valid integer != 0");
					}
					psfc = psfList.factory(psf,dim,FloatType.class);
					psfc.getParamenters();
				}
			});
		}
		
		gd.showDialog();
		
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
		
		ImageJFunctions.show(PSFimg);
		
		return PSFimg;
	}
}
