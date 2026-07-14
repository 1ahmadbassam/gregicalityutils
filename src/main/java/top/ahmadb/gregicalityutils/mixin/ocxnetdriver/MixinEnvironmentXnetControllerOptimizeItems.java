package top.ahmadb.gregicalityutils.mixin.ocxnetdriver;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import net.minecraft.item.ItemStack;
import org.dave.ocxnetdriver.driver.controller.EnvironmentXnetController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = EnvironmentXnetController.class, remap = false)
public abstract class MixinEnvironmentXnetControllerOptimizeItems {

    @Inject(
            method = "getItems(Lli/cil/oc/api/machine/Context;Lli/cil/oc/api/machine/Arguments;)[Ljava/lang/Object;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void injectGetItemsFormat(Context context, Arguments args, CallbackInfoReturnable<Object[]> cir) {
        Object[] ret = cir.getReturnValue();
        
        // Ensure the original method succeeded and returned the List<ItemStack>
        if (ret != null && ret.length > 0 && ret[0] instanceof List) {
            @SuppressWarnings("unchecked")
            List<ItemStack> originalList = (List<ItemStack>) ret[0];
            
            // Flexibly detect the 'advanced' boolean argument.
            // If side is omitted: getItems(pos, true) -> args[1] is boolean
            // If side is provided: getItems(pos, side, true) -> args[2] is boolean
            boolean advanced = false;
            if (args.isBoolean(1)) {
                advanced = args.checkBoolean(1);
            } else if (args.isBoolean(2)) {
                advanced = args.checkBoolean(2);
            }

            // Create the new root table
            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("slots", originalList.size());

            // Create the sparse contents array
            List<Map<String, Object>> contentList = new ArrayList<>();
            
            for (int i = 0; i < originalList.size(); i++) {
                ItemStack stack = originalList.get(i);
                
                // Skip empty slots entirely
                if (stack != null && !stack.isEmpty()) {
                    Map<String, Object> itemData = new HashMap<>();
                    
                    // Add 1 to the slot index because Lua tables are 1-indexed!
                    itemData.put("slot", i + 1); 
                    itemData.put("label", stack.getDisplayName());
                    itemData.put("size", stack.getCount());

                    // Only append heavy data if advanced is true
                    if (advanced) {
                        itemData.put("name", stack.getItem().getRegistryName() != null ? stack.getItem().getRegistryName().toString() : "unknown");
                        itemData.put("damage", stack.getItemDamage());
                        itemData.put("maxDamage", stack.getMaxDamage());
                        itemData.put("maxSize", stack.getMaxStackSize());
                        itemData.put("hasTag", stack.hasTagCompound());
                    }
                    contentList.add(itemData);
                }
            }
            
            finalResult.put("content", contentList);
            
            // Overwrite the return value with our memory-efficient map
            cir.setReturnValue(new Object[]{ finalResult });
        }
    }
}