package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import uk.ac.bris.cs.gamekit.graph.Graph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<>();
	private Map<Ticket, Integer> tickets;

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
		for(PlayerConfiguration player : restOfTheDetectives){
			configurations.add(requireNonNull(player));
		}
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);

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
		tix(firstDetective, mrX);
	}

	public void tix(PlayerConfiguration firstDetective, PlayerConfiguration mrX){

		List<PlayerConfiguration> detectives = new ArrayList<>();
		for(PlayerConfiguration rest : detectives){
			if(rest.tickets.containsKey(SECRET) || rest.tickets.containsKey(DOUBLE) || !rest.tickets.containsKey(TAXI) ||
					!rest.tickets.containsKey(UNDERGROUND) || !rest.tickets.containsKey(BUS))
				throw new IllegalArgumentException("Detectives cannot use this ticket");
		}
		detectives.add(0, firstDetective);

		if(!mrX.tickets.containsKey(TAXI) || !mrX.tickets.containsKey(BUS) || !mrX.tickets.containsKey(UNDERGROUND) ||
				!mrX.tickets.containsKey(SECRET) || !mrX.tickets.containsKey(DOUBLE))
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

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> colours = new ArrayList<>();

		for(ScotlandYardPlayer player : players)
			colours.add(player.colour());

		return Collections.unmodifiableList(colours);
		//throw new RuntimeException("Implement me");
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public boolean isGameOver() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public int getCurrentRound() {
		// TODO
		throw new RuntimeException("Implement me");
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
