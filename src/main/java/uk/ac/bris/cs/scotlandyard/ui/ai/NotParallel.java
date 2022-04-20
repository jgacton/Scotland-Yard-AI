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

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {
        calls = 0;
        prunes = 0;

        Board.GameState state = (Board.GameState) board;
        List<Move> moves = topLevelPruning(state);
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
        }
        return bestMove;
    }

    public int minimax(Board.GameState state, int depth, int alpha, int beta, boolean isMrXMove) {
        if(depth == 0 || !state.getWinner().isEmpty()) {
            return Evaluator.evaluateBoard(state, this.gameTree.getMoveWhichGenerated(state));
        }

        if(isMrXMove) {
            int maxEval = -1000000;
            for(Move move : state.getAvailableMoves()) {
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
    private List<Move> topLevelPruning(Board.GameState state) {
        List<Move> prunedMoves = new ArrayList<>();
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        List<Integer> nodesVisited = new ArrayList<>();
        for(Move move : state.getAvailableMoves()) {
            boolean occupied = false;
            for(Piece piece : state.getPlayers()) {
                if(piece.isDetective()) {
                    if(state.getDetectiveLocation((Piece.Detective) piece).orElseThrow().equals(move.accept(getDestinationFinal))) occupied = true;
                }
            }

            if(!occupied && Evaluator.getShortestPathToDetective(state.advance(move), move.accept(getDestinationFinal)) <= Evaluator.getShortestPathToDetective(state, move.source())) {
                if(!nodesVisited.contains(move.accept(getDestinationFinal))) {
                    nodesVisited.add(move.accept(getDestinationFinal));
                    prunedMoves.add(move);
                }
            }
        }

        List<Move> superPrunedMoves = prunedMoves;
        for(Move move1 : prunedMoves) {
            for(Move move2 : prunedMoves) {
                if(move1.accept(getDestinationFinal).equals(move2.accept(getDestinationFinal))) {
                    if(Evaluator.getTicketCostOfMove(move1) < Evaluator.getTicketCostOfMove(move2)) {
                        superPrunedMoves.remove(move2);
                    }
                }
            }
        }
        return superPrunedMoves;
    }
}