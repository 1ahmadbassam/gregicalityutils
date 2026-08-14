package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.capabilities.impl.QubitConsumeRecipeLogic;
import gregtech.api.recipes.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = QubitConsumeRecipeLogic.class, remap = false)
public abstract class MixinQubitConsumeRecipeLogic extends gregicadditions.capabilities.impl.GAMultiblockRecipeLogic {

    @Shadow(remap = false)
    private int recipeQubit;

    public MixinQubitConsumeRecipeLogic(gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController metaTileEntity) {
        super(metaTileEntity);
    }

    /**
     * @author ahmadb
     * @reason Safely retrieve the qubitConsume property to prevent server crashes 
     * when running standard recipes that don't require Qubits.
     */
    @Overwrite(remap = false)
    protected void setupRecipe(Recipe recipe) {
        super.setupRecipe(recipe);
        
        try {
            this.recipeQubit = recipe.getIntegerProperty("qubitConsume");
        } catch (IllegalArgumentException | NullPointerException e) {
            // If the recipe doesn't have the property, default to 0 instead of crashing!
            this.recipeQubit = 0;
        }
    }
}