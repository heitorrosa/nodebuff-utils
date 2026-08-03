package dev.heitorrosa.nodebuffutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class NodebuffUtilsClient implements ClientModInitializer {

    private static final KeyMapping TOGGLE_EATING = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.nodebuffutils.toggle_eating", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));

    private static final KeyMapping TOGGLE_USE_PRIORITY = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.nodebuffutils.toggle_use_priority", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));

    @Override
    public void onInitializeClient() {
        NodebuffUtilsConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_EATING.consumeClick()) {
                NodebuffUtilsConfig.oldEating = !NodebuffUtilsConfig.oldEating;
                NodebuffUtilsConfig.save();
            }
            while (TOGGLE_USE_PRIORITY.consumeClick()) {
                NodebuffUtilsConfig.itemUsePriority = !NodebuffUtilsConfig.itemUsePriority;
                NodebuffUtilsConfig.save();
            }
        });
    }
}
