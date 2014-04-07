package mosaic.core.detection;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mosaic.core.utils.CircleMask;
import mosaic.core.utils.Connectivity;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.MosaicUtils.ToARGB;
import mosaic.core.utils.Point;
import mosaic.core.utils.RectangleMask;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.plugins.ParticleTracker3DModular_.Trajectory;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;

import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;


	/**
	 * Defines a MyFrame that is based upon an ImageProcessor or information from a text file.
	 * @param <RandomsAccessible>
	 */
	public class MyFrame 
	{

		static Map<Integer,RegionIteratorMask> CircleCache;
		static Map<Integer,RegionIteratorMask> RectangleCache;
		
		//		Particle[] particles;		// an array Particle, holds all the particles detected in this frame
		//									// after particle discrimination holds only the "real" particles
		Vector<Particle> particles;
		int particles_number;				// number of particles initialy detected 
		public int real_particles_number;	// number of "real" particles discrimination
		public int frame_number;			// Serial number of this frame in the movie (can be 0)
		StringBuffer info_before_discrimination;// holdes string with ready to print info
											// about this frame before particle discrimination 

		/* only relevant to frames representing real images */
		private ImageStack original_ips;	// the original image, this is used for the featurePointDetector to access  corresponding the image data. 
		//		ImageStack original_fps; // the original image after convertion to float processor (if already float, then a copy)
		//		ImageStack restored_fps; // the floating processor after image restoration
		float threshold;					// threshold for particle detection 
		boolean normalized = false;
		int linkrange;
		int p_radius = -1;
		
		/**
		 * 
		 * Cleanup circle cache
		 * 
		 */
		
		static public void cleanCache()
		{
			CircleCache.clear();
			RectangleCache.clear();
		}
		
		/**
		 * 
		 * Init the Cache circle
		 * 
		 */
		
		static public void initCache()
		{
			CircleCache = new HashMap<Integer,RegionIteratorMask>();
			RectangleCache = new HashMap<Integer,RegionIteratorMask>();
		}
		
		/**
		 * Default constructor 
		 * 
		 */
		public MyFrame () 
		{
		}
		
		/**
		 * Constructor for ImageProcessor based MyFrame.
		 * <br>All particles and other information will be derived from the given <code>ImageProcessor</code>
		 * by applying Detector methods  
		 * @param ip the original ImageProcessor upon this MyFrame is based, will remain unchanged!
		 * @param frame_num the serial number of this frame in the movie
		 * @param aLinkrange link range
		 */
		public MyFrame (ImageStack ips, int frame_num, int aLinkrange) {
			this.original_ips = ips;
			this.frame_number = frame_num;
			this.linkrange = aLinkrange;
		}

		
		/**
		 * Constructor for text mode
		 * 
		 * @deprecated
		 * 
		 */
		public MyFrame (String path, int frame_num, int aLinkrange) {
			loadParticlesFromFile(path);
		}
		
		/**
		 * Constructor for text mode
		 * 
		 * @deprecated
		 * 
		 */
		public MyFrame (BufferedReader r,String path, int frame_num, int aLinkrange) {
			try {
				loadParticlesFromFileMultipleFrame(r,path,frame_num);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/**
		 * 
		 * Constructor for text mode from a vector of particles
		 * 
		 * @param Vector of particles in the frames
		 * @param frame frame number
		 * @param aLinkRange linking range
		 * 
		 */
		
		public MyFrame(Vector<Particle> p, int frame, int aLinkrange)
		{
	        this.frame_number = frame;
			this.particles_number = p.size();
	        this.real_particles_number = p.size();
			
	        /* initialise the particles array */
	        this.particles = p;
	        
	        for (int i = 0 ; i < this.particles.size() ; i++)
	        {
	        	this.particles.get(i).setLinkRange(aLinkrange);
	        }
		}
		/**
		 * ONLY FOR text_files_mode.
		 * <br>Loads particles information for all frames from the file located 
		 * at the given path and adds these particles to the <code>particles</code> array. 
		 * <br>These particles are considered to be "after discrimination".
		 * <br>File must have the word 'frame' (case sensitive) at the beginning of the first line and when a frame 
		 * start
		 * followed by any number of space characters (\t \n) and the frame number.
		 * <br>Each next line represents a particle in the frame number given at the first line.
		 * <br>Each line must have 2 numbers or more separated by one or more space characters.
		 * <br>The 2 first numbers represents the X and Y coordinates of the particle (respectfully).
		 * <br>The next numbers represent other information of value about the particle
		 * (this information can be plotted later along a trajectory).
		 * <br>The number of parameters must be equal for all particles.  
		 * 
		 * @deprecated
		 * 
		 * @param path full path to the file (including full file name) e.g c:\ImageJ\frame0.txt
		 * @return false if there was any problem
		 * @see Particle   
		 */
		
		private boolean loadParticlesFromFile(String path) 
		{
			boolean ret;
			BufferedReader r;
			
			try
			{
				r = new BufferedReader(new FileReader(path));
			
				ret = loadParticlesFromFile(r,path);
			
				/* close file */
				r.close();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
				return false;
			}
            
            return ret;
		}
		
		public void setParticleRadius(int pt_radius)
		{
			p_radius = pt_radius;
		}
		
		private boolean loadParticlesFromFileMultipleFrame(BufferedReader r, String path, int nf) throws IOException 
		{
			Vector<String[]> particles_info = new Vector<String[]>(); 	// a vector to hold all particles info as String[]
			String[] particle_info; 				// will hold all the info for one particle (splitted)
			String[] frame_number_info;				// will fold the frame info line (splitted)
			String line;
	            
	        /* set this frame number from the first line*/
			r.mark(1024);
	        line = r.readLine();

	        if (line == null)	return false;
	        
	        line = line.trim();
	        frame_number_info = line.split(",");
	        if (frame_number_info.length < 2) 
	        {
	            IJ.error("Malformed line, expacting \"frame x\", founded " + line);
	            return false;
	        }
	        if (frame_number_info[0] != null) {
	            this.frame_number = Integer.parseInt(frame_number_info[0]);
	        }
	        
	        r.reset();
		    /* go over all lines, count number of particles and save the information as String */
	        while (true) 
	        {
	        	r.mark(1024);
		        line = r.readLine();
		        if (line == null) break;
		        
		        line = line.trim();
				if (line.startsWith("%"))	line = line.substring(1);
				line = line.trim();
				particles_info.addElement(line.split(","));
				if (frame_number != Integer.parseInt(particles_info.lastElement()[0]))
				{particles_info.remove(particles_info.size()-1);r.reset();break;}
				
				this.particles_number++;
		    }
	        
	        /* initialise the particles array */
	        this.particles = new Vector<Particle>();
	        
	        Iterator<String[]> iter = particles_info.iterator();
	        int counter = 0;
	        
	        /* go over all particles String info and construct Particles Ojectes from it*/
	        while (iter.hasNext())
	        {
	        	particle_info = iter.next();
	        	
	        	if (particle_info.length < 2)
	        	{
		            IJ.error("Malformed line, expacting 2 float, feeding: " + Integer.toString(particle_info.length) );
		            return false;
	        	}
	        	
	        	if (particle_info.length <= 3)
	        	{
	        		this.particles.addElement(new Particle(
	        				Float.parseFloat(particle_info[1]), Float.parseFloat(particle_info[2]), 0.0f, 
	        				this.frame_number, particle_info, linkrange));
	        	}
	        	else
	        	{
	        		this.particles.addElement(new Particle(
	        				Float.parseFloat(particle_info[1]), Float.parseFloat(particle_info[2]), Float.parseFloat(particle_info[3]), 
	        				this.frame_number, particle_info, linkrange));
	        	}
	        		
	        	//max_coord = Math.max((int)Math.max(this.particles.elementAt(counter).x, this.particles.elementAt(counter).y), max_coord);
	        	//if (momentum_from_text) {
	        	if (particle_info.length < 8 || particle_info[3] == null || particle_info[4] == null || particle_info[5] == null || particle_info[6] == null || particle_info[7] == null ||particle_info[8] == null) {
//	        		IJ.error("File: " + path + "\ndoes not have momentum values at positions 4 to 8 for all particles");
//	        		this.particles = null;
//	        		return false;
	        		this.particles.elementAt(counter).m0 = 0;
	        		this.particles.elementAt(counter).m1 = 0;
	        		this.particles.elementAt(counter).m2 = 0;
	        		this.particles.elementAt(counter).m3 = 0;
	        		this.particles.elementAt(counter).m4 = 0;
	        	} else {
	        		this.particles.elementAt(counter).m0 = Float.parseFloat(particle_info[3]);
	        		this.particles.elementAt(counter).m1 = Float.parseFloat(particle_info[4]);
	        		this.particles.elementAt(counter).m2 = Float.parseFloat(particle_info[5]);
	        		this.particles.elementAt(counter).m3 = Float.parseFloat(particle_info[6]);
	        		this.particles.elementAt(counter).m4 = Float.parseFloat(particle_info[7]);
	        	}
	        	//}
	        	counter++;
	        }
	        if (particles_info != null) particles_info.removeAllElements();
	        return true;
		}
		
		private boolean loadParticlesFromFile(BufferedReader r,String path) throws IOException 
		{    
			Vector<String[]> particles_info = new Vector<String[]>(); 	// a vector to hold all particles info as String[]
			String[] particle_info; 				// will hold all the info for one particle (splitted)
			String[] frame_number_info;				// will fold the frame info line (splitted)
			String line;
	            
	        /* set this frame number from the first line*/
	        line = r.readLine();
	        if (line == null || !line.startsWith("frame")) {
	            IJ.error("File: " + path + "\ndoesnt have the string 'frame' in the begining if the first line");
	            return false;
	        }
	        line = line.trim();
	        frame_number_info = line.split("\\s+");
	        if (frame_number_info.length < 2) {
	            IJ.error("Malformed line, expacting \"frame x\", founded " + line);
	            return false;
	        }
	        if (frame_number_info[1] != null) {
	            this.frame_number = Integer.parseInt(frame_number_info[1]);
	        }
	            
		    /* go over all lines, count number of particles and save the information as String */
	        while (true) {
		        line = r.readLine();
		        if (line == null) break;
		        line = line.trim();
				if (line.startsWith("%"))	line = line.substring(1);
				line = line.trim();
				particles_info.addElement(line.split("\\s+"));
				this.particles_number++;
		    }
	        
	        /* initialise the particles array */
	        this.particles = new Vector<Particle>();
	        
	        Iterator<String[]> iter = particles_info.iterator();
	        int counter = 0;
	        
	        /* go over all particles String info and construct Particles Objects from it*/
	        while (iter.hasNext()) {
	        	particle_info = iter.next();
	        	
	        	if (particle_info.length < 2)
	        	{
		            IJ.error("Malformed line, expacting 2 float, feeding: " + Integer.toString(particle_info.length) );
		            return false;
	        	}
	        	
	        	if (particle_info.length == 2)
	        	{
	        		this.particles.addElement(new Particle(
	        				Float.parseFloat(particle_info[0]), Float.parseFloat(particle_info[1]), 0.0f, 
	        				this.frame_number, particle_info, linkrange));
	        	}
	        	else
	        	{
	        		this.particles.addElement(new Particle(
	        				Float.parseFloat(particle_info[0]), Float.parseFloat(particle_info[1]), Float.parseFloat(particle_info[2]), 
	        				this.frame_number, particle_info, linkrange));
	        	}
	        		
	        	//max_coord = Math.max((int)Math.max(this.particles.elementAt(counter).x, this.particles.elementAt(counter).y), max_coord);
	        	//if (momentum_from_text) {
	        	if (particle_info.length < 8 || particle_info[3] == null || particle_info[4] == null || particle_info[5] == null || particle_info[6] == null || particle_info[7] == null ||particle_info[8] == null) {
//	        		IJ.error("File: " + path + "\ndoes not have momentum values at positions 4 to 8 for all particles");
//	        		this.particles = null;
//	        		return false;
	        		this.particles.elementAt(counter).m0 = 0;
	        		this.particles.elementAt(counter).m1 = 0;
	        		this.particles.elementAt(counter).m2 = 0;
	        		this.particles.elementAt(counter).m3 = 0;
	        		this.particles.elementAt(counter).m4 = 0;
	        	} else {
	        		this.particles.elementAt(counter).m0 = Float.parseFloat(particle_info[3]);
	        		this.particles.elementAt(counter).m1 = Float.parseFloat(particle_info[4]);
	        		this.particles.elementAt(counter).m2 = Float.parseFloat(particle_info[5]);
	        		this.particles.elementAt(counter).m3 = Float.parseFloat(particle_info[6]);
	        		this.particles.elementAt(counter).m4 = Float.parseFloat(particle_info[7]);
	        	}
	        	//}
	        	counter++;
	        }
	        if (particles_info != null) particles_info.removeAllElements();
	        return true;
		}
		
		
		/**
		 * ONLY FOR text_files_mode.
		 * <br>Loads particles information for this frame from the file located 
		 * at the given path and adds these particles to the <code>particles</code> array. 
		 * <br>These particles are considered to be "after discrimination".
		 * <br>File must have the word 'frame' (case sensitive) at the beginning of the first line
		 * followed by any number of space characters (\t \n) and the frame number.
		 * <br>Each next line represents a particle in the frame number given at the first line.
		 * <br>Each line must have 2 numbers or more separated by one or more space characters.
		 * <br>The 2 first numbers represents the X and Y coordinates of the particle (respectfully).
		 * <br>The next numbers represent other information of value about the particle
		 * (this information can be plotted later along a trajectory).
		 * <br>The number of parameters must be equal for all particles.
		 * <br>For more about X and Y coordinates (they are not in the usual graph coord) see <code>Particle</code>  
		 * @param path full path to the file (including full file name) e.g c:\ImageJ\frame0.txt
		 * @return false if there was any problem
		 * @see Particle   
		 */
	/*	private boolean loadParticlesFromFile (String path) {
	        
			Vector<String[]> particles_info = new Vector<String[]>(); 	// a vector to hold all particles info as String[]
			String[] particle_info; 				// will hold all the info for one particle (splitted)
			String[] frame_number_info;				// will fold the frame info line (splitted)
			String line;
			
	        try {	        	*/
	            /* open the file */
	  /*      	BufferedReader r = new BufferedReader(new FileReader(path));*/
	            
	            /* set this frame number from the first line*/
/*	            line = r.readLine();
	            if (line == null || !line.startsWith("frame")) {
	            	IJ.error("File: " + path + "\ndoesnt have the string 'frame' in the begining if the first line");
	            	return false;
	            }
	            line = line.trim();
	            frame_number_info = line.split("\\s+");
	            if (frame_number_info.length < 2) {
	            	IJ.error("Malformed line expacting \"frame x\", founded " + line);
	            	return false;
	            }
	            if (frame_number_info[1] != null) {
	            	this.frame_number = Integer.parseInt(frame_number_info[1]);
	            }*/
	            
		        /* go over all lines, count number of particles and save the information as String */
/*	            while (true) {
		            line = r.readLine();		            
		            if (line == null) break;
		            line = line.trim();
					if (line.startsWith("%"))	line = line.substring(1);
					line = line.trim();
					particles_info.addElement(line.split("\\s+"));
					this.particles_number++;
		        }*/
	            /* close file */
/*	            r.close();
	        }
	        catch (Exception e) {
	            IJ.error(e.getMessage());
	            return false;
	        }*/
	        
	        /* initialise the particles array */
/*	        this.particles = new Vector<Particle>();
	        
	        Iterator<String[]> iter = particles_info.iterator();
	        int counter = 0;*/
	        
	        /* go over all particles String info and construct Particles Ojectes from it*/
/*	        while (iter.hasNext()) {
	        	particle_info = iter.next();
	        	
	        	if (particle_info[2] == null)
	        	{
	        		this.particles.addElement(new Particle(
	        				Float.parseFloat(particle_info[0]), Float.parseFloat(particle_info[1]), 0.0f, 
	        				this.frame_number, particle_info, linkrange));
	        	}
	        	else
	        	{
	        		this.particles.addElement(new Particle(
	        				Float.parseFloat(particle_info[0]), Float.parseFloat(particle_info[1]), Float.parseFloat(particle_info[2]), 
	        				this.frame_number, particle_info, linkrange));
	        	}
	        		
	        	//max_coord = Math.max((int)Math.max(this.particles.elementAt(counter).x, this.particles.elementAt(counter).y), max_coord);
	        	//if (momentum_from_text) {
	        	if (particle_info.length < 8 || particle_info[3] == null || particle_info[4] == null || particle_info[5] == null || particle_info[6] == null || particle_info[7] == null ||particle_info[8] == null) {
//	        		IJ.error("File: " + path + "\ndoes not have momentum values at positions 4 to 8 for all particles");
//	        		this.particles = null;
//	        		return false;
	        		this.particles.elementAt(counter).m0 = 0;
	        		this.particles.elementAt(counter).m1 = 0;
	        		this.particles.elementAt(counter).m2 = 0;
	        		this.particles.elementAt(counter).m3 = 0;
	        		this.particles.elementAt(counter).m4 = 0;
	        	} else {
	        		this.particles.elementAt(counter).m0 = Float.parseFloat(particle_info[3]);
	        		this.particles.elementAt(counter).m1 = Float.parseFloat(particle_info[4]);
	        		this.particles.elementAt(counter).m2 = Float.parseFloat(particle_info[5]);
	        		this.particles.elementAt(counter).m3 = Float.parseFloat(particle_info[6]);
	        		this.particles.elementAt(counter).m4 = Float.parseFloat(particle_info[7]);
	        	}
	        	//}
	        	counter++;
	        }
	        if (particles_info != null) particles_info.removeAllElements();
	        return true;
		}*/

		/**
		 * Generates a "ready to print" string with all the 
		 * particles positions AFTER discrimination in this frame.
		 * @return a <code>StringBuffer</code> with the info
		 */
		private StringBuffer getFrameInfoAfterDiscrimination() {


//			NumberFormat nf = NumberFormat.getInstance();
//			nf.setMaximumFractionDigits(6);
//			nf.setMinimumFractionDigits(6); 
			 
			DecimalFormat nf = new DecimalFormat("#####0.000000");
			nf.setGroupingUsed(false);
			// I work with StringBuffer since its faster than String
			StringBuffer info = new StringBuffer("%\tParticles after non-particle discrimination (");
			info.append(this.real_particles_number);
			info.append(" particles):\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info.append("%\t\t");
				info.append(nf.format(this.particles.elementAt(i).x));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).y));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).z));

				//special version:
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m0));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m1));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m2));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m3));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m4));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).score));

				info.append("\n");

			}
			return info;
		}

		/**
		 * Generates a "ready to print" StringBuffer with all the particles initial
		 * and refined positions BEFORE discrimination in this frame.
		 * <br>sets <code>info_before_discrimination</code> to hold this info
		 * @see #info_before_discrimination
		 */
		public void generateFrameInfoBeforeDiscrimination() {

//			NumberFormat nf = NumberFormat.getInstance();
//			nf.setMaximumFractionDigits(6);
//			nf.setMinimumFractionDigits(6); 
			 
			DecimalFormat nf = new DecimalFormat("#####0.000000");
			nf.setGroupingUsed(false);

			// I work with StringBuffer since its faster than String
			StringBuffer info = new StringBuffer("% Frame ");
			info.append(this.frame_number);
			info.append(":\n");
			info.append("%\t");
			info.append(this.particles_number);
			info.append(" particles found\n");
			info.append("%\tDetected particle positions:\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info.append("%\t\t");
				info.append(nf.format(this.particles.elementAt(i).original_x));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).original_y));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).original_z));
				info.append("\n");
			}
			info.append("%\tParticles after position refinement:\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info.append("%\t\t");
				info.append(nf.format(this.particles.elementAt(i).x));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).y));
				info.append(" ");
				info.append(nf.format(this.particles.elementAt(i).z));

				//special version:
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m0));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m1));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m2));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m3));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).m4));
				//				info.append(" ");
				//				info.append(nf.format(this.particles.elementAt(i).score));

				info.append("\n");
			}
			info_before_discrimination = info;
		}

		/**
		 * Generates (in real time) a "ready to print" StringBuffer with this frame 
		 * information before and after non particles discrimination
		 * @return a StringBuffer with the info
		 * @see MyFrame#getFrameInfoAfterDiscrimination()
		 * @see #info_before_discrimination
		 */
		public StringBuffer getFullFrameInfo() {
			StringBuffer info = new StringBuffer();
			info.append(info_before_discrimination);
			info.append(getFrameInfoAfterDiscrimination());
			return info;					
		}

		/**
		 * Generates a "ready to print" string that shows for each particle in this frame 
		 * (AFTER discrimination) all the particles it is linked to.
		 * @return a String with the info
		 */	
		public String toString() {			
			return toStringBuffer().toString();
		}

		/**
		 * The method <code>toString()</code> calls this method
		 * <br>Generates a "ready to print" StringBuffer that shows for each particle in this frame 
		 * (AFTER discrimination) all the particles it is linked to.
		 * @return a <code>StringBuffer</code> with the info
		 */	
		public StringBuffer toStringBuffer() 
		{
			// work with StringBuffer since its faster than String
			 
			DecimalFormat nf = new DecimalFormat("#####0.000000");
			nf.setGroupingUsed(false);
			StringBuffer sb = new StringBuffer("% Frame ");
			sb.append(this.frame_number);
			sb.append("\n");
			for(int j = 0; j < this.particles.size(); j++) 
			{
				sb.append("%\tParticle ");
				sb.append(j);
				sb.append(" (");
				sb.append(nf.format(this.particles.elementAt(j).x));
				sb.append(", ");
				sb.append(nf.format(this.particles.elementAt(j).y));
				sb.append(", ");		
				sb.append(nf.format(this.particles.elementAt(j).z));
				sb.append(")\n");	
				for(int k = 0; k < linkrange; k++) 
				{
					sb.append("%\t\tlinked to particle ");
					sb.append(this.particles.elementAt(j).next[k]);
					sb.append(" in frame ");
					sb.append((this.frame_number + k + 1));
					sb.append("\n");					
				}
			}
			return sb;
		}

		/**
		 * 
		 * Return particles vector
		 * 
		 * @return a vector of particle
		 */
		
		public Vector<Particle> getParticles(){
			return this.particles;
		}
		
		/**
		 * Generates (in real time) a "ready to save" <code>StringBuffer</code> with information
		 * about the detected particles defined in this MyFrame.
		 * <br>The format of the returned <code>StringBuffer</code> is the same as expected when 
		 * loading particles information from text files
		 * @param with_momentum if true, the momentum values (m0, m2) are also included
		 * if false - only x and y values are included
		 * @return the <code>StringBuffer</code> with this information
		 * @see MyFrame#loadParticlesFromFile(String) 
		 */
		public StringBuffer frameDetectedParticlesForSave(boolean with_momentum) 
		{	 
			DecimalFormat nf = new DecimalFormat("#####0.000000");
			nf.setGroupingUsed(false);
			StringBuffer info1 = new StringBuffer("frame ");
			info1.append(this.frame_number);
			info1.append("\n");
			for (int i = 0; i<this.particles.size(); i++) {
				info1.append(nf.format(this.particles.elementAt(i).x));
				info1.append(" ");
				info1.append(nf.format(this.particles.elementAt(i).y));		
				info1.append(" ");
				info1.append(nf.format(this.particles.elementAt(i).z));	
				if (with_momentum) {
					info1.append(" ");
					info1.append(nf.format(this.particles.elementAt(i).m0));
					info1.append(" ");
					info1.append(nf.format(this.particles.elementAt(i).m2));					
				}
				info1.append("\n");				
			}
			return info1;
		}

		/**
		 * Creates a <code>ByteProcessor</code> and draws on it the particles defined in this MyFrame 
		 * <br>The background color is <code>Color.black</code>
		 * <br>The color of the dots drawn for each particle is <code>Color.white</code>
		 * <br>particles position have floating point precision but can be drawn only at integer precision - 
		 * therefore the created image is only an estimation
		 * @param width defines the width of the created <code>ByteProcessor</code>
		 * @param height defines the height of the created <code>ByteProcessor</code>
		 * @return the created processor
		 * @see ImageProcessor#drawDot(int, int)
		 * 
		 * @deprecated
		 */
		@SuppressWarnings("unused")
		private ImageStack createImage(int width, int height, int depth) 
		{
			ImageStack is = new ImageStack(width, height);
			for(int d = 0; d < depth; d++) 
			{
				ImageProcessor ip = new ByteProcessor(width, height);
				ip.setColor(Color.black);
				ip.fill();
				is.addSlice(null, ip);
				ip.setColor(Color.white);
			}
			for (int i = 0; i<this.particles.size(); i++) 
			{
				is.getProcessor(Math.round(this.particles.elementAt(i).z) + 1).drawDot(
						Math.round(this.particles.elementAt(i).y), 
						Math.round(this.particles.elementAt(i).x));
			}
			return is;		
		}

		
		

		/**
		 * Creates a <code>ByteProcessor</code> and draws on it the particles defined in this MyFrame 
		 * <br>The background color is <code>Color.black</code>
		 * <br>The color of the dots drawn for each particle is <code>Color.white</code>
		 * <br>particles position have floating point precision but can be drawn only at integer precision - 
		 * therefore the created image is only an estimation
		 * @param width defines the width of the created <code>ByteProcessor</code>
		 * @param height defines the height of the created <code>ByteProcessor</code>
		 * @return the created processor
		 * @see ImageProcessor#drawDot(int, int)
		 * 
		 * @deprecated
		 */
		public ImageProcessor createImage(int width, int height) {
			ImageProcessor ip = new ByteProcessor(width, height);
			ip.setColor(Color.black);
			ip.fill();
			ip.setColor(Color.white);
			for (int i = 0; i<this.particles.size(); i++) {
				ip.drawDot(Math.round(this.particles.elementAt(i).y), Math.round(this.particles.elementAt(i).x));
			}
			return ip;		
		}

		public void setParticles(Vector<Particle> particles, int particles_number) {
			this.particles = particles;
			this.particles_number = particles_number;
		}
		
		public ImageStack getOriginalImageStack(){
			return this.original_ips;
		}
		

		public <T extends IntegerType<T>> ARGBType convertoARGBType(T data)
		{
			ARGBType t = new ARGBType();
			int td = data.getInteger();
			
			t.set(ARGBType.rgba(td, td, td, 255.0f));
			return t;
		}
		
		static private void drawParticlesWithRadius(RandomAccessibleInterval<ARGBType> out, List<Particle> pt , Calibration cal, int col, int p_radius)
		{
			RandomAccess<ARGBType> out_a = out.randomAccess();
			
	        int sz[] = new int [out_a.numDimensions()];
		   	 
	        for ( int d = 0; d < out_a.numDimensions(); ++d )
	        {
	            sz[d] = (int) out.dimension( d );
	        }
			
	        // Iterate on all particles
	        
	        double radius = p_radius;

	        float sp[] = new float[out_a.numDimensions()];
	        	
	    	float scaling[] = new float[3];
	    		
	    	if (cal != null)
	    	{
	    		scaling[0] = (float) (cal.pixelWidth);
	    		scaling[1] = (float) (cal.pixelHeight);
	    		scaling[2] = (float) (cal.pixelDepth);
	    	}
	    	else
	    	{
	    		scaling[0] = 1.0f;
	    		scaling[1] = 1.0f;
	    		scaling[2] = 1.0f;
	    	}
	    		
	    	// Create a circle Mask and an iterator
	    	
	    	RegionIteratorMask rg_m = null;
	    	
	    	float min_s = minScaling(scaling);
	    	int rc = (int) (radius / min_s);
	    	scaling[0] /= min_s;
	    	scaling[1] /= min_s;
	    	scaling[2] /= min_s;
		    if ((rg_m = CircleCache.get(rc)) == null)
		    {
		    	if (rc < 1) rc = 1;
	        	CircleMask cm = new CircleMask(rc, 2*rc + 1, out_a.numDimensions(), scaling);
	        	rg_m = new RegionIteratorMask(cm, sz);
	    		CircleCache.put(rc, rg_m);
	    	}
	    	
	        Iterator<Particle> pt_it = pt.iterator();
	        
	        while (pt_it.hasNext())
	        {
	        	Particle ptt = pt_it.next();
	        	
	        	// Draw the Circle
	        			
	        	Point p_c = null;
	        	if (out_a.numDimensions() == 2)
	        		p_c = new Point((int)(ptt.x/scaling[0]),(int)(ptt.y/scaling[1]));
	        	else
	        		p_c = new Point((int)(ptt.x/scaling[0]),(int)(ptt.y/scaling[1]),(int)(ptt.z/scaling[2]));
	        			
	        			
	        	rg_m.setMidPoint(p_c);
	        			
		        while ( rg_m.hasNext() )
		        {
		        	Point p = rg_m.nextP();
		        			
		        	if (p.isInside(sz))
		        	{
		        		out_a.setPosition(p.x);
		        		out_a.get().set(col);
		        	}
		        }
	        }
		}
		
		public static float minScaling(float s[])
		{
			float min = Float.MAX_VALUE;
			for (int i = 0 ; i < s.length ; i++)
			{
				if (s[i] < min)
					min = s[i];
			}
			
			return min;
		}
		
		static private void drawParticles(RandomAccessibleInterval<ARGBType> out, List<Particle> pt , Calibration cal, int col)
		{
			RandomAccess<ARGBType> out_a = out.randomAccess();
			
	        int sz[] = new int [out_a.numDimensions()];
		   	 
	        for ( int d = 0; d < out_a.numDimensions(); ++d )
	        {
	            sz[d] = (int) out.dimension( d );
	        }
			
	        // Iterate on all particles
	        
	        while (pt.size() != 0)
	        {
	        	double radius;
	        	if (out_a.numDimensions() == 2)
	        		radius = Math.sqrt(pt.get(0).m0/Math.PI);
	        	else
	        		radius = Math.cbrt(pt.get(0).m0*3.0/4.0/Math.PI);
	        	
	        	if (radius < 1.0)
	        		radius = 1.0;
	        	
	        	float sp[] = new float[out_a.numDimensions()];
	        	
	    		float scaling[] = new float[3];
	    		float scaling_[] = new float[3];
	    		
	    		if (cal != null)
	    		{
	    			scaling[0] = (float) (cal.pixelWidth);
	    			scaling[1] = (float) (cal.pixelHeight);
	    			scaling[2] = (float) (cal.pixelDepth);
	    		}
	    		else
	    		{
	    			scaling[0] = 1.0f;
	    			scaling[1] = 1.0f;
	    			scaling[2] = 1.0f;
	    		}
	    		
	    		// Create a circle Mask and an iterator
	    		
		    	RegionIteratorMask rg_m = null;
		    	
		    	float min_s = minScaling(scaling);
		    	int rc = (int) (radius / min_s);
		    	scaling_[0] = scaling[0] / min_s;
		    	scaling_[1] = scaling[1] / min_s;
		    	scaling_[2] = scaling[2] / min_s;
			    if ((rg_m = CircleCache.get(rc)) == null)
			    {
			    	if (rc < 1) rc = 1;
		        	CircleMask cm = new CircleMask(rc, 2*rc + 1, out_a.numDimensions(), scaling_);
		        	rg_m = new RegionIteratorMask(cm, sz);
		    		CircleCache.put(rc, rg_m);
		    	}
	        	
	        	Iterator<Particle> pt_it = pt.iterator();
	        	
	        	while (pt_it.hasNext())
	        	{
	        		Particle ptt = pt_it.next();
	        		
		        	double radius_r;
		        	if (out_a.numDimensions() == 2)
		        		radius_r = Math.sqrt(pt.get(0).m0/Math.PI);
		        	else
		        		radius_r = Math.cbrt(pt.get(0).m0*3.0/4.0/Math.PI);
	        		
	        		if (radius_r == 0) radius_r = 1;
	        		
	        		// if particle has the same radius
	        	
	        		if (radius_r == radius)
	        		{
	        			// Draw the Circle
	        			
	        			Point p_c = null;
	        			if (out_a.numDimensions() == 2)
	        				p_c = new Point((int)(ptt.x/scaling[0]),(int)(ptt.y/scaling[1]));
	        			else
	        				p_c = new Point((int)(ptt.x/scaling[0]),(int)(ptt.y/scaling[1]),(int)(ptt.z/scaling[2]));
	        			
	        			
	        			rg_m.setMidPoint(p_c);
	        			
		        		while ( rg_m.hasNext() )
		        		{
		        			Point p = rg_m.nextP();
		        			
		        			if (p.isInside(sz))
		        			{
		        				out_a.setPosition(p.x);
		        				out_a.get().set(col);
		        			}
		        		}	
		        		pt_it.remove();
	        		}
	        	}
	        }
		}
		

		/**
		 * 
		 * Bresenham line 3D algorithm
		 * 
		 * @param out Image where to draw
		 * @param p1 start point
		 * @param p2 end line
		 * @param col Color of the line
		 */
		static private void drawLine(RandomAccessibleInterval<ARGBType> out, Particle p1, Particle p2, int col)
		{
	        // the number of dimensions
	        int numDimensions = out.numDimensions();
	        
	        long dims[] = new long[numDimensions];
	        out.dimensions(dims);
			
			RandomAccess<ARGBType> out_a = out.randomAccess();

			    int i, dx, dy, dz, l, m, n, x_inc, y_inc, z_inc, err_1, err_2, dx2, dy2, dz2;
			    long pixel[] = new long[3];
			    
			    pixel[0] = (int) p1.x;
			    pixel[1] = (int) p1.y;
			    pixel[2] = (int) p1.z;
			    dx = (int) (p2.x - p1.x);
			    dy = (int) (p2.y - p1.y);
			    dz = (int) (p2.z - p1.z);
			    x_inc = (dx < 0) ? -1 : 1;
			    l = Math.abs(dx);
			    y_inc = (dy < 0) ? -1 : 1;
			    m = Math.abs(dy);
			    z_inc = (dz < 0) ? -1 : 1;
			    n = Math.abs(dz);
			    dx2 = l << 1;
			    dy2 = m << 1;
			    dz2 = n << 1;

			    if ((l >= m) && (l >= n)) 
			    {
			        err_1 = dy2 - l;
			        err_2 = dz2 - l;
			        for (i = 0; i < l; i++) 
			        {
			        	boolean out_pix = false;
			        	for (int k = 0 ; k < out_a.numDimensions(); k++)
			        	{
			        		if (pixel[k] >= dims[k])
			        		{
			        			out_pix = true;
			        			break;
			        		}
			        	}
			        	
			        	if (out_pix == true)
			        		continue;
			        	
		    	        out_a.setPosition(pixel);
		    	        	
		    	        out_a.get().set(col);
			            if (err_1 > 0) 
			            {
			                pixel[1] += y_inc;
			                err_1 -= dx2;
			            }
			            if (err_2 > 0) 
			            {
			                pixel[2] += z_inc;
			                err_2 -= dx2;
			            }
			            err_1 += dy2;
			            err_2 += dz2;
			            pixel[0] += x_inc;
			        }
			    } 
			    else if ((m >= l) && (m >= n)) 
			    {
			        err_1 = dx2 - m;
			        err_2 = dz2 - m;
			        for (i = 0; i < m; i++) 
			        {
			        	boolean out_pix = false;
			        	for (int k = 0 ; k < out_a.numDimensions(); k++)
			        	{
			        		if (pixel[k] >= dims[k])
			        		{
			        			out_pix = true;
			        			break;
			        		}
			        	}
			        	
			        	if (out_pix == true)
			        		continue;
			        		
		    	        out_a.setPosition(pixel);
		    	        out_a.get().set(col);
			            if (err_1 > 0) 
			            {
			                pixel[0] += x_inc;
			                err_1 -= dy2;
			            }
			            if (err_2 > 0) 
			            {
			                pixel[2] += z_inc;
			                err_2 -= dy2;
			            }
			            err_1 += dx2;
			            err_2 += dz2;
			            pixel[1] += y_inc;
			        }
			    } 
			    else 
			    {
			        err_1 = dy2 - n;
			        err_2 = dx2 - n;
			        for (i = 0; i < n; i++) 
			        {
			        	boolean out_pix = false;
			        	for (int k = 0 ; k < out_a.numDimensions(); k++)
			        	{
			        		if (pixel[k] >= dims[k])
			        		{
			        			out_pix = true;
			        			break;
			        		}
			        	}
			        	
			        	if (out_pix == true)
			        		continue;
			        	
		    	        out_a.setPosition(pixel);
		    	        out_a.get().set(col);
			            if (err_1 > 0) {
			                pixel[1] += y_inc;
			                err_1 -= dz2;
			            }
			            if (err_2 > 0) {
			                pixel[0] += x_inc;
			                err_2 -= dz2;
			            }
			            err_1 += dy2;
			            err_2 += dx2;
			            pixel[2] += z_inc;
			        }
			    }
			    
	        	boolean out_pix = false;
	        	for (int k = 0 ; k < out_a.numDimensions(); k++)
	        	{
	        		if (pixel[k] >= dims[k])
	        		{
	        			out_pix = true;
	        			break;
	        		}
	        	}
	        	
	        	if (out_pix == false)
	        	{
	        		out_a.setPosition(pixel);
	        		out_a.get().set(col);
	        	}
		}
		
		/**
		 * 
		 * Bresenham line 3D algorithm bold
		 * 
		 * @param out Image where to draw
		 * @param p1 start point
		 * @param p2 end line
		 * @param col Color of the line
		 */
		static private void drawLineBold(RandomAccessibleInterval<ARGBType> out, Particle p1, Particle p2, int col, int w)
		{
			// w
			
			int bold[] = new int [3];
			bold[0] = w;
			bold[1] = w;
			bold[2] = w;
			
			RegionIteratorMask rg_m = null;
			
			if ((rg_m = RectangleCache.get(w)) != null)
			{
				RectangleMask rm = new RectangleMask(bold);
				int size[] = new int [3];
				size[0] = (int) out.dimension(0);
				size[1] = (int) out.dimension(1);
				size[2] = (int) out.dimension(2);
				
				rg_m = new RegionIteratorMask(rm, size);
			}
				
	        // the number of dimensions
	        int numDimensions = out.numDimensions();
	        
	        long dims[] = new long[numDimensions];
	        out.dimensions(dims);
			
			RandomAccess<ARGBType> out_a = out.randomAccess();

			int i, dx, dy, dz, l, m, n, x_inc, y_inc, z_inc, err_1, err_2, dx2, dy2, dz2;
			long pixel[] = new long[3];
			    
			while (rg_m.hasNext())
			{
				Point p = rg_m.nextP();
				Point middle = new Point(w/2,w/2,w/2);
				
			    pixel[0] = (int) p1.x + p.x[0] - middle.x[0];
			    pixel[1] = (int) p1.y + p.x[1] - middle.x[1];
			    pixel[2] = (int) p1.z + p.x[2] - middle.x[2];
			    dx = (int) (p2.x + p.x[0] - middle.x[0] - p1.x);
			    dy = (int) (p2.y + p.x[1] - middle.x[1] - p1.y);
			    dz = (int) (p2.z + p.x[2] - middle.x[2] - p1.z);
			    x_inc = (dx < 0) ? -1 : 1;
			    l = Math.abs(dx);
			    y_inc = (dy < 0) ? -1 : 1;
			    m = Math.abs(dy);
			    z_inc = (dz < 0) ? -1 : 1;
			    n = Math.abs(dz);
			    dx2 = l << 1;
			    dy2 = m << 1;
			    dz2 = n << 1;

			    if ((l >= m) && (l >= n)) 
			    {
			        err_1 = dy2 - l;
			        err_2 = dz2 - l;
			        for (i = 0; i < l; i++) 
			        {
			        	boolean out_pix = false;
			        	for (int k = 0 ; k < out_a.numDimensions(); k++)
			        	{
			        		if (pixel[k] >= dims[k])
			        		{
			        			out_pix = true;
			        			break;
			        		}
			        	}
			        	
			        	if (out_pix == true)
			        		continue;
			        	
		    	        out_a.setPosition(pixel);
		    	        	
		    	        out_a.get().set(col);
			            if (err_1 > 0) 
			            {
			                pixel[1] += y_inc;
			                err_1 -= dx2;
			            }
			            if (err_2 > 0) 
			            {
			                pixel[2] += z_inc;
			                err_2 -= dx2;
			            }
			            err_1 += dy2;
			            err_2 += dz2;
			            pixel[0] += x_inc;
			        }
			    } 
			    else if ((m >= l) && (m >= n)) 
			    {
			        err_1 = dx2 - m;
			        err_2 = dz2 - m;
			        for (i = 0; i < m; i++) 
			        {
			        	boolean out_pix = false;
			        	for (int k = 0 ; k < out_a.numDimensions(); k++)
			        	{
			        		if (pixel[k] >= dims[k])
			        		{
			        			out_pix = true;
			        			break;
			        		}
			        	}
			        	
			        	if (out_pix == true)
			        		continue;
			        		
		    	        out_a.setPosition(pixel);
		    	        out_a.get().set(col);
			            if (err_1 > 0) 
			            {
			                pixel[0] += x_inc;
			                err_1 -= dy2;
			            }
			            if (err_2 > 0) 
			            {
			                pixel[2] += z_inc;
			                err_2 -= dy2;
			            }
			            err_1 += dx2;
			            err_2 += dz2;
			            pixel[1] += y_inc;
			        }
			    } 
			    else 
			    {
			        err_1 = dy2 - n;
			        err_2 = dx2 - n;
			        for (i = 0; i < n; i++) 
			        {
			        	boolean out_pix = false;
			        	for (int k = 0 ; k < out_a.numDimensions(); k++)
			        	{
			        		if (pixel[k] >= dims[k])
			        		{
			        			out_pix = true;
			        			break;
			        		}
			        	}
			        	
			        	if (out_pix == true)
			        		continue;
			        	
		    	        out_a.setPosition(pixel);
		    	        out_a.get().set(col);
			            if (err_1 > 0) {
			                pixel[1] += y_inc;
			                err_1 -= dz2;
			            }
			            if (err_2 > 0) {
			                pixel[0] += x_inc;
			                err_2 -= dz2;
			            }
			            err_1 += dy2;
			            err_2 += dx2;
			            pixel[2] += z_inc;
			        }
			    }
			    
	        	boolean out_pix = false;
	        	for (int k = 0 ; k < out_a.numDimensions(); k++)
	        	{
	        		if (pixel[k] >= dims[k])
	        		{
	        			out_pix = true;
	        			break;
	        		}
	        	}
	        	
	        	if (out_pix == false)
	        	{
	        		out_a.setPosition(pixel);
	        		out_a.get().set(col);
	        	}
			}
		}
		
		static private void drawLines(RandomAccessibleInterval<ARGBType> out, List<pParticle> lines , Calibration cal, int col)
		{
			if (cal == null)
			{
				cal = new Calibration();
				cal.pixelDepth = 1.0;
				cal.pixelHeight = 1.0;
				cal.pixelWidth = 1.0;
			}
			
			RandomAccess<ARGBType> out_a = out.randomAccess();
			
	        int sz[] = new int [out_a.numDimensions()];
		   	
	        for ( int d = 0; d < out_a.numDimensions(); ++d )
	        {
	            sz[d] = (int) out.dimension( d );
	        }
			
	        // Iterate on all lines
	        
	        for (pParticle ptt : lines )
	        {
	        	Particle p_end = new Particle(ptt.p1);
	        	Particle p_ini = new Particle(ptt.p2);
	        	
	        	if (cal != null)
	        	{
	        		p_ini.x /= (float)cal.pixelWidth;
	        		p_ini.y /= (float)cal.pixelHeight;
	        		p_ini.z /= (float)cal.pixelDepth;
    	
	        		p_end.x /= (float)cal.pixelWidth;
	        		p_end.y /= (float)cal.pixelHeight;
	        		p_end.z /= (float)cal.pixelDepth;
	        	}
	        	
	        	drawLine(out,p_ini,p_end, col);
	        	
	        	double radius = Math.cbrt(ptt.p1.m0 / 3.0f * 4.0f);
	        	
	        	float scaling[] = new float[3];
	        	scaling[0] = (float) cal.pixelWidth;
	        	scaling[1] = (float) cal.pixelHeight;
	        	scaling[2] = (float) cal.pixelDepth;
		    	float min_s = minScaling(scaling);
		    	int rc = (int) (radius / min_s);
		    	scaling[0] /= min_s;
		    	scaling[1] /= min_s;
		    	scaling[2] /= min_s;
		    	rc /= scaling[2];

	        	
	        	// draw several lines on z
	        	
	        	for (int i = 1 ; i <= rc ; i++)
	        	{
	        		if (ptt.p1.z / (float)cal.pixelDepth - i >= 0 && ptt.p2.z / (float)cal.pixelDepth - i >= 0)
	        		{
	    	        	p_end = new Particle(ptt.p1);
	    	        	p_ini = new Particle(ptt.p2);
	        	
	    	        	if (cal != null)
	    	        	{
	    	        		p_ini.x /= (float)cal.pixelWidth;
	    	        		p_ini.y /= (float)cal.pixelHeight;
		        			p_ini.z = p_ini.z / (float)cal.pixelDepth - i;
	        	
	    	        		p_end.x /= (float)cal.pixelWidth;
	    	        		p_end.y /= (float)cal.pixelHeight;
		        			p_end.z = p_end.z / (float)cal.pixelDepth - i;
	    	        	}
	        			
	        			drawLine(out,p_ini,p_end, col);
	        		}
	        		
	        		
	        		
	        		if (ptt.p2.x / (float)cal.pixelDepth + i < out.dimension(out.numDimensions()-1) && ptt.p1.z / (float)cal.pixelDepth + i < out.dimension(out.numDimensions()-1))
	        		{
	    	        	p_end = new Particle(ptt.p1);
	    	        	p_ini = new Particle(ptt.p2);
	        	
	    	        	if (cal != null)
	    	        	{
	    	        		p_ini.x /= (float)cal.pixelWidth;
	    	        		p_ini.y /= (float)cal.pixelHeight;
		        			p_ini.z = p_ini.z / (float)cal.pixelDepth + i;
	        	
	    	        		p_end.x /= (float)cal.pixelWidth;
	    	        		p_end.y /= (float)cal.pixelHeight;
		        			p_end.z = p_end.z /(float)cal.pixelDepth + i;
	    	        	}
	        			
	        			drawLine(out,p_ini,p_end, col);
	        		}
	        	}
	        }
		}
		
		/**
		 * 
		 * Draw particles on out image
		 * 
		 * @param out Out image
		 * @param particles particles vector
		 * @param cal Calibration (scaling factor between particle position and the image pixel)
		 * @param col Color
		 */
		
		static private void drawParticles(Img<ARGBType> out, Vector<Particle> particles, Calibration cal, int col)
		{
	        // Create a list of particles
	        
	        List<Particle> pt = new ArrayList<Particle>();
	 
	        for (int i = 0 ; i < particles.size() ; i++)
	        {
	        	pt.add(particles.get(i));
	        }
	        
	        drawParticles(out,pt,cal,col);
		}
		
		/**
		 * 
		 * Draw particles on out image
		 * 
		 * @param out Out image
		 * @param cal Calibration (scaling factor between particle position and the image pixel)
		 * @param col Color
		 */
		
		private void drawParticles(Img<ARGBType> out, Calibration cal, int col)
		{
	        // Create a list of particles
	        
	        List<Particle> pt = new ArrayList<Particle>();
	 
	        for (int i = 0 ; i < particles.size() ; i++)
	        {
	        	pt.add(particles.get(i));
	        }
	        
	        if (p_radius == -1)
	        	drawParticles(out,pt,cal,col);
	        else
	        	drawParticlesWithRadius(out,pt,cal,col,p_radius);
		}
		
		/**
		 * 
		 * Create an image from the particle information
		 * 
		 * @param vMax size of the image
		 * @param frame number
		 * @return the image
		 */
		
		public  Img<ARGBType> createImage( int [] vMax ,int frame)
		{	        
	        // Create image
	        
	        final ImgFactory< ARGBType > imgFactory = new ArrayImgFactory< ARGBType >();
	        Img<ARGBType> out = imgFactory.create(vMax, new ARGBType());
	        
	        drawParticles(out,null,ARGBType.rgba(255, 0, 0, 255));
	        
	        return out;
		}
		
		/**
		 * 
		 * Create an image from particle information with background
		 * 
		 * @param background background image
		 * @param cal calibration
		 * @return image video
		 */
		
		public  <T> Img<ARGBType> createImage(Img<T> background, Calibration cal)
		{
	        // the number of dimensions
	        int numDimensions = background.numDimensions();
	        
	        long dims[] = new long[numDimensions];
	        background.dimensions(dims);
	        
	        // Create image
	        
	        final ImgFactory< ARGBType > imgFactory = new ArrayImgFactory< ARGBType >();
	        Img<ARGBType> out = imgFactory.create(dims, new ARGBType());
	        
	        Cursor<ARGBType> curOut = out.cursor();
	        Cursor<T> curBack = background.cursor();
	        
	        if (curBack.hasNext())
	        {
	        	curOut.fwd();
	        	curBack.fwd();
	        }
        	
	        // get conversion;
	        
	        ToARGB conv = MosaicUtils.getConversion(curBack.get());
	        
	        // Copy the background
	        
	        while (curBack.hasNext())
	        {
	        	curOut.fwd();
	        	curBack.fwd();
	        	
	        	curOut.get().set(conv.toARGB(curBack.get()));
	        }
	        
	        drawParticles(out,cal, ARGBType.rgba(255, 0, 0, 255));
	        
	        return out;
		}
		
		public enum DrawType
		{
			TRAJECTORY_HISTORY,
			PREV,
			NEXT,
			PREV_NEXT,
			TRAJECTORY_HISTORY_WITH_NEXT
		}
		
		class pParticle
		{
			Particle p1;
			Particle p2;
			
			pParticle(Particle p1, Particle p2)
			{
				this.p1 = p1;
				this.p2 = p2;
			}
			
			void translate(Rectangle focus)
			{
				p1.translate(focus);
				p2.translate(focus);
			}
		}
		
		/**
		 * 
		 * Update the image
		 * 
		 * @param out An array of frames
		 * @param A focus area
		 * @param a Vector of trajectories
		 * @param start_frame of the focus view
		 * @param Type of draw
		 *
		 */
		
		static public void updateImage(RandomAccessibleInterval<ARGBType> out , Rectangle focus, int start_frame, Vector<Trajectory> tr, Calibration cal , DrawType typ)
		{
			// Adjust calibration according to magnification
			
			int scale_x = (int) (out.dimension(0) / focus.width);
			int scale_y = (int) (out.dimension(1) / focus.height);
			
			Calibration cal_ = new Calibration();
			cal_.pixelWidth = cal.pixelWidth / scale_x;
			cal_.pixelHeight = cal.pixelHeight / scale_y;
			cal_.pixelDepth = cal.pixelDepth;
			
			// Get image
	        
			MyFrame f = new MyFrame();
	        Cursor<ARGBType> curOut = Views.iterable(out).cursor();
	        
	        //
	        
	        int nframe = (int) out.dimension(out.numDimensions()-1);
	        
	        Vector<Particle> vp = new Vector<Particle>();
	        Vector<pParticle> lines = new Vector<pParticle>();
	        
	        // Collect particles to draw and spline to draw
	        
	        for (int frame = 0 ; frame < nframe ; frame++)
	        {
	        	for (int t = 0 ; t < tr.size() ; t++)
	        	{
	        		if (tr.get(t).toDisplay() == false)
	        			continue;
	        		
	        		vp.clear();
	        		lines.clear();
	        	
	        		if ( frame + start_frame >= tr.get(t).start_frame && frame+start_frame <= tr.get(t).stop_frame )
	        		{
	        			// Check all particles frames, if particle is in frame add it
	        		
	        			for (int j = 0 ; j < tr.get(t).existing_particles.length ; j++)
	        			{
	        				if (tr.get(t).existing_particles[j].getFrame() == frame+start_frame)
	        				{
	        					// Particle to draw
	        				
	        					Particle p = new Particle(tr.get(t).existing_particles[j]);
	        					p.translate(focus);
	        					vp.add(p);
	        				
	        					// Collect spline
	        				
	        					if (typ == DrawType.NEXT)
	        					{
	        						if (j+1 < tr.get(t).existing_particles.length)
	        						{
	        							pParticle l1 = f.new pParticle(new Particle(tr.get(t).existing_particles[j]),new Particle(tr.get(t).existing_particles[j+1]));
	        							l1.translate(focus);
	        							lines.add(l1);
	        						}
	        					}
	        					else if (typ == DrawType.PREV)
	        					{
	        						if (j-1 >= 0)
	        						{
	        							pParticle l1 = f.new pParticle(new Particle(tr.get(t).existing_particles[j]),new Particle(tr.get(t).existing_particles[j+1]));
	        							l1.translate(focus);
	        							lines.add(l1);
	        						}	
	        					}
	        					else if (typ == DrawType.PREV_NEXT)
	        					{
	        						if (j+1 < tr.get(t).existing_particles.length)
	        						{
	        							pParticle l1 = f.new pParticle(new Particle(tr.get(t).existing_particles[j]),new Particle(tr.get(t).existing_particles[j+1]));
	        							l1.translate(focus);
	        							lines.add(l1);
	        						}
	        						if (j-1 >= 0)
	        						{
	        							pParticle l1 = f.new pParticle(new Particle(tr.get(t).existing_particles[j]),new Particle(tr.get(t).existing_particles[j+1]));
	        							l1.translate(focus);
	        							lines.add(l1);
	        						}
	        					}
	        					else if (typ == DrawType.TRAJECTORY_HISTORY)
	        					{
	        						for (int i = j ; i >= 1  ; i--)
	        						{
	        							pParticle l1 = f.new pParticle(new Particle(tr.get(t).existing_particles[i]),new Particle(tr.get(t).existing_particles[i-1]));
	        							l1.translate(focus);
	        							lines.add(l1);
	        						}
	        					}
	        					else if (typ == DrawType.TRAJECTORY_HISTORY_WITH_NEXT)
	        					{
	        						for (int i = j ; i >= 1  ; i--)
	        						{
	        							pParticle l1 = f.new pParticle(new Particle(tr.get(t).existing_particles[i]),new Particle(tr.get(t).existing_particles[i-1]));
	        							l1.translate(focus);
	        							lines.add(l1);
	        						}
	        						if (j+1 < tr.get(t).existing_particles.length)
	        						{
	        							pParticle l1 = f.new pParticle(new Particle(tr.get(t).existing_particles[j]),new Particle(tr.get(t).existing_particles[j+1]));
	        							l1.translate(focus);
	        							lines.add(l1);
	        						}
	        					}
	        				}
	        			}
	        		}
	        		
                    RandomAccessibleInterval< ARGBType > view = Views.hyperSlice( out, out.numDimensions()-1, frame );
	        		
			        drawParticles(view,vp,cal_, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));
			        drawLines(view,lines,cal_, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));
	        	}
	        } 
		}
		
		/**
		 * 
		 * Update the image
		 * 
		 * @param out An array of frames
		 * @param A vector of Trajectory
		 * @param cal Calibration
		 * @param Type of draw
		 *
		 */
		
		static public  void updateImage(Img<ARGBType> out , Vector<Trajectory> tr , Calibration cal, DrawType typ)
		{
			for (int i = 0 ; i < tr.size() ; i++)
				updateImage(out,tr,cal,new Color(tr.get(i).color.getRed(), tr.get(i).color.getGreen(), tr.get(i).color.getBlue(), tr.get(i).color.getTransparency()),typ);
		}
		
		/**
		 * 
		 * Update the image
		 * 
		 * @param An array of frames
		 * @param A vector of Trajectory
		 * @param Color to use for draw
		 * @param Type of draw
		 *
		 */
		
		static public  void updateImage(Img<ARGBType> out , Vector<Trajectory> tr , Calibration cal , Color cl , DrawType typ)
		{
			// Get image
	        
			MyFrame f = new MyFrame();
	        Cursor<ARGBType> curOut = out.cursor();
	        
	        //
	        
	        int nframe = (int) out.dimension(out.numDimensions()-1);
	        
	        Vector<Particle> vp = new Vector<Particle>();
	        Vector<pParticle> lines = new Vector<pParticle>();
	        
	        // Collect particles to draw and spline to draw
	        
	        for (int frame = 0 ; frame < nframe ; frame++)
	        {
	        	for (int t = 0 ; t < tr.size() ; t++)
	        	{
	        		if (tr.get(t).toDisplay() == false)
	        			continue;
	        		
	        		vp.clear();
	        		lines.clear();
	        	
	        		if ( frame >= tr.get(t).start_frame && frame <= tr.get(t).stop_frame )
	        		{
	        			// Check all particles frames, if particle is in frame add it
	        		
	        			for (int j = 0 ; j < tr.get(t).existing_particles.length ; j++)
	        			{
	        				if (tr.get(t).existing_particles[j].getFrame() == frame)
	        				{
	        					// Particle to draw
	        				
	        					vp.add(tr.get(t).existing_particles[j]);
	        				
	        					// Collect spline
	        				
	        					if (typ == DrawType.NEXT)
	        					{
	        						if (j+1 < tr.get(t).existing_particles.length)
	        						{
	        							lines.add(f.new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        						}
	        					}
	        					else if (typ == DrawType.PREV)
	        					{
	        						if (j-1 >= 0)
	        						{
	        							lines.add(f.new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j-1]));
	        						}	
	        					}
	        					else if (typ == DrawType.PREV_NEXT)
	        					{
	        						if (j+1 < tr.get(t).existing_particles.length)
	        						{
	        							lines.add(f.new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        						}
	        						if (j-1 >= 0)
	        						{
	        							lines.add(f.new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j-1]));
	        						}
	        					}
	        					else if (typ == DrawType.TRAJECTORY_HISTORY)
	        					{
	        						for (int i = j ; i >= 1  ; i--)
	        						{
	        							lines.add(f.new pParticle(tr.get(t).existing_particles[i],tr.get(t).existing_particles[i-1]));
	        						}
	        					}
	        					else if (typ == DrawType.TRAJECTORY_HISTORY_WITH_NEXT)
	        					{
	        						for (int i = j ; i >= 1  ; i--)
	        						{
	        							lines.add(f.new pParticle(tr.get(t).existing_particles[i],tr.get(t).existing_particles[i-1]));
	        						}
	        						if (j+1 < tr.get(t).existing_particles.length)
	        						{
	        							lines.add(f.new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        						}
	        					}
	        				}
	        			}
	        		}
	        		
                    RandomAccessibleInterval< ARGBType > view = Views.hyperSlice( out, out.numDimensions()-1, frame );
	        		
			        drawParticles(view,vp,cal, ARGBType.rgba(cl.getRed(), cl.getGreen(), cl.getBlue(), cl.getTransparency()));
			        drawLines(view,lines,cal, ARGBType.rgba(cl.getRed(), cl.getGreen(), cl.getBlue(), cl.getTransparency()));
	        	}
	        }
		}
		
		/**
		 * 
		 * Create an image from particle information with background and trajectory information
		 * 
		 * @param background background image
		 * @param cal calibration
		 * @return image video
		 */
		
		public  <T> Img<ARGBType> createImage(Img<T> background, Vector<Trajectory> tr ,Calibration cal, int frame, DrawType typ)
		{
			// if you have no trajectory draw use the other function
			
			if (tr == null)
			{return createImage(background,cal);}
			
	        // the number of dimensions
	        int numDimensions = background.numDimensions();
	        
	        long dims[] = new long[numDimensions];
	        background.dimensions(dims);
	        
	        // Create image
	        
	        final ImgFactory< ARGBType > imgFactory = new ArrayImgFactory< ARGBType >();
	        Img<ARGBType> out = imgFactory.create(dims, new ARGBType());
	        
	        Cursor<ARGBType> curOut = out.cursor();
	        Cursor<T> curBack = background.cursor();
	        
	        if (curBack.hasNext())
	        {
	        	curOut.fwd();
	        	curBack.fwd();
	        }
	        
	        // get the conversion
	        
	        ToARGB conv = MosaicUtils.getConversion(curBack.get());
	        
	        // Copy the background
	        
	        while (curBack.hasNext())
	        {
	        	curOut.fwd();
	        	curBack.fwd();
	        
	        	
	        	curOut.get().set(conv.toARGB(curBack.get()));
	        }
	        
	        Vector<Particle> vp = new Vector<Particle>();
	        Vector<pParticle> lines = new Vector<pParticle>();
	        
	        // Collect particles to draw and spline to draw
	        
	        for (int t = 0 ; t < tr.size() ; t++)
	        {
        		if (tr.get(t).toDisplay() == false)
        			continue;
	        	
	        	vp.clear();
	        	lines.clear();
	        	
	        	if ( frame >= tr.get(t).start_frame && frame <= tr.get(t).stop_frame )
	        	{
	        		// Check all particles frames, if particle is in frame add it
	        		
	        		for (int j = 0 ; j < tr.get(t).existing_particles.length ; j++)
	        		{
	        			if (tr.get(t).existing_particles[j].getFrame() == frame)
	        			{
	        				// Particle to draw
	        				
	        				vp.add(tr.get(t).existing_particles[j]);
	        				
	        				// Collect spline
	        				
	        				if (typ == DrawType.NEXT)
	        				{
	        					if (j+1 < tr.get(t).existing_particles.length)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        					}
	        				}
	        				else if (typ == DrawType.PREV)
	        				{
	        					if (j-1 >= 0)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j-1]));
	        					}	
	        				}
	        				else if (typ == DrawType.PREV_NEXT)
	        				{
	        					if (j+1 < tr.get(t).existing_particles.length)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        					}
	        					if (j-1 >= 0)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j-1]));
	        					}
	        				}
	        				else if (typ == DrawType.TRAJECTORY_HISTORY)
	        				{
	        					for (int i = j ; i >= 1  ; i--)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[i],tr.get(t).existing_particles[i-1]));
	        					}
	        				}
	        				else if (typ == DrawType.TRAJECTORY_HISTORY_WITH_NEXT)
	        				{
	        					for (int i = j ; i >= 1  ; i--)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[i],tr.get(t).existing_particles[i-1]));
	        					}
	        					if (j+1 < tr.get(t).existing_particles.length)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        					}
	        				}
	        			}
	        		}
	        	}
		        drawParticles(out,vp,cal, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));
		        drawLines(out,lines,cal, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));
	        }
	        
	        return out;
		}
		
		/**
		 * 
		 * Create an image from particle information and trajectory information
		 * 
		 * @param background background image
		 * @param cal calibration
		 * @return image video
		 */
		
		public  Img<ARGBType> createImage(int vMax[],  Vector<Trajectory> tr , int frame, DrawType typ)
		{
			// if you have no trajectory draw use the other function
			
			if (tr == null)
			{return createImage(vMax,frame);}
			
	        // the number of dimensions
	        
	        // Create image
	        
	        final ImgFactory< ARGBType > imgFactory = new ArrayImgFactory< ARGBType >();
	        Img<ARGBType> out = imgFactory.create(vMax, new ARGBType());
	        
	        Cursor<ARGBType> curOut = out.cursor();
	        
	        //
	        
	        Vector<Particle> vp = new Vector<Particle>();
	        Vector<pParticle> lines = new Vector<pParticle>();
	        
	        // Collect particles to draw and spline to draw
	        
	        for (int t = 0 ; t < tr.size() ; t++)
	        {
        		if (tr.get(t).toDisplay() == false)
        			continue;
        		
	        	vp.clear();
	        	lines.clear();
	        	
	        	if ( frame >= tr.get(t).start_frame && frame <= tr.get(t).stop_frame )
	        	{
	        		// Check all particles frames, if particle is in frame add it
	        		
	        		for (int j = 0 ; j < tr.get(t).existing_particles.length ; j++)
	        		{
	        			if (tr.get(t).existing_particles[j].getFrame() == frame)
	        			{
	        				// Particle to draw
	        				
	        				vp.add(tr.get(t).existing_particles[j]);
	        				
	        				// Collect spline
	        				
	        				if (typ == DrawType.NEXT)
	        				{
	        					if (j+1 < tr.get(t).existing_particles.length)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        					}
	        				}
	        				else if (typ == DrawType.PREV)
	        				{
	        					if (j-1 >= 0)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j-1]));
	        					}	
	        				}
	        				else if (typ == DrawType.PREV_NEXT)
	        				{
	        					if (j+1 < tr.get(t).existing_particles.length)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        					}
	        					if (j-1 >= 0)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j-1]));
	        					}
	        				}
	        				else if (typ == DrawType.TRAJECTORY_HISTORY)
	        				{
	        					for (int i = j ; i >= 1  ; i--)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[i],tr.get(t).existing_particles[i-1]));
	        					}
	        				}
	        				else if (typ == DrawType.TRAJECTORY_HISTORY_WITH_NEXT)
	        				{
	        					for (int i = j ; i >= 1  ; i--)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[i],tr.get(t).existing_particles[i-1]));
	        					}
	        					if (j+1 < tr.get(t).existing_particles.length)
	        					{
	        						lines.add(new pParticle(tr.get(t).existing_particles[j],tr.get(t).existing_particles[j+1]));
	        					}
	        				}
	        			}
	        		}
	        	}
		        drawParticles(out,vp,null, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));
		        drawLines(out,lines,null, ARGBType.rgba(tr.get(t).color.getRed(), tr.get(t).color.getGreen(), tr.get(t).color.getBlue(), tr.get(t).color.getTransparency()));
	        }
	        
	        return out;
		}
		
		/**
		 * 
		 * Remove double particles in the frame, needed for segmentation
		 * some tool produce double regions
		 * 
		 */
		
		public void removeDoubleParticles()
		{
			boolean f = false;
			Vector<Particle> p = new Vector<Particle>();
			
	        for (int i = 0 ; i < particles.size() ; i++)
	        {
	        	f = false;
	        	
	        	for (int j = i + 1 ; j < particles.size() ; j++)
	        	{
	        		if (particles.get(i).match(particles.get(j)))
	        			f = true;
	        	}
	        	
	        	if (f == false)
	        		p.add(particles.get(i));
	        }
	        
	        particles = p;
		}
		
	}
