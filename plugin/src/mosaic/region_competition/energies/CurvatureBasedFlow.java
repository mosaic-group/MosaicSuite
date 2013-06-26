package mosaic.region_competition.energies;

import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;
import mosaic.region_competition.RegionIterator;
import mosaic.region_competition.RegionIteratorMask;
import mosaic.region_competition.RegionIteratorSphere;
import mosaic.region_competition.SphereMask;

public class CurvatureBasedFlow
{
	RegionIteratorSphere sphereIt;
	LabelImage labelImage;
	
	int dim;
	int[] inputDims; 
	int[] m_Size;
	int rad;
	
	SphereMask sphere; 
	byte[] mask;
	

	public CurvatureBasedFlow(int rad, LabelImage labelImage)
	{
		this.rad = rad;
		this.dim = labelImage.getDim();
		this.inputDims = labelImage.getDimensions();
		this.labelImage = labelImage;
		
		sphere = new SphereMask(rad, 2*rad+1, dim);
		sphereIt = new RegionIteratorSphere(sphere, inputDims);
		
		m_Size = sphere.getDimensions();
		mask = sphere.mask;
	}
	
	
	/**
	 * uglier but faster version without sphere iterator 
	 * but field accesses
	 */
	public double generateData(Point origin, int aFrom, int aTo)
	{
		int vNto = 0;
		int vNFrom = 0;
		
/*		Point half = (new Point(m_Size)).div(2);
		Point start = origin.sub(half);				// "upper left point"*/

		sphereIt.setMidPoint(origin);
		
		while (sphereIt.hasNext())
		{
			int idx = sphereIt.next();
			int absLabel=labelImage.getLabelAbs(idx);
			
			//directly access data; only 1-2% faster
//			int absLabel=labelImage.labelIP.get(idx);
//			if(absLabel >= LabelImage.negOfs)
//				absLabel-=LabelImage.negOfs;
			
			if(absLabel==aTo)
			{
				vNto++;
			}
			else if(absLabel==aFrom)
			{
				vNFrom++;
			}
		}
		
/*		final byte fgVal = sphere.fgVal;
		
		RegionIterator it = new RegionIterator(labelImage.getDimensions(), this.m_Size, start.x);
		RegionIteratorMask maskIt = new RegionIteratorMask(inputDims, this.m_Size, start.x);
		
		while(it.hasNext())	// iterate over sphere region
		{
			int idx = it.next();
			
			int maskIdx = maskIt.next();
			int maskvalue = mask[maskIdx];
			
			if(maskvalue==fgVal)
			{
				int absLabel=labelImage.getLabelAbs(idx);
				
				//directly access data; only 1-2% faster
//				int absLabel=labelImage.labelIP.get(idx);
//				if(absLabel >= LabelImage.negOfs)
//					absLabel-=LabelImage.negOfs;
				
				if(absLabel==aTo)
				{
					vNto++;
				}
				else if(absLabel==aFrom)
				{
					vNFrom++;
				}
			} // is in region
		}*/

		double vCurvatureFlow = 0.0;
		double vVolume;
		
		final float r = this.rad;
		
		if(dim == 2)
		{
			vVolume = 3.141592f * r*r;
		} 
		else if(dim == 3)
		{
			vVolume = 1.3333333f * 3.141592f * r*r*r;
		} 
		else
		{
			vVolume = 0.0;
			throw new RuntimeException("Curvature flow only implemented for 2D and 3D");
		}
		
		///////////////////////////////////////////////
		
		
		if(aFrom==labelImage.bgLabel)	// growing
		{
			int vN=vNto;
			if (dim== 2) {
				vCurvatureFlow -= 3.0f * 3.141592f / r *
				((vN) / vVolume - 0.5f);
			} else if (dim == 3) {
				vCurvatureFlow -= 16.0f / (3.0f * r)*
				((vN) / vVolume - 0.5f);
			}
		}
		else
		{
		    if (aTo == labelImage.bgLabel) //proper shrinking
	    	{
		        int vN = vNFrom;
		        // This is a point on the contour (innerlist) OR
		        // touching the contour (Outer list)
		        if (dim == 2) {
		            vCurvatureFlow += 3.0f * 3.141592f / r *
		                    ((vN) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow += 16.0f / (3.0f * r)*
		                    ((vN) / vVolume - 0.5f);
		        }
		    } else  // fighting fronts
		    {
		        if (dim == 2) {
		            vCurvatureFlow -= 3.0f * 3.141592f / r *
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 3.0f * 3.141592f / r *
		                    ((vNFrom) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow -= 16.0f / (3.0f * r)*
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 16.0f / (3.0f * r)*
		                    ((vNFrom) / vVolume - 0.5f);
		        }
		        //                vCurvatureFlow *= 0.5f;
		
		    }
		}
		
		return vCurvatureFlow;
	}
	
	class CurvatureBasedFlowNice extends CurvatureBasedFlow
	{

		public CurvatureBasedFlowNice(int rad, LabelImage labelImage)
		{
			super(rad, labelImage);
		}

		
		/**
		 * nicer but slower version with sphere iterator
		 */
		@Override
		public double generateData(Point origin, int fromLabel, int toLabel)
		{
			int vNto = 0;
			int vNFrom = 0;
			
			sphereIt.setMidPoint(origin);
			while(sphereIt.hasNext())
			{
				int idx = sphereIt.next();
				
				int absLabel=labelImage.getLabelAbs(idx);
				if(absLabel==toLabel)
				{
					vNto++;
				}
				else if(absLabel==fromLabel)
				{
					vNFrom++;
				}
			}
		
			double vCurvatureFlow = 0.0;
			double vVolume;
			
			final float r = this.rad;
			
			if(dim == 2)
			{
				vVolume = 3.141592f * r*r;
			} 
			else if(dim == 3)
			{
				vVolume = 1.3333333f * 3.141592f * r*r*r;
			} 
			else
			{
				vVolume = 0.0;
				throw new RuntimeException("Curvature flow only implemented for 2D and 3D");
			}
			
			
			///////////////////////////////////////////////
			
			
			if(fromLabel==labelImage.bgLabel)	// growing
			{
				int vN=vNto;
				if (dim== 2) {
					vCurvatureFlow -= 3.0f * 3.141592f / r *
					((vN) / vVolume - 0.5f);
				} else if (dim == 3) {
					vCurvatureFlow -= 16.0f / (3.0f * r)*
					((vN) / vVolume - 0.5f);
				}
			}
			else
			{
			    if (toLabel == labelImage.bgLabel) //proper shrinking
		    	{
			        int vN = vNFrom;
			        // This is a point on the contour (innerlist) OR
			        // touching the contour (Outer list)
			        if (dim == 2) {
			            vCurvatureFlow += 3.0f * 3.141592f / r *
			                    ((vN) / vVolume - 0.5f);
			        } else if (dim == 3) {
			            vCurvatureFlow += 16.0f / (3.0f * r)*
			                    ((vN) / vVolume - 0.5f);
			        }
			    } else  // fighting fronts
			    {
			        if (dim == 2) {
			            vCurvatureFlow -= 3.0f * 3.141592f / r *
			                    ((vNto) / vVolume - 0.5f);
			            vCurvatureFlow += 3.0f * 3.141592f / r *
			                    ((vNFrom) / vVolume - 0.5f);
			        } else if (dim == 3) {
			            vCurvatureFlow -= 16.0f / (3.0f * r)*
			                    ((vNto) / vVolume - 0.5f);
			            vCurvatureFlow += 16.0f / (3.0f * r)*
			                    ((vNFrom) / vVolume - 0.5f);
			        }
			        //                vCurvatureFlow *= 0.5f;
			
			    }
			}
			
			return vCurvatureFlow;
		}
		
	}
	
	
}