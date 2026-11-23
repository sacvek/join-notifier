package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public class PlayerEditScreen extends Screen {
    private static final int PANEL_WIDTH = 276;
    private static final int PANEL_HEIGHT = 216;

    private static final String[] COLOR_KEYS = {
            "GREEN", "AQUA", "GOLD", "LIGHT_PURPLE", "YELLOW", "RED", "WHITE"
    };
    private static final int[] COLOR_RGB = {
            0xFF55FF55, 0xFF55FFFF, 0xFFFFAA00, 0xFFFF55FF, 0xFFFFFF55, 0xFFFF5555, 0xFFFFFFFF
    };

    private final Screen parent;
    private final PlayerEntry entry;

    private boolean notifyLeave;
    private String selectedColor;
    private EditBox tagInput;
    private EditBox serverInput;
    private Button eventModeButton;

    public PlayerEditScreen(Screen parent, PlayerEntry entry) {
        super(Component.literal("Настройка: " + entry.name));
        this.parent = parent;
        this.entry = entry;
        this.notifyLeave = entry.notifyLeave;
        this.selectedColor = entry.color != null ? entry.color : "GREEN";
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        // 1. Кнопка переключения событий: Только вход / Вход и выход
        this.eventModeButton = Button.builder(getEventButtonLabel(), button -> {
            this.notifyLeave = !this.notifyLeave;
            button.setMessage(getEventButtonLabel());
        }).bounds(left + 12, top + 34, PANEL_WIDTH - 24, 20).build();
        this.addRenderableWidget(this.eventModeButton);

        // 2. Поле ввода кастомного тега (например: Друг)
        this.tagInput = new EditBox(
                this.font,
                left + 12,
                top + 72,
                PANEL_WIDTH - 24,
                18,
                Component.literal("Тег")
        );
        this.tagInput.setMaxLength(12);
        this.tagInput.setHint(Component.literal("Тег в чате (например: Друг)"));
        this.tagInput.setValue(this.entry.tag != null ? this.entry.tag : "");
        this.addRenderableWidget(this.tagInput);

        // 3. Поле IP сервера + кнопка "Текущий"
        int currentBtnW = 64;
        this.serverInput = new EditBox(
                this.font,
                left + 12,
                top + 138,
                PANEL_WIDTH - 28 - currentBtnW,
                18,
                Component.literal("IP сервера")
        );
        this.serverInput.setMaxLength(64);
        this.serverInput.setHint(Component.literal("Пусто = все серверы"));
        this.serverInput.setValue(this.entry.serverIp != null ? this.entry.serverIp : "");
        this.addRenderableWidget(this.serverInput);

        this.addRenderableWidget(Button.builder(Component.literal("Текущий"), button -> {
            if (this.minecraft != null && this.minecraft.getCurrentServer() != null) {
                ServerData data = this.minecraft.getCurrentServer();
                if (data.ip != null) {
                    this.serverInput.setValue(PlayerEntry.normalizeIp(data.ip));
                }
            }
        }).bounds(left + PANEL_WIDTH - 12 - currentBtnW, top + 137, currentBtnW, 20).build());

        // 4. Кнопка Сохранить снизу
        this.addRenderableWidget(Button.builder(Component.literal("Сохранить"), button -> {
            this.entry.notifyLeave = this.notifyLeave;
            this.entry.tag = this.tagInput.getValue().trim();
            this.entry.color = this.selectedColor;
            this.entry.serverIp = PlayerEntry.normalizeIp(this.serverInput.getValue());
            ConfigManager.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(left + 12, top + PANEL_HEIGHT - 30, PANEL_WIDTH - 24, 20).build());
    }

    private Component getEventButtonLabel() {
        return Component.literal(this.notifyLeave
                ? "События: Вход и выход с сервера"
                : "События: Только вход на сервер");
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        UiHelper.drawVanillaPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        // Кнопка назад
        int backX = left + 8;
        int backY = top + 6;
        boolean backHover = UiHelper.isInside(mouseX, mouseY, backX, backY, 18, 16);
        UiHelper.drawSmallButton(graphics, backX, backY, 18, 16, backHover);
        graphics.drawString(this.font, "←", backX + 5, backY + 4, 0xFFFFFFFF, true);

        // Голова игрока и заголовок
        int titleW = this.font.width(this.title);
        int headerX = left + (PANEL_WIDTH - titleW - 16) / 2;
        UiHelper.drawPlayerHead(graphics, this.entry.name, headerX, top + 8, 12);
        graphics.drawString(this.font, this.title, headerX + 16, top + 10, 0xFF404040, false);

        // Подписи секций
        graphics.drawString(this.font, "Тег игрока (необязательно):", left + 12, top + 60, 0xFF404040, false);
        graphics.drawString(this.font, "Цвет подсветки ника:", left + 12, top + 96, 0xFF404040, false);

        // Палитра из 7 цветов
        int swatchStartX = left + 12;
        int swatchY = top + 108;
        int swatchSize = 16;
        for (int i = 0; i < COLOR_KEYS.length; i++) {
            int sx = swatchStartX + i * (swatchSize + 5);
            boolean active = COLOR_KEYS[i].equalsIgnoreCase(this.selectedColor);
            graphics.fill(sx - 1, swatchY - 1, sx + swatchSize + 1, swatchY + swatchSize + 1,
                    active ? 0xFFFFFFFF : 0xFF222222);
            graphics.fill(sx, swatchY, sx + swatchSize, swatchY + swatchSize, COLOR_RGB[i]);
        }

        // Предпросмотр того, как ник будет выглядеть в чате
        String previewTag = this.tagInput.getValue().trim();
        String previewText = (previewTag.isEmpty() ? "" : "[" + previewTag + "] ") + this.entry.name;
        int previewRgb = getSelectedRgb();
        int previewBoxX = swatchStartX + COLOR_KEYS.length * (swatchSize + 5) + 4;
        graphics.fill(previewBoxX, swatchY - 1, left + PANEL_WIDTH - 12, swatchY + swatchSize + 1, 0xFF2B2B2B);
        String shortPreview = this.font.plainSubstrByWidth(previewText, (left + PANEL_WIDTH - 16) - (previewBoxX + 4));
        graphics.drawString(this.font, shortPreview, previewBoxX + 4, swatchY + 4, previewRgb, true);

        graphics.drawString(this.font, "Фильтр по серверу (IP):", left + 12, top + 127, 0xFF404040, false);
    }

    private int getSelectedRgb() {
        for (int i = 0; i < COLOR_KEYS.length; i++) {
            if (COLOR_KEYS[i].equalsIgnoreCase(this.selectedColor)) {
                return COLOR_RGB[i];
            }
        }
        return 0xFF55FF55;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();

            int left = (this.width - PANEL_WIDTH) / 2;
            int top = (this.height - PANEL_HEIGHT) / 2;

            // Кнопка назад
            if (UiHelper.isInside(mx, my, left + 8, top + 6, 18, 16)) {
                if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                return true;
            }

            // Клик по квадратику цвета
            int swatchStartX = left + 12;
            int swatchY = top + 108;
            int swatchSize = 16;
            for (int i = 0; i < COLOR_KEYS.length; i++) {
                int sx = swatchStartX + i * (swatchSize + 5);
                if (UiHelper.isInside(mx, my, sx - 1, swatchY - 1, swatchSize + 2, swatchSize + 2)) {
                    this.selectedColor = COLOR_KEYS[i];
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}