package ph.karinderya.pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import ph.karinderya.pos.trial.TrialState;
import ph.karinderya.pos.trial.TrialStatus;

public class TrialStatusTest {

  static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
  static final Duration LENGTH = Duration.ofDays(200);

  static TrialStatus at(Instant now, Instant lastSeen) {
    return TrialStatus.evaluate(START, lastSeen, now, LENGTH, false);
  }

  @Test
  public void activeRightAfterInstall() {
    TrialStatus status = at(START, null);
    assertEquals(TrialState.ACTIVE, status.state());
    assertEquals(200, status.daysLeft());
    assertFalse(status.shouldWarn());
  }

  @Test
  public void partialDayRoundsUp() {
    assertEquals(1, at(START.plus(LENGTH).minusSeconds(1), null).daysLeft());
  }

  @Test
  public void warnsInLastSevenDays() {
    assertFalse(at(START.plus(Duration.ofDays(192)).minusSeconds(1), null).shouldWarn());
    TrialStatus status = at(START.plus(Duration.ofDays(193)), null);
    assertEquals(7, status.daysLeft());
    assertTrue(status.shouldWarn());
  }

  @Test
  public void expiresExactlyAtDeadline() {
    TrialStatus status = at(START.plus(LENGTH), null);
    assertEquals(TrialState.EXPIRED, status.state());
    assertTrue(status.isBlocked());
  }

  @Test
  public void expiredLatchSurvivesClockRollback() {
    TrialStatus status = TrialStatus.evaluate(START, null, START, LENGTH, true);
    assertEquals(TrialState.EXPIRED, status.state());
  }

  @Test
  public void clockSetBackwardsBlocks() {
    Instant lastSeen = START.plus(Duration.ofDays(50));
    TrialStatus status = at(lastSeen.minus(Duration.ofDays(10)), lastSeen);
    assertEquals(TrialState.CLOCK_ROLLED_BACK, status.state());
    assertTrue(status.isBlocked());
  }

  @Test
  public void smallClockCorrectionIsTolerated() {
    Instant lastSeen = START.plus(Duration.ofDays(50));
    assertEquals(TrialState.ACTIVE, at(lastSeen.minus(Duration.ofMinutes(30)), lastSeen).state());
  }

  @Test
  public void exemptNeverBlocks() {
    assertFalse(TrialStatus.exempt().isBlocked());
    assertFalse(TrialStatus.exempt().shouldWarn());
  }
}
