package top.ahmadb.gregicalityutils.capability;

import gregtech.api.items.armor.ArmorMetaItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Method;

public class BaublesDelegate {

    private static Method getBaublesHandlerMethod;
    private static boolean initialized = false;

    public static ItemStack getBauble(EntityPlayer player, EntityEquipmentSlot slot) {
        try {
            // Lazily initialize the reflection method
            if (!initialized) {
                Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                getBaublesHandlerMethod = apiClass.getMethod("getBaublesHandler", EntityPlayer.class);
                initialized = true;
            }

            if (getBaublesHandlerMethod != null) {
                // IBaublesItemHandler extends IItemHandler, so this cast is 100% safe and doesn't require Baubles imports
                IItemHandler handler = (IItemHandler) getBaublesHandlerMethod.invoke(null, player);
                
                if (handler != null) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack baubleStack = handler.getStackInSlot(i);
                        
                        if (!baubleStack.isEmpty() && baubleStack.getItem() instanceof ArmorMetaItem) {
                            ArmorMetaItem<?> armorItem = (ArmorMetaItem<?>) baubleStack.getItem();
                            
                            // Ensure we only return a chestplate when asked for a chestplate, etc.
                            if (armorItem.isValidArmor(baubleStack, slot, player)) {
                                return baubleStack;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Gracefully ignore errors if baubles changes or reflection fails
        }
        
        return ItemStack.EMPTY;
    }
}