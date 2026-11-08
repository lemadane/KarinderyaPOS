package ph.karinderya.pos.data.model;

public enum PaymentMethod {
    CASH("Cash"),
    GCASH("GCash"),
    MAYA("Maya");

     final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
