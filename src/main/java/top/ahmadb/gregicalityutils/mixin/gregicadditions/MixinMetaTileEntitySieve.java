package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.integrations.exnihilocreatio.machines.MetaTileEntitySieve;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MetaTileEntitySieve.class, remap = false)
public abstract class MixinMetaTileEntitySieve extends SimpleMachineMetaTileEntity {

    public MixinMetaTileEntitySieve() {
        // Explicitly calling the parent constructor to satisfy the compiler
        super(null, null, null, 0);
    }

    @Inject(method = "createExportItemHandler", at = @At("HEAD"), cancellable = true)
    private void gu$increaseElectricSieveCapacity(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        // Signature: NotifiableItemStackHandler(int size, MetaTileEntity entity, boolean isOutput)
        cir.setReturnValue(new NotifiableItemStackHandler(54, this, true));
    }
}