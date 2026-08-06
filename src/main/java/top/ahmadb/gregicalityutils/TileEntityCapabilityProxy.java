package top.ahmadb.gregicalityutils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileEntityCapabilityProxy extends TileEntity {

    public BlockPos itemIn, fluidIn, itemOut, fluidOut;
    private final ProxyItemHandler itemHandler = new ProxyItemHandler();
    private final ProxyFluidHandler fluidHandler = new ProxyFluidHandler();

    @Override
    public void onLoad() {
        super.onLoad();
        ProxyRegistry.register(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        ProxyRegistry.unregister(this);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        ProxyRegistry.unregister(this);
    }

    // Called by the Packet handler when GUI is saved
    public void updateCoordinates(BlockPos ii, BlockPos fi, BlockPos io, BlockPos fo) {
        ProxyRegistry.unregister(this);
        this.itemIn = ii;
        this.fluidIn = fi;
        this.itemOut = io;
        this.fluidOut = fo;
        ProxyRegistry.register(this);
        markDirty();
        if (world != null) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (itemIn != null) compound.setLong("ItemIn", itemIn.toLong());
        compound.setBoolean("HasItemIn", itemIn != null);

        if (fluidIn != null) compound.setLong("FluidIn", fluidIn.toLong());
        compound.setBoolean("HasFluidIn", fluidIn != null);

        if (itemOut != null) compound.setLong("ItemOut", itemOut.toLong());
        compound.setBoolean("HasItemOut", itemOut != null);

        if (fluidOut != null) compound.setLong("FluidOut", fluidOut.toLong());
        compound.setBoolean("HasFluidOut", fluidOut != null);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        itemIn = compound.getBoolean("HasItemIn") ? BlockPos.fromLong(compound.getLong("ItemIn")) : null;
        fluidIn = compound.getBoolean("HasFluidIn") ? BlockPos.fromLong(compound.getLong("FluidIn")) : null;
        itemOut = compound.getBoolean("HasItemOut") ? BlockPos.fromLong(compound.getLong("ItemOut")) : null;
        fluidOut = compound.getBoolean("HasFluidOut") ? BlockPos.fromLong(compound.getLong("FluidOut")) : null;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, writeToNBT(new NBTTagCompound()));
    }
    
    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandler);
        return super.getCapability(capability, facing);
    }

    private <T> T getCapFromPos(BlockPos targetPos, Capability<T> cap) {
        if (targetPos == null || world == null) return null;
        TileEntity te = world.getTileEntity(targetPos);
        if (te == null || te instanceof TileEntityCapabilityProxy) return null;
        if (te.hasCapability(cap, null)) return te.getCapability(cap, null);
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (te.hasCapability(cap, facing)) return te.getCapability(cap, facing);
        }
        return null;
    }

    private class ProxyItemHandler implements IItemHandler {
        private IItemHandler getIn() { return getCapFromPos(itemIn, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY); }
        private IItemHandler getOut() { return getCapFromPos(itemOut, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY); }
        @Override public int getSlots() { int s = 0; if (getIn() != null) s += getIn().getSlots(); if (getOut() != null) s += getOut().getSlots(); return Math.max(1, s); }
        @Override public ItemStack getStackInSlot(int slot) { IItemHandler in = getIn(), out = getOut(); int inSlots = in != null ? in.getSlots() : 0; if (slot < inSlots) return in.getStackInSlot(slot); if (out != null && slot - inSlots < out.getSlots()) return out.getStackInSlot(slot - inSlots); return ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { IItemHandler in = getIn(); int inSlots = in != null ? in.getSlots() : 0; if (slot < inSlots) return in.insertItem(slot, stack, simulate); return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { IItemHandler in = getIn(), out = getOut(); int inSlots = in != null ? in.getSlots() : 0; if (slot >= inSlots && out != null && slot - inSlots < out.getSlots()) return out.extractItem(slot - inSlots, amount, simulate); return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { IItemHandler in = getIn(), out = getOut(); int inSlots = in != null ? in.getSlots() : 0; if (slot < inSlots) return in.getSlotLimit(slot); if (out != null && slot - inSlots < out.getSlots()) return out.getSlotLimit(slot - inSlots); return 64; }
    }

    private class ProxyFluidHandler implements IFluidHandler {
        private IFluidHandler getIn() { return getCapFromPos(fluidIn, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY); }
        private IFluidHandler getOut() { return getCapFromPos(fluidOut, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY); }
        @Override public IFluidTankProperties[] getTankProperties() { List<IFluidTankProperties> props = new ArrayList<>(); if (getIn() != null) Collections.addAll(props, getIn().getTankProperties()); if (getOut() != null) Collections.addAll(props, getOut().getTankProperties()); return props.toArray(new IFluidTankProperties[0]); }
        @Override public int fill(FluidStack resource, boolean doFill) { return getIn() != null ? getIn().fill(resource, doFill) : 0; }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) { return getOut() != null ? getOut().drain(resource, doDrain) : null; }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return getOut() != null ? getOut().drain(maxDrain, doDrain) : null; }
    }
}