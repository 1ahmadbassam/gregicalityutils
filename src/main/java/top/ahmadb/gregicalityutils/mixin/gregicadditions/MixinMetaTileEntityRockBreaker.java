package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.MetaTileEntityRockBreaker;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = MetaTileEntityRockBreaker.class, remap = false)
public abstract class MixinMetaTileEntityRockBreaker {

    /**
     * @author ahmadb
     * @reason Allow liquid checks from UP and DOWN faces.
     */
    @Overwrite
    private boolean checkSides(BlockStaticLiquid liquid) {
        // Cast 'this' to MetaTileEntity to access the inherited public methods directly
        MetaTileEntity self = (MetaTileEntity) (Object) this;
        
        EnumFacing frontFacing = self.getFrontFacing();
        for (EnumFacing side : EnumFacing.VALUES) {
            // ONLY ignore the front facing (output face) now
            if (side == frontFacing) continue;
            
            if (self.getWorld().getBlockState(self.getPos().offset(side)) == liquid.getDefaultState()) {
                return true;
            }
        }
        return false;
    }
}