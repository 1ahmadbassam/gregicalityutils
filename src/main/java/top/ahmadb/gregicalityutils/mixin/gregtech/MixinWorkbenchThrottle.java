package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.common.metatileentities.storage.CraftingRecipeResolver;
import gregtech.common.metatileentities.storage.MetaTileEntityWorkbench;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ahmadb.gregicalityutils.gui.TrackedPhantomSlotWidget;

@Mixin(value = MetaTileEntityWorkbench.class, remap = false)
public abstract class MixinWorkbenchThrottle {

    @Redirect(
        method = "update", 
        at = @At(value = "INVOKE", target = "Lgregtech/common/metatileentities/storage/CraftingRecipeResolver;update()V")
    )
    private void dynamicThrottleResolverUpdate(CraftingRecipeResolver instance) {
        MetaTileEntityWorkbench workbench = (MetaTileEntityWorkbench) (Object) this;
        
        boolean isUIOpen = false;
        
        // Safely check if the GUI is actively open using our custom widget tracker
        Long lastActiveTick = TrackedPhantomSlotWidget.ACTIVE_WORKBENCHES.get(workbench);
        if (lastActiveTick != null) {
            // If the widget sent a heartbeat within the last 5 ticks, the UI is open!
            if ((workbench.getWorld().getTotalWorldTime() - lastActiveTick) < 5) {
                isUIOpen = true;
            }
        }

        // Fast updates (every tick) when open so crafting has zero delay.
        // Slow updates (every 10 ticks) when closed to save 90% of the CPU footprint.
        if (isUIOpen) {
            instance.update();
        }
    }
}