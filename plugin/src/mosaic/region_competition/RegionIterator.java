package mosaic.region_competition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Stephan
 * Iterator to iterate over a (arbitrary dimensional) rectangular region 
 * in the context of / relative to a input image (with the same dimension) 
 */
public class RegionIterator implements Iterator<Integer>, Iterable<Integer>
{

	int dimensions;
	int[] region;		// dimensions of region
	int[] ofs;			// offset
	int[] input;		// dimensions of input
	
	int size;			// size of (cropped) region
	
	/**
	 * 
	 * @param input 	dimensions of the input image
	 * @param region 	dimensions of the region
	 * @param ofs 		offset of the region in the input image (upper left)
	 */
	public RegionIterator(int[] input, int[] region, int[] ofs) 
	{
		dimensions=input.length;
		
		setInput(input);
		setRegion(region);
		setOfs(ofs);
		
		reset();
		
		crop();
	}
	
	
	private void setInput(int[] dim)
	{
		if(dim.length==dimensions)
		{
			this.input=dim.clone();
		}
		else
		{
			throw new RuntimeException("dimensions not matching in region iterator");
		}
	}
	
	/**
	 * Call crop() afterwards
	 */
	void setRegion(int[] region)
	{
		//TODO if public, maybe crop here, save old ofs and region sizes

		if(region.length==dimensions)
		{
			this.region=region.clone();
		}
		else
		{
			throw new RuntimeException("dimensions not matching in region iterator");
		}
	}
	
	/**
	 * Call crop() afterwards
	 */
	void setOfs(int[] ofs)
	{
		//TODO if public, maybe crop here, save old ofs and region sizes
		
		if(ofs.length==dimensions)
		{
			this.ofs=ofs.clone();
		}
		else
		{
			throw new RuntimeException("dimensions not matching in region iterator");
		}
	}


	/**
	 * Crops, i.e recalculates sizes of the offsets and sizes of the region 
	 * in such a way, that the region lies within the input image
	 * 
	 * TODO ofs[] and region[] are overwritten, so stay cropped forever 
	 * if they were cropped once. 
	 */
	void crop()
	{
		for(int i=0; i<dimensions; i++)
		{
			// crop small values
			if(ofs[i]<0){
				region[i]+=ofs[i]; // shrink region for left border alignment
				ofs[i]=0;
			}
			// crop large values
			//TODO correct? reuse of region, ofs
			if(ofs[i]+region[i]>input[i]){
				region[i]=input[i]-ofs[i];	//shrink region for right border alignment
			}
		}
		
		// recalculate size
		size=1;
		for(int i=0; i<dimensions; i++)
		{
			size*=region[i];
		}
		
		// recalculate first index
		calcStartIndex();
	}

	/**
	 * calculates the first valid index of the region
	 */
	private void calcStartIndex()
	{
		// calc startindex
		itInput=0;
		int fac = 1;
		for(int i=0; i<dimensions; i++)
		{
			itInput+=ofs[i]*fac;
			fac *= input[i]; 
		}
	}

	////////////// Iterator ///////////////
	
	private int it;				// iterator in cropped region
	private int itInput=0;		// iterator in input
	private int[] itDim;		// counts dimension wraps
	
	@Override
	public boolean hasNext() 
	{
		return (it<size);
	}


	@Override
	public Integer next() 
	{
		int result=itInput;
		
		// calculate indices for next step
		
		it++;
		itInput++;
		itDim[0]++;
		
		//TODO ersetze itCounter durch it%region[i]==0 oder so
		int prod = 1;
		for(int i=0; i<dimensions; i++)
		{
			if(itDim[i]>=region[i]) // some dimension(s) wrap(s)
			{
				//TODO prod*(...) sind schritte, die man nicht macht. merke diese, und wisse, wo man absolut wäre?
				// we reached the end of region in this dimension, so add the step not made in input to the input iterator 
				itInput = itInput+prod*(input[i]-region[i]);
				itDim[i]=0;
				itDim[(i+1)%dimensions]++; 		// '%' : last point doesn't exceeds array bounds
				prod*=input[i];
				//continue, wrap over other dimensions
			}
			else // no wrap
			{
				break;
			}
		}
		
		return result;
	}


	@Override
	public void remove() 
	{
		// not needed in this context
	}
	
	@Override
	public Iterator<Integer> iterator()
	{
		return this;
	}


	public void reset()
	{
		it=0;
		itDim = new int[dimensions];
		itInput=0;
		
		calcStartIndex();
	}
	
	////////////////////////////////////////////////////////
	
	public static void tester()
	{
		int[] testinput={100,100,100};
		int[] testofs={-50,-50,-50};
		int[] testregion = {200,200,200};
		
		IndexIterator it = new IndexIterator(testregion);
		int size = it.getSize();
		
		int mask[] = new int[size];
		for(int i=0; i<size; i++)
		{
			mask[i]=i;
		}
		
		RegionIterator regionIt = new RegionIterator(testinput, testregion, testofs);
		RegionIteratorMask maskIt = new RegionIteratorMask(testinput, testregion, testofs);
		
		while(regionIt.hasNext())
		{
			int idx = regionIt.next();
			int imask = maskIt.next();
			
			int x=mask[imask];
			
			
			System.out.println(""+it.indexToPoint(x)+" ");
		}
		
		System.out.println("fertig");
		
	}
	
	
	public static boolean test()
	{
		int[] testinput={100,100,100};
		int[] testofs={50,50,50};
		int[] testregion = {60,60,60};
		
		List<Integer> list1 = null; 
		List<Point> list2 = null;

		for(int i=0; i<50; i++)
		{
			list1= RegionItTest(testinput, testofs, testregion);
			list2 = naiveTest(testinput, testofs, testregion);
		}
		
		IndexIterator proofer = new IndexIterator(testinput);
		for(int i=0; i<list1.size(); i++)
		{
			Point p = proofer.indexToPoint(list1.get(i));
			if(!p.equals(list2.get(i)))
				return false;
		}
		
//		if(list1.equals(list2))
//			return true;
//		else
//			return false;
		
		return true;
	}
	
	public static List<Integer> RegionItTest(int[] testinput, int[] testofs, int[] testregion)
	{
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		RegionIterator testiterator = new RegionIterator(testinput, testregion, testofs);
//		IndexIterator proofer = new IndexIterator(testinput);
		
		Timer t = new Timer();
		t.tic();
		
		while(testiterator.hasNext())
		{
			int index = testiterator.next();
//			Point p = proofer.indexToPoint(index);
			list.add(index);
		}
		
		t.toc();
		System.out.println("region it = "+t.lastResult());
		
		return list;
	}
	
	public static List<Point> naiveTest(int[] testinput, int[] testofs, int[] testregion)
	{
		
		ArrayList<Point> list = new ArrayList<Point>();
		
		IndexIterator iterator = new IndexIterator(testregion);
		IndexIterator labelImageIt = new IndexIterator(testinput);
		
		Timer t = new Timer();
		t.tic();
		
		Point pstart = new Point(testofs);
		
		int size = iterator.getSize();
		for(int i=0; i<size; i++)	// over region
		{
			Point ofs = iterator.indexToPoint(i);
			Point p = pstart.add(ofs);
			if(labelImageIt.isInBound(p))
			{
				list.add(p);
//				System.out.println(p);
			}
		}
		
		t.toc();
		
		System.out.println("naive it  = "+t.lastResult());
		
		return list;
	}
	
}


/**
 * Iterates over a Region within an InputImage, 
 * but returns indices relative to the region
 */
class RegionIteratorMask extends RegionIterator
{
	/**
	 * Iterating over region is implemented in such a way, 
	 * that region is used as input (for RegionIterator) and
	 * the part of the region within input is used as region. 
	 * So the indices are returned relative to region. 
	 */
	public RegionIteratorMask(int[] input, int[] region, int[] ofs) 
	{
		
		super(region, region, ofs);
		
		int[] maskSizes= region.clone();
		int[] maskOfs = new int[dimensions];
		Arrays.fill(maskOfs, 0);
		
		for(int i=0; i<dimensions; i++)
		{
			if(ofs[i]<0)
			{
				maskOfs[i]= (-ofs[i]);	// start in mask and not at 0,0
				maskSizes[i]+=ofs[i];	// this may be done in crop
			}
			else
			{
				// start in mask at 0,0
			}
			//mask overflow
			if(ofs[i]+region[i]>=input[i])
			{
				int diff=ofs[i]+region[i]-input[i];
				maskSizes[i]-=diff;
			}
		}
		
		setRegion(maskSizes);
		setOfs(maskOfs);
		crop();
		
	}
	
}
