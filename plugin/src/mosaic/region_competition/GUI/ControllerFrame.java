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
 * @author Stephan Seemler
 */
public class ControllerFrame extends JFrame {
    private static final long serialVersionUID = -2978938221002810146L;

    public ControllerFrame(final Region_Competition aMVC) {
        JPanel panel = new JPanel();
        panel.add(addPauseResumeButton(aMVC));
        panel.add(addStopButton(aMVC));
        panel.add(addEditButton(aMVC));

        add(panel);
        pack();
        setLocationByPlatform(true);
    }

    private JButton addEditButton(final Region_Competition aMVC) {
        JButton editButton = new JButton("Edit");
        editButton.setToolTipText("Edit Parameters");
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final GenericDialogGUI userDialog = new GenericDialogGUI(aMVC.settings, aMVC.getOriginalImPlus());
                userDialog.showDialog();
                userDialog.processInput();
            }
        });
        return editButton;
    }

    private JButton addStopButton(final Region_Competition aMVC) {
        JButton stopButton = new JButton("Stop");
        stopButton.setToolTipText("Stops algorithm after current iteration");
        stopButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final Algorithm algorithm = aMVC.getAlgorithm();
                algorithm.stop();
                dispose();
            }
        });
        return stopButton;
    }

    private JButton addPauseResumeButton(final Region_Competition aMVC) {
        final JButton resumeButton = new JButton("Pause");
        resumeButton.setToolTipText("Pauses/Resumes algorithm after current iteration");
        resumeButton.addActionListener(new ActionListener() {

            boolean isPaused = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                final Algorithm algorithm = aMVC.getAlgorithm();

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

                pack();
                validate();
            }
        });
        return resumeButton;
    }
}
