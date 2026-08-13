package top.ahmadb.gregicalityutils.mixin.ae2;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.DualityInterface;
import appeng.util.InventoryAdaptor;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ahmadb.gregicalityutils.ae2.ISmartBlockingDuality;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinDualityInterface implements ISmartBlockingDuality {

    @Shadow private boolean isBlocking() { throw new AssertionError(); }
    @Shadow private boolean hasItemsToSend() { throw new AssertionError(); }
    @Shadow private boolean hasItemsToSendFacing() { throw new AssertionError(); }
    @Shadow abstract boolean isCustomInvBlocking(TileEntity te, EnumFacing s);

    // Per-interface persistent state
    @Unique private boolean gcu$isSmartBlocking = false;
    
    @Unique private InventoryCrafting gcu$currentTable = null;

    @Override
    public boolean gcu$isSmartBlocking() { 
        return this.gcu$isSmartBlocking; 
    }

    @Override
    public void gcu$setSmartBlocking(boolean state) { 
        this.gcu$isSmartBlocking = state; 
    }

    @Override
    public InventoryCrafting gcu$getCurrentTable() {
        return this.gcu$currentTable;
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
        this.gcu$currentTable = table;
    }

    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void gcu$afterPushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table, CallbackInfoReturnable<Boolean> cir) {
        this.gcu$currentTable = null;
    }

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE", target = "Lappeng/helpers/DualityInterface;invIsBlocked(Lappeng/util/InventoryAdaptor;)Z", remap = false))
    private boolean gcu$redirectInvIsBlockedPush(DualityInterface instance, InventoryAdaptor ad) {
        if (!this.gcu$isSmartBlocking) {
            return ad.containsItems();
        }
        
        if (!ad.containsItems()) return false;
        
        return !gcu$isTargetValidForSmartBlocking(ad);
    }

    @Redirect(method = "pushPattern", at = @At(value = "INVOKE", target = "Lappeng/helpers/DualityInterface;isCustomInvBlocking(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Z", remap = false))
    private boolean gcu$redirectCustomInvBlockedPush(DualityInterface instance, TileEntity te, EnumFacing s) {
        if (!this.gcu$isSmartBlocking) {
            return this.isCustomInvBlocking(te, s);
        }
        
        appeng.util.inv.BlockingInventoryAdaptor ad = appeng.util.inv.BlockingInventoryAdaptor.getAdaptor(te, s.getOpposite());
        if (ad == null || !ad.containsBlockingItems()) {
            return false;
        }
        
        return !gcu$isTargetValidForSmartBlocking(ad);
    }

    @Unique
    private boolean gcu$isTargetValidForSmartBlocking(Iterable<appeng.util.inv.ItemSlot> ad) {
        if (this.gcu$currentTable == null) return false;
        
        for (appeng.util.inv.ItemSlot slot : ad) {
            ItemStack inMachine = slot.getItemStack();
            if (inMachine != null && !inMachine.isEmpty()) {
                boolean foundMatch = false;
                for (int i = 0; i < this.gcu$currentTable.getSizeInventory(); i++) {
                    ItemStack inTable = this.gcu$currentTable.getStackInSlot(i);
                    if (inTable != null && !inTable.isEmpty()) {
                        if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(inMachine, inTable)) {
                            foundMatch = true;
                            break;
                        }
                    }
                }
                if (!foundMatch) {
                    return false; // Found an alien item, so it IS blocked
                }
            }
        }
        return true; // All items matched recipe inputs
    }
}