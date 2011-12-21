package mosaic.region_competition;

import java.util.Iterator;

public class SphereBitmapImageSource
{
	int m_Size[];
	int m_Radius[];
	
	int m_BackgroundValue;
	int m_ForegroundValue;
	
	int dim;
	
	LabelImage labelImage;
	IndexIterator iterator;
	
	SphereBitmapImageSource(int NDimensions, LabelImage labelImage)
	{
		dim=NDimensions;
		this.labelImage=labelImage;
		
		m_Size = new int[dim];
		
		int[] vSize = new int[dim];
//		int[] vIndex = new int[dim];
		m_Radius = new int[dim];
		
		//Initial image is 64 wide in each direction.
		for (int i = 0; i < dim; i++) 
		{
			m_Size[i] = 64;
//		    m_Spacing[i] = 1.0;
//		    m_Origin[i] = 0.0;
//		    vSize[i] = 0;
//		    vIndex[i] = 0;
		    m_Radius[i] = 10;
		}
		
		iterator = new IndexIterator(m_Size);
//		m_Direction.SetIdentity();
		
		
		m_BackgroundValue = 0;
		m_ForegroundValue = 1;

    }
	
	double GenerateData(Point origin, int aFrom, int aTo)
	{
		
		Point half = (new Point(m_Size)).div(2);
		
		Point start = origin.sub(half);				// "upper left point"
		
		int vNFrom=0;
		int vNto=0;
		
		//TODO precalculate this!
		
		int size = iterator.getSize();
		for(int i=0; i<size; i++)	// over region
		{
			Point ofs = iterator.indexToPoint(i);
			Point p = start.add(ofs);
			
			// is point in region inside original data
			if(labelImage.iterator.isInBound(p))
			{
				int[] vIndex = p.x;

				float vHypEllipse = 0;
				for(int vD = 0; vD < dim; vD++)
				{
					vHypEllipse += (vIndex[vD] - (m_Size[vD]-1) / 2.0)*(vIndex[vD] - (m_Size[vD]-1) / 2.0)
									/(m_Radius[vD] * m_Radius[vD]);
				}

				if(vHypEllipse <= 1.0f)
				{
//					outIt.Set((m_ForegroundValue));
// std::cout << "1 ";
					
					int absLabel=labelImage.getAbs(p);
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
//					outIt.Set((m_BackgroundValue));
// std::cout << "0 ";
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
		float m_CurvatureMaskRadius= labelImage.settings.m_CurvatureMaskRadius;
		// end dummy
		
		if(dim == 2)
		{
			vVolume = 3.141592f * m_CurvatureMaskRadius * m_CurvatureMaskRadius;
		} 
		else if(dim == 3)
		{
			vVolume = 1.3333333f * 3.141592f * 
				m_CurvatureMaskRadius * m_CurvatureMaskRadius * m_CurvatureMaskRadius;
		} 
		else
		{
			vVolume = 0.0;
			assert(dim == 2 || dim == 3) : "Curvature flow only implemented for 2D and 3D";
			throw new RuntimeException("Curvature flow only implemented for 2D and 3D");
		}
		
		
		///////////////////////////////////////////////
		
		
		if(aFrom==labelImage.bgLabel)	// growing
		{
			int vN=vNto;
			if (dim== 2) {
				vCurvatureFlow -= 3.0f * 3.141592f / m_CurvatureMaskRadius *
				((vN) / vVolume - 0.5f);
			} else if (dim == 3) {
				vCurvatureFlow -= 16.0f / (3.0f * m_CurvatureMaskRadius)*
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
		            vCurvatureFlow += 3.0f * 3.141592f / m_CurvatureMaskRadius *
		                    ((vN) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow += 16.0f / (3.0f * m_CurvatureMaskRadius)*
		                    ((vN) / vVolume - 0.5f);
		        }
		    } else  // fighting fronts
		    {
		        if (dim == 2) {
		            vCurvatureFlow -= 3.0f * 3.141592f / m_CurvatureMaskRadius *
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 3.0f * 3.141592f / m_CurvatureMaskRadius *
		                    ((vNFrom) / vVolume - 0.5f);
		        } else if (dim == 3) {
		            vCurvatureFlow -= 16.0f / (3.0f * m_CurvatureMaskRadius)*
		                    ((vNto) / vVolume - 0.5f);
		            vCurvatureFlow += 16.0f / (3.0f * m_CurvatureMaskRadius)*
		                    ((vNFrom) / vVolume - 0.5f);
		        }
		        //                vCurvatureFlow *= 0.5f;
		
		    }
		}
		
		return vCurvatureFlow;
	}
	
}


