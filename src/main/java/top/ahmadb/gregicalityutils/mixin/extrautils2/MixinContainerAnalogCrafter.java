package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.gui.backend.DynamicContainerTile;
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

    // We target getRSWidget because its signature contains ZERO vanilla classes, preventing obfuscation mismatches.
    @Inject(method = "<init>", 
            at = @At(
                    value = "INVOKE", 
                    target = "Lcom/rwtema/extrautils2/tile/TileAdvInteractor;getRSWidget(IILcom/rwtema/extrautils2/utils/datastructures/NBTSerializable$NBTEnum;Lcom/rwtema/extrautils2/utils/datastructures/NBTSerializable$Int;)Lcom/rwtema/extrautils2/gui/backend/WidgetClickMCButtonChoices;", 
                    shift = At.Shift.AFTER
            ))
    private void gcu$addCustomWidgets(TileAnalogCrafter tile, EntityPlayer player, CallbackInfo ci) {
        if (tile instanceof IAnalogCrafterExtensions) {
            IAnalogCrafterExtensions ext = (IAnalogCrafterExtensions) tile;

            // The RS widget is generated at y = this.height + 4
            // We use this exact same Y coordinate to align our widgets nicely next to it.
            int currentY = this.height + 4;

            // X = 4 (left margin) + 18 (RS widget width) + 4 (padding)
            this.addWidget(new WidgetClickMCButtonBoolean.NBTBoolean(
                    26, currentY,
                    ext.gcu_getLimitToOne(),
                    Lang.translate("Limit to 1"),
                    Lang.translate("Limits input slots strictly to 1 item to prevent recipe pipeline clogging")
            ));

            // Setup Speed Upgrade slot at the far right of the GUI
            int rightX = this.playerInvWidth - 4 - 18;
            this.addWidget(ext.gcu_getUpgrades().getSpeedUpgradeSlot(rightX, currentY));
        }
    }
}