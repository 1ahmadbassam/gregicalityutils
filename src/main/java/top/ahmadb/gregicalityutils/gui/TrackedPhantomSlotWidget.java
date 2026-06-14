package top.ahmadb.gregicalityutils.gui;

import gregtech.api.gui.widgets.PhantomSlotWidget;
import gregtech.api.util.ItemStackKey;
import gregtech.common.inventory.IItemInfo;
import gregtech.common.metatileentities.storage.CraftingRecipeResolver;
import gregtech.common.metatileentities.storage.MetaTileEntityWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.ahmadb.gregicalityutils.mixin.gregtech.MixinMetaTileEntityWorkbenchAccessor;

public class TrackedPhantomSlotWidget extends PhantomSlotWidget {

    private final MetaTileEntityWorkbench workbench;
    private final IItemHandlerModifiable gridHandler;
    private final int slotX;
    private final int slotY;
    private final int slotIndex;

    // Native UI Sync Variables
    private int lastAvailable = -1;  // Server-side tracking
    private int clientAvailable = 0; // Client-side drawing

    public TrackedPhantomSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition, int yPosition, MetaTileEntityWorkbench workbench) {
        super(itemHandler, slotIndex, xPosition, yPosition);
        this.workbench = workbench;
        this.gridHandler = itemHandler;
        this.slotX = xPosition;
        this.slotY = yPosition;
        this.slotIndex = slotIndex;
    }

    // --- SERVER SIDE: Detect changes and push to client ---
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        
        if (this.workbench != null && !this.workbench.getWorld().isRemote) {
            CraftingRecipeResolver resolver = ((MixinMetaTileEntityWorkbenchAccessor) this.workbench).invokeGetRecipeResolver();
            
            if (resolver != null) {
                ItemStack stackInSlot = this.gridHandler.getStackInSlot(this.slotIndex);
                int currentAvailable = 0;
                
                if (!stackInSlot.isEmpty()) {
                    IItemInfo info = resolver.getItemSourceList().getItemInfo(new ItemStackKey(stackInSlot));
                    currentAvailable = (info != null) ? info.getTotalItemAmount() : 0;
                }
                
                // FIX: Capture the value in a 'final' variable for the Java 8 lambda
                final int amountToSend = currentAvailable;
                
                // If the number changed, let the ModularUI framework handle sending the packet!
                if (amountToSend != this.lastAvailable) {
                    this.lastAvailable = amountToSend;
                    // Widget Update ID 777 uniquely identifies this specific data payload
                    writeUpdateInfo(777, buf -> buf.writeInt(amountToSend)); 
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
            // Calculate Demand (Client already knows the grid contents)
            int totalRequired = 0;
            for (int i = 0; i < this.gridHandler.getSlots(); i++) {
                ItemStack gridStack = this.gridHandler.getStackInSlot(i);
                if (!gridStack.isEmpty() && Item.getIdFromItem(gridStack.getItem()) == Item.getIdFromItem(stackInSlot.getItem()) && gridStack.getMetadata() == stackInSlot.getMetadata()) {
                    totalRequired++;
                }
            }

            // If we lack the items, render the visuals
            if (this.clientAvailable < totalRequired) {
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

                // Render Number
                FontRenderer font = Minecraft.getMinecraft().fontRenderer;
                String text = String.valueOf(this.clientAvailable);

                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, 300);

                int textX = renderX + 18 - font.getStringWidth(text);
                int textY = renderY + 9;
                font.drawStringWithShadow(text, textX, textY, 0xFF5555);

                GlStateManager.popMatrix();

                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
            }
        }
    }
}