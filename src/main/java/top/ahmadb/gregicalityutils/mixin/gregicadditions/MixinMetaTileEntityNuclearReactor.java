package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.nuclear.MetaTileEntityNuclearReactor;
import gregtech.api.recipes.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MetaTileEntityNuclearReactor.class, remap = false)
public class MixinMetaTileEntityNuclearReactor {

    /**
     * @author ahmadb
     * @reason Prevent NPE on world load when checkIfJammed passes a null recipe before it is restored.
     */
    @Inject(method = "checkRecipe", at = @At("HEAD"), cancellable = true)
    private void gu$nullCheckRecipeOnWorldLoad(Recipe recipe, boolean consumeIfSuccess, CallbackInfoReturnable<Boolean> cir) {
        // If the recipe hasn't been rebuilt yet from NBT data, abort the check gracefully instead of crashing.
        if (recipe == null) {
            cir.setReturnValue(true);
        }
    }
    
    /**
     * @author ahmadb
     * @reason Flips the 'isDistinct' boolean passed to the super constructor from false to true.
     */
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 0, ordinal = 0))
    private int gu$enableDistinctBuses(int original) {
        // In Java bytecode, 0 represents 'false' and 1 represents 'true'.
        // We return 1 to silently change super(..., false, ...) into super(..., true, ...)
        return 1; 
    }
}