package top.ahmadb.gregicalityutils.gui;

import gregtech.api.gui.widgets.PhantomSlotWidget;
import gregtech.api.items.toolitem.ToolMetaItem;
import gregtech.api.util.ItemStackKey;
import gregtech.common.inventory.IItemInfo;
import gregtech.common.metatileentities.storage.CraftingRecipeResolver;
import gregtech.common.metatileentities.storage.MetaTileEntityWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.oredict.OreDictionary;
import top.ahmadb.gregicalityutils.mixin.gregtech.MixinMetaTileEntityWorkbenchAccessor;

public class TrackedPhantomSlotWidget extends PhantomSlotWidget {

    public static final java.util.WeakHashMap<MetaTileEntityWorkbench, Long> ACTIVE_WORKBENCHES = new java.util.WeakHashMap<>();
    private final MetaTileEntityWorkbench workbench;
    private final IItemHandlerModifiable gridHandler;
    private final int slotX;
    private final int slotY;
    private final int slotIndex;

    private int lastAvailable = -1;
    private ItemStack lastActualItem = ItemStack.EMPTY;
    private int clientAvailable = 0; 

    public TrackedPhantomSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition, int yPosition, MetaTileEntityWorkbench workbench) {
        super(itemHandler, slotIndex, xPosition, yPosition);
        this.workbench = workbench;
        this.gridHandler = itemHandler;
        this.slotX = xPosition;
        this.slotY = yPosition;
        this.slotIndex = slotIndex;
    }

    // --- OREDICT & EQUIVALENCE CHECKER ---
    private boolean isItemEquivalentOptimized(ItemStack blueprint, int[] cachedBlueprintIDs, ItemStack target) {
        if (blueprint.isEmpty() || target.isEmpty()) return false;
        
        if (OreDictionary.itemMatches(blueprint, target, false)) return true;

        // Early exit: if the blueprint has no OreDict tags, it cannot match via tags
        if (cachedBlueprintIDs.length == 0) return false;

        // We only fetch the target IDs if a match is actually possible
        int[] targetIDs = OreDictionary.getOreIDs(target);

        for (int id1 : cachedBlueprintIDs) {
            for (int id2 : targetIDs) {
                if (id1 == id2) return true;
            }
        }
        return false;
    }

    // --- MANUAL TOOL SCANNER ---
    private int countInHandler(IItemHandler handler, ItemStack blueprint, boolean isGTTool, boolean isDamageable) {
        if (handler == null) return 0;
        int count = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (isGTTool) {
                    if (stack.getItem() == blueprint.getItem() && stack.getMetadata() == blueprint.getMetadata()) count += stack.getCount();
                } else if (isDamageable) {
                    if (stack.getItem() == blueprint.getItem()) count += stack.getCount();
                }
            }
        }
        return count;
    }

    private int countToolInNetwork(ItemStack blueprint, boolean isGTTool, boolean isDamageable) {
        int count = 0;
        MixinMetaTileEntityWorkbenchAccessor accessor = (MixinMetaTileEntityWorkbenchAccessor) this.workbench;
        
        count += countInHandler(accessor.getInternalInventory(), blueprint, isGTTool, isDamageable);
        count += countInHandler(accessor.getToolInventory(), blueprint, isGTTool, isDamageable);
        
        World world = this.workbench.getWorld();
        BlockPos pos = this.workbench.getPos();
        
        for (EnumFacing side : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(side));
            if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite())) {
                count += countInHandler(te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()), blueprint, isGTTool, isDamageable);
            }
        }
        return count;
    }

    // --- SERVER SIDE: Detect changes and push to client ---
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (this.workbench != null && !this.workbench.getWorld().isRemote) {
            ACTIVE_WORKBENCHES.put(this.workbench, this.workbench.getWorld().getTotalWorldTime());
            CraftingRecipeResolver resolver = ((MixinMetaTileEntityWorkbenchAccessor) this.workbench).invokeGetRecipeResolver();

            if (resolver != null) {
                ItemStack stackInSlot = this.gridHandler.getStackInSlot(this.slotIndex);
                int currentAvailable = 0;
                ItemStack actualItemInNetwork = ItemStack.EMPTY;

                if (!stackInSlot.isEmpty()) {
                    // CACHE 1: Grab the blueprint IDs once per tick, not per network item!
                    int[] blueprintIDs = OreDictionary.getOreIDs(stackInSlot);

                    for (ItemStackKey storedKey : resolver.getItemSourceList().getStoredItems()) {
                        ItemStack storedStack = storedKey.getItemStack();

                        if (isItemEquivalentOptimized(stackInSlot, blueprintIDs, storedStack)) {
                            IItemInfo info = resolver.getItemSourceList().getItemInfo(storedKey);
                            if (info != null && info.getTotalItemAmount() > 0) {
                                currentAvailable += info.getTotalItemAmount();
                                
                                if (actualItemInNetwork.isEmpty()) {
                                    actualItemInNetwork = storedStack.copy();
                                }
                            }
                        }
                    }
                }

                final int amountToSend = currentAvailable;
                final ItemStack itemToSend = actualItemInNetwork;

                if (amountToSend != this.lastAvailable || !ItemStack.areItemStacksEqual(itemToSend, this.lastActualItem)) {
                    this.lastAvailable = amountToSend;
                    this.lastActualItem = itemToSend.copy();

                    writeUpdateInfo(777, buf -> {
                        buf.writeInt(amountToSend);
                    });
                }
            }
        }
    }

    // --- CLIENT SIDE: Receive UI packet and update cache ---
    @Override
    public void readUpdateInfo(int id, PacketBuffer buffer) {
        if (id == 777) {
            this.clientAvailable = buffer.readInt();
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    // --- CLIENT SIDE: Render visuals ---
    @Override
    public void drawInForeground(int mouseX, int mouseY) {
        super.drawInForeground(mouseX, mouseY);

        ItemStack stackInSlot = this.gridHandler.getStackInSlot(this.slotIndex);

        if (!stackInSlot.isEmpty() && workbench != null) {
            boolean isGTTool = stackInSlot.getItem() instanceof ToolMetaItem;
            boolean isDamageable = stackInSlot.isItemStackDamageable();

            // Calculate Demand using OreDict Equivalence
            int demandUpToThisSlot = 0;
            
            // CACHE: Grab the IDs once per frame instead of inside the loop
            int[] cachedBlueprintIDs = OreDictionary.getOreIDs(stackInSlot);

            for (int i = 0; i <= this.slotIndex; i++) {
                ItemStack gridStack = this.gridHandler.getStackInSlot(i);
                if (!gridStack.isEmpty()) {
                    if (isGTTool) {
                        if (gridStack.getItem() == stackInSlot.getItem() && gridStack.getMetadata() == stackInSlot.getMetadata()) demandUpToThisSlot++;
                    } else if (isDamageable) {
                        if (gridStack.getItem() == stackInSlot.getItem()) demandUpToThisSlot++;
                    } else {
                        // USE THE NEW OPTIMIZED METHOD HERE
                        if (isItemEquivalentOptimized(stackInSlot, cachedBlueprintIDs, gridStack)) demandUpToThisSlot++;
                    }
                }
            }

            // If the supply ran out before fulfilling this slot, tint it red.
            if (this.clientAvailable < demandUpToThisSlot) {
                GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                int renderX = this.slotX;
                int renderY = this.slotY;
                
                if (screen != null) {
                    int guiLeft = (screen.width - 176) / 2;
                    int guiTop = (screen.height - 221) / 2;
                    renderX += guiLeft;
                    renderY += guiTop;
                }

                // Render Tint
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.colorMask(true, true, true, false);
                Gui.drawRect(renderX, renderY, renderX + 17, renderY + 17, 0x66FF0000);
                GlStateManager.colorMask(true, true, true, true);
                
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
            }
        }
    }
}