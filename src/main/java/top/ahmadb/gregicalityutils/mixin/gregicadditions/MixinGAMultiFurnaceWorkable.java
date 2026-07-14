package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.capabilities.impl.GAMultiblockRecipeLogic;
import gregicadditions.machines.multi.override.MetaTileEntityMultiFurnace;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import gregtech.api.util.InventoryUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Mixin(targets = "gregicadditions.machines.multi.override.MetaTileEntityMultiFurnace$GAMultiFurnaceWorkable", remap = false)
public abstract class MixinGAMultiFurnaceWorkable extends GAMultiblockRecipeLogic {

    public MixinGAMultiFurnaceWorkable(RecipeMapMultiblockController tileEntity) {
        super(tileEntity);
    }

    @Shadow(remap = false)
    private void computeOutputItemStacks(Collection<ItemStack> recipeOutputs, ItemStack outputStack, int overclockAmount) { }

    @Shadow(aliases = "this$0", remap = false)
    private MetaTileEntityMultiFurnace furnace;
    
    /**
     * @author ahmadb
     * @reason GTCE Nomifactory Edition
     */
    @Overwrite(remap = false)
    protected void trySearchNewRecipe() {
        long maxVoltage = getMaxVoltage();
        Recipe currentRecipe = null;
        IItemHandlerModifiable importInventory = getInputInventory();
        IMultipleTankHandler importFluids = getInputTank();

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

        if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe)) {
            setupRecipe(currentRecipe);
        }
        
        metaTileEntity.getNotifiedItemInputList().clear();
    }

    /**
     * @author ahmadb
     * @reason GTCE Nomifactory Edition
     */
    @Overwrite(remap = false)
    protected Recipe findRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs) {
        int currentItemsEngaged = 0;
        
        // FIX: Cast the outer class to our new Accessor Interface to bypass protected visibility
        IMetaTileEntityMultiFurnaceAccessor furnaceAccessor = (IMetaTileEntityMultiFurnaceAccessor) this.furnace;
        
        final int maxItemsLimit = 32 * furnaceAccessor.getHeatingCoilLevel();
        final ArrayList<CountableIngredient> recipeInputs = new ArrayList<>();
        final ArrayList<ItemStack> recipeOutputs = new ArrayList<>();

        boolean matchedRecipe = false;
        boolean canFitOutputs = true;

        for (int index = 0; index < inputs.getSlots() && currentItemsEngaged < maxItemsLimit; index++) {
            final ItemStack currentInputItem = inputs.getStackInSlot(index);
            if (currentInputItem.isEmpty()) continue;

            Recipe matchingRecipe = recipeMap.findRecipe(maxVoltage,
                    Collections.singletonList(currentInputItem),
                    Collections.emptyList(), 0);
                    
            CountableIngredient inputIngredient;
            if (matchingRecipe != null) {
                inputIngredient = matchingRecipe.getInputs().get(0);
                matchedRecipe = true; 
            } else {
                continue;
            }

            if (inputIngredient == null) {
                throw new IllegalStateException(String.format("Got recipe with null ingredient %s", matchingRecipe));
            }

            int itemsLeftUntilMax = (maxItemsLimit - currentItemsEngaged);
            if (itemsLeftUntilMax >= inputIngredient.getCount()) {
                int craftsPossible = currentInputItem.getCount() / inputIngredient.getCount();
                int craftsUntilMax = itemsLeftUntilMax / inputIngredient.getCount();
                int recipeMultiplier = Math.min(craftsPossible, craftsUntilMax);

                ArrayList<ItemStack> temp = new ArrayList<>(recipeOutputs);
                computeOutputItemStacks(temp, matchingRecipe.getOutputs().get(0), recipeMultiplier);

                canFitOutputs = InventoryUtils.simulateItemStackMerge(temp, this.getOutputInventory());

                if (!canFitOutputs) break;

                temp.removeAll(recipeOutputs);
                recipeOutputs.addAll(temp);

                recipeInputs.add(new CountableIngredient(inputIngredient.getIngredient(),
                        inputIngredient.getCount() * recipeMultiplier));

                currentItemsEngaged += inputIngredient.getCount() * recipeMultiplier;
            }
        }

        this.invalidInputsForRecipes = !matchedRecipe;
        this.isOutputsFull = !canFitOutputs;

        if (recipeInputs.isEmpty()) {
            return null;
        }

        return recipeMap.recipeBuilder()
                .inputsIngredients(recipeInputs)
                .outputs(recipeOutputs)
                // FIX: Use the accessor here as well
                .EUt(Math.max(1, 16 / furnaceAccessor.getHeatingCoilDiscount()))
                .duration((int) Math.max(1.0, 256 * (currentItemsEngaged / (maxItemsLimit * 1.0))))
                .build().getResult();
    }
}