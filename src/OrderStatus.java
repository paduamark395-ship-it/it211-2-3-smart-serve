/**
 * The stages an order goes through in Smart Serve.
 * Responsibility: define the allowed order of statuses and block invalid jumps.
 */
public enum OrderStatus {
    RECEIVED, PREPARING, READY, COMPLETED;

    /** Returns the next status in the flow. Throws if the order is already completed. */
    public OrderStatus next() {
        if (this == COMPLETED) {
            throw new IllegalStateException("Order is already completed.");
        }
        return values()[ordinal() + 1];
    }

    /** An order may only move forward by exactly one step. */
    public boolean canMoveTo(OrderStatus target) {
        return target != null && target.ordinal() == this.ordinal() + 1;
    }

    /** Customer-friendly text for the tracking screen. */
    public String getLabel() {
        switch (this) {
            case RECEIVED:  return "Order received";
            case PREPARING: return "Being prepared";
            case READY:     return "Ready for pickup";
            default:        return "Completed";
        }
    }
}
