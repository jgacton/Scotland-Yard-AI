package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.List;

public class Pruning {
    private static final Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));

    private static List<Move> pruneExpensiveAndDuplicateMoves(Board.GameState state) {
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        List<Integer> nodesVisited = new ArrayList<>();
        List<Move> movesToPrune = new ArrayList<>(List.copyOf(state.getAvailableMoves()));
        // Gets list of unique destination nodes for all moves in this state
        for(Move move : movesToPrune) {
            boolean occupied = false;
            for(Piece piece : state.getPlayers()) {
                if(piece.isDetective()) {
                    if(state.getDetectiveLocation((Piece.Detective) piece).orElseThrow().equals(move.accept(getDestinationFinal))) occupied = true;
                }
            }

            if(!occupied && !nodesVisited.contains(move.accept(getDestinationFinal))) {
                nodesVisited.add(move.accept(getDestinationFinal));
            }
        }

        List<Move> cheapMoves = new ArrayList<>();
        // For each unique destination node, finds the move with the lowest ticket cost.
        for(int n : nodesVisited) {
            Move cheapestMove = movesToPrune.get(0);
            int cheapestCost = 100;
            for(Move move : movesToPrune) {
                if(n == move.accept(getDestinationFinal) && Evaluator.getTicketCostOfMove(move) < cheapestCost) {
                    cheapestCost = Evaluator.getTicketCostOfMove(move);
                    cheapestMove = move;
                }
            }
            cheapMoves.add(cheapestMove);
        }
        return cheapMoves;
    }

    public static List<Move> pruneMrXMoves(Board.GameState state) {
        List<Move> uniqueMoves = pruneExpensiveAndDuplicateMoves(state);
        List<Move> movesToEvaluate = new ArrayList<>(List.copyOf(uniqueMoves));
        int currentShortestDistanceToDetective = Evaluator.getShortestPathToDetective(state, uniqueMoves.get(0).source());

        for(Move move : uniqueMoves) {
            if(Evaluator.getShortestPathToDetective(state.advance(move), move.accept(getDestinationFinal)) < currentShortestDistanceToDetective) {
                movesToEvaluate.remove(move);
            }
        }

        if(movesToEvaluate.size() > 0) return movesToEvaluate;
        return uniqueMoves;
    }

    public static List<Move> pruneDetectiveMoves(Board.GameState state) {
        List<Move> uniqueMoves = pruneExpensiveAndDuplicateMoves(state);
        List<Move> movesToEvaluate = new ArrayList<>(List.copyOf(uniqueMoves));
        if(state.getMrXTravelLog().size() < 3) return uniqueMoves;
        int currentEvaluation = Evaluator.evaluateForDetective(state);
        for(Move move : uniqueMoves) {
            if(Evaluator.evaluateForDetective(state.advance(move)) > currentEvaluation) {
                movesToEvaluate.remove(move);
            }
        }

        if(movesToEvaluate.size() > 0) return movesToEvaluate;
        return uniqueMoves;
    }


}
