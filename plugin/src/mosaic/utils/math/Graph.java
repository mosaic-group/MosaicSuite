package mosaic.utils.math;

import java.util.Set;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class Graph {
    static class Vertex {
        private String iName;
        
        Vertex(String aName) {iName = aName;}

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((iName == null) ? 0 : iName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Vertex other = (Vertex) obj;
            if (iName == null) {
                if (other.iName != null) return false;
            }
            else if (!iName.equals(other.iName)) return false;
            return true;
        }
        
        @Override
        public String toString() {return iName;}
    }
    
    static <V, E extends DefaultEdge> WeightedGraph<V, DefaultWeightedEdge> simplifySimipleUndirectedGraph(UndirectedGraph<V, E> aGraph) {
        // Create new weighted graph which is same as input
        WeightedGraph<V, DefaultWeightedEdge> weightedGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for (V v : aGraph.vertexSet()) weightedGraph.addVertex(v);
        for (E e : aGraph.edgeSet()) {
            DefaultWeightedEdge edge = weightedGraph.addEdge(aGraph.getEdgeSource(e), aGraph.getEdgeTarget(e));
            weightedGraph.setEdgeWeight(edge, aGraph.getEdgeWeight(e));
        }
        
        boolean done = false;
        while (!done) {
            done = true;
            for (V v  : weightedGraph.vertexSet()) {
                Set<DefaultWeightedEdge> edges = weightedGraph.edgesOf(v);
                // If we have only two edges connected to vertex we can remove it and merge edges
                if (edges.size() == 2) {
                    DefaultWeightedEdge[] e = edges.toArray(new DefaultWeightedEdge[0]);
                    double weightOfEdge1 = weightedGraph.getEdgeWeight(e[0]);
                    V sourceVertexOfEdge1 = weightedGraph.getEdgeSource(e[0]);
                    V targetVertexOfEdge1 = weightedGraph.getEdgeTarget(e[0]);
                    double weightOfEdge2 = weightedGraph.getEdgeWeight(e[1]);
                    V sourceVertexOfEdge2 = weightedGraph.getEdgeSource(e[1]);
                    V targetVertexOfEdge2 = weightedGraph.getEdgeTarget(e[1]);

                    // Find vertices on the left and right of current vertex
                    V newVertexSource = (sourceVertexOfEdge1.equals(v)) ? targetVertexOfEdge1 : sourceVertexOfEdge1;
                    V newVertexTarget = (sourceVertexOfEdge2.equals(v)) ? targetVertexOfEdge2 : sourceVertexOfEdge2;
                    
                    // We cannot continue since there is another edge connecting this vertices
                    if (weightedGraph.getEdge(newVertexSource, newVertexTarget) != null) continue;
                    
                    // Remove current vertex and create new edge connecting left and right vertices
                    weightedGraph.removeVertex(v);
                    DefaultWeightedEdge newEdge = weightedGraph.addEdge(newVertexSource, newVertexTarget);
                    weightedGraph.setEdgeWeight(newEdge, weightOfEdge1 + weightOfEdge2);
                    
                    // Noramaly we can remove elements from the set and continu looping but.. we are removing
                    // vertex from the graph and not sure if we could conitnue. Break then and continue on 
                    // outside loop...
                    done = false;
                    break;
                }
            }
        }
        
        return weightedGraph;
    }
}
