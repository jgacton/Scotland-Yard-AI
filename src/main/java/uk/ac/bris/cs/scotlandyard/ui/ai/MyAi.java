package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {
	GameTree gameTree;

	@Nonnull @Override public String name() { return "Bubble :)"; }

	// Returns move to be played by the AI
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		Board.GameState state = (Board.GameState) board;
		List<Move> moves = pruneExpensiveAndDuplicateMoves(state);
		if(moves.size() == 1) return moves.get(0);

		Move bestMove = moves.get(new Random().nextInt(moves.size()));
		boolean isMrXMove = isMrXMove(state);

		this.gameTree = new GameTree(state);
		int bestEval = -1000000;

		for(Move moveToEvaluate : moves) {
			Board.GameState nextState = state.advance(moveToEvaluate);
			gameTree.appendGameTree(state, moveToEvaluate, nextState);

			int currentEval = minimax(nextState, state.getPlayers().size(), -1000000, 1000000, isMrXMove(nextState));

			if(isMrXMove && currentEval > bestEval) {
				bestEval = currentEval;
				bestMove = moveToEvaluate;
			} else if(!isMrXMove && currentEval < Math.abs(bestEval)) {
				bestEval = currentEval;
				bestMove = moveToEvaluate;
			}
		}
		return bestMove;
	}


	public int minimax(Board.GameState state, int depth, int alpha, int beta, boolean isMrXMove) {
		if(depth == 0 || !state.getWinner().isEmpty()) {
			Move precedingMove = this.gameTree.getMoveWhichGenerated(state);
			return Evaluator.evaluateBoard(state, precedingMove);
		}

		int bestEval = -1000000;
		List<Move> moves = pruneExpensiveAndDuplicateMoves(state);

		for(Move move : moves) {
			Board.GameState nextState = state.advance(move);
			this.gameTree.appendGameTree(state, move, nextState);
			int eval = minimax(nextState, depth - 1, alpha, beta, isMrXMove(nextState));

			if(isMrXMove) {
				if(eval > bestEval) bestEval = eval;
				if(eval > alpha) alpha = eval;
			} else {
				if(eval < Math.abs(bestEval)) bestEval = eval;
				if(eval < beta) beta = eval;
			}

			if(beta <= alpha) break;
		}

		return bestEval;
	}

	private List<Move> pruneExpensiveAndDuplicateMoves(Board.GameState state) {
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

	// helper function to check if Mr X move
	private boolean isMrXMove(Board board) {
		var moves = board.getAvailableMoves().asList();
		return moves.stream().anyMatch(x -> x.commencedBy().isMrX());
	}
}
