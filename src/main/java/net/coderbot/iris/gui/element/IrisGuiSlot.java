package net.coderbot.iris.gui.element;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

/**
 * Devilica: the list base of the shader pack screens on 1.12.2's {@link GuiSlot}. Angelica redraws the whole list in
 * {@code drawScreen} to fix 1.7.10's scrolling; 1.12.2's own drawing and mouse handling already do that, so this
 * keeps only Iris's changes: optional background, scrollbar at the right edge, a smaller header padding while drawing
 * entries, and clicks that know the mouse button.
 */
public abstract class IrisGuiSlot extends GuiSlot {
    @Setter @Getter protected boolean renderBackground = true;

    protected IrisGuiSlot(Minecraft mc, int width, int height, int top, int bottom, int slotHeight) {
        super(mc, width, height, top, bottom, slotHeight);
        this.centerListVertically = false;
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {
        if (this.renderBackground) {
            super.drawContainerBackground(tessellator);
        }
    }

    @Override
    protected int getScrollBarX() {
        // Position the scrollbar at the rightmost edge of the screen.
        return this.width - 6;
    }

    /** How far below its slot an entry is drawn: this padding of 2 while drawing plus 4 in the lists' drawSlot */
    protected static final int ENTRY_OFFSET = 6;

    // The bottom edge also shades 4 px, so the content runs past the slots, or the last row ends up under the buttons
    @Override
    protected int getContentHeight() {
        return super.getContentHeight() + ENTRY_OFFSET + 4;
    }

    // The slot under the mouse is where its entry is drawn, or the lower edge of a row clicks the row below
    @Override
    public int getSlotIndexFromScreenCoords(int posX, int posY) {
        return super.getSlotIndexFromScreenCoords(posX, posY - ENTRY_OFFSET);
    }

    @Override
    protected void drawSelectionBox(int x, int y, int mouseX, int mouseY, float partialTicks) {
        final int oldPadding = this.headerPadding;
        this.headerPadding = 2;
        super.drawSelectionBox(x, y, mouseX, mouseY, partialTicks);
        this.headerPadding = oldPadding;
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
        // Clicks come through mouseClicked, which knows the button
    }

    protected boolean elementClicked(int index, boolean doubleClick, int mouseX, int mouseY, int button) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.getEnabled() || !this.isMouseYWithinSlotBounds(mouseY)) {
            return false;
        }
        final int index = this.getSlotIndexFromScreenCoords(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        final boolean doubleClick = index == this.selectedElement && Minecraft.getSystemTime() - this.lastClicked < 250L;
        final boolean handled = this.elementClicked(index, doubleClick, mouseX, mouseY, mouseButton);
        this.selectedElement = index;
        this.lastClicked = Minecraft.getSystemTime();
        return handled;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        return false;
    }
}
