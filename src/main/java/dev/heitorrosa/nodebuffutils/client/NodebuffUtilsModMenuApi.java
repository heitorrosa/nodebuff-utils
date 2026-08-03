package dev.heitorrosa.nodebuffutils.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration: adds a Config button to the Nodebuff Utils entry in
 * the mod list, opening {@link NodebuffUtilsScreen}.
 */
public class NodebuffUtilsModMenuApi implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NodebuffUtilsScreen::new;
    }
}
