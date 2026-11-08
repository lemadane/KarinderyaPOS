package ph.karinderya.pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ph.karinderya.pos.data.model.BackupData;
import ph.karinderya.pos.data.model.BackupMenuItem;
import ph.karinderya.pos.data.model.CartLine;
import ph.karinderya.pos.data.model.MenuItem;
import ph.karinderya.pos.util.Json;

public class JsonTest {

    @Test
    public void backupRoundTripsRecordsWithUuidAndInstant() throws Exception {
        MenuItem item = new MenuItem(UUID.randomUUID(), "Sinigang", "Sour soup", "Ulam", 9000, null, true, true,
                TestData.CREATED, TestData.CREATED);
        BackupData data = new BackupData(BackupData.CURRENT_VERSION, Instant.parse("2026-10-19T12:00:00Z"),
                List.of(new BackupMenuItem(item, "AAEC")), List.of(TestData.order()), List.of());

        String json = Json.mapper().writeValueAsString(data);
        assertTrue(json, json.contains("\"exportedAt\":\"2026-10-19T12:00:00Z\""));
        assertTrue(json, json.contains("\"id\":\"" + item.id() + "\""));

        BackupData restored = Json.mapper().readValue(json, BackupData.class);
        assertEquals(data, restored);
    }

    @Test
    public void olderBackupMenuItemDefaultsMealOfDayToFalse() throws Exception {
        String json = """
                {
                  "id": "00000000-0000-0000-0000-000000000001",
                  "name": "Sinigang",
                  "description": "Sour soup",
                  "category": "Ulam",
                  "priceCents": 9000,
                  "photoPath": null,
                  "available": true,
                  "createdAt": "2026-10-19T12:00:00Z",
                  "updatedAt": "2026-10-19T12:00:00Z"
                }
                """;

        MenuItem restored = Json.mapper().readValue(json, MenuItem.class);
        assertFalse(restored.mealOfDay());
    }

    @Test
    public void ignoresUnknownFieldsAndOnlyMapsComponents() throws Exception {
        CartLine line = new CartLine(UUID.randomUUID(), "Turon", 2000, 3, null);
        String json = Json.mapper().writeValueAsString(line);
        assertTrue(json, !json.contains("lineTotalCents"));
        CartLine restored = Json.mapper().readValue(json.replace("{", "{\"future\":1,"), CartLine.class);
        assertEquals(line, restored);
    }
}
