package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAiParallel implements Ai {

    @Nonnull @Override public String name() { return "Squeak :)"; }

    // Returns move to be played by the AI
    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        Board.GameState state = (Board.GameState) board;
        List<Move> moves = pruneExpensiveAndDuplicateMoves(state);
        if(moves.size() == 1) return moves.get(0);

        Move bestMove = moves.get(new Random().nextInt(moves.size()));
        boolean isMrXMove = isMrXMove(state);

        List<Callable<Integer>> topLevelCalls = new ArrayList<>();
        for(Move moveToEvaluate : moves) {
            GameTree gameTree = new GameTree(state);
            Board.GameState nextState = state.advance(moveToEvaluate);
            gameTree.appendGameTree(state, moveToEvaluate, nextState);
            MiniMaxCallable MM = new MiniMaxCallable(nextState, nextState.getPlayers().size(), isMrXMove(nextState), gameTree, -1000000, 1000000);

            topLevelCalls.add(MM);
        }

        ExecutorService executor = Executors.newFixedThreadPool(moves.size());
        List<Future<Integer>> evals;
        try {
            evals = executor.invokeAll(topLevelCalls);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int bestEval = -1000000;
        for(int i = 0; i < evals.size(); i++) {
            try {
                int currentEval = evals.get(i).get();
                if(isMrXMove && currentEval > bestEval) {
                    bestEval = evals.get(i).get();
                    bestMove = moves.get(i);
                } else if(!isMrXMove && currentEval < Math.abs(bestEval)) {
                    bestEval = currentEval;
                    bestMove = moves.get(i);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        executor.shutdown();
        return bestMove;
    }

    private List<Move> pruneExpensiveAndDuplicateMoves(Board.GameState state) {
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
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

    // helper function to check if Mr X move
    private boolean isMrXMove(Board board) {
        var moves = board.getAvailableMoves().asList();
        return moves.stream().anyMatch(x -> x.commencedBy().isMrX());
    }
}
