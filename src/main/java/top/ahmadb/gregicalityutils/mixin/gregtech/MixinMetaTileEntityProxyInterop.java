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

    @Inject(method = "update", at = @At("TAIL"), remap = false)
    private void interceptUpdateForProxies(CallbackInfo ci) {
        World world = getWorld();
        if (world == null || world.isRemote) return;

        int dim = world.provider.getDimension();
        BlockPos pos = getPos();

        // 1. Process Item Proxies
        // We do a direct O(1) hash lookup. This completely bypasses the bug in Nomifactory's Item Buses.
        List<TileEntityCapabilityProxy> itemProxies = ProxyRegistry.getItemOutProxies(dim, pos);
        if (itemProxies != null && !itemProxies.isEmpty()) {
            IItemHandler myItemHandler = getCoverCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (myItemHandler != null) {
                // Fast check to ensure we actually have items before querying ME interface capabilities
                boolean hasItems = false;
                for (int i = 0; i < myItemHandler.getSlots(); i++) {
                    if (!myItemHandler.getStackInSlot(i).isEmpty()) {
                        hasItems = true; break;
                    }
                }
                if (hasItems) {
                    for (TileEntityCapabilityProxy proxy : itemProxies) {
                        if (proxy.isInvalid()) continue; // Safety check for Carry On / block breaking
                        for (EnumFacing proxyFacing : EnumFacing.VALUES) {
                            TileEntity adjacentToProxy = world.getTileEntity(proxy.getPos().offset(proxyFacing));
                            if (adjacentToProxy == null || adjacentToProxy instanceof TileEntityCapabilityProxy) continue;

                            IItemHandler targetHandler = adjacentToProxy.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, proxyFacing.getOpposite());
                            if (targetHandler != null) {
                                moveInventoryItems(myItemHandler, targetHandler);
                            }
                        }
                    }
                }
            }
        }

        // 2. Process Fluid Proxies
        List<TileEntityCapabilityProxy> fluidProxies = ProxyRegistry.getFluidOutProxies(dim, pos);
        if (fluidProxies != null && !fluidProxies.isEmpty()) {
            IFluidHandler myFluidHandler = getCoverCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
            if (myFluidHandler != null) {
                boolean hasFluids = false;
                for (IFluidTankProperties prop : myFluidHandler.getTankProperties()) {
                    FluidStack fs = prop.getContents();
                    if (fs != null && fs.amount > 0) {
                        hasFluids = true; break;
                    }
                }
                if (hasFluids) {
                    for (TileEntityCapabilityProxy proxy : fluidProxies) {
                        if (proxy.isInvalid()) continue; // Safety check for Carry On / block breaking
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
            }
        }
    }
}