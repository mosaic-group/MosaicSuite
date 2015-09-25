package mosaic.plugins;

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

import mosaic.core.cluster.BatchList;
import mosaic.core.cluster.ClusterGUI;
import mosaic.core.cluster.ClusterProfile;
import mosaic.core.cluster.ClusterProfile.hw;
import mosaic.core.cluster.FileClusterProfile;
import mosaic.core.cluster.QueueProfile;
import mosaic.core.utils.DataCompression;
import mosaic.core.utils.DataCompression.Algorithm;


/**
 * @author Pietro Incardona
 *
 * Small utility to create cluster profile
 *
 */

public class NewClusterProfile implements PlugInFilter // NO_UCD
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
     * @param cp Cluster profile
     *
     */

    @SuppressWarnings("unchecked")
    void popupClusterProfile(ClusterProfile cp)
    {
        final Vector<QueueProfile> cq = new Vector<QueueProfile>();
        final Vector<Choice> cc;

        fcp = new FileClusterProfile(null);

        final GenericDialog gd = new GenericDialog("New cluster profile");

        gd.setTitle("New cluster profile");
        if (cp != null)
        {
            gd.addStringField("profile", cp.getProfileName());
            gd.addStringField("address", cp.getAccessAddress());
            gd.addChoice("Batch", BatchList.getList(), BatchList.getList()[0]);
            gd.addStringField("run_dir", ((FileClusterProfile)cp).getRunningDirRaw());

            int totalq = 0;
            for (final hw hd : hw.values())
            {
                final QueueProfile[] qp = cp.getQueues(hd);

                // Populate cq

                for (int i = 0 ; i < qp.length ; i++) {
                    cq.add(qp[i]);
                }

                totalq += qp.length;
            }

            final String[] qs = new String[totalq];
            int cnt = 0;

            for (final hw hd : hw.values())
            {
                final QueueProfile[] qp = cp.getQueues(hd);
                for (int i = 0 ; i < qp.length ; i++)
                {
                    qs[cnt] = qp[i].getqueue() + " " + qp[i].gethardware() + " " + qp[i].getlimit();
                    cnt++;
                }
            }
            gd.addChoice("Queues", qs,qs[0]);
            gd.addChoice("compression", new String[]{""}, new String(""));
        }
        else
        {
            gd.addStringField("profile", "");
            gd.addStringField("address", "");
            gd.addChoice("Batch", BatchList.getList(), BatchList.getList()[0]);
            gd.addStringField("run_dir", "");
            gd.addChoice("queues", new String[]{""}, new String(""));
            gd.addChoice("compression", new String[]{""}, new String(""));
        }
        cc = gd.getChoices();

        Button optionButton = new Button("Add");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx=2; c.gridy=4; c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton,c);

        // Action listener for add queue button

        optionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // Show the set queue window

                final GenericDialog gd = new GenericDialog("Set queue");
                gd.addStringField("name", "");
                gd.addNumericField("limit", 0.0,2);
                gd.addStringField("hardware","CPU");

                gd.showDialog();

                if (gd.wasOKed())
                {
                    // Store the queue profile

                    final QueueProfile q = new QueueProfile();
                    q.setqueue(gd.getNextString());
                    q.setlimit(gd.getNextNumber());
                    q.sethardware(gd.getNextString());

                    cq.add(q);

                    // Add the entry to the combo box list, removing all the components and recreating the
                    // list.

                    cc.get(1).removeAll();

                    for (int i = 0 ; i < cq.size() ; i++) {
                        cc.get(1).add(cq.get(i).getqueue() + " " + cq.get(i).gethardware() + " " + cq.get(i).getlimit());
                    }
                    cc.get(1).select(0);
                }
            }
        });

        // Action listener for edit queue button

        optionButton = new Button("Edit");
        c = new GridBagConstraints();
        c.gridx=3; c.gridy=4; c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton,c);

        optionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // get the selected queue

                final int id = cc.get(1).getSelectedIndex();
                final QueueProfile q = cq.get(id);

                // popup the window

                final GenericDialog gd = new GenericDialog("Set queue");
                gd.addStringField("name", q.getqueue());
                gd.addNumericField("limit", q.getlimit(),2);
                gd.addStringField("hardware",q.gethardware());

                gd.showDialog();

                if (gd.wasOKed())
                {
                    q.setqueue(gd.getNextString());
                    q.setlimit(gd.getNextNumber());
                    q.sethardware(gd.getNextString());

                    // store the modified queue

                    cq.set(id, q);

                    // refresh the list in the combo box

                    cc.get(1).removeAll();

                    for (int i = 0 ; i < cq.size() ; i++) {
                        cc.get(1).add(cq.get(i).getqueue() + " " + cq.get(i).gethardware() + " " + cq.get(i).getlimit());
                    }
                    cc.get(1).select(0);
                }
            }
        });

        // Action listener for delete queue button

        optionButton = new Button("Delete");
        c = new GridBagConstraints();
        c.gridx=4; c.gridy=4; c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton,c);

        optionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // get the selected queue

                final int id = cc.get(1).getSelectedIndex();

                cq.remove(id);

                // refresh the list

                cc.get(1).removeAll();

                // refresh the list in the combo box

                for (int i = 0 ; i < cq.size() ; i++) {
                    cc.get(1).add(cq.get(i).getqueue() + " " + cq.get(i).gethardware() + " " + cq.get(i).getlimit());
                }
                cc.get(1).select(0);
            }
        });

        optionButton = new Button("Add");
        c = new GridBagConstraints();
        c.gridx=2; c.gridy=5; c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton,c);

        // Action listener for add compression algorithm button

        optionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final GenericDialog gd = new GenericDialog("Compression");
                final DataCompression dc = new DataCompression();
                final Vector<Algorithm> cl = dc.getCompressorList();

                // create compressor list

                final String [] compressors = new String[cl.size()];
                for (int i = 0 ; i < cl.size() ; i++)
                {
                    compressors[i] = cl.get(i).name;
                }

                gd.addChoice("Compression",compressors, compressors[0]);

                gd.showDialog();

                if (gd.wasOKed())
                {
                    final String cmp = gd.getNextChoice();

                    fcp.setCompressorString(cmp);

                    // refresh the list

                    cc.get(2).removeAll();

                    // refresh the list in the combo box, Iterate through all possible algorithm
                    // and check if they are active in the meta-data

                    for (int i = 0 ; i < cl.size() ; i++)
                    {
                        if (fcp.isActiveCompressorString(cl.get(i).name) == true)
                        {
                            cc.get(2).add(cl.get(i).name);
                        }
                    }
                }
            }
        });

        optionButton = new Button("Delete");
        c = new GridBagConstraints();
        c.gridx=3; c.gridy=5; c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton,c);

        // Action listener for delete compression algorithm button

        optionButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final DataCompression dc = new DataCompression();
                final Vector<Algorithm> cl = dc.getCompressorList();

                // Selected index

                //				int id = cc.get(2).getSelectedIndex();

                fcp.removeCompressorString(cc.get(2).getSelectedItem());

                // refresh the list

                cc.get(2).removeAll();

                // refresh the list in the combo box, Iterate through all possible algorithm
                // and check if they are active in the meta-data

                for (int i = 0 ; i < cl.size() ; i++)
                {
                    if (fcp.isActiveCompressorString(cl.get(i).name) == true)
                    {
                        cc.get(2).add(cl.get(i).name);
                    }
                }
            }
        });


        gd.showDialog();

        if (gd.wasOKed())
        {
            // Create an interplugins CSV

            fcp.setProfileName(gd.getNextString());
            fcp.setAccessAddress(gd.getNextString());
            fcp.setRunningDir(gd.getNextString());

            fcp.setBatchSystemString(gd.getNextChoice());

            for (int i = 0 ; i < cq.size() ; i++)
            {
                fcp.setAcc(hw.valueOf(cq.get(i).gethardware()));
                fcp.setQueue(cq.get(i).getlimit(), cq.get(i).getqueue());
            }

            ClusterGUI.createClusterProfileDir();
            final String dir = ClusterGUI.getClusterProfileDir() + File.separator + fcp.getProfileName() + ".csv";
            fcp.writeConfigFile(new File(dir));
        }

        // Reload cluster profiles

        final ClusterProfile[] cpA = ClusterGUI.getClusterProfiles();

        cpa.removeAll();
        for (int i = 0 ; i < cpA.length ; i++)
        {
            cpa.add( cpA[i].getProfileName());
        }
    }

    @Override
    public int setup(String arg0, ImagePlus arg1)
    {
        final GenericDialog gd = new GenericDialog("New/edit cluster profile");

        final ClusterProfile[] cp = ClusterGUI.getClusterProfiles();
        final String cp_names[] = new String[cp.length];

        for (int i = 0 ; i < cp.length ; i++)
        {
            cp_names[i] = cp[i].getProfileName();
        }
        if (cp_names.length != 0) {
            gd.addChoice("Cluster profiles", cp_names, cp_names[0]);
        }
        else {
            gd.addChoice("Cluster profiles", new String[]{""}, "");
        }

        cpa = (Choice) gd.getChoices().get(0);

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
                final int id = cpa.getSelectedIndex();
                popupClusterProfile(cp[id]);
            }
        });

        gd.showDialog();


        return DONE;
    }
}