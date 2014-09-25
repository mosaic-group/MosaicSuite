package mosaic.core.ipc;

import java.util.Vector;

import org.supercsv.cellprocessor.ParseBool;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;


class ProcessorGeneral
{
	public CellProcessor getProcessorImage_ID() {return new ParseInt();}
	public CellProcessor getProcessorObject_ID()	{return new ParseInt();}
	public CellProcessor getProcessorSize()	{return new ParseDouble();}
	public CellProcessor getProcessorPerimeter()	{return new ParseDouble();}
	public CellProcessor getProcessorLength()	{return new ParseDouble();}
	public CellProcessor getProcessorIntensity()	{return new ParseDouble();}
	public CellProcessor getProcessorx()		{return new ParseDouble();}
	public CellProcessor getProcessory()		{return new ParseDouble();}
	public CellProcessor getProcessorz()		{return new ParseDouble();}
	public CellProcessor getProcessorCoord_X()		{return new ParseDouble();}
	public CellProcessor getProcessorCoord_Y()		{return new ParseDouble();}
	public CellProcessor getProcessorCoord_Z()		{return new ParseDouble();}
	public CellProcessor getProcessorFrame() {return new ParseInt();}
	public CellProcessor getProcessorSurface()		{return new ParseDouble();}
	public CellProcessor getProcessorOverlap_with_ch()		{return new ParseDouble();}
	public CellProcessor getProcessorColoc_object_size()		{return new ParseDouble();}
	public CellProcessor getProcessorColoc_object_intensity()	{return new ParseDouble();}
	public CellProcessor getProcessorSingle_Coloc()				{return new ParseBool();}
	public CellProcessor getProcessorColoc_image_intensity()		{return new ParseDouble();}
	
	class Map
	{
		String from;
		String To;
		
		Map(String m1, String m2)
		{
			from = m1;
			To = m2;
		}
	}
	
	Vector<Map> stp;
	
	ProcessorGeneral()
	{
		stp = new Vector<Map>();

		stp.add(new Map("x","Coord_X"));
		stp.add(new Map("y","Coord_Y"));
		stp.add(new Map("z","Coord_Z"));
		stp.add(new Map("Frame","Image_ID"));
		stp.add(new Map("mean","Intensity"));
		stp.add(new Map("label","Object_ID"));
		stp.add(new Map("size","Size"));
	}
	
	/**
	 * 
	 * Injective Map
	 * 
	 * @param from String
	 * @return To String
	 */
	
	public String getMap(String from)
	{
		for (int i = 0 ; i < stp.size() ; i++)
		{
			if (stp.get(i).from.equals(from))
			{
				return stp.get(i).To;
			}
		}
		return from;
	}
}