package src;

/**
 * Custom exception for invalid moves 
 * Exists to let the solver know when it's found an invalid move
 * It's valid and expected for the solver to generate invalid moves, that's an integral part of the backtracking algorithm
 * This exception is just a way to communicate that information back to the solver
 * 
 * Potential improvements:
 * - This might be an inefficient way to handle invalid moves, as exceptions can be costly in terms of performance
*/
public class InvalidMoveException extends Throwable{
    private final String message;
    private final Location location;

    public InvalidMoveException(String message, Location location) {
        this.message = message;
        this.location = location;
    }

    public String getMessage() {
        return message;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "InvalidMoveException{" +
                "message='" + message + '\'' +
                ", location=" + location +
                '}';
    }
}
