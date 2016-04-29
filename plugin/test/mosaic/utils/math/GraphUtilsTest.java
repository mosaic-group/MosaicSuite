package mosaic.utils.math;

import static org.junit.Assert.*;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Test;

import mosaic.utils.math.GraphUtils.IntVertex;
import mosaic.utils.math.GraphUtils.StrVertex;


public class GraphUtilsTest {

    @Test
    public void testSimplifySimipleUndirectedGraphTree() {
        int num = 6;
        StrVertex[] v = new StrVertex[num];
        for (int i = 0; i < v.length; i++) v[i] = new StrVertex(new String(new char[]{(char) ('A' + i)}));
        
        // A --- B --- C --- D --- E
        //                   |
        //                   F
        UndirectedGraph<Object, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        for (Object cv : v) graph.addVertex(cv);
        graph.addEdge(v[0],v[1]);
        graph.addEdge(v[1],v[2]);
        graph.addEdge(v[2],v[3]);
        graph.addEdge(v[3],v[4]);
        graph.addEdge(v[5],v[3]);
        
        WeightedGraph<Object, DefaultWeightedEdge> simpleGraph = GraphUtils.simplifySimipleUndirectedGraph(graph);
        
        // Expected (B,C removed):
        // A --- D --- E
        //       |
        //       F
        
        // Check if we have all edges/vertices as expected
        assertEquals(3, simpleGraph.edgeSet().size());
        assertEquals(4, simpleGraph.vertexSet().size());
        assertTrue(simpleGraph.vertexSet().contains(v[0]));
        assertTrue(simpleGraph.vertexSet().contains(v[3]));
        assertTrue(simpleGraph.vertexSet().contains(v[4]));
        assertTrue(simpleGraph.vertexSet().contains(v[5]));
        
        // Check if weight of edges are updated properly
        assertNotNull(simpleGraph.getEdge(v[0], v[3]));
        assertEquals(3, simpleGraph.getEdgeWeight(simpleGraph.getEdge(v[0], v[3])), 0.0);
        assertNotNull(simpleGraph.getEdge(v[3], v[4]));
        assertEquals(1, simpleGraph.getEdgeWeight(simpleGraph.getEdge(v[3], v[4])), 0.0);
        assertNotNull(simpleGraph.getEdge(v[3], v[5]));
        assertEquals(1, simpleGraph.getEdgeWeight(simpleGraph.getEdge(v[3], v[5])), 0.0);
    }
    
    @Test
    public void testSimplifySimipleUndirectedGraphLoop() {
        int num = 8;
        StrVertex[] v = new StrVertex[num];
        for (int i = 0; i < v.length; i++) v[i] = new StrVertex(new String(new char[]{(char) ('A' + i)}));
        
        // A --- B --- C --- D --- E \
        //                   |         H
        //                   F --- G /
        UndirectedGraph<Object, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        for (Object cv : v) graph.addVertex(cv);
        graph.addEdge(v[0],v[1]);
        graph.addEdge(v[1],v[2]);
        graph.addEdge(v[2],v[3]);
        graph.addEdge(v[3],v[4]);
        graph.addEdge(v[5],v[3]);
        graph.addEdge(v[5],v[6]);
        graph.addEdge(v[6],v[7]);
        graph.addEdge(v[7],v[4]);
        
        WeightedGraph<Object, DefaultWeightedEdge> simpleGraph = GraphUtils.simplifySimipleUndirectedGraph(graph);
        
        // Expected (B,C removed):
        // A --- D --- G
        //        \   /
        //          H
        // Check if we have all edges/vertices as expected
        assertEquals(4, simpleGraph.edgeSet().size());
        assertEquals(4, simpleGraph.vertexSet().size());
        assertTrue(simpleGraph.vertexSet().contains(v[0]));
        assertTrue(simpleGraph.vertexSet().contains(v[3]));
        assertTrue(simpleGraph.vertexSet().contains(v[6]));
        assertTrue(simpleGraph.vertexSet().contains(v[7]));
        
        // Check if weight of edges are updated properly
        assertNotNull(simpleGraph.getEdge(v[0], v[3]));
        assertEquals(3, simpleGraph.getEdgeWeight(simpleGraph.getEdge(v[0], v[3])), 0.0);
        assertNotNull(simpleGraph.getEdge(v[3], v[6]));
        assertEquals(2, simpleGraph.getEdgeWeight(simpleGraph.getEdge(v[3], v[6])), 0.0);
        assertNotNull(simpleGraph.getEdge(v[6], v[7]));
        assertEquals(1, simpleGraph.getEdgeWeight(simpleGraph.getEdge(v[6], v[7])), 0.0);
        assertNotNull(simpleGraph.getEdge(v[3], v[7]));
        assertEquals(2, simpleGraph.getEdgeWeight(simpleGraph.getEdge(v[3], v[7])), 0.0);
    }

    @Test
    public void testMatrixToGraph8connected() {
        Matrix img = new Matrix(new double[][] { 
            { 0, 1, 1, 0 }, 
            { 0, 0, 0, 1 }, 
            { 0, 1, 1, 1 }});
        
        UndirectedGraph<IntVertex, DefaultEdge> graph = GraphUtils.matrixToGraph(img, true /* 8-connected */);
        
        // Expected:
        // 3 --- 6 --  
        //            \
        //             10
        //         /    |
        // 5 --- 8 --- 11
        assertEquals(6, graph.edgeSet().size());
        assertEquals(6, graph.vertexSet().size());
        assertTrue(graph.vertexSet().contains(new IntVertex(3)));
        assertTrue(graph.vertexSet().contains(new IntVertex(5)));
        assertTrue(graph.vertexSet().contains(new IntVertex(6)));
        assertTrue(graph.vertexSet().contains(new IntVertex(8)));
        assertTrue(graph.vertexSet().contains(new IntVertex(10)));
        assertTrue(graph.vertexSet().contains(new IntVertex(11)));
        
        assertNotNull(graph.getEdge(new IntVertex(3), new IntVertex(6)));
        assertNotNull(graph.getEdge(new IntVertex(6), new IntVertex(10)));
        assertNotNull(graph.getEdge(new IntVertex(10), new IntVertex(8)));
        assertNotNull(graph.getEdge(new IntVertex(10), new IntVertex(11)));
        assertNotNull(graph.getEdge(new IntVertex(11), new IntVertex(8)));
        assertNotNull(graph.getEdge(new IntVertex(8), new IntVertex(5)));
    }
    
    @Test
    public void testMatrixToGraph4connected() {
        Matrix img = new Matrix(new double[][] { 
            { 0, 1, 1, 0 }, 
            { 0, 0, 0, 1 }, 
            { 0, 1, 1, 1 }});
        
        UndirectedGraph<IntVertex, DefaultEdge> graph = GraphUtils.matrixToGraph(img, false /* 4-connected */);
        
        // Expected disconnected graph:
        // 3 --- 6   
        //            
        //             10
        //              |
        // 5 --- 8 --- 11
        assertEquals(4, graph.edgeSet().size());
        assertEquals(6, graph.vertexSet().size());
        assertTrue(graph.vertexSet().contains(new IntVertex(3)));
        assertTrue(graph.vertexSet().contains(new IntVertex(5)));
        assertTrue(graph.vertexSet().contains(new IntVertex(6)));
        assertTrue(graph.vertexSet().contains(new IntVertex(8)));
        assertTrue(graph.vertexSet().contains(new IntVertex(10)));
        assertTrue(graph.vertexSet().contains(new IntVertex(11)));
        
        assertNotNull(graph.getEdge(new IntVertex(3), new IntVertex(6)));
        assertNotNull(graph.getEdge(new IntVertex(10), new IntVertex(11)));
        assertNotNull(graph.getEdge(new IntVertex(11), new IntVertex(8)));
        assertNotNull(graph.getEdge(new IntVertex(8), new IntVertex(5)));
    }
    
    @Test
    public void testMinimumSpanningTree() {
        // 3 --- 6 --  
        //            \
        //             10
        //         /    |
        // 5 --- 8 --- 11
        Matrix img = new Matrix(new double[][] { 
            { 0, 1, 1, 0 }, 
            { 0, 0, 0, 1 }, 
            { 0, 1, 1, 1 }});
        
        UndirectedGraph<IntVertex, DefaultEdge> graph = GraphUtils.matrixToGraph(img, true /* 8-connected */);
        
        UndirectedGraph<IntVertex, DefaultEdge> mst = GraphUtils.minimumSpanningTree(graph);
        
        // Expected:
        // 3 --- 6 --  
        //            \
        //             10
        //              |
        // 5 --- 8 --- 11
        assertEquals(5, mst.edgeSet().size());
        assertEquals(6, mst.vertexSet().size());
        assertTrue(mst.vertexSet().contains(new IntVertex(3)));
        assertTrue(mst.vertexSet().contains(new IntVertex(5)));
        assertTrue(mst.vertexSet().contains(new IntVertex(6)));
        assertTrue(mst.vertexSet().contains(new IntVertex(8)));
        assertTrue(mst.vertexSet().contains(new IntVertex(10)));
        assertTrue(mst.vertexSet().contains(new IntVertex(11)));
        
        assertNotNull(mst.getEdge(new IntVertex(3), new IntVertex(6)));
        assertNotNull(mst.getEdge(new IntVertex(6), new IntVertex(10)));
        assertNotNull(mst.getEdge(new IntVertex(10), new IntVertex(11)));
        assertNotNull(mst.getEdge(new IntVertex(11), new IntVertex(8)));
        assertNotNull(mst.getEdge(new IntVertex(8), new IntVertex(5)));
    }
}
