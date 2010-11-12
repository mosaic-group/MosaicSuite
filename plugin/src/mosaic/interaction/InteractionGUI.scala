package mosaic.interaction

import mosaic.interaction.input.MatlabData
import javax.swing.JTextField
import mosaic.core.sampling.HypothesisTesting
import mosaic.core.sampling.QDistribution
import scalala.tensor.dense.DenseVector
import mosaic.core.optimization.LikelihoodOptimizer
import ij.gui.GenericDialog
import mosaic.interaction.input.ImagePreparation
import ij._
import javax.swing.{JFrame, JPanel,JTabbedPane,JButton, JComboBox, JLabel, JTextArea, SwingConstants}
import swing._
import java.awt.{GridLayout, Dimension, Color}
import java.awt.event._
import ij.plugin.BrowserLauncher
import scalala.Scalala._


trait InteractionGUI extends ImageListener with ActionListener with ImagePreparation  {
	
	 val model = new InteractionModel

	 var frame: JFrame = null
	 var superPanel,imgTab: JPanel = null
	 var tabs: JTabbedPane = null
	 var linkPDF, analyze: JButton = null
	 var imgAS, imgBS: ComboBox[ImagePlus] = null
	 var warning: JLabel = null
	 val warText = "Please check red labeled tabs before launching analysis"
	 var inputSource = 0
		
	// no ActionListner needed
	implicit def actionPerformedWrapper(func: (ActionEvent) => Unit) = 
    new ActionListener { def actionPerformed(e:ActionEvent) = func(e) }

	 
	 def GUI() {
		 frame = new JFrame("Co-Localization Interaction Plugin")
		 frame.setSize(400, 585)
		 frame.setResizable(true)
		 frame.setVisible(true)
		 
		 superPanel = new JPanel()
		 frame.getContentPane().add(superPanel)
		 tabs = new JTabbedPane(SwingConstants.BOTTOM);
	     tabs.setOpaque(true);
	     warning=new JLabel(warText);
	     warning.setForeground(Color.RED);
	     analyze=new JButton("Analyze");
	     analyze.addActionListener(this);

	     // Input type selection
		 val gd = new GenericDialog("Input type selection...", IJ.getInstance());
		 gd.addChoice("Input source:", Array("Images","Matlab File .mat", "Matlab Debug", "Debug"), "Images")
		 gd.addChoice("Dimensions:", Array("2D","3D"), "3D")
		 gd.showDialog()
		 inputSource = gd.getNextChoiceIndex
		 model.dim = gd.getNextChoiceIndex + 2
		 
		 // TODO Remove debugging
		 if (inputSource == 3) {
			 if (WindowManager.getIDList == null){
				(new ij.plugin.Macro_Runner).run("JAR:macros/StacksOpen_.ijm") 
			}
			 inputSource = 0
		 }
		 
		 if (inputSource == 0) {
			 //imgTab
			 imgTab = new JPanel(new GridLayout(2,2))
			 imgTab.setPreferredSize(new Dimension(380, 45));
			 superPanel.add(imgTab)

			 
			 imgAS = new ComboBox(getImgList) {
				 renderer = swing.ListView.Renderer(_.getTitle) // The renderer is just the title field of the ImagePlus class.
			 }
			 setImage(imgAS.selection.item,0)
			 imgAS.peer.addActionListener((e:ActionEvent) => setImage(imgAS.selection.item,0) )
			 imgTab.add(new JLabel("Image of reference group Y"));
			 imgTab.add(imgAS.peer)
			 
			 imgBS = new ComboBox(getImgList) {
				 renderer = swing.ListView.Renderer(_.getTitle) // The renderer is just the title field of the ImagePlus class.
			 }
			 setImage(imgBS.selection.item,1) 
			 imgBS.peer.addActionListener((e:ActionEvent) => setImage(imgBS.selection.item,1) )
			 imgTab.add(new JLabel("Image of group X"));
			 imgTab.add(imgBS.peer)
		 }
		 if (inputSource == 1) 
			 matlabInput
		 
		 		
		 superPanel.add(tabs)
		 
	     //add tabs
	     aboutTab
	     chromaticAberTab
	     potentialTab
	   
        superPanel.add(warning);
	    superPanel.add(analyze);
	    
	    def matlabInput {
			 val matlabTab = new JPanel(new GridLayout(6,2))
			 matlabTab.setPreferredSize(new Dimension(380, 200));
			 superPanel.add(matlabTab)
			 
			// object positions
			val matlabPath = new JTextField("filepath");
	        val matY = new JTextField("Matrix Y");
	        val matX = new JTextField("Matrix X");
	
	        val chroValTab = new JPanel(new GridLayout(3,3));
	        
	        matlabTab.add(matlabPath)
	        matlabTab.add(new JLabel(""))
	        matlabTab.add(new JLabel("reference group Y:"));
	        matlabTab.add(new JLabel("group X:"));
	        matlabTab.add(matY)
	        matlabTab.add(matX)
	        
	        // object domain
	        val dx = new JTextField("1");
	        val dy = new JTextField("1");
	        val dz = new JTextField("1");
	        matlabTab.add(new JLabel("Domain x"));
	        matlabTab.add(dx)
	        matlabTab.add(new JLabel("Domain y"));
	        matlabTab.add(dy)
	        matlabTab.add(new JLabel("Domain z"));
	        matlabTab.add(dz)
	        
	        matlabPath.addActionListener((e:ActionEvent) => setPrefs)
	        matY.addActionListener((e:ActionEvent) => setPrefs)
	        matX.addActionListener((e:ActionEvent) => setPrefs)

	        dx.addActionListener((e:ActionEvent) => setPrefs)
	        dx.addActionListener((e:ActionEvent) => setPrefs)
	        dx.addActionListener((e:ActionEvent) => setPrefs)
	        def setPrefs {
	        	Prefs.set("ia.matlabPath", matlabPath.getText())
	        	Prefs.set("ia.MatrixY", matY.getText())
	        	Prefs.set("ia.MatrixX", matX.getText())
	        	Prefs.set("ia.dx", (dx.getText()).toInt)
	        	Prefs.set("ia.dy", (dy.getText()).toInt)
	        	Prefs.set("ia.dz", (dz.getText()).toInt)
	        }
		 }
	 
		 def aboutTab {
			val aboutTab = new JPanel();
	        aboutTab.setPreferredSize(new Dimension(380, 200));
			val aboutsubTab1=new JPanel();
	        aboutsubTab1.setPreferredSize(new Dimension(380, 145));
	        val aboutTxt = new JTextArea("Please refer to and cite:\n Helmuth colocalization  .\n\n\nFreely downloadable from:\nhttp://www.biomedcentral.com/1471-2105/11/372");
	        aboutTxt.setSize(380,155);
	        aboutTxt.setLineWrap(true);
	        aboutTxt.setEditable(false);
	        aboutTxt.setBackground(frame.getBackground());
	        aboutsubTab1.add(aboutTxt);
	        aboutTab.add(aboutsubTab1);
	        
	        val aboutsubTab2=new JPanel();
	        aboutsubTab2.setPreferredSize(new Dimension(380, 45));
	        linkPDF=new JButton("Click here to download the pdf");
	        linkPDF.addActionListener(this);
	        aboutsubTab2.add(linkPDF);
	        aboutTab.add(aboutsubTab2);
	        
	        tabs.add("About", aboutTab)
		 }
		 
		 def potentialTab {
			 val potentialTab = new JPanel(new GridLayout(1,2))
			 potentialTab.setPreferredSize(new Dimension(380, 200));
	
			 potentialTab.add(new JLabel("Potential shape"))
			 val scalaCB: ComboBox[Potential] = new ComboBox(PotentialFunctions.potentials) {
				 renderer = swing.ListView.Renderer(_.name) // The renderer is just the name field of the Potential class.
			 }
		     scalaCB.peer.addActionListener((e:ActionEvent) => model.potentialShape = scalaCB.selection.item)
		     potentialTab.add(scalaCB.peer)
	// scala swing style seems not to work, probably problem with event types (scala and java mixed)
	//	     scalaCB.listenTo(scalaCB)
	//	     scalaCB.reactions += {
	//	            case scala.swing.event.SelectionChanged(`scalaCB`) => {
	//	            	model.potentialShape = scalaCB.selection.item
	//	            }
	//	     }
			 tabs.add("Potential", potentialTab)
		 }
		 
		 // Chromatic aberration tab
		 def chromaticAberTab {
			val chroTab = new JPanel(new GridLayout(2,1));
	        chroTab.setPreferredSize(new Dimension(380, 200));
	
	        chroTab.add(new JLabel("Chromatic aberration"));
	
	        val xIntecept = new JTextField("0.0");
	        xIntecept.addActionListener((e:ActionEvent) => Prefs.set("ia.xIntercept", (xIntecept.getText()).toDouble))
	        val yIntecept = new JTextField("0.0");
	        yIntecept.addActionListener((e:ActionEvent) => Prefs.set("ia.yIntecept", (yIntecept.getText()).toDouble))
	        val xSlope = new JTextField("1.0");
	        xSlope.addActionListener((e:ActionEvent) => Prefs.set("ia.xSlope", (xSlope.getText()).toDouble))
	        val ySlope = new JTextField("1.0");
	        ySlope.addActionListener((e:ActionEvent) => Prefs.set("ia.ySlope", (ySlope.getText()).toDouble))
	
	        val chroValTab = new JPanel(new GridLayout(3,3));
	        
	        chroValTab.add(new JLabel("Coord."));
	        chroValTab.add(new JLabel("intercept:"));
	        chroValTab.add(new JLabel("slope:"));
	        chroValTab.add(new JLabel("X"));
	        chroValTab.add(xIntecept)
	        chroValTab.add(xSlope)
	        chroValTab.add(new JLabel("Y"));
	        chroValTab.add(yIntecept)
	        chroValTab.add(ySlope)
	        chroTab.add(chroValTab)
	        
	        tabs.add("Chromatic aberration", chroTab)
		 }
	 
	 }
	 
	 def actionPerformed(e: ActionEvent) {
        val origin=e.getSource();
        
        //Modifiers: none=16, shift=17, ctrl=18, alt=24
        if (origin==linkPDF){
            BrowserLauncher.openURL("http://www.biomedcentral.com/content/pdf/1471-2105-11-372.pdf")
        }
        
        
        if (origin==analyze) {
        	calculateDistances
        	analyzeDistances
        }
    }
	 
	def getImgList: List[ImagePlus] = {
    		var imgList:List[ImagePlus] = Nil
		if (WindowManager.getImageCount()==0){
        	allocateTwoImages()
        }        
        
        if (WindowManager.getImageCount()!=0){
            val IDList=WindowManager.getIDList();
            for (val i <- (0 until IDList.length)){
                val currImg=WindowManager.getImage(IDList(i));
                if (currImg.getBitDepth()!=24 && currImg.getBitDepth()!=32){
                    imgList = currImg::imgList
                }
            }
        }
        imgList
	}
	 
	def updateImgList(img: ImagePlus){
		// changes
    	imgAS.peer.removeAllItems()
    	imgBS.peer.removeAllItems()
    	val imgList = getImgList
        imgList.map(imgAS.peer.addItem(_))
        imgList.map(imgBS.peer.addItem(_))
        
        var nbImg= imgList.size
        
        if (nbImg<2){
            tabs.setSelectedIndex(0);
            tabs.setEnabled(false);
            analyze.setEnabled(false);
            warning.setText("At least 2 images should be opened to run interaction analysis");
        }else{
            tabs.setEnabled(true);
            analyze.setEnabled(true);
            warning.setText(warText); 
            
            if (imp(0) == null)
            	imp(0) = imgList(0)
            	imgAS.selection.item = imp(0)
            if (imp(1) == null)
            	imp(1) = imgList(1)
            	imgBS.selection.item = imp(1)

        }
    }
		 
	def imageOpened(imp: ImagePlus){
        updateImgList(imp)
    }
    
    def imageClosed(imp: ImagePlus){
        updateImgList(imp)
    }
    
    def imageUpdated(imp: ImagePlus){}
    
    
    def calculateDistances {
    
    	val (domainSize,isInDomain,refGroup, testGroup) = inputSource match {
				case 0 => generateModelInputFromImages
				case 1 => MatlabData.readModelInput
				case 2 => InteractionModelTest.readMatlabData
		}
    	if (refGroup != null) {
    		    	
			println("Image size " + domainSize(0) + "," + domainSize(1) + "," + domainSize(2) +  ", Sizes of refGroup, testGroup: " + refGroup.size + "," + testGroup.size)
	//		no images below here
			
			val refGroupInDomain = refGroup.filter(isInDomain)
			val testGroupInDomain = testGroup.filter(isInDomain)
			println("In domain: Sizes of refGroupInDomain, testGroupInDomain: " + refGroupInDomain.size + "," + testGroupInDomain.size)
	
	// nearest neighbor search(2x)
			model.initNearestNeighbour(refGroupInDomain)
			model.qOfd = model.calculateQofD(model.meshInCell(domainSize, isInDomain), model.getDistances)
			model.D = model.findD(testGroupInDomain)
			model.pOfD = model.estimateDensity(model.D)
    	}
    }
	
	def analyzeDistances {
	//		nll optimization CMA
			val fitfun = new LikelihoodOptimizer(new DenseVector(model.qOfd._1), new DenseVector(model.qOfd._2),new DenseVector(model.D), model.potentialShape.function);
			(model.potentialShape.nbrParam,model.potentialShape.nonParamFlag) match { case (nbr,flag) => fitfun.nbrParameter = nbr;fitfun.nonParametric = flag }
			val estimatedPotentialParameter = model.potentialParamEst(fitfun)
			
	//		hypothesis testing
	//		Monte Carlo sample Tk with size K from the null distribution of T obtained by sampling N distances di from q(d)
	//		additional Monte Carlo sample Uk to rank U
			val qD = new QDistribution(new DenseVector(model.qOfd._1), new DenseVector(model.qOfd._2))
			//val pD = new QDistribution(new DenseVector(model.pOfD._1), new DenseVector(model.pOfD._2))
			//val samp = new DenseVector(qD.sample(1000).toArray)
			//hist(samp,100)
			HypothesisTesting.N = model.D.size
			HypothesisTesting.f = model.potentialShape.function(_,PotentialFunctions.defaultParameters(estimatedPotentialParameter).tail)
			val D = new DenseVector(model.D)
			HypothesisTesting.testHypothesis(qD, D)
			HypothesisTesting.testNonParamHypothesis(qD, D)
	}
}