package dev.heitorrosa.nodebuffutils.client;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.fabricmc.api.ClientModInitializer;

public class NodebuffUtilsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NodebuffUtilsConfig.load();
    }
}
