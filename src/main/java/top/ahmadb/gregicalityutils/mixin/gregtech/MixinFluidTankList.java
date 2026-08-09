package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.capability.impl.FluidTankList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ahmadb.gregicalityutils.FastAE2Detector;

@Mixin(value = FluidTankList.class, remap = false)
public class MixinFluidTankList {

    @Shadow(remap = false) protected boolean allowSameFluidFill;

    @Redirect(
        method = "fillTanksImpl", 
        at = @At(value = "FIELD", target = "Lgregtech/api/capability/impl/FluidTankList;allowSameFluidFill:Z"), 
        remap = false
    )
    private boolean redirectAllowSameFluidFill(FluidTankList instance) {
        if (this.allowSameFluidFill) return true;
        return FastAE2Detector.isAE2();
    }

    @Inject(method = "allowSameFluidFill", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideAllowSameFluidFillGetter(CallbackInfoReturnable<Boolean> cir) {
        if (this.allowSameFluidFill) return;
        
        if (FastAE2Detector.isAE2()) {
            cir.setReturnValue(true);
        }
    }
}