package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.TileEntitySteamMixer;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.NotifiableFilteredFluidHandler;
import gregtech.api.capability.impl.NotifiableFluidTank;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.SteamMetaTileEntity;
import gregtech.api.recipes.ModHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = TileEntitySteamMixer.class, remap = false)
public abstract class MixinTileEntitySteamMixer extends SteamMetaTileEntity {

    public MixinTileEntitySteamMixer() {
        super(null, null, null, false);
    }

    /**
     * @author ahmadb
     * @reason Fix Nomifactory notification compatibility for the item import bus.
     */
    @Overwrite(remap = false)
    public IItemHandlerModifiable createImportItemHandler() {
        return new NotifiableItemStackHandler(4, this, false);
    }

    /**
     * @author ahmadb
     * @reason Fix Nomifactory notification compatibility for the item export bus.
     */
    @Overwrite(remap = false)
    public IItemHandlerModifiable createExportItemHandler() {
        return new NotifiableItemStackHandler(1, this, true);
    }

    /**
     * @author ahmadb
     * @reason Fix Nomifactory notification compatibility for the fluid import tanks (including Steam).
     */
    @Overwrite(remap = false)
    public FluidTankList createImportFluidHandler() {
        this.steamFluidTank = (new NotifiableFilteredFluidHandler(this.getSteamCapacity(), this, false)).setFillPredicate(ModHandler::isSteam);
        return new FluidTankList(false, 
                this.steamFluidTank, 
                new NotifiableFluidTank(32000, this, false), 
                new NotifiableFluidTank(32000, this, false));
    }

    /**
     * @author ahmadb
     * @reason Fix Nomifactory notification compatibility for the fluid export tanks.
     */
    @Overwrite(remap = false)
    protected FluidTankList createExportFluidHandler() {
        return new FluidTankList(false, new NotifiableFluidTank(32000, this, true));
    }
}