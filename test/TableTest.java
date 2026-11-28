import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TableTest {
    @Test
    void validTableIsCreated() {
        Table t = new Table(3, 4);
        assertEquals(3, t.getNumber());
        assertEquals(4, t.getSeats());
    }

    @Test
    void badNumberOrSeatsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Table(0, 4));
        assertThrows(IllegalArgumentException.class, () -> new Table(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Table(1, -2));
    }
}
