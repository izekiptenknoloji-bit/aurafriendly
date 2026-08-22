package com.izekip.izessentials.client.config.screen;

import com.izekip.izessentials.client.config.ShortcutEntry;
import com.izekip.izessentials.client.gui.AuraTheme;
import com.izekip.izessentials.client.core.MacroSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 12 macro slotunu duzenlenebilir satirlar olarak gosteren liste (1.21.4 varyanti).
 * NOT: 1.21.4'te AbstractSelectionList.Entry'nin override noktasi "render(...)"
 * (9 parametreli, konum/boyut parametre olarak gelir) - 1.21.11'deki "renderContent"
 * ve getContentX()/getContentWidth() yardimcilari bu surumde yok, o yuzden bu dosya
 * 1.21.11 varyanindan ayri tutuluyor.
 */
public class ShortcutListWidget extends ContainerObjectSelectionList<ShortcutListWidget.Entry> {
	public static final int ROW_HEIGHT = 26;

	public ShortcutListWidget(Minecraft minecraft, int width, int height, int y0, Font font, List<ShortcutEntry> shortcuts) {
		super(minecraft, width, height, y0, ROW_HEIGHT);
		for (int i = 0; i < shortcuts.size() && i < MacroSlots.SLOTS.length; i++) {
			addEntry(new Entry(font, shortcuts.get(i), MacroSlots.SLOTS[i], i + 1));
		}
	}

	@Override
	public int getRowWidth() {
		return Math.min(400, getWidth() - 12);
	}

	public static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
		private static final int INFO_WIDTH = 70;
		private static final int LABEL_WIDTH = 70;
		private static final int GAP = 4;

		private final Font font;
		private final KeyMapping keyMapping;
		private final int slotNumber;
		private final EditBox labelBox;
		private final EditBox commandBox;
		private final Checkbox enabledBox;
		private final List<AbstractWidget> widgets;

		Entry(Font font, ShortcutEntry data, KeyMapping keyMapping, int slotNumber) {
			this.font = font;
			this.keyMapping = keyMapping;
			this.slotNumber = slotNumber;

			this.labelBox = new EditBox(font, 0, 0, LABEL_WIDTH, 18, Component.literal("label"));
			this.labelBox.setMaxLength(24);
			this.labelBox.setValue(data.label);
			this.labelBox.setResponder(value -> data.label = value);

			this.commandBox = new EditBox(font, 0, 0, 60, 18, Component.literal("command"));
			this.commandBox.setMaxLength(256);
			this.commandBox.setValue(data.commandText);
			this.commandBox.setHint(Component.literal("/home ya da duz mesaj"));
			this.commandBox.setResponder(value -> data.commandText = value);

			this.enabledBox = Checkbox.builder(Component.literal(""), font)
					.selected(data.enabled)
					.onValueChange((checkbox, value) -> data.enabled = value)
					.build();

			this.widgets = List.of(labelBox, commandBox, enabledBox);
		}

		@Override
		public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
				int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
			guiGraphics.drawString(font, "Slot " + slotNumber, left, top, AuraTheme.TEXT_WHITE);
			guiGraphics.drawString(font, keyMapping.getTranslatedKeyMessage(), left, top + 11, AuraTheme.TEXT_MUTED);

			int enabledWidth = Math.max(enabledBox.getWidth(), 14);
			int commandX = left + INFO_WIDTH + LABEL_WIDTH + GAP * 2;
			int commandWidth = Math.max(40, width - INFO_WIDTH - LABEL_WIDTH - enabledWidth - GAP * 3);

			labelBox.setX(left + INFO_WIDTH);
			labelBox.setY(top);
			commandBox.setX(commandX);
			commandBox.setY(top);
			commandBox.setWidth(commandWidth);
			enabledBox.setX(left + width - enabledWidth);
			enabledBox.setY(top);

			labelBox.render(guiGraphics, mouseX, mouseY, partialTick);
			commandBox.render(guiGraphics, mouseX, mouseY, partialTick);
			enabledBox.render(guiGraphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return widgets;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return widgets;
		}

		@Override
		public boolean isDragging() {
			return false;
		}

		@Override
		public void setDragging(boolean dragging) {
			// Bu satirlarda surukleme davranisi yok.
		}
	}
}
