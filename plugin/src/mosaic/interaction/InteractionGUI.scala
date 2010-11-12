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
import javax.swing.{JFrame, JPanel,JTabbedPane,JButton, JComboBox, JLabel, JTextArea, SwingConstants, DefaultComboBoxModel}
import swing._
import java.awt.{GridLayout, Dimension, Color}
import java.awt.event._
import ij.plugin.BrowserLauncher
import scalala.Scalala._


trait InteractionGUI extends ActionListener with ij.ImageListener with ImagePreparation  {
	
	 val model = new InteractionModel

	 var frame: JFrame = null
	 var superPanel,imgTab: JPanel = null
	 var tabs, analyzingTabs: JTabbedPane = null
	 var linkPDF, distCalc, paramPot, nonParamPot, nonParamTest: JButton = null
	 var imgAS, imgBS: ComboBox[ImagePlus] = null
	 var warning: JLabel = null
	 val warText = "Please check red labeled tabs before launching analysis"
	 var inputSource = 0
		
	// no ActionListener needed
	implicit def actionPerformedWrapper(func: (ActionEvent) => Unit) = 
    new ActionListener { def actionPerformed(e:ActionEvent) = func(e) }
	 
	 def GUI() {
		 // Input type selection dialog
		 val gd = new GenericDialog("Input type selection...", IJ.getInstance());
		 gd.addChoice("Input source:", Array("Images","Matlab File .mat", "Matlab Debug", "Debug"), "Images")
		 gd.addChoice("Dimensions:", Array("2D","3D"), "3D")
		 gd.showDialog()
		 inputSource = gd.getNextChoiceIndex
		 model.dim = gd.getNextChoiceIndex + 2
		 dim = model.dim
		 
		 
		 // Main GUI
		 frame = new JFrame("Co-Localization Interaction Plugin")
		 frame.setSize(400, 720)
		 frame.setResizable(true)
		 frame.setVisible(true)

		 superPanel = new JPanel()
		 frame.getContentPane().add(superPanel)
		 
		 // Initialization
		 tabs = new JTabbedPane(SwingConstants.TOP)
		 tabs.setOpaque(true)
		 aboutTab
		 
		 analyzingTabs = new JTabbedPane(SwingConstants.BOTTOM)
		 analyzingTabs.setOpaque(true)
		 analyzingTabs.setPreferredSize(new Dimension(400, 200));

		 warning=new JLabel(warText)
		 warning.setForeground(Color.RED);
		 distCalc=new JButton("Calculate Distances")
		 distCalc.addActionListener(this)
		 paramPot=new JButton("Estimate Parametric Potential and Test")
		 paramPot.addActionListener(this)
		 nonParamPot=new JButton("Estimate Non-Parametric Potential")
		 nonParamPot.addActionListener(this)
		 nonParamTest=new JButton("Test with Non-Parametric Statistic")
		 nonParamTest.addActionListener(this)


		 
		 
		 // TODO Remove debugging input
		 if (inputSource == 3) {
			 if (WindowManager.getIDList == null){
				 (new ij.plugin.Macro_Runner).run("JAR:macros/StacksOpen_.ijm") 
			 }
			 inputSource = 0
		 }

		 inputSource match {
			 case 1 => matlabInput
			 case 0 =>  {
				 //imgTab
				 imgTab = new JPanel(new GridLayout(2,2))
				 imgTab.setPreferredSize(new Dimension(380, 45));
				 superPanel.add(imgTab)
				 val imgList = getImgList
	
				 imgAS = getComboBox(imgList,0)
	//			 imgAS.peer.removeAllItems()
	//			 imgList.foreach(imgAS.peer.addItem(_))
				 
				 setImage(imgList(0),0)
				 imgTab.add(new JLabel("Image of reference group Y"));
				 imgTab.add(imgAS.peer)
	
				 imgBS = getComboBox(imgList,1)
				 setImage(imgList(1),1) 
				 imgTab.add(new JLabel("Image of group X"));
				 imgTab.add(imgBS.peer)
				 updateImgList(null)
				 
				 // add this, that it get informed by open/close/update changes of images.
				 //ImagePlus.addImageListener(this)
	
				 chromaticAberTab
				 maskTab
			 }
			 case _ =>
		 }
		superPanel.add(tabs)
		//superPanel.add(warning);
		superPanel.add(distCalc);

		analyzingGUI
		
		
	    
	    def matlabInput {
			 val matlabTab = new JPanel(new GridLayout(6,2))
			 matlabTab.setPreferredSize(new Dimension(380, 200));
			 superPanel.add(matlabTab)
			 
			// object positions
			val matlabPath = new JTextField("Path incl. name of .mat-file.");
	        val matY = new JTextField("Y");
	        val matX = new JTextField("X");
	
	        val chroValTab = new JPanel(new GridLayout(3,3));
	        
	        matlabTab.add(matlabPath)
	        matlabTab.add(new JLabel(""))
	        matlabTab.add(new JLabel("Reference Group Y:"));
	        matlabTab.add(new JLabel("Group X:"));
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
	        if (model.dim == 3) {
	        	matlabTab.add(new JLabel("Domain z"));
	        	matlabTab.add(dz)
	        }
	        
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
	        val aboutTxt = new JTextArea("Please refer to and cite:\n "+
	        			"Jo A. Helmuth, Beyond co-localization: inferring spatial \n "+
	        			"interactions between sub-cellular structures from microscopy images "+
	        			"\n\n\nFreely downloadable from:\nhttp://www.biomedcentral.com/1471-2105/11/372");
	        aboutTxt.setSize(380,155);
	        aboutTxt.setLineWrap(true);
	        aboutTxt.setEditable(false);
	        aboutTxt.setBackground(frame.getBackground());
	        aboutsubTab1.add(aboutTxt);
	        aboutTab.add(aboutsubTab1);
	        
	        val aboutsubTab2=new JPanel();
	        aboutsubTab2.setPreferredSize(new Dimension(380, 45));
	        linkPDF=new JButton("Download the paper as pdf");
	        linkPDF.addActionListener(this);
	        aboutsubTab2.add(linkPDF);
	        aboutTab.add(aboutsubTab2);
	        
	        tabs.add("About", aboutTab)
		 }

		 
		 // Chromatic aberration tab
		 def chromaticAberTab {
			val chroTab = new JPanel(new GridLayout(2,1));
	        chroTab.setPreferredSize(new Dimension(380, 200));
	
	        //chroTab.add(new JLabel("Chromatic aberration"));
	        val chromPlugin = new JButton("Find Chromatic aberration with plugin.")
	        chromPlugin.addActionListener((e:ActionEvent) => {val x = IJ.runPlugIn("mosaic.interaction.ChromaticAberration","openImages")})
	        chroTab.add(chromPlugin)
	        
	        val xIntecept = new JTextField("0.0");
	        xIntecept.addActionListener((e:ActionEvent) => Prefs.set("ia.xIntercept", (xIntecept.getText()).toDouble))
	        val yIntecept = new JTextField("0.0");
	        yIntecept.addActionListener((e:ActionEvent) => Prefs.set("ia.yIntecept", (yIntecept.getText()).toDouble))
	        val xSlope = new JTextField("1.0");
	        xSlope.addActionListener((e:ActionEvent) => Prefs.set("ia.xSlope", (xSlope.getText()).toDouble))
	        val ySlope = new JTextField("1.0");
	        ySlope.addActionListener((e:ActionEvent) => Prefs.set("ia.ySlope", (ySlope.getText()).toDouble))
	
	        val chroValTab = new JPanel(new GridLayout(3,3));
	        chroValTab.setPreferredSize(new Dimension(380, 150))
	        
	        chroValTab.add(new JLabel("Coord."));
	        chroValTab.add(new JLabel("intercept:"));
	        chroValTab.add(new JLabel("slope:"));
	        chroValTab.add(new JLabel("x axis (horizontal)"));
	        chroValTab.add(xIntecept)
	        chroValTab.add(xSlope)
	        chroValTab.add(new JLabel("y axis (vertical)"));
	        chroValTab.add(yIntecept)
	        chroValTab.add(ySlope)
	        chroTab.add(chroValTab)
	        
	        tabs.add("Chromatic aberration", chroTab)
		 }
		 
		 def maskTab {
			val maskT = new JPanel(new GridLayout(3,1));
	        maskT.setPreferredSize(new Dimension(380, 200));
	
	        val openMask = new JButton("Open an existing binary "+ model.dim +"D image as mask.")
	        openMask.addActionListener((e:ActionEvent) => {
	        	cellOutline = new input.CellOutline(openImage(model.dim), true)
	        })
	        maskT.add(openMask)
	        val generateMask = new JButton("Generate mask automaticly based on the reference image.")
	        generateMask.addActionListener((e:ActionEvent) => cellOutlineGeneration)
	        maskT.add(generateMask)
	        
	        tabs.add("Mask", maskT)
			 
		 }
	 }
	 
	 def analyzingGUI {
		 	analyzingTabs.setVisible(false) // setVisible(true), when q(d) and D is available
		    superPanel.add(analyzingTabs)
        	potentialTab
			nonParametricPotTab
			nonParamTestTab
		 		 
		 		 
		 def potentialTab {
			 val potentialTab = new JPanel()
			 potentialTab.setPreferredSize(new Dimension(400, 100));
	
			 val potentialPanel = new JPanel(new GridLayout(3,2))
			 potentialPanel.setPreferredSize(new Dimension(380, 100));

			 potentialPanel.add(new JLabel("Potential shape"))
			 val scalaCB: ComboBox[Potential] = new ComboBox(PotentialFunctions.parametricPotentials) {
				 renderer = swing.ListView.Renderer(_.name) // The renderer is just the name field of the Potential class.
			 }
		     scalaCB.peer.addActionListener((e:ActionEvent) => model.potentialShape = scalaCB.selection.item)
		     potentialPanel.add(scalaCB.peer)
	// scala swing style seems not to work, probably problem with event types (scala and java mixed)
	//	     scalaCB.listenTo(scalaCB)
	//	     scalaCB.reactions += {
	//	            case scala.swing.event.SelectionChanged(`scalaCB`) => {
	//	            	model.potentialShape = scalaCB.selection.item
	//	            }
	//	     }
		     potentialTab.add(potentialPanel)
		     
		     val mcSamples = new JTextField("1000");
	         val alpha = new JTextField("0.05");
		     
		     potentialPanel.add(new JLabel("Monte Carlo Samples"))
		     potentialPanel.add(mcSamples)
		     potentialPanel.add(new JLabel("Significance Level"))
		     potentialPanel.add(alpha)
		     def setPrefs {
	        	HypothesisTesting.K = (mcSamples.getText()).toInt//; Prefs.set("pPT.mcSamples", HypothesisTesting.K)
	        	HypothesisTesting.alpha = (alpha.getText()).toDouble//; Prefs.set("pPT.alpha", HypothesisTesting.alpha)
	        }
		     
		     
		     potentialTab.add(paramPot)
		     paramPot.addActionListener((e:ActionEvent) => {setPrefs;parametricPotentialEstimation})

			 analyzingTabs.add("Parametric Potential", potentialTab)
		 }
		 
		 def nonParametricPotTab {
			  val nonParPotTab = new JPanel()
			 nonParPotTab.setPreferredSize(new Dimension(400, 100));
	
			 val potentialPanel = new JPanel(new GridLayout(3,2))
			 potentialPanel.setPreferredSize(new Dimension(380, 100));
			 potentialPanel.add(new JLabel("Potential"))
			 val scalaCB: ComboBox[Potential] = new ComboBox(PotentialFunctions.nonParametricPotentials) {
				 renderer = swing.ListView.Renderer(_.name) // The renderer is just the name field of the Potential class.
			 }
		     scalaCB.peer.addActionListener((e:ActionEvent) => model.potentialShape = scalaCB.selection.item)
		     potentialPanel.add(scalaCB.peer)
			 
		     val smoothness = new JTextField("2");
		     val nbrParameter = new JTextField("10");
	        
		     potentialPanel.add(new JLabel("# Support Points"))
		     potentialPanel.add(nbrParameter)
		     potentialPanel.add(new JLabel("Smoothness"))
		     potentialPanel.add(smoothness)
		     
		     def setPrefs {
		    	 Prefs.set("nPP.smoothness",(smoothness.getText()).toDouble)
		    	 Prefs.set("nPP.nbrParameter", (nbrParameter.getText()).toInt)
	        }

		     nonParPotTab.add(potentialPanel)
		     

			 nonParPotTab.add(nonParamPot)
			 nonParamPot.addActionListener((e:ActionEvent) => {
				 setPrefs
				 nonParametricPotentialEstimation
			 })
			 analyzingTabs.add("Non-Parametric Potential", nonParPotTab)
		 }
		 
		 def nonParamTestTab {
			 val nonParTestTab = new JPanel(new GridLayout(4,2))
			 
			val bins = new JTextField("20");
	        val mcSamples = new JTextField("1000");
	        val alpha = new JTextField("0.05");
	        
	        nonParTestTab.add(new JLabel("Distance Count Bins"))
	        nonParTestTab.add(bins)
	        nonParTestTab.add(new JLabel("Monte Carlo Samples"))
	        nonParTestTab.add(mcSamples)
	        nonParTestTab.add(new JLabel("Significance Level"))
			nonParTestTab.add(alpha)
			 
			def setPrefs {
	        	HypothesisTesting.L = (bins.getText()).toInt//; Prefs.set("nPT.bins",HypothesisTesting.L)
	        	HypothesisTesting.K = (mcSamples.getText()).toInt//; Prefs.set("nPT.mcSamples", HypothesisTesting.K)
	        	HypothesisTesting.alpha = (alpha.getText()).toDouble//; Prefs.set("nPT.alpha", HypothesisTesting.alpha)
	        }
			 
			 nonParTestTab.add(nonParamTest)
			 nonParamTest.addActionListener((e:ActionEvent) => {
				 setPrefs
				 nonParametricTest})
			 analyzingTabs.add("Non-Parametric Test", nonParTestTab)
			 
		 }
		 
	 }
	 
	 def actionPerformed(e: ActionEvent) {
        val origin=e.getSource();
        
        //Modifiers: none=16, shift=17, ctrl=18, alt=24
        if (origin==linkPDF){
            BrowserLauncher.openURL("http://www.biomedcentral.com/content/pdf/1471-2105-11-372.pdf")
        }
        
        
        if (origin==distCalc) {
        	calculateDistances
        	
        	// analyzing GUI
        	analyzingTabs.setVisible(true)

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
        imgList.foreach(imgAS.peer.addItem(_))
        imgList.foreach(imgBS.peer.addItem(_))
        
        var nbImg= imgList.size
        
        if (nbImg<2){
            tabs.setSelectedIndex(0);
            tabs.setEnabled(false);
            distCalc.setEnabled(false);
            warning.setText("At least 2 images should be opened to run interaction analysis");
        }else{
            tabs.setEnabled(true);
            distCalc.setEnabled(true);
            warning.setText(warText); 
            
            if (imp(0) == null)
            	imp(0) = imgList(0)
            	imgAS.selection.item = imp(0)
            if (imp(1) == null)
            	imp(1) = imgList(1)
            	imgBS.selection.item = imp(1)

        }
    }
	
	def getComboBox(imgList: List[ImagePlus], i:Int): ComboBox[ImagePlus] = {
		 new ComboBox(imgList) {
				 renderer = swing.ListView.Renderer(_.getTitle) // The renderer is just the title field of the ImagePlus class.
				 peer.addActionListener((e:ActionEvent) => setImage(selection.item,i) )
				 peer.setModel(new DefaultComboBoxModel) // <- to be mutable
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
				case 2 => InteractionModelTest.readMatlabData // Debug input
		}
    	
    	//	no image data is used after point detection, so now images below here

    	if (refGroup != null) {
    		    	
			println("Image size " + domainSize(0) + "," + domainSize(1) + "," + domainSize(2) +  ", Sizes of refGroup, testGroup: " + refGroup.size + "," + testGroup.size)
			
			val refGroupInDomain = refGroup.filter(isInDomain)
			val testGroupInDomain = testGroup.filter(isInDomain)
			println("In domain: Sizes of refGroupInDomain, testGroupInDomain: " + refGroupInDomain.size + "," + testGroupInDomain.size)
		
			if (refGroupInDomain.size < 1 || testGroupInDomain.size < 10) {
				IJ.showMessage("Warning","Not enough points in domain: Reference Y, X: " + refGroupInDomain.size + "," + testGroupInDomain.size + ".\n Check mask, ROI and domain size.")
			}else {
				// (2x) nearest neighbor search, for q(d) and for D
				model.initNearestNeighbour(refGroupInDomain)
				model.qOfd = model.calculateQofD(model.meshInCell(domainSize, isInDomain), model.getDistances)
				model.D = model.findD(testGroupInDomain)
				// kernel density estimation for q(d)
				model.pOfD = model.estimateDensity(model.D)
    		}
		}
    	
    }
	
	def parametricPotentialEstimation {
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
			val testResult = HypothesisTesting.testHypothesis(qD, D)
			IJ.showMessage("Parametric Potential Test: Results", testResult._3)

	}
	
	def nonParametricPotentialEstimation{
			//		nll optimization CMA
			val fitfun = new LikelihoodOptimizer(new DenseVector(model.qOfd._1), new DenseVector(model.qOfd._2),new DenseVector(model.D), model.potentialShape.function);
			fitfun.nonParametricSmoothness = Prefs.get("nPP.smoothness",2)
			fitfun.nbrParameter = Prefs.get("nPP.nbrParameter",5).toInt
			fitfun.nonParametric = true
			val estimatedPotentialParameter = model.potentialParamEst(fitfun)
	}
	
	def nonParametricTest {
		val qD = new QDistribution(new DenseVector(model.qOfd._1), new DenseVector(model.qOfd._2))
		HypothesisTesting.N = model.D.size
		HypothesisTesting.maxdInDomain = model.qOfd._2.last // greatest distance for which q(d) > 0
		val D = new DenseVector(model.D)
		val testResult = HypothesisTesting.testNonParamHypothesis(qD, D)
		IJ.showMessage("Non-Parametric Test: Results", testResult._3)
	}
}