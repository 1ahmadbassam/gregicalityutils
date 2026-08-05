package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import baubles.api.BaubleType;
import gregicadditions.armor.PowerlessJetpack;
import gregtech.api.items.armor.ArmorMetaItem.ArmorMetaValueItem;
import gregtech.api.items.metaitem.stats.IItemCapabilityProvider;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ahmadb.gregicalityutils.capability.BaubleProvider;

@Mixin(value = PowerlessJetpack.class, remap = false)
public abstract class MixinPowerlessJetpack {

    @SuppressWarnings("rawtypes")
    @Inject(method = "addToolComponents", at = @At("TAIL"))
    private void gu$injectBaubleCapability(ArmorMetaValueItem mvi, CallbackInfo ci) {
        if (Loader.isModLoaded("baubles")) {
            mvi.addComponents(new IItemCapabilityProvider() {
                @Override
                public ICapabilityProvider createProvider(ItemStack itemStack) {
                    return new BaubleProvider(itemStack, BaubleType.BODY);
                }
            });
        }
    }
}