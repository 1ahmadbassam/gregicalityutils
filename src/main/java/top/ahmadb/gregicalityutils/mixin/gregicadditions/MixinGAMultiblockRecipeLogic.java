package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.capabilities.impl.GAMultiblockRecipeLogic;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = GAMultiblockRecipeLogic.class, remap = false)
public abstract class MixinGAMultiblockRecipeLogic extends MultiblockRecipeLogic {

    public MixinGAMultiblockRecipeLogic(RecipeMapMultiblockController tileEntity) {
        super(tileEntity);
    }

    @Shadow(remap = false)
    protected int lastRecipeIndex;

    @Shadow(remap = false)
    protected abstract List<IItemHandlerModifiable> getInputBuses();

    /**
     * @author ahmadb
     * @reason Overwriting the laggy polling array logic to use GTCEu's notification events.
     */
    @Overwrite(remap = false)
    protected boolean checkRecipeInputsDirty(IItemHandler inputs, IMultipleTankHandler fluidInputs, int index) {
        boolean isDirty = this.hasNotifiedInputs();
        metaTileEntity.getNotifiedItemInputList().clear();
        metaTileEntity.getNotifiedFluidInputList().clear();
        return isDirty;
    }

    /**
     * @author ahmadb
     * @reason Porting GTCEu deadlock prevention. If outputs are full, isOutputsFull must be flagged.
     */
    @Overwrite(remap = false)
    protected boolean setupAndConsumeRecipeInputs(Recipe recipe, int index) {
        RecipeMapMultiblockController controller = (RecipeMapMultiblockController) metaTileEntity;
        if (controller.checkRecipe(recipe, false)) {

            int[] resultOverclock = calculateOverclock(recipe.getEUt(), recipe.getDuration());
            int totalEUt = resultOverclock[0] * resultOverclock[1];
            IItemHandlerModifiable importInventory = getInputBuses().get(index);
            IItemHandlerModifiable exportInventory = getOutputInventory();
            IMultipleTankHandler importFluids = getInputTank();
            IMultipleTankHandler exportFluids = getOutputTank();

            if (!(totalEUt >= 0 ? getEnergyStored() >= (totalEUt > getEnergyCapacity() / 2 ? resultOverclock[0] : totalEUt) :
                    (getEnergyStored() - resultOverclock[0] <= getEnergyCapacity()))) {
                return false;
            }

            // Tell the main machine loop to stall if the output bus doesn't have room
            if (!MetaTileEntity.addItemsToItemHandler(exportInventory, true, recipe.getAllItemOutputs(exportInventory.getSlots()))) {
                this.isOutputsFull = true;
                return false;
            }
            if (!MetaTileEntity.addFluidsToFluidHandler(exportFluids, true, recipe.getFluidOutputs())) {
                this.isOutputsFull = true;
                return false;
            }

            this.isOutputsFull = false;

            if (recipe.matches(true, importInventory, importFluids)) {
                controller.checkRecipe(recipe, true);
                return true;
            }
        }
        return false;
    }

    /**
     * @author ahmadb
     * @reason Updating distinct bus logic to rely on the GTCEu notification system.
     */
    @Overwrite(remap = false)
    private void trySearchNewRecipeDistinct() {
        long maxVoltage = getMaxVoltage();
        Recipe currentRecipe = null;
        List<IItemHandlerModifiable> importInventory = getInputBuses();
        IMultipleTankHandler importFluids = getInputTank();

        // Check cache hit
        if (previousRecipe != null && previousRecipe.matches(false, importInventory.get(lastRecipeIndex), importFluids)) {
            currentRecipe = previousRecipe;
            if (setupAndConsumeRecipeInputs(currentRecipe, lastRecipeIndex)) {
                setupRecipe(currentRecipe);
                metaTileEntity.getNotifiedItemInputList().clear();
                metaTileEntity.getNotifiedFluidInputList().clear();
                return;
            }
        }

        // Cache miss, check each distinct bus
        boolean foundRecipe = false;
        for (int i = 0; i < importInventory.size(); i++) {
            IItemHandlerModifiable bus = importInventory.get(i);
            
            currentRecipe = findRecipe(maxVoltage, bus, importFluids);
            if (currentRecipe != null) {
                this.previousRecipe = currentRecipe;
                foundRecipe = true;
            }
            
            if (currentRecipe != null && setupAndConsumeRecipeInputs(currentRecipe, i)) {
                lastRecipeIndex = i;
                setupRecipe(currentRecipe);
                break;
            }
        }

        // Flag deadlocks if no recipe matched
        this.invalidInputsForRecipes = !foundRecipe;
        metaTileEntity.getNotifiedItemInputList().clear();
        metaTileEntity.getNotifiedFluidInputList().clear();
    }
}