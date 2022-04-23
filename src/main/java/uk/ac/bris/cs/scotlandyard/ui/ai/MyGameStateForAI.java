package uk.ac.bris.cs.scotlandyard.ui.ai;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyGameStateForAI implements Board.GameState {
    private final GameSetup setup;
    private final ImmutableSet<Piece> remaining;
    private final ImmutableList<LogEntry> log;
    private final Player mrX;
    private final List<Player> detectives;
    private ImmutableSet<Move> moves;
    // state came from and move came from stores the preceding state and move which the current state came from
    private final GameState stateCameFrom;
    private final Move moveCameFrom;
    public MyGameStateForAI(Board.GameState currentState) {
        this.setup = currentState.getSetup();
        this.log = currentState.getMrXTravelLog();
        this.moves = currentState.getAvailableMoves();
        this.stateCameFrom = null;
        this.moveCameFrom = null;
        List<Player> copyOfDet = new ArrayList<>();
        List<Piece> onlyDetPieces = currentState.getPlayers().stream().filter(Piece::isDetective).collect(Collectors.toList());
        Piece mrXPiece = currentState.getPlayers().stream().filter(Piece::isMrX).collect(Collectors.toList()).get(0);;
        int mrXLoc = helpMrXLoc(currentState);
        ImmutableMap<ScotlandYard.Ticket, Integer> ticketsMrX = helpTickets(currentState, mrXPiece);
        this.mrX = new Player(mrXPiece, ticketsMrX, mrXLoc);
        for(Piece detPieces : onlyDetPieces) {
            System.out.println("before disaster");
            System.out.println("potato");
            System.out.println("potato2");
            System.out.println("after disaster");
            Player det = new Player(detPieces, helpTickets(currentState, detPieces), currentState.getDetectiveLocation((Piece.Detective) detPieces).orElseThrow());
            copyOfDet.add(det);
        }
        this.detectives = copyOfDet;
        this.remaining = findRemaining(currentState);
    }

    private ImmutableSet<Piece> findRemaining(Board.GameState currentState) {
        Set<Piece> copyRemaining = new HashSet<>();
        if(currentState.getAvailableMoves().stream().anyMatch(x -> x.commencedBy().isMrX())) {
            copyRemaining.add(this.mrX.piece());
        }
        for(Player detective : this.detectives) {
            if(currentState.getAvailableMoves().stream().anyMatch(x -> x.commencedBy().equals(detective.piece()))) {
                copyRemaining.add(detective.piece());
            }
        }
        return ImmutableSet.copyOf(copyRemaining);

    }
    private ImmutableMap<ScotlandYard.Ticket, Integer> helpTickets(Board.GameState currentState, Piece piece) {
        Map<ScotlandYard.Ticket, Integer> tickets = new HashMap<>();
        int numberBuses = currentState.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.BUS);
        int numberTaxi = currentState.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.TAXI);
        int numberUnderground = currentState.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.UNDERGROUND);
        if (piece.isMrX()) {
            int numberSecret = currentState.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.SECRET);
            int numberDouble = currentState.getPlayerTickets(piece).orElseThrow().getCount(ScotlandYard.Ticket.DOUBLE);
            tickets.put(ScotlandYard.Ticket.SECRET, numberSecret);
            tickets.put(ScotlandYard.Ticket.DOUBLE, numberDouble);
        }
        tickets.put(ScotlandYard.Ticket.BUS, numberBuses);
        tickets.put(ScotlandYard.Ticket.TAXI, numberTaxi);
        tickets.put(ScotlandYard.Ticket.UNDERGROUND, numberUnderground);
        return ImmutableMap.copyOf(tickets);
    }
    private int helpMrXLoc(Board.GameState currentState) {
        if(currentState.getMrXTravelLog().get(currentState.getMrXTravelLog().size()-1).location().isPresent()) {
            return currentState.getMrXTravelLog().get(currentState.getMrXTravelLog().size()-1).location().orElseThrow();
        }
        else {
            
        }
        /*if(currentState.getMrXTravelLog().isEmpty()) {
            return 19;
        }
        if(currentState.getMrXTravelLog().get(currentState.getMrXTravelLog().size()-1).location().isEmpty()) {
            return 19;
        }
        else {return currentState.getMrXTravelLog().get(currentState.getMrXTravelLog().size()-1).location().orElseThrow(); }*/
    }
    public MyGameStateForAI(
            final GameSetup setup,
            final ImmutableSet<Piece> remaining,
            final ImmutableList<LogEntry> log,
            final Player mrX,
            List<Player> detectives,
            GameState stateCameFrom,
            Move moveCameFrom) {

        this.setup = setup;
        this.remaining = remaining;
        this.log = log;
        this.mrX = mrX;
        this.detectives = detectives;
        this.stateCameFrom = stateCameFrom;
        this.moveCameFrom = moveCameFrom;

        if(remaining.contains(mrX.piece())) {
            createMrXMoves();
        } else {
            Set<Move> singleMoves = new HashSet<>();

            for(Player detective : detectives) {
                if(remaining.contains(detective.piece())) {
                    singleMoves.addAll(makeSingleMoves(this.setup, this.detectives, detective, detective.location()));
                }
            }
            this.moves = ImmutableSet.<Move>builder().addAll(singleMoves).build();
        }
    }


    private void createMrXMoves() {
        Set<Move> singleMoves = ImmutableSet.copyOf(makeSingleMoves(this.setup, this.detectives, this.mrX, mrX.location()));

        if(mrX.has(ScotlandYard.Ticket.DOUBLE) && setup.moves.size() > 1) {
            Set<Move> doubleMoves = ImmutableSet.copyOf(makeDoubleMoves(this.setup, this.detectives, this.mrX, mrX.location()));
            this.moves = ImmutableSet.<Move>builder().addAll(singleMoves).addAll(doubleMoves).build();
        } else {
            this.moves = ImmutableSet.<Move>builder().addAll(singleMoves).build();
        }
    }

    @Nonnull
    @Override
    public GameSetup getSetup() {
        return setup;
    }

    @Nonnull
    @Override
    public ImmutableSet<Piece> getPlayers() {
        Set<Piece> playersMutable = new HashSet<>();
        playersMutable.add(mrX.piece());

        for (Player detective : detectives) {
            playersMutable.add(detective.piece());
        }

        return ImmutableSet.copyOf(playersMutable);
    }

    @Nonnull
    @Override
    public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
        for (Player player : detectives) {
            if (player.piece().equals(detective)) {
                return Optional.of(player.location());
            }
        }

        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<TicketBoard> getPlayerTickets(Piece piece) {
        ImmutableMap<ScotlandYard.Ticket, Integer> tickets = null;

        if(mrX.piece().equals(piece)) {
            tickets = mrX.tickets();
        }

        for (Player detective : detectives) {
            if (detective.piece().equals(piece)) {
                tickets = detective.tickets();
            }
        }

        if(tickets == null) return Optional.empty();

        ImmutableMap<ScotlandYard.Ticket, Integer> finalTickets = tickets;
        return Optional.of(finalTickets::get);
    }

    @Nonnull
    @Override
    public ImmutableList<LogEntry> getMrXTravelLog() {
        return this.log;
    }

    // sees if Mr X lands on a detective and if so detective wins
    private boolean checkMrXOnDetective() {
        // if Mr X location is detective location they have accidentally landed on a detective
        for(Player detective : detectives) {
            if (detective.location() == mrX.location()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkMrXCornered() {
        // gets all the single moves of Mr X
        Set<Move> singleMovesMrX = makeSingleMoves(setup, detectives, mrX, mrX.location());

        // if there are single moves, Mr X can make a move and is not cornered
        return singleMovesMrX.isEmpty();
    }
    private boolean mrXFillsLog() {
        // checks if the size of true/false of moves equals the size of the log
        return this.log.size() == this.setup.moves.size();
    }
    private void emptyGetMoves(){
        Set<Move> empty = Collections.emptySet();
        this.moves = ImmutableSet.copyOf(empty);
    }
    private boolean checkDetectiveUnableToGet() {
        // stores number of detectives unable to make a move
        int numberOfDetectivesUnable = 0;
        // for each detective checks if they have at least one of each ticket
        for(Player detective : detectives) {
            if(detective.hasAtLeast(ScotlandYard.Ticket.BUS, 1) ||
                    detective.hasAtLeast(ScotlandYard.Ticket.TAXI, 1)
                    || detective.hasAtLeast(ScotlandYard.Ticket.UNDERGROUND, 1)) {
                // if the detective does then a move can be made, so false is returned
                return false;
            }
            // add the number of detectives unable to make a move to this list
            else {numberOfDetectivesUnable = numberOfDetectivesUnable + 1;}
        }
        // if the number of detectives unable to make a move are all the detectives
        // then no detective can make a move and true is returned
        // otherwise some can make a move and false is returned
        return numberOfDetectivesUnable == detectives.size();
    }

    @Nonnull
    @Override
    public ImmutableSet<Piece> getWinner() {
        Set<Piece> winners = new HashSet<>();
        // check whether Mr X fills all his log
        boolean mrXWinsLogFilled = mrXFillsLog();
        // check whether detectives have run out of tickets
        boolean detectiveNoLongerPlay = checkDetectiveUnableToGet();
        // checks if no detectives can make a move or if the log is filled
        // both indicate that Mr X wins
        if(mrXWinsLogFilled || detectiveNoLongerPlay) {
            // empty all the available moves
            emptyGetMoves();
            // winner stores Mr X
            return ImmutableSet.of(mrX.piece());
        }
        else{
            // Mr X is clearly not the winner
            // check if Mr X accidentally lands on a detective
            boolean detectiveWinMrXCaptured = checkMrXOnDetective();
            // check if Mr X is cornered by detectives
            boolean detectiveWinMrXCorner = checkMrXCornered();
            // if Mr X captured or cornered detectives win
            if(detectiveWinMrXCaptured || detectiveWinMrXCorner) {
                emptyGetMoves();
                return ImmutableSet
                        .copyOf(detectives
                                .stream()
                                .map(Player::piece)
                                .collect(Collectors.toSet()));
            }
        }
        // need to check if game still going
        // if moves is empty and nobody wins indication of a new round
        if(this.moves.isEmpty()) {
            // so creates moves for Mr X
            createMrXMoves();
        }
        return ImmutableSet.copyOf(winners);
    }

    // Returns true if the given node is unoccupied by a detective and false otherwise
    private static boolean nodeUnOccupied(List<Player> detectives, int node) {
        for (Player detective : detectives) {
            if (detective.location() == node) return false;
        }
        return true;
    }

    // Finds and constructs all possible single moves for a given player
    private static Set<Move> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

        Set<Move> singleMoves = new HashSet<>();

        for(int destination : setup.graph.adjacentNodes(source)) {

            // Generates single moves for normal tickets (Taxi, Bus, Train)
            for(ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
                if(player.has(t.requiredTicket()) && nodeUnOccupied(detectives, destination)) {
                    Move.SingleMove move = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
                    singleMoves.add(move);
                }
            }

            // Generates single moves for secret tickets
            if(player.has(ScotlandYard.Ticket.SECRET) && nodeUnOccupied(detectives, destination)) {
                Move.SingleMove move = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
                singleMoves.add(move);
            }
        }
        return singleMoves;
    }

    // Finds and constructs all possible double moves for a given player
    private static Set<Move> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

        Set<Move> doubleMoves = new HashSet<>();

        for(int destination1 : setup.graph.adjacentNodes(source)) {
            if(nodeUnOccupied(detectives, destination1)) {

                for(int destination2 : setup.graph.adjacentNodes(destination1)) {
                    if(nodeUnOccupied(detectives, destination2)) {

                        for(ScotlandYard.Transport t1 : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of()))) {
                            if(player.has(t1.requiredTicket()) || player.has(ScotlandYard.Ticket.SECRET)) {

                                for(ScotlandYard.Transport t2 : Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of()))) {
                                    if(player.has(t2.requiredTicket()) || player.has(ScotlandYard.Ticket.SECRET)) {

                                        if(t1.requiredTicket().equals(t2.requiredTicket()) && player.hasAtLeast(t1.requiredTicket(), 2)) {
                                            Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2);
                                            doubleMoves.add(doubleMove);
                                        } else if (!t1.requiredTicket().equals(t2.requiredTicket())) {
                                            Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2);
                                            doubleMoves.add(doubleMove);
                                        }

                                        if(player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2)) {
                                            Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, ScotlandYard.Ticket.SECRET, destination2);
                                            doubleMoves.add(doubleMove);
                                        }

                                        if(player.has(t1.requiredTicket()) && player.has(ScotlandYard.Ticket.SECRET)) {
                                            Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, ScotlandYard.Ticket.SECRET, destination2);
                                            doubleMoves.add(doubleMove);
                                        }

                                        if(player.has(ScotlandYard.Ticket.SECRET) && player.has(t2.requiredTicket())) {
                                            Move.DoubleMove doubleMove = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, t2.requiredTicket(), destination2);
                                            doubleMoves.add(doubleMove);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return doubleMoves;
    }

    @Nonnull
    @Override
    public ImmutableSet<Move> getAvailableMoves() {
        return this.moves;
    }

    public Move getMoveCameFrom() {
        return moveCameFrom;
    }

    public Board.GameState getStateCameFrom() {
        return stateCameFrom;
    }

    // Checks if the current MrX move is hidden or not, and adds the corresponding log entry, or multiple log
    // entries if MrX uses a double move.
    private List<LogEntry> LogMrXMove(List<LogEntry> logEntry, ScotlandYard.Ticket ticketUsed, int destination) {
        List<LogEntry> checking = new ArrayList<>(logEntry);
        boolean revealMove = this.setup.moves.get(checking.size());
        LogEntry myNewLogEntry;

        if(revealMove) { myNewLogEntry = LogEntry.reveal(ticketUsed, destination); }
        else { myNewLogEntry = LogEntry.hidden(ticketUsed); }

        checking.add(myNewLogEntry);

        return checking;
    }

    @Nonnull
    @Override

    public MyGameStateForAI advance(Move move) {

        if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

        // gets the final destination , for single moves this is just x.destination
        // for double it is x.destination2
        Move.Visitor<Integer> getDestinationFinal = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination2));
        int destinationFinal = move.accept(getDestinationFinal);

        // gets the final tickets
        Move.Visitor<ScotlandYard.Ticket> getTicketFinal = new Move.FunctionalVisitor<>((x -> x.ticket), (x ->x.ticket2));
        ScotlandYard.Ticket ticketUsedFinal = move.accept(getTicketFinal);

        // gets if a move made is a double move or not
        Move.Visitor<Boolean> ifIsDouble = new Move.FunctionalVisitor<>((x -> false), (x -> true));
        boolean isDouble = move.accept(ifIsDouble);
        if(move.commencedBy().isMrX()) {
            // stores a new log entry with the added move
            ImmutableList<LogEntry> logEntryFinal;
            List<LogEntry> logEntry = List.copyOf(this.log);
            Player newMrXChangedLoc;

            if(!isDouble) {
                // enter the move into the log
                logEntryFinal = ImmutableList.copyOf(LogMrXMove(logEntry, ticketUsedFinal, destinationFinal));

                // takes used ticket away from Mr X by returning a new Mr X without this ticket
                Player newMrXUsedTicket  = mrX.use(ticketUsedFinal);

                // moves Mr X to their new destination by returning a new Mr X at this destination
                newMrXChangedLoc =  newMrXUsedTicket.at(destinationFinal);
            }
            else {
                // gets the intermediate destination
                Move.Visitor<Integer> getDestinationIntermediate = new Move.FunctionalVisitor<>((x -> x.destination), (x -> x.destination1));
                int destinationIntermediate = move.accept(getDestinationIntermediate);

                // gets the intermediate tickets
                Move.Visitor<ScotlandYard.Ticket> getTicketIntermediate = new Move.FunctionalVisitor<>((x -> x.ticket), (x ->x.ticket1));
                ScotlandYard.Ticket ticketUsedIntermediate = move.accept(getTicketIntermediate);

                // double move of Mr X
                List<LogEntry> logEntryFirstMove;

                // add their first move into the log
                logEntryFirstMove = LogMrXMove(logEntry, ticketUsedIntermediate, destinationIntermediate);

                // add their second move into the log
                logEntryFinal = ImmutableList.copyOf(LogMrXMove(logEntryFirstMove,ticketUsedFinal, destinationFinal));

                // create new Mr X objects as previously
                Player newMrXUsedTicket1  = mrX.use(ticketUsedIntermediate);
                Player newMrXUsedTicket2  = newMrXUsedTicket1.use(ticketUsedFinal);
                Player newMrXUsedDouble = newMrXUsedTicket2.use(ScotlandYard.Ticket.DOUBLE);
                newMrXChangedLoc =  newMrXUsedDouble.at(destinationFinal);
                System.out.println("the dest final is " + destinationFinal);

            }

            // change to detectives turn means remove Mr X from remaining
            Set<Piece> remainingUpdated = remaining
                    .stream()
                    .filter(d -> !d.isMrX())
                    .collect(Collectors.toSet());

            for(Player detective : detectives) {
                remainingUpdated.add(detective.piece());
            }

            // returns a new game state and swaps to the detective turn
            return new MyGameStateForAI(setup, ImmutableSet.copyOf(remainingUpdated), logEntryFinal, newMrXChangedLoc, detectives, this, move);
        }

        else {
            // get the given detective piece
            Player currentDetective = detectives
                    .stream()
                    .filter(d -> d.piece().equals(move.commencedBy()))
                    .findFirst()
                    .orElseThrow();

            // returns detective at the new location
            Player currentDetectiveNewLoc = currentDetective.at(destinationFinal);

            // returns detective with one less ticket
            Player currentDetectiveTickLost = currentDetectiveNewLoc.use(ticketUsedFinal);

            // returns a Mr X with the ticket
            Player newMrX = mrX.give(ticketUsedFinal);

            // ensuring particular detective does not move again in this round
            Set<Piece> remainingUpdated;

            // update rU to not have cD
            remainingUpdated = remaining
                    .stream()
                    .filter(d -> !d.equals(currentDetectiveTickLost.piece()))
                    .collect(Collectors.toSet());

            // checks if no moves left for detective, then it is mrX turn
            if(remainingUpdated.isEmpty()) {
                remainingUpdated.add(mrX.piece());
                for(Player detective : detectives) {
                    remainingUpdated.add(detective.piece());
                }
            }

            // update detectives and replace cD with cDTL
            List<Player> detectivesUpdated = detectives
                    .stream()
                    .filter(d -> !d.equals(currentDetective))
                    .collect(Collectors.toList());
            detectivesUpdated.add(currentDetectiveTickLost);
            // the state which the new game state will come from is the current state on which advance is called
            return new MyGameStateForAI(setup, ImmutableSet.copyOf(remainingUpdated), this.log, newMrX, detectivesUpdated, this, move);
        }
    }
}



