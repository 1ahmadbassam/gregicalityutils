package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.compatibility.CompatHelper;
import com.rwtema.extrautils2.compatibility.StackHelper;
import com.rwtema.extrautils2.crafting.NullRecipe;
import com.rwtema.extrautils2.itemhandler.SingleStackHandler;
import com.rwtema.extrautils2.itemhandler.StackDump;
import com.rwtema.extrautils2.itemhandler.XUCrafter;
import com.rwtema.extrautils2.tile.TileAnalogCrafter;
import com.rwtema.extrautils2.tile.TilePower;
import com.rwtema.extrautils2.utils.datastructures.ArrayAccess;
import com.rwtema.extrautils2.utils.datastructures.NBTSerializable;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileAnalogCrafter.class, remap = false)
public abstract class MixinTileAnalogCrafter extends TilePower {

    @Shadow private SingleStackHandler output;
    @Shadow private StackDump extraStacks;
    @Shadow private NBTSerializable.Int progress;
    @Shadow private NBTSerializable.Int max_progress;
    @Shadow private NBTSerializable.NBTBoolean sticky;
    @Shadow private NBTSerializable.NBTBoolean spread;
    @Shadow public NBTSerializable.NBTEnum<?> redstone_state;
    @Shadow public NBTSerializable.NBTBoolean powered;
    @Shadow public NBTSerializable.Int pulses;
    @Shadow XUCrafter crafter;
    @Shadow private ItemStackHandler contents;

    @Shadow private IRecipe getRecipe() { return null; }
    @Shadow private void trySpreadItems() {}

    /**
     * @author ahmadb
     * @reason Reorders update logic to check for full output inventory before performing 
     * expensive recipe calculations and item spreading, fixing massive tick lag.
     */
    @Inject(method = {"update", "func_73660_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    public void optimizedUpdate(CallbackInfo ci) {
        // Cancel the original method immediately so the unoptimized code never runs
        ci.cancel();

        if (this.world.isRemote) return;

        if (this.extraStacks.hasStacks()) {
            this.extraStacks.attemptDump(this.output);
        }

        if ((this.world.getTotalWorldTime() % 4L) != 0L) {
            return;
        }

        IRecipe recipe = this.getRecipe();
        if (recipe == NullRecipe.INSTANCE) {
            this.progress.value = 0;
            this.max_progress.value = -1;
            return;
        }

        // =========================================================
        // OPTIMIZATION: Early Crafting Result & Output Check
        // =========================================================
        this.crafter.loadStacks(this.contents);
        
        if (!recipe.matches(this.crafter, this.world)) {
            return; 
        }

        ItemStack craftingResult = recipe.getCraftingResult(this.crafter);

        if (StackHelper.isNull(craftingResult) || StackHelper.isNonNull(this.output.insertItem(0, craftingResult, true))) {
            this.max_progress.value = 0;
            this.progress.value = 0;
            return;
        }
        // =========================================================

        if (this.spread.value) {
            this.trySpreadItems();
            this.crafter.loadStacks(this.contents);
        }

        String rsState = ((Enum<?>) this.redstone_state.value).name();
        switch (rsState) {
            case "OPERATE_REDSTONE_ON":
                if (!this.powered.value) {
                    this.progress.value = this.max_progress.value = 0;
                    return;
                }
                break;
            case "OPERATE_REDSTONE_OFF":
                if (this.powered.value) {
                    this.progress.value = this.max_progress.value = 0;
                    return;
                }
                break;
            case "OPERATE_REDSTONE_PULSE":
                if (this.pulses.value == 0) {
                    this.progress.value = this.max_progress.value = 0;
                    return;
                }
                break;
        }

        if (this.sticky.value) {
            for (int i = 0; i < this.contents.getSlots(); i++) {
                ItemStack stackInSlot = this.contents.getStackInSlot(i);
                if (StackHelper.isNonNull(stackInSlot)) {
                    if (StackHelper.getStacksize(stackInSlot) == 1 && stackInSlot.getMaxStackSize() > 1) {
                        this.progress.value = 0;
                        this.max_progress.value = 0;
                        return;
                    }
                }
            }
        }

        if (this.max_progress.value <= 0) {
            this.max_progress.value = StackHelper.getStacksize(craftingResult) * 4 * 5; 
        }

        this.progress.value += 4; 

        if (this.progress.value >= this.max_progress.value) {
            if (this.pulses.value > 0) this.pulses.value--;

            this.progress.value = 0;
            this.output.insertItem(0, craftingResult, false);

            ArrayAccess<ItemStack> remainingStacks = CompatHelper.getArray10List11(CompatHelper.getRemainingItems(this.crafter, this.world));
            for (int i = 0; i < remainingStacks.length(); ++i) {
                ItemStack curStack = this.crafter.getStackInSlot(i);
                ItemStack remainStack = remainingStacks.get(i);

                if (StackHelper.isNonNull(curStack)) {
                    this.contents.extractItem(i, 1, false);
                }

                if (StackHelper.isNonNull(remainStack)) {
                    remainStack = this.contents.insertItem(i, remainStack, false);
                    if (StackHelper.isNonNull(remainStack)) {
                        this.extraStacks.addStack(remainStack);
                    }
                }
            }
        }
    }
}