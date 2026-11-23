package com.example.joinnotifier;

import net.minecraft.ChatFormatting;
import java.util.Locale;

public class PlayerEntry {
    public String name;
    public boolean enabled = true;
    public boolean notifyLeave = false;
    public String tag = "";
    public String color = "GREEN";
    public String serverIp = "";

    public PlayerEntry() {
    }

    public PlayerEntry(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
        normalize();
    }

    public void normalize() {
        if (this.name == null) this.name = "";
        if (this.tag == null) this.tag = "";
        if (this.color == null || this.color.isEmpty()) this.color = "GREEN";
        if (this.serverIp == null) this.serverIp = "";
    }

    public ChatFormatting getChatFormatting() {
        return switch (this.color.toUpperCase(Locale.ROOT)) {
            case "AQUA" -> ChatFormatting.AQUA;
            case "GOLD" -> ChatFormatting.GOLD;
            case "LIGHT_PURPLE" -> ChatFormatting.LIGHT_PURPLE;
            case "YELLOW" -> ChatFormatting.YELLOW;
            case "RED" -> ChatFormatting.RED;
            case "WHITE" -> ChatFormatting.WHITE;
            default -> ChatFormatting.GREEN;
        };
    }

    public int getRgbColor() {
        return switch (this.color.toUpperCase(Locale.ROOT)) {
            case "AQUA" -> 0xFF55FFFF;
            case "GOLD" -> 0xFFFFAA00;
            case "LIGHT_PURPLE" -> 0xFFFF55FF;
            case "YELLOW" -> 0xFFFFFF55;
            case "RED" -> 0xFFFF5555;
            case "WHITE" -> 0xFFFFFFFF;
            default -> 0xFF55FF55;
        };
    }

    public boolean matchesServer(String currentNormalizedIp) {
        if (this.serverIp == null || this.serverIp.trim().isEmpty()) {
            return true; // Привязки нет — работает на всех серверах
        }
        if (currentNormalizedIp == null || currentNormalizedIp.isEmpty()) {
            return false;
        }
        String target = normalizeIp(this.serverIp);
        String currentHost = currentNormalizedIp.contains(":")
                ? currentNormalizedIp.substring(0, currentNormalizedIp.indexOf(':'))
                : currentNormalizedIp;
        return currentNormalizedIp.equals(target) || currentHost.equals(target);
    }

    public static String normalizeIp(String ip) {
        if (ip == null) return "";
        String s = ip.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith(":25565")) {
            s = s.substring(0, s.length() - 6);
        }
        return s;
    }
}