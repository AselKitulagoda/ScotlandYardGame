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
	private int xLocation;
	private TicketMove move1, move2;
	private boolean revealRound = false;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {

		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);

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

	@Override
	public void registerSpectator(Spectator spectator) {
		if(spectators.contains(requireNonNull(spectator))){
			throw new IllegalArgumentException("The spectator is already registered!");
		}
		else {
			spectators.add(spectator);
		}
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		if(spectators.contains(requireNonNull(spectator))){
			spectators.remove(spectator);
		}
		else {
			throw new IllegalArgumentException("The spectator is not registered!");
		}
	}

	private ScotlandYardPlayer playerFromColour(Colour colour) {
		for(ScotlandYardPlayer x : players){
			if(colour == x.colour())
				return x;
		}
		throw new IllegalArgumentException("colour not found!");
	}


	@Override
	public void visit(PassMove move){

	}

	@Override
	public void visit(TicketMove move){
		ScotlandYardPlayer p = playerFromColour(move.colour());
		ScotlandYardPlayer mrX = playerFromColour(BLACK);
		move1 = move;
		p.location(move.destination());
		p.removeTicket(move.ticket());
		if(p.isDetective()) mrX.addTicket(move.ticket());

		if(p.isMrX())
			currentRound += 1;
			startRound();
	}

	@Override
	public void visit(DoubleMove move){
		ScotlandYardPlayer p = playerFromColour(move.colour());
		move1 = move.firstMove();
		move2 = move.secondMove();

		if(spectators.isEmpty()){
			p.removeTicket(DOUBLE);
			p.removeTicket(move1.ticket());
			p.removeTicket(move2.ticket());
			currentRound += 2;
		}
		p.location(move.finalDestination());
	}

	public Set<TicketMove> createMoves(Colour colour, int startingPoint) {
		ScotlandYardPlayer p = playerFromColour(colour);
		Set<TicketMove> ticketMoves = new HashSet<>();
		List<Integer> locations = new ArrayList<>();
		Node<Integer> start = new Node<>(startingPoint);
		Collection<Edge<Integer, Transport>> possibilities = graph.getEdgesFrom(start);

		for(ScotlandYardPlayer player : players){
			if(player.isDetective())
				locations.add(player.location());
		}

		for(Edge<Integer, Transport> edge : possibilities){
			if(p.hasTickets(fromTransport(edge.data()), 1) && !(locations.contains(edge.destination().value())))
				ticketMoves.add(new TicketMove(colour, fromTransport(edge.data()), edge.destination().value()));
			if(p.isMrX() && p.hasTickets(SECRET) && !(locations.contains(edge.destination().value())))
				ticketMoves.add(new TicketMove(colour, SECRET, edge.destination().value()));
		}
		return ticketMoves;
	}

	private Set<Move> validMove(Colour player) {

		ScotlandYardPlayer p = playerFromColour(player);
		Set<Move> validMoves = new HashSet<>();
		int lastRound = rounds.size() - 1;

		if(p.isMrX()){

			Set<TicketMove> singleMove = new HashSet<>();
			singleMove.addAll(createMoves(player, p.location()));
			validMoves.addAll(singleMove);

			if(currentRound < lastRound && p.hasTickets(DOUBLE)){

				for(TicketMove move1 : singleMove){

					Set<TicketMove> doubleMove = new HashSet<>();
					doubleMove.addAll(createMoves(BLACK, move1.destination()));

					for(TicketMove move2 : doubleMove){

						if(p.hasTickets(move2.ticket()))
							validMoves.add(new DoubleMove(BLACK, move1, move2));

						if(move1.ticket() == move2.ticket() && !p.hasTickets(move1.ticket(), 2)){
							validMoves.remove(new DoubleMove(BLACK, move1, move2));
						}
					}
				}
			}
		}
		if(p.isDetective()) {
			validMoves.addAll(createMoves(player, p.location()));
		}
		if(p.isDetective() && validMoves.isEmpty()){
			validMoves.add(new PassMove(player));
		}
		return validMoves;
	}

	private boolean roundsAreOver(){
		if(currentRound == rounds.size() && currentPlayer == players.size() - 1)
			return true;
		else
			return false;
	}

	private boolean mrXIsCaught(){
		ScotlandYardPlayer mrX = playerFromColour(BLACK);
		for(ScotlandYardPlayer p : players){
			if(p.isDetective()){
				if(p.location() == mrX.location())
					return true;
			}
		}
		return false;
	}

	private boolean mrXIsStuck(){
		if(validMove(BLACK).isEmpty() && currentPlayer == players.size() - 1  ){
			return true;
		}
		return false;
	}

	private boolean detectivesAreStuck(){
		for(ScotlandYardPlayer p : players){
			if(p.isDetective() && !createMoves(p.colour(), p.location()).isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public void accept(Move move) {

		requireNonNull(move);
		revealRound = players.get(currentPlayer).isMrX() && rounds.get(currentRound);

		if (!validMove(move.colour()).contains(move))
			throw new IllegalArgumentException("Incorrect move!");

		move.visit(this);

		if(!isGameOver()){
			if(currentPlayer < players.size() - 1){

				currentPlayer += 1;
				if(players.get(currentPlayer - 1).isMrX()) startRound();
				if(!(move instanceof DoubleMove)) updateSpectators(move);
				playMove();
			}
			else {
				currentPlayer = 0;
				if(!(move instanceof DoubleMove)) updateSpectators(move);
				for(Spectator s : spectators) s.onRotationComplete(this);
			}
		}
		else {
			updateSpectators(move);
			for(Spectator s : spectators) s.onGameOver(this, getWinningPlayers());
		}

	}

	private void startRound(){
		for(Spectator s : spectators){
			s.onRoundStarted(this, currentRound);
		}
	}

	@Override
	public void startRotate() {

		if(isGameOver())
			throw new IllegalArgumentException("Game is over!");
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

	private void updateSpectators(Move move){
		for(Spectator s : spectators){
			s.onMoveMade(this, move);
		}
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
		//ScotlandYardPlayer mrX = playerFromColour(BLACK);
		for(ScotlandYardPlayer player : players) {
			if (colour == player.colour()) {
				if (player.isDetective())
					return Optional.of(player.location());
				else if (revealRound && player.isMrX()){
					xLocation = player.location();
					return Optional.of(xLocation);
				}
				else if(!revealRound && player.isMrX())
					return Optional.of(xLocation);
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
		if(roundsAreOver() || mrXIsCaught() || mrXIsStuck() || detectivesAreStuck()){
			return true;
		}
		else
			return false;
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