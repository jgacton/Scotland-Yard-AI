package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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

	// Returns move to be played by the AI
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		// Gets all possible moves in the position from the current game board
		var moves = board.getAvailableMoves().asList();

		// Any move commenced by MrX => it's MrX's turn
		if(moves.stream().anyMatch(x -> x.commencedBy().isMrX())) {

			// Visitor pattern implementation to get destination of MrX move
			Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
			// Iterates through all players
			for(Piece piece : board.getPlayers()) {

				final int location; // Stores location of detective
				if(piece.isDetective()) {
					// If the piece is a detective
					location = board.getDetectiveLocation((Piece.Detective) piece).orElseThrow();
					// Filter out moves where the destination is the location of the detective
					moves = ImmutableList.copyOf(moves.stream().filter(x -> !x.accept(getDestinationFinal).equals(location)).toList());
				}
			}
			System.out.println(getShortestPath(board.getSetup().graph, 21, 77));
		}
		return moves.get(new Random().nextInt(moves.size()));
	}

	private int getShortestPath(ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph, int source, int destination) {

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