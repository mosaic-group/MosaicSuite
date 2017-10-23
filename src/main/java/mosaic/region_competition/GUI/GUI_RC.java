package mosaic.region_competition.GUI;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.plugins.Region_Competition.InitializationType;
import mosaic.plugins.Region_Competition.RegularizationType;
import mosaic.region_competition.PluginSettingsRC;

public class GUI_RC extends GUI {
    PluginSettingsRC settingsRc;
    
    public GUI_RC(PluginSettingsRC aSettings, ImagePlus aInputImg, boolean aInRcMode) {
        super("Region Competition", aSettings, aInputImg, aInRcMode);
        settingsRc = aSettings;
    }
    
    /**
     * Create parameters dialog
     */
    @Override
    protected void createParametersDialog() {
        GenericDialog gd = new GenericDialog("Region Competition Parameters");
        final Font bf = new Font(null, Font.BOLD, 12);
        
        int gridy = 1;
        final int gridx = 2;

        gd.addMessage("Energy and initialization settings", bf);
        // Energy Functional
        final EnergyFunctionalType[] energyValues = EnergyFunctionalType.values();
        final String[] energyItems = new String[energyValues.length];
        for (int i = 0; i < energyValues.length; ++i) {
            energyItems[i] = energyValues[i].name();
        }
        gd.addChoice("E_data", energyItems, iSettings.m_EnergyFunctional.name());
        Choice choiceEnergy = (Choice) gd.getChoices().lastElement();
        {
            Button optionButton = new Button("Options");
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = gridx;
            c.gridy = gridy++;
            c.anchor = GridBagConstraints.EAST;
            gd.add(optionButton, c);

            optionButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String energy = choiceEnergy.getSelectedItem();
                    SettingsBaseGUI energyGUI = EnergyGUI.factory(iSettings, energy);
                    energyGUI.createDialog();
                    energyGUI.showDialog();
                    energyGUI.processDialog();
                }
            });
        }

        // Regularization
        final RegularizationType[] regularizationValues = RegularizationType.values();
        final String[] regularizationItems = new String[regularizationValues.length];
        for (int i = 0; i < regularizationValues.length; ++i) {
            regularizationItems[i] = regularizationValues[i].name();
        }
        gd.addChoice("E_length", regularizationItems, iSettings.regularizationType.name());
        
        Choice choiceRegularization = (Choice) gd.getChoices().lastElement();
        {
            Button optionButton = new Button("Options");
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.EAST;
            c.gridx = gridx;
            c.gridy = gridy++;
            gd.add(optionButton, c);

            optionButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final String type = choiceRegularization.getSelectedItem();
                    final SettingsBaseGUI gui = RegularizationGUI.factory(iSettings, type);
                    gui.createDialog();
                    gui.showDialog();
                    gui.processDialog();
                }
            });
        }

        // Label Image Initialization
        final InitializationType[] initTypes = InitializationType.values();
        final String[] initializationItems = new String[initTypes.length];
        for (int i = 0; i < initTypes.length; ++i) {
            initializationItems[i] = initTypes[i].name();
        }
        gd.addChoice("Initialization", initializationItems, iSettings.labelImageInitType.name());
        
        // save reference to this choice, so we can handle it
        Choice initializationChoice = (Choice) gd.getChoices().lastElement();

        Button optionButton = new Button("Options");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridx;
        c.gridy = gridy++;
        c.anchor = GridBagConstraints.EAST;
        gd.add(optionButton, c);

        optionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final String type = initializationChoice.getSelectedItem();
                final SettingsBaseGUI gui = InitializationGUI.factory(iSettings, type);
                gui.createDialog();
                gui.showDialog();
                gui.processDialog();
            }
        });
        
        gd.addMessage("\nGeneral settings", bf);
        gd.addNumericField("Lambda E_length", iSettings.m_EnergyContourLengthCoeff, 4, 8, "");
        gd.addNumericField("Max_Iterations", iSettings.m_MaxNbIterations, 0, 8, "");
        gd.addCheckboxGroup(1, 4, new String[] { "Fusion", "Fission", "Handles" }, new boolean[] { iSettings.m_AllowFusion, iSettings.m_AllowFission, iSettings.m_AllowHandles });

        gd.addNumericField("Theta E_merge", iSettings.m_RegionMergingThreshold, 4, 8, "");
        gd.addNumericField("Oscillation threshold (Convergence)", settingsRc.m_OscillationThreshold, 4, 8, "");
        
        gd.showDialog();

        // On OK, read parameters
        if (gd.wasOKed()) {
            // Energy Choice
            final String energy = gd.getNextChoice();
            iSettings.m_EnergyFunctional = EnergyFunctionalType.valueOf(energy);
            final EnergyGUI eg = EnergyGUI.factory(iSettings, iSettings.m_EnergyFunctional);
            eg.createDialog();
            eg.processDialog();
            
            // Regularization Choice
            final String regularization = gd.getNextChoice();
            iSettings.regularizationType = RegularizationType.valueOf(regularization);
            iSettings.m_EnergyContourLengthCoeff = (float) gd.getNextNumber();
            iSettings.m_MaxNbIterations = (int) gd.getNextNumber();
            
            // Topological constraints
            iSettings.m_AllowFusion = gd.getNextBoolean();
            iSettings.m_AllowFission = gd.getNextBoolean();
            iSettings.m_AllowHandles = gd.getNextBoolean();

            // RC settings
            iSettings.m_RegionMergingThreshold = (float) gd.getNextNumber();
            settingsRc.m_OscillationThreshold = gd.getNextNumber();

            // Initialization
            final String initialization = gd.getNextChoice();
            final InitializationType type = InitializationType.valueOf(initialization);
            iSettings.labelImageInitType = type;
            final InitializationGUI ig = InitializationGUI.factory(iSettings, iSettings.labelImageInitType);
            ig.createDialog();
            ig.processDialog();
            
        }
    }
}
