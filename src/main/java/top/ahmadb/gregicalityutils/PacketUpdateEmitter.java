package top.ahmadb.gregicalityutils;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateEmitter implements IMessage {
    private BlockPos pos;
    private int level;

    public PacketUpdateEmitter() {} // Required empty constructor

    public PacketUpdateEmitter(BlockPos pos, int level) {
        this.pos = pos;
        this.level = level;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.level = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeInt(this.level);
    }

    public static class Handler implements IMessageHandler<PacketUpdateEmitter, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateEmitter message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = ctx.getServerHandler().player.world.getTileEntity(message.pos);
                if (te instanceof TileEntityAnalogEmitter) {
                    ((TileEntityAnalogEmitter) te).setSignalLevel(message.level);
                }
            });
            return null;
        }
    }
}