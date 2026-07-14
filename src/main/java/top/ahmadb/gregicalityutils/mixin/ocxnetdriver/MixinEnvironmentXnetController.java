package top.ahmadb.gregicalityutils.mixin.ocxnetdriver;

import gregtech.api.capability.impl.ItemHandlerProxy;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.dave.ocxnetdriver.driver.controller.EnvironmentXnetController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EnvironmentXnetController.class, remap = false)
public abstract class MixinEnvironmentXnetController {

    @Redirect(
            method = {"transferItem", "getItems", "store"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/tileentity/TileEntity;getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Ljava/lang/Object;",
                    remap = false
            )
    )
    private Object redirectGetCapability(TileEntity instance, Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            
            if (instance instanceof MetaTileEntityHolder) {
                MetaTileEntity mte = ((MetaTileEntityHolder) instance).getMetaTileEntity();
                
                if (mte != null) {
                    // ONLY bypass if GregTech is using the restrictive proxy (Processing Machines)
                    if (mte.getItemInventory() instanceof ItemHandlerProxy) {
                        return new CombinedInvWrapper(
                                mte.getImportItems(),
                                mte.getExportItems()
                        );
                    }
                    // If it's not a proxy (e.g., GT Chests), let the vanilla logic handle it below
                }
            }
        }
        
        // Normal vanilla/Forge behavior
        return instance.getCapability(capability, facing);
    }
}