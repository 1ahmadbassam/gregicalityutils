package top.ahmadb.gregicalityutils.mixin.gregtech;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.AbstractRecipeLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractRecipeLogic.class)
public abstract class MixinAbstractRecipeLogic {

    @Shadow(remap = false)
    protected boolean allowOverclocking;
    
    @Shadow(remap = false)
    protected abstract int getOverclockingTier(long voltage);

    /**
     * @author ahmadb
     * @reason Implements the ULV overclock cap fix while maintaining the original 
     *         method signature for gregicadditions compatibility.
     */
    @Overwrite(remap = false)
    protected int[] calculateOverclock(int EUt, long voltage, int duration) {
        if (!this.allowOverclocking) {
            return new int[]{EUt, duration};
        }

        boolean negativeEU = EUt < 0;
        int tier = this.getOverclockingTier(voltage);
        if (GTValues.V[tier] <= (long) EUt || tier == 0) {
            return new int[]{EUt, duration};
        }

        if (negativeEU) {
            EUt = -EUt;
        }

        // Logic for recipes at or below 16 EU/t
        if (EUt <= 16) {
            int multiplier = EUt <= 8 ? tier : tier - 1;
            int speedCap = 31 - Integer.numberOfLeadingZeros(duration);
            if (multiplier > speedCap) {
                multiplier = speedCap;
            }
            EUt *= (1 << (2 * multiplier));
            duration /= (1 << multiplier);
        } else {
            // Logic for standard recipes above 16 EU/t
            int speedCap = (int) (Math.log(duration) / Math.log(2.8));
            
            // Replicate Recipe.getBaseTier() exactly to avoid rounding errors
            int baseTier = 0;
            while (EUt > GTValues.V[baseTier]) {
                baseTier++;
            }
            
            int dt = tier - baseTier;
            if (dt > speedCap) {
                dt = speedCap;
            }
            
            if (dt > 0) {
                EUt *= (int) Math.pow(4.0, dt);
                // Remove the premature (int) cast. Java will divide as a double 
                // and correctly truncate the final result down to an int automatically.
                duration /= Math.pow(2.8, dt);
            }
        }

        return new int[]{negativeEU ? -EUt : EUt, duration};
    }
}