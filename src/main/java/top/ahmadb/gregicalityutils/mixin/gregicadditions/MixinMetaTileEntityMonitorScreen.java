package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.covers.CoverDigitalInterface;
import gregicadditions.item.behaviors.monitorPlugin.MonitorPluginBaseBehavior;
import gregicadditions.machines.multi.centralmonitor.MetaTileEntityMonitorScreen;
import gregicadditions.utils.Tuple;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = MetaTileEntityMonitorScreen.class, remap = false)
public abstract class MixinMetaTileEntityMonitorScreen extends MetaTileEntityMultiblockPart {

    public MixinMetaTileEntityMonitorScreen(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
    }

    @Shadow public Tuple<BlockPos, EnumFacing> coverPos;
    @Shadow public CoverDigitalInterface coverTMP;
    @Shadow public CoverDigitalInterface.MODE mode;
    @Shadow public MonitorPluginBaseBehavior plugin;

    @Shadow public abstract CoverDigitalInterface getCoverFromPosSide(Tuple<BlockPos, EnumFacing> posFacing);
    @Shadow public abstract void setMode(Tuple<BlockPos, EnumFacing> cover, CoverDigitalInterface.MODE mode);

    /**
     * @author ahmadb
     * @reason Fix the 5-frame freeze. Do not cache the cover object across ticks, 
     * as chunk syncs will orphan the reference and permanently freeze the screen.
     */
    @Overwrite
    public boolean isActive() {
        if (this.coverPos != null && this.mode != CoverDigitalInterface.MODE.PROXY) {
            
            // FIX: Fetch the live cover from the world every tick instead of trusting coverTMP
            CoverDigitalInterface cover = this.getCoverFromPosSide(this.coverPos);
            
            if (cover != null && cover.isProxy()) {
                this.coverTMP = cover; // Update the pointer so renderScreen() can use the live one
                return true;
            }
            return false;
        }
        return this.plugin != null;
    }

    /**
     * @author ahmadb
     * @reason Prevent covers from unlinking due to cable network load race conditions.
     */
    @Overwrite
    public void updateCoverValid(List<Tuple<BlockPos, EnumFacing>> covers) {
        if (this.coverPos != null) {
            if (!covers.contains(this.coverPos)) {
                World world = this.getWorld();
                if (world != null && !world.isBlockLoaded(this.coverPos.getKey())) {
                    return; 
                }
                CoverDigitalInterface realCover = getCoverFromPosSide(this.coverPos);
                if (realCover == null || !realCover.isProxy()) {
                    setMode(null, CoverDigitalInterface.MODE.PROXY);
                }
            }
        }
    }
}