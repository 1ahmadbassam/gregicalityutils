package top.ahmadb.gregicalityutils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

public class TileEntityTwerkSimulator extends TileEntity implements ITickable {

    private int ticks = 0;

    @Override
    public void update() {
        // Always ensure this logic only runs on the Server side
        if (world.isRemote || !(world instanceof WorldServer)) return; 

        // If the block is receiving a redstone signal, do not increment ticks or apply growth
        if (world.isBlockPowered(pos)) {
            return;
        }

        ticks++;
        if (ticks >= GUConfig.twerkRate) {
            ticks = 0;
            applyGrowthEffect();
        }
    }

    private void applyGrowthEffect() {
        int r = GUConfig.twerkRadius;

        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft((WorldServer) world);
        ItemStack dummyBonemeal = new ItemStack(Items.DYE, 1, 15);

        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos target = pos.add(x, y, z);
                    IBlockState state = world.getBlockState(target);
                    Block block = state.getBlock();

                    if (block instanceof IGrowable) {
                        IGrowable growable = (IGrowable) block;

                        if (growable.canGrow(world, target, state, false)) {
                            if (block.getClass().getSimpleName().equals("BlockEnderLilly")) {
                                growable.grow(world, world.rand, target, state);
                                world.playEvent(2005, target, 0);
                            }
                            else if (ItemDye.applyBonemeal(dummyBonemeal.copy(), world, target, fakePlayer, EnumHand.MAIN_HAND)) {
                                world.playEvent(2005, target, 0); 
                            }
                        }
                    }
                    else if (block instanceof BlockReed || block instanceof BlockCactus || block instanceof BlockNetherWart) {
                        block.updateTick(world, target, state, world.rand);
                        world.playEvent(2005, target, 0);
                    }
                }
            }
        }
    }
}