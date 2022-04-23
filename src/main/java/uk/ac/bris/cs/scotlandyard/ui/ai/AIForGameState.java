package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class AIForGameState implements  Ai{

    @Nonnull
    @Override
    public String name() {
        return "potato";
    }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        MyGameStateForAI currentState = new MyGameStateForAI((Board.GameState) board);
        Move bestMove = currentState.getAvailableMoves().asList().get(new Random().nextInt(currentState.getAvailableMoves().size()));
        return bestMove;

    }
}
