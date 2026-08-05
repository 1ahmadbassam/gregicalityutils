package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.armor.NightvisionGoggles;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ahmadb.gregicalityutils.capability.BaublesDelegate;

@Mixin(value = NightvisionGoggles.class, remap = false)
public abstract class MixinNightvisionGoggles {

    @Redirect(
        method = "onArmorTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;getItemStackFromSlot(Lnet/minecraft/inventory/EntityEquipmentSlot;)Lnet/minecraft/item/ItemStack;"
        )
    )
    private ItemStack gu$redirectNVSlotCheck(EntityPlayer player, EntityEquipmentSlot slot) {
        ItemStack vanillaStack = player.getItemStackFromSlot(slot);
        
        if (!vanillaStack.isEmpty() && vanillaStack.getItem() instanceof gregtech.api.items.armor.ArmorMetaItem) {
            return vanillaStack;
        }
        
        if (Loader.isModLoaded("baubles")) {
            ItemStack baubleStack = BaublesDelegate.getBauble(player, slot);
            if (!baubleStack.isEmpty()) {
                return baubleStack;
            }
        }
        return vanillaStack;
    }
}