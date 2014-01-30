package mosaic.core.ipc;

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
}