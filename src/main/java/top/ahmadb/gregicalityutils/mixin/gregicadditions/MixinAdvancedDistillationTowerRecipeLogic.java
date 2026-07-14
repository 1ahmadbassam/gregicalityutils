package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.GAUtility;
import gregicadditions.machines.multi.advance.MetaTileEntityAdvancedDistillationTower;
import gregicadditions.machines.multi.simple.MultiRecipeMapMultiblockController.MultiRecipeMapMultiblockRecipeLogic;
import gregicadditions.utils.GALog;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap; // FIX: Added missing import
import gregtech.api.util.InventoryUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;
import java.util.stream.Collectors;

// FIX: Because the inner class is public, we can target it cleanly using 'value'
@Mixin(value = MetaTileEntityAdvancedDistillationTower.AdvancedDistillationTowerRecipeLogic.class, remap = false)
public abstract class MixinAdvancedDistillationTowerRecipeLogic extends MultiRecipeMapMultiblockRecipeLogic {

    public MixinAdvancedDistillationTowerRecipeLogic(RecipeMapMultiblockController tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack, RecipeMap<?>[] recipeMaps) {
        super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack, recipeMaps);
    }

    // FIX: Removed the @Shadow methods. Because we extend MultiRecipeMapMultiblockRecipeLogic, 
    // we inherit these methods natively and can just call them directly!

    /**
     * @author ahmadb
     * @reason Strip out the dead forceRecipeRecheck() call to prevent Nomifactory crashes.
     */
    @Overwrite(remap = false)
    protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
        int maxItemsLimit = this.getStack();
        int EUt;
        int duration;
        int currentTier = getOverclockingTier(maxVoltage);
        int tierNeeded;
        int minMultiplier = Integer.MAX_VALUE;

        int mode = ((MetaTileEntityAdvancedDistillationTower) this.getMetaTileEntity()).getRecipeMapIndex();

        tierNeeded = Math.max(1, GAUtility.getTierByVoltage(matchingRecipe.getEUt()));
        maxItemsLimit *= currentTier - tierNeeded;

        // REMOVED: forceRecipeRecheck();

        if (mode == 0) { // Distillation tower = 2 parallel/oc, max 8
            maxItemsLimit *= 2;
            maxItemsLimit = Math.max(1, maxItemsLimit);
            maxItemsLimit = Math.min(8, maxItemsLimit);
        } else { // Others = 8 parallel/oc, max 64
            maxItemsLimit *= 8;
            maxItemsLimit = Math.max(1, maxItemsLimit);
            maxItemsLimit = Math.min(64, maxItemsLimit);
        }

        Set<ItemStack> countIngredients = new HashSet<>();
        if (!matchingRecipe.getInputs().isEmpty()) {
            this.findIngredients(countIngredients, inputs); // Called natively via inheritance
            minMultiplier = Math.min(maxItemsLimit, this.getMinRatioItem(countIngredients, matchingRecipe, maxItemsLimit));
        }

        Map<String, Integer> countFluid = new HashMap<>();
        if (!matchingRecipe.getFluidInputs().isEmpty()) {
            this.findFluid(countFluid, fluidInputs);
            minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, maxItemsLimit));
        }

        if (minMultiplier == Integer.MAX_VALUE) {
            GALog.logger.error("Cannot calculate ratio of items for large multiblocks");
            return null;
        }

        EUt = matchingRecipe.getEUt();
        duration = matchingRecipe.getDuration();

        List<CountableIngredient> newRecipeInputs = new ArrayList<>();
        List<FluidStack> newFluidInputs = new ArrayList<>();
        List<ItemStack> outputI = new ArrayList<>();
        List<FluidStack> outputF = new ArrayList<>();
        this.multiplyInputsAndOutputs(newRecipeInputs, newFluidInputs, outputI, outputF, matchingRecipe, minMultiplier);

        RecipeBuilder<?> newRecipe = recipeMap.recipeBuilder();
        this.copyChancedItemOutputs(newRecipe, matchingRecipe, minMultiplier);

        List<ItemStack> totalOutputs = newRecipe.getChancedOutputs().stream().map(Recipe.ChanceEntry::getItemStack).collect(Collectors.toList());
        totalOutputs.addAll(outputI);
        
        boolean canFitOutputs = InventoryUtils.simulateItemStackMerge(totalOutputs, this.getOutputInventory());
        if (!canFitOutputs) {
            return matchingRecipe; // Parent logic will catch the full output bus and stall cleanly
        }

        newRecipe.inputsIngredients(newRecipeInputs)
                .fluidInputs(newFluidInputs)
                .outputs(outputI)
                .fluidOutputs(outputF)
                .EUt(Math.max(1, EUt * this.getEUtPercentage() / 100))
                .duration((int) Math.max(3, duration * (this.getDurationPercentage() / 100.0)));

        return newRecipe.build().getResult();
    }
}