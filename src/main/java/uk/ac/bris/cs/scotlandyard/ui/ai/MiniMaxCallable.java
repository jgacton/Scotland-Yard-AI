package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.concurrent.Callable;

public class MiniMaxCallable implements Callable {
    private Board.GameState state;
    private int depth;
    private boolean isMrX;
    private GameTree tree;
    private int alpha;
    private int beta;
    Evaluator evaluator = new Evaluator();

    public MiniMaxCallable(Board.GameState state, int depth, boolean isMrX, GameTree tree, int alpha, int beta) {
        this.state = state;
        this.depth = depth;
        this.isMrX = isMrX;
        this.tree = tree;
        this.alpha = alpha;
        this.beta = beta;
    }

    private int minimaxAlphaBetaPruning(Board.GameState node, int depth , boolean isMrX, GameTree tree, int alpha, int beta) {
        //|| !node.getWinner().isEmpty()
        if(depth == 0) {
            //System.out.println(tree.predecessors(node).isEmpty());
            System.out.println("BEFORE WE DO THE MOVE");
            System.out.println(node);
            //System.out.println(tree.nodes().contains(node));
            Move whereCameFrom = tree.getMoveWhichGenerated(node);
            System.out.println("AFTER WE DO THE MOVE");
            //System.out.println("Here");
            return evaluator.getEvaluation(node, whereCameFrom);
        }
        if(isMrX) {
            System.out.println("ERROR HAPPENS BEFORE A MR X ");
            int maxEval = -1000000;
            int i = 1;
            System.out.println(node.getAvailableMoves().size());
            for(Move move : node.getAvailableMoves()) {
                System.out.println("move number : " + i);
                i++;
                Board.GameState nextState = node.advance(move);
                // adds the next state to the game tree
                tree.appendGameTree(node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, false, tree, alpha, beta);
                if(eval > maxEval) {
                    maxEval = eval;
                }
                if(eval > alpha) {
                    alpha = eval;
                }
                if(beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        } else {
            System.out.println("ERROR HAPPENS BEFORE A DETECTIVE");
            int minEval = 1000000;
            for(Move move : node.getAvailableMoves()) {
                Board.GameState nextState = node.advance(move);
                tree.appendGameTree(node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, isMrXMove(nextState), tree, alpha, beta);
                if(eval < minEval) {
                    minEval = eval;
                }
                if(eval < beta) {
                    beta = eval;
                }
                if(beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
    }

    private boolean isMrXMove(Board board) {
        return board.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    @Override
    public Object call() {
        return minimaxAlphaBetaPruning(state, depth, isMrX, tree, alpha, beta);
    }
}
