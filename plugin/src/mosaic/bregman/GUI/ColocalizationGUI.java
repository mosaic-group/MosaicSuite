package mosaic.bregman.GUI;


import java.awt.Checkbox;
import java.awt.Font;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mosaic.bregman.Analysis;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;


public class ColocalizationGUI implements ItemListener, ChangeListener, TextListener
{
	ImagePlus imgch1;
	ImagePlus imgch2;
	int ni,nj,nz,nc;

	JSlider t1,t2;
	TextField v1, v2;
	Checkbox m1,m2;

	boolean init1=false;
	boolean init2=false;
	ImagePlus maska_im1,maska_im2;

	//max and min intensity values in channel 1 and 2
	double max=0;
	double min=Double.POSITIVE_INFINITY;
	double max2=0;
	double min2=Double.POSITIVE_INFINITY;

	double val1,val2;

	boolean fieldval = false;
	boolean sliderval = false;
	boolean boxval= false;
	boolean refreshing = false;

	double minrange=0.001;
	double maxrange=1;
	double logmin =  Math.log10(minrange);
	double logspan= Math.log10(maxrange) - Math.log10(minrange);
	int maxslider=1000;

	private JLabel warning = new JLabel("");


	public ColocalizationGUI(ImagePlus ch1, ImagePlus ch2)
	{
		imgch1=ch1;
		imgch2=ch2;
	}


	public void run(String arg) 
	{
		Font bf = new Font(null, Font.BOLD,12);
		
		GenericDialog  gd = new GenericDialog("Colocalization options");
		
		gd.setInsets(-10,0,3);
		gd.addMessage("Cell masks (two channels images)",bf);
		
		gd.setModal(false);
		String sgroup3[] = {"Cell_mask_channel_1", "Cell_mask_channel_2"};
		boolean bgroup3[] = {false, false};

		bgroup3[0] = Analysis.p.usecellmaskX;
		bgroup3[1] = Analysis.p.usecellmaskY;
		
		t1= new JSlider();
		t2= new JSlider();

		gd.addCheckboxGroup(1, 2, sgroup3, bgroup3);
		
		gd.addNumericField("threshold_channel_1 (0 to 1)", Analysis.p.thresholdcellmask, 4);
		Panel p1 = new Panel();
		p1.add(t1);
		gd.addPanel(p1);
		
		gd.addNumericField("threshold_channel_2 (0 to 1)", Analysis.p.thresholdcellmasky, 4);
		
		Panel p2 = new Panel();
		p2.add(t2);
		gd.addPanel(p2);
		
		
		v1= (TextField) gd.getNumericFields().elementAt(0);
		v2= (TextField) gd.getNumericFields().elementAt(1);
		
		m1= (Checkbox) gd.getCheckboxes().elementAt(0);
		m2= (Checkbox) gd.getCheckboxes().elementAt(1);

		
		t1.addChangeListener(this);
		t2.addChangeListener(this);

		v1.addTextListener(this);
		v2.addTextListener(this);
		
		m1.addItemListener(this);
		m2.addItemListener(this);

		
		t1.setMinimum(0);
		t1.setMaximum(maxslider);
			
		
		t2.setMinimum(0);
		t2.setMaximum(maxslider);
		
		
		t1.setValue( (int) logvalue(Analysis.p.thresholdcellmask));
		t2.setValue( (int) logvalue(Analysis.p.thresholdcellmasky));
			
		val1 = new Double((v1.getText()));	
		val2 = new Double((v2.getText()));
		
		
		gd.showDialog();

		if(Analysis.p.usecellmaskX && imgch1!=null)
		{
			maska_im1= new ImagePlus();
			initpreviewch1(imgch1);
			previewBinaryCellMask(new Double((v1.getText())),imgch1,maska_im1,1);
			maska_im1.show();						
			init1=true;
				
		}
		
		if(Analysis.p.usecellmaskY && imgch2!=null)
		{
			maska_im2= new ImagePlus();
			initpreviewch2(imgch2);
			previewBinaryCellMask(new Double((v2.getText())),imgch2,maska_im2,2);
			maska_im2.show();						
			init2=true;
		}

		if (gd.wasCanceled()) return;

		
		Analysis.p.usecellmaskX= gd.getNextBoolean();
		Analysis.p.usecellmaskY= gd.getNextBoolean();
		Analysis.p.thresholdcellmask= gd.getNextNumber();
		Analysis.p.thresholdcellmasky= gd.getNextNumber();
		
	}
	
	
	public double expvalue(double slidervalue){

		return(Math.pow(10,(slidervalue/maxslider)*logspan + logmin));

	}

	public double logvalue(double tvalue){

		return(maxslider*(Math.log10(tvalue) - logmin)/logspan);

	}
	
	
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();	// Identify checkbox that was clicked

		boxval=true;
		if(source == m1)
		{
			boolean b=m1.getState();
			if(b){
				if(imgch1!=null){
					if(maska_im1==null)
						maska_im1= new ImagePlus();
					initpreviewch1(imgch1);
					previewBinaryCellMask(new Double((v1.getText())),imgch1,maska_im1,1);						
					maska_im1.show();						
					init1=true;
				}
				else
					warning.setText("Please open an image first.");
			}
			else{
				//hide and clean
				if(maska_im1!=null)
				maska_im1.hide();
				//maska_im1=null;
				init1=false;
			}
		}

		if(source==m2)
		{
			boolean b=m2.getState();
			if(b){
				if(imgch2!=null){
					if(maska_im2==null)
						maska_im2= new ImagePlus();
					initpreviewch2(imgch2);
					previewBinaryCellMask(new Double((v2.getText())),imgch2,maska_im2,2);
					maska_im2.show();
					init2=true;
				}
				else{
					warning.setText("Please open an image with two channels first.");
				}
			}
			else{
				//close and clean
				if(maska_im2!=null)
					maska_im2.hide();
//				maska_im2.close();
//				maska_im2=null;
				init2=false;
			}

		}
		//IJ.log("boxval to false");
		boxval=false;
	}

	
	public void textValueChanged(TextEvent e){
		Object source = e.getSource();
		
		if(!boxval && !sliderval){//prevents looped calls
			fieldval=true;
			if (source == v1 && init1) {
				double v = new Double((v1.getText()));
				if(!sliderval && val1!=v && !refreshing){
					val1=v;
					previewBinaryCellMask(v,imgch1,maska_im1,1);
					int vv= (int) (logvalue(v));
					t1.setValue(vv);

				}


			} else if (source == v2 && init2) {
				double v =new Double((v2.getText()));
				if(!sliderval && val2!=v && !refreshing){
					val2=v;
					previewBinaryCellMask(v,imgch2,maska_im2,2);
					int vv= (int) (logvalue(v));
					t2.setValue(vv);

				}
			} else if (source == v1 && !init1) {
				double v =new Double((v1.getText()));
				if(!sliderval){

					int vv= (int) (logvalue(v));
					t1.setValue(vv);

				}

			} else if (source == v2 && !init2) {
				double v =new Double((v2.getText()));
				if(!sliderval){

					int vv= (int) (logvalue(v));
					t2.setValue(vv);

				}
			} 

			fieldval=false;
		}
	}


	public void stateChanged(ChangeEvent e) {
		Object origin=e.getSource();


		if(!boxval && !fieldval){//prevents looped calls
			sliderval=true;
			if (origin==t1 && init1 && !t1.getValueIsAdjusting()){
				double value=t1.getValue();
				double vv= expvalue(value);

				if(!fieldval && val1!=vv){
					v1.setText(String.format(Locale.US,"%.4f", vv));
					val1=vv;
					previewBinaryCellMask(vv,imgch1,maska_im1,1);
				}
				refreshing = false;
			}

			
			if ((origin==t1 && init1 && t1.getValueIsAdjusting()) || (origin==t1 && !init1)){
				double value=t1.getValue();
				double vv= expvalue(value);
				if(!fieldval) {v1.setText(String.format(Locale.US,"%.4f", vv));
				}
				refreshing = true;
			}

			
			if (origin==t2 && init2 && !t2.getValueIsAdjusting()){
				double value=t2.getValue();
				double vv= expvalue(value);
				if(!fieldval && val2!=vv) {
					v2.setText(String.format(Locale.US,"%.4f", vv));
					previewBinaryCellMask(vv,imgch2,maska_im2,2);
					val2=vv;
				}
				refreshing = false;
			}

			
			if ((origin==t2 && init2 && t2.getValueIsAdjusting()) || (origin==t2 && !init2)){
				double value=t2.getValue();
				double vv= expvalue(value);
				if(!fieldval) {v2.setText(String.format(Locale.US,"%.4f", vv));
				}
				refreshing = true;
			}

			sliderval = false;

		}

	}
	
	//find min and max values in channel 1
	public void initpreviewch1(ImagePlus img)
	{

		ni=img.getWidth();
		nj=img.getHeight();
		nz=img.getNSlices();

		ImageProcessor imp;
		for (int z=0; z<nz; z++)
		{
			img.setSlice(z+1);
			imp=img.getProcessor();
			for (int i=0; i<ni; i++)
			{  
				for (int j=0;j< nj; j++)
				{
					if(imp.getPixel(i,j)>max)max=imp.getPixel(i,j);
					if(imp.getPixel(i,j)<min)min=imp.getPixel(i,j);
				}	
			}
		}

	}

	//find min and max values in channel 2
	public void initpreviewch2(ImagePlus img)
	{

		ni=img.getWidth();
		nj=img.getHeight();
		nz=img.getNSlices();
		
		ImageProcessor imp;
		for (int z=0; z<nz; z++){
			img.setSlice(z+1);
			imp=img.getProcessor();
			for (int i=0; i<ni; i++){  
				for (int j=0;j< nj; j++){  
					if(imp.getPixel(i,j)>max2)max2=imp.getPixel(i,j);
					if(imp.getPixel(i,j)<min2)min2=imp.getPixel(i,j);
				}	
			}
		}
	}
	
	//compute and display cell mask
	public void previewBinaryCellMask(double threshold_i, ImagePlus img, ImagePlus maska_im, int channel)
	{
		
		int ns =img.getSlice();
		double threshold;

		ImageProcessor imp;

		if(channel==1)
			threshold= threshold_i*(max-min)+min;
		else
			threshold= threshold_i*(max2-min2)+min2;


		ImageStack maska_ims= new ImageStack(ni,nj);

		for (int z=0; z<nz; z++){
			img.setSlice(z+1);
			imp=img.getProcessor();
			byte[] maska_bytes = new byte[ni*nj];
			for (int i=0; i<ni; i++){  
				for (int j=0;j< nj; j++){  
					if(imp.getPixel(i,j)>threshold)
						maska_bytes[j * ni + i]=(byte) 255;
					else 
						maska_bytes[j * ni + i]=0;

				}	
			}
			ByteProcessor bp = new ByteProcessor(ni, nj);
			bp.setPixels(maska_bytes);
			maska_ims.addSlice("", bp); 
		}

		maska_im.setStack("Cell mask channel " + channel,maska_ims);
		
		IJ.run(maska_im, "Invert", "stack");
		IJ.run(maska_im, "Fill Holes", "stack");
		IJ.run(maska_im, "Open", "stack");
		IJ.run(maska_im, "Invert", "stack");

		maska_im.updateAndDraw();
		maska_im.changes= false;

		img.setSlice(ns);
	}
}
	
	