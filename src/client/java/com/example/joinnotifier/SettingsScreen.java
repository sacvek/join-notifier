package com.example.joinnotifier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 168;

    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("gui.joinnotifier.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int btnW = PANEL_WIDTH - 24;

        // 1. Режим уведомлений (Чат / Плашка / Чат + Плашка)
        this.addRenderableWidget(Button.builder(getModeLabel(), button -> {
            ConfigManager.cycleNotifyMode();
            button.setMessage(getModeLabel());
        }).bounds(left + 12, top + 34, btnW, 20).build());

        // 2. Стиль оформления (Классический / Классический чёрный)
        this.addRenderableWidget(Button.builder(getStyleLabel(), button -> {
            ConfigManager.cycleUiStyle();
            button.setMessage(getStyleLabel());
        }).bounds(left + 12, top + 62, btnW, 20).build());

        // 3. Кнопка входа в Админ-панель
        this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.settings.admin_panel"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new AdminLoginScreen(this));
            }
        }).bounds(left + 12, top + 90, btnW, 20).build());

        // 4. Кнопка "Готово"
        this.addRenderableWidget(Button.builder(Component.translatable("gui.joinnotifier.settings.done"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(left + 12, top + PANEL_HEIGHT - 30, btnW, 20).build());
    }

    private Component getModeLabel() {
        return switch (ConfigManager.getNotifyMode()) {
            case CHAT -> Component.translatable("gui.joinnotifier.mode.chat");
            case TOAST -> Component.translatable("gui.joinnotifier.mode.toast");
            case BOTH -> Component.translatable("gui.joinnotifier.mode.both");
        };
    }

    private Component getStyleLabel() {
        return switch (ConfigManager.getUiStyle()) {
            case CLASSIC -> Component.translatable("gui.joinnotifier.style.classic");
            case CLASSIC_DARK -> Component.translatable("gui.joinnotifier.style.classic_dark");
        };
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        UiHelper.drawVanillaPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        int backX = left + 8;
        int backY = top + 6;
        boolean backHover = UiHelper.isInside(mouseX, mouseY, backX, backY, 18, 16);
        UiHelper.drawSmallButton(graphics, backX, backY, 18, 16, backHover);
        graphics.drawString(this.font, "←", backX + 5, backY + 4, 0xFFFFFFFF, true);

        int titleW = this.font.width(this.title);
        graphics.drawString(this.font, this.title, left + (PANEL_WIDTH - titleW) / 2 + 6, top + 10, UiHelper.getPrimaryTextColor(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int left = (this.width - PANEL_WIDTH) / 2;
            int top = (this.height - PANEL_HEIGHT) / 2;
            if (UiHelper.isInside(event.x(), event.y(), left + 8, top + 6, 18, 16)) {
                if (this.minecraft != null) this.minecraft.setScreen(this.parent);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
