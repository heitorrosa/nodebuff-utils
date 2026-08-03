package dev.heitorrosa.nodebuffutils.mixin;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.7.10/1.8.9 drop-eating: drops register on the server in real time while
 * the eat keeps running in the backend; each drop flicks the CLIENT-side use
 * state off so the eating slow-down and sprint debuff drop, producing the
 * classic "little jitters" at near-normal speed.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Shadow
    private boolean startedUsingItem;

    @Inject(method = "drop", at = @At("HEAD"))
    private void nodebuffutils$flickClientUseOffOnDrop(final boolean all, final CallbackInfoReturnable<Boolean> cir) {
        if (!NodebuffUtilsConfig.oldEating || all) {
            return; // drop-all empties the hand and cancels the eat — same as 1.7.10/1.8.9
        }
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (self.isUsingItem()
            && !self.getUseItem().isEmpty()
            && self.getUseItem().getUseAnimation() == ItemUseAnimation.EAT
            && self.getUseItem().getCount() > 1) {
            this.startedUsingItem = false;
        }
    }
}
