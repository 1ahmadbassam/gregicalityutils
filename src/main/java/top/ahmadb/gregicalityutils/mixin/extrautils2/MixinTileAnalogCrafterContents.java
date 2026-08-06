package top.ahmadb.gregicalityutils.mixin.extrautils2;

import top.ahmadb.gregicalityutils.extrautils2.IAnalogCrafterExtensions;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "com.rwtema.extrautils2.tile.TileAnalogCrafter$2",
        "com.rwtema.extrautils2.tile.TileAnalogCrafter$3"
}, remap = false)
public abstract class MixinTileAnalogCrafterContents {

    @Unique private java.lang.reflect.Field gcu$this0Field = null;
    @Unique private boolean gcu$fieldSearched = false;

    @Inject(method = "getStackLimit(ILnet/minecraft/item/ItemStack;)I", at = @At("RETURN"), cancellable = true, require = 0)
    public void gcu$limitStackSize(int slot, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        
        if ((Object) this instanceof ItemStackHandler) {
            
            if (!gcu$fieldSearched) {
                try {
                    gcu$this0Field = this.getClass().getDeclaredField("this$0");
                    gcu$this0Field.setAccessible(true);
                } catch (NoSuchFieldException e) { }
                gcu$fieldSearched = true;
            }

            if (gcu$this0Field != null) {
                try {
                    Object outer = gcu$this0Field.get(this);
                    if (outer instanceof IAnalogCrafterExtensions) {
                        if (((IAnalogCrafterExtensions) outer).gcu_getLimitToOne().value) {
                            int originalLimit = cir.getReturnValueI();
                            // Overwrite output size up to 1
                            if (originalLimit > 0) cir.setReturnValue(1);
                        }
                    }
                } catch (IllegalAccessException e) { }
            }
        }
    }
}