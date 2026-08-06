package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.rwtema.extrautils2.compatibility.CompatHelper;
import com.rwtema.extrautils2.compatibility.StackHelper;
import com.rwtema.extrautils2.crafting.NullRecipe;
import com.rwtema.extrautils2.itemhandler.SingleStackHandler;
import com.rwtema.extrautils2.itemhandler.SingleStackHandlerUpgrades;
import com.rwtema.extrautils2.itemhandler.StackDump;
import com.rwtema.extrautils2.itemhandler.XUCrafter;
import com.rwtema.extrautils2.power.PowerManager;
import com.rwtema.extrautils2.tile.TileAnalogCrafter;
import com.rwtema.extrautils2.tile.TilePower;
import com.rwtema.extrautils2.transfernodes.Upgrade;
import com.rwtema.extrautils2.utils.datastructures.ArrayAccess;
import com.rwtema.extrautils2.utils.datastructures.NBTSerializable;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

@Mixin(value = TileAnalogCrafter.class, remap = false)
public abstract class MixinTileAnalogCrafter extends TilePower implements IAnalogCrafterExtensions {

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
    @Shadow protected abstract <T> T registerNBT(String name, T object);
    @Shadow NBTSerializable.NBTByteArray slot_sides;
    @Shadow private net.minecraftforge.items.IItemHandler[] sideHandlers;
    @Shadow private com.rwtema.extrautils2.itemhandler.PublicWrapper.Extract extractHandler;

    @Unique private SingleStackHandlerUpgrades gcu_upgrades;
    @Unique private NBTSerializable.NBTBoolean gcu_limit_to_one;
    @Unique private int gcu_accumulator = 0;
    @Unique private int gcu_lastSlotSidesHash = 0;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gcu$onInit(CallbackInfo ci) {
        this.gcu_upgrades = new SingleStackHandlerUpgrades(EnumSet.of(Upgrade.SPEED)) {
            @Override
            protected void onContentsChanged() {
                MixinTileAnalogCrafter.this.markDirty();
                PowerManager.instance.markDirty(MixinTileAnalogCrafter.this);
            }
        };
        this.gcu_limit_to_one = this.registerNBT("limit_to_one", new NBTSerializable.NBTBoolean(false));
    }

    @Override
    public net.minecraft.nbt.NBTTagCompound writeToNBT(net.minecraft.nbt.NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        if (this.gcu_upgrades != null) {
            compound.setTag("gcu_upgrades_data", this.gcu_upgrades.serializeNBT());
        }
        return compound;
    }

    @Override
    public void readFromNBT(net.minecraft.nbt.NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (this.gcu_upgrades != null && compound.hasKey("gcu_upgrades_data")) {
            this.gcu_upgrades.deserializeNBT(compound.getCompoundTag("gcu_upgrades_data"));
        }
    }

    @Override
    protected Iterable<ItemStack> getDropHandler() {
        return com.rwtema.extrautils2.compatibility.InventoryHelper.getItemHandlerIterator(this.contents, this.output, this.gcu_upgrades);
    }

    @Override
    public SingleStackHandlerUpgrades gcu_getUpgrades() {
        return this.gcu_upgrades;
    }

    @Override
    public NBTSerializable.NBTBoolean gcu_getLimitToOne() {
        return this.gcu_limit_to_one;
    }

    @Inject(method = "getPower", at = @At("HEAD"), cancellable = true)
    public void gcu$getPower(CallbackInfoReturnable<Float> cir) {
        int level = this.gcu_upgrades.getLevel(Upgrade.SPEED);
        if (level == 0) cir.setReturnValue(Float.NaN);
        else cir.setReturnValue(Upgrade.SPEED.getPowerUse(level));
    }

    @Inject(method = "getSideHandler", at = @At("HEAD"))
    private void gcu$invalidateStaleSideHandlers(int face, CallbackInfoReturnable<net.minecraftforge.items.IItemHandler> cir) {
        if (this.slot_sides != null && this.slot_sides.array != null) {
            int currentHash = java.util.Arrays.hashCode(this.slot_sides.array);
            if (currentHash != this.gcu_lastSlotSidesHash) {
                this.sideHandlers = null;
                this.gcu_lastSlotSidesHash = currentHash;
            }
        }
    }

    @Inject(method = "getSideHandler", at = @At("RETURN"), cancellable = true)
    private void gcu$fixOutputSides(int face, CallbackInfoReturnable<net.minecraftforge.items.IItemHandler> cir) {
        if (cir.getReturnValue() == null) {
            if (this.sideHandlers != null) {
                this.sideHandlers[face] = this.extractHandler;
            }
            cir.setReturnValue(this.extractHandler);
        }
    }

    @Inject(method = {"update", "func_73660_a"}, at = @At("HEAD"), cancellable = true)
    public void optimizedUpdate(CallbackInfo ci) {
        ci.cancel();
        if (this.world.isRemote) return;

        if (this.extraStacks.hasStacks()) {
            this.extraStacks.attemptDump(this.output);
        }

        int speed = 1 + this.gcu_upgrades.getLevel(Upgrade.SPEED);
        this.gcu_accumulator += speed;

        while (this.gcu_accumulator >= 4) {
            this.gcu_accumulator -= 4;

            boolean craftedOrWorking = gcu$performCraftingStep();
            if (!craftedOrWorking) {
                this.gcu_accumulator = 0; 
                break;
            }
        }
    }

    @Unique
    private boolean gcu$performCraftingStep() {
        IRecipe recipe = this.getRecipe();
        if (recipe == NullRecipe.INSTANCE) {
            this.progress.value = 0;
            this.max_progress.value = -1;
            return false;
        }

        this.crafter.loadStacks(this.contents);

        if (!recipe.matches(this.crafter, this.world)) {
            return false;
        }

        ItemStack craftingResult = recipe.getCraftingResult(this.crafter);

        if (StackHelper.isNull(craftingResult) || StackHelper.isNonNull(this.output.insertItem(0, craftingResult, true))) {
            this.max_progress.value = 0;
            this.progress.value = 0;
            return false;
        }

        if (this.spread.value) {
            this.trySpreadItems();
            this.crafter.loadStacks(this.contents);
        }

        String rsState = ((Enum<?>) this.redstone_state.value).name();
        switch (rsState) {
            case "OPERATE_REDSTONE_ON":
                if (!this.powered.value) {
                    this.progress.value = this.max_progress.value = 0;
                    return false;
                }
                break;
            case "OPERATE_REDSTONE_OFF":
                if (this.powered.value) {
                    this.progress.value = this.max_progress.value = 0;
                    return false;
                }
                break;
            case "OPERATE_REDSTONE_PULSE":
                if (this.pulses.value == 0) {
                    this.progress.value = this.max_progress.value = 0;
                    return false;
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
                        return false;
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
                    ItemStack currentInSlot = this.contents.getStackInSlot(i);
                    if (StackHelper.isNull(currentInSlot)) {
                        this.contents.setStackInSlot(i, remainStack);
                    } else {
                        remainStack = this.contents.insertItem(i, remainStack, false);
                        if (StackHelper.isNonNull(remainStack)) {
                            this.extraStacks.addStack(remainStack);
                        }
                    }
                }
            }
        }
        return true;
    }
}