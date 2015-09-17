package mosaic.core.cluster;

import ij.IJ;

import java.io.File;
import java.util.Vector;

import mosaic.core.utils.DataCompression;
import mosaic.io.csv.CSV;
import mosaic.io.csv.CsvColumnConfig;

import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;


/**
 * 
 * Read a file and convert into a Cluster Profile, can be directly used with
 * ClusterSession
 * 
 * @author Pietro Incardona
 *
 */

public class FileClusterProfile extends GeneralProfile
{
	CSV<QueueProfile> csv;
	CsvColumnConfig occ;
	
	public FileClusterProfile(File filename)
	{
		csv = new CSV<QueueProfile>(QueueProfile.class);
		occ = new CsvColumnConfig(new String[]{"queue","hardware","limit"}, new CellProcessor[]{null,null,new ParseDouble()});
		occ.fieldMapping = new String[]{"queue","hardware","limit"};
		occ.cellProcessors = new CellProcessor[]{null,null,new ParseDouble()};
		
		if (filename != null)
		{
			Vector<QueueProfile> cp = csv.Read(filename.getAbsolutePath(), occ);
		
			for (int i = 0 ; i < cp.size() ; i++)
			{
				if (cp.get(i).hardware.equals("CPU"))
				{
					setAcc(hw.CPU);
				}
				else if (cp.get(i).hardware.equals("GPU"))
				{
					setAcc(hw.GPU);
				}
				setQueue(cp.get(i).limit,cp.get(i).queue);
			}
		
			String batch = csv.getMetaInformation("batch");
		
			if (batch != null && batch.equals("LSF"))
			{
				setBatchSystem(new LSFBatch(this));
			}
			else
			{
				IJ.error("Error", batch + " batch system is not supported");
			}
		}
		setAcc(hw.CPU);
	}
	
	@Override
	public String getProfileName() 
	{
		return csv.getMetaInformation("profile");
	}

	@Override
	public void setProfileName(String ProfileName_) 
	{
		csv.setMetaInformation("profile",ProfileName_);
	}

	public String getAccessAddress()
	{
		return csv.getMetaInformation("address");
	}
	
	public void setAccessAddress(String addr)
	{
		csv.setMetaInformation("address", addr);
	}
	
	/**
	 * 
	 * Return the running dir without any replacement
	 * 
	 * @return the raw string
	 * 
	 */
	
	public String getRunningDirRaw()
	{
		return csv.getMetaInformation("run_dir");
	}
	
	@Override
	public String getRunningDir() 
	{
		String meta = csv.getMetaInformation("run_dir");
		if (meta.replace("*",getUsername()) == null)
		{
			return csv.getMetaInformation("run_dir") + File.separator;
		}
		else
		{
			return meta.replace("*", getUsername()) + File.separator;
		}
		
	}

	@Override
	public void setRunningDir(String RunningDir_) 
	{
		csv.setMetaInformation("run_dir",RunningDir_);
	}

	@Override
	public String getImageJCommand() 
	{
		return "fiji";
	}

	@Override
	public void setImageJCommand(String ImageJCommand_)
	{
	}
	
	@Override
	public boolean hasCompressor(DataCompression.Algorithm a)
	{
		if (a == null)	return true;
		
		String cp_name = a.name;
		String has_cp = csv.getMetaInformation(cp_name);
		
		if (has_cp == null)
			return false;
		
		if (has_cp.equals("true"))
			return true;
		else
			return false;
	}
	
	/**
	 * 
	 * Write the configuration file
	 * 
	 * @param Config filename
	 * 
	 */
	
	public void writeConfigFile(File CsvFilename)
	{
		Vector<QueueProfile> vq = new Vector<QueueProfile>();
		for (hw Acc : hw.values())
		{
			QueueProfile[] q = getQueues(Acc);
			for (int i = 0 ; i < q.length ; i++)
			{
				vq.add(q[i]);
			}
		}
		csv.Write(CsvFilename.getAbsolutePath(), vq, occ, false);
	}
	
	/**
	 * 
	 * Set the batch system by string
	 * (used to write csv config file)
	 * 
	 * @param Batch system string
	 * 
	 */
	
	public void setBatchSystemString(String bc)
	{
		csv.setMetaInformation("batch", bc);
	}
	
	/**
	 * 
	 * Set one compressor by string
	 * 
	 * @param compressor string string
	 * 
	 */
	
	public void setCompressorString(String bc)
	{
		csv.setMetaInformation(bc, "true");
	}
	
	/**
	 * 
	 * Remove a compressor from the list of compression algorithm by String
	 * 
	 * @param cp
	 */
	public void removeCompressorString(String cp)
	{
		csv.removeMetaInformation(cp);
	}
	
	/**
	 * 
	 *  Check if a compressor algorithm is active
	 * 
	 * @param cp Compressor to check
	 * @return return true if active 
	 * 
	 */
	public boolean isActiveCompressorString(String cp)
	{
		String s = csv.getMetaInformation(cp);
		
		if (s == null)
			return false;
		
		if (s.equals("true"))
			return true;
		
		return false;
	}
}
