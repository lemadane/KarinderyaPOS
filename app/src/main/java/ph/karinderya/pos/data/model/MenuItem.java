package ph.karinderya.pos.data.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MenuItem(
    @JsonProperty("id")
    UUID id,
    @JsonProperty("name")
    String name,
    @JsonProperty("description")
    String description,
    @JsonProperty("category")
    String category,
    @JsonProperty("priceCents")
    long priceCents,
    @JsonProperty("photoPath")
    String photoPath,
    @JsonProperty("available")
    boolean available,
    @JsonProperty("mealOfDay")
    boolean mealOfDay,
    @JsonProperty("createdAt")
    Instant createdAt,
    @JsonProperty("updatedAt")
    Instant updatedAt) {

  @JsonCreator
  public MenuItem {
  }

  public boolean hasPhoto() {
    return photoPath != null && !photoPath.isEmpty();
  }

  public MenuItem withId(UUID newId) {
    return new MenuItem(
        newId,
        name,
        description,
        category,
        priceCents,
        photoPath,
        available,
        mealOfDay,
        createdAt,
        updatedAt);
  }

  public MenuItem withPhotoPath(String newPath) {
    return new MenuItem(
        id,
        name,
        description,
        category,
        priceCents,
        newPath,
        available,
        mealOfDay,
        createdAt,
        updatedAt);
  }
}
