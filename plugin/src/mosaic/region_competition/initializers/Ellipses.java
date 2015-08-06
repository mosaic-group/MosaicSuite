package mosaic.region_competition.initializers;

// from: LabelImage

//import ij.gui.OvalRoi;
//import ij.gui.Roi;
//import ij.process.ImageProcessor;
//
//import java.util.Random;
//import java.util.Scanner;
//
//
//	
//	/**
//	 * initial guess by generating random ellipses (may overlap)
//	 */
//	public void initialGuessRandom()
//	{
//		Random rand = new Random();
//		
//		int maxNum=5;
//		int n=1+rand.nextInt(maxNum);
//		
//		int ellipses[][]= new int[n][5];
//		
//		System.out.println("generating "+n+" random ellipses: ");
//		System.out.println("x, y, w, h, label");
//		System.out.println(n);
//		
//		for (int i=0; i<n; i++)
//		{
//			int min = 10;
//			int w=min+rand.nextInt(width/2-min);
//			int h=min+rand.nextInt(height/2-min);
//			
//			int x = w+rand.nextInt(width-2*w);
//			int y = h+rand.nextInt(height-2*h);
//			
//			int label=i+1;
////			int label=i+labelDispenser.getNewLabel();
//			
//			ellipses[i][0]=x;
//			ellipses[i][1]=y;
//			ellipses[i][2]=w;
//			ellipses[i][3]=h;
//			ellipses[i][4]=label;
//			
////			Roi roi = new OvalRoi(x, y, w, h);
////			labelIP.setValue(label);
////			labelIP.fill(roi);
//			
//			System.out.println(x+" "+y+" "+w+" "+h+" "+label+" ");
//		}
//		
//		initialGuessEllipses(ellipses);
//	}
//
//	/**
//	 * Only 2D <br>
//	 * creates an initial guess from an array of ellipses. 
//	 * @param ellipses array of ellipses
//	 */
//	public void initialGuessEllipses(int ellipses[][])
//	{
//		ImageProcessor proc = labelPlus.getImageStack().getProcessor(1);
//		
//		int x, y, w, h;
//		int label;
//		int n = ellipses.length;
//
//		System.out.println(n);
//
//		for (int i = 0; i < n; i++) 
//		{
//			int e[] = ellipses[i];
//			x = e[0];
//			y = e[1];
//			w = e[2];
//			h = e[3];
//			label = e[4];
//
//			Roi roi = new OvalRoi(x, y, w, h);
//			proc.setValue(label);
//			proc.fill(roi);
//
//			System.out.println(x + " " + y + " " + w + " " + h + " " + label + " ");
//		}
//	}
//	
//	
//	/**
//	 * Debug function, to read in critical initial guesses (string output from random ellipses)
//	 */
//	public void initialGuessEllipsesFromString(String s)
//	{
//		Scanner scanner = new Scanner(s);
//		int n = scanner.nextInt();
//		
//		int ellipses[][] = new int[n][5]; //5=2*dim+1
//		
//		for (int i = 0; i < n; i++) 
//		{
//			int e[]=ellipses[i];
//			//coords
//			for (int j=0; j<dim; j++){
//				e[j]=scanner.nextInt();
//			}
//			//sizes
//			for (int j=dim; j<2*dim; j++){
//				e[j]=scanner.nextInt();
//			}
//			//label
//			e[2*dim]=scanner.nextInt();
//		}
//		initialGuessEllipses(ellipses);
//	}