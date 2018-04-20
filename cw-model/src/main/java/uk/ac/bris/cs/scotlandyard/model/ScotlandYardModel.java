package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.*;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.*;

import uk.ac.bris.cs.gamekit.graph.Graph;

import javax.swing.text.html.Option;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private int currentPlayer = 0;
	private int currentRound = ScotlandYardView.NOT_STARTED;
	private Collection<Spectator> spectators = new CopyOnWriteArrayList<>();
	private int lastLocation = 0;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {

		this.rounds = requireNonNull(rounds); //makes sure rounds are not null
		this.graph = requireNonNull(graph);   //makes sure graph is not null

		if(rounds.isEmpty()) throw new IllegalArgumentException("Empty rounds!");
		if(graph.isEmpty()) throw new IllegalArgumentException("Empty graph!");
		if(mrX.colour != BLACK) throw new IllegalArgumentException("Mr. X should be black!");

		//Putting the player configurations in a temporary list and check on a loop
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		for(PlayerConfiguration player : restOfTheDetectives)
			configurations.add(requireNonNull(player));
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

		//Adding the players to the ScotlandYardPlayer list from the PlayerConfiguration list
		for(PlayerConfiguration x : configurations){
			players.add(new ScotlandYardPlayer(x.player, x.colour, x.location, x.tickets));
		}

		//Checking duplicate locations
		Set<Integer> locationSet = new HashSet<>();
		for(PlayerConfiguration player : configurations){
			if(locationSet.contains(player.location))
				throw new IllegalArgumentException("Duplicate location!");
			locationSet.add(player.location);
		}

		//Checking duplicate colours
		Set<Colour> colourSet = new HashSet<>();
		for(PlayerConfiguration player : configurations){
			if(colourSet.contains(player.colour))
				throw new IllegalArgumentException("Duplicate colour!");
			colourSet.add(player.colour);
		}
		checkTickets(firstDetective, mrX, restOfTheDetectives);
	}

	//Method to check whether all ticket types exist and assigning respective tickets to players
	public void checkTickets(PlayerConfiguration firstDetective, PlayerConfiguration mrX, PlayerConfiguration... restOfTheDetectives){
		List<PlayerConfiguration> detectives = new ArrayList<>();

		if(!firstDetective.tickets.containsKey(SECRET) || !firstDetective.tickets.containsKey(DOUBLE) || !firstDetective.tickets.containsKey(TAXI) || !firstDetective.tickets.containsKey(UNDERGROUND) || !firstDetective.tickets.containsKey(BUS))
			throw new IllegalArgumentException("error");

		if(firstDetective.tickets.get(SECRET) != 0 || firstDetective.tickets.get(DOUBLE) != 0 )
			throw new IllegalArgumentException("error!");

		for(PlayerConfiguration rest : restOfTheDetectives){
			detectives.add(requireNonNull(rest));
			if(!rest.tickets.containsKey(SECRET) || !rest.tickets.containsKey(DOUBLE) || !rest.tickets.containsKey(TAXI) || !rest.tickets.containsKey(UNDERGROUND) || !rest.tickets.containsKey(BUS))
				throw new IllegalArgumentException("error");

			if(rest.tickets.get(SECRET) != 0 || rest.tickets.get(DOUBLE) != 0 )
				throw new IllegalArgumentException("error!");
		}
		if(!mrX.tickets.containsKey(TAXI) || !mrX.tickets.containsKey(BUS) || !mrX.tickets.containsKey(UNDERGROUND) || !mrX.tickets.containsKey(SECRET) || !mrX.tickets.containsKey(DOUBLE))
			throw new IllegalArgumentException("MrX must have all tickets");
	}

	//Registering the spectators (not null)
	@Override
	public void registerSpectator(Spectator spectator) {
		if(spectators.contains(requireNonNull(spectator)))
			throw new IllegalArgumentException("The spectator is already registered!");
		else spectators.add(spectator);
	}

	//Unregistering the spectators iff they exist before
	@Override
	public void unregisterSpectator(Spectator spectator) {
		if(spectators.contains(requireNonNull(spectator)))
			spectators.remove(spectator);
		else throw new IllegalArgumentException("The spectator is not registered!");
	}

	//Method to get the ScotlandYardPlayer for a given colour
	private ScotlandYardPlayer playerFromColour(Colour colour) {
		for(ScotlandYardPlayer x : players){
			if(colour == x.colour()) return x;
		}
		throw new IllegalArgumentException("colour not found!");
	}

	//MoveVisitor method for a Pass Move
	@Override
	public void visit(PassMove move){
		moveMade(move);
	}

	//MoveVisitor method for a Ticket Move (single move)
	@Override
	public void visit(TicketMove move){
		ScotlandYardPlayer p = playerFromColour(move.colour());
		ScotlandYardPlayer mrX = playerFromColour(BLACK);
		p.location(move.destination());
		p.removeTicket(move.ticket());

		if(p.isDetective()) mrX.addTicket(move.ticket());

		Move hiddenMove = move;

		if(move.colour().isMrX()) {

			if (!rounds.get(currentRound)) {
				hiddenMove = new TicketMove(BLACK, move.ticket(), lastLocation);
			}

			else lastLocation = move.destination();
			startRound();
		}
		moveMade(hiddenMove);
	}

	//MoveVisitor method for a Double Move (only for mrX)
	@Override
	public void visit(DoubleMove move){

		ScotlandYardPlayer mrX = playerFromColour(BLACK);
		TicketMove move1 = move.firstMove();
		TicketMove move2 = move.secondMove();
		mrX.removeTicket(DOUBLE);

		boolean revealRound = rounds.get(currentRound);

		if(!revealRound && !rounds.get(currentRound + 1)){ //checking whether current round and round after are hidden rounds
			moveMade(new DoubleMove(BLACK, move1.ticket(), lastLocation, move2.ticket(), lastLocation));
		}
		else if (revealRound && !rounds.get(currentRound + 1)) { //checking whether the current round is reveal and the round after is hidden
			moveMade(new DoubleMove(BLACK, move1.ticket(), move1.destination(), move2.ticket(), move1.destination()));
		}
		else if (!revealRound && rounds.get(currentRound + 1)){ //checking whether the current round is hidden but the round after is reveal
			moveMade(new DoubleMove(BLACK, move1.ticket(), lastLocation, move2.ticket(), move2.destination()));
		}
		else {
			moveMade(move);
		}

		mrX.location(move1.destination());
		mrX.removeTicket(move1.ticket());
		Move hiddenMove = move1;
		if(move.colour().isMrX()) {
			if (!rounds.get(currentRound)) {
				hiddenMove = new TicketMove(BLACK, move1.ticket(), lastLocation);
			}
			else lastLocation = move1.destination();
		}
		startRound();
		moveMade(hiddenMove);

		mrX.location(move2.destination());
		mrX.removeTicket(move2.ticket());
		hiddenMove = move2;
		if(move.colour().isMrX()) {
			if (!rounds.get(currentRound)) {
				hiddenMove = new TicketMove(BLACK, move2.ticket(), lastLocation);
			}
			else lastLocation = move2.destination();
		}
		startRound();
		moveMade(hiddenMove);
	}

	//Method to create all possible moves from a given location for a colour. (Does not check if valid)
	public Set<TicketMove> createMoves(Colour colour, int startingPoint) {
		ScotlandYardPlayer p = playerFromColour(colour);
		Set<TicketMove> ticketMoves = new HashSet<>();
		List<Integer> locations = new ArrayList<>();

		for(ScotlandYardPlayer player : players){ //storing the detectives' current locations in a list to make it easier to check whether location is occupied
			if(player.isDetective()) locations.add(player.location());
		}

		Node<Integer> start = new Node<>(startingPoint);
		Collection<Edge<Integer, Transport>> possibilities = graph.getEdgesFrom(start); //getting all the possible edges to move to from a point for a given transport.

		for(Edge<Integer, Transport> edge : possibilities){
			if(p.hasTickets(fromTransport(edge.data()), 1) && !(locations.contains(edge.destination().value()))) //asserting whether player has sufficient tickets and making sure the location to move to is free
				ticketMoves.add(new TicketMove(colour, fromTransport(edge.data()), edge.destination().value()));
			if(p.isMrX() && p.hasTickets(SECRET) && !(locations.contains(edge.destination().value()))) //checking whether mrX is using a secret ticket to make a move
				ticketMoves.add(new TicketMove(colour, SECRET, edge.destination().value()));
		}
		return ticketMoves;
	}

	private Set<Move> validMove(Colour player) {
		ScotlandYardPlayer p = playerFromColour(player);
		Set<Move> validMoves = new HashSet<>();
		int lastRound = rounds.size() - 1;

		if(p.isMrX()){
			Set<TicketMove> singleMove = new HashSet<>(); //set to store the moves if mrX plays a single move
			singleMove.addAll(createMoves(player, p.location())); //adding the possible moves to the single move set
			validMoves.addAll(singleMove); //adding the single moves to valid moves using the addAll method

			if(currentRound < lastRound && p.hasTickets(DOUBLE)){ //checking whether the currentRound is not the last round and asserting whether mrX has a double move ticket
				for(TicketMove move1 : singleMove){

					Set<TicketMove> doubleMove = new HashSet<>(); //creating a double move set
					doubleMove.addAll(createMoves(BLACK, move1.destination())); //adding the possible moves from the destination of move1

					for(TicketMove move2 : doubleMove){
						if(p.hasTickets(move2.ticket())) validMoves.add(new DoubleMove(BLACK, move1, move2)); //checking whether mrX has the respective move2 ticket and adding the double moves to the valid moves set
						if(move1.ticket() == move2.ticket() && !p.hasTickets(move1.ticket(), 2)){ //checking whether the tickets used are the same and hence removing the move avoiding overlap
							validMoves.remove(new DoubleMove(BLACK, move1, move2));
						}
					}
				}
			}
		}
		if(p.isDetective()) validMoves.addAll(createMoves(player, p.location())); //adding the detectives' moves to valid moves
		if(p.isDetective() && validMoves.isEmpty()) validMoves.add(new PassMove(player)); //adding a pass move for a detective iff the valid moves set is empty

		return Collections.unmodifiableSet(validMoves);
	}

	//Method to check whether the rounds of the game are over
	private boolean roundsAreOver(){
		if(currentRound == rounds.size()  && currentPlayer == 0) return true;
		else return false;
	}

	//Method to check whether mrX has been caught
	private boolean mrXIsCaught(){
		ScotlandYardPlayer mrX = playerFromColour(BLACK);
		for(ScotlandYardPlayer p : players){
			if(p.isDetective()){
				if(p.location() == mrX.location()) return true;
			}
		}
		return false;
	}

	//Method to check whether mrX is stuck and cannot make a move
	private boolean mrXIsStuck(){
		if(validMove(BLACK).isEmpty() && currentPlayer == 0) return true;
		return false;
	}

	//Method to check whether the detectives are stuck.
	private boolean detectivesAreStuck(){
		for(ScotlandYardPlayer p : players){
			if(p.isDetective() && !createMoves(p.colour(), p.location()).isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public void accept(Move move) {

		currentPlayer = (currentPlayer + 1) % players.size();

		requireNonNull(move);

		if (!validMove(move.colour()).contains(move))
			throw new IllegalArgumentException("Incorrect move!");

		move.visit(this);

		if(!isGameOver()){
			if(currentPlayer > 0){

//				if(!(move instanceof DoubleMove)) {
//				if(players.get(currentPlayer - 1).isMrX())
//					startRound();
//					moveMade(move);
//				}
				playMove();
			}
			else {
//				if(!(move instanceof DoubleMove)) moveMade(move);
				for(Spectator s : spectators) s.onRotationComplete(this);
			}
		}
		else {
			for(Spectator s : spectators) s.onGameOver(this, getWinningPlayers());
		}
	}

	private void startRound(){
		currentRound += 1;
		for(Spectator s : spectators) s.onRoundStarted(this, currentRound);
	}

	@Override
	public void startRotate() {

		if(isGameOver()) throw new IllegalStateException("Game is over!");
		else {
			currentPlayer = 0;
			ScotlandYardPlayer mrX = playerFromColour(BLACK);
			mrX.player().makeMove(this, mrX.location(), validMove(BLACK), this);
		}
	}

	private void playMove(){
		ScotlandYardPlayer p = playerFromColour(getCurrentPlayer());
		p.player().makeMove(this, p.location(), validMove(p.colour()), this);
	}

	private void moveMade(Move move){
		for(Spectator s : spectators) s.onMoveMade(this, move);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableCollection(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> colours = new ArrayList<>();
		for(ScotlandYardPlayer player : players) {
			colours.add(player.colour());
		}
		return Collections.unmodifiableList(colours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		Set<Colour> winner = new HashSet<>();
		List<Colour> allPlayers = getPlayers();

		if(roundsAreOver() || detectivesAreStuck()){
			winner.add(BLACK);
		}
		else if(mrXIsStuck() || mrXIsCaught()){
			winner.addAll(allPlayers);
			winner.remove(BLACK);
		}
		return Collections.unmodifiableSet(winner);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		for(ScotlandYardPlayer player : players) {
			if (colour == player.colour()) {
				if (player.isDetective())
					return Optional.of(player.location());
				else
					return Optional.of(lastLocation);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for(ScotlandYardPlayer player : players){
			if(colour == player.colour()){
				if(player.tickets().containsKey(ticket))
					return Optional.of(player.tickets().get(ticket));
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		if(roundsAreOver() || mrXIsCaught() || mrXIsStuck() || detectivesAreStuck()) return true;
		else return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return getPlayers().get(currentPlayer);
	}

	@Override
	public int getCurrentRound() {
		return currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(graph);
	}

}
