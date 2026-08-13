package top.ahmadb.gregicalityutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;
import top.ahmadb.gregicalityutils.GregicalityUtils;
import top.ahmadb.gregicalityutils.PacketToggleSmartBlocking;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GregicalityUtils.MODID)
public class SmartBlockingKeybind {
    
    public static final KeyBinding toggleSmartBlock = new KeyBinding("key.smartblock.toggle", Keyboard.KEY_NONE, "key.categories.gregicalityutils");

    public static void init() {
        ClientRegistry.registerKeyBinding(toggleSmartBlock);
    }

    @SubscribeEvent
    public static void onKeyInput(KeyInputEvent event) {
        if (toggleSmartBlock.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            RayTraceResult mop = mc.objectMouseOver;
            
            if (mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK) {
                GregicalityUtils.NETWORK.sendToServer(new PacketToggleSmartBlocking(mop.getBlockPos(), mop.sideHit));
            } else {
                mc.player.sendMessage(new TextComponentString("\u00A7cYou must be looking at an AE2 Interface to configure it."));
            }
        }
    }
}