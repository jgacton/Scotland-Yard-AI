package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class Test implements Ai {
    GameTree gameTree = GameTree.getTree();
    private final Dijkstra dijkstra = Dijkstra.getDijkstra();
    int totalEvaluated;

    @Nonnull @Override public String name() { return "Test"; }

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        Board.GameState state = (Board.GameState) board;
        gameTree.addRootNode(state);
        ImmutableList<Move> moves = state.getAvailableMoves().asList();
        totalEvaluated = 0;

        Move bestMove = moves.get(new Random().nextInt(moves.size()));
        if(isMrXMove(state)) {
            int bestEval = -1000000;
            for(Move move : moves) {
                Board.GameState nextState = state.advance(move);
                gameTree.appendGameTree(state, move, nextState);
                int currentEval = minimaxAlphaBetaPruning(nextState, state.getPlayers().size(), isMrXMove(nextState), -1000000, 1000000);
                if(currentEval > bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
            }
        } else {
            int bestEval = 1000000;
            for(Move move : moves) {
                Board.GameState nextState = state.advance(move);
                gameTree.appendGameTree(state, move, nextState);
                int currentEval = minimaxAlphaBetaPruning(nextState, state.getPlayers().size(), isMrXMove(nextState), -1000000, 1000000);
                if(currentEval < bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
            }
        }
        System.out.println(totalEvaluated);
        return bestMove;
    }

    private int minimaxAlphaBetaPruning(Board.GameState node, int depth , boolean isMrX, int alpha, int beta) {
        if(depth == 0 || !node.getWinner().isEmpty()) {
            totalEvaluated++;
            return evaluateBoard(node);
        }
        if(isMrX) {
            int maxEval = -1000000;
            for(Move move : node.getAvailableMoves()) {
                Board.GameState nextState = node.advance(move);
                gameTree.appendGameTree(node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, isMrXMove(nextState), alpha, beta);
                if(eval > maxEval) maxEval = eval;
                if(eval > alpha) alpha = eval;
                if(beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = 1000000;
            for(Move move : node.getAvailableMoves()) {
                Board.GameState nextState = node.advance(move);
                gameTree.appendGameTree(node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, isMrXMove(nextState), alpha, beta);
                if(eval < minEval) minEval = eval;
                if(eval < beta) beta = eval;
                if(beta <= alpha) break;
            }
            return minEval;
        }
    }

    private int evaluateBoard(Board.GameState state) {
        if(!state.getWinner().isEmpty()) {
            if(state.getWinner().stream().anyMatch(Piece::isMrX)) {
                return 1000000;
            }
            return -1000000;
        }

        Move move = gameTree.getMoveWhichGenerated(state);

        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        int totalDistanceToDetectives = 0;
        int currentShortestPath = 1000;
        int currentLongestPath = 0;
        for (Piece piece : state.getPlayers()) {
            if (piece.isDetective()) {
                int pathLength = dijkstra.getShortestPath(state.getSetup().graph, move.accept(getDestinationFinal), state.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
                if (pathLength < currentShortestPath) {
                    currentShortestPath = pathLength;
                } else if (pathLength > currentLongestPath) {
                    currentLongestPath = pathLength;
                }
                totalDistanceToDetectives += pathLength;
            }
        }

        int range = currentLongestPath - currentShortestPath;

        return (4 * currentShortestPath) + (3 * totalDistanceToDetectives) + (2 * getSumAvailableMoves(state)) + range;
    }

    private boolean isMrXMove(Board board) {
        return board.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    private int getSumAvailableMoves(Board.GameState state) {
        int sum = 0;
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
                    sum++;
                }
            }
        }
        return sum;
    }
}
