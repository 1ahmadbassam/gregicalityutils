package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.integrations.exnihilocreatio.machines.SteamSieve;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.SteamMetaTileEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = SteamSieve.class, remap = false)
public abstract class MixinSteamSieve extends SteamMetaTileEntity {

    public MixinSteamSieve() {
        super(null, null, null, false);
    }

    /**
     * @author ahmadb
     * @reason Fix Nomifactory notification compatibility for the import bus.
     */
    @Overwrite(remap = false)
    public IItemHandlerModifiable createImportItemHandler() {
        return new NotifiableItemStackHandler(2, this, false);
    }

    /**
     * @author ahmadb
     * @reason Fix Nomifactory notification compatibility and increase capacity to 54.
     */
    @Overwrite(remap = false)
    public IItemHandlerModifiable createExportItemHandler() {
        return new NotifiableItemStackHandler(54, this, true);
    }
}