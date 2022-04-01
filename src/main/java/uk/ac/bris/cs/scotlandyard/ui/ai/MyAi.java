package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.stream.Collectors;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "Bubble :)"; }

	// Returns move to be played by the AI
	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		// Gets all possible moves in the position from the current game board
		var moves = board.getAvailableMoves().asList();
		boolean mrxMove = moves.stream().anyMatch(x -> x.commencedBy().isMrX());

		// Moves contains MrX move => it's MrX's turn
		if (mrxMove) {
			MutableValueGraph<Board.GameState, Move> myGameTree = gameTree(board);
			System.out.println(myGameTree.successors((Board.GameState) board));
			int bestMoveMrX = minimax((Board.GameState) board, 1, isMrXMove(board), myGameTree, board);
			System.out.println(bestMoveMrX);
			Move moveCausingBest = findingRightSuccessor((Board.GameState) board, myGameTree, board, bestMoveMrX);
			System.out.println(moveCausingBest);

			return moveCausingBest;
		}
		// If it's not MrX's turn, return a random detective move
		return moves.get(new Random().nextInt(moves.size()));
	}

	// produced a gameTree
	private MutableValueGraph<Board.GameState, Move> gameTree(Board.GameState board, int depth) {
		MutableValueGraph<Board.GameState, Move> gameTree = ValueGraphBuilder.directed().build();
		gameTree.addNode(board);


		if(depth == 0) {
			return gameTree;
		}
		for(Move move : board.getAvailableMoves()) {
			gameTree = appendGameTree(gameTree, gameTree((board.advance(move)), depth-1), move);
		}
		return gameTree;
	}

	private MutableValueGraph<Board.GameState, Move> appendGameTree(MutableValueGraph<Board.GameState, Move> parent, MutableValueGraph<Board.GameState, Move> child, Move move) {
		Board.GameState parentRoot = parent.nodes().stream().filter(x -> parent.inDegree(x) == 0).collect(Collectors.toList()).get(0);
		Board.GameState childRoot = child.nodes().stream().filter(x -> child.inDegree(x) == 0).collect(Collectors.toList()).get(0);

		for(Board.GameState state : child.nodes()) {
			parent.addNode(state);
		}
		parent.putEdgeValue(parentRoot, childRoot, move);

		for(Board.GameState state : child.nodes()) {
			if(findingPredecessors(state, child).isPresent()) {
				parent.putEdgeValue(child.predecessors(state).stream().toList().get(0), state, findingPredecessors(state, child).orElseThrow());
			}
		}

		return parent;
	}

	private int minimax(Board.GameState node, int depth , boolean isMrX, MutableValueGraph<Board.GameState, Move> tree, Board board) {
		if(depth == 0) {
			// return the shortest path from Mr X to the detective for that given move
			Move whereCameFrom = findingPredecessors(node, tree).orElseThrow();
			return evaluateBoard(board, whereCameFrom);
		}
		// select here the largest shortest path from the ones given
		if(isMrX) {
			int val = -10000000;
			depth = depth - 1;
			//Board.GameState getRandomState = tree.successors(node).stream().toList().get(0);
			System.out.println(tree.successors(node));
			for(Board.GameState state : tree.successors(node)) {
				int possibleReplacement = minimax(state, depth, false, tree, board);
				if(possibleReplacement > val) {
					val = possibleReplacement;
				}
			}
			return val;
		}
		// select here the shortest shortest path from the ones given
		else {
			int val = 10000000;
			depth = depth - 1;
			for(Board state : tree.successors(node)) {
				int possibleReplacement = minimax((Board.GameState) state, depth, true, tree, board);
				if(possibleReplacement < val) {
					val = possibleReplacement;
				}
			}
			return val;
		}
	}

	// Returns an integer value for the worth of a future game state (higher is better for MrX)
	// currently returns the shortest path from Mr X to a detective for a given move
	private int evaluateBoard(Board board, Move move) {
		/*
		We currently take into account:
		- If MrX wins in this state, large +ve eval
		- If detectives win in this state, large -ve eval
		- Else return the shortest path between MrX and a detective
		Need to take into account:
		- Tickets available to MrX (higher better, which tickets are most useful?)
		- Tickets available to detectives (lower better, again type of ticket matters)
		- Connectedness of MrX location (how fast can he get far away) (higher better, maybe score as number of nodes
		available in <= 2-4 moves)?
		- MrX should aim to be on as connected a node as possible on moves where he has to reveal himself
		 */

		if(!board.getWinner().isEmpty()) {
			if(board.getWinner().stream().anyMatch(Piece::isMrX)) {
				return 1000000;
			}
			return -1000000;
		}

		// Visitor pattern implementation to get destination of MrX move
		Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));

		// Iterates through all players
		// for each move we get the shortest distance from Mr X destination to the move
		int currentShortestPath = 1000;
		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				// find the smallest path from the destination of the move to the detective location
				int pathLength = getShortestPath(board.getSetup().graph, move.accept(getDestinationFinal), board.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
				// if the path is smaller than the current shortest path
				if (pathLength < currentShortestPath) {
					currentShortestPath = pathLength;
				}
			}
		}
		// the shortest path for a given move from Mr X destination to detective
		// stores in currentShortestPath
		return currentShortestPath;
	}

	// Implements Dijkstra's algorithms to return the length of the shortest path between a source and destination node
	private int getShortestPath(ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph, int source, int destination) {
		if(! (graph.nodes().contains(source) && graph.nodes().contains(destination))) {
			return 1000000;
		}

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

	// helper function to check if Mr X move
	private boolean isMrXMove(Board board) {
		var moves = board.getAvailableMoves().asList();
		return moves.stream().anyMatch(x -> x.commencedBy().isMrX());
	}

	private Board.GameState getStateFromBoard(Board board, boolean mrxMove) {
		var moves = board.getAvailableMoves().asList();
		Player mrx = null;
		List<Player> detectives = new ArrayList<>();
		// sets required variables to be passed into build later
		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				detectives.add(new Player(piece, getPieceTickets(board, piece), board.getDetectiveLocation((Piece.Detective) piece).orElseThrow()));
			} else {
				int mrxLoc = 1;
				if (mrxMove) {
					mrxLoc = moves.get(0).source();
				} else {
					for(int i = board.getMrXTravelLog().size()-1; i >= 0; i--) {
						if (board.getMrXTravelLog().get(i).location().isPresent()) {
							mrxLoc = board.getMrXTravelLog().get(i).location().orElseThrow();
						}
					}
				}
				mrx = new Player(piece, getPieceTickets(board, piece), mrxLoc);
			}
		}
		return new MyGameStateFactory().build(board.getSetup(), mrx, ImmutableList.copyOf(detectives));
	}

	// helper function for getStateFromBoard
	private ImmutableMap<ScotlandYard.Ticket, Integer> getPieceTickets(Board board, Piece piece) {
		Map<ScotlandYard.Ticket, Integer> tickets = new HashMap<>();
		tickets.put(ScotlandYard.Ticket.DOUBLE, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.DOUBLE));
		tickets.put(ScotlandYard.Ticket.BUS, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.BUS));
		tickets.put(ScotlandYard.Ticket.SECRET, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.SECRET));
		tickets.put(ScotlandYard.Ticket.TAXI, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.TAXI));
		tickets.put(ScotlandYard.Ticket.UNDERGROUND, board.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.UNDERGROUND));
		return ImmutableMap.copyOf(tickets);
	}

	// finds the move which resulted in the best heuristic
	private Move findingRightSuccessor(Board.GameState node, MutableValueGraph<Board.GameState, Move> tree, Board board, int mrXMax) {
		for(Board.GameState state : tree.successors(node)) {
			if((evaluateBoard(board, tree.edgeValue(node, state).orElseThrow())) == (mrXMax)) {
				return tree.edgeValue(node, state).orElseThrow();
			}
		}
		return tree.edgeValue(node, tree.successors(node).stream().toList().get(0)).orElseThrow();
	}

	// finds the move a node came from in the tree
	private Optional<Move> findingPredecessors(Board.GameState node, MutableValueGraph<Board.GameState, Move> tree) {
		Board.GameState stateCameFrom = (Board.GameState) tree.predecessors(node).stream().toList().get(0);
		return tree.edgeValue(stateCameFrom, node);
	}

}
