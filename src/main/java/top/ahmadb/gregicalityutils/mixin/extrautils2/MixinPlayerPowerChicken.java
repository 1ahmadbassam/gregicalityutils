package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.items.ItemChickenRing;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ahmadb.gregicalityutils.client.RingToggleKeybind;

// Mixin for the Chicken Ring
@Mixin(value = ItemChickenRing.PlayerPowerChicken.class, remap = false)
public class MixinPlayerPowerChicken {

    @Redirect(
        method = "tickClient",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z",
            remap = true
        )
    )
    private boolean redirectChickenJumpKey(KeyBinding instance) {
        if (!RingToggleKeybind.areRingsEnabled) {
            return false;
        }
        return instance.isKeyDown();
    }
}