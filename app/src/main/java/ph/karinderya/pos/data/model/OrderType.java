package ph.karinderya.pos.data.model;

public enum OrderType {
    DINE_IN("Dine-in"),
    TAKE_OUT("Take-out");

     final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
