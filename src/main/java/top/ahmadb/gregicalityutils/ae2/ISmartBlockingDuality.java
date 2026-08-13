package top.ahmadb.gregicalityutils.ae2;

import net.minecraft.inventory.InventoryCrafting;

public interface ISmartBlockingDuality {
    boolean gcu$isSmartBlocking();
    void gcu$setSmartBlocking(boolean state);
    InventoryCrafting gcu$getCurrentTable();
}