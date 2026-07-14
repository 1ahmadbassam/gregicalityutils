package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.backend.XUBlockStateCreator;
import com.rwtema.extrautils2.tile.TileAdvInteractor;
import com.rwtema.extrautils2.tile.TileUse;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileUse.class, remap = false)
public abstract class MixinTileUse extends TileAdvInteractor {

    @Shadow private net.minecraftforge.items.ItemStackHandler contents;

    // Use a solid world-time timestamp instead of a ticking countdown
    private long xu2_nextAllowedTick = 0L;
    private ItemStack[] xu2_headSnapshot = new ItemStack[9];
    private ItemStack[] xu2_sleepSnapshot = new ItemStack[9];
    private IBlockState xu2_headBlockState = null;

    /**
     * @author ahmadb
     * @reason Throttles laggy click events by enforcing a timestamp safety window
     * if the previous operation failed to do any inventory or world work.
     */
    @Inject(method = "operate", at = @At("HEAD"), cancellable = true)
    private void optimizedOperateHead(CallbackInfoReturnable<Boolean> cir) {
        long currentWorldTime = this.world.getTotalWorldTime();
        boolean inventoryChangedExternally = false;

        // 1. Snapshot the current inventory state
        for (int i = 0; i < 9; i++) {
            ItemStack current = this.contents.getStackInSlot(i);
            this.xu2_headSnapshot[i] = (current == null || current.isEmpty()) ? ItemStack.EMPTY : current.copy();

            // 2. If we are waiting out a failure window, check if a pipe/hopper changed our items
            if (currentWorldTime < this.xu2_nextAllowedTick) {
                ItemStack sleepStack = this.xu2_sleepSnapshot[i];
                if (sleepStack == null) sleepStack = ItemStack.EMPTY;

                if (!ItemStack.areItemStacksEqual(this.xu2_headSnapshot[i], sleepStack)) {
                    inventoryChangedExternally = true;
                }
            }
        }

        // 3. Evaluate the Throttling window
        if (currentWorldTime < this.xu2_nextAllowedTick) {
            if (inventoryChangedExternally) {
                // A pipe/hopper interacted with us! Wake up immediately to process the new item.
                this.xu2_nextAllowedTick = 0L; 
            } else {
                // Bypass the operation safely without compounding the delay
                cir.setReturnValue(true); 
                return;
            }
        }

        // 4. Capture the block state before the right-click occurs
        EnumFacing side = this.getBlockState().getValue(XUBlockStateCreator.ROTATION_ALL);
        BlockPos targetPosBlock = this.getPos().offset(side);
        this.xu2_headBlockState = this.world.getBlockState(targetPosBlock);
    }

    /**
     * Checks the aftermath of the action. If neither the block nor our inventory updated,
     * calculate a small 20-tick window before allowing another heavy click sequence.
     */
    @Inject(method = "operate", at = @At("RETURN"))
    private void optimizedOperateReturn(CallbackInfoReturnable<Boolean> cir) {
        long currentWorldTime = this.world.getTotalWorldTime();

        // Only process if we actually ran a fresh click check this tick
        if (currentWorldTime >= this.xu2_nextAllowedTick) {
            boolean didWork = false;

            // Check A: Did our inventory change (e.g., filled a bucket)?
            for (int i = 0; i < 9; i++) {
                ItemStack current = this.contents.getStackInSlot(i);
                if (current == null) current = ItemStack.EMPTY;

                ItemStack headStack = this.xu2_headSnapshot[i];
                if (headStack == null) headStack = ItemStack.EMPTY;

                if (!ItemStack.areItemStacksEqual(current, headStack)) {
                    didWork = true;
                    break;
                }
            }

            // Check B: Did the block change (e.g., Quark harvested/replanted the crop)?
            if (!didWork && this.xu2_headBlockState != null) {
                EnumFacing side = this.getBlockState().getValue(XUBlockStateCreator.ROTATION_ALL);
                BlockPos targetPosBlock = this.getPos().offset(side);
                IBlockState currentTargetState = this.world.getBlockState(targetPosBlock);

                if (currentTargetState != this.xu2_headBlockState) {
                    didWork = true;
                }
            }

            // If absolutely nothing happened, set a 1-second (20-tick) safety window
            if (!didWork) {
                this.xu2_nextAllowedTick = currentWorldTime + 20L; 
                
                // Keep track of our item arrangement during this rest period
                for (int i = 0; i < 9; i++) {
                    ItemStack current = this.contents.getStackInSlot(i);
                    this.xu2_sleepSnapshot[i] = (current == null || current.isEmpty()) ? ItemStack.EMPTY : current.copy();
                }
            } else {
                // Work was successful! Reset the window so subsequent fast-actions can continue
                this.xu2_nextAllowedTick = 0L;
            }
        }
    }
}