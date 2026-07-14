package top.ahmadb.gregicalityutils.mixin.extrautils2;

import com.google.common.collect.Multimap;
import com.rwtema.extrautils2.entity.chunkdata.EntityChunkData;
import com.rwtema.extrautils2.interblock.FlatTransferNodeHandler;
import com.rwtema.extrautils2.itemhandler.SingleStackHandlerFilter;
import com.rwtema.extrautils2.utils.CapGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;

@Mixin(value = FlatTransferNodeHandler.class, remap = false)
public abstract class MixinFlatTransferNodes {

    @Overwrite
    public boolean onUpdate(Chunk chunk, Multimap<BlockPos, FlatTransferNodeHandler.FlatTransferNode> entries) {
        if (entries.isEmpty()) return true;

        Iterator<FlatTransferNodeHandler.FlatTransferNode> iterator = entries.values().iterator();

        while (iterator.hasNext()) {
            FlatTransferNodeHandler.FlatTransferNode node = iterator.next();
            try {
                if (!node.isDead && node.process(chunk.getWorld())) {
                    node.dropItemStack(chunk.getWorld());
                    EntityChunkData.markChunkDirty(chunk);
                    node.isDead = true;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                node.isDead = true;
            }

            if (node.isDead) {
                iterator.remove();
            }
        }

        return entries.isEmpty();
    }
}

@Mixin(value = FlatTransferNodeHandler.FlatTransferNode.class, remap = false)
abstract class MixinFlatTransferNodeInner {

    @Shadow public BlockPos pos;
    @Shadow public EnumFacing side;
    @Shadow public boolean extract;
    @Shadow public SingleStackHandlerFilter.EitherFilter filter;
    @Shadow public FlatTransferNodeHandler.FlatTransferNode.Type type;

    @Unique private int xu2opt$tickOffset = -1;
    @Unique private BlockPos xu2opt$neighbourPos = null;
    @Unique private int xu2opt$lastInputSlot = 0;
    @Unique private int xu2opt$lastOutputSlot = 0;
    @Unique private long xu2opt$sleepUntil = 0;

    @Overwrite
    public boolean process(World world) {
        long time = world.getTotalWorldTime();

        if (this.xu2opt$tickOffset == -1) {
            this.xu2opt$tickOffset = Math.abs(this.pos.hashCode()) % 20;
            this.xu2opt$neighbourPos = this.pos.offset(this.side);
        }

        if ((time + this.xu2opt$tickOffset) % 20 != 0 || time < this.xu2opt$sleepUntil) {
            return false;
        }

        TileEntity owner = world.getTileEntity(this.pos);
        if (owner == null) return true;

        TileEntity neighbour = world.getTileEntity(this.xu2opt$neighbourPos);
        if (neighbour == null) return false;

        TileEntity input, output;
        EnumFacing dir;

        if (this.extract) {
            input = owner;
            output = neighbour;
            dir = this.side;
        } else {
            input = neighbour;
            output = owner;
            dir = this.side.getOpposite();
        }

        if (this.type == FlatTransferNodeHandler.FlatTransferNode.Type.ITEM) {
            this.xu2opt$processItems(world, input, output, dir);
        } else if (this.type == FlatTransferNodeHandler.FlatTransferNode.Type.FLUIDS) {
            // Updated to call the new Accessor Interface
            FlatTransferNodeHandlerAccessor.getFluidProcessor().process(world, input, output, dir, this.filter);
        }

        return false;
    }

    @Unique
    private void xu2opt$processItems(World world, TileEntity input, TileEntity output, EnumFacing dir) {
        if (!CapGetter.ItemHandler.hasInterface(input, dir) || !CapGetter.ItemHandler.hasInterface(output, dir.getOpposite())) {
            return;
        }

        IItemHandler inputCap = CapGetter.ItemHandler.getInterface(input, dir);
        IItemHandler outputCap = CapGetter.ItemHandler.getInterface(output, dir.getOpposite());

        if (inputCap == null || outputCap == null || inputCap == outputCap) return;

        int inSlots = inputCap.getSlots();
        int outSlots = outputCap.getSlots();
        if (inSlots == 0 || outSlots == 0) return;

        if (this.xu2opt$lastInputSlot >= inSlots) this.xu2opt$lastInputSlot = 0;
        if (this.xu2opt$lastOutputSlot >= outSlots) this.xu2opt$lastOutputSlot = 0;

        boolean anyValidItemFound = false;

        for (int i = 0; i < inSlots; i++) {
            int slotIn = (this.xu2opt$lastInputSlot + i) % inSlots;

            if (inputCap.getStackInSlot(slotIn).isEmpty()) continue;

            ItemStack extractSim = inputCap.extractItem(slotIn, 1, true);
            if (extractSim.isEmpty() || !this.filter.matches(extractSim)) continue;

            anyValidItemFound = true;

            for (int j = 0; j < outSlots; j++) {
                int slotOut = (this.xu2opt$lastOutputSlot + j) % outSlots;

                ItemStack insertSim = outputCap.insertItem(slotOut, extractSim, true);
                if (insertSim.isEmpty()) { 
                    
                    ItemStack extracted = inputCap.extractItem(slotIn, 1, false);
                    if (!extracted.isEmpty()) {
                        ItemStack leftover = outputCap.insertItem(slotOut, extracted, false);
                        
                        if (!leftover.isEmpty()) {
                            leftover = inputCap.insertItem(slotIn, leftover, false);
                            if (!leftover.isEmpty()) {
                                com.rwtema.extrautils2.itemhandler.InventoryHelper.dropItemStack(
                                    world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), leftover
                                );
                            }
                        }
                    }
                    this.xu2opt$lastInputSlot = slotIn;
                    this.xu2opt$lastOutputSlot = slotOut;
                    return; 
                }
            }
        }

        if (anyValidItemFound) {
            this.xu2opt$sleepUntil = world.getTotalWorldTime() + 40;
        }
    }
}