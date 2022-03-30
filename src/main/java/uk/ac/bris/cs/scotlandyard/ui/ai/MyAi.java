package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "Bubble :)"; }

	// Returns move to be played by the AI
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		// Gets all possible moves in the position from the current game board
		var moves = board.getAvailableMoves().asList();

		// Need to create a Player for mrX and an immutable list of players for the detectives from the list of pieces
		// supplied in the board, in order to create a gamestate object, so we can call advance() later

		Player mrx = null;
		List<Player> detectives = new ArrayList<>();
		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				detectives.add(new Player(piece, getPieceTickets(board, piece), board.getDetectiveLocation((Piece.Detective) piece).orElseThrow()));
			} else {
				int mrxLoc = 0;
				if (moves.stream().anyMatch(x -> x.commencedBy().isMrX())) {
					mrxLoc = moves.get(0).source();
				} else {
					for(int i = board.getMrXTravelLog().size()-1; i >= 0; i++) {
						if (board.getMrXTravelLog().get(i).location().isPresent()) {
							mrxLoc = board.getMrXTravelLog().get(i).location().orElseThrow();
						}
					}
				}
				mrx = new Player(piece, getPieceTickets(board, piece), mrxLoc);
			}
		}
		Board.GameState currentState = new MyGameStateFactory().build(board.getSetup(), mrx, ImmutableList.copyOf(detectives));

		// Moves contains MrX move => it's MrX's turn
		if (moves.stream().anyMatch(x -> x.commencedBy().isMrX())) {
			// Visitor pattern implementation to get destination of MrX move
			Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
			// Iterates through all players
			for (Piece piece : board.getPlayers()) {
				final int location; // Stores location of detective
				if (piece.isDetective()) {
					// If the piece is a detective get it's location
					location = board.getDetectiveLocation((Piece.Detective) piece).orElseThrow();
					// Filter out moves where the destination is the location of the detective
					// The way we have implemented getAvailableMoves, surely the moves where the destination is the location of the detective are ignored?
					moves = ImmutableList.copyOf(moves.stream().filter(x -> !x.accept(getDestinationFinal).equals(location)).toList());
				}
			}
			Move mrXFurthestAwayFromClosestDetective = moves.get(new Random().nextInt(moves.size()));
			int mrXFurthestAwayFromClosestDetectivePath = 0;
			for (Move move : moves) {
				// for each move we get the shortest distance from Mr X destination to the move
				Move currentMoveClosestDetectiveToX = moves.get(new Random().nextInt(moves.size()));
				int currentShortestPath = 1000;
				for (Piece piece : board.getPlayers()) {
					if (piece.isDetective()) {
						// find the smallest path from the destination of the move to the detective location
						int pathLength = getShortestPath(board.getSetup().graph, move.accept(getDestinationFinal), board.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
						// if the path is smaller than the current shortest path
						if (pathLength < currentShortestPath) {
							// the new currentMoveClosestDetectiveToX is the current move
							currentMoveClosestDetectiveToX = move;
							currentShortestPath = pathLength;
						}
					}
				}
				// the shortest path for a given move from Mr X destination to detective
				// stores in currentShortestPath
				// corresponding move stored in currentMoveClosestDetectiveToX
				// we want the move where Mr X is the furthest away from the detective it is closet to
				// mrXFurthestAwayFromClosestDetectivePath stores the path where Mr X is the furthest away from detective closest to
				// cSP stores the smallest distance for current move
				// if this is larger than the current lSSP
				// then we have found a move where Mr X is further away from detective closest to,
				// and so we set this to be our new move
				if(currentShortestPath > mrXFurthestAwayFromClosestDetectivePath) {
					mrXFurthestAwayFromClosestDetectivePath = currentShortestPath;
					mrXFurthestAwayFromClosestDetective = currentMoveClosestDetectiveToX;
				}
			}
			return mrXFurthestAwayFromClosestDetective;
		}
		// If it's not MrX's turn, return a random detective move
		return moves.get(new Random().nextInt(moves.size()));
	}

	private ImmutableMap<ScotlandYard.Ticket, Integer> getPieceTickets(Board board, Piece piece) {
		Map<ScotlandYard.Ticket, Integer> tickets = new HashMap<>();
		tickets.put(ScotlandYard.Ticket.DOUBLE, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.DOUBLE));
		tickets.put(ScotlandYard.Ticket.BUS, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.BUS));
		tickets.put(ScotlandYard.Ticket.SECRET, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.SECRET));
		tickets.put(ScotlandYard.Ticket.TAXI, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.TAXI));
		tickets.put(ScotlandYard.Ticket.UNDERGROUND, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.UNDERGROUND));
		return ImmutableMap.copyOf(tickets);
	}

	// Returns an integer value for the worth of a future gamestate (higher is better for MrX)
	private int evaluateBoard(Board board) {
		/*
		Need to take into account:
		- Distance from MrX to detectives
		- Tickets available to MrX
		- Tickets available to detectives
		- Connectedness of MrX location (how fast can he get far away)
		-
		 */
		return 0;
	}

	// Implements Dijkstra's algorithms to return the length of the shortest path between a source and destination node
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
}
