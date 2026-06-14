package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.util.ItemStackKey;
import gregtech.common.inventory.IItemList.InsertMode;
import gregtech.common.inventory.itemsource.ItemSource;
import gregtech.common.inventory.itemsource.ItemSourceList;
import gregtech.common.inventory.itemsource.NetworkItemInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(value = ItemSourceList.class, remap = false)
public abstract class MixinItemSourceList {

    @Shadow protected List<ItemSource> handlerInfoList;
    @Shadow protected Map<ItemStackKey, NetworkItemInfo> itemInfoMap;

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void smartInsertItem(ItemStackKey itemStack, int amount, boolean simulate, InsertMode insertMode, CallbackInfoReturnable<Integer> cir) {
        int amountToInsert = amount;

        // Smart Routing: Try to insert into inventories that ALREADY contain this item
        NetworkItemInfo existingInfo = this.itemInfoMap.get(itemStack);
        if (existingInfo != null) {
            for (ItemSource source : this.handlerInfoList) {
                // Check if this specific source currently holds the item (using a simulated 1-item extraction)
                if (source.extractItem(itemStack, 1, true) > 0) {
                    int inserted = source.insertItem(itemStack, amountToInsert, simulate);
                    amountToInsert -= inserted;
                    
                    if (amountToInsert == 0) {
                        cir.setReturnValue(amount); // All items neatly stacked with their existing pairs
                        return;
                    }
                }
            }
        }

        // Fallback Routing: If it is a completely new item or existing chests are full, use standard GTCE priority
        if (insertMode == InsertMode.HIGHEST_PRIORITY) {
            for (ItemSource itemSource : handlerInfoList) {
                int inserted = itemSource.insertItem(itemStack, amountToInsert, simulate);
                amountToInsert -= inserted;
                if (amountToInsert == 0) break;
            }
        } else {
            for (int i = handlerInfoList.size() - 1; i >= 0; i--) {
                ItemSource itemSource = handlerInfoList.get(i);
                int inserted = itemSource.insertItem(itemStack, amountToInsert, simulate);
                amountToInsert -= inserted;
                if (amountToInsert == 0) break;
            }
        }

        // Complete the intercept
        cir.setReturnValue(amount - amountToInsert);
    }
}