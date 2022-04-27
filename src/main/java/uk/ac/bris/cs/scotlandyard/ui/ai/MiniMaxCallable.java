package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MiniMaxCallable implements Callable<Integer> {
    private final Board.GameState state;
    private final int depth;
    private final boolean isMrX;
    private final GameTree tree;
    private final int alpha;
    private final int beta;

    public MiniMaxCallable(Board.GameState state, int depth, boolean isMrX, GameTree tree, int alpha, int beta) {
        this.state = state;
        this.depth = depth;
        this.isMrX = isMrX;
        this.tree = tree;
        this.alpha = alpha;
        this.beta = beta;
    }

    private int minimaxAlphaBetaPruning(Board.GameState state, int depth, GameTree tree, boolean isMrX, int alpha, int beta) {
        if(depth == 0) {
            Move whereCameFrom = tree.getMoveWhichGenerated(state);
            return Evaluator.evaluateBoard(state, whereCameFrom);
        }
        if(isMrX) {
            int maxEval = -1000000;
            for(Move move : state.getAvailableMoves()) {
                Board.GameState nextState = state.advance(move);
                tree.appendGameTree(state, move, nextState);

                int eval = minimaxAlphaBetaPruning(nextState, depth - 1, tree,  false, alpha, beta);

                if(eval > maxEval) maxEval = eval;
                if(eval > alpha) alpha = eval;
                if(beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = 1000000;
            int numOfDetectives = state.getPlayers().size() - 1;
            int firstLevelMoves = state.getAvailableMoves().size();
            int i = 0;
            while(i < firstLevelMoves) {
                Board.GameState dOneState = state.advance(state.getAvailableMoves().stream().toList().get(i));
                tree.appendGameTree(state, state.getAvailableMoves().stream().toList().get(i), dOneState);
                if(numOfDetectives >= 2) {
                    int secondLevelMoves = dOneState.getAvailableMoves().size();
                    int j = 0;
                    while(j < secondLevelMoves) {
                        Board.GameState dTwoState = dOneState.advance(dOneState.getAvailableMoves().stream().toList().get(j));
                        tree.appendGameTree(dOneState, state.getAvailableMoves().stream().toList().get(i), dTwoState);
                        if(numOfDetectives >= 3) {
                            int thirdLevelMoves = dTwoState.getAvailableMoves().size();
                            int k = 0;
                            while(k < thirdLevelMoves) {
                                Board.GameState dThreeState = dTwoState.advance(dTwoState.getAvailableMoves().stream().toList().get(k));
                                tree.appendGameTree(dTwoState, state.getAvailableMoves().stream().toList().get(i), dThreeState);
                                if(numOfDetectives >= 4) {
                                    int fourthLevelMoves = dThreeState.getAvailableMoves().size();
                                    int l = 0;
                                    while(l < fourthLevelMoves) {
                                        Board.GameState dFourState = dThreeState.advance(dThreeState.getAvailableMoves().stream().toList().get(l));
                                        tree.appendGameTree(dThreeState, state.getAvailableMoves().stream().toList().get(i), dFourState);
                                        if(numOfDetectives >= 5) {
                                            int fifthLevelMoves = dFourState.getAvailableMoves().size();
                                            int m = 0;
                                            while(m < fifthLevelMoves) {
                                                Board.GameState dFiveState = dFourState.advance(dFourState.getAvailableMoves().stream().toList().get(m));
                                                tree.appendGameTree(dFourState, state.getAvailableMoves().stream().toList().get(i), dFiveState);
                                                int eval = minimaxAlphaBetaPruning(dFiveState, depth - 1, tree, isMrXMove(dFiveState), alpha, beta);
                                                if(eval < minEval) minEval = eval;
                                                if(eval < beta) beta = eval;
                                                if(beta <= alpha) break;
                                                m++;
                                            }
                                        } else {
                                            int eval = minimaxAlphaBetaPruning(dFourState, depth - 1, tree, isMrXMove(dFourState), alpha, beta);
                                            if(eval < minEval) minEval = eval;
                                            if(eval < beta) beta = eval;
                                            if(beta <= alpha) break;
                                        }
                                        if(beta <= alpha) break;
                                        l++;
                                    }
                                } else {
                                    int eval = minimaxAlphaBetaPruning(dThreeState, depth - 1, tree, isMrXMove(dThreeState), alpha, beta);
                                    if(eval < minEval) minEval = eval;
                                    if(eval < beta) beta = eval;
                                    if(beta <= alpha) break;
                                }
                                if(beta <= alpha) break;
                                k++;
                            }
                        } else {
                            int eval = minimaxAlphaBetaPruning(dTwoState, depth - 1, tree, isMrXMove(dTwoState), alpha, beta);
                            if(eval < minEval) minEval = eval;
                            if(eval < beta) beta = eval;
                            if(beta <= alpha) break;
                        }
                        if(beta <= alpha) break;
                        j++;
                    }
                } else {
                    int eval = minimaxAlphaBetaPruning(dOneState, depth - 1, tree, isMrXMove(dOneState), alpha, beta);
                    if(eval < minEval) minEval = eval;
                    if(eval < beta) beta = eval;
                    if(beta <= alpha) break;
                }
                if(beta <= alpha) break;
                i++;
            }
            return minEval;
        }
    }

    private List<Move> pruneExpensiveMoves(Board.GameState state) {
        List<Move> moves = state.getAvailableMoves().asList();
        List<Move> cheapMoves = new ArrayList<>(List.copyOf(moves));

        for(int i = 0; i < moves.size() - 1; i++) {
            Move move1 = moves.get(i);
            for(int j = i + 1; j < moves.size(); j++) {
                Move move2 = moves.get(j);

                if(Evaluator.getTicketCostOfMove(move1) <= Evaluator.getTicketCostOfMove(move2)) {
                    cheapMoves.remove(move2);
                }
            }
        }

        return cheapMoves;
    }

    private boolean isMrXMove(Board board) {
        return board.getAvailableMoves().asList().stream().anyMatch(x -> x.commencedBy().isMrX());
    }

    @Override
    public Integer call() {
        return minimaxAlphaBetaPruning(this.state, this.depth, this.tree, this.isMrX, this.alpha, this.beta);
    }
}