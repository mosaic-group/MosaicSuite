package mosaic.plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import mosaic.core.cluster.ClusterGUI;
import mosaic.core.cluster.ClusterProfile;
import mosaic.core.cluster.ClusterSession;
import mosaic.core.cluster.FileClusterProfile;
import mosaic.core.cluster.hw;
import mosaic.core.utils.DataCompression;
import mosaic.core.utils.Segmentation;
import mosaic.core.utils.DataCompression.Algorithm;
import mosaic.core.cluster.QueueProfile;


/**
 * @author Pietro Incardona
 * 
 * Small utility to create cluster profile
 * 
 */

public class NewClusterProfile implements PlugInFilter
{
	FileClusterProfile fcp;
	
	@Override
	public void run(ImageProcessor arg0) 
	{}
	
	Choice cpa;
	
	/**
	 * 
	 * Popup a window to create a new/edit cluster profile configuration file
	 * 
	 */
	
	void popupClusterProfile(ClusterProfile cp)
	{
		final Vector<QueueProfile> cq = new Vector<QueueProfile>();
		final Vector<Choice> cc;
		
		GenericDialog gd = new GenericDialog("New cluster profile");
		
		gd.setTitle("New cluster profile");
		if (cp != null)
		{
			gd.addStringField("profile", cp.getProfileName());
			gd.addStringField("address", cp.getAccessAddress());
			gd.addStringField("run_dir", cp.getRunningDir());
			
			int totalq = 0;
			for (hw hd : hw.values())
			{
				QueueProfile[] qp = cp.getQueues(hd);
				totalq += qp.length;
			}
			
			String[] qs = new String[totalq];
			int cnt = 0;
			
			for (hw hd : hw.values())
			{
				QueueProfile[] qp = cp.getQueues(hd);
				for (int i = 0 ; i < qp.length ; i++)
				{
					qs[cnt] = qp[i].getqueue() + " " + qp[i].gethardware() + " " + qp[i].getlimit();
					cnt++;
				}
			}
			gd.addChoice("Queues", qs,qs[0]);
			gd.addChoice("compressor", new String[]{""}, new String(""));
		}
		else
		{
			gd.addStringField("profile", "");
			gd.addStringField("address", "");
			gd.addStringField("run_dir", "");
			gd.addChoice("queues", new String[]{""}, new String(""));
			gd.addChoice("compressor", new String[]{""}, new String(""));
		}
		cc = gd.getChoices();
		
		Button optionButton = new Button("Add");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=2; c.gridy=3; c.anchor = GridBagConstraints.EAST;
		gd.add(optionButton,c);
		
		optionButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				GenericDialog gd = new GenericDialog("Set queue");
				gd.addStringField("name", "");
				gd.addNumericField("limit", 0.0,2);
				gd.addStringField("hardware","CPU");
				
				gd.showDialog();
				
				if (gd.wasOKed())
				{
					QueueProfile q = new QueueProfile();
					q.setqueue(gd.getNextString());
					q.setlimit(gd.getNextNumber());
					q.sethardware(gd.getNextString());
					
					cq.add(q);
					
					cc.get(0).removeAll();
					
					for (int i = 0 ; i < cq.size() ; i++)
						cc.get(0).add(cq.get(i).getqueue());
					cc.get(0).select(0);
				}
			}
		});
		
		optionButton = new Button("Add");
		c = new GridBagConstraints();
		c.gridx=2; c.gridy=4; c.anchor = GridBagConstraints.EAST;
		gd.add(optionButton,c);
		
		optionButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
			}
		});
		
		
		gd.showDialog();
		
		if (gd.wasOKed())
		{
			// Create an interplugins CSV
			
			fcp = new FileClusterProfile(null);
			
			fcp.setProfileName(gd.getNextString());
			fcp.setAccessAddress(gd.getNextString());
			fcp.setRunningDir(gd.getNextString());
			
			fcp.setBatchSystemString(gd.getNextString());
			
			for (int i = 0 ; i < cq.size() ; i++)
			{
				fcp.setAcc(hw.valueOf(cq.get(i).gethardware()));
				fcp.setQueue(cq.get(i).getlimit(), cq.get(i).getqueue());
			}
			
			ClusterGUI.createClusterProfileDir();
			String dir = ClusterGUI.getClusterProfileDir() + File.separator + fcp.getProfileName() + ".csv";
			fcp.writeConfigFile(new File(dir));
		}
	}
	
	@Override
	public int setup(String arg0, ImagePlus arg1) 
	{
		GenericDialog gd = new GenericDialog("New/edit cluster profile");
		
		final ClusterProfile[] cp = ClusterGUI.getClusterProfiles();
		String cp_names[] = new String[cp.length];
		
		for (int i = 0 ; i < cp.length ; i++)
		{
			cp_names[i] = cp[i].getProfileName();
		}
		if (cp_names.length != 0)
			gd.addChoice("Cluster profiles", cp_names, cp_names[0]);
		else
			gd.addChoice("Cluster profiles", new String[]{""}, "");
			
		Button optionButton = new Button("New");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=2; c.gridy=0; c.anchor = GridBagConstraints.EAST;
		gd.add(optionButton,c);
		
		optionButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				popupClusterProfile(null);
			}
		});
		
		optionButton = new Button("Edit");
		c = new GridBagConstraints();
		c.gridx=3; c.gridy=0; c.anchor = GridBagConstraints.EAST;
		gd.add(optionButton,c);
		
		optionButton.addActionListener(new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int id = cpa.getSelectedIndex();
				popupClusterProfile(cp[id]);
			}
		});
		
		gd.showDialog();
		
		
		return DONE;
	}					
}		