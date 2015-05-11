package mosaic.region_competition.energies;

import ij.measure.Calibration;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIteratorMask;
import mosaic.core.utils.SphereMask;
import mosaic.region_competition.LabelImageRC;

public class CurvatureBasedFlow
{
	RegionIteratorMask sphereIt;
	LabelImageRC labelImage;
	
	int dim;
	int[] inputDims; 
	int[] m_Size;
	int rad;
	
	SphereMask sphere; 
	byte[] mask;
	

	public CurvatureBasedFlow(int rad, LabelImageRC labelImage, Calibration cal)
	{
		this.rad = rad;
		this.dim = labelImage.getDim();
		this.inputDims = labelImage.getDimensions();
		this.labelImage = labelImage;
		
		float spacing[] = null;
		if (cal != null)
		{
			if (labelImage.getDim() == 2 && cal != null)
			{
				spacing = new float[2];
				spacing[0] = (float) cal.pixelWidth;
				spacing[1] = (float) cal.pixelHeight;
				sphere = new SphereMask(rad, 2*rad+1, dim, spacing, false);
			}
			else
			{
				spacing = new float[3];
				spacing[0] = (float) cal.pixelWidth;
				spacing[1] = (float) cal.pixelHeight;
				spacing[2] = (float) cal.pixelDepth;
				sphere = new SphereMask(rad, 2*rad+1, dim, spacing, false);
			}
		}
		else
		{
			sphere = new SphereMask(rad, 2*rad+1, dim);
		}
		
		sphereIt = new RegionIteratorMask(sphere, inputDims);
		
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
}