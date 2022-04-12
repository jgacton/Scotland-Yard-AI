package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;

import com.google.common.collect.Iterators;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

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
			MutableValueGraph<Board.GameState, Move> myGameTree = ValueGraphBuilder.directed().build();
			myGameTree.addNode((Board.GameState) board);
			System.out.println("IT IS A MR X MOVE");
			//MutableValueGraph<Board.GameState, Move> myGameTree2 = gameTreeOriginal((Board.GameState) board, board.getPlayers().size());
			//Set<Move> movesToConsider = new HashSet<>();
			//MutableValueGraph<Board.GameState, Move> myGameTree = gameTree((Board.GameState) board, 1);
			System.out.println("number of nodes in game tree is " + myGameTree.nodes().size());
			//for(Board.GameState ignored : myGameTree.successors((Board.GameState) board)) {
			//	System.out.println("SUCCESSOR EXISTS");
			//}
			int bestMoveMrX = minimaxAlphaBetaPruning((Board.GameState) board, 2, isMrXMove(board), myGameTree, board, -10000, 10000);
			System.out.println("THE BEST LOCATION IS" + bestMoveMrX);
			//System.out.println(bestMoveMrX);
			Move moveCausingBest = findingRightSuccessor((Board.GameState) board, myGameTree, board, bestMoveMrX);
			System.out.println("THE BEST MOVE CHOSEN IS" + moveCausingBest);
			//System.out.println(moveCausingBest);
			return moveCausingBest;
		}
		// If it's not MrX's turn, return a random detective move
		return moves.get(new Random().nextInt(moves.size()));
	}

	// movesSimplified contains no repetition of locations
	// chooses the optimal move based on ticket cost from a repetition of moves
	private Move simplifiedMoves(Set<Move> movesSimplified) {
		// cost is ticket cost
		int cost = 10000;
		// var is dummy variable
		int var = 0;
		// move to finalise is optimal move to take
		Move moveToFinalise = movesSimplified.stream().toList().get(0);
		// for each move in movesSimplifed
		for(Move move : movesSimplified) {
			// we go through the ticket and get a cost
			for(ScotlandYard.Ticket ticket : move.tickets()) {
				var = 0;
				var = getTicketCost(ticket) + var;
			}
			// the cheaper the cost the better it is for us
			if(var < cost) {
				cost = var;
				moveToFinalise = move;
			}
		}
		return moveToFinalise;
	}

	// produced a gameTree
	private MutableValueGraph<Board.GameState, Move> gameTree(Board.GameState board, int depth) {

		System.out.println("DEPTH OF " + depth);
		MutableValueGraph<Board.GameState, Move> gameTree = ValueGraphBuilder.directed().build();
		Set<Move> movesToConsider = new HashSet<>();
		// adds current state as root of tree
		gameTree.addNode(board);
		Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
		// ending conditions
		if(depth == 0 || !board.getWinner().isEmpty()) {
			return gameTree;
		}
		// movesToConsider contains moves with no repetition of location
		// sameDestFinal contains moves with the same end location
		// go through each move in all available moves
		System.out.println("THE ALL MOVES ARE " + board.getAvailableMoves());
		System.out.println(board.getAvailableMoves().size());
		Set<Move> initialMoves = board.getAvailableMoves();
		for(Move move : initialMoves) {
			System.out.println("THE MOVE IS " + move);
			int destinationFinal = move.accept(getDestinationFinal);
			System.out.println("THE DESTINATION FINAL IS " + destinationFinal);
			// if this moves end destination is not in movesToConsider then this end location has not been considered
			if(movesToConsider.stream().noneMatch(x -> x.accept(getDestinationFinal).equals(destinationFinal))) {
				System.out.println("WE ARE IN LOCATION REQUIRED");
				// we add all moves with this same end location to sameDestFinal
				/* for(Move moveToCompare : board.getAvailableMoves()) {
					if(moveToCompare.accept(getDestinationFinal) == (destinationFinal)) {
						System.out.println("The comparison move is " + moveToCompare);
						System.out.println("The comparison move loc is " + moveToCompare.accept(getDestinationFinal));
						System.out.println("The dest final is : " + destinationFinal);
						sameDestFinal.add(moveToCompare);
					}
				} */
				Set<Move> sameDestFinal = board.getAvailableMoves().stream().filter(x -> x.accept(getDestinationFinal).equals(destinationFinal)).collect(Collectors.toSet());
				initialMoves =  initialMoves.stream().filter(x -> !sameDestFinal.contains(x)).collect(Collectors.toSet());
				System.out.println("SAME DEST FINAL IS " + sameDestFinal);
				// we choose the optimal move to take
				Move rightMove = simplifiedMoves(sameDestFinal);
				System.out.println("THE RIGHT MOVE IS " + rightMove);
				// we add this to movesToConsider
				movesToConsider.add(rightMove);
			}
		}
		// now for each move in moves to consider
		// we go through the game tree repeating the process
		System.out.println("THE MOVES TO CONSIDER ARE " + movesToConsider);
		for(Move move : movesToConsider) {
			System.out.println("the moves are as follows : " + move);
			appendGameTree(gameTree, gameTree((board.advance(move)), depth - 1), move);
		}
		return gameTree;
	}

	private MutableValueGraph<Board.GameState, Move> gameTreeOriginal(Board.GameState board, int depth) {
		MutableValueGraph<Board.GameState, Move> gameTree = ValueGraphBuilder.directed().build();
		gameTree.addNode(board);


		if(depth == 0) {
			return gameTree;
		}
		for(Move move : board.getAvailableMoves()) {
			appendGameTree(gameTree, gameTreeOriginal((board.advance(move)), depth - 1), move);
		}
		return gameTree;
	}

	private void appendGameTree(MutableValueGraph<Board.GameState, Move> parent, MutableValueGraph<Board.GameState, Move> child, Move move) {
		Board.GameState parentRoot = parent.nodes().stream().filter(x -> parent.inDegree(x) == 0).toList().get(0);
		Board.GameState childRoot = child.nodes().stream().filter(x -> child.inDegree(x) == 0).toList().get(0);

		for(Board.GameState state : child.nodes()) {
			parent.addNode(state);
		}
		parent.putEdgeValue(parentRoot, childRoot, move);

		for(Board.GameState state : child.nodes()) {
			if(findingPredecessors(state, child).isPresent()) {
				parent.putEdgeValue(child.predecessors(state).stream().toList().get(0), state, findingPredecessors(state, child).orElseThrow());
			}
		}
	}

	private int minimaxAlphaBetaPruning(Board.GameState node, int depth , boolean isMrX, MutableValueGraph<Board.GameState, Move> tree, Board board, int alpha, int beta) {
		if(depth == 0) {
			// return the shortest path from Mr X to the detective for that given move
			Move whereCameFrom = findingPredecessors(node, tree).orElseThrow();
			return evaluateBoard(board, whereCameFrom);
		}
		for(Move move : node.getAvailableMoves()) {
			appendGameTree(tree, gameTreeOriginal(node.advance(move), 1), move);
		}
		System.out.println("is empty is : " + tree.successors(node).isEmpty());
		// select here the largest shortest path from the ones given
		if(isMrX) {
			int val = -10000000;
			depth = depth - 1;
			System.out.println("depth is : " + depth);
			//Board.GameState getRandomState = tree.successors(node).stream().toList().get(0);
			for(Board.GameState state : tree.successors(node)) {
				int possibleReplacement = minimaxAlphaBetaPruning(state, depth, false, tree, board, alpha, beta);
				if(possibleReplacement > val) {
					val = possibleReplacement;
				}
				if(possibleReplacement > alpha) {
					alpha = possibleReplacement;
				}
				if(beta <= alpha) {
					break;
				}

			}
			return val;
		}
		// select here the minimum shortest path from the ones given
		else {
			int val = 10000000;
			depth = depth - 1;
			for(Board.GameState state : tree.successors(node)) {
				int possibleReplacement = minimaxAlphaBetaPruning(state, depth, state.getAvailableMoves().stream().anyMatch(x -> x.commencedBy().isMrX()), tree, board, alpha, beta);
				if(possibleReplacement < val) {
					val = possibleReplacement;
				}
				if(possibleReplacement <= beta) {
					beta = possibleReplacement;
				}
				if(beta <= alpha) {
					break;
				}
			}
			return val;
		}
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
			//System.out.println(tree.successors(node));
			for(Board.GameState state : tree.successors(node)) {
				int possibleReplacement = minimax(state, depth, false, tree, board);
				if(possibleReplacement > val) {
					val = possibleReplacement;
				}
			}
			return val;
		}
		// select here the minimum shortest path from the ones given
		else {
			int val = 10000000;
			depth = depth - 1;
			for(Board.GameState state : tree.successors(node)) {
				int possibleReplacement = minimax(state, depth, state.getAvailableMoves().stream().anyMatch(x -> x.commencedBy().isMrX()), tree, board);
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
		int totalDistanceToDetectives = 0;

		// Iterates through all players
		// for each move we get the shortest distance from Mr X destination to the move
		int currentShortestPath = 1000;
		int currentLongestPath = 0;
		for (Piece piece : board.getPlayers()) {
			if (piece.isDetective()) {
				// find the smallest path from the destination of the move to the detective location
				int pathLength = getShortestPath(board.getSetup().graph, move.accept(getDestinationFinal), board.getDetectiveLocation((Piece.Detective) piece).orElseThrow());
				// If shortest path to this detective shorter than paths to previous detectives, update shortest path.
				// Similarly for longest path.
				if (pathLength < currentShortestPath) {
					currentShortestPath = pathLength;
				} else if (pathLength > currentLongestPath) {
					currentLongestPath = pathLength;
				}

				// Increment total distance to all detectives with shortest path to this detective.
				totalDistanceToDetectives += pathLength;
			}
		}

		/*
		if(move.commencedBy().isMrX()) {
			return currentShortestPath + nOrderNeighbours(board, move.accept(getDestinationFinal), 2);
		}
		// the shortest path for a given move from Mr X destination to detective
		// stores in currentShortestPath
		for(ScotlandYard.Ticket ticket : move.tickets()) {
			totalDistanceToDetectives -= getTicketCost(ticket);
		}
		*/

		// Return total distance to all detectives, subtracting the range between the closest and furthest detectives away.
		return totalDistanceToDetectives - (currentLongestPath - currentShortestPath);
	}

	private int getTicketCost(ScotlandYard.Ticket ticket) {
		return switch (ticket) {
			case TAXI -> 1;
			case BUS -> 3;
			case UNDERGROUND  -> 3;
			case SECRET -> 2;
			case DOUBLE -> 0;
		};
	}

	// Helper function which gets the number of neighbours of order n of a given node on a given board.
	// E.g. n = 1 gives the number of immediate neighbours, n = 2 gets the number of immediate neighbours + neighbours
	// of neighbours.
	private int nOrderNeighbours(Board board, int node, int n) {
		int numOfNeighbours = 1;

		if(n == 0) {
			return numOfNeighbours;
		}

		for(int neighbour : board.getSetup().graph.adjacentNodes(node)) {
			numOfNeighbours += nOrderNeighbours(board, neighbour, n-1);
		}

		return numOfNeighbours;
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
		if(tree.predecessors(node).isEmpty()) return Optional.empty();
		Board.GameState stateCameFrom = tree.predecessors(node).stream().toList().get(0);
		return tree.edgeValue(stateCameFrom, node);
	}
}
