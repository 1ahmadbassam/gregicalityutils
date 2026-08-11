package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.machines.multi.simple.TileEntityChemicalPlant;
import gregtech.api.multiblock.PatternMatchContext;
import gregtech.common.blocks.BlockWireCoil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ahmadb.gregicalityutils.gregicadditions.IChemicalPlantCoilBonus;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = TileEntityChemicalPlant.class, remap = false)
public abstract class MixinTileEntityChemicalPlant extends gregicadditions.machines.multi.simple.MultiRecipeMapMultiblockController implements IChemicalPlantCoilBonus {

    public MixinTileEntityChemicalPlant(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, null, 0, 0, 0, 0, null);
    }

    // Create our new speed bonus variable
    @Unique
    private int gu$speedBonus;

    @Override
    public int gu$getSpeedBonus() {
        return this.gu$speedBonus;
    }

    /**
     * @author ahmadb
     * @reason Add missing coil tier calculation to the Chemical Plant.
     */
    @Inject(method = "formStructure", at = @At("TAIL"))
    private void gu$formStructureCoilBonus(PatternMatchContext context, CallbackInfo ci) {
        // The Chemical Plant uses TileEntityLargeChemicalReactor.heatingCoilPredicate(),
        // which saves the temperature under "reactorCoilTemperature".
        int temperature = context.getOrDefault("reactorCoilTemperature", 0);
        
        int tier = 0;
        for (BlockWireCoil.CoilType type : BlockWireCoil.CoilType.values()) {
            if (type.getCoilTemperature() == temperature) {
                tier = type.ordinal();
                break;
            }
        }
        if (tier == 0 && temperature > 1800) {
            tier = Math.max(0, (temperature - 1800) / 900);
        }
        
        // Apply a 5% speed bonus per tier, capped at 95%
        this.gu$speedBonus = Math.min(95, tier * 5);
    }

    // Append to the item tooltip
    @Inject(method = "addInformation", at = @At("TAIL"))
    public void gu$addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced, CallbackInfo ci) {
        tooltip.add("Each coil tier above Cupronickel provides an additional §c5%§7 speed bonus.");
    }

    // Reset the bonus if the machine breaks
    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.gu$speedBonus = 0;
    }

    // Render the speed bonus in the machine's GUI
    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        if (this.isStructureFormed() && !this.hasProblems() && this.gu$speedBonus > 0) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.universal.speed_increase", this.gu$speedBonus).setStyle(new Style().setColor(TextFormatting.AQUA)));
        }
    }

    // --- Save and Sync the Bonus ---
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("gu$speedBonus", this.gu$speedBonus);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.gu$speedBonus = data.getInteger("gu$speedBonus");
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeInt(this.gu$speedBonus);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.gu$speedBonus = buf.readInt();
    }
}