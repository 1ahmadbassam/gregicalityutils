package top.ahmadb.gregicalityutils.mixin.gtce2oc;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.util.math.BlockPos;
import org.eientei.gtce2oc.driver.values.MachineObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MachineObject.class, remap = false)
public abstract class MixinMachineObject {

    // Shadow the absolute position stored in the MachineObject
    @Shadow private BlockPos pos;

    @Callback(doc = "function():number, number, number -- Returns the absolute X, Y, Z coordinates of this machine.")
    public Object[] getPosition(final Context context, final Arguments args) {
        if (this.pos == null) {
            return new Object[] {null, "invalid position"};
        }
        return new Object[] {this.pos.getX(), this.pos.getY(), this.pos.getZ()};
    }
}