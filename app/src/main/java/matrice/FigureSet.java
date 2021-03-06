package matrice;

import androidx.annotation.Nullable;

/**
 * Enum for signaling type of chosen figure set to Game handler
 */
public enum FigureSet {
    PLUMP(0), TICTACTOE(1), PLUSMINUS(2);

    private final int id;

    /**
     * Default constructor for enum type object
     * @param id desired id of the new object
     */
    FigureSet(int id) {
        this.id = id;
    }

    /**
     * Function for getting enum object when passing Integer id
     * @param id of the enum type
     * @return Returns the type matching the given id or null if missing value
     */
    @Nullable
    public static FigureSet fromId(int id) {
        for(FigureSet type : values()) {
            if(type.getId() == id)
                return type;
        }
        return null;
    }

    public int getId() {
        return this.id;
    }
}
