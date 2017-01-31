package mosaic.region_competition.DRS;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.Test;


public class RngTest {

    @Test
    public void testRng() {
        // Compare with itk MersenneTwister output (expected values taken from c++ impl)
        Rng rng = new Rng();
        assertEquals(rng.GetIntegerVariate(10), 6);
        assertEquals(rng.GetUniformVariate(2, 5), 4.71738, 0.00001);
        assertEquals(rng.GetVariate(), 0.835009, 0.00001);
        
        // Default value of seed should be 5489 (as in standard boost / c++ impl).
        rng = new Rng(5489);
        assertEquals(rng.GetIntegerVariate(10), 6);
        assertEquals(rng.GetUniformVariate(2, 5), 4.71738, 0.00001);
        assertEquals(rng.GetVariate(), 0.835009, 0.00001);
    }
    
    static public void superMethod(Object o) {
        mosaic.utils.Debug.print("EL", o);
    }
    
    @Test
    public void testMap() {
        ConcurrentHashMap<String, Integer> m = new ConcurrentHashMap<>();
        Map<String, Integer> mm = new HashMap<String, Integer>();
        final int num = 5;
        for (int i = 0; i < num; ++i) {
            m.put("a" + i, 2*i);
            mm.put("a" + i, 2*i);
        }
        
        List<String> sl = new ArrayList<>();
        mm.forEach( (k, v) -> System.out.println("Key:" + k + " Val: " + v));
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        numbers.forEach(System.out::println);
        numbers.forEach(RngTest::superMethod);
        System.out.println(numbers.stream().filter(w -> w > 3).mapToInt(Integer::intValue).min().getAsInt());
        Map<Integer, String> all = mm.entrySet().stream().filter(e -> e.getValue() > 4).collect(Collectors.toMap(e -> e.getValue(), e-> e.getKey()));
        System.out.println("ALL->" + mm);
        System.out.println("ALL->" + all);
        return;
    }
}
