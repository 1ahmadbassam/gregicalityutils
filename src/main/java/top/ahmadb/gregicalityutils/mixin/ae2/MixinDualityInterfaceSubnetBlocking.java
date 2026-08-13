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
        method = "isBusy",
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
        method = "isBusy",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/storage/data/IItemList;isEmpty()Z",
            remap = false
        )
    )
    private boolean gcu$redirectIsEmpty(IItemList<IAEItemStack> instance) {
        IStorageMonitorable sm = GCU_CURRENT_SM.get();
        GCU_CURRENT_SM.remove();

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

        return true;
    }
}
