package net.coderbot.iris.gui.element;

import lombok.Getter;
import lombok.Setter;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.element.shaderselection.BaseEntry;
import net.coderbot.iris.gui.element.shaderselection.LabelEntry;
import net.coderbot.iris.gui.element.shaderselection.ShaderPackEntry;
import net.coderbot.iris.gui.element.shaderselection.TopButtonRowEntry;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ShaderPackSelectionList extends IrisGuiSlot {
    @Getter
    private final ShaderPackScreen screen;
    @Getter
    private final TopButtonRowEntry topButtonRow;

    @Setter
    @Getter
    private ShaderPackEntry applied = null;

    @Setter
    @Getter
    private ShaderPackEntry selected = null;

    private final List<BaseEntry> entries = new ArrayList<>();

    public ShaderPackSelectionList(ShaderPackScreen screen, Minecraft client, int width, int height, int top, int bottom, int left, int right) {
        super(client, width, height, top, bottom, 20);

        this.screen = screen;
        this.topButtonRow = new TopButtonRowEntry(this, Iris.getIrisConfig().areShadersEnabled());

        refresh();
    }

    public void refresh() {
        this.entries.clear();

        final Collection<String> names;

        try {
            names = Iris.getShaderpacksDirectoryManager().enumerate();
        } catch (Exception e) {
            Iris.logger.error("Error reading files while constructing selection UI", e);

            this.addLabelEntries(
                "",
                I18n.format("options.iris.shaderPackSelection.readError"),
                "",
                I18n.format("options.iris.shaderPackSelection.readError.logs"),
                I18n.format("options.iris.shaderPackSelection.readError.permissions")
            );

            return;
        }

        this.entries.add(topButtonRow);

        // Only allow the enable/disable shaders button if the user has added a shader pack. Otherwise, the button will be disabled.
        topButtonRow.allowEnableShadersButton = names.size() > 0;

        int index = 0;

        for (String name : names) {
            index++;
            addPackEntry(index, name);
        }

    }

    public void addPackEntry(int index, String name) {
        final ShaderPackEntry entry = new ShaderPackEntry(this, name);

        Iris.getIrisConfig().getShaderPackName().ifPresent(currentPackName -> {
            if (name.equals(currentPackName)) {
                setSelected(entry);
                setApplied(entry);
            }
        });

        this.entries.add(entry);
    }

    public void addLabelEntries(String ... lines) {
        for (String text : lines) {
            this.entries.add(new LabelEntry(this, text));
        }
    }

    public void select(String name) {
        for (BaseEntry entry : this.entries) {
            if (entry instanceof ShaderPackEntry shaderPackEntry && name.equals(shaderPackEntry.getPackName())) {
                setSelected(shaderPackEntry);
                return;
            }
        }
    }

    @Override
    protected int getSize() {
        return this.entries.size();
    }

    @Override
    protected boolean elementClicked(int index, boolean doubleClick, int mouseX, int mouseY, int mouseButton) {
        // Only do anything on left-click
        if (mouseButton != 0) {
            return false;
        }
        final BaseEntry entry = this.entries.get(index);
        if(entry instanceof ShaderPackEntry shaderPackEntry) {
            this.setSelected(shaderPackEntry);
            if (!topButtonRow.shadersEnabled) {
                topButtonRow.setShadersEnabled(true);
            }
            return true;
        } else if( entry instanceof TopButtonRowEntry topButtonRowEntry) {
            return topButtonRowEntry.mouseClicked(mouseX, mouseY, 0);
        }
        return false;
   }

    @Override
    protected boolean isSelected(int idx) {
        return this.entries.get(idx).equals(this.selected);
    }

    @Override
    public int getListWidth() {
        return Math.min(308, width - 50);
    }

    @Override
    protected void drawBackground() {
        // Do nothing
    }


    @Override
    protected void drawSlot(int index, int x, int y, int i1, int mouseX, int mouseY, float partialTicks) {
        final Tessellator tessellator = Tessellator.getInstance();
        final BaseEntry entry = this.entries.get(index);
        final boolean isMouseOver = this.getSlotIndexFromScreenCoords(mouseX, mouseY) == index;
        entry.drawEntry(screen, index, x - 2, y + 4, this.getListWidth(), tessellator, mouseX, mouseY, isMouseOver);
    }

}
