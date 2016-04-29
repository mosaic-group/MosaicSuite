package mosaic.utils.math;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Test;

import mosaic.utils.math.Graph.Vertex;


public class GraphTest {

    @Test
    public void testSimplifySimipleUndirectedGraphTree() {
        int num = 6;
        Vertex[] v = new Vertex[num];
        for (int i = 0; i < v.length; i++) v[i] = new Vertex(new String(new char[]{(char) ('A' + i)}));
        
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
        
        WeightedGraph<Object, DefaultWeightedEdge> simpleGraph = Graph.simplifySimipleUndirectedGraph(graph);
        
        // Expected (B,C removed):
        // A --- D --- E
        //       |
        //       F
        System.out.println(simpleGraph);
    }
    
    @Test
    public void testSimplifySimipleUndirectedGraphLoop() {
        int num = 8;
        Vertex[] v = new Vertex[num];
        for (int i = 0; i < v.length; i++) v[i] = new Vertex(new String(new char[]{(char) ('A' + i)}));
        
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
        
        WeightedGraph<Object, DefaultWeightedEdge> simpleGraph = Graph.simplifySimipleUndirectedGraph(graph);
        
        // Expected (B,C removed):
        // A --- D --- G
        //        \   /
        //          H
        System.out.println(simpleGraph);
        for (DefaultWeightedEdge edge : simpleGraph.edgeSet()) {
        System.out.println(edge + " " +simpleGraph.getEdgeWeight(edge));
        }
    }

}
