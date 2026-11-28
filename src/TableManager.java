import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the tables and their reservations.
 * Responsibility: show which tables are free and stop a table being booked twice.
 */
public class TableManager {
    private final List<Table> tables;
    private final List<Reservation> reservations = new ArrayList<>();
    private int nextId = 1;

    public TableManager(List<Table> tables) {
        if (tables == null || tables.isEmpty()) throw new IllegalArgumentException("At least one table is required.");
        this.tables = new ArrayList<>(tables);
    }

    public List<Table> getTables() { return Collections.unmodifiableList(tables); }
    public List<Reservation> getReservations() { return Collections.unmodifiableList(reservations); }

    /** All reservations made by one customer. A customer is recognised by phone number. */
    public List<Reservation> getReservationsOf(Customer customer) {
        List<Reservation> mine = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.getCustomer().getPhone().equals(customer.getPhone())) mine.add(r);
        }
        return mine;
    }

    /** Used when loading saved data: adds an existing reservation and keeps new ids unique. */
    public void restore(Reservation reservation) {
        reservations.add(reservation);
        nextId = Math.max(nextId, reservation.getId() + 1);
    }

    public Table findTable(int number) {
        for (Table t : tables) {
            if (t.getNumber() == number) return t;
        }
        throw new IllegalArgumentException("No table " + number + ".");
    }

    /** The reservation using this table at the given time, or null if nobody is using it. */
    public Reservation activeReservation(int tableNumber, LocalDateTime time) {
        findTable(tableNumber);
        for (Reservation r : reservations) {
            if (r.getTable().getNumber() == tableNumber && r.isActiveAt(time)) return r;
        }
        return null;
    }

    /** The earliest time, at or after the given time, when the table is not in use. */
    public LocalDateTime freeAgainAt(int tableNumber, LocalDateTime time) {
        LocalDateTime free = time;
        Reservation r = activeReservation(tableNumber, free);
        while (r != null) {                          // back-to-back bookings keep the table busy
            free = r.getEndTime();
            r = activeReservation(tableNumber, free);
        }
        return free;
    }

    public boolean isAvailable(int tableNumber, LocalDateTime when) {
        return isAvailable(tableNumber, when, Reservation.DEFAULT_DURATION_MINUTES);
    }

    /** True if the table is free for a stay that starts at 'when' and lasts 'minutes'. */
    public boolean isAvailable(int tableNumber, LocalDateTime when, int minutes) {
        findTable(tableNumber);
        for (Reservation r : reservations) {
            if (r.getTable().getNumber() == tableNumber && r.overlaps(when, minutes)) return false;
        }
        return true;
    }

    public List<Table> availableTables(LocalDateTime when) {
        List<Table> free = new ArrayList<>();
        for (Table t : tables) {
            if (isAvailable(t.getNumber(), when)) free.add(t);
        }
        return free;
    }

    public Reservation reserve(Customer customer, int tableNumber, LocalDateTime when, int partySize) {
        return reserve(customer, tableNumber, when, partySize, Reservation.DEFAULT_DURATION_MINUTES);
    }

    /** Reserve a table for a stay of the length the customer chose. */
    public Reservation reserve(Customer customer, int tableNumber, LocalDateTime when,
                               int partySize, int durationMinutes) {
        Table table = findTable(tableNumber);
        Reservation reservation = new Reservation(nextId, customer, table, when, partySize, durationMinutes);
        if (!isAvailable(tableNumber, when, durationMinutes)) {
            throw new IllegalStateException("Table " + tableNumber + " is already reserved at that time.");
        }
        reservations.add(reservation);
        nextId++;
        return reservation;
    }

    public Reservation findReservation(int reservationId) {
        for (Reservation r : reservations) {
            if (r.getId() == reservationId) return r;
        }
        throw new IllegalArgumentException("No reservation with id " + reservationId + ".");
    }

    /** The customer leaves early; the table is free from this moment. */
    public void checkOut(int reservationId, LocalDateTime now) {
        findReservation(reservationId).checkOut(now);
    }

    public void cancel(int reservationId) {
        boolean removed = reservations.removeIf(r -> r.getId() == reservationId);
        if (!removed) throw new IllegalArgumentException("No reservation with id " + reservationId + ".");
    }
}
