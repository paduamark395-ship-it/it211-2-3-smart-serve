import java.time.LocalDateTime;

/**
 * A booking of one table at a certain time, for a length of time the customer chooses.
 * Responsibility: hold the booking, know when it ends, and let the customer check out early.
 */
public class Reservation {
    public static final int DEFAULT_DURATION_MINUTES = 90;
    public static final int MIN_DURATION_MINUTES = 30;
    public static final int MAX_DURATION_MINUTES = 240;

    private final int id;
    private final Customer customer;
    private final Table table;
    private final LocalDateTime dateTime;
    private final int partySize;
    private final int durationMinutes;
    private LocalDateTime checkedOutAt;          // null until the customer checks out early

    /** Booking for the default stay of 90 minutes. */
    public Reservation(int id, Customer customer, Table table, LocalDateTime dateTime, int partySize) {
        this(id, customer, table, dateTime, partySize, DEFAULT_DURATION_MINUTES);
    }

    /** Booking for a stay the customer chooses (30 to 240 minutes). */
    public Reservation(int id, Customer customer, Table table, LocalDateTime dateTime,
                       int partySize, int durationMinutes) {
        this(id, customer, table, dateTime, partySize, durationMinutes, true);
    }

    private Reservation(int id, Customer customer, Table table, LocalDateTime dateTime,
                        int partySize, int durationMinutes, boolean mustBeInFuture) {
        if (id <= 0) throw new IllegalArgumentException("Reservation id must be positive.");
        if (customer == null) throw new IllegalArgumentException("Customer is required.");
        if (table == null) throw new IllegalArgumentException("Table is required.");
        if (dateTime == null) throw new IllegalArgumentException("Date and time are required.");
        // A one-minute grace lets a customer book "right now" by typing the current time to the minute.
        if (mustBeInFuture && dateTime.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new IllegalArgumentException("Reservation time cannot be in the past.");
        }
        if (partySize <= 0) throw new IllegalArgumentException("Party size must be at least 1.");
        if (partySize > table.getSeats()) {
            throw new IllegalArgumentException("Table " + table.getNumber() + " only seats " + table.getSeats() + ".");
        }
        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("Stay time must be between " + MIN_DURATION_MINUTES
                    + " and " + MAX_DURATION_MINUTES + " minutes.");
        }
        this.id = id;
        this.customer = customer;
        this.table = table;
        this.dateTime = dateTime;
        this.partySize = partySize;
        this.durationMinutes = durationMinutes;
    }

    /** Rebuilds a saved reservation. A stay that is already in progress has a start in the past. */
    public static Reservation restored(int id, Customer customer, Table table, LocalDateTime dateTime,
                                       int partySize, int durationMinutes, LocalDateTime checkedOutAt) {
        Reservation r = new Reservation(id, customer, table, dateTime, partySize, durationMinutes, false);
        r.checkedOutAt = checkedOutAt;
        return r;
    }

    public int getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Table getTable() { return table; }
    public LocalDateTime getDateTime() { return dateTime; }
    public int getPartySize() { return partySize; }
    public int getDurationMinutes() { return durationMinutes; }
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public boolean isCheckedOut() { return checkedOutAt != null; }

    /** The time the table is free again: the check-out time if they left early, otherwise the planned end. */
    public LocalDateTime getEndTime() {
        return checkedOutAt != null ? checkedOutAt : dateTime.plusMinutes(durationMinutes);
    }

    /** True while the party is using the table: from the start time until the end time. */
    public boolean isActiveAt(LocalDateTime time) {
        return !time.isBefore(dateTime) && time.isBefore(getEndTime());
    }

    /** The customer leaves early. Only works while the stay is in progress. */
    public void checkOut(LocalDateTime now) {
        if (now == null) throw new IllegalArgumentException("Time is required.");
        if (checkedOutAt != null) throw new IllegalStateException("You have already checked out.");
        if (now.isBefore(dateTime)) {
            throw new IllegalStateException("This reservation has not started yet. Cancel it instead.");
        }
        if (!now.isBefore(dateTime.plusMinutes(durationMinutes))) {
            throw new IllegalStateException("This reservation has already ended.");
        }
        checkedOutAt = now;
    }

    /** True if a new booking that starts at 'start' and lasts 'minutes' would overlap this one. */
    public boolean overlaps(LocalDateTime start, int minutes) {
        return start.isBefore(getEndTime()) && dateTime.isBefore(start.plusMinutes(minutes));
    }

    /** True if another default-length booking starting at this time would overlap this one. */
    public boolean clashesWith(LocalDateTime other) {
        return overlaps(other, DEFAULT_DURATION_MINUTES);
    }

    @Override
    public String toString() {
        return "Reservation #" + id + ": " + table + " for " + partySize + " at " + dateTime
                + " (" + durationMinutes + " min)";
    }
}
