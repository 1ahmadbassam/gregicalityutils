package top.ahmadb.gregicalityutils;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import java.io.IOException;

public class GuiCapabilityProxy extends GuiScreen {
    private final TileEntityCapabilityProxy te;
    
    // We keep individual references for easy parsing
    private GuiTextField itemInX, itemInY, itemInZ;
    private GuiTextField fluidInX, fluidInY, fluidInZ;
    private GuiTextField itemOutX, itemOutY, itemOutZ;
    private GuiTextField fluidOutX, fluidOutY, fluidOutZ;
    
    // Array to make drawing and input handling much cleaner
    private final GuiTextField[] allFields = new GuiTextField[12];

    public GuiCapabilityProxy(TileEntityCapabilityProxy te) {
        this.te = te;
    }

    @Override
    public void initGui() {
        super.initGui();
        int cx = width / 2;
        int cy = height / 2;
        
        int boxWidth = 40;
        int boxHeight = 16;
        
        int xPos = cx - 40;
        int yPos = cx + 5;
        int zPos = cx + 50;

        int row1 = cy - 55;
        int row2 = cy - 30;
        int row3 = cy - 5;
        int row4 = cy + 20;

        int id = 0;
        // Item In
        allFields[0] = itemInX = new GuiTextField(id++, fontRenderer, xPos, row1, boxWidth, boxHeight);
        allFields[1] = itemInY = new GuiTextField(id++, fontRenderer, yPos, row1, boxWidth, boxHeight);
        allFields[2] = itemInZ = new GuiTextField(id++, fontRenderer, zPos, row1, boxWidth, boxHeight);
        
        // Fluid In
        allFields[3] = fluidInX = new GuiTextField(id++, fontRenderer, xPos, row2, boxWidth, boxHeight);
        allFields[4] = fluidInY = new GuiTextField(id++, fontRenderer, yPos, row2, boxWidth, boxHeight);
        allFields[5] = fluidInZ = new GuiTextField(id++, fontRenderer, zPos, row2, boxWidth, boxHeight);
        
        // Item Out
        allFields[6] = itemOutX = new GuiTextField(id++, fontRenderer, xPos, row3, boxWidth, boxHeight);
        allFields[7] = itemOutY = new GuiTextField(id++, fontRenderer, yPos, row3, boxWidth, boxHeight);
        allFields[8] = itemOutZ = new GuiTextField(id++, fontRenderer, zPos, row3, boxWidth, boxHeight);
        
        // Fluid Out
        allFields[9] = fluidOutX = new GuiTextField(id++, fontRenderer, xPos, row4, boxWidth, boxHeight);
        allFields[10] = fluidOutY = new GuiTextField(id++, fontRenderer, yPos, row4, boxWidth, boxHeight);
        allFields[11] = fluidOutZ = new GuiTextField(id++, fontRenderer, zPos, row4, boxWidth, boxHeight);

        // Pre-fill fields with existing data
        fillPos(te.itemIn, itemInX, itemInY, itemInZ);
        fillPos(te.fluidIn, fluidInX, fluidInY, fluidInZ);
        fillPos(te.itemOut, itemOutX, itemOutY, itemOutZ);
        fillPos(te.fluidOut, fluidOutX, fluidOutY, fluidOutZ);

        buttonList.add(new GuiButton(0, cx - 50, cy + 55, 100, 20, "Save & Close"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int cx = width / 2;
        int cy = height / 2;

        // Title
        drawCenteredString(fontRenderer, "Capability I/O Proxy", cx, cy - 90, 0xFFFFFF);
        
        // Column Headers
        drawCenteredString(fontRenderer, "X", cx - 20, cy - 70, 0xAAAAAA);
        drawCenteredString(fontRenderer, "Y", cx + 25, cy - 70, 0xAAAAAA);
        drawCenteredString(fontRenderer, "Z", cx + 70, cy - 70, 0xAAAAAA);

        // Row Labels
        drawString(fontRenderer, "Item In:", cx - 95, cy - 51, 0xCCCCCC);
        drawString(fontRenderer, "Fluid In:", cx - 95, cy - 26, 0xCCCCCC);
        drawString(fontRenderer, "Item Out:", cx - 95, cy - 1, 0xCCCCCC);
        drawString(fontRenderer, "Fluid Out:", cx - 95, cy + 24, 0xCCCCCC);

        // Draw all 12 text boxes
        for (GuiTextField field : allFields) {
            field.drawTextBox();
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (GuiTextField field : allFields) {
            // Only capture the event if a text box is focused, preventing 'E' from instantly closing the GUI
            if (field.textboxKeyTyped(typedChar, keyCode)) {
                return; 
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (GuiTextField field : allFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            GregicalityUtils.NETWORK.sendToServer(new PacketUpdateCapabilityProxy(
                te.getPos(),
                parsePos(itemInX, itemInY, itemInZ),
                parsePos(fluidInX, fluidInY, fluidInZ),
                parsePos(itemOutX, itemOutY, itemOutZ),
                parsePos(fluidOutX, fluidOutY, fluidOutZ)
            ));
            mc.displayGuiScreen(null);
        }
    }

    private void fillPos(BlockPos pos, GuiTextField xField, GuiTextField yField, GuiTextField zField) {
        if (pos != null) {
            xField.setText(String.valueOf(pos.getX()));
            yField.setText(String.valueOf(pos.getY()));
            zField.setText(String.valueOf(pos.getZ()));
        }
    }

    private BlockPos parsePos(GuiTextField xField, GuiTextField yField, GuiTextField zField) {
        String xTxt = xField.getText().trim();
        String yTxt = yField.getText().trim();
        String zTxt = zField.getText().trim();
        
        // If all fields in a row are empty, return null to represent an unlinked channel
        if (xTxt.isEmpty() && yTxt.isEmpty() && zTxt.isEmpty()) return null;
        
        try {
            return new BlockPos(Integer.parseInt(xTxt), Integer.parseInt(yTxt), Integer.parseInt(zTxt));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}