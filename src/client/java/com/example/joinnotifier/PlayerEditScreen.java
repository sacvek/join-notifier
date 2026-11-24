package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

public class PlayerEditScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 278;

    private static final String[] COLOR_KEYS = {
            "GREEN", "AQUA", "GOLD", "LIGHT_PURPLE", "YELLOW", "RED", "WHITE"
    };
    private static final int[] COLOR_RGB = {
            0xFF55FF55, 0xFF55FFFF, 0xFFFFAA00, 0xFFFF55FF, 0xFFFFFF55, 0xFFFF5555, 0xFFFFFFFF
    };
    private static final String[] SOUND_KEYS = {
            "NONE", "BELL", "XP", "LEVEL", "CAT", "ANVIL", "NOTE"
    };

    private final Screen parent;
    private final PlayerEntry entry;

    private boolean notifyLeave;
    private String selectedColor;
    private String selectedSound;
    private EditBox tagInput;
    private EditBox groupInput;
    private EditBox serverInput;

    public PlayerEditScreen(Screen parent, PlayerEntry entry) {
        super(Component.translatable("gui.joinnotifier.edit.title", entry.name));
        this.parent = parent;
        this.entry = entry;
        this.notifyLeave = entry.notifyLeave;
        this.selectedColor = entry.color != null ? entry.color : "GREEN";
        this.selectedSound = entry.sound != null ? entry.sound : "NONE";
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int fullW = PANEL_WIDTH - 24;

        // 1. События (Вход / Вход и выход)
        this.addRenderableWidget(Button.builder(getEventButtonLabel(), button -> {
            this.notifyLeave = !this.notifyLeave;
            button.setMessage(getEventButtonLabel());
        }).bounds(left + 12, top + 30, fullW, 20).build());

        // 2. Тег игрока
        this.tagInput = new EditBox(this.font, left + 12, top + 66, fullW, 18,
                Component.translatable("gui.joinnotifier.edit.tag_label"));
        this.tagInput.setMaxLength(12);
        this.tagInput.setHint(Component.translatable("gui.joinnotifier.edit.tag_hint"));
        this.tagInput.setValue(this.entry.tag != null ? this.entry.tag : "");
        this.addRenderableWidget(this.tagInput);

        // 3. Группа / Категория
        this.groupInput = new EditBox(this.font, left + 12, top + 100, fullW, 18,
                Component.translatable("gui.joinnotifier.edit.group_label"));
        this.groupInput.setMaxLength(16);
        this.groupInput.setHint(Component.translatable("gui.joinnotifier.edit.group_hint"));
        this.groupInput.setValue(this.entry.group != null ? this.entry.group : "");
        this.addRenderableWidget(this.groupInput);

        // 4. Звук уведомления + кнопка прослушивания [▶]
        int playBtnW = 24;
        this.addRenderableWidget(Button.builder(getSoundButtonLabel(), button -> {
            cycleSound();
            button.setMessage(getSoundButtonLabel());
        }).bounds(left + 12, top + 162, fullW - playBtnW - 4, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("▶"), button -> {
            SoundEvent se = PlayerEntry.getSoundEventByKey(this.selectedSound);
            if (se != null && this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(se, 1.0F, 1.0F);
            }
        }).bounds(left + PANEL_WIDTH - 12 - playBtnW, top + 162, playBtnW, 20).build());

        // 5. Фильтр по серверу (IP) + кнопка "Текущий"
        int currentBtnW = 68;
        this.serverInput = new EditBox(this.font, left + 12, top + 200, fullW - currentBtnW - 4, 18,
                Component.translatable("gui.joinnotifier.edit.server_label"));
        this.serverInput.setMaxLength(64);
        this.serverInput.setHint(Component.translatable("gui.joinnotifier.edit.server_hint"));
        this.serverInput.setValue(this.entry.serverIp != null ? this.entry.serverIp : "");
        this.addRenderableWidget(this.serverInput);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.edit.current_server"), button -> {
            if (this.minecraft != null && this.minecraft.getCurrentServer() != null) {
                ServerData data = this.minecraft.getCurrentServer();
                if (data.ip != null) {
                    this.serverInput.setValue(PlayerEntry.normalizeIp(data.ip));
                }
            }
        }).bounds(left + PANEL_WIDTH - 12 - currentBtnW, top + 199, currentBtnW, 20).build());

        // 6. Сохранить
        this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.edit.save"), button -> {
            this.entry.notifyLeave = this.notifyLeave;
            this.entry.tag = this.tagInput.getValue().trim();
            this.entry.group = this.groupInput.getValue().trim();
            this.entry.color = this.selectedColor;
            this.entry.sound = this.selectedSound;
            this.entry.serverIp = PlayerEntry.normalizeIp(this.serverInput.getValue());
            ConfigManager.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(left + 12, top + PANEL_HEIGHT - 28, fullW, 20).build());
    }

    private Component getEventButtonLabel() {
        return Component.translatable(this.notifyLeave
                ? "gui.joinnotifier.edit.events_both"
                : "gui.joinnotifier.edit.events_join");
    }

    private void cycleSound() {
        for (int i = 0; i < SOUND_KEYS.length; i++) {
            if (SOUND_KEYS[i].equalsIgnoreCase(this.selectedSound)) {
                this.selectedSound = SOUND_KEYS[(i + 1) % SOUND_KEYS.length];
                return;
            }
        }
        this.selectedSound = "NONE";
    }

    private Component getSoundButtonLabel() {
        Component soundName = Component.translatable("gui.joinnotifier.sound." + this.selectedSound.toLowerCase());
        return Component.translatable("gui.joinnotifier.edit.sound_btn", soundName);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int textColor = UiHelper.getPrimaryTextColor();

        UiHelper.drawVanillaPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        int backX = left + 8;
        int backY = top + 6;
        boolean backHover = UiHelper.isInside(mouseX, mouseY, backX, backY, 18, 16);
        UiHelper.drawSmallButton(graphics, backX, backY, 18, 16, backHover);
        graphics.drawString(this.font, "←", backX + 5, backY + 4, 0xFFFFFFFF, true);

        int titleW = this.font.width(this.title);
        int headerX = left + (PANEL_WIDTH - titleW - 16) / 2;
        UiHelper.drawPlayerHead(graphics, this.entry.name, headerX, top + 8, 12);
        graphics.drawString(this.font, this.title, headerX + 16, top + 10, textColor, false);

        graphics.drawString(this.font, Component.translatable("gui.joinnotifier.edit.tag_label"), left + 12, top + 55, textColor, false);
        graphics.drawString(this.font, Component.translatable("gui.joinnotifier.edit.group_label"), left + 12, top + 89, textColor, false);
        graphics.drawString(this.font, Component.translatable("gui.joinnotifier.edit.color_label"), left + 12, top + 123, textColor, false);

        int swatchStartX = left + 12;
        int swatchY = top + 135;
        int swatchSize = 16;
        for (int i = 0; i < COLOR_KEYS.length; i++) {
            int sx = swatchStartX + i * (swatchSize + 5);
            boolean active = COLOR_KEYS[i].equalsIgnoreCase(this.selectedColor);
            graphics.fill(sx - 1, swatchY - 1, sx + swatchSize + 1, swatchY + swatchSize + 1,
                    active ? 0xFFFFFFFF : 0xFF222222);
            graphics.fill(sx, swatchY, sx + swatchSize, swatchY + swatchSize, COLOR_RGB[i]);
        }

        String previewTag = this.tagInput.getValue().trim();
        String previewText = (previewTag.isEmpty() ? "" : "[" + previewTag + "] ") + this.entry.name;
        int previewRgb = getSelectedRgb();
        int previewBoxX = swatchStartX + COLOR_KEYS.length * (swatchSize + 5) + 4;
        graphics.fill(previewBoxX, swatchY - 1, left + PANEL_WIDTH - 12, swatchY + swatchSize + 1, 0xFF1B1B1B);
        String shortPreview = this.font.plainSubstrByWidth(previewText, (left + PANEL_WIDTH - 16) - (previewBoxX + 4));
        graphics.drawString(this.font, shortPreview, previewBoxX + 4, swatchY + 4, previewRgb, true);

        graphics.drawString(this.font, Component.translatable("gui.joinnotifier.edit.server_label"), left + 12, top + 189, textColor, false);
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

            if (UiHelper.isInside(mx, my, left + 8, top + 6, 18, 16)) {
                if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                return true;
            }

            int swatchStartX = left + 12;
            int swatchY = top + 135;
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
