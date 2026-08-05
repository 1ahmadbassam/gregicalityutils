package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.covers.CoverDigitalInterface;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.IWorkable;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.ICoverable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CoverDigitalInterface.class, remap = false)
public abstract class MixinCoverDigitalInterface extends CoverBehavior {

    public MixinCoverDigitalInterface(ICoverable coverHolder, EnumFacing attachedSide) {
        super(coverHolder, attachedSide);
    }

    @Shadow private CoverDigitalInterface.MODE mode;
    @Shadow private int[] proxyMode;
    @Shadow private FluidTankProperties[] fluids;
    @Shadow private ItemStack[] items;
    @Shadow private int maxItemCapability;
    @Shadow private long energyStored;
    @Shadow private long energyCapability;
    @Shadow private long energyInputPerDur;
    @Shadow private long energyOutputPerDur;
    @Shadow private List<Long> inputEnergyList;
    @Shadow private List<Long> outputEnergyList;
    @Shadow private int progress;
    @Shadow private int maxProgress;
    @Shadow private boolean isActive;
    @Shadow private boolean isWorkingEnable;
    @Shadow private int slot;

    @Shadow public abstract IFluidHandler getFluidCapability();
    @Shadow public abstract IItemHandler getItemCapability();
    @Shadow public abstract IEnergyContainer getEnergyCapability();
    @Shadow public abstract IWorkable getMachineCapability();

    /**
     * @author ahmadb
     * @reason Fix graph data accumulation bug caused by mutating server state inside a network packet lambda.
     */
    @Overwrite
    private void syncAllInfo() {
        if (mode == CoverDigitalInterface.MODE.FLUID || (mode == CoverDigitalInterface.MODE.PROXY && proxyMode[0] > 0)) {
            boolean syncFlag = false;
            IFluidHandler fluidHandler = this.getFluidCapability();
            if (fluidHandler != null) {
                IFluidTankProperties[] fluidTankProperties = fluidHandler.getTankProperties();
                if (fluidTankProperties.length != fluids.length) {
                    fluids = new FluidTankProperties[fluidTankProperties.length];
                    syncFlag = true;
                }
                List<Integer> toUpdate = new ArrayList<>();
                for (int i = 0; i < fluidTankProperties.length; i++) {
                    FluidStack content = fluidTankProperties[i].getContents();
                    if (fluids[i] == null || (content == null && fluids[i].getContents() != null) || (content != null && fluids[i].getContents() == null) ||
                            fluidTankProperties[i].getCapacity() != fluids[i].getCapacity() ||
                            fluidTankProperties[i].canDrain() != fluids[i].canDrain() ||
                            fluidTankProperties[i].canFill() != fluids[i].canFill()) {
                        syncFlag = true;
                        fluids[i] = new FluidTankProperties(content, fluidTankProperties[i].getCapacity(), fluidTankProperties[i].canFill(), fluidTankProperties[i].canDrain());
                        toUpdate.add(i);
                    } else if(content != null && (content.amount != fluids[i].getContents().amount || !content.isFluidEqual(fluids[i].getContents()))) {
                        syncFlag = true;
                        fluids[i] = new FluidTankProperties(content, fluidTankProperties[i].getCapacity(), fluidTankProperties[i].canFill(), fluidTankProperties[i].canDrain());
                        toUpdate.add(i);
                    }
                }
                if (syncFlag) writeUpdateData(2, packetBuffer->{
                    packetBuffer.writeVarInt(fluids.length);
                    packetBuffer.writeVarInt(toUpdate.size());
                    for (Integer index : toUpdate) {
                        packetBuffer.writeVarInt(index);
                        NBTTagCompound nbt = new NBTTagCompound();
                        nbt.setInteger("Capacity", fluids[index].getCapacity());
                        if (fluids[index].getContents() != null) {
                            fluids[index].getContents().writeToNBT(nbt);
                        }
                        packetBuffer.writeCompoundTag(nbt);
                    }
                });
            }
        }
        
        if (mode == CoverDigitalInterface.MODE.ITEM || (mode == CoverDigitalInterface.MODE.PROXY && proxyMode[1] > 0)) {
            boolean syncFlag = false;
            IItemHandler itemHandler = this.getItemCapability();
            if(itemHandler != null) {
                int size = itemHandler.getSlots();
                if (this.slot < size) {
                    int maxStoredItems = itemHandler.getSlotLimit(this.slot);
                    if (maxStoredItems != maxItemCapability) {
                        maxItemCapability = maxStoredItems;
                        syncFlag = true;
                    }
                }
                List<Integer> toUpdate = new ArrayList<>();
                if (items.length != size) {
                    items = new ItemStack[size];
                    syncFlag = true;
                }
                for (int i = 0; i < size; i++) {
                    if (items[i] == null) {
                        items[i] = ItemStack.EMPTY;
                    }
                    ItemStack content = itemHandler.getStackInSlot(i);
                    if (!ItemStack.areItemStacksEqual(items[i], content)) {
                        syncFlag = true;
                        items[i] = content.copy();
                        toUpdate.add(i);
                    }
                }
                if (syncFlag) writeUpdateData(3, packetBuffer -> {
                    packetBuffer.writeVarInt(maxItemCapability);
                    packetBuffer.writeVarInt(items.length);
                    packetBuffer.writeVarInt(toUpdate.size());
                    for (Integer index : toUpdate) {
                        packetBuffer.writeVarInt(index);
                        packetBuffer.writeCompoundTag(CoverDigitalInterface.fixItemStackSer(items[index]));
                    }
                });
            }
        }
        
        if (this.mode == CoverDigitalInterface.MODE.ENERGY || (mode == CoverDigitalInterface.MODE.PROXY && proxyMode[2] > 0)) {
            IEnergyContainer energyContainer = this.getEnergyCapability();
            if (energyContainer != null) {
                if (energyStored != energyContainer.getEnergyStored() || energyCapability != energyContainer.getEnergyCapacity()) {
                    energyStored = energyContainer.getEnergyStored();
                    energyCapability = energyContainer.getEnergyCapacity();
                    writeUpdateData(4, packetBuffer -> {
                        packetBuffer.writeLong(energyStored);
                        packetBuffer.writeLong(energyCapability);
                    });
                }
                if (this.coverHolder.getTimer() % 20 == 0) { //per second
                    
                    // FIX: Process graph state OUTSIDE the packet lambda so it runs even if no players track the chunk!
                    long in = energyInputPerDur;
                    long out = energyOutputPerDur;
                    
                    inputEnergyList.add(in);
                    outputEnergyList.add(out);
                    if (inputEnergyList.size() > 13) {
                        inputEnergyList.remove(0);
                        outputEnergyList.remove(0);
                    }
                    energyInputPerDur = 0;
                    energyOutputPerDur = 0;

                    writeUpdateData(5, packetBuffer -> {
                        packetBuffer.writeLong(in);
                        packetBuffer.writeLong(out);
                    });
                }
            }
        }
        
        if (this.mode == CoverDigitalInterface.MODE.MACHINE || (mode == CoverDigitalInterface.MODE.PROXY && proxyMode[3] > 0)) {
            IWorkable workable = this.getMachineCapability();
            if (workable != null) {
                int progress = workable.getProgress();
                int maxProgress = workable.getMaxProgress();
                boolean isActive = workable.isActive();
                boolean isWorkingEnable = workable.isWorkingEnabled();
                
                if (isActive != this.isActive || isWorkingEnable != this.isWorkingEnable || this.progress != progress || this.maxProgress != maxProgress) {
                    this.progress = progress;
                    this.maxProgress = maxProgress;
                    this.isWorkingEnable = isWorkingEnable;
                    this.isActive = isActive;
                    writeUpdateData(6, packetBuffer -> {
                        packetBuffer.writeInt(progress);
                        packetBuffer.writeInt(maxProgress);
                        packetBuffer.writeBoolean(isActive);
                        packetBuffer.writeBoolean(isWorkingEnable);
                    });
                }
                if (this.coverHolder.getTimer() % 20 == 0) {
                    IEnergyContainer energyContainer = this.getEnergyCapability();
                    if (energyContainer != null) {
                        if (energyStored != energyContainer.getEnergyStored() || energyCapability != energyContainer.getEnergyCapacity()) {
                            energyStored = energyContainer.getEnergyStored();
                            energyCapability = energyContainer.getEnergyCapacity();
                            writeUpdateData(4, packetBuffer -> {
                                packetBuffer.writeLong(energyStored);
                                packetBuffer.writeLong(energyCapability);
                            });
                        }
                    }
                }
            }
        }
    }
}