package top.ahmadb.gregicalityutils;

import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyRegistry {
    // Dimension ID -> (Target BlockPos -> List of Proxies targeting it)
    private static final Map<Integer, Map<BlockPos, List<TileEntityCapabilityProxy>>> ITEM_OUT = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<BlockPos, List<TileEntityCapabilityProxy>>> FLUID_OUT = new ConcurrentHashMap<>();

    public static void register(TileEntityCapabilityProxy proxy) {
        if (proxy.getWorld() == null || proxy.getWorld().isRemote) return;
        int dim = proxy.getWorld().provider.getDimension();
        
        if (proxy.itemOut != null) {
            ITEM_OUT.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(proxy.itemOut, k -> new ArrayList<>())
                    .add(proxy);
        }
        if (proxy.fluidOut != null) {
            FLUID_OUT.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(proxy.fluidOut, k -> new ArrayList<>())
                    .add(proxy);
        }
    }

    public static void unregister(TileEntityCapabilityProxy proxy) {
        if (proxy.getWorld() == null || proxy.getWorld().isRemote) return;
        int dim = proxy.getWorld().provider.getDimension();
        
        if (ITEM_OUT.containsKey(dim)) {
            for (List<TileEntityCapabilityProxy> list : ITEM_OUT.get(dim).values()) {
                list.remove(proxy);
            }
        }
        if (FLUID_OUT.containsKey(dim)) {
            for (List<TileEntityCapabilityProxy> list : FLUID_OUT.get(dim).values()) {
                list.remove(proxy);
            }
        }
    }

    public static List<TileEntityCapabilityProxy> getItemOutProxies(int dimension, BlockPos target) {
        Map<BlockPos, List<TileEntityCapabilityProxy>> dimMap = ITEM_OUT.get(dimension);
        return dimMap != null ? dimMap.get(target) : null;
    }

    public static List<TileEntityCapabilityProxy> getFluidOutProxies(int dimension, BlockPos target) {
        Map<BlockPos, List<TileEntityCapabilityProxy>> dimMap = FLUID_OUT.get(dimension);
        return dimMap != null ? dimMap.get(target) : null;
    }
}