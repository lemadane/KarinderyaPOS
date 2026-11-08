package ph.karinderya.pos.ui.menu;

import java.util.List;

import ph.karinderya.pos.data.model.MenuItem;

/** Everything the dish editor needs from the database; {@code item} is null for a new dish. */
public record MenuEditorData(List<String> categories, MenuItem item) {
}
