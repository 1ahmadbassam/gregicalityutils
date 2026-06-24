package top.ahmadb.gregicalityutils.mixin.gtce2oc;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import org.eientei.gtce2oc.tile.TileEntityGTCEBridge;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = TileEntityGTCEBridge.class, remap = false)
public abstract class MixinTileEntityGTCEBridge {

    @Callback(doc = "function():number, number, number -- Returns the absolute X, Y, Z coordinates of this bridge block.")
    public Object[] getBridgePosition(final Context context, final Arguments args) {
        // Cast 'this' to TileEntity to grab the standard Minecraft getPos() method
        BlockPos pos = ((TileEntity) (Object) this).getPos();
        return new Object[] {pos.getX(), pos.getY(), pos.getZ()};
    }
}