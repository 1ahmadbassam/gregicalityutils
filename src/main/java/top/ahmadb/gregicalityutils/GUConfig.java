package top.ahmadb.gregicalityutils;

import net.minecraftforge.common.config.Config;

@Config(modid = "gregicalityutils")
public class GUConfig {

    @Config.Comment("How many ticks between simulated twerks (20 ticks = 1 second)")
    @Config.RangeInt(min = 1, max = 1200)
    public static int twerkRate = 10;

    @Config.Comment("Radius of the twerk effect (e.g. 3 means a 7x7x3 area)")
    @Config.RangeInt(min = 1, max = 10)
    public static int twerkRadius = 3;

    @Config.Comment("How many ticks between sheep stimulation attempts (20 ticks = 1 second)")
    @Config.RangeInt(min = 1, max = 1200)
    public static int sheepStimulatorRate = 20;
}