package mosaic.region_competition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RegionIterator implements Iterator<Integer>
{

	int dimensions;
	int[] region;
	int[] ofs;
	int[] input;
	
	int size;
	
	public RegionIterator(int[] input, int[] region, int[] ofs) 
	{
		dimensions=input.length;
		
		setInput(input);
		setRegion(region);
		setOfs(ofs);
		
		reset();
		
		crop();
	}
	
	
	void setInput(int[] dim)
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
	
	void setRegion(int[] region)
	{
		
		if(region.length==dimensions)
		{
			this.region=region.clone();
		}
		else
		{
			throw new RuntimeException("dimensions not matching in region iterator");
		}
		
	}
	
	void setOfs(int[] ofs)
	{
		if(ofs.length==dimensions)
		{
			this.ofs=ofs.clone();
		}
		else
		{
			throw new RuntimeException("dimensions not matching in region iterator");
		}
		
		//TODO if public, maybe crop here, save old ofs and reginsizes
	}


	void crop()
	{
		//TODO save old values for sizes
		
		// crop small values
		for(int i=0; i<dimensions; i++)
		{
			if(ofs[i]<0){
				region[i]+=ofs[i]; // shrink region for left border alignment
				ofs[i]=0;
			}
			//TODO correct? reuse of region, ofs
			if(ofs[i]+region[i]>input[i]){
				region[i]=input[i]-ofs[i];	//shrink region for right border alignment
			}
		}
		
		// crop large values
		size=1;
		for(int i=0; i<dimensions; i++)
		{
			size*=region[i];
		}
		
		calcStartIndex();
	}

	public void calcStartIndex()
	{
		// calc startindex
		itStart=0;
		int fac = 1;
		for(int i=0; i<dimensions; i++)
		{
			itStart+=ofs[i]*fac;
			fac *= input[i]; 
		}
	}

	////////////// Iterator ///////////////
	
	private int it;
	private int itStart=0;
	private int[] itCounter;
	
	@Override
	public boolean hasNext() 
	{
		return (it<size);
	}


	@Override
	public Integer next() 
	{
		//TODO add startofs
		//TODO maybe save last result and add to this
		int result=itStart;
//		int fac =1;
//		for(int i=0; i<dimensions; i++)
//		{
//			result+=(ofs[i]+itCounter[i])*fac;
//			fac *= input[i]; 
//		}
		
		it++;
		itStart++;
		itCounter[0]++;
		
		//TODO ersetze itCounter durch it%region[i]==0 oder so
		int prod = 1;
		for(int i=0; i<dimensions; i++)
		{
			if(itCounter[i]>=region[i]) // some dimension(s) wrap(s)
			{
				//TODO itStart wrap stimmt nur für erste dimension
				//TODO prod*(...) sind schritte, die man nicht macht. merke diese, und wisse, wo man absolut wäre?
				itStart = itStart+prod*(input[i]-region[i]);
				itCounter[i]=0;
				itCounter[(i+1)%dimensions]++; 		// % last point doesn't exceeds array bounds
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
		// TODO Auto-generated method stub
	}
	
	public void reset()
	{
		it=0;
		itCounter = new int[dimensions];
		itStart=0;
		
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

class RegionIteratorMask extends RegionIterator
{
	public RegionIteratorMask(int[] input, int[] region, int[] ofs) 
	{
		
		super(region, region, ofs);
		
		
		int[] maskSizes= region.clone();
		int[] maskOfs = new int[dimensions];
		for(int i=0; i<dimensions; i++)
		{
			if(ofs[i]<0)
			{
				maskOfs[i]= (-ofs[i]);	// start in mask not at 0,0
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