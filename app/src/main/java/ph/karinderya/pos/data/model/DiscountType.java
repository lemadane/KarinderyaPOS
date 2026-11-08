package ph.karinderya.pos.data.model;

public enum DiscountType {
  NONE("None", 0),
  SENIOR("Senior Citizen", 20),
  PWD(
      "PWD",
      20),
  CUSTOM("Custom discount", 0);

  final String label;
  final int percent;

  DiscountType(String label, int percent) {
    this.label = label;
    this.percent = percent;
  }

  public String label() {
    return label;
  }

  public int percent() {
    return percent;
  }
}
