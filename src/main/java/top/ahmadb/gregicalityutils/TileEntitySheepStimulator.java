package top.ahmadb.gregicalityutils;

import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;

import java.util.List;

public class TileEntitySheepStimulator extends TileEntity implements ITickable {

    @Override
    public void update() {
        // Only run on the server to prevent ghost blocks
        if (this.world.isRemote) return;

        // REDSTONE CONTROL: Stop functioning if the block is receiving a redstone signal
        if (this.world.isBlockPowered(this.pos)) return;

        // CONFIG RATE: Use the tick rate defined in GUConfig.java
        if (this.world.getTotalWorldTime() % GUConfig.sheepStimulatorRate == 0) {
            // Creates a 9x9x9 area centered on the stimulator
            AxisAlignedBB aabb = new AxisAlignedBB(this.pos).grow(4.0D);
            List<EntitySheep> sheepList = this.world.getEntitiesWithinAABB(EntitySheep.class, aabb);

            for (EntitySheep sheep : sheepList) {
                if (sheep.getSheared() && !sheep.isChild()) {
                    BlockPos posBelow = new BlockPos(sheep.posX, sheep.posY - 0.2D, sheep.posZ);

                    if (this.world.getBlockState(posBelow).getBlock() == Blocks.GRASS) {
                        // Play particles/sound, convert grass to dirt, and trigger vanilla eat logic
                        this.world.playEvent(2001, posBelow, Block.getIdFromBlock(Blocks.GRASS));
                        this.world.setBlockState(posBelow, Blocks.DIRT.getDefaultState(), 2);
                        sheep.eatGrassBonus();
                    }
                }
            }
        }
    }
}