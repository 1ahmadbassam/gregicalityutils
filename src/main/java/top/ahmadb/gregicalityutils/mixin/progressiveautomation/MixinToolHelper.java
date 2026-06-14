package top.ahmadb.gregicalityutils.mixin.progressiveautomation;

import com.vanhal.progressiveautomation.common.util.ToolHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ToolHelper.class, remap = false)
public class MixinToolHelper {

    private static final String GT_TAG = "GT.ToolStats";

    /**
     * Checks if the item is a GregTech 1.12.2 Axe (meta_tool with damage/metadata 3)
     */
    private static boolean isGregTechAxe(ItemStack stack) {
        return stack.getItem().getRegistryName() != null && 
               stack.getItem().getRegistryName().toString().equals("gregtech:meta_tool") && 
               stack.getItemDamage() == 3;
    }

    @Inject(method = "getType", at = @At("HEAD"), cancellable = true)
    private static void gregicality_getType(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        if (!itemStack.isEmpty() && isGregTechAxe(itemStack)) {
            cir.setReturnValue(2); // 2 is PA's TYPE_AXE
        }
    }

    @Inject(method = "getLevel", at = @At("HEAD"), cancellable = true)
    private static void gregicality_getLevel(ItemStack itemStack, CallbackInfoReturnable<Integer> cir) {
        if (!itemStack.isEmpty() && isGregTechAxe(itemStack)) {
            
            // Default to PA Wood Tier (0) so the GUI accepts it instantly
            int mappedLevel = 0; 

            if (itemStack.hasTagCompound() && itemStack.getTagCompound().hasKey(GT_TAG)) {
                NBTTagCompound stats = itemStack.getTagCompound().getCompoundTag(GT_TAG);
                if (stats.hasKey("HarvestLevel")) {
                    int gtLevel = stats.getInteger("HarvestLevel");
                    
                    // Modpack Custom Logic: 
                    // GT Tiers 1, 2, 3 -> PA Wood Tier (0)   -> Accepted by Wooden Chopper
                    // GT Tiers 4+      -> PA Iron Tier (2)   -> Accepted by Iron Chopper
                    if (gtLevel >= 4) {
                        mappedLevel = 2;
                    } else {
                        mappedLevel = 0;
                    }
                }
            }
            
            cir.setReturnValue(mappedLevel);
        }
    }

    @Inject(method = "isBroken", at = @At("HEAD"), cancellable = true)
    private static void gregicality_isBroken(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!itemStack.isEmpty() && isGregTechAxe(itemStack)) {
            
            boolean broken = false;

            if (itemStack.hasTagCompound() && itemStack.getTagCompound().hasKey(GT_TAG)) {
                NBTTagCompound stats = itemStack.getTagCompound().getCompoundTag(GT_TAG);
                if (stats.hasKey("Dmg") && stats.hasKey("MaxDurability")) {
                    long currentDmg = stats.getLong("Dmg");
                    long maxDur = stats.getLong("MaxDurability");
                    broken = (currentDmg >= maxDur);
                }
            }
            cir.setReturnValue(broken);
        }
    }
}