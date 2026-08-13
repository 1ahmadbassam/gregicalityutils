package top.ahmadb.gregicalityutils;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = GregicalityUtils.MODID, name = GregicalityUtils.NAME, version = GregicalityUtils.VERSION)
public class GregicalityUtils {

    public static final String MODID = "gregicalityutils";
    public static final String NAME = "Gregicality Utils";
    public static final String VERSION = "1.0";

    // Required so Forge can inject the active instance of your mod
    @Mod.Instance(MODID)
    public static GregicalityUtils instance;

    // The network channel we use to send the + and - button clicks from the GUI to the Server
    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    // Initialize our blocks
    public static final Block ANALOG_EMITTER = new BlockAnalogEmitter().setCreativeTab(net.minecraft.creativetab.CreativeTabs.REDSTONE);
    public static final Block TWERK_SIMULATOR = new BlockTwerkSimulator();
    public static final Block SHEEP_STIMULATOR = new BlockSheepStimulator();
    
    public static final Block CAPABILITY_PROXY = new BlockCapabilityProxy().setCreativeTab(net.minecraft.creativetab.CreativeTabs.REDSTONE); // Adjust tab if needed

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Register the networking packet. ID 0, goes to the SERVER side.
        NETWORK.registerMessage(PacketUpdateEmitter.Handler.class, PacketUpdateEmitter.class, 0, Side.SERVER);
        
        // Register the Capability Proxy packet on ID 1
        NETWORK.registerMessage(PacketUpdateCapabilityProxy.Handler.class, PacketUpdateCapabilityProxy.class, 1, Side.SERVER);

        // Register the Smart Blocking packet on ID 2
        NETWORK.registerMessage(PacketToggleSmartBlocking.Handler.class, PacketToggleSmartBlocking.class, 2, Side.SERVER);

        // ONLY initialize client-side features like keybinds if we are on the physical client
        if (event.getSide() == Side.CLIENT) {
            top.ahmadb.gregicalityutils.client.RingToggleKeybind.init();
            top.ahmadb.gregicalityutils.client.SmartBlockingKeybind.init();
        }
    }

    // This nested class handles all the standard 1.12.2 Forge registries automatically
    @Mod.EventBusSubscriber
    public static class RegistrationHandler {

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            // Register the Blocks
            event.getRegistry().register(ANALOG_EMITTER);
            event.getRegistry().register(TWERK_SIMULATOR);
            event.getRegistry().register(SHEEP_STIMULATOR);
            event.getRegistry().register(CAPABILITY_PROXY);
            
            // Register the TileEntities
            GameRegistry.registerTileEntity(TileEntityAnalogEmitter.class, new ResourceLocation(MODID, "analog_emitter"));
            GameRegistry.registerTileEntity(TileEntityTwerkSimulator.class, new ResourceLocation(MODID, "twerk_simulator"));
            GameRegistry.registerTileEntity(TileEntitySheepStimulator.class, new ResourceLocation(MODID, "sheep_stimulator"));
            GameRegistry.registerTileEntity(TileEntityCapabilityProxy.class, new ResourceLocation(MODID, "capability_proxy"));
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            // Register the Item forms of the blocks so they can exist in the inventory
            event.getRegistry().register(new ItemBlock(ANALOG_EMITTER).setRegistryName(ANALOG_EMITTER.getRegistryName()));
            event.getRegistry().register(new ItemBlock(TWERK_SIMULATOR).setRegistryName(TWERK_SIMULATOR.getRegistryName()));
            event.getRegistry().register(new ItemBlock(SHEEP_STIMULATOR).setRegistryName(SHEEP_STIMULATOR.getRegistryName()));
            
            event.getRegistry().register(new ItemBlock(CAPABILITY_PROXY).setRegistryName(CAPABILITY_PROXY.getRegistryName()));
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            // Register the visual models for the inventory items
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(ANALOG_EMITTER), 0, 
                new ModelResourceLocation(ANALOG_EMITTER.getRegistryName(), "inventory"));
                
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(TWERK_SIMULATOR), 0, 
                new ModelResourceLocation(TWERK_SIMULATOR.getRegistryName(), "inventory"));
                
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(SHEEP_STIMULATOR), 0, 
                new ModelResourceLocation(SHEEP_STIMULATOR.getRegistryName(), "inventory"));
                
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(CAPABILITY_PROXY), 0, 
                new ModelResourceLocation(CAPABILITY_PROXY.getRegistryName(), "inventory"));
        }
    }
}