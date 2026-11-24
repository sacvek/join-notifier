package com.example.joinnotifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;

public class UiHelper {
    public static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static boolean isDarkTheme() {
        return ConfigManager.getUiStyle() == ConfigManager.UiStyle.CLASSIC_DARK;
    }

    public static int getPrimaryTextColor() {
        return isDarkTheme() ? 0xFFEAEAEA : 0xFF404040;
    }

    public static int getSecondaryTextColor() {
        return isDarkTheme() ? 0xFFA8A8A8 : 0xFF555555;
    }

    public static void drawVanillaPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (isDarkTheme()) {
            graphics.fill(x + 1, y, x + width - 1, y + height, 0xFF0A0A0A);
            graphics.fill(x, y + 1, x + width, y + height - 1, 0xFF0A0A0A);
            graphics.fill(x + 1, y + 1, x + width - 2, y + 3, 0xFF4A4A4A);
            graphics.fill(x + 1, y + 1, x + 3, y + height - 2, 0xFF4A4A4A);
            graphics.fill(x + 2, y + height - 3, x + width - 1, y + height - 1, 0xFF141414);
            graphics.fill(x + width - 3, y + 2, x + width - 1, y + height - 1, 0xFF141414);
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFF242424);
        } else {
            graphics.fill(x + 1, y, x + width - 1, y + height, 0xFF000000);
            graphics.fill(x, y + 1, x + width, y + height - 1, 0xFF000000);
            graphics.fill(x + 1, y + 1, x + width - 2, y + 3, 0xFFFFFFFF);
            graphics.fill(x + 1, y + 1, x + 3, y + height - 2, 0xFFFFFFFF);
            graphics.fill(x + 2, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
            graphics.fill(x + width - 3, y + 2, x + width - 1, y + height - 1, 0xFF555555);
            graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFFC6C6C6);
        }
    }

    public static void drawInsetBox(GuiGraphics graphics, int x, int y, int width, int height) {
        if (isDarkTheme()) {
            graphics.fill(x, y, x + width, y + height, 0xFF121212);
            graphics.fill(x + 1, y + 1, x + width, y + height, 0xFF3E3E3E);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF181818);
        } else {
            graphics.fill(x, y, x + width, y + height, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF555555);
        }
    }

    public static void drawSmallButton(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered) {
        if (isDarkTheme()) {
            graphics.fill(x, y, x + w, y + h, 0xFF0A0A0A);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFF4C4C4C : 0xFF363636);
            graphics.fill(x + 1, y + 1, x + w - 2, y + 2, 0xFF606060);
            graphics.fill(x + 1, y + 1, x + 2, y + h - 2, 0xFF606060);
            graphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xFF1E1E1E);
            graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF1E1E1E);
        } else {
            graphics.fill(x, y, x + w, y + h, 0xFF000000);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? 0xFFA0A0A0 : 0xFF8B8B8B);
            graphics.fill(x + 1, y + 1, x + w - 2, y + 2, 0xFFCCCCCC);
            graphics.fill(x + 1, y + 1, x + 2, y + h - 2, 0xFFCCCCCC);
            graphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xFF444444);
            graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF444444);
        }
    }

    public static void drawRedSlash(GuiGraphics graphics, int x, int y, int length) {
        for (int i = 0; i < length; i++) {
            int px = x + i;
            int py = y + length - 1 - i;
            graphics.fill(px, py, px + 2, py + 2, 0xFFFF1E1E);
        }
    }

    public static void drawLightbulbIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 2, y, x + 6, y + 1, 0xFFFFD83D);
        graphics.fill(x + 1, y + 1, x + 7, y + 5, 0xFFFFD83D);
        graphics.fill(x + 2, y + 5, x + 6, y + 6, 0xFFE5B822);
        graphics.fill(x + 2, y + 1, x + 3, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 6, x + 6, y + 8, 0xFF4A4A4A);
        graphics.fill(x + 3, y + 8, x + 5, y + 9, 0xFF2B2B2B);
    }

    public static void drawGearIcon(GuiGraphics graphics, int x, int y) {
        int c = 0xFFE6E6E6;
        graphics.fill(x + 3, y, x + 5, y + 8, c);
        graphics.fill(x, y + 3, x + 8, y + 5, c);
        graphics.fill(x + 1, y + 1, x + 7, y + 7, c);
        graphics.fill(x + 3, y + 3, x + 5, y + 5, 0xFF3A3A3A);
    }

    public static void drawBellIcon(GuiGraphics graphics, int x, int y, boolean muted) {
        int mainColor = muted ? 0xFF999999 : 0xFFFFD83D;
        int darkColor = muted ? 0xFF666666 : 0xFFD9A31E;
        graphics.fill(x + 3, y, x + 5, y + 1, darkColor);
        graphics.fill(x + 2, y + 1, x + 6, y + 3, mainColor);
        graphics.fill(x + 1, y + 3, x + 7, y + 6, mainColor);
        graphics.fill(x, y + 6, x + 8, y + 7, darkColor);
        graphics.fill(x + 3, y + 7, x + 5, y + 9, darkColor);
    }

    public static boolean isPlayerOnline(String playerName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && playerName != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                if (info.getProfile() != null && playerName.equalsIgnoreCase(info.getProfile().name())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void drawOnlineDot(GuiGraphics graphics, int x, int y, boolean online) {
        graphics.fill(x, y, x + 4, y + 4, 0xFF111111);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, online ? 0xFF4CFF4C : 0xFF888888);
    }

    public static void drawPlayerHead(GuiGraphics graphics, String playerName, int x, int y, int size) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && playerName != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                if (info.getProfile() != null && playerName.equalsIgnoreCase(info.getProfile().name())) {
                    PlayerFaceRenderer.draw(graphics, info.getSkin(), x, y, size);
                    return;
                }
            }
        }
        graphics.fill(x, y, x + size, y + size, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFFB88458);
        graphics.fill(x + 1, y + 1, x + size - 1, y + 4, 0xFF3B2716);
        graphics.fill(x + 2, y + 5, x + 4, y + 7, 0xFFFFFFFF);
        graphics.fill(x + 3, y + 5, x + 4, y + 7, 0xFF3F3080);
        graphics.fill(x + size - 4, y + 5, x + size - 2, y + 7, 0xFFFFFFFF);
        graphics.fill(x + size - 4, y + 5, x + size - 3, y + 7, 0xFF3F3080);
        graphics.fill(x + 4, y + 8, x + size - 4, y + 10, 0xFF7A4B32);
    }
}
