package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.integrations.exnihilocreatio.machines.SteamRockBreaker;
import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidTank;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SteamRockBreaker.class, remap = false)
public abstract class MixinSteamRockBreaker extends MetaTileEntity {

    // Dummy constructor required because we extend MetaTileEntity
    public MixinSteamRockBreaker(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    // Shadow the internal steam tank
    @Shadow
    private FluidTank steamFluidTank;

    // Shadow the static final constant
    @Shadow
    @Final
    private static int STEAM_DRAIN_PER_CYCLE;

    /**
     * @author ahmadb
     * @reason Allow liquid checks from UP and DOWN faces.
     */
    @Overwrite
    private boolean checkSides(BlockStaticLiquid liquid) {
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
     * Inject at the TAIL of the update method to handle the Witchwater logic.
     */
    @Inject(method = "update", at = @At("TAIL"))
    private void injectedUpdate(CallbackInfo ci) {
        if (this.getWorld().isRemote) return;

        // Dynamically fetch Witchwater from the registry
        Block witchWaterBlock = Block.getBlockFromName("exnihilocreatio:witchwater");
        if (witchWaterBlock == null) return;

        // Ensure we meet the exact same timing and fluid conditions as standard stone generation
        if (this.getTimer() % 32 == 0 && this.steamFluidTank.getFluidAmount() >= STEAM_DRAIN_PER_CYCLE * 4) {
            
            // Check for our Water + Witchwater combination
            if (checkSidesBlock(Blocks.WATER) && checkSidesBlock(witchWaterBlock)) {
                
                // Calculate the highest redstone signal hitting the machine
                int largestSignal = 0;
                for (EnumFacing face : EnumFacing.VALUES) {
                    largestSignal = Math.max(this.getInputRedstoneSignal(face, false), largestSignal);
                }

                // Determine output based on redstone signal
                ItemStack output;
                if (largestSignal >= 8) {
                    output = new ItemStack(Blocks.DIRT, 1, 1); // Coarse Dirt
                } else {
                    output = new ItemStack(Blocks.DIRT, 1, 0); // Normal Dirt
                }

                // Insert into the single export slot and drain steam
                this.exportItems.insertItem(0, output, false);
                this.steamFluidTank.drain(STEAM_DRAIN_PER_CYCLE, true);
            }
        }
    }
}