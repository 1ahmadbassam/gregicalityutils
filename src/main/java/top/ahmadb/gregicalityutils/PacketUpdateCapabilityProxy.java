package top.ahmadb.gregicalityutils;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateCapabilityProxy implements IMessage {
    private BlockPos tilePos, itemIn, fluidIn, itemOut, fluidOut;
    private boolean hItemIn, hFluidIn, hItemOut, hFluidOut;

    public PacketUpdateCapabilityProxy() {}

    public PacketUpdateCapabilityProxy(BlockPos tilePos, BlockPos itemIn, BlockPos fluidIn, BlockPos itemOut, BlockPos fluidOut) {
        this.tilePos = tilePos;
        this.itemIn = itemIn; this.hItemIn = itemIn != null;
        this.fluidIn = fluidIn; this.hFluidIn = fluidIn != null;
        this.itemOut = itemOut; this.hItemOut = itemOut != null;
        this.fluidOut = fluidOut; this.hFluidOut = fluidOut != null;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tilePos = BlockPos.fromLong(buf.readLong());
        hItemIn = buf.readBoolean(); if (hItemIn) itemIn = BlockPos.fromLong(buf.readLong());
        hFluidIn = buf.readBoolean(); if (hFluidIn) fluidIn = BlockPos.fromLong(buf.readLong());
        hItemOut = buf.readBoolean(); if (hItemOut) itemOut = BlockPos.fromLong(buf.readLong());
        hFluidOut = buf.readBoolean(); if (hFluidOut) fluidOut = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(tilePos.toLong());
        buf.writeBoolean(hItemIn); if (hItemIn) buf.writeLong(itemIn.toLong());
        buf.writeBoolean(hFluidIn); if (hFluidIn) buf.writeLong(fluidIn.toLong());
        buf.writeBoolean(hItemOut); if (hItemOut) buf.writeLong(itemOut.toLong());
        buf.writeBoolean(hFluidOut); if (hFluidOut) buf.writeLong(fluidOut.toLong());
    }

    public static class Handler implements IMessageHandler<PacketUpdateCapabilityProxy, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateCapabilityProxy message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.tilePos);
                if (te instanceof TileEntityCapabilityProxy) {
                    TileEntityCapabilityProxy proxy = (TileEntityCapabilityProxy) te;
                    proxy.updateCoordinates(message.itemIn, message.fluidIn, message.itemOut, message.fluidOut);
                }
            });
            return null;
        }
    }
}