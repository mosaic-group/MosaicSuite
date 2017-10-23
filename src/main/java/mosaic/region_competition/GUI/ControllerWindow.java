package mosaic.region_competition.GUI;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class ControllerWindow extends JFrame {
    private static final long serialVersionUID = -2978938221002810146L;

    protected final Controller iController;
    
    public ControllerWindow(final Controller aController) {
        iController = aController;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        JPanel panel = new JPanel();
        panel.add(addPauseResumeButton());
        panel.add(addStopButton());

        add(panel);
        pack();
        setLocationByPlatform(true);
        
        addWindowListener(new WindowListener() {
            @Override
            public void windowClosing(WindowEvent e) {
                aController.stop();
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                aController.stop();
            }

            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {}

            @Override
            public void windowActivated(WindowEvent e) {}
        });
    }

    private JButton addStopButton() {
        JButton stopButton = new JButton("Stop");
        stopButton.setToolTipText("Stops algorithm after current iteration");
        stopButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                iController.stop();
                dispose();
            }
        });
        return stopButton;
    }

    private JButton addPauseResumeButton() {
        final JButton resumeButton = new JButton("Pause");
        resumeButton.setToolTipText("Pauses/Resumes algorithm after current iteration");
        resumeButton.addActionListener(new ActionListener() {
            private boolean isPaused = false;

            @Override
            public void actionPerformed(ActionEvent e) {

                if (!isPaused) {
                    isPaused = true;
                    resumeButton.setText("Resume");
                    iController.pause();
                }
                else {
                    isPaused = false;
                    resumeButton.setText("Pause");
                    iController.resume();
                }

                pack();
                validate();
            }
        });
        return resumeButton;
    }
}
