package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.simple.TileEntityLargeThermalCentrifuge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TileEntityLargeThermalCentrifuge.class, remap = false)
public interface TileEntityLargeThermalCentrifugeAccessor {

    /**
     * @author ahmadb
     * @reason Bypasses protected/private access to read the speedBonus field.
     */
    @Accessor(value = "speedBonus", remap = false)
    int gu$getSpeedBonus();

}