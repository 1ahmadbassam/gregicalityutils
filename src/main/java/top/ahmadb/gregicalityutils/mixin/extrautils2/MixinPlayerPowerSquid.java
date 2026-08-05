package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.items.ItemChickenRing;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ahmadb.gregicalityutils.client.RingToggleKeybind;

// Mixin for the Flying Squid Ring
@Mixin(value = ItemChickenRing.PlayerPowerSquid.class, remap = false)
public class MixinPlayerPowerSquid {

    @Redirect(
        method = "tickClient",
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z",
            remap = true // Must be true because KeyBinding is vanilla code
        )
    )
    private boolean redirectSquidJumpKey(KeyBinding instance) {
        // If our custom toggle is false, force the ring to think the jump key isn't pressed
        if (!RingToggleKeybind.areRingsEnabled) {
            return false;
        }
        // Otherwise, return the actual state of the jump key
        return instance.isKeyDown();
    }
}