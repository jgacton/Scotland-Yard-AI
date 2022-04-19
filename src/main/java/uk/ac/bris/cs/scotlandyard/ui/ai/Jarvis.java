package uk.ac.bris.cs.scotlandyard.ui.ai;

        import java.util.*;
        import java.util.concurrent.Future;
        import java.util.concurrent.TimeUnit;
        import javax.annotation.Nonnull;

        import com.google.common.collect.ImmutableList;
        import io.atlassian.fugue.Pair;
        import uk.ac.bris.cs.scotlandyard.model.*;

        import java.util.concurrent.*;

public class Jarvis implements Ai {
    private final Evaluator evaluator = new Evaluator();
    private final Dijkstra dijkstra = Dijkstra.getDijkstra();

    @Nonnull @Override public String name() { return "Jarvis"; }

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        boolean mrxMove = isMrXMove(board);

        Board.GameState state = (Board.GameState) board;
        GameTree gameTree = GameTree.getTree();
        gameTree.addRootNode(state);
        ImmutableList<Move> moves = state.getAvailableMoves().asList();

        if (mrxMove) {
            Move bestMove = moves.get(new Random().nextInt(moves.size()));
            int bestEval = -1000000;
            List<Future<Integer>> evals = new ArrayList<>();
            for(Move move : moves) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                gameTree.appendGameTree(state, move, state.advance(move));
                MiniMaxCallable MM = new MiniMaxCallable(state, state.getPlayers().size(), isMrXMove(state), gameTree, -1000000, 1000000);
                evals.add(executor.submit(MM));
            }

            while(true) {
                if(evals.stream().allMatch(Future::isDone)) {
                    break;
                }
            }

            for(int i = 0; i < evals.size(); i++) {
                try {
                    if(evals.get(i).get() > bestEval) {
                        bestEval = evals.get(i).get();
                        bestMove = moves.get(i);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            //System.out.println(bestEval + " " + bestMove.toString());
            return bestMove;
        }
        List<Piece> detectives = new ArrayList<>();
        // detectives contains all the detectives who still need to go
        for(Move move : moves) {
            if(!detectives.contains(move.commencedBy())) {
                detectives.add(move.commencedBy());
            }
        }
        //System.out.println(detectives.size());
        Move bestMove = moves.get(new Random().nextInt(moves.size()));
        int bestEval = 1000000;
        for(Move move : moves) {
            gameTree.appendGameTree(state, move, state.advance(move));
            // currentEval calls with a depth such that the 0th depth will be a node of mrX turn
            int currentEval = minimaxAlphaBetaPruning(state.advance(move), detectives.size() + (state.getPlayers().size()), isMrXMove(state.advance(move)), gameTree, -1000000, 1000000);
            if(currentEval < bestEval) {
                bestEval = currentEval;
                bestMove = move;
            }
        }
        //System.out.println(bestEval + " " + bestMove.toString());
        return bestMove;
    }

    private int minimaxAlphaBetaPruning(Board.GameState node, int depth , boolean isMrX, GameTree tree, int alpha, int beta) {
        //|| !node.getWinner().isEmpty()
        if(depth == 0) {
            //System.out.println(tree.predecessors(node).isEmpty());
            System.out.println("BEFORE WE DO THE MOVE");
            System.out.println(node);
            //System.out.println(tree.nodes().contains(node));
            Move whereCameFrom = tree.getMoveWhichGenerated(node);
            System.out.println("AFTER WE DO THE MOVE");
            //System.out.println("Here");
            return evaluator.getEvaluation(node, whereCameFrom);
        }
        if(isMrX) {
            System.out.println("ERROR HAPPENS BEFORE A MR X ");
            int maxEval = -1000000;
            int i = 1;
            System.out.println(node.getAvailableMoves().size());
            for(Move move : node.getAvailableMoves()) {
                System.out.println("move number : " + i);
                i++;
                Board.GameState nextState = node.advance(move);
                // adds the next state to the game tree
                tree.appendGameTree(node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, false, tree, alpha, beta);
                if(eval > maxEval) {
                    maxEval = eval;
                }
                if(eval > alpha) {
                    alpha = eval;
                }
                if(beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        } else {
            System.out.println("ERROR HAPPENS BEFORE A DETECTIVE");
            int minEval = 1000000;
            for(Move move : node.getAvailableMoves()) {
                Board.GameState nextState = node.advance(move);
                tree.appendGameTree(node, move, nextState);
                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, isMrXMove(nextState), tree, alpha, beta);
                if(eval < minEval) {
                    minEval = eval;
                }
                if(eval < beta) {
                    beta = eval;
                }
                if(beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
    }

    private boolean isMrXMove(Board board) {
        return board.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    public void run() {

    }
}