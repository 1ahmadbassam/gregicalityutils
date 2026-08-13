package top.ahmadb.gregicalityutils.mixin.ae2;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ahmadb.gregicalityutils.ae2.ISmartBlockingDuality;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinDualityInterface implements ISmartBlockingDuality {

    @Shadow private boolean isBlocking() { throw new AssertionError(); }
    @Shadow private boolean hasItemsToSend() { throw new AssertionError(); }
    @Shadow private boolean hasItemsToSendFacing() { throw new AssertionError(); }

    @Unique private int gcu$lastInputHash = 0;
    @Unique private int gcu$currentPatternHash = 0;
    
    // Per-interface persistent state
    @Unique private boolean gcu$isSmartBlocking = false;

    @Override
    public boolean gcu$isSmartBlocking() { 
        return this.gcu$isSmartBlocking; 
    }

    @Override
    public void gcu$setSmartBlocking(boolean state) { 
        this.gcu$isSmartBlocking = state; 
    }

    // Save the state to NBT
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void gcu$writeToNBT(NBTTagCompound data, CallbackInfo ci) {
        data.setBoolean("gcu_smartBlocking", this.gcu$isSmartBlocking);
    }

    // Load the state from NBT
    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void gcu$readFromNBT(NBTTagCompound data, CallbackInfo ci) {
        this.gcu$isSmartBlocking = data.getBoolean("gcu_smartBlocking");
    }

    @Inject(method = "isBusy", at = @At("HEAD"), cancellable = true)
    private void gcu$smartBlockIsBusy(CallbackInfoReturnable<Boolean> cir) {
        // Only bypass if both blocking AND smart blocking are active on THIS interface
        if (this.isBlocking() && this.gcu$isSmartBlocking) {
            boolean busy = this.hasItemsToSend() || this.hasItemsToSendFacing();
            cir.setReturnValue(busy);
        }
    }

    @Inject(method = "pushPattern", at = @At("HEAD"))
    private void gcu$beforePushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table, CallbackInfoReturnable<Boolean> cir) {
        if (patternDetails != null) {
            this.gcu$currentPatternHash = patternDetails.hashCode();
        }
    }

    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void gcu$afterPushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table, CallbackInfoReturnable<Boolean> cir) {
        this.gcu$currentPatternHash = 0;
    }

    @Inject(method = "isBlocking", at = @At("RETURN"), cancellable = true)
    private void gcu$smartIsBlocking(CallbackInfoReturnable<Boolean> cir) {
        // Only apply GTNH bypass if THIS interface is set to smart blocking
        if (cir.getReturnValueZ() && this.gcu$isSmartBlocking) {
            if (this.gcu$currentPatternHash != 0 && this.gcu$currentPatternHash == this.gcu$lastInputHash) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "onPushPatternSuccess", at = @At("HEAD"))
    private void gcu$onPushPatternSuccess(ICraftingPatternDetails pattern, CallbackInfo ci) {
        if (pattern != null) {
            this.gcu$lastInputHash = pattern.hashCode();
        }
    }
}