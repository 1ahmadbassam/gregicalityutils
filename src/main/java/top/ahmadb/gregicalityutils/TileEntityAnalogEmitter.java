package top.ahmadb.gregicalityutils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;

public class TileEntityAnalogEmitter extends TileEntity {
    private int signalLevel = 1;
    private boolean isPowered = false;

    public int getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(int level) {
        this.signalLevel = MathHelper.clamp(level, 1, 15);
        this.markDirty();
        
        if (this.world != null && !this.world.isRemote) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyNeighborsOfStateChange(this.pos, state.getBlock(), false);
        }
    }

    public boolean isPowered() {
        return isPowered;
    }

    public void setPowered(boolean powered) {
        if (this.isPowered != powered) {
            this.isPowered = powered;
            this.markDirty();
            
            // When power state changes, force the block to update its outputs
            if (this.world != null && !this.world.isRemote) {
                IBlockState state = this.world.getBlockState(this.pos);
                this.world.notifyNeighborsOfStateChange(this.pos, state.getBlock(), false);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("SignalLevel", this.signalLevel);
        compound.setBoolean("IsPowered", this.isPowered);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.signalLevel = compound.getInteger("SignalLevel");
        this.isPowered = compound.getBoolean("IsPowered");
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 3, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }
}