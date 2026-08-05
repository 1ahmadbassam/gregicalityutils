package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.simple.LargeSimpleRecipeMapMultiblockController;
import gregtech.api.recipes.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LargeSimpleRecipeMapMultiblockController.class, remap = false)
public class MixinLargeSimpleRecipeMapMultiblockController {

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
}