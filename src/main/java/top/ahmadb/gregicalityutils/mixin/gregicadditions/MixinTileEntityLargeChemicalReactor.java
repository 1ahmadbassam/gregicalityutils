package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.simple.TileEntityLargeChemicalReactor;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.common.blocks.BlockWireCoil;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityLargeChemicalReactor.class, remap = false)
public abstract class MixinTileEntityLargeChemicalReactor extends gregicadditions.capabilities.impl.GARecipeMapMultiblockController {

    // Dummy constructor required by Java to extend the superclass in a Mixin
    public MixinTileEntityLargeChemicalReactor(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, null, false, false, false);
    }

    @Shadow(remap = false)
    private int energyBonus;

    /**
     * @author ahmadb
     * @reason Change the energy bonus of the Large Chemical Reactor to scale continuously 
     * by 5% per coil tier, matching the Cracking Unit's behavior.
     */
    @Overwrite(remap = false)
    protected void formStructure(PatternMatchContext context) {
        // Run the base setup first
        super.formStructure(context);
        
        // Retrieve the coil temperature saved by the machine's block predicate
        int temperature = context.getOrDefault("reactorCoilTemperature", 0);
        
        int tier = 0;
        
        // Iterate through standard GregTech coils to find the matching tier index
        for (BlockWireCoil.CoilType type : BlockWireCoil.CoilType.values()) {
            if (type.getCoilTemperature() == temperature) {
                tier = type.ordinal();
                break;
            }
        }
        
        // Fallback for custom coils added by other mods (assumes GT's standard +900K per tier starting from Cupronickel at 1800K)
        if (tier == 0 && temperature > 1800) {
            tier = Math.max(0, (temperature - 1800) / 900);
        }
        
        // Apply 5% discount per tier. 
        // Capped at a maximum of 95% discount to ensure the recipe never reaches 0 EU/t.
        this.energyBonus = Math.min(95, tier * 5);
    }
}