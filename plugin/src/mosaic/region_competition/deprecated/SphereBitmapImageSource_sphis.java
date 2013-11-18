package mosaic.region_competition.deprecated;

import mosaic.core.utils.IndexIterator;
import mosaic.region_competition.LabelImageRC;
import mosaic.core.utils.MaskIterator;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import mosaic.core.utils.RegionIteratorMask;

public class SphereBitmapImageSource_sphis
{
	public int m_Size[];
	int m_Radius[];
	float rad;
	
	int m_BackgroundValue;
	protected int m_ForegroundValue;
	
	int dim;
	
	protected LabelImageRC labelImage;
	IndexIterator iterator;
	
	protected int mask[]=null;
	
	
	/**
	 * @param labelImage
	 * @param radius		radius of sphere
	 * @param size			size of the region
	 */
	public SphereBitmapImageSource_sphis(LabelImageRC labelImage, int radius, int size)
	{
		this.labelImage=labelImage;
		this.dim = labelImage.getDim();
		rad = radius;
		
		m_BackgroundValue = 0;
		m_ForegroundValue = 1;
		
		m_Size = new int[dim];
		
//		int[] vSize = new int[dim];
//		int[] vIndex = new int[dim];
		m_Radius = new int[dim];
		
		//Initial image is 64 wide in each direction.
		for (int i = 0; i < dim; i++) 
		{
			m_Radius[i] = radius;
			m_Size[i] = size;
//		    m_Spacing[i] = 1.0;
//		    m_Origin[i] = 0.0;
//		    vSize[i] = 0;
//		    vIndex[i] = 0;
		}
		
		iterator = new IndexIterator(m_Size);
		
		mask = new int[iterator.getSize()];
		fillMask();			
		
//		m_Direction.SetIdentity();

    }
	
	private void fillMask()
	{
		int size = iterator.getSize();
		for(int i=0; i<size; i++)	// over region
		{
			Point ofs = iterator.indexToPoint(i);
			
			int[] vIndex = (ofs).x;

			float vHypEllipse = 0;
			for(int vD = 0; vD < dim; vD++)
			{
				vHypEllipse += 
					(vIndex[vD] - (m_Size[vD]-1) / 2.0)
					*(vIndex[vD] - (m_Size[vD]-1) / 2.0)
					/(m_Radius[vD] * m_Radius[vD]);
			}
			
			if(vHypEllipse <= 1.0f)
			{
				// is in region
				mask[i]=m_ForegroundValue;
			} 
			else
			{
				mask[i]=m_BackgroundValue;
			}
		} //for
	}
	
	public boolean isInMask(int maskIdx)
	{
		int maskvalue = mask[maskIdx];
		return maskvalue == m_ForegroundValue;
	}
	
	
	/**
	 * Faster version with new Iterator, Index-based
	 * 
	 * @param origin	Midpoint in input image
	 * @param aFrom		label from
	 * @param aTo		label to
	 * @return			vCurvatureFlow
	 */
	public double GenerateData2(Point origin, int aFrom, int aTo)
	{
		Point half = (new Point(m_Size)).div(2);
		Point start = origin.sub(half);				// "upper left point"
		
		int vNFrom=0;
		int vNto=0;
		
		//TODO change region iterator so one can reuse the iterator and just set the new ofs
		RegionIterator it = new RegionIterator(labelImage.getDimensions(), this.m_Size, start.x);
		MaskIterator maskIt = new MaskIterator(labelImage.getDimensions(), this.m_Size, start.x);
		
		while(it.hasNext())	// iterate over sphere region
		{
			int idx = it.next();
			
			int maskIdx = maskIt.next();
			int maskvalue = mask[maskIdx];
			
			if(maskvalue==m_ForegroundValue)
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
			else
			{
//					mask[i]==m_BackgroundValue;
			}
		}
		
		
		
		///////////////////////////////////////////////////////////////////////
		
		double vCurvatureFlow = 0.0;
		double vVolume;
		
		//TODO dummy
//		final float rad = labelImage.settings.m_CurvatureMaskRadius;
//		float m_CurvatureMaskRadius = this.rad;
		float rad = this.rad;
		// end dummy
		
		if(dim == 2)
		{
			vVolume = 3.141592f * rad * rad;
		} 
		else if(dim == 3)
		{
			vVolume = 1.3333333f * 3.141592f * 
				rad * rad * rad;
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
				vCurvatureFlow -= 3.0f * 3.141592f / rad *
				((vN) / vVolume - 0.5f);
			} else if (dim == 3) {
				vCurvatureFlow -= 16.0f / (3.0f * rad)*
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
		            vCurvatureFlow += 3.0f * 3.141592f / rad *
		                    ((vN) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow += 16.0f / (3.0f * rad)*
		                    ((vN) / vVolume - 0.5f);
		        }
		    } else  // fighting fronts
		    {
		        if (dim == 2) {
		            vCurvatureFlow -= 3.0f * 3.141592f / rad *
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 3.0f * 3.141592f / rad *
		                    ((vNFrom) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow -= 16.0f / (3.0f * rad)*
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 16.0f / (3.0f * rad)*
		                    ((vNFrom) / vVolume - 0.5f);
		        }
		        //                vCurvatureFlow *= 0.5f;
		
		    }
		}
		
		return vCurvatureFlow;
		}
	
	
	/**
	 * Slower old version, Point-based
	 * 
	 * @param origin	Midpoint in input image
	 * @param aFrom		label from
	 * @param aTo		label to
	 * @return			vCurvatureFlow
	 */
	public double GenerateData(Point origin, int aFrom, int aTo)
	{

		Point half = (new Point(m_Size)).div(2);
		Point start = origin.sub(half);				// "upper left point"
		
		int vNFrom=0;
		int vNto=0;
		
		int size = iterator.getSize();
		for(int i=0; i<size; i++)	// over region
		{
			Point ofs = iterator.indexToPoint(i);
			Point p = start.add(ofs);
			
			// is point in region inside original data
			if(labelImage.iterator.isInBound(p))
			{

//				System.out.println(""+p+" "+vHypEllipse);
				if(mask[i]==m_ForegroundValue)
				{
					int absLabel=labelImage.getLabelAbs(p);
					
					if(absLabel==aTo)
					{
						vNto++;
					}
					if(absLabel==aFrom)
					{
						vNFrom++;
					}
				} // is in region
				else
				{
//					mask[i]==m_BackgroundValue;
				}
			}
			else
			{
				// not in bound
			}
		}//for
		
		
		
		///////////////////////////////////////////////////////////////////////
		
		double vCurvatureFlow = 0.0;
		double vVolume;
		
		//TODO dummy
//		float m_CurvatureMaskRadius= labelImage.settings.m_CurvatureMaskRadius;
//		float m_CurvatureMaskRadius = this.rad;
		float rad = this.rad;
		// end dummy
		
		if(dim == 2)
		{
			vVolume = 3.141592f * rad * rad;
		} 
		else if(dim == 3)
		{
			vVolume = 1.3333333f * 3.141592f * 
				rad * rad * rad;
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
				vCurvatureFlow -= 3.0f * 3.141592f / rad *
				((vN) / vVolume - 0.5f);
			} else if (dim == 3) {
				vCurvatureFlow -= 16.0f / (3.0f * rad)*
				((vN) / vVolume - 0.5f);
			}
		}
		else
		{
		    if (aTo == 0) //proper shrinking
	    	{
		        int vN = vNFrom;
		        // This is a point on the contour (innerlist) OR
		        // touching the contour (Outer list)
		        if (dim == 2) {
		            vCurvatureFlow += 3.0f * 3.141592f / rad *
		                    ((vN) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow += 16.0f / (3.0f * rad)*
		                    ((vN) / vVolume - 0.5f);
		        }
		    } else  // fighting fronts
		    {
		        if (dim == 2) {
		            vCurvatureFlow -= 3.0f * 3.141592f / rad *
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 3.0f * 3.141592f / rad *
		                    ((vNFrom) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow -= 16.0f / (3.0f * rad)*
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 16.0f / (3.0f * rad)*
		                    ((vNFrom) / vVolume - 0.5f);
		        }
		        //                vCurvatureFlow *= 0.5f;
		
		    }
		}
		
		return vCurvatureFlow;
	}
	
	
	public int[] getRegionSize()
	{
		return m_Size;
	}
	
}






