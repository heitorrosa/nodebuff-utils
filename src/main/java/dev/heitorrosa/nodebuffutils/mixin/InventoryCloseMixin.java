package dev.heitorrosa.nodebuffutils.mixin;

import dev.heitorrosa.nodebuffutils.NodebuffUtilsConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LocalPlayer.class)
public class InventoryCloseMixin {

    @Inject(method = "closeContainer", at = @At("HEAD"))
    private void nodebuffutils$clearCarriedOnClose(final CallbackInfo ci) {
        if (!NodebuffUtilsConfig.dropCarriedOnClose) {
            return;
        }
        LocalPlayer self = (LocalPlayer) (Object) this;
        AbstractContainerMenu menu = self.containerMenu;
        if (menu != null && !menu.getCarried().isEmpty()) {
            menu.setCarried(ItemStack.EMPTY);
        }
    }
}
