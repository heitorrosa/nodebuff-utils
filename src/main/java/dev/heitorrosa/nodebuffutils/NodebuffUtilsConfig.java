package dev.heitorrosa.nodebuffutils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public final class NodebuffUtilsConfig {

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nodebuff-utils.json");

    public static boolean oldEating = true;
    public static boolean itemUsePriority = true;
    public static boolean dropCarriedOnClose = true;

    private NodebuffUtilsConfig() {
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                JsonObject json = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
                if (hasBoolean(json, "oldEating")) {
                    oldEating = json.get("oldEating").getAsBoolean();
                }
                if (hasBoolean(json, "itemUsePriority")) {
                    itemUsePriority = json.get("itemUsePriority").getAsBoolean();
                }
                if (hasBoolean(json, "dropCarriedOnClose")) {
                    dropCarriedOnClose = json.get("dropCarriedOnClose").getAsBoolean();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean hasBoolean(JsonObject json, String key) {
        return json != null && json.has(key) && json.get(key) != null && json.get(key).isJsonPrimitive();
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("oldEating", oldEating);
        json.addProperty("itemUsePriority", itemUsePriority);
        json.addProperty("dropCarriedOnClose", dropCarriedOnClose);
        try {
            Files.writeString(PATH, json.toString());
        } catch (IOException ignored) {
        }
    }
}
