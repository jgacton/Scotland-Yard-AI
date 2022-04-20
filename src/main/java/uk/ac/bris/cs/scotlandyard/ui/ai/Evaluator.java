package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Evaluator {

    public static int evaluateBoard(Board.GameState state, Move move) {

        if(!state.getWinner().isEmpty()) {
            if(state.getWinner().stream().anyMatch(Piece::isMrX)) {
                return 1000000;
            }
            System.out.println("Detectives win.");
            return -1000000;
        }

        if(move.commencedBy().isMrX()) {
            Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
            int totalDistanceToDetectives = 0;
            int currentShortestPath = 1000;
            int currentLongestPath = 0;
            for (Piece piece : state.getPlayers()) {
                if (piece.isDetective()) {
                    int pathLength = Dijkstra.getShortestPath(state.getSetup().graph, move.accept(getDestinationFinal), state.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
                    if (pathLength < currentShortestPath) {
                        currentShortestPath = pathLength;
                    } else if (pathLength > currentLongestPath) {
                        currentLongestPath = pathLength;
                    }
                    totalDistanceToDetectives += pathLength;
                }
            }

            int range = currentLongestPath - currentShortestPath;

            return (4 * currentShortestPath * currentShortestPath) + (3 * totalDistanceToDetectives) + (2 * getSumAvailableMoves(state)) + range;
        } else {
            //state.getMrXTravelLog().asList().get(0).
            return 0;
        }
    }

    private static int getSumAvailableMoves(Board.GameState state) {
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

    public static int getShortestPathToDetective(Board.GameState state, int mrXLoc) {
        int currentShortestPath = 1000;
        for (Piece piece : state.getPlayers()) {
            if (piece.isDetective()) {
                int pathLength = Dijkstra.getShortestPath(state.getSetup().graph, mrXLoc, state.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
                if (pathLength < currentShortestPath) {
                    currentShortestPath = pathLength;
                }
            }
        }
        return currentShortestPath;
    }

    public static int getTicketCost(ScotlandYard.Ticket ticket) {
        return switch (ticket) {
            case TAXI -> 1;
            case BUS -> 2;
            case UNDERGROUND  -> 3;
            case SECRET -> 4;
            case DOUBLE -> 0;
        };
    }

    public static int getTicketCostOfMove(Move move) {
        int sum = 0;
        Iterator<ScotlandYard.Ticket> iterator = move.tickets().iterator();
        while(iterator.hasNext()) {
            sum += getTicketCost(iterator.next());
        }
        return sum;
    }
}
