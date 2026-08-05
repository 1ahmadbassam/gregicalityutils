package top.ahmadb.gregicalityutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(Side.CLIENT)
public class RingToggleKeybind {
    
    public static KeyBinding toggleRings;
    public static boolean areRingsEnabled = true; // Default state

    public static void init() {
        toggleRings = new KeyBinding(
            "key.gregicalityutils.togglerings", 
            KeyConflictContext.IN_GAME, 
            Keyboard.KEY_G, // Default key, configurable by user
            "Gregicality Utils"
        );
        ClientRegistry.registerKeyBinding(toggleRings);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (toggleRings.isPressed()) {
            areRingsEnabled = !areRingsEnabled;
            
            // Optional: Send a chat message to the player so they know the state changed
            if (Minecraft.getMinecraft().player != null) {
                String state = areRingsEnabled ? "Enabled" : "Disabled";
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString("Extra Utilities Flight Rings: " + state));
            }
        }
    }
}