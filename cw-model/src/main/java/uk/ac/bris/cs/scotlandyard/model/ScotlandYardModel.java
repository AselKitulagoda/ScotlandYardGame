package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.YELLOW;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.*;

import uk.ac.bris.cs.gamekit.graph.Graph;

import javax.swing.text.html.Option;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private int currentIndex = 0;
	//private Colour currentPlayer;
	private int currentRound = ScotlandYardView.NOT_STARTED;

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
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	private Set<Move> validMove(Colour player) {
		ScotlandYardPlayer p = playerFromColour(player);
		Set<Move> validMoves = new HashSet<>();
		validMoves.add(new PassMove(player));
		Node<Integer> currentNode = nodeFromOptional(getPlayerLocation(player));
		Collection<Edge<Integer, Transport>> possibilities = graph.getEdgesFrom(currentNode);

			for (Edge<Integer, Transport> edge : possibilities) {

					if (p.hasTickets(fromTransport(edge.data())) && (p.location() != edge.destination().value()))
						validMoves.add(new TicketMove(player, fromTransport(edge.data()), edge.destination().value()));

			}

		return validMoves;
	}

	private Node<Integer> nodeFromOptional(Optional<Integer> x){
		return graph.getNode(x.get());
	}

	@Override
	public void accept(Move move){
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		ScotlandYardPlayer playa = playerFromColour(getCurrentPlayer());
		playa.player().makeMove(this, playa.location(), validMove(playa.colour()), this);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
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
		return Collections.unmodifiableSet(winner);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
			for(ScotlandYardPlayer player : players) {
				if (colour == player.colour()) {
					if (rounds.get(getCurrentRound()) && player.isMrX())
						return Optional.of(player.location());
					else if(!rounds.get(getCurrentRound()) && player.isMrX())
						return Optional.of(0);
					else if(rounds.get(getCurrentRound()) && player.isDetective())
						return Optional.of(0);
					else if (!rounds.get(getCurrentRound()) && player.isDetective())
						return Optional.of(player.location());
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
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		/*List<Colour> x = getPlayers();
		for(int i = currentIndex; i < x.size(); i++) {
			if(currentIndex == players.size() + 1) currentIndex = 0;
			currentIndex++;
			currentPlayer = x.get(i);
			break;
		} */
		return BLACK;
	}

	private ScotlandYardPlayer playerFromColour(Colour colour) {
		for(ScotlandYardPlayer x : players){
			if(colour == x.colour())
				return x;
		}
		throw new IllegalArgumentException("colour not found!");
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
