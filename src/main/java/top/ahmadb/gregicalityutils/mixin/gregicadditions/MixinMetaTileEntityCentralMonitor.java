package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.centralmonitor.MetaTileEntityCentralMonitor;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.pipenet.tile.TileEntityPipeBase;
import gregtech.common.pipelike.cable.net.EnergyNet;
import gregtech.common.pipelike.cable.net.WorldENet;
import gregtech.common.pipelike.cable.tile.TileEntityCable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;

@Mixin(value = MetaTileEntityCentralMonitor.class, remap = false)
public abstract class MixinMetaTileEntityCentralMonitor extends MultiblockWithDisplayBase {

    public MixinMetaTileEntityCentralMonitor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Shadow
    private EnergyContainerList inputEnergy;

    @Shadow
    private WeakReference<EnergyNet> currentEnergyNet;

    /**
     * @author ahmadb
     * @reason Force runtime variables to initialize on world load if they are missing.
     * This prevents the tick-loop crash at tick 20 when it attempts to consume EU.
     */
    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdate(CallbackInfo ci) {
        if (!this.getWorld().isRemote && this.isStructureFormed() && this.inputEnergy == null) {
            // The multiblock loaded from NBT, but bypassed formStructure().
            // Forcing a pattern check cleanly drops and re-forms the structure instantly,
            // properly populating inputEnergy, activeNodes, and covers.
            this.checkStructurePattern();
        }
    }

    /**
     * @author ahmadb
     * @reason Fix NullPointerException when currentEnergyNet is uninitialized on cable scan.
     */
    @Overwrite
    private EnergyNet getEnergyNet() {
        if (!this.getWorld().isRemote) {
            TileEntity te = this.getWorld().getTileEntity(this.getPos().offset(frontFacing.getOpposite()));
            if (te instanceof TileEntityPipeBase) {
                TileEntityPipeBase<?, ?> tileEntityCable = (TileEntityCable) te;
                
                // Safely check the reference to avoid NPE
                EnergyNet energyNet = this.currentEnergyNet != null ? this.currentEnergyNet.get() : null;
                
                if (energyNet != null && energyNet.isValid() && energyNet.containsNode(tileEntityCable.getPipePos())) {
                    return energyNet; 
                }
                
                WorldENet worldENet = (WorldENet) tileEntityCable.getPipeBlock().getWorldPipeNet(tileEntityCable.getPipeWorld());
                energyNet = worldENet.getNetFromPos(tileEntityCable.getPipePos());
                
                if (energyNet != null) {
                    this.currentEnergyNet = new WeakReference<>(energyNet);
                }
                return energyNet;
            }
        }
        return null;
    }
}