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

    private NodebuffUtilsConfig() {
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                JsonObject json = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
                oldEating = json.get("oldEating").getAsBoolean();
                itemUsePriority = json.get("itemUsePriority").getAsBoolean();
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("oldEating", oldEating);
        json.addProperty("itemUsePriority", itemUsePriority);
        try {
            Files.writeString(PATH, json.toString());
        } catch (IOException ignored) {
        }
    }
}
