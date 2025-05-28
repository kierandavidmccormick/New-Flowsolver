package src;

// For now, this is just a container class
// Later on, this will also contain heuristics for move ranking
public class Move {
    private final Coordinate start;
    private final Coordinate direction;
    private final int score;

    public Move(Coordinate start, Coordinate direction, Board board) {
        this.start = start;
        this.direction = direction;
        this.score = Move.evaluateMove(start, direction, board);
    }

    static int evaluateMove(Coordinate start, Coordinate direction, Board board) {
        // Not yet implemented
        return 0;
    }

    public Coordinate getStart() {
        return start;
    }
    
    public Coordinate getDirection() {
        return direction;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "Move{" +
                "start=" + start +
                ", direction=" + direction.toString() +
                ", score=" + score +
                '}';
    }
}
