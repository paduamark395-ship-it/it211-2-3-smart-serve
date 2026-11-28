import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomerTest {

    @Test
    void validCustomerIsCreated() {
        Customer c = new Customer(1, "  Juan Dela Cruz ", "09171234567");
        assertEquals(1, c.getId());
        assertEquals("Juan Dela Cruz", c.getName());   // trimmed
        assertEquals("09171234567", c.getPhone());
    }

    @Test
    void emptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Customer(1, "   ", "09171234567"));
    }

    @Test
    void nullNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Customer(1, null, "09171234567"));
    }

    @Test
    void invalidPhoneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Ana", "abc123"));
        assertThrows(IllegalArgumentException.class, () -> new Customer(1, "Ana", "123"));
    }

    @Test
    void nonPositiveIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Customer(0, "Ana", "09171234567"));
        assertThrows(IllegalArgumentException.class, () -> new Customer(-5, "Ana", "09171234567"));
    }

    @Test
    void settersValidateToo() {
        Customer c = new Customer(2, "Ana", "09171234567");
        assertThrows(IllegalArgumentException.class, () -> c.setName(""));
        assertThrows(IllegalArgumentException.class, () -> c.setPhone("12"));
        c.setName("Ana Reyes");
        assertEquals("Ana Reyes", c.getName());
    }
}
