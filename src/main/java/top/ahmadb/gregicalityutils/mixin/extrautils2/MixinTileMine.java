package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.tile.TileMine;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Mixin(value = TileMine.class, remap = false)
public abstract class MixinTileMine extends TileEntity {

    /**
     * Intercepts the tryHarvestBlock call inside operate().
     * Note: remap = true on the @At because tryHarvestBlock is a Vanilla method that needs obfuscation mapping.
     */
    @Redirect(
            method = "operate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/PlayerInteractionManager;tryHarvestBlock(Lnet/minecraft/util/math/BlockPos;)Z",
                    remap = true 
            )
    )
    private boolean redirectTryHarvestBlock(PlayerInteractionManager interactionManager, BlockPos offset) {
        World world = this.getWorld();
        if (world == null) return false;

        IBlockState blockState = world.getBlockState(offset);
        Block targetBlock = blockState.getBlock();

        // Check if the targeted block is a giant mushroom
        boolean isMushroom = targetBlock == Blocks.BROWN_MUSHROOM_BLOCK || targetBlock == Blocks.RED_MUSHROOM_BLOCK;

        // If it's not a mushroom, just do the normal single-block break
        if (!isMushroom) {
            return interactionManager.tryHarvestBlock(offset);
        }

        // --- MUSHROOM VEIN MINER LOGIC ---
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(offset);
        visited.add(offset);

        boolean anySuccess = false;
        int blocksMined = 0;
        int MAX_BLOCKS = 128; // Safety limit

        while (!queue.isEmpty() && blocksMined < MAX_BLOCKS) {
            BlockPos currentPos = queue.poll();
            IBlockState currentState = world.getBlockState(currentPos);

            // If the neighboring block is the same type of mushroom block
            if (currentState.getBlock() == targetBlock) {
                // Attempt to break it
                if (interactionManager.tryHarvestBlock(currentPos)) {
                    anySuccess = true;
                    blocksMined++;

                    // Add all 6 adjacent blocks to the queue to check them next
                    for (EnumFacing facing : EnumFacing.VALUES) {
                        BlockPos neighbor = currentPos.offset(facing);
                        if (visited.add(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        // Return true if we successfully mined at least one block
        return anySuccess;
    }
}