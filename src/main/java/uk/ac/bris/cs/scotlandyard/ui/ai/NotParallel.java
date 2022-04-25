package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class NotParallel implements Ai {
    GameTree gameTree;
    int calls;
    @Nonnull @Override public String name() { return "Not Parallel"; }

    @Override public void onStart() {}

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        Board.GameState state = (Board.GameState) board;


        Move bestMove = state.getAvailableMoves().asList().get(new Random().nextInt(state.getAvailableMoves().size()));
        boolean isMrXMove = isMrXMove(state);

        this.calls = 0;

        if(this.gameTree == null) {
            this.gameTree = new GameTree(state);
        }

        List<Move> moves = pruneExpensiveMoves(state);
        if(isMrXMove) {
            moves = pruneMoves(state);

            if(moves.size() == 1) return moves.get(0);

            int bestEval = -1000000;

            for(Move move : moves) {
                Board.GameState nextState = state.advance(move);
                this.gameTree.appendGameTree(state, move, nextState);
                //BackTracking trackerNextState = new BackTracking(nextState, move, state);
                int currentEval = minimax(nextState, state.getPlayers().size(), -1000000, 1000000, isMrXMove(nextState));
                this.calls++;

                if(currentEval > bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
            }
        } else {
            List<Piece> detectives = new ArrayList<>();

            for(Move move : state.getAvailableMoves()) {
                if(!detectives.contains(move.commencedBy())) {
                    detectives.add(move.commencedBy());
                }
            }

            int bestEval = 1000000;

            //List<Move> moves = pruneExpensiveMoves(state);

            for(Move move : moves) {
                Board.GameState nextState = state.advance(move);
                //BackTracking trackerNextState = new BackTracking(nextState, move, state);
                //System.out.println("first level " + trackerNextState.getMoveCameFrom());
                this.gameTree.appendGameTree(state, move, nextState);
                //int currentEval = minimaxTracker(nextState, (state.getPlayers().size()) + detectives.size(), -1000000, 1000000, isMrXMove(nextState), trackerNextState);
                int currentEval = minimax(nextState, (state.getPlayers().size()) + detectives.size(), -1000000, 1000000, isMrXMove(nextState));
                this.calls++;
                System.out.println("the current eval is : " + currentEval);
                //System.out.println("the second current correct eval is : " + currentEval2);
                if(currentEval < bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
            }
        }
        // System.out.println("Calls: " + this.calls);
        // System.out.println("Moves in position: " + state.getAvailableMoves().size() + ", moves evaluated: " + moves.size());
        // System.out.println("Move: " + bestMove + ", cost: " + Evaluator.getTicketCostOfMove(bestMove));
        return bestMove;
    }

    public int minimaxTracker(Board.GameState state, int depth, int alpha, int beta, boolean isMrXMove, BackTracking tracker) {
        if(depth == 0 || !state.getWinner().isEmpty()) {
            System.out.println("the move we came from is and now at depth 0 : " + tracker.getMoveCameFrom());
            return Evaluator.evaluateBoard(state, tracker.getMoveCameFrom());
            //return Evaluator.evaluateBoard(state, this.gameTree.getMoveWhichGenerated(state));
        }

        int bestEval = -1000000;
        List<Move> moves = pruneExpensiveMoves(state);

        for(Move move : moves) {
            Board.GameState nextState = state.advance(move);
            BackTracking trackerForState = new BackTracking(nextState, move, state);
            this.gameTree.appendGameTree(state, move, nextState);
            if(depth == 1) {
                System.out.println("at a depth of 1 we have : " + tracker.getMoveCameFrom());
            }
            int eval = minimaxTracker(nextState, depth - 1, alpha, beta, isMrXMove(nextState), trackerForState);
            this.calls++;

            if(isMrXMove) {
                if(eval > bestEval) bestEval = eval;
                if(eval > alpha) alpha = eval;
            } else {
                if(eval < Math.abs(bestEval)) bestEval = eval;
                if(eval < beta) beta = eval;
            }

            if(beta <= alpha) break;
        }

        return bestEval;
    }


    public int minimax(Board.GameState state, int depth, int alpha, int beta, boolean isMrXMove) {
        if(depth == 0 || !state.getWinner().isEmpty()) {
            Move precedingMove = this.gameTree.getMoveWhichGenerated(state);
            return Evaluator.evaluateBoard(state, precedingMove);
        }

        int bestEval = -1000000;
        List<Move> moves = pruneExpensiveMoves(state);

        for(Move move : moves) {
            Board.GameState nextState = state.advance(move);

            this.gameTree.appendGameTree(state, move, nextState);

            int eval = minimax(nextState, depth - 1, alpha, beta, isMrXMove(nextState));
            this.calls++;

            if(isMrXMove) {
                if(eval > bestEval) bestEval = eval;
                if(eval > alpha) alpha = eval;
            } else {
                if(eval < Math.abs(bestEval)) bestEval = eval;
                if(eval < beta) beta = eval;
            }

            if(beta <= alpha) break;
        }

        return bestEval;
    }

    private boolean isMrXMove(Board.GameState state) {
        return state.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    // Don't evaluate moves which decrease distance to detective
    // Don't evaluate moves which decrease total distance to detectives
    private List<Move> pruneMoves(Board.GameState state) {
        List<Move> cheapMoves = pruneExpensiveMoves(state);

        int currentDistance = Evaluator.getShortestPathToDetective(state, state.getAvailableMoves().asList().get(0).source());
        int currentTotalDistance = Evaluator.getTotalDistanceToDetectives(state, state.getAvailableMoves().asList().get(0).source());

        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        List<Move> prunedMoves = new ArrayList<>(List.copyOf(cheapMoves));

        for(Move move : cheapMoves) {

            Board.GameState nextState = state.advance(move);
            int nextDistance = Evaluator.getShortestPathToDetective(nextState, move.accept(getDestinationFinal));

            if(nextDistance < currentDistance) {
                prunedMoves.remove(move);
            } else {
                int nextTotalDistance = Evaluator.getTotalDistanceToDetectives(nextState, move.accept(getDestinationFinal));

                if(nextTotalDistance < currentTotalDistance) {
                    prunedMoves.remove(move);
                }
            }
        }

        if(prunedMoves.size() == 0) return cheapMoves;

        return prunedMoves;
    }

    // Don't evaluate moves which have the same destination as another move with cheaper tickers
    private List<Move> pruneExpensiveMoves(Board.GameState state) {
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        List<Integer> nodesVisited = new ArrayList<>();
        for(Move move : state.getAvailableMoves()) {
            boolean occupied = false;
            for(Piece piece : state.getPlayers()) {
                if(piece.isDetective()) {
                    if(state.getDetectiveLocation((Piece.Detective) piece).orElseThrow().equals(move.accept(getDestinationFinal))) occupied = true;
                }
            }

            if(!occupied) {
                if(!nodesVisited.contains(move.accept(getDestinationFinal))) {
                    nodesVisited.add(move.accept(getDestinationFinal));
                }
            }
        }

        List<Move> moves = state.getAvailableMoves().asList();
        List<Move> cheapMoves = new ArrayList<>();
        for(int n : nodesVisited) {
            Move cheapestMove = moves.get(0);
            int cheapestCost = 10;
            for(Move move : moves) {
                if(n == move.accept(getDestinationFinal) && Evaluator.getTicketCostOfMove(move) < cheapestCost) {
                    cheapestCost = Evaluator.getTicketCostOfMove(move);
                    cheapestMove = move;
                }
            }
            cheapMoves.add(cheapestMove);
        }
        return cheapMoves;
    }

    @Override public void onTerminate() {}
}
