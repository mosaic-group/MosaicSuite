package mosaic.utils.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

public class GraphUtils {
    public static class StrVertex {
        private String iLabel;
        
        public StrVertex(String aName) {iLabel = aName;}

        public String getLabel() { return iLabel; }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((iLabel == null) ? 0 : iLabel.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            StrVertex other = (StrVertex) obj;
            if (iLabel == null) {
                if (other.iLabel != null) return false;
            }
            else if (!iLabel.equals(other.iLabel)) return false;
            return true;
        }
        
        @Override
        public String toString() {return iLabel;}
    }
    
    public static class IntVertex {
        private int iLabel;
        
        public IntVertex(int aNum) {iLabel = aNum;}

        public int getLabel() { return iLabel; }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + iLabel;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            IntVertex other = (IntVertex) obj;
            if (iLabel != other.iLabel) return false;
            return true;
        }

        @Override
        public String toString() {return Integer.toString(iLabel);}
    }
    
    
    /**
     * Concatenates all vertices/edges which are considered not changing graph topology i.e. vertices which
     * have only two edges. After this operation graph will have same number of leafs, loops, and forks as 
     * input. This is mainly useful operation for calculating quickly leaf2leaf paths.
     * 
     * Example:
     * 
     * A --- B --- C --- D --- E
     *                   |
     *                   F
     * with all edges weight equal to 1, will be reconstructed to graph like:
     * 
     *  A --- D --- E
     *        |
     *        F
     * with AD weight equal to 3, and DE, DF equal to 1
     *                    
     * @param aGraph graph to be "simplified"
     * @return weighted graph
     */
    public static <V, E extends DefaultEdge> WeightedGraph<V, DefaultWeightedEdge> simplifySimipleUndirectedGraph(UndirectedGraph<V, E> aGraph) {
        // Create new weighted graph which is same as input
        WeightedGraph<V, DefaultWeightedEdge> weightedGraph = new SimpleWeightedGraph<V, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        for (V v : aGraph.vertexSet()) weightedGraph.addVertex(v);
        for (E e : aGraph.edgeSet()) {
            DefaultWeightedEdge edge = weightedGraph.addEdge(aGraph.getEdgeSource(e), aGraph.getEdgeTarget(e));
            weightedGraph.setEdgeWeight(edge, aGraph.getEdgeWeight(e));
        }
        
        boolean done = false;
        while (!done) {
            // Normally we can remove elements from the set and continue looping but.. we are removing
            // vertex from the graph and because its internal structures updates it is not sure if we 
            // could continue. Break then and continue on outside loop...
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
                    
                    done = false;
                    break;
                }
            }
        }
        
        return weightedGraph;
    }
    
    /**
     * Runs MST and creates new graph with proper vertices and eges.
     * @param aGraph input graph
     * @return new MST graph
     */
    public static <V, E extends DefaultEdge> UndirectedGraph<V, E> minimumSpanningTree(UndirectedGraph<V, E> aGraph) {
        KruskalMinimumSpanningTree<V, E> mst = new KruskalMinimumSpanningTree<>(aGraph);
        UndirectedGraph<V, E> graphMst = new SimpleGraph<>(aGraph.getEdgeFactory());
        for (V v : aGraph.vertexSet()) graphMst.addVertex(v);
        for (E e : mst.getEdgeSet()) graphMst.addEdge(aGraph.getEdgeSource(e), aGraph.getEdgeTarget(e));
        
        return graphMst;
    }
    
    /**
     * Crates graph from matrix considering every element != 0 as a graph vertex.
     * @param aMatrix - input matrix
     * @param aIs8connected - connectivity
     * @return graphs created from provided matrix
     */
    public static UndirectedGraph<IntVertex, DefaultEdge> matrixToGraph(Matrix aMatrix, boolean aIs8connected) {
        List<List<Integer>> graphConnections = findAllElementsOfObject(aMatrix, aIs8connected);
        UndirectedGraph<IntVertex, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        for (int i = 0; i < graphConnections.size(); ++i) {
            int sourceVertexId = graphConnections.get(i).get(0);
            IntVertex v = new IntVertex(sourceVertexId);
            graph.addVertex(v);

            for (int z = 1; z < graphConnections.get(i).size(); z++) {
                int targetVertexId = graphConnections.get(i).get(z);
                IntVertex vz = new IntVertex(targetVertexId);
                graph.addVertex(vz);
                graph.addEdge(v, vz);
            }
        }
        
        return graph;
    }
    
    /**
     * Generates connectivity list for creating undirected graph in form:
     * [ [source vertex 1, target neighbor 1, target neighbor 2,... ]
     *   [source vertex n, target neighbor 1, target neighbor 2,... ]
     * ]
     * All elements of input matrix different from 0 are considered as a graph vertex.
     * NOTE: Every connectivity is given only once, so if "source vertex m" has on list connectivity
     *       to "neighbor n", then "source vertex n" does not have on list "neighbor m".
     * @param aMatrix - input Matrix
     * @param aIs8connected - connectivity between vertices
     * @return connectivity list
     */
    private static List<List<Integer>> findAllElementsOfObject(Matrix aMatrix, boolean aIs8connected) {
        aMatrix = Matlab.logical(aMatrix, 0); // make 0s and 1s
        // List of found elements belonging to one componnent
        final List<List<Integer>> elements = new ArrayList<List<Integer>>();

        final double[][] aM = aMatrix.getArrayXY();
        final int aWidth = aM.length;
        final int aHeight = aM[0].length;

        while (true) {
            // Find first non-zero element of matrix, it will be our seed for searching.
            int startXpoint = -1;
            int startYpoint = -1;
            // Go through whole array
            for (int x = 0; x < aWidth; ++x) {
                for (int y = 0; y < aHeight; ++y) {
                    if (aM[x][y] != 0) {
                        startXpoint = x;
                        startYpoint = y;
                    }
                }
            }
            if (startXpoint < 0) return elements;

            // List of elements to be visited
            final List<Integer> q = new ArrayList<Integer>();

            // Initialize list with entry point
            q.add(startXpoint * aHeight + startYpoint);

            // Iterate until all elements of component are visited
            while (!q.isEmpty()) {
                // Get first element on the list and remove it
                final int id = q.remove(0);
                final int x = id / aHeight;
                final int y = id % aHeight;

                // Clear element and add it to element's container
                aM[x][y] = 0;
                List<Integer> ll = new ArrayList<Integer>();
                ll.add(id);
                elements.add(ll);

                // Check all neighbours of currently processed pixel
                // (do some additional logic to skip point itself and to handle 4/8
                // base connectivity)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            final int indX = x + dx;
                            final int indY = y + dy;
                            if (indX >= 0 && indX < aWidth && indY >= 0 && indY < aHeight) {
                                if (aIs8connected || (dy * dx == 0)) {
                                    if (aM[indX][indY] == 1) {
                                        final int idx = indX * aHeight + indY;
                                        ll.add(idx);
                                        if (!q.contains(idx)) {
                                            // If element was not visited yet put it on list
                                            q.add(idx);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
