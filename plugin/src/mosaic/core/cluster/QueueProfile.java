package mosaic.core.cluster;

import mosaic.core.ipc.ICSVGeneral;
import mosaic.core.ipc.Outdata;
import mosaic.core.ipc.StubProp;

public class QueueProfile extends StubProp implements ICSVGeneral, Outdata<QueueProfile>
{
	String queue;
	String hardware;
	double limit;
	
	public void setqueue(String queue)
	{
		this.queue = queue;
	}
	
	public void sethardware(String hardware)
	{
		this.hardware = hardware;
	}
	
	public void setlimit(double limit)
	{
		this.limit = limit;
	}
	
	public String getqueue()
	{
		return queue;
	}
	
	public String gethardware()
	{
		return hardware;
	}
	
	public double getlimit()
	{
		return limit;
	}

	@Override
	public void setData(QueueProfile r) 
	{
		queue = r.queue;
		hardware = r.hardware;
		limit = r.limit;
	}
};
