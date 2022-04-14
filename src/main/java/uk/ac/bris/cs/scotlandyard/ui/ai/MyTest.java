package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyTest implements Ai {

    @Nonnull @Override public String name() { return "Jarvis"; }

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        boolean mrxMove = isMrXMove(board);

        Board.GameState state = (Board.GameState) board;
        MutableValueGraph<Board.GameState, Move> gameTree = gameTreeOfNode(state);
        ImmutableList<Move> moves = state.getAvailableMoves().asList();

        if (mrxMove) {
            Move bestMove = moves.get(new Random().nextInt(moves.size()));
            int bestEval = -1000000;
            for(Move move : moves) {
                appendGameTree(gameTree, state, move, state.advance(move));
                int currentEval = minimaxAlphaBetaPruning(state.advance(move), state.getPlayers().size(), true, gameTree, -1000000, 100000);
                if(currentEval > bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
            }
            System.out.println(bestEval + " " + bestMove.toString());
            return bestMove;
        }
        List<Piece> detectives = new ArrayList<>();
        for(Move move : moves) {
            if(!detectives.contains(move.commencedBy())) {
                detectives.add(move.commencedBy());
            }
        }
        System.out.println(detectives.size());
        Move bestMove = moves.get(new Random().nextInt(moves.size()));
        int bestEval = 1000000;
        for(Move move : moves) {
            appendGameTree(gameTree, state, move, state.advance(move));
            int currentEval = minimaxAlphaBetaPruning(state.advance(move), detectives.size() + (state.getPlayers().size()), false, gameTree, -1000000, 1000000);
            if(currentEval < bestEval) {
                bestEval = currentEval;
                bestMove = move;
            }
        }
        System.out.println(bestEval + " " + bestMove.toString());
        return bestMove;
    }

    private MutableValueGraph<Board.GameState, Move> gameTreeOfNode(Board.GameState state) {
        MutableValueGraph<Board.GameState, Move> gameState = ValueGraphBuilder.directed().build();
        gameState.addNode(state);
        return gameState;
    }

    private void appendGameTree(MutableValueGraph<Board.GameState, Move> parentTree, Board.GameState parentNode, Move move, Board.GameState childNode) {
        parentTree.addNode(childNode);
        parentTree.putEdgeValue(parentNode, childNode, move);
    }

    private int minimaxAlphaBetaPruning(Board.GameState node, int depth , boolean isMrX, MutableValueGraph<Board.GameState, Move> tree, int alpha, int beta) {
        if(depth == 0 || !node.getWinner().isEmpty()) {
            Move whereCameFrom = tree.edgeValue(tree.predecessors(node).stream().toList().get(0), node).orElseThrow();
            System.out.println("Here");
            return evaluateBoard(node, whereCameFrom);
        }
        if(isMrX) {
            int maxEval = -1000000;
            for(Move move : node.getAvailableMoves()) {
                Board.GameState nextState = node.advance(move);
                appendGameTree(tree, node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, false, tree, alpha, beta);
                if(eval > maxEval) {
                    maxEval = eval;
                }
                if(eval > alpha) {
                    alpha = eval;
                }
                if(beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        } else {
            int minEval = 1000000;
            for(Move move : node.getAvailableMoves()) {
                Board.GameState nextState = node.advance(move);
                appendGameTree(tree, node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, isMrXMove(nextState), tree, alpha, beta);
                if(eval < minEval) {
                    minEval = eval;
                }
                if(eval < beta) {
                    beta = eval;
                }
                if(beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
        /*
        int val = -1000000;
        for(Move move : node.getAvailableMoves()) {
            Board.GameState nextState = node.advance(move);
            appendGameTree(tree, node, move, nextState);

            if(isMrX) {
                int possibleReplacement = minimaxAlphaBetaPruning(nextState, depth - 1, false, tree, alpha, beta).orElseThrow();
                if(possibleReplacement > val) {
                    val = possibleReplacement;
                }
                if(possibleReplacement > alpha) {
                    alpha = possibleReplacement;
                }
            } else {
                val = Math.abs(val);
                int possibleReplacement = minimaxAlphaBetaPruning(nextState, depth - 1, isMrXMove(nextState), tree, alpha, beta).orElseThrow();
                if(possibleReplacement < val) {
                    val = possibleReplacement;
                }
                if(possibleReplacement < beta) {
                    beta = possibleReplacement;
                }
            }
            if(beta <= alpha) {
                break;
            }
        }
        if(beta <= alpha) {
            return Optional.empty();
        }
        return Optional.of(val);

         */
    }

    private int evaluateBoard(Board.GameState state, Move move) {
        if(move.commencedBy().isDetective()) System.out.println("Evaluating after detective move");

        if(!state.getWinner().isEmpty()) {
            if(state.getWinner().stream().anyMatch(Piece::isMrX)) {
                return 1000000;
            }
            return -1000000;
        }

        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        int totalDistanceToDetectives = 0;
        int currentShortestPath = 1000;
        int currentLongestPath = 0;
        for (Piece piece : state.getPlayers()) {
            if (piece.isDetective()) {
                int pathLength = getShortestPath(state.getSetup().graph, move.accept(getDestinationFinal), state.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
                if (pathLength < currentShortestPath) {
                    currentShortestPath = pathLength;
                } else if (pathLength > currentLongestPath) {
                    currentLongestPath = pathLength;
                }
                totalDistanceToDetectives += pathLength;
            }
        }

        int range = currentLongestPath - currentShortestPath;

        int evaluation = (4 * currentShortestPath) + (3 * totalDistanceToDetectives) + (2 * getSumAvailableMoves(state)) + range;

        //System.out.println("Board evaluation is: " + evaluation);

        return evaluation;
    }

    private int getShortestPath(ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph, int source, int destination) {
        if(! (graph.nodes().contains(source) && graph.nodes().contains(destination))) {
            return 1000000;
        }

        Set<Integer> Q = new HashSet<>();
        Integer[] dist = new Integer[199];
        Integer[] prev = new Integer[199];

        for(Integer N : graph.nodes()) {
            dist[N-1] = 1000000;
            prev[N-1] = null;
            Q.add(N);
        }

        dist[source - 1] = 0;

        while(!Q.isEmpty()) {

            int minDist = 1000000;
            int u = 0;
            for(Integer N : Q) {
                if(dist[N-1] < minDist) {
                    minDist = dist[N-1];
                    u = N;
                }
            }

            Q.remove(u);

            for(Integer v : graph.adjacentNodes(u)) {
                if(Q.contains(v)) {
                    int edgeVal = 0;
                    if(!graph.edgeValue(u, v).orElseThrow().isEmpty()) {
                        edgeVal = 1;
                    }
                    int alt = dist[u-1] + edgeVal;
                    if(alt < dist[v-1]) {
                        dist[v-1] = alt;
                        prev[v-1] = u;
                    }
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        path.add(destination);
        while(destination != source) {
            path.add(prev[destination-1]);
            destination = prev[destination-1];
        }

        Collections.reverse(path);

        return(path.size() - 1);
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
