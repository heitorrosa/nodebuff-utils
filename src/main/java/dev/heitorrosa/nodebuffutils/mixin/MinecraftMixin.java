package dev.heitorrosa.nodebuffutils.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.7.10-style click priority: right-click (use item) starts even while
 * left-clicking, instead of vanilla aborting {@code startUseItem} while
 * {@code MultiPlayerGameMode.isDestroying()}. The hold-repeat path keeps its
 * vanilla throttle (no machine-gunning buckets while attacking).
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    private MultiPlayerGameMode gameMode;

    @Shadow
    private void startUseItem() {
    }

    @Definition(id = "gameMode", field = "Lnet/minecraft/client/Minecraft;gameMode:Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;")
    @Definition(id = "isDestroying", method = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z")
    @Expression("this.gameMode.isDestroying()")
    @ModifyExpressionValue(method = "startUseItem", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean nodebuffutils$ignoreDestroying(final boolean original) {
        return NodebuffUtilsConfig.itemUsePriority ? false : original;
    }

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = 1)
    )
    private void nodebuffutils$noHoldRepeatWhileDestroying(final Minecraft instance) {
        if (!NodebuffUtilsConfig.itemUsePriority || !this.gameMode.isDestroying()) {
            this.startUseItem();
        }
    }
}
