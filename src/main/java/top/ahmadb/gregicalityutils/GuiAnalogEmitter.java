package top.ahmadb.gregicalityutils;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class GuiAnalogEmitter extends GuiScreen {
    private final BlockPos pos;
    private TileEntityAnalogEmitter te;

    public GuiAnalogEmitter(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void initGui() {
        super.initGui();
        TileEntity tile = this.mc.world.getTileEntity(pos);
        if (tile instanceof TileEntityAnalogEmitter) {
            this.te = (TileEntityAnalogEmitter) tile;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Button ID 0 is minus, ID 1 is plus
        this.buttonList.add(new GuiButton(0, centerX - 50, centerY, 40, 20, "-"));
        this.buttonList.add(new GuiButton(1, centerX + 10, centerY, 40, 20, "+"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (this.te == null) return;
        
        int current = te.getSignalLevel();
        if (button.id == 0 && current > 1) {
            current--;
        } else if (button.id == 1 && current < 15) {
            current++;
        }
        
        te.setSignalLevel(current);
        // Assuming you setup a simple network wrapper in your main class called NETWORK
        GregicalityUtils.NETWORK.sendToServer(new PacketUpdateEmitter(pos, current));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        if (this.te != null) {
            this.drawCenteredString(this.fontRenderer, "Signal Level: " + te.getSignalLevel(), this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}