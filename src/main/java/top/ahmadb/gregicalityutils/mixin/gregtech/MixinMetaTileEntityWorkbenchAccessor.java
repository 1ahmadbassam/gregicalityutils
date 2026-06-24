package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.common.metatileentities.storage.CraftingRecipeResolver;
import gregtech.common.metatileentities.storage.MetaTileEntityWorkbench;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MetaTileEntityWorkbench.class, remap = false)
public interface MixinMetaTileEntityWorkbenchAccessor {
    @Invoker("getRecipeResolver")
    CraftingRecipeResolver invokeGetRecipeResolver();

    @Accessor("internalInventory")
    ItemStackHandler getInternalInventory();

    @Accessor("toolInventory")
    ItemStackHandler getToolInventory();
}