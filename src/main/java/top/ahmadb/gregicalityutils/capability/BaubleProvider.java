package top.ahmadb.gregicalityutils.capability;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import gregtech.api.items.armor.ArmorMetaItem;
import gregtech.api.items.armor.IArmorLogic;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import top.ahmadb.gregicalityutils.mixin.gregtech.IArmorMetaItemAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BaubleProvider implements ICapabilityProvider, IBauble {

    private final ItemStack itemStack;
    private final BaubleType baubleType;

    public BaubleProvider(ItemStack itemStack, BaubleType baubleType) {
        this.itemStack = itemStack;
        this.baubleType = baubleType;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE) {
            return BaublesCapabilities.CAPABILITY_ITEM_BAUBLE.cast(this);
        }
        return null;
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return this.baubleType;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer && itemstack.getItem() instanceof ArmorMetaItem) {
            ArmorMetaItem<?> armorItem = (ArmorMetaItem<?>) itemstack.getItem();
            IArmorMetaItemAccessor accessor = (IArmorMetaItemAccessor) armorItem;
            IArmorLogic logic = accessor.gu$getArmorLogic(itemstack);
            
            if (logic != null) {
                // Capture the NBT before the armor tick runs
                NBTTagCompound oldNbt = itemstack.hasTagCompound() ? itemstack.getTagCompound().copy() : null;
                
                logic.onArmorTick(player.world, (EntityPlayer) player, itemstack);
                
                // If the Jetpack consumed energy/fluid or toggled hover mode, force Baubles to sync to the client
                NBTTagCompound newNbt = itemstack.getTagCompound();
                if (!player.world.isRemote && ((oldNbt == null && newNbt != null) || (oldNbt != null && !oldNbt.equals(newNbt)))) {
                    IBaublesItemHandler handler = BaublesApi.getBaublesHandler((EntityPlayer) player);
                    if (handler != null) {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            if (handler.getStackInSlot(i) == itemstack) {
                                handler.setChanged(i, true); // Triggers the Baubles network sync packet
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}