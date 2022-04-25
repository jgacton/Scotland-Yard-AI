package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Evaluator {

    public static int evaluateBoard(MyGameStateForAI state, Move move) {
        if(!state.getWinner().isEmpty()) {
            if(state.getWinner().stream().anyMatch(Piece::isMrX)) {
                return 1000000;
            }
            return -1000000;
        }

        if(move.commencedBy().isMrX()) {
            return evaluateForMrX(state, move);
        } else {
            return evaluateForDetective(state);
        }
    }

    public static int evaluateForMrX(MyGameStateForAI state, Move move) {
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));

        int totalDistanceToDetectives = getTotalDistanceToDetectives(state, move.accept(getDestinationFinal));
        int currentShortestPath = getShortestPathToDetective(state, move.accept(getDestinationFinal));
        int availableMoves = getSumAvailableMoves(state);

        return (2 * availableMoves * availableMoves) + totalDistanceToDetectives + currentShortestPath;
    }

    public static int evaluateForDetective(MyGameStateForAI state) {
        var graph = state.getSetup().graph;

        List<LogEntry> logbook = state.getMrXTravelLog().asList();

        if(logbook.size() < 3) {
            return graph.nodes().size() - state.getPlayers().size() + 1;
        } else {
            int sumShortestDistances = 0;
            List<Integer> possibleMrXLocations = state.getPossibleMrXLocations();

            for(int mrXLoc : possibleMrXLocations) {
                sumShortestDistances += getShortestPathToDetective(state, mrXLoc);
            }

            return sumShortestDistances;
        }
    }

    public static int getSumAvailableMoves(Board.GameState state) {
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

    public static int getTotalDistanceToDetectives(Board.GameState state, int mrXLoc) {
        int totalDistance = 0;
        for (Piece piece : state.getPlayers()) {
            if (piece.isDetective()) {
                totalDistance += Dijkstra.getShortestPath(state.getSetup().graph, mrXLoc, state.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
            }
        }
        return totalDistance;
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
        for (ScotlandYard.Ticket ticket : move.tickets()) {
            sum += getTicketCost(ticket);
        }
        return sum;
    }

    private static List<Integer> getDetectiveLocations(Board.GameState state) {
        List<Integer> detectiveLocations = new ArrayList<>();
        for(Piece piece : state.getPlayers()) {
            if(piece.isDetective()) {
                detectiveLocations.add(state.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
            }
        }
        return detectiveLocations;
    }
}
