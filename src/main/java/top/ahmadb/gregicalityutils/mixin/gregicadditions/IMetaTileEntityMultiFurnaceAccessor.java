package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.override.MetaTileEntityMultiFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MetaTileEntityMultiFurnace.class, remap = false)
public interface IMetaTileEntityMultiFurnaceAccessor {

    @Accessor(value = "heatingCoilLevel", remap = false)
    int getHeatingCoilLevel();

    @Accessor(value = "heatingCoilDiscount", remap = false)
    int getHeatingCoilDiscount();

}