package top.ahmadb.gregicalityutils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockAnalogEmitter extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockAnalogEmitter() {
        super(Material.IRON);
        this.setRegistryName("analog_emitter");
        this.setTranslationKey("gregicalityutils.analog_emitter"); 
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.setHardness(3.0F);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer).getOpposite());
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        this.updatePowerState(worldIn, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        this.updatePowerState(worldIn, pos, state);
    }

    private void updatePowerState(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) return;
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityAnalogEmitter) {
            EnumFacing inputFace = state.getValue(FACING);
            // Check if there is redstone power coming straight into our input face
            boolean isPowered = world.getRedstonePower(pos.offset(inputFace), inputFace) > 0;
            ((TileEntityAnalogEmitter) te).setPowered(isPowered);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            if (!world.isRemote) {
                world.setBlockState(pos, state.withProperty(FACING, state.getValue(FACING).rotateY()));
            }
            return true;
        }

        if (world.isRemote) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiAnalogEmitter(pos));
        }
        return true;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        // Stop the front face from outputting power
        if (side.getOpposite() == blockState.getValue(FACING)) {
            return 0;
        }

        // Only emit if the TileEntity says it is powered
        TileEntity te = blockAccess.getTileEntity(pos);
        if (te instanceof TileEntityAnalogEmitter) {
            TileEntityAnalogEmitter emitter = (TileEntityAnalogEmitter) te;
            if (emitter.isPowered()) {
                return emitter.getSignalLevel();
            }
        }
        return 0;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }
    
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityAnalogEmitter();
    }
}