package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.List;

public class Evaluator {
    Dijkstra dijkstra = Dijkstra.getDijkstra();

    public int getEvaluation(Board.GameState state, Move move) {
        return evaluateBoard(state, move);
    }

    private int evaluateBoard(Board.GameState state, Move move) {
        //if(move.commencedBy().isDetective()) System.out.println("Evaluating after detective move");

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

        int evaluation = (4 * currentShortestPath) + (3 * totalDistanceToDetectives) + (2 * getSumAvailableMoves(state)) + range;

        //System.out.println("Board evaluation is: " + evaluation);

        return evaluation;
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
