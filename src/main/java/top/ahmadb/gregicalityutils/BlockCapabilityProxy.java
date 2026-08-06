package top.ahmadb.gregicalityutils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockCapabilityProxy extends Block {

    public BlockCapabilityProxy() {
        super(Material.IRON);
        setRegistryName("capability_proxy");
        setTranslationKey("gregicalityutils.capability_proxy");
        this.setHardness(3.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityCapabilityProxy();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            openGui(pos);
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    private void openGui(BlockPos pos) {
        TileEntity te = Minecraft.getMinecraft().world.getTileEntity(pos);
        if (te instanceof TileEntityCapabilityProxy) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiCapabilityProxy((TileEntityCapabilityProxy) te));
        }
    }
}