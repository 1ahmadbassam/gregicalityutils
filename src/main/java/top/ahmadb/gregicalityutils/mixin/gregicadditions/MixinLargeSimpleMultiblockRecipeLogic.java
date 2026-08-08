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
    @Shadow(remap = false) protected abstract void copyChancedItemOutputs(RecipeBuilder<?> newRecipe, Recipe oldRecipe, int multiplier);


    /**
     * @author ahmadb
     * @reason Prevent parallelism cache trapping by forcing the machine to always recalculate the recipe multiplier.
     */
    @Overwrite(remap = false)
    protected void trySearchNewRecipeCombined() {
        long maxVoltage = getMaxVoltage();
        if (metaTileEntity instanceof LargeSimpleRecipeMapMultiblockController) {
            maxVoltage = Math.min(maxVoltage, ((LargeSimpleRecipeMapMultiblockController) metaTileEntity).maxVoltage);;
        }
        
        IItemHandlerModifiable importInventory = getInputInventory();
        IMultipleTankHandler importFluids = getInputTank();

        // Always bypass the cache. Caching a 1x recipe traps the machine into running 1x forever.
        Recipe currentRecipe = findRecipe(maxVoltage, importInventory, importFluids);
        if (currentRecipe != null) {
            this.previousRecipe = currentRecipe;
        }
        
        // GTCEu Deadlock prevention flag
        this.invalidInputsForRecipes = (currentRecipe == null);

        if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe)) {
            setupRecipe(currentRecipe);
            
            // Clear the notification list ONLY because the recipe successfully started
            metaTileEntity.getNotifiedItemInputList().clear();
            metaTileEntity.getNotifiedFluidInputList().clear();
        } else if (currentRecipe == null) {
            // Clear the notification list because no valid recipe exists for these inputs
            metaTileEntity.getNotifiedItemInputList().clear();
            metaTileEntity.getNotifiedFluidInputList().clear();
        }
    }

/**
     * @author ahmadb
     * @reason Completely rewrites parallel recipe generation to bypass order-dependent HashSet bugs 
     * and correctly group ingredients to prevent the RecipeBuilder from overwriting them.
     */
    @Overwrite(remap = false)
    protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
        int maxItemsLimit = this.stack;
        int currentTier = getOverclockingTier(maxVoltage);
        int tierNeeded = Math.max(1, GAUtility.getTierByVoltage(matchingRecipe.getEUt()));
        maxItemsLimit *= (currentTier - tierNeeded);
        maxItemsLimit = Math.max(1, maxItemsLimit);

        int minMultiplier = maxItemsLimit;

        // Safely calculate item multiplier
        if (!matchingRecipe.getInputs().isEmpty()) {
            for (CountableIngredient ci : matchingRecipe.getInputs()) {
                if (ci.getCount() == 0) continue; 
                
                int totalAvailable = 0;
                for (int i = 0; i < inputs.getSlots(); i++) {
                    ItemStack stack = inputs.getStackInSlot(i);
                    if (!stack.isEmpty() && ci.getIngredient().apply(stack)) {
                        totalAvailable += stack.getCount();
                    }
                }
                minMultiplier = Math.min(minMultiplier, totalAvailable / ci.getCount());
            }
        }

        // Safely calculate fluid multiplier
        if (!matchingRecipe.getFluidInputs().isEmpty()) {
            for (FluidStack fs : matchingRecipe.getFluidInputs()) {
                if (fs.amount == 0) continue;
                
                int totalAvailable = 0;
                for (int i = 0; i < fluidInputs.getTanks(); i++) {
                    FluidStack tankFluid = fluidInputs.getTankAt(i).getFluid();
                    if (tankFluid != null && tankFluid.isFluidEqual(fs)) {
                        totalAvailable += tankFluid.amount;
                    }
                }
                minMultiplier = Math.min(minMultiplier, totalAvailable / fs.amount);
            }
        }

        if (minMultiplier <= 0) return null;

        RecipeBuilder<?> newRecipe = recipeMap.recipeBuilder();

        // 1. Group and apply Item Inputs
        List<CountableIngredient> newRecipeInputs = new ArrayList<>();
        List<CountableIngredient> newNotConsumables = new ArrayList<>();
        
        for (CountableIngredient ci : matchingRecipe.getInputs()) {
            if (ci.getCount() == 0 || ci.getIngredient().getClass().getSimpleName().equals("IntCircuitIngredient")) {
                newNotConsumables.add(new CountableIngredient(ci.getIngredient(), 0));
            } else {
                newRecipeInputs.add(new CountableIngredient(ci.getIngredient(), ci.getCount() * minMultiplier));
            }
        }
        
        if (!newRecipeInputs.isEmpty()) newRecipe.inputsIngredients(newRecipeInputs);
        for (CountableIngredient ci : newNotConsumables) newRecipe.notConsumable(ci.getIngredient());

        // 2. Group and apply Fluid Inputs
        List<FluidStack> newFluidInputs = new ArrayList<>();
        for (FluidStack fs : matchingRecipe.getFluidInputs()) {
            if (fs.amount > 0) newFluidInputs.add(new FluidStack(fs.getFluid(), fs.amount * minMultiplier));
        }
        if (!newFluidInputs.isEmpty()) newRecipe.fluidInputs(newFluidInputs);

        // 3. Group and apply Item Outputs
        List<ItemStack> outputI = new ArrayList<>();
        for (ItemStack s : matchingRecipe.getOutputs()) {
            ItemStack itemCopy = s.copy();
            itemCopy.setCount(s.getCount() * minMultiplier);
            outputI.add(itemCopy);
        }
        if (!outputI.isEmpty()) newRecipe.outputs(outputI);

        // 4. Group and apply Fluid Outputs
        List<FluidStack> outputF = new ArrayList<>();
        for (FluidStack f : matchingRecipe.getFluidOutputs()) {
            FluidStack fluidCopy = f.copy();
            fluidCopy.amount = f.amount * minMultiplier;
            outputF.add(fluidCopy);
        }
        if (!outputF.isEmpty()) newRecipe.fluidOutputs(outputF);

        // Reconstruct Chanced Outputs
        copyChancedItemOutputs(newRecipe, matchingRecipe, minMultiplier);

        // Check if outputs can fit
        List<ItemStack> totalOutputs = newRecipe.getChancedOutputs().stream().map(Recipe.ChanceEntry::getItemStack).collect(Collectors.toList());
        totalOutputs.addAll(outputI);
        
        if (!InventoryUtils.simulateItemStackMerge(totalOutputs, this.getOutputInventory())) {
            return matchingRecipe; 
        }

        // Apply EUt and duration scaling
        newRecipe.EUt(Math.max(1, matchingRecipe.getEUt() * this.EUtPercentage / 100))
                 .duration((int) Math.max(3, matchingRecipe.getDuration() * (this.durationPercentage / 100.0)));

        return newRecipe.build().getResult();
    }
   
    /**
     * @author ahmadb
     * @reason Prevent circuits from bottlenecking the parallelization multiplier.
     */
    @Overwrite(remap = false)
    protected int getMinRatioItem(Set<ItemStack> countIngredients, Recipe r, int maxItemsLimit) {
        int minMultiplier = Integer.MAX_VALUE;
        for (CountableIngredient ci : r.getInputs()) {
            // Skip empty counts AND skip circuits so they don't limit the multiplier
            if (ci.getCount() == 0 || ci.getIngredient().getClass().getSimpleName().equals("IntCircuitIngredient")) {
                continue;
            }
            for (ItemStack wholeItemStack : countIngredients) {
                if (ci.getIngredient().apply(wholeItemStack)) {
                    int ratio = Math.min(maxItemsLimit, wholeItemStack.getCount() / ci.getCount());
                    if (ratio < minMultiplier) {
                        minMultiplier = ratio;
                    }
                    break;
                }
            }
        }
        return minMultiplier;
    }

    /**
     * @author ahmadb
     * @reason Prevent circuit ingredients from losing their notConsumable property and being multiplied.
     */
    @Overwrite(remap = false)
    protected void multiplyInputsAndOutputs(List<CountableIngredient> newRecipeInputs, List<FluidStack> newFluidInputs, List<ItemStack> outputI, List<FluidStack> outputF, Recipe r, int multiplier) {
        for (CountableIngredient ci : r.getInputs()) {
            // If it's a circuit, pass it through exactly as it is without multiplying
            if (ci.getIngredient().getClass().getSimpleName().equals("IntCircuitIngredient")) {
                newRecipeInputs.add(ci);
            } else {
                CountableIngredient newIngredient = new CountableIngredient(ci.getIngredient(), ci.getCount() * multiplier);
                newRecipeInputs.add(newIngredient);
            }
        }
        for (FluidStack fs : r.getFluidInputs()) {
            FluidStack newFluid = new FluidStack(fs.getFluid(), fs.amount * multiplier);
            newFluidInputs.add(newFluid);
        }
        for (ItemStack s : r.getOutputs()) {
            int num = s.getCount() * multiplier;
            ItemStack itemCopy = s.copy();
            itemCopy.setCount(num);
            outputI.add(itemCopy);
        }
        for (FluidStack f : r.getFluidOutputs()) {
            int fluidNum = f.amount * multiplier;
            FluidStack fluidCopy = f.copy();
            fluidCopy.amount = fluidNum;
            outputF.add(fluidCopy);
        }
    }
}