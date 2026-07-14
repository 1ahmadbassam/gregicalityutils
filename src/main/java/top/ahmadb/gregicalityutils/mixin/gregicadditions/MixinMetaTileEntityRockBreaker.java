package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.MetaTileEntityRockBreaker;
import gregtech.api.metatileentity.TieredMetaTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MetaTileEntityRockBreaker.class, remap = false)
public abstract class MixinMetaTileEntityRockBreaker extends TieredMetaTileEntity {

    // Dummy constructor required by the compiler since we are extending TieredMetaTileEntity
    // The Mixin processor ignores this.
    public MixinMetaTileEntityRockBreaker(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
    }

    // Shadow the private method from MetaTileEntityRockBreaker
    @Shadow
    private int getEnergyPerBlockBreak() { return 0; }

    /**
     * @author ahmadb
     * @reason Allow liquid checks from UP and DOWN faces.
     */
    @Overwrite
    private boolean checkSides(BlockStaticLiquid liquid) {
        // Reroute the original check to our new Block-based checker
        return checkSidesBlock(liquid);
    }

    /**
     * A broader version of checkSides that accepts standard Blocks 
     * to safely handle Modded fluids like Witchwater.
     */
    private boolean checkSidesBlock(Block block) {
        EnumFacing frontFacing = this.getFrontFacing();
        for (EnumFacing side : EnumFacing.VALUES) {
            // ONLY ignore the front facing (output face)
            if (side == frontFacing) continue;
            
            if (this.getWorld().getBlockState(this.getPos().offset(side)).getBlock() == block) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inject at the TAIL of the update method to add Witchwater logic
     * without breaking the original stone generation.
     */
    @Inject(method = "update", at = @At("TAIL"))
    private void injectedUpdate(CallbackInfo ci) {
        // Run on server only and tick rate check
        if (this.getWorld().isRemote || this.getTimer() % 20 != 0) return;

        // Dynamically fetch Witchwater from the registry
        Block witchWaterBlock = Block.getBlockFromName("exnihilocreatio:witchwater");
        if (witchWaterBlock == null) return;

        // Check for our fluid combinations
        if (checkSidesBlock(Blocks.WATER) && checkSidesBlock(witchWaterBlock)) {
            
            if (this.energyContainer.getEnergyStored() >= getEnergyPerBlockBreak()) {
                int stackSize = (int) Math.pow(2, getTier());

                // Metadata 0 = Dirt, Metadata 1 = Coarse Dirt
                ItemStack dirt = new ItemStack(Blocks.DIRT, stackSize, 0);
                ItemStack coarseDirt = new ItemStack(Blocks.DIRT, stackSize, 1);

                // Distribute across the 16 slots (0-7 for dirt, 8-15 for coarse dirt)
                for (int i = 0; i < 8; i++) {
                    dirt = this.exportItems.insertItem(i, dirt, false);
                }
                for (int i = 8; i < 16; i++) {
                    coarseDirt = this.exportItems.insertItem(i, coarseDirt, false);
                }

                // Deduct energy if items were successfully pushed into the output buffer
                if (dirt.getCount() != stackSize || coarseDirt.getCount() != stackSize) {
                    this.energyContainer.removeEnergy(getEnergyPerBlockBreak());
                }
            }
        }
    }
}