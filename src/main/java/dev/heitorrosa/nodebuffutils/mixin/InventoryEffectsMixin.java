package dev.heitorrosa.nodebuffutils.mixin;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EffectsInInventory.class)
public class InventoryEffectsMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void nodebuffutils$hideInventoryEffects(final GuiGraphicsExtractor extractor, final int x, final int y, final CallbackInfo ci) {
        if (NodebuffUtilsConfig.hideStatusEffects) {
            ci.cancel();
        }
    }
}
