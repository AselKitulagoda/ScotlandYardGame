package uk.ac.bris.cs.oxo.standard;

import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.matrix.ImmutableMatrix;
import uk.ac.bris.cs.gamekit.matrix.Matrix;
import uk.ac.bris.cs.gamekit.matrix.SquareMatrix;
import uk.ac.bris.cs.oxo.Cell;
import uk.ac.bris.cs.oxo.Outcome;
import uk.ac.bris.cs.oxo.Player;
import uk.ac.bris.cs.oxo.Side;
import uk.ac.bris.cs.oxo.Spectator;

public class OXO implements OXOGame, Consumer<Move> {

	private Player noughtSide, crossSide;
	private Side currentSide;
	private int size;
	private Consumer<Move> callback;
	private final List<Spectator> spectators = new CopyOnWriteArrayList<>();
	private final SquareMatrix<Cell> matrix;

	public OXO(int size, Side startSide, Player noughtSide, Player crossSide) {

		if (size <= 0){
			throw new IllegalArgumentException("Invalid size!");
		}

		this.noughtSide = requireNonNull(noughtSide);
		this.crossSide = requireNonNull(crossSide);
		this.currentSide = requireNonNull(startSide);
		this.matrix = new SquareMatrix<Cell>(size, new Cell());
	}

	@Override
	public void registerSpectators(Spectator... spectators) {
		this.spectators.addAll(Arrays.asList(spectators));
	}

	@Override
	public void unregisterSpectators(Spectator... spectators) {
		this.spectators.removeAll(Arrays.asList(spectators));
	}

	private Set<Move> validMoves() {
		Set<Move> moves = new HashSet<>();
		for(int row = 0; row < matrix.rowSize(); row++) {
			for(int col = 0; col < matrix.columnSize(); col++) {
				if(matrix.get(row, col).isEmpty())
					moves.add(new Move(row, col));
			}
		}
		return moves;
	}

	@Override
	public void accept(Move move) {
		if (validMoves().contains(move)){
			matrix.put(move.row, move.column, new Cell(currentSide));
			for(Spectator spec : spectators){
				spec.moveMade(currentSide, move);
				if(validMoves().isEmpty() || )
			}
			currentSide = currentSide.other();
			start();
		} else throw new IllegalArgumentException("Move does not exist!");
	}

	@Override
	public void start() {
		Player player = (currentSide == Side.CROSS) ? crossSide : noughtSide;
		player.makeMove(this, validMoves(), this);
	}

	@Override
	public Matrix<Cell> board() {
		return new ImmutableMatrix<>(matrix);
	}

	@Override
	public Side currentSide() {
		return currentSide;
	}
}
