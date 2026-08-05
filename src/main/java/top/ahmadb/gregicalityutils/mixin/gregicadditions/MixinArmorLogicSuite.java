package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import baubles.api.BaubleType;
import gregicadditions.armor.ArmorLogicSuite;
import gregtech.api.items.armor.ArmorMetaItem.ArmorMetaValueItem;
import gregtech.api.items.metaitem.stats.IItemCapabilityProvider;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ahmadb.gregicalityutils.capability.BaubleProvider;

@Mixin(value = ArmorLogicSuite.class, remap = false)
public abstract class MixinArmorLogicSuite {

    @Shadow
    protected EntityEquipmentSlot SLOT;

    @SuppressWarnings("rawtypes")
    @Inject(method = "addToolComponents", at = @At("TAIL"))
    private void gu$injectBaubleCapability(ArmorMetaValueItem mvi, CallbackInfo ci) {
        if (Loader.isModLoaded("baubles")) {
            BaubleType type = (SLOT == EntityEquipmentSlot.HEAD) ? BaubleType.HEAD :
                              (SLOT == EntityEquipmentSlot.CHEST) ? BaubleType.BODY :
                              BaubleType.TRINKET;

            mvi.addComponents(new IItemCapabilityProvider() {
                @Override
                public ICapabilityProvider createProvider(ItemStack itemStack) {
                    return new BaubleProvider(itemStack, type);
                }
            });
        }
    }
}