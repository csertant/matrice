package matrice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Class representing the current state of the game table
 *
 * Variables:
 * boardSize (int): the size of the game board. Can be 3 or higher.
 * state (Boolean[][]): the state of the table.
 */
public class GameState {

    private int boardSize;
    private Boolean[][] state;

    /* Static functions */

    /**
     *Static function that generates random state. Used to get arbitrary start and end states for a game.
     * @param boardSize Size of the game board.
     * @return Returns an arbitrary game state.
     */
    @NonNull
    private static Boolean[][] generate(int boardSize) {
        Boolean[][] state = new Boolean[boardSize][boardSize];
        Random rand = new Random();

        for(int i = 0; i < boardSize; i++)
        {
            for(int j = 0; j < boardSize; j++) {
                state[i][j] = rand.nextBoolean();
            }
        }
        return state;
    }

    /* Constructors */

    /**
     * Basic Constructor function. Initialises state with random values.
     * @param boardSize Size of the game table
     */
    GameState(int boardSize) {
        this.setBoardSize(boardSize);
        this.setState(GameState.generate(boardSize));
    }

    /**
     * Copy constructor for GameState objects.
     * @param other The state that is wished to be copied to the new object.
     */
    GameState(@NonNull GameState other) {
        this.setBoardSize(other.getBoardSize());
        this.state = new Boolean[boardSize][boardSize];
        for(int i = 0; i < boardSize; i++) {
            for(int j = 0; j < boardSize; j++) {
                this.state[i][j] = other.getCell(i, j);
            }
        }
    }

    /**
     * Constructor for String based creation. Used in Activity management.
     * @param stateString String that stores the state.
     * @param boardSize Size of the game board.
     */
    GameState(String stateString, int boardSize) {
        this.setBoardSize(boardSize);
        this.setState(this.stringToState(stateString));
    }

    /* Getters and Setters */

    public int getBoardSize() {
        return boardSize;
    }

    private void setBoardSize(int boardSize) throws IllegalArgumentException {
        /* Board size can be 3 or higher */
        if(boardSize < 3)
        {
            throw new IllegalArgumentException("Invalid board size: " + boardSize);
        }
        this.boardSize = boardSize;
    }

    private Boolean[][] getState() {
        return state;
    }

    private void setState(Boolean[][] other) {
        this.state = new Boolean[boardSize][boardSize];
        for(int i = 0; i < boardSize; i++) {
            for(int j = 0; j < boardSize; j++) {
                this.state[i][j] = other[i][j];
            }
        }
    }

    /**
     * Updates the requested cell of the game state with the given value
     * @param row Row of the cell to be updated.
     * @param col Column of the cell to be updated.
     * @param value New value.
     */
    public void setCell(int row, int col, Boolean value) throws IllegalArgumentException {
        if(value == null)
            throw new IllegalArgumentException("Value of a field cell can not be null.");
        if(row >= this.boardSize || col >= this.boardSize || row < 0 || col < 0)
            throw new IllegalArgumentException("Invalid field identifier (row or column).");
        this.state[row][col] = value;
    }

    /**
     * Gets the value of the wished cell of the game state
     * @param row Row of the cell to be got.
     * @param col Column of the cell to be got.
     * @return Value of the specified location.
     */
    public Boolean getCell(int row, int col) {
        return this.state[row][col];
    }

    /* Other */

    /**
     * Inverts the value of the given cell of the table
     * @param row Row of the cell to be inverted.
     * @param col Column of the cell to be inverted.
     */
    void invertCell(int row, int col) throws IllegalArgumentException {
        if(row < 0 || col < 0 || row >= this.boardSize || col >= this.boardSize)
            throw new IllegalArgumentException("Invalid field identifier (row or column)");
        this.state[row][col] = !this.state[row][col];
    }

    /**
     * Rotates the given row of the state by one towards left.
     * @param row Number of row to be rotated.
     * @throws IllegalArgumentException Throws exception when id is out of the range of the board
     * size.
     */
    void rotateRow(int row) throws IllegalArgumentException {
        if(row < 0 || row >= this.boardSize)
            throw new IllegalArgumentException("Invalid field identifier (row or column)");
        Boolean tempCell = this.state[row][0];
        for(int i = 0; i < this.boardSize-1; i++) {
            this.state[row][i] = this.state[row][i+1];
        }
        this.state[row][boardSize-1] = tempCell;
    }

    /**
     * Rotates the given column of the state by one upwards.
     * @param col Number of column to be rotated.
     * @throws IllegalArgumentException Throws exception when id is out of the range of the board
     * size.
     */
    void rotateColumn(int col) throws IllegalArgumentException {
        if(col < 0 || col >= this.boardSize)
            throw new IllegalArgumentException("Invalid field identifier (row or column)");
        Boolean tempCell = this.state[0][col];
        for(int i = 0; i < this.boardSize-1; i++) {
            this.state[i][col] = this.state[i+1][col];
        }
        this.state[boardSize-1][col] = tempCell;
    }

    /**
     * Calculates the integer format of the given current state. Used for logging user path.
     * @return Id of the current State.
     */
    public int getStateId() {
        int id = 0;
        int exponent = (boardSize * boardSize) - 1;
        for(int i = 0; i < this.boardSize; i++)
        {
            for(int j = 0; j < this.boardSize; j++)
            {
                if(this.state[i][j] == Boolean.TRUE)
                {
                    id += (int) Math.pow(2, exponent);
                }
                exponent--;
            }
        }
        return id;
    }

    /* Equality check */

    /**
     * Overrides the default equals() method
     * @param obj Object to be compared with this.
     * @return Result of the inspection.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        /* If compares to itself returns true */
        if(obj == this)
            return true;
        if(obj == null)
            return false;
        /* If not an instance of the GameState class returns false*/
        if(!(obj instanceof GameState))
            return false;

        /* Casting type so that variables are comparable */
        GameState other = (GameState)obj;

        if(this.boardSize!=other.getBoardSize())
            return false;

        /* Checking state values */
        for(int i = 0; i < this.boardSize; i++)
        {
            for(int j = 0; j < this.boardSize; j++)
            {
                if(!(this.state[i][j].equals(other.getCell(i,j))))
                    return false;
            }
        }
        return true;
    }

    /* Logging */

    /**
     * Returns the data of the current State.
     * @return String that stores data in a formatted way.
     */
    public String logState() {
        return "State id= " + this.getStateId() + ", boardSize= " + this.getBoardSize() + ", pattern=" + this.toString();
    }

    /**
     * Writes the essential data of the state to a String. Used to save a state.
     * @return String in <boolean value><whitespace><boolean value>... format.
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for(int i = 0; i < this.boardSize; i++)
        {
            for(int j = 0; j < this.boardSize; j++)
            {
                output.append(this.state[i][j].toString()).append(" ");
            }
        }
        return output.toString();
    }

    /**
     * Restores a state upon given formatted String. Used in constructor of string based creation.
     * @param input String that stores the state in
     * <boolean value><whitespace><boolean value>... format.
     * @return GameState state.
     * @throws IllegalArgumentException If input does not contains all boardsize*boardsize values
     * throws an exception.
     */
    @NonNull
    private Boolean[][] stringToState(@NonNull String input) throws IllegalArgumentException {
        Boolean[][] state = new Boolean[this.boardSize][this.boardSize];
        String[] tokens = input.split(" ");
        if(tokens.length != boardSize*boardSize)
            throw new IllegalArgumentException("Not enough field values to initialise state.");
        int index = 0;
        for(int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                state[i][j] = tokens[index].equals("true");
                index++;
            }
        }
        return state;
    }
}
