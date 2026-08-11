package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.simple.TileEntityLargeThermalCentrifuge;
import gregtech.api.recipes.Recipe;
import gregtech.api.util.GTUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "gregicadditions.machines.multi.simple.TileEntityLargeThermalCentrifuge$LargeThermalCentrifugeWorkableHandler", remap = false)
public abstract class MixinLargeThermalCentrifugeWorkableHandler extends gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.LargeSimpleMultiblockRecipeLogic {

    public MixinLargeThermalCentrifugeWorkableHandler(gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController tileEntity) {
        super(tileEntity, 100, 100, 100, 1);
    }

    /**
     * @author ahmadb
     * @reason Fix fatal math error where a percentage of EU/t was being subtracted from the duration instead of duration itself.
     */
    @Overwrite(remap = false)
    protected void setupRecipe(Recipe recipe) {
        long maxVoltage = getMaxVoltage();
        if (metaTileEntity instanceof TileEntityLargeThermalCentrifuge) {
            // Apply the bottleneck fix we established earlier!
            maxVoltage = Math.min(maxVoltage, ((TileEntityLargeThermalCentrifuge) metaTileEntity).maxVoltage);
        }
        
        int[] resultOverclock = calculateOverclock(recipe.getEUt(), maxVoltage, recipe.getDuration());
        this.progressTime = 1;

        TileEntityLargeThermalCentrifuge metaTileEntity = (TileEntityLargeThermalCentrifuge) getMetaTileEntity();
        int speedBonus = ((TileEntityLargeThermalCentrifugeAccessor) metaTileEntity).gu$getSpeedBonus();

        // FIX: Subtract a percentage of the DURATION (resultOverclock[1]), not EU/t (resultOverclock[0])
        resultOverclock[1] -= (int) (resultOverclock[1] * speedBonus * 0.01f);

        setMaxProgress(resultOverclock[1]);
        this.recipeEUt = resultOverclock[0];
        this.fluidOutputs = GTUtility.copyFluidList(recipe.getFluidOutputs());
        int tier = getMachineTierForRecipe(recipe);
        this.itemOutputs = GTUtility.copyStackList(recipe.getResultItemOutputs(Integer.MAX_VALUE, random, tier));
        
        if (this.wasActiveAndNeedsUpdate) {
            this.wasActiveAndNeedsUpdate = false;
        } else {
            this.setActive(true);
        }
    }
}