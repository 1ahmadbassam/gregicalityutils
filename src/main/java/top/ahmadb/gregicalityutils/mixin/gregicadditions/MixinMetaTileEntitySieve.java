package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.integrations.exnihilocreatio.machines.MetaTileEntitySieve;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.CycleButtonWidget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.ToggleButtonWidget;
import gregtech.api.metatileentity.SimpleMachineMetaTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MetaTileEntitySieve.class, remap = false)
public abstract class MixinMetaTileEntitySieve extends SimpleMachineMetaTileEntity {

    public MixinMetaTileEntitySieve() {
        super(null, null, null, 0);
    }

    // 1. Your Inventory Expansion
    @Inject(method = "createExportItemHandler", at = @At("HEAD"), cancellable = true)
    private void gu$increaseElectricSieveCapacity(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        cir.setReturnValue(new NotifiableItemStackHandler(54, this, true));
    }

    // 2. Overwriting the UI to add the Overclock button
    @Inject(method = "createUI", at = @At("HEAD"), cancellable = true)
    private void gu$createUIWithOverclock(EntityPlayer player, CallbackInfoReturnable<ModularUI> cir) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176, 216)
                .widget(new LabelWidget(6, 6, getMetaFullName()))
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 8, 134)
                .widget(new SlotWidget(this.importItems, 0, 35, 25).setBackgroundTexture(GuiTextures.SLOT))
                .widget(new SlotWidget(this.importItems, 1, 53, 25).setBackgroundTexture(GuiTextures.SLOT))
                .widget(new ProgressWidget(this.workable::getProgressPercent, 78, 24, 20, 18, GuiTextures.PROGRESS_BAR_SIFT, ProgressWidget.MoveType.VERTICAL_INVERTED));

        int leftButtonStartX = 7;
        if (this.exportItems.getSlots() > 0) {
            builder.widget(new ToggleButtonWidget(leftButtonStartX, 62, 18, 18,
                    GuiTextures.BUTTON_ITEM_OUTPUT, this::isAutoOutputItems, this::setAutoOutputItems)
                    .setTooltipText("gregtech.gui.item_auto_output.tooltip"));
        }

        // --- THE MISSING OVERCLOCK BUTTON ---
        builder.widget(new CycleButtonWidget(leftButtonStartX + 18, 62, 18, 18,
                this.workable.getAvailableOverclockingTiers(), this.workable::getOverclockTier, this.workable::setOverclockTier)
                .setTooltipHoverString("gregtech.gui.overclock.description")
                .setButtonTexture(GuiTextures.BUTTON_OVERCLOCK));

        // Draw the 6x4 output grid
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 4; x++) {
                builder.widget(new SlotWidget(this.exportItems, y * 4 + x, 98 + x * 18, 14 + y * 18, true, false).setBackgroundTexture(GuiTextures.SLOT));
            }
        }

        cir.setReturnValue(builder.build(getHolder(), player));
    }
}