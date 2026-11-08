package ph.karinderya.pos.trial;

public enum TrialState {
  ACTIVE,
  EXPIRED,
  /** The device clock is earlier than a time the app has already seen. */
  CLOCK_ROLLED_BACK,
  /** Debug builds skip the trial. */
  EXEMPT;

  public boolean isBlocked() {
    return this == EXPIRED || this == CLOCK_ROLLED_BACK;
  }
}
