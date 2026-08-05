package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.items.armor.ArmorMetaItem;
import gregtech.api.items.armor.IArmorLogic;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ArmorMetaItem.class, remap = false)
public interface IArmorMetaItemAccessor {

    @Invoker("getArmorLogic")
    IArmorLogic gu$getArmorLogic(ItemStack itemStack);

}