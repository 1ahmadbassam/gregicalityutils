package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.common.metatileentities.storage.CraftingRecipeResolver;
import gregtech.common.metatileentities.storage.MetaTileEntityWorkbench;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MetaTileEntityWorkbench.class, remap = false)
public interface MixinMetaTileEntityWorkbenchAccessor {
    // Allows us to call the private getRecipeResolver() method
    @Invoker("getRecipeResolver")
    CraftingRecipeResolver invokeGetRecipeResolver();
}