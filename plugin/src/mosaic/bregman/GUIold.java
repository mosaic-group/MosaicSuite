package mosaic.bregman;



import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JFrame;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;

import javax.swing.SwingConstants;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.Border;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
//import javax.swing.JFrame;


public class GUIold implements ActionListener,PropertyChangeListener
{
	public JFrame frame;
	//Initialize Buttons

	//private JPanel TopPanel =  new JPanel(new GridLayout(3,1));
	private JPanel TopPanel =  new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
	private JPanel Panel1 =  new JPanel();//(new GridLayout(2,2));
	private JPanel Panel1a =  new JPanel();
	private JPanel Panel1b =  new JPanel();	
	private JPanel Panel2 =  new JPanel();
	private JPanel Panel3 =  new JPanel();
	private JPanel Panel4 =  new JPanel();
	private JPanel Panel5 =  new JPanel();

	//for panel 1
	private JButton loadA = new JButton("Open");
	private JButton loadB = new JButton("Open");
	private JLabel imgx = new JLabel("Image X");
	private JLabel imgy = new JLabel("Image Y");

	private JButton segmentA = new JButton("Segment X");
	private JButton segmentB = new JButton("Segment Y");
	private JButton coloc = new JButton("Colocalization");


	//for panel 2
	private JLabel batch = new JLabel("Batch Colocalization");
	private JButton SelectFolder = new JButton("Select Folder");
	private JButton Loop = new JButton("Loop Settings");

	//for panel 4
	private JCheckBox livedisplay= new JCheckBox("Live display", Analysis.p.livedisplay);
	private JFormattedTextField ldata= new JFormattedTextField(Analysis.p.minves_size);
	private JFormattedTextField lreg= new JFormattedTextField(Analysis.p.lreg_);
	private JFormattedTextField max= new JFormattedTextField(Analysis.p.nthreads);
	private JFormattedTextField maxsize= new JFormattedTextField(Analysis.p.maxves_size);
	private JFormattedTextField minInt= new JFormattedTextField(Analysis.p.min_intensity);
	private JFormattedTextField minIntY= new JFormattedTextField(Analysis.p.min_intensityY);
	private JFormattedTextField window= new JFormattedTextField(Analysis.p.size_rollingball);
	private JFormattedTextField rlevel= new JFormattedTextField(Analysis.p.regionSegmentLevel);
	
	private JFormattedTextField levels= new JFormattedTextField(Analysis.p.nlevels);
	private JCheckBox usePSF= new JCheckBox("PSF", Analysis.p.usePSF);
	
	private JCheckBox background= new JCheckBox("Background removal", Analysis.p.removebackground);
	
	private JLabel maxsizel= new JLabel("Max vesicle size");
	private JLabel rlevell= new JLabel("Local threshold level");
	private JLabel minIntl= new JLabel("Min intensity X");
	private JLabel minIntlY= new JLabel("Min intensity Y");
	private JLabel ldatal = new JLabel("Min vesicle size");
	private JLabel lregl = new JLabel("Lambda prior");
	private JLabel levelsl = new JLabel("Levels");
	private JCheckBox zones= new JCheckBox("Multiple regions", Analysis.p.findregionthresh);
	private JCheckBox zonesdisp= new JCheckBox("display", Analysis.p.dispvoronoi);
	private JLabel psfl = new JLabel("PSF");
	
	private JLabel segmentparams = new JLabel("Segmentation Options");
	//for panel 5
	private JLabel colocparams = new JLabel("Colocalization Options");
	private JLabel overl= new JLabel("Overlap threshold");
	private JCheckBox colocsegAB= new JCheckBox("X overlap with Y", Analysis.p.cAB);
	private JCheckBox colocsegBA= new JCheckBox("Y overlap with X", Analysis.p.cBA);
	private JCheckBox pixelint= new JCheckBox("X Vesicles intensities", Analysis.p.cint);
	private JCheckBox pixelintY= new JCheckBox("Y Vesicles intensities", Analysis.p.cintY);
	private JFormattedTextField over= new JFormattedTextField(Analysis.p.colocthreshold);

	
	
	private JCheckBox cellmaskl = new JCheckBox("Use X cell mask", Analysis.p.usecellmaskX);
	private JCheckBox cellmaskly = new JCheckBox("Use Y cell mask", Analysis.p.usecellmaskY);
	private JFormattedTextField cellmaskt= new JFormattedTextField(Analysis.p.thresholdcellmask);
	private JFormattedTextField cellmaskty= new JFormattedTextField(Analysis.p.thresholdcellmasky);
	
	private Border blackBorder;

	public GUIold() {
		initialize();
	}

	public void initialize()
	{

		blackBorder=BorderFactory.createLineBorder(Color.black);

		frame = new JFrame();
		frame.setSize(450, 610);
		frame.setResizable(false);


		Panel1.setPreferredSize(new Dimension(438, 150));
		Panel1.setBorder(blackBorder);

		Panel2.setPreferredSize(new Dimension(438, 60));
		Panel2.setBorder(blackBorder);

		Panel3.setPreferredSize(new Dimension(438, 355));
		//Panel3.setBorder(blackBorder);

		Panel4.setPreferredSize(new Dimension(216, 355));
		Panel4.setBorder(blackBorder);

		Panel5.setPreferredSize(new Dimension(216, 355));
		Panel5.setBorder(blackBorder);




		TopPanel.add(Panel1);
		TopPanel.add(Panel2);
		TopPanel.add(Panel3);

		Panel3.setLayout(null);
		Panel4.setBounds(0,0,216,355);
		Panel5.setBounds(222,0,216,355);
		
		Panel3.add(Panel4);
		Panel3.add(Panel5);



		//p1
		Panel1.setLayout(null);
		Panel1a.setBounds(2,2,435,90);
		Panel1b.setBounds(2,95,435,45);
		
		Panel1.add(Panel1a);
		Panel1.add(Panel1b);
		
		
		GroupLayout gl_panel = new GroupLayout(Panel1a);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGap(74)
					.addComponent(imgx)
					.addGap(74)
					.addComponent(loadA, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
				.addGroup(gl_panel.createSequentialGroup()
					.addGap(74)
					.addComponent(imgy)
					.addGap(74)
					.addComponent(loadB, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGap(12)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(5)
							.addComponent(imgx))
						.addComponent(loadA))
					.addGap(6)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(5)
							.addComponent(imgy))
						.addComponent(loadB)))
		);
		Panel1a.setLayout(gl_panel);
		
		
		
		Panel1a.add(loadA);
		Panel1a.add(loadB);
		Panel1a.add(imgx);
		Panel1a.add(imgy);

		Panel1b.add(segmentA);
		Panel1b.add(segmentB);
		Panel1b.add(coloc);


		Panel2.setLayout(null);

		//p2
		batch.setSize(batch.getPreferredSize());
		batch.setLocation(25,22);//(74,22);
		SelectFolder.setSize(SelectFolder.getPreferredSize());
		SelectFolder.setLocation(250,17);//170,17 (250,17);
		Loop.setSize(Loop.getPreferredSize());
		Loop.setLocation(300,17);
		Panel2.add(batch);
		Panel2.add(SelectFolder);
		//Panel2.add(Loop);
		Panel4.setLayout(null);

		
		ldata.setHorizontalAlignment(SwingConstants.CENTER);
		ldata.setColumns(4);
		
		lreg.setHorizontalAlignment(SwingConstants.CENTER);
		lreg.setColumns(4);
		
		
		cellmaskt.setHorizontalAlignment(SwingConstants.CENTER);
		cellmaskt.setColumns(4);
		cellmaskty.setHorizontalAlignment(SwingConstants.CENTER);
		cellmaskty.setColumns(4);
		over.setHorizontalAlignment(SwingConstants.CENTER);
		over.setColumns(4);
		
		max.setHorizontalAlignment(SwingConstants.CENTER);
		max.setColumns(4);
		maxsize.setHorizontalAlignment(SwingConstants.CENTER);
		maxsize.setColumns(4);
		minInt.setHorizontalAlignment(SwingConstants.CENTER);
		minInt.setColumns(4);
		minIntY.setHorizontalAlignment(SwingConstants.CENTER);
		minIntY.setColumns(4);
		levels.setHorizontalAlignment(SwingConstants.CENTER);
		levels.setColumns(4);
		window.setHorizontalAlignment(SwingConstants.CENTER);
		window.setColumns(2);
		rlevel.setHorizontalAlignment(SwingConstants.CENTER);
		rlevel.setColumns(4);
		
		segmentparams.setSize(segmentparams.getPreferredSize());
		livedisplay.setSize(livedisplay.getPreferredSize());
		ldata.setSize(ldata.getPreferredSize());
		lreg.setSize(lreg.getPreferredSize());
		cellmaskt.setSize(cellmaskt.getPreferredSize());
		cellmaskty.setSize(cellmaskty.getPreferredSize());
		over.setSize(over.getPreferredSize());
		levels.setSize(levels.getPreferredSize());
		ldatal.setSize(ldatal.getPreferredSize());
		lregl.setSize(lregl.getPreferredSize());
		rlevel.setSize(rlevel.getPreferredSize());
		rlevell.setSize(rlevell.getPreferredSize());
		levelsl.setSize(levelsl.getPreferredSize());
		background.setSize(background.getPreferredSize());
		zones.setSize(zones.getPreferredSize());
		zonesdisp.setSize(zonesdisp.getPreferredSize());
		psfl.setSize(psfl.getPreferredSize());
		usePSF.setSize(usePSF.getPreferredSize());
		maxsize.setSize(maxsize.getPreferredSize());
		maxsizel.setSize(maxsizel.getPreferredSize());
		minInt.setSize(minInt.getPreferredSize());
		minIntl.setSize(minIntl.getPreferredSize());
		minIntY.setSize(minIntY.getPreferredSize());
		minIntlY.setSize(minIntlY.getPreferredSize());
		window.setSize(window.getPreferredSize());
		
		segmentparams.setLocation(20,6);
		
		livedisplay.setLocation(10,30);
		background.setLocation(10,60);

		window.setLocation(169,60);
		
		lreg.setLocation(145,150);
		lregl.setLocation(20,155);
		
		
		rlevel.setLocation(145,120);
		rlevell.setLocation(20,125);
		
		
		zones.setLocation(10,90);
		zonesdisp.setLocation(138,90);
		ldata.setLocation(145,180);
		ldatal.setLocation(20,185);

		maxsize.setLocation(145,210);
		maxsizel.setLocation(20,215);
		
		minInt.setLocation(145,240);
		minIntl.setLocation(20,245);
		
		minIntY.setLocation(145,270);
		minIntlY.setLocation(20,275);
		
		levels.setLocation(145,300);
		levelsl.setLocation(20,305);
		
		usePSF.setLocation(15,330);
		psfl.setLocation(145,335);
		
		
		Panel4.add(segmentparams);
		Panel4.add(livedisplay);
		Panel4.add(ldata);
		Panel4.add(window);
		Panel4.add(lreg);
		Panel4.add(levels);
		
		Panel4.add(ldatal);
		Panel4.add(lregl);
		Panel4.add(rlevel);
		Panel4.add(rlevell);
		Panel4.add(levelsl);
		Panel4.add(background);
		Panel4.add(zones);
		Panel4.add(zonesdisp);
		Panel4.add(usePSF);
		
		Panel4.add(psfl);
		Panel4.add(maxsize);
		Panel4.add(maxsizel);
		
		Panel4.add(minInt);
		Panel4.add(minIntl);
		Panel4.add(minIntY);
		Panel4.add(minIntlY);
		
		
		//+PSF
		
		//panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		pixelint.setSize(pixelint.getPreferredSize());
		pixelintY.setSize(pixelintY.getPreferredSize());
		//pixelcorr.setLocation(15,35);
		
		colocsegAB.setSize(colocsegAB.getPreferredSize());
		//colocsegAB.setLocation(15,35);
		
		colocsegBA.setSize(colocsegBA.getPreferredSize());
		cellmaskl.setSize(cellmaskl.getPreferredSize());
		cellmaskly.setSize(cellmaskly.getPreferredSize());
		//colocsegA.setLocation(15,35);
		
		Panel5.add(colocparams);
		Panel5.add(pixelint);
		Panel5.add(pixelintY);
		Panel5.add(colocsegAB);
		Panel5.add(colocsegBA);
		Panel5.add(cellmaskl);
		Panel5.add(cellmaskt);
		Panel5.add(cellmaskly);
		Panel5.add(cellmaskty);
		Panel5.add(overl);
		Panel5.add(over);
		//Panel5.add(max);
		
		//Add Panel to Frame
		frame.add(TopPanel);

		//Add ActionListener for Buttons
		loadA.addActionListener(this);
		loadB.addActionListener(this);

		segmentA.addActionListener(this);
		segmentB.addActionListener(this);
		coloc.addActionListener(this);
		SelectFolder.addActionListener(this);
		Loop.addActionListener(this);
		
		livedisplay.addActionListener(this);
		background.addActionListener(this);
		zones.addActionListener(this);
		zonesdisp.addActionListener(this);
		usePSF.addActionListener(this);
		
		
		colocsegAB.addActionListener(this);
		colocsegBA.addActionListener(this);
		cellmaskl.addActionListener(this);
		cellmaskly.addActionListener(this);
		pixelint.addActionListener(this);
		pixelintY.addActionListener(this);
		
		window.addPropertyChangeListener(this);
		ldata.addPropertyChangeListener(this);
		lreg.addPropertyChangeListener(this);
		rlevel.addPropertyChangeListener(this);
		cellmaskt.addPropertyChangeListener(this);
		cellmaskty.addPropertyChangeListener(this);
		over.addPropertyChangeListener(this);
		max.addPropertyChangeListener(this);
		maxsize.addPropertyChangeListener(this);
		minInt.addPropertyChangeListener(this);
		minIntY.addPropertyChangeListener(this);
		levels.addPropertyChangeListener(this);

		
		//configure.addActionListener(this);
		//psf.addActionListener(this);
		//calibration.addActionListener(this);
		//start.addActionListener(this);
		//help.addActionListener(this);
		//exit.addActionListener(this);


		//Show Frame
		////this.pack();
		frame.setVisible(true);
	}


	/**
	 * Implements interface method of ActionListener.
	 */
	/** Called when a field's "value" property changes. */
	public void propertyChange(PropertyChangeEvent e) {
		Object source = e.getSource();
		if (source == ldata) {
			Analysis.p.minves_size=(int) ((Number)ldata.getValue()).doubleValue();
		} else if (source == lreg) {
//			Analysis.p.lreg=((Number)lreg.getValue()).doubleValue();
		} else if (source == rlevel) {
			Analysis.p.regionSegmentLevel=(int) ((Number)rlevel.getValue()).doubleValue();
		} else if (source == cellmaskt) {
			Analysis.p.thresholdcellmask=((Number)cellmaskt.getValue()).doubleValue();
		} else if (source == cellmaskty) {
			Analysis.p.thresholdcellmasky=((Number)cellmaskty.getValue()).doubleValue();
		} else if (source == over) {
			Analysis.p.colocthreshold=((Number)over.getValue()).doubleValue();	
		} else if (source == window) {
			Analysis.p.size_rollingball=(int)((Number)window.getValue()).doubleValue();
		} else if (source == max) {
			Analysis.p.nthreads=(int) ((Number)max.getValue()).doubleValue();
		} else if (source == maxsize) {
			Analysis.p.maxves_size=(int) ((Number)maxsize.getValue()).doubleValue();
		} else if (source == minInt) {
			Analysis.p.min_intensity=((Number)minInt.getValue()).doubleValue();
		} else if (source == minIntY) {
			Analysis.p.min_intensityY=((Number)minIntY.getValue()).doubleValue();
		} else if (source == levels) {
			Analysis.p.nlevels=(((Number)levels.getValue()).intValue());
			//Analysis.p.nlevels=  ((Number)levels.getValue()).intValue();

		}


	}

	public void actionPerformed(ActionEvent ae) {
		Object source = ae.getSource();	// Identify Button that was clicked

		if(source == loadA)
		{
			Analysis.loadA();
			//Analysis.load2channels();
			loadA.setText(Analysis.imgA.getTitle());
		}
		if(source == loadB)
		{
			Analysis.loadB();
			loadB.setText(Analysis.imgB.getTitle());			
		}

		if(source == segmentA)
		{
			//Analysis.p.livedisplay=true;
			Analysis.segmentA();
		}
		
		if(source == segmentB)
		{//TODO
			//Analysis.p.livedisplay=true;
			Analysis.segmentb();
		}

		if(source == coloc)
		{
//			Analysis.coloc();
		}

		if(source == SelectFolder)
		{
			Analysis.bcoloc();
			Analysis.p.looptest=false;
			//Analysis.looptest_settings();
		}
		if(source == Loop)
		{
			Analysis.p.looptest=true;
			Analysis.bcoloc();
		}

		if (source == livedisplay) {
			Analysis.p.livedisplay=livedisplay.isSelected();
		}
		if (source == colocsegBA) {
			Analysis.p.cBA=colocsegBA.isSelected();			
		}
		if (source == cellmaskl) {
			Analysis.p.usecellmaskX=cellmaskl.isSelected();			
		}
		if (source == cellmaskly) {
			Analysis.p.usecellmaskY=cellmaskly.isSelected();			
		}
		if (source == colocsegAB) {
			Analysis.p.cAB=colocsegAB.isSelected();
		}
		if (source == pixelint) {
			Analysis.p.cint=pixelint.isSelected();
		}
		if (source == pixelintY) {
			Analysis.p.cint=pixelintY.isSelected();
		}
		
		if (source == usePSF) {
			Analysis.p.usePSF=usePSF.isSelected();
			//if(usePSF)levels.
		}
		
		if (source == background) {
			//todo
			Analysis.p.removebackground=background.isSelected();
		}
		if (source == zones) {
			//todo
			Analysis.p.findregionthresh=zones.isSelected();
		}
		if (source == zonesdisp) {
			//todo
			Analysis.p.dispvoronoi=zonesdisp.isSelected();
		}


	}	
}