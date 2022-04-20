package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.concurrent.*;

public class MyTest implements Ai {

    @Nonnull @Override public String name() { return "Jarvis"; }

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        boolean mrxMove = isMrXMove(board);

        Board.GameState state = (Board.GameState) board;
        ImmutableList<Move> moves = state.getAvailableMoves().asList();

        if (mrxMove) {
            Move bestMove = moves.get(new Random().nextInt(moves.size()));
            int bestEval = -1000000;
            List<Callable<Integer>> topLevelCalls = new ArrayList<>();
            for(Move move : moves) {
                GameTree gameTree = new GameTree(state);
                gameTree.appendGameTree(state, move, state.advance(move));
                MiniMaxCallable MM = new MiniMaxCallable(state, 2, isMrXMove(state.advance(move)), gameTree, -1000000, 1000000);
                topLevelCalls.add(MM);
            }
            ExecutorService executor = Executors.newFixedThreadPool(moves.size());
            List<Future<Integer>> evals = null;
            try {
                evals = executor.invokeAll(topLevelCalls);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            for(int i = 0; i < evals.size(); i++) {
                try {
                    if(evals.get(i).get() > bestEval) {
                        bestEval = evals.get(i).get();
                        bestMove = moves.get(i);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            executor.shutdown();
            System.out.println("Best eval: " + bestEval);
            return bestMove;
        }
        Move bestMove = moves.get(new Random().nextInt(moves.size()));
        /*
        List<Piece> detectives = new ArrayList<>();
        // detectives contains all the detectives who still need to go
        for(Move move : moves) {
            if(!detectives.contains(move.commencedBy())) {
                detectives.add(move.commencedBy());
            }
        }

        int bestEval = 1000000;
        List<Callable<Integer>> topLevelCalls = new ArrayList<>();
        for(Move move : moves) {
            GameTree gameTree = new GameTree(state);
            gameTree.appendGameTree(state, move, state.advance(move));
            MiniMaxCallable MM = new MiniMaxCallable(state, state.getPlayers().size() + detectives.size(), isMrXMove(state), gameTree, -1000000, 1000000);
            topLevelCalls.add(MM);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<Future<Integer>> evals = null;
        try {
            evals = executor.invokeAll(topLevelCalls);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for(int i = 0; i < evals.size(); i++) {
            try {
                if(evals.get(i).get() < bestEval) {
                    bestEval = evals.get(i).get();
                    bestMove = moves.get(i);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        executor.shutdown();
        */
        return bestMove;
    }

    private boolean isMrXMove(Board board) {
        return board.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }
}