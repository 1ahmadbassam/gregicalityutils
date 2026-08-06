package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.gui.backend.DynamicContainerTile;
import com.rwtema.extrautils2.gui.backend.IWidget;
import com.rwtema.extrautils2.gui.backend.WidgetClickMCButtonBoolean;
import com.rwtema.extrautils2.tile.TileAnalogCrafter;
import com.rwtema.extrautils2.utils.Lang;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ahmadb.gregicalityutils.extrautils2.IAnalogCrafterExtensions;

@Mixin(value = TileAnalogCrafter.ContainerAnalogCrafter.class, remap = false)
public abstract class MixinContainerAnalogCrafter extends DynamicContainerTile {

    public MixinContainerAnalogCrafter() {
        super(null); 
    } 

    @Inject(method = "<init>", 
            at = @At(
                    value = "INVOKE", 
                    target = "Lcom/rwtema/extrautils2/tile/TileAdvInteractor;getRSWidget(IILcom/rwtema/extrautils2/utils/datastructures/NBTSerializable$NBTEnum;Lcom/rwtema/extrautils2/utils/datastructures/NBTSerializable$Int;)Lcom/rwtema/extrautils2/gui/backend/WidgetClickMCButtonChoices;", 
                    shift = At.Shift.AFTER
            ))
    private void gcu$addCustomWidgets(TileAnalogCrafter tile, EntityPlayer player, CallbackInfo ci) {
        if (tile instanceof IAnalogCrafterExtensions) {
            IAnalogCrafterExtensions ext = (IAnalogCrafterExtensions) tile;

            int currentY = this.height + 4;
            IWidget w;

            this.addWidget(w = new WidgetClickMCButtonBoolean.NBTBoolean(
                    26, currentY,
                    ext.gcu_getLimitToOne(),
                    Lang.translate("Limit 1"),
                    Lang.translate("Limits input slots strictly to 1 item to prevent recipe pipeline clogging")
            ));

            this.addWidget(new WidgetClickMCButtonBoolean.NBTBoolean(
                    w.getX() + w.getW() + 4, currentY,
                    ext.gcu_getStrictSlots(),
                    Lang.translate("Lock"),
                    Lang.translate("When toggled ON, it records which slots are currently filled. Crafting halts if any of these locked slots become empty.")
            ));

            // Setup Speed Upgrade slot at the far right of the GUI
            int rightX = this.playerInvWidth - 4 - 18;
            this.addWidget(ext.gcu_getUpgrades().getSpeedUpgradeSlot(rightX, currentY));
        }
    }
}