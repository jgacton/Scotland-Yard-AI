package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

public class BackTracking {
    private final Move moveCameFrom;

    public BackTracking(Board.GameState stateCameFrom, Move moveCameFrom, Board.GameState currentState) {
        this.moveCameFrom = moveCameFrom;
    }

    public Move getMoveCameFrom() {
        return moveCameFrom;
    }
}
