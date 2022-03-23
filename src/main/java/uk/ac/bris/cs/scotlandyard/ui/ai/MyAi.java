package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;


import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;
import java.util.*;

public class MyAi implements Ai {
	//private ImmutableValueGraph<Integer, Float> gameTree;

	@Nonnull @Override public String name() { return "Bubble :)"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();
		if(moves.stream().anyMatch(x -> x.commencedBy().isMrX())) {
			System.out.println("it is Mr X");
			Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
			List<Move> goodMoves = new ArrayList<>();
			for(Piece piece : board.getPlayers()) {
				final int location;
				if(piece.isDetective()) {
					location = board.getDetectiveLocation((Piece.Detective) piece).orElseThrow();
					goodMoves.addAll(moves.stream().filter(x -> !x.accept(getDestinationFinal).equals(location)).toList());
				}
			}
			System.out.println(goodMoves);
			return goodMoves.get(new Random().nextInt(goodMoves.size()));
		} else {
			return moves.get(new Random().nextInt(moves.size()));
		}
	}

	/*private ImmutableValueGraph<Integer, Float> generateGameTree() {
		return null;
	}

	private Float miniMax(Integer node, int depth, boolean maximisingPlayer) {
		if(depth == 0 || !currentBoard.getWinner().isEmpty()) {
			return getHeuristicValue();
		}
		return null;
	}

	private Float getHeuristicValue(Move move) {

		Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
		int destinationFinal = move.accept(getDestinationFinal);

		for(Piece player : this.currentBoard.getPlayers()) {
			if(player.isDetective()) {

			}
		}

		return null;
	}*/
}

/* How deep to go in game tree, can figure out average number of connections of nodes on game board */