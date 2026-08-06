package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.metatileentity.MetaTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ahmadb.gregicalityutils.ProxyRegistry;
import top.ahmadb.gregicalityutils.TileEntityCapabilityProxy;

import java.util.List;

@Mixin(value = MetaTileEntity.class, remap = false)
public abstract class MixinMetaTileEntityProxyInterop {

    @Shadow(remap = false) public abstract World getWorld();
    @Shadow(remap = false) public abstract BlockPos getPos();
    @Shadow(remap = false) public abstract <T> T getCoverCapability(Capability<T> capability, EnumFacing side);
    @Shadow(remap = false) protected static void moveInventoryItems(IItemHandler sourceInventory, IItemHandler targetInventory) { }
    
    // Shadow Nomifactory's Notifiable Lists so we can access them
    @Shadow(remap = false) protected List<net.minecraftforge.items.IItemHandlerModifiable> notifiedItemOutputList;
    @Shadow(remap = false) protected List<IFluidHandler> notifiedFluidOutputList;

    @Inject(method = "update", at = @At("TAIL"), remap = false)
    private void interceptUpdateForProxies(CallbackInfo ci) {
        World world = getWorld();
        if (world == null || world.isRemote) return;

        // 1. TPS Optimization: If this machine is asleep (no notifications), do absolutely nothing!
        if (this.notifiedItemOutputList.isEmpty() && this.notifiedFluidOutputList.isEmpty()) {
            return;
        }

        int dim = world.provider.getDimension();
        BlockPos pos = getPos();
        
        // Check if this is a Hatch/Bus rather than a Main Controller
        boolean isHatch = this instanceof gregtech.api.metatileentity.multiblock.IMultiblockPart;

        // 2. Process Item Proxies
        if (!this.notifiedItemOutputList.isEmpty()) {
            List<TileEntityCapabilityProxy> itemProxies = ProxyRegistry.getItemOutProxies(dim, pos);
            IItemHandler myItemHandler = getCoverCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            
            if (itemProxies != null && !itemProxies.isEmpty() && myItemHandler != null) {
                for (TileEntityCapabilityProxy proxy : itemProxies) {
                    for (EnumFacing proxyFacing : EnumFacing.VALUES) {
                        TileEntity adjacentToProxy = world.getTileEntity(proxy.getPos().offset(proxyFacing));
                        if (adjacentToProxy == null || adjacentToProxy instanceof TileEntityCapabilityProxy) continue;

                        IItemHandler targetHandler = adjacentToProxy.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, proxyFacing.getOpposite());
                        if (targetHandler != null) {
                            moveInventoryItems(myItemHandler, targetHandler); // Push directly into ME Interface
                        }
                    }
                }
            }
            
            // Deadlock Prevention & Sleep Logic
            // We only clear the hatch's notification list if the proxy successfully emptied it.
            // If the AE2 Interface is full, the list is NOT cleared, causing it to retry next tick.
            if (isHatch && myItemHandler != null) {
                boolean hasItems = false;
                for (int i = 0; i < myItemHandler.getSlots(); i++) {
                    if (!myItemHandler.getStackInSlot(i).isEmpty()) {
                        hasItems = true; break;
                    }
                }
                if (!hasItems) this.notifiedItemOutputList.clear();
            }
        }

        // 3. Process Fluid Proxies
        if (!this.notifiedFluidOutputList.isEmpty()) {
            List<TileEntityCapabilityProxy> fluidProxies = ProxyRegistry.getFluidOutProxies(dim, pos);
            IFluidHandler myFluidHandler = getCoverCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
            
            if (fluidProxies != null && !fluidProxies.isEmpty() && myFluidHandler != null) {
                for (TileEntityCapabilityProxy proxy : fluidProxies) {
                    for (EnumFacing proxyFacing : EnumFacing.VALUES) {
                        TileEntity adjacentToProxy = world.getTileEntity(proxy.getPos().offset(proxyFacing));
                        if (adjacentToProxy == null || adjacentToProxy instanceof TileEntityCapabilityProxy) continue;

                        IFluidHandler targetHandler = adjacentToProxy.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, proxyFacing.getOpposite());
                        if (targetHandler != null) {
                            gregtech.api.util.GTFluidUtils.transferFluids(myFluidHandler, targetHandler, Integer.MAX_VALUE);
                        }
                    }
                }
            }
            
            // Deadlock Prevention & Sleep Logic
            if (isHatch && myFluidHandler != null) {
                boolean hasFluids = false;
                for (IFluidTankProperties prop : myFluidHandler.getTankProperties()) {
                    FluidStack fs = prop.getContents();
                    if (fs != null && fs.amount > 0) {
                        hasFluids = true; break;
                    }
                }
                if (!hasFluids) this.notifiedFluidOutputList.clear();
            }
        }
    }
}