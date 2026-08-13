package top.ahmadb.gregicalityutils;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import top.ahmadb.gregicalityutils.ae2.ISmartBlockingDuality;

public class PacketToggleSmartBlocking implements IMessage {
    private BlockPos pos;
    private EnumFacing side;

    public PacketToggleSmartBlocking() {} 

    public PacketToggleSmartBlocking(BlockPos pos, EnumFacing side) {
        this.pos = pos;
        this.side = side;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        int sideIndex = buf.readInt();
        this.side = sideIndex >= 0 ? EnumFacing.values()[sideIndex] : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeInt(this.side != null ? this.side.ordinal() : -1);
    }

    public static class Handler implements IMessageHandler<PacketToggleSmartBlocking, IMessage> {
        @Override
        public IMessage onMessage(PacketToggleSmartBlocking message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                DualityInterface duality = null;
                
                // Check if it's a block interface
                if (te instanceof IInterfaceHost) {
                    duality = ((IInterfaceHost) te).getInterfaceDuality();
                } 
                // Check if it's a part interface on a cable bus
                else if (te instanceof IPartHost) {
                    IPart part = ((IPartHost) te).getPart(message.side);
                    if (part instanceof IInterfaceHost) {
                        duality = ((IInterfaceHost) part).getInterfaceDuality();
                    }
                }

                if (duality != null) {
                    ISmartBlockingDuality smartDuality = (ISmartBlockingDuality) duality;
                    boolean newState = !smartDuality.gcu$isSmartBlocking();
                    
                    smartDuality.gcu$setSmartBlocking(newState);
                    duality.saveChanges(); // Trigger NBT save to persist state
                    
                    String stateStr = newState ? "\u00A7aSmart Blocking" : "\u00A7cRegular Blocking";
                    player.sendMessage(new TextComponentString("Interface configured to: " + stateStr));
                } else {
                    player.sendMessage(new TextComponentString("\u00A7cTarget is not an AE2 Interface."));
                }
            });
            return null;
        }
    }
}