package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class Dijkstra {

    public static int getShortestPath(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, int source, int destination) {
        if(!(graph.nodes().contains(source) && graph.nodes().contains(destination))) {
            return 1000000;
        }

        Set<Integer> Q = new HashSet<>();
        Integer[] dist = new Integer[199];
        Integer[] prev = new Integer[199];

        for(Integer N : graph.nodes()) {
            dist[N-1] = 1000000;
            prev[N-1] = null;
            Q.add(N);
        }

        dist[source - 1] = 0;

        while(!Q.isEmpty()) {

            int minDist = 1000000;
            int u = 0;
            for(Integer N : Q) {
                if(dist[N-1] < minDist) {
                    minDist = dist[N-1];
                    u = N;
                }
            }

            Q.remove(u);

            for(Integer v : graph.adjacentNodes(u)) {
                if(Q.contains(v)) {
                    int edgeVal = 0;
                    if(!graph.edgeValue(u, v).orElseThrow().isEmpty()) {
                        edgeVal = 1;
                    }
                    int alt = dist[u-1] + edgeVal;
                    if(alt < dist[v-1]) {
                        dist[v-1] = alt;
                        prev[v-1] = u;
                    }
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        path.add(destination);
        while(destination != source) {
            path.add(prev[destination-1]);
            destination = prev[destination-1];
        }

        Collections.reverse(path);

        return(path.size() - 1);
    }
}
