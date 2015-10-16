package mosaic.region_competition.wizard;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import mosaic.core.utils.IntensityImage;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Point;
import mosaic.core.utils.RegionIterator;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.plugins.Region_Competition.InitializationType;
import mosaic.region_competition.LabelImageRC;
import mosaic.region_competition.PointCM;
import mosaic.region_competition.Settings;
import mosaic.region_competition.wizard.RCProgressWin.StatusSel;
import mosaic.region_competition.wizard.score_function.ScoreFunction;
import mosaic.region_competition.wizard.score_function.ScoreFunctionInit;
import mosaic.region_competition.wizard.score_function.ScoreFunctionRCsmo;
import mosaic.region_competition.wizard.score_function.ScoreFunctionRCtop;
import mosaic.region_competition.wizard.score_function.ScoreFunctionRCvol;


public class RCWWin extends JDialog implements MouseListener, Runnable {

    private static final long serialVersionUID = 1L;

    private enum segType {
        Tissue, Cell, Other
    }

    protected Settings ref_save;
    private final JPanel contentPane;
    private final JComboBox<String> b1;
    private ImagePlus img[];
    protected segType sT;

    public void start(Settings s) {
        ref_save = s;
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    final RCWWin frame = new RCWWin();
                    frame.ref_save = ref_save;
                    frame.setVisible(true);
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private double Ask(String title, String message) {
        final GenericDialog ask = new GenericDialog(title);
        ask.addNumericField(message, 1, 1);
        ask.showDialog();

        if (ask.wasCanceled()) {
            return -1;
        }
        else {
            return ask.getNextNumber();
        }
    }

    private String[] GetROI() {
        RoiManager manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }
        final Roi[] roisArray = manager.getRoisAsArray();
        final String[] md = new String[roisArray.length];
        int i = 0;
        for (final Roi roi : roisArray) {
            md[i] = roi.getName();
            i++;
        }
        return md;
    }

    private static JButton getButtonSubComponent(Container container) {
        if (container instanceof JButton) {
            return (JButton) container;
        }
        else {
            final Component[] components = container.getComponents();
            for (final Component component : components) {
                if (component instanceof Container) {
                    return getButtonSubComponent((Container) component);
                }
            }
        }
        return null;
    }

    /**
     * Create the frame.
     */
    public RCWWin() {
        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new GridLayout(0, 2, 0, 0));

        final JLabel lblNewLabel = new JLabel("What you are segmenting: ");
        contentPane.add(lblNewLabel);

        sT = segType.Tissue;
        final JComboBox<segType> comboBox = new JComboBox<segType>(segType.values());
        contentPane.add(comboBox);
        comboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                sT = (segType) comboBox.getSelectedItem();
            }
        });

        final JLabel lblNewLabel_1 = new JLabel("<html>Do you have a Point spread <br>function image ?</html>");
        contentPane.add(lblNewLabel_1);

        final JButton btnNewButton = new JButton("Browse");
        contentPane.add(btnNewButton);

        final JLabel lblNewLabel_2 = new JLabel("<html>Select a region of interest <br>with the imageJ selection<br> tool</html>");
        contentPane.add(lblNewLabel_2);

        final JComboBox<String> comboBox_1 = new JComboBox<String>();

        final String[] md = GetROI();

        comboBox_1.setBackground(Color.YELLOW);
        contentPane.add(comboBox_1);
        comboBox_1.setModel((new DefaultComboBoxModel<String>(md)));

        b1 = comboBox_1;

        // Add a refresh for the action listener

        getButtonSubComponent(comboBox_1).addMouseListener(this);

        // OK and Cancel button

        final JButton btnOKButton = new JButton("OK");
        contentPane.add(btnOKButton);
        btnOKButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                ComputePar();
            }
        });

        final JButton btnCancelButton = new JButton("Cancel");
        contentPane.add(btnCancelButton);

    }

    protected void ComputePar() {
        int i = 0;

        // Get if is Tissue or Cell

        // Get the regions

        final RoiManager manager = RoiManager.getInstance();
        final Roi[] roisArray = manager.getRoisAsArray();
        img = new ImagePlus[roisArray.length];
        for (final Roi roi : roisArray) {
            final Rectangle b = roi.getBounds();

            // Convert the whole image to float and normalize
            img[i] = new ImagePlus(roi.getName(), MosaicUtils.normalizeAllSlices(ij.WindowManager.getImage(roi.getImageID())).getProcessor());
            final ImageProcessor ip = img[i].getProcessor();
            ip.setRoi(b.x, b.y, b.width, b.height);
            img[i].setProcessor(null, ip.crop());
            i++;
        }

        // Start computation thread

        final Thread t = new Thread(this, "Compute parameters");
        t.start();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
        final String[] md = GetROI();
        b1.setModel((new DefaultComboBoxModel<String>(md)));
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }

    private Settings OptimizeWithCMA(ScoreFunction fi, Settings s, double aDev[], String Question, double stop, boolean debug) {
        // Start a progress window

        final RCProgressWin RCp = new RCProgressWin(9, img.length);
        RCp.SetProgress(0);
        RCp.setVisible(true);

        final double aMean[] = fi.getAMean(s);

        CMAEvolutionStrategy solver = null;
        boolean restart = true;
        int restart_it = 0;
        while (restart == true && restart_it < 5) {
            solver = new CMAEvolutionStrategy();
            solver.parameters.setPopulationSize(6);
            solver.parameters.setMu(4);
            solver.readProperties();
            solver.setDimension(aMean.length);
            solver.setInitialX(aMean);
            solver.setInitialStandardDeviations(aDev);
            solver.options.stopFitness = stop;

            // initialize cma and get fitness array to fill in later

            final double[] fitness = solver.init();

            // initial output file

            solver.writeToDefaultFilesHeaders(0);

            // iteration loop

            int n_it = 0;

            while (solver.stopConditions.getNumber() == 0) {
                final double[][] pop = solver.samplePopulation();

                for (int i = 0; i < pop.length; i++) {
                    while (!fi.isFeasible(pop[i])) {
                        solver.resampleSingle(i);
                    }

                    fitness[i] = fi.valueOf(pop[i]);
                    // String test[] = {"/tmp/RC_test.tif"};
                    if (RCp.getSelectionStatus() == StatusSel.STOP) {
                        break;
                    }

                    if (fi.getTypeImage() == ScoreFunction.TypeImage.IMAGEPLUS) {
                        RCp.SetImage(fitness[i], fi.createSettings(s, pop[i]), fi.getImagesIP());
                    }
                    else {
                        RCp.SetImage(fitness[i], fi.createSettings(s, pop[i]), fi.getImagesString());
                    }
                }

                if (RCp.getSelectionStatus() == StatusSel.STOP) {
                    break;
                }

                for (int i = 0; i < pop.length; i++) {
                    System.out.println("Pop " + i);
                    for (int j = 0; j < pop[i].length; j++) {
                        System.out.println(pop[i][j]);
                    }
                    System.out.println("fitness " + fitness[i]);
                }

                if (debug == true) {
                    System.out.println("Press Enter to continue");
                    try {
                        System.in.read();
                    }
                    catch (final Exception e) {
                    }
                }

                solver.updateDistribution(fitness);

                solver.writeToDefaultFiles();
                final int outmod = 150;
                if (solver.getCountIter() % (15 * outmod) == 1) {
                    solver.printlnAnnotation();
                }
                if (solver.getCountIter() % outmod == 1) {
                    solver.println();
                }

                n_it++;
                if (n_it >= 40) {
                    break;
                }

                RCp.SetProgress((int) (n_it / 40.0 * 100.0));

                System.out.println(n_it);
            }

            RCp.SetProgress(100);

            // final output

            solver.writeToDefaultFiles(1);
            solver.println();
            solver.println("Terminated due to");
            for (final String sc : solver.stopConditions.getMessages()) {
                solver.println("    " + sc);
            }
            solver.println("best function value " + solver.getBestFunctionValue() + " at evaluation " + solver.getBestEvaluationNumber());

            /*
             * System.out.println("Parameter 0: " + (solver.getBestX())[0]);
             * System.out.println("Parameter 1: " + (solver.getBestX())[1]);
             */

            RCp.SetStatusMessage(Question);
            RCp.waitClose();
            s = RCp.getSelection();

            // evaluate mean value as it is the best estimator for the optimum
            solver.setFitnessOfMeanX(fi.valueOf(fi.getAMean(s)));

            fi.valueOf(fi.getAMean(s));
            fi.show();

            restart = false;

            // double ans = Ask("Question",Question);
            /*
             * if (ans == 1.0)
             * restart = false;
             * else
             * {
             * restart = true;
             * fi.incrementStep();
             * }
             */

            restart_it++;
        }

        return s;
    }

    
    public static PointCM[] createCMModel(LabelImageRC lirc) {
        // set of the old labels
        final HashMap<Integer, PointCM> Labels = new HashMap<Integer, PointCM>(); // set

        final int size = lirc.getSize();

        // what are the old labels?
        for (int i = 0; i < size; i++) {
            final int l = lirc.getLabel(i);
            if (lirc.isSpecialLabel(l)) {
                continue;
            }
            if (Labels.get(l) == null) {
                final PointCM tmp = new PointCM();
                tmp.p = new Point(new int [lirc.getDimensions().length]);
                Labels.put(l, tmp);
            }
        }

        final int[] off = new int[] { 0, 0 };

        final RegionIterator img = new RegionIterator(lirc.getDimensions(), lirc.getDimensions(), off);

        while (img.hasNext()) {
            final Point p = img.getPoint();
            final int i = img.next();
//            if (lirc.getDataLabel()[i] != LabelImage.BGLabel && lirc.getDataLabel()[i] != LabelImageRC.ForbiddenLabel) {
            if (!lirc.isSpecialLabel(lirc.getDataLabel()[i])) {
                final int id = Math.abs(lirc.getDataLabel()[i]);

                Labels.get(id).p = Labels.get(id).p.add(p);
                Labels.get(id).count++;
            }
        }

        for (final PointCM p : Labels.values()) {
            p.p = p.p.div(p.count);
        }

        return Labels.values().toArray(new PointCM[Labels.size()]);
    }
    
    @Override
    public void run() {
        // Start with standard settings

        final Settings s = ref_save;
        s.labelImageInitType = InitializationType.LocalMax;

        // if is a Tissue produce pow(2,d) < r < pow(3,d) regions (region tol 8)
        // if is a Cell produce 1 region (region tol 16)

        boolean AreaSet = false;

        // Convert ImagePlus into Intensity image

        int sigma = 2;
        double tol = 0.005;
        int r_t = 8;
        int rad = 8;

        sigma = 4;
        tol = 0.01;
        r_t = 0;
        rad = 0;

        final IntensityImage in[] = new IntensityImage[img.length];
        final LabelImageRC lb[] = new LabelImageRC[img.length];

        for (int i = 0; i < img.length; i++) {
            in[i] = new IntensityImage(img[i], false);
            lb[i] = new LabelImageRC(in[i].getDimensions());
        }

        // Set initialization local minima

        ScoreFunctionInit fi = null;

        if (sT == segType.Cell || sT == segType.Tissue) {
            for (int i = 0; i < img.length; i++) {
                in[i] = new IntensityImage(img[i], false);
                lb[i] = new LabelImageRC(in[i].getDimensions());
                in[i].getImageIP().show();
            }

            fi = new ScoreFunctionInit(in, lb, r_t, rad);
            for (int i = 0; i < img.length; i++) {
                if (sT == segType.Tissue) {
                    fi.setObject(i, (int) (1.5 * Ask("Question", "How many edge do you see on " + in[i].getImageIP().getShortTitle() + "?")));
                }
                else {
                    fi.setObject(i, (int) Ask("Question", "How many object do you see on " + in[i].getImageIP().getShortTitle() + "?"));
                }
            }
        }
        else {
            fi = new ScoreFunctionInit(in, lb, r_t, rad);
        }

        final double aMean[] = new double[2];
        double aDev[] = new double[2];
        aMean[0] = sigma;
        aMean[1] = tol;
        aDev[0] = 0.5;
        aDev[1] = 0.005;

        s.copy(OptimizeWithCMA(fi, s, aDev, "Check whenever all objects has at least 1 or more region (Yes = 1)", 0.3, false));

        // we work on E_lenght and R_k E_merge PS_radius Ballon

        s.labelImageInitType = InitializationType.LocalMax;
        s.m_EnergyFunctional = EnergyFunctionalType.e_PC;
        s.m_CurvatureMaskRadius = 4;
        if (sT == segType.Tissue) {
            s.m_RegionMergingThreshold = 2.0f;
            // s.m_BalloonForceCoeff = 0.03f;
            // s.m_GaussPSEnergyRadius = 22;
            // s.m_ConstantOutwardFlow = (float) 0.04;
        }

        /*
         * img[0].show();
         * lb[0] = RCPainter(img[0],s);
         * ImagePlus lb_m = lb[0].createMeanImage();
         * lb_m.show();
         */

        final int sizeA[] = new int[in.length];
        final double sizeS[] = new double[in.length];
        final int stop[] = new int[in.length];
        final double stopd[] = new double[in.length];

        for (int i = 0; i < lb.length; i++) {
            final ScoreFunctionRCvol tmpA = new ScoreFunctionRCvol(in, lb, s);
            sizeA[i] = tmpA.Area(lb[i]);
            final LabelImageRC lbtmp = new LabelImageRC(lb[i]);
            final ScoreFunctionRCsmo tmpS = new ScoreFunctionRCsmo(in, lb, s);
            lbtmp.initBoundary();
            lbtmp.initContour();
            sizeS[i] = tmpS.Smooth(lbtmp);
        }

        int fun = 0;

        while (fun <= 3) {
            // choose the score

            fun = (int) Ask("Choose the scoring function", "Choose the scooring function 1 = volume, 2 = Topology, 3=Smooth");

            //

            if (fun == 1) {
                aDev = new double[2];

                aDev[0] = 3.0;
                aDev[1] = 0.01;

                final ScoreFunctionRCvol fiRC = new ScoreFunctionRCvol(in, lb, s);

                final double factor[] = new double[in.length];

                for (int i = 0; i < in.length; i++) {
                    factor[i] = Ask("Area fix", "Increase the region by a factor of " + in[i].getImageIP().getShortTitle());
                    stop[i] = (int) (Math.abs(((factor[i] * sizeA[i]) - sizeA[i])) / 100.0 * 50.0);
                    sizeA[i] = (int) (factor[i] * sizeA[i]);
                }
                fiRC.setArea(sizeA);

                s.copy(OptimizeWithCMA(fiRC, s, aDev, "Check and choose the best segmentation", stop[0], false));

                // s.m_GaussPSEnergyRadius = (int)solver.getBestX()[0];
                // s.m_BalloonForceCoeff = (float)solver.getBestX()[1];

                // Recalculate Area and smooth

                for (int i = 0; i < in.length; i++) {
                    System.out.println("Area target: " + sizeA[i]);
                    sizeA[i] = fiRC.Area(fiRC.getLabel(i));
                    System.out.println("Area: " + sizeA[i]);
                    final ScoreFunctionRCsmo tmpS = new ScoreFunctionRCsmo(in, lb, s);
                    final LabelImageRC lbtmp = fiRC.getLabel(i);
                    lbtmp.initBoundary();
                    lbtmp.initContour();
                    sizeS[i] = tmpS.Smooth(lbtmp);
                }

                // Set

                AreaSet = true;

                // Print out

                System.out.println("Mask radius  " + s.m_CurvatureMaskRadius);
                System.out.println("Baloon force " + s.m_BalloonForceCoeff);
                System.out.println("PS radius " + s.m_GaussPSEnergyRadius);
                // System.out.println("Area target " + fiRC.Area[0] + "  Area reached: " + sizeA[0]);
                System.out.println("Smooth " + sizeS[0]);

            }
            else if (fun == 2) {
                aDev = new double[1];
                aDev[0] = 0.005;

                // Show the image and wait to close

                final ScoreFunctionRCtop fiRC = new ScoreFunctionRCtop(in, lb, s);

                fiRC.MergeAndDivideWin(lb);

                lb[0].show("Result of Topological fix", 255);

                //

                for (int i = 0; i < lb.length; i++) {
                    final PointCM mod[] = createCMModel(lb[i]);
                    fiRC.setCMModel(mod, i);
                }

                s.copy(OptimizeWithCMA(fiRC, s, aDev, "Check whenever the segmentation is reasonable", stopd[0], false));
            }
            else if (fun == 3) {
                aDev = new double[2];

                aDev[0] = 2.0;
                aDev[1] = 0.005;

                final ScoreFunctionRCsmo fiRC = new ScoreFunctionRCsmo(in, lb, s);

                final double factor = Ask("Smooth fix", "Increase the smooth of the region");

                sizeS[0] = factor * sizeS[0];
                stopd[0] = (Math.abs(((factor * sizeS[0]) - sizeS[0])) / 100.0 * 50.0);
                fiRC.setSmooth(sizeS);

                final ScoreFunctionRCvol tmpA = new ScoreFunctionRCvol(in, lb, s);
                sizeA[0] = tmpA.Area(fiRC.getLabel(0));

                if (AreaSet == true) {
                    fiRC.setArea(sizeA);
                }

                s.copy(OptimizeWithCMA(fiRC, s, aDev, "Check whenever the segmentation is reasonable", stopd[0], false));

                // Recalculate Area and smooth

                for (int i = 0; i < in.length; i++) {
                    final ScoreFunctionRCvol tmpS = new ScoreFunctionRCvol(in, lb, s);
                    System.out.println("Area target: " + sizeA[i]);
                    sizeA[i] = tmpS.Area(fiRC.getLabel(i));
                    System.out.println("Area: " + sizeA[i]);
                    final LabelImageRC lbtmp = fiRC.getLabel(i);
                    lbtmp.initBoundary();
                    lbtmp.initContour();
                    sizeS[i] = fiRC.Smooth(lbtmp);
                }

                // Print out

                System.out.println("Mask radius  " + s.m_CurvatureMaskRadius);
                System.out.println("Energy Contour Length  " + s.m_EnergyContourLengthCoeff);
                System.out.println("Baloon force " + s.m_BalloonForceCoeff);
                System.out.println("PS radius " + s.m_GaussPSEnergyRadius);
                System.out.println("Area " + sizeA[0]);
            }
        }
    }
}
