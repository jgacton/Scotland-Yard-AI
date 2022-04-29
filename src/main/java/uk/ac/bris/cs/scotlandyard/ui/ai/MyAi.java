package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {
    @Nonnull @Override public String name() { return "Bubble :)"; }

    // Returns move to be played by the AI
    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        Board.GameState state = (Board.GameState) board;
        boolean isMrXMove = isMrXMove(state);
        List<Move> moves;

        if(isMrXMove) moves = Pruning.pruneMrXMoves(state);
        else moves = Pruning.pruneDetectiveMoves(state);
        if(moves.size() == 1) return moves.get(0);

        Move bestMove = moves.get(new Random().nextInt(moves.size()));

        List<Callable<Integer>> topLevelCalls = new ArrayList<>();

        for(Move moveToEvaluate : moves) {
            GameTree gameTree = new GameTree(state);
            Board.GameState nextState = state.advance(moveToEvaluate);

            gameTree.appendGameTree(state, moveToEvaluate, nextState);

            MiniMaxCallable MM = new MiniMaxCallable(nextState, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, isMrXMove(nextState), gameTree);

            topLevelCalls.add(MM);
        }

        ExecutorService executor = Executors.newFixedThreadPool(moves.size());
        List<Future<Integer>> evaluations;
        try {
            evaluations = executor.invokeAll(topLevelCalls);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(isMrXMove) {
            int maxEval = Integer.MIN_VALUE;
            for(int i = 0; i < evaluations.size(); i++) {
                try {
                    int currentEval = evaluations.get(i).get();
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
            for(int i = 0; i < evaluations.size(); i++) {
                try {
                    int currentEval = evaluations.get(i).get();
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



    // helper function to check if Mr X move
    private boolean isMrXMove(Board board) {
        var moves = board.getAvailableMoves().asList();
        return moves.stream().anyMatch(x -> x.commencedBy().isMrX());
    }
}
