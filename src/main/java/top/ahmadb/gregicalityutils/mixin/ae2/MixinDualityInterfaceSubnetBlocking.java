package top.ahmadb.gregicalityutils.mixin.ae2;

import appeng.api.AEApi;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.IStorageMonitorableAccessor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.helpers.DualityInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DualityInterface.class, remap = false)
public class MixinDualityInterfaceSubnetBlocking {

    @Unique
    private static final ThreadLocal<IStorageMonitorable> GCU_CURRENT_SM = new ThreadLocal<>();

    @Redirect(
        method = {"isBusy", "pushPattern"},
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/storage/IStorageMonitorableAccessor;getInventory(Lappeng/api/networking/security/IActionSource;)Lappeng/api/storage/IStorageMonitorable;",
            remap = false
        )
    )
    private IStorageMonitorable gcu$captureSM(IStorageMonitorableAccessor instance, IActionSource src) {
        IStorageMonitorable sm = instance.getInventory(src);
        GCU_CURRENT_SM.set(sm);
        return sm;
    }

    @Redirect(
        method = {"isBusy", "pushPattern"},
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/storage/data/IItemList;isEmpty()Z",
            remap = false
        )
    )
    private boolean gcu$redirectIsEmpty(IItemList<IAEItemStack> instance) {
        IStorageMonitorable sm = GCU_CURRENT_SM.get();
        GCU_CURRENT_SM.remove();

        top.ahmadb.gregicalityutils.ae2.ISmartBlockingDuality smartDuality = (top.ahmadb.gregicalityutils.ae2.ISmartBlockingDuality) this;

        // If smart blocking is disabled, or we are in isBusy (which bypassed if smart blocking was enabled)
        if (!smartDuality.gcu$isSmartBlocking() || smartDuality.gcu$getCurrentTable() == null) {
            // 1. Original item check
            if (!instance.isEmpty()) {
                return false; // not empty, block it
            }

            // 2. Check fluids if available
            if (sm != null) {
                try {
                    IMEMonitor<IAEFluidStack> fluidInv = sm.getInventory(AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
                    if (fluidInv != null) {
                        IItemList<IAEFluidStack> fluidList = fluidInv.getStorageList();
                        if (fluidList != null && !fluidList.isEmpty()) {
                            return false; // Found fluid, so it's NOT empty
                        }
                    }
                } catch (Exception e) {
                    // Ignore if fluid channel is unavailable
                }
            }

            return true; // it's empty
        }

        // Smart blocking logic for subnet (only runs in pushPattern because isBusy returns early if smart blocking is ON)
        net.minecraft.inventory.InventoryCrafting table = smartDuality.gcu$getCurrentTable();

        // 1. Check items
        for (IAEItemStack aeStack : instance) {
            net.minecraft.item.ItemStack inMachine = aeStack.createItemStack();
            boolean found = false;
            for (int i = 0; i < table.getSizeInventory(); i++) {
                net.minecraft.item.ItemStack inTable = table.getStackInSlot(i);
                if (inTable != null && !inTable.isEmpty()) {
                    if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(inMachine, inTable)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return false; // Alien item found, block it
        }

        // 2. Check fluids
        if (sm != null) {
            try {
                IMEMonitor<IAEFluidStack> fluidInv = sm.getInventory(AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
                if (fluidInv != null) {
                    IItemList<IAEFluidStack> fluidList = fluidInv.getStorageList();
                    if (fluidList != null) {
                        for (IAEFluidStack aeFluid : fluidList) {
                            boolean found = false;
                            net.minecraftforge.fluids.FluidStack fluid = aeFluid.getFluidStack();
                            if (fluid != null && fluid.getFluid() != null) {
                                for (int i = 0; i < table.getSizeInventory(); i++) {
                                    net.minecraft.item.ItemStack inTable = table.getStackInSlot(i);
                                    if (inTable != null && !inTable.isEmpty() && inTable.hasTagCompound() && inTable.getItem() != null) {
                                        if (inTable.getItem().getRegistryName().toString().equals("ae2fc:fluid_drop")) {
                                            net.minecraft.nbt.NBTTagCompound tag = inTable.getTagCompound();
                                            if (tag != null && tag.hasKey("Fluid")) {
                                                net.minecraft.nbt.NBTTagCompound fluidTag = tag.getCompoundTag("Fluid");
                                                if (fluidTag.getString("FluidName").equals(fluid.getFluid().getName())) {
                                                    found = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!found) return false; // Alien fluid found
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return true; // All items and fluids matched recipe!
    }
}
