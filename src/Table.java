/**
 * A table in the restaurant.
 * Responsibility: hold the table's number and how many people it seats.
 */
public class Table {
    private final int number;
    private final int seats;

    public Table(int number, int seats) {
        if (number <= 0) throw new IllegalArgumentException("Table number must be positive.");
        if (seats <= 0) throw new IllegalArgumentException("Seats must be at least 1.");
        this.number = number;
        this.seats = seats;
    }

    public int getNumber() { return number; }
    public int getSeats() { return seats; }

    @Override
    public String toString() { return "Table " + number + " (" + seats + " seats)"; }
}
