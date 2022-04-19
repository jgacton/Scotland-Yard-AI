package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

public class GameTree {

    private GameTree() {

    }

    private final MutableValueGraph<Board.GameState, Move> tree = ValueGraphBuilder.directed().build();
    private static GameTree gameTree;

    public static GameTree getTree() {
        if(gameTree == null) {
            gameTree = new GameTree();
        }
        return gameTree;
    }

    public void addRootNode(Board.GameState state) {
        if(tree.nodes().size() == 0) {
            tree.addNode(state);
        }
    }

    public void appendGameTree(Board.GameState parentNode, Move move, Board.GameState childNode) {
        tree.addNode(childNode);
        tree.putEdgeValue(parentNode, childNode, move);
    }

    public Move getMoveWhichGenerated(Board.GameState state) {
        return tree.edgeValue(tree.predecessors(state).stream().toList().get(0), state).orElseThrow();
    }
}
