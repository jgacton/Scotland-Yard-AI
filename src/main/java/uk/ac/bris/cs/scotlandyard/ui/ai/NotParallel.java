package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class NotParallel implements Ai {
    GameTree gameTree;
    int calls;
    int prunes;
    @Nonnull @Override public String name() { return "Not Parallel"; }

    @Override public void onStart() {}

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {
        calls = 0;
        prunes = 0;

        Board.GameState state = (Board.GameState) board;
        List<Move> moves = pruneMoves(state);
        Move bestMove = state.getAvailableMoves().asList().get(new Random().nextInt(state.getAvailableMoves().size()));
        boolean isMrXMove = isMrXMove(state);

        if(this.gameTree == null) {
            this.gameTree = new GameTree(state);
        }

        if(isMrXMove) {
            int bestEval = -1000000;
            for(Move move : moves) {
                Board.GameState nextState = state.advance(move);
                this.gameTree.appendGameTree(state, move, nextState);
                int currentEval = minimax(nextState, state.getPlayers().size(), -1000000, 1000000, isMrXMove(nextState));
                calls++;
                if(currentEval > bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
            }
            System.out.println("Calls: " + calls);
            System.out.println("Prunes: " + prunes);
            System.out.println("Moves: " + state.getAvailableMoves().size() + ", Moves evaluated: " + moves.size());
        } else if(state.getMrXTravelLog().size() >= 3) {
            List<LogEntry> log = state.getMrXTravelLog();
            int lastMrXLoc;
            int i = log.size() - 1;
            while(log.get(i).location().isEmpty()) {
                i--;
            }
            lastMrXLoc = log.get(i).location().get();

        }
        return bestMove;
    }

    public int minimax(Board.GameState state, int depth, int alpha, int beta, boolean isMrXMove) {
        if(depth == 0 || !state.getWinner().isEmpty()) {
            return Evaluator.evaluateBoard(state, this.gameTree.getMoveWhichGenerated(state));
        }

        if(isMrXMove) {
            List<Move> moves = pruneMoves(state);
            int maxEval = -1000000;
            for(Move move : moves) {
                Board.GameState nextState = state.advance(move);
                this.gameTree.appendGameTree(state, move, nextState);
                int eval = minimax(nextState, depth - 1, alpha, beta, isMrXMove(nextState));
                calls++;
                if(eval > maxEval) maxEval = eval;
                if(eval > alpha) alpha = eval;
                if(beta <= alpha) {
                    prunes++;
                    break;
                }
            }
            return maxEval;
        } else {
            int minEval = -1000000;
            for(Move move : state.getAvailableMoves()) {
                Board.GameState nextState = state.advance(move);
                this.gameTree.appendGameTree(state, move, nextState);
                calls++;
                int eval = minimax(nextState, depth - 1, alpha, beta, isMrXMove(nextState));
                if(eval < minEval) minEval = eval;
                if(eval < beta) beta = eval;
                if(beta <= alpha) {
                    prunes++;
                    break;
                }
            }
            return minEval;
        }
    }

    private boolean isMrXMove(Board.GameState state) {
        return state.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    // Prunes top level of moves by removing moves that decrease shortest distance to a detective and duplicate moves
    // with a higher ticket cost.
    private List<Move> pruneMoves(Board.GameState state) {
        List<Move> prunedMoves = new ArrayList<>();
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        for(Move move : state.getAvailableMoves()) {
            boolean occupied = false;
            for(Piece piece : state.getPlayers()) {
                if(piece.isDetective()) {
                    if(state.getDetectiveLocation((Piece.Detective) piece).orElseThrow().equals(move.accept(getDestinationFinal))) occupied = true;
                }
            }

            if(!occupied && Evaluator.getShortestPathToDetective(state.advance(move), move.accept(getDestinationFinal)) <= Evaluator.getShortestPathToDetective(state, move.source())) {
                prunedMoves.add(move);
            }
        }

        List<Move> extraPrunedMoves = new ArrayList<>();
        for(int i = 0; i < prunedMoves.size() - 1; i++) {
            Move move1 = prunedMoves.get(i);
            for(int j = i + 1; j < prunedMoves.size(); j++) {
                Move move2 = prunedMoves.get(j);
                if(move1.accept(getDestinationFinal).equals(move2.accept(getDestinationFinal))) {
                    if(Evaluator.getTicketCostOfMove(move1) < Evaluator.getTicketCostOfMove(move2)) {
                        extraPrunedMoves.add(move1);
                    }
                }
            }
        }
        return extraPrunedMoves;
    }

    @Override public void onTerminate() {}
}