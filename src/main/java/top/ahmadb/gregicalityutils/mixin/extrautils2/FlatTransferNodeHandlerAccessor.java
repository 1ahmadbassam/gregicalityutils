package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.interblock.FlatTransferNodeHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FlatTransferNodeHandler.class, remap = false)
public interface FlatTransferNodeHandlerAccessor {

    @Accessor("itemHandlerProcesser")
    static FlatTransferNodeHandler.Processer<IItemHandler> getItemProcessor() {
        throw new AssertionError("Mixin failed to apply accessor!");
    }

    @Accessor("fluidHandlerProcesser")
    static FlatTransferNodeHandler.Processer<IFluidHandler> getFluidProcessor() {
        throw new AssertionError("Mixin failed to apply accessor!");
    }
}