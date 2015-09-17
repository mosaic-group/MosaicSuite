package mosaic.region_competition.GUI;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import mosaic.plugins.Region_Competition;
import mosaic.region_competition.Algorithm;


/**
 * Class that control the algorithm, you can stop or put on pause the algorithm
 *
 * @author Stephan Seemler
 */

public class ControllerFrame extends JFrame {

    private static final long serialVersionUID = -2978938221002810146L;

    Region_Competition MVC;
    // Algorithm algorithm;

    JFrame controllerFrame;
    JPanel panel;

    JButton resumeButton;
    JButton stopButton;
    JButton editButton;

    public ControllerFrame(Region_Competition mvc) {
        this.MVC = mvc;

        controllerFrame = this;

        panel = new JPanel();

        // Pause/Resume
        resumeButton = new JButton("Pause");
        resumeButton.setToolTipText("Pauses/Resumes algorithm after current iteration");
        resumeButton.addActionListener(new ActionListener() {

            boolean isPaused = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                Algorithm algorithm = MVC.getAlgorithm();

                if (!isPaused) {
                    isPaused = true;
                    resumeButton.setText("Resume");
                    algorithm.pause();
                }
                else {
                    isPaused = false;
                    resumeButton.setText("Pause");
                    algorithm.resume();
                }

                controllerFrame.pack();
                controllerFrame.validate();
            }
        });
        panel.add(resumeButton);

        // Stop
        stopButton = new JButton("Stop");
        stopButton.setToolTipText("Stops algorithm after current iteration");
        stopButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Algorithm algorithm = MVC.getAlgorithm();
                algorithm.stop();
                // cancelButton.setVisible(false);
                controllerFrame.dispose();
            }
        });
        // p.setUndecorated(true);
        panel.add(stopButton);

        // Edit
        editButton = new JButton("Edit");
        editButton.setToolTipText("Edit Parameters");
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                GenericDialogGUI userDialog = new GenericDialogGUI(MVC.settings, MVC.getOriginalImPlus());
                userDialog.showDialog();
                userDialog.processInput();
            }
        });
        panel.add(editButton);

        // /////////////////////////////////////////////

        controllerFrame.add(panel);
        controllerFrame.pack();
        controllerFrame.setLocationByPlatform(true);

        // cancelButton.setLocationRelativeTo(IJ.getInstance());
        // java.awt.Point p = cancelButton.getLocation();
        // p.x-=150;
        // cancelButton.setLocation(p);

        // controllerFrame.setVisible(true);
    }

}
