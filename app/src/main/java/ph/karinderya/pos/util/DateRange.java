package ph.karinderya.pos.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Half-open range {@code [from, to)}. */
public record DateRange(Instant from, Instant to) {

    public static DateRange lastDays(int days) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        return new DateRange(today.minusDays(days - 1L).atStartOfDay(zone).toInstant(),
                today.plusDays(1).atStartOfDay(zone).toInstant());
    }

    public static DateRange today() {
        return lastDays(1);
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(from) && instant.isBefore(to);
    }
}
