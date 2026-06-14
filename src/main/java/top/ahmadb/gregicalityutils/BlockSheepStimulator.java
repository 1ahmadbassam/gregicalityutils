package top.ahmadb.gregicalityutils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockSheepStimulator extends Block {

    public BlockSheepStimulator() {
        // Wood material so it fits early game/steam age vibes
        super(Material.WOOD); 
        setRegistryName("sheep_stimulator");
        setTranslationKey("gregicalityutils.sheep_stimulator");
        setCreativeTab(CreativeTabs.MISC); // Change to your mod's tab if you have one
        setHardness(2.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntitySheepStimulator();
    }
}