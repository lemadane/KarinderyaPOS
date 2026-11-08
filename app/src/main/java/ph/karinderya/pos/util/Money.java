package ph.karinderya.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Philippine peso helpers. Amounts are stored as centavos ({@code long}) to avoid rounding errors. */
public final class Money {

  public static final String PESO = "\u20B1";

  Money() {
  }

  public static String format(long cents) {
    DecimalFormat format =
        new DecimalFormat(
            "#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.US));
    String amount =
        format.format(BigDecimal.valueOf(Math.abs(cents), 2));
    return (cents < 0 ? "-" : "") + PESO + amount;
  }

  public static String toPlain(long cents) {
    return BigDecimal.valueOf(
        cents,
        2).toPlainString();
  }

  /** Parses user input such as "1,250.5" or "₱80" into centavos. */
  public static long parse(String text) {
    if (text == null) {
      throw new NumberFormatException(
          "Amount is required");
    }
    String cleaned =
        text.replace(
            PESO,
            "").replace(
                ",",
                "")
        .trim();
    if (cleaned.isEmpty()) {
      throw new NumberFormatException(
          "Amount is required");
    }
    BigDecimal value =
        new BigDecimal(cleaned).setScale(
            2,
            RoundingMode.HALF_UP);
    if (value.signum() < 0) {
      throw new NumberFormatException(
          "Amount cannot be negative");
    }
    return value.movePointRight(2)
        .longValueExact();
  }

  public static long parseOrZero(String text) {
    try {
      return parse(text);
    } catch (NumberFormatException | ArithmeticException e) {
      return 0;
    }
  }
}
