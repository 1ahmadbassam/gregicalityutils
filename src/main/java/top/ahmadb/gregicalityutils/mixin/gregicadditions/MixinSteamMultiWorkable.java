package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.capabilities.impl.SteamMultiWorkable;
import gregicadditions.capabilities.impl.SteamMultiblockRecipeLogic;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.util.InventoryUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Mixin(value = SteamMultiWorkable.class, remap = false)
public abstract class MixinSteamMultiWorkable extends SteamMultiblockRecipeLogic {

    // Dummy constructor to satisfy the Java compiler (Mixins strip this at runtime)
    public MixinSteamMultiWorkable() {
        super(null, null, null, 0);
    }

    @Shadow(remap = false)
    @Final
    private int MAX_PROCESSES;

    @Shadow(remap = false)
    private void computeOutputItemStacks(Collection<ItemStack> recipeOutputs, ItemStack outputStack, int recipeAmount) { }

    @Shadow(remap = false)
    private Recipe doChancedOnlyRecipe(Recipe matchingRecipe, ItemStack stack) { return null; }

    /**
     * @author ahmadb
     * @reason Integrating GTCEu notification system and removing forceRecipeRecheck for Nomifactory compatibility.
     */
    @Overwrite(remap = false)
    protected void trySearchNewRecipe() {
        long maxVoltage = getMaxVoltage(); // Will always be LV voltage
        Recipe currentRecipe = null;
        IItemHandlerModifiable importInventory = getInputInventory();

        // Use the new event-driven notification checks instead of the laggy dirty checks
        if (hasNotifiedInputs() ||
            previousRecipe == null ||
            !previousRecipe.matches(false, importInventory, new FluidTankList(false))) {

            currentRecipe = findRecipe(maxVoltage, importInventory, null);
        } else {
            currentRecipe = previousRecipe;
        }

        if (currentRecipe != null) {
            this.previousRecipe = currentRecipe;
        }

        if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe)) {
            setupRecipe(currentRecipe);
        }
        
        // Clear the notification list so it doesn't poll every tick
        metaTileEntity.getNotifiedItemInputList().clear();
    }

    /**
     * @author ahmadb
     * @reason Porting deadlock prevention from GTCEu state trackers.
     */
    @Overwrite(remap = false)
    protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs) {
        int currentItemsEngaged = 0;
        final ArrayList<CountableIngredient> recipeInputs = new ArrayList<>();
        final ArrayList<ItemStack> recipeOutputs = new ArrayList<>();
        int recipeEUt = 0;
        int recipeDuration = 1;
        float speedBonusPercent = 0.0F; // Currently unused

        // Track inventory states
        boolean matchedRecipe = false;
        boolean canFitOutputs = true;

        for (int index = 0; index < inputs.getSlots() && currentItemsEngaged < MAX_PROCESSES; index ++) {
            final ItemStack currentInputItem = inputs.getStackInSlot(index);

            if (currentInputItem.isEmpty()) continue;

            Recipe matchingRecipe = recipeMap.findRecipe(maxVoltage,
                    Collections.singletonList(currentInputItem),
                    Collections.emptyList(), 0);
                    
            CountableIngredient inputIngredient;
            if (matchingRecipe != null) {
                matchedRecipe = true; // We found at least one valid recipe
                if (matchingRecipe.getOutputs().isEmpty()) {
                    // If we return early, we must explicitly reset the state trackers so the machine doesn't deadlock
                    this.invalidInputsForRecipes = false;
                    this.isOutputsFull = false;
                    return doChancedOnlyRecipe(matchingRecipe, currentInputItem);
                }
                inputIngredient = matchingRecipe.getInputs().get(0);
                recipeEUt = matchingRecipe.getEUt();
                recipeDuration = matchingRecipe.getDuration();
            } else {
                continue;
            }

            if (inputIngredient == null) {
                throw new IllegalStateException(String.format("Recipe with null ingredient %s", matchingRecipe));
            }

            int itemsLeftUntilMax = (MAX_PROCESSES - currentItemsEngaged);
            if (itemsLeftUntilMax >= inputIngredient.getCount()) {

                int recipeMultiplier = Math.min((currentInputItem.getCount() / inputIngredient.getCount()),
                        (itemsLeftUntilMax / inputIngredient.getCount()));

                ArrayList<ItemStack> temp = new ArrayList<>(recipeOutputs);
                computeOutputItemStacks(temp, matchingRecipe.getOutputs().get(0), recipeMultiplier);

                canFitOutputs = InventoryUtils.simulateItemStackMerge(temp, this.getOutputInventory());
                if (!canFitOutputs) break; // Output bus is full

                temp.removeAll(recipeOutputs);
                recipeOutputs.addAll(temp);

                recipeInputs.add(new CountableIngredient(inputIngredient.getIngredient(),
                        inputIngredient.getCount() * recipeMultiplier));

                currentItemsEngaged += inputIngredient.getCount() * recipeMultiplier;
            }
        }

        // Communicate inventory state back to base GTCE to prevent deadlocks
        this.invalidInputsForRecipes = !matchedRecipe;
        this.isOutputsFull = !canFitOutputs;

        if (recipeInputs.isEmpty()) {
            return null;
        }

        return recipeMap.recipeBuilder()
                .inputsIngredients(recipeInputs)
                .outputs(recipeOutputs)
                .EUt(Math.min(32, (int)Math.ceil(recipeEUt * 1.33)))
                .duration(Math.max(recipeDuration, (int)(recipeDuration * (100.0F / (100.0F + speedBonusPercent)) * 1.5)))
                .build().getResult();
    }
}