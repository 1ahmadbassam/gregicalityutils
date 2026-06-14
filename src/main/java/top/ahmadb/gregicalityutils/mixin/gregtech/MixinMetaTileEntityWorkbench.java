package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.gui.widgets.PhantomSlotWidget;
import gregtech.common.metatileentities.storage.MetaTileEntityWorkbench;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.ahmadb.gregicalityutils.gui.TrackedPhantomSlotWidget;

@Mixin(value = MetaTileEntityWorkbench.class, remap = false)
public abstract class MixinMetaTileEntityWorkbench {

    @Redirect(method = "createWorkbenchTab", at = @At(value = "NEW", target = "gregtech/api/gui/widgets/PhantomSlotWidget"))
    private PhantomSlotWidget replacePhantomSlotWithTrackedSlot(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition, int yPosition) {
        return new TrackedPhantomSlotWidget(itemHandler, slotIndex, xPosition, yPosition, (MetaTileEntityWorkbench) (Object) this);
    }
}