package mosaic.plugins;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import mosaic.bregman.output.CSVOutput;
import mosaic.bregman.output.Region3DTrack;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.ipc.OutputChoose;
import mosaic.core.psf.psf;
import mosaic.core.psf.psfList;
import mosaic.core.utils.CircleMask;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.core.utils.SphereMask;
import mosaic.region_competition.GUI.EnergyGUI;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * 
 * Plugins to produce regions with ground truth
 * 
 * @author Pietro Incardona
 *
 */


public class RegionCreator implements PlugInFilter
{
	int N_region;
	int Max_radius;
	int Min_radius;
	int Max_intensity;
	int Min_intensity;
	int N_frame;
	long Image_sz[];
	float Spacing[];
	psf<FloatType> cPSF;
	
	String conv;
	Choice cConv;
	String nModel;
	Choice cNoise;
	
	/**
	 * 
	 * Draw a Sphere list with radius on out
	 * 
	 * @param out Image
	 * @param pt list of point
	 * @param cal spacing
	 * @param intensity of your region
	 * @param p_radius radius of your region
	 */
	
	void drawSphereWithRadius(RandomAccessibleInterval<UnsignedByteType> out, List<Point> pt , float[] cal, UnsignedByteType intensity, int p_radius)
	{
		if (cal == null)
		{
			cal = new float[out.numDimensions()];
			for (int i = 0 ; i < out.numDimensions() ; i++)
			{
				cal[i] = 1;
			}
		}
		
		RandomAccess<UnsignedByteType> out_a = out.randomAccess();
		
        int sz[] = new int [out_a.numDimensions()];
	   	 
        for ( int d = 0; d < out_a.numDimensions(); ++d )
        {
            sz[d] = (int) out.dimension( d );
        }
		
        // Iterate on all particles
        
        double radius = p_radius;
    	
    	// Create a circle Mask and an iterator
    	
    	RegionIteratorMask rg_m = null;
    	
    	float min_s = MyFrame.minScaling(cal);
    	int rc = (int) (radius / min_s);
		for (int i = 0 ; i < out.numDimensions() ; i++)
		{
			cal[i] /= min_s;
		}

	    if (rc < 1) rc = 1;
        SphereMask cm = new SphereMask(rc, 2*rc + 1, out_a.numDimensions(), cal);
        rg_m = new RegionIteratorMask(cm, sz);
    	
        Iterator<Point> pt_it = pt.iterator();
        
        while (pt_it.hasNext())
        {
        	Point ptt = pt_it.next();
        	
        	// Draw the Sphere
       	
        	Point p_c = new Point(ptt);
        	p_c.div(cal);
        			
        	rg_m.setMidPoint(p_c);
        			
	        while ( rg_m.hasNext() )
	        {
	        	Point p = rg_m.nextP();
	        			
	        	if (p.isInside(sz))
	        	{
	        		out_a.setPosition(p.x);
	        		out_a.get().set(intensity);
	        	}
	        }
        }
	}
	
	/**
	 * 
	 * Calculate how many point you can create on the grid on each dimension
	 * 
	 * @return number of grid points on each dimension
	 * @param spacing between points
	 */
	
	int [] calculateGridPoint(int spacing)
	{
		// Calculate the grid size
		
		int szi = 0;
		
    	for (int i = 0; i < Image_sz.length-1 ; i++)
    	{
    		if (Image_sz[i] != 1)
    		{szi++;}
    		else
    		{break;}
    	}
		
    	int [] gs = new int[szi];
    	
    	// Calculate the number of grid point
    	
    	for (int i = 0; i < szi ; i++)
    	{
    		gs[i] = (int) ((Image_sz[i] - spacing/Spacing[i] ) / (spacing/Spacing[i]));
    	}
    	
    	return gs;
	}
	
	/**
	 * 
	 * Return the total point in the grid
	 * 
	 * @param gs grid point on each dimension
	 * @return the total number
	 */
	
	long totalGridPoint(int gs[])
	{
		long gp = 1;
		
		for (int i = 0 ; i < gs.length ; i++)
		{
			gp *= gs[i];
		}
		
		return gp;
	}
	
	/**
	 * 
	 * Fill the grid of point
	 * 
	 * @param p Array of points as output
	 * @param i grid specification
	 * @param spacing between point
	 */
	
	void FillGridPoint(Point p[], int i[], int spacing)
	{
		int cnt = 0;
		RegionIterator rg = new RegionIterator(i);
		
		Point t = new Point(i.length);
		for (int s = 0 ; s < i.length ; s++)
		{
			t.x[s] = 1;
		}
		
		while (rg.hasNext() && cnt < p.length)
		{
			rg.next();
			p[cnt] = rg.getPoint();
			p[cnt] = p[cnt].add(t);
			p[cnt] = p[cnt].mult(spacing);
			p[cnt] = p[cnt].div(Spacing);
			cnt++;
		}
	}
	
	double VolRadius(double radius)
	{
		return Math.PI * radius * radius;
	}
	
	@Override
	public void run(ImageProcessor arg0) 
	{
    	
		// Vector if output region
		
		Vector<Region3DTrack> pt_r = new Vector<Region3DTrack>();
		
		// Grid of possible 
		
        final ImgFactory< UnsignedByteType > imgFactory = new ArrayImgFactory< UnsignedByteType >();
        Img<UnsignedByteType> out = imgFactory.create(Image_sz, new UnsignedByteType());
		
        Cursor<UnsignedByteType> c = out.cursor();
        while(c.hasNext())
        {
        	UnsignedByteType bt = c.next();
        	bt.set(10);
        }
        
        // for each frame
        
        for (int i = 0 ; i < Image_sz[Image_sz.length-1]; i++)
        {	
        	// set intensity
        	
        	int radius = 10;
        	UnsignedByteType ri = new UnsignedByteType();
        	ri.set(150);
        	
        	int gs[] = null;
        	gs = calculateGridPoint(2*radius + 1);
        	long np = totalGridPoint(gs);
        	if (np == 0)
        	{
        		IJ.error("The size of the image is too small or the region too big");
        		return;
        	}
        	
        	if (np < N_region)
        	{
        		IJ.error("Too much region increase the size of the image or reduce the number of the region");
        		return;
        	}
        		
        	Point p[] = new Point[(int)np];
        	FillGridPoint(p,gs,2*radius+1);
        	
        	// shuffle
        	
        	Collections.shuffle(Arrays.asList(p));
        	Vector<Point> pt = new Vector<Point>();
        	
        	for (int k = 0 ; k < N_region ; k++)
        	{
        		pt.add(p[k]);
        	}
        	
        	// Create a view of out fixing frame
        	
        	IntervalView<UnsignedByteType> vti = Views.hyperSlice(out, Image_sz.length-1, i);
        	
        	// Draw sphere
        	
        	drawSphereWithRadius(vti,pt,Spacing,ri,radius);
        	
            // Convolve the pictures
            
    		cPSF.convolve(vti);
        	
    		for (int s = 0 ; s < pt.size() ; s++)
    		{
    			Region3DTrack tmp = new Region3DTrack();
    			
    			tmp.setData(pt.get(s));
    			tmp.setSize(VolRadius(radius));
    			tmp.setIntensity((double)ri.get());
    			tmp.setFrame(i);
    			
    			pt_r.add(tmp);
    		}
        }
        
        //
        
        ImageJFunctions.show(out);
        
		// Output ground thruth
		
		InterPluginCSV<Region3DTrack> P_csv = new InterPluginCSV<Region3DTrack>(Region3DTrack.class);
		
		// get output folder
		
		String output = IJ.getDirectory("Choose output directory");;
		
		//
		
		OutputChoose oc = new OutputChoose();
		oc.map = CSVOutput.Region3DTrack_map;
		oc.cel = CSVOutput.getRegion3DTrackCellProcessor();
		
		P_csv.Write(output, pt_r, oc, false);
	}

	@Override
	public int setup(String arg0, ImagePlus original_imp) 
	{
		/* get user defined params and set more initial params accordingly 	*/	

		GenericDialog gd = new GenericDialog("Region creator");
		
		String nsn[] = {"Poisson"};
		
		gd.addNumericField("Max radius", 10.0, 0);
		gd.addNumericField("Min radius", 10.0, 0);
		gd.addNumericField("Max intensity", 1.0, 3);
		gd.addNumericField("Min intensity", 0.1, 3);
		gd.addNumericField("N frame", 10.0, 0);
		gd.addNumericField("Image X", 512.0, 0);
		gd.addNumericField("Image Y", 512.0, 0);
		gd.addNumericField("Image Z", 1.0, 0);
		gd.addNumericField("Spacing X", 1.0, 1);
		gd.addNumericField("Spacing Y", 1.0, 1);
		gd.addNumericField("Spacing Z", 1.0, 1);
		gd.addNumericField("N regions", 20, 0);
		
		gd.addChoice("Noise", nsn, nsn[0]);
		cNoise = (Choice)gd.getChoices().lastElement();
		
		Button optionButton = new Button("Options");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=2; c.gridy=12; c.anchor = GridBagConstraints.EAST;
		gd.add(optionButton,c);
		
		optionButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
			}
		});
		
		String nsc[] = {"Gauss"};
		
		gd.addChoice("Blur", nsc, nsc[0]);
		cConv = (Choice)gd.getChoices().lastElement();
		
		optionButton = new Button("Options");
		c = new GridBagConstraints();
		c.gridx=2; c.gridy=13; c.anchor = GridBagConstraints.EAST;
		gd.add(optionButton,c);
		
		optionButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				conv = cConv.getSelectedItem();
	    		psf<FloatType> cPSF = psfList.factory(conv,3,FloatType.class);
	    		if (cPSF == null)
	    		{
	    			IJ.error("Cannot create " + conv + ", convolution PSF");
	    			return;
	    		}
	    		cPSF.getParamenters();
			}
		});
		
		gd.showDialog();
		
		if (gd.wasCanceled())
		{
			return DONE;
		}
		
		Max_radius = (int) gd.getNextNumber();
		Min_radius = (int) gd.getNextNumber();
		Max_intensity = (int) gd.getNextNumber();
		Min_intensity = (int) gd.getNextNumber();
		
		long[] tmp = new long[4];
		tmp[3] = (long) gd.getNextNumber();
		tmp[0] = (long) gd.getNextNumber();
		tmp[1] = (long) gd.getNextNumber();
		tmp[2] = (long) gd.getNextNumber();
		if (tmp[2] == 1)
		{
			// 2D
			
			tmp[2] = tmp[3];
			Image_sz = new long[3];
			Image_sz[0] = tmp[0];
			Image_sz[1] = tmp[1];
			Image_sz[2] = tmp[2];
			
			Spacing = new float[2];
			
			Spacing[0] = (int)gd.getNextNumber();
			Spacing[1] = (int)gd.getNextNumber();
			gd.getNextNumber();
			N_region = (int) gd.getNextNumber();
		}
		else
		{
			// 3D
			
			Image_sz = new long[4];
			Image_sz[0] = tmp[0];
			Image_sz[1] = tmp[1];
			Image_sz[2] = tmp[2];
			Image_sz[3] = tmp[3];
			
			Spacing = new float[3];
		
			Spacing[0] = (int)gd.getNextNumber();
			Spacing[1] = (int)gd.getNextNumber();
			Spacing[2] = (int)gd.getNextNumber();
			N_region = (int) gd.getNextNumber();
		}
		
		// Get noise model
		
		nModel = gd.getNextChoice();
		
		// Get convolution
		
		conv = gd.getNextChoice();
		
		run(null);
		return DONE;
	}
	
}
