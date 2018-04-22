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
	private ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
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
		checkTickets();
	}

	//Method to check whether all ticket types exist and assigning respective tickets to players
	public void checkTickets(){
		for(PlayerConfiguration p : configurations){
			for(Ticket t : Ticket.values()){
				if(!p.tickets.containsKey(t)) throw new IllegalArgumentException("Player does not have respective tickets!");
			}
			if(p.colour.isDetective()){
				if(p.tickets.get(DOUBLE) != 0 || p.tickets.get(SECRET) != 0) throw new IllegalArgumentException("Detectives should not contain these tickets!");
			}
		}
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
						if(move1.ticket() == move2.ticket() && !p.hasTickets(move1.ticket(), 2))validMoves.remove(new DoubleMove(BLACK, move1, move2));
						//checking whether the tickets used are the same and hence removing the move avoiding overlap
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
		p.removeTicket(move.ticket()); //removes the detective's ticket after move has been made

		if(p.isDetective()) mrX.addTicket(move.ticket()); //adding the detective's ticket to mrX's pile of tickets

		Move hiddenMove = move; //setting the hidden move to the move if it is not a reveal round

		if(move.colour().isMrX()) {

			if (!rounds.get(currentRound)) {
				hiddenMove = new TicketMove(BLACK, move.ticket(), lastLocation); //setting the hidden move to a move showing Mr X's last known location
			}
			else lastLocation = move.destination(); //updates Mr X's last known location to the move's destination if its a reveal round
			startRound(); //updates the spectators telling them the round has started
		}
		moveMade(hiddenMove);
	}

	//MoveVisitor method for a Double Move (only for mrX)
	@Override
	public void visit(DoubleMove move){

		ScotlandYardPlayer mrX = playerFromColour(BLACK);
		TicketMove move1 = move.firstMove();
		TicketMove move2 = move.secondMove();
		mrX.removeTicket(DOUBLE); //removing the double move ticket from Mr X

		boolean revealRound = rounds.get(currentRound);

		if(!revealRound && !rounds.get(currentRound + 1)){ //checking whether current round and round after are hidden rounds
			moveMade(new DoubleMove(BLACK, move1.ticket(), lastLocation, move2.ticket(), lastLocation)); //if none are reveal rounds, show the spectators and players, the last location
		}
		else if (revealRound && !rounds.get(currentRound + 1)) { //checking whether the current round is reveal and the round after is hidden
			moveMade(new DoubleMove(BLACK, move1.ticket(), move1.destination(), move2.ticket(), move1.destination()));
		}
		else if (!revealRound && rounds.get(currentRound + 1)){ //checking whether the current round is hidden but the round after is reveal
			moveMade(new DoubleMove(BLACK, move1.ticket(), lastLocation, move2.ticket(), move2.destination()));
		}
		else {
			moveMade(move); //updates the spectators telling them the move has been made
		}

		mrX.location(move1.destination()); //setting Mr X's location to the first move's destination
		mrX.removeTicket(move1.ticket()); //removing the first move's ticket after making a move

		Move hiddenMove = move1; //setting the first move to a hidden move

		if(move.colour().isMrX()) {
			if (!revealRound) { //checking whether the current is a reveal round
				hiddenMove = new TicketMove(BLACK, move1.ticket(), lastLocation); //updating the hidden move to the single move made, showing the players and spectators the last known location
			}
			else lastLocation = move1.destination(); //sets Mr X's location to the new destination (after the first move)
		}
		startRound(); //starting the round
		moveMade(hiddenMove); //updating the spectators that the hidden move has been made

		mrX.location(move2.destination()); //setting Mr X's location to the second move's destination
		mrX.removeTicket(move2.ticket()); //removing the second move's ticket after making a move

		hiddenMove = move2; //setting the second move to a hidden move

		if(move.colour().isMrX()) {
			if (!rounds.get(currentRound)) { //checking whether the current is a reveal round
				hiddenMove = new TicketMove(BLACK, move2.ticket(), lastLocation);
			}
			else lastLocation = move2.destination(); //sets Mr X's location to the new destination (after the second move)
		}
		startRound(); //starting the round
		moveMade(hiddenMove); //updating the spectators that the hidden move has been made
	}

	@Override
	public void accept(Move move) {
		currentPlayer = (currentPlayer + 1) % players.size();// increments aswell as resets the current player if it goes out of bounds in the players list
		requireNonNull(move);

		if (!validMove(move.colour()).contains(move)) throw new IllegalArgumentException("Incorrect move!");// throws if valid moves does not contain the move

		move.visit(this);// calls visit on the object itself

		if(!isGameOver()){
			if(currentPlayer > 0) playMove(); // if game isnt over move is played
			else rotationComplete();
		}
		else returnWinningPlayers();// returns winning players if game is won
	}

	private void startRound(){
		currentRound += 1; //increments round before a move has been made
		for(Spectator s : spectators) s.onRoundStarted(this, currentRound);// calling move made on spectators
	}

	private void moveMade(Move move){
		for(Spectator s : spectators) s.onMoveMade(this, move);
	}

	private void rotationComplete(){
		for(Spectator s : spectators) s.onRotationComplete(this);
	}

	private void returnWinningPlayers(){
		for(Spectator s : spectators) s.onGameOver(this, getWinningPlayers());
	}

	@Override
	public void startRotate() {

		if(isGameOver()) throw new IllegalStateException("Game is over!"); //throws if game over conditions has been met
		else {
			currentPlayer = 0; //resets current player to zero if round is complete
			ScotlandYardPlayer mrX = playerFromColour(BLACK);
			mrX.player().makeMove(this, mrX.location(), validMove(BLACK), this); //calls make move on MrX
		}
	}

	private void playMove(){ // makes a move for a detective
		ScotlandYardPlayer p = playerFromColour(getCurrentPlayer());
		p.player().makeMove(this, p.location(), validMove(p.colour()), this);
	}

	@Override
	public Collection<Spectator> getSpectators() { //returns the list of spectators as added above
		return Collections.unmodifiableCollection(spectators);
	}

	@Override
	public List<Colour> getPlayers() { // returns the list of the players' colours
		List<Colour> colours = new ArrayList<>();
		for(ScotlandYardPlayer player : players) colours.add(player.colour());
		return Collections.unmodifiableList(colours);
	}

	@Override
	public Set<Colour> getWinningPlayers() { //returns the set of winning players' colours based on the winning conditions
		Set<Colour> winner = new HashSet<>();
		List<Colour> allPlayers = getPlayers();

		if(roundsAreOver() || detectivesAreStuck()) winner.add(BLACK);
		else if(mrXIsStuck() || mrXIsCaught()){
			winner.addAll(allPlayers);
			winner.remove(BLACK);
		}
		return Collections.unmodifiableSet(winner);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) { //returns the player's location based on the round. returns Mr X's last known location. Returns empty if player doesn't exist
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
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) { //returns the player's ticket count. Returns empty if player doesn't exist
		for(ScotlandYardPlayer player : players){
			if(colour == player.colour()){
				if(player.tickets().containsKey(ticket))
					return Optional.of(player.tickets().get(ticket));
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() { //returns true if winning conditions are met
		if(roundsAreOver() || mrXIsCaught() || mrXIsStuck() || detectivesAreStuck()) return true;
		else return false;
	}

	@Override
	public Colour getCurrentPlayer() { return getPlayers().get(currentPlayer); } //returns the current player

	@Override
	public int getCurrentRound() { return currentRound;	} // returns the current round

	@Override
	public List<Boolean> getRounds() { return Collections.unmodifiableList(rounds); } //returns the list of rounds (boolean)

	@Override
	public Graph<Integer, Transport> getGraph() { return new ImmutableGraph<>(graph); } //returns the graph (board) of the game
}