package dev.heitorrosa.nodebuffutils.mixin;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


// 26.3 moved client-side drop from LocalPlayer.drop(Z)Z to MultiPlayerGameMode.dropItem(LocalPlayer,Z)V.
@Mixin(MultiPlayerGameMode.class)
public class LocalPlayerMixin {

    @Inject(method = "dropItem", at = @At("HEAD"))
    private void nodebuffutils$flickClientUseOffOnDrop(final LocalPlayer player, final boolean all, final CallbackInfo ci) {
        if (!NodebuffUtilsConfig.oldEating || all) {
            return;
        }
        if (player.isUsingItem()
            && !player.getUseItem().isEmpty()
            && player.getUseItem().getUseAnimation() == ItemUseAnimation.EAT
            && player.getUseItem().getCount() > 1) {
            ((LocalPlayerAccessor) player).nodebuffutils$setStartedUsingItem(false);
        }
    }
}
