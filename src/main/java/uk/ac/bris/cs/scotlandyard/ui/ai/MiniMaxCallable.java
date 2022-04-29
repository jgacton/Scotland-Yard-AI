package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;
import java.util.concurrent.Callable;

public class MiniMaxCallable implements Callable<Integer> {
    private final Board.GameState state;
    private final int depth;
    private final boolean isMrXMove;
    private final GameTree gameTree;
    private final int alpha;
    private final int beta;

    public MiniMaxCallable(Board.GameState state, int depth, int alpha, int beta, boolean isMrXMove, GameTree gameTree) {
        this.state = state;
        this.depth = depth;
        this.alpha = alpha;
        this.beta = beta;
        this.isMrXMove = isMrXMove;
        this.gameTree = gameTree;
    }

    private int minimax(Board.GameState state, int depth, int alpha, int beta, boolean isMrXMove) {
        if(depth == 0 || !state.getWinner().isEmpty()) {
            Move precedingMove = this.gameTree.getMoveWhichGenerated(state);
            return Evaluator.evaluateBoard(state, precedingMove);
        }
        if(isMrXMove) {
            int maxEval = Integer.MIN_VALUE;
            List<Move> movesToEvaluate = Pruning.pruneMrXMoves(state);

            for(Move moveToEvaluate : movesToEvaluate) {
                Board.GameState nextState = state.advance(moveToEvaluate);
                this.gameTree.appendGameTree(state, moveToEvaluate, nextState);
                int currentEval = minimax(nextState, depth - 1, alpha, beta, false);

                if(currentEval > maxEval) maxEval = currentEval;
                if(currentEval > alpha) alpha = currentEval;
                if(beta <= alpha) break;
            }

            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            List<Move> movesToEvaluate = Pruning.pruneDetectiveMoves(state);

            for(Move moveToEvaluate : movesToEvaluate) {
                Board.GameState nextState = state.advance(moveToEvaluate);
                this.gameTree.appendGameTree(state, moveToEvaluate, nextState);
                int currentEval = minimax(nextState, depth - 1, alpha, beta, isMrXMove(nextState));

                if(currentEval < minEval) minEval = currentEval;
                if(currentEval < beta) beta = currentEval;
                if(beta <= alpha) break;
            }

            return minEval;
        }
    }

    private boolean isMrXMove(Board board) {
        return board.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    @Override
    public Integer call() {
        return minimax(this.state, this.depth, this.alpha, this.beta, this.isMrXMove);
    }
}