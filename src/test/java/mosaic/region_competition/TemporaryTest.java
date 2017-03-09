package mosaic.region_competition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.StackStatistics;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.region_competition.DRS.Rng;
import mosaic.region_competition.DRS.SobelImg;
import mosaic.region_competition.DRS.SobelVolume;
import mosaic.test.framework.CommonBase;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Temporary file to keep some tests/examples of libraries used during DRS implementation.
 * TODO: This file is to be removed at the end of implementation. 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class TemporaryTest extends CommonBase {
    private static final Logger logger = Logger.getLogger(TemporaryTest.class);

    @Test
    @Ignore
    public void genSobel3DviaImglib2() {
        ImagePlus imp = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/tutorials/advanced-imglib2/images/t1-head.tif");
        imp.setStack(imp.getImageStack().convertToFloat());
        Img<FloatType> img3 = SobelImg.filter(ImageJFunctions.wrapFloat(imp), false);
        final ImagePlus ip = ImageJFunctions.wrap(img3, ""); 
        IJ.save(ip, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/HeadSobel6.tif");
    }

    @Test
    @Ignore
    public void genSobel3viaVolume() {
        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/tutorials/advanced-imglib2/images/t1-head.tif");
        img.setStack(img.getImageStack().convertToFloat());
        img.show();
        SobelVolume vn = new SobelVolume(img);
        vn.sobel3D();
        ImagePlus out = new ImagePlus("XXXX", vn.getImageStack());
        StackStatistics ss = new StackStatistics(out);
        out.setDisplayRange(ss.min, ss.max);
        out.show();
        IJ.save(out, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/HeadSobel.tif");
    }

    @Test
    @Ignore
    public void genSobel2D() throws InterruptedException {
        ImagePlus img = IJ.openImage("https://upload.wikimedia.org/wikipedia/commons/3/3f/Bikesgray.jpg");
        img.setStack(img.getImageStack().convertToFloat());
        img.show();

        SobelVolume vn = new SobelVolume(img);
        vn.sobel2D();
        ImagePlus out = new ImagePlus("XXXX", vn.getImageStack());
        StackStatistics ss = new StackStatistics(out);
        out.setDisplayRange(ss.min,  ss.max);        
        out.show();

        Img<FloatType> img3 = SobelImg.filter(ImageJFunctions.wrapFloat(img), false);
        ImageJFunctions.wrap(img3, "imglib2 output").show();
        IJ.save(out, "/Users/gonciarz/Documents/MOSAIC/work/testInputs/BikesgraySobel.tif");
        Thread.sleep(15000);
    }

    @Test
    @Ignore
    public void test() {
        Rng rng = new Rng();
        for (int i = 0; i < 3; ++i) {
            System.out.println(rng.GetIntegerVariate(10));
            System.out.println(rng.GetUniformVariate(2, 5));
            System.out.println(rng.GetVariate());
        }

        List<Pair<Integer, Double>> pmf = new ArrayList<>();
        pmf.add(new Pair<Integer, Double>(0, 40.0));
        pmf.add(new Pair<Integer, Double>(3, 10.0));
        pmf.add(new Pair<Integer, Double>(2, 10.0));
        pmf.add(new Pair<Integer, Double>(1, 40.0));
        EnumeratedDistribution<Integer> drng = new EnumeratedDistribution<>(new Rng(), pmf);
        for (int i = 0; i < 5; ++i) {
            double x = drng.sample(); 
            System.out.println(x);
        }
        mosaic.utils.Debug.print(drng.getPmf()); 
    }

    @Test
    @Ignore
    public void testDistributionFromSobel() throws InterruptedException {
        ImagePlus img = IJ.openImage("/Users/gonciarz/Documents/MOSAIC/work/testInputs/BikesgraySobel.tif");

        EnumeratedDistribution<Integer> drng = generateImgDistribution(img);
        generateProbabilityPlot(drng, 512).show();

        Thread.sleep(15000);
    }

    /**
     * Generates enumerated distribution from provided image.
     */
    EnumeratedDistribution<Integer> generateImgDistribution(ImagePlus aImg) {
        int index = 0;
        List<Pair<Integer, Double>> pmf = new ArrayList<>();
        if (aImg.getNSlices() > 1) {
            ImageStack is = aImg.getImageStack();
            for (int i = 1; i < is.size(); i++) {
                float[] pixels = (float[])is.getPixels(i);
                for (int x = 0; x < pixels.length; x++) {
                    pmf.add(new Pair<Integer, Double>(index++, (double) pixels[x]));
                }
            }
        }
        else {
            float[] pixels = (float[])aImg.getProcessor().getPixels();
            for (int x = 0; x < pixels.length; x++) {
                pmf.add(new Pair<Integer, Double>(index++, (double) pixels[x]));
            }
        }
        return new EnumeratedDistribution<>(new Rng(), pmf);
    }

    /**
     * Generates plot from enumerated distribution by putting all elements into bins. All probabilities
     * for given bin are summed.
     */
    Plot generateProbabilityPlot(EnumeratedDistribution<Integer> aDistribution, int aNumOfBins) {
        List<Pair<Integer, Double>> pmf = aDistribution.getPmf();

        // Find min/max for calculating bin width
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < pmf.size(); ++i) {
            Pair<Integer, Double> p = pmf.get(i);
            double v = p.getFirst();
            if (v > max) max = v;
            if (v < min) min = v;
        }

        // Create data for plot
        double binWidth = (max - min) / aNumOfBins;
        double sumOfProbs = 0.0;
        double[] xv = new double[aNumOfBins];
        double[] yv = new double[aNumOfBins];
        for (int i = 0; i < aNumOfBins; ++i) xv[i] = min + i * binWidth;
        for (int i = 0; i < pmf.size(); ++i) {
            Pair<Integer, Double> p = pmf.get(i);
            int idx = (int)((aNumOfBins-1) * ((double)p.getFirst() - min) / (max - min));
            yv[idx] += p.getSecond();
            sumOfProbs += p.getSecond();
        }
        logger.debug("Min: " + min + " Max: " + max + " NumOfBins: " + aNumOfBins + " Bin width: " + binWidth + " SumOfProbs: " + sumOfProbs);
        
        return new Plot("Probability distribution", "Values", "Prob", xv, yv);
    }
    
    public class Bin {
        private final int iCapacity;
        private int iSum = 0;
        private List<Integer> iNumbers = new ArrayList<Integer>();
        
        Bin(int aCapacity) {iCapacity = aCapacity;}
        
        Bin add(int aElement) {
            if (iSum + aElement <= iCapacity) {
                iNumbers.add(aElement); 
                iSum += aElement; 
                return this;
            }
            throw new RuntimeException("Capacity! " + aElement);
        }
        List<Integer> getElements() { return iNumbers; }
        int getCapacity() { return iCapacity; }
        int getUsedCapacity() { return iSum; }
        int getRemainingCapacity() { return iCapacity - iSum; }
        
        @Override
        public String toString() { return "Bin: " + iNumbers; }
    }
    
    List<Integer> getNumsFromBins(List<Bin> aBins) {
        List<Integer> nums = new ArrayList<>();
        for (Bin b : aBins) nums.addAll(b.getElements());
        Collections.sort(nums);
        Collections.reverse(nums);
        return nums;
    }
    
    public List<Bin> generateSolutions(int aBinCapacity, int maxVal, int aNumOfBins) {
        final Random rnd = new Random();
        List<Bin> bins = new ArrayList<>(aNumOfBins);
        for (int b = 0; b < aNumOfBins; ++b) {
            Bin bin = new Bin(aBinCapacity);
            int sum = 0;
            while (sum < aBinCapacity) {
                int range = maxVal <= (aBinCapacity - sum) ? maxVal : aBinCapacity - sum;
                int e = rnd.nextInt(range) + 1;
                sum += e;
                bin.add(e);
            }
            bins.add(bin);
        }
        return bins;
    }

    List<Bin> bestFitDecreasing(int aNumbers[], int aCapacity){
        List<Bin> bins = new ArrayList<>();

        for (int n : aNumbers) {
            // Search bin with minimum remaining capacity but still able to hold new element
            Bin bin = null;
            int min = aCapacity;
            for (Bin b : bins) {
                int rc = b.getRemainingCapacity();
                if (rc >= n && rc < min) {
                    bin = b;
                    min = rc;
                    if (rc == n) break;
                }
            }
            // If not proper bin found -> create new
            if (bin == null) {
                bin = new Bin(aCapacity);
                bins.add(bin);
            }
            
            bin.add(n);
        }
        return bins;
    }
    
    @Test
    @Ignore
    public void packStuff() {
        int capa = 37;
        int num = 7;
        int iters = 1;
//        List<List<Integer>> err = new ArrayList<>();
        for (int it = 0; it < iters; ++it) {
            List<Bin> bins = generateSolutions(capa, capa/3, num);
            List<Integer> nums = getNumsFromBins(bins);
            int a[] = new int[nums.size()];
            int n = 0; for (int i : nums) a[n++] = i;
//            a = new int [] {80, 15, 14, 5, 4, 1,};
//            capa = 100;
            List<Bin> result = bestFitDecreasing(a, capa);
//            a = new int[] { 4, 4, 4, 3, 2, 2, 2, 1}; capa = 12;
//            a = new int[] {3, 2, 1, 1, 1, 1, 1, 1}; capa = 9;
//            a = new int[] {3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1}; capa = 9;
            a = new int[] {3, 2, 2, 1, 1}; capa = 8;
//            a = new int[] {3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}; capa = 9;
//            a = new int[] {5, 4, 3, 3, 3, 2}; capa = 14;
            fitStuff(a, capa);
            System.out.println("BINS: " + result.size() + " " + nums.size() + " EL: " + nums + " " + result);
            //a = new int [] {100, 98, 96, 93, 91, 87, 81, 59, 58, 55, 50, 43, 22, 21, 20, 15, 14, 10, 8, 6, 5, 4, 3, 1, 0};
            //capa = 100;
            
//            if (result.size() != num) {
//                if (err.contains(nums)) continue;
//                err.add(nums);
//                System.out.println("ERRROR!!!!!!!!!");
//                System.out.println(bins);
//                System.out.println(nums);
//                System.out.println("BINS: " + result.size() + " EL: " + a.length + " " + result);
////                break;
//            }
        }
    }
    
    List<Bin> fitStuff(int aNumbers[], int aCapacity){
        List<Bin> bins = new ArrayList<>();

        List<List<Integer>> sets = new ArrayList<>();
        Set<List<Integer>> subsol = new HashSet<>();
        
//        findAll(aNumbers, 1, aNumbers.length - 1, aNumbers[0], aCapacity, "", sets, new ArrayList<>());
//        System.out.println("SETS1: " + sets.size() + " " + sets);
//        System.out.println();
//        sets.clear();
////        
//        findMaxDescend(aNumbers, 1, aNumbers.length - 1, aNumbers[0], aCapacity, "", sets, new ArrayList<>());
//        System.out.println("SETS2: " + sets.size() + " " + sets);
//        System.out.println();
//        sets.clear();
        
        findOpt(aNumbers, 1, aNumbers[0], aCapacity, "", sets, new ArrayList<>(), subsol);
        System.out.println("SETS3: " + sets.size() + " " + sets);
        System.out.println("SUBS:  " + subsol.size() + " " + subsol);
        sets.clear();
        subsol.clear();
        System.out.println();
        
        find(aNumbers, 1, aNumbers[0], aCapacity, "", sets, new ArrayList<>(), subsol);
        System.out.println("SETS4: " + sets.size() + " " + sets);
        System.out.println("SUBS:  " +subsol.size() + " " + subsol);
        System.out.println();
        
        
        return bins;
    }
    
    void findOpt(int aNumbers[], int idx, int sum, int cap, String str, List<List<Integer>> sets, List<Integer> oneSol, Set<List<Integer>> subsol) {
        List<Integer> letsRock = new ArrayList<Integer>();
        int cnt = 1;
        int prev = aNumbers[0];
        for (int i = 1; i < aNumbers.length; ++i) {
            if (aNumbers[i] == prev) {
                ++cnt;
            }
            else {
                letsRock.add(cnt);
                letsRock.add(prev);
                cnt = 1;
                prev = aNumbers[i];
            }
        }
        letsRock.add(cnt);
        letsRock.add(prev);
        
        int nums[] = new int[letsRock.size()/2];
        int cardinality[] = new int[letsRock.size()/2];
        
        for (int i = 0; i < letsRock.size()/2; ++i) {
            cardinality[i] = letsRock.get(i*2);
            nums[i] = letsRock.get(i*2 + 1);
        }
        
        System.out.println(letsRock);
        mosaic.utils.Debug.print(nums);
        mosaic.utils.Debug.print(cardinality);
        findOpt3(nums, cardinality, idx, sum, cap, str, sets, oneSol, subsol);
    }
    
    void findOpt3(int aNumbers[], int aCard[], int idx, int sum, int cap, String str, List<List<Integer>> sets, List<Integer> oneSol, Set<List<Integer>> subsol) {
        boolean wentDeeper = false;
        int myInd = 0;
        for (int i = 0; i < aNumbers.length; ++i) {
            if (str.equals("")) System.out.println("--- " + i + "/" + aNumbers.length);
            int added = aCard[i];
            for (int j = aCard[i]; j >= 0; --j) {
                myInd++;
                System.out.println(str + i + "   "+ added + " of " + aNumbers[i] + "  idx="+idx + "    " + myInd) ;
                if (myInd <= idx) continue;
                if (added * aNumbers[i] <= cap - sum) {
                    wentDeeper = true;
                    int nextIdx = 0; for (int k = 0; k <= i; ++k) nextIdx += aCard[k];
                    for (int k = 0; k < added; ++k) oneSol.add(aNumbers[i]);
                    findOpt3(aNumbers, aCard, nextIdx, sum + added * aNumbers[i], cap, str + "     ", sets, oneSol, subsol);
                    for (int k = 0; k < added - 1; ++k) oneSol.remove(oneSol.size() - 1);
                }
                added--;
            }
        }
        if (!wentDeeper) {
//            System.out.println(oneSol);
            if (!subsol.contains(oneSol)) { 
                sets.add(new ArrayList<Integer>(oneSol));
                for (int i = 0; i <= oneSol.size() - 1; ++i) {
                    ArrayList<Integer> sublist = new ArrayList<>( oneSol.subList(i, oneSol.size()) );
                    if (!subsol.add(sublist)) break;
                }
              for (int i = 0; i <= oneSol.size() - 1; ++i) {
                  ArrayList<Integer> sublist = new ArrayList<>( oneSol.subList(0, i + 1) );
                  subsol.add(sublist);
//                  if (!subsol.add(sublist)) break;
              }
            }
        }
    }
    
    void findOpt2(int aNumbers[], int aCard[], int idx, int sum, int cap, String str, List<List<Integer>> sets, List<Integer> oneSol, Set<List<Integer>> subsol) {
        boolean wentDeeper = false;
        int myInd = 0;
        for (int i = 0; i < aNumbers.length; ++i) {
            if (str.equals("")) System.out.println("--- " + i + "/" + aNumbers.length);
            int added = 1;
            for (int j = 0; j < aCard[i]; ++j) {
                myInd++;
//                System.out.println(str + i + "   "+ j + " of " + aNumbers[i] + "  idx="+idx + "    " + myInd) ;
                if (myInd <= idx) continue;
                if (added * aNumbers[i] <= cap - sum) {
                    oneSol.add(aNumbers[i]);
                    wentDeeper = true;
                    int nextIdx = 0; for (int k = 0; k <= i; ++k) nextIdx += aCard[k];
//                    System.out.println(nextIdx);
                    findOpt2(aNumbers, aCard, nextIdx, sum + added * aNumbers[i], cap, str + "     ", sets, oneSol, subsol);
                    added++;
                }
            }
            for (int k = 0; k < added - 1; ++k) {oneSol.remove(oneSol.size() - 1);}
        }
        if (!wentDeeper) {
            if (!subsol.contains(oneSol)) { 
                sets.add(new ArrayList<Integer>(oneSol));
                for (int i = 0; i <= oneSol.size() - 1; ++i) {
                    ArrayList<Integer> sublist = new ArrayList<>( oneSol.subList(i, oneSol.size()) );
                    if (!subsol.add(sublist)) break;
                }
              for (int i = 0; i <= oneSol.size() - 1; ++i) {
                  ArrayList<Integer> sublist = new ArrayList<>( oneSol.subList(0, i + 1) );
                  subsol.add(sublist);
//                  if (!subsol.add(sublist)) break;
              }
            }
        }
    }
    
    void find(int aNumbers[], int idx, int sum, int cap, String str, List<List<Integer>> sets, List<Integer> oneSol, Set<List<Integer>> subsol) {
        boolean wentDeeper = false;
        for (int i = idx; i < aNumbers.length; ++i) {
            if (str.equals("")) System.out.println("--- " + i + "/" + aNumbers.length);
            if (aNumbers[i] <= cap - sum) {
                oneSol.add(aNumbers[i]);
                wentDeeper = true;
                find(aNumbers, i + 1, sum + aNumbers[i], cap, str + "     ", sets, oneSol, subsol);
                oneSol.remove(oneSol.size() - 1);
            }
        }
        if (!wentDeeper) {
            if (!subsol.contains(oneSol)) {
//                System.out.println(oneSol);
                sets.add(new ArrayList<Integer>(oneSol));
                for (int i = 0; i <= oneSol.size() - 1; ++i) {
                    ArrayList<Integer> sublist = new ArrayList<>( oneSol.subList(i, oneSol.size()) );
                    if (!subsol.add(sublist)) break;
                }
//                for (int i = 0; i <= oneSol.size() - 1; ++i) {
//                    ArrayList<Integer> sublist = new ArrayList<>( oneSol.subList(0, i + 1) );
//                    subsol.add(sublist);
////                    if (!subsol.add(sublist)) break;
//                }
            }
        }
    }
    
    void findMaxDescend(int aNumbers[], int idx, int idx2, int sum, int cap, String str, List<List<Integer>> sets, List<Integer> oneSol) {
        boolean wentDeeper = false;
        for (int i = idx; i <= idx2; ++i) {
            if (aNumbers[i] <= cap - sum) {
                oneSol.add(aNumbers[i]);
                wentDeeper = true;
                findMaxDescend(aNumbers, i + 1, idx2, sum + aNumbers[i], cap, str + "     ", sets, oneSol);
                oneSol.remove(oneSol.size() - 1);
            }
        }
        if (!wentDeeper) {
            sets.add(new ArrayList<Integer>(oneSol));
        }
    }
    
    void findAll(int aNumbers[], int idx, int idx2, int sum, int cap, String str, List<List<Integer>> sets, List<Integer> oneSol) {
        for (int i = idx; i <= idx2; ++i) {
            if (aNumbers[i] <= cap - sum) {
                oneSol.add(aNumbers[i]);
                sets.add(new ArrayList<Integer>(oneSol));
                findAll(aNumbers, i + 1, idx2, sum + aNumbers[i], cap, str + "     ", sets, oneSol);
                oneSol.remove(oneSol.size() - 1);
            }
            else {
            }
        }
    }
    
    @Test
    @Ignore
    public void testLabelImg() {
        String fileName1 = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/3balls3d.tif";
        ImagePlus img = IJ.openImage(fileName1);
        LabelImage li1 = new LabelImage(img);
        li1.connectedComponents();
        ImagePlus show = li1.show("LI1"); show.setSlice(9);
        
        mosaic.utils.Debug.print(show.getDisplayRangeMin(), show.getDisplayRangeMax(), show.getDisplayMode());
        
        IJ.run(show, "Set... ", "zoom=300");
        show.setSlice(8);
        String fileName2 = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/3objects.tif";
        fileName2 = "/Users/gonciarz/Documents/MOSAIC/work/testInputs/BikesgraySobel.tif";
        img = IJ.openImage(fileName2);
        IntensityImage li2 = new IntensityImage(img);
        li2.show("LI2");
        
        
        sleep(13000);
    }
    
// Recursive all solutions in python:
//    
//    boxSizes, itemSizes = [5, 3, 6], [1, 2, 2, 3, 5]
//
//            def recurse(boxes, itemIndex, solution, itemsUsed):
//                global itemSizes
//                if itemsUsed == len(itemSizes):
//                    print solution
//                    return
//                for i in range(len(boxes)):
//                    for j in range(itemIndex, len(itemSizes)):
//                        if boxes[i] - itemSizes[j] >= 0:
//                            boxes[i] -= itemSizes[j]
//                            solution[i].append(itemSizes[j])
//                            recurse(boxes, j + 1, solution, itemsUsed + 1)
//                            solution[i].pop()
//                            boxes[i] += itemSizes[j]
//
//            recurse(boxSizes, 0, [[] for i in range(len(boxSizes))], 0)
}
