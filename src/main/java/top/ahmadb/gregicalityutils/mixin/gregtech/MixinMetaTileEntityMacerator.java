package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.GTValues;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.RecipeLogicEnergy;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.GTUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

// Target the anonymous class directly
@Mixin(targets = "gregtech.common.metatileentities.electric.MetaTileEntityMacerator$1")
public abstract class MixinMetaTileEntityMacerator extends RecipeLogicEnergy {

    // Dummy constructor required by the Java compiler.
    // We explicitly cast to Supplier<IEnergyContainer> to satisfy javac's strict generic typing.
    public MixinMetaTileEntityMacerator() {
        super((MetaTileEntity) null, (RecipeMap<?>) null, (Supplier<IEnergyContainer>) null);
    }

    @Inject(method = "getMachineTierForRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void gu$fixMaceratorTier(Recipe recipe, CallbackInfoReturnable<Integer> cir) {
        int baseTier = recipe.getBaseTier();
        
        // Inherited legally through 'extends RecipeLogicEnergy'
        int tier = GTUtility.getTierByVoltage(this.getMaxVoltage());

        // If the recipe base tier is above MV, use default standard logic
        if (baseTier > GTValues.MV) {
            cir.setReturnValue(super.getMachineTierForRecipe(recipe));
            return;
        }

        // Otherwise, apply the restricted bonus logic for lower-tier recipes
        if (tier > GTValues.MV) {
            cir.setReturnValue(tier - (GTValues.MV - baseTier));
        } else {
            cir.setReturnValue(baseTier);
        }
    }
}