package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.GAEventHandler;
import gregicadditions.armor.ArmorLogicSuite;
import gregicadditions.armor.PowerlessJetpack;
import gregtech.api.items.armor.ArmorMetaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ahmadb.gregicalityutils.capability.BaublesDelegate;

@Mixin(value = GAEventHandler.class, remap = false)
public class MixinGAEventHandler {

    @Inject(method = "onRender", at = @At("HEAD"))
    private void gu$renderBaubleHUD(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        
        if (mc.inGameHasFocus && mc.world != null && !mc.gameSettings.showDebugInfo && Minecraft.isGuiEnabled()) {
            if (Loader.isModLoaded("baubles")) {
                // Check the bauble slot safely using our reflection delegate
                ItemStack baubleStack = BaublesDelegate.getBauble(mc.player, EntityEquipmentSlot.CHEST);
                
                if (!baubleStack.isEmpty() && baubleStack.getItem() instanceof ArmorMetaItem) {
                    ArmorMetaItem<?>.ArmorMetaValueItem armorMetaValue = ((ArmorMetaItem<?>) baubleStack.getItem()).getItem(baubleStack);
                    
                    if (armorMetaValue.getArmorLogic() instanceof ArmorLogicSuite) {
                        ArmorLogicSuite armorLogic = (ArmorLogicSuite) armorMetaValue.getArmorLogic();
                        if (armorLogic.isNeedDrawHUD()) armorLogic.drawHUD(baubleStack);
                    } else if (armorMetaValue.getArmorLogic() instanceof PowerlessJetpack) {
                        PowerlessJetpack armorLogic = (PowerlessJetpack) armorMetaValue.getArmorLogic();
                        if (armorLogic.isNeedDrawHUD()) armorLogic.drawHUD(baubleStack);
                    }
                }
            }
        }
    }
}