package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MiniMaxCallable implements Callable<Integer> {
    private final Board.GameState state;
    private final int depth;
    private final boolean isMrXMove;
    private final GameTree gameTree;
    private final int alpha;
    private final int beta;
    private final Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));

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
            List<Move> movesToEvaluate = pruneMrXMoves(state);

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
            List<Move> movesToEvaluate = pruneDetectiveMoves(state);

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

    private List<Move> pruneExpensiveAndDuplicateMoves(Board.GameState state) {
        List<Integer> nodesVisited = new ArrayList<>();
        List<Move> movesToPrune = new ArrayList<>(List.copyOf(state.getAvailableMoves()));
        // Gets list of unique destination nodes for all moves in this state
        for(Move move : movesToPrune) {
            boolean occupied = false;
            for(Piece piece : state.getPlayers()) {
                if(piece.isDetective()) {
                    if(state.getDetectiveLocation((Piece.Detective) piece).orElseThrow().equals(move.accept(getDestinationFinal))) occupied = true;
                }
            }

            if(!occupied && !nodesVisited.contains(move.accept(getDestinationFinal))) {
                nodesVisited.add(move.accept(getDestinationFinal));
            }
        }

        List<Move> cheapMoves = new ArrayList<>();
        // For each unique destination node, finds the move with the lowest ticket cost.
        for(int n : nodesVisited) {
            Move cheapestMove = movesToPrune.get(0);
            int cheapestCost = 100;
            for(Move move : movesToPrune) {
                if(n == move.accept(getDestinationFinal) && Evaluator.getTicketCostOfMove(move) < cheapestCost) {
                    cheapestCost = Evaluator.getTicketCostOfMove(move);
                    cheapestMove = move;
                }
            }
            cheapMoves.add(cheapestMove);
        }
        return cheapMoves;
    }

    private List<Move> pruneMrXMoves(Board.GameState state) {
        List<Move> uniqueMoves = pruneExpensiveAndDuplicateMoves(state);
        List<Move> movesToEvaluate = new ArrayList<>(List.copyOf(uniqueMoves));
        int currentShortestDistanceToDetective = Evaluator.getShortestPathToDetective(state, uniqueMoves.get(0).source());

        for(Move move : uniqueMoves) {
            if(Evaluator.getShortestPathToDetective(state.advance(move), move.accept(getDestinationFinal)) < currentShortestDistanceToDetective) {
                movesToEvaluate.remove(move);
            }
        }

        if(movesToEvaluate.size() > 0) return movesToEvaluate;
        return uniqueMoves;
    }

    private List<Move> pruneDetectiveMoves(Board.GameState state) {
        List<Move> uniqueMoves = pruneExpensiveAndDuplicateMoves(state);
        List<Move> movesToEvaluate = new ArrayList<>(List.copyOf(uniqueMoves));
        if(state.getMrXTravelLog().size() < 3) return uniqueMoves;
        int currentEvaluation = Evaluator.evaluateForDetective(state);
        for(Move move : uniqueMoves) {
            if(Evaluator.evaluateForDetective(state.advance(move)) > currentEvaluation) {
                movesToEvaluate.remove(move);
            }
        }

        if(movesToEvaluate.size() > 0) return movesToEvaluate;
        return uniqueMoves;
    }

    private boolean isMrXMove(Board board) {
        return board.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    @Override
    public Integer call() {
        return minimax(this.state, this.depth, this.alpha, this.beta, this.isMrXMove);
    }
}