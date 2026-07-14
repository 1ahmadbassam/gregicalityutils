package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.GAUtility;
import gregicadditions.capabilities.impl.GAMultiblockRecipeLogic;
import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.util.InventoryUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(value = gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.LargeSimpleMultiblockRecipeLogic.class, remap = false)
public abstract class MixinLargeSimpleMultiblockRecipeLogic extends GAMultiblockRecipeLogic {

    public MixinLargeSimpleMultiblockRecipeLogic(RecipeMapMultiblockController tileEntity) {
        super(tileEntity);
    }

    // Shadowing the fields from the inner class
    @Shadow(remap = false) @Final private int EUtPercentage;
    @Shadow(remap = false) @Final private int durationPercentage;
    @Shadow(remap = false) @Final private int chancePercentage;
    @Shadow(remap = false) @Final private int stack;

    // Shadowing the helper methods from the inner class
    @Shadow(remap = false) protected abstract void findIngredients(Set<ItemStack> countIngredients, IItemHandlerModifiable inputs);
    @Shadow(remap = false) protected abstract int getMinRatioItem(Set<ItemStack> countIngredients, Recipe r, int maxItemsLimit);
    @Shadow(remap = false) protected abstract void findFluid(Map<String, Integer> countFluid, IMultipleTankHandler fluidInputs);
    @Shadow(remap = false) protected abstract int getMinRatioFluid(Map<String, Integer> countFluid, Recipe r, int maxItemsLimit);
    @Shadow(remap = false) protected abstract void multiplyInputsAndOutputs(List<CountableIngredient> newRecipeInputs, List<FluidStack> newFluidInputs, List<ItemStack> outputI, List<FluidStack> outputF, Recipe r, int multiplier);
    @Shadow(remap = false) protected abstract void copyChancedItemOutputs(RecipeBuilder<?> newRecipe, Recipe oldRecipe, int multiplier);

    /**
     * @author ahmadb
     * @reason Integrate GTCEu notification system and deadlock prevention state trackers.
     */
    @Overwrite(remap = false)
    protected void trySearchNewRecipeCombined() {
        long maxVoltage = getMaxVoltage();
        if (metaTileEntity instanceof LargeSimpleRecipeMapMultiblockController) {
            maxVoltage = ((LargeSimpleRecipeMapMultiblockController) metaTileEntity).maxVoltage;
        }
        
        Recipe currentRecipe = null;
        IItemHandlerModifiable importInventory = getInputInventory();
        IMultipleTankHandler importFluids = getInputTank();

        // Use the event-driven notification checks instead of laggy dirty polling
        if (hasNotifiedInputs() ||
            previousRecipe == null ||
            !previousRecipe.matches(false, importInventory, importFluids)) {
            
            currentRecipe = findRecipe(maxVoltage, importInventory, importFluids);
        } else {
            currentRecipe = previousRecipe;
        }

        if (currentRecipe != null) {
            this.previousRecipe = currentRecipe;
        }
        
        // GTCEu Deadlock prevention flag
        this.invalidInputsForRecipes = (currentRecipe == null);

        if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe)) {
            setupRecipe(currentRecipe);
        }
        
        // Clear the notification list so it doesn't poll every tick
        metaTileEntity.getNotifiedItemInputList().clear();
        metaTileEntity.getNotifiedFluidInputList().clear();
    }

    /**
     * @author ahmadb
     * @reason Strip forceRecipeRecheck() call to prevent crashes on Nomifactory forks.
     */
    @Overwrite(remap = false)
    protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
        int maxItemsLimit = this.stack;
        int EUt;
        int duration;
        int currentTier = getOverclockingTier(maxVoltage);
        int tierNeeded;
        int minMultiplier = Integer.MAX_VALUE;

        tierNeeded = Math.max(1, GAUtility.getTierByVoltage(matchingRecipe.getEUt()));
        maxItemsLimit *= currentTier - tierNeeded;
        maxItemsLimit = Math.max(1, maxItemsLimit);

        // REMOVED: forceRecipeRecheck();

        Set<ItemStack> countIngredients = new HashSet<>();
        if (!matchingRecipe.getInputs().isEmpty()) {
            this.findIngredients(countIngredients, inputs);
            minMultiplier = Math.min(maxItemsLimit, this.getMinRatioItem(countIngredients, matchingRecipe, maxItemsLimit));
        }

        Map<String, Integer> countFluid = new HashMap<>();
        if (!matchingRecipe.getFluidInputs().isEmpty()) {
            this.findFluid(countFluid, fluidInputs);
            minMultiplier = Math.min(minMultiplier, this.getMinRatioFluid(countFluid, matchingRecipe, maxItemsLimit));
        }

        if (minMultiplier == Integer.MAX_VALUE) {
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
        copyChancedItemOutputs(newRecipe, matchingRecipe, minMultiplier);

        List<ItemStack> totalOutputs = newRecipe.getChancedOutputs().stream().map(Recipe.ChanceEntry::getItemStack).collect(Collectors.toList());
        totalOutputs.addAll(outputI);
        
        boolean canFitOutputs = InventoryUtils.simulateItemStackMerge(totalOutputs, this.getOutputInventory());
        if (!canFitOutputs) {
            return matchingRecipe; // Falls back to 1x processing, which will be caught by setupAndConsumeRecipeInputs handling isOutputsFull
        }

        newRecipe.inputsIngredients(newRecipeInputs)
                .fluidInputs(newFluidInputs)
                .outputs(outputI)
                .fluidOutputs(outputF)
                .EUt(Math.max(1, EUt * this.EUtPercentage / 100))
                .duration((int) Math.max(3, duration * (this.durationPercentage / 100.0)));

        return newRecipe.build().getResult();
    }
}