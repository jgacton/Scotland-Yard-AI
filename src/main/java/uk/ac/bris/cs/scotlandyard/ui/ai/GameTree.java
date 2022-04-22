package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

public class GameTree {
    private final MutableValueGraph<Board.GameState, Move> tree;

    public GameTree(Board.GameState root) {
        this.tree = ValueGraphBuilder.directed().build();
        this.tree.addNode(root);
    }

    public void appendGameTree(Board.GameState parentNode, Move move, Board.GameState childNode) {
        this.tree.addNode(childNode);
        this.tree.putEdgeValue(parentNode, childNode, move);
    }

    public Move getMoveWhichGenerated(Board.GameState state) {
        return this.tree.edgeValue(this.tree.predecessors(state).stream().toList().get(0), state).orElseThrow();
    }
}
