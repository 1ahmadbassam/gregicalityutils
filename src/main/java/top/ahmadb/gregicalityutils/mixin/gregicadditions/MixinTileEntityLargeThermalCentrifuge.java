package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.item.components.MotorCasing;
import gregicadditions.machines.multi.simple.TileEntityLargeThermalCentrifuge;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.common.blocks.BlockWireCoil;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityLargeThermalCentrifuge.class, remap = false)
public abstract class MixinTileEntityLargeThermalCentrifuge extends gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController {

    // Dummy constructor matching the superclass
    public MixinTileEntityLargeThermalCentrifuge(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, null, 0, 0, 0, 0);
    }

    @Shadow(remap = false)
    private int speedBonus;

    /**
     * @author ahmadb
     * @reason Fix the NBT string key mismatch ("reactorCoilTemperature" -> "blastFurnaceTemperature"), 
     * apply continuous 5% scaling per coil tier, and RESTORE the maxVoltage motor calculation.
     */
    @Overwrite(remap = false)
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        
        MotorCasing.CasingType motor = context.getOrDefault("Motor", MotorCasing.CasingType.MOTOR_LV);
        int min = motor.getTier();
        this.maxVoltage = (long) (Math.pow(4, min) * 8);
        
        // Grab the correct key saved by the predicate
        int temperature = context.getOrDefault("blastFurnaceTemperature", 0);
        
        int tier = 0;
        for (BlockWireCoil.CoilType type : BlockWireCoil.CoilType.values()) {
            if (type.getCoilTemperature() == temperature) {
                tier = type.ordinal();
                break;
            }
        }
        
        // Fallback for custom coils added by other mods
        if (tier == 0 && temperature > 1800) {
            tier = Math.max(0, (temperature - 1800) / 900);
        }
        
        // Apply a 5% speed bonus per tier, capped at 95%
        this.speedBonus = Math.min(95, tier * 5);
    }
}