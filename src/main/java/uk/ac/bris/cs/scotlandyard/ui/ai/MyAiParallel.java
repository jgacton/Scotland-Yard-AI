package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAiParallel implements Ai {
    Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
    @Nonnull @Override public String name() { return "Squeak :)"; }

    // Returns move to be played by the AI
    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        Board.GameState state = (Board.GameState) board;
        boolean isMrXMove = isMrXMove(state);
        List<Move> moves;

        if(isMrXMove) moves = pruneMrXMoves(state);
        else moves = pruneDetectiveMoves(state);
        if(moves.size() == 1) return moves.get(0);

        Move bestMove = moves.get(new Random().nextInt(moves.size()));

        List<Callable<Integer>> topLevelCalls = new ArrayList<>();

        for(Move moveToEvaluate : moves) {
            GameTree gameTree = new GameTree(state);
            Board.GameState nextState = state.advance(moveToEvaluate);

            gameTree.appendGameTree(state, moveToEvaluate, nextState);

            MiniMaxCallable MM = new MiniMaxCallable(nextState, nextState.getPlayers().size(), Integer.MIN_VALUE, Integer.MAX_VALUE, isMrXMove(nextState), gameTree);

            topLevelCalls.add(MM);
        }

        ExecutorService executor = Executors.newFixedThreadPool(moves.size());
        List<Future<Integer>> evals;
        try {
            evals = executor.invokeAll(topLevelCalls);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(isMrXMove) {
            int maxEval = Integer.MIN_VALUE;
            for(int i = 0; i < evals.size(); i++) {
                try {
                    int currentEval = evals.get(i).get();
                    if(currentEval > maxEval) {
                        maxEval = currentEval;
                        bestMove = moves.get(i);
                    }
                } catch(InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            int minEval = Integer.MAX_VALUE;
            for(int i = 0; i < evals.size(); i++) {
                try {
                    int currentEval = evals.get(i).get();
                    if(currentEval < minEval) {
                        minEval = currentEval;
                        bestMove = moves.get(i);
                    }
                } catch(InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
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

    // helper function to check if Mr X move
    private boolean isMrXMove(Board board) {
        var moves = board.getAvailableMoves().asList();
        return moves.stream().anyMatch(x -> x.commencedBy().isMrX());
    }
}
