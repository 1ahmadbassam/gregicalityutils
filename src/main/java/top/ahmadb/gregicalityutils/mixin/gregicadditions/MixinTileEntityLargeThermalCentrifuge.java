package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.simple.TileEntityLargeThermalCentrifuge;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.common.blocks.BlockWireCoil;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityLargeThermalCentrifuge.class, remap = false)
public abstract class MixinTileEntityLargeThermalCentrifuge extends gregicadditions.capabilities.impl.GARecipeMapMultiblockController {

    public MixinTileEntityLargeThermalCentrifuge(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, null, false, false, false);
    }

    @Shadow(remap = false)
    private int speedBonus;

    /**
     * @author ahmadb
     * @reason Fix the NBT string key mismatch ("reactorCoilTemperature" -> "blastFurnaceTemperature") 
     * and apply continuous 5% scaling per coil tier.
     */
    @Overwrite(remap = false)
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        
        // Grab the correct key saved by the predicate
        int temperature = context.getOrDefault("blastFurnaceTemperature", 0);
        
        int tier = 0;
        for (BlockWireCoil.CoilType type : BlockWireCoil.CoilType.values()) {
            if (type.getCoilTemperature() == temperature) {
                tier = type.ordinal();
                break;
            }
        }
        if (tier == 0 && temperature > 1800) {
            tier = Math.max(0, (temperature - 1800) / 900);
        }
        
        // Apply a 5% speed bonus per tier, capped at 95%
        this.speedBonus = Math.min(95, tier * 5);
    }
}