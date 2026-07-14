package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.cover.CoverBehavior;
import gregtech.api.util.ItemStackKey;
import gregtech.common.covers.CoverConveyor;
import gregtech.common.covers.filter.ItemFilterContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = CoverConveyor.class, remap = false)
public abstract class MixinCoverConveyor {

    @Shadow protected ItemFilterContainer itemFilterContainer;

    // A cross-tick cache to remember jammed items and the tick they failed on
    @Unique
    private final Map<ItemStackKey, Long> ahmadb$failedInsertionsCooldown = new HashMap<>();

    @Inject(
            method = "moveInventoryItems(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraftforge/items/IItemHandler;I)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void optimizeMoveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory, int maxTransferAmount, CallbackInfoReturnable<Integer> cir) {
        int itemsLeftToTransfer = maxTransferAmount;
        
        // FIX: Cast 'this' to CoverBehavior to access the inherited coverHolder field directly
        long currentTimer = ((CoverBehavior) (Object) this).coverHolder.getOffsetTimer();

        for (int srcIndex = 0; srcIndex < sourceInventory.getSlots(); srcIndex++) {
            
            // OPTIMIZATION 1: Peek at the slot. This prevents expensive deep-cloning of ItemStacks!
            ItemStack slotStack = sourceInventory.getStackInSlot(srcIndex);
            if (slotStack.isEmpty()) continue;

            ItemStackKey key = new ItemStackKey(slotStack);

            // OPTIMIZATION 2: Cooldown Backoff
            // If the Drawer Slave rejected this item recently (within the last 20 ticks / 1 second), skip the scan entirely.
            if (ahmadb$failedInsertionsCooldown.containsKey(key)) {
                if (currentTimer - ahmadb$failedInsertionsCooldown.get(key) < 20) {
                    continue;
                } else {
                    // Cooldown has expired, we are allowed to check the Drawer Slave again
                    ahmadb$failedInsertionsCooldown.remove(key);
                }
            }

            if (!itemFilterContainer.testItemStack(slotStack)) continue;

            // Now we simulate the actual extraction ONLY for valid, un-rejected items
            ItemStack sourceStack = sourceInventory.extractItem(srcIndex, itemsLeftToTransfer, true);
            if (sourceStack.isEmpty()) continue;

            // The massive Storage Drawer scan happens here
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInventory, sourceStack, true);
            int amountToInsert = sourceStack.getCount() - remainder.getCount();

            if (amountToInsert > 0) {
                sourceStack = sourceInventory.extractItem(srcIndex, amountToInsert, false);
                if (!sourceStack.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(targetInventory, sourceStack, false);
                    itemsLeftToTransfer -= sourceStack.getCount();

                    if (itemsLeftToTransfer <= 0) {
                        break;
                    }
                }
            } else {
                // The Drawer Slave rejected it. Put this item type on cooldown!
                ahmadb$failedInsertionsCooldown.put(key, currentTimer);
            }
        }
        cir.setReturnValue(maxTransferAmount - itemsLeftToTransfer);
    }
}