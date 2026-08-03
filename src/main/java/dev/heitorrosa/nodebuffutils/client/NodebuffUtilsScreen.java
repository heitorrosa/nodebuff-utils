package dev.heitorrosa.nodebuffutils.client;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class NodebuffUtilsScreen extends Screen {

    private final Screen lastScreen;

    public NodebuffUtilsScreen(Screen lastScreen) {
        super(Component.translatable("screen.nodebuffutils.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        Checkbox eating = Checkbox.builder(Component.translatable("screen.nodebuffutils.old_eating"), this.font)
            .pos(this.width / 2 - 155, this.height / 2 - 40)
            .selected(NodebuffUtilsConfig.oldEating)
            .onValueChange((checkbox, value) -> {
                NodebuffUtilsConfig.oldEating = value;
                NodebuffUtilsConfig.save();
            })
            .build();
        Checkbox priority = Checkbox.builder(Component.translatable("screen.nodebuffutils.item_use_priority"), this.font)
            .pos(this.width / 2 - 155, this.height / 2 - 12)
            .selected(NodebuffUtilsConfig.itemUsePriority)
            .onValueChange((checkbox, value) -> {
                NodebuffUtilsConfig.itemUsePriority = value;
                NodebuffUtilsConfig.save();
            })
            .build();
        this.addRenderableWidget(eating);
        this.addRenderableWidget(priority);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
            .bounds(this.width / 2 - 100, this.height / 2 + 24, 200, 20)
            .build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}
