package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.integrations.exnihilocreatio.machines.SteamSieve;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SteamSieve.class, remap = false)
public class MixinSteamSieve {

    @Inject(method = "createExportItemHandler", at = @At("HEAD"), cancellable = true)
    private void gu$increaseSteamSieveCapacity(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        // Overrides the hardcoded 24 with 54
        cir.setReturnValue(new ItemStackHandler(54));
    }
}