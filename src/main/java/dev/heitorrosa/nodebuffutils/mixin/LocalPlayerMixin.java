package dev.heitorrosa.nodebuffutils.mixin;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Shadow
    private boolean startedUsingItem;

    @Inject(method = "drop", at = @At("HEAD"))
    private void nodebuffutils$flickClientUseOffOnDrop(final boolean all, final CallbackInfoReturnable<Boolean> cir) {
        if (!NodebuffUtilsConfig.oldEating || all) {
            return;
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
