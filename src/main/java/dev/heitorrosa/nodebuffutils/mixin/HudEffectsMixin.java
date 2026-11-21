package dev.heitorrosa.nodebuffutils.mixin;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Hud.class)
public class HudEffectsMixin {

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void nodebuffutils$hideHudEffects(final GuiGraphicsExtractor extractor, final DeltaTracker tracker, final CallbackInfo ci) {
        if (NodebuffUtilsConfig.hideStatusEffects) {
            ci.cancel();
        }
    }
}
