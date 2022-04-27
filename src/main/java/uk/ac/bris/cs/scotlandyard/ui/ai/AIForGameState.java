package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class AIForGameState implements  Ai{

    MyGameStateForAI currentState;
    @Nonnull
    @Override
    public String name() {
        return "potato";
    }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        Board.GameState state = (Board.GameState) board;
        if(this.currentState == null) {
            this.currentState = new MyGameStateForAI(state, state.getAvailableMoves().asList().get(0).source());
        }

        //Board.GameState currentStateTest = new MyGameStateForAI((Board.GameState) board);
        //MyGameStateForAI currentState = new MyGameStateForAI((Board.GameState) board);
        Move bestMove = currentState.getAvailableMoves().asList().get(new Random().nextInt(currentState.getAvailableMoves().size()));
        boolean isMrXMove = isMrXMove(currentState);
        System.out.println(isMrXMove);

        List<Move> moves = pruneExpensiveMoves();
        if(isMrXMove) {
            moves = pruneMoves();

            if(moves.size() == 1) return moves.get(0);
            int bestEval = -1000000;
            for(Move move : moves) {
                currentState = currentState.advance(move);
                int currentEval = minimax(currentState.getPlayers().size(), -1000000, 1000000, isMrXMove(currentState));
                if(currentEval > bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
                currentState = currentState.revert();
            }
        } else {
            List<Piece> detectives = new ArrayList<>();
            for(Move move : currentState.getAvailableMoves()) {
                if(!detectives.contains(move.commencedBy())) {
                    detectives.add(move.commencedBy());
                }
            }
            int bestEval = 1000000;
            //List<Move> moves = pruneExpensiveMoves(state);
            for(Move move : moves) {
                currentState = currentState.advance(move);

                int currentEval = minimax((currentState.getPlayers().size()) + detectives.size(), -1000000, 1000000, isMrXMove(currentState));

                if(currentEval < bestEval) {
                    bestEval = currentEval;
                    bestMove = move;
                }
                currentState = currentState.revert();
            }
        }
        currentState = currentState.advance(bestMove);
        return bestMove;
    }

    private boolean isMrXMove(MyGameStateForAI state) {
        return state.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    private List<Move> pruneExpensiveMoves() {
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        List<Integer> nodesVisited = new ArrayList<>();
        for(Move move : currentState.getAvailableMoves()) {
            boolean occupied = false;
            for(Piece piece : currentState.getPlayers()) {
                if(piece.isDetective()) {
                    if(currentState.getDetectiveLocation((Piece.Detective) piece).orElseThrow().equals(move.accept(getDestinationFinal))) occupied = true;
                }
            }

            if(!occupied) {
                if(!nodesVisited.contains(move.accept(getDestinationFinal))) {
                    nodesVisited.add(move.accept(getDestinationFinal));
                }
            }
        }

        List<Move> moves = currentState.getAvailableMoves().asList();
        List<Move> cheapMoves = new ArrayList<>();
        for(int n : nodesVisited) {
            Move cheapestMove = moves.get(0);
            int cheapestCost = 10;
            for(Move move : moves) {
                if(n == move.accept(getDestinationFinal) && Evaluator.getTicketCostOfMove(move) < cheapestCost) {
                    cheapestCost = Evaluator.getTicketCostOfMove(move);
                    cheapestMove = move;
                }
            }
            cheapMoves.add(cheapestMove);
        }
        return cheapMoves;
    }

    private List<Move> pruneMoves() {
        List<Move> cheapMoves = pruneExpensiveMoves();

        int currentDistance = Evaluator.getShortestPathToDetective(currentState, currentState.getAvailableMoves().asList().get(0).source());
        int currentTotalDistance = Evaluator.getTotalDistanceToDetectives(currentState, currentState.getAvailableMoves().asList().get(0).source());

        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        List<Move> prunedMoves = new ArrayList<>(List.copyOf(cheapMoves));

        for(Move move : cheapMoves) {

            this.currentState = currentState.advance(move);
            int nextDistance = Evaluator.getShortestPathToDetective(currentState, move.accept(getDestinationFinal));

            if(nextDistance < currentDistance) {
                prunedMoves.remove(move);
            } else {
                int nextTotalDistance = Evaluator.getTotalDistanceToDetectives(currentState, move.accept(getDestinationFinal));

                if(nextTotalDistance < currentTotalDistance) {
                    prunedMoves.remove(move);
                }
            }
            this.currentState = currentState.revert();
        }

        if(prunedMoves.size() == 0) return cheapMoves;

        return prunedMoves;
    }

    public int minimax(int depth, int alpha, int beta, boolean isMrXMove) {
        if(depth == 0 || !currentState.getWinner().isEmpty()) {
            return Evaluator.evaluateBoard(currentState, currentState.getMoveCameFrom());
        }

        int bestEval = -1000000;
        List<Move> moves = pruneExpensiveMoves();

        for(Move move : moves) {
            currentState = currentState.advance(move);
            int eval = minimax(depth - 1, alpha, beta, isMrXMove(currentState));
            if(isMrXMove) {
                if(eval > bestEval) bestEval = eval;
                if(eval > alpha) alpha = eval;
            } else {
                if(eval < Math.abs(bestEval)) bestEval = eval;
                if(eval < beta) beta = eval;
            }
             currentState = currentState.revert();
            if(beta <= alpha) break;
        }
        return bestEval;
    }
}
