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

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Register the networking packet. ID 0, goes to the SERVER side.
        NETWORK.registerMessage(PacketUpdateEmitter.Handler.class, PacketUpdateEmitter.class, 0, Side.SERVER);
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
            
            // Register the TileEntities
            GameRegistry.registerTileEntity(TileEntityAnalogEmitter.class, new ResourceLocation(MODID, "analog_emitter"));
            GameRegistry.registerTileEntity(TileEntityTwerkSimulator.class, new ResourceLocation(MODID, "twerk_simulator"));
            GameRegistry.registerTileEntity(TileEntitySheepStimulator.class, new ResourceLocation(MODID, "sheep_stimulator"));
        }

        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            // Register the Item forms of the blocks so they can exist in the inventory
            event.getRegistry().register(new ItemBlock(ANALOG_EMITTER).setRegistryName(ANALOG_EMITTER.getRegistryName()));
            event.getRegistry().register(new ItemBlock(TWERK_SIMULATOR).setRegistryName(TWERK_SIMULATOR.getRegistryName()));
            event.getRegistry().register(new ItemBlock(SHEEP_STIMULATOR).setRegistryName(SHEEP_STIMULATOR.getRegistryName()));
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
        }
    }
}