package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.TileEntityAssemblyLine;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.AbstractRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.CountableIngredient;
import gregtech.api.recipes.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractRecipeLogic.class, remap = false)
public abstract class MixinAbstractRecipeLogic {

    // We removed the @Shadow for metaTileEntity/getMetaTileEntity entirely
    // because Mixin cannot shadow inherited members from MTETrait.

    @Shadow protected abstract IItemHandlerModifiable getInputInventory();
    @Shadow protected abstract IMultipleTankHandler getInputTank();
    @Shadow protected abstract IItemHandlerModifiable getOutputInventory();
    @Shadow protected abstract IMultipleTankHandler getOutputTank();
    @Shadow protected abstract int[] calculateOverclock(int EUt, int duration);
    @Shadow protected abstract long getEnergyStored();
    @Shadow protected abstract long getEnergyCapacity();
    @Shadow protected boolean isOutputsFull;

    /**
     * @author ahmadb
     * @reason Intercept Assembly Line recipe searches to enforce length requirements and allow shapeless matching.
     */
    @Inject(method = "findRecipe", at = @At("HEAD"), cancellable = true)
    private void gu$shapelessFindRecipe(long maxVoltage, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, CallbackInfoReturnable<Recipe> cir) {
        // Cast 'this' to AbstractRecipeLogic to access inherited methods legally
        MetaTileEntity mte = ((AbstractRecipeLogic) (Object) this).getMetaTileEntity();
        
        if (mte instanceof TileEntityAssemblyLine) {
            TileEntityAssemblyLine assemblyLine = (TileEntityAssemblyLine) mte;
            
            for (Recipe recipe : assemblyLine.recipeMap.getRecipeList()) {
                if (recipe.getEUt() > maxVoltage) continue;

                // Size check: number of input slots (slices) must be >= recipe's distinct item count
                if (inputs.getSlots() < recipe.getInputs().size()) continue;

                // Perform a Shapeless Match for Items and Fluids
                if (gu$matchesShapeless(recipe, inputs, fluidInputs, false)) {
                    cir.setReturnValue(recipe);
                    return;
                }
            }
            cir.setReturnValue(null);
        }
    }

    /**
     * @author ahmadb
     * @reason Intercept Assembly Line consumption to extract items dynamically across the entire inventory.
     */
    @Inject(method = "setupAndConsumeRecipeInputs", at = @At("HEAD"), cancellable = true)
    private void gu$shapelessConsumeRecipe(Recipe recipe, CallbackInfoReturnable<Boolean> cir) {
        // Cast 'this' to AbstractRecipeLogic to access inherited methods legally
        MetaTileEntity mte = ((AbstractRecipeLogic) (Object) this).getMetaTileEntity();
        
        if (mte instanceof TileEntityAssemblyLine) {
            int[] resultOverclock = calculateOverclock(recipe.getEUt(), recipe.getDuration());
            long totalEUt = (long) resultOverclock[0] * resultOverclock[1];

            if (!(totalEUt >= 0 ? getEnergyStored() >= (totalEUt > getEnergyCapacity() / 2 ? resultOverclock[0] : totalEUt) :
                    (getEnergyStored() - resultOverclock[0] <= getEnergyCapacity()))) {
                cir.setReturnValue(false);
                return;
            }

            IItemHandlerModifiable exportInventory = getOutputInventory();
            IMultipleTankHandler exportFluids = getOutputTank();

            if (!MetaTileEntity.addItemsToItemHandler(exportInventory, true, recipe.getAllItemOutputs(exportInventory.getSlots()))) {
                this.isOutputsFull = true;
                cir.setReturnValue(false);
                return;
            }
            if (!MetaTileEntity.addFluidsToFluidHandler(exportFluids, true, recipe.getFluidOutputs())) {
                this.isOutputsFull = true;
                cir.setReturnValue(false);
                return;
            }

            this.isOutputsFull = false;

            // Consume ingredients fully shapelessly
            if (gu$matchesShapeless(recipe, getInputInventory(), getInputTank(), true)) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }

    @Unique
    private boolean gu$matchesShapeless(Recipe recipe, IItemHandlerModifiable inputs, IMultipleTankHandler fluidInputs, boolean consume) {
        // Check Items
        int[] simulatedInventory = new int[inputs.getSlots()];
        for (int i = 0; i < inputs.getSlots(); i++) {
            simulatedInventory[i] = inputs.getStackInSlot(i).getCount();
        }

        int[] toConsume = new int[inputs.getSlots()];

        for (CountableIngredient ci : recipe.getInputs()) {
            int required = ci.getCount();
            boolean isNonConsumable = (required == 0);
            int checkAmount = isNonConsumable ? 1 : required;
            boolean found = false;

            for (int i = 0; i < inputs.getSlots(); i++) {
                if (checkAmount == 0) break;
                ItemStack stackInSlot = inputs.getStackInSlot(i);
                if (!stackInSlot.isEmpty() && simulatedInventory[i] > 0 && ci.getIngredient().apply(stackInSlot)) {
                    int taken = Math.min(checkAmount, simulatedInventory[i]);
                    checkAmount -= taken;

                    if (!isNonConsumable) {
                        simulatedInventory[i] -= taken;
                        toConsume[i] += taken;
                    }
                    if (checkAmount == 0) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found && checkAmount > 0) {
                return false;
            }
        }

        // Check Fluids
        FluidStack[] simulatedFluids = new FluidStack[fluidInputs.getTanks()];
        for (int i = 0; i < fluidInputs.getTanks(); i++) {
            FluidStack fs = fluidInputs.getTankAt(i).getFluid();
            simulatedFluids[i] = fs == null ? null : fs.copy();
        }
        int[] fluidToConsume = new int[fluidInputs.getTanks()];

        for (FluidStack recipeFluid : recipe.getFluidInputs()) {
            int required = recipeFluid.amount;
            boolean isNonConsumable = (required == 0);
            int checkAmount = isNonConsumable ? 1 : required;
            boolean found = false;

            for (int i = 0; i < fluidInputs.getTanks(); i++) {
                if (checkAmount == 0) break;
                FluidStack tankFluid = simulatedFluids[i];
                if (tankFluid != null && tankFluid.isFluidEqual(recipeFluid)) {
                    int taken = Math.min(checkAmount, tankFluid.amount);
                    checkAmount -= taken;
                    
                    if (!isNonConsumable) {
                        tankFluid.amount -= taken;
                        fluidToConsume[i] += taken;
                    }
                    if (checkAmount == 0) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found && checkAmount > 0) return false;
        }

        if (!consume) return true;

        // Execution: Actually consume the items
        for (int i = 0; i < inputs.getSlots(); i++) {
            if (toConsume[i] > 0) {
                inputs.extractItem(i, toConsume[i], false);
            }
        }

        // Execution: Actually consume the fluids
        for (int i = 0; i < fluidInputs.getTanks(); i++) {
            if (fluidToConsume[i] > 0) {
                FluidStack fluidToDrain = fluidInputs.getTankAt(i).getFluid().copy();
                fluidToDrain.amount = fluidToConsume[i];
                fluidInputs.drain(fluidToDrain, true);
            }
        }

        return true;
    }
}