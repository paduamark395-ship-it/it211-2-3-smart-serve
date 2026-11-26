/**
 * A person who places orders or reserves tables in Smart Serve.
 * Responsibility: hold and validate the customer's own details.
 */
public class Customer {
    private final int id;
    private String name;
    private String phone;

    public Customer(int id, String name, String phone) {
        if (id <= 0) {
            throw new IllegalArgumentException("Customer id must be positive.");
        }
        this.id = id;
        setName(name);
        setPhone(phone);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.matches("\\d{7,15}")) {
            throw new IllegalArgumentException("Phone must be 7 to 15 digits.");
        }
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "Customer #" + id + ": " + name + " (" + phone + ")";
    }
}
