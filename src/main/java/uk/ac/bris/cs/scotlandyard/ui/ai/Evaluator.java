package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;
import java.util.*;

public class Evaluator {

    public static int evaluateBoard(Board.GameState state, Move precedingMove) {
        if(!state.getWinner().isEmpty()) {
            if(state.getWinner().stream().anyMatch(Piece::isMrX)) {
                return 1000000;
            }
            return -1000000;
        }

        if(precedingMove.commencedBy().isMrX()) {
            return evaluateForMrX(state, precedingMove);
        } else {
            int evaluation = evaluateForDetective(state);
            System.out.println("Evaluating after detective move: " + evaluation);
            return evaluation;
        }
    }

    public static int evaluateForMrX(Board.GameState state, Move precedingMove) {
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));

        int currentShortestPath = getShortestPathToDetective(state, precedingMove.accept(getDestinationFinal));
        if(state.getSetup().moves.get(state.getMrXTravelLog().size() - 1) && currentShortestPath == 1) return -1000000;
        int totalDistanceToDetectives = getTotalDistanceToDetectives(state, precedingMove.accept(getDestinationFinal));
        int availableMoves = getSumAvailableMoves(state);

        return availableMoves + totalDistanceToDetectives + currentShortestPath;
    }

    public static int evaluateForDetective(Board.GameState state) {
        var graph = state.getSetup().graph;

        List<LogEntry> logbook = state.getMrXTravelLog().asList();

        if(logbook.size() < 3) {
            return graph.nodes().size() - state.getPlayers().size() + 1;
        } else {
            int i = logbook.size() - 1;
            while(logbook.get(i).location().isEmpty()) {
                i--;
            }
            int lastMrXLocation = logbook.get(i).location().get();

            return getTotalDistanceToDetectives(state, lastMrXLocation);
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
        List<Integer> detectiveLocations = getDetectiveLocations(state);
        for(int location : detectiveLocations) {
            int pathLength = Dijkstra.getShortestPath(state.getSetup().graph, mrXLoc, location);
            if(pathLength < currentShortestPath) currentShortestPath = pathLength;
        }
        return currentShortestPath;
    }

    public static int getTotalDistanceToDetectives(Board.GameState state, int mrXLoc) {
        int totalDistance = 0;
        List<Integer> detectiveLocations = getDetectiveLocations(state);
        for (int location : detectiveLocations) {
            totalDistance += Dijkstra.getShortestPath(state.getSetup().graph, mrXLoc, location);
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
