package mosaic.core.detection;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.imglib2.RandomAccess;
import java.util.Vector;

import mosaic.core.utils.CircleMask;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIteratorMask;
import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.hypersphere.HyperSphereCursor;
import net.imglib2.img.Img;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.type.numeric.RealType;


	/**
	 * Defines a MyFrame that is based upon an ImageProcessor or information from a text file.
	 * @param <RandomsAccessible>
	 */
	public class MyFrame<RandomsAccessible> {

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
		/**
		 * Constructor for ImageProcessor based MyFrame.
		 * <br>All particles and other information will be derived from the given <code>ImageProcessor</code>
		 * by applying Detector methods  
		 * @param ip the original ImageProcessor upon this MyFrame is based, will remain unchanged!
		 * @param frame_num the serial number of this frame in the movie
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
		
		private boolean loadParticlesFromFile (String path) {
			boolean ret;
			BufferedReader r;
			
			try {
				r = new BufferedReader(new FileReader(path));
			
				ret = loadParticlesFromFile(r,path);
			
				/* close file */
				r.close();
			}
			catch (Exception e) {
				IJ.error(e.getMessage());
				return false;
			}
            
            return ret;
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
		
		private boolean loadParticlesFromFile(BufferedReader r,String path) throws IOException {
	        
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
	        
	        /* go over all particles String info and construct Particles Ojectes from it*/
	        while (iter.hasNext()) {
	        	particle_info = iter.next();
	        	
	        	if (particle_info.length < 2)
	        	{
		            IJ.error("Malformed line, expacting 2 float, feeding: " + Integer.toString(particle_info.length) );
		            return false;
	        	}
	        	
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
		public StringBuffer toStringBuffer() {

			// work with StringBuffer since its faster than String

//			NumberFormat nf = NumberFormat.getInstance();
//			nf.setMaximumFractionDigits(6);
//			nf.setMinimumFractionDigits(6); 
			 
			DecimalFormat nf = new DecimalFormat("#####0.000000");
			nf.setGroupingUsed(false);
			StringBuffer sb = new StringBuffer("% Frame ");
			sb.append(this.frame_number);
			sb.append("\n");
			for(int j = 0; j < this.particles.size(); j++) {
				sb.append("%\tParticle ");
				sb.append(j);
				sb.append(" (");
				sb.append(nf.format(this.particles.elementAt(j).x));
				sb.append(", ");
				sb.append(nf.format(this.particles.elementAt(j).y));
				sb.append(", ");		
				sb.append(nf.format(this.particles.elementAt(j).z));
				sb.append(")\n");	
				for(int k = 0; k < linkrange; k++) {
					sb.append("%\t\tlinked to particle ");
					sb.append(this.particles.elementAt(j).next[k]);
					sb.append(" in frame ");
					sb.append((this.frame_number + k + 1));
					sb.append("\n");					
				}
			}
			return sb;
		}

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
		public StringBuffer frameDetectedParticlesForSave(boolean with_momentum) {

//			NumberFormat nf = NumberFormat.getInstance();
//			nf.setMaximumFractionDigits(6);
//			nf.setMinimumFractionDigits(6); 
			 
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
		
		public <T extends RealType< T >> Img<T> createImage(Img<T> background, Calibration cal, int frame)
		{
	        // the number of dimensions
	        int numDimensions = background.numDimensions();
	        int sz[] = new int [numDimensions];
	 
	        for ( int d = 0; d < numDimensions; ++d )
	        {
	            sz[d] = (int) background.dimension( d );
	        }
	        
	        long dims[];
	        background.dimensions(dims);
	        
	        // Add 1 more dimensions and copy dimensions
	        
	        long dims_c[] = new long[dims.length + 1];
	        
	        for (int i = 0 ; i < dims.length ; i++)
	        {
	        	dims_c[i] = dims[i];
	        }
	        dims_c[dims.length] = 3;
	        
	        // Create image
	        
	        Img<T> out = background.factory().create(dims_c, background.firstElement());
	        RandomAccess<T> out_a = out.randomAccess();
	        
	        // define the center and radius
	        Point center = new Point( background.numDimensions() );

	 
	        // Create a list of particles
	        
	        List<Particle> pt = new ArrayList<Particle>();
	 
	        for (int i = 0 ; i < particles.size() ; i++)
	        {
	        	pt.add(particles.get(i));
	        }
	        
	        // Iterate on all particles
	        
	        while (pt.size() != 0)
	        {
	        	int radius = (int) Math.cbrt(pt.get(0).m0*4.0/3.0/Math.PI);
	        	float sp[] = new float[numDimensions];
	        	
	    		float scaling[] = new float[3];

	    		scaling[0] = (float) (1.0f/cal.pixelWidth);
	    		scaling[1] = (float) (1.0f/cal.pixelHeight);
	    		scaling[2] = (float) (1.0f/cal.pixelDepth);
	    		
	    		// Create a circle Mask and an iterator
	    		
	        	CircleMask cm = new CircleMask(radius, 2*radius + 1, numDimensions, scaling);
	        	RegionIteratorMask rg_m = new RegionIteratorMask(cm, sz);
	        	
	        	for (Particle ptt : pt)
	        	{
	        		int radius_r = (int) Math.cbrt(ptt.m0*4.0/3.0/Math.PI);
	        		
	        		// if particle has the same radius
	        	
	        		if (radius_r == radius)
	        		{
	        			// Draw the Circle
	        			
		        		while ( rg_m.hasNext() )
		        		{
		        			Point p = rg_m.nextP();
		        			out_a.setPosition(p.x);
		        			out_a.get().set(arg0);
		        		}	
	        			
	        		}
	        	}
	        }
		}
		
	}
