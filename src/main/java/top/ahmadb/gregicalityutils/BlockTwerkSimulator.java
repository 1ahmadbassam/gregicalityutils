package top.ahmadb.gregicalityutils;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockTwerkSimulator extends Block implements ITileEntityProvider {

    public BlockTwerkSimulator() {
        super(Material.IRON);
        setRegistryName("twerk_simulator");
        setTranslationKey("gregicalityutils.twerk_simulator");
        setCreativeTab(CreativeTabs.MISC);
        setHardness(3.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTwerkSimulator();
    }
}