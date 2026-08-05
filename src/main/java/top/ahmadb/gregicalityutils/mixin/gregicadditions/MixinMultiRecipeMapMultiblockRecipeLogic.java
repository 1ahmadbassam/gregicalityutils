package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.GAUtility;
import gregicadditions.machines.multi.simple.MultiRecipeMapMultiblockController;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.InventoryUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = MultiRecipeMapMultiblockController.MultiRecipeMapMultiblockRecipeLogic.class, remap = false)
public abstract class MixinMultiRecipeMapMultiblockRecipeLogic extends gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController.LargeSimpleMultiblockRecipeLogic {

    @Shadow(remap = false)
    @Final
    private RecipeMap<?>[] recipeMaps;

    public MixinMultiRecipeMapMultiblockRecipeLogic(RecipeMapMultiblockController tileEntity, int EUtPercentage, int durationPercentage, int chancePercentage, int stack) {
        super(tileEntity, EUtPercentage, durationPercentage, chancePercentage, stack);
    }

/**
     * @author ahmadb
     * @reason Completely rewrites parallel recipe generation to bypass order-dependent HashSet bugs, 
     * correctly group ingredients to prevent the RecipeBuilder from overwriting them, and preserve circuits.
     */
    @Overwrite(remap = false)
    protected Recipe createRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, Recipe matchingRecipe) {
        int maxItemsLimit = this.getStack();
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

        // Get the currently selected RecipeMap dynamically
        MultiRecipeMapMultiblockController metaTileEntity = (MultiRecipeMapMultiblockController) getMetaTileEntity();
        int recipeMapIndex = metaTileEntity.getRecipeMapIndex();
        RecipeBuilder<?> newRecipe = this.recipeMaps[recipeMapIndex].recipeBuilder();

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

        // Apply EUt and duration scaling using parent getters
        newRecipe.EUt(Math.max(1, matchingRecipe.getEUt() * this.getEUtPercentage() / 100))
                 .duration((int) Math.max(3, matchingRecipe.getDuration() * (this.getDurationPercentage() / 100.0)));

        return newRecipe.build().getResult();
    }
}