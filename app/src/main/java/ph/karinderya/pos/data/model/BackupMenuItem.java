package ph.karinderya.pos.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BackupMenuItem(
    @JsonProperty("item")
    MenuItem item,
    @JsonProperty("photoBase64")
    String photoBase64) {

  @JsonCreator
  public BackupMenuItem {
  }
}
